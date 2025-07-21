package com.mothblank.lumorareader.domain.model

// Represents a quote with optional theme

data class Quote(
    val id: Int = 0,
    val text: String,
    val author: String,
    val theme: String? = null
) 