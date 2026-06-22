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

### 3. Sliding-Window PDF Render Cache Pruning (June 22, 2026)
- **Problem:** Rendered PDF pages (bitmaps up to 16MB each) were cached in `pageCache` but never evicted during scrolling, leading to high RAM consumption and eventual `OutOfMemoryError` crashes during long reading sessions.
- **Solution:** Added `prunePdfPageCache(currentPageIndex)` in [ReaderViewModel.kt](file:///C:/LumoraReader/app/src/main/java/com/mothblank/lumorareader/presentation/reader/ReaderViewModel.kt) to keep only a sliding window of $\pm$ 5 pages in memory and evict the rest during scrolling.

### 4. Zero-Disk EPUB Stream Metadata Parsing (June 22, 2026)
- **Problem:** `BookRepositoryImpl.getBookDetails` ran on the main thread and created a heavy, temporary `EpubBookReader` instance just to read title and author metadata, which fully unzipped the entire EPUB and wrote hundreds of files to disk twice back-to-back.
- **Solution:** Optimized [BookRepositoryImpl.kt](file:///C:/LumoraReader/app/src/main/java/com/mothblank/lumorareader/data/repository/BookRepositoryImpl.kt) to use `Dispatchers.IO` and added a stream-based zip parser using `ZipInputStream` and XML-Pull. Metadata is now parsed directly from the content stream in milliseconds with zero disk writes.

### 5. Lazy EPUB Chapter Chunking (June 22, 2026)
- **Problem:** `EpubBookReader` eagerly read every single chapter, executed multiple heavy path-resolving regex replacements, and loaded all text into memory in the constructor just to calculate chunk groups.
- **Solution:** Updated `createChunks` in [EpubBookReader.kt](file:///C:/LumoraReader/app/src/main/java/com/mothblank/lumorareader/data/EpubBookReader.kt) to compute chunk boundaries using unzipped chapter file sizes on disk (`chapterFile.length()`) instead of reading their contents, achieving instant document opening times.

## Phase 4: Metadata Parsing & Advanced Widget Styling

### 1. Real EPUB Metadata & Table of Contents Parsing
- **Problem:** Book repository returned placeholder values for EPUB metadata (title, author, table of contents) because `EpubBookReader` lacked XML parsing.
- **Solution:** Modified `BookReader` to implement `Closeable` and refactored `EpubBookReader` to use namespace-independent XML-pull metadata parsing. Added custom NCX (EPUB 2) and Nav XHTML (EPUB 3) parser routines that map reference paths to reading order spine indices.

### 2. Premium Widget Personalization
- **Problem:** Home screen widgets lacked customization controls like font sizes, customized typefaces, or background opacities.
- **Solution:** Extended preference storage datastore with `fontSize`, `fontFamily`, and `transparency`. Redesigned configuration UI with responsive sliders and chip selectors, and updated `QuoteWidget` content layouts to dynamically render and paint layout custom designs based on instance settings.

## Phase 5: Visual Polish, Accessibility & Theme Alignment (June 22, 2026)

### 1. Explicit Back Navigation
- **Problem:** Navigation locks occurred on screens like `LanguageScreen` (due to missing top bars) or lacked back routes in top app bars.
- **Solution:** Wired the back navigation icon in `QuotesScreen.kt` using `navController.popBackStack()` and fully enabled secure exit routes across all secondary screens.

### 2. Form Guidance & Inputs Accessibility
- **Problem:** Forms and search inputs (e.g. quote text fields, filter search bar) lacked placeholders to guide user input.
- **Solution:** Added dedicated string resources in `strings.xml` and passed `placeholder` parameters into text fields in `EditQuoteDialog.kt` and `QuotesScreen.kt`.

### 3. Widget Config Screen Theme Integration
- **Problem:** `QuoteWidgetConfigActivity.kt` used standard `MaterialTheme`, completely ignoring the user's custom-selected theme.
- **Solution:** Injected `UserPreferencesViewModel` to retrieve the user's active theme and dynamically wrapped the config screen in `LumoraTheme`. Defaulted the selected theme chip to the user's active theme.

## Phase 6: Security Hardening & Input Sanitation (June 22, 2026)

### 1. Quote Input Length Restrictions
- **Problem:** Unrestricted quote inputs could allow excessive data to be stored, resulting in local performance denial of service (DoS), UI layout breaking, or database bloat.
- **Solution:** Enforced a robust 2000-character limit on quote text and a 100-character limit on the author field in [EditQuoteDialog.kt](file:///C:/LumoraReader/app/src/main/java/com/mothblank/lumorareader/presentation/quotes/components/EditQuoteDialog.kt), paired with Material Design 3 supporting text counters.

### 2. Isolated FileProvider Sandbox Boundary
- **Problem:** The app's `FileProvider` in `provider_paths.xml` shared `path="."` of the cache directory, granting access to the entire app cache directory (violating the Principle of Least Privilege).
- **Solution:** Refactored [EpubBookReader.kt](file:///C:/LumoraReader/app/src/main/java/com/mothblank/lumorareader/data/EpubBookReader.kt) to extract and render temporary HTML chunks within an isolated `epub_cache` subdirectory inside the cache folder. Tightened [provider_paths.xml](file:///C:/LumoraReader/app/src/main/res/xml/provider_paths.xml) to share only `path="epub_cache"`.

### 3. Log Information Leak Prevention
- **Problem:** Logging failure to parse EPUB metadata dynamically from zip stream printed the full local file URI, potentially leaking sensitive user paths (such as directories containing the user's name) in warning/error log reports.
- **Solution:** Updated [BookRepositoryImpl.kt](file:///C:/LumoraReader/app/src/main/java/com/mothblank/lumorareader/data/repository/BookRepositoryImpl.kt) to use `fileName` instead of the full URI path during error reporting.

## Build & Verification Commands
- **Run unit tests:** `.\gradlew test`
- **Build debug APK:** `.\gradlew assembleDebug`

