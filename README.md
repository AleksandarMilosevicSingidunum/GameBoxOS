# GameBox OS

GameBox OS is a controller-first Android living-room shell for a docked phone. It presents one coherent interface for a local game library, authorized or homebrew catalog, media launchers, streaming tools, and Samsung DeX.

GameBox is an Android/DeX application, not a custom ROM or emulator. Game sources must be user-owned backups, homebrew, freeware, open-source, or otherwise authorized content.

## Current development status

This repository is an active pre-1.0 implementation of the August 2026 **GameBox Development Blueprint**. The blueprint is a six-month working plan, not a claim that every item is already complete.

Status as of 29 August 2026:

| Blueprint area | Status | Current implementation |
| --- | --- | --- |
| Android/Compose shell | Implemented | Dark GameBox theme, phone portrait/landscape and large DeX layouts |
| Controller navigation | Partial | Focusable cards, LB/RB tabs, Back/B handling, and focus restoration; physical-device and reconnect testing remain |
| Primary UI | Implemented foundation | Home, Library, Store, Details, Downloads, Media, PC Hub, and Settings |
| Catalog and discovery | Implemented foundation | Room-backed catalog, search, platform/genre filters, favorites, cached authorized manifests |
| Remote providers | Partial | Configurable HTTPS catalog with strict validation, offline cache, out-of-band credential abstraction, optional Basic auth, safe WebDAV/S3 URI builders, and bounded transport client; S3 request-signing boundary with AWS Signature V4 and authenticated transport wiring; WebDAV Basic auth hardens incomplete credentials |
| Downloads | Implemented foundation | Durable WorkManager jobs, notifications, low-space reserve, pause/resume, Range validation, retries, cancellation, size limits, SHA-256 verification, and atomic install |
| Install/uninstall | Partial | App-private verified install and content-only uninstall prototype; full storage-volume and freed-space UX remain |
| Saves | Partial | Persistent save records, checksum-protected backup/restore, import/export, retention prototype, deterministic sync conflict resolver, and cross-game safety checks; real emulator save adapters and cloud transport remain |
| Emulator integration | Partial | Allowlisted emulator handoff with per-game package selection and graphics profile persistence exposed in Details, read-only FileProvider access, return tracking, and capability registry; production adapter validation remains |
| Media and PC | Partial | Installed-app detection, typed availability states, and safe launch shortcuts for media, Moonlight, Winlator, Termux, Files, browser, and Android settings |
| Offline operation | Partial | Cached catalog and persistent local state; full airplane-mode acceptance testing remains |
| Diagnostics | Partial | Sanitized export report and visible download errors; structured log collection and recovery bundles remain |
| CI and releases | Partial | Unit tests, debug APK builds, SHA-256 artifacts, and alpha release workflow; signed production builds, rollback, and update channels remain |
| External storage | Partial | SAF folder selection, persisted permissions, read/write status, disconnect detection, and safe content-migration planning; copy execution now classifies removable-storage outages as retryable with recovery metrics; physical SAF unplug testing remains |
| Target hardware | Not validated | Galaxy A53 controller testing and Galaxy S23 Ultra/DeX/HDMI/Ethernet/thermal/SSD soak testing require physical hardware |
| Enclosure/handoff | Not started | Hardware enclosure, hub, cooling, cabling, and recovery-button work follows software maturity |

## Implemented highlights

- Kotlin, Jetpack Compose, Room, DataStore, and WorkManager
- Responsive normal-phone and 16:9 living-room layouts
- Persistent catalog, install state, favorites, play history, downloads, and save records
- Authorized HTTPS catalog configuration with bounded responses and atomic offline caching
- Verified remote download pipeline with true pause/resume and safe job-scoped cleanup
- App-private staging; content is promoted only after complete SHA-256 verification
- Download notifications, byte progress, failure reasons, retry, and cancellation
- Save-safe uninstall and user-controlled backup import/export
- Media/PC launch hubs and Android system-setting shortcuts
- SAF external-library selection with persisted permissions and non-destructive disconnect/read-only status
- Sanitized diagnostics export that excludes credentials, source URLs, checksums, paths, and save contents
- Automated unit tests and debug APK builds
- Friendly emulator selection with validated graphics profiles, launch-time profile handoff, and unsupported-platform guidance

## Important remaining work for Blueprint 1.0

- Replace the diagnostic text payload with an authorized runnable homebrew fixture
- Validate at least one real emulator adapter for every officially supported platform group
- Validate additional production emulator adapters and apply graphics profiles to adapter launch arguments
- Implement authenticated WebDAV/S3-style providers and provider recovery guidance
- Add real emulator save discovery and authenticated cloud transport
- Add confirmed content migration to the selected SAF/SSD library and complete physical disconnect/unplug safety tests
- Add Compose instrumentation, accessibility, lifecycle, migration, offline, and failure-recovery tests
- Complete physical controller testing on Galaxy A53
- Complete Galaxy S23 Ultra DeX/HDMI/Ethernet/charging/thermal/reconnect soak testing
- Add signed production builds, update/rollback procedures, and recovery documentation
- Complete the physical enclosure and hardware handoff

## Blueprint implementation backlog

The remaining work is tracked in these concrete groups:

- Implement authenticated WebDAV and S3 catalog network clients using the transport abstractions.
- Connect content migration planning/execution to SAF document-tree copy operations and verify disconnect recovery.
- Add real emulator save discovery, adapter-specific save import/export, and authenticated cloud synchronization.
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

Each CI run produces a temporary debug APK and SHA-256 artifact. Release APKs are debug-signed development builds until production signing is configured.

## Safety invariants

- Never install a remote file before checksum verification succeeds.
- Never follow untrusted catalog or download redirects.
- Never allow catalog credentials inside URLs; resolve complete credential pairs through the credential store, redact them in diagnostics/logging, and send only over HTTPS.
- Never let an install path escape GameBox app-private storage.
- Cancel removes only that job's partial content.
- Uninstall keeps saves, metadata, favorites, and play history by default.
- Diagnostics exclude credentials, remote paths, checksums, file paths, and save contents.
