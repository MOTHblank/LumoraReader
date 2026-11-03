package com.mothblank.lumorareader.data.db.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/** Search segments retain their original XHTML spine and UTF-16 text offset. */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "epub_search_fts")
data class EpubSearchFtsEntity(
    val bookUri: String,
    val spineIndex: Int,
    val segmentIndex: Int,
    val textOffset: Int,
    val title: String,
    val content: String
)
