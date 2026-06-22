package com.mothblank.lumorareader.features.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.glance.state.GlanceStateDefinition
import java.io.File

object QuoteWidgetStateDefinition : GlanceStateDefinition<Preferences> {

    // Commented out to prevent single-instance overwrite of multiple homescreen widgets:
    // private val Context.dataStore by preferencesDataStore(
    //     name = "quoteWidget"
    // )

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(fileKey)
        }
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "$fileKey.preferences_pb")
    }

    object Keys {
        val quoteIdKey = intPreferencesKey("widget_quote_id")
        val quoteTextKey = stringPreferencesKey("widget_quote_text")
        val quoteAuthorKey = stringPreferencesKey("widget_quote_author")
        val themeNameKey = stringPreferencesKey("widget_theme_name")
        val fontSizeKey = intPreferencesKey("widget_font_size")
        val fontFamilyKey = stringPreferencesKey("widget_font_family")
        val transparencyKey = floatPreferencesKey("widget_transparency")
    }
}