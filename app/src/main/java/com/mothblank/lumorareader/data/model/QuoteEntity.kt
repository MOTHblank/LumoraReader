package com.mothblank.lumorareader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "quotes")
@TypeConverters(StringListConverter::class)
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val author: String,
    val theme: String? = null,
    val imagePath: String? = null,
    val date: String? = null,
    val starred: Boolean = false,
    val categories: List<String> = emptyList(),
    // Add book context fields
    val bookTitle: String? = null,
    val bookAuthor: String? = null,
    val pageNumber: Int? = null,
    val chapterTitle: String? = null
)