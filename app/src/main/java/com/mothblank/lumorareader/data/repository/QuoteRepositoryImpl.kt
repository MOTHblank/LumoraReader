package com.mothblank.lumorareader.data.repository

import com.mothblank.lumorareader.data.db.dao.QuoteDao
import com.mothblank.lumorareader.data.model.QuoteEntity
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteRepositoryImpl @Inject constructor(
    private val dao: QuoteDao
) : QuoteRepository {

    override fun getFilteredQuotes(query: String, theme: String?, starred: Boolean?, sortBy: String): Flow<List<Quote>> {
        return dao.getFilteredQuotes(query, theme, starred, sortBy).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun observeForBook(uri: String): Flow<List<Quote>> =
        dao.observeForBook(uri).map { entries -> entries.map { it.toDomainModel() } }

    override suspend fun getQuoteById(id: Int): Quote? {
        return dao.getQuoteById(id)?.toDomainModel()
    }

    override suspend fun saveQuote(quote: Quote) {
        dao.saveQuote(quote.toEntity())
    }

    override suspend fun deleteQuoteById(id: Int) {
        dao.deleteQuoteById(id)
    }

    override suspend fun getRandomQuote(): Quote? {
        return dao.getRandomQuote()?.toDomainModel()
    }
}

// MAPPING EXTENSIONS - Updated to include all fields
private fun QuoteEntity.toDomainModel(): Quote {
    return Quote(
        id = this.id,
        text = this.text,
        author = this.author,
        theme = this.theme,
        imagePath = this.imagePath,
        date = this.date,
        starred = this.starred,
        categories = this.categories,
        bookTitle = this.bookTitle,
        bookAuthor = this.bookAuthor,
        pageNumber = this.pageNumber,
        chapterTitle = this.chapterTitle,
        sourceUri = this.sourceUri,
        epubChunkIndex = this.epubChunkIndex,
        epubAnchor = this.epubAnchor,
        note = this.note
    )
}

private fun Quote.toEntity(): QuoteEntity {
    return QuoteEntity(
        id = this.id,
        text = this.text,
        author = this.author,
        theme = this.theme,
        imagePath = this.imagePath,
        date = this.date,
        starred = this.starred,
        categories = this.categories,
        bookTitle = this.bookTitle,
        bookAuthor = this.bookAuthor,
        pageNumber = this.pageNumber,
        chapterTitle = this.chapterTitle,
        sourceUri = this.sourceUri,
        epubChunkIndex = this.epubChunkIndex,
        epubAnchor = this.epubAnchor,
        note = this.note
    )
}