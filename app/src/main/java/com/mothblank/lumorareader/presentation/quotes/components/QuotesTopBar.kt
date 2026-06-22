package com.mothblank.lumorareader.presentation.quotes.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.presentation.quotes.SortBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortBy: SortBy,
    onSortByChange: (SortBy) -> Unit,
    filterTheme: String?,
    onFilterThemeChange: (String?) -> Unit,
    allThemes: List<String>,
    onExport: () -> Unit,
    onImport: () -> Unit,
    // New parameters for discrete controls
    onShowFilterOptions: () -> Unit,
    onShowMoreOptions: () -> Unit,
    // New parameter for the scroll-aware background
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.quotes),
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            // Filter/Search icon
            IconButton(onClick = onShowFilterOptions) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(id = R.string.filter_search)
                )
            }

            // More options icon
            IconButton(onClick = onShowMoreOptions) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.more_options)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}