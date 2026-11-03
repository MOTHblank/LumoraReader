package com.mothblank.lumorareader.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.mothblank.lumorareader.data.db.dao.EpubSearchDao
import com.mothblank.lumorareader.data.db.dao.EpubSearchHit
import com.mothblank.lumorareader.data.db.dao.EpubSearchRawHit
import com.mothblank.lumorareader.data.db.entity.EpubIndexStatusEntity
import com.mothblank.lumorareader.data.db.entity.EpubSearchFtsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

/** Offline, segmented EPUB indexing. Partial coverage is never reported as complete. */
@Singleton
class EpubSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: EpubSearchDao
) {
    private val indexMutex = Mutex()

    fun search(raw: String): Flow<List<EpubSearchHit>> {
        val tokens = Regex("""[\p{L}\p{N}]{2,}""").findAll(raw)
            .take(8).map { it.value }.toList()
        if (tokens.isEmpty()) return flowOf(emptyList())
        return dao.search(tokens.joinToString(" AND ") { "\"" + it + "\"" })
            .map { hits -> hits.map { it.toHit() } }
    }

    fun statuses(): Flow<List<EpubIndexStatusEntity>> = dao.observeStatuses()

    private fun EpubSearchRawHit.toHit(): EpubSearchHit {
        val numbers = positions.trim().split(Regex("""\s+""")).mapNotNull { it.toIntOrNull() }
        // FTS4 offsets: (column, query-token, byte-start, byte-length).
        val first = numbers.chunked(4).firstOrNull { it.size == 4 && it[0] == 5 }
        val bytes = indexedText.toByteArray(Charsets.UTF_8)
        val begin = (first?.get(2) ?: 0).coerceIn(0, bytes.size)
        val length = (first?.get(3) ?: 0).coerceIn(0, bytes.size - begin)
        val utf16Before = String(bytes, 0, begin, Charsets.UTF_8).length
        val match = String(bytes, begin, length, Charsets.UTF_8).trim().take(80)
        return EpubSearchHit(
            rowId, bookUri, spineIndex, bookTitle, sectionTitle, snippet,
            textOffset + utf16Before, match
        )
    }

    suspend fun indexBook(
        uri: String,
        reader: EpubBookReader,
        prioritize: Boolean = false,
        restart: Boolean = false
    ) = indexMutex.withLock {
        val spineCount = reader.getSpineCount()
        val fingerprint = sourceFingerprint(uri, spineCount)
        val previous = dao.getStatus(uri)
        var status = if (restart || previous == null || previous.fingerprint != fingerprint) {
            dao.deleteForBook(uri)
            EpubIndexStatusEntity(uri, fingerprint, 0, 0, spineCount, 0, 0,
                System.currentTimeMillis(), "IN_PROGRESS", prioritize)
                .also { dao.upsertStatus(it) }
        } else previous
        if (status.outcome == "FULL" ||
            (status.outcome == "PARTIAL" && !prioritize && !status.priority)
        ) return@withLock
        if (prioritize && !status.priority) {
            status = status.copy(priority = true)
            dao.upsertStatus(status)
        }
        val bookBudget = if (status.priority) PRIORITY_CHARS_PER_BOOK else DEFAULT_CHARS_PER_BOOK
        val chapters = reader.getDetailedTableOfContents()
        var spineIndex = status.nextSpineIndex.coerceIn(0, spineCount)
        var startOffset = status.nextTextOffset.coerceAtLeast(0)
        var indexedChars = status.indexedChars
        var failures = status.unreadableSpines
        try {
            while (spineIndex < spineCount && indexedChars < bookBudget) {
                currentCoroutineContext().ensureActive()
                val allowance = minOf(SEGMENT_CHARS, bookBudget - indexedChars)
                val segment = reader.searchTextSegment(spineIndex, startOffset, allowance)
                // Overwrite a potentially uncheckpointed segment after an interrupted write.
                if (startOffset == 0) dao.deleteSpine(uri, spineIndex)
                else dao.deleteSpineFrom(uri, spineIndex, startOffset)
                if (segment.text.isNotBlank()) {
                    val title = chapters.lastOrNull { it.spineIndex <= spineIndex }?.title
                        ?.take(120)?.ifBlank { null } ?: "Section " + (spineIndex + 1)
                    dao.insert(EpubSearchFtsEntity(
                        uri, spineIndex, startOffset / SEGMENT_CHARS, startOffset, title, segment.text
                    ))
                }
                indexedChars += segment.text.length
                if (segment.incomplete) failures++
                val anotherSegment = segment.hasMore && segment.nextOffset > startOffset
                if (anotherSegment) {
                    startOffset = segment.nextOffset
                } else {
                    spineIndex++
                    startOffset = 0
                }
                status = status.copy(
                    nextSpineIndex = spineIndex,
                    nextTextOffset = startOffset,
                    indexedChars = indexedChars,
                    unreadableSpines = failures,
                    updatedAt = System.currentTimeMillis(),
                    outcome = "IN_PROGRESS"
                )
                dao.upsertStatus(status)
                if (spineIndex % 12 == 0) pruneOldBooks(uri)
                yield()
            }
            status = status.copy(
                outcome = if (spineIndex >= spineCount && failures == 0) "FULL" else "PARTIAL",
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertStatus(status)
            pruneOldBooks(uri)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                dao.upsertStatus(status.copy(outcome = "INTERRUPTED",
                    updatedAt = System.currentTimeMillis()))
            }
            throw cancelled
        } catch (failure: Exception) {
            dao.upsertStatus(status.copy(outcome = "INTERRUPTED",
                updatedAt = System.currentTimeMillis()))
            throw failure
        }
    }

    private suspend fun pruneOldBooks(currentUri: String) {
        while (dao.totalIndexedChars() > MAX_GLOBAL_INDEXED_CHARS) {
            val oldest = dao.oldestOtherBook(currentUri) ?: break
            dao.deleteForBook(oldest)
            dao.deleteStatus(oldest)
        }
    }

    private fun sourceFingerprint(uri: String, spineCount: Int): String {
        var size = -1L
        var lastModified = -1L
        val contentUri = Uri.parse(uri)
        try {
            context.contentResolver.query(
                contentUri, arrayOf(OpenableColumns.SIZE), null, null, null
            )?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && column >= 0 && !cursor.isNull(column)) {
                    size = cursor.getLong(column)
                }
            }
        } catch (_: Exception) { }
        try {
            context.contentResolver.query(
                contentUri, arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                null, null, null
            )?.use { cursor ->
                val column = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (cursor.moveToFirst() && column >= 0 && !cursor.isNull(column)) {
                    lastModified = cursor.getLong(column)
                }
            }
        } catch (_: Exception) { }
        return size.toString() + ":" + lastModified + ":" + spineCount
    }

    companion object {
        private const val SEGMENT_CHARS = 16_000
        private const val DEFAULT_CHARS_PER_BOOK = 1_200_000
        private const val PRIORITY_CHARS_PER_BOOK = 4_000_000
        private const val MAX_GLOBAL_INDEXED_CHARS = 8_000_000L
    }
}
