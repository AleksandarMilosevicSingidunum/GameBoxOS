# Metadata provider refactor

GameBox now uses a keyless, metadata-only Wikidata fallback when enriching catalog titles.
The enrichment path no longer depends on a TheGamesDB API key or on Vimm page markup:

1. Search Wikidata for an exact normalized title.
2. Read the matching entity's English description, original release year, and cover image
   claim when available.
3. Merge only missing GameBox fields, preserving catalog data and never creating a binary
   source URL.
4. Cache the result for the current process so a catalog refresh does not repeatedly query
   the same title.

The metadata editor's explicit **Find metadata match** flow now asks Wikidata and Libretro
for candidates; the user must confirm a result before provider metadata changes. The
obsolete TheGamesDB API-key catalog sync, health tracking, and request/parser code have
been removed. Existing Room platform IDs are retained so upgrades do not discard cached
catalog data. Vimm remains a discovery/navigation source and is not treated as a metadata API.

Wikidata and Wikimedia Commons are public services, so production clients must continue to
use bounded HTTPS requests, exact-title matching, cancellation, and graceful fallback. A
missing or ambiguous Wikidata result leaves the original catalog metadata unchanged.

Validation: `:app:testDebugUnitTest` and `:app:assembleDebug` passed on 1 October 2026.
The connected compact-TV semantics regression is still a separate hardware-test issue: the
API-35 emulator passed, while Honor instrumentation crashed before reporting an assertion.
