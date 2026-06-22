package com.mothblank.lumorareader.di

import android.content.Context
import androidx.room.Room
import com.mothblank.lumorareader.data.db.AppDatabase
import com.mothblank.lumorareader.data.db.dao.QuoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton // Ensures there is only ONE instance of the database in the whole app.
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lumora-db"
        )
            // .addMigrations(...) // Add your migrations here when you have them.
            .build()
    }

    @Provides
    @Singleton // DAOs should be singletons as they are tied to the singleton database.
    fun provideQuoteDao(database: AppDatabase): QuoteDao {
        // Hilt knows how to provide the 'database' parameter because of the function above.
        return database.quoteDao()
    }
}