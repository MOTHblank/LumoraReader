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

/** Quote source locations and personal notes; all old quote rows remain intact. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE quotes ADD COLUMN sourceUri TEXT")
        db.execSQL("ALTER TABLE quotes ADD COLUMN epubChunkIndex INTEGER")
        db.execSQL("ALTER TABLE quotes ADD COLUMN epubAnchor TEXT")
        db.execSQL("ALTER TABLE quotes ADD COLUMN note TEXT")
    }
}

/** Add bookmarks without altering or resetting any existing quote or reading position. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reading_bookmarks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bookUri TEXT NOT NULL,
                label TEXT NOT NULL,
                note TEXT,
                epubChunkIndex INTEGER,
                epubAnchor TEXT,
                pdfPageIndex INTEGER,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_bookmarks_bookUri ON reading_bookmarks (bookUri)")
    }
}

/**
 * Preserve saved EPUB anchors, PDF locations and existing books while adding
 * explicit library organization fields. Opened books default to READING.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reading_history ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE reading_history ADD COLUMN readingStatus TEXT NOT NULL DEFAULT 'READING'")
        db.execSQL("ALTER TABLE reading_history ADD COLUMN collectionName TEXT")
    }
}

/** Add foreground-session insights without altering books, quotes or bookmarks. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reading_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bookUri TEXT NOT NULL,
                startedAt INTEGER NOT NULL,
                durationMillis INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_sessions_bookUri ON reading_sessions(bookUri)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_sessions_startedAt ON reading_sessions(startedAt)")
    }
}
