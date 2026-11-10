package com.mothblank.lumorareader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.mothblank.lumorareader.data.db.dao.PdfSearchDao
import com.mothblank.lumorareader.data.db.dao.PdfSearchHit
import com.mothblank.lumorareader.data.db.entity.PdfIndexStatusEntity
import com.mothblank.lumorareader.data.db.entity.PdfSearchFtsEntity
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import timber.log.Timber
import java.io.Writer
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

/** PDFBox-backed page search, resumable and independent of raster rendering. */
@Singleton
class PdfSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PdfSearchDao
) {
    private val indexMutex = Mutex()
    private val activeBook = MutableStateFlow<String?>(null)

    fun search(raw: String): Flow<List<PdfSearchHit>> {
        val tokens = Regex("""[\p{L}\p{N}]{2,}""").findAll(raw).take(8).map { it.value }.toList()
        if (tokens.isEmpty()) return flowOf(emptyList())
        return dao.search(tokens.joinToString(" AND ") { "\"" + it + "\"" })
    }

    fun statuses(): Flow<List<PdfIndexStatusEntity>> =
        combine(dao.observeStatuses(), activeBook) { statuses, active ->
            statuses.map { status ->
                if (status.outcome == "IN_PROGRESS" && status.bookUri != active)
                    status.copy(outcome = "INTERRUPTED") else status
            }
        }

    suspend fun indexBook(uri: String, prioritize: Boolean = false, restart: Boolean = false) = indexMutex.withLock {
        withContext(Dispatchers.IO) {
            activeBook.value = uri
            var checkpoint: PdfIndexStatusEntity? = null
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                val scratch = MemoryUsageSetting.setupMixed(8L * 1024 * 1024).apply {
                    setTempDir(context.cacheDir)
                }
                val input = context.contentResolver.openInputStream(Uri.parse(uri))
                    ?: throw java.io.FileNotFoundException("PDF document is inaccessible")
                input.use { stream ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    // PDFBox consumes the input stream while constructing the document.
                    // Hash those same bytes, avoiding another potentially expensive provider read.
                    val hashingStream = DigestInputStream(stream, digest)
                    PDDocument.load(hashingStream, scratch).use { document ->
                        val total = document.numberOfPages
                        val fingerprint = sourceFingerprint(uri, total, digest.digest())
                        val previous = dao.getStatus(uri)
                        var status = if (restart || previous == null || previous.fingerprint != fingerprint ||
                            (prioritize && previous.truncatedPages > 0)
                        ) {
                            dao.deleteForBook(uri)
                            PdfIndexStatusEntity(uri, fingerprint, 0, total, 0, 0, 0,
                                System.currentTimeMillis(), "IN_PROGRESS").also { dao.upsertStatus(it) }
                        } else previous
                        checkpoint = status
                        if (status.outcome == "FULL" || status.outcome == "IMAGE_ONLY" ||
                            status.outcome.startsWith("OCR_") ||
                            (status.outcome == "PARTIAL" && !prioritize)) return@use
                        val budget = if (prioritize) PRIORITY_BOOK_CHARS else DEFAULT_BOOK_CHARS
                        val maxPages = if (prioritize) PRIORITY_PAGES else DEFAULT_PAGES
                        val pageCap = if (prioritize) PRIORITY_PAGE_CHARS else DEFAULT_PAGE_CHARS
                        val stripper = PDFTextStripper().apply { sortByPosition = true }
                        var page = status.nextPageIndex
                        while (page < total && page < maxPages && status.indexedChars < budget) {
                            currentCoroutineContext().ensureActive()
                            val output = LimitedWriter(minOf(pageCap, budget - status.indexedChars))
                            stripper.startPage = page + 1
                            stripper.endPage = page + 1
                            stripper.writeText(document, output)
                            val text = output.value().trim()
                            dao.deletePage(uri, page)
                            if (text.isNotBlank()) dao.insert(PdfSearchFtsEntity(uri, page, text))
                            status = status.copy(
                                nextPageIndex = page + 1,
                                indexedChars = status.indexedChars + text.length,
                                searchablePages = status.searchablePages + if (text.isNotBlank()) 1 else 0,
                                truncatedPages = status.truncatedPages + if (output.truncated) 1 else 0,
                                updatedAt = System.currentTimeMillis(),
                                outcome = "IN_PROGRESS"
                            )
                            checkpoint = status
                            dao.upsertStatus(status)
                            page++
                            if (page % 12 == 0) prune(uri)
                            yield()
                        }
                        status = status.copy(
                            outcome = when {
                                page < total || status.truncatedPages > 0 -> "PARTIAL"
                                status.searchablePages == 0 -> "IMAGE_ONLY"
                                else -> "FULL"
                            },
                            updatedAt = System.currentTimeMillis()
                        )
                        checkpoint = status
                        dao.upsertStatus(status)
                        prune(uri)
                    }
                }
            } catch (cancelled: CancellationException) {
                checkpoint?.let {
                    withContext(NonCancellable) {
                        dao.upsertStatus(it.copy(outcome = "INTERRUPTED",
                            updatedAt = System.currentTimeMillis()))
                    }
                }
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "PDF text index paused")
                checkpoint?.let { dao.upsertStatus(it.copy(outcome = "INTERRUPTED",
                    updatedAt = System.currentTimeMillis())) }
            } finally {
                activeBook.value = null
            }
        }
    }

    /**
     * Opt-in OCR for PDFs that have no extractable text.
     *
     * A normal index pass is performed first so the source fingerprint is revalidated.
     * OCR checkpoints use OCR_* outcomes, keeping recognized text distinguishable from
     * directly extracted PDF text without duplicating the FTS storage schema.
     */
    suspend fun ocrBook(uri: String, restart: Boolean = false) {
        indexBook(uri, prioritize = true)
        indexMutex.withLock {
            withContext(Dispatchers.IO) {
                activeBook.value = uri
                var checkpoint: PdfIndexStatusEntity? = null
                var recognizer: com.google.mlkit.vision.text.TextRecognizer? = null
                try {
                    val previous = dao.getStatus(uri) ?: return@withContext
                    val isOcr = previous.outcome.startsWith("OCR_")
                    if (!isOcr && previous.outcome != "IMAGE_ONLY") return@withContext
                    if (previous.outcome == "OCR_FULL" && !restart) return@withContext

                    var status = if (restart || !isOcr || previous.outcome == "OCR_EMPTY") {
                        dao.deleteForBook(uri)
                        previous.copy(
                            nextPageIndex = 0,
                            indexedChars = 0,
                            searchablePages = 0,
                            truncatedPages = 0,
                            updatedAt = System.currentTimeMillis(),
                            outcome = "OCR_IN_PROGRESS"
                        ).also { dao.upsertStatus(it) }
                    } else {
                        previous.copy(
                            updatedAt = System.currentTimeMillis(),
                            outcome = "OCR_IN_PROGRESS"
                        ).also { dao.upsertStatus(it) }
                    }
                    checkpoint = status

                    val descriptor = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")
                        ?: throw java.io.FileNotFoundException("PDF document is inaccessible")
                    descriptor.use { fileDescriptor ->
                        PdfRenderer(fileDescriptor).use { renderer ->
                            val total = minOf(renderer.pageCount, status.totalPages)
                            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                            var pageIndex = status.nextPageIndex.coerceAtMost(total)
                            while (pageIndex < total &&
                                pageIndex < PRIORITY_PAGES &&
                                status.indexedChars < PRIORITY_BOOK_CHARS
                            ) {
                                currentCoroutineContext().ensureActive()
                                val remaining = PRIORITY_BOOK_CHARS - status.indexedChars
                                var text = ""
                                var truncated = false

                                renderer.openPage(pageIndex).use { page ->
                                    val scale = minOf(
                                        OCR_RENDER_WIDTH.toFloat() / page.width.coerceAtLeast(1),
                                        OCR_RENDER_MAX_HEIGHT.toFloat() / page.height.coerceAtLeast(1)
                                    )
                                    val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                                    val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    try {
                                        bitmap.eraseColor(Color.WHITE)
                                        page.render(
                                            bitmap,
                                            null,
                                            null,
                                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                        )
                                        val recognized = Tasks.await(
                                            recognizer!!.process(InputImage.fromBitmap(bitmap, 0))
                                        ).text.trim()
                                        val limit = minOf(PRIORITY_PAGE_CHARS, remaining)
                                        text = recognized.take(limit)
                                        truncated = recognized.length > text.length
                                    } finally {
                                        bitmap.recycle()
                                    }
                                }

                                dao.deletePage(uri, pageIndex)
                                if (text.isNotBlank()) {
                                    dao.insert(PdfSearchFtsEntity(uri, pageIndex, text))
                                }
                                status = status.copy(
                                    nextPageIndex = pageIndex + 1,
                                    indexedChars = status.indexedChars + text.length,
                                    searchablePages = status.searchablePages +
                                        if (text.isNotBlank()) 1 else 0,
                                    truncatedPages = status.truncatedPages + if (truncated) 1 else 0,
                                    updatedAt = System.currentTimeMillis(),
                                    outcome = "OCR_IN_PROGRESS"
                                )
                                checkpoint = status
                                dao.upsertStatus(status)
                                pageIndex++
                                if (pageIndex % 8 == 0) prune(uri)
                                yield()
                            }

                            status = status.copy(
                                outcome = when {
                                    pageIndex < total || status.truncatedPages > 0 -> "OCR_PARTIAL"
                                    status.searchablePages == 0 -> "OCR_EMPTY"
                                    else -> "OCR_FULL"
                                },
                                updatedAt = System.currentTimeMillis()
                            )
                            checkpoint = status
                            dao.upsertStatus(status)
                            prune(uri)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    checkpoint?.let {
                        withContext(NonCancellable) {
                            dao.upsertStatus(
                                it.copy(
                                    outcome = "OCR_INTERRUPTED",
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "PDF OCR index paused")
                    checkpoint?.let {
                        dao.upsertStatus(
                            it.copy(
                                outcome = "OCR_INTERRUPTED",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                } finally {
                    recognizer?.close()
                    activeBook.value = null
                }
            }
        }
    }

    private suspend fun prune(except: String) {
        while (dao.totalIndexedChars() > MAX_GLOBAL_CHARS) {
            val victim = dao.evictionCandidate(except) ?: break
            dao.deleteForBook(victim)
            dao.deleteStatus(victim)
        }
    }

    private fun sourceFingerprint(uri: String, pageCount: Int, contentHash: ByteArray): String {
        val parsed = Uri.parse(uri)
        fun number(column: String): Long = try {
            context.contentResolver.query(parsed, arrayOf(column), null, null, null)?.use { c ->
                val n = c.getColumnIndex(column)
                if (c.moveToFirst() && n >= 0 && !c.isNull(n)) c.getLong(n) else -1L
            } ?: -1L
        } catch (_: Exception) { -1L }
        // Version prefix invalidates older metadata-only index checkpoints once.
        // Keep provider metadata as a cheap identity signal alongside the content hash.
        return "sha256-v2:" + number(OpenableColumns.SIZE) + ":" +
            number(DocumentsContract.Document.COLUMN_LAST_MODIFIED) + ":" + pageCount + ":" +
            contentHash.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private class LimitedWriter(private val limit: Int) : Writer() {
        private val text = StringBuilder(minOf(limit, 8192))
        var truncated = false
            private set
        override fun write(chars: CharArray, offset: Int, length: Int) {
            val accepted = minOf(length, limit - text.length)
            if (accepted > 0) text.append(chars, offset, accepted)
            if (accepted < length) truncated = true
        }
        override fun flush() = Unit
        override fun close() = Unit
        fun value(): String = text.toString()
    }

    companion object {
        private const val DEFAULT_BOOK_CHARS = 1_200_000
        private const val PRIORITY_BOOK_CHARS = 3_000_000
        private const val DEFAULT_PAGE_CHARS = 40_000
        private const val PRIORITY_PAGE_CHARS = 80_000
        private const val DEFAULT_PAGES = 1500
        private const val PRIORITY_PAGES = 5000
        private const val MAX_GLOBAL_CHARS = 6_000_000L
        private const val OCR_RENDER_WIDTH = 1600
        private const val OCR_RENDER_MAX_HEIGHT = 2400
    }
}
