# Phase 10 — Theme-safe interaction polish

Phase 10 is deliberately a **conservation pass**. LumoraReader's existing visual identity, animation style, and multi-theme system are product features; changes here improve legibility, affordances, and edge cases without replacing them.

## Protected visual contracts

1. **The home screen remains the animated floating-bubble layout.** Its four asymmetric menu bubbles, offsets, motions, palette-derived backgrounds, gloss and halo particles are unchanged at unconstrained sizes. The geometry scales uniformly only when a screen is narrower/shorter than the original design's available area.
2. **Quote cards retain their individual quote-theme palette.** `EnhancedQuoteCard` continues using `theme.toColorScheme()` and `representativeColor` rather than forcing the app's currently selected theme on every quote.
3. **Reader quick actions and theme choices remain bubble controls.** This phase changes spoken labels and selected-state announcements, not their iconography or layout.
4. **Library cards retain Material 3 surfaces, color roles, shapes and control order.** Only their action labels are shortened on compact widths to prevent crowding.
5. All new strings use **English defaults and PT-BR translations**; no hardcoded light/dark colors are introduced.

## Functional refinements

- A shared `InteractiveBubble` now places its spoken label on the clickable card and marks its icon as decorative, reducing duplicate TalkBack announcements.
- The home bubble layout uses parent constraints to scale on compact devices while retaining the existing color and movement formulas.
- The quote theme filter is populated from the distinct stored themes, independently of the current filtered result set. Searching for a particular theme no longer makes other filter choices disappear.
- The quotation screen exposes a compact active-filter summary and a one-action clear, plus a distinct no-match state. Existing card styling and the staggered grid stay intact.
- Quote-card labels (expand/collapse, page, notes, source jump and actions) are localized; text expansion state resets when a different quote occupies a reused card.
- Quote-card footers keep the original three icon actions when space allows. On compact two-column cards, those actions move into a labeled overflow menu so the author is still readable; the quote-specific color scheme and card styling do not change.
- Reader quick-action descriptions and theme selected states are localized for screen readers.

## Non-goals

No new themes, changes to `LumoraTheme` or `toColorScheme`, altered reading-progress storage, new onboarding screens, new animations, database schema changes, third-party UI libraries, or navigation redesigns.

## Validation scope

Check source-level signatures, localized resources, and diff scope before merging. As requested, no builds, tests or GitHub Actions are run. Android layout, touch targets and TalkBack should be verified locally during the user's normal device workflow.
