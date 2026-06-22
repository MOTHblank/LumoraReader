package com.mothblank.lumorareader.domain.model

sealed interface Page {
    data class PdfPage(val image: android.graphics.Bitmap) : Page
    data class EpubPage(val bodyContent: String, val baseUrl: String) : Page
}