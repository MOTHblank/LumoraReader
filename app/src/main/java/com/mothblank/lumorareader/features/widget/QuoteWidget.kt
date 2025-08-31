package com.mothblank.lumorareader.features.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mothblank.lumorareader.domain.usecase.GetRandomQuoteUseCase
import com.mothblank.lumorareader.features.widget.WidgetDI
import com.mothblank.lumorareader.R
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.state.currentState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class QuoteWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    private val coroutineScope = MainScope()

    // Ponto de entrada para obter dependências do Hilt
    private fun getGetRandomQuoteUseCase(context: Context): GetRandomQuoteUseCase {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDI::class.java
        )
        return hiltEntryPoint.getRandomQuoteUseCase()
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val text = prefs[stringPreferencesKey("quote_text")]
            val author = prefs[stringPreferencesKey("quote_author")]
            val isLoading = text == null || author == null

            if (isLoading) {
                coroutineScope.launch {
                    val getRandomQuote = getGetRandomQuoteUseCase(context)
                    val quote = getRandomQuote()
                    updateAppWidgetState(context, id) { state ->
                        state[stringPreferencesKey("quote_text")] = quote?.text ?: "Nenhuma citação encontrada."
                        state[stringPreferencesKey("quote_author")] = quote?.author ?: "Adicione uma citação no app."
                    }
                    this@QuoteWidget.update(context, id)
                }
            }

            WidgetContent(
                text = text ?: "Carregando...",
                author = author ?: "",
                isLoading = isLoading
            )
        }
    }
}

import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.clickable
import androidx.glance.modifier.modifier
import androidx.glance.unit.ColorProvider
import com.mothblank.lumorareader.MainActivity

@Composable
fun WidgetContent(text: String, author: String, isLoading: Boolean) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.color.widget_background)
            .clickable(onClick = actionStartActivity<MainActivity>())
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\"$text\"",
                style = TextStyle(fontSize = 18.sp, color = ColorProvider(R.color.widget_text_color))
            )
            if (author.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "- $author",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(R.color.widget_text_color))
                )
            }
        }
        if (isLoading) {
            CircularProgressIndicator(modifier = GlanceModifier.align(Alignment.Center))
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Refresh",
                modifier = GlanceModifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clickable(onClick = actionRunCallback<RefreshActionCallback>())
            )
        }
    }
} 