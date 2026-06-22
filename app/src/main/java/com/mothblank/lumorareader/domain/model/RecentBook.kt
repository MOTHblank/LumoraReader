package com.mothblank.lumorareader.domain.model


@JvmInline
value class Timestamp(val millis: Long)

/**
 * Represents a recently opened book.
 */
data class RecentBook(
    val title: String,
    val uri: String,
    val lastOpened: Timestamp,

    // Optional but powerful additions for a richer UI
    val author: String? = null,
    val coverImageUri: String? = null, // A local URI to a cached cover image
    val progress: Float? = null // Reading progress, e.g., 0.5f for 50%
)