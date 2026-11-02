package com.mothblank.lumorareader.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.mothblank.lumorareader.data.db.dao.EpubSearchDao
import com.mothblank.lumorareader.data.db.dao.EpubSearchHit
import com.mothblank.lumorareader.data.db.entity.EpubIndexStatusEntity
import com.mothblank.lumorareader.data.db.entity.EpubSearchFtsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

/** Offline incremental EPUB indexing with bounded storage and restartable progress. */
@Singleton
class EpubSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: EpubSearchDao
) {
    private val indexMutex = Mutex()

    fun search(raw: String): Flow<List<EpubSearchHit>> {
        // Quote tokens so search text cannot be interpreted as FTS syntax.
        val tokens = Regex("""[\p{L}\p{N}]{2,}""").findAll(raw)
            .take(8).map { it.value }.toList()
        if (tokens.isEmpty()) return flowOf(emptyList())
        return dao.search(tokens.joinToString(" AND ") { "\"" + it + "\"" })
    }

    suspend fun indexBook(uri: String, reader: EpubBookReader) = indexMutex.withLock {
        val spineCount = reader.getSpineCount()
        val fingerprint = sourceFingerprint(uri, spineCount)
        val existing = dao.getStatus(uri)
        val status = if (existing != null && existing.fingerprint == fingerprint) {
            existing
        } else {
            dao.deleteForBook(uri)
            val fresh = EpubIndexStatusEntity(
                uri, fingerprint, 0, 0, System.currentTimeMillis(), false
            )
            dao.upsertStatus(fresh)
            fresh
        }
        if (status.complete) return@withLock

        val chapters = reader.getDetailedTableOfContents()
        val limit = minOf(spineCount, MAX_SECTIONS_PER_BOOK)
        var indexedChars = status.indexedChars
        var next = status.nextSpineIndex.coerceIn(0, limit)
        while (next < limit && indexedChars < MAX_CHARS_PER_BOOK) {
            currentCoroutineContext().ensureActive()
            val allowance = minOf(MAX_CHARS_PER_SECTION, MAX_CHARS_PER_BOOK - indexedChars)
            val text = reader.textForSearch(next, allowance)
            dao.deleteSpine(uri, next)
            if (text.isNotBlank()) {
                val title = chapters.lastOrNull { it.spineIndex <= next }?.title
                    ?.take(120)?.ifBlank { null } ?: "Section " + (next + 1)
                dao.insert(EpubSearchFtsEntity(uri, next, title, text))
                indexedChars += text.length
            }
            next++
            dao.upsertStatus(
                EpubIndexStatusEntity(
                    uri, fingerprint, next, indexedChars, System.currentTimeMillis(),
                    next >= limit || indexedChars >= MAX_CHARS_PER_BOOK
                )
            )
            if (next % 20 == 0) pruneOldBooks(uri)
            yield()
        }
        dao.upsertStatus(
            EpubIndexStatusEntity(uri, fingerprint, next, indexedChars, System.currentTimeMillis(), true)
        )
        pruneOldBooks(uri)
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
        try {
            context.contentResolver.query(
                Uri.parse(uri),
                arrayOf(OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                    if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                        lastModified = cursor.getLong(modifiedIndex)
                    }
                }
            }
        } catch (_: Exception) {
            // Not every SAF provider exposes document modification metadata.
        }
        return size.toString() + ":" + lastModified + ":" + spineCount
    }

    companion object {
        private const val MAX_SECTIONS_PER_BOOK = 256
        private const val MAX_CHARS_PER_SECTION = 60_000
        private const val MAX_CHARS_PER_BOOK = 1_200_000
        private const val MAX_GLOBAL_INDEXED_CHARS = 6_000_000L
    }
}
