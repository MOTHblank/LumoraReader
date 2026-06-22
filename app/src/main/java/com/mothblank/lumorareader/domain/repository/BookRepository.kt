package com.mothblank.lumorareader.domain.repository

import com.mothblank.lumorareader.domain.Book
import com.mothblank.lumorareader.domain.usecase.BookReader

interface BookRepository {
    // Gets metadata about a book from its URI
    suspend fun getBookDetails(uri: String): Book

    // Opens a book and prepares it for reading
    suspend fun openBook(book: Book): BookReader
}