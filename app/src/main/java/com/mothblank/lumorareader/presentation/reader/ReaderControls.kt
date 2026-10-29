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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
internal fun ReadingPositionSelector(
    uiState: ReaderUiState,
    onEpubSeek: (Int) -> Unit,
    onPdfSeek: (Int) -> Unit
) {
    val count = if (uiState.isPdf) uiState.pdfPageCount else uiState.epubChunkCount
    if (count < 2) return
    val index = if (uiState.isPdf) uiState.currentPdfPage else uiState.currentChunkIndex
    var selected by remember(uiState.bookType, index) { mutableStateOf(index.toFloat()) }
    val seekLabel = stringResource(
        if (uiState.isPdf) R.string.reader_jump_page else R.string.reader_jump_section
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (uiState.isPdf) "Jump to page" else "Jump to section",
                style = MaterialTheme.typography.titleSmall)
            Text("${selected.roundToInt() + 1} / $count",
                style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            modifier = Modifier.semantics {
                contentDescription = seekLabel
            },
            value = selected.coerceIn(0f, (count - 1).toFloat()),
            onValueChange = { selected = it },
            onValueChangeFinished = {
                val target = selected.roundToInt().coerceIn(0, count - 1)
                if (uiState.isPdf) onPdfSeek(target) else onEpubSeek(target)
            },
            valueRange = 0f..(count - 1).toFloat()
        )
    }
}

@Composable
fun ScrollableReaderSettingsMenu(
    uiState: ReaderUiState,
    onThemeChange: (LumoraTheme) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onMarginChange: (String) -> Unit,
    onPdfNightModeChange: (Boolean) -> Unit,
    onPdfPageSpacingChange: (Int) -> Unit,
    onEnterFocusMode: () -> Unit,
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

        // A single explicit way into focus mode. The main reader keeps a
        // separate accessible exit action rather than trapping keyboard users.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.reader_focus_mode),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.reader_focus_mode_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onEnterFocusMode,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.reader_enter_focus_mode)) }
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
                        contentDescription = if (uiState.theme == theme)
                            stringResource(R.string.reader_theme_selected, stringResource(theme.aName))
                        else stringResource(R.string.reader_theme_option, stringResource(theme.aName)),
                        color = theme.representativeColor,
                        isSelected = uiState.theme == theme,
                        onClick = { onThemeChange(theme) }
                    )
                }
            }
        }

        HorizontalDivider()

        // Reading typography. Sliders commit on release to avoid generating
        // a new EPUB WebView for every pointer movement.
        var pendingFontSize by remember(uiState.fontSizeScale) {
            mutableStateOf(uiState.fontSizeScale)
        }
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
                    text = stringResource(
                        R.string.reader_font_size_percentage,
                        (pendingFontSize * 100).roundToInt()
                    ),
                    modifier = Modifier.padding(if (isLandscape) 12.dp else 16.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * pendingFontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Enhanced slider with labels
            val fontSizeDescription = stringResource(R.string.reader_font_size_a11y)
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
                    modifier = Modifier.semantics { contentDescription = fontSizeDescription },
                    value = pendingFontSize,
                    onValueChange = { pendingFontSize = it },
                    onValueChangeFinished = {
                        if (pendingFontSize != uiState.fontSizeScale) onFontSizeChange(pendingFontSize)
                    },
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

        if (uiState.isEpub) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var pendingLineSpacing by remember(uiState.epubLineSpacing) {
                    mutableStateOf(uiState.epubLineSpacing)
                }
                Text(stringResource(R.string.reader_typography_section),
                    style = MaterialTheme.typography.titleMedium)
                val formattedSpacing = String.format(
                    java.util.Locale.getDefault(), "%.1f", pendingLineSpacing
                )
                Text(
                    stringResource(R.string.reader_line_height_value, formattedSpacing),
                    style = MaterialTheme.typography.bodyMedium
                )
                val lineSpacingLabel = stringResource(R.string.reader_line_spacing)
                Slider(
                    value = pendingLineSpacing,
                    onValueChange = { pendingLineSpacing = it },
                    onValueChangeFinished = {
                        if (pendingLineSpacing != uiState.epubLineSpacing) {
                            onLineSpacingChange(pendingLineSpacing)
                        }
                    },
                    valueRange = 1.4f..2.2f,
                    steps = 7,
                    modifier = Modifier.semantics {
                        contentDescription = lineSpacingLabel
                    }
                )
                Text(stringResource(R.string.reader_margin_setting),
                    style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Compact", "Comfortable", "Wide").forEach { option ->
                        val label = stringResource(
                            when (option) {
                                "Compact" -> R.string.reader_margin_compact
                                "Wide" -> R.string.reader_margin_wide
                                else -> R.string.reader_margin_comfortable
                            }
                        )
                        OutlinedButton(
                            onClick = { onMarginChange(option) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (uiState.epubMargin == option)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) { Text(label, maxLines = 1) }
                    }
                }
            }
            HorizontalDivider()
        } else if (uiState.isPdf) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.reader_pdf_display),
                    style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.reader_pdf_night_mode),
                            style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.reader_pdf_night_mode_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(
                        checked = uiState.pdfNightMode,
                        onCheckedChange = onPdfNightModeChange
                    )
                }
                Text(stringResource(R.string.reader_pdf_spacing),
                    style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(8 to R.string.reader_pdf_spacing_compact,
                        24 to R.string.reader_pdf_spacing_comfortable).forEach { (spacing, labelId) ->
                        OutlinedButton(
                            onClick = { onPdfPageSpacingChange(spacing) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (uiState.pdfPageSpacingDp == spacing)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) { Text(stringResource(labelId), maxLines = 1) }
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
        contentDescription = stringResource(R.string.reader_orientation_portrait) +
            if (uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) stringResource(R.string.reader_selected) else "",
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
        contentDescription = stringResource(R.string.reader_orientation_landscape) +
            if (uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) stringResource(R.string.reader_selected) else "",
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
        contentDescription = stringResource(R.string.reader_orientation_auto) +
            if (uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) stringResource(R.string.reader_selected) else "",
        color = if (uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline,
        isSelected = uiState.orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        onClick = { onOrientationChange(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }
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

