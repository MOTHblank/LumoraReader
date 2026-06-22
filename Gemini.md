# LumoraReader Project Instructions

**Project Root:** `C:\LumoraReader`

## Tech Stack
- **Language:** Kotlin Android
- **Architecture:** ViewModel pattern
- **DI:** Hilt
- **UI:** Jetpack Compose
- **E-book:** Readium 3.1.1 (verify API changes)
- **PDF:** Pdfium

## Code Standards
- Follow Kotlin official style guide
- 4-space indentation
- KDoc comments for all public functions
- Comment out (don't delete) when changing major files

## Build Commands
- Tests: `./gradlew test`
- Debug build: `./gradlew assembleDebug`

## Current Objective
(you may edit this category)

**You are a software engineer on FUTURE PLANNING DUTY.**
 ###- Is the widget ready to be deployed?
   * Technical Assessment Points & Files Involved:
       1. UI/UX Review:
           * `com/mothblank/lumorareader/features/widget/QuoteWidget.kt`: Examine the Content composable for visual appeal, responsiveness to different widget sizes, and adherence to Material Design guidelines. Consider adding more customization
              options (e.g., font size, background opacity) if not already present.
           * `com/mothblank/lumorareader/features/widget/QuoteWidgetConfigActivity.kt`: Review the layout and user flow for configuring the widget. Ensure it's intuitive and provides clear feedback.
       2. Error Handling:
           * `com/mothblank/lumorareader/features/widget/RefreshQuoteAction.kt`: Verify robust error handling for data fetching (e.g., what happens if getRandomQuote() returns null, or if getQuoteById() fails). Ensure user-friendly messages are
             displayed in the widget.
       3. Manifest and Build Configuration:
           * `AndroidManifest.xml`: Confirm that the QuoteWidgetProvider and QuoteWidgetConfigActivity are correctly declared with all necessary intent filters and metadata.
           * `app/build.gradle.kts`: Check proguardFiles and consumer-rules.pro to ensure that no essential widget-related code (especially from Glance or Room) is being stripped during release builds.