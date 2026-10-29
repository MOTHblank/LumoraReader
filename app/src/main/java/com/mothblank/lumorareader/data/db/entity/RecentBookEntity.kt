package com.mothblank.lumorareader.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_history")
data class RecentBookEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val author: String? = null,
    val coverImageUri: String? = null,
    val lastOpened: Long,
    val progress: Float? = null,
    val positionIndex: Int = 0,
    val scrollOffset: Int = 0,
    val epubAnchor: String? = null,
    val isAvailable: Boolean = true,
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "'READING'") val readingStatus: String = "READING",
    val collectionName: String? = null
)
