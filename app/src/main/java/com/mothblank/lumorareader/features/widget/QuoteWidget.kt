package com.mothblank.lumorareader.features.widget

import android.content.ComponentName
import android.content.Context
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mothblank.lumorareader.MainActivity
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.ui.theme.LumoraTheme

class QuoteWidget : GlanceAppWidget() {

    override val stateDefinition = QuoteWidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val text = prefs[QuoteWidgetStateDefinition.Keys.quoteTextKey] ?: stringResource(id = R.string.configure_your_widget)
            val author = prefs[QuoteWidgetStateDefinition.Keys.quoteAuthorKey] ?: stringResource(id = R.string.lumora_reader_author)

            // Read the saved theme name from the widget's state, falling back to DEFAULT.
            val themeName = prefs[QuoteWidgetStateDefinition.Keys.themeNameKey] ?: LumoraTheme.DEFAULT.name
            val theme = try {
                LumoraTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                LumoraTheme.DEFAULT
            }

            // Get the corresponding color resource IDs for the selected theme.
            val (bgColorRes, textColorRes) = getWidgetThemeColors(theme)

            WidgetContent(
                text = text,
                author = author,
                backgroundColorRes = bgColorRes,
                textColorRes = textColorRes
            )
        }
    }
}

/**
 * A helper that maps a LumoraTheme enum to its corresponding color resource IDs
 * defined in res/values/colors.xml. This is the crucial link between your
 * app's theme system and the Glance widget framework.
 */
private fun getWidgetThemeColors(theme: LumoraTheme): Pair<Int, Int> {
    return when (theme) {
        LumoraTheme.GOTH -> R.color.theme_goth_primaryContainer to R.color.theme_goth_onPrimaryContainer
        LumoraTheme.COQUETTE -> R.color.theme_coquette_primaryContainer to R.color.theme_coquette_onPrimaryContainer
        LumoraTheme.PIRATE -> R.color.theme_pirate_primaryContainer to R.color.theme_pirate_onPrimaryContainer
        LumoraTheme.NATURE -> R.color.theme_nature_primaryContainer to R.color.theme_nature_onPrimaryContainer
        LumoraTheme.VAMPIRE -> R.color.theme_vampire_primaryContainer to R.color.theme_vampire_onPrimaryContainer
        LumoraTheme.ROMANCE -> R.color.theme_romance_primaryContainer to R.color.theme_romance_onPrimaryContainer
        LumoraTheme.GRUNGE -> R.color.theme_grunge_primaryContainer to R.color.theme_grunge_onPrimaryContainer
        LumoraTheme.MYSTIC -> R.color.theme_mystic_primaryContainer to R.color.theme_mystic_onPrimaryContainer
        LumoraTheme.CYBER -> R.color.theme_cyber_primaryContainer to R.color.theme_cyber_onPrimaryContainer
        else -> R.color.theme_default_primaryContainer to R.color.theme_default_onPrimaryContainer
    }
}

@Composable
fun WidgetContent(
    text: String,
    author: String,
    @ColorRes backgroundColorRes: Int,
    @ColorRes textColorRes: Int
) {
    val launchAppAction = actionStartActivity(ComponentName(LocalContext.current, MainActivity::class.java))
    val refreshAction = actionRunCallback<RefreshQuoteAction>()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(backgroundColorRes)) // Use the resource ID
            .clickable(launchAppAction)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\"$text\"",
            style = TextStyle(fontSize = 18.sp, color = ColorProvider(textColorRes)),
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )
        Text(
            text = "- $author",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(textColorRes)),
            modifier = GlanceModifier.padding(bottom = 16.dp)
        )
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = stringResource(id = R.string.refresh_quote),
            modifier = GlanceModifier
                .size(24.dp)
                .clickable(refreshAction)
        )
    }
}