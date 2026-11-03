package com.mothblank.lumorareader.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Explicit coverage and a precise per-chapter resume checkpoint. */
@Entity(tableName = "epub_index_status")
data class EpubIndexStatusEntity(
    @PrimaryKey val bookUri: String,
    val fingerprint: String,
    val nextSpineIndex: Int,
    val nextTextOffset: Int,
    val totalSpines: Int,
    val indexedChars: Int,
    val unreadableSpines: Int,
    val updatedAt: Long,
    val outcome: String,
    val priority: Boolean
)
