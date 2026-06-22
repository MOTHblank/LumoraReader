package com.mothblank.lumorareader.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.ui.preferences.UserPreferencesRepository
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

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
}