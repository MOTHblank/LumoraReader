package com.mothblank.lumorareader.presentation.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.presentation.preferences.UserPreferencesViewModel
import com.mothblank.lumorareader.ui.language.SupportedLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    navController: NavController? = null,
    userPreferencesViewModel: UserPreferencesViewModel = hiltViewModel()
) {
    val currentLanguage by userPreferencesViewModel.language.collectAsState()

    Scaffold(
        topBar = {
            if (navController != null) {
                TopAppBar(
                    title = { Text(stringResource(R.string.language_settings)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.select_language),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(SupportedLanguage.entries) { language ->
                LanguageItem(
                    language = language,
                    isSelected = language == currentLanguage,
                    onLanguageSelected = { selectedLanguage ->
                        // Use the unified ViewModel instead of individual LanguageViewModel
                        userPreferencesViewModel.setLanguage(selectedLanguage)
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: SupportedLanguage,
    isSelected: Boolean,
    onLanguageSelected: (SupportedLanguage) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLanguageSelected(language) }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = { onLanguageSelected(language) }
        )
        Text(
            text = language.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}