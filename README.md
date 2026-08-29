# GameBox OS

GameBox OS is a controller-first Android living-room shell for a docked phone. It presents one coherent interface for a local game library, authorized or homebrew catalog, media launchers, streaming tools, and Samsung DeX.

GameBox is an Android/DeX application, not a custom ROM or emulator. Game sources must be user-owned backups, homebrew, freeware, open-source, or otherwise authorized content. An optional native Windows Companion is provided for local Windows-library launching; it does not replace the Android/DeX shell.

## Current development status

This repository is an active pre-1.0 implementation of the August 2026 **GameBox Development Blueprint**. The blueprint is a six-month working plan, not a claim that every item is already complete.

Status as of 29 August 2026:

| Blueprint area | Status | Current implementation |
| --- | --- | --- |
| Android/Compose shell | Implemented | Dark GameBox theme, phone portrait/landscape and large DeX layouts |
| Windows companion | Implemented foundation | Native .NET 8/WPF local-library launcher with search, favorites, atomic JSON state, safe file validation, and self-contained win-x64 CI artifact |
| Controller navigation | Partial | Focusable cards, LB/RB tabs, Back/B handling, and focus restoration; physical-device and reconnect testing remain |
| Primary UI | Implemented foundation | Home, Library, Store, Details, Downloads, Media, PC Hub, and Settings |
| Catalog and discovery | Implemented foundation | Room-backed catalog, search, platform/genre filters, favorites, cached authorized manifests |
| Remote providers | Partial | Configurable HTTPS catalog with strict validation, offline cache, out-of-band credential abstraction, optional Basic auth, safe WebDAV/S3 URI builders, and bounded transport client; S3 request-signing boundary with AWS Signature V4 and authenticated transport wiring; WebDAV Basic auth hardens incomplete credentials; shared bounded recovery classification covers auth, rate limits, transient network, and permanent failures |
| Downloads | Implemented | Durable WorkManager jobs, notifications, measured speed and ETA, preemptive low-storage warnings using the worker reserve, pause/resume, Range validation, retries, cancellation, size limits, SHA-256 verification, and atomic install |
| Install/uninstall | Partial | App-private verified install plus explicit save-safe uninstall confirmation with exact freed and retained byte counts; generalized production content adapters and physical storage validation remain |
| Saves | Partial | Platform save-adapter registry, real directory discovery, reactive per-game save presence in Details, multi-artifact backup/restore, atomic checksum-protected snapshot manifests, import/export, retention, deterministic sync conflict resolution, and cross-game safety checks; authenticated cloud synchronization and physical emulator validation remain |
| Emulator integration | Partial | Allowlisted emulator handoff with per-game package selection and graphics profile persistence exposed in Details, read-only FileProvider access, return tracking, and capability registry; production adapter validation remains |
| Media and PC | Partial | Installed-app detection, typed availability states, safe launch shortcuts, persistent hide-unavailable policy, responsive empty/setup states, Moonlight connectivity status, bounded host reachability probes, and recent sessions for media, Moonlight, Winlator, Termux, Files, browser, and Android settings; physical streaming validation remains |
| Offline operation | Partial | Cached catalog and persistent local state; full airplane-mode acceptance testing remains |
| Diagnostics | Partial | Sanitized export report, bounded structured event collection, lifecycle wiring, and visible download errors; full recovery bundles and physical failure validation remain |
| CI and releases | Partial | Unit tests, debug APK builds, SHA-256 artifacts, and alpha release workflow; signed production builds, rollback, and update channels remain |
| External storage | Partial | SAF folder selection, persisted permissions, read/write status, non-destructive document-tree copy execution with explicit confirmation states, safe partial-file finalization, disconnect detection, and retryable outage classification; physical unplug testing remains |
| Target hardware | Not validated | Galaxy A53 controller testing and Galaxy S23 Ultra/DeX/HDMI/Ethernet/thermal/SSD soak testing require physical hardware |
| Enclosure/handoff | Not started | Hardware enclosure, hub, cooling, cabling, and recovery-button work follows software maturity |

## Implemented highlights

- Kotlin, Jetpack Compose, Room, DataStore, and WorkManager
- Responsive normal-phone and 16:9 living-room layouts
- Persistent catalog, install state, favorites, play history, downloads, and save records
- Authorized HTTPS catalog configuration with bounded responses and atomic offline caching
- Verified remote download pipeline with true pause/resume and safe job-scoped cleanup
- App-private staging; content is promoted only after complete SHA-256 verification
- Download notifications, byte progress, measured speed/ETA, low-storage warnings, failure reasons, retry, and cancellation
- Save-safe uninstall, reactive per-game save presence, durable multi-artifact snapshot manifests, and user-controlled backup import/export
- Configurable Media/PC launch hubs with installed-only filtering, Moonlight network status/recent sessions, and Android system-setting shortcuts
- SAF external-library selection with persisted permissions, explicit migration confirmation gating, and retryable disconnect/read-only status
- Sanitized diagnostics export that excludes credentials, source URLs, checksums, paths, and save contents
- Automated unit tests, debug APK builds, and self-contained Windows Companion builds
- Friendly emulator selection with validated graphics profiles, launch-time profile handoff, and unsupported-platform guidance

## Important remaining work for Blueprint 1.0

- Replace the diagnostic text payload with an authorized runnable homebrew fixture
- Validate at least one real emulator adapter for every officially supported platform group
- Validate additional production emulator adapters and apply graphics profiles to adapter launch arguments
- Use the recovery policy in provider transports and complete end-to-end authenticated WebDAV/S3 integration testing
- Complete authenticated cloud save synchronization and validate real save adapters against production emulators
- Complete physical disconnect/unplug safety tests for confirmed SAF migrations
- Add Compose instrumentation, accessibility, lifecycle, migration, offline, and failure-recovery tests
- Complete physical controller testing on Galaxy A53
- Complete Galaxy S23 Ultra DeX/HDMI/Ethernet/charging/thermal/reconnect soak testing
- Add signed production builds, update/rollback procedures, and recovery documentation
- Complete the physical enclosure and hardware handoff
- Expand the optional Windows companion with catalog synchronization and richer PC-runtime integrations
- Validate host reachability and physical LAN/controller streaming

## Blueprint implementation backlog

The remaining work is tracked in these concrete groups:

- Complete end-to-end authenticated WebDAV/S3 provider recovery and integration testing.
- Verify disconnect recovery and add Compose coverage for confirmed SAF migration flows.
- Validate adapter-specific save discovery/import/export against production emulators and add authenticated cloud synchronization.
- Apply graphics profiles to adapter-specific launch arguments where supported.
- Add Compose instrumentation, accessibility, lifecycle, migration, offline, and recovery tests.
- Complete physical controller, DeX, HDMI, Ethernet, charging, thermal, SSD, and unplug/reattach validation.
- Add production signing, update channels, rollback/recovery procedures, and release documentation.
- Complete enclosure, cooling, cabling, and hardware handoff work.

## Build and test

Open the repository root in the current stable Android Studio, install Android SDK 36, sync Gradle, and run the app on an emulator or Android phone.

CI runs:

```text
testDebugUnitTest
assembleDebug
```

The optional Windows Companion runs its core test harness and publishes a self-contained `win-x64` ZIP from `.github/workflows/windows-ci.yml`.

Each CI run produces a temporary debug APK and SHA-256 artifact. Release APKs are debug-signed development builds until production signing is configured.

## Safety invariants

- Never install a remote file before checksum verification succeeds.
- Never follow untrusted catalog or download redirects.
- Never allow catalog credentials inside URLs; resolve complete credential pairs through the credential store, redact them in diagnostics/logging, and send only over HTTPS.
- Never let an install path escape GameBox app-private storage.
- Cancel removes only that job's partial content.
- Uninstall keeps saves, metadata, favorites, and play history by default.
- Diagnostics exclude credentials, remote paths, checksums, file paths, and save contents.
