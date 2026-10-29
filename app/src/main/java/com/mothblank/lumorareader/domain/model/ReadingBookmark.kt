package com.mothblank.lumorareader.domain.model

/** A durable, document-specific reading location with an optional personal annotation. */
data class ReadingBookmark(
    val id: Long = 0,
    val bookUri: String,
    val label: String,
    val note: String? = null,
    val epubChunkIndex: Int? = null,
    val epubAnchor: String? = null,
    val pdfPageIndex: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isEpub: Boolean get() = epubChunkIndex != null
    val isPdf: Boolean get() = pdfPageIndex != null
}
