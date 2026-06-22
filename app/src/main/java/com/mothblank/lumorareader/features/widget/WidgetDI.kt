package com.mothblank.lumorareader.features.widget

import com.mothblank.lumorareader.domain.usecase.GetRandomQuoteUseCase
import com.mothblank.lumorareader.domain.repository.QuoteRepository
// import com.mothblank.lumorareader.ui.theme.ThemePreferenceRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Ponto de entrada para o Hilt injetar dependências em classes que não suportam injeção direta
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDI {
    fun getRandomQuoteUseCase(): GetRandomQuoteUseCase
    fun getQuoteRepository(): QuoteRepository

    // Unused and unified into UserPreferencesRepository:
    // fun getThemePreferenceRepository(): ThemePreferenceRepository
} 