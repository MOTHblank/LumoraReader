package com.mothblank.lumorareader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mothblank.lumorareader.data.db.converter.DateTimeConverter
import com.mothblank.lumorareader.data.db.dao.QuoteDao
import com.mothblank.lumorareader.data.db.migration.MIGRATION_1_2
import com.mothblank.lumorareader.data.db.migration.MIGRATION_1_3
import com.mothblank.lumorareader.data.db.migration.MIGRATION_2_3
import com.mothblank.lumorareader.data.db.migration.MIGRATION_3_4
import com.mothblank.lumorareader.data.db.migration.MIGRATION_4_5
import com.mothblank.lumorareader.data.db.migration.MIGRATION_5_6
import com.mothblank.lumorareader.data.db.migration.MIGRATION_6_7
import com.mothblank.lumorareader.data.db.migration.MIGRATION_7_8
import com.mothblank.lumorareader.data.db.migration.MIGRATION_8_9
import com.mothblank.lumorareader.data.db.migration.MIGRATION_9_10
import com.mothblank.lumorareader.data.db.migration.MIGRATION_10_11
import com.mothblank.lumorareader.data.db.migration.MIGRATION_11_12
import com.mothblank.lumorareader.data.db.dao.EpubSearchDao
import com.mothblank.lumorareader.data.db.dao.PdfSearchDao
import com.mothblank.lumorareader.data.db.entity.PdfSearchFtsEntity
import com.mothblank.lumorareader.data.db.entity.PdfIndexStatusEntity
import com.mothblank.lumorareader.data.db.entity.EpubSearchFtsEntity
import com.mothblank.lumorareader.data.db.entity.EpubIndexStatusEntity
import com.mothblank.lumorareader.data.db.dao.RecentBookDao
import com.mothblank.lumorareader.data.db.entity.RecentBookEntity
import com.mothblank.lumorareader.data.db.entity.ReadingBookmarkEntity
import com.mothblank.lumorareader.data.db.dao.ReadingBookmarkDao
import com.mothblank.lumorareader.data.db.dao.ReadingSessionDao
import com.mothblank.lumorareader.data.db.entity.ReadingSessionEntity
import com.mothblank.lumorareader.data.model.QuoteEntity
import com.mothblank.lumorareader.data.model.StringListConverter

@Database(
    entities = [
        QuoteEntity::class,
        RecentBookEntity::class,
        ReadingBookmarkEntity::class,
        ReadingSessionEntity::class,
        EpubSearchFtsEntity::class,
        EpubIndexStatusEntity::class,
        PdfSearchFtsEntity::class,
        PdfIndexStatusEntity::class,
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class, StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun epubSearchDao(): EpubSearchDao
    abstract fun pdfSearchDao(): PdfSearchDao
    abstract fun quoteDao(): QuoteDao
    abstract fun recentBookDao(): RecentBookDao
    abstract fun readingBookmarkDao(): ReadingBookmarkDao
    abstract fun readingSessionDao(): ReadingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lumora_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 