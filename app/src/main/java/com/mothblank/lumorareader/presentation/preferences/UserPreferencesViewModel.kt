package com.mothblank.lumorareader.presentation.preferences

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.ui.language.SupportedLanguage
import com.mothblank.lumorareader.ui.preferences.UserPreferencesRepository
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPreferencesViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // --- Theme State ---
    val theme: StateFlow<LumoraTheme> = userPreferencesRepository.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LumoraTheme.DEFAULT
        )

    val fontSizeScale: StateFlow<Float> = userPreferencesRepository.fontSizeScale
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 1.0f
        )

    val fontFamily: StateFlow<String> = userPreferencesRepository.fontFamily
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "Georgia"
        )

    // --- Language State ---
    val language: StateFlow<SupportedLanguage> = userPreferencesRepository.language
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SupportedLanguage.English
        )

    // --- Theme Actions ---
    fun setTheme(theme: LumoraTheme) {
        viewModelScope.launch {
            userPreferencesRepository.setTheme(theme)
        }
    }

    fun setFontSizeScale(scale: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setFontSizeScale(scale)
        }
    }

    fun setFontFamily(fontFamily: String) {
        viewModelScope.launch {
            userPreferencesRepository.setFontFamily(fontFamily)
        }
    }

    // --- Language Actions ---
    fun setLanguage(language: SupportedLanguage) {
        viewModelScope.launch {
            userPreferencesRepository.setLanguage(language)
        }
    }
}