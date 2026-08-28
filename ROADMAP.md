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
- [x] Domain transition tests
- [ ] Physical controller check on Galaxy A53
- [ ] Compose focus restoration and navigation tests

## 0.2 - Persistent catalog shell

- [x] Initial Room catalog schema and DAO
- [x] Preferences DataStore singleton
- [x] Application-level repository container and seed catalog
- [x] Cached authorized manifest fixture
- [x] Versioned manifest parser and asset provider
- [x] Duplicate, malformed, and unsupported manifest tests
- [x] Seed timestamp persisted in DataStore
- [ ] Database migration and DAO integration tests
- [x] Durable Room download-job table and v1-to-v2 migration
- [x] Pause, resume, cancel, and staged-transition tests
- [x] Details and download state restoration across process restart
- [x] Offline refresh policy preserves local state and missing entries

## 0.3 - First vertical slice

Browse -> details -> install an authorized test file -> verify -> Library -> launch one adapter -> return -> retain save record -> uninstall content -> reinstall.

## 0.5 - Save-safe platform support

Save backup and restore, emulator capability registry, per-game adapters, conservative uninstall service, and user-controlled backup provider.

## 1.0 - Target hardware release

Galaxy S23 Ultra and DeX validation, controller and HDMI reconnect tests, external-storage safety, soak testing, signed build, rollback procedure, and documented hardware handoff.
