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
import com.mothblank.lumorareader.data.EpubChapter
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

                // Location indicator that never covers selected-text actions.
                if (!uiState.isLoading && (uiState.isEpub || uiState.isPdf) &&
                    uiState.selectedText.isBlank() && !uiState.isMenuVisible
                ) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (uiState.isEpub)
                                        uiState.currentChapterTitle ?: "Section ${uiState.currentChunkIndex + 1}"
                                    else "Page ${uiState.currentPdfPage + 1} / ${uiState.pdfPageCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("${(uiState.readingProgress.coerceIn(0f, 1f) * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { uiState.readingProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                uiState.quoteHighlightText?.let { passage ->
                    if (!uiState.isLoading) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter)
                                .fillMaxWidth().padding(12.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 3.dp
                        ) {
                            Row(modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatQuote, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    passage, modifier = Modifier.weight(1f),
                                    maxLines = 2, style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(onClick = { viewModel.dismissQuoteHighlight() }) {
                                    Text("Dismiss")
                                }
                            }
                        }
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
                    if (uiState.isEpub) {
                        item {
                            OutlinedButton(
                                onClick = {
                                    showContents = true
                                    viewModel.onToggleMenuVisibility()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Contents · ${uiState.currentChapterTitle ?: "Browse chapters"}")
                            }
                        }
                    }
                    if ((uiState.isEpub && uiState.epubChunkCount > 1) ||
                        (uiState.isPdf && uiState.pdfPageCount > 1)
                    ) {
                        item {
                            ReadingPositionSelector(
                                uiState = uiState,
                                onEpubSeek = { viewModel.goToEpubChunk(it) },
                                onPdfSeek = { viewModel.onPdfPageChanged(it) }
                            )
                        }
                    }
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
        if (showContents) {
            val entries = uiState.tableOfContents.takeIf { it.isNotEmpty() }
                ?: (0 until uiState.epubChunkCount).map {
                    EpubChapter("Section ${it + 1}", it, -1)
                }
            AlertDialog(
                onDismissRequest = { showContents = false },
                title = { Text("Table of contents") },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                        items(entries.size) { i ->
                            val chapter = entries[i]
                            TextButton(
                                onClick = {
                                    if (chapter.spineIndex >= 0)
                                        viewModel.goToEpubChapter(chapter.spineIndex)
                                    else viewModel.goToEpubChunk(chapter.chunkIndex)
                                    showContents = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = chapter.title,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start,
                                    color = if (chapter.chunkIndex == uiState.currentChunkIndex)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text("${chapter.chunkIndex + 1}")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showContents = false }) { Text("Close") }
                }
            )
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
// Enum to track input lock states
enum class InputLockState {
    NONE,
    TEXT_SELECTION,
    MENU_TRANSITION
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