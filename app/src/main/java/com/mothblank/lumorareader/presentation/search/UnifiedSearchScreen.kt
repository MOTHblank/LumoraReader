package com.mothblank.lumorareader.presentation.search

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mothblank.lumorareader.R

/** Unified local discovery. Exact source URIs are used for bookmark and quotation navigation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSearchScreen(
    navController: NavHostController,
    viewModel: UnifiedSearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val totals = results.activity
    val lastSevenMinutes = totals.recentMillis / 60_000L
    val allMinutes = totals.totalMillis / 60_000L

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.discovery_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.discovery_search)) },
                    placeholder = { Text(stringResource(R.string.discovery_search_hint)) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(stringResource(R.string.discovery_activity),
                            style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.discovery_recent_minutes, lastSevenMinutes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            stringResource(R.string.discovery_all_minutes, allMinutes, totals.sessions),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            stringResource(R.string.discovery_activity_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (results.isEmpty) {
                item {
                    Text(
                        stringResource(
                            if (query.isBlank()) R.string.discovery_empty_library
                            else R.string.discovery_no_results
                        ),
                        modifier = Modifier.padding(vertical = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (results.books.isNotEmpty()) {
                item {
                    SearchSectionHeader(stringResource(R.string.discovery_books, results.books.size))
                }
                items(results.books, key = { "book_${it.uri}" }) { book ->
                    SearchResultCard(
                        headline = book.title,
                        detail = listOfNotNull(book.author, book.collectionName).joinToString(" · ")
                            .ifBlank { stringResource(R.string.discovery_open_book) },
                        icon = { Icon(Icons.Default.Book, contentDescription = null) },
                        enabled = book.isAvailable,
                        onClick = { navController.navigate("reader/${Uri.encode(book.uri)}") }
                    )
                }
            }
            if (results.bookmarks.isNotEmpty()) {
                item {
                    SearchSectionHeader(
                        stringResource(R.string.discovery_bookmarks, results.bookmarks.size)
                    )
                }
                items(results.bookmarks, key = { "bookmark_${it.id}" }) { bookmark ->
                    SearchResultCard(
                        headline = bookmark.label,
                        detail = bookmark.note?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.discovery_saved_location),
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                        enabled = true,
                        onClick = {
                            navController.navigate(
                                "reader/${Uri.encode(bookmark.bookUri)}?bookmarkId=${bookmark.id}"
                            )
                        }
                    )
                }
            }
            if (results.quotes.isNotEmpty()) {
                item {
                    SearchSectionHeader(
                        stringResource(R.string.discovery_quotes, results.quotes.size)
                    )
                }
                items(results.quotes, key = { "quote_${it.id}" }) { quote ->
                    val source = quote.sourceUri
                    val linked = !source.isNullOrBlank() &&
                        (quote.pageNumber != null || quote.epubChunkIndex != null ||
                            quote.epubAnchor != null)
                    SearchResultCard(
                        headline = quote.text,
                        detail = listOfNotNull(quote.bookTitle, quote.note).joinToString(" · ")
                            .ifBlank { quote.author },
                        icon = { Icon(Icons.Default.FormatQuote, contentDescription = null) },
                        enabled = true,
                        onClick = {
                            if (linked && source != null) {
                                navController.navigate(
                                    "reader/${Uri.encode(source)}?quoteId=${quote.id}"
                                )
                            } else {
                                navController.navigate(
                                    "quotes?search=${Uri.encode(quote.text.take(80))}"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SearchResultCard(
    headline: String,
    detail: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    headline,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
