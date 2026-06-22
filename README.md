# LumoraReader Theme System

## Overview
LumoraReader supports a dynamic theming system, allowing users to choose from multiple visual styles (themes) that affect the entire app. Themes are defined in code and can be switched at runtime from the Theme screen. The selected theme is persisted and restored across app restarts.

## How the Theme System Works
- All themes are defined in `ui/theme/LumoraTheme.kt` and `ui/theme/Color.kt`.
- The main theme provider is the `LumoraTheme` composable, which takes a `LumoraThemeType` parameter.
- The current theme is managed by a `ThemeViewModel` and persisted using DataStore.
- Users can select a theme in the Theme screen, which previews and applies the theme instantly.

## Adding a New Theme
1. **Define Colors:**
   - In `ui/theme/Color.kt`, add new `Color` values for your theme's palette (primary, secondary, tertiary, accent, etc.).
2. **Register the Theme:**
   - Add a new entry to the `LumoraThemeType` enum in `ui/theme/LumoraTheme.kt`.
3. **Wire Up the Palette:**
   - In `getLumoraColorScheme` (in `LumoraTheme.kt`), add a new case for your theme, returning a `lightColorScheme` or `darkColorScheme` with your colors.
   - Explicitly set `onPrimary`, `onSecondary`, and `onTertiary` for good contrast.
4. **Update the UI:**
   - The new theme will automatically appear in the Theme selection screen.

## Best Practices
- **Contrast:** Always ensure text and icons have high contrast with their backgrounds. Use `onPrimary`, `onSecondary`, etc., to control this.
- **Accessibility:** Test your theme with accessibility tools (screen readers, color blindness simulators) to ensure readability.
- **Focus Indicators:** Use a color that stands out for focus rings or outlines.
- **Naming:** Use descriptive names for your theme and color variables.

## Example: Adding a "Ocean" Theme
1. In `Color.kt`:
   ```kotlin
   val OceanPrimary = Color(0xFF0288D1)
   val OceanSecondary = Color(0xFFB3E5FC)
   val OceanTertiary = Color(0xFF01579B)
   val OceanAccent = Color(0xFF00B8D4)
   ```
2. In `LumoraThemeType`:
   ```kotlin
   Ocean,
   ```
3. In `getLumoraColorScheme`:
   ```kotlin
   LumoraThemeType.Ocean -> lightColorScheme(
       primary = OceanPrimary,
       secondary = OceanSecondary,
       tertiary = OceanTertiary,
       onPrimary = Color.White,
       onSecondary = Color.Black,
       onTertiary = OceanAccent
   )
   ```

## Where to Look
- Theme logic: `ui/theme/LumoraTheme.kt`
- Color palettes: `ui/theme/Color.kt`
- Theme selection UI: `presentation/theme/ThemeScreen.kt`
- Theme persistence: `ui/theme/ThemePreferenceRepository.kt`, `presentation/theme/ThemeViewModel.kt`

---
For questions or contributions, see the code comments or open an issue! 

# FolioReader/Readium Theme Integration

## Overview
FolioReader in LumoraReader is fully integrated with the app-wide Lumora theme system. All visual aspects of the reading experience—including EPUB content, overlays, toolbars, highlights, and media controls—are themed according to the selected LumoraThemeType.

## How It Works
- The selected theme is passed to FolioActivity via intent when opening a book.
- FolioActivity maps the theme to a FolioThemeColors palette (background, text, highlight, nav bar, selection, etc.).
- The EPUB content is themed by injecting a CSS `<style>` block on every page load, using a pagination listener and JavaScript.
- Publisher styles are disabled for full control.
- All overlays, toolbars, and custom views in FolioReader reference FolioThemeColors for their colors.

## Extending Theme Support
1. **To add a new theme:**
   - Add your palette to `FolioThemeColors` in `FolioActivity.kt` and to the app’s theme system.
   - Update the theme selection UI if needed.
2. **To theme a new overlay or custom view:**
   - In your Fragment or View, get the current theme from the parent FolioActivity:
     ```kotlin
     val folioColors = getFolioThemeColors((activity as? FolioActivity)?.lumoraTheme ?: LumoraThemeType.Default)
     ```
   - Apply `folioColors.background`, `folioColors.text`, etc., to your UI elements.
3. **To update highlight/annotation color:**
   - Use `folioColors.highlight` when creating highlights or custom annotation views.
4. **To update the EPUB content CSS:**
   - Edit `generateFolioThemeCss()` in `FolioActivity.kt` to add or adjust CSS rules for new theme properties.

## Best Practices
- Always use `FolioThemeColors` for any UI element in FolioReader that should match the app theme.
- For overlays, toolbars, and custom views, reference the parent activity’s theme.
- For EPUB content, inject CSS on every page load (pagination event) to ensure consistency.
- If you add new overlays or features, update them to use the theme system for a seamless experience.

## Troubleshooting
- If a UI element is not themed, check that it references `FolioThemeColors` and not legacy config or hardcoded colors.
- If CSS is not applied to EPUB content, ensure the pagination listener and JavaScript injection are working as expected.

## Example: Theming a New Overlay
```kotlin
val folioColors = getFolioThemeColors((activity as? FolioActivity)?.lumoraTheme ?: LumoraThemeType.Default)
overlayView.setBackgroundColor(folioColors.background)
overlayTextView.setTextColor(folioColors.text)
```

---
For questions or contributions, see the code comments or open an issue! 