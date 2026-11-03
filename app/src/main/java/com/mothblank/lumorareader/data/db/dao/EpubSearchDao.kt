package com.mothblank.lumorareader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mothblank.lumorareader.data.db.entity.EpubIndexStatusEntity
import com.mothblank.lumorareader.data.db.entity.EpubSearchFtsEntity
import kotlinx.coroutines.flow.Flow

/** Lightweight result passed to the UI after byte offsets are decoded in the repository. */
data class EpubSearchHit(
    val rowId: Long,
    val bookUri: String,
    val spineIndex: Int,
    val bookTitle: String,
    val sectionTitle: String,
    val snippet: String,
    val characterOffset: Int,
    val matchedText: String
)

/** FTS4 offsets are byte positions inside the indexed content column. */
data class EpubSearchRawHit(
    val rowId: Long,
    val bookUri: String,
    val spineIndex: Int,
    val bookTitle: String,
    val sectionTitle: String,
    val snippet: String,
    val textOffset: Int,
    val indexedText: String,
    val positions: String
)

@Dao
interface EpubSearchDao {
    @Query("""
        SELECT epub_search_fts.rowid AS rowId,
               epub_search_fts.bookUri AS bookUri,
               epub_search_fts.spineIndex AS spineIndex,
               reading_history.title AS bookTitle,
               epub_search_fts.title AS sectionTitle,
               snippet(epub_search_fts, '[', ']', '…', 5, 12) AS snippet,
               epub_search_fts.textOffset AS textOffset,
               epub_search_fts.content AS indexedText,
               offsets(epub_search_fts) AS positions
        FROM epub_search_fts
        INNER JOIN reading_history ON reading_history.uri = epub_search_fts.bookUri
        WHERE epub_search_fts.content MATCH :query AND reading_history.isAvailable = 1
        ORDER BY reading_history.lastOpened DESC, epub_search_fts.spineIndex, epub_search_fts.segmentIndex
        LIMIT 40
    """)
    fun search(query: String): Flow<List<EpubSearchRawHit>>

    @Query("SELECT * FROM epub_index_status WHERE bookUri = :uri LIMIT 1")
    suspend fun getStatus(uri: String): EpubIndexStatusEntity?

    @Query("SELECT * FROM epub_index_status")
    fun observeStatuses(): Flow<List<EpubIndexStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatus(status: EpubIndexStatusEntity)

    @Insert
    suspend fun insert(entry: EpubSearchFtsEntity)

    @Query("DELETE FROM epub_search_fts WHERE bookUri = :uri AND spineIndex = :spineIndex")
    suspend fun deleteSpine(uri: String, spineIndex: Int)

    @Query("DELETE FROM epub_search_fts WHERE bookUri = :uri AND spineIndex = :spineIndex AND textOffset >= :fromOffset")
    suspend fun deleteSpineFrom(uri: String, spineIndex: Int, fromOffset: Int)

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
