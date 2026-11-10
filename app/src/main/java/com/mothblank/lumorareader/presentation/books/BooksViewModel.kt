package com.mothblank.lumorareader.presentation.books

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.data.RecentBooksRepository
import com.mothblank.lumorareader.data.EpubSearchRepository
import com.mothblank.lumorareader.data.EpubBookReader
import com.mothblank.lumorareader.data.PdfSearchRepository
import com.mothblank.lumorareader.domain.BookType
import com.mothblank.lumorareader.data.db.entity.EpubIndexStatusEntity
import com.mothblank.lumorareader.data.db.entity.PdfIndexStatusEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.mothblank.lumorareader.domain.model.RecentBook
import com.mothblank.lumorareader.domain.model.ReadingStatus
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
    private val bookRepository: BookRepository,
    private val epubSearchRepository: EpubSearchRepository,
    private val pdfSearchRepository: PdfSearchRepository
) : ViewModel() {

    // 1️⃣  In-memory cache for metadata
    private val metaCache = mutableMapOf<String, Book>() // uri -> Book

    private val _recentBooks = MutableStateFlow<List<RecentBook>>(emptyList())
    val recentBooks: StateFlow<List<RecentBook>> = _recentBooks.asStateFlow()

    val epubIndexStatuses: StateFlow<Map<String, EpubIndexStatusEntity>> =
        epubSearchRepository.statuses().map { rows -> rows.associateBy { it.bookUri } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val pdfIndexStatuses: StateFlow<Map<String, PdfIndexStatusEntity>> =
        pdfSearchRepository.statuses().map { rows -> rows.associateBy { it.bookUri } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _indexingBooks = MutableStateFlow<Set<String>>(emptySet())
    val indexingBooks: StateFlow<Set<String>> = _indexingBooks.asStateFlow()

    /** Continue a partial index with a larger per-book allowance, or rebuild on request. */
    fun indexBookContent(uri: String, restart: Boolean) {
        if (uri in _indexingBooks.value) return
        _indexingBooks.update { it + uri }
        viewModelScope.launch {
            try {
                val details = cachedBookDetails(uri)
                withContext(Dispatchers.IO) {
                    when (details.type) {
                        BookType.PDF -> pdfSearchRepository.indexBook(uri, prioritize = true, restart = restart)
                        BookType.EPUB -> {
                            val reader = bookRepository.openBook(details) as EpubBookReader
                            reader.use {
                                epubSearchRepository.indexBook(
                                    uri, reader, prioritize = true, restart = restart
                                )
                            }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Unable to index library book")
            } finally {
                _indexingBooks.update { it - uri }
            }
        }
    }

    /** Run or resume OCR only for PDFs whose normal text extraction found no searchable text. */
    fun indexPdfWithOcr(uri: String, restart: Boolean = false) {
        if (uri in _indexingBooks.value) return
        _indexingBooks.update { it + uri }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    pdfSearchRepository.ocrBook(uri, restart = restart)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Unable to OCR-index PDF")
            } finally {
                _indexingBooks.update { it - uri }
            }
        }
    }

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

    fun setFavorite(book: RecentBook, favorite: Boolean) {
        viewModelScope.launch { recentBooksRepository.setFavorite(book.uri, favorite) }
    }

    fun setReadingStatus(book: RecentBook, status: ReadingStatus) {
        viewModelScope.launch { recentBooksRepository.setReadingStatus(book.uri, status) }
    }

    fun setCollection(book: RecentBook, collection: String?) {
        viewModelScope.launch { recentBooksRepository.setCollection(book.uri, collection) }
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