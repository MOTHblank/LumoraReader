package com.mothblank.lumorareader.presentation.reader

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.BuildConfig
import com.mothblank.lumorareader.data.EpubBookReader
import com.mothblank.lumorareader.data.RecentBooksRepository
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
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
    private val quoteRepository: QuoteRepository,
    private val recentBooksRepository: RecentBooksRepository,
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
    private var readerGeneration = 0L
    private var epubLoadGeneration = 0L
    private var epubChunkJob: Job? = null
    private var bookOpenJob: Job? = null
    @Volatile private var currentScrollOffset = 0
    private var initialChunkLoaded = false
    private var positionSaveJob: Job? = null
    private var currentBookUri: String? = null
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
        openBook(bookUri)
    }

    private fun openBook(uri: String) {
        // Invalidate requests before replacing the reader, even if opening suspends.
        bookOpenJob?.cancel()
        readerGeneration++
        pdfPageJobs.values.forEach { it.cancel() }
        pdfPageJobs.clear()
        pageCache.clear()
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

                _uiState.update { it.copy(bookTitle = bookDetails.title, bookAuthor = bookDetails.author) }
                val epubReader = bookReader as? EpubBookReader
                if (epubReader != null) {
                    val chunkCount = epubReader.getChunkCount()
                    val initialChunk = if (chunkCount > 0) {
                        (saved?.positionIndex ?: 0).coerceIn(0, chunkCount - 1)
                    } else 0
                    currentScrollOffset = if (saved?.positionIndex == initialChunk) {
                        saved?.scrollOffset?.coerceAtLeast(0) ?: 0
                    } else 0
                    initialChunkLoaded = false
                    _uiState.update {
                        it.copy(bookType = BookType.EPUB, epubChunkCount = chunkCount, currentChunkIndex = initialChunk)
                    }
                    if (chunkCount > 0) {
                        loadEpubChunk(initialChunk)
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "This EPUB has no readable content") }
                    }
                } else {
                    val pageCount = bookReader?.getPageCount() ?: 0
                    val initialPage = if (pageCount > 0) {
                        (saved?.positionIndex ?: 0).coerceIn(0, pageCount - 1)
                    } else 0
                    _uiState.update {
                        it.copy(bookType = BookType.PDF, pdfPageCount = pageCount,
                            currentPdfPage = initialPage, isLoading = false)
                    }
                    if (pageCount > 0) {
                        for (page in initialPage..minOf(initialPage + 4, pageCount - 1)) {
                            loadPdfPage(page)
                        }
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

    private fun loadEpubChunk(chunkIndex: Int) {
        val epubReader = bookReader as? EpubBookReader ?: return
        if (chunkIndex !in 0 until epubReader.getChunkCount()) return

        epubChunkJob?.cancel()
        val requestGeneration = ++epubLoadGeneration
        val bookGeneration = readerGeneration
        epubChunkJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, epubFileUri = null) }
            try {
                val currentState = uiState.value
                val currentTheme = currentState.theme
                val currentFontSizeScale = currentState.fontSizeScale
                val cacheKey = "$chunkIndex-${currentTheme.name}-$currentFontSizeScale"
                val cachedChunk = chunkFileCache[cacheKey] ?: withContext(Dispatchers.IO) {
                    val file = epubReader.generateScrollableHtmlFile(
                        chunkIndex = chunkIndex,
                        fontSizeScale = currentFontSizeScale,
                        theme = currentTheme
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
                                (uiState.value.currentChunkIndex + 1f) / epubReader.getChunkCount()
                            )
                        } catch (error: Exception) {
                            Timber.w(error, "Could not save departing EPUB section")
                        }
                    }
                    currentScrollOffset = 0
                }
                if (requestGeneration != epubLoadGeneration ||
                    bookGeneration != readerGeneration || bookReader !== epubReader
                ) return@launch
                initialChunkLoaded = true
                _uiState.update {
                    it.copy(isLoading = false, epubFileUri = cachedChunk.uri, currentChunkIndex = chunkIndex)
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

    // One render per page and reader generation; stale renders never populate the UI cache.
    fun loadPdfPage(index: Int) {
        val reader = bookReader ?: return
        if (!uiState.value.isPdf || index !in 0 until uiState.value.pdfPageCount ||
            pageCache.containsKey(index) || pdfPageJobs.containsKey(index)
        ) return
        val generation = readerGeneration
        val renderJob = viewModelScope.launch {
            try {
                val pageResult = withContext(Dispatchers.IO) { reader.getPage(index) }
                val center = uiState.value.currentPdfPage
                if (generation == readerGeneration && bookReader === reader &&
                    uiState.value.isPdf && index in (center - 5)..(center + 5)
                ) {
                    pageCache[index] = pageResult
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "Failed to load PDF page $index")
            } finally {
                if (pdfPageJobs[index] === coroutineContext[Job]) {
                    pdfPageJobs.remove(index)
                }
            }
        }
        pdfPageJobs[index] = renderJob
    }

    fun onPdfPageChanged(index: Int) {
        if (index !in 0 until uiState.value.pdfPageCount) return
        if (!uiState.value.isTextSelectionActive) {
            _uiState.update { it.copy(currentPdfPage = index) }
            schedulePositionSave(index, 0, uiState.value.pdfPageCount)
            resetPdfPageZoom()

            prunePdfPageCache(index)
            if (index > 0) loadPdfPage(index - 1)
            loadPdfPage(index)
            loadPdfPage(index + 1)
            loadPdfPage(index + 2)
            loadPdfPage(index + 3)
        }
    }

    private fun prunePdfPageCache(currentPageIndex: Int) {
        val keepRange = (currentPageIndex - 5)..(currentPageIndex + 5)
        val keysToRemove = pageCache.keys.filter { it !in keepRange }
        keysToRemove.forEach { pageCache.remove(it) }
        pdfPageJobs.entries.filter { it.key !in keepRange }.forEach { (index, job) ->
            pdfPageJobs.remove(index)
            job.cancel()
        }
        if (keysToRemove.isNotEmpty()) {
            Timber.d("Pruned ${keysToRemove.size} pages from PDF render cache. Remaining cache size: ${pageCache.size}")
        }
    }

    fun onToggleMenuVisibility() {
        if (!uiState.value.isTextSelectionActive) {
            _uiState.update { it.copy(isMenuVisible = !it.isMenuVisible) }
        }
    }

    fun onThemeChange(theme: LumoraTheme) {
        viewModelScope.launch {
            userPreferencesRepository.setTheme(theme)
            // The flow in 'init' will trigger a state update. We just need to reload the chunk.
            if (uiState.value.isEpub) {
                loadEpubChunk(uiState.value.currentChunkIndex)
            }
        }
    }

    fun onFontSizeChange(scale: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setFontSizeScale(scale)
            if (uiState.value.isEpub) {
                loadEpubChunk(uiState.value.currentChunkIndex)
            }
        }
    }


    fun onTextSelected(text: String) {
        _uiState.update {
            it.copy(
                selectedText = text.trim(),
                isTextSelectionActive = text.trim().isNotEmpty()
            )
        }
    }

    fun clearTextSelection() {
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
                val detectedTheme = autoCategorizeTheme(selectedText)
                val quote = Quote(
                    text = selectedText,
                    author = currentState.bookAuthor ?: "Unknown Author",
                    theme = detectedTheme.name,
                    date = currentDate,
                    starred = false,
                    bookTitle = currentState.bookTitle,
                    bookAuthor = currentState.bookAuthor,
                    pageNumber = if (currentState.isPdf) currentState.currentPdfPage + 1 else null,
                    chapterTitle = if (currentState.isEpub) "Chapter ${currentState.currentChunkIndex + 1}" else null
                )
                try {
                    quoteRepository.saveQuote(quote)
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
        _uiState.update {
            it.copy(
                pdfPageZoomScale = scale,
                pdfPageOffsetX = offsetX,
                pdfPageOffsetY = offsetY
            )
        }
    }

    fun resetPdfPageZoom() {
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

    fun getScrollPosition(): Int = currentScrollOffset

    private fun schedulePositionSave(index: Int, offset: Int, total: Int) {
        val uri = currentBookUri ?: return
        if (total <= 0) return
        positionSaveJob?.cancel()
        positionSaveJob = viewModelScope.launch {
            // Collapse frequent WebView scroll callbacks into one database write.
            delay(500)
            try {
                recentBooksRepository.updateReadingPosition(
                    uri, index, offset, (index + 1f) / total
                )
            } catch (error: Exception) {
                Timber.w(error, "Could not persist reading position")
            }
        }
    }

    // Enhanced auto-categorization with better keyword matching
    private val themeKeywords: Map<LumoraTheme, Set<String>> = mapOf(
        LumoraTheme.GOTH to setOf("dark", "darkness", "night", "shadow", "shadows", "goth", "gothic", "black", "mystery", "mysterious", "moon", "moonlight", "noir", "obscure", "void", "raven", "gloom", "gloomy", "macabre", "death", "melancholy", "somber", "sinister", "haunting", "eerie"),
        LumoraTheme.COQUETTE to setOf("love", "loving", "pink", "sweet", "sweetness", "coquette", "cute", "adorable", "affection", "darling", "charm", "charming", "flirt", "flirty", "blush", "blushing", "rose", "roses", "kiss", "kisses", "lace", "delicate", "tender", "gentle", "beauty", "beautiful", "pretty"),
        LumoraTheme.PIRATE to setOf("sea", "ocean", "pirate", "pirates", "adventure", "adventures", "ship", "ships", "treasure", "treasures", "sail", "sailing", "island", "islands", "rum", "captain", "voyage", "voyages", "map", "maps", "parrot", "anchor", "skull", "compass", "storm", "waves", "port", "harbor"),
        LumoraTheme.NATURE to setOf("nature", "natural", "tree", "trees", "green", "earth", "forest", "forests", "leaf", "leaves", "river", "rivers", "mountain", "mountains", "wild", "wilderness", "flower", "flowers", "meadow", "meadows", "breeze", "wind", "garden", "gardens", "root", "roots", "wood", "woods", "sun", "sunshine", "sky", "cloud", "clouds", "grass", "flora", "fauna"),
        LumoraTheme.ROMANCE to setOf("romance", "romantic", "rose", "roses", "heart", "hearts", "cupid", "valentine", "sweetheart", "passion", "passionate", "petal", "petals", "embrace", "embracing", "forever", "eternal", "desire", "longing", "beloved", "soulmate", "intimacy", "devotion", "adoration"),
        LumoraTheme.GRUNGE to setOf("grunge", "grungy", "dirty", "worn", "distressed", "punk", "rebel", "rebellion", "urban", "street", "graffiti", "angst", "angry", "static", "concrete", "rough", "raw", "broken", "torn", "faded", "weathered", "gritty"),
        LumoraTheme.MYSTIC to setOf("magic", "magical", "mystic", "mystical", "enchantment", "enchanted", "spiritual", "spirit", "aura", "cosmos", "cosmic", "galaxy", "celestial", "dream", "dreams", "fantasy", "esoteric", "oracle", "alchemy", "mysterious", "supernatural", "divine", "transcendent", "ethereal", "astral"),
        LumoraTheme.CYBER to setOf("cyber", "cybernetic", "tech", "technology", "digital", "code", "coding", "matrix", "neon", "circuit", "circuits", "glitch", "virtual", "hacker", "hackers", "binary", "future", "futuristic", "android", "androids", "synth", "synthetic", "artificial", "robot", "robots", "data", "network"),
        LumoraTheme.VAMPIRE to setOf("vampire", "vampires", "blood", "bloody", "red", "crimson", "nightmare", "nightmares", "fang", "fangs", "bat", "bats", "scarlet", "immortal", "immortality", "gothic", "bite", "biting", "grave", "graves", "moonlight", "eternal", "undead", "dark", "darkness", "bite", "neck", "thirst")
    )

    private fun autoCategorizeTheme(text: String): LumoraTheme {
        val inputWords = text.lowercase()
            .split(Regex("[^a-z0-9]"))
            .filter { it.isNotBlank() && it.length > 2 } // Filter out very short words
            .toSet()
        if (inputWords.isEmpty()) return LumoraTheme.DEFAULT
        val normalized = " ${text.lowercase().replace(Regex("[^a-z0-9 ]"), " ")} "
        return themeKeywords.entries
            .maxByOrNull { (_, keywords) ->
                keywords.count { word -> normalized.contains(" $word ") }
            }?.key ?: LumoraTheme.DEFAULT
        val themeScores = themeKeywords.mapValues { (_, keywords) ->
            inputWords.count { word ->
                keywords.any { keyword ->
                    word.contains(keyword) || keyword.contains(word)
                }
            }
        }
        val bestMatch = themeScores.filter { it.value > 0 }.maxByOrNull { it.value }
        return bestMatch?.key ?: LumoraTheme.DEFAULT
    }

    override fun onCleared() {
        bookOpenJob?.cancel()
        epubChunkJob?.cancel()
        pdfPageJobs.values.forEach { it.cancel() }
        pdfPageJobs.clear()
        positionSaveJob?.cancel()
        clearEpubChunkCache()
        deferredChunkDeletes.values.flatten().forEach { deleteChunkFile(it) }
        deferredChunkDeletes.clear()
        epubFilesInUse.clear()
        bookReader?.close()
        pageCache.clear()
        super.onCleared()
    }
}

data class ReaderUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val bookType: BookType? = null,
    val isMenuVisible: Boolean = false,
    val theme: LumoraTheme = LumoraTheme.DEFAULT,
    val fontSizeScale: Float = 1.0f,
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
}