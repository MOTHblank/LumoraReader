package com.mothblank.lumorareader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.domain.model.Page
import com.mothblank.lumorareader.domain.usecase.BookReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Native PDF rendering at the requested on-screen resolution.
 *
 * PdfRenderer is not thread-safe: guard opening, rendering and closing pages
 * with the same lock. A tile is a normalized source rectangle of a PDF page,
 * rendered directly, rather than cropping a very large full-page bitmap.
 */
class PdfBookReader(
    private val context: Context,
    bookUri: String
) : BookReader {
    private val renderLock = Any()
    private val pfd: ParcelFileDescriptor
    private val renderer: PdfRenderer
    private var closed = false

    init {
        var openedDescriptor: ParcelFileDescriptor? = null
        try {
            val descriptor = context.contentResolver.openFileDescriptor(Uri.parse(bookUri), "r")
                ?: throw IOException("The PDF content provider returned no file descriptor")
            openedDescriptor = descriptor
            val openedRenderer = PdfRenderer(descriptor)
            pfd = descriptor
            renderer = openedRenderer
        } catch (error: Exception) {
            try {
                openedDescriptor?.close()
            } catch (closeError: Exception) {
                error.addSuppressed(closeError)
            }
            throw IOException(context.getString(R.string.failed_to_open_pdf_file, bookUri), error)
        }
    }

    override fun getPageCount(): Int = synchronized(renderLock) {
        if (closed) 0 else renderer.pageCount
    }

    // BookReader fallback. The reader UI uses renderPage with its actual width.
    override suspend fun getPage(pageIndex: Int): Page = renderPage(pageIndex, 1080)

    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Page.PdfPage =
        renderRegion(pageIndex, RectF(0f, 0f, 1f, 1f), targetWidthPx, MAX_PAGE_PIXELS)

    suspend fun renderTile(pageIndex: Int, region: RectF, targetWidthPx: Int): Page.PdfPage =
        renderRegion(pageIndex, region, targetWidthPx, MAX_TILE_PIXELS)

    private suspend fun renderRegion(
        pageIndex: Int,
        region: RectF,
        requestedWidthPx: Int,
        maxPixels: Int
    ): Page.PdfPage = withContext(Dispatchers.IO) {
        require(region.left >= 0f && region.top >= 0f &&
            region.right <= 1f && region.bottom <= 1f &&
            region.width() > 0f && region.height() > 0f &&
            region.left.isFinite() && region.top.isFinite() &&
            region.right.isFinite() && region.bottom.isFinite()
        ) { "Invalid normalized PDF region" }

        synchronized(renderLock) {
            check(!closed) { "PDF renderer already closed" }
            if (pageIndex !in 0 until renderer.pageCount) {
                throw IndexOutOfBoundsException(context.getString(R.string.invalid_page_index, pageIndex))
            }
            renderer.openPage(pageIndex).use { page ->
                val desiredWidth = requestedWidthPx.coerceIn(1, MAX_RENDER_SIDE).toFloat()
                val aspectRatio = page.height.toFloat() * region.height() /
                    (page.width.toFloat() * region.width())
                val desiredHeight = desiredWidth * aspectRatio
                // Budget each allocation before creating any bitmap, including tall pages.
                val pixelScale = minOf(
                    1f,
                    MAX_RENDER_SIDE / desiredWidth,
                    MAX_RENDER_SIDE / desiredHeight,
                    sqrt(maxPixels.toFloat() / (desiredWidth * desiredHeight))
                )
                val width = (desiredWidth * pixelScale).roundToInt().coerceIn(1, MAX_RENDER_SIDE)
                val height = (desiredHeight * pixelScale).roundToInt().coerceIn(1, MAX_RENDER_SIDE)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    // Map full-page PDF coordinates into this visible bitmap subsection.
                    val transform = Matrix().apply {
                        setScale(
                            width / (page.width.toFloat() * region.width()),
                            height / (page.height.toFloat() * region.height())
                        )
                        postTranslate(
                            -region.left * width / region.width(),
                            -region.top * height / region.height()
                        )
                    }
                    page.render(
                        bitmap,
                        Rect(0, 0, width, height),
                        transform,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )
                    Page.PdfPage(bitmap)
                } catch (error: Exception) {
                    bitmap.recycle()
                    throw error
                }
            }
        }
    }

    override fun getTableOfContents(): List<Pair<String, Int>> = emptyList()

    override fun close() {
        synchronized(renderLock) {
            if (closed) return
            closed = true
            try {
                renderer.close()
            } finally {
                pfd.close()
            }
        }
    }

    companion object {
        private const val MAX_RENDER_SIDE = 3072
        private const val MAX_PAGE_PIXELS = 3_000_000 // ~12 MiB ARGB_8888
        private const val MAX_TILE_PIXELS = 2_000_000 // ~8 MiB ARGB_8888
    }
}
