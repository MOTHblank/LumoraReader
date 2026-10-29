package com.mothblank.lumorareader.data

import com.mothblank.lumorareader.data.db.dao.ReadingBookmarkDao
import com.mothblank.lumorareader.data.db.entity.ReadingBookmarkEntity
import com.mothblank.lumorareader.domain.model.ReadingBookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingBookmarksRepository @Inject constructor(
    private val dao: ReadingBookmarkDao
) {
    fun observeBook(uri: String): Flow<List<ReadingBookmark>> =
        dao.observeBook(uri).map { list -> list.map { it.asModel() } }

    fun searchAll(query: String): Flow<List<ReadingBookmark>> =
        dao.searchAll(query).map { entries -> entries.map { it.asModel() } }

    suspend fun getForBook(id: Long, uri: String): ReadingBookmark? =
        dao.getForBook(id, uri)?.asModel()

    suspend fun add(bookmark: ReadingBookmark) {
        require(bookmark.bookUri.isNotBlank())
        require((bookmark.epubChunkIndex != null) != (bookmark.pdfPageIndex != null))
        dao.insert(bookmark.asEntity().copy(id = 0))
    }

    suspend fun updateNote(bookUri: String, id: Long, label: String, note: String?) {
        val existing = dao.getForBook(id, bookUri) ?: return
        dao.update(existing.copy(label = label.take(120), note = note?.take(2000)))
    }

    suspend fun delete(bookUri: String, id: Long) = dao.deleteFromBook(id, bookUri)

    private fun ReadingBookmarkEntity.asModel() = ReadingBookmark(
        id, bookUri, label, note, epubChunkIndex, epubAnchor, pdfPageIndex, createdAt
    )

    private fun ReadingBookmark.asEntity() = ReadingBookmarkEntity(
        id, bookUri, label, note, epubChunkIndex, epubAnchor, pdfPageIndex, createdAt
    )
}
