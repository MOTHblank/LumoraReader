package com.mothblank.lumorareader.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mothblank.lumorareader.data.model.QuoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the "quotes" table.
 * This interface defines all database operations for quotes.
 */
@Dao
interface QuoteDao {

    /**
     * Retrieves a list of quotes that are filtered and sorted at the database level.
     * This is the primary method for displaying quotes in the main app UI. It is highly
     * efficient as it avoids loading the entire table into memory.
     *
     * @param query The text to search for in the quote's text or author.
     * @param theme The theme name to filter by. If null, all themes are included.
     * @param starred If true, only starred quotes are returned. If null, all are included.
     * @param sortBy A string representation of the sorting method (e.g., "DATE_DESC").
     * @return A Flow that emits the list of matching quotes whenever the data changes.
     */
    @Query("""
        SELECT * FROM quotes 
        WHERE 
            (:query = '' OR text LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR bookTitle LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR categories LIKE '%' || :query || '%')
            AND (:theme IS NULL OR theme = :theme)
            AND (:starred IS NULL OR starred = :starred)
        ORDER BY
            CASE WHEN :sortBy = 'DATE_DESC' THEN date END DESC,
            CASE WHEN :sortBy = 'DATE_ASC' THEN date END ASC,
            CASE WHEN :sortBy = 'AUTHOR' THEN author END ASC,
            CASE WHEN :sortBy = 'TEXT_LENGTH' THEN LENGTH(text) END DESC
    """)
    fun getFilteredQuotes(query: String, theme: String?, starred: Boolean?, sortBy: String): Flow<List<QuoteEntity>>

    /**
     * Fetches a single quote by its primary key (id).
     * This is used by the widget to display a specific, user-selected quote.
     * @return A single [QuoteEntity] or null if no quote with the given id is found.
     */
    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    suspend fun getQuoteById(id: Int): QuoteEntity?

    /** Book-specific excerpts for the reading notebook; bound by URI, never title. */
    @Query("SELECT * FROM quotes WHERE sourceUri = :uri ORDER BY date DESC, id DESC")
    fun observeForBook(uri: String): Flow<List<QuoteEntity>>

    /**
     * Inserts a new quote or updates an existing one if the primary key already exists.
     * The @Upsert annotation handles the insert-or-update logic automatically.
     */
    @Upsert
    suspend fun saveQuote(quote: QuoteEntity)

    /**
     * Deletes a quote from the database using its unique ID.
     */
    @Query("DELETE FROM quotes WHERE id = :id")
    suspend fun deleteQuoteById(id: Int)

    /**
     * Fetches a single random quote from the table.
     * This is used by the widget when it's set to "Random" mode.
     * @return A single random [QuoteEntity] or null if the table is empty.
     */
    @Query("SELECT * FROM quotes ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomQuote(): QuoteEntity?

    /**
     * Get quotes by book title for better organization
     */
    @Query("SELECT * FROM quotes WHERE bookTitle = :bookTitle ORDER BY pageNumber ASC, date DESC")
    fun getQuotesByBook(bookTitle: String): Flow<List<QuoteEntity>>

    /**
     * Get all unique book titles that have quotes
     */
    @Query("SELECT DISTINCT bookTitle FROM quotes WHERE bookTitle IS NOT NULL ORDER BY bookTitle ASC")
    suspend fun getAllBookTitles(): List<String>

    /**
     * Get quote count by theme for statistics
     */
    @Query("SELECT theme, COUNT(*) as count FROM quotes WHERE theme IS NOT NULL GROUP BY theme ORDER BY count DESC")
    suspend fun getQuoteCountByTheme(): List<ThemeCount>
}

/**
 * Data class to represent theme statistics
 */
data class ThemeCount(
    val theme: String,
    val count: Int
)