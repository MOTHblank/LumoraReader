package com.mothblank.lumorareader.core.di

import android.content.Context
import androidx.room.Room
import com.mothblank.lumorareader.data.local.database.AppDatabase
import com.mothblank.lumorareader.data.repository.QuoteRepositoryImpl
import com.mothblank.lumorareader.domain.repository.QuoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideQuoteRepository(db: AppDatabase): QuoteRepository {
        return QuoteRepositoryImpl(db.quoteDao())
    }
} 