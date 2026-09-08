# Production critical path — 8 September 2026

Implementation and validation are tracked separately. Green UI/build checks do
not prove working emulators, providers, saves or release signing.

| Priority | Work | Current evidence / next gate |
| --- | --- | --- |
| P0 | Honest first-run library and installation state | Merged in PR #267 after Android unit/instrumentation, phone/DeX screenshot and Windows build checks passed. Catalog claims no longer create installed or queued games. |
| P0 | One complete authorized game journey | Galaxy Patrol is bundled and hash-pinned. Install → real RetroArch execution/input → save → return → uninstall/reinstall preservation is not yet proven end-to-end. |
| P0 | Reproducible signed release and upgrade | Debug APK and API-35 UI tests pass. Production signing, clean install/upgrade and rollback validation remain separate gates. |
| P1 | Remove simulated installation controls | Merged and automated checks passed in PR #267. Manual completion APIs removed; fake repository moved to unit-test sources. Details pause/resume uses the real download controller; in-progress actions open Downloads. Unavailable-source jobs report recovery guidance rather than simulate progress. |
| P1 | Durable download updates | PRs #269–#271 merged after their checks passed. Ordered Room writes read persisted rows, startup waits for database records, work identity distinguishes fast reinstalls, and remote recovery verifies current content while preserving uninstall state. Live provider transfer/recovery still needs separate evidence. |
| P1 | General game lifecycle | Generalized emulator/save/uninstall adapters need full supported-console validation. Parser support is not emulator compatibility. |
| P1 | Real providers | TheGamesDB transport/enrichment exists; live authenticated discovery/media, limits and failures require provider evidence. |
| P1 | Windows communication journey | Pairing/authentication, transfer/synchronization and recovery require a complete Android↔Windows integration run, not only independent builds. |
| P2 | UI interaction and populated-state coverage | PR #266 passed unit, instrumentation and real phone/DeX screenshot jobs. Populated provider, active downloads, large fonts and physical controller flows need broader coverage. |
| P2 | Hardware and external services | Physical DeX/controller/storage/thermal checks and authenticated cloud endpoints must be validated when resources are available; they do not block independent software work. |

The catalog repair changes database state only for six known legacy demo IDs
without local content references or a real download source. It does not delete
files, save records, favorites, history or emulator preferences. Imported copies,
real remote downloads, Galaxy Patrol and unrelated user records are excluded.

Catalog manifests retain the legacy initialState field for schema compatibility,
but it cannot install or enqueue games. New entries always start NOT_INSTALLED;
verified local operations remain authoritative for subsequent state.

PR #268's real WorkManager/Room/files lifecycle test passed without skipping and
was merged. It covers hash-verified installation, synthetic save backup,
content-only uninstall, reinstall and restore. It does not validate emulator
gameplay or emulator-produced saves.

The follow-up startup fix waits for Galaxy Patrol's database row before applying
restored WorkManager state. Its regression extends the lifecycle test by delaying
library availability while observing actual completed work. Subsequent runs exposed
a fast-reinstall failure: verified content and WorkManager success coexisted with
a QUEUED library row. PR #270 now retains work identity in observable download
state and serializes installation-state writes. Commit 008fd131 passed all 12
API-35 instrumentation tests with zero skips/failures; this is automated lifecycle
evidence, not proof of RetroArch gameplay or emulator-produced saves.

PR #271 additionally defers remote-work reconciliation until both database rows
exist and preserves a completed download's later uninstall across controller
restart. Its injected-work regression is not a live provider transfer test.
PR #272 resolves the actual MediaStore export path rather than guessing a filename
that Android may rename, and marks missing/altered launch content for reinstall
without invalidating valid content when emulator setup fails. PRs #271 and #272
are merged after all five checks passed on each; their API-35 instrumentation runs
completed 13 tests each. MediaStore export validation does not prove RetroArch
execution or resolve every possible black-screen cause.

PR #273 is merged after build, unit, instrumentation and phone/DeX screenshot
checks passed. It replaces production synthetic save creation with a real
document-picker import, rejecting empty/oversized files and existing-save
overwrites. The lifecycle test imports test-only bytes through that API before
backup, uninstall, reinstall and restore. See [managed save import](save-import.md)
for the current Galaxy Patrol scope and the separate emulator synchronization gap.
