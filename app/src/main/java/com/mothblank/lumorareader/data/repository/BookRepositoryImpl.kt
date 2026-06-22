package com.mothblank.lumorareader.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.mothblank.lumorareader.data.EpubBookReader
import com.mothblank.lumorareader.data.PdfBookReader
import com.mothblank.lumorareader.domain.Book
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.domain.usecase.BookReader
import com.mothblank.lumorareader.domain.BookType
import com.mothblank.lumorareader.domain.repository.BookRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BookRepository {

    override suspend fun openBook(book: Book): BookReader {
        // This part is correct and doesn't need to change.
        return when (book.type) {
            BookType.PDF -> PdfBookReader(context, book.uri)
            BookType.EPUB -> EpubBookReader(context, book.uri)
        }
    }

    override suspend fun getBookDetails(uri: String): Book {
        // 1. Get the real file name from the Content URI.
        val fileName = getFileName(Uri.parse(uri)) ?: context.getString(R.string.unknown)
        Timber.d("Resolved file name from URI: $fileName")

        // 2. Determine the book type from the file name's extension.
        val bookType = when {
            fileName.endsWith(".pdf", ignoreCase = true) -> BookType.PDF
            fileName.endsWith(".epub", ignoreCase = true) -> BookType.EPUB
            else -> throw IllegalArgumentException(context.getString(R.string.unsupported_file_type, uri, fileName))
        }

        // 3. Create the book object with real metadata if it is an EPUB.
        var title = fileName.substringBeforeLast(".")
        var author: String? = null

        if (bookType == BookType.EPUB) {
            try {
                EpubBookReader(context, uri).use { reader ->
                    title = reader.title.ifEmpty { title }
                    author = reader.author
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse EPUB metadata dynamically for $uri")
            }
        }

        return Book(
            uri = uri,
            title = title,
            author = author,
            type = bookType
        )
    }

    /**
     * A helper function to query the ContentResolver for the display name
     * of a file from its Content URI.
     */
    private fun getFileName(uri: Uri): String? {
        // The ContentResolver can query the metadata of a Content URI.
        // The DISPLAY_NAME column holds the original file name.
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        // Fallback for URIs that don't support this query (less common)
        return uri.lastPathSegment
    }
}