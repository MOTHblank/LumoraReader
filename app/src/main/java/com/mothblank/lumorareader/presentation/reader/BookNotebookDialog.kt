package com.mothblank.lumorareader.presentation.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.domain.model.ReadingBookmark
import com.mothblank.lumorareader.domain.model.Quote
import kotlin.math.roundToInt

/** An in-reader overview: progress, location bookmarks, annotations and source-linked quotes. */
@Composable
fun BookNotebookDialog(
    state: ReaderUiState,
    onDismiss: () -> Unit,
    onAddBookmark: (String, String?) -> Unit,
    onEditBookmark: (Long, String, String?) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onJumpBookmark: (ReadingBookmark) -> Unit,
    onJumpQuote: (Quote) -> Unit
) {
    var editing by remember { mutableStateOf<ReadingBookmark?>(null) }
    var addNew by remember { mutableStateOf(false) }
    val locationLabel = if (state.isPdf) {
        stringResource(R.string.reader_notebook_page, state.currentPdfPage + 1)
    } else {
        state.currentChapterTitle ?: stringResource(
            R.string.reader_notebook_section, state.currentChunkIndex + 1
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_notebook_title)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 510.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        state.bookTitle ?: stringResource(R.string.reader_notebook_untitled),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2
                    )
                    state.bookAuthor?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        stringResource(R.string.reader_notebook_progress, (state.readingProgress.coerceIn(0f, 1f) * 100).roundToInt()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { state.readingProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(locationLabel, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { addNew = true },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.reader_notebook_add)) }
                }
                item {
                    HorizontalDivider()
                    Text(
                        stringResource(R.string.reader_notebook_bookmarks, state.bookmarks.size),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                if (state.bookmarks.isEmpty()) {
                    item {
                        Text(stringResource(R.string.reader_notebook_no_bookmarks),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(state.bookmarks, key = { "bookmark_${it.id}" }) { bookmark ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(bookmark.label, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            bookmark.note?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3,
                                    overflow = TextOverflow.Ellipsis)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    onJumpBookmark(bookmark)
                                    onDismiss()
                                }) { Text(stringResource(R.string.reader_notebook_jump)) }
                                TextButton(onClick = { editing = bookmark }) {
                                    Text(stringResource(R.string.reader_notebook_edit))
                                }
                                TextButton(onClick = { onDeleteBookmark(bookmark.id) }) {
                                    Text(stringResource(R.string.reader_notebook_delete))
                                }
                            }
                        }
                    }
                }
                item {
                    HorizontalDivider()
                    Text(
                        stringResource(R.string.reader_notebook_quotes, state.bookQuotes.size),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                if (state.bookQuotes.isEmpty()) {
                    item { Text(stringResource(R.string.reader_notebook_no_quotes),
                        style = MaterialTheme.typography.bodySmall) }
                }
                items(state.bookQuotes.take(30), key = { "quote_${it.id}" }) { quote ->
                    Card {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(quote.text, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall)
                            quote.note?.takeIf { it.isNotBlank() }?.let {
                                Text(it, maxLines = 2, style = MaterialTheme.typography.labelSmall)
                            }
                            if (quote.epubChunkIndex != null || quote.pageNumber != null) {
                                TextButton(onClick = {
                                    onJumpQuote(quote)
                                    onDismiss()
                                }) { Text(stringResource(R.string.reader_notebook_jump)) }
                            }
                        }
                    }
                }
                if (state.bookQuotes.size > 30) {
                    item { Text(stringResource(R.string.reader_notebook_quotes_limit)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reader_notebook_close))
            }
        }
    )

    if (addNew || editing != null) {
        val selected = editing
        var label by remember(selected?.id, addNew) {
            mutableStateOf(selected?.label ?: locationLabel)
        }
        var note by remember(selected?.id, addNew) {
            mutableStateOf(selected?.note.orEmpty())
        }
        AlertDialog(
            onDismissRequest = { addNew = false; editing = null },
            title = { Text(stringResource(R.string.reader_notebook_annotation)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { if (it.length <= 120) label = it },
                        label = { Text(stringResource(R.string.reader_notebook_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 2000) note = it },
                        label = { Text(stringResource(R.string.reader_notebook_note)) },
                        minLines = 2, maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = label.isNotBlank(),
                    onClick = {
                        if (selected != null) {
                            onEditBookmark(selected.id, label, note)
                        } else onAddBookmark(label, note)
                        addNew = false
                        editing = null
                    }
                ) { Text(stringResource(R.string.reader_notebook_save)) }
            },
            dismissButton = {
                TextButton(onClick = { addNew = false; editing = null }) {
                    Text(stringResource(R.string.reader_notebook_cancel))
                }
            }
        )
    }
}
