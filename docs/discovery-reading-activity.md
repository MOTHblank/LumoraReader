# Phase 9 — Unified discovery and reading activity

## Unified search

The `search` navigation destination is available from Home, Library and
Quotations. Its model combines three authoritative sources:

- Books come from `RecentBooksRepository.observeRecentBooks()`, with local
  search across title, author and collection. Favorite and recent order are
  used for prioritization.
- Quotations come from `QuoteRepository.searchAll()` (database-filtered,
  capped at 40). Text, authors, source title, personal notes and categories
  are searchable. Manual/unlinked quotes remain included.
- Bookmarks come from `ReadingBookmarksRepository.searchAll()`
  (database-filtered, capped at 40). Titles and personal notes are searchable.

Database search uses `instr(lower(column), lower(:query))`, so literal percent
and underscore characters are not interpreted as SQL LIKE wildcards. The UI
debounces queries and caps rendered results. Empty search displays recent
items.

A linked quote opens its exact source through the pre-existing optional
`quoteId` reader argument. Unlinked quotes go to the quotation library with
a prefilled text search.

A bookmark opens
`reader/{bookUri}?bookmarkId={bookmarkId}`. The reader checks ownership
using `ReadingBookmarksRepository.getForBook(id, uri)` before applying
the EPUB anchor/chunk or PDF page; a bookmark ID is never enough to change
the source URI. Old reader and quotation URLs remain usable.

## Reading activity

Room v9 introduces `reading_sessions` with the book URI, wall-clock session
start and monotonic foreground duration in milliseconds. Schema migration
`MIGRATION_8_9` is registered by both database construction paths and does
not alter or replace previous history, bookmarks or quotations.

The reader starts a segment only once the book has loaded and the screen
lifecycle is STARTED. It closes the segment at STOP or disposal, computes
elapsed time using `SystemClock.elapsedRealtime()`, and writes a capped
duration. Session writes are best-effort and segments shorter than one second
are omitted. Abrupt process termination may lose an unfinished segment.
Foreground time is **not** active attention, reading speed or a streak.

The discovery screen reports the recorded total and last-seven-days
foreground minutes and session count. The Home screen shows a compact
seven-day summary, and the per-book notebook shows recorded minutes
and sessions for that book. Historical activity before this version
cannot be reconstructed and is not invented.

All labels introduced in this phase have English and PT-BR resources.
Other locales use Android's default-resource fallback.

## Boundaries

- No external search API or internet content index.
- No copies of book contents are written to the search tables.
- No background clock or periodic polling for activity. The database updates
  when foreground segments end.
- Database search is bounded by query limits; for substantially larger
  libraries, a future version could use a dedicated SQLite FTS table.
- The original EPUB and PDF readers and their cache budgets remain untouched.

Source review is not Android compilation, migration execution or device
validation. These remain deliberately unrun in this phase.
