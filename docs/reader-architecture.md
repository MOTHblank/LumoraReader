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
