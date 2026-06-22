package com.mothblank.lumorareader.features.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.domain.repository.QuoteRepository
import com.mothblank.lumorareader.data.repository.QuoteRepositoryImpl
import com.mothblank.lumorareader.data.db.dao.QuoteDao
import com.mothblank.lumorareader.domain.usecase.GetRandomQuoteUseCase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.mothblank.lumorareader.R
import timber.log.Timber

class RefreshQuoteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Timber.d("RefreshQuoteAction triggered for glanceId: $glanceId")

        val hiltEntryPoint = EntryPointAccessors.fromApplication(context, WidgetDI::class.java)
        val getRandomQuote = hiltEntryPoint.getRandomQuoteUseCase()
        val quoteRepository = hiltEntryPoint.getQuoteRepository()

        // Get the state DataStore for this specific widget instance
        val dataStore = QuoteWidgetStateDefinition.getDataStore(context, glanceId.toString())

        // Read the saved quote ID from the DataStore
        val quoteId = dataStore.data.first()[QuoteWidgetStateDefinition.Keys.quoteIdKey] ?: -1

        val quoteToDisplay: Quote?

        try {
            if (quoteId != -1) {
                Timber.d("Refetching specific quote with ID: $quoteId")
                quoteToDisplay = quoteRepository.getQuoteById(quoteId)
            } else {
                Timber.d("Fetching a new random quote")
                quoteToDisplay = getRandomQuote()
            }
        } catch (e: Exception) {
            Timber.e(e, "Database access failed during widget refresh.")
            // Gracefully display alternative error layouts on the widget if database fails
            updateAppWidgetState(context, QuoteWidgetStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[QuoteWidgetStateDefinition.Keys.quoteIdKey] = -1
                    this[QuoteWidgetStateDefinition.Keys.quoteTextKey] = context.getString(R.string.widget_database_error)
                    this[QuoteWidgetStateDefinition.Keys.quoteAuthorKey] = context.getString(R.string.widget_database_error_sub)
                }
            }
            QuoteWidget().update(context, glanceId)
            return
        }

        updateAppWidgetState(context, QuoteWidgetStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[QuoteWidgetStateDefinition.Keys.quoteIdKey] = quoteToDisplay?.id ?: -1
                this[QuoteWidgetStateDefinition.Keys.quoteTextKey] = quoteToDisplay?.text ?: context.getString(R.string.no_quote_found)
                this[QuoteWidgetStateDefinition.Keys.quoteAuthorKey] = quoteToDisplay?.author ?: context.getString(R.string.lumora_reader_author_refresh)
            }
        }

        QuoteWidget().update(context, glanceId)
    }
}