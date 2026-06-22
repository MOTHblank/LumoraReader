package com.mothblank.lumorareader.ui.language

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "language_prefs"
private val Context.dataStore by preferencesDataStore(DATASTORE_NAME)

object LanguagePreferenceRepository {
    private val LANGUAGE_KEY = stringPreferencesKey("selected_language")

    /**
     * Flow that emits the currently selected [SupportedLanguage]. Defaults to English if nothing
     * has been selected yet.
     */
    fun languageFlow(context: Context): Flow<SupportedLanguage> =
        context.dataStore.data.map { prefs ->
            val tag = prefs[LANGUAGE_KEY] ?: SupportedLanguage.English.tag
            SupportedLanguage.fromTag(tag)
        }

    /**
     * Persists the selected [language] and immediately applies it using
     * [AppCompatDelegate.setApplicationLocales] so that Resources are reloaded.
     */
    suspend fun setLanguage(context: Context, language: SupportedLanguage) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language.tag
        }
        val appLocales = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(appLocales)
    }
}
