package com.mothblank.lumorareader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mothblank.lumorareader.data.db.entity.ReadingBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingBookmarkDao {
    @Query("SELECT * FROM reading_bookmarks WHERE bookUri = :uri ORDER BY createdAt DESC, id DESC")
    fun observeBook(uri: String): Flow<List<ReadingBookmarkEntity>>

    @Query("""
        SELECT * FROM reading_bookmarks WHERE
            :query = '' OR
            instr(lower(label), lower(:query)) > 0 OR
            instr(lower(COALESCE(note, '')), lower(:query)) > 0
        ORDER BY createdAt DESC, id DESC
        LIMIT 40
    """)
    fun searchAll(query: String): Flow<List<ReadingBookmarkEntity>>

    @Insert
    suspend fun insert(bookmark: ReadingBookmarkEntity): Long

    @Update
    suspend fun update(bookmark: ReadingBookmarkEntity)

    @Query("SELECT * FROM reading_bookmarks WHERE id = :id AND bookUri = :uri LIMIT 1")
    suspend fun getForBook(id: Long, uri: String): ReadingBookmarkEntity?

    @Query("DELETE FROM reading_bookmarks WHERE id = :id AND bookUri = :uri")
    suspend fun deleteFromBook(id: Long, uri: String)
}
