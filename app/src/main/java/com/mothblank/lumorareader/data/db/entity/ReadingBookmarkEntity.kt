package com.mothblank.lumorareader.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "reading_bookmarks", indices = [Index("bookUri")])
data class ReadingBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookUri: String,
    val label: String,
    val note: String? = null,
    val epubChunkIndex: Int? = null,
    val epubAnchor: String? = null,
    val pdfPageIndex: Int? = null,
    val createdAt: Long
)
