package com.mothblank.lumorareader.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mothblank.lumorareader.domain.model.RecentBook
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
/**
 * Manages the list of recently opened books using SharedPreferences.
 * This class is managed by Hilt as a singleton.
 */
@Singleton // 1. Tells Hilt to create only ONE instance of this class for the entire app.
class RecentBooksRepository @Inject constructor( // 2. @Inject tells Hilt that it can use this constructor to create the class.
    @ApplicationContext private val context: Context // 3. @ApplicationContext tells Hilt to automatically provide the app's Context here.
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    /**
     * Retrieves the list of recent books from SharedPreferences.
     */
    fun getRecentBooks(): List<RecentBook> {
        val json = prefs.getString(KEY_RECENT_BOOKS, null)
        return if (json != null) {
            val type = object : TypeToken<List<RecentBook>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
    /**
     * Adds a book to the top of the recent books list, ensuring no duplicates
     * and trimming the list to a maximum size.
     */
    fun addRecentBook(book: RecentBook) {
        val recentBooks = getRecentBooks().toMutableList()
        // Remove existing entry to avoid duplicates and move it to the top
        recentBooks.removeAll { it.uri == book.uri }
        recentBooks.add(0, book) // Add to the top of the list
        // Keep the list size limited
        val limitedList = if (recentBooks.size > MAX_RECENT_BOOKS) {
            recentBooks.subList(0, MAX_RECENT_BOOKS)
        } else {
            recentBooks
        }
        val json = gson.toJson(limitedList)
        prefs.edit().putString(KEY_RECENT_BOOKS, json).apply()
    }

    /**
     * Clears all recent books from the list.
     */
    fun clearRecentBooks() {
        prefs.edit().remove(KEY_RECENT_BOOKS).apply()
    }

    /**
     * Removes a specific book from the recent books list.
     */
    fun removeRecentBook(uri: String) {
        val recentBooks = getRecentBooks().toMutableList()
        recentBooks.removeAll { it.uri == uri }
        val json = gson.toJson(recentBooks)
        prefs.edit().putString(KEY_RECENT_BOOKS, json).apply()
    }

    companion object {
        private const val PREFS_NAME = "LumoraReaderPrefs"
        private const val KEY_RECENT_BOOKS = "recent_books"
        private const val MAX_RECENT_BOOKS = 10
    }
}