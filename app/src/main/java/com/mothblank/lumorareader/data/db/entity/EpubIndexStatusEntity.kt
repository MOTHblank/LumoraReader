package com.mothblank.lumorareader.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Restartable checkpoint committed after every indexed spine section. */
@Entity(tableName = "epub_index_status")
data class EpubIndexStatusEntity(
    @PrimaryKey val bookUri: String,
    val fingerprint: String,
    val nextSpineIndex: Int,
    val indexedChars: Int,
    val updatedAt: Long,
    val complete: Boolean
)
