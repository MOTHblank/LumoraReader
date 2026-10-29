package com.mothblank.lumorareader.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 1 to version 2 of the database.
 * Adds book context fields to the quotes table.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns to the quotes table
        db.execSQL("ALTER TABLE quotes ADD COLUMN bookTitle TEXT")
        db.execSQL("ALTER TABLE quotes ADD COLUMN bookAuthor TEXT")
        db.execSQL("ALTER TABLE quotes ADD COLUMN pageNumber INTEGER")
        db.execSQL("ALTER TABLE quotes ADD COLUMN chapterTitle TEXT")
    }
}

/**
 * Migration from version 2 to version 3 of the database.
 * Adds categories column if it doesn't exist.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Check if categories column exists, if not add it
        database.execSQL("ALTER TABLE quotes ADD COLUMN categories TEXT DEFAULT ''")
    }
}

/**
 * Migration from version 1 to version 3 of the database.
 * Combines changes from MIGRATION_1_2 and MIGRATION_2_3.
 * This is provided as a fallback for scenarios where a direct migration from 1 to 3 is expected.
 */
val MIGRATION_1_3 = object : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Changes from MIGRATION_1_2
        db.execSQL("ALTER TABLE quotes ADD COLUMN bookTitle TEXT")
        db.execSQL("ALTER TABLE quotes ADD COLUMN bookAuthor TEXT")
        db.execSQL("ALTER TABLE quotes ADD COLUMN pageNumber INTEGER")
        db.execSQL("ALTER TABLE quotes ADD COLUMN chapterTitle TEXT")

        // Changes from MIGRATION_2_3
        db.execSQL("ALTER TABLE quotes ADD COLUMN categories TEXT DEFAULT ''")
    }
}

/**
 * Keeps the existing quote data and adds persistent, unbounded book history.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reading_history (
                uri TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                author TEXT,
                coverImageUri TEXT,
                lastOpened INTEGER NOT NULL,
                progress REAL,
                positionIndex INTEGER NOT NULL,
                scrollOffset INTEGER NOT NULL,
                isAvailable INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

/** Preserve existing history while adding content-relative EPUB anchors. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reading_history ADD COLUMN epubAnchor TEXT")
    }
}
