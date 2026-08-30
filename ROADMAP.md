# GameBox OS Roadmap

The development blueprint is guidance. Work proceeds in small, testable increments while preserving one vertical slice.

## 0.1 - Day-zero shell

- [x] Android and Compose project
- [x] GameBox dark TV theme
- [x] Controller-focusable cards
- [x] Home, Library, Store, Details, and Downloads mock screens
- [x] Fixture catalog and explicit install states
- [x] LB/RB tab switching and controller Back handling
- [x] Fake install, pause, resume, verify, and install transitions
- [x] CI-built debug APK with SHA-256 artifact
- [ ] Physical controller check on Galaxy A53
- [x] Per-tab game-card focus restoration, including off-screen LazyRow items
- [x] Focus-memory policy unit tests
- [x] Compose navigation instrumentation tests

## 0.2 - Persistent catalog shell

- [x] Initial Room catalog schema and DAO
- [x] Preferences DataStore singleton
- [x] Application-level repository container and seed catalog
- [x] Cached authorized manifest fixture
- [x] Versioned manifest parser and asset provider
- [x] Duplicate, malformed, and unsupported manifest tests
- [x] Seed timestamp persisted in DataStore
- [x] Database migration and DAO integration tests

  Android instrumentation coverage now exercises the Room DAO against an in-memory database, including rich metadata persistence and state updates.
- [x] Durable Room download-job table and v1-to-v2 migration
- [x] Pause, resume, cancel, and staged-transition tests
- [x] Details and download state restoration across process restart
- [x] Offline refresh policy preserves local state and missing entries

## 0.3 - First vertical slice

Browse -> details -> install an authorized test file -> verify -> Library -> launch one adapter -> return -> retain save record -> uninstall content -> reinstall.

- [x] Deterministic traversal-safe install paths
- [x] Streaming SHA-256 verification primitive
- [x] Save-retaining uninstall planner
- [x] Staged streaming transfer core with cancellation and size limits
- [x] Commit gated behind successful checksum verification
- [x] WorkManager adapter for the bundled authorized test asset
- [x] App-private filesystem staging with atomic promotion
- [x] Wire the authorized test install action and real worker progress into the UI
- [x] Allowlisted RetroArch package adapter and return/play-session tracking
- [x] Reverified, read-only FileProvider content handoff to the emulator adapter
- [x] Replace diagnostic text payload with an authorized runnable homebrew fixture
- [x] Persistent save record retained through content-only uninstall and reinstall
- [x] Restart reconciliation of WorkManager success against real content and checksum

## 0.5 - Save-safe platform support

- [x] App-private staged save backup and restore
- [x] Backup checksum sidecar and tamper rejection
- [x] Emulator capability registry and per-game adapter
- [x] Conservative content-only uninstall service
- [x] User-controlled Storage Access Framework backup import/export
- [x] Controller-visible save operation success and failure feedback

## 1.0 - Target hardware release

Galaxy S23 Ultra and DeX validation, controller and HDMI reconnect tests, external-storage safety, soak testing, signed build, rollback procedure, and documented hardware handoff.


## Latest verification

- [x] Blueprint-aligned focus, hover, press, and elevation motion on game cards
- [x] Optional HTTPS artwork and rich catalog metadata fields
- [x] TheGamesDB metadata enrichment adapter (API key supplied out-of-band)
- [x] Catalog artwork URL validation tests
- [x] Clarified emulator content-handoff failure messaging
- [x] Bundled authorized Galaxy Patrol NES fixture with pinned checksum and emulator-core requirement

Physical Galaxy/DeX/controller testing, production signing credentials, and enclosure validation remain hardware/deployment gates rather than locally verifiable code tasks.
