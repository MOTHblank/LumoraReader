package com.mothblank.lumorareader.data.db.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/** Searchable content with stable source spine positions (FTS4, fully offline). */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "epub_search_fts")
data class EpubSearchFtsEntity(
    val bookUri: String,
    val spineIndex: Int,
    val title: String,
    val content: String
)
