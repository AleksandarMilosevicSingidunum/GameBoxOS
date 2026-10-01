# GameBox OS Roadmap

The development blueprint is guidance. Work proceeds in small, testable increments while preserving one vertical slice.

## Requirement audit snapshot — 1 October 2026

This snapshot maps the continuing goal to current evidence. A checked item is
implemented and covered by the repository's automated or documented evidence;
hardware, live-provider, and real-emulator claims are kept separate.

### Done and evidenced

- Android/Compose shell, Room catalog state, DataStore settings, WorkManager
  downloads, checksum-gated promotion, legal-copy import profiles, save-safe
  uninstall, SAF save/external-library flows, and Windows companion foundation
  are implemented with JVM/instrumentation coverage.
- The Blueprint TV shell is implemented: 16:9 layout, fullscreen/cutout policy,
  D-pad/LB/RB navigation, focus restoration, Quick Actions, Downloads states,
  safe-area controls, install-to-Library continuity, uninstall confirmation,
  and controller-reachable settings actions.
- TV software evidence is recorded for 1280×720, 1920×1080, and 3840×2160
  profiles. The latest normal-profile `TvActivityTest` run passed `19/19`.
- The legal Galaxy Patrol fixture has a verified TV Store → install → Library
  path. RetroArch handoff from TV `Play Now` has also been observed through the
  real `RetroActivityFuture` activity.

### Implemented, but validation remains partial

- The TV RetroArch return path is implemented in the launch/session controller,
  but the latest API-35 probe hit a SystemUI ANR while RetroArch remained
  top-resumed after Back. Prior Honor evidence is retained separately; a fresh
  emulator return pass is still required.
- Vimm's Lair remains discovery/navigation plus user-initiated browser-download
  tracking. It must never resolve or download binary URLs itself. Live browser,
  DownloadManager completion, legal import, and physical-TV validation remain
  separate evidence gates.
- PPSSPP/Dolphin and other adapter-specific gameplay/save flows, live
  WebDAV/S3 endpoints, physical accessibility, and production Windows install
  are not yet complete acceptance evidence.

### Remaining software work, ordered by impact

1. Restore the API-35 TV emulator endpoint and close the TV Play Now → emulator
   exit → GameBox return regression; keep the test authoritative rather than
   using a forced stop as a substitute for Back/exit.
2. Add compact-TV semantics coverage for secondary Details actions, including
   authorized import, browser-download tracking, uninstall, and save actions.
3. Validate the legal import lifecycle through Android DocumentsUI on a TV-sized
   surface, including ZIP/7z extraction, source cleanup, managed directory
   placement, uninstall cleanup, and reimport identity preservation.
4. Validate at least one additional real emulator adapter and adapter-specific
   save recovery; retain explicit missing-core/failed-handoff behavior.
5. Complete live authenticated provider/cloud-save endpoint validation, clean
   checkout CI/release gates, signed artifacts, update-channel and rollback
   execution, and Windows production installation/upgrade evidence.

### Hardware/live gates that do not block software work

- Physical controller/reconnect and long-duration Honor Desktop/TV soak.
- Honor OEM fullscreen/freeform framing, HDMI overscan, Ethernet/PD/thermal,
  external-storage unplug recovery, and physical TV screenshots.
- Real provider credentials/endpoints, commercial-game compatibility, and
  physical emulator gameplay/save evidence.

## 0.1 - Day-zero shell

- [x] Android and Compose project
- [x] GameBox dark TV theme
- [x] Controller-focusable cards
- [x] Home, Library, Store, Details, and Downloads mock screens
- [x] Fixture catalog and explicit install states
- [x] LB/RB tab switching and controller Back handling
- [x] Fake install, pause, resume, verify, and install transitions
- [x] CI-built debug APK with SHA-256 artifact
- [ ] Physical controller check on Honor Magic5 Pro
- [x] Per-tab game-card focus restoration, including off-screen LazyRow items
- [x] Focus-memory policy unit tests
- [x] Compose navigation instrumentation tests
- [x] Clean API-35 emulator CI execution of Compose, Room, migration, lifecycle, navigation, and accessibility instrumentation tests

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

Honor Magic5 Pro wired external-display/Honor Desktop Mode validation with the purchased UGREEN 7-in-1 hub, controller and HDMI reconnect tests, external-storage safety, soak testing, signed build, rollback procedure, and documented hardware handoff.


## Latest verification

- [x] Blueprint-aligned focus, hover, press, and elevation motion on game cards
- [x] Optional HTTPS artwork and rich catalog metadata fields
- [x] TheGamesDB metadata enrichment adapter (API key supplied out-of-band)
- [x] Catalog artwork URL validation tests
- [x] Clarified emulator content-handoff failure messaging
- [x] Bundled authorized Galaxy Patrol NES fixture with pinned checksum and emulator-core requirement

Physical Honor Magic5 Pro/external-display/controller testing, production signing credentials, real provider/emulator integration, and enclosure validation remain hardware/deployment gates rather than locally verifiable code tasks. A clean API-35 emulator now executes the Android instrumentation suite in CI; this does not replace physical-device acceptance testing.

