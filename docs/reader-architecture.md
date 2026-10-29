# Reader presentation architecture

This document describes the Phase 5 split. It is a design contract for future
changes, not a replacement for the code.

## Boundaries

| Source | Responsibilities | Must not own |
| --- | --- | --- |
| `ReaderScreen.kt` | Top-level Compose screen, reader overlays, selection actions, settings/contents dialogs, orientation restoration | PDF bitmap caches, EPUB HTML generation, persistence writes |
| `ReaderControls.kt` | Shared navigation, EPUB typography/theme/orientation controls, accessible selection sliders | Document loading or view-model state mutation except through callbacks |
| `EpubReaderContent.kt` | WebView lifecycle and JavaScript bridge for text-relative anchors, selection and quote highlighting | Room history, chapter cache ownership, primary navigation state |
| `PdfReaderContent.kt` | Lazy PDF page layout, touch gestures, accessible zoom buttons and page navigation | Native PdfRenderer resources or bitmap caches |
| `ReaderViewModel.kt` | Session lifecycle, saved positions, EPUB chunk reloads, PDF render requests/cache budget, quote creation and user preference coordination | Composable layout or platform-specific visual controls |
| `ReaderUiState.kt` | Immutable presentation contract | Side effects or repository access |
| `QuoteThemeClassifier.kt` | Deterministic optional quote theme suggestion | Replacing explicit user category/theme choices |

The reader deliberately retains the existing `PdfBookReader` and
`EpubBookReader` implementations. No new third-party viewer framework was
introduced during the extraction.

## EPUB lifecycle

1. A book is opened and its reading history and optional quote destination are resolved.
2. `ReaderViewModel` obtains or generates a bounded EPUB HTML chunk.
3. `EpubScrollReader` creates a WebView keyed to its generated URI; it installs
   the bridge before loading and guards callbacks from retired URIs.
4. JavaScript restores a spine/text anchor, then falls back to fractional or
   legacy pixel positioning when an anchor is missing.
5. Position callbacks return to `ReaderViewModel`, which persists updates
   on a debounce. Only the VM may update reading history.
6. When a chunk is retired, disposal releases the WebView and the VM may
   evict the generated file once no active view uses it.

Never disable the current URI check on bridge callbacks, or replace the
WebView key with a singleton WebView shared across generated HTML files.

## PDF rendering

The ViewModel owns bitmap cache budgets, stale job cancellation and the
zoom-tile request generation. `PdfReaderContent` only displays the current
bitmap/tile and sends interactions back to the VM. All zoom controls (two-finger
gestures and explicit buttons) invoke the same bounded scale and pan API.

Do not keep additional strong bitmap references in navigation components.
Screen rotation changes the requested viewport dimensions and invalidates the
appropriate rendered pages.

## Accessibility

- Every icon-only reader action must have an explicit spoken label.
- Navigation and font-size sliders must have an accessible name in addition
  to their value, not only adjacent visual text.
- PDF zoom must remain usable without a two-finger gesture.
- A discoverable settings control must remain available without tapping book
  content to toggle the menu.
- Reader progress must be understandable when read aloud.
- Reusable strings are in `res/values/reader_accessibility.xml`; PT-BR
  equivalents are in `res/values-pt-rBR/reader_accessibility.xml`. Other
  locales fall back to the default resources until translated.

## Stability constraints

Preserve Room migrations, EPUB content anchors, PDF page-level bookmarks,
quotation source links and the existing file URI/file-provider behavior.
Generated content is untrusted and must not gain unrestricted file/network
access through WebView settings.

Android build and on-device accessibility behavior require subsequent
verification; source-level review alone cannot establish those results.


## Phase 6: reading presentation preferences

Reader appearance is split into two layers:

- Persistent presentation settings live in `UserPreferencesRepository` (DataStore).
  EPUB line spacing is bounded to 1.4–2.2, margins are a whitelist of
  Compact/Comfortable/Wide, PDF night colors are a boolean, and PDF page
  spacing allows 8 or 24 dp. Existing font and theme settings are unchanged.
- `ReaderUiState.focusMode` is session-only and deliberately not persisted.
  It hides normal overlays without disabling text selection or document access.
  Users exit with a page tap, the Back action, or an accessible exit control.

EPUB layout changes regenerate only the currently requested HTML chunk. The
cache key includes line spacing and margin settings, preserving the cached
versions of other presentations for nearby navigation. The existing
spine-relative anchor remains the source of truth for restoring a location
after reflow. Font-size and line-spacing sliders commit only after dragging
finishes to avoid rapid repeated WebView recreation.

PDF night rendering is a draw-time inversion color filter applied to both
normal pages and zoom tiles. The `PdfRenderer` configuration, native page
geometry, and cached bitmap bytes do not change. This treatment also inverts
photographs and illustrations; it is a display preference rather than a
photographic color-preservation feature.

Both customization resource files have English and PT-BR strings, with default
resource fallback for other locales.

The main reader surfaces call `onReaderSurfaceTapped()` rather than toggling
settings directly, so an existing focus session always exits cleanly before
a settings sheet can open.


## Phase 7: book notebook and durable annotations

The reading notebook is available from the reader toolbar and the reader
settings sheet. It reports the active document's progress, saved location
bookmarks, annotations, and recent source-linked quotations.

- `ReadingBookmarkEntity` is keyed by an auto-generated ID and contains a
  required `bookUri`, label, optional note, and **either** an EPUB
  chunk/text-relative anchor **or** a zero-based PDF page index.
- `ReadingBookmarkDao` filters all update/delete operations by the source URI,
  protecting bookmarks from accidental cross-book updates. Bookmarks are kept
  in their own `reading_bookmarks` table; this intentionally avoids touching
  the existing `quotes` and `reading_history` rows.
- Room schema version 7 introduces that new table and its URI index through
  `MIGRATION_6_7`. Both `AppDatabase.getDatabase()` and the Hilt provider
  register the complete migration chain through 7. Do not use a destructive
  fallback or change an existing database filename during future migrations.
- `ReadingBookmarksRepository` is responsible for persistence and mapping;
  `ReaderViewModel` owns the active document, location selection, and
  cancellation of per-book Flow collectors.
- EPUB bookmark jumps use the original spine/character offset when present,
  regenerated through the established WebView lifecycle. Unanchored legacy
  entries return to the start of their saved chunk instead of restoring a
  previous unrelated viewport location.
- PDF bookmark jumps use an existing page-navigation path, preserving
  resolution-aware rendering and the cache budget.
- The notebook shows up to 30 recent book quotations for responsive rendering;
  the full quotation library remains the canonical place for managing all
  quotations. Book-linked filtering uses the exact `sourceUri`, not a title.
  Unlinked older or manually entered quotations remain visible under
  **All books**, but cannot be reliably assigned to a source automatically.

The notebook is a reader presentation component; it does not open its own
independent book reader or retain large page bitmaps.

Compile and device validation were deliberately left to the project's
normal local validation workflow. Source inspection is not equivalent to
an Android build.
