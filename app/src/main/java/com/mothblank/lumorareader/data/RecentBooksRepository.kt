package com.mothblank.lumorareader.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mothblank.lumorareader.data.db.dao.RecentBookDao
import com.mothblank.lumorareader.data.db.entity.RecentBookEntity
import com.mothblank.lumorareader.domain.model.RecentBook
import com.mothblank.lumorareader.domain.model.ReadingStatus
import com.mothblank.lumorareader.domain.model.Timestamp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent, unbounded library history. Retains reading progress when a book is opened again.
 * The previous SharedPreferences list is imported once, without replacing existing Room rows.
 */
@Singleton
class RecentBooksRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val dao: RecentBookDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val migrationMutex = Mutex()

    private suspend fun migrateLegacyHistory() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        migrationMutex.withLock {
            if (prefs.getBoolean(KEY_MIGRATED, false)) return@withLock
            val json = prefs.getString(KEY_RECENT_BOOKS, null)
            val books: List<RecentBook> = try {
                if (json.isNullOrBlank()) {
                    emptyList()
                } else {
                    val type = object : TypeToken<List<RecentBook>>() {}.type
                    gson.fromJson<List<RecentBook>>(json, type) ?: emptyList()
                }
            } catch (e: Exception) {
                // Do not discard a legacy history we have not successfully imported.
                Timber.e(e, "Failed to migrate legacy recent books")
                return@withLock
            }

            // Insertion preserves the old order because timestamps are carried over.
            books.forEach { book ->
                if (dao.getByUri(book.uri) == null) {
                    dao.upsert(book.toEntity())
                }
            }
            prefs.edit().putBoolean(KEY_MIGRATED, true).remove(KEY_RECENT_BOOKS).apply()
        }
    }

    fun observeRecentBooks(): Flow<List<RecentBook>> = flow {
        migrateLegacyHistory()
        emitAll(dao.observeAll().map { books -> books.map { it.toModel() } })
    }

    suspend fun getRecentBooks(): List<RecentBook> {
        migrateLegacyHistory()
        return dao.getAll().map { it.toModel() }
    }

    suspend fun getRecentBook(uri: String): RecentBook? {
        migrateLegacyHistory()
        return dao.getByUri(uri)?.toModel()
    }

    suspend fun addRecentBook(book: RecentBook) {
        migrateLegacyHistory()
        val previous = dao.getByUri(book.uri)
        dao.upsert(
            book.toEntity().copy(
                progress = previous?.progress ?: book.progress,
                positionIndex = previous?.positionIndex ?: book.positionIndex,
                scrollOffset = previous?.scrollOffset ?: book.scrollOffset,
                epubAnchor = previous?.epubAnchor ?: book.epubAnchor,
                isFavorite = previous?.isFavorite ?: book.isFavorite,
                readingStatus = previous?.readingStatus
                    ?: runCatching { book.readingStatus.name }.getOrDefault("READING"),
                collectionName = previous?.collectionName ?: book.collectionName,
                coverImageUri = book.coverImageUri ?: previous?.coverImageUri,
                author = book.author ?: previous?.author,
                isAvailable = true
            )
        )
    }

    suspend fun updateReadingPosition(uri: String, index: Int, offset: Int, progress: Float, anchor: String? = null) {
        migrateLegacyHistory()
        dao.updateReadingPosition(
            uri = uri,
            index = index.coerceAtLeast(0),
            offset = offset.coerceAtLeast(0),
            progress = progress.coerceIn(0f, 1f),
            anchor = anchor
        )
    }

    suspend fun setFavorite(uri: String, favorite: Boolean) {
        migrateLegacyHistory()
        dao.setFavorite(uri, favorite)
    }

    suspend fun setReadingStatus(uri: String, status: ReadingStatus) {
        migrateLegacyHistory()
        dao.setReadingStatus(uri, status.name)
    }

    suspend fun setCollection(uri: String, collection: String?) {
        migrateLegacyHistory()
        dao.setCollection(uri, collection?.trim()?.take(80)?.ifBlank { null })
    }

    suspend fun markUnavailable(uri: String) {
        migrateLegacyHistory()
        dao.updateAvailability(uri, false)
    }

    suspend fun removeRecentBook(uri: String) {
        migrateLegacyHistory()
        dao.deleteByUri(uri)
    }

    suspend fun clearRecentBooks() {
        migrateLegacyHistory()
        dao.deleteAll()
    }

    private fun RecentBook.toEntity() = RecentBookEntity(
        uri = uri,
        title = title,
        author = author,
        coverImageUri = coverImageUri,
        lastOpened = lastOpened.millis,
        progress = progress,
        positionIndex = positionIndex,
        scrollOffset = scrollOffset,
        epubAnchor = epubAnchor,
        isAvailable = isAvailable,
        isFavorite = isFavorite,
        // Gson may deserialize legacy SharedPreferences JSON without this
        // newly introduced enum field. Never discard old history on that account.
        readingStatus = runCatching { readingStatus.name }.getOrDefault("READING"),
        collectionName = collectionName
    )

    private fun RecentBookEntity.toModel() = RecentBook(
        uri = uri,
        title = title,
        author = author,
        coverImageUri = coverImageUri,
        lastOpened = Timestamp(lastOpened),
        progress = progress,
        positionIndex = positionIndex,
        scrollOffset = scrollOffset,
        epubAnchor = epubAnchor,
        isAvailable = isAvailable,
        isFavorite = isFavorite,
        readingStatus = runCatching { ReadingStatus.valueOf(readingStatus) }.getOrDefault(ReadingStatus.READING),
        collectionName = collectionName
    )

    companion object {
        private const val PREFS_NAME = "LumoraReaderPrefs"
        private const val KEY_RECENT_BOOKS = "recent_books"
        private const val KEY_MIGRATED = "recent_books_room_migrated"
    }
}
