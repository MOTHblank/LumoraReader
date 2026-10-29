package com.mothblank.lumorareader.presentation.quotes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.domain.repository.QuoteRepository
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class QuoteViewModel @Inject constructor(
    private val repository: QuoteRepository
) : ViewModel() {
    // --- 1. SINGLE SOURCE OF TRUTH for the entire screen's state ---
    private val _uiState = MutableStateFlow(QuotesUiState())
    val uiState: StateFlow<QuotesUiState> = _uiState.asStateFlow()

    // --- 2. REACTIVE DATA FLOW ---
    // This private flow listens for changes in our UI state's filter options.
    // Whenever a filter changes, it automatically triggers a new, efficient database query.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredQuotesFlow: Flow<List<Quote>> = _uiState
        .map { it.filterOptions } // We only care when the filter options change
        .distinctUntilChanged()   // Don't re-query if the options are identical
        .flatMapLatest { options ->
            repository.getFilteredQuotes(
                query = options.searchQuery,
                theme = options.filterTheme,
                starred = if (options.filterStarred) true else null,
                sortBy = options.sortBy.name
            )
        }

    init {
        // Start collecting the quotes flow as soon as the ViewModel is created.
        // The collected quotes are used to update our main UI state.
        viewModelScope.launch {
            filteredQuotesFlow.collect { quotes ->
                _uiState.update { it.copy(quotes = quotes) }
            }
        }
    }

    // --- 3. PUBLIC FUNCTIONS to handle UI events ---
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(filterOptions = it.filterOptions.copy(searchQuery = query)) }
    }

    fun onSortByChange(sortBy: SortBy) {
        _uiState.update { it.copy(filterOptions = it.filterOptions.copy(sortBy = sortBy)) }
    }

    fun onFilterThemeChange(theme: String?) {
        _uiState.update { it.copy(filterOptions = it.filterOptions.copy(filterTheme = theme)) }
    }

    fun onFilterStarredChange(isStarred: Boolean) {
        _uiState.update { it.copy(filterOptions = it.filterOptions.copy(filterStarred = isStarred)) }
    }

    fun onExportQuotes(uri: Uri, context: Context, quotes: List<Quote>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = Gson().toJson(quotes)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }
            } catch (e: Exception) {
                // Handle error - could emit to a shared flow for UI feedback
            }
        }
    }

    fun onImportQuotes(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        reader.readText()
                    }
                }
                if (!json.isNullOrBlank()) {
                    val type = object : TypeToken<List<Quote>>() {}.type
                    val importedQuotes: List<Quote> = Gson().fromJson(json, type)
                    importedQuotes.forEach { quote ->
                        // Reset ID to 0 to create new quotes instead of overwriting
                        saveQuote(quote.copy(id = 0))
                    }
                }
            } catch (e: Exception) {
                // Handle error - could emit to a shared flow for UI feedback
            }
        }
    }

    fun saveQuote(quote: Quote) {
        viewModelScope.launch {
            repository.saveQuote(quote)
        }
    }

    fun addQuote(quote: Quote) {
        viewModelScope.launch {
            if (quote.text.isNotBlank() && quote.author.isNotBlank()) {
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                repository.saveQuote(
                    quote.copy(
                        id = 0,
                        text = quote.text.trim(),
                        author = quote.author.trim(),
                        theme = quote.theme ?: autoCategorizeTheme(quote.text).name,
                        date = currentDate
                    )
                )
            }
        }
    }

    fun editQuote(quote: Quote) {
        viewModelScope.launch {
            // An explicit user theme/category choice must not be silently
            // overwritten by automatic text classification during editing.
            repository.saveQuote(quote.copy(theme = quote.theme ?: autoCategorizeTheme(quote.text).name))
        }
    }

    fun deleteQuote(id: Int) {
        viewModelScope.launch {
            repository.deleteQuoteById(id)
        }
    }

    fun toggleStarred(quote: Quote) {
        viewModelScope.launch {
            repository.saveQuote(quote.copy(starred = !quote.starred))
        }
    }

    // --- 4. ENHANCED HELPER LOGIC (Auto-categorization) ---
    private val themeKeywords: Map<LumoraTheme, Set<String>> = mapOf(
        LumoraTheme.GOTH to setOf(
            "dark", "darkness", "night", "shadow", "shadows", "goth", "gothic", "black",
            "mystery", "mysterious", "moon", "moonlight", "noir", "obscure", "void",
            "raven", "gloom", "gloomy", "macabre", "death", "melancholy", "somber",
            "sinister", "haunting", "eerie", "midnight", "abyss", "phantom", "spectral"
        ),
        LumoraTheme.COQUETTE to setOf(
            "love", "loving", "pink", "sweet", "sweetness", "coquette", "cute", "adorable",
            "affection", "darling", "charm", "charming", "flirt", "flirty", "blush",
            "blushing", "rose", "roses", "kiss", "kisses", "lace", "delicate", "tender",
            "gentle", "beauty", "beautiful", "pretty", "feminine", "soft", "grace"
        ),
        LumoraTheme.PIRATE to setOf(
            "sea", "ocean", "pirate", "pirates", "adventure", "adventures", "ship", "ships",
            "treasure", "treasures", "sail", "sailing", "island", "islands", "rum",
            "captain", "voyage", "voyages", "map", "maps", "parrot", "anchor", "skull",
            "compass", "storm", "waves", "port", "harbor", "crew", "fleet", "maritime"
        ),
        LumoraTheme.NATURE to setOf(
            "nature", "natural", "tree", "trees", "green", "earth", "forest", "forests",
            "leaf", "leaves", "river", "rivers", "mountain", "mountains", "wild",
            "wilderness", "flower", "flowers", "meadow", "meadows", "breeze", "wind",
            "garden", "gardens", "root", "roots", "wood", "woods", "sun", "sunshine",
            "sky", "cloud", "clouds", "grass", "flora", "fauna", "organic", "botanical"
        ),
        LumoraTheme.ROMANCE to setOf(
            "romance", "romantic", "rose", "roses", "heart", "hearts", "cupid", "valentine",
            "sweetheart", "passion", "passionate", "petal", "petals", "embrace",
            "embracing", "forever", "eternal", "desire", "longing", "beloved", "soulmate",
            "intimacy", "devotion", "adoration", "amour", "courtship", "enchantment"
        ),
        LumoraTheme.GRUNGE to setOf(
            "grunge", "grungy", "dirty", "worn", "distressed", "punk", "rebel", "rebellion",
            "urban", "street", "graffiti", "angst", "angry", "static", "concrete", "rough",
            "raw", "broken", "torn", "faded", "weathered", "gritty", "industrial", "harsh"
        ),
        LumoraTheme.MYSTIC to setOf(
            "magic", "magical", "mystic", "mystical", "enchantment", "enchanted",
            "spiritual", "spirit", "aura", "cosmos", "cosmic", "galaxy", "celestial",
            "dream", "dreams", "fantasy", "esoteric", "oracle", "alchemy", "mysterious",
            "supernatural", "divine", "transcendent", "ethereal", "astral", "metaphysical"
        ),
        LumoraTheme.CYBER to setOf(
            "cyber", "cybernetic", "tech", "technology", "digital", "code", "coding",
            "matrix", "neon", "circuit", "circuits", "glitch", "virtual", "hacker",
            "hackers", "binary", "future", "futuristic", "android", "androids", "synth",
            "synthetic", "artificial", "robot", "robots", "data", "network", "quantum"
        ),
        LumoraTheme.VAMPIRE to setOf(
            "vampire", "vampires", "blood", "bloody", "red", "crimson", "nightmare",
            "nightmares", "fang", "fangs", "bat", "bats", "scarlet", "immortal",
            "immortality", "gothic", "bite", "biting", "grave", "graves", "moonlight",
            "eternal", "undead", "dark", "darkness", "neck", "thirst", "nocturnal"
        )
    )

    private fun autoCategorizeTheme(text: String): LumoraTheme {
        val cleanText = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ") // Replace punctuation with spaces
            .split(Regex("\\s+")) // Split on whitespace
            .filter { it.length > 2 } // Filter out very short words
            .toSet()

        if (cleanText.isEmpty()) return LumoraTheme.DEFAULT

        // Calculate match scores for each theme
        val themeScores = themeKeywords.mapValues { (_, keywords) ->
            var score = 0
            cleanText.forEach { word ->
                keywords.forEach { keyword ->
                    when {
                        word == keyword -> score += 3 // Exact match gets highest score
                        word.contains(keyword) -> score += 2 // Partial match
                        keyword.contains(word) -> score += 1 // Reverse partial match
                    }
                }
            }
            score
        }

        // Find the theme with the highest score
        val bestMatch = themeScores.filter { it.value > 0 }.maxByOrNull { it.value }
        return bestMatch?.key ?: LumoraTheme.DEFAULT
    }
}

// --- 5. STATE DEFINITION CLASSES ---
data class QuotesUiState(
    val quotes: List<Quote> = emptyList(),
    val filterOptions: QuoteFilterOptions = QuoteFilterOptions(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class QuoteFilterOptions(
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.DATE_DESC,
    val filterTheme: String? = null,
    val filterStarred: Boolean = false
)

enum class SortBy {
    DATE_DESC,
    DATE_ASC,
    AUTHOR,
    TEXT_LENGTH
}