package com.mothblank.lumorareader.presentation.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.domain.model.Page
import com.mothblank.lumorareader.presentation.home.components.InteractiveBubble
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import com.mothblank.lumorareader.ui.theme.representativeColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(viewModel: ReaderViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBookInfo by remember { mutableStateOf(false) }
    var showContents by remember { mutableStateOf(false) }
    val shareBook: () -> Unit = {
        val title = uiState.bookTitle?.takeIf { it.isNotBlank() } ?: "Untitled book"
        val author = uiState.bookAuthor?.takeIf { it.isNotBlank() }
        val description = if (author == null) title else "$title\nBy $author"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, description)
        }
        context.startActivity(Intent.createChooser(intent, "Share book"))
    }
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        activity?.let { act ->
            viewModel.setOriginalOrientation(act.requestedOrientation)
        }
    }
    LaunchedEffect(uiState.orientation) {
        val activity = context as? Activity
        activity?.requestedOrientation = uiState.orientation
    }
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            activity?.let { act ->
                // Restore the original orientation when leaving the reader
                act.requestedOrientation = viewModel.restoreOriginalOrientation()
            }
        }
    }
    LumoraTheme(theme = uiState.theme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when {
                    uiState.isLoading -> LoadingView()
                    uiState.error != null -> ErrorView(
                        error = uiState.error,
                        onRetry = { viewModel.retryLoading() })

                    uiState.isEpub -> EpubScrollReader(viewModel)
                    uiState.isPdf -> PdfScrollReader(viewModel)
                }

                // Show save status message (centered, above other elements)
                uiState.quoteSaveStatus?.let { status ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (status.contains("success"))
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (status.contains("success"))
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Enhanced selected text preview and quote options with interactive bubbles
                AnimatedVisibility(
                    visible = uiState.selectedText.isNotBlank(),
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Selected text preview with enhanced styling
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FormatQuote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Selected Text",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.selectedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 4
                                )
                            }
                        }

                        // Interactive bubble options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Clear selection bubble
                            InteractiveBubble(
                                size = 72.dp,
                                text = "",                        // icon-only mode
                                icon = Icons.Default.Close,
                                color = MaterialTheme.colorScheme.error,
                                isSelected = false,
                                onClick = { viewModel.clearTextSelection() }
                            )

                            // Save quote bubble with selection animation
                            InteractiveBubble(
                                size = 72.dp,
                                text = "",                        // icon-only mode
                                icon = Icons.Default.FormatQuote,
                                color = MaterialTheme.colorScheme.primary,
                                isSelected = true,               // keeps the glow
                                onClick = { viewModel.saveSelectedQuote() }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isMenuVisible) {
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = false // Allow partial expansion for better landscape handling
            )
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            ModalBottomSheet(
                onDismissRequest = { viewModel.onToggleMenuVisibility() },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dragHandle = {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .size(width = 32.dp, height = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    ) {}
                }
            ) {
                // Make the entire content scrollable with proper height constraints
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isLandscape) {
                                // In landscape, set a specific height range
                                Modifier
                                    .heightIn(
                                        min = (configuration.screenHeightDp * 0.5f).dp,
                                        max = (configuration.screenHeightDp * 0.8f).dp
                                    )
                            } else {
                                // In portrait, allow natural sizing with minimum
                                Modifier.heightIn(min = 200.dp)
                            }
                        )
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Add navigation bar based on book type
                    if (uiState.isEpub && uiState.hasMultipleChunks) {
                        item {
                            ChunkNavigationBar(
                                currentChunk = uiState.currentChunkIndex,
                                totalChunks = uiState.epubChunkCount,
                                onPreviousChunk = { viewModel.previousEpubChunk() },
                                onNextChunk = { viewModel.nextEpubChunk() }
                            )
                        }

                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else if (uiState.isPdf && uiState.pdfPageCount > 1) {
                        item {
                            PdfPageNavigationBar(
                                currentPage = uiState.currentPdfPage + 1,
                                totalPages = uiState.pdfPageCount,
                                onPreviousPage = {
                                    if (uiState.currentPdfPage > 0) {
                                        viewModel.onPdfPageChanged(uiState.currentPdfPage - 1)
                                    }
                                },
                                onNextPage = {
                                    if (uiState.currentPdfPage < uiState.pdfPageCount - 1) {
                                        viewModel.onPdfPageChanged(uiState.currentPdfPage + 1)
                                    }
                                },
                                onPageSelected = { page ->
                                    if (page in 1..uiState.pdfPageCount) {
                                        viewModel.onPdfPageChanged(page - 1)
                                    }
                                }
                            )
                        }

                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    item {
                        ScrollableReaderSettingsMenu(
                            uiState = uiState,
                            onThemeChange = { viewModel.onThemeChange(it) },
                            onFontSizeChange = { viewModel.onFontSizeChange(it) },
                            onFontFamilyChange = { viewModel.onFontFamilyChange(it) },
                            onOrientationChange = { viewModel.onOrientationChange(it) },
                            onRefresh = { viewModel.retryLoading() },
                            onShare = shareBook,
                            onShowInfo = { showBookInfo = true },
                            isLandscape = isLandscape
                        )
                    }
                }
            }
        }
        if (showBookInfo) {
            val format = when {
                uiState.isPdf -> "PDF"
                uiState.isEpub -> "EPUB"
                else -> "Unknown"
            }
            AlertDialog(
                onDismissRequest = { showBookInfo = false },
                title = { Text("Book information") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Title: ${uiState.bookTitle?.takeIf { it.isNotBlank() } ?: "Unknown"}")
                        Text("Author: ${uiState.bookAuthor?.takeIf { it.isNotBlank() } ?: "Unknown"}")
                        Text("Format: $format")
                        if (uiState.isPdf) {
                            Text("Page: ${uiState.currentPdfPage + 1} of ${uiState.pdfPageCount}")
                        } else if (uiState.isEpub) {
                            Text("Section: ${uiState.currentChunkIndex + 1} of ${uiState.epubChunkCount}")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBookInfo = false }) { Text("Close") }
                }
            )
        }
    }
}
@Composable
fun ScrollableReaderSettingsMenu(
    uiState: ReaderUiState,
    onThemeChange: (LumoraTheme) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onOrientationChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onShare: () -> Unit,
    onShowInfo: () -> Unit,
    isLandscape: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Enhanced header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Reader Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        // Orientation section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Screen Orientation",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Orientation options - use a row for landscape, keep vertical for portrait
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OrientationButtons(uiState, onOrientationChange)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OrientationButtons(uiState, onOrientationChange)
                }
            }
        }

        HorizontalDivider()

        // Theme section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    stringResource(id = R.string.theme_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Enhanced theme selection with interactive bubbles
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.height(80.dp) // Fixed height to prevent layout issues
            ) {
                items(LumoraTheme.entries) { theme ->
                    InteractiveBubble(
                        size = 56.dp,
                        text = "",               // empty = icon-only
                        icon = Icons.Default.Palette,
                        color = theme.representativeColor,
                        isSelected = uiState.theme == theme,
                        onClick = { onThemeChange(theme) }
                    )
                }
            }
        }

        HorizontalDivider()

        // Font size section with enhanced UI
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    stringResource(id = R.string.font_size),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Font size preview - make it more compact in landscape
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Sample text at ${String.format("%.0f", uiState.fontSizeScale * 100)}%",
                    modifier = Modifier.padding(if (isLandscape) 12.dp else 16.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * uiState.fontSizeScale
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Enhanced slider with labels
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "80%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "200%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Slider(
                    value = uiState.fontSizeScale,
                    onValueChange = onFontSizeChange,
                    valueRange = 0.8f..2.0f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }
        }

        HorizontalDivider()

        if (uiState.isEpub) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reading font", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Georgia", "Sans", "Mono").forEach { choice ->
                        FilledTonalButton(
                            onClick = { onFontFamilyChange(choice) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (uiState.fontFamily == choice)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text(choice) }
                    }
                }
            }
            HorizontalDivider()
        }

        // Additional settings with interactive bubbles - more compact in landscape
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InteractiveBubble(
                    size = if (isLandscape) 48.dp else 56.dp,
                    text = "",
                    icon = Icons.Default.Refresh,
                    contentDescription = "Reload book",
                    color = MaterialTheme.colorScheme.secondary,
                    isSelected = false,
                    onClick = onRefresh
                )

                InteractiveBubble(
                    size = if (isLandscape) 48.dp else 56.dp,
                    text = "",
                    icon = Icons.Default.Share,
                    contentDescription = "Share book",
                    color = MaterialTheme.colorScheme.tertiary,
                    isSelected = false,
                    onClick = onShare
                )

                InteractiveBubble(
                    size = if (isLandscape) 48.dp else 56.dp,
                    text = "",
                    icon = Icons.Default.Info,
                    contentDescription = "Book information",
                    color = MaterialTheme.colorScheme.outline,
                    isSelected = false,
                    onClick = onShowInfo
                )
            }
        }
    }
}
@Composable
fun OrientationButtons(
    uiState: ReaderUiState,
    onOrientationChange: (Int) -> Unit
) {
    // Portrait option
    InteractiveBubble(
        size = 56.dp,
        text = "",
        icon = Icons.Default.StayCurrentPortrait,
        color = if (uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline,
        isSelected = uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        onClick = { onOrientationChange(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) }
    )

    // Landscape option
    InteractiveBubble(
        size = 56.dp,
        text = "",
        icon = Icons.Default.StayCurrentLandscape,
        color = if (uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline,
        isSelected = uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        onClick = { onOrientationChange(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) }
    )

    // Auto option
    InteractiveBubble(
        size = 56.dp,
        text = "",
        icon = Icons.Default.ScreenRotation,
        color = if (uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline,
        isSelected = uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        onClick = { onOrientationChange(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }
    )
}
@Composable
fun PdfPageNavigationBar(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageSelected: (Int) -> Unit
) {
    var showPageDialog by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Previous button with enhanced styling
            FilledTonalIconButton(
                onClick = onPreviousPage,
                enabled = currentPage > 1,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (currentPage > 1)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (currentPage > 1)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.previous_page)
                )
            }

            // Page indicator with enhanced styling
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                modifier = Modifier.clickable { showPageDialog = true }
            ) {
                Text(
                    text = "Page $currentPage of $totalPages",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Next button with enhanced styling
            FilledTonalIconButton(
                onClick = onNextPage,
                enabled = currentPage < totalPages,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (currentPage < totalPages)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (currentPage < totalPages)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = stringResource(id = R.string.next_page)
                )
            }
        }
    }

    // Page jump dialog
    if (showPageDialog) {
        AlertDialog(
            onDismissRequest = { showPageDialog = false },
            title = { Text("Go to Page") },
            text = {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = {
                        // Only allow numeric input
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            pageInput = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    label = { Text("Page number") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val page = pageInput.toIntOrNull()
                        if (page != null && page in 1..totalPages) {
                            // Call the onPageSelected function to navigate to the page
                            onPageSelected(page)
                            showPageDialog = false
                            pageInput = ""
                        }
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPageDialog = false
                        pageInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun EpubScrollReader(viewModel: ReaderViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fileUri = uiState.epubFileUri
    val jsBundle = remember { generateJsBundle() }

    if (fileUri != null) {
        // Recreate the AndroidView when a generated HTML URL changes. Updating
        // the old WebView's selection flag alone cannot display a new section.
        key(fileUri) {
            var webView by remember { mutableStateOf<WebView?>(null) }
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            val activeView = this
                            settings.javaScriptEnabled = true
                            settings.allowContentAccess = false
                            settings.allowFileAccess = true
                            @Suppress("DEPRECATION")
                            settings.allowFileAccessFromFileURLs = false
                            @Suppress("DEPRECATION")
                            settings.allowUniversalAccessFromFileURLs = false
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                settings.safeBrowsingEnabled = true
                            }
                            settings.domStorageEnabled = true
                            isVerticalScrollBarEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.setSupportZoom(true)

                            addJavascriptInterface(object : Any() {
                                @JavascriptInterface
                                fun saveEpubPosition(pixelOffset: Int, spineIndex: Int, characterOffset: Int, fraction: Float) {
                                    activeView.post {
                                        viewModel.saveEpubPosition(fileUri, pixelOffset, spineIndex, characterOffset, fraction)
                                    }
                                }
                                @JavascriptInterface
                                fun onScrollNearBottom() {
                                    activeView.post {
                                        if (viewModel.uiState.value.epubFileUri == fileUri) {
                                            viewModel.onScrollNearEndOfSection()
                                        }
                                    }
                                }
                                @JavascriptInterface
                                fun onContentLoaded() = Unit
                                @JavascriptInterface
                                fun onTap() {
                                    activeView.post { viewModel.onToggleMenuVisibility() }
                                }
                            }, "AndroidInterface")

                            addJavascriptInterface(object : Any() {
                                @JavascriptInterface
                                fun onTextSelected(text: String, spineIndex: Int, characterOffset: Int, fraction: Float) {
                                    activeView.post {
                                        if (viewModel.uiState.value.epubFileUri == fileUri) {
                                            viewModel.onTextSelected(
                                                text, fileUri, spineIndex, characterOffset, fraction
                                            )
                                        }
                                    }
                                }
                                @JavascriptInterface
                                fun onSelectionCleared() {
                                    activeView.post {
                                        if (viewModel.uiState.value.epubFileUri == fileUri) {
                                            viewModel.clearTextSelection()
                                        }
                                    }
                                }
                            }, "AndroidSelectionInterface")

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(v: WebView?, u: String?) {
                                    if (v == null || u != fileUri) return
                                    v.evaluateJavascript(jsBundle) {
                                        val anchor = viewModel.getEpubAnchor()
                                        val command = "window.lumoraRestorePosition(" +
                                            (anchor?.spineIndex ?: -1) + "," +
                                            (anchor?.characterOffset ?: 0) + "," +
                                            (anchor?.fraction ?: -1f) + "," +
                                            viewModel.getScrollPosition() + ");"
                                        v.evaluateJavascript(command, null)
                                    }
                                }
                            }
                            webView = this
                            loadUrl(fileUri)
                        }
                    },
                    update = { wv ->
                        wv.evaluateJavascript(
                            "window.isTextSelectionActive = " + uiState.isTextSelectionActive + ";", null
                        )
                    }
                )
            }
            DisposableEffect(fileUri) {
                viewModel.onEpubWebViewAttached(fileUri)
                onDispose {
                    webView?.let { w ->
                        w.stopLoading()
                        w.loadUrl("about:blank")
                        w.removeAllViews()
                        w.destroy()
                    }
                    webView = null
                    viewModel.onEpubWebViewReleased(fileUri)
                }
            }
        }
    } else if (uiState.isLoading) {
        LoadingView()
    }
}

// MARK: - JavaScript Bundle (Memoized)
private fun generateJsBundle(): String = """
    (function(){
        const sel = () => window.getSelection();
        const fire = (e, p) => window.AndroidInterface?.[e]?.(p);
        const LONG = 450, MAX_MOVE = 20, TAP_MOVE = 24, TAP_TIME = 280;
        let sx, sy, st;
        let longTimer;
        let scrollTimeout;
        
        // ✅ Add tap detection variables
        let tapStartX, tapStartY;
        let tapTimeout;
        
        // Capture a spine-specific text offset and scroll fraction. These
        // remain meaningful after changes to font size and viewport geometry.
        let restoring = false;
        const fractionOfScroll = () => {
            const max = Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
            return max ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;
        };
        const visibleTextAnchor = () => {
            const x = Math.max(1, Math.min(window.innerWidth - 1, window.innerWidth * 0.4));
            for (const y of [32, 64, window.innerHeight * 0.22, window.innerHeight * 0.45]) {
                const caret = document.caretRangeFromPoint?.(x, Math.min(y, window.innerHeight - 1));
                const node = caret?.startContainer;
                if (!node) continue;
                const element = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement;
                const section = element?.closest?.('[data-lumora-spine]');
                if (!section) continue;
                const range = document.createRange();
                range.selectNodeContents(section);
                try {
                    range.setEnd(node, caret.startOffset);
                    return {
                        spine: Number(section.dataset.lumoraSpine),
                        offset: range.toString().length
                    };
                } catch (_) { /* Fraction-only fallback for non-text content. */ }
            }
            return null;
        };
        const saveScroll = () => {
            if (restoring) return;
            clearTimeout(scrollTimeout);
            scrollTimeout = setTimeout(() => {
                if (restoring) return;
                const y = Math.round(window.pageYOffset || document.documentElement.scrollTop);
                const h = document.documentElement.scrollHeight;
                const v = window.innerHeight;
                if (!sel() || sel().isCollapsed) {
                    const anchor = visibleTextAnchor();
                    window.AndroidInterface?.saveEpubPosition?.(
                        y, anchor?.spine ?? -1, anchor?.offset ?? 0, fractionOfScroll()
                    );
                }
                if (y + v + 50 >= h) fire('onScrollNearBottom');
            }, 150);
        };
        window.lumoraRestorePosition = (spine, offset, fraction, oldPixels) => {
            restoring = true;
            const restore = () => {
                let restored = false;
                if (spine >= 0 && offset >= 0) {
                    const section = document.querySelector('[data-lumora-spine="' + spine + '"]');
                    if (section) {
                        const walker = document.createTreeWalker(section, NodeFilter.SHOW_TEXT);
                        let remaining = offset, node;
                        while ((node = walker.nextNode())) {
                            const len = node.textContent.length;
                            if (remaining <= len) {
                                const range = document.createRange();
                                range.setStart(node, Math.max(0, remaining));
                                range.setEnd(node, Math.min(len, remaining + 1));
                                const rect = range.getBoundingClientRect();
                                if (rect && rect.height > 0 && Number.isFinite(rect.top)) {
                                    window.scrollTo(0, Math.max(0, window.scrollY + rect.top - 28));
                                    restored = true;
                                }
                                break;
                            }
                            remaining -= len;
                        }
                    }
                }
                if (!restored && fraction >= 0 && fraction <= 1) {
                    const max = Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
                    window.scrollTo(0, fraction * max);
                    restored = true;
                }
                if (!restored) window.scrollTo(0, Math.max(0, oldPixels));
            };
            requestAnimationFrame(() => {
                restore();
                // Images and fonts can settle after the first layout pass.
                setTimeout(restore, 220);
                setTimeout(() => {
                    restore();
                    restoring = false;
                }, 550);
            });
        };
        window.addEventListener('scroll', saveScroll, { passive: true });

        document.addEventListener('touchstart', (e) => {
            if (e.touches.length === 0) return;
            const t = e.touches[0]; sx = t.clientX; sy = t.clientY; st = Date.now();
            
            // ✅ Add tap detection initialization
            tapStartX = t.clientX;
            tapStartY = t.clientY;
            clearTimeout(tapTimeout);
            tapTimeout = setTimeout(() => {
                tapStartX = tapStartY = null;
            }, 500);
            
            if (!sel()?.isCollapsed) return;
            longTimer = setTimeout(() => {
                const dx = Math.abs(e.touches[0].clientX - sx);
                const dy = Math.abs(e.touches[0].clientY - sy);
                if (dx > MAX_MOVE || dy > MAX_MOVE) return;
                const r = document.caretRangeFromPoint(sx, sy);
                if (!r || r.startContainer.nodeType !== Node.TEXT_NODE) return;
                const txt = r.startContainer.textContent;
                let s = r.startOffset, e = r.startOffset;
                while (s > 0 && /\w/.test(txt[s - 1])) s--;
                while (e < txt.length && /\w/.test(txt[e])) e++;
                r.setStart(r.startContainer, s);
                r.setEnd(r.startContainer, e);
                sel()?.removeAllRanges();
                sel()?.addRange(r);
            }, LONG);
        }, { passive: true });
        
        // ✅ Add tap detection
        document.addEventListener('touchend', (e) => {
            clearTimeout(longTimer);
            
            // ✅ Add tap detection logic
            if (!tapStartX || !tapStartY) return;
            const touch = e.changedTouches[0];
            const dx = Math.abs(touch.clientX - tapStartX);
            const dy = Math.abs(touch.clientY - tapStartY);
            if (dx < 20 && dy < 20) {
                window.AndroidInterface?.onTap?.();
            }
        });
        
        document.addEventListener('touchmove', () => clearTimeout(longTimer));
        document.addEventListener('selectionchange', () => {
            const selection = sel();
            const txt = selection?.toString()?.trim();
            if (txt && txt.length > 3 && selection.rangeCount > 0) {
                const range = selection.getRangeAt(0);
                const node = range.startContainer;
                const element = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement;
                const section = element?.closest?.('[data-lumora-spine]');
                let spine = -1, offset = 0;
                if (section) {
                    try {
                        const preceding = document.createRange();
                        preceding.selectNodeContents(section);
                        preceding.setEnd(node, range.startOffset);
                        spine = Number(section.dataset.lumoraSpine);
                        offset = preceding.toString().length;
                    } catch (_) { /* Keep section fraction when markup is unusual. */ }
                }
                window.AndroidSelectionInterface?.onTextSelected(
                    txt, spine, offset, fractionOfScroll()
                );
            } else {
                window.AndroidSelectionInterface?.onSelectionCleared();
            }
        });
        window.AndroidInterface?.onContentLoaded();
    })();
""".trimIndent()

// MARK: - JS Interface for Scroll & Content
class WebViewScrollInterface(private val viewModel: ReaderViewModel) {
    @JavascriptInterface
    fun saveScrollPosition(pos: Int) {
        viewModel.saveScrollPosition(pos)
    }

    @JavascriptInterface
    fun onScrollNearBottom() {
        viewModel.onScrollNearEndOfSection()
    }

    @JavascriptInterface
    fun onContentLoaded() {
        // Optional
    }
}

// MARK: - JS Interface for Text Selection
class WebViewSelectionInterface(private val viewModel: ReaderViewModel) {
    @JavascriptInterface
    fun onTextSelected(text: String) {
        viewModel.onTextSelected(text)
    }

    @JavascriptInterface
    fun onSelectionCleared() {
        viewModel.clearTextSelection()
    }
}

// Enum to track input lock states
enum class InputLockState {
    NONE,
    TEXT_SELECTION,
    MENU_TRANSITION
}

@Composable
fun PdfScrollReader(viewModel: ReaderViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = uiState.currentPdfPage)

    val currentVisiblePage by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            val firstSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            val next = if (firstSize > 0 &&
                listState.firstVisibleItemScrollOffset > firstSize / 2
            ) firstVisible + 1 else firstVisible
            next.coerceIn(0, (uiState.pdfPageCount - 1).coerceAtLeast(0))
        }
    }

    // Avoid interpreting intermediate pages in an animated jump as user
    // scrolling; otherwise navigation to a distant page can be interrupted.
    var programmaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(currentVisiblePage, programmaticScroll) {
        if (!programmaticScroll) {
            if (currentVisiblePage != uiState.currentPdfPage) {
                viewModel.onPdfPageChanged(currentVisiblePage)
            }
            for (page in (currentVisiblePage - 1)..(currentVisiblePage + 2)) {
                viewModel.loadPdfPage(page)
            }
        }
    }

    LaunchedEffect(uiState.currentPdfPage) {
        if (uiState.currentPdfPage != currentVisiblePage &&
            listState.layoutInfo.totalItemsCount > uiState.currentPdfPage
        ) {
            programmaticScroll = true
            try {
                listState.animateScrollToItem(uiState.currentPdfPage)
            } finally {
                programmaticScroll = false
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { viewModel.onToggleMenuVisibility() })
            }
    ) {
        val screenWidth = maxWidth
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        LaunchedEffect(widthPx) { viewModel.onPdfViewportWidthChanged(widthPx) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            state = listState,
            // One-finger page scrolling stays enabled when not zoomed.
            userScrollEnabled = uiState.pdfPageZoomScale <= 1.01f,
            flingBehavior = ScrollableDefaults.flingBehavior(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                count = uiState.pdfPageCount,
                key = { index -> "pdf_page_$index" }
            ) { index ->
                PdfPageItem(
                    pageIndex = index,
                    page = viewModel.pageCache[index],
                    tile = if (uiState.currentPdfPage == index) viewModel.pdfZoomTile else null,
                    isCurrentPage = uiState.currentPdfPage == index,
                    zoom = if (uiState.currentPdfPage == index) uiState.pdfPageZoomScale else 1f,
                    offsetX = if (uiState.currentPdfPage == index) uiState.pdfPageOffsetX else 0f,
                    offsetY = if (uiState.currentPdfPage == index) uiState.pdfPageOffsetY else 0f,
                    screenWidth = screenWidth,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun PdfPageItem(
    pageIndex: Int,
    page: Page?,
    tile: ReaderViewModel.PdfZoomTile?,
    isCurrentPage: Boolean,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    screenWidth: Dp,
    viewModel: ReaderViewModel
) {
    // A page that was evicted from the byte-bounded LRU can load again when
    // it becomes visible, without eagerly rendering the entire document.
    LaunchedEffect(pageIndex, page) {
        if (page == null) viewModel.loadPdfPage(pageIndex)
    }

    when (page) {
        is Page.PdfPage -> {
            val aspectRatio = page.image.width.toFloat() / page.image.height
            val displayHeight = screenWidth / aspectRatio

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(displayHeight)
                    .padding(horizontal = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pageIndex, isCurrentPage) {
                                if (!isCurrentPage) return@pointerInput
                                // Do not capture single-finger scrolls until
                                // zoomed; two fingers begin zoom manipulation.
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val pointersDown = event.changes.count { it.pressed }
                                        val state = viewModel.uiState.value
                                        if (pointersDown >= 2 ||
                                            (state.pdfPageZoomScale > 1.01f && pointersDown > 0)
                                        ) {
                                            val oldZoom = state.pdfPageZoomScale
                                            val newZoom = (oldZoom * event.calculateZoom()).coerceIn(1f, 4f)
                                            val factor = newZoom / oldZoom
                                            val movement = event.calculatePan()
                                            val centroid = event.calculateCentroid(useCurrent = false)
                                            val centerX = size.width / 2f
                                            val centerY = size.height / 2f
                                            val nextX = state.pdfPageOffsetX * factor +
                                                (centroid.x - centerX) * (1f - factor) + movement.x
                                            val nextY = state.pdfPageOffsetY * factor +
                                                (centroid.y - centerY) * (1f - factor) + movement.y
                                            val maxX = size.width * (newZoom - 1f) / 2f
                                            val maxY = size.height * (newZoom - 1f) / 2f
                                            viewModel.onPdfPageZoomChange(
                                                newZoom,
                                                nextX.coerceIn(-maxX, maxX),
                                                nextY.coerceIn(-maxY, maxY)
                                            )
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                    ) {
                        val drawWidth = size.width.roundToInt().coerceAtLeast(1)
                        val drawHeight = size.height.roundToInt().coerceAtLeast(1)
                        withTransform({
                            translate(offsetX, offsetY)
                            scale(zoom, zoom, pivot = size.center)
                        }) {
                            drawImage(
                                image = page.image.asImageBitmap(),
                                dstSize = IntSize(drawWidth, drawHeight)
                            )
                            if (tile != null && tile.pageIndex == pageIndex && zoom > 1.01f) {
                                drawImage(
                                    image = tile.page.image.asImageBitmap(),
                                    dstOffset = IntOffset(
                                        (tile.region.left * drawWidth).roundToInt(),
                                        (tile.region.top * drawHeight).roundToInt()
                                    ),
                                    dstSize = IntSize(
                                        (tile.region.width() * drawWidth).roundToInt().coerceAtLeast(1),
                                        (tile.region.height() * drawHeight).roundToInt().coerceAtLeast(1)
                                    )
                                )
                            }
                        }
                    }
                    if (isCurrentPage && zoom > 1.01f) {
                        TextButton(
                            onClick = { viewModel.resetPdfPageZoom() },
                            modifier = Modifier.align(Alignment.TopEnd),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)
                            )
                        ) {
                            Text("Reset zoom · ${(zoom * 100).roundToInt()}%")
                        }
                    }
                }
            }
        }

        else -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenWidth * 1.414f)
                    .padding(horizontal = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Loading page ${pageIndex + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmartGestureOverlay(
    onToggleMenu: () -> Unit,
    isInputLocked: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isInputLocked) {
                if (!isInputLocked) {
                    detectTapGestures(
                        onTap = { onToggleMenu() }
                    )
                }
            }
    )
}

@Composable
fun ReaderSettingsMenu(
    uiState: ReaderUiState,
    onThemeChange: (LumoraTheme) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onOrientationChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onShare: () -> Unit,
    onShowInfo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Enhanced header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Reader Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            // Orientation section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ScreenRotation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Screen Orientation",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Orientation options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Portrait option
                    InteractiveBubble(
                        size = 64.dp,
                        text = "",
                        icon = Icons.Default.StayCurrentPortrait,
                        color = if (uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                        isSelected = uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                        onClick = { onOrientationChange(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) }
                    )

                    // Landscape option
                    InteractiveBubble(
                        size = 64.dp,
                        text = "",
                        icon = Icons.Default.StayCurrentLandscape,
                        color = if (uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                        isSelected = uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                        onClick = { onOrientationChange(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) }
                    )

                    // Auto option
                    InteractiveBubble(
                        size = 64.dp,
                        text = "",
                        icon = Icons.Default.ScreenRotation,
                        color = if (uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                        isSelected = uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                        onClick = { onOrientationChange(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }
                    )
                }
            }

            HorizontalDivider()

            Text(
                stringResource(id = R.string.theme_title),
                style = MaterialTheme.typography.titleMedium
            )

            // Enhanced theme selection with interactive bubbles
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(LumoraTheme.entries) { theme ->
                    InteractiveBubble(
                        size = 64.dp,
                        text = "",               // empty = icon-only
                        contentDescription = stringResource(id = theme.aName),
                        icon = Icons.Default.Palette,
                        color = theme.representativeColor,
                        isSelected = uiState.theme == theme,
                        onClick = { onThemeChange(theme) }
                    )
                }
            }

            HorizontalDivider()

            // Font size section with enhanced UI
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        stringResource(id = R.string.font_size),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Font size preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Sample text at ${String.format("%.0f", uiState.fontSizeScale * 100)}%",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * uiState.fontSizeScale
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Enhanced slider with labels
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "80%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "200%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Slider(
                        value = uiState.fontSizeScale,
                        onValueChange = onFontSizeChange,
                        valueRange = 0.8f..2.0f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            HorizontalDivider()

            // Additional settings with interactive bubbles
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InteractiveBubble(
                    size = 56.dp,
                    text = "",
                    icon = Icons.Default.Refresh,
                    contentDescription = "Reload book",
                    color = MaterialTheme.colorScheme.secondary,
                    isSelected = false,
                    onClick = onRefresh
                )

                InteractiveBubble(
                    size = 56.dp,
                    text = "",
                    icon = Icons.Default.Share,
                    contentDescription = "Share book",
                    color = MaterialTheme.colorScheme.tertiary,
                    isSelected = false,
                    onClick = onShare
                )

                InteractiveBubble(
                    size = 56.dp,
                    text = "",
                    icon = Icons.Default.Info,
                    contentDescription = "Book information",
                    color = MaterialTheme.colorScheme.outline,
                    isSelected = false,
                    onClick = onShowInfo
                )
            }
        }
    }
}

@Composable
fun ThemeChip(theme: LumoraTheme, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(theme.representativeColor)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = theme.aName),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ChunkNavigationBar(
    currentChunk: Int,
    totalChunks: Int,
    onPreviousChunk: () -> Unit,
    onNextChunk: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Previous button with enhanced styling
            FilledTonalIconButton(
                onClick = onPreviousChunk,
                enabled = currentChunk > 0,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (currentChunk > 0)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (currentChunk > 0)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.previous_section)
                )
            }

            // Progress indicator with enhanced styling
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = stringResource(id = R.string.section_progress, currentChunk + 1, totalChunks),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Next button with enhanced styling
            FilledTonalIconButton(
                onClick = onNextChunk,
                enabled = currentChunk < totalChunks - 1,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (currentChunk < totalChunks - 1)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (currentChunk < totalChunks - 1)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = stringResource(id = R.string.next_section)
                )
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Enhanced loading indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }

            Text(
                text = stringResource(id = R.string.loading_book),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ErrorView(error: String?, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = error ?: stringResource(id = R.string.unknown_error_occurred),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                FilledTonalButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.retry))
                }
            }
        }
    }
}