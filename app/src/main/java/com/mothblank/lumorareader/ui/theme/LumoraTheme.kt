package com.mothblank.lumorareader.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mothblank.lumorareader.R
import java.io.Serializable
import androidx.compose.ui.graphics.lerp

enum class LumoraTheme(@StringRes val aName: Int) : Serializable {
    DEFAULT(R.string.theme_default),
    GOTH(R.string.theme_goth),
    COQUETTE(R.string.theme_coquette),
    PIRATE(R.string.theme_pirate),
    NATURE(R.string.theme_nature),
    VAMPIRE(R.string.theme_vampire),
    ROMANCE(R.string.theme_romance),
    GRUNGE(R.string.theme_grunge),
    MYSTIC(R.string.theme_mystic),
    CYBER(R.string.theme_cyber)
}

internal data class ThemePalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color
)

internal val ThemePalettes = mapOf(
    LumoraTheme.DEFAULT to ThemePalette(
        primary = Color(0xFF6366F1), onPrimary = Color(0xFFFFFFFF), // Indigo primary
        primaryContainer = Color(0xFF10B981), onPrimaryContainer = Color(0xFFFFFFFF), // Emerald container
        secondary = Color(0xFF8B5CF6), onSecondary = Color(0xFFFFFFFF), // Purple secondary
        tertiary = Color(0xFF06B6D4), onTertiary = Color(0xFFFFFFFF) // Cyan tertiary
    ),
    LumoraTheme.GOTH to ThemePalette(
        primary = Color(0xFF9D4EDD), onPrimary = Color(0xFF000000), // Dark Purple primary
        primaryContainer = Color(0xFF240046), onPrimaryContainer = Color(0xFF8B8B8B), // Very Dark Purple container
        secondary = Color(0xFF121212), onSecondary = Color(0xFF8B8B8B), // Pure Dark Grey secondary
        tertiary = Color(0xFF7B2CBF), onTertiary = Color(0xFFE6E6E6) // Rich Purple tertiary
    ),
    LumoraTheme.PIRATE to ThemePalette(
        primary = Color(0xFF8B4513), onPrimary = Color(0xFFF5DEB3), // Saddle Brown primary
        primaryContainer = Color(0xFFD2B48C), onPrimaryContainer = Color(0xFF2F1B14), // Tan container
        secondary = Color(0xFFDEB887), onSecondary = Color(0xFF2F1B14), // Burlywood secondary
        tertiary = Color(0xFFF4A460), onTertiary = Color(0xFF2F1B14) // Sandy Brown tertiary
    ),
    LumoraTheme.COQUETTE to ThemePalette(
        primary = Color(0xFFF8BBD9), onPrimary = Color(0xFF4A4A4A), // Soft Pink primary
        primaryContainer = Color(0xFFFFF0F5), onPrimaryContainer = Color(0xFF5A5A5A), // Lavender Blush container
        secondary = Color(0xFFE6E6FA), onSecondary = Color(0xFF5A5A5A), // Lavender secondary
        tertiary = Color(0xFFFFB6C1), onTertiary = Color(0xFF4A4A4A) // Light Pink tertiary
    ),
    LumoraTheme.NATURE to ThemePalette(
        primary = Color(0xFF228B22), onPrimary = Color(0xFFFFFFFF), // Forest Green primary
        primaryContainer = Color(0xFF90EE90), onPrimaryContainer = Color(0xFF0D4D0D), // Light Green container
        secondary = Color(0xFF98FB98), onSecondary = Color(0xFF0D4D0D), // Pale Green secondary
        tertiary = Color(0xFF006400), onTertiary = Color(0xFFFFFFFF) // Dark Green tertiary
    ),
    LumoraTheme.VAMPIRE to ThemePalette(
        primary = Color(0xFF8B0000), onPrimary = Color(0xFFFFFFFF), // Dark Red primary
        primaryContainer = Color(0xFF4B0000), onPrimaryContainer = Color(0xFFE0B9B9), // Very Dark Red container
        secondary = Color(0xFF2F2F2F), onSecondary = Color(0xFFE0B9B9), // Dark Grey secondary
        tertiary = Color(0xFF4B0082), onTertiary = Color(0xFFFFFFFF) // Indigo tertiary
    ),
    LumoraTheme.ROMANCE to ThemePalette(
        primary = Color(0xFF8B0000), onPrimary = Color(0xFFFFFFFF), // Deep Red primary
        primaryContainer = Color(0xFF4B0000), onPrimaryContainer = Color(0xFFFFE4E1), // Very Dark Red container
        secondary = Color(0xFF800020), onSecondary = Color(0xFFFFE4E1), // Burgundy secondary
        tertiary = Color(0xFFB22222), onTertiary = Color(0xFFFFFFFF) // Fire Brick tertiary
    ),
    LumoraTheme.GRUNGE to ThemePalette(
        primary = Color(0xFF708090), onPrimary = Color(0xFFFFFFFF), // Slate Grey primary
        primaryContainer = Color(0xFF2F4F4F), onPrimaryContainer = Color(0xFFD3D3D3), // Dark Slate Grey container
        secondary = Color(0xFF363636), onSecondary = Color(0xFFD3D3D3), // Very Dark Grey secondary
        tertiary = Color(0xFF556B2F), onTertiary = Color(0xFFFFFFFF) // Dark Olive Green tertiary
    ),
    LumoraTheme.MYSTIC to ThemePalette(
        primary = Color(0xFF8A2BE2), onPrimary = Color(0xFFFFFFFF), // Blue Violet primary
        primaryContainer = Color(0xFF4B0082), onPrimaryContainer = Color(0xFFE9D6F7), // Indigo container
        secondary = Color(0xFF483D8B), onSecondary = Color(0xFFE9D6F7), // Dark Slate Blue secondary
        tertiary = Color(0xFF9370DB), onTertiary = Color(0xFFFFFFFF) // Medium Purple tertiary
    ),
    LumoraTheme.CYBER to ThemePalette(
        primary = Color(0xFF00FF41), onPrimary = Color(0xFF000000), // Matrix Green primary
        primaryContainer = Color(0xFF003300), onPrimaryContainer = Color(0xFF00FF41), // Very Dark Green container
        secondary = Color(0xFF000000), onSecondary = Color(0xFF00FF41), // Pure Black secondary
        tertiary = Color(0xFF00CC33), onTertiary = Color(0xFF000000) // Bright Green tertiary
    )
)

val LumoraTheme.representativeColor: Color
    get() = (ThemePalettes[this] ?: ThemePalettes.getValue(LumoraTheme.DEFAULT)).primary

fun LumoraTheme.toColorScheme(): ColorScheme {
    val palette = ThemePalettes[this] ?: ThemePalettes.getValue(LumoraTheme.DEFAULT)

    // Create a beautiful gradient background for the DEFAULT theme
    val background = if (this == LumoraTheme.DEFAULT) {
        // Dark gradient background similar to the bubble screen
        Color(0xFF0F172A)
    } else {
        // For other themes, use the original logic
        lerp(palette.primaryContainer, palette.secondary, 0.6f)
    }

    val onBackground = if (this == LumoraTheme.DEFAULT) {
        Color.White
    } else {
        palette.onPrimaryContainer
    }

    // --- Create Dynamic Surfaces and Containers using LERP ---
    // The surface is the secondary color, tinted slightly with the primary accent color.
    val surface = lerp(palette.secondary, palette.primary, 0.05f)

    // The surfaceVariant is tinted slightly more for emphasis.
    val surfaceVariant = lerp(palette.secondary, palette.primary, 0.10f)

    // The containers are distinct but related, created by tinting the main surface.
    val secondaryContainer = lerp(surface, palette.secondary, 0.2f)
    val tertiaryContainer = lerp(surface, palette.tertiary, 0.2f)

    return ColorScheme(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        secondary = palette.secondary,
        onSecondary = palette.onSecondary,
        tertiary = palette.tertiary,
        onTertiary = palette.onTertiary,

        background = background,
        onBackground = onBackground,

        surface = surface,
        onSurface = palette.onSecondary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = palette.onSecondary,

        secondaryContainer = secondaryContainer,
        onSecondaryContainer = palette.onSecondary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = palette.onTertiary,

        surfaceTint = palette.primary,
        inversePrimary = palette.onPrimary,
        inverseSurface = onBackground,
        inverseOnSurface = background,
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = onBackground.copy(alpha = 0.5f),
        outlineVariant = onBackground.copy(alpha = 0.3f),
        scrim = Color(0x99000000)
    )
}

fun Color.lerp(other: Color, fraction: Float): Color {
    val red = (this.red * (1 - fraction) + other.red * fraction)
    val green = (this.green * (1 - fraction) + other.green * fraction)
    val blue = (this.blue * (1 - fraction) + other.blue * fraction)
    return Color(red, green, blue, alpha)
}

@Composable
fun LumoraTheme(
    theme: LumoraTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = theme.toColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}