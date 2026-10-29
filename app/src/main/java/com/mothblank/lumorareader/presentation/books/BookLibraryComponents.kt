package com.mothblank.lumorareader.presentation.books

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.domain.model.ReadingStatus
import com.mothblank.lumorareader.domain.model.RecentBook

internal fun statusTextResource(status: ReadingStatus): Int = when (status) {
    ReadingStatus.WANT_TO_READ -> R.string.library_status_want_to_read
    ReadingStatus.READING -> R.string.library_status_reading
    ReadingStatus.ON_HOLD -> R.string.library_status_on_hold
    ReadingStatus.FINISHED -> R.string.library_status_finished
}

@Composable
internal fun LibraryStats(books: List<RecentBook>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val metrics = listOf(
            R.string.library_stats_reading to books.count { it.readingStatus == ReadingStatus.READING },
            R.string.library_stats_finished to books.count { it.readingStatus == ReadingStatus.FINISHED },
            R.string.library_stats_favorites to books.count { it.isFavorite }
        )
        metrics.forEach { (label, value) ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun LibraryBookCard(
    book: RecentBook,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOrganize: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .width(78.dp)
                        .height(112.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    book.coverImageUri?.takeIf { it.isNotBlank() }?.let { cover ->
                        AsyncImage(
                            model = cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    book.author?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            stringResource(statusTextResource(book.readingStatus)),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    book.collectionName?.let {
                        Text(
                            stringResource(R.string.library_collection_label, it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!book.isAvailable) {
                        Text(
                            stringResource(R.string.library_unavailable),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(
                            if (book.isFavorite) R.string.library_favorite_remove
                            else R.string.library_favorite_add
                        ),
                        tint = if (book.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            book.progress?.let { progress ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.library_progress_percent, (progress.coerceIn(0f, 1f) * 100).toInt()),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            stringResource(R.string.library_last_opened, java.text.DateFormat.getDateInstance().format(java.util.Date(book.lastOpened.millis))),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Only abbreviate the actions when they would otherwise crowd
                // each other. Buttons, colors, shape and order are unchanged.
                val compact = maxWidth < 330.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpen,
                        enabled = book.isAvailable
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(
                            if (compact) R.string.library_read_short
                            else R.string.library_open_book
                        ))
                    }
                    TextButton(onClick = onOrganize) {
                        Icon(Icons.Default.Edit, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        if (!compact) {
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.library_edit))
                        } else {
                            Text(stringResource(R.string.library_edit_short))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.library_remove))
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditLibraryBookDialog(
    book: RecentBook,
    existingCollections: List<String>,
    onDismiss: () -> Unit,
    onSave: (ReadingStatus, String?) -> Unit
) {
    var status by remember(book.uri) { mutableStateOf(book.readingStatus) }
    var collection by remember(book.uri) { mutableStateOf(book.collectionName.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_edit_book)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(
                    stringResource(R.string.library_edit_details),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(stringResource(R.string.library_filter_status), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReadingStatus.entries) { item ->
                        FilterChip(
                            selected = status == item,
                            onClick = { status = item },
                            label = { Text(stringResource(statusTextResource(item))) }
                        )
                    }
                }
                OutlinedTextField(
                    value = collection,
                    onValueChange = { if (it.length <= 80) collection = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.library_collection_name)) },
                    singleLine = true
                )
                Text(
                    stringResource(R.string.library_create_collection),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (existingCollections.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(existingCollections.take(15)) { name ->
                            SuggestionChip(
                                onClick = { collection = name },
                                label = { Text(name, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(status, collection.trim().ifBlank { null }) }) {
                Text(stringResource(R.string.library_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_cancel))
            }
        }
    )
}
