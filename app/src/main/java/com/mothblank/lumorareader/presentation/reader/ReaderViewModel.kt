package com.mothblank.lumorareader.presentation.reader

import android.content.Context
import android.graphics.RectF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.pm.ActivityInfo
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.BuildConfig
import com.mothblank.lumorareader.data.EpubBookReader
import com.mothblank.lumorareader.data.EpubChapter
import com.mothblank.lumorareader.data.PdfBookReader
import com.mothblank.lumorareader.data.RecentBooksRepository
import com.mothblank.lumorareader.data.ReadingBookmarksRepository
import com.mothblank.lumorareader.domain.model.ReadingBookmark
import com.mothblank.lumorareader.domain.model.EpubReadingAnchor
import com.mothblank.lumorareader.domain.model.RecentBook
import com.mothblank.lumorareader.domain.model.Timestamp
import com.mothblank.lumorareader.domain.usecase.BookReader
import com.mothblank.lumorareader.domain.BookType
import com.mothblank.lumorareader.domain.model.Page
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.domain.repository.BookRepository
import com.mothblank.lumorareader.domain.repository.QuoteRepository
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import com.mothblank.lumorareader.ui.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
    private val quoteRepository: QuoteRepository,
    private val recentBooksRepository: RecentBooksRepository,
    private val bookmarksRepository: ReadingBookmarksRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    val pageCache = mutableStateMapOf<Int, Page>()
    private var bookReader: BookReader? = null
    private data class CachedEpubChunk(val uri: String, val file: File)
    private val maxCachedEpubChunks = 8
    // Access-ordered LRU: retain file paths, not just content:// URIs.
    private val chunkFileCache = LinkedHashMap<String, CachedEpubChunk>(16, 0.75f, true)
    private val epubFilesInUse = mutableMapOf<String, Int>()
    private val deferredChunkDeletes = mutableMapOf<String, MutableSet<File>>()
    private val pdfPageJobs = mutableMapOf<Int, Job>()
    private val pdfLru = LinkedHashMap<Int, Page.PdfPage>(16, 0.75f, true)
    private val pdfRenderedWidths = mutableMapOf<Int, Int>()
    private var pdfViewportWidthPx = 1080
    // Bound all retained PDF pixel data, not just the number of page indices.
    private val pdfCacheBudgetBytes = minOf(
        32L * 1024 * 1024,
        (Runtime.getRuntime().maxMemory() / 8).coerceAtLeast(8L * 1024 * 1024)
    )
    data class PdfZoomTile(val pageIndex: Int, val region: RectF, val zoom: Float, val page: Page.PdfPage)
    var pdfZoomTile by mutableStateOf<PdfZoomTile?>(null)
        private set
    private var pdfTileJob: Job? = null
    private var pdfTileRequestId = 0L
    private var readerGeneration = 0L
    private var epubLoadGeneration = 0L
    private var epubNavigationGeneration = 0L
    private var epubChunkJob: Job? = null
    private var bookOpenJob: Job? = null
    @Volatile private var currentScrollOffset = 0
    @Volatile private var currentEpubAnchor: EpubReadingAnchor? = null
    private var selectedEpubAnchor: EpubReadingAnchor? = null
    private var initialChunkLoaded = false
    private var positionSaveJob: Job? = null
    private var currentBookUri: String? = null
    private var bookmarkObserveJob: Job? = null
    private var quoteObserveJob: Job? = null
    private var originalOrientation: Int? = null

    init {
        viewModelScope.launch {
            userPreferencesRepository.theme.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
        // Remove the orientation flow collection - don't restore saved orientation
        viewModelScope.launch {
            userPreferencesRepository.fontSizeScale.collect { scale ->
                _uiState.update { it.copy(fontSizeScale = scale) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.fontFamily.collect { family ->
                _uiState.update { it.copy(fontFamily = family) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.epubLineSpacing.collect { spacing ->
                _uiState.update { it.copy(epubLineSpacing = spacing) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.epubMargin.collect { margin ->
                _uiState.update { it.copy(epubMargin = margin) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.pdfNightMode.collect { night ->
                _uiState.update { it.copy(pdfNightMode = night) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.pdfPageSpacingDp.collect { spacing ->
                _uiState.update { it.copy(pdfPageSpacingDp = spacing) }
            }
        }
        retryLoading()
    }
    fun setOriginalOrientation(orientation: Int) {
        if (originalOrientation == null) {
            originalOrientation = orientation
        }
    }
    fun onOrientationChange(orientation: Int) {
        _uiState.update { it.copy(orientation = orientation) }
        // Don't call themePreferenceRepository.setOrientation() anymore
    }

    // Restore original orientation when leaving the reader
    fun restoreOriginalOrientation(): Int {
        return originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    fun retryLoading() {
        val bookUri = savedStateHandle.get<String>("bookUri")
        if (bookUri == null) {
            _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.book_uri_missing)) }
            return
        }
        currentBookUri = bookUri
        observeBookNotebook(bookUri)
        openBook(bookUri)
    }

    private fun observeBookNotebook(bookUri: String) {
        bookmarkObserveJob?.cancel()
        quoteObserveJob?.cancel()
        _uiState.update { it.copy(bookmarks = emptyList(), bookQuotes = emptyList()) }
        bookmarkObserveJob = viewModelScope.launch {
            bookmarksRepository.observeBook(bookUri).collect { bookmarks ->
                if (currentBookUri == bookUri) {
                    _uiState.update { it.copy(bookmarks = bookmarks) }
                }
            }
        }
        quoteObserveJob = viewModelScope.launch {
            quoteRepository.observeForBook(bookUri).collect { quotes ->
                if (currentBookUri == bookUri) {
                    _uiState.update { it.copy(bookQuotes = quotes) }
                }
            }
        }
    }

    /** Save a bookmark at the precise EPUB spine/character location or PDF page. */
    fun addBookmark(label: String, note: String?) {
        val uri = currentBookUri ?: return
        val state = uiState.value
        if (state.isLoading || (!state.isEpub && !state.isPdf)) return
        val cleanedLabel = label.trim().take(120).ifEmpty {
            if (state.isPdf) "Page ${state.currentPdfPage + 1}"
            else state.currentChapterTitle ?: "Section ${state.currentChunkIndex + 1}"
        }
        val cleanedNote = note?.trim()?.take(2000)?.ifEmpty { null }
        val bookmark = ReadingBookmark(
            bookUri = uri,
            label = cleanedLabel,
            note = cleanedNote,
            epubChunkIndex = state.currentChunkIndex.takeIf { state.isEpub },
            epubAnchor = currentEpubAnchor?.serialize().takeIf { state.isEpub },
            pdfPageIndex = state.currentPdfPage.takeIf { state.isPdf }
        )
        viewModelScope.launch {
            try { bookmarksRepository.add(bookmark) }
            catch (e: Exception) { Timber.e(e, "Failed to add reading bookmark") }
        }
    }

    fun editBookmark(id: Long, label: String, note: String?) {
        val uri = currentBookUri ?: return
        viewModelScope.launch {
            try { bookmarksRepository.updateNote(uri, id, label.trim().ifEmpty { "Bookmark" }, note?.trim()?.ifEmpty { null }) }
            catch (e: Exception) { Timber.e(e, "Failed to edit reading bookmark") }
        }
    }

    fun deleteBookmark(id: Long) {
        val uri = currentBookUri ?: return
        viewModelScope.launch {
            try { bookmarksRepository.delete(uri, id) }
            catch (e: Exception) { Timber.e(e, "Failed to delete reading bookmark") }
        }
    }

    fun goToBookmark(bookmark: ReadingBookmark) {
        if (bookmark.bookUri != currentBookUri) return
        val state = uiState.value
        if (state.isTextSelectionActive) return
        if (state.isEpub) {
            val anchor = EpubReadingAnchor.parse(bookmark.epubAnchor)
            val epub = bookReader as? EpubBookReader ?: return
            val index = anchor?.spineIndex?.takeIf { it >= 0 }
                ?.let { epub.chunkForSpine(it) }
                ?: bookmark.epubChunkIndex ?: return
            loadEpubChunk(index, targetAnchor = anchor, forceFreshHtml = true)
        } else if (state.isPdf) {
            bookmark.pdfPageIndex?.let { onPdfPageChanged(it) }
        }
    }

    fun goToNotebookQuote(quote: Quote) {
        if (quote.sourceUri != currentBookUri || uiState.value.isTextSelectionActive) return
        val state = uiState.value
        if (state.isEpub) {
            val epub = bookReader as? EpubBookReader ?: return
            val anchor = EpubReadingAnchor.parse(quote.epubAnchor)
            val index = anchor?.spineIndex?.takeIf { it >= 0 }
                ?.let { epub.chunkForSpine(it) } ?: quote.epubChunkIndex ?: return
            loadEpubChunk(
                index, targetAnchor = anchor, forceFreshHtml = true,
                quoteToHighlight = quote.text.take(2000)
            )
        } else if (state.isPdf) {
            quote.pageNumber?.let { onPdfPageChanged(it - 1) }
            _uiState.update { it.copy(quoteHighlightText = quote.text.take(2000)) }
        }
    }

    private fun openBook(uri: String) {
        // Invalidate requests before replacing the reader, even if opening suspends.
        bookOpenJob?.cancel()
        readerGeneration++
        positionSaveJob?.cancel()
        currentEpubAnchor = null
        selectedEpubAnchor = null
        pdfPageJobs.values.forEach { it.cancel() }
        pdfPageJobs.clear()
        clearPdfCache()
        pdfTileJob?.cancel()
        pdfTileRequestId++
        pdfZoomTile = null
        epubChunkJob?.cancel()
        epubLoadGeneration++
        clearEpubChunkCache()
        bookOpenJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Reading history is best-effort: a database error must not block opening a file.
                val saved = try {
                    recentBooksRepository.getRecentBook(uri)
                } catch (error: Exception) {
                    Timber.w(error, "Could not restore reading position")
                    null
                }
                // Only apply a quote destination to the exact document that owns it.
                val requestedQuote = savedStateHandle.get<Int>("quoteId")
                    ?.takeIf { it > 0 }
                    ?.let { quoteId ->
                        try {
                            quoteRepository.getQuoteById(quoteId)?.takeIf { it.sourceUri == uri }
                        } catch (error: Exception) {
                            Timber.w(error, "Could not resolve quotation destination")
                            null
                        }
                    }
                val requestedEpubAnchor = EpubReadingAnchor.parse(requestedQuote?.epubAnchor)
                val bookDetails = bookRepository.getBookDetails(uri)
                bookReader?.close()
                bookReader = withContext(Dispatchers.IO) { bookRepository.openBook(bookDetails) }
                try {
                    recentBooksRepository.addRecentBook(
                        RecentBook(
                            title = bookDetails.title,
                            uri = uri,
                            author = bookDetails.author,
                            coverImageUri = bookDetails.coverImageUri,
                            lastOpened = Timestamp(System.currentTimeMillis())
                        )
                    )
                } catch (error: Exception) {
                    Timber.w(error, "Could not save book history")
                }

                _uiState.update {
                    it.copy(
                        bookTitle = bookDetails.title,
                        bookAuthor = bookDetails.author,
                        quoteHighlightText = requestedQuote?.text?.take(2000)
                    )
                }
                val epubReader = bookReader as? EpubBookReader
                if (epubReader != null) {
                    val chunkCount = epubReader.getChunkCount()
                    val initialChunk = if (chunkCount > 0) {
                        (requestedEpubAnchor?.spineIndex?.takeIf { it >= 0 }
                            ?.let { epubReader.chunkForSpine(it) }
                            ?: requestedQuote?.epubChunkIndex
                            ?: saved?.positionIndex ?: 0).coerceIn(0, chunkCount - 1)
                    } else 0
                    currentScrollOffset = if (requestedQuote == null && saved?.positionIndex == initialChunk) {
                        saved?.scrollOffset?.coerceAtLeast(0) ?: 0
                    } else 0
                    currentEpubAnchor = if (requestedQuote != null) requestedEpubAnchor
                        else if (saved?.positionIndex == initialChunk) EpubReadingAnchor.parse(saved?.epubAnchor)
                        else null
                    initialChunkLoaded = false
                    val contents = epubReader.getDetailedTableOfContents()
                        .filter { it.title.isNotBlank() && it.chunkIndex in 0 until chunkCount }
                        .distinct().sortedWith(compareBy({ it.spineIndex }, { it.title }))
                    _uiState.update {
                        it.copy(
                            bookType = BookType.EPUB,
                            epubChunkCount = chunkCount,
                            currentChunkIndex = initialChunk,
                            tableOfContents = contents,
                            currentSpineIndex = currentEpubAnchor?.spineIndex ?: -1,
                            readingProgress = (saved?.progress ?: 0f).coerceIn(0f, 1f)
                        )
                    }
                    if (chunkCount > 0) {
                        loadEpubChunk(initialChunk)
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "This EPUB has no readable content") }
                    }
                } else {
                    val pageCount = bookReader?.getPageCount() ?: 0
                    val initialPage = if (pageCount > 0) {
                        (requestedQuote?.pageNumber?.minus(1) ?: saved?.positionIndex ?: 0).coerceIn(0, pageCount - 1)
                    } else 0
                    _uiState.update {
                        it.copy(bookType = BookType.PDF, pdfPageCount = pageCount,
                            currentPdfPage = initialPage, isLoading = false,
                            tableOfContents = emptyList(),
                            readingProgress = if (pageCount > 0) (initialPage + 1f) / pageCount else 0f)
                    }
                    if (pageCount > 0) {
                        loadPdfPage(initialPage)
                        loadPdfPage(initialPage + 1)
                    }
                }
            } catch (error: Exception) {
                if (error is FileNotFoundException || error is SecurityException ||
                    error.cause is FileNotFoundException || error.cause is SecurityException) {
                    try {
                        recentBooksRepository.markUnavailable(uri)
                    } catch (historyError: Exception) {
                        Timber.w(historyError, "Could not mark book unavailable")
                    }
                }
                _uiState.update {
                    it.copy(isLoading = false,
                        error = context.getString(R.string.failed_to_open_book, error.message))
                }
            }
        }
    }

    private fun loadEpubChunk(
        chunkIndex: Int,
        themeOverride: LumoraTheme? = null,
        scaleOverride: Float? = null,
        fontOverride: String? = null,
        lineSpacingOverride: Float? = null,
        marginOverride: String? = null,
        targetAnchor: EpubReadingAnchor? = null,
        forceFreshHtml: Boolean = false,
        quoteToHighlight: String? = null
    ) {
        val epubReader = bookReader as? EpubBookReader ?: return
        if (chunkIndex !in 0 until epubReader.getChunkCount()) return

        epubChunkJob?.cancel()
        val requestGeneration = ++epubLoadGeneration
        val bookGeneration = readerGeneration
        epubChunkJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, epubFileUri = null) }
            try {
                val currentState = uiState.value
                val currentTheme = themeOverride ?: currentState.theme
                val currentFontSizeScale = scaleOverride ?: currentState.fontSizeScale
                val fontFamily = fontOverride ?: currentState.fontFamily
                val lineSpacing = (lineSpacingOverride ?: currentState.epubLineSpacing).coerceIn(1.4f, 2.2f)
                val margins = marginOverride ?: currentState.epubMargin
                val cacheKey = "$chunkIndex-${currentTheme.name}-$currentFontSizeScale-$fontFamily-$lineSpacing-$margins" +
                    if (forceFreshHtml) "-jump-${++epubNavigationGeneration}" else ""
                val cachedChunk = chunkFileCache[cacheKey] ?: withContext(Dispatchers.IO) {
                    val file = epubReader.generateScrollableHtmlFile(
                        chunkIndex = chunkIndex,
                        fontSizeScale = currentFontSizeScale,
                        theme = currentTheme,
                        fontFamily = fontFamily,
                        lineSpacing = lineSpacing,
                        pageMargin = margins
                    )
                    val authority = "${BuildConfig.APPLICATION_ID}.provider"
                    CachedEpubChunk(FileProvider.getUriForFile(context, authority, file).toString(), file)
                }.also { chunkFileCache[cacheKey] = it }

                if (requestGeneration != epubLoadGeneration ||
                    bookGeneration != readerGeneration || bookReader !== epubReader
                ) return@launch

                // The old page may still be displayed. Defer its deletion until disposal.
                evictEpubChunks(protectedUri = cachedChunk.uri)
                if (initialChunkLoaded && chunkIndex != uiState.value.currentChunkIndex) {
                    positionSaveJob?.cancel()
                    val uri = currentBookUri
                    if (uri != null) {
                        try {
                            recentBooksRepository.updateReadingPosition(
                                uri, uiState.value.currentChunkIndex, currentScrollOffset,
                                (uiState.value.currentChunkIndex +
                                    (currentEpubAnchor?.fraction ?: 0f)) / epubReader.getChunkCount(),
                                currentEpubAnchor?.serialize()
                            )
                        } catch (error: Exception) {
                            Timber.w(error, "Could not save departing EPUB section")
                        }
                    }
                    currentScrollOffset = 0
                    currentEpubAnchor = null
                }
                if (requestGeneration != epubLoadGeneration ||
                    bookGeneration != readerGeneration || bookReader !== epubReader
                ) return@launch
                if (targetAnchor != null) {
                    currentScrollOffset = 0
                    currentEpubAnchor = targetAnchor
                }
                initialChunkLoaded = true
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        epubFileUri = cachedChunk.uri,
                        currentChunkIndex = chunkIndex,
                        readingProgress = (chunkIndex +
                            (currentEpubAnchor?.fraction ?: 0f)) / epubReader.getChunkCount(),
                        currentSpineIndex = currentEpubAnchor?.spineIndex ?: -1,
                        quoteHighlightText = if (quoteToHighlight != null) quoteToHighlight
                            else if (targetAnchor != null ||
                                chunkIndex != currentState.currentChunkIndex
                            ) null else it.quoteHighlightText
                    )
                }
                schedulePositionSave(chunkIndex, currentScrollOffset, epubReader.getChunkCount())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "Failed to load EPUB chunk $chunkIndex")
                if (requestGeneration == epubLoadGeneration) {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load content chunk.") }
                }
            }
        }
    }

    // Called from the WebView's lifecycle on the main thread.
    fun onEpubWebViewAttached(uri: String) {
        epubFilesInUse[uri] = (epubFilesInUse[uri] ?: 0) + 1
    }

    fun onEpubWebViewReleased(uri: String) {
        val remaining = (epubFilesInUse[uri] ?: 0) - 1
        if (remaining > 0) {
            epubFilesInUse[uri] = remaining
        } else {
            epubFilesInUse.remove(uri)
            // An evicted page is safe to delete only after its last WebView is gone.
            deferredChunkDeletes.remove(uri)?.forEach { file ->
                if (chunkFileCache.values.none { it.uri == uri && it.file == file }) {
                    deleteChunkFile(file)
                }
            }
        }
    }

    private fun deleteChunkFile(file: File): Boolean {
        if (!file.exists() || file.delete()) return true
        Timber.w("Failed to delete cached EPUB chunk: ${file.absolutePath}")
        return false
    }

    private fun evictEpubChunks(protectedUri: String? = null) {
        val iterator = chunkFileCache.entries.iterator()
        while (chunkFileCache.size > maxCachedEpubChunks && iterator.hasNext()) {
            val entry = iterator.next().value
            if (entry.uri == protectedUri) continue
            iterator.remove()
            if ((epubFilesInUse[entry.uri] ?: 0) > 0) {
                deferredChunkDeletes.getOrPut(entry.uri) { mutableSetOf() }.add(entry.file)
            } else {
                deleteChunkFile(entry.file)
            }
        }
    }

    private fun clearEpubChunkCache() {
        chunkFileCache.values.forEach { entry ->
            if ((epubFilesInUse[entry.uri] ?: 0) > 0) {
                deferredChunkDeletes.getOrPut(entry.uri) { mutableSetOf() }.add(entry.file)
            } else {
                deleteChunkFile(entry.file)
            }
        }
        chunkFileCache.clear()
    }

    fun goToEpubChapter(spineIndex: Int) {
        val reader = bookReader as? EpubBookReader ?: return
        val state = uiState.value
        if (!state.isEpub || state.isTextSelectionActive || spineIndex < 0) return
        val chunk = reader.chunkForSpine(spineIndex) ?: return
        val anchor = EpubReadingAnchor.create(spineIndex, 0, 0f) ?: return
        // Even a chapter in the same chunk must produce a fresh WebView so its
        // restore script targets the chosen heading rather than the old scroll.
        loadEpubChunk(chunk, targetAnchor = anchor, forceFreshHtml = true)
    }

    fun goToEpubChunk(index: Int) {
        val state = uiState.value
        if (!state.isEpub || state.isTextSelectionActive || index !in 0 until state.epubChunkCount) return
        if (index != state.currentChunkIndex) loadEpubChunk(index)
    }

    fun dismissQuoteHighlight() {
        _uiState.update { it.copy(quoteHighlightText = null) }
    }

    fun nextEpubChunk() {
        if (!uiState.value.isTextSelectionActive) {
            loadEpubChunk(uiState.value.currentChunkIndex + 1)
        }
    }

    fun previousEpubChunk() {
        if (!uiState.value.isTextSelectionActive) {
            loadEpubChunk(uiState.value.currentChunkIndex - 1)
        }
    }

    fun onScrollNearEndOfSection() {
        if (uiState.value.isEpub && uiState.value.currentChunkIndex < uiState.value.epubChunkCount - 1) {
            nextEpubChunk()
        }
    }

    /** Re-render when the measured physical viewport changes (including rotation). */
    fun onPdfViewportWidthChanged(widthPx: Int) {
        if (widthPx <= 0 || !uiState.value.isPdf) return
        if (abs(pdfViewportWidthPx - widthPx) <= 8) return
        pdfViewportWidthPx = widthPx
        pdfPageJobs.values.forEach { it.cancel() }
        pdfPageJobs.clear()
        clearPdfCache()
        resetPdfPageZoom() // old pan offsets belong to the previous viewport
        loadPdfPage(uiState.value.currentPdfPage)
        loadPdfPage(uiState.value.currentPdfPage + 1)
    }

    /**
     * Cache entries are live Compose references. Eviction drops references
     * instead of recycling bitmaps still being drawn on a previous frame.
     */
    private fun clearPdfCache() {
        pdfLru.clear()
        pdfRenderedWidths.clear()
        pageCache.clear()
    }

    private fun cachedPdfBytes(): Long =
        pdfLru.values.sumOf { it.image.allocationByteCount.toLong() } +
            (pdfZoomTile?.page?.image?.allocationByteCount?.toLong() ?: 0L)

    private fun trimPdfCache() {
        val protectedPage = uiState.value.currentPdfPage
        while (cachedPdfBytes() > pdfCacheBudgetBytes && pdfLru.isNotEmpty()) {
            val candidate = pdfLru.keys.firstOrNull { it != protectedPage }
                ?: pdfLru.keys.first()
            pdfLru.remove(candidate)
            pdfRenderedWidths.remove(candidate)
            pageCache.remove(candidate)
        }
    }

    // One render per page and reader generation. The renderer serializes PDF
    // native calls and the LRU ensures page count alone cannot exhaust memory.
    fun loadPdfPage(index: Int) {
        val reader = bookReader as? PdfBookReader ?: return
        if (!uiState.value.isPdf || index !in 0 until uiState.value.pdfPageCount) return
        val width = pdfViewportWidthPx
        if (pageCache[index] != null && (pdfRenderedWidths[index] ?: 0) >= width * 0.9f) {
            pdfLru[index] // refresh LRU recency
            return
        }
        if (pdfPageJobs.containsKey(index)) return
        val generation = readerGeneration
        val renderJob = viewModelScope.launch {
            try {
                val pixelLimit = (pdfCacheBudgetBytes / 8).toInt().coerceAtLeast(250_000)
                val page = reader.renderPage(index, width, pixelLimit)
                if (generation == readerGeneration && bookReader === reader &&
                    uiState.value.isPdf && width == pdfViewportWidthPx &&
                    index in (uiState.value.currentPdfPage - 3)..(uiState.value.currentPdfPage + 3)
                ) {
                    pdfLru[index] = page
                    pdfRenderedWidths[index] = width
                    pageCache[index] = page
                    trimPdfCache()
                    if (index == uiState.value.currentPdfPage) schedulePdfTile()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "Failed to render PDF page $index")
            } finally {
                if (pdfPageJobs[index] === coroutineContext[Job]) {
                    pdfPageJobs.remove(index)
                }
            }
        }
        pdfPageJobs[index] = renderJob
    }

    private fun invalidatePdfTile() {
        pdfTileRequestId++
        pdfTileJob?.cancel()
        pdfTileJob = null
        pdfZoomTile = null
    }

    private fun schedulePdfTile() {
        pdfTileJob?.cancel()
        val state = uiState.value
        val pageIndex = state.currentPdfPage
        if (!state.isPdf || state.pdfPageZoomScale < 1.45f) {
            invalidatePdfTile()
            return
        }
        val base = pageCache[pageIndex] as? Page.PdfPage ?: return
        val reader = bookReader as? PdfBookReader ?: return
        val zoom = state.pdfPageZoomScale
        val displayWidth = pdfViewportWidthPx.toFloat()
        val displayHeight = displayWidth * base.image.height / base.image.width
        val centerX = 0.5f - state.pdfPageOffsetX / (displayWidth * zoom)
        val centerY = 0.5f - state.pdfPageOffsetY / (displayHeight * zoom)
        val half = 0.5f / zoom
        val gutter = 0.12f / zoom
        val region = RectF(
            (centerX - half - gutter).coerceIn(0f, 1f),
            (centerY - half - gutter).coerceIn(0f, 1f),
            (centerX + half + gutter).coerceIn(0f, 1f),
            (centerY + half + gutter).coerceIn(0f, 1f)
        )
        if (region.width() < 0.001f || region.height() < 0.001f) return
        val visibleLeft = (centerX - half).coerceIn(0f, 1f)
        val visibleRight = (centerX + half).coerceIn(0f, 1f)
        val visibleTop = (centerY - half).coerceIn(0f, 1f)
        val visibleBottom = (centerY + half).coerceIn(0f, 1f)
        val existing = pdfZoomTile
        if (existing != null && existing.pageIndex == pageIndex &&
            existing.zoom >= zoom * 0.85f &&
            existing.region.left <= visibleLeft + 0.001f &&
            existing.region.top <= visibleTop + 0.001f &&
            existing.region.right >= visibleRight - 0.001f &&
            existing.region.bottom >= visibleBottom - 0.001f
        ) return
        val requestId = ++pdfTileRequestId
        val generation = readerGeneration
        pdfTileJob = viewModelScope.launch {
            delay(180) // only rasterize after the pinch/pan settles
            try {
                val renderWidth = (region.width() * displayWidth * zoom * 1.15f)
                    .roundToInt().coerceAtLeast(1)
                val pixelLimit = (pdfCacheBudgetBytes / 8).toInt().coerceAtLeast(250_000)
                val tile = reader.renderTile(pageIndex, region, renderWidth, pixelLimit)
                if (generation == readerGeneration && bookReader === reader &&
                    requestId == pdfTileRequestId && uiState.value.currentPdfPage == pageIndex &&
                    uiState.value.pdfPageZoomScale >= 1.45f
                ) {
                    pdfZoomTile = PdfZoomTile(pageIndex, RectF(region), zoom, tile)
                    trimPdfCache()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "Failed to render zoom tile on PDF page $pageIndex")
            }
        }
    }

    fun onPdfPageChanged(index: Int) {
        if (index !in 0 until uiState.value.pdfPageCount) return
        if (!uiState.value.isTextSelectionActive) {
            if (index != uiState.value.currentPdfPage) {
                resetPdfPageZoom()
            }
            _uiState.update {
                it.copy(
                    currentPdfPage = index,
                    readingProgress = (index + 1f) / it.pdfPageCount.coerceAtLeast(1),
                    quoteHighlightText = if (index == it.currentPdfPage) it.quoteHighlightText else null
                )
            }
            schedulePositionSave(index, 0, uiState.value.pdfPageCount)
            prunePdfPageCache(index)
            loadPdfPage(index)
            loadPdfPage(index - 1)
            loadPdfPage(index + 1)
            loadPdfPage(index + 2)
        }
    }

    private fun prunePdfPageCache(currentPageIndex: Int) {
        val keepRange = (currentPageIndex - 3)..(currentPageIndex + 3)
        pdfLru.keys.filter { it !in keepRange }.forEach { index ->
            pdfLru.remove(index)
            pdfRenderedWidths.remove(index)
            pageCache.remove(index)
        }
        pdfPageJobs.entries.filter { it.key !in keepRange }.forEach { (index, job) ->
            pdfPageJobs.remove(index)
            job.cancel()
        }
        trimPdfCache()
    }

    fun onToggleMenuVisibility() {
        if (!uiState.value.isTextSelectionActive) {
            _uiState.update { it.copy(isMenuVisible = !it.isMenuVisible, focusMode = false) }
        }
    }

    fun setFocusMode(enabled: Boolean) {
        if (uiState.value.isTextSelectionActive) return
        _uiState.update { it.copy(focusMode = enabled, isMenuVisible = false) }
    }

    fun onReaderSurfaceTapped() {
        if (uiState.value.focusMode) setFocusMode(false) else onToggleMenuVisibility()
    }

    fun onThemeChange(theme: LumoraTheme) {
        viewModelScope.launch {
            userPreferencesRepository.setTheme(theme)
            // The flow in 'init' will trigger a state update. We just need to reload the chunk.
            if (uiState.value.isEpub) {
                loadEpubChunk(uiState.value.currentChunkIndex, themeOverride = theme)
            }
        }
    }

    fun onFontFamilyChange(family: String) {
        if (family !in setOf("Georgia", "Sans", "Mono")) return
        viewModelScope.launch {
            userPreferencesRepository.setFontFamily(family)
            if (uiState.value.isEpub) {
                loadEpubChunk(uiState.value.currentChunkIndex, fontOverride = family)
            }
        }
    }

    fun onFontSizeChange(scale: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setFontSizeScale(scale)
            if (uiState.value.isEpub) {
                loadEpubChunk(uiState.value.currentChunkIndex, scaleOverride = scale)
            }
        }
    }


    fun onEpubLineSpacingChange(spacing: Float) {
        val next = spacing.coerceIn(1.4f, 2.2f)
        viewModelScope.launch {
            userPreferencesRepository.setEpubLineSpacing(next)
            if (uiState.value.isEpub) {
                loadEpubChunk(uiState.value.currentChunkIndex, lineSpacingOverride = next)
            }
        }
    }

    fun onEpubMarginChange(margin: String) {
        if (margin !in setOf("Compact", "Comfortable", "Wide")) return
        viewModelScope.launch {
            userPreferencesRepository.setEpubMargin(margin)
            if (uiState.value.isEpub) {
                loadEpubChunk(uiState.value.currentChunkIndex, marginOverride = margin)
            }
        }
    }

    fun onPdfNightModeChange(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setPdfNightMode(enabled) }
    }

    fun onPdfPageSpacingChange(spacingDp: Int) {
        if (spacingDp !in setOf(8, 24)) return
        viewModelScope.launch { userPreferencesRepository.setPdfPageSpacingDp(spacingDp) }
    }

    fun onTextSelected(text: String, sourceFileUri: String? = null, spineIndex: Int = -1, characterOffset: Int = 0, fraction: Float = 0f) {
        val normalized = text.trim()
        selectedEpubAnchor = if (normalized.isNotEmpty() &&
            sourceFileUri != null && sourceFileUri == uiState.value.epubFileUri
        ) EpubReadingAnchor.create(spineIndex, characterOffset, fraction) else null
        _uiState.update {
            it.copy(
                selectedText = normalized,
                isTextSelectionActive = normalized.isNotEmpty()
            )
        }
    }

    fun clearTextSelection() {
        selectedEpubAnchor = null
        _uiState.update {
            it.copy(
                selectedText = "",
                isTextSelectionActive = false
            )
        }
    }

    fun saveSelectedQuote() {
        val selectedText = _uiState.value.selectedText
        if (selectedText.isNotBlank()) {
            viewModelScope.launch {
                val currentState = _uiState.value
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                // Auto-categorize the theme based on text content
                val detectedTheme = QuoteThemeClassifier.detect(selectedText)
                val quote = Quote(
                    text = selectedText,
                    author = currentState.bookAuthor ?: "Unknown Author",
                    theme = detectedTheme.name,
                    date = currentDate,
                    starred = false,
                    bookTitle = currentState.bookTitle,
                    bookAuthor = currentState.bookAuthor,
                    pageNumber = if (currentState.isPdf) currentState.currentPdfPage + 1 else null,
                    chapterTitle = if (currentState.isEpub) "Section ${currentState.currentChunkIndex + 1}" else null,
                    sourceUri = currentBookUri,
                    epubChunkIndex = if (currentState.isEpub) currentState.currentChunkIndex else null,
                    epubAnchor = if (currentState.isEpub) selectedEpubAnchor?.serialize() else null
                )
                try {
                    quoteRepository.saveQuote(quote)
                    selectedEpubAnchor = null
                    _uiState.update {
                        it.copy(
                            selectedText = "",
                            isTextSelectionActive = false,
                            quoteSaveStatus = "Quote saved successfully!"
                        )
                    }
                    // Clear the success message after 3 seconds
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _uiState.update { it.copy(quoteSaveStatus = null) }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            quoteSaveStatus = "Failed to save quote: ${e.message}",
                            // Keep selection active on error so user can try again
                            isTextSelectionActive = true
                        )
                    }
                }
            }
        }
    }

    // PDF Zoom functions
    fun onPdfPageZoomChange(scale: Float, offsetX: Float, offsetY: Float) {
        val clampedScale = scale.coerceIn(1f, 4f)
        val width = pdfViewportWidthPx.toFloat()
        val bitmap = (pageCache[uiState.value.currentPdfPage] as? Page.PdfPage)?.image
        val height = if (bitmap != null && bitmap.width > 0) {
            width * bitmap.height / bitmap.width
        } else width * 1.414f
        // Accessible zoom buttons and touch gestures use the same bounded pan.
        // Without this, zooming out can strand the viewport outside the page.
        val maxX = width * (clampedScale - 1f) / 2f
        val maxY = height * (clampedScale - 1f) / 2f
        _uiState.update {
            it.copy(
                pdfPageZoomScale = clampedScale,
                pdfPageOffsetX = offsetX.coerceIn(-maxX, maxX),
                pdfPageOffsetY = offsetY.coerceIn(-maxY, maxY)
            )
        }
        schedulePdfTile()
    }

    fun resetPdfPageZoom() {
        invalidatePdfTile()
        _uiState.update {
            it.copy(
                pdfPageZoomScale = 1f,
                pdfPageOffsetX = 0f,
                pdfPageOffsetY = 0f
            )
        }
    }

    // EPUB JavaScript bridge calls this on a WebView thread.
    fun saveScrollPosition(position: Int) {
        val state = uiState.value
        if (!state.isEpub || state.isLoading || position < 0 || position == currentScrollOffset) return
        currentScrollOffset = position
        schedulePositionSave(state.currentChunkIndex, position, state.epubChunkCount)
    }

    /**
     * Accept location updates only from the currently displayed generated HTML.
     * The WebView bridge calls this on the UI thread.
     */
    fun saveEpubPosition(
        sourceFileUri: String,
        pixelOffset: Int,
        spineIndex: Int,
        characterOffset: Int,
        fraction: Float
    ) {
        val state = uiState.value
        if (!state.isEpub || state.isLoading || state.epubFileUri != sourceFileUri ||
            pixelOffset < 0
        ) return
        val anchor = EpubReadingAnchor.create(spineIndex, characterOffset, fraction) ?: return
        if (currentScrollOffset == pixelOffset && currentEpubAnchor == anchor) return
        currentScrollOffset = pixelOffset
        currentEpubAnchor = anchor
        _uiState.update {
            it.copy(
                readingProgress = (state.currentChunkIndex + anchor.fraction) /
                    state.epubChunkCount.coerceAtLeast(1),
                currentSpineIndex = anchor.spineIndex
            )
        }
        schedulePositionSave(state.currentChunkIndex, pixelOffset, state.epubChunkCount)
    }

    fun getEpubAnchor(): EpubReadingAnchor? = currentEpubAnchor

    fun getScrollPosition(): Int = currentScrollOffset

    private fun schedulePositionSave(index: Int, offset: Int, total: Int) {
        val uri = currentBookUri ?: return
        if (total <= 0) return
        val isEpub = uiState.value.isEpub
        val anchor = if (isEpub) currentEpubAnchor?.serialize() else null
        val progress = if (isEpub) (index + (currentEpubAnchor?.fraction ?: 0f)) / total
            else (index + 1f) / total
        positionSaveJob?.cancel()
        positionSaveJob = viewModelScope.launch {
            // Collapse frequent WebView scroll callbacks into one database write.
            delay(500)
            try {
                recentBooksRepository.updateReadingPosition(
                    uri, index, offset, progress.coerceIn(0f, 1f), anchor
                )
            } catch (error: Exception) {
                Timber.w(error, "Could not persist reading position")
            }
        }
    }

    override fun onCleared() {
        bookmarkObserveJob?.cancel()
        quoteObserveJob?.cancel()
        bookOpenJob?.cancel()
        epubChunkJob?.cancel()
        pdfPageJobs.values.forEach { it.cancel() }
        pdfPageJobs.clear()
        pdfTileJob?.cancel()
        pdfZoomTile = null
        positionSaveJob?.cancel()
        clearEpubChunkCache()
        deferredChunkDeletes.values.flatten().forEach { deleteChunkFile(it) }
        deferredChunkDeletes.clear()
        epubFilesInUse.clear()
        bookReader?.close()
        clearPdfCache()
        super.onCleared()
    }
}
