package com.mothblank.lumorareader.di

import android.content.Context
import androidx.room.Room
import com.mothblank.lumorareader.data.db.AppDatabase
import com.mothblank.lumorareader.data.db.dao.QuoteDao
import com.mothblank.lumorareader.data.db.dao.RecentBookDao
import com.mothblank.lumorareader.data.db.migration.MIGRATION_1_2
import com.mothblank.lumorareader.data.db.migration.MIGRATION_1_3
import com.mothblank.lumorareader.data.db.migration.MIGRATION_2_3
import com.mothblank.lumorareader.data.db.migration.MIGRATION_3_4
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    @Singleton // DAOs should be singletons as they are tied to the singleton database.
    fun provideQuoteDao(database: AppDatabase): QuoteDao {
        // Hilt knows how to provide the 'database' parameter because of the function above.
        return database.quoteDao()
    }

    @Provides
    fun provideRecentBookDao(database: AppDatabase): RecentBookDao = database.recentBookDao()
}