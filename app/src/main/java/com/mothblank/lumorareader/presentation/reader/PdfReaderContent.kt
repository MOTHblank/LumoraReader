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
            contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp)
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

