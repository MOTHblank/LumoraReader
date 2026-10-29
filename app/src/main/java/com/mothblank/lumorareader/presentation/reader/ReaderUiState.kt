package com.mothblank.lumorareader.presentation.reader

import android.content.pm.ActivityInfo
import com.mothblank.lumorareader.domain.BookType
import com.mothblank.lumorareader.data.EpubChapter
import com.mothblank.lumorareader.domain.model.ReadingBookmark
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.ui.theme.LumoraTheme

/** Immutable rendering state. Repository and cache ownership stay in the ViewModel. */
data class ReaderUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val bookType: BookType? = null,
    val isMenuVisible: Boolean = false,
    val theme: LumoraTheme = LumoraTheme.DEFAULT,
    val fontSizeScale: Float = 1.0f,
    val fontFamily: String = "Georgia",
    val epubLineSpacing: Float = 1.7f,
    val epubMargin: String = "Comfortable",
    val pdfNightMode: Boolean = false,
    val pdfPageSpacingDp: Int = 8,
    /** Session-only mode; reading position and customization preferences survive. */
    val focusMode: Boolean = false,
    val readingProgress: Float = 0f,
    val tableOfContents: List<EpubChapter> = emptyList(),
    val currentSpineIndex: Int = -1,
    val quoteHighlightText: String? = null,
    val bookmarks: List<ReadingBookmark> = emptyList(),
    val bookQuotes: List<Quote> = emptyList(),
    val bookForegroundMillis: Long = 0L,
    val bookForegroundSessions: Int = 0,
    val epubFileUri: String? = null,
    val epubChunkCount: Int = 1,
    val currentChunkIndex: Int = 0,
    val pdfPageCount: Int = 0,
    val currentPdfPage: Int = 0,
    val selectedText: String = "",
    val quoteSaveStatus: String? = null,
    val orientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
    // Book information for quote context
    val bookTitle: String? = null,
    val bookAuthor: String? = null,
    // New state to track if text selection is active
    val isTextSelectionActive: Boolean = false,
    // PDF Zoom state
    val pdfPageZoomScale: Float = 1f,
    val pdfPageOffsetX: Float = 0f,
    val pdfPageOffsetY: Float = 0f
) {
    val isEpub: Boolean get() = bookType == BookType.EPUB
    val isPdf: Boolean get() = bookType == BookType.PDF
    val hasMultipleChunks: Boolean get() = epubChunkCount > 1
    val currentChapterTitle: String? get() =
        if (currentSpineIndex >= 0) {
            tableOfContents.lastOrNull { it.spineIndex <= currentSpineIndex }?.title
        } else {
            tableOfContents.firstOrNull { it.chunkIndex == currentChunkIndex }?.title
                ?: tableOfContents.lastOrNull { it.chunkIndex < currentChunkIndex }?.title
        }
}
