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
import com.mothblank.lumorareader.data.model.QuoteEntity
import com.mothblank.lumorareader.data.model.StringListConverter

@Database(
    entities = [
        QuoteEntity::class,
    ],
    version = 3,  // Update version number
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class, StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)  // Add migrations here
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 