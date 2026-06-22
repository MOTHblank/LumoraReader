package com.mothblank.lumorareader.presentation.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.ui.language.SupportedLanguage
import com.mothblank.lumorareader.ui.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val currentLanguage: StateFlow<SupportedLanguage> =
        userPreferencesRepository.language
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SupportedLanguage.English
            )

    fun onLanguageSelected(language: SupportedLanguage) {
        viewModelScope.launch {
            userPreferencesRepository.setLanguage(language)
            // Apply the language change immediately
            val appLocales = LocaleListCompat.forLanguageTags(language.tag)
            AppCompatDelegate.setApplicationLocales(appLocales)
        }
    }
}