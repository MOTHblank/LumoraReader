package com.mothblank.lumorareader.data

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mothblank.lumorareader.domain.model.Page
import com.mothblank.lumorareader.domain.usecase.BookReader
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import com.mothblank.lumorareader.ui.theme.ThemePalettes
import com.mothblank.lumorareader.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

class EpubBookReader(
    private val context: Context,
    private val bookUri: String
) : BookReader {

    private val tempDir: File
    val contentBaseUrl: String
    private val spine: List<String>
    private val chapterCache = mutableMapOf<Int, String>()
    private val maxChunkSize = 75000
    private val chunkedSpine = mutableListOf<List<Int>>()
    private val bodyContentRegex = Regex("<body[^>]*>(.*?)</body>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    // Media handling
    private val mediaFiles = mutableMapOf<String, String>() // original path -> temp file path
    private val cssFiles = mutableListOf<String>()

    // Metadata & ToC
    var title: String = ""
        private set
    var author: String? = null
        private set
    private var navHref: String? = null
    private var ncxHref: String? = null
    private val tableOfContents = mutableListOf<Pair<String, Int>>()

    init {
        // Security hardening: Put temp HTML/EPUB unzipped files in a dedicated subdirectory
        // of cacheDir so we only share that specific subdirectory in FileProvider,
        // enforcing the principle of least privilege.
        val cacheSubdir = File(context.cacheDir, "epub_cache")
        tempDir = File(cacheSubdir, "epub_${bookUri.hashCode()}").apply { mkdirs() }
        unzipEpub()

        val filename = Uri.parse(bookUri).lastPathSegment ?: "Unknown Book"
        title = filename.substringBeforeLast(".")

        val opfPath = parseContainer()
        contentBaseUrl = "file://${tempDir.absolutePath}/"
        spine = parseOpf(opfPath)
        createChunks()

        // Parse Table of Contents
        val toc = mutableListOf<Pair<String, Int>>()
        navHref?.let { toc.addAll(parseNav(it)) }
        if (toc.isEmpty()) {
            ncxHref?.let { toc.addAll(parseNcx(it)) }
        }
        tableOfContents.addAll(toc)
    }

    // --- Public Interface Methods ---

    override fun getPageCount(): Int = chunkedSpine.size

    override suspend fun getPage(pageIndex: Int): Page = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= spine.size) throw IndexOutOfBoundsException(context.getString(R.string.invalid_chapter_index, pageIndex))
        val chapterPath = spine[pageIndex]
        val opfParent = File(parseContainer()).parent ?: ""
        val chapterFile = File(tempDir, File(opfParent, chapterPath).path)
        val htmlContent = chapterFile.readText(Charsets.UTF_8)
        Page.EpubPage(htmlContent, contentBaseUrl)
    }

    override fun getTableOfContents(): List<Pair<String, Int>> = tableOfContents

    override fun close() {
        tempDir.deleteRecursively()
        chapterCache.clear()
        mediaFiles.clear()
        cssFiles.clear()
    }

    // --- Chunking & Content Generation ---

    fun getChunkCount(): Int = chunkedSpine.size

    private fun createChunks() {
        chunkedSpine.clear()
        var currentChunk = mutableListOf<Int>()
        var currentSize = 0
        spine.forEachIndexed { index, chapterPath ->
            // Use the on-disk file size of the extracted chapter as a proxy for character size.
            // This prevents reading every chapter and executing path-resolving regexes at startup.
            val chapterFile = File(tempDir, chapterPath)
            val chapterSize = if (chapterFile.exists()) chapterFile.length().toInt() else 0
            if (currentSize + chapterSize > maxChunkSize && currentChunk.isNotEmpty()) {
                chunkedSpine.add(currentChunk.toList())
                currentChunk = mutableListOf()
                currentSize = 0
            }
            currentChunk.add(index)
            currentSize += chapterSize
        }
        if (currentChunk.isNotEmpty()) chunkedSpine.add(currentChunk.toList())
    }

    private fun getChapterContent(chapterIndex: Int): String {
        return chapterCache.getOrPut(chapterIndex) {
            val chapterPath = spine.getOrNull(chapterIndex) ?: return@getOrPut ""
            val chapterFile = File(tempDir, chapterPath)
            if (!chapterFile.exists()) return@getOrPut ""
            try {
                val fullContent = chapterFile.readText(Charsets.UTF_8)
                // Process the content to fix media paths
                val processedContent = processMediaPaths(fullContent, File(chapterPath).parent ?: "")
                val matchResult = bodyContentRegex.find(processedContent)
                matchResult?.groups?.get(1)?.value?.trim() ?: processedContent.trim()
            } catch (e: Exception) {
                Timber.e(e, context.getString(R.string.failed_to_read_chapter_content, chapterPath))
                ""
            }
        }
    }

    private fun processMediaPaths(htmlContent: String, chapterDir: String): String {
        var processedContent = htmlContent

        // Fix image paths
        val imgRegex = Regex("""<img[^>]+src\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        processedContent = imgRegex.replace(processedContent) { matchResult ->
            val originalSrc = matchResult.groupValues[1]
            val resolvedPath = resolveMediaPath(originalSrc, chapterDir)
            matchResult.value.replace(originalSrc, resolvedPath)
        }

        // Fix CSS link paths
        val cssRegex = Regex("""<link[^>]+href\s*=\s*["']([^"']+\.css)["'][^>]*>""", RegexOption.IGNORE_CASE)
        processedContent = cssRegex.replace(processedContent) { matchResult ->
            val originalHref = matchResult.groupValues[1]
            val resolvedPath = resolveMediaPath(originalHref, chapterDir)
            matchResult.value.replace(originalHref, resolvedPath)
        }

        // Fix other media references (audio, video, etc.)
        val mediaRegex = Regex("""(?:src|href)\s*=\s*["']([^"']+\.(png|jpg|jpeg|gif|svg|mp3|mp4|webm|ogg|ttf|otf|woff|woff2))["']""", RegexOption.IGNORE_CASE)
        processedContent = mediaRegex.replace(processedContent) { matchResult ->
            val originalPath = matchResult.groupValues[1]
            val resolvedPath = resolveMediaPath(originalPath, chapterDir)
            matchResult.value.replace(originalPath, resolvedPath)
        }

        return processedContent
    }

    private fun resolveMediaPath(originalPath: String, chapterDir: String): String {
        // Handle absolute paths that start with /
        val cleanPath = if (originalPath.startsWith("/")) originalPath.substring(1) else originalPath

        // Resolve relative to chapter directory
        val fullPath = if (chapterDir.isNotEmpty()) {
            File(chapterDir, cleanPath).path.replace('\\', '/')
        } else {
            cleanPath
        }

        // Check if file exists in temp directory
        val mediaFile = File(tempDir, fullPath)
        return if (mediaFile.exists()) {
            "file://${mediaFile.absolutePath}"
        } else {
            // Try to find the file by name only (fallback)
            val fileName = File(cleanPath).name
            val foundFile = findMediaFile(fileName)
            foundFile?.let { "file://${it.absolutePath}" } ?: originalPath
        }
    }

    private fun findMediaFile(fileName: String): File? {
        return tempDir.walkTopDown()
            .filter { it.isFile && it.name.equals(fileName, ignoreCase = true) }
            .firstOrNull()
    }

    suspend fun generateScrollableHtmlFile(
        chunkIndex: Int,
        fontSizeScale: Float,
        theme: LumoraTheme
    ): File = withContext(Dispatchers.IO) {
        if (chunkIndex !in chunkedSpine.indices) throw IndexOutOfBoundsException(context.getString(R.string.invalid_chunk_index, chunkIndex))

        val palette = ThemePalettes.getValue(theme)
        val backgroundColor = palette.primaryContainer
        val textColor = palette.onPrimaryContainer

        val chapterIndices = chunkedSpine[chunkIndex]
        val combinedBodyContent = chapterIndices.joinToString("\n<hr class=\"chapter-break\">\n") { getChapterContent(it) }
        val fontSizeEm = 1.0f * fontSizeScale

        val bgHex = backgroundColor.toHex()
        val textHex = textColor.toHex()

        // Include original CSS files
        val cssIncludes = cssFiles.joinToString("\n") { cssPath ->
            val cssFile = File(tempDir, cssPath)
            if (cssFile.exists()) {
                """<link rel="stylesheet" type="text/css" href="file://${cssFile.absolutePath}">"""
            } else ""
        }

        val scrollingCss = """
            <style>
                html { -webkit-text-size-adjust: 100%; }
                body {
                    margin: 0; padding: 20px; font-family: Georgia, serif;
                    line-height: 1.6; font-size: ${fontSizeEm}em;
                    max-width: 800px; margin: 0 auto;
                    text-align: justify;
                    overflow-x: hidden;
                    word-wrap: break-word;
                    background-color: $bgHex !important;
                    color: $textHex !important;
                }
                .chapter-break { border: none; border-top: 1px solid $textHex; opacity: 0.5; margin: 40px 0; }
                img { max-width: 100%; height: auto; display: block; margin: 1em 0; }
                a { color: $textHex; text-decoration: underline; }
                /* Override any existing colors to match theme */
                p, div, span, h1, h2, h3, h4, h5, h6 { color: $textHex !important; }
            </style>
        """.trimIndent()

        val finalHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
            <meta charset="utf-8">
            $cssIncludes
            $scrollingCss
        </head>
        <body>
            $combinedBodyContent
        </body>
        </html>
        """.trimIndent()

        val tempHtmlFile = File(tempDir, "scroll_view_chunk_${chunkIndex}_${theme.name}_${fontSizeScale}.html")
        tempHtmlFile.writeText(finalHtml, Charsets.UTF_8)
        return@withContext tempHtmlFile
    }

    private fun Color.toHex(): String {
        return String.format("#%06X", (0xFFFFFF and this.toArgb()))
    }

    // --- Robust Parsing Engine ---

    private fun unzipEpub() {
        context.contentResolver.openInputStream(Uri.parse(bookUri))?.use { inputStream ->
            ZipInputStream(inputStream).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    val file = File(tempDir, entry.name)
                    
                    // Security Check: Prevent Zip Slip path traversal vulnerability
                    val canonicalPath = file.canonicalPath
                    val tempDirCanonicalPath = tempDir.canonicalPath
                    if (!canonicalPath.startsWith(tempDirCanonicalPath + File.separator) && canonicalPath != tempDirCanonicalPath) {
                        throw SecurityException("Malicious ZIP entry with path traversal: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { it.write(zipInputStream.readBytes()) }

                        // Track media files
                        val extension = file.extension.lowercase()
                        when {
                            extension in setOf("png", "jpg", "jpeg", "gif", "svg", "webp") -> {
                                mediaFiles[entry.name] = file.absolutePath
                            }
                            extension == "css" -> {
                                cssFiles.add(entry.name)
                            }
                            extension in setOf("ttf", "otf", "woff", "woff2") -> {
                                mediaFiles[entry.name] = file.absolutePath
                            }
                            extension in setOf("mp3", "mp4", "webm", "ogg") -> {
                                mediaFiles[entry.name] = file.absolutePath
                            }
                        }
                    }
                    entry = zipInputStream.nextEntry
                }
            }
        } ?: throw IOException(context.getString(R.string.failed_to_open_input_stream, bookUri))

        Timber.d("Extracted ${mediaFiles.size} media files and ${cssFiles.size} CSS files")
    }

    private fun parseContainer(): String {
        val containerFile = File(tempDir, "META-INF/container.xml")
        if (!containerFile.exists()) throw IOException(context.getString(R.string.container_xml_not_found))
        try {
            FileInputStream(containerFile).use { fis ->
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(fis, null)
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
            }
        } catch (e: Exception) {
            throw IOException(context.getString(R.string.failed_to_parse_container_xml, e.message), e)
        }
        throw IOException(context.getString(R.string.no_valid_opf_file_found))
    }

    private fun parseOpf(opfPath: String): List<String> {
        val opfFile = File(tempDir, opfPath)
        if (!opfFile.exists()) throw IOException(context.getString(R.string.opf_file_not_found, opfPath))

        val manifest = mutableMapOf<String, ManifestItem>()
        val spineRefs = mutableListOf<SpineItem>()
        val allHtmlItems = mutableListOf<String>()
        val opfDir = File(opfPath).parent ?: ""
        var tocId: String? = null

        try {
            FileInputStream(opfFile).use { fis ->
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(fis, null)
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
                            "item" -> {
                                val id = parser.getAttributeValue(null, "id")
                                val href = parser.getAttributeValue(null, "href")
                                val mediaType = parser.getAttributeValue(null, "media-type")
                                val properties = parser.getAttributeValue(null, "properties")
                                if (id != null && href != null && mediaType != null) {
                                    val resolvedHref = resolveRelativePath(opfDir, href)
                                    val item = ManifestItem(id, resolvedHref, mediaType)
                                    manifest[id] = item
                                    if (isContentDocument(mediaType, href)) {
                                        allHtmlItems.add(resolvedHref)
                                    }
                                    if (properties != null && properties.split(Regex("\\s+")).contains("nav")) {
                                        navHref = resolvedHref
                                    }
                                    if (mediaType == "application/x-dtbncx+xml") {
                                        ncxHref = resolvedHref
                                    }
                                }
                            }
                            "spine" -> {
                                tocId = parser.getAttributeValue(null, "toc")
                            }
                            "itemref" -> {
                                val idref = parser.getAttributeValue(null, "idref")
                                if (idref != null && (parser.getAttributeValue(null, "linear") ?: "yes") == "yes") {
                                    spineRefs.add(SpineItem(idref))
                                }
                            }
                        }
                    }
                    parser.next()
                }
            }
        } catch (e: Exception) {
            throw IOException(context.getString(R.string.failed_to_parse_opf_file, e.message), e)
        }

        // Post-resolve the NCX file if referenced by toc ID in the spine element
        val manifestItemForToc = manifest[tocId ?: ""]
        if (manifestItemForToc != null) {
            ncxHref = manifestItemForToc.href
        }

        val readingOrder = buildReadingOrder(manifest, spineRefs, allHtmlItems)
        if (readingOrder.isEmpty()) throw IOException(context.getString(R.string.no_readable_content_found))
        Timber.d("Found ${readingOrder.size} content files in reading order")
        return readingOrder
    }

    private fun buildReadingOrder(manifest: Map<String, ManifestItem>, spineRefs: List<SpineItem>, allHtmlItems: List<String>): List<String> {
        val spineOrder = spineRefs.mapNotNull { manifest[it.idref]?.href?.takeIf { href -> fileExists(href) } }
        if (spineOrder.isNotEmpty()) {
            Timber.d(context.getString(R.string.using_spine_order, spineOrder.size))
            return spineOrder
        }
        val validHtmlItems = allHtmlItems.filter { fileExists(it) }.sorted()
        if (validHtmlItems.isNotEmpty()) {
            Timber.w(context.getString(R.string.spine_invalid_using_manifest, validHtmlItems.size))
            return validHtmlItems
        }
        val opfDir = (manifest.values.firstOrNull()?.href?.let { File(it).parent } ?: "")
        val scannedFiles = scanForContentFiles(opfDir)
        if (scannedFiles.isNotEmpty()) {
            Timber.w(context.getString(R.string.using_directory_scan_fallback, scannedFiles.size))
            return scannedFiles
        }
        return emptyList()
    }

    private fun isContentDocument(mediaType: String, href: String): Boolean =
        mediaType.lowercase() in setOf("application/xhtml+xml", "text/html") || href.substringAfterLast('.', "").lowercase() in setOf("html", "xhtml", "htm")

    private fun fileExists(relativePath: String): Boolean = File(tempDir, relativePath).exists()
    
    private fun resolveRelativePath(basePath: String, relativePath: String): String {
        val path = if (basePath.isEmpty()) relativePath else File(basePath, relativePath).path
        return path.replace('\\', '/').replace("//", "/")
    }

    private fun scanForContentFiles(opfDir: String): List<String> {
        val contentDir = if (opfDir.isEmpty()) tempDir else File(tempDir, opfDir)
        return contentDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in setOf("html", "xhtml", "htm") }
            .sortedBy { it.name }
            .map { it.relativeTo(tempDir).path.replace('\\', '/') }
            .toList()
    }

    private fun findSpineIndex(href: String): Int {
        val normalizedHref = href.replace('\\', '/').trim().lowercase()
        spine.forEachIndexed { index, spinePath ->
            val normalizedSpine = spinePath.replace('\\', '/').trim().lowercase()
            if (normalizedSpine == normalizedHref || normalizedSpine.endsWith("/$normalizedHref") || normalizedHref.endsWith("/$normalizedSpine")) {
                return index
            }
        }
        return -1
    }

    private fun parseNcx(ncxPath: String): List<Pair<String, Int>> {
        val ncxFile = File(tempDir, ncxPath)
        if (!ncxFile.exists()) return emptyList()

        val toc = mutableListOf<Pair<String, Int>>()
        val ncxDir = File(ncxPath).parent ?: ""

        try {
            FileInputStream(ncxFile).use { fis ->
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(fis, null)
                var lastText: String? = null

                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG) {
                        val localName = parser.name.substringAfter(':').lowercase()
                        when (localName) {
                            "text" -> {
                                lastText = parser.nextText()?.trim()
                            }
                            "content" -> {
                                val src = parser.getAttributeValue(null, "src")
                                if (lastText != null && src != null) {
                                    val resolvedSrc = resolveRelativePath(ncxDir, src)
                                    val cleanSrc = resolvedSrc.substringBefore('#').replace('\\', '/').replace("//", "/")
                                    val spineIndex = findSpineIndex(cleanSrc)
                                    if (spineIndex != -1) {
                                        toc.add(Pair(lastText, spineIndex))
                                    }
                                }
                            }
                        }
                    }
                    parser.next()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse NCX file: $ncxPath")
        }
        return toc
    }

    private fun parseNav(navPath: String): List<Pair<String, Int>> {
        val navFile = File(tempDir, navPath)
        if (!navFile.exists()) return emptyList()

        val toc = mutableListOf<Pair<String, Int>>()
        val navDir = File(navPath).parent ?: ""

        try {
            FileInputStream(navFile).use { fis ->
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(fis, null)

                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG) {
                        val localName = parser.name.substringAfter(':').lowercase()
                        if (localName == "a") {
                            val href = parser.getAttributeValue(null, "href")
                            val label = parser.nextText()?.trim()
                            if (href != null && !label.isNullOrEmpty()) {
                                val resolvedHref = resolveRelativePath(navDir, href)
                                val cleanHref = resolvedHref.substringBefore('#').replace('\\', '/').replace("//", "/")
                                val spineIndex = findSpineIndex(cleanHref)
                                if (spineIndex != -1) {
                                    toc.add(Pair(label, spineIndex))
                                }
                            }
                        }
                    }
                    parser.next()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Nav file: $navPath")
        }
        return toc
    }

    private data class ManifestItem(val id: String, val href: String, val mediaType: String)
    private data class SpineItem(val idref: String)
}