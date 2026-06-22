package com.mothblank.lumorareader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.mothblank.lumorareader.domain.usecase.BookReader
import com.mothblank.lumorareader.domain.model.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mothblank.lumorareader.R
import java.io.IOException

/**
 * A BookReader implementation for handling PDF files.
 *
 * This class is responsible for opening a PDF from a given URI,
 * rendering its pages into Bitmaps, and managing the file resources.
 *
 * @param context The application context, needed to access the ContentResolver.
 * @param bookUri The string representation of the content URI for the PDF file.
 */
class PdfBookReader(
    private val context: Context,
    bookUri: String
) : BookReader {

    // The file descriptor for the PDF file.
    private val pfd: ParcelFileDescriptor

    // The core PDF rendering engine provided by Android.
    private val renderer: PdfRenderer

    init {
        try {
            // Open the file descriptor from the content URI. The "r" mode is for "read".
            pfd = context.contentResolver.openFileDescriptor(Uri.parse(bookUri), "r")!!
            // Initialize the PdfRenderer with the file descriptor.
            renderer = PdfRenderer(pfd)
        } catch (e: IOException) {
            // If the file cannot be opened, wrap the exception and rethrow.
            throw IOException(context.getString(R.string.failed_to_open_pdf_file, bookUri), e)
        }
    }

    /**
     * Returns the total number of pages in the PDF document.
     */
    override fun getPageCount(): Int = renderer.pageCount

    /**
     * Renders a single page of the PDF into a Bitmap.
     *
     * This is a suspend function that performs the rendering on an I/O-optimized thread
     * to avoid blocking the UI.
     *
     * @param pageIndex The zero-based index of the page to render.
     * @return A [Page.PdfPage] containing the rendered [Bitmap].
     * @throws IndexOutOfBoundsException if the pageIndex is invalid.
     */
    override suspend fun getPage(pageIndex: Int): Page = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
            throw IndexOutOfBoundsException(context.getString(R.string.invalid_page_index, pageIndex))
        }

        // Use the 'use' block to automatically close the PdfRenderer.Page object
        // after it's been used. This is crucial for memory management.
        renderer.openPage(pageIndex).use { page ->
            // Create a bitmap with the exact dimensions of the PDF page.
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)

            // Render the page content onto the bitmap.
            // The other null parameters mean we render the full page with no clipping.
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            // Return the result wrapped in our sealed interface.
            Page.PdfPage(bitmap)
        }
    }

    /**
     * Returns the table of contents.
     * Note: Standard PDFs do not have a simple, universally parsable ToC structure
     * like EPUBs do. For this implementation, we return an empty list.
     * Advanced libraries would be needed to parse PDF bookmarks.
     */
    override fun getTableOfContents(): List<Pair<String, Int>> {
        return emptyList()
    }

    /**
     * Closes the PdfRenderer and the file descriptor.
     * This MUST be called when the reader is no longer needed to release system resources
     * and prevent memory leaks. The ViewModel's `onCleared()` is the perfect place for this.
     */
    override fun close() {
        try {
            renderer.close()
            pfd.close()
        } catch (e: IOException) {
            // Log or handle the exception if closing fails.
            e.printStackTrace()
        }
    }
}