package com.mothblank.lumorareader.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Recorded only when a reader screen leaves the foreground. */
@Entity(tableName = "reading_sessions", indices = [
    Index("bookUri"), Index("startedAt")
])
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookUri: String,
    val startedAt: Long,
    val durationMillis: Long
)
