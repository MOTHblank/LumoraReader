@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.mothblank.lumorareader.data.db.converter

import androidx.room.TypeConverter
import kotlin.time.Instant

/**
 * Room TypeConverter for kotlinx.datetime.Instant.
 * Converts between Instant and Long for database storage.
 */

class DateTimeConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    fun toTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }
} 