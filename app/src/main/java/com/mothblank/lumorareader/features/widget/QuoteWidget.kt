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

class QuoteWidget : GlanceAppWidget() {

    // Ponto de entrada para obter dependências do Hilt
    private fun getGetRandomQuoteUseCase(context: Context): GetRandomQuoteUseCase {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDI::class.java
        )
        return hiltEntryPoint.getRandomQuoteUseCase()
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val getRandomQuote = getGetRandomQuoteUseCase(context)
        val quote = getRandomQuote()

        provideContent {
            // If you have a custom GlanceTheme, use it here. Otherwise, remove or replace.
            WidgetContent(
                text = quote?.text ?: "Nenhuma citação encontrada.",
                author = quote?.author ?: "Adicione uma citação no app."
            )
        }
    }
}

@Composable
fun WidgetContent(text: String, author: String) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.color.widget_background) // Defina a cor em res/values/colors.xml
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\"$text\"",
            style = TextStyle(fontSize = 18.sp, color = ColorProvider(R.color.widget_text_color))
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "- $author",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(R.color.widget_text_color))
        )
    }
} 