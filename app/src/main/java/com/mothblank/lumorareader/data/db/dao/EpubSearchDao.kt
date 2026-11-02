package com.mothblank.lumorareader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mothblank.lumorareader.data.db.entity.EpubIndexStatusEntity
import com.mothblank.lumorareader.data.db.entity.EpubSearchFtsEntity
import kotlinx.coroutines.flow.Flow

data class EpubSearchHit(
    val bookUri: String,
    val spineIndex: Int,
    val bookTitle: String,
    val sectionTitle: String,
    val snippet: String
)

@Dao
interface EpubSearchDao {
    @Query("""
        SELECT epub_search_fts.bookUri AS bookUri,
               epub_search_fts.spineIndex AS spineIndex,
               reading_history.title AS bookTitle,
               epub_search_fts.title AS sectionTitle,
               snippet(epub_search_fts, '[', ']', '…', 3, 12) AS snippet
        FROM epub_search_fts
        INNER JOIN reading_history ON reading_history.uri = epub_search_fts.bookUri
        WHERE epub_search_fts MATCH :query AND reading_history.isAvailable = 1
        ORDER BY reading_history.lastOpened DESC
        LIMIT 40
    """)
    fun search(query: String): Flow<List<EpubSearchHit>>

    @Query("SELECT * FROM epub_index_status WHERE bookUri = :uri LIMIT 1")
    suspend fun getStatus(uri: String): EpubIndexStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatus(status: EpubIndexStatusEntity)

    @Insert
    suspend fun insert(entry: EpubSearchFtsEntity)

    @Query("DELETE FROM epub_search_fts WHERE bookUri = :uri AND spineIndex = :spineIndex")
    suspend fun deleteSpine(uri: String, spineIndex: Int)

    @Query("DELETE FROM epub_search_fts WHERE bookUri = :uri")
    suspend fun deleteForBook(uri: String)

    @Query("DELETE FROM epub_index_status WHERE bookUri = :uri")
    suspend fun deleteStatus(uri: String)

    @Query("DELETE FROM epub_search_fts")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM epub_index_status")
    suspend fun deleteAllStatuses()

    @Query("SELECT COALESCE(SUM(indexedChars), 0) FROM epub_index_status")
    suspend fun totalIndexedChars(): Long

    @Query("SELECT bookUri FROM epub_index_status WHERE bookUri != :except ORDER BY updatedAt ASC LIMIT 1")
    suspend fun oldestOtherBook(except: String): String?
}
