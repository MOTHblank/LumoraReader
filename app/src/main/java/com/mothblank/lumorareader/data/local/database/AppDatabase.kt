package com.mothblank.lumorareader.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mothblank.lumorareader.data.model.QuoteEntity

@Database(entities = [QuoteEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
} 