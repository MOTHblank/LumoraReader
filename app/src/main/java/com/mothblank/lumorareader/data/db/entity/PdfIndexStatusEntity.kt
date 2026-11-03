package com.mothblank.lumorareader.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Page-level checkpoint. An interrupted operation resumes at nextPageIndex. */
@Entity(tableName = "pdf_index_status")
data class PdfIndexStatusEntity(
    @PrimaryKey val bookUri: String,
    val fingerprint: String,
    val nextPageIndex: Int,
    val totalPages: Int,
    val indexedChars: Int,
    val searchablePages: Int,
    val truncatedPages: Int,
    val updatedAt: Long,
    val outcome: String
)
