package com.mothblank.lumorareader.domain

enum class BookType { PDF, EPUB }

/**
 * Immutable domain model with sensible defaults and
 * forward-compatible optional fields.
 */
data class Book(
    val uri: String,
    val title: String,
    val author: String? = null,
    val type: BookType,
    val coverImageUri: String? = null   // optional, nullable
)