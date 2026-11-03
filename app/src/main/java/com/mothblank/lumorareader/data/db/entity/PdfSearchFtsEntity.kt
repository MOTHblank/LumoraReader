package com.mothblank.lumorareader.data.db.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "pdf_search_fts")
data class PdfSearchFtsEntity(
    val bookUri: String,
    val pageIndex: Int,
    val content: String
)
