package com.mothblank.lumorareader.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.presentation.home.components.InteractiveBubble
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

data class FloatingBubble(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val size: Float,
    val speed: Float,
    val color: Color,
    val opacity: Float,
    val phase: Float
)

data class MenuBubble(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color,
    val description: String,
    val size: Dp,
    val offsetX: Dp,
    val offsetY: Dp,
    val floatRadius: Float,
    val floatSpeed: Float,
    val floatPhase: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubbleHomeScreen(
    navController: NavController,
    overviewViewModel: HomeOverviewViewModel = hiltViewModel()
) {
    val overview by overviewViewModel.overview.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val colorScheme = MaterialTheme.colorScheme

    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Floating background bubbles with theme-aware colors
    var bubbles by remember {
        mutableStateOf(
            List(30) { index ->
                FloatingBubble(
                    id = index,
                    startX = Random.nextFloat() * screenWidth,
                    startY = Random.nextFloat() * screenHeight,
                    size = Random.nextFloat() * 120f + 15f,
                    speed = Random.nextFloat() * 1.5f + 0.3f,
                    color = when (Random.nextInt(5)) {
                        0 -> colorScheme.primary.copy(alpha = Random.nextFloat() * 0.4f + 0.1f)
                        1 -> colorScheme.secondary.copy(alpha = Random.nextFloat() * 0.4f + 0.1f)
                        2 -> colorScheme.tertiary.copy(alpha = Random.nextFloat() * 0.4f + 0.1f)
                        3 -> colorScheme.primaryContainer.copy(alpha = Random.nextFloat() * 0.3f + 0.1f)
                        else -> colorScheme.secondaryContainer.copy(alpha = Random.nextFloat() * 0.3f + 0.1f)
                    },
                    opacity = Random.nextFloat() * 0.7f + 0.2f,
                    phase = Random.nextFloat() * 2f * PI.toFloat()
                )
            }
        )
    }

    // Animation time
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_animation")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    // Asymmetric menu bubbles with very different sizes and spread out positions
    val menuBubbles = remember(colorScheme) {
        listOf(
            MenuBubble(
                title = "Books", // Will be replaced with stringResource in Composable
                icon = Icons.Default.Book,
                route = "books",
                color = colorScheme.primary,
                description = "Your reading collection", // Will be replaced with stringResource in Composable
                size = 180.dp, // Much bigger
                offsetX = (-80).dp, // Far left
                offsetY = (-60).dp, // Up
                floatRadius = 35f,
                floatSpeed = 0.6f,
                floatPhase = 0f
            ),
            MenuBubble(
                title = "Quotes", // Will be replaced with stringResource in Composable
                icon = Icons.Default.FormatQuote,
                route = "quotes",
                color = colorScheme.secondary,
                description = "Inspiring passages", // Will be replaced with stringResource in Composable
                size = 120.dp,
                offsetX = 90.dp, // Far right
                offsetY = (-30).dp, // Slightly up
                floatRadius = 25f,
                floatSpeed = 1.1f,
                floatPhase = PI.toFloat() / 2
            ),
            MenuBubble(
                title = "Themes", // Will be replaced with stringResource in Composable
                icon = Icons.Default.Palette,
                route = "theme",
                color = colorScheme.tertiary,
                description = "Customize appearance", // Will be replaced with stringResource in Composable
                size = 135.dp,
                offsetX = (-70).dp, // Left
                offsetY = 80.dp, // Down
                floatRadius = 40f,
                floatSpeed = 0.8f,
                floatPhase = PI.toFloat()
            ),
            MenuBubble(
                title = "Language", // Will be replaced with stringResource in Composable
                icon = Icons.Default.Language,
                route = "language",
                color = colorScheme.primaryContainer,
                description = "Language settings", // Will be replaced with stringResource in Composable
                size = 85.dp, // Much smaller
                offsetX = 110.dp, // Far right
                offsetY = 70.dp, // Down
                floatRadius = 20f,
                floatSpeed = 1.6f,
                floatPhase = 3 * PI.toFloat() / 2
            )
        )
    }

    var selectedBubble by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Enhanced floating background bubbles
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            bubbles.forEach { bubble ->
                val x = bubble.startX + sin(time * bubble.speed * 0.008f + bubble.phase) * 60f
                val y = bubble.startY + cos(time * bubble.speed * 0.006f + bubble.phase) * 40f - (time * bubble.speed * 0.08f) % size.height

                val adjustedY = if (y < -bubble.size) {
                    size.height + bubble.size
                } else y

                // Main bubble with pulsing effect
                val pulseFactor = 1f + sin(time * 0.015f + bubble.phase) * 0.15f
                drawCircle(
                    color = bubble.color,
                    radius = bubble.size * pulseFactor,
                    center = Offset(x, adjustedY),
                    alpha = bubble.opacity * (0.7f + sin(time * 0.02f + bubble.phase) * 0.3f)
                )

                // Stronger glow effect for atmosphere
                drawCircle(
                    color = bubble.color.copy(alpha = 0.4f),
                    radius = bubble.size * 1.8f * pulseFactor,
                    center = Offset(x, adjustedY),
                    alpha = bubble.opacity * 0.4f
                )

                // Inner highlight for depth
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = bubble.size * 0.3f,
                    center = Offset(x - bubble.size * 0.2f, adjustedY - bubble.size * 0.2f),
                    alpha = bubble.opacity * 0.6f
                )
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(22.dp))

            // App title with enhanced styling
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Light
                ),
                color = colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(id = R.string.immerse_yourself),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 14.dp)
            )

            FilledTonalButton(
                onClick = { navController.navigate("search") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_search))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.home_activity_summary,
                    overview.bookCount,
                    overview.currentlyReading,
                    overview.foregroundMillisWeek / 60_000L
                ),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onBackground.copy(alpha = 0.78f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Asymmetric floating menu bubbles spread across more screen area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Adapt bubble region to portrait devices and controls
                contentAlignment = Alignment.Center
            ) {
                menuBubbles.forEachIndexed { index, bubble ->
                    // Calculate floating position
                    val floatX = sin(time * bubble.floatSpeed * 0.01f + bubble.floatPhase) * bubble.floatRadius
                    val floatY = cos(time * bubble.floatSpeed * 0.008f + bubble.floatPhase) * (bubble.floatRadius * 0.6f)

                    // Get localized strings
                    val localizedTitle = when (bubble.route) {
                        "books" -> stringResource(R.string.books)
                        "quotes" -> stringResource(R.string.quotes)
                        "theme" -> stringResource(R.string.themes)
                        "language" -> stringResource(R.string.language)
                        else -> bubble.title
                    }

                    val onColor = when (bubble.route) {
                        "books" -> colorScheme.onPrimary
                        "quotes" -> colorScheme.onSecondary
                        "theme" -> colorScheme.onTertiary
                        "language" -> colorScheme.onPrimaryContainer
                        else -> colorScheme.onPrimary
                    }

                    Box(
                        modifier = Modifier
                            .offset(
                                x = bubble.offsetX + floatX.dp,
                                y = bubble.offsetY + floatY.dp
                            )
                    ) {
                        InteractiveBubble(
                            size = bubble.size,
                            text = localizedTitle,
                            icon = bubble.icon,
                            color = bubble.color,
                            isSelected = selectedBubble == index,
                            animationTime = time,
                            onColor = onColor,
                            onClick = {
                                selectedBubble = index
                                navController.navigate(bubble.route)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Enhanced subtitle with breathing animation
            val breathingAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breathing"
            )

            Text(
                text = stringResource(id = R.string.tap_a_bubble),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = colorScheme.onBackground.copy(alpha = breathingAlpha),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        // Cleanup selection after navigation
        LaunchedEffect(selectedBubble) {
            if (selectedBubble != null) {
                delay(400)
                selectedBubble = null
            }
        }
    }
}