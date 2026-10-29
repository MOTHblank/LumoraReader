package com.mothblank.lumorareader.presentation.books

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.domain.model.ReadingStatus
import com.mothblank.lumorareader.domain.model.RecentBook
import java.util.Locale

/** Searchable, locally organized book library. No network or external cover service. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    navController: NavHostController,
    viewModel: BooksViewModel = hiltViewModel()
) {
    val books by viewModel.recentBooks.collectAsState()
    val context = LocalContext.current
    var search by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var statusName by rememberSaveable { mutableStateOf<String?>(null) }
    var collectionFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sortOrder by rememberSaveable { mutableStateOf("RECENT") }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showCollectionMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecentBook?>(null) }
    var removing by remember { mutableStateOf<RecentBook?>(null) }

    val allCollections = remember(books) {
        books.mapNotNull { it.collectionName?.takeIf(String::isNotBlank) }
            .distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val visible = remember(books, search, favoritesOnly, statusName, collectionFilter, sortOrder) {
        val query = search.trim().lowercase(Locale.ROOT)
        val selected = books.filter { book ->
            (!favoritesOnly || book.isFavorite) &&
                (statusName == null || book.readingStatus.name == statusName) &&
                (collectionFilter == null || book.collectionName == collectionFilter) &&
                (query.isEmpty() || listOfNotNull(book.title, book.author, book.collectionName)
                    .any { it.lowercase(Locale.ROOT).contains(query) })
        }
        when (sortOrder) {
            "TITLE" -> selected.sortedBy { it.title.lowercase(Locale.ROOT) }
            "PROGRESS" -> selected.sortedWith(
                compareByDescending<RecentBook> { it.progress ?: 0f }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
            )
            else -> selected.sortedByDescending { it.lastOpened.millis }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { chosen ->
            // Some document providers offer a transient grant only.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    chosen, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.addRecentBook(chosen)
            navController.navigate("reader/${Uri.encode(chosen.toString())}")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.library), fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FilledTonalButton(
                    onClick = { filePicker.launch(arrayOf("application/epub+zip", "application/pdf")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.library_open_new))
                }
            }
            item { LibraryStats(books) }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text(stringResource(R.string.library_search)) },
                    placeholder = { Text(stringResource(R.string.library_search_hint)) }
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = !favoritesOnly && statusName == null && collectionFilter == null,
                            onClick = {
                                favoritesOnly = false
                                statusName = null
                                collectionFilter = null
                            },
                            label = { Text(stringResource(R.string.library_all)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = !favoritesOnly },
                            label = { Text(stringResource(R.string.library_favorites)) }
                        )
                    }
                    item {
                        Box {
                            FilterChip(
                                selected = statusName != null,
                                onClick = { showStatusMenu = true },
                                label = {
                                    Text(statusName?.let { status ->
                                        ReadingStatus.entries.firstOrNull { it.name == status }?.let {
                                            stringResource(statusTextResource(it))
                                        }
                                    } ?: stringResource(R.string.library_filter_status))
                                }
                            )
                            DropdownMenu(
                                expanded = showStatusMenu,
                                onDismissRequest = { showStatusMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_all)) },
                                    onClick = { statusName = null; showStatusMenu = false }
                                )
                                ReadingStatus.entries.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(statusTextResource(status))) },
                                        onClick = { statusName = status.name; showStatusMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Box {
                            FilterChip(
                                selected = collectionFilter != null,
                                onClick = { showCollectionMenu = true },
                                label = {
                                    Text(
                                        collectionFilter ?: stringResource(R.string.library_filter_collection),
                                        maxLines = 1
                                    )
                                }
                            )
                            DropdownMenu(
                                expanded = showCollectionMenu,
                                onDismissRequest = { showCollectionMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_all_collections)) },
                                    onClick = { collectionFilter = null; showCollectionMenu = false }
                                )
                                allCollections.forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name, maxLines = 1) },
                                        onClick = { collectionFilter = name; showCollectionMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.library_books_count, visible.size),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Tune, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(
                                    when (sortOrder) {
                                        "TITLE" -> R.string.library_sort_title
                                        "PROGRESS" -> R.string.library_sort_progress
                                        else -> R.string.library_sort_recent
                                    }
                                )
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf(
                                "RECENT" to R.string.library_sort_recent,
                                "TITLE" to R.string.library_sort_title,
                                "PROGRESS" to R.string.library_sort_progress
                            ).forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(label)) },
                                    onClick = { sortOrder = key; showSortMenu = false }
                                )
                            }
                        }
                    }
                }
            }
            if (visible.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Book,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(
                                if (books.isEmpty()) R.string.no_books_opened_yet
                                else R.string.library_empty_filtered
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (books.isNotEmpty()) {
                            TextButton(onClick = {
                                search = ""; favoritesOnly = false; statusName = null; collectionFilter = null
                            }) { Text(stringResource(R.string.library_filter_reset)) }
                        }
                    }
                }
            } else {
                items(visible, key = { it.uri }) { book ->
                    LibraryBookCard(
                        book = book,
                        onOpen = { navController.navigate("reader/${Uri.encode(book.uri)}") },
                        onToggleFavorite = { viewModel.setFavorite(book, !book.isFavorite) },
                        onOrganize = { editing = book },
                        onRemove = { removing = book }
                    )
                }
            }
        }
    }

    editing?.let { book ->
        EditLibraryBookDialog(
            book = book,
            existingCollections = allCollections,
            onDismiss = { editing = null },
            onSave = { status, collection ->
                viewModel.setReadingStatus(book, status)
                viewModel.setCollection(book, collection)
                editing = null
            }
        )
    }
    removing?.let { book ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.library_remove)) },
            text = { Text(stringResource(R.string.library_remove_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeRecentBook(book.uri)
                    removing = null
                }) { Text(stringResource(R.string.library_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) {
                    Text(stringResource(R.string.library_cancel))
                }
            }
        )
    }
}
