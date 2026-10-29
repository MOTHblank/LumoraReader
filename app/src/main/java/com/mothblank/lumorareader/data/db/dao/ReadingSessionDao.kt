package com.mothblank.lumorareader.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mothblank.lumorareader.data.db.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

data class ReadingActivityTotals(
    val totalMillis: Long,
    val recentMillis: Long,
    val sessions: Int,
    val recentSessions: Int
)

@Dao
interface ReadingSessionDao {
    @Insert
    suspend fun insert(session: ReadingSessionEntity)

    /** Calendar lookback is supplied by the caller; values are derived from saved sessions only. */
    @Query("""
        SELECT COALESCE(SUM(durationMillis), 0) AS totalMillis,
            COALESCE(SUM(CASE WHEN startedAt >= :since THEN durationMillis ELSE 0 END), 0) AS recentMillis,
            COUNT(*) AS sessions,
            COALESCE(SUM(CASE WHEN startedAt >= :since THEN 1 ELSE 0 END), 0) AS recentSessions
        FROM reading_sessions
    """)
    fun observeTotals(since: Long): Flow<ReadingActivityTotals>

    @Query("""
        SELECT COALESCE(SUM(durationMillis), 0) AS totalMillis,
            COALESCE(SUM(CASE WHEN startedAt >= :since THEN durationMillis ELSE 0 END), 0) AS recentMillis,
            COUNT(*) AS sessions,
            COALESCE(SUM(CASE WHEN startedAt >= :since THEN 1 ELSE 0 END), 0) AS recentSessions
        FROM reading_sessions WHERE bookUri = :uri
    """)
    fun observeBookTotals(uri: String, since: Long): Flow<ReadingActivityTotals>
}
