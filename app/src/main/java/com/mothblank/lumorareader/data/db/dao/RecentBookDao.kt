package com.mothblank.lumorareader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mothblank.lumorareader.data.db.entity.RecentBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentBookDao {
    @Query("SELECT * FROM reading_history ORDER BY lastOpened DESC")
    fun observeAll(): Flow<List<RecentBookEntity>>

    @Query("SELECT * FROM reading_history ORDER BY lastOpened DESC")
    suspend fun getAll(): List<RecentBookEntity>

    @Query("SELECT * FROM reading_history WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): RecentBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: RecentBookEntity)

    @Query("UPDATE reading_history SET positionIndex = :index, scrollOffset = :offset, progress = :progress, epubAnchor = :anchor WHERE uri = :uri")
    suspend fun updateReadingPosition(uri: String, index: Int, offset: Int, progress: Float, anchor: String?)

    @Query("UPDATE reading_history SET isAvailable = :available WHERE uri = :uri")
    suspend fun updateAvailability(uri: String, available: Boolean)

    @Query("DELETE FROM reading_history WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM reading_history")
    suspend fun deleteAll()
}
