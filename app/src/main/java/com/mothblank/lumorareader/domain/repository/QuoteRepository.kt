package com.mothblank.lumorareader.domain.repository

import com.mothblank.lumorareader.domain.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun getAllQuotes(): Flow<List<Quote>>
    suspend fun getRandomQuote(): Quote?
    suspend fun saveQuote(quote: Quote)
} 