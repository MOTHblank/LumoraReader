package com.mothblank.lumorareader.domain.usecase

import com.mothblank.lumorareader.domain.model.Page

interface BookReader {
    fun getPageCount(): Int
    suspend fun getPage(pageIndex: Int): Page
    fun getTableOfContents(): List<Pair<String, Int>> // Title and page index
    fun close()
}