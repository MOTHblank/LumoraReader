package com.mothblank.lumorareader.domain.model

data class Quote(
    val id: Int = 0,
    val text: String,
    val author: String,
    val theme: String? = null,
    val imagePath: String? = null,
    val date: String? = null,
    val starred: Boolean = false,
    val categories: List<String> = emptyList(),
    // Add book context information
    val bookTitle: String? = null,
    val bookAuthor: String? = null,
    val pageNumber: Int? = null,
    val chapterTitle: String? = null
)