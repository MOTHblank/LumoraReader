package com.mothblank.lumorareader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mothblank.lumorareader.ui.theme.LumoraReaderTheme // Assuming this is your theme file
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // We keep the ViewModel in case you want to navigate to the quotes list
    // private val viewModel: QuoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LumoraReaderTheme {
                BubbleHomeScreen()
            }
        }
    }
}

@Composable
fun BubbleHomeScreen() {
    val context = LocalContext.current

    // This is the main container with the dark gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B3E), // Dark Blue
                        Color(0xFF1E3A8A)  // Lighter Blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Theme Bubble (Bottom Right)
        InteractiveBubble(
            size = 120.dp,
            text = "Theme",
            icon = Icons.Rounded.Palette,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 80.dp, y = 160.dp),
            gradient = Brush.radialGradient(
                colors = listOf(
                    Color(0x6693C5FD), // Lighter, semi-transparent
                    Color(0x4D3B82F6)
                ),
                radius = 150f
            )
        ) {
            Toast.makeText(context, "Theme customization coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Quotes Bubble (Top Right)
        InteractiveBubble(
            size = 180.dp,
            text = "Quotes",
            icon = Icons.Rounded.FormatQuote,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 60.dp, y = -120.dp),
            gradient = Brush.radialGradient(
                colors = listOf(
                    Color(0xAAFB923C), // Orange-ish tint
                    Color(0x99F472B6)  // Pink-ish tint
                ),
                radius = 250f
            )
        ) {
            // TODO: Navigate to the Quotes screen
            Toast.makeText(context, "Navigating to Quotes...", Toast.LENGTH_SHORT).show()
        }

        // Books Bubble (Center Left)
        InteractiveBubble(
            size = 240.dp,
            text = "Books",
            icon = Icons.Rounded.AutoStories,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-80).dp, y = 20.dp),
            gradient = Brush.radialGradient(
                colors = listOf(
                    Color(0xAAF472B6), // Pink
                    Color(0x9967E8F9)  // Cyan
                ),
                radius = 300f
            )
        ) {
            // Launch the FolioReader with teaching.epub from assets
            val intent = android.content.Intent(context, com.mothblank.lumorareader.folioreader.ui.activity.FolioActivity::class.java)
            intent.putExtra(
                com.mothblank.lumorareader.folioreader.ui.activity.FolioActivity.INTENT_EPUB_SOURCE_PATH,
                "file:///android_asset/teaching.epub"
            )
            intent.putExtra(
                com.mothblank.lumorareader.folioreader.ui.activity.FolioActivity.INTENT_EPUB_SOURCE_TYPE,
                com.mothblank.lumorareader.folioreader.ui.activity.FolioActivity.EpubSourceType.ASSETS
            )
            context.startActivity(intent)
        }
    }
}

@Composable
fun InteractiveBubble(
    size: Dp,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    gradient: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush = gradient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // No ripple effect to keep the bubble clean
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glossy effect overlay
        Box(
            modifier = Modifier
                .size(size)
                .offset(x = (-20).dp, y = (-20).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.0f)
                        ),
                        radius = size.value / 2
                    ),
                    shape = CircleShape
                )
        )

        // Content (Icon and Text)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(size * 0.35f) // Icon size is proportional to bubble size
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = (size.value / 12).sp, // Text size is proportional
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun BubbleHomeScreenPreview() {
    LumoraReaderTheme {
        BubbleHomeScreen()
    }
}