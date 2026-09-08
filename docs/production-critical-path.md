# Production critical path — 8 September 2026

Implementation and validation are tracked separately. Green UI/build checks do
not prove working emulators, providers, saves or release signing.

| Priority | Work | Current evidence / next gate |
| --- | --- | --- |
| P0 | Honest first-run library and installation state | Merged in PR #267 after Android unit/instrumentation, phone/DeX screenshot and Windows build checks passed. Catalog claims no longer create installed or queued games. |
| P0 | One complete authorized game journey | Galaxy Patrol is bundled and hash-pinned. Install → real RetroArch execution/input → save → return → uninstall/reinstall preservation is not yet proven end-to-end. |
| P0 | Reproducible signed release and upgrade | Debug APK and API-35 UI tests pass. Production signing, clean install/upgrade and rollback validation remain separate gates. |
| P1 | Remove simulated installation controls | Merged and automated checks passed in PR #267. Manual completion APIs removed; fake repository moved to unit-test sources. Details pause/resume uses the real download controller; in-progress actions open Downloads. Unavailable-source jobs report recovery guidance rather than simulate progress. |
| P1 | Durable download updates | PR #269 merged after all five checks passed; instrumentation completed 11 tests with zero skips/failures. Ordered Room writes read persisted rows, not the UI snapshot. Remote WorkManager startup reconciliation remains a separate gap. |
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
library availability while observing actual completed work; validation pending.
