package com.mothblank.lumorareader.domain.usecase

import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.domain.repository.QuoteRepository
import javax.inject.Inject

class GetRandomQuoteUseCase @Inject constructor(
    private val repository: QuoteRepository
) {
    suspend operator fun invoke(): Quote? {
        return repository.getRandomQuote()
    }
} 