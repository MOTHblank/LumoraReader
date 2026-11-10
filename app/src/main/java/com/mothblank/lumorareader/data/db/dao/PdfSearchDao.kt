package com.mothblank.lumorareader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mothblank.lumorareader.data.db.entity.PdfIndexStatusEntity
import com.mothblank.lumorareader.data.db.entity.PdfSearchFtsEntity
import kotlinx.coroutines.flow.Flow

data class PdfSearchHit(
    val bookUri: String,
    val bookTitle: String,
    val pageIndex: Int,
    val snippet: String,
    val source: String
)

@Dao
interface PdfSearchDao {
    @Query("""
        SELECT pdf_search_fts.bookUri AS bookUri,
               reading_history.title AS bookTitle,
               pdf_search_fts.pageIndex AS pageIndex,
               snippet(pdf_search_fts, '[', ']', '…', 2, 12) AS snippet,
               CASE WHEN pdf_index_status.outcome LIKE 'OCR_%' THEN 'OCR' ELSE 'TEXT' END AS source
        FROM pdf_search_fts
        JOIN reading_history ON reading_history.uri = pdf_search_fts.bookUri
        LEFT JOIN pdf_index_status ON pdf_index_status.bookUri = pdf_search_fts.bookUri
        WHERE pdf_search_fts.content MATCH :query AND reading_history.isAvailable = 1
        ORDER BY reading_history.lastOpened DESC, pdf_search_fts.pageIndex ASC
        LIMIT 40
    """)
    fun search(query: String): Flow<List<PdfSearchHit>>

    @Query("SELECT * FROM pdf_index_status WHERE bookUri = :uri LIMIT 1")
    suspend fun getStatus(uri: String): PdfIndexStatusEntity?

    @Query("SELECT * FROM pdf_index_status")
    fun observeStatuses(): Flow<List<PdfIndexStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatus(status: PdfIndexStatusEntity)

    @Insert
    suspend fun insert(entry: PdfSearchFtsEntity)

    @Query("DELETE FROM pdf_search_fts WHERE bookUri = :uri AND pageIndex = :page")
    suspend fun deletePage(uri: String, page: Int)

    @Query("DELETE FROM pdf_search_fts WHERE bookUri = :uri")
    suspend fun deleteForBook(uri: String)

    @Query("DELETE FROM pdf_index_status WHERE bookUri = :uri")
    suspend fun deleteStatus(uri: String)

    @Query("DELETE FROM pdf_search_fts")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM pdf_index_status")
    suspend fun deleteAllStatuses()

    @Query("SELECT COALESCE(SUM(indexedChars), 0) FROM pdf_index_status")
    suspend fun totalIndexedChars(): Long

    @Query("""
        SELECT status.bookUri
        FROM pdf_index_status AS status
        LEFT JOIN reading_history AS history ON history.uri = status.bookUri
        WHERE status.bookUri != :except
          AND COALESCE(history.isFavorite, 0) = 0
        ORDER BY
            CASE WHEN history.uri IS NULL THEN 0 ELSE 1 END ASC,
            COALESCE(history.lastOpened, 0) ASC,
            CASE WHEN status.outcome LIKE 'OCR_%' THEN 1 ELSE 0 END ASC,
            status.indexedChars ASC,
            status.updatedAt ASC
        LIMIT 1
    """)
    suspend fun evictionCandidate(except: String): String?
}
