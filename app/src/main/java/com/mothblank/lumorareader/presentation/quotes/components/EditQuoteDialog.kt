package com.mothblank.lumorareader.presentation.quotes.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.R

@Composable
fun EditQuoteDialog(
    quote: Quote,
    onDismiss: () -> Unit,
    onConfirm: (id: Int, text: String, author: String) -> Unit
) {
    var editingText by remember { mutableStateOf(TextFieldValue(quote.text)) }
    var editingAuthor by remember { mutableStateOf(TextFieldValue(quote.author)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.edit_quote)) },
        text = {
            Column {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    label = { Text(stringResource(id = R.string.quote_text)) },
                    placeholder = { Text(stringResource(id = R.string.quote_text_placeholder)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editingAuthor,
                    onValueChange = { editingAuthor = it },
                    label = { Text(stringResource(id = R.string.author)) },
                    placeholder = { Text(stringResource(id = R.string.author_placeholder)) }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(quote.id, editingText.text, editingAuthor.text)
                    onDismiss()
                },
                enabled = editingText.text.isNotBlank() && editingAuthor.text.isNotBlank()
            ) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}