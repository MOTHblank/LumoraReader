package com.mothblank.lumorareader.presentation.reader

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.BuildConfig
import com.mothblank.lumorareader.data.EpubBookReader
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
    private val quoteRepository: QuoteRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    val pageCache = mutableStateMapOf<Int, Page>()
    private var bookReader: BookReader? = null
    private val chunkFileCache = mutableMapOf<String, String>() // Key is "chunkIndex-themeName-fontSize"
    // Store scroll position for each book URI
    private val scrollPositions = mutableMapOf<String, Int>()
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val bookDetails = bookRepository.getBookDetails(uri)
                bookReader = withContext(Dispatchers.IO) { bookRepository.openBook(bookDetails) }
                // Store book information for quotes
                _uiState.update { currentState ->
                    currentState.copy(
                        bookTitle = bookDetails.title,
                        bookAuthor = bookDetails.author
                    )
                }
                if (bookReader is EpubBookReader) {
                    val epubReader = bookReader as EpubBookReader
                    _uiState.update { it.copy(bookType = BookType.EPUB, epubChunkCount = epubReader.getChunkCount()) }
                    loadEpubChunk(0)
                } else {
                    val pageCount = bookReader?.getPageCount() ?: 0
                    _uiState.update { it.copy(bookType = BookType.PDF, pdfPageCount = pageCount, isLoading = (pageCount == 0)) }
                    if (pageCount > 0) {
                        // Preload first few pages
                        loadPdfPage(0)
                        loadPdfPage(1)
                        loadPdfPage(2)
                        loadPdfPage(3)
                        loadPdfPage(4)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.failed_to_open_book, e.message)) }
            }
        }
    }

    private fun loadEpubChunk(chunkIndex: Int) {
        val epubReader = bookReader as? EpubBookReader ?: return
        if (chunkIndex < 0 || chunkIndex >= epubReader.getChunkCount()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, epubFileUri = null) }
            try {
                val currentState = uiState.value
                val currentTheme = currentState.theme
                val currentFontSizeScale = currentState.fontSizeScale
                val cacheKey = "$chunkIndex-${currentTheme.name}-$currentFontSizeScale"
                val fileUri = chunkFileCache.getOrPut(cacheKey) {
                    withContext(Dispatchers.IO) {
                        val file = epubReader.generateScrollableHtmlFile(
                            chunkIndex = chunkIndex,
                            fontSizeScale = currentFontSizeScale,
                            theme = currentTheme // Pass the theme enum
                        )
                        val authority = "${BuildConfig.APPLICATION_ID}.provider"
                        FileProvider.getUriForFile(context, authority, file).toString()
                    }
                }
                _uiState.update {
                    it.copy(isLoading = false, epubFileUri = fileUri, currentChunkIndex = chunkIndex)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load content chunk.") }
            }
        }
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

    // Dedicated function to load a single PDF page
    fun loadPdfPage(index: Int) {
        if (index < 0 || index >= uiState.value.pdfPageCount || pageCache.containsKey(index)) return
        viewModelScope.launch {
            try {
                val pageResult = withContext(Dispatchers.IO) { bookReader?.getPage(index) }
                if (pageResult != null) pageCache[index] = pageResult
            } catch (e: Exception) {
                Timber.e(e, "Failed to load PDF page $index")
            }
        }
    }

    fun onPdfPageChanged(index: Int) {
        if (!uiState.value.isTextSelectionActive) {
            _uiState.update { it.copy(currentPdfPage = index) }
            // Reset zoom when page changes
            resetPdfPageZoom()
            // Preload adjacent pages
            // Previous page
            if (index > 0) {
                loadPdfPage(index - 1)
            }
            // Next pages
            loadPdfPage(index + 1)
            loadPdfPage(index + 2)
            loadPdfPage(index + 3)
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

    // Scroll position management
    fun saveScrollPosition(position: Int) {
        currentBookUri?.let { uri ->
            scrollPositions[uri] = position
        }
    }

    fun getScrollPosition(): Int {
        return currentBookUri?.let { uri ->
            scrollPositions[uri] ?: 0
        } ?: 0
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
        bookReader?.close()
        pageCache.clear()
        chunkFileCache.values.forEach { uri ->
            try {
                val file = Uri.parse(uri).toFile()
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Timber.w("Failed to delete cached EPUB chunk: $e")
            }
        }
        chunkFileCache.clear()
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