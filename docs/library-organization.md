# Library organization — Phase 8

The library remains a single source of truth in Room's `reading_history` table. New metadata lives on the existing URI-keyed book rows, so opening a document cannot create a duplicate library record.

## Persistent fields

Room 8 adds three fields via `MIGRATION_7_8`:

| Column | SQLite | Default | Meaning |
| --- | --- | --- | --- |
| `isFavorite` | INTEGER NOT NULL | 0 | Manually toggled favorite |
| `readingStatus` | TEXT NOT NULL | 'READING' | One of `WANT_TO_READ`, `READING`, `ON_HOLD`, `FINISHED` |
| `collectionName` | TEXT nullable | NULL | Trimmed user-defined collection name, maximum 80 characters |

The migration is registered in both `AppDatabase.getDatabase()` and the Hilt `DatabaseModule`. It does not alter or drop reading positions, EPUB anchors, bookmarks, or quotation data.

Status is a user decision. Completing 100% of a document does not silently change `readingStatus`, and reopening a finished book does not reset it.

Collections are simple optional **labels** associated with books. They can be created by typing a new name when organizing a book and are discoverable while at least one book uses the label. Empty standalone collections and shared/synced shelves are not included in this phase.

## Persistence boundary

`RecentBooksRepository` preserves prior favorites, explicit status, collection, reading progress, cover and author metadata when a book is re-opened. All edits route through narrowly scoped DAO updates by URI, avoiding accidental overwrites of progress or reading position.

Legacy SharedPreferences data imported with Gson predates these fields. Missing status values are treated as `READING` when converted rather than dropping that legacy entry.

Removing a book from the library removes only the `reading_history` row. It does **not** delete the original EPUB/PDF, linked quotations, or previously saved source-specific bookmarks. Reopening the same URI can therefore restore those independent annotations.

## Presentation

`BooksScreen` owns ephemeral search, filter and sort controls; `BooksViewModel` exposes the canonical Room-backed list and mutation commands. `BookLibraryComponents.kt` owns cards, statistics and the edit dialog.

- Search matches title, author and collection name.
- Filters combine favorites, explicit reading status and collection (including unassigned books).
- Sorts include recently opened, title and descending reading progress.
- Cover thumbnails use the existing Coil Compose dependency and only the stored local cover URI; they do not search external web sources.
- Removing a book requires confirmation; the reader is disabled for entries marked unavailable.
- Material 3 surfaces, typography and colors come from the existing active theme.

New strings are provided in English and Brazilian Portuguese. Other supported languages use the English resource fallback pending translations.

The changes have been inspected at source level only; Android compilation and device checks were not executed.
