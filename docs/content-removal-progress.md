# General content removal — validated software path

Addresses the audit P1 general game-ID uninstall gap. This branch follows the
validated and merged session-recovery PR #278. The production software path is now
validated; remaining process-death and cross-worker stress gates are tracked separately.

- Recorded multi-file imports, provider downloads and bundled Galaxy Patrol use
  one game-owned exact-file remover. It rejects cross-game paths, traversal,
  symbolic links and directory deletion; validates the entire set before deletion.
- SaveSafetyController resolves the current game's manifest, rechecks its state
  and manifest before removal. When a managed save is recorded, it creates and
  verifies a fresh atomic backup before deleting any game content; backup failure
  aborts removal. Saves, backups, metadata and history remain retained. Partial
  filesystem failure marks content missing and offers retry. The production Room
  state write is awaited before reporting successful removal.
- Game details exposes a confirmation for other installed/imported games too.
  Preview runs off the UI thread and reports actual recorded-file count and bytes.
  Launch preparation/active handoff/recovery errors disable the removal buttons.
- Local JUnit coverage passes for multi-file removal/retention/retry, invalid later
  entries preventing earlier deletion, downloaded and bundled ownership paths.

Added Android tests exercise the production controller with Room, multi-file removal,
actual disc-set reimport/registration, save/history preservation, verified pre-removal
backup, fail-closed backup errors and symlink refusal.
Those automated fixtures use synthetic content/save bytes; they are not emulator gameplay
evidence. The complete Android JVM, API-35 instrumentation, and Honor instrumentation
suites have passed for the production paths.
Production-dialog tests additionally cover unsafe preview refusal, cancel without
deletion, a single confirmation call, and disabled actions until removal completes.

The connected Honor Magic6 Pro then supplied runtime acceptance for the user-visible
path: authenticated companion content was imported, launched through real
RetroArch/FCEUmm, returned through RetroArch's own Close Content and Quit actions, and
removed through the production confirmation. The confirmation retained saves, backups,
metadata, history and external emulator files; direct app-sandbox checks confirmed the
49,168-byte ROM was gone while the managed save remained with its expected SHA-256.
The API-35 and Honor instrumentation suites also cover the production controller and
dialog paths. This is complete software uninstall lifecycle evidence for the exercised
single-game path, not proof of process-kill durability or every emulator adapter.

The same physical session also repeated an authenticated companion content upload with
an identical SHA-256. Android kept the installed content state and managed-save presence
stable, after which the replacement-test file was removed through the same confirmation
and verified local-backup path.

Still pending: operation concurrency with import/download workers under stress,
process-death/power-loss recovery, physical picker behavior for reimport, and real
emulator save compatibility across adapters. Those are separate acceptance gates.

Legacy Galaxy-only APIs remain for existing lifecycle tests; production details
is being moved to the general confirmation. No user game files were deleted during
development; tests remove only their own temporary fixtures.
