package com.mothblank.lumorareader.di

import android.content.Context
import com.mothblank.lumorareader.data.RecentBooksRepository
import com.mothblank.lumorareader.data.repository.BookRepositoryImpl
import com.mothblank.lumorareader.domain.repository.BookRepository
import com.mothblank.lumorareader.data.repository.QuoteRepositoryImpl
import com.mothblank.lumorareader.domain.repository.QuoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBookRepository(
        bookRepositoryImpl: BookRepositoryImpl
    ): BookRepository

    // ADD THIS FUNCTION
    @Binds
    @Singleton
    abstract fun bindQuoteRepository(
        quoteRepositoryImpl: QuoteRepositoryImpl
    ): QuoteRepository
}

