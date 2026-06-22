package com.mothblank.lumorareader.features.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.presentation.common.ThemeChip
import com.mothblank.lumorareader.presentation.quotes.QuoteViewModel
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuoteWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finishWithResult(Activity.RESULT_CANCELED)
            return
        }

        setContent {
            MaterialTheme {
                ConfigScreen(
                    onConfigComplete = { selectedQuote, selectedTheme ->
                        saveConfigurationAndFinish(selectedQuote, selectedTheme)
                    }
                )
            }
        }
    }

    private fun saveConfigurationAndFinish(selectedQuote: Quote?, selectedTheme: LumoraTheme) {
        val context: Context = this

        lifecycleScope.launch {
            try {
                // THIS IS THE FIX: Use GlanceAppWidgetManager to handle the ID conversion and update.
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)

                // 1. Update the state for our widget instance.
                updateAppWidgetState(context, QuoteWidgetStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[QuoteWidgetStateDefinition.Keys.quoteIdKey] = selectedQuote?.id ?: -1
                        this[QuoteWidgetStateDefinition.Keys.quoteTextKey] = selectedQuote?.text ?: "Random Quote"
                        this[QuoteWidgetStateDefinition.Keys.quoteAuthorKey] = selectedQuote?.author ?: "LumoraReader"
                        this[QuoteWidgetStateDefinition.Keys.themeNameKey] = selectedTheme.name
                    }
                }

                // 2. Tell the widget to update its UI with the new state.
                QuoteWidget().update(context, glanceId)

                // 3. Finish the activity successfully.
                finishWithResult(Activity.RESULT_OK)

            } catch (e: Exception) {
                finishWithResult(Activity.RESULT_CANCELED)
            }
        }
    }

    private fun finishWithResult(resultCode: Int) {
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(resultCode, resultValue)
        finish()
    }
}

@Composable
fun ConfigScreen(
    viewModel: QuoteViewModel = hiltViewModel(),
    onConfigComplete: (selectedQuote: Quote?, selectedTheme: LumoraTheme) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quotes = uiState.quotes
    var selectedIndex by remember { mutableStateOf(-1) }
    var selectedTheme by remember { mutableStateOf(LumoraTheme.DEFAULT) }

    Column(Modifier.fillMaxSize()) {
        Text(stringResource(id = R.string.select_a_quote), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { selectedIndex = -1 }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedIndex == -1, onClick = { selectedIndex = -1 })
                    Text(stringResource(id = R.string.random_quote), modifier = Modifier.padding(start = 8.dp))
                }
            }
            itemsIndexed(quotes) { index, quote ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { selectedIndex = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedIndex == index, onClick = { selectedIndex = index })
                    Text("\"${quote.text.take(40)}...\" - ${quote.author}", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        Text(stringResource(id = R.string.select_a_theme), style = MaterialTheme. typography.headlineSmall, modifier = Modifier.padding(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(LumoraTheme.entries) { theme ->
                ThemeChip(
                    theme = theme,
                    isSelected = theme == selectedTheme,
                    onClick = { selectedTheme = theme }
                )
            }
        }

        Button(
            onClick = {
                val selectedQuote = if (selectedIndex == -1) null else quotes.getOrNull(selectedIndex)
                onConfigComplete(selectedQuote, selectedTheme)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(id = R.string.confirm))
        }
    }
}