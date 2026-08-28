# GameBox OS Roadmap

The development blueprint is guidance. Work proceeds in small, testable increments while preserving one vertical slice.

## 0.1 - Day-zero shell

- [x] Android and Compose project
- [x] GameBox dark TV theme
- [x] Controller-focusable cards
- [x] Home, Library, and Store shells
- [x] Fixture catalog and explicit install states
- [x] Starter domain test
- [ ] Physical controller check on Galaxy A53
- [ ] Compose focus and navigation tests

## 0.2 - Persistent catalog shell

Room schema, DataStore preferences, cached manifest fixtures, details and downloads screens, offline-first repository tests.

## 0.3 - First vertical slice

Browse -> details -> install an authorized test file -> verify -> Library -> launch one adapter -> return -> retain save record -> uninstall content -> reinstall.

## 0.5 - Save-safe platform support

Save backup and restore, emulator capability registry, per-game adapters, conservative uninstall service, and user-controlled backup provider.

## 1.0 - Target hardware release

Galaxy S23 Ultra and DeX validation, controller and HDMI reconnect tests, external-storage safety, soak testing, signed build, rollback procedure, and documented hardware handoff.
