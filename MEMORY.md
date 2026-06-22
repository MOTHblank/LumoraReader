# 🧠 MEMORY.md - LumoraReader Long-Term Memory

This file serves as our session continuity and long-term memory. It captures architectural decisions, bug history, and key lessons.

## Project Structure & Architecture
- **Root Directory:** `C:\LumoraReader` (the initial workspace `AndroidStudioProjects\LumoraReader` is a placeholder).
- **Core Stack:** Kotlin, Jetpack Compose, Hilt, Readium 3.1.1, Pdfium.
- **Preference Storage:**
  - Unified everything into `UserPreferencesRepository.kt` (under `com.mothblank.lumorareader.ui.preferences`), which writes to `user_preferences`.
  - Deleted the redundant `ThemePreferenceRepository.kt` (backed up to `.bak`) to prevent split-state desynchronization.

## Critical Bugs Fixed (June 22, 2026)

### 1. Multi-Widget Configuration Overwrites
- **Problem:** `QuoteWidgetStateDefinition.kt` returned a shared static preferences database for all widgets, so configuring one widget overwrote every other widget instance on the home screen.
- **Solution:** Modified `getDataStore` to dynamically generate and return preferred per-instance DataStores using `PreferenceDataStoreFactory` and `context.preferencesDataStoreFile(fileKey)`.

### 3. Preference Repository Desynchronization
- **Problem:** The reader screen used `ThemePreferenceRepository` ("theme_settings") while general settings and views used `UserPreferencesRepository` ("user_preferences"). They were out of sync.
- **Solution:** Added orientation preferences to `UserPreferencesRepository`, updated `ReaderViewModel` to inject `UserPreferencesRepository`, and removed `ThemePreferenceRepository`.

### 4. Broken Quote Deletion UI Flow
- **Problem:** `QuotesGrid` and `EnhancedQuoteCard` lacked any way for users to click and trigger a deletion, despite the backend logic being fully implemented.
- **Solution:** Added a visual trash `IconButton` and an `onDeleteClick` parameter to `EnhancedQuoteCard` and wired it up through `QuotesGrid`.

## Build & Verification Commands
- **Run unit tests:** `.\gradlew test`
- **Build debug APK:** `.\gradlew assembleDebug`
