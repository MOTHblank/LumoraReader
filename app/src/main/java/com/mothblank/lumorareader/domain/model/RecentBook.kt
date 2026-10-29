package com.mothblank.lumorareader.domain.model


@JvmInline
value class Timestamp(val millis: Long)

/**
 * Represents a recently opened book.
 */
enum class ReadingStatus { WANT_TO_READ, READING, ON_HOLD, FINISHED }

data class RecentBook(
    val title: String,
    val uri: String,
    val lastOpened: Timestamp,

    // Optional but powerful additions for a richer UI
    val author: String? = null,
    val coverImageUri: String? = null, // A local URI to a cached cover image
    val progress: Float? = null, // Normalized progress from 0f to 1f
    val positionIndex: Int = 0, // PDF page or EPUB chunk, zero-based
    val scrollOffset: Int = 0, // EPUB scroll offset within the chunk
    val epubAnchor: String? = null, // Stable EPUB content position and fractional fallback
    val isAvailable: Boolean = true,
    val isFavorite: Boolean = false,
    val readingStatus: ReadingStatus = ReadingStatus.READING,
    val collectionName: String? = null
)