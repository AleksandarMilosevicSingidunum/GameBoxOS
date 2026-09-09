# General content removal — work in progress

Addresses the audit P1 general game-ID uninstall gap. This branch follows the
validated and merged session-recovery PR #278. Uninstall acceptance remains pending.

- Recorded multi-file imports, provider downloads and bundled Galaxy Patrol use
  one game-owned exact-file remover. It rejects cross-game paths, traversal,
  symbolic links and directory deletion; validates the entire set before deletion.
- SaveSafetyController resolves the current game's manifest, rechecks its state
  and manifest before removal, and retains saves, backups, metadata and history.
  Partial filesystem failure marks content missing and offers retry. The production
  Room state write is awaited before reporting successful removal.
- Game details exposes a confirmation for other installed/imported games too.
  Preview runs off the UI thread and reports actual recorded-file count and bytes.
  Launch preparation/active handoff/recovery errors disable the removal buttons.
- Three local JUnit tests passed: multi-file removal/retention/retry, invalid later
  entry preventing earlier deletion, downloaded and bundled ownership paths.

Added Android tests exercise the production controller with Room, multi-file removal,
actual disc-set reimport/registration, save/history preservation and symlink refusal.
Their content/save bytes are synthetic, not emulator gameplay evidence. CI pending.

Pending: full Android build and runtime confirmation tests, operation concurrency with
import/download workers and real emulator save
retention/reimport/reinstall UAT. This is not complete uninstall lifecycle acceptance.

Legacy Galaxy-only APIs remain for existing lifecycle tests; production details
is being moved to the general confirmation. No user game files were deleted during
development; tests remove only their own temporary fixtures.
