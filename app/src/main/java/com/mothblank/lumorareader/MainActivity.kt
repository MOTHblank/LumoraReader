package com.mothblank.lumorareader

import android.content.Intent
import android.net.Uri
import androidx.activity.enableEdgeToEdge
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mothblank.lumorareader.presentation.books.BooksScreen
import com.mothblank.lumorareader.presentation.home.BubbleHomeScreen
import com.mothblank.lumorareader.presentation.language.LanguageScreen
import com.mothblank.lumorareader.presentation.preferences.UserPreferencesViewModel
import com.mothblank.lumorareader.presentation.quotes.QuotesScreen
import com.mothblank.lumorareader.presentation.reader.ReaderScreen
import com.mothblank.lumorareader.presentation.theme.ThemeScreen
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.stringResource
import com.mothblank.lumorareader.ui.theme.toColorScheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val bookUriToOpen = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            val userPreferencesViewModel: UserPreferencesViewModel = hiltViewModel()

            // Collect theme state
            val selectedTheme by userPreferencesViewModel.theme.collectAsState()
            val colorScheme = selectedTheme.toColorScheme()
            val animatedBackgroundColor by animateColorAsState(
                targetValue = colorScheme.background,
                animationSpec = tween(durationMillis = 500),
                label = stringResource(id = R.string.theme_background_animation)
            )

            // Collect language state and apply changes reactively
            val selectedLanguage by userPreferencesViewModel.language.collectAsState()
            LaunchedEffect(selectedLanguage) {
                Timber.d("Applying locale '${selectedLanguage.tag}' reactively")
                val appLocales = LocaleListCompat.forLanguageTags(selectedLanguage.tag)
                AppCompatDelegate.setApplicationLocales(appLocales)
            }

            LumoraTheme(theme = selectedTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = animatedBackgroundColor
                ) {
                    val navController = rememberNavController()

                    val bookUri by bookUriToOpen.collectAsState()
                    LaunchedEffect(bookUri) {
                        bookUri?.let { uri ->
                            val encodedUri = Uri.encode(uri.toString())
                            navController.navigate("reader/$encodedUri")
                            bookUriToOpen.value = null
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") { BubbleHomeScreen(navController) }
                        composable("quotes") { QuotesScreen(navController) }
                        composable("theme") { ThemeScreen(navController) }
                        composable("language") { LanguageScreen(navController) }
                        composable("books") { BooksScreen(navController) }
                        composable(
                            route = "reader/{bookUri}?quoteId={quoteId}",
                            arguments = listOf(
                                navArgument("bookUri") { type = NavType.StringType },
                                navArgument("quoteId") { type = NavType.IntType; defaultValue = -1 }
                            )
                        ) {
                            ReaderScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Timber.d("Handling intent: ${intent?.action} with data: ${intent?.data}")
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            bookUriToOpen.value = intent.data
        }
    }
}