# GameBox OS

GameBox OS is a controller-first Android living-room shell for a docked phone. It presents one coherent interface for a local game library, authorized or homebrew catalog, media launchers, streaming tools, and Android external-display/desktop modes such as Honor Desktop Mode or Samsung DeX.

GameBox is an Android external-display application, not a custom ROM or emulator. Game sources must be user-owned backups, homebrew, freeware, open-source, or otherwise authorized content. An optional native Windows Companion is provided for local Windows-library launching; it does not replace the Android device shell.

## Current development status

Android UI update (7 September 2026): a new Blueprint-focused pass refines the
dashboard proportions, artwork, console rails, game details, phone safe areas
and input feedback. See [UI changes and visual QA profiles](docs/android-ui-blueprint-pass.md).
The workflow captures the real app on phone and DeX-sized Android emulators;
these captures do not replace physical-device acceptance testing.

See the [production critical path](docs/production-critical-path.md) for current
release blockers. Starter catalog entries are discovery metadata, not preinstalled
games; installation state must come from local operations.

Legal emulator fixtures are tracked in the [legal homebrew test matrix](docs/legal-homebrew-test-matrix.md).
Only license-cleared or locally reproducible homebrew is eligible for acceptance runs;
commercial game binaries and unauthorized provider downloads are excluded.

This repository is an active pre-1.0 implementation of the August 2026 **GameBox Development Blueprint**. The blueprint is a six-month working plan, not a claim that every item is already complete.

Status as of 11 October 2026:

Recent implementation, verification, and open hardware gates are tracked in the
[project roadmap](ROADMAP.md). The last recorded full Android JVM run passed 779
tests; it could not be rerun from the current restricted shell because Gradle
could not load `native-platform.dll`. The Windows core self-test and an isolated
WPF build passed on 11 October. The Honor Magic6 Pro is the user's selected
deployment device, but the latest device check found it asleep with no active TV
output. Physical-controller acceptance and the live Vimm download-to-import flow
remain unverified; this status is not a release-completion claim.

| Blueprint area | Status | Current implementation |
| --- | --- | --- |
| Android/Compose shell | Implemented | Dark GameBox theme, blueprint-aligned 16:9 home hierarchy with hero/quick-launch/device-status regions, resizeable phone portrait/landscape layouts, IME-aware resizing, modern back-invoked navigation, and large DeX layouts |
| Windows companion | Implemented foundation | Authenticated, cancellable, checksum-verified managed-save upload/download with Android conflict preservation; live Honor LAN discovery/status/library/configuration/save/content probes now pass; native .NET 8/WPF local-library launcher with search, favorites, available-only and platform filters, favorites/title and recent-play sorting, editable validated title/platform/arguments, localized play history with safe reset, atomic JSON backup/restore, visible missing-target status, metadata-preserving relocation, duplicate-target prevention, Show in folder, bounded Start Menu/desktop, Steam-library, and Epic Games manifest discovery, bounded validated HTTPS catalog synchronization, self-contained win-x64 ZIP and reproducible unsigned MSIX CI artifacts with checksums/provenance; production MSIX signing/install acceptance remains |
| Controller navigation | Partial | Focusable cards, debounced analog/hat navigation, LB/RB tabs, Back/B handling, nearest-neighbor focus restoration, and contextual X/Y actions across every primary screen, including state-safe play/install/resume, favorite, batch transfer operations, hub visibility/system actions, and cyclic Settings-section traversal; physical-device/reconnect testing remains |
| Primary UI | Blueprint-matched large-screen implementation | Shared 16:9 living-room shell, original scalable GameBox brand mark, distinct console glyphs, full-color Media/PC launcher marks, centered icon navigation, clock/notification chrome, controller/profile footer, vivid artwork, side information rails, dense content rows, featured heroes, focus/hover/press motion, and blueprint-specific Home, Library, Store, Details, Downloads, Media, PC Hub, Settings, and save-safe uninstall layouts; Settings has functional scroll-linked wide navigation plus phone section chips and live controller/download/network/layout summaries; phone layouts remain responsive rather than forcing the DeX ratio |
| Catalog and discovery | Implemented foundation | Room-backed catalog, search, platform/genre filters, favorites, cached authorized manifests, pinned MIT-licensed runnable Galaxy Patrol NES fixture, console-first Store filters, configurable HTTPS/JSON discovery, and optional Vimm's Lair title-page discovery that remains separate from verified content import/install |
| Remote providers | Partial | Keyless Wikidata exact-title lookup supplies missing year/descriptions/general imagery; Libretro's named-thumbnail archive supplies console box art and screenshots where published, with manual metadata/artwork correction and exact-title confidence gating; configurable HTTPS catalog with strict validation, offline cache, out-of-band credential abstraction, optional Basic auth, safe WebDAV/S3 URI builders, and bounded transport client; S3 request-signing boundary with AWS Signature V4 and authenticated transport wiring; WebDAV Basic auth hardens incomplete credentials; shared bounded recovery classification is applied by authenticated transports for auth, rate limits, transient network, and permanent failures, with JUnit coverage; the production provider contract exposes refresh, authenticated connection testing, capability reporting, and authorized source resolution; Settings can select HTTPS, WebDAV, or S3-compatible catalogs, configure transport-specific fields, store Basic/SigV4 credentials in Android Keystore, clear them, and run a controller-accessible Save & test action; real endpoint validation remains pending |
| Downloads | Implemented | Durable WorkManager jobs, notifications, measured speed and ETA, preemptive low-storage warnings using the worker reserve, pause/resume, Range validation, retries, cancellation, size limits, SHA-256 verification, and atomic install |
| Install/uninstall | Partial | Missing imported content has checksum-verified Locate and confirmation-gated Forget actions that retain saves and library metadata; app-private verified install, platform-aware authorized-copy import profiles spanning current Nintendo/Sony/Sega/Microsoft/retro catalog families (including Switch XCI/NSP and 3DS/CIA), unrestricted Android document visibility followed by strict extension validation, one-pass CRC32/MD5/SHA-1/SHA-256 hashing, persistent promotion into Library, atomic multi-file `.cue`/`.gdi`/`.mds`/`.ccd` set import with dependency validation, and general game-ID-based content-only uninstall confirmation with exact freed bytes, verified local/cloud save preflight, retained saves/backups/metadata/history, and partial-removal recovery; additional production content adapters and physical storage validation remain |
| Saves | Partial | Platform save-adapter registry, real directory discovery, reactive per-game save presence in Details, multi-artifact backup/restore, atomic checksum-protected snapshot manifests, import/export, retention, deterministic sync conflict resolution, explicit game-scoped restore rejection, bounded backup creation/import, and orchestration that executes upload/download/conflict operations with automated JUnit coverage; Settings now exposes WebDAV/S3 endpoint and Android-Keystore credential configuration, while every per-game Details save card performs real bounded Basic/SigV4 upload/download of game-scoped, self-verifying envelopes and preserves a conflicting local copy before restore; real-endpoint integration and physical emulator validation remain |
| Emulator integration | Partial | Allowlisted standalone emulator handoff with per-game package selection and graphics profile persistence exposed in Details, read-only FileProvider access for verified downloads and user-imported copies, launch-time SHA-256 re-verification of primary and companion disc files, complete companion-URI grants, return tracking, documented PPSSPP ACTION_VIEW content handoff, Dolphin AutoStartFiles launch wiring, and scoped standalone adapters for NES.emu, PS1/DuckStation, N64/M64Plus FZ, Dreamcast/Flycast, and other mapped systems; live NES.emu installation/launch and the remaining production adapters still need physical validation |
| Media and PC | Partial | Installed-app detection, typed availability states, safe launch shortcuts, persistent hide-unavailable policy, responsive empty/setup states, Moonlight connectivity status, bounded host reachability probes, interactive PC host-probe panel with validated persistent host/port configuration and explicit clearing, a deliberate controller-accessible return to the Android/DeX desktop, and recent sessions for media, Moonlight, Winlator, Termux, Files, browser, and Android settings; physical streaming validation remains |
| Offline operation | Implemented foundation | Cached catalog and persistent local state, explicit network-aware fallback selection, accessible offline banner, resilient bundled fallback after remote-provider failure, actionable retry messaging, and non-fatal optional metadata enrichment; full device airplane-mode acceptance testing remains |
| Diagnostics | Partial | Sanitized report, bounded redacted event collection, lifecycle wiring, visible download errors, and a 2 MiB ZIP recovery bundle export; physical failure validation remains |
| CI and releases | Partial | Unit tests, debug APK builds, SHA-256 artifacts, alpha release workflow, deterministic channel-readiness gating, validated APK provenance manifests with size/hash/channel metadata, rollback tag metadata, and tag/channel consistency validation, release/rollback tag existence checks, serialized production publishing, and verified asset upload; protected signed-production workflow is implemented; configuring repository signing secrets and executing the real update channel remain |
| External storage | Partial | SAF folder selection, persisted permissions, read/write status, real filesDir/installed discovery, exact migration planning, Settings confirmation/execution with result totals, verified document-tree cutover that removes internal sources only after size/SHA-256 confirmation, external-library emulator launch and content-only uninstall integration, explicit confirmation states, disconnect detection, and retryable outage classification; physical unplug testing remains |
| Target hardware | Partially observed, not validated | The selected deployment device is the Honor Magic6 Pro in the user's wired TV/desktop-mode setup. Physical controller input, HDMI reconnect, charging/network concurrency, thermal behavior, storage, cold-boot/no-touch recovery, and soak testing remain physical gates. |
| Enclosure/handoff | Re-scope required | The existing [hardware handoff](docs/hardware-enclosure-handoff.md) is specific to a Magic5 Pro and a UGREEN hub; it is not acceptance evidence for the selected Magic6 Pro setup. Revisit only after the actual dock and physical measurements are confirmed. |

## Implemented highlights

- Kotlin, Jetpack Compose, Room, DataStore, and WorkManager
- Responsive normal-phone and 16:9 living-room layouts
- Blueprint-matched DeX presentation with compact typography, glass panels, vivid box art, left/right context rails, controller focus rings, hover lift, press compression, and animated screen entrances
- Scroll-linked Settings navigation with selected-state feedback, controller-focusable wide rail, phone section chips, and truthful controller/download/network/layout status cards
- Persistent catalog, install state, favorites, play history, downloads, and save records
- Authorized HTTPS catalog configuration with bounded responses and atomic offline caching
- Offline-installable Galaxy Patrol NES homebrew fixture with pinned source, MIT attribution, iNES/header/size/SHA-256 CI verification, and a standalone NES.emu launch route
- Verified remote download pipeline with true pause/resume and safe job-scoped cleanup
- App-private staging; content is promoted only after complete SHA-256 verification
- Platform-aware legal-copy import with friendly format guidance for Switch, 3DS, GameCube, Wii, PS1/PS2/PS3, PSP/Vita, Sega, Xbox, and common cartridge/arcade systems
- Download notifications, byte progress, measured speed/ETA, low-storage warnings, failure reasons, retry, and cancellation
- Save-safe uninstall, reactive per-game save presence, durable multi-artifact snapshot manifests, user-controlled backup import/export, and bounded WebDAV/S3 cloud upload/restore with encrypted credentials and conflict preservation
- Configurable Media/PC launch hubs with installed-only filtering, Moonlight network status/recent sessions, interactive PC host probing, and Android system-setting shortcuts
- SAF external-library selection with persisted permissions, explicit migration confirmation, verified move/cutover, launch resolution, exact uninstall, and retryable disconnect/read-only status
- Sanitized diagnostics export that excludes credentials, source URLs, checksums, paths, and save contents
- Automated unit tests covering storage/provider/release/accessibility contracts plus a clean API-35 emulator run of the Compose migration, navigation, Room, lifecycle, and accessibility instrumentation suite; debug APK builds, self-contained Windows Companion builds, and green Android/Windows CI verification on the latest main commit with superseded-run cancellation, bounded Gradle/build steps, and executable catalog-timeout coverage
- Friendly emulator selection with validated graphics profiles, launch-time profile handoff, PPSSPP free/Gold/Legacy detection for PSP, and unsupported-platform guidance
- Deterministic release manifest generation containing APK hash, size, channel, and optional rollback tag
- Network-aware offline catalog selection with a reactive accessible offline-mode banner, explicit degraded refresh states, bundled recovery after provider failure, and non-fatal optional metadata enrichment
- Release validation rejects unsupported tags, manifest channel/tag mismatches, and missing rollback tags for signed channels before publication

## Important remaining work for Blueprint 1.0

The active implementation goal covers all remaining software work below. Physical hardware, enclosure, thermal, cabling, and fabrication validation are tracked separately and are excluded from that software goal.

- Extend the emulator lifecycle to every officially supported platform group; complete managed-save and cloud-save acceptance separately
- Validate PPSSPP/Dolphin intent behavior and additional production emulator adapters on physical target devices
- Complete end-to-end authenticated WebDAV/S3 integration testing and physical recovery validation
- Validate authenticated cloud-save byte transfer against real WebDAV/S3 endpoints and real save adapters against production emulators
- Complete physical disconnect/unplug safety tests for confirmed SAF migrations
- Complete physical-device Compose accessibility/lifecycle validation and expand airplane-mode and failure-recovery scenarios (the hosted API-35 emulator instrumentation suite is now a required CI gate)
- Complete separately connected physical controller testing and long-duration Honor external-display/DeX soak validation (the connected-TV GameBox smoke route is verified)
- Complete Honor Magic6 Pro wired-TV/desktop-mode, charging/network, thermal, reconnect, and long-session soak testing with the user's deployment dock
- Configure signed production builds and execute real update-channel/rollback validation using generated release manifests
- Re-scope the physical hardware handoff for the selected Honor Magic6 Pro and confirmed dock before measurements, BOM, CAD, or prototype acceptance
- Extend the authenticated Windows managed-save/content synchronization beyond the completed debug LAN session with maximum-size transfer, disk-full, replacement, routed-network, MSIX signing/install, and physical Windows validation; bounded 6.3 MiB streaming and live cancellation now pass
- Validate physical LAN/controller streaming and host-probe behavior on target devices

## Build and test

Open the repository root in the current stable Android Studio, install Android SDK 36, sync Gradle, and run the app on an emulator or Android phone.

CI runs:

```text
testDebugUnitTest
assembleDebug
```

For local instrumentation, use the validated `GameBox_API35` Google APIs x86_64
API-35 image and run `connectedDebugAndroidTest` with
`ANDROID_SERIAL=emulator-5554`. Test counts and device-specific outcomes change as
the suite evolves; see the [project roadmap](ROADMAP.md) for dated results. Emulator
instrumentation does not replace an unlocked Honor test, and Honor phone-screen
results do not count as TV acceptance. The latest recorded physical-TV legal-homebrew
smoke flow launched Galaxy Patrol through NES.emu and returned to GameBox; controller
gameplay in that run used synthetic input, and the HONOR dock/touch controls remained
visible. Physical-controller, clean-fullscreen, representative save/restore, and
broader emulator-matrix acceptance remain open.

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

The bundled Galaxy Patrol NES content remains under the app-private legacy path `filesDir/installed/retro/galaxy-patrol/content/galaxy-patrol.nes`; it is not exposed as a public filesystem path. GameBox verifies its pinned SHA-256 and hands the scoped content URI to the standalone `com.explusalpha.NesEmu` app with `ACTION_VIEW`; Galaxy Patrol no longer defaults to RetroArch. The roadmap records that NES.emu 1.5.85 launched and rendered the fixture on the Honor's 1920×1080 TV output, with return to GameBox verified. This does not establish clean fullscreen (the HONOR dock and emulator touch controls remained visible), physical-controller gameplay, or save/restore continuity. RetroArch-specific code remains only for compatibility with explicitly constructed legacy capabilities, not as a platform default.

### Online metadata and box art

Runtime game enrichment and manual metadata candidates use Wikidata's keyless entity API for exact-title year/description/general imagery and Libretro's keyless named-thumbnail archive for console box art and screenshots where available. The obsolete TheGamesDB metadata scraper, API-key sync, and provider-health path have been removed; legacy cached platform IDs remain in the database schema for upgrade compatibility. Wikidata/Libretro lookups never provide game binaries. Discovery details can open storefront/source searches (Steam, GOG, Nintendo, PlayStation Store, itch.io, GitHub where applicable) and an explicitly configured Vimm's Lair discovery source. Vimm integration is limited to title discovery/navigation and a user-initiated browser-download handoff: GameBox watches completed Android DownloadManager entries matching the selected title, verifies the local file through the normal importer, moves it into managed Library storage, and removes the browser source only after successful registration. It never requests Vimm binary URLs itself or treats Vimm as a trusted checksum source; manual local import remains available. Artwork is accepted only over HTTPS and malformed or insecure results preserve existing/fallback imagery. Binary installation remains governed by the catalog's authorized HTTPS source and SHA-256 checksum.

### Importing authorized local copies

New legal local or browser-tracked imports use a console-scoped tree such as
`files/imports/psp/<game-id>/`. ZIP and 7Z archives are extracted into that
tree; only supported files are registered, and the source archive is removed
after successful registration. Content-only uninstall removes every
registered extracted file while retaining saves, metadata, and history.

### On-device file organization

GameBox keeps its working files in Android's app-private storage; these are not
loose folders in the phone's public Downloads area. The main layout is:

```text
<GameBox app-private files>/
  imports/<console>/<game-id>/   Imported game files (archives are not retained)
  installed/                     Bundled or managed install content
  saves/                         GameBox-managed saves
  save-backups/                  Safety copies of managed saves
  save-backup-manifests/         Integrity/index data for those copies
  emulator-saves/                Discovered emulator save snapshots
  emulator-save-backups/         Safety copies of emulator snapshots
  emulator-save-backup-manifests/
  save-conflicts/                Preserved files when restore finds a conflict
  database-backups/              Pre-migration database safety copies
  catalog/                       Small discovery/catalog cache
  datastore/                     Android-managed preferences and app state
```

These directories are created as needed; an absent directory does not indicate
missing data. The Room library database and Android-managed app state are also
private to GameBox and are not safe cleanup targets.

Use GameBox's Library import and content-only uninstall actions to manage game
files. Do not clear app data or delete these folders manually: doing so can
remove imported games, saves, recovery data, or the library database. Public
`Download/GameBox/` is only used for the bundled Galaxy Patrol test fixture.

Open a title in Store discovery and select **Import authorized copy**. The Android picker displays every document MIME type because console images such as `.xci`, `.nsp`, `.rvz`, `.wbfs`, `.chd`, and `.cia` are often exposed as generic binary files. GameBox then applies a strict format allowlist for the selected console before it reads, hashes, and copies the file into app-private storage. A successful copy is promoted into the persistent Library with its artwork and metadata, and supported emulator adapters receive a temporary read-only URI only after launch-time SHA-256 verification. Current profiles cover Nintendo handheld and home consoles, PlayStation families, Sega families, Xbox, Atari, Neo Geo, PC Engine, WonderSwan, arcade, retro, and homebrew formats. Unknown catalog platforms use the recognized console-format union while executable and script types remain rejected.

Import support means safe file acceptance, persistent Library registration, and launch handoff where an approved adapter exists; it does not decrypt protected content, provide firmware or console keys, or bundle an emulator. A Switch `.xci`/`.nsp` copy can be imported and retained in Library, but Play requires a compatible, legally configured emulator adapter, which is not yet implemented for Switch. For disc-based systems, choose **Import multi-file disc set** and select the descriptor plus every referenced track together. GameBox validates `.cue`, `.gdi`, `.mds`/`.mdf`, and `.ccd`/`.img` dependencies, commits the replacement atomically, retains checksums for every file, and grants the approved emulator temporary read-only access to the complete set.

### Cloud save setup

Open **Settings → Saves & Cloud Sync**, choose WebDAV or S3-compatible storage, and enter an existing HTTPS collection/bucket-prefix endpoint. GameBox appends a game-scoped `.gamebox-save` object name; do not put credentials, query parameters, or a filename in the endpoint. WebDAV uses Basic authentication, while S3-compatible storage uses AWS Signature V4 with the configured region. Credentials are AES-GCM encrypted with an Android Keystore key and are excluded from DataStore and diagnostics. In Galaxy Patrol Details, **Upload cloud copy** creates a checksum-protected envelope and **Restore cloud copy** verifies it before replacement; a different local save is first retained under app-private conflict storage. Real provider behavior still depends on server permissions and must be validated against the chosen endpoint.
