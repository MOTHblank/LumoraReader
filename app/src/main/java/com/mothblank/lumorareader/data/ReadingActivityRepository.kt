package com.mothblank.lumorareader.data

import com.mothblank.lumorareader.data.db.dao.ReadingSessionDao
import com.mothblank.lumorareader.data.db.dao.ReadingActivityTotals
import com.mothblank.lumorareader.data.db.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingActivityRepository @Inject constructor(private val dao: ReadingSessionDao) {
    fun observeTotals(since: Long): Flow<ReadingActivityTotals> = dao.observeTotals(since)

    suspend fun recordForegroundSession(bookUri: String, startTimeMillis: Long, durationMillis: Long) {
        if (bookUri.isBlank() || durationMillis < 1_000L) return
        // Prevent unrealistic durations if the process was suspended unexpectedly.
        dao.insert(ReadingSessionEntity(
            bookUri = bookUri,
            startedAt = startTimeMillis,
            durationMillis = durationMillis.coerceAtMost(24L * 60 * 60 * 1000)
        ))
    }
}
