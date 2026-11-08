# GameBox OS — project audit and overhaul plan

What this repo does: GameBox is an Android living-room launcher for a phone connected to a TV. It combines a game library, discovery, downloads/imports, standalone-emulator launching, save management, media/PC shortcuts, and an optional Windows companion.

This plan assumes one owner, one docked HONOR phone, one TV, a Bluetooth/USB controller, and optionally one paired Windows PC. For performance acceptance, use a proposed 1,000-game normal library and a 10,000-entry stress library, with up to two large transfers. These are proposed test workloads, not measured limits of the current app.

Audit date: **28 October 2026**. Baseline: working tree on commit `44925167`, including existing uncommitted changes. This audit changes no application code, device settings, game files, or saves. It adds this document only.

### Implementation update — 28 October 2026

- Findings 1–3: implemented in the working tree. The installed homebrew primary action launches, an unconfigured cloud endpoint no longer blocks save-safe local uninstall, and cloud-copy labels no longer claim payload encryption.
- Focused JVM tests and debug/test APK builds passed. On the HONOR, isolated instrumentation tests passed for local-only uninstall/reimport and for refusing removal when a local backup fails.
- Debug APK `CDA8934F8076C1C35A91A8F899681C1F0CCE56F5D7A1AE74CB21B9EDC310A3C3` was installed and launched on physical TV display 16. A TV-targeted injected gamepad X event opened the installed Galaxy Patrol in NES.emu. The controller was disconnected, so this does not establish physical-controller acceptance. The HONOR taskbar reappeared after the cold launch.
- Configured cloud-provider failure, actual physical-controller input, and the full save continuity journey remain to be verified. Findings 4 onward remain open.

### Later implementation update — 28 October 2026

- Findings 4 and 5: added manifest visibility for melonDS and a registry/manifest contract test; replaced unsupported graphics-profile choices with guidance and an action to open the installed emulator. Focused JVM checks and both debug APK builds passed. On the physical HONOR TV, NES.emu opened from the new action. melonDS itself is not installed, so a real DS detection and launch remain unverified.
- Started the requested console-style visual overhaul: updated the shared backdrop and panels, Home hero and status cards, game-settings layout, and portrait-art backgrounds. Existing focus, transition, and reduced-motion behavior was preserved. Inspected Home, Library, and Store on the physical TV; additional screens and physical-controller navigation still require a full visual and interaction pass.
- Installed debug APK SHA-256 `63606031902F9408A80CEFC2FCF8968E050826E46D17BB01E668610E703B4F08` on the HONOR and verified it resumed on physical TV display 16. The OEM taskbar remains visible after a cold start.
- Continued the visual pass across Downloads, Settings, Media, PC, shared game-details, and discovery-details surfaces, reusing the existing Blueprint styling and shared artwork rather than adding a new UI framework. The main Home, Library, Store, Downloads, Media, PC, Settings, and game-details routes were visually inspected from captures taken on the physical 1920×1080 HDMI display (display 16).
- `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest` passed for this UI pass. Debug APK SHA-256 `5E5077639E60397883A8F1F2EED37B2878B9E741BB9B90645EE23D0B20D5E3E6` was installed on the HONOR for TV inspection. `git diff --check` reported no whitespace errors; Git printed only its configured LF-to-CRLF notices.
- Remaining hardware issue: the HONOR desktop taskbar is visible over the bottom of the game-details screen and covers part of the controller footer. Input diagnostics exposed no connected gamepad, so physical controller navigation was unavailable. Captures and any injected input do not count as physical-controller verification; deep dialogs and every settings subsection were not exhaustively inspected.

## Recommendation

Keep the existing stack and its data-safety mechanisms. Finish a reliable install → play → save → exit → uninstall → reinstall loop before expanding platforms or adding more visual effects.

The project needs consolidation around its existing services, consistent user actions, and stronger acceptance evidence. It does **not** need a Kotlin rewrite, a new UI framework, a replacement database, or a new collection of dependencies.

## 1. Scope and evidence

The blueprint and `UPDATED_PROJECT_GOAL.md` describe the broad product. Later user decisions determine the current target:

- Primary deployment is the HONOR phone through a dock/wired projection to a TV. There is no separate Android TV product requirement.
- Actual TV output is the hardware acceptance surface. Emulator CI remains useful software evidence, but does not prove HONOR desktop-mode behavior.
- Prefer standalone emulators; PS1 uses DuckStation. Do not build a new RetroArch-core installation experience.
- Preserve console-grouped discovery, artwork, controller search, ZIP/7z imports, tracked downloads, verified source cleanup, and save-safe uninstall.
- Keep Windows, Media, and PC functionality as secondary lanes. They are existing blueprint features, not candidates for silent deletion.

Inspected areas: Android entry points, navigation/input, display policy, Details/Store/import UI, download controllers/workers and persistence, import/extraction/publication/recovery, Room schemas/repositories, save/cloud operations, metadata/artwork clients, emulator registry/intent policy, companion listener/protocol, Windows library storage/UI/core, build/release workflows, and documentation/test structure.

Repository inventory: **731 tracked files**, **309 Android production Kotlin files**, **211 JVM test files**, **94 Android instrumentation test files**, and **26 Windows C# files**. File counts describe scope; they are not themselves evidence of bloat.

Evidence limits:

- This was a risk-focused repository audit, not a claim that every line of every file was read.
- No fresh full test suite, live provider request, or hardware journey was run for this planning request.
- The latest recorded Android unit run had 781 tests, zero failures/errors, and one skipped test. APK assembly, instrumentation-APK compilation, and lint passed in the preceding implementation session. Compilation is not execution of the controller tests.
- The previous TV session verified the current debug package's Vimm correction and Galaxy Patrol → NES.emu → GameBox handoff. Physical-controller acceptance and the complete save/import lifecycle remain open.
- An audit of dependency vulnerabilities, signing-secret configuration, hosted CI state, cloud accounts, and published releases is outside this source-only pass.

### Keep these foundations

| Existing foundation | Why it should stay |
| --- | --- |
| Compose, Room, DataStore, WorkManager | Appropriate for a local Android appliance; already integrated. |
| SHA-256 verification, bounded network/file reads, scoped FileProvider access | Important protection around large imported/downloaded files. |
| Import transaction and registration journals | Recover file publication and database registration after interruption. |
| Content ownership manifests, mutation gates, content-only uninstall | Prevent removal of saves and unrelated files. |
| Atomic save snapshots and conflict copies | Useful recovery primitives; strengthen integration rather than replacing them. |
| Companion TLS pinning, signed authorization, replay guard, bounded staging | Preserve these protections while improving concurrency and UX. |
| Original icons, focus treatment, reduced-motion support, controller keyboard | Existing UI assets and interaction work are reusable. |
| Commons Compress plus XZ | They serve the requested 7z/LZMA2 import path; no removal is justified. |

## 2. Findings and required changes

Priority: **P0** blocks a trustworthy core flow or user-facing claim; **P1** is core reliability/integration work; **P2** is production hardening or measured optimization. Confidence below distinguishes source-confirmed behavior from a hardware gate.

### Must fix

### 1. Installed fixture actions can reinstall when the footer says Play — P0

**What this is:** Hero, footer, and lower Details controls decide the primary action separately.

**Problem:** `detailControllerActionPlan` labels an installed game “Play,” but `performPrimaryAction()` checks `isAuthorizedFixture` first and always calls `authorizedDownloadController.install()`. The installed fixture also showed an active “Install game” control in the previous TV session.

**Fix:** Choose the action from install state first, then select the appropriate installer only for install/retry states. Reuse that same action decision for hero, footer, and content panel.

**If skipped:** The most visible validation game can reinstall instead of launch when the user presses the advertised controller shortcut.

Evidence: [GameDetailsScreen.kt:182](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/GameDetailsScreen.kt:182), [GameDetailsContentPanel.kt:165](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/GameDetailsContentPanel.kt:165). Acceptance: installed fixture X/primary launches exactly once; no worker is enqueued; uninstalled/retry states still install; active work opens Downloads.

**Status (29 October 2026):** Fixed. `fixtureAction` routes installed Galaxy Patrol to Play, while the hero and Details content panel choose their action from install state. `AuthorizedInstallFeedbackTest.installedFixtureOffersOnePlayActionInsteadOfReinstall` checks the installed presentation and one Play callback; `DetailControllerActionPlanTest` covers the controller shortcut and fixture action states. The legal Galaxy Patrol TV lifecycle also launched NES.emu and returned to GameBox. A fresh physical controller X/A pass is still pending.

### 2. Optional cloud saves obstruct local uninstall — P0

**What this is:** Uninstall verifies a local save backup and attempts cloud protection.

**Problem:** With a managed save but no cloud endpoint configured, preflight reports cloud failure and removal attempts upload anyway. The user must acknowledge an unavailable service they never enabled.

**Fix:** Distinguish unconfigured cloud storage from configured-but-unavailable storage. Skip cloud upload only when unconfigured; always verify the local backup. Retain acknowledgement/failure protection for a configured service that fails.

**If skipped:** Normal offline/local users encounter misleading cloud warnings in the basic uninstall flow.

Evidence: [ManagedCloudSaveOperations.kt:38](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/storage/ManagedCloudSaveOperations.kt:38), [GameContentRemovalCoordinator.kt:76](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/storage/GameContentRemovalCoordinator.kt:76). Acceptance: local-only offline uninstall works with a verified local snapshot; configured cloud failure blocks removal until acknowledged; saves are unchanged in both paths.

**Status (29 October 2026):** Fixed for local-only accounts. `ManagedCloudSaveOperations` reports cloud protection as not required when no endpoint is configured, while the removal coordinator always makes a fresh local backup before deleting content. `GeneralContentRemovalTest.importedDiscSetUninstallAndReimportRetainProgressMetadata` passed on the HONOR; it confirmed `NOT_REQUIRED`, removed only the test game's content, and retained both the save and verified backup. Configured-but-unavailable acknowledgement remains covered by `CloudBackupUninstallGateTest`.

### 3. “Encrypted cloud copy” overstates the save format — P0

**What this is:** Details advertises encrypted cloud save copies.

**Problem:** `CloudSaveEnvelopeCodec.encode()` writes the save payload directly after metadata and a checksum. This is not payload encryption. HTTPS and encrypted credential storage do not encrypt the stored cloud object.

**Fix:** Immediately use truthful wording such as “Cloud backup over HTTPS.” If encryption at rest remains a product promise, specify authenticated encryption, key recovery/export, format migration, and cross-device restore before implementing it.

**If skipped:** Users make privacy decisions based on a protection the file format does not provide.

Evidence: [PerGameSaveCard.kt:82](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/PerGameSaveCard.kt:82), [CloudSaveEnvelopeCodec.kt:40](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/save/CloudSaveEnvelopeCodec.kt:40). Acceptance: labels/documentation match the actual format; any later encrypted format rejects tampering and supports recovery on a replacement device.

**Status (29 October 2026):** The misleading “encrypted cloud copy” wording has been removed. The UI calls it a **Cloud copy**; README and `docs/cloud-safe-uninstall.md` state that the save payload is not encrypted at rest, while credentials are encrypted with Android Keystore and cloud transport requires HTTPS. Payload encryption and cross-device key recovery remain future work if required by the product.

### 4. The DS emulator registry and Android package visibility disagree — P1

**What this is:** The registry offers `me.magnum.melonds` for Nintendo DS.

**Problem:** The manifest must declare every emulator package used by the platform registry because readiness relies on `getLaunchIntentForPackage()`. Missing a query makes an installed emulator appear unavailable on Android versions with package visibility restrictions.

**Fix:** Declare every approved standalone package needed for detection. Keep package visibility and the registry aligned, and verify DS launch on hardware.

**If skipped:** A user can install the recommended DS emulator and still be unable to select or launch it from GameBox.

Evidence: [EmulatorCapabilityRegistry.kt:97](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/launch/EmulatorCapabilityRegistry.kt:97), [AndroidManifest.xml:8](/D:/GameBoxOS/app/src/main/AndroidManifest.xml:8), [GameLaunchController.kt:148](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/launch/GameLaunchController.kt:148).

**Status (29 October 2026):** The manifest declares both `me.magnum.melonds` and `me.magnum.melondualds`. `EmulatorSettingsTest.approvedEmulatorsAreVisibleThroughManifestQueries` derives the approved package choices from every supported platform and asserts that the manifest exposes them. The HONOR TV matrix records a successful melonDS homebrew title-screen launch and return; WatermelonDS-specific handoff remains unverified.

### 5. Graphics-profile controls do not apply a graphics profile — P1

**What this is:** Game settings offer Compatibility, Balanced, and Performance choices.

**Problem:** Emulator-specific graphics profiles have no implemented configuration contract, so GameBox must not imply that a profile selection changes rendering.

**Fix:** Hide unsupported profile controls, explain that graphics are configured in the emulator, and offer Open emulator. Enable automatic profiles only for adapters with a tested way to apply and verify them.

**If skipped:** Users select a performance option that has no demonstrated effect on their game.

Evidence: [GameSettingsPanel.kt:105](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/GameSettingsPanel.kt:105), [EmulatorIntentPolicy.kt:11](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/launch/EmulatorIntentPolicy.kt:11), [GameLaunchController.kt:267](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/launch/GameLaunchController.kt:267).

**Status (29 October 2026):** The settings UI no longer presents Compatibility/Balanced/Performance choices as functional. It says to adjust graphics in the emulator and provides **Open emulator** when one is ready. Intent tests confirm GameBox reports graphics profiles as unapplied. Automatic per-emulator profiles remain unimplemented and are not advertised.

### 6. Manual imports are owned by a screen's coroutine scope — P1

**What this is:** Selecting a local copy starts extraction/import from a Composable.

**Problem:** `GameDetailsManualImportCard` uses `rememberCoroutineScope()` for the operation. Leaving that composition cancels its job. The recovery journals protect partially published state, but they do not make a large manual import a durable operation that continues in Download Manager.

**Fix:** Move accepted import jobs and their progress into the existing background-operation infrastructure. Persist URI access where available, job identity, stage, cancellation, and recovery state; make Details and Downloads observe the same job.

**If skipped:** Users can lose an in-progress import by navigating away, even though downloaded-copy imports use a durable monitor.

Evidence: [GameDetailsManualImportCard.kt:32](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/GameDetailsManualImportCard.kt:32), [DiscoveryImportWorkflow.kt:44](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/DiscoveryImportWorkflow.kt:44). Acceptance: leave Details, return, recreate the process, and explicitly cancel; every outcome remains visible and contains no half-installed library entry.

**Status (29 October 2026):** Implemented for both Library and Store detail import entry points. Imports now use unique WorkManager jobs, persist picker URI grants and progress in the existing download record store, appear in Download Manager, support cancellation, and release persisted grants after terminal completion. Focused unit tests and debug APK assembly pass. On the physical HDMI display, the Store manual-import action opened the SAF picker and Back returned to the same details screen. Full import completion, process-death recovery, archive cleanup, and cancellation rollback still need an end-to-end TV run with an unused legal test file; no existing game was overwritten during this pass.

**TV acceptance update (29 October 2026):** Imported the legal MIT @rkanoid GBA fixture through the GameBox picker on HDMI; GameBox showed it installed, and Play Now launched GBA.emu at Level 1. The completed row appeared in Download Manager as **Added to Library**. Force-stopping and relaunching GameBox preserved both the installed title and its completed import history. Corrected the section heading from “Completed browser downloads” to “Completed imports and downloads”; the updated APK rendered the corrected heading on HDMI. Android JVM tests and debug APK assembly passed. Import progress, process death during an active import, archive cleanup, and cancellation rollback remain unverified.

**Recovery update (29 October 2026):** Startup reconciliation now reschedules persisted manual-import records still marked `QUEUED`, closing the process-death gap between saving the record and enqueueing its unique WorkManager job. A unit regression test covers selecting only queued manual imports. The full Android JVM suite passed (816 tests, 0 failures/errors, 1 skipped), and the updated APK was installed and cold-started twice on physical HONOR HDMI. No queued import was present on-device, so recovery after an actual interrupted import and the full import/cancellation lifecycle remain unverified.

### 7. Windows library writes can overlap and some UI failures escape — P1

**What this is:** Several asynchronous WPF handlers persist the local JSON library.

**Problem:** `LibraryStore.SaveAsync()` uses the same `Path + ".tmp"` file for all calls without serializing writers. Rapid changes can overlap, and Favorite/Clear History/Remove handlers do not catch save failures; an I/O exception can escape an `async void` event handler.

**Fix:** Serialize persistence of immutable library snapshots, retain atomic promotion, clean failed temporary files, and surface save errors consistently in the UI. Avoid publishing stale snapshots over newer edits.

**If skipped:** Rapid clicks or storage errors can lose a change or terminate the companion.

Evidence: [LibraryStore.cs:43](/D:/GameBoxOS/windows/GameBox.Windows.Core/LibraryStore.cs:43), [MainWindow.xaml.cs:799](/D:/GameBoxOS/windows/GameBox.Windows/MainWindow.xaml.cs:799). Acceptance: concurrent favorite/history edits and simulated disk failures preserve a readable latest library and keep the window running.

**Status (29 October 2026):** `LibraryStore.SaveAsync` serializes writes by normalized full path across store instances and removes an unfinished temporary file after a failed save. Favorite, Clear History, Remove, and missing-entry cleanup restore the in-memory library and report an error if persistence fails. The Windows core self-check overlaps 24 writes through two separate `LibraryStore` instances sharing one path and simulates a blocked destination; both verify valid persistence and temp-file cleanup. The core harness passes, and the WPF app builds with zero warnings/errors. Desktop interaction with an actual disk failure and cross-process writer ordering remain unverified.

### Should fix

### 8. Metadata matching and cache identity are too weak — P1

**What this is:** Wikidata enriches missing metadata using an exact normalized title.

**Problem:** Lookup does not establish a video-game entity type or platform before taking the first matching entity. A title shared by a book, film, remake, and game can supply the wrong description/year/image. The title-only cache also stores failed/null lookups indefinitely and has no eviction despite its initial capacity of 128.

**Fix:** Match game identity using entity type plus available platform/year/external IDs; leave ambiguous results unresolved for manual selection. Bound the cache, distinguish absence from transient failure, and expire failures so reconnect/refresh can recover.

**If skipped:** Wrong metadata can persist, and a temporary outage can prevent later enrichment until process restart.

Evidence: [WikidataMetadataClient.kt:38](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/catalog/WikidataMetadataClient.kt:38), [WikidataMetadataClient.kt:59](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/catalog/WikidataMetadataClient.kt:59), [WikidataMetadataClient.kt:96](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/catalog/WikidataMetadataClient.kt:96). Tests must include same-title media, remakes, offline → online retry, and user-override preservation.

**Status (29 October 2026):** Failed lookups are not retained, and the access-ordered successful-result cache is capped at 128 titles. Automatic enrichment accepts only Wikidata entities whose `P31` includes video game (`Q7889`). A unique exact-title game match is used directly; if multiple remain, a unique match to the library's existing year or known platform can disambiguate. Platform identity uses Wikidata `P400` labels with aliases for supported consoles. If the source has platform claims but none match the library platform, automatic enrichment leaves metadata unchanged. Ambiguous results remain available in the manual picker, which now shows platform labels. Focused JVM tests cover cache retry/eviction, non-game filtering, ambiguous titles, year/platform disambiguation, and manual candidate labels.

**TV metadata recheck (29 October 2026):** Vimm search returned console-grouped results with box art, but opening the Mario & Luigi result showed **Extra source details unavailable** in GameBox. The corresponding Vimm Vault page rendered in the tracked browser. This points to detail hydration/parser or fallback coverage, not search-result artwork. Investigate the current Vimm page contract and provide a reliable metadata fallback; the actual game download was not selected.

### 9. Finish the gap between managed save copies and actual emulator saves — P1

**What this is:** GameBox has snapshot/restore primitives, a save-adapter registry, SAF tree configuration, and a manual single-file save card.

**Problem:** Restoring a managed copy explicitly does not restore the emulator's live save. That honest boundary is useful, but it is not yet the blueprint's verified resume-after-reinstall experience for each supported standalone emulator.

**Fix:** Wire and verify existing adapters where a supported shared-storage contract exists. Keep explicit export/import guidance for restricted emulators. Track game identity, save format, adapter version, discovered files, and verified restore separately from “managed copy present.”

**If skipped:** A successful GameBox restore can leave the user with a backup file but no resumed progress in the emulator.

Evidence: [PerGameSaveCard.kt:100](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/PerGameSaveCard.kt:100), [EmulatorSaveStorageController.kt:10](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/storage/EmulatorSaveStorageController.kt:10), [SaveAdapterRegistry.kt:3](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/storage/SaveAdapterRegistry.kt:3). Acceptance requires a real emulator-created save, reinstall, restore/import into that emulator, and visible resumed progress.

**Safety update (29 October 2026):** Automatic pre-launch recovery now restores only snapshot paths missing from the live emulator save root. It refuses to restore when live-save inspection fails, preventing a stale snapshot from overwriting newer progress after an interrupted session. The complete Android JVM suite passed (815 tests, 0 failures/errors, 1 skipped) and the debug APK was installed on HONOR HDMI. The real-emulator save/resume lifecycle remains unverified; explicit user-requested restore still replaces selected save files.

### 10. Unify the operation view and make source cleanup a consistent policy — P1

**What this is:** Authorized remote downloads use WorkManager/Room; browser transfers use Android DownloadManager plus a JSON preference store; manual imports have screen-local progress.

**Problem:** These separate owners require the UI to reconcile different status, retry, and progress models. Cleanup also differs: single-file imports take a cleanup flag, while disc-set registration deletes each selected source unconditionally.

**Fix:** Introduce one persisted operation/read model for the UI while retaining transport-specific workers. Define source cleanup explicitly: tracked downloads move after verified registration; extracted archives are deleted only after commit; manual-copy policy is visible and consistent for single files and disc sets. Record denied deletion as an actionable leftover, not import failure.

**If skipped:** Downloads can feel absent or inconsistent, and users cannot predict which selected originals will remain.

Evidence: [BrowserDownloadStore.kt:80](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/importer/BrowserDownloadStore.kt:80), [RoomDownloadRepository.kt:30](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/data/RoomDownloadRepository.kt:30), [DiscoveryImportWorkflow.kt:73](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/DiscoveryImportWorkflow.kt:73), [DiscoveryImportWorkflow.kt:131](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/DiscoveryImportWorkflow.kt:131).

**Status (29 October 2026):** Manual disc-set imports now remove only selected ZIP/7z archives after successful registration; raw CUE/BIN and other selected disc files are retained. The success message reports retained files and archive deletion failures. `SevenZipImportLifecycleTest` now covers both archive cleanup and preservation of raw disc-set sources. The updated debug APK (`227C6BB08563BB5D22BA1A6CA2F2268D024AD1A11F88EC5793A0D7879B4F1648`) passed that focused instrumentation test on the HONOR. A later TV reimport test exposed that **Locate files** still deleted raw ROMs. Repair now follows the same ZIP/7z-only policy and tells the user before selection; full JVM tests/builds passed, and the actual TV repair restored the game while preserving the shared SNES source hash. Unified status ownership and end-to-end interrupted-download recovery remain open.

### 11. A long companion transfer blocks other connections — P1 for the companion

**What this is:** The Android companion listener accepts and handles each TLS connection inside one loop on one executor.

**Problem:** Request processing includes reading the full upload and running its route before accepting another connection. One legitimate large transfer can therefore delay status/configuration requests from the same paired PC. Inactivity timeouts do not prevent this while bytes keep arriving.

**Fix:** Use a small bounded connection-worker pool and explicit transfer limits. Preserve game mutation locks, authentication, replay checks, and request-size limits.

**If skipped:** The PC can appear disconnected or unresponsive during an otherwise healthy upload.

Evidence: [CompanionEndpointService.kt:23](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/companion/CompanionEndpointService.kt:23), [CompanionEndpointService.kt:63](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/companion/CompanionEndpointService.kt:63). Acceptance: slow upload plus concurrent status request; second conflicting mutation stays serialized; disconnect/cancel releases capacity.

**Status (29 October 2026):** The listener now dispatches to a fixed four-worker pool with an eight-connection queue; overload is rejected with HTTP 503. On the HONOR, a stalled incomplete TLS request remained open while a separate authenticated status request returned 200 in under 1.5 seconds. Graceful executor shutdown also removed an interruption fatal trace exposed by the first device run. JVM tests, debug assembly, Android-test assembly, and the corrected focused HONOR test passed. A real large upload plus concurrent status request, conflicting concurrent mutations, and cancellation/capacity recovery remain unverified.

### 12. Publish truthful platform readiness and complete first-run TV setup — P1

**What this is:** Discovery presets/import formats cover more platforms than the standalone launch registry.

**Problem:** Being searchable or importable does not prove that a console is playable. Several requested systems have no configured launch option. HONOR fullscreen and physical-controller behavior also remain hardware gates; the per-app Dock action worked, but cold ADB restart restored the Dock.

**Fix:** Give each platform separate discovery, import, emulator-detected, setup-required, and hardware-verified states. Add a controller-first setup path for storage, emulator/firmware requirements, controls, save location, and TV routing. Expose supported limitations instead of enabling misleading actions.

**If skipped:** Users import a game and only discover afterward that GameBox cannot launch or configure it.

Evidence: [EmulatorCapabilityRegistry.kt:78](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/launch/EmulatorCapabilityRegistry.kt:78), [RomImportPolicy.kt:37](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/importer/RomImportPolicy.kt:37), [ROADMAP.md](/D:/GameBoxOS/ROADMAP.md). Preserve requested console visibility; do not label all visible consoles as supported playback.

**Status (29 October 2026):** The latest debug APK (`23C271D7A44327557F3F46FAF93DD9606D71E604883DBA7F9D200D441269B0B3`) is running on physical HDMI display 48 at 1920×1080. After install/launch the HONOR Dock returned; applying **Open in full screen mode** hid it, and the settled capture filled the TV without the Dock. It still requires reapplication after cold launch. The Bluetooth controller is detected; a physical A opened Luigi's Mansion details and B returned Home before the device-test incident below. No game was launched in that check.

**TV acceptance update (29 October 2026):** Installed the current debug variant alongside production and enabled HONOR **Open in full screen mode** for both GameBox and NES.emu. The Dock disappeared from the settled TV view and from NES.emu gameplay. Bundled Galaxy Patrol installed, appeared in Download Manager and Library, launched NES.emu 1.5.85, rendered gameplay, and returned to GameBox through **Close Content → Exit**. The session return message appeared. A force-stop/relaunch brought the Dock back; applying the per-app fullscreen setting again removed both Dock and transient window chrome from the settled TV capture. Physical controller input and other emulator fullscreen modes remain open.

**Update (29 October 2026):** The actual HDMI capture again shows the HONOR Dock over GameBox. The taskbar icon menu offered only **Start in Windowed Mode**, so it did not provide the fullscreen action. ADB could not start its local server in this pass, preventing another device settings check. The user could not provide physical A/X presses, so controller acceptance remains pending. The official Play! Google Play listing opened on TV but reported the app unavailable because it was made for an older Android version; no install or sideload was attempted. PS2 remains unverified. See `docs/HONOR_TV_EMULATOR_TEST_MATRIX_2026-10-28.md`.

**TV recheck (29 October 2026):** Reconnected to physical HDMI after restoring ADB access and installed the updated debug build. The HONOR app drawer's GameBox context menu offers **Add to Desktop**, **Pin to Taskbar**, and **Uninstall**; it has no per-app fullscreen choice. The taskbar menu still offers only **Start in Windowed Mode**. The Dock remains visible, and no global setting was changed.

**Dreamcast handoff update (29 October 2026):** The current debug APK was tested on HDMI display 48 with the MIT Chroma Circuit CDI. GameBox imported and verified the 1,528,713-byte copy, then Flycast v2.7 opened and rejected its granted `content://` URI as `Invalid URI`. The import was removed through GameBox afterward. No permissions were changed, and no game boot was achieved. Flycast's installed Android 35 package does not declare `MANAGE_EXTERNAL_STORAGE`; both the content-URI route and the earlier `file://` export route remain unresolved. Input for this run was ADB-injected, not physical-controller input. See the dated test matrix entry for full evidence.

### 13. Move operation ownership out of UI and consolidate duplicated decisions — P1/P2

**What this is:** There are useful feature helpers, but operation logic and presentation are still mixed.

**Problem:** The background browser coordinator imports `ui.DiscoveryImportWorkflow`; UI coroutine scopes own manual imports; Details has multiple action decisions. Wide/compact renderers can therefore drift even when they look polished.

**Fix:** Move import orchestration to the importer/application layer. Create feature-level state and commands shared by both renderers; keep layout differences in the renderers. Reuse a canonical platform identity/alias table across import, discovery, emulator, and artwork mappings.

**If skipped:** Each new console or action requires coordinated edits across unrelated screens and mappings, making regressions easier.

Evidence: [BrowserDownloadCoordinator.kt:17](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/importer/BrowserDownloadCoordinator.kt:17), [GameDetailsManualImportCard.kt:32](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/GameDetailsManualImportCard.kt:32). Keep the current Gradle module initially; do not split files or modules merely to reduce line counts.

**Status (29 October 2026):** Moved `DiscoveryImportWorkflow`, the imported-library record factory, and archive cleanup policy into the importer package. Browser-download and manual-import workers now depend only on that operation layer; UI screens call the same workflow. Focused import policy/registration/recovery JVM tests passed, the Android test APK compiled, and `SevenZipImportLifecycleTest` passed directly on the HONOR using its temporary fixture (`OK (1 test)`). The debug APK built and was verified top-resumed on physical HDMI display 48. Broader operation-state consolidation and end-to-end process-death recovery remain open.

### 14. Make builds, release gates, and device evidence reproducible — P1

**What this is:** CI builds and tests previews, while local validation uses a cached Gradle distribution.

**Problem at audit time:** There was no tracked Gradle Wrapper; workflows pinned Gradle 9.5.0 while recent local checks used 9.7.1. Normal Android CI does not run lint, and the signed-release workflow invokes debug unit tests plus release assembly. The earlier debug/production-package mix-up also invalidated multiple device conclusions.

**Fix:** Add one pinned wrapper and use it everywhere. Require lint, relevant unit/integration gates, artifact provenance, and signed upgrade/rollback checks before production. Use the existing package-aware HONOR install script and record package, APK hash, source tree state, display ID, emulator version, and physical versus injected input for hardware evidence.

**If skipped:** A green check or screenshot can describe a different build or a narrower test than the release claim.

Evidence: [android-ci.yml:42](/D:/GameBoxOS/.github/workflows/android-ci.yml:42), [release-production.yml:95](/D:/GameBoxOS/.github/workflows/release-production.yml:95), [install-honor-projection.ps1](/D:/GameBoxOS/scripts/install-honor-projection.ps1). Compile UI tests in CI, execute them on disposable CI devices, and reserve physical TV acceptance for the final deployment path.

**Hardware-test incident (29 October 2026):** An attempted `connectedDebugAndroidTest` run encountered two ADB aliases for the same HONOR; Gradle/UTP also failed creating a result path containing the IP serial's colon. During that setup the debug package's first-install time reset, and subsequent read-only inspection found its Room database contained no imported games and its private import folders were empty. Before setup, the debug Library visibly contained Luigi's Mansion, GBA homebrew, and N64 homebrew. The separate production package remains installed with its starter catalog, and shared N64/SNES homebrew originals remain on the device, but they do not restore the missing private imports. Temporary screenshots/test APKs were removed; no recovery source for the missing imported content was found. Do not use Gradle connected-device orchestration against this HONOR until duplicate ADB aliases are resolved and app-data preservation is verified; run isolated instrumentation with a guarded, explicit device flow instead.

**Build/lint update (29 October 2026):** Added the Gradle 9.7.1 wrapper with the official distribution SHA-256 and switched Android CI, instrumentation, preview, APK, and production workflows to it. The wrapper JAR hash matches Gradle's published checksum. Forced `:app:testDebugUnitTest --rerun-tasks` passed (816 tests, 0 failures/errors, 1 skipped), `:app:assembleDebugAndroidTest` passed, and `lintDebug` passed with 0 errors and 32 existing warnings. Android CI now runs `lintDebug`. A lint-discovered API-30 `Activity.display` access in game settings was replaced with the existing API-compatible display helper for the declared API 26 minimum. Hosted workflow execution, artifact provenance, and signed upgrade/rollback gates remain open.

### 15. Define Android backup/restore ownership explicitly — P2, before production

**What this is:** The manifest enables Android backup without explicit extraction/backup rules.

**Problem:** The application has large game binaries, durable saves/database state, Keystore-backed credential ciphertext, and device-specific pairing/display configuration. They do not all have the same backup and restore requirements. This audit did not test platform backup behavior or prove a data disclosure.

**Fix:** Specify and implement backup inclusion/exclusion rules. Separate recoverable library/save metadata from re-downloadable content and device-bound secrets. Make restored credentials/pairing request setup again when their keys are unavailable.

**If skipped:** Replacement-device recovery can be incomplete or misleading, even if a backup operation appears successful.

Evidence: [AndroidManifest.xml:41](/D:/GameBoxOS/app/src/main/AndroidManifest.xml:41), [AndroidKeystoreSecretStore.kt](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/settings/AndroidKeystoreSecretStore.kt). Acceptance: restore to a clean profile, retain intended saves/history, and show explicit reauthentication rather than silent failure.

### Measured optimization work

### 16. Reduce serial search/enrichment waits — P2

**What this is:** All-console Vimm search runs platforms sequentially, and catalog enrichment iterates the full library serially.

**Problem:** A slow platform/provider delays unrelated results. Repeated refresh/import activity can also repeat metadata work. No fresh latency benchmark was run for this audit.

**Fix:** Show partial console results as they arrive, impose a small global concurrency limit, retain cancellation/generation guards, and deduplicate identical in-flight work. Enrich missing/stale fields incrementally instead of walking unchanged entries on every refresh; respect provider rate limits.

**If skipped:** A single slow endpoint makes the whole search or enrichment experience look stalled.

Evidence: [VimmLairSearch.kt:46](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/source/VimmLairSearch.kt:46), [RoomGameRepository.kt:100](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/data/RoomGameRepository.kt:100). Measure first-result time, completion time, request count, cancellation latency, and offline-cache behavior before/after.

**TV search acceptance (29 October 2026):** The actual HDMI UI searched `MAIO`, returned Mario titles with box art, and exposed an incorrect **Mais** correction. Suggestions now prefer a uniquely most frequent close word in the returned titles and are suppressed on ties. The same query on the updated debug build suggested **Mario**; activating it reran the search and rendered 65 console-grouped results with artwork. This fixes the observed correction defect but does not measure serial-search latency. The complete Android JVM suite passed (810 tests, 0 failures, 1 skipped), and the debug plus instrumentation APKs built.

### 17. Decode artwork for its actual destination — P2

**What this is:** The custom loader already bounds downloads, bitmap dimensions, and cache sizes.

**Problem:** Thumbnail cards and large heroes share the same decode ceiling of 1600 pixels and a 16 MiB memory cache. Concurrent misses can repeat downloads/decodes; disk pruning scans and sorts the cache directory during publication.

**Fix:** Keep the existing bounds; add destination-size-aware decoding/cache keys and shared in-flight requests. Move pruning off individual image loads or batch it. Adopt a new image library only if measurements show the existing implementation would be more costly to maintain.

**If skipped:** Large libraries may waste memory and network work while scrolling, despite small visible cards.

Evidence: [RemoteArtwork.kt:56](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/RemoteArtwork.kt:56), [RemoteArtwork.kt:177](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/ui/RemoteArtwork.kt:177). Measure TV frame times, peak bitmap memory, cache hits, and duplicate requests; do not promise a speedup without the measurements.

### 18. Avoid unnecessary archive work and verify decoder limits — P2

**What this is:** ZIP/7z extraction has path/file-count/output-size safeguards and staging cleanup.

**Problem:** The 7z path opens/scans the archive to calculate a progress total and opens it again for extraction. The importer should also explicitly validate memory/time behavior for large solid archives; output-byte limits alone are not evidence of bounded decoder resource use.

**Fix:** Benchmark a single extraction pass with an estimate or indeterminate total, and retain byte/file/path limits. Inspect the installed decoder's memory-limit options before selecting a budget. Add cancellation and memory-pressure cases for large/solid/encrypted/malformed archives.

**If skipped:** Large imports can do avoidable work or have unverified memory behavior on the deployment phone.

Evidence: [RomArchiveExtractor.kt:89](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/importer/RomArchiveExtractor.kt:89), [RomArchiveExtractor.kt:157](/D:/GameBoxOS/app/src/main/java/com/gamebox/os/importer/RomArchiveExtractor.kt:157). This is an optimization/hardening task, not a reproduced archive crash in this audit.

### Cleanup

### 19. Separate active status, historical evidence, and generated files — P2

**What this is:** The working directory contains many captures, temporary emulator configurations, build caches, and game folders. The roadmap and audit log are roughly 380 KB and 443 KB respectively.

**Problem:** The README still describes 11 October blockers that later evidence superseded. Large chronological logs obscure the current backlog; untracked capture/game directories are not fully covered by `.gitignore`.

**Fix:** Keep one short current-status page and one prioritized backlog with requirement IDs. Archive dated investigation logs without losing evidence. Ignore generated captures, `tmp/`, and user game directories while retaining the explicitly licensed fixture and source assets. Remove the root scratch `test.txt` and obsolete helpers/tests only after exact reference checks.

**If skipped:** The next developer can test an old build, follow stale setup instructions, or accidentally stage user content.

Evidence: [README.md](/D:/GameBoxOS/README.md), [.gitignore](/D:/GameBoxOS/.gitignore), [ROADMAP.md](/D:/GameBoxOS/ROADMAP.md), [audit-follow-up.md](/D:/GameBoxOS/docs/audit-follow-up.md). No files should be deleted as part of this plan without a scoped inventory.

## 3. What to add to the user experience

These additions complete existing promises; they are not a request for a larger product.

| Addition | Minimum complete behavior |
| --- | --- |
| TV-first setup | Detect external display, controller, storage permission, emulator availability, and save access; guide only the missing steps and allow safe deferral. |
| Capability/readiness matrix | Distinguish searchable, importable, launchable, setup-required, and hardware-verified for every requested console. |
| One operations view | Show downloads, extraction, verification, registration, and cleanup with persistent progress, retry/cancel, and actionable leftovers. |
| Honest per-game readiness | Explain missing emulator/BIOS/firmware/configuration before Play; keep user-owned firmware outside bundled assets. |
| Save recovery journey | Show what GameBox can discover, back up, restore automatically, or export for manual emulator import. Never equate a managed backup with resumed gameplay. |
| Controller acceptance/setup | A/B enter/exit text mode, controller keyboard, focus restoration, reconnect, and per-emulator setup are verified on the real controller and TV. |
| Storage management | Show actual game directories, free space needed for staging, retained saves, removable content, and source files left because deletion was denied. |
| Failure/recovery UX | Clear empty/offline/permission-expired/missing-file/paused/checksum-failure states; preserve pending operations after returning from browsers/emulators. |

Do not silently disable device security to obtain an appliance-like wake flow. HONOR lock, Dock, HDMI reconnect, and emulator handoff behavior need a documented, verified deployment setup.

## 4. What to remove, retire, or defer

| Item | Action and boundary |
| --- | --- |
| Duplicate primary-action decisions | Replace with one action plan; retain separate layouts. |
| Fake-effect graphics controls | Hide until supported; keep explicit emulator-settings access. |
| “Encrypted” labels without encrypted payloads | Remove immediately; encryption itself needs a clear design/recovery decision. |
| Fixture-ID exceptions throughout product code | Gradually describe the legal sample through the same install/source policies as other games. Keep its licensed ROM and tests. |
| Legacy RetroArch setup/core-management paths | Stop expanding them. Migrate legacy per-game selections before removing compatibility code; preserve external saves and existing library identity. |
| TheGamesDB remnants | No production network client was found in this pass. Keep old database columns/migrations for compatibility until an intentional migration removes them; remove stale documentation claims. |
| Android TV as a separate deployment target | Remove obsolete product/setup claims where present. Keep wired-TV rendering, external-display hosts, controller keyboard, and useful CI display-size coverage. Do not delete files just because their names contain “TV.” |
| Root captures, `.ini` experiments, logs, `tmp/`, generated build output | Relocate/ignore or remove only after an inventory separates evidence, generated data, and user files. |
| User ROMs, BIOS files, saves, emulator settings | Preserve. Never treat these as automatic repository or phone cleanup targets. |
| Historical Samsung/A53/S23 and inconsistent HONOR enclosure assumptions | Archive old target-specific guidance; reconcile actual phone/dock identifiers before enclosure or electrical claims. |
| New storefront/companion/media feature expansion | Defer until the HONOR install/play/save loop passes; keep working existing features. |
| New frameworks/dependencies | None justified by this audit. In particular, do not replace Room/WorkManager/Compose merely for structural uniformity. |

## 5. Overhaul boundaries

Use the existing packages; create a Gradle module only when it supplies an actual isolation/build benefit.

```text
Compose screens + controller actions
              |
       Feature state/commands
              |
   Install/import/save/launch use cases
              |
Room operation/library state + existing recovery journals
              |
DownloadManager / WorkManager / SAF / standalone adapters
```

The UI observes operations rather than owning them. Transport implementations remain separate because Android DownloadManager, authenticated HTTP, and local SAF have different capabilities. They share user-facing state, stable IDs, content manifests, progress, and recovery conventions.

Consolidate platform identity and action rules before moving files. Keep database migrations, legacy aliases, and saved-game identity stable while doing so. Avoid a single giant “manager” that replaces several focused existing services.

For Windows, keep Core separate from WPF, serialize library persistence, and extract transfer/pairing/library screen state from `MainWindow` only along those responsibilities. The current console self-test harness is useful; add named, independently runnable tests around the failure cases instead of rewriting the entire harness first.

## 6. Ordered implementation plan

| Phase | Work | Exit condition |
| --- | --- | --- |
| 0 — Freeze the baseline | Record current diff and tests; preserve user files; establish pinned build/package/display identity; curate active backlog from historical logs. | A clean checkout builds reproducibly, and every acceptance record identifies the exact artifact. |
| 1 — Correct misleading/broken actions | Findings 1–5 and Windows write safety from 7. Fix action dispatch, cloud opt-in semantics, privacy wording, DS visibility, and unsupported profile controls. | Regression tests assert actual dispatched operations, not just labels; no unsupported setting claims an effect. |
| 2 — Make operations durable | Findings 6, 10, 13. Move import ownership out of Composables; unify observable job stages; preserve journal/manifest safety and cleanup policy. | Navigating away or restarting does not lose accepted work; cancellation and cleanup are predictable. |
| 3 — Prove save continuity | Findings 2, 9, 15. Test real emulator saves, local backup/export/restore, uninstall/reinstall, permission loss, and replacement-profile recovery. | At least the legal NES slice visibly resumes progress after reinstall; supported emulator families have explicit evidence or a truthful manual workflow. |
| 4 — Complete standalone readiness and TV control | Findings 4, 5, 12. Build the capability matrix and setup flow, then verify emulator launch, physical input, overlay suppression, return, cold start, and HDMI reconnect. | Every advertised playable platform has a named tested adapter/version; all basic flows work on the real controller and TV. |
| 5 — Improve metadata and measured speed | Findings 8, 16–18. Fix identity first, then optimize search, requests, image sizing, and extraction using recorded benchmarks. | No ambiguous automatic metadata overwrite; measured improvements with unchanged safety/recovery tests. |
| 6 — Harden secondary lanes and release | Findings 7, 11, 14, 15, 19. Finish companion concurrency/Windows error paths, backup rules, signed upgrade/rollback, documentation, and packaging. | Release artifacts, upgrade/migration, recovery, and hardware soak gates all pass for the selected deployment. |

Suggested first implementation batch: **1 → 2 → 3 → 4 → 5**. Each is small enough to review independently. Durable import/state changes come next; they should not be mixed with large visual or database restructuring in the same change.

## 7. Required acceptance gates

1. **Artifact identity:** Verify package, source commit plus dirty-tree state, APK SHA-256, phone/display IDs, and emulator versions. Debug and production have separate data; do not infer production upgrade behavior from debug success.
2. **Core loop:** First run → discover → import/download → verify → Library → configure emulator → Play → real controller input → create save → exit → return → uninstall content → reinstall → resume the same progress.
3. **Recovery:** App/process death at copy, extraction, verification, file publication, database registration, and source cleanup; no lost original, duplicate install, or corrupt library state.
4. **Transfer behavior:** Pause/resume with server Range support; explicit fallback when resume is impossible; retry after browser navigation/process restart; low storage and permission revocation.
5. **Storage:** ZIP/7z, multi-file discs, verified source deletion, external-storage disconnect/reconnect, retained saves, and accurate freed-byte reporting.
6. **Controller:** Physical D-pad/analog/A/B/X/Y/shoulders, long press/repeat, text entry/exit, dialogs, focus restoration, sleep/reconnect, browser and emulator handoff. Injected events remain separate evidence.
7. **TV hardware:** Dock/fullscreen startup, phone lock/unlock, HDMI loss/reconnect, charging plus controller plus Ethernet/SSD, and sustained gameplay/transfer soak. Do not use phone-screen or virtual-display tests to close this gate.
8. **Offline:** Cached Library/artwork/settings remain usable; failed metadata/source requests recover after reconnect without app restart.
9. **Companion:** TLS/pairing/replay/version errors, slow transfer plus concurrent status, cancellation, checksum mismatch, disk full, and reconnect.
10. **Release:** Required software checks; supported-schema migration; production signing/provenance; install/upgrade/rollback on the intended app ID; explicit pending hardware gates prevent a production-ready claim.

Cloud endpoint tests, licensed emulator firmware, final hub measurements, signing access, and a connected physical controller are external prerequisites when their corresponding phase is reached. Their absence must not block unrelated code work or be replaced by invented success.

## 8. Definition of completion

The project is complete only when the agreed capability matrix, core loop, failure/recovery cases, release gates, and actual HONOR TV deployment all have current evidence. Screen completeness, passing unit tests, and a successful APK build are necessary but insufficient.

**Verdict:** Keep the architecture's safety foundations. Correct actions and claims first, make operations durable, prove real save continuity, and then finish TV/controller acceptance and performance work.

**Lean:** No defensible net line-count saving is claimed before implementation. No dependency removal is justified. Remove redundant decisions, unsupported controls, stale code after migration, and generated clutter; keep checksums, journals, snapshots, migrations, and regression evidence.

**Not checked:** Every file line by line; fresh test execution; current hosted CI/release status; dependency vulnerability databases; real cloud storage; firmware/game compatibility across all consoles; physical controller, cold boot, final dock, and thermal/charging soak. Those remain explicit execution and acceptance tasks, not findings of success.
