package com.mothblank.lumorareader.data.model

import androidx.room.TypeConverter

/**
 * Room TypeConverter for List<String>.
 * Converts between List<String> and String for database storage.
 */
class StringListConverter {
    @TypeConverter
    fun fromList(list: List<String>?): String = list?.joinToString(",") ?: ""
    
    @TypeConverter
    fun toList(data: String?): List<String> = data?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
} 