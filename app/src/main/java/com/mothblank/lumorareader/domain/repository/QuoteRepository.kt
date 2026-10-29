package com.mothblank.lumorareader.domain.repository

import com.mothblank.lumorareader.domain.model.Quote
import kotlinx.coroutines.flow.Flow

/**
 * The contract for the repository that manages all quote-related data operations.
 */
interface QuoteRepository {

    /**
     * Retrieves a filtered and sorted list of quotes directly from the data source.
     * This is the primary method for displaying quotes.
     * @param query The text to search for in the quote's text or author.
     * @param theme The theme name to filter by. If null, all themes are included.
     * @param starred If true, only starred quotes are returned. If null, all are included.
     * @param sortBy A string representation of the sorting method (e.g., "DATE_DESC").
     * @return A Flow that emits the list of matching quotes.
     */
    fun getFilteredQuotes(query: String, theme: String?, starred: Boolean?, sortBy: String): Flow<List<Quote>>
    /** Quotes with a verified document URI; legacy unlinked quotes stay in the library. */
    fun observeForBook(uri: String): Flow<List<Quote>>

    /**
     * Saves a new quote or updates an existing one.
     */
    suspend fun saveQuote(quote: Quote)

    /**
     * Deletes a quote by its unique ID.
     */
    suspend fun deleteQuoteById(id: Int)

    /**
     * Retrieves a single random quote from the database.
     */
    suspend fun getRandomQuote(): Quote?

    suspend fun getQuoteById(id: Int): Quote?
}