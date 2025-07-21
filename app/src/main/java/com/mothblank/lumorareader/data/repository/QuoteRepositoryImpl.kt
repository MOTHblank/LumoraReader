package com.mothblank.lumorareader.data.repository

import com.mothblank.lumorareader.data.local.database.QuoteDao
import com.mothblank.lumorareader.data.model.toDomain
import com.mothblank.lumorareader.data.model.toEntity
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val dao: QuoteDao
) : QuoteRepository {

    override fun getAllQuotes(): Flow<List<Quote>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRandomQuote(): Quote? {
        return dao.getRandom()?.toDomain()
    }

    override suspend fun saveQuote(quote: Quote) {
        // No futuro, a detecção de tema ocorreria aqui antes de salvar
        dao.insert(quote.toEntity())
    }
} 