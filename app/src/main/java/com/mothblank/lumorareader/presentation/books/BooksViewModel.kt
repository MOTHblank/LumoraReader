package com.mothblank.lumorareader.presentation.books

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.data.RecentBooksRepository
import com.mothblank.lumorareader.domain.model.RecentBook
import com.mothblank.lumorareader.domain.model.Timestamp
import com.mothblank.lumorareader.domain.Book
import com.mothblank.lumorareader.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val recentBooksRepository: RecentBooksRepository,
    private val bookRepository: BookRepository
) : ViewModel() {

    // 1️⃣  In-memory cache for metadata
    private val metaCache = mutableMapOf<String, Book>() // uri -> Book

    private val _recentBooks = MutableStateFlow<List<RecentBook>>(emptyList())
    val recentBooks: StateFlow<List<RecentBook>> = _recentBooks.asStateFlow()

    init {
        fetchRecentBooks()
    }

    private fun fetchRecentBooks() {
        viewModelScope.launch {
            recentBooksRepository.observeRecentBooks().collect { books ->
                _recentBooks.value = books
            }
        }
    }

    /**
     * 2️⃣  Cached metadata lookup
     */
    private suspend fun cachedBookDetails(uri: String): Book =
        metaCache.getOrPut(uri) { bookRepository.getBookDetails(uri) }

    fun addRecentBook(uri: Uri) {
        viewModelScope.launch {
            val bookDetails = cachedBookDetails(uri.toString())
            val newBook = RecentBook(
                title         = bookDetails.title,
                author        = bookDetails.author,
                uri           = uri.toString(),
                lastOpened    = Timestamp(System.currentTimeMillis()),
                coverImageUri = bookDetails.coverImageUri
            )
            recentBooksRepository.addRecentBook(newBook)
        }
    }

    fun removeRecentBook(uri: String) {
        viewModelScope.launch {
            metaCache.remove(uri)              // optional: evict from cache
            recentBooksRepository.removeRecentBook(uri)
        }
    }

    fun clearRecentBooks() {
        viewModelScope.launch {
            metaCache.clear()                  // optional: clear cache
            recentBooksRepository.clearRecentBooks()
        }
    }
}