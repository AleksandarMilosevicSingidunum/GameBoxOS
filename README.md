# GameBox OS

GameBox OS is a controller-first Android living-room shell for a docked phone. It presents one coherent interface for a local game library, authorized or homebrew catalog, media launchers, streaming tools, and Samsung DeX.

GameBox is an Android/DeX application, not a custom ROM or emulator. Game sources must be user-owned backups, homebrew, freeware, open-source, or otherwise authorized content. An optional native Windows Companion is provided for local Windows-library launching; it does not replace the Android/DeX shell.

## Current development status

This repository is an active pre-1.0 implementation of the August 2026 **GameBox Development Blueprint**. The blueprint is a six-month working plan, not a claim that every item is already complete.

Status as of 31 August 2026:

Latest verified increments: the primary shell now includes an original scalable GameBox hex/cube mark, distinct PS2/GameCube/Wii/PSP/Dreamcast/3DS/Switch/Homebrew platform glyphs, and full-color code-native Media/PC/quick-launch brand marks; RetroArch Android handoff supports launcher ROM extras, package aliases, actionable controller guidance, and an ACTION_VIEW compatibility fallback; catalog refresh distinguishes normal success, offline bundled fallback, remote-provider fallback, and terminal error; Store recovery messages are accessible and actionable; optional TheGamesDB transport or per-entry enrichment failures no longer block the authorized catalog; TheGamesDB discovery resolves provider box art, fan art, clear logos, and screenshots from the documented `include=boxart` payload and offers fixed PS2/GameCube/Wii/PSP/Dreamcast/3DS/Switch/Homebrew filters with an up-to-20-title-per-console sync; Android and Windows CI passed on each merged increment.

| Blueprint area | Status | Current implementation |
| --- | --- | --- |
| Android/Compose shell | Implemented | Dark GameBox theme, blueprint-aligned 16:9 home hierarchy with hero/quick-launch/device-status regions, resizeable phone portrait/landscape layouts, IME-aware resizing, modern back-invoked navigation, and large DeX layouts |
| Windows companion | Implemented foundation | Native .NET 8/WPF local-library launcher with search, favorites, available-only and platform filters, favorites/title and recent-play sorting, editable validated title/platform/arguments, localized play history with safe reset, atomic JSON backup/restore, visible missing-target status, metadata-preserving relocation, duplicate-target prevention, Show in folder, bounded Start Menu/desktop, Steam-library, and Epic Games manifest discovery, bounded validated HTTPS catalog synchronization, and a self-contained win-x64 CI artifact |
| Controller navigation | Partial | Focusable cards, LB/RB tabs, Back/B handling, and focus restoration; physical-device and reconnect testing remain |
| Primary UI | Blueprint-matched large-screen implementation | Shared 16:9 living-room shell, original scalable GameBox brand mark, distinct console glyphs, full-color Media/PC launcher marks, centered icon navigation, clock/notification chrome, controller/profile footer, vivid artwork, side information rails, dense content rows, featured heroes, focus/hover/press motion, and blueprint-specific Home, Library, Store, Details, Downloads, Media, PC Hub, Settings, and save-safe uninstall layouts; Settings has functional scroll-linked wide navigation plus phone section chips and live controller/download/network/layout summaries; phone layouts remain responsive rather than forcing the DeX ratio |
| Catalog and discovery | Implemented foundation | Room-backed catalog, search, platform/genre filters, favorites, cached authorized manifests, pinned MIT-licensed runnable Galaxy Patrol NES fixture, and a console-first Store with PS2/GameCube/Wii/PSP/Dreamcast/3DS/Switch/Homebrew chips |
| Remote providers | Partial | Runtime TheGamesDB enrichment with Android Keystore-backed key configuration; documented `include=boxart` media parsing for HTTPS cover art, fan art, clear logos, and screenshots; sequential sync of up to 20 titles per requested console; configurable HTTPS catalog with strict validation, offline cache, out-of-band credential abstraction, optional Basic auth, safe WebDAV/S3 URI builders, and bounded transport client; S3 request-signing boundary with AWS Signature V4 and authenticated transport wiring; WebDAV Basic auth hardens incomplete credentials; shared bounded recovery classification is applied by authenticated transports for auth, rate limits, transient network, and permanent failures, with JUnit coverage |
| Downloads | Implemented | Durable WorkManager jobs, notifications, measured speed and ETA, preemptive low-storage warnings using the worker reserve, pause/resume, Range validation, retries, cancellation, size limits, SHA-256 verification, and atomic install |
| Install/uninstall | Partial | App-private verified install, platform-aware authorized-copy import profiles spanning current Nintendo/Sony/Sega/Microsoft/retro catalog families (including Switch XCI/NSP and 3DS/CIA), unrestricted Android document visibility followed by strict extension validation, one-pass CRC32/MD5/SHA-1/SHA-256 hashing, persistent promotion into Library, atomic multi-file `.cue`/`.gdi`/`.mds`/`.ccd` set import with dependency validation, and explicit save-safe uninstall confirmation with exact freed and retained byte counts; generalized production content adapters and physical storage validation remain |
| Saves | Partial | Platform save-adapter registry, real directory discovery, reactive per-game save presence in Details, multi-artifact backup/restore, atomic checksum-protected snapshot manifests, import/export, retention, deterministic sync conflict resolution, explicit game-scoped restore rejection, bounded backup creation/import, and orchestration that executes upload/download/conflict operations with automated JUnit coverage; Settings now exposes WebDAV/S3 endpoint and Android-Keystore credential configuration, while Details performs real bounded Basic/SigV4 upload/download of game-scoped, self-verifying envelopes and preserves a conflicting local copy before restore; real-endpoint integration and physical emulator validation remain |
| Emulator integration | Partial | Allowlisted emulator handoff with per-game package selection and graphics profile persistence exposed in Details, read-only FileProvider access for verified downloads and user-imported copies, launch-time SHA-256 re-verification of primary and companion disc files, complete companion-URI grants, return tracking, capability registry, documented PPSSPP Args graphics mapping, Dolphin AutoStartFiles launch wiring, and approved scoped handoffs for PS1/DuckStation, N64/M64Plus FZ, and Dreamcast/Flycast with RetroArch fallbacks; RetroArch launcher and ACTION_VIEW fallback paths are covered by tests, while production adapter validation remains |
| Media and PC | Partial | Installed-app detection, typed availability states, safe launch shortcuts, persistent hide-unavailable policy, responsive empty/setup states, Moonlight connectivity status, bounded host reachability probes, interactive PC host-probe panel, and recent sessions for media, Moonlight, Winlator, Termux, Files, browser, and Android settings; physical streaming validation remains |
| Offline operation | Implemented foundation | Cached catalog and persistent local state, explicit network-aware fallback selection, accessible offline banner, resilient bundled fallback after remote-provider failure, actionable retry messaging, and non-fatal optional metadata enrichment; full device airplane-mode acceptance testing remains |
| Diagnostics | Partial | Sanitized report, bounded redacted event collection, lifecycle wiring, visible download errors, and a 2 MiB ZIP recovery bundle export; physical failure validation remains |
| CI and releases | Partial | Unit tests, debug APK builds, SHA-256 artifacts, alpha release workflow, deterministic channel-readiness gating, validated APK provenance manifests with size/hash/channel metadata, rollback tag metadata, and tag/channel consistency validation, release/rollback tag existence checks, serialized production publishing, and verified asset upload; protected signed-production workflow is implemented; configuring repository signing secrets and executing the real update channel remain |
| External storage | Partial | SAF folder selection, persisted permissions, read/write status, real filesDir/installed discovery, exact migration planning, Settings confirmation/execution with result totals, non-destructive document-tree copy execution with explicit confirmation states, safe partial-file finalization, disconnect detection, and retryable outage classification; physical unplug testing remains |
| Target hardware | Not validated | Galaxy A53 controller testing and Galaxy S23 Ultra/DeX/HDMI/Ethernet/thermal/SSD soak testing require physical hardware |
| Enclosure/handoff | Handoff prepared | Blueprint-grounded entry gates, component criteria, measurement worksheet, DeX/hub/thermal/SSD validation matrix, CAD release checklist, prototype acceptance, BOM record, recovery path, and final handoff package are specified in [the hardware handoff](docs/hardware-enclosure-handoff.md); physical measurement, CAD, fabrication, and validation remain |

## Implemented highlights

- Kotlin, Jetpack Compose, Room, DataStore, and WorkManager
- Responsive normal-phone and 16:9 living-room layouts
- Blueprint-matched DeX presentation with compact typography, glass panels, vivid box art, left/right context rails, controller focus rings, hover lift, press compression, and animated screen entrances
- Scroll-linked Settings navigation with selected-state feedback, controller-focusable wide rail, phone section chips, and truthful controller/download/network/layout status cards
- Persistent catalog, install state, favorites, play history, downloads, and save records
- Authorized HTTPS catalog configuration with bounded responses and atomic offline caching
- Offline-installable Galaxy Patrol NES homebrew fixture with pinned source, MIT attribution, iNES/header/size/SHA-256 CI verification, and RetroArch handoff
- Verified remote download pipeline with true pause/resume and safe job-scoped cleanup
- App-private staging; content is promoted only after complete SHA-256 verification
- Platform-aware legal-copy import with friendly format guidance for Switch, 3DS, GameCube, Wii, PS1/PS2/PS3, PSP/Vita, Sega, Xbox, and common cartridge/arcade systems
- Download notifications, byte progress, measured speed/ETA, low-storage warnings, failure reasons, retry, and cancellation
- Save-safe uninstall, reactive per-game save presence, durable multi-artifact snapshot manifests, user-controlled backup import/export, and bounded WebDAV/S3 cloud upload/restore with encrypted credentials and conflict preservation
- Configurable Media/PC launch hubs with installed-only filtering, Moonlight network status/recent sessions, interactive PC host probing, and Android system-setting shortcuts
- SAF external-library selection with persisted permissions, explicit migration confirmation dialog, retryable disconnect/read-only status, and non-destructive copy execution
- Sanitized diagnostics export that excludes credentials, source URLs, checksums, paths, and save contents
- Automated unit tests covering storage/provider/release/accessibility contracts plus a clean API-35 emulator run of the Compose migration, navigation, Room, lifecycle, and accessibility instrumentation suite; debug APK builds, self-contained Windows Companion builds, and green Android/Windows CI verification on the latest main commit with superseded-run cancellation, bounded Gradle/build steps, and executable catalog-timeout coverage
- Friendly emulator selection with validated graphics profiles, launch-time profile handoff, and unsupported-platform guidance
- Deterministic release manifest generation containing APK hash, size, channel, and optional rollback tag
- Network-aware offline catalog selection with a reactive accessible offline-mode banner, explicit degraded refresh states, bundled recovery after provider failure, and non-fatal optional metadata enrichment
- Release validation rejects unsupported tags, manifest channel/tag mismatches, and missing rollback tags for signed channels before publication

## Important remaining work for Blueprint 1.0

The active implementation goal covers all remaining software work below. Physical hardware, enclosure, thermal, cabling, and fabrication validation are tracked separately and are excluded from that software goal.

- Validate the bundled Galaxy Patrol handoff in RetroArch and at least one real emulator adapter for every officially supported platform group
- Validate PPSSPP/Dolphin intent behavior and additional production emulator adapters on physical target devices
- Complete end-to-end authenticated WebDAV/S3 integration testing and physical recovery validation
- Validate authenticated cloud-save byte transfer against real WebDAV/S3 endpoints and real save adapters against production emulators
- Complete physical disconnect/unplug safety tests for confirmed SAF migrations
- Complete physical-device Compose accessibility/lifecycle validation and expand airplane-mode and failure-recovery scenarios (the hosted API-35 emulator instrumentation suite is now a required CI gate)
- Complete physical controller testing on Galaxy A53
- Complete Galaxy S23 Ultra DeX/HDMI/Ethernet/charging/thermal/reconnect soak testing
- Configure signed production builds and execute real update-channel/rollback validation using generated release manifests
- Execute the prepared physical enclosure handoff: measure the final S23/hub/cable layout, select and validate the BOM, release CAD, fabricate, and complete prototype acceptance
- Expand the optional Windows companion with richer streaming integrations, installer signing, and physical Windows validation; Steam, Epic Games, and GOG discovery are implemented
- Validate physical LAN/controller streaming and host-probe behavior on target devices

## Blueprint implementation backlog

The remaining work is tracked in these concrete groups:

- Complete end-to-end authenticated WebDAV/S3 provider integration and physical recovery testing.
- Verify physical disconnect recovery for Settings-driven migrations.
- Validate adapter-specific save discovery/import/export against production emulators and exercise authenticated cloud byte transport against real WebDAV/S3 endpoints.
- Run the Compose accessibility/migration/lifecycle suite on physical target devices and add offline/recovery scenarios; the same suite already runs on a clean API-35 emulator in CI.
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

The optional Windows Companion discovers Windows shortcuts plus Steam, Epic Games, and GOG installations, runs its core test harness, and publishes a self-contained `win-x64` ZIP from `.github/workflows/windows-ci.yml`.

Each CI run produces a temporary debug APK and SHA-256 artifact. Release APKs are debug-signed development builds until production signing is configured.

## Safety invariants

- Never install a remote file before checksum verification succeeds.
- Never follow untrusted catalog or download redirects.
- Never allow catalog credentials inside URLs; resolve complete credential pairs through the credential store, redact them in diagnostics/logging, and send only over HTTPS.
- Never let an install path escape GameBox app-private storage.
- Cancel removes only that job's partial content.
- Uninstall keeps saves, metadata, favorites, and play history by default.
- Diagnostics exclude credentials, remote paths, checksums, file paths, and save contents.


### Galaxy Patrol storage and launch

The bundled Galaxy Patrol NES content is installed under the app-private path `filesDir/installed/retro/galaxy-patrol/content/galaxy-patrol.nes`. It is intentionally not exposed as a public filesystem path. GameBox verifies the pinned SHA-256 before launch, grants RetroArch a temporary read-only `content://` URI, and starts RetroArch's `RetroActivityFuture` with the installed `fceumm_libretro_android.so` core path. The path is passed as `/data/data/<RetroArch package>/cores/fceumm_libretro_android.so`, the stable Android app-private location used by RetroArch's external-launch contract. GameBox also clears a stale RetroArch activity before each handoff. If automatic launch is rejected, force-stop RetroArch once, reopen it to confirm **Nintendo - NES / Famicom (FCEUmm)** is installed, then retry Play from GameBox. The explicit core path is required because current Android RetroArch builds can otherwise open a black surface when an external launcher does not provide a resolvable `LIBRETRO` core.

### Online metadata and box art

GameBox wires the optional TheGamesDB metadata adapter (`TheGamesDbMetadataClient`) into runtime catalog refresh. Configure or clear its API key in Settings; the key is AES-GCM encrypted with an Android Keystore key and never stored in DataStore, URLs, diagnostics, or logs. The Store's console sync requests `include=boxart`, resolves the provider's HTTPS `base_url` entries, and caches cover art, fan art/banner backgrounds, clear logos, and screenshot thumbnails when the title supplies them. Each console sync is capped at 20 games to keep the living-room catalog responsive. The adapter enriches authorized catalog entries with HTTPS artwork and descriptions only. It never downloads game binaries. Discovery details can open a legal storefront/source search (Steam, GOG, Nintendo, PlayStation Store, itch.io, or GitHub where applicable); GameBox does not integrate ROM sites or download copyrighted game files. Relative TheGamesDB artwork paths are resolved only against credential-free HTTPS provider bases; insecure or malformed artwork preserves the existing fallback. TheGamesDB API requires an API key and is documented at https://api.thegamesdb.net/. Binary installation remains governed by the catalog's authorized HTTPS source and SHA-256 checksum.

### Importing authorized local copies

Open a title in Store discovery and select **Import authorized copy**. The Android picker displays every document MIME type because console images such as `.xci`, `.nsp`, `.rvz`, `.wbfs`, `.chd`, and `.cia` are often exposed as generic binary files. GameBox then applies a strict format allowlist for the selected console before it reads, hashes, and copies the file into app-private storage. A successful copy is promoted into the persistent Library with its artwork and metadata, and supported emulator adapters receive a temporary read-only URI only after launch-time SHA-256 verification. Current profiles cover Nintendo handheld and home consoles, PlayStation families, Sega families, Xbox, Atari, Neo Geo, PC Engine, WonderSwan, arcade, retro, and homebrew formats. Unknown catalog platforms use the recognized console-format union while executable and script types remain rejected.

Import support means safe file acceptance, persistent Library registration, and launch handoff where an approved adapter exists; it does not decrypt protected content, provide firmware or console keys, or bundle an emulator. A Switch `.xci`/`.nsp` copy can be imported and retained in Library, but Play requires a compatible, legally configured emulator adapter, which is not yet implemented for Switch. For disc-based systems, choose **Import multi-file disc set** and select the descriptor plus every referenced track together. GameBox validates `.cue`, `.gdi`, `.mds`/`.mdf`, and `.ccd`/`.img` dependencies, commits the replacement atomically, retains checksums for every file, and grants the approved emulator temporary read-only access to the complete set.

### Cloud save setup

Open **Settings → Saves & Cloud Sync**, choose WebDAV or S3-compatible storage, and enter an existing HTTPS collection/bucket-prefix endpoint. GameBox appends a game-scoped `.gamebox-save` object name; do not put credentials, query parameters, or a filename in the endpoint. WebDAV uses Basic authentication, while S3-compatible storage uses AWS Signature V4 with the configured region. Credentials are AES-GCM encrypted with an Android Keystore key and are excluded from DataStore and diagnostics. In Galaxy Patrol Details, **Upload cloud copy** creates a checksum-protected envelope and **Restore cloud copy** verifies it before replacement; a different local save is first retained under app-private conflict storage. Real provider behavior still depends on server permissions and must be validated against the chosen endpoint.

