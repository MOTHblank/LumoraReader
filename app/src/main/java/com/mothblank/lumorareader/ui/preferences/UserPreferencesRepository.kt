package com.mothblank.lumorareader.ui.preferences

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mothblank.lumorareader.ui.language.SupportedLanguage
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme_preference")
        val ORIENTATION = intPreferencesKey("orientation")
        val FONT_SIZE_SCALE = floatPreferencesKey("font_size_scale")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val LANGUAGE = stringPreferencesKey("selected_language")
    }

    // --- Theme ---
    val theme: Flow<LumoraTheme> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            try {
                prefs[PreferencesKeys.THEME]?.let { LumoraTheme.valueOf(it) } ?: LumoraTheme.DEFAULT
            } catch (e: IllegalArgumentException) {
                LumoraTheme.DEFAULT
            }
        }
        .distinctUntilChanged()

    suspend fun setTheme(theme: LumoraTheme) {
        context.dataStore.edit { it[PreferencesKeys.THEME] = theme.name }
    }

    // --- Font Size Scale ---
    val fontSizeScale: Flow<Float> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.FONT_SIZE_SCALE] ?: 1.0f
        }

    suspend fun setFontSizeScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE_SCALE] = scale
        }
    }

    // --- Orientation ---
    val orientation: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.ORIENTATION] ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

    suspend fun setOrientation(orientation: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ORIENTATION] = orientation
        }
    }

    // --- Font Family ---
    val fontFamily: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.FONT_FAMILY] ?: "Georgia"
        }

    suspend fun setFontFamily(fontFamily: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_FAMILY] = fontFamily
        }
    }

    // --- Language ---
    val language: Flow<SupportedLanguage> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val tag = prefs[PreferencesKeys.LANGUAGE] ?: SupportedLanguage.English.tag
            SupportedLanguage.fromTag(tag)
        }
        .distinctUntilChanged()

    suspend fun setLanguage(language: SupportedLanguage) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LANGUAGE] = language.tag
        }
    }
}