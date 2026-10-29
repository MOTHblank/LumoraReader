package com.mothblank.lumorareader.presentation.quotes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.ui.theme.LumoraTheme

@Composable
fun EditQuoteDialog(
    quote: Quote,
    onDismiss: () -> Unit,
    onConfirm: (Quote) -> Unit
) {
    var text by remember(quote.id) { mutableStateOf(quote.text) }
    var author by remember(quote.id) { mutableStateOf(quote.author) }
    var note by remember(quote.id) { mutableStateOf(quote.note.orEmpty()) }
    var tags by remember(quote.id) { mutableStateOf(quote.categories.joinToString(", ")) }
    var theme by remember(quote.id) { mutableStateOf(quote.theme) }
    var themesOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (quote.id == 0) "Add quotation" else "Edit quotation") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 530.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 2000) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quotation") },
                    supportingText = { Text("${text.length} / 2000") }
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { if (it.length <= 100) author = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Author") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 4000) note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Personal note (optional)") },
                    minLines = 2,
                    supportingText = { Text("${note.length} / 4000") }
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { if (it.length <= 500) tags = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Categories (comma-separated)") },
                    supportingText = { Text("Optional tags for organizing quotations") }
                )
                androidx.compose.foundation.layout.Box {
                    OutlinedButton(onClick = { themesOpen = true }) {
                        Text("Theme: ${theme ?: "Automatic (suggested)"}")
                    }
                    DropdownMenu(expanded = themesOpen, onDismissRequest = { themesOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Automatic (suggested)") },
                            onClick = { theme = null; themesOpen = false }
                        )
                        LumoraTheme.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name.replace('_', ' ')) },
                                onClick = { theme = option.name; themesOpen = false }
                            )
                        }
                    }
                }
                if (quote.sourceUri != null) {
                    Text(
                        text = "Source: ${quote.bookTitle ?: "Book"}" +
                            (quote.pageNumber?.let { " · Page $it" }
                                ?: quote.chapterTitle?.let { " · $it" }.orEmpty())
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        quote.copy(
                            text = text.trim(),
                            author = author.trim(),
                            note = note.trim().ifEmpty { null },
                            categories = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(20),
                            theme = theme
                        )
                    )
                    onDismiss()
                },
                enabled = text.isNotBlank() && author.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
