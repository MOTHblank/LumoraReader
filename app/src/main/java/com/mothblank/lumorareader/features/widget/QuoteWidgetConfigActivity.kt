package com.mothblank.lumorareader.features.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.mothblank.lumorareader.presentation.preferences.UserPreferencesViewModel
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
            val userPreferencesViewModel: UserPreferencesViewModel = hiltViewModel()
            val activeTheme by userPreferencesViewModel.theme.collectAsState()

            LumoraTheme(theme = activeTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConfigScreen(
                        onConfigComplete = { selectedQuote, selectedTheme, fontSize, fontFamily, transparency ->
                            saveConfigurationAndFinish(selectedQuote, selectedTheme, fontSize, fontFamily, transparency)
                        }
                    )
                }
            }
        }
    }

    private fun saveConfigurationAndFinish(
        selectedQuote: Quote?,
        selectedTheme: LumoraTheme,
        fontSize: Int,
        fontFamily: String,
        transparency: Float
    ) {
        val context: Context = this

        lifecycleScope.launch {
            try {
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)

                // 1. Update the state for our widget instance.
                updateAppWidgetState(context, QuoteWidgetStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[QuoteWidgetStateDefinition.Keys.quoteIdKey] = selectedQuote?.id ?: -1
                        this[QuoteWidgetStateDefinition.Keys.quoteTextKey] = selectedQuote?.text ?: "Random Quote"
                        this[QuoteWidgetStateDefinition.Keys.quoteAuthorKey] = selectedQuote?.author ?: "LumoraReader"
                        this[QuoteWidgetStateDefinition.Keys.themeNameKey] = selectedTheme.name
                        this[QuoteWidgetStateDefinition.Keys.fontSizeKey] = fontSize
                        this[QuoteWidgetStateDefinition.Keys.fontFamilyKey] = fontFamily
                        this[QuoteWidgetStateDefinition.Keys.transparencyKey] = transparency
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
    userPreferencesViewModel: UserPreferencesViewModel = hiltViewModel(),
    onConfigComplete: (selectedQuote: Quote?, selectedTheme: LumoraTheme, fontSize: Int, fontFamily: String, transparency: Float) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quotes = uiState.quotes
    val activeThemeState by userPreferencesViewModel.theme.collectAsState()
    var selectedIndex by remember { mutableStateOf(-1) }
    var selectedTheme by remember { mutableStateOf(LumoraTheme.DEFAULT) }
    
    LaunchedEffect(activeThemeState) {
        selectedTheme = activeThemeState
    }
    
    // Custom styling configurations
    var fontSize by remember { mutableStateOf(18) }
    var fontFamily by remember { mutableStateOf("SansSerif") }
    var transparency by remember { mutableStateOf(0.0f) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = stringResource(id = R.string.select_a_quote),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
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
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        
        Text(
            text = stringResource(id = R.string.select_a_theme),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
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

        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        
        Text(
            text = "Widget Styling",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        // Font Size
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Font Size", style = MaterialTheme.typography.bodyLarge)
            Text("${fontSize} sp", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = fontSize.toFloat(),
            onValueChange = { fontSize = it.toInt() },
            valueRange = 12f..24f,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Font Family
        Text(
            text = "Font Family",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("SansSerif", "Serif", "Monospace").forEach { family ->
                CustomStyleChip(
                    text = family,
                    isSelected = fontFamily == family,
                    onClick = { fontFamily = family }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Background Transparency
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Background Transparency", style = MaterialTheme.typography.bodyLarge)
            Text("${(transparency * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = transparency,
            onValueChange = { transparency = it },
            valueRange = 0f..1f,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val selectedQuote = if (selectedIndex == -1) null else quotes.getOrNull(selectedIndex)
                onConfigComplete(selectedQuote, selectedTheme, fontSize, fontFamily, transparency)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(id = R.string.confirm))
        }
    }
}

@Composable
fun CustomStyleChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}