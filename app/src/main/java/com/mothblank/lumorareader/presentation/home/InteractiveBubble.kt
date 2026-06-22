package com.mothblank.lumorareader.presentation.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlin.math.*

/**
 * Enhanced bubble with **icon+label** and **icon-only** modes.
 * Icon size is auto-scaled to look good in both cases.
 */
@Composable
fun InteractiveBubble(
    size: Dp,
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    animationTime: Float = 0f,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val iconOnly = text.isBlank()

    /* ------- Animations ------- */
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val haloAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.7f else 0f,
        animationSpec = tween(durationMillis = 250)
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 8.dp,
        animationSpec = tween(durationMillis = 250)
    )

    /* ------- Sizing helpers ------- */
    val iconBoxSize = remember(size) { size * 0.62f } // golden ratio
    val iconSize = with(density) {
        // never smaller than 24 dp, never larger than 56 dp
        iconBoxSize.toPx().coerceIn(24f, 56f).toDp()
    }

    Box(
        modifier = modifier
            .size(size * 1.2f) // room for halo
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        /* 1. Halo stroke when selected */
        if (isSelected) {
            Box(
                Modifier
                    .size(size + 16.dp)
                    .border(
                        width = 2.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = haloAlpha), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
        }

        /* 2. Actual bubble card */
        Card(
            onClick = onClick,
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.92f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            modifier = Modifier.size(size)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                /* 3. Gloss spot (top-left highlight) */
                Box(
                    Modifier
                        .size(size)
                        .offset(x = -size * 0.18f, y = -size * 0.18f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(0.35f), Color.Transparent),
                                radius = size.value * 0.35f
                            ),
                            shape = CircleShape
                        )
                )

                /* 4. Content column */
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text.ifBlank { null },
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(iconSize)
                    )

                    if (!iconOnly) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = text,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        /* 5. Floating particles (unchanged) */
        if (isSelected && animationTime > 0f) {
            repeat(6) { idx ->
                val angle = (idx * 60f + animationTime * 2f) * PI.toFloat() / 180f
                val radius = (size.value * 0.55f) + sin(animationTime * 0.05f) * 12f
                val x = cos(angle) * radius
                val y = sin(angle) * radius
                Box(
                    Modifier
                        .offset(x.dp, y.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.8f))
                        .alpha(0.8f + sin(animationTime * 0.1f + idx) * 0.2f)
                )
            }
        }
    }
}