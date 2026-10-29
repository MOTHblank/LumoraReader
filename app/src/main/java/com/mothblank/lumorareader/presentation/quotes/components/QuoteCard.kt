package com.mothblank.lumorareader.presentation.quotes.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import com.mothblank.lumorareader.ui.theme.representativeColor
import com.mothblank.lumorareader.ui.theme.toColorScheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedQuoteCard(
    quote: Quote,
    onEditClick: (Quote) -> Unit,
    onAddToWidgetClick: (Quote) -> Unit,
    onDeleteClick: (Quote) -> Unit,
    onJumpClick: (Quote) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFullText by remember { mutableStateOf(false) }

    // Get theme-based colors
    val theme = quote.theme?.let { themeName ->
        LumoraTheme.entries.find { it.name == themeName }
    } ?: LumoraTheme.DEFAULT

    val themeColor = theme.representativeColor
    val themeColorScheme = theme.toColorScheme()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = themeColorScheme.primaryContainer.copy(alpha = 0.7f),
            contentColor = themeColorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Quote text
            Text(
                text = quote.text,
                style = MaterialTheme.typography.bodyLarge,
                color = themeColorScheme.onPrimaryContainer,
                maxLines = if (showFullText) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Show more/less button for long quotes
            if (quote.text.length > 200) {
                TextButton(
                    onClick = { showFullText = !showFullText },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = if (showFullText) "Show less" else "Show more",
                        style = MaterialTheme.typography.labelMedium,
                        color = themeColorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simplified book context information
            if (quote.bookTitle != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = themeColorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = quote.bookTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = themeColorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Page number if present
                quote.pageNumber?.let { pageNumber ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Page $pageNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!quote.note.isNullOrBlank()) {
                Text(
                    text = "Note: ${quote.note}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (quote.categories.isNotEmpty()) {
                Text(
                    text = quote.categories.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (quote.sourceUri != null &&
                (quote.pageNumber != null || quote.epubChunkIndex != null)
            ) {
                TextButton(onClick = { onJumpClick(quote) }) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Jump to passage")
                }
            }

            // Footer with action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author name
                Text(
                    text = "— ${quote.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = themeColorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Action buttons
                Row {
                    IconButton(
                        onClick = { onEditClick(quote) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = themeColorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(
                        onClick = { onAddToWidgetClick(quote) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Widget",
                            tint = themeColorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(
                        onClick = { onDeleteClick(quote) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = themeColorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}