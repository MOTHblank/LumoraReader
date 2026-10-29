package com.mothblank.lumorareader.domain.model

data class QuoteBookOption(val sourceUri: String, val bookTitle: String)

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
    val chapterTitle: String? = null,
    // Source is optional: existing/manual quotes remain valid without a link.
    val sourceUri: String? = null,
    val epubChunkIndex: Int? = null,
    val epubAnchor: String? = null,
    val note: String? = null
)