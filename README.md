# GameBox OS

GameBox OS is a controller-first Android living-room shell for a docked phone. It presents one coherent interface for a local game library, authorized or homebrew catalog, media launchers, streaming tools, and Samsung DeX.

GameBox is an Android/DeX application, not a custom ROM or emulator. Game sources must be user-owned backups, homebrew, freeware, open-source, or otherwise authorized content. An optional native Windows Companion is provided for local Windows-library launching; it does not replace the Android/DeX shell.

## Current development status

This repository is an active pre-1.0 implementation of the August 2026 **GameBox Development Blueprint**. The blueprint is a six-month working plan, not a claim that every item is already complete.

Status as of 30 August 2026:

| Blueprint area | Status | Current implementation |
| --- | --- | --- |
| Android/Compose shell | Implemented | Dark GameBox theme, blueprint-aligned 16:9 home hierarchy with hero/quick-launch/device-status regions, resizeable phone portrait/landscape layouts, IME-aware resizing, modern back-invoked navigation, and large DeX layouts |
| Windows companion | Implemented foundation | Native .NET 8/WPF local-library launcher with search, favorites, available-only and platform filters, favorites/title and recent-play sorting, editable validated title/platform/arguments, localized play history with safe reset, atomic JSON backup/restore, visible missing-target status, metadata-preserving relocation, duplicate-target prevention, Show in folder, bounded Start Menu/desktop, Steam-library, and Epic Games manifest discovery, bounded validated HTTPS catalog synchronization, and a self-contained win-x64 CI artifact |
| Controller navigation | Partial | Focusable cards, LB/RB tabs, Back/B handling, and focus restoration; physical-device and reconnect testing remain |
| Primary UI | Implemented foundation | Home, Library, Store, Details, Downloads, Media, PC Hub, Settings, migration confirmation UI, accessible tab/card/progress/live-region semantics, saveable navigation/detail/focus restoration, JVM UI contracts, and compiled Compose instrumentation coverage |
| Catalog and discovery | Implemented foundation | Room-backed catalog, search, platform/genre filters, favorites, cached authorized manifests, and a pinned MIT-licensed runnable Galaxy Patrol NES fixture |
| Remote providers | Partial | Configurable HTTPS catalog with strict validation, offline cache, out-of-band credential abstraction, optional Basic auth, safe WebDAV/S3 URI builders, and bounded transport client; S3 request-signing boundary with AWS Signature V4 and authenticated transport wiring; WebDAV Basic auth hardens incomplete credentials; shared bounded recovery classification is applied by authenticated transports for auth, rate limits, transient network, and permanent failures, with JUnit coverage |
| Downloads | Implemented | Durable WorkManager jobs, notifications, measured speed and ETA, preemptive low-storage warnings using the worker reserve, pause/resume, Range validation, retries, cancellation, size limits, SHA-256 verification, and atomic install |
| Install/uninstall | Partial | App-private verified install plus explicit save-safe uninstall confirmation with exact freed and retained byte counts; generalized production content adapters and physical storage validation remain |
| Saves | Partial | Platform save-adapter registry, real directory discovery, reactive per-game save presence in Details, multi-artifact backup/restore, atomic checksum-protected snapshot manifests, import/export, retention, deterministic sync conflict resolution, cross-game safety checks, and orchestration that executes upload/download/conflict operations with automated JUnit coverage; credential-safe cloud sync contract with HTTPS/payload/offline guards plus bounded Basic/SigV4 authenticated upload/download byte transport and shared recovery handling; real-endpoint integration and physical emulator validation remain |
| Emulator integration | Partial | Allowlisted emulator handoff with per-game package selection and graphics profile persistence exposed in Details, read-only FileProvider access, return tracking, capability registry, documented PPSSPP Args graphics mapping, and Dolphin AutoStartFiles launch wiring; production adapter validation remains |
| Media and PC | Partial | Installed-app detection, typed availability states, safe launch shortcuts, persistent hide-unavailable policy, responsive empty/setup states, Moonlight connectivity status, bounded host reachability probes, interactive PC host-probe panel, and recent sessions for media, Moonlight, Winlator, Termux, Files, browser, and Android settings; physical streaming validation remains |
| Offline operation | Implemented foundation | Cached catalog and persistent local state, explicit network-aware fallback selection, and a reactive offline-mode banner; full airplane-mode acceptance testing remains |
| Diagnostics | Partial | Sanitized report, bounded redacted event collection, lifecycle wiring, visible download errors, and a 2 MiB ZIP recovery bundle export; physical failure validation remains |
| CI and releases | Partial | Unit tests, debug APK builds, SHA-256 artifacts, alpha release workflow, deterministic channel-readiness gating, validated APK provenance manifests with size/hash/channel metadata, rollback tag metadata, and tag/channel consistency validation, release/rollback tag existence checks, serialized production publishing, and verified asset upload; protected signed-production workflow is implemented; configuring repository signing secrets and executing the real update channel remain |
| External storage | Partial | SAF folder selection, persisted permissions, read/write status, real filesDir/installed discovery, exact migration planning, Settings confirmation/execution with result totals, non-destructive document-tree copy execution with explicit confirmation states, safe partial-file finalization, disconnect detection, and retryable outage classification; physical unplug testing remains |
| Target hardware | Not validated | Galaxy A53 controller testing and Galaxy S23 Ultra/DeX/HDMI/Ethernet/thermal/SSD soak testing require physical hardware |
| Enclosure/handoff | Handoff prepared | Blueprint-grounded entry gates, component criteria, measurement worksheet, DeX/hub/thermal/SSD validation matrix, CAD release checklist, prototype acceptance, BOM record, recovery path, and final handoff package are specified in [the hardware handoff](docs/hardware-enclosure-handoff.md); physical measurement, CAD, fabrication, and validation remain |

## Implemented highlights

- Kotlin, Jetpack Compose, Room, DataStore, and WorkManager
- Responsive normal-phone and 16:9 living-room layouts
- Persistent catalog, install state, favorites, play history, downloads, and save records
- Authorized HTTPS catalog configuration with bounded responses and atomic offline caching
- Offline-installable Galaxy Patrol NES homebrew fixture with pinned source, MIT attribution, iNES/header/size/SHA-256 CI verification, and RetroArch handoff
- Verified remote download pipeline with true pause/resume and safe job-scoped cleanup
- App-private staging; content is promoted only after complete SHA-256 verification
- Download notifications, byte progress, measured speed/ETA, low-storage warnings, failure reasons, retry, and cancellation
- Save-safe uninstall, reactive per-game save presence, durable multi-artifact snapshot manifests, user-controlled backup import/export, and bounded cloud-sync validation
- Configurable Media/PC launch hubs with installed-only filtering, Moonlight network status/recent sessions, interactive PC host probing, and Android system-setting shortcuts
- SAF external-library selection with persisted permissions, explicit migration confirmation dialog, retryable disconnect/read-only status, and non-destructive copy execution
- Sanitized diagnostics export that excludes credentials, source URLs, checksums, paths, and save contents
- Automated unit tests covering storage/provider/release/accessibility contracts plus compiled Compose migration/accessibility instrumentation APKs, debug APK builds, self-contained Windows Companion builds, and green Android/Windows CI verification on the latest main commit with superseded-run cancellation, bounded Gradle/build steps, and executable catalog-timeout coverage
- Friendly emulator selection with validated graphics profiles, launch-time profile handoff, and unsupported-platform guidance
- Deterministic release manifest generation containing APK hash, size, channel, and optional rollback tag
- Network-aware offline catalog selection with a reactive accessible offline-mode banner
- Release validation rejects unsupported tags and manifest channel/tag mismatches before publication

## Important remaining work for Blueprint 1.0

- Validate the bundled Galaxy Patrol handoff in RetroArch and at least one real emulator adapter for every officially supported platform group
- Validate PPSSPP/Dolphin intent behavior and additional production emulator adapters on physical target devices
- Complete end-to-end authenticated WebDAV/S3 integration testing and physical recovery validation
- Validate authenticated cloud-save byte transfer against real WebDAV/S3 endpoints and real save adapters against production emulators
- Complete physical disconnect/unplug safety tests for confirmed SAF migrations
- Execute Compose accessibility/lifecycle instrumentation on devices and expand airplane-mode and failure-recovery scenarios
- Complete physical controller testing on Galaxy A53
- Complete Galaxy S23 Ultra DeX/HDMI/Ethernet/charging/thermal/reconnect soak testing
- Configure signed production builds and execute real update-channel/rollback validation using generated release manifests
- Execute the prepared physical enclosure handoff: measure the final S23/hub/cable layout, select and validate the BOM, release CAD, fabricate, and complete prototype acceptance
- Expand the optional Windows companion with additional storefront discovery beyond Steam and Epic Games, richer streaming integrations, installer signing, and physical Windows validation
- Validate physical LAN/controller streaming and host-probe behavior on target devices

## Blueprint implementation backlog

The remaining work is tracked in these concrete groups:

- Complete end-to-end authenticated WebDAV/S3 provider integration and physical recovery testing.
- Verify physical disconnect recovery for Settings-driven migrations.
- Validate adapter-specific save discovery/import/export against production emulators and exercise authenticated cloud byte transport against real WebDAV/S3 endpoints.
- Run the compiled Compose accessibility/migration/lifecycle suite on devices and add offline/recovery scenarios.
- Complete physical controller, DeX, HDMI, Ethernet, charging, thermal, SSD, and unplug/reattach validation.
- Configure production signing and execute update-channel/rollback validation using generated artifact manifests.
- Execute the repository hardware handoff specification: target measurements, BOM selection, CAD/fabrication, cooling/cabling validation, and prototype acceptance.

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
