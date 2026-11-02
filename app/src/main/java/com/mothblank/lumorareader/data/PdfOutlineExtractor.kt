package com.mothblank.lumorareader.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem

/**
 * Optional PDF outline extraction. Works with named/GoTo destinations and
 * nested outline nodes, but never changes rendering or page navigation.
 */
internal object PdfOutlineExtractor {
    private const val MAX_OUTLINE_NODES = 1024
    private const val MAX_DEPTH = 16
    private const val MAX_ENTRIES = 512

    fun extract(context: Context, uri: String, pageCount: Int): List<Pair<String, Int>> {
        if (pageCount <= 0) return emptyList()
        PDFBoxResourceLoader.init(context.applicationContext)
        val input = context.contentResolver.openInputStream(Uri.parse(uri)) ?: return emptyList()
        input.use { stream ->
            // A bounded in-memory scratch portion prevents large PDFs from
            // buffering their full content into the app heap.
            val memory = MemoryUsageSetting.setupMixed(8L * 1024 * 1024).apply {
                setTempDir(context.cacheDir)
            }
            PDDocument.load(stream, memory).use { document ->
                val root = document.documentCatalog.documentOutline ?: return emptyList()
                val entries = ArrayList<Pair<String, Int>>()
                val visited = HashSet<Any>()
                var inspected = 0

                fun visit(node: PDOutlineItem, depth: Int) {
                    if (inspected >= MAX_OUTLINE_NODES || depth > MAX_DEPTH ||
                        entries.size >= MAX_ENTRIES || !visited.add(node.cosObject)
                    ) return
                    inspected++
                    val title = node.title?.replace(Regex("\\s+"), " ")?.trim()?.take(160)
                    if (!title.isNullOrEmpty()) {
                        val destination = try {
                            node.findDestinationPage(document)
                        } catch (_: Exception) {
                            null // External/unsupported destinations have no local page.
                        }
                        val page = destination?.let { document.pages.indexOf(it) } ?: -1
                        if (page in 0 until pageCount) {
                            val indent = "  ".repeat(depth.coerceAtMost(6))
                            entries.add((indent + title) to page)
                        }
                    }
                    for (child in node.children()) {
                        if (inspected >= MAX_OUTLINE_NODES || entries.size >= MAX_ENTRIES) break
                        visit(child, depth + 1)
                    }
                }

                for (item in root.children()) {
                    if (inspected >= MAX_OUTLINE_NODES || entries.size >= MAX_ENTRIES) break
                    visit(item, 0)
                }
                return entries
            }
        }
    }
}
