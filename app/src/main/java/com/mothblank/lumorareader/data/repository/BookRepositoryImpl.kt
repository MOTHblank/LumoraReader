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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BookRepository {

    override suspend fun openBook(book: Book): BookReader = withContext(Dispatchers.IO) {
        when (book.type) {
            BookType.PDF -> PdfBookReader(context, book.uri)
            BookType.EPUB -> EpubBookReader(context, book.uri)
        }
    }

    override suspend fun getBookDetails(uri: String): Book = withContext(Dispatchers.IO) {
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
                val metadata = parseEpubMetadataFromUri(Uri.parse(uri))
                metadata.first?.let { title = it }
                author = metadata.second
            } catch (e: Exception) {
                // Security check: Use filename instead of the full URI path to prevent leaking user storage structure/names in logs
                Timber.e(e, "Failed to parse EPUB metadata dynamically from zip stream for $fileName")
            }
        }

        Book(
            uri = uri,
            title = title,
            author = author,
            type = bookType
        )
    }

    /**
     * Helper to read EPUB metadata directly from the zip stream without extracting any files to disk.
     */
    private fun parseEpubMetadataFromUri(uri: Uri): Pair<String?, String?> {
        var opfPath: String? = null
        var title: String? = null
        var author: String? = null

        // Pass 1: Find container.xml in ZIP to get OPF path
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipInputStream ->
                    var entry = zipInputStream.nextEntry
                    while (entry != null) {
                        if (entry.name == "META-INF/container.xml") {
                            opfPath = parseContainerFromStream(zipInputStream)
                            break
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error finding container.xml in ZIP stream")
        }

        // Pass 2: Find the OPF file in ZIP to parse Title and Creator
        if (opfPath != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipInputStream ->
                        var entry = zipInputStream.nextEntry
                        while (entry != null) {
                            if (entry.name == opfPath) {
                                val meta = parseMetadataFromOpfStream(zipInputStream)
                                title = meta.first
                                author = meta.second
                                break
                            }
                            zipInputStream.closeEntry()
                            entry = zipInputStream.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing OPF metadata from ZIP stream")
            }
        }

        return Pair(title, author)
    }

    private fun parseContainerFromStream(inputStream: java.io.InputStream): String? {
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(NonClosingInputStream(inputStream), null)
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                    val fullPath = parser.getAttributeValue(null, "full-path")
                    val mediaType = parser.getAttributeValue(null, "media-type")
                    if (fullPath != null && (mediaType == "application/oebps-package+xml" || fullPath.endsWith(".opf", true))) {
                        return fullPath
                    }
                }
                parser.next()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse container XML from stream")
        }
        return null
    }

    private fun parseMetadataFromOpfStream(inputStream: java.io.InputStream): Pair<String?, String?> {
        var title: String? = null
        var author: String? = null
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(NonClosingInputStream(inputStream), null)
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    val localName = parser.name.substringAfter(':').lowercase()
                    when (localName) {
                        "title" -> {
                            val text = parser.nextText()?.trim()
                            if (!text.isNullOrEmpty()) {
                                title = text
                            }
                        }
                        "creator" -> {
                            val text = parser.nextText()?.trim()
                            if (!text.isNullOrEmpty()) {
                                author = text
                            }
                        }
                    }
                }
                parser.next()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse OPF XML from stream")
        }
        return Pair(title, author)
    }

    private class NonClosingInputStream(private val delegate: java.io.InputStream) : java.io.InputStream() {
        override fun read(): Int = delegate.read()
        override fun read(b: ByteArray): Int = delegate.read(b)
        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
        override fun close() { /* Do nothing to prevent closing ZipInputStream */ }
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