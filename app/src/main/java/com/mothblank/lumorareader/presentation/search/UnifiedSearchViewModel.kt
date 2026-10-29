package com.mothblank.lumorareader.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.data.ReadingActivityRepository
import com.mothblank.lumorareader.data.ReadingBookmarksRepository
import com.mothblank.lumorareader.data.RecentBooksRepository
import com.mothblank.lumorareader.data.db.dao.ReadingActivityTotals
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.domain.model.ReadingBookmark
import com.mothblank.lumorareader.domain.model.RecentBook
import com.mothblank.lumorareader.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class UnifiedSearchResults(
    val books: List<RecentBook> = emptyList(),
    val quotes: List<Quote> = emptyList(),
    val bookmarks: List<ReadingBookmark> = emptyList(),
    val activity: ReadingActivityTotals = ReadingActivityTotals(0L, 0L, 0, 0)
) {
    val isEmpty: Boolean get() = books.isEmpty() && quotes.isEmpty() && bookmarks.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class UnifiedSearchViewModel @Inject constructor(
    private val booksRepository: RecentBooksRepository,
    private val quotesRepository: QuoteRepository,
    private val bookmarksRepository: ReadingBookmarksRepository,
    activityRepository: ReadingActivityRepository
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun setQuery(value: String) { _query.value = value.take(120) }

    private val matches = _query
        .map { it.trim() }
        .debounce(180L)
        .distinctUntilChanged()
        .flatMapLatest { term ->
            combine(
                booksRepository.observeRecentBooks(),
                quotesRepository.searchAll(term),
                bookmarksRepository.searchAll(term)
            ) { books, quotes, bookmarks ->
                val phrase = term.lowercase()
                val matchingBooks = books.filter { book ->
                    phrase.isEmpty() || listOfNotNull(book.title, book.author, book.collectionName)
                        .any { it.contains(phrase, ignoreCase = true) }
                }.sortedWith(compareByDescending<RecentBook> { it.isFavorite }
                    .thenByDescending { it.lastOpened.millis })
                    .take(if (term.isBlank()) 6 else 25)
                Triple(
                    matchingBooks,
                    quotes.take(if (term.isBlank()) 6 else 30),
                    bookmarks.take(if (term.isBlank()) 6 else 30)
                )
            }
        }

    private val since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

    val results: StateFlow<UnifiedSearchResults> = combine(
        matches,
        activityRepository.observeTotals(since)
    ) { (books, quotes, bookmarks), totals ->
        UnifiedSearchResults(books, quotes, bookmarks, totals)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        UnifiedSearchResults()
    )
}
