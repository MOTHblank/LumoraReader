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

### 2. Preference Repository Desynchronization
- **Problem:** The reader screen used `ThemePreferenceRepository` ("theme_settings") while general settings and views used `UserPreferencesRepository` ("user_preferences"). They were out of sync.
- **Solution:** Added orientation preferences to `UserPreferencesRepository`, updated `ReaderViewModel` to inject `UserPreferencesRepository`, and removed `ThemePreferenceRepository`.

### 3. Broken Quote Deletion UI Flow
- **Problem:** `QuotesGrid` and `EnhancedQuoteCard` lacked any way for users to click and trigger a deletion, despite the backend logic being fully implemented.
- **Solution:** Added a visual trash `IconButton` and an `onDeleteClick` parameter to `EnhancedQuoteCard` and wired it up through `QuotesGrid`.

## Phase 3: Robustness & Performance

### 1. Safe PDF Bitmap Allocation
- **Problem:** Allocating exact high-resolution PDF page size bitmaps can easily exceed heap limits, resulting in `OutOfMemoryError` failures on large or scanned documents.
- **Solution:** Introduced aspect-ratio preserved scaling bounds in [PdfBookReader.kt](file:///C:/LumoraReader/app/src/main/java/com/mothblank/lumorareader/data/PdfBookReader.kt) to downscale huge PDF pages exceeding `MAX_BITMAP_DIMENSION` (2048 pixels) to fit safely in memory.

### 2. Widget Database Try-Catch Protections
- **Problem:** Failures or delays in database access (e.g. SQLiteException) during widget refresh operations would crash the widget state update loop.
- **Solution:** Wrapped all database operations in [RefreshQuoteAction.kt](file:///C:/LumoraReader/app/src/main/java/com/mothblank/lumorareader/features/widget/RefreshQuoteAction.kt) in a `try-catch` block. On failure, it gracefully catches the exception, registers error messaging state in the widget, and renders localized alternate fallback messages defined in [strings.xml](file:///C:/LumoraReader/app/src/main/res/values/strings.xml).

## Build & Verification Commands
- **Run unit tests:** `.\gradlew test`
- **Build debug APK:** `.\gradlew assembleDebug`
