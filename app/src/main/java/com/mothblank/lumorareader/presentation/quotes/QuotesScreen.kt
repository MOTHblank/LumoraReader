package com.mothblank.lumorareader.presentation.quotes

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.presentation.quotes.components.EditQuoteDialog
import com.mothblank.lumorareader.presentation.quotes.components.QuotesGrid
import com.mothblank.lumorareader.presentation.quotes.components.QuotesTopBar
import kotlinx.coroutines.launch
import com.mothblank.lumorareader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesScreen(
    navController: NavHostController,
    initialSearch: String = "",
    viewModel: QuoteViewModel = hiltViewModel()
) {
    LaunchedEffect(initialSearch) {
        if (initialSearch.isNotBlank()) viewModel.onSearchQueryChange(initialSearch)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quotes = uiState.quotes
    val filterOptions = uiState.filterOptions
    val activeFilterCount = listOf(
        filterOptions.searchQuery.isNotBlank(),
        filterOptions.filterTheme != null,
        filterOptions.filterBookUri != null,
        filterOptions.filterStarred
    ).count { it }
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var quoteToEdit by remember { mutableStateOf<Quote?>(null) }
    var quoteToDelete by remember { mutableStateOf<Quote?>(null) }
    var showAddQuoteDialog by remember { mutableStateOf(false) }
    var recentlyDeletedQuote by remember { mutableStateOf<Quote?>(null) }

    // New state variables for discrete controls
    var showFilterDialog by remember { mutableStateOf(false) }
    var showMoreOptionsDropdown by remember { mutableStateOf(false) }

    // Get string resources outside of coroutines
    val quotesExportedMessage = stringResource(id = R.string.quotes_exported)
    val quotesImportedMessage = stringResource(id = R.string.quotes_imported)
    val quoteDeletedMessage = stringResource(id = R.string.quote_deleted)
    val undoLabel = stringResource(id = R.string.undo)
    val addedToWidgetMessage = stringResource(id = R.string.add_to_widget)

    // Export and import launchers
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            viewModel.onExportQuotes(uri, context, quotes)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(quotesExportedMessage)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.onImportQuotes(uri, context)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(quotesImportedMessage)
            }
        }
    }

    val scrollFraction = remember {
        derivedStateOf {
            if (gridState.layoutInfo.visibleItemsInfo.isNotEmpty() && gridState.firstVisibleItemIndex == 0) {
                val firstItem = gridState.layoutInfo.visibleItemsInfo.first()
                if (firstItem.size.height > 0) {
                    (gridState.firstVisibleItemScrollOffset.toFloat() / firstItem.size.height.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            } else if (gridState.layoutInfo.visibleItemsInfo.isEmpty()) {
                0f
            } else {
                1f
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            QuotesTopBar(
                searchQuery = filterOptions.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                sortBy = filterOptions.sortBy,
                onSortByChange = { viewModel.onSortByChange(it) },
                filterTheme = filterOptions.filterTheme,
                onFilterThemeChange = { viewModel.onFilterThemeChange(it) },
                allThemes = uiState.availableThemes,
                onExport = { exportLauncher.launch("lumora_quotes.json") },
                onImport = { importLauncher.launch("application/json") },
                onShowFilterOptions = { showFilterDialog = true },
                onShowMoreOptions = { showMoreOptionsDropdown = true },
                onOpenDiscovery = { navController.navigate("search") },
                containerColor = lerp(
                    start = MaterialTheme.colorScheme.background.copy(alpha = 0f),
                    stop = MaterialTheme.colorScheme.surface,
                    fraction = scrollFraction.value
                ),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddQuoteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add_quote)
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (activeFilterCount > 0) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        AssistChip(
                            onClick = { showFilterDialog = true },
                            label = {
                                Text(stringResource(R.string.quotes_active_filters, activeFilterCount))
                            }
                        )
                    }
                    item {
                        TextButton(onClick = { viewModel.clearFilters() }) {
                            Text(stringResource(R.string.quotes_clear_filters))
                        }
                    }
                }
            }
            if (quotes.isEmpty()) {
                if (activeFilterCount > 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            stringResource(R.string.quotes_no_filter_results),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { viewModel.clearFilters() }) {
                            Text(stringResource(R.string.quotes_clear_filters))
                        }
                    }
                } else {
                    EmptyQuotesView(modifier = Modifier.weight(1f))
                }
            } else {
                QuotesGrid(
                    modifier = Modifier.weight(1f),
                    quotes = quotes,
                    gridState = gridState,
                    onDeleteClicked = { quote -> quoteToDelete = quote },
                    onEditClicked = { quote -> quoteToEdit = quote },
                    onJumpClick = { quote ->
                        quote.sourceUri?.takeIf { it.isNotBlank() }?.let { source ->
                            navController.navigate("reader/${Uri.encode(source)}?quoteId=${quote.id}")
                        }
                    },
                    onAddToWidgetClick = { quote ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(addedToWidgetMessage)
                        }
                    }
                )
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        ModalBottomSheet(
            onDismissRequest = { showFilterDialog = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Search field
                OutlinedTextField(
                    value = filterOptions.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    label = { Text(stringResource(id = R.string.search_quotes)) },
                    placeholder = { Text(stringResource(id = R.string.search_quotes_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Group quotations by the document they were captured from.
                // Selecting "All books" retains manually entered and legacy quotes.
                val books = uiState.bookSources
                var bookPickerExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { bookPickerExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = books.firstOrNull {
                                it.sourceUri == filterOptions.filterBookUri
                            }?.bookTitle ?: stringResource(R.string.quote_all_books),
                            maxLines = 1
                        )
                    }
                    DropdownMenu(
                        expanded = bookPickerExpanded,
                        onDismissRequest = { bookPickerExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quote_all_books)) },
                            onClick = {
                                viewModel.onBookSourceChange(null)
                                bookPickerExpanded = false
                            }
                        )
                        books.forEach { book ->
                            DropdownMenuItem(
                                text = { Text(book.bookTitle, maxLines = 1) },
                                onClick = {
                                    viewModel.onBookSourceChange(book.sourceUri)
                                    bookPickerExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sort dropdown
                var sortMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.sort_by) + " " +
                                    filterOptions.sortBy.name.replace("_", " ")
                                        .lowercase()
                                        .replaceFirstChar { it.uppercase() }
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortBy.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sort.name.replace("_", " ")
                                            .lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    )
                                },
                                onClick = {
                                    viewModel.onSortByChange(sort)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Theme dropdown
                var themeMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { themeMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.theme) + " " +
                                    (filterOptions.filterTheme ?: stringResource(id = R.string.all))
                        )
                    }
                    DropdownMenu(
                        expanded = themeMenuExpanded,
                        onDismissRequest = { themeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.all)) },
                            onClick = {
                                viewModel.onFilterThemeChange(null)
                                themeMenuExpanded = false
                            }
                        )
                        uiState.availableThemes.forEach { theme ->
                            DropdownMenuItem(
                                text = { Text(theme) },
                                onClick = {
                                    viewModel.onFilterThemeChange(theme)
                                    themeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { viewModel.clearFilters() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.quotes_clear_filters))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // More Options Dropdown
    if (showMoreOptionsDropdown) {
        DropdownMenu(
            expanded = showMoreOptionsDropdown,
            onDismissRequest = { showMoreOptionsDropdown = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.export)) },
                onClick = {
                    exportLauncher.launch("lumora_quotes.json")
                    showMoreOptionsDropdown = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.import_quotes)) },
                onClick = {
                    importLauncher.launch("application/json")
                    showMoreOptionsDropdown = false
                }
            )
        }
    }

    if (quoteToEdit != null) {
        EditQuoteDialog(
            quote = quoteToEdit!!,
            onDismiss = { quoteToEdit = null },
            onConfirm = { updated ->
                viewModel.editQuote(updated)
                quoteToEdit = null
            }
        )
    }

    if (showAddQuoteDialog) {
        EditQuoteDialog(
            quote = Quote(text = "", author = ""),
            onDismiss = { showAddQuoteDialog = false },
            onConfirm = { created ->
                viewModel.addQuote(created)
                showAddQuoteDialog = false
            }
        )
    }

    if (quoteToDelete != null) {
        AlertDialog(
            onDismissRequest = { quoteToDelete = null },
            title = { Text(stringResource(id = R.string.delete_quote)) },
            text = { Text(stringResource(id = R.string.delete_quote_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val quote = quoteToDelete!!
                        viewModel.deleteQuote(quote.id)
                        recentlyDeletedQuote = quote
                        quoteToDelete = null
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = quoteDeletedMessage,
                                actionLabel = undoLabel
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                recentlyDeletedQuote?.let { viewModel.saveQuote(it) }
                            }
                        }
                    }
                ) { Text(stringResource(id = R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { quoteToDelete = null }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun EmptyQuotesView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.no_quotes_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}