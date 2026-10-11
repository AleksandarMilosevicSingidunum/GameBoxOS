# GameBox OS Roadmap

## 11 October 2026 — HONOR TV Vimm search and focus retest

- [x] On the physical 1920×1080 HDMI output, used GameBox's on-screen search
  keyboard to submit the legal/freeware term `Freedoom` to the configured Vimm
  source. The 16-console search completed with four results and offered
  **Did you mean “Freedom”?**; results were grouped by console.
- [x] Opened a result's details and followed GameBox's **Download** action to
  Vimm's Vault inside `TrackedSourceBrowserActivity`. Vimm's detail request was
  unavailable in GameBox, but the actual page loaded, and its Download button
  had the visible yellow focus cue on first load. Used **Back to GameBox**;
  GameBox confirmed it returned without starting a download.
- [x] One injected D-pad Right moved browser focus from the focused Download
  control to Vimm's footer copyright link. This is not physical-controller
  acceptance, but it shows directional WebView traversal is not intuitive on
  this page. No file was downloaded and the website's Download control was
  never activated.
- [x] Restored HONOR's per-app **Open in full screen mode** and maximized
  GameBox. Final HDMI capture had no Dock, taskbar, or window chrome.
- [ ] Bluetooth controller is still absent from Android's input inventory.
  Physical A activation and a lawful end-to-end Vimm transfer remain untested;
  do not test by activating a commercial game download.

## 11 October 2026 — Controller detection and navigation retest

- [x] On the physical HONOR TV session, queried Android's live input inventory:
  it exposed the touchscreen and device key/DPAD inputs but no GAMEPAD or
  JOYSTICK source. GameBox's TV Home correctly reported **Not connected**.
- [x] Focused JVM tests passed on the current checkout:
  `RuntimeDeviceStatusTest`, `GameBoxRootInputTest`, and
  `GamepadBrowserNavigationTest` (`:app:testDebugUnitTest`). This verifies the
  source filter and synthetic key mapping only.
- [ ] No code-side mismatch was established. Re-test with the Bluetooth
  controller connected to the HONOR; physical D-pad/A/B and Vimm Download
  activation are still not accepted until verified on the real device.

## 11 October 2026 — Galaxy Patrol Start/gameplay visual retest

- [x] On the actual HDMI TV, relaunched the verified Galaxy Patrol fixture in
  NES.emu 1.5.85 and tapped its visible **Start** touch control. The projected
  image changed, but captures alternated between a sparse starfield/sprite
  view and the earlier sparse view; a single D-pad touch did not establish a
  reliable directional response.
- [x] Closed content through NES.emu's confirmation and exited through its own
  menu. GameBox resumed on Galaxy Patrol details with the game still installed;
  the app was left in its current-session fullscreen mode.
- [ ] This is inconclusive for actual gameplay/rendering and input acceptance.
  No physical controller was connected; keep the NES fixture's gameplay quality
  and NES.emu control mapping as open acceptance gates.

## 11 October 2026 — HONOR TV 7-Zip import, launch, and cleanup

- [x] Used the user-provided PSP `.7z` from `D:\GameBoxOS\games` as the source;
  preserved the PC original and staged a separate temporary copy on HONOR.
- [x] Selected it through Android DocumentsUI on the TV. GameBox displayed its
  import progress and Cancel action, extracted/verified the ISO, registered it
  in Library with artwork (1,532 MB), and removed the staged archive only after
  successful import. SHA-256 was surfaced in the completion status.
- [x] **Play Now** handed the app-private content URI to PPSSPP v1.20.4 and the
  game title menu rendered on the 1920×1080 TV. Exited through PPSSPP's own menu
  and returned to GameBox without entering gameplay or changing saves.
- [x] GameBox's uninstall removed the one imported content file (1,605,664,768
  bytes). It left the expected stale library reference; the app's explicit
  **Forget file references** action removed that QA entry. Verified the source
  folder was empty, the app-private import directory contained no game file,
  and removed only the empty temporary staging folder. PC source remains intact.
- [ ] PPSSPP first appeared with HONOR window chrome/Dock and required the
  device's per-app **Open in full screen mode** plus maximize action. The
  emulator's touchscreen controls remained visible. Controller is disconnected,
  so physical input and hiding touch controls safely are still unverified.
- [ ] This did not exercise an actual Vimm transfer, gameplay/save persistence,
  cancellation/error recovery on-device, or production package behavior.

## 11 October 2026 — HONOR TV recovery of missing legal homebrew

- [x] After a fresh debug-app launch, GameBox showed Galaxy Patrol as **missing
  files**. Read-only inspection confirmed `files/installed` had no game file;
  did not infer why the file had disappeared.
- [x] Reinstalled the bundled legal fixture via the normal GameBox details
  **Install game** action. The page changed to **installed**, and the expected
  `files/installed/retro/galaxy-patrol/content/galaxy-patrol.nes` appeared; its
  on-device SHA-256 matched the pinned fixture hash
  `97c1757ffd6a5bc1a591809b2b0f8988741f61f6abd82889c148ecae8a2f471f`.
- [x] **Play Now** launched NES.emu 1.5.85 on the HDMI display. Used the
  emulator's **Close Content** confirmation, then its **Exit** action; GameBox
  resumed and Home showed Galaxy Patrol installed under **Continue Playing**.
- [ ] The game view was predominantly black with a small moving sprite and
  NES.emu touch controls. This confirms launch/handoff/return, not acceptable
  gameplay rendering or gameplay/save behavior. Gameplay controls were not
  tested. No physical controller was connected; HONOR's Dock appeared during
  gameplay and was absent again after emulator exit.

## 11 October 2026 — HONOR TV fullscreen restored after import QA

- [x] Rechecked the actual 1920×1080 projection after the import/uninstall flow.
  HONOR had left two GameBox activities/windows layered, with a smaller desktop
  window over the prior screen. Maximizing that window from its own title bar
  restored a single GameBox Home view.
- [x] Verified the final TV capture fills the display with no HONOR Dock,
  Android taskbar, or app window chrome. No app data or game files were changed
  during this recovery.
- [x] Force-stopped and relaunched `com.gamebox.os.debug` on the physical HDMI
  display. The TV taskbar returned despite GameBox's existing
  `WindowInsetsControllerCompat` immersive-bars request. Applying HONOR's
  per-app **Open in full screen mode** and maximizing hid it for that session;
  a second clean relaunch brought it back. This is now a reproduced OEM policy
  persistence limitation, not an unimplemented system-bars request.
- [ ] The app cannot currently keep HONOR's taskbar hidden across process
  restarts by itself. Controller input and Vimm download activation remain
  unverified.

## 11 October 2026 — HONOR TV Vimm focus and fullscreen follow-up

- [x] Repeated Home → Store → Vimm `GOD OF WAR` search on the physical
  1920×1080 TV, opened God of War: Chains of Olympus details, and used its
  GameBox **Download** action to reach the exact PSP vault page. The page's
  Download button received the visible yellow focus cue; a display-routed
  D-pad Down moved focus to the adjacent Play Online control. B returned to
  GameBox, which showed “Returned without starting a download.”
- [x] Download Manager remained at 0 active and 0 queued, with only the
  existing completed Galaxy Patrol entry. No website download was activated.
- [x] `GamepadBrowserNavigationTest` passed using the cached Gradle 9.7.1
  distribution and Android Studio's bundled JDK; this verifies key mapping,
  not physical controller input or a live website download.
- [x] Found HONOR's **Open in full screen mode** setting had reset for the
  debug GameBox package. Reapplied it and maximized the temporary window; the
  Vimm page then filled the TV with no Dock or window chrome, and B returned to
  fullscreen GameBox.
- [ ] Android still reports the physical Bluetooth controller disconnected.
  D-pad behavior was injected through ADB; real controller input and A
  activation were not tested. We intentionally did not activate the Download
  control for this copyrighted title. Repeat activation only on a lawful test
  download whose distribution is authorized.

## 11 October 2026 — Current Android JVM regression suite

- [x] `:app:testDebugUnitTest` passed on the current checkout: 780 tests,
  0 failures, 0 errors, 1 skipped. This supplements but does not replace the
  physical-Honor flows below.
- [x] Re-ran the full suite on this checkout: 781 tests, 0 failures, 0 errors,
  1 skipped. `:app:lintDebug` and `:app:assembleRelease` also completed
  successfully. Android lint wrote HTML and SARIF reports under
  `app/build/reports/`.

## 11 October 2026 — On-device 7-Zip importer lifecycle

- [x] Ran `SevenZipImportLifecycleTest` on the physical HONOR PGT-N19 / API 36
  (`connectedDebugAndroidTest`, 1 test, 0 failures). It generated a synthetic
  `.7z` archive in app cache and verified the extracted payload was registered
  before the source archive was removed; malformed input and missing input
  were rejected while preserving available source data. The test cleaned its
  temporary app-cache directory in `finally`.
- [ ] This exercises the importer on Honor, not the TV-visible DocumentsUI
  selection/progress/cancel flow. The physical controller is still unavailable;
  do not count this as end-to-end import acceptance.

## 11 October 2026 — HONOR TV game launch and return verification

- [x] On the physical 1920×1080 HDMI display, opened Galaxy Patrol details in
  the current `com.gamebox.os.debug` build (0.1.0-debug). Play Now launched
  `com.explusalpha.NesEmu` with the verified app-private
  `galaxy-patrol.nes` content URI; the homebrew rendered on the TV. This
  reproduces the working debug-build flow and distinguishes it from the older
  production 0.1.0 package, which did not launch in the preceding retest.
- [x] Exited the emulator through its own Exit action. GameBox resumed on
  Galaxy Patrol details without a crash or loss of the installed game.
- [x] Applied HONOR Dock → **Open in full screen mode** to NES.emu. The Dock
  disappeared for that emulator; GameBox's own per-app fullscreen setting also
  remains enabled. This is device configuration, not a setting GameBox can
  apply to third-party emulator packages automatically.
- [ ] NES.emu still displayed its touch-control overlay during gameplay. The
  controller was disconnected, so hiding touch controls or validating physical
  controller mapping/gameplay remains untested. Do not disable fallback touch
  input until a connected-controller path is verified.
- [ ] Physical controller acceptance and production-build game launch remain
  open. The active debug test did not modify production app data or game files.

## 11 October 2026 — HONOR TV manual import entry point

- [x] Fixed game details for listings without a verified installer: guidance
  now points to the on-page **Import your copy** action instead of suggesting
  an authorized install or sending users to Library.
- [x] On the physical 1920×1080 HDMI display, opened Cave Story details in
  `com.gamebox.os.debug` and verified the import card and guidance. Activated
  **Import your copy**; Android DocumentsUI opened on the TV in an empty QA
  folder. Back navigation cancelled cleanly and returned to the game details
  page with “No files selected.” No file was selected or imported.
- [ ] This verifies picker launch/cancel only, not actual file selection,
  extraction, registration, source cleanup, or physical-controller operation.
  The debug build is installed alongside production; production remains 0.1.0.

## 11 October 2026 — HONOR TV Vimm search, tracked page, and return

- [x] Reinstalled the current debug APK in place without clearing its data and
  launched it on the physical HDMI display. Used the SurfaceFlinger physical
  display ID for TV-only captures; Android's logical display ID is not accepted
  by `screencap` on this device.
- [x] Searched Vimm for `Cave Story`: one WiiWare result appeared in its console
  section. Opened details and activated GameBox's tracked **Download** action.
  The Vimm vault opened in `TrackedSourceBrowserActivity`; its actual Download
  control was focused with the visible yellow outline. No site download was
  activated. Vimm interposed a Cloudflare human-check before showing the vault.
- [x] Sent controller-B/back to return to GameBox details. It displayed
  “Returned without starting a download.” No transfer was started.
- [x] HONOR initially showed its Dock on the tracked browser and after return.
  Applied the per-app **Open in full screen mode** setting while the tracked
  activity was active, then maximized its window. Verified the page had no Dock
  or window chrome, and B returned to GameBox with the Dock still hidden.
- [ ] The physical Bluetooth controller remains disconnected; TV checks used
  display-routed touch/key injection. The focused Vimm Download was not
  activated (copyrighted game), so end-to-end browser download/import remains
  unverified. Vimm detail metadata enrichment also failed for this result.
- [ ] Artwork diagnosis: visible result cards already run the lazy exact-title
  Libretro box-art lookup. On TV, a second Vimm search returned nine God of War
  results grouped by console: both PS2 covers and the PSP *Chains of Olympus*
  cover loaded, while some PS3/PSP entries still showed initials. Cave Story
  WiiWare also showed initials after lookup. Cached artwork matching requires
  the same console, so the unrelated 3DS Cave Story cover is correctly not
  reused. Vimm's listing had no image and its detail lookup failed; a correct
  WiiWare cover remains missing.
- [x] `LibretroThumbnailClientTest` and `ConfiguredSourceResultTest` passed in
  the focused JVM regression run.

## 11 October 2026 — HONOR TV Vimm route and browser Back acceptance

- [x] On the physical Honor with the debug app on external display 12
  (1920×1080), searched `MARIO`, received 65 results grouped by console with
  artwork, and opened Mario & Luigi: Superstar Saga details. The details
  fallback was accurate; GameBox's Download action opened the tracked Vimm page.
- [x] Sent controller-B key input while the live Vimm page was open. GameBox
  returned to the details screen and showed “Returned without starting a
  download.” No site download control was activated and no ROM was downloaded.
- [x] On production, three R1 tab changes reached Downloads on the TV. It
  showed 0 active, 0 queued/paused, and one completed Galaxy Patrol marked
  Ready, with 436.3 GB free. No transfer was started or removed.
- [x] On the production 1920×1080 TV surface, D-pad/A navigation reached
  Media → PC Hub → Settings. The primary layouts rendered; Settings content
  followed focus into the Controller/Downloads/Emulators sections, and the
  installed Library showed Galaxy Patrol as available offline. No setup,
  launch, storage, or settings action was activated.
- [ ] This was TV-side Android key injection and touchscreen text/result
  selection, not a physical Bluetooth-controller acceptance; HONOR reports no
  controller connected. The focused instrumentation run crashed before
  assertions because `HwPCUtils#HwActivityStarterEx` force-stopped
  `com.gamebox.os.debug` while starting its test Activity.
- [ ] Production `com.gamebox.os` remains unchanged at version 0.1.0. Durable
  Download Manager → automatic-import behavior and physical-controller input
  remain unverified.

## 11 October 2026 — Full host verification and HONOR test-runner limitation

- [x] Android JVM suite: 780 tests, 0 failures/errors, 1 skipped; `:app:lintDebug`
  passes with 24 warnings and no errors; `:app:assembleRelease` succeeds.
- [x] Windows companion core self-test executable passes.
- [x] Restored the installed production `com.gamebox.os` app to Home on the
  1920×1080 HDMI TV. Its bottom Dock and transient window controls are absent
  after settling; production data was not modified.
- [ ] HONOR-targeted instrumentation remains unreliable in Desktop mode: the
  PC activity manager force-stops `com.gamebox.os.debug` as the runner starts
  its test Activity, killing the instrumentation process before assertions.
  This is a runner/display interaction, not proof of an app crash. Avoid using
  that mode's instrumentation results as acceptance until a runner that stays
  alive on the external display is available.
- [ ] Physical controller still reports disconnected; live Vimm browser →
  Download Manager → import and physical controller acceptance remain open.

## 11 October 2026 — Honor TV details and authorized-install verification

- [x] Captured the actual HDMI panel at 1920×1080: GameBox fills the TV, has no
  window border, and the HONOR Dock/taskbar is absent. Returned the live app to
  Home after testing; the final TV capture shows the launcher and legal-homebrew
  carousel.
- [x] Store exposes three legal/homebrew-style entries (Celeste Classic,
  Freedoom, OpenMW). Celeste Classic has no checksum-backed installer; the old
  detail copy incorrectly promised an authorized install and showed a disabled
  “Install” action. Updated the hero and content guidance to say that no
  verified installer is configured, direct the user to Library → import their
  own copy, and label the disabled action “No installer.” Screen-reader
  description now matches the visible guidance.
- [x] Ran `GameDetailsHeroTest` on the physical Honor: 11/11 passed, including
  D-pad focus from Play to Vimm Download and both no-installer regression tests.
  The duplicate `512 MB` test assertion now accounts for its two legitimate UI
  occurrences.
- [x] Temporarily installed the debug build and opened Celeste Classic on the
  HDMI TV. Verified the updated hero text, “No installer” state, and matching
  Library-import instruction visually. Removed the temporary debug package and
  restored the production app to Home; the settled production capture is again
  borderless and Dock-free.
- [x] Ran `AuthorizedInstallLifecycleTest` on the physical Honor: 1/1 passed
  for the authorized Galaxy Patrol install → uninstall → reinstall and save /
  backup retention flow.
- [ ] The tests used the debug instrumentation variant. The installed
  `com.gamebox.os` production app remains version 0.1.0; deploy a correctly
  signed build before treating the copy fix as live on the launcher.
- [ ] No external Bluetooth controller is enumerated; GameBox still reports
  `Controller: Not connected`. Automated key-event tests are not physical-input
  acceptance.
- [ ] Vimm integration currently provides search/discovery and opens the site;
  it does not download game binaries. Vimm direct-download/automatic-import
  acceptance remains open. No commercial game was downloaded.

## 11 October 2026 — Honor TV fullscreen and Vimm search flow

- [x] Read-only Android diagnostics identify the connected HDMI TV as active
  display 12 at 1920×1080. GameBox `MainActivity` is fullscreen on that display.
- [x] Used HONOR's GameBox Dock menu **Open in full screen mode** setting.
  The settled TV capture has no Dock/taskbar, and `dumpsys window` reports the
  HONOR `Dock` window as `GONE` (`mViewVisibility=0x8`) while GameBox remains
  visible. This per-app HONOR setting is left enabled to satisfy the requested
  living-room fullscreen behavior.
- [x] On the TV surface, the Home Vimm action opened Store; the Vimm source
  opened the controller search keyboard. Display-targeted Android D-pad/A/B
  key events moved Q→W, entered `W`, and closed the keyboard with the query
  preserved. Activating the Vimm source then displayed 628 results grouped by
  console with artwork. No game was downloaded; results included commercial
  titles and this run did not select a legal homebrew download.
- [x] A display-targeted R1 event switched Store to Downloads; the fullscreen
  view remained clear of the Dock and showed 0 active, 0 queued/paused, and 1
  completed item (Galaxy Patrol).
- [ ] Android reports no attached external gamepad; GameBox says
  `Controller: Not connected`. These synthetic key-event checks do not replace
  physical Bluetooth-controller acceptance. Vimm Download → durable transfer →
  automatic import and emulator play/return are still open.
- [x] The temporary Android UI-hierarchy diagnostic was removed from
  `/data/local/tmp` after inspection.

## 11 October 2026 — Honor TV reconnect and controller gate

- [x] Reopened GameBox on the connected physical `HONOR PGT-N19 API 36.0`
  from Android Studio and observed its wide 16:9 surface on the wired TV output.
- [x] The current TV capture has no visible GameBox title bar, but the HONOR
  Dock/taskbar still overlays the bottom edge. GameBox already requests
  borderless/immersive fullscreen; the per-app HONOR fullscreen setting was not
  confirmed applied. A menu-selection attempt did not prove that the Dock was
  hidden.
- [x] A misaligned UI click opened Cave Story details; no download or install
  was started. Android Studio exposes no accessibility tree for the TV content,
  and a single injected Right key produced no clear focus change.
- [ ] GameBox reports the controller disconnected. Physical D-pad navigation,
  Vimm download-to-Download-Manager import, emulator play/return, and TV
  fullscreen/Dock acceptance remain unverified. ADB server startup failed on
  both ports 5037 and 5038, so this run did not use ADB.
- [x] `GameBox.Windows.Core.Tests` passes when its test temp root is redirected
  from the sandbox temp directory into the writable workspace.

## 11 October 2026 — Prefer the game-specific Vimm download action

- [x] Updated the tracked browser's initial focus selection to prefer a visible
  action explicitly labeled “Download this game/file/ROM” over a general site
  navigation link that only says “Download”; if the specific action appears
  asynchronously, the observer upgrades focus when it is inserted.
- [x] Extended the tracked-browser instrumented fixture with a preceding site
  navigation Download link, a hidden decoy, and a delayed game-specific action;
  retained its assertions for visible focus, controller-A trusted activation,
  and in-app popup routing.
- [x] Executed the production focus script in Node against a DOM fixture:
  game-specific target wins over the earlier generic link, hidden actions are
  excluded, generic fallback works, and focus upgrades when the real action is
  inserted after initial page load.
- [ ] Build/instrumentation verification is pending: Gradle fails to load
  `native-platform.dll`, and ADB cannot start its server in this session.
- [ ] Confirm the real Vimm page labels and the physical Honor/controller flow
  before treating this as a verified fix.

## 11 October 2026 — Reject duplicate payload names across selected archives

- [x] Fixed an import-integrity bug: two differently named archives in one
  disc-set selection could contain the same internal ROM filename, and the
  second payload silently replaced the first staged file. Archive promotion now
  rejects the collision and uses a non-replacing move.
- [x] Added a regression test that stages one archive, attempts the colliding
  second archive, and verifies the first payload's bytes remain unchanged.
- [x] Compiled the changed stager and its regression test with the Android
  Studio Kotlin compiler against cached project classes, then ran all 13 cached
  importer test classes with JUnitCore: 58 tests passed, including ZIP/7z
  extraction, traversal, transaction recovery, and the duplicate-payload
  collision regression. Java temp files were directed to a workspace-local
  directory because the sandbox temp path denied normal file moves.
- [ ] The full Gradle suite and Android instrumentation still could not run in
  this shell: Gradle fails to load `native-platform.dll`; physical TV/controller
  and Vimm transfer acceptance remain separate open gates.

## 11 October 2026 — Windows companion verification and hardware gate recheck

- [x] `GameBox.Windows.Core.Tests` passes when its GUID-scoped temporary test
  root is redirected into the writable workspace (`tmp/windows-tests`). The
  default sandbox temp directory denied the atomic library-file replace; no
  product-code change was needed.
- [x] Built the WPF app to an isolated unique output directory with
  `BuildProjectReferences=false`: 0 warnings and 0 errors. An in-place build
  could not replace the existing apphost/DLL because the running Windows app
  holds those files open; it was not terminated.
- [ ] Android JVM tests could not start in the restricted shell because Gradle
  failed to load `native-platform.dll`; the elevated retry was unavailable due
  to the approval-service usage limit. The Honor is asleep, its built-in display
  is off, and no TV output is active, so no UI or controller acceptance was run.
- [ ] Blueprint hardware gates remain: physical controller navigation,
  Vimm/download-manager/import completion, representative emulator/save
  recovery, offline launch, and the long living-room soak.

## 11 October 2026 — Non-blocking catalog seed and Honor install/save slice

- [x] Fixed cold-start catalog seeding: bundled/remote catalog rows are committed
  before best-effort metadata requests begin. Previously, serial Wikidata and
  artwork lookups wrapped the seed provider and kept the library empty until
  every lookup returned. Deferred metadata writes are conditional so refreshed
  provider data and user overrides win over stale responses.
- [x] On the physical Honor, the previously timing-out
  `AuthorizedInstallLifecycleTest` now passes end to end: bundled legal homebrew
  install and checksum verification, Download Manager state, completed-work
  recovery, save import/backup, content-only uninstall, reinstall, and save
  restore. Four in-memory Room DAO cases also pass, including preservation of
  user overrides and rejection of stale metadata writes.
- [x] Full Android JVM suite: 779 tests, 0 failures/errors, 1 skipped;
  instrumentation compilation and `:app:lintDebug` pass. Only production
  `com.gamebox.os` remains installed after the test run.
- [ ] This is a non-UI synthetic vertical slice; emulator gameplay, live network
  transfer, TV/controller interaction, and the living-room soak remain open.

## 11 October 2026 — Honor importer cancellation and recovery checks

- [x] On the physical Honor, `AuthorizedRomImporterCancellationTest` passed:
  cancellation during a 4 MiB copy stopped before completion and removed staging
  files. `AuthorizedRomImporterStorageFailureTest` passed using an isolated
  unwritable-console fixture and left its obstruction untouched.
- [x] `RemoteDownloadRecoveryTest` passed with synthetic WorkManager states and
  a UUID-scoped synthetic payload: verified completion updates both persisted
  states once, uninstall remains authoritative, and missing/altered content
  fails closed. The fixture payload is removed after the test.
- [ ] These non-UI tests do not verify live network transfer, Android's
  DownloadManager/Vimm callback, TV UI/controller flow, or full emulator launch.

## 11 October 2026 — Honor save-discovery symlink safety

- [x] The host JVM symlink traversal test is skipped on this Windows account
  because it lacks symlink-creation privilege. Added and ran its directory-save
  discovery equivalent on the physical Honor; linked save files/directories and
  a recursive directory link were ignored/rejected, while an unrelated sentinel
  save remained unchanged. Fixture stayed in app cache and was removed.
- [ ] This is isolated filesystem coverage only; real emulator save discovery,
  backup/restore continuity, and TV interaction still need end-to-end acceptance.

## 11 October 2026 — Browser download import failure recovery

- [x] Discovery imports now return a typed success/failure outcome; the durable
  download coordinator no longer guesses import status from user-facing text.
  A missing/unopenable staged file remains FAILED in Download Manager.
- [x] The Android import lifecycle test asserts success, malformed-archive
  failure, and unavailable-source failure; all three passed on the physical
  Honor using isolated app-cache fixtures (no library/game files touched).
  Full Android JVM suite passes: 779 tests, 0 failures/errors, 1 skipped;
  `:app:lintDebug` and `:app:assembleRelease` pass.
- [ ] Verify DownloadManager completion through automatic import on Honor/TV,
  including Android permission-loss behavior; the device is currently asleep and
  only its built-in display is connected.

## 11 October 2026 — Vimm browser controller activation

- [x] Controller A is dispatched as a native Enter key directly to the active
  Vimm WebView, avoiding Activity/Compose focus interception while preserving
  WebView's user-gesture handling for download popups. D-pad Center remains
  unchanged.
- [x] Full Android JVM suite passes (779 tests, 0 failures/errors, 1 skipped),
  Android instrumentation sources compile, and `:app:lintDebug` plus
  `:app:assembleRelease` pass. The release APK is unsigned and not installed.
- [ ] The WebView controller regression and Vimm download still need physical
  Honor plus TV verification. The controller is disconnected and the Honor
  reports asleep with only its built-in display; no UI test was launched on the
  phone screen.

## 11 October 2026 — Standalone remote-content descriptor coverage

- [x] Added native NES, Game Boy, and Game Boy Color remote file types, plus
  missing catalog aliases (including DS/DSi and Sega Genesis); a matrix now
  checks every platform currently offered a standalone emulator against its
  default content extension, then checks the launch adapter matches the
  downloader's relative path and MIME type. The matrix caught the missing DS
  alias.
- [x] Added uninstall coverage proving downloaded standalone content removes
  only its descriptor-owned files and preserves unrelated files.
- [x] Full Android JVM suite passes: 779 tests, 0 failures/errors, 1 skipped;
  `:app:lintDebug` and `:app:assembleRelease` pass.
- [ ] This verifies path/MIME policy and buildability, not an authorized live
  download or emulator launch on the Honor/TV.

## 11 October 2026 — Phone-dock deployment manifest guard

- [x] Added a regression test requiring the ordinary Android launcher entry and
  rejecting Leanback launcher registration or required Android TV features.
- [x] Full Android JVM suite passes: 777 tests, 0 failures/errors, 1 skipped.
- [ ] This checks the install manifest only; Honor wired-projection behavior is
  still not verified while the phone is asleep and HDMI is disconnected.

## 11 October 2026 — Windows companion core and UI build

- [x] `dotnet run --project windows/GameBox.Windows.Core.Tests/GameBox.Windows.Core.Tests.csproj
  --configuration Release --no-restore` passes. This is a self-test executable,
  so `dotnet test` only builds it and does not execute the checks.
- [x] Windows companion UI builds with 0 warnings and 0 errors when directed to
  an isolated output folder; the normal output was locked by the already-running
  `GameBox.Windows` process, which was left untouched.
- [ ] This is compile/core-policy evidence only. Windows UI behavior and a live
  Honor-to-PC companion journey were not retested in this pass.

## 11 October 2026 — Long-form artwork platform aliases

- [x] Added missing Libretro system-directory mappings for Nintendo Game Boy
  labels and long-form Sony, Sega, and Microsoft platform names; short Vimm
  labels continue to resolve to the same artwork directories.
- [x] Alias regression test and full Android JVM suite pass: 777 tests,
  0 failures/errors, 1 skipped.
- [x] The current tree passes `:app:lintDebug` and `:app:assembleRelease`; the
  output is unsigned and remains uninstalled.
- [ ] This validates mapping policy only; live artwork coverage and TV rendering
  remain unverified, and Switch has no Libretro archive directory.

## 11 October 2026 — Vimm native controller activation regression

- [x] Controller A now reaches Vimm WebView controls as native `DPAD_CENTER`
  instead of a page-script `.click()`, preserving the site's user-gesture path;
  controller-opened popups remain confined to GameBox.
- [x] Android JVM tests pass (775 tests, 0 failures/errors, 1 skipped), and the
  WebView instrumentation fixture compiles with assertions for a trusted click
  and in-app popup.
- [x] `:app:lintDebug` and `:app:assembleRelease` pass; the output remains an
  unsigned APK and was not installed on the Honor.
- [ ] The instrumentation fixture was not run: Honor is asleep, HDMI is absent,
  and earlier Desktop Mode instrumentation destabilized the debug app. Live
  Vimm and physical-controller acceptance remain unverified.

## 11 October 2026 — WiiWare download descriptor and Vimm controller follow-up

- [x] WiiWare remote content now defaults to `.wad` with the Wii WAD MIME type,
  including Vimm download URLs without a file suffix; the policy test and full
  Android JVM suite pass (775 tests, 0 failures/errors, 1 skipped).
- [ ] Vimm download-button activation remains unverified against the live page
  and physical controller. Controller was unavailable; TV acceptance is pending.

## 11 October 2026 — Honor launch-session recovery runtime acceptance

- [x] `LaunchSessionRecoveryTest` passed all three physical Honor cases using
  temporary/in-memory Room databases: a confirmed session recovers playtime
  exactly once after reopen, an interrupted dispatch records no invented
  playtime, and the v11 session-table migration preserves game data.
- [ ] This validates persistence/recovery policy, not an emulator launch or
  the external-app return path; those still require the awake TV setup.

## 11 October 2026 — Honor authorized vertical-slice attempt

- [x] The first attempt timed out before the catalog row appeared. Investigation
  traced the wait to synchronous, serial metadata enrichment before the initial
  Room seed. This historical failed attempt was not counted as a pass; the
  follow-up fix and successful physical-Honor run are recorded above.

## 11 October 2026 — Honor persistence and recovery runtime acceptance

- [x] On the physical Honor PGT-N19, `DiscoveryDetailLookupTest` passed on a
  unique throwaway Room database: exact detail lookup survived close/reopen,
  recovered metadata/screenshots for row 301 beyond the first page, and
  returned null for a missing ID. No app library or display UI was touched.
- [x] `ReleasedSchemaMigrationMatrixTest` passed on the same device across
  every released schema version (v1 through current), preserving its sentinel
  row and confirming current tables/user_version. Its named debug-test fixtures
  were deleted by the test's `finally` cleanup.
- [x] On Honor, `BrowserDownloadStorePersistenceTest` passed: a synthetic
  tracked record survived store recreation and the original debug-app preference
  value was restored. `RemoteDownloadRecoveryTest` also passed against fake
  WorkManager states, covering successful completion, no duplicate replay,
  uninstall-state precedence, missing content, and checksum mismatch; its
  UUID-scoped synthetic file was removed in `finally`.
- [x] `PerGameSaveControllerTest` passed both save-isolation, import/backup/
  restore, failed-empty-backup, and lifecycle-lock cases using an in-memory DB
  and disposable cache roots. `SaveBackupSymlinkTest` passed both symlink
  defenses on Honor, preserving sentinel files outside the save tree.
- [x] `GeneralContentRemovalTest` passed all three Honor cases: exact managed
  multi-file uninstall with progress/metadata/save retention and reimport,
  fail-closed behavior when save backup fails, and symlink deletion defense.
  `ReimportIdentityIntegrationTest` also passed: different bytes were rejected
  without replacing the installed content/save; matching content reimported.
- [x] `AuthorizedRomImporterCancellationTest` passed on Honor: a synthetic
  4 MiB import was cancelled at the progress boundary and left no staged game
  content. `DownloadRepositoryIntegrationTest` passed on-device against
  in-memory Room, confirming queued updates submitted before observation still
  settle durably as completed with the final byte count.
- [x] `AuthorizedRomImporterStorageFailureTest` passed on Honor: an unavailable
  console directory is returned as a visible import failure without opening an
  unused URI or altering the isolated obstruction.
- [x] `HttpsCatalogFreshnessTest` passed both Honor runtime cases using fake
  HTTPS responses only: stale cache remains browseable after failed/malformed
  refresh, cache is scoped to its endpoint, and cancelling a blocked refresh
  disconnects its connection. `DiscoveryFavoritePersistenceTest` also passed
  on-device with in-memory Room rows.
- [x] `CompanionEndpointServiceLifecycleTest` passed on Honor/Android 16:
  the foreground companion service started, served certificate-pinned TLS with
  authenticated status, rejected plaintext HTTP, and stopped cleanly; debug
  settings/service state was restored.
- [x] Post-test package check found only the production `com.gamebox.os`
  package installed; instrumentation/debug test packages were not left behind.
- [ ] These runtime checks strengthen persistence/migration evidence only; they
  do not replace target-TV input, download/import, save/reinstall, offline, or
  long-duration acceptance.

## 11 October 2026 — Verification follow-up

- [x] Re-ran the offline Android gate after the latest changes: JVM tests,
  debug/instrumentation APK assembly, and lint all pass (774 JVM tests; 1
  skipped). Forced a fresh JVM execution afterward: 208 suites, 774 tests,
  0 failures, 0 errors, 1 skipped.
- [x] Re-ran the Windows core self-test executable; it passes. The first
  sandboxed run failed only when the harness performed its atomic temp-file
  move; the same binary passed with normal Windows filesystem permissions.
- [ ] The blueprint's release gate (pp. 6, 51, 67, 73) is broader than these
  software checks: save/reinstall continuity, offline launch, provider/network
  failures, external-storage disconnect, HDMI reconnect, and a 2-3 hour
  controller/emulator/media soak still need evidence on the target setup.
- [ ] Vimm controller navigation/download still needs a real TV acceptance
  run. A fresh ADB check still reports the connected Honor asleep, with only
  its built-in display; this pass therefore verifies code/build only, not a
  physical download.

## 11 October 2026 — Download UI test isolation and sleep-state check

- Made `DownloadsScreen` accept explicit browser-download records and isolated
  its synthetic state tests from the device's persisted user queue. Removed a
  redundant unbounded Compose idle wait; the suite still uses bounded status
  assertions. Debug APK, instrumentation APK, lint, and all 773 JVM tests pass.
- The three-case Downloads UI instrumentation did not complete on Honor. ADB
  reported `mWakefulness=Asleep` with the Notification Shade focused and no
  external TV display, so the run was stopped and is not counted as a pass.
  Repeat when the Honor is awake/projecting; do not substitute its phone screen.

## 11 October 2026 — Unsupported emulator guidance before Play

- [x] Added one capability query to the existing launch controller/registry so
  Details can distinguish “no approved adapter exists” from an installed game
  that is playable. Unsupported installed titles now show a disabled Play
  action and clear platform-specific guidance in the hero and content panel.
- [x] Added registry coverage for PS3, Switch, Xbox, Xbox 360, and unknown
  systems, plus a UI regression that confirms the unsupported Play action is
  disabled. The local gate passed: 774 JVM tests (0 failures, 0 errors,
  1 skipped), debug and instrumentation APK assembly, and lint.
- [ ] UI instrumentation and couch-distance wording still need verification on
  the awake Honor/TV; current device state is asleep and HDMI is absent.

## 11 October 2026 — Honor 7-Zip import lifecycle

- [x] Ran `SevenZipImportLifecycleTest` on the physical PGT-N19. A synthetic
  PSP `.7z` archive extracted and registered its payload before the source
  archive was removed; a malformed archive failed safely, retained the source,
  and created no game registration. All files were confined to a unique cache
  directory, so the personal library and saves were untouched.
- [ ] This was a device-runtime test without UI or TV projection; DocumentsUI,
  progress visibility, and TV/controller acceptance remain separate gates.

## 11 October 2026 — Vimm browser controller activation

- Reproduced on the Honor Magic6 Pro that controller A did not activate a
  focused Vimm-style Download link, even though the page focus indicator was
  present. The Android instrumentation test failed at the link click assertion.
- Updated the Vimm browser path to click the currently focused interactive page
  element on A/key-up while preserving its DOM focus; popup downloads remain in
  the tracked GameBox WebView. The focused browser-flow instrumentation test
  now passes on the attached Honor using a local HTML fixture and synthetic
  controller input. No game files were downloaded.
- Physical controller acceptance is still pending; the controller was
  unavailable for this run. TV projection was not verified in this test.

## 6 October 2026 — Honor HDMI emulator launch regression

- Launched the bundled Galaxy Patrol legal homebrew through GameBox on the
  physical Honor HDMI display. Android accepted the RetroArch activity launch,
  but HONOR initially placed it in a floating `hw-pc-freeform` task
  (`[192,103]–[1344,794]`) above GameBox and left its Dock visible; GameBox's
  fullscreen launch hint does not override this OEM behavior.
- Used HONOR's taskbar app menu → **Open in full screen mode** for RetroArch.
  After the transition settled, the game occupied the 1920×1080 TV surface,
  the Dock and RetroArch touch overlay were absent, and the Galaxy Patrol ship
  and stars rendered. This is an HONOR per-app setting, not a code-controlled
  mode; repeat for each external emulator installation. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-retroarch-fullscreen-settled.png`.
- One normal Back action did not exit RetroArch. Its HONOR Dock menu's normal
  **Quit** action did return to the existing GameBox MainActivity without a
  force-stop; controller/Back exit remains unverified. The controller was
  subsequently reported disconnected and raw `/dev/input/event13` monitoring
  captured no physical button events. No personal games or saves were touched.
  Evidence: `docs/evidence/hardware-run-2026-10-06/honor-tv-after-quit.png`.
- Corrected Settings → Display to explain HONOR's per-emulator fullscreen
  action, based on the observed Dock behavior, and that GameBox cannot set
  window policy for third-party apps. The targeted guidance unit test and
  `assembleDebug` passed; installed the update with `adb install -r` (app data
  preserved) and verified the text on the actual TV. The update reset
  GameBox's HONOR per-app Dock preference; the app is back on TV Home, while
  the Dock is visible. Global taskbar settings were not changed. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-display-guidance.png`.

## Product target

GameBox OS is an Android app running on the Honor phone and projected through
its dock to a TV. The app should feel like a full-screen, controller-first
console launcher. Android TV is not a deployment target; API emulators may be
used only as supporting software-test environments and do not count as TV or
Honor acceptance.

## Status snapshot — 5 October 2026

The codebase already has a useful launcher and legal-homebrew vertical slice.
The main missing pieces are end-to-end reliability on the actual docked-phone
setup, robust game-file lifecycle validation, and release-quality service and
emulator integrations. A feature is complete only when implemented, integrated,
automated-tested, exercised through its intended flow, documented, and merged.

### Refactor and UI polish (ongoing)

- Extracted the Home dashboard, console-mode setup banner, collection rails,
  status cards, and quick-launch tiles from `GameBoxApp.kt` into `HomeScreen.kt`.
- Moved Home hero/recents/install/focus selection into a Compose-independent
  presentation model with unit coverage. The current `GameBoxApp.kt` host is
  13,183 bytes; Home content lives in `HomeScreen.kt`.
- Store filtering/sorting, console policy, and Homebrew aliases now have pure
  selectors/policy tests; `BlueprintCatalogScreen.kt` compiles independently
  against the app's Compose dependencies. Store and Library now share keyed
  remembered filtering, with Library summary work keyed only to its game list.
  Home, Library, Settings, diagnostics, and system status share a refreshed
  storage-capacity snapshot; Downloads also uses it for preflight messaging.
  PC/Media hub behavior is represented by `AppHubKind`, not inferred from UI
  copy. Fullscreen bars use the modern insets controller rather than deprecated
  fullscreen flags. ZIP/7z source staging is bounded by the remaining import
  allowance, partial archive copies are removed on failure, and initial
  progress callbacks happen before source streams are opened. Archive and
  ordinary-file copy/extract/hash work now lives in `RomImportSourceStager`;
  the importer retains transaction, identity, registration, and rollback policy.
  Registration confirmation/rollback journal I/O is serialized with imports and
  recovery and runs on IO; post-Room transaction completion is non-cancellable.
- Remaining large implementation boundaries include Store orchestration,
  Settings state/lifecycle coordination, library/details flow, and import
  transaction orchestration.
  Store's configured-source and catalog-result detail routes are isolated from
  search/filter orchestration. Store console/result selection is now one
  persisted state holder that clears a discovery detail when its console
  changes. Settings' SAF controllers, asynchronous permission inspections, and
  external-display folder pickers are grouped in a storage-management state
  holder. Installed-content migration planning and confirmation also use a
  dedicated state holder; filesystem scans run on IO, and scan failures expose
  a retry path without allowing confirmation on an incomplete plan. Continue
  reducing Store orchestration and Settings state with regression coverage
  rather than splitting mechanically. The wide Settings section rail is now an
  isolated component, with a Compose interaction test proving focus does not
  change selection and activation does. The compact App Hub no longer carries
  unreachable wide-grid branches after its wide-layout early return, and the
  dedicated wide PC/Media presentation is separated from launch/state
  coordination.
- UI polish remains ongoing. The unlocked Honor Magic6 Pro is projecting to
  its physical 1920×1080/60 HDMI panel (not Studio's capture display). The
  current debug APK is installed with user data preserved. Home, Library,
  Store, Media, PC, and Settings were visually reviewed on that TV; the Dock is
  hidden in GameBox after restoring HONOR's per-app fullscreen setting. The
  Library's offline count now fits on one line, and long console names wrap
  across two lines in both Library and Store. The wide Home hero/collection
  cards were shortened so the entire Recently played row clears the persistent
  controller footer at the initial 1080p scroll position. PC Hub connection
  setup now precedes session history, putting the host fields and primary
  action in the first viewport. These changes were installed and visually
  verified on the physical HDMI output. A paired
  Wireless Controller remains disconnected, so physical controller navigation
  is still unaccepted.
  GameBox → Dolphin gameplay renders correctly once HONOR's window fullscreen
  control is used, but a fresh launch returns to windowed mode.

### Current verification snapshot

- **JVM:** 774 tests, 0 failures, 0 errors, 1 skipped in the latest local
  Gradle 9.7.1 run.
- **Android instrumentation:** the last recorded full API-35 Orchestrator suite passed on
  `emulator-5554`: 104 test cases, 0 failures, 0 errors, 1 skip. The skip is
  `SafSaveAdapterDeviceTest.configuredSaveRootRoundTripsARealDocument`, which
  requires a configured real document root. The suite includes lifecycle/save
  round-trip, wide-TV Play Now, Store refresh/error states, DocumentsUI 7z
  reimport, and the complete screenshot/navigation sweep. One screenshot check
  was corrected to assert the stable Details destination instead of requiring
  a specific install-state action on whichever home-hero title was seeded.
- The aggregate includes the PPSSPP settings/readiness test against the
  emulator's real PackageManager state, where no PPSSPP variant is installed.
- **CI:** the complete instrumentation suite has passed locally on the API-35
  emulator; a fresh clean-CI run is still required to close the CI gate.
- **Latest local Android gate:** `testDebugUnitTest assembleDebug
  assembleDebugAndroidTest lintDebug` passed with cached Gradle 9.7.1 and
  the installed Android Studio JDK. The latest run passed 774 JVM tests (0
  failures/errors, 1 skip); app and instrumentation APKs assembled. Lint reports
  24 warnings and 0 errors. Hosted CI and a full post-change instrumentation run remain
  outstanding.
- **Honor focused instrumentation:** the current debug and test APKs were
  installed with `adb install -r` (app data preserved). The physical Honor
  passed `ExternalDisplayActivityTest.externalDisplayActivityUsesBorderlessViewportAndCutoutPolicy`
  and `ExternalDisplayActivityTest.externalDisplayActivityReappliesBorderlessPolicyAfterConfigurationChange`.
  This verifies Android window/insets behavior on the handset, not TV rendering.
- **Honor/TV:** latest debug APK installed with `adb install -r` and app data
  preserved. A fresh post-install launch confirmed GameBox Home, Library, and
  Store on the physical 1920×1080/60 HDMI output; the HONOR Dock was restored to
  hidden mode. The entire Recently played row is now visible above the footer
  at initial scroll in
  `docs/evidence/hardware-run-2026-10-05/honor-tv-home-recent-row-fit.png`.
  Two-line console labels are readable in Library and Store in
  `docs/evidence/hardware-run-2026-10-05/honor-tv-library-console-labels-wrapped.png`
  and
  `docs/evidence/hardware-run-2026-10-05/honor-tv-store-console-labels-wrapped.png`.
  The compact offline-count row remains visible in
  `docs/evidence/hardware-run-2026-10-05/honor-tv-library-layout-after-fix.png`.
  The controller is bonded but `STATE_DISCONNECTED`. Dolphin still needs its
  window fullscreen control on each launch; its TV/game behavior has been
  recorded in the detailed hardware section below.
- **Immediate next gate:** reconnect the physical controller and validate real
  D-pad/A/B navigation and emulator mappings on the HDMI TV. Continue the
  standalone-emulator launch audit, prioritizing automatic borderless launch;
  emulator or injected-input results do not close either physical gate.

- **Latest focused Store test:** `CatalogFeaturedHeroTest` passed 2/2 cases on
  the API-35 emulator. The Store featured hero is now a separate composable.
  Configured-source action buttons now share one component across compact and
  wide catalog presentations. The post-change offline gate passed: 667 JVM
  tests (0 failures/errors, 1 skip), both APKs assembled, and lint completed
  with 11 warnings; the focused configured-source interaction suite passed
  3/3 on the API-35 emulator. These results do not replace Honor/TV validation.
- **Home boundary:** launcher/controller/storage state is now kept in the
  `HomeScreen` coordinator while dashboard composition lives in
  `HomeDashboardContent`; quick-launch text ellipsizes at constrained widths.
  Full offline gate passed again (667 JVM tests; app/test APKs; lint: 11
  warnings), and `HomeQuickLaunchLayoutTest` passed on API-35. The refactor does
  not yet have physical Honor/TV visual verification.
- **Import flow boundary:** Discovery Details now sends single-file and disc-set
  imports through one coroutine/progress/cancel/error/finalization path. Its
  configured-source filtering and browse-link resolution are also extracted
  into a tested policy. The latest offline gate passed with 669 JVM tests (0
  failures/errors, 1 skip), both APKs assembled, and lint at 11 warnings; the
  focused browser/download/import action suite passed 3/3 on API-35. Physical
  Honor/TV end-to-end import verification remains open.
- **Controller back consistency:** Android Back and controller B now share one
  tested dismissal priority for quick actions, status, profiles, and Details.
  Latest offline gate passed with 670 JVM tests (0 failures/errors, 1 skip),
  both APKs assembled, lint at 11 warnings, and root controller dispatch passed
  1/1 on API-35. No Android-TV-specific declarations remain in app sources.
- **Footer readability:** increased the controller footer's minimum height and
  constrained the profile/controller status stack to one visible line each.
  `ControllerFooterLayoutTest` verifies the status remains inside footer bounds;
  it passed 1/1 on API-35. A new Honor HDMI capture is still needed to verify the
  actual projection after the change.
- **Quick Actions focus:** the wide-layout dialog now focuses its first action
  on open, keeping the controller flow immediately actionable. The full offline
  gate passed (670 JVM tests, 0 failures/errors, 1 skip; app/test APKs; 11 lint
  warnings), and `QuickActionsInitialFocusTest` passed 1/1 on API-35. A physical
  Honor/TV focus check remains pending while the handset is locked.
- **Adaptive handset host:** `MainActivity` no longer forces landscape or opts
  out of resizeability; the external-display host retains its landscape
  contract. Home manifest tests passed 3/3, and the full gate passed with 670
  JVM tests and lint reduced from 11 to 8 warnings. A physical portrait/HDMI
  recheck remains pending.

### Shortest path to a complete everyday experience

1. **Make the TV session dependable (P0):** finish full-screen HDMI behavior,
   physical controller navigation, reconnect/overscan, and a complete play-and-
   return pass on the docked Honor. This is the deployment acceptance gate.
2. **Make getting games safe and obvious (P1):** finish ZIP/7z import, visible
   progress/cancel/retry, correct console folders, duplicate handling, and
   verified move/extract/uninstall behavior without losing saves.
3. **Make discovery trustworthy (P2):** complete Vimm's lawful discovery
   mappings, browser-download tracking into user-confirmed import, metadata and
   artwork fallbacks, offline/error handling, and controller-only search.
4. **Make play support predictable (P3):** validate emulator handoff and return,
   controls, saves, unsupported-system guidance, and recovery for each supported
   emulator; avoid implying that an import profile alone means a system is
   playable.
5. **Make the launcher comfortable for daily use (P4):** onboarding, library
   organization, accessibility, readable couch-distance UI, optional sound and
   motion, and sustained-session performance.
6. **Make it maintainable and shippable (P5–P6):** prove real backup/sync
   providers, then finish signing, updates, rollback, privacy/support guidance,
   compatibility notes, and any retained Windows companion scope.

The roadmap's detailed checklists below remain the source of truth. Do not
advance a stage merely because its UI exists: its exit criteria require working
flows and evidence at the appropriate test boundary.

### Existing foundation (evidence exists; scope is not overstated)

- Compose/Room catalog, persistent settings, WorkManager download pipeline,
  checksum-gated content promotion, scoped file handoff, save records, and
  conservative save-safe uninstall.
- Blueprint-inspired home/library/store/details/download/settings surfaces,
  controller focus/navigation, external-display host, and 16:9 layouts.
- Legal Galaxy Patrol homebrew Store → install → verified Library flow; launch
  dispatch to the installed RetroArch `RetroActivityFuture` has been observed.
- Import, re-import, save backup/restore, cloud-provider abstractions, Windows
  companion foundations, and Vimm's Lair legal discovery/browser-tracking
  foundations have code and automated coverage to varying degrees.
- Software layout checks exist at 1280×720, 1920×1080, and 3840×2160. These
  do not prove Honor OEM behavior, physical controller support, or gameplay.

## Roadmap to a complete user experience

### User-journey map: what is already usable vs. what is still missing

| Journey stage | Working foundation | Remaining experience gap |
| --- | --- | --- |
| First launch | Home/library/store shell, persistent catalog, controller-oriented layouts | Guided setup for storage, controller, display behavior, and where to get legal games; recoverable onboarding |
| Find a game | Store/catalog browsing, metadata adapters, Vimm legal-discovery preset, controller-driven on-screen search | Complete and verified console mappings; resilient metadata/artwork fallback and local correction; offline/error/retry states |
| Bring in a game | Authorized homebrew install, browser-download tracking, SAF import/reimport, ZIP/7z extraction foundations | TV-tested progress/cancel/retry; DownloadManager-to-import confirmation; safe handling of duplicates, bad archives, low storage, and permission loss |
| Organize the library | Room-backed catalog, managed paths, details, favorites/recent foundations | Consistent filters/sorting, editable metadata/artwork, robust duplicate identity, and predictable multi-console directory/import/uninstall behavior |
| Play | Galaxy Patrol → standalone NES.emu launch and return to GameBox on wired HDMI; launch-session recovery code and tests | Automatic fullscreen handoff on Honor, save/session persistence verification, a real controller pass, and additional console runtime checks |
| Protect progress | Local save records, checksum-verified backup/restore primitives, save-safe uninstall | Physical save round trip; visible backup freshness/status; conflict-safe recovery and a validated live cloud provider |
| Keep using it | Settings, download jobs, desktop-display host, Windows companion foundations | First-class update/recovery path, compatibility/support guidance, long-session stability, accessibility and performance acceptance |

This is a user-visible completion map, not a claim that each foundation is
finished. The priority sections below define implementation and evidence gates.

### P0 — Make the docked-phone TV session dependable

- [ ] Launch directly into the correct HDMI display and retain full-screen,
  borderless presentation across app resume, rotation/display reconnect, and
  launch of another app. Resolve Honor's freeform/task framing and bottom dock
  overlap where the OS permits; document unavoidable OEM chrome honestly.
- [ ] Verify the entire legal-game loop on the Honor/TV: Store → install →
  verified Library entry → Play Now → emulator → controller input → normal
  emulator exit → GameBox return. Preserve save data across uninstall/reinstall.
- [ ] Pair and test a physical controller: D-pad focus, A/B actions, shoulders,
  search without a hardware keyboard, reconnect, and clear on-screen hints.
- [ ] Reconfirm physical controller detection on the current Honor session.
  Earlier evidence showed the Nintendo Switch Pro controller name, but the
  latest HDMI screen reports “Not connected” and Android's live input-device
  inventory contains no GAMEPAD/JOYSTICK device. A button press was reported,
  but physical events and app navigation are not yet verified.
- [ ] Verify that physical D-pad/A/B/shoulder presses reach GameBox, move focus,
  activate search and Play Now, and recover focus after returning from an
  emulator. An event listener on the controller input device produced no events
  during the observation window; repeat while the user is actively pressing it.
- [ ] Verify overscan/safe-area behavior, HDMI unplug/replug, dock power/thermal
  stability, and a sustained play/idle session.
- [x] Hide the HONOR bottom Dock by selecting the app's per-app full-screen
  presentation; the actual 1920×1080 HDMI capture shows the GameBox footer
  clear of system chrome (`docs/evidence/hardware-run-2026-10-04/honor-bottom-space-final.png`).
  Persistence across app update, relaunch, or HDMI reconnect remains part of
  the separate full-screen continuity check above.

**Exit criteria:** recorded device/build/display details, screenshots or video
from the physical TV, logs for launch and normal exit, and a repeatable
controller-driven pass. No emulator-only result substitutes for this gate.

### P1 — Finish import, storage, and game lifecycle

- [ ] Exercise Android DocumentsUI on the projected setup for single files and
  ZIP/7z archives; show extraction/import progress, cancellation, errors, and
  retry without leaving partial installs.
- [ ] Prove archive contents land under the correct console/game directory,
  source archives are removed only after successful verification, duplicates
  are handled safely, and uninstall removes managed game files but retains saves.
- [ ] Re-import after uninstall and confirm stable game identity, metadata,
  artwork, save records, and launch configuration.
- [ ] Test low-storage, permission revocation, corrupted archive, unsupported
  format, interrupted copy, and external-storage disconnect recovery.

**Exit criteria:** instrumented file-integrity assertions plus a user-visible
DocumentsUI flow; no orphaned staging files or destructive cleanup on failure.

### P2 — Complete discovery and download journeys

- [x] Verify the Vimm preset's supported-console list and mappings against the
  app's selectable platform names; give unsupported systems an explicit
  explanation rather than a broken search. Unit tests verify the exact
  requested 18-console preset, that every preset name has a Store filter, that
  every supported Vimm console has a unique vault mapping/import profile, and
  that 3DS/Switch are explicitly skipped with an explanatory Settings message.
  Vimm remains discovery-only; browsing/download starts from an explicit user
  action. Rechecked with 4 passing `StoreConsoleTest` cases on 10 October.
- [ ] Test the in-app browser's user-initiated download callback → app-owned
  DownloadManager completion/failure → verified import → source cleanup with a
  redistributable homebrew game on Honor-to-TV hardware. Also cover duplicate,
  unrelated, interrupted, and unknown-filename downloads and app restart.
- [ ] Ensure catalog metadata/artwork has resilient fallbacks when providers
  omit, rate-limit, or return stale data; let users correct title/platform and
  artwork locally.
- [ ] Cover offline search, empty/loading/error states, refresh/retry, and
  controller-only search/input on the projected display.

**Exit criteria:** source-specific tests, legal homebrew end-to-end fixture,
and verified DownloadManager-to-import evidence; no copyrighted binary needed.

### P3 — Make emulator support predictable

- [x] Exercise RetroArch's normal Close Content/Quit path on the Honor and
  verify GameBox regains focus, clears the pending launch journal, and updates
  Galaxy Patrol play history after the host never paused.
- [x] On API-35 emulator, verify PSP Game Settings explains that PPSSPP must be
  installed and disables each unavailable approved package choice; this tests
  missing-emulator guidance only, not a PPSSPP game launch.
- [ ] Close automatic RetroArch fullscreen, emulator-save preservation, and
  physical-controller gameplay on the Honor/TV; the normal-return evidence does
  not prove those independent requirements.
- [ ] Validate a second adapter (PPSSPP or Dolphin) through real game launch,
  missing-app/core messaging, normal exit, and save backup/recovery.
- [ ] Make per-game emulator/core selection and graphics settings understandable
  and persistent; explain unsupported platforms before the user presses Play.
- [ ] Cover emulator update/permission changes and failed handoff recovery.

### P4 — Polish everyday launcher usability and accessibility

- [ ] Audit controller focus order and spoken semantics for import, browser
  tracking, download controls, uninstall/confirmation, saves, settings, dialogs,
  and every disabled/busy/error state at compact and TV dimensions.
- [ ] Add first-run setup for storage/library location, controller guidance,
  preferred display/home behavior, and legal-content expectations; keep defaults
  safe and allow setup to be revisited.
- [ ] Finish profile switching, favorites/recently played, library filters and
  sorting, metadata editing, and clear empty states without hiding installed
  games behind discovery failures.
- [ ] Verify startup/resume performance, reduced motion/sound options, readable
  text at couch distance, and consistent feedback for long operations.

### P4 user-facing acceptance checklist

- [ ] A first-time user can configure the library and controller from the TV,
  find a legal game, understand which sources are discovery-only, and recover
  from an empty library without guessing where files belong.
- [ ] Every long-running task (download, import/extraction, verification,
  backup/restore, sync) exposes progress, cancel/pause where safe, completion,
  and actionable failure/retry states using controller-accessible controls.
- [ ] All dialogs and destination screens preserve a predictable Back path,
  restore focus to the initiating control, and announce title/status/error
  information to accessibility services.
- [ ] Library operations remain understandable and reversible: identify the
  exact managed files and saves affected before uninstall, and never silently
  delete a source archive when verification or registration is incomplete.
- [ ] Couch-distance readability, reduced-motion behavior, optional sound,
  startup/resume latency, and sustained-session thermal behavior are measured
  on the actual docked Honor setup.

### P5 — Complete backup and sync integrations

- [ ] Validate live authenticated WebDAV and S3-compatible providers, including
  connection setup, TLS/auth errors, conflict policy, retry, and revoke/disconnect.
- [ ] Demonstrate encrypted/verified save upload, restore preflight, conflict
  handling, and offline/local-backup recovery on at least one real endpoint.
- [ ] Make sync status and backup age visible per game; never silently replace a
  newer local save.

### P6 — Ship and support the product

- [ ] Complete clean-checkout CI for unit, instrumentation, lint, dependency,
  and package checks; publish reproducible versioned artifacts and checksums.
- [ ] Set up production signing, upgrade path, update channel, release notes,
  rollback and recovery procedure, and privacy/data-retention documentation.
- [ ] Finish Windows companion production install/upgrade/uninstall and large
  transfer validation if it remains part of the supported product scope.
- [ ] Maintain a compatibility matrix for Honor model/OS, dock, TV resolution,
  controller, emulator/core, storage provider, and known limitations.

## Validation and release gates

1. **Automated:** focused JVM and Android UI/instrumentation coverage passes in
   clean CI; record exact test counts and device/API where applicable.
2. **Integration:** file, download, save, metadata, emulator, and provider flows
   use real boundaries or faithful test doubles as appropriate.
3. **Physical target:** Honor phone docked to HDMI TV, not phone-only screenshots
   and not an Android TV device. Record controller connected state and any OEM
   overlays on every relevant run.
4. **Release:** signed artifact installs/upgrades, preserves user data, and has
   a tested rollback/recovery path.

Current automated-test caveat: a full API-35 Orchestrator run has now passed
locally (104 cases, 0 failures, 0 errors, 1 SAF-document-root-dependent skip),
after earlier cross-test instability and retained-app-data issues. Keep the CI
gate open until a fresh clean-CI run is green; the local emulator run does not
replace that CI evidence or physical Honor/TV acceptance.

Hardware, external services, production credentials, and commercial-game
compatibility remain explicit evidence gates; do not describe them as complete
based only on source code or emulator tests.

## Roadmap use

Work top-to-bottom by priority, but keep distinct evidence lanes: repository
tests prove software behavior, emulators provide supporting UI/integration
coverage, and only the Honor Magic6 Pro connected through its dock to a TV
passes the deployment-hardware gate. Android TV is not a product target. For
each item, record implementation, test result, hardware result (if applicable),
and known limitations before checking it off. Do not count an enumerated
controller, compiled instrumentation test, or emulator screenshot as proof of
physical controller navigation or TV acceptance.

## 0.1 - Day-zero shell

- [x] Android and Compose project
- [x] GameBox dark TV theme
- [x] Controller-focusable cards
- [x] Home, Library, Store, Details, and Downloads mock screens
- [x] Fixture catalog and explicit install states
- [x] LB/RB tab switching and controller Back handling
- [x] Fake install, pause, resume, verify, and install transitions
- [x] CI-built debug APK with SHA-256 artifact
- [ ] Physical controller check on Honor Magic6 Pro docked to HDMI TV
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

Honor Magic6 Pro docked to an HDMI TV: controller and reconnect tests,
external-storage safety, thermal/soak testing, signed build, rollback
procedure, and documented hardware handoff.


## Latest verification

- [x] Blueprint-aligned focus, hover, press, and elevation motion on game cards
- [x] Optional HTTPS artwork and rich catalog metadata fields
- [x] TheGamesDB metadata enrichment adapter (API key supplied out-of-band)
- [x] Catalog artwork URL validation tests
- [x] Clarified emulator content-handoff failure messaging
- [x] Bundled authorized Galaxy Patrol NES fixture with pinned checksum and emulator-core requirement

Physical Honor Magic6 Pro/external-display/controller testing, production
signing credentials, real provider/emulator integration, and enclosure
validation remain hardware/deployment gates rather than locally verifiable
code tasks. A clean API-35 Android emulator executes supporting instrumentation
in CI; this does not replace physical-device acceptance testing.

## 2026-10-03 Honor wired-TV emulator and metadata checkpoint

- [x] Installed the latest debug GameBox build on the Honor PGT-N19 with app data preserved; Android unit tests, assembly, and lint pass.
- [x] Mapped Vimm preset NES, Game Boy Color, Game Boy Advance, SNES, N64, DS, PS1, and Dreamcast to explicit RetroArch core contracts. The corresponding eight core families were installed through RetroArch's Core Downloader on the Honor. NES and DS have real TV game-launch coverage; the other cores still need legal game-by-game launch checks. Game Boy/Color shares Gambatte.
- [x] Installed official Dolphin 2609 for GameCube, Wii, and WiiWare, and official Azahar 2126.1.2 vanilla for 3DS. Both open on the physical external display; no game launch is yet verified for either. Azahar's first-run setup remains pending.
- [x] Added keyless Libretro thumbnail lookup for boxart and gameplay screenshots, with Wikidata for descriptive metadata and exact-title artwork candidate search. TheGamesDB is no longer the game-detail metadata candidate, but the legacy Store bulk-sync and API-key settings remain and must be replaced before claiming full removal.
- [ ] Supply Switch artwork from a different source: the official Libretro thumbnail archive has no Switch directory. WiiWare currently probes the shared Wii artwork directory.
- [x] Enabled RetroArch's controller-aware overlay-hide setting on the Honor. Verify with a paired physical controller, including reconnect and overlay restoration behavior; the setting alone is not acceptance proof.
- [ ] Provide verified emulator/launch paths for PS2, PS3, Switch, Xbox, and Xbox 360; do not route these to an unrelated RetroArch core or an untrusted APK. PS3 is an experimental Libretro possibility, not validated on this phone.
- [ ] Make GameBox and every launched emulator truly borderless on Honor wired projection. The Honor desktop shelf/taskbar remains visible in the physical TV capture even after GameBox's fullscreen request. Dolphin can be made fullscreen using Honor's desktop-shell control; GameBox cannot hide Honor's system shelf through its app window policy, and each emulator's fullscreen behavior needs its own setting/verification.
- [x] Automatically backfill provider artwork for existing catalog/library entries and new imports using Libretro thumbnails before Wikidata, with an atomic update that preserves user artwork overrides. Exact Libretro archive covers replace older provider art when available.
- [x] Add distinct bundled cover illustrations for all eight starter-catalog titles that lack reliable provider box art. Verified the starter cards and an imported Nintendo DS title on the physical TV.
- [x] Removed the obsolete TheGamesDB API-key/settings and bulk-sync runtime. Existing Room platform IDs remain for upgrade compatibility; Switch still needs another artwork source, and imported cover/screenshot availability varies by source/platform.

## 2026-10-03 physical-TV UI pass

- [x] Reopened the installed GameBox build on the Honor's wired 1920×1080 TV display and reviewed Home, Store, Library, and a game-details screen. Navigation and layouts render at full TV resolution. Captures: `docs/evidence/gamebox-tv-checkpoint2.png`, `gamebox-store-tv2.png`, `gamebox-library-tv2.png`, and `gamebox-details-tv2.png`.
- [x] Confirmed the Android fullscreen request transitions GameBox out of multi-window on display 263. Honor's desktop shelf still remains visible below the app.
- [x] Replaced the wide Store's lower-page TheGamesDB health/API-key and bulk-sync actions with “Find more games”, “Search Vimm's Lair”, and “Refresh catalog”; installed and visually checked on the Honor's wired TV. Evidence: `docs/evidence/gamebox-store-footer-tv4.png`. Compact Store and Settings still have legacy TheGamesDB surfaces to remove.
- [x] Store copy and compact discovery heading now use plain game-discovery language rather than naming TheGamesDB as the user-facing discovery flow.
- [x] Physical TV walkthrough typed a query with the on-screen keyboard, ran a real Vimm search, opened a DS result, and reached the local-copy import workflow. The selected Vimm result's detail endpoint returned HTTP 404; the screen now presents a compact, sanitized fallback notice instead of a raw red exception. Unit tests/build/lint pass; repeat the details state on TV to visually verify the final notice.
- [x] Reinstalled the passing APK on Honor and reopened GameBox on the 1920×1080 TV. The Home UI fills its app window, but the Honor desktop shelf remains visible at the bottom of the captured frame (`docs/evidence/stale-detail-final.png`).
- [x] Reduced the extra external-display bottom inset from 44 dp to 20 dp so the Home viewport uses more TV height while keeping the controller footer above Honor's shelf. Unit tests, APK build, and lint pass; the installed wired-TV capture is `docs/evidence/gamebox-tv-final-layout.png`. A TV swipe brings the full Recently Played row into view (`docs/evidence/home-scroll-check.png`).
- [x] Rechecked the current Store on the installed Honor build at 1920×1080. The console rail, empty-catalog state, Vimm source action, and game-source cards render on TV without the old wide-layout TheGamesDB health panel (`docs/evidence/gamebox-tv-store-current.png`).
- [x] Verified the refreshed artwork on the physical wired-TV Home and Store: bundled starter covers and imported Mario & Luigi archive cover render in their cards (`docs/evidence/gamebox-boxart-tv.png`, `docs/evidence/gamebox-store-boxart-tv.png`).
- [x] Sent a D-pad-right event to the TV-hosted Home UI and confirmed focus advanced to the Xbox quick-launch card (`docs/evidence/gamebox-tv-dpad-nav.png`). This validates the TV focus path with Android's D-pad input source, not physical-controller pairing or reconnect behavior.
- [ ] Controller acceptance remains open: current Android input enumeration contains only `USB OPTICAL MOUSE` (mouse plus keyboard/joystick HID interfaces); no gamepad is presently registered. GameBox's “Not connected” status is accurate for this observed device state.
- [ ] Resolve the Honor desktop shelf at the OS/deployment level. On the current Honor build, Android reports GameBox fullscreen on display 263 with app/task bounds `[0,0][1920,1080]`; the visible bottom `Dock` is a separate `NAVIGATION_BAR` window owned by `com.hihonor.desktop.systemui`. GameBox cannot hide that system-owned window through its app window policy. Do not claim the shelf is hidden by GameBox.
- [ ] Repeat controller navigation with the actual Bluetooth/gamepad connected. At this pass Android enumerated only `USB OPTICAL MOUSE` with keyboard/joystick sources, while GameBox showed “Not connected”; the pad could not be tested in this state.
- [ ] Review remaining destination screens and end-to-end TV flows with the physical controller attached; keep the TV display as the acceptance target.

## 2026-10-03 remaining end-goal requirements

- [ ] Audit and install every compatible RetroArch core required by the 18-system default Vimm preset. Currently the Honor has the eight core families used for NES, GB/GBC, GBA, SNES, N64, DS, PS1, and Dreamcast; several still lack a legal per-core gameplay check. Core installation must use RetroArch's own updater because GameBox cannot write into another app's private core directory.
- [ ] Keep non-RetroArch systems on dedicated emulator adapters: PPSSPP (PSP), Dolphin (GameCube/Wii/WiiWare), and Azahar (3DS) are available/installed, but not all are launch-verified. PS2 has only a legacy AetherSX2 adapter and is not acceptance-ready. PS3, Switch, original Xbox, and Xbox 360 have no verified, supported Honor emulator path yet; do not offer false playable states or download untrusted APKs. PC-only upstreams and experimental Android forks are not equivalent to a supported TV path.
- [ ] Make emulator fullscreen an adapter-specific setting/launch contract and verify each supported emulator on the external display. App-level immersive flags do not control another app's window or Honor's shelf.
- [ ] Hide RetroArch touch controls during controller play and confirm the condition using the paired physical pad. RetroArch's controller-aware overlay-hide option is enabled, but Android currently reports no connected gamepad to GameBox, so this acceptance check remains open. For non-RetroArch emulators, verify each app's own touch-overlay setting.
- [x] Finish replacing TheGamesDB: runtime enrichment and user-confirmed metadata candidates now use keyless Wikidata/Libretro sources; obsolete API-key, health, and bulk-sync code is removed while cached Room IDs are retained.
- [ ] Add a Switch-capable artwork source and test metadata, boxart, and screenshots during local import; retain safe empty-image fallbacks.
- [ ] Resolve the Honor desktop shelf/taskbar at the bottom of the TV output through a supported Honor desktop-mode setting or deployment configuration. The current GameBox window fills the display area available to it; app code cannot remove system-owned shelf UI.

## 2026-10-03 box-art regression follow-up

- [x] Remote artwork loading now follows up to four validated HTTPS redirects (common for provider/CDN image URLs); rejected insecure redirects and bad cached image bytes fall back safely.
- [x] Libretro box-art matching now passes the stored game region for imported/library titles.
- [x] Focused artwork unit tests pass; the updated debug APK was installed on the Honor Magic6 Pro and checked on its physical 1920×1080 HDMI TV. Home and Library display the bundled Galaxy Patrol and imported Nintendo artwork (`artifacts/gamebox-art-check-20261003.png`, `artifacts/gamebox-library-art-check-20261003b.png`).
- [x] Strengthened the game-poster lower scrim so TV-sized Library titles and status text remain legible against detailed cover art; rebuilt, reran focused tests, reinstalled, and inspected the final Library view (`artifacts/gamebox-library-art-final-20261003.png`).
- [x] Renamed the Library's “Installed Games” shelf to “All Games” because the collection intentionally includes titles that still need an imported copy; verified the label on the physical HDMI TV (`artifacts/gamebox-library-label-verified.png`).
- [x] Fixed the Store featured hero to pass the title fallback to `RemoteArtwork`, so bundled starter covers (for example Cave Story) appear when no provider URL exists. Full unit tests and debug APK assembly pass; the APK is installed on the Honor with app data preserved.
- [x] Extracted the bundled-cover title lookup and added unit regression coverage for all eight starter covers plus unknown-title fallback; targeted test and APK assembly pass.
- [x] Previous behavior preferred bundled starter covers over provider URLs; this was superseded on 2026-10-03 because it masked real library/import artwork. Bundled covers are now offline/loading fallbacks, with remote art rendered above them.
- [x] Compact Library now labels its mixed installed/importable collection “All games,” matching the wide layout rather than implying every listed title is installed; full unit tests/build pass and the APK was installed on the Honor.
- [x] Raised the Home game-card bottom scrim and added a gradual mid-stop so landscape-card titles/status remain readable over bright artwork; identified from the physical-TV D-pad capture (`docs/evidence/gamebox-tv-dpad-nav.png`), full unit tests/build pass, and the APK was installed on the Honor.
- [ ] Recheck the featured cover on the physical TV after the phone is unlocked. The latest HDMI capture showed HONOR's “Phone is locked, please unlock” projection screen, so the installed change could not yet be visually accepted on-device.
- [ ] Recheck bundled starter covers and the compact Library label on the physical TV after the phone is unlocked.
- [ ] Visually verify the revised Home-card contrast on the physical TV after the phone is unlocked.
- [ ] Latest hardware recheck: HDMI is still active at 1920×1080, but Android foregrounds `LockScreenWindow`; input enumeration contains only `USB OPTICAL MOUSE` (plus system virtual devices), not the Bluetooth gamepad. Do not claim fresh UI or physical-gamepad acceptance until unlocked and the controller is enumerated.
- [x] Fixed transient box-art misses from becoming permanent for the app session: Libretro thumbnail lookups cache successful images only, so imported and preloaded entries retry after network recovery. A regression test exercises a miss followed by a successful retry; full unit suite passes, the debug APK builds, and it is installed on Honor with app data preserved.
- [x] Improved Libretro artwork matching for imported filenames with stacked region/language/revision suffixes by progressively removing trailing qualifiers while retaining region-first candidates; regression test and full unit suite pass, and the updated debug APK is installed on Honor.
- [x] Remote artwork cards now make up to three bounded attempts (immediate, 1.5 s, 4 s) before staying on the designed placeholder, so a transient CDN/network error can recover while the card remains visible. Added retry-policy test; full unit suite/build pass and updated APK installed on Honor.
- [x] Increased the lower artwork scrim on non-hero game cards to protect title/status contrast over bright, busy box art, based on the observed Honor/TV Home cards. Unit tests and app/instrumentation builds pass; new app build installed with data preserved.
- [ ] Verify the stronger cover-card scrim on the physical TV when the Honor is unlocked; current projection remains at the lock screen.
- [ ] Recheck library and preloaded-catalog covers on the physical TV once the Honor is unlocked; the device is reachable, but the external display currently shows `LockScreenWindow`, so this change has not had a fresh visual hardware acceptance pass.
- [x] Added a Compose regression test that renders the compact Library with a `NOT_INSTALLED` title and asserts it appears under “All games,” not “Installed games.” Instrumentation APK compilation and unit tests pass; execution on the Honor remains pending unlock.
- [x] Remote cover images now fade in over their placeholder (220 ms, instant with reduced motion); bundled artwork remains immediate. Unit tests and both app/instrumentation APK builds pass, and the latest app build is installed on the Honor. Physical-TV visual check remains pending unlock.
- [x] Controller status detection now accepts keyboard/joystick combo HID devices when they expose standard stick/hat axes, while still rejecting the Honor dock's generic mouse/joystick interface. Regression tests cover both cases; unit and instrumentation compilation pass, and the APK is installed. Verify with the user's Bluetooth pad when it is enumerated.
- [x] Increased game availability badges from 8sp to 10sp with larger icon/padding for couch-distance reading on a 1080p TV; unit tests and app/instrumentation builds pass, and the APK is installed on the Honor. Visual acceptance remains pending unlock.
- [x] Increased the persistent controller footer from 11sp/17dp glyphs to 12sp/20dp (button letters 10sp), and slightly enlarged profile/status typography. Unit tests, instrumentation compilation, and the existing footer-bounds test source are in place; builds pass and the APK is installed. Verify full-width fit on the physical TV after unlock.
- [x] Extended the controller-footer Compose test with a 1920dp TV-width check for legend/profile separation and right-edge containment. Instrumentation APK compilation, app assembly, and unit tests pass; the on-device Compose run is still pending unlock.
- [x] Fixed controller B/Back priority while Store or Library's controller keyboard is open: the active text editor now dismisses first instead of the shell navigating away. Added registry regression coverage; full unit tests and app/instrumentation builds pass, and the app is installed on the Honor. Verify with the actual pad when unlocked/connected.
- [x] Strengthened the B-key regression to route through `onGameBoxRootInput`; it asserts the active keyboard closes before global navigation and a second B reaches the global action. Instrumentation APK compiles; execution on the Honor is still pending unlock.
- [x] Added a D-pad-right navigation test for the top tabs, asserting focus moves Home→Library while Home remains selected until A/activation. Instrumentation APK compiles and unit tests pass; on-device execution remains pending unlock.
- [x] Strengthened the D-pad test to exercise the production `TopNav` layout at 1920dp rather than a simplified row of tab buttons. Instrumentation APK compiles; physical-pad/TV execution remains pending an unlocked Honor and connected controller.
- [x] Library game Details now requests console screenshots from the shared Libretro thumbnail client and merges them with cached discovery screenshots, deduplicated; missing/unsupported results remain non-blocking. Added merge regression coverage; full unit suite and app/instrumentation builds pass, and APK is installed on Honor with app data preserved.
- [ ] Open an imported game with a matching screenshot on the unlocked Honor/TV and visually verify the gallery; current projection remains locked, and a real network/source match is not accepted solely from local tests.
- [x] Removed the obsolete TheGamesDB key-entry panel from Settings and its dead footer component/test; compact Store no longer offers TheGamesDB sync or API-key instructions and now points to configured sources. Replaced settings copy with accurate Wikidata/Libretro coverage guidance. Full unit suite and app/instrumentation builds pass; updated APK installed on Honor.
- [ ] Visually recheck compact Store and Settings source guidance on-device; the Honor's 1920×1080 external display still foregrounds `LockScreenWindow`. Legacy TheGamesDB repository/sync classes remain for cached-data/backward compatibility and have not yet been removed from the backend.
- [x] Libretro box-art/screenshot filename variants are now probed in bounded batches of four concurrent HTTPS HEAD requests instead of serially. This sharply reduces worst-case artwork lookup latency while preserving region/name preference order; concurrency, preferred selection, transient retry, unit tests, and APK builds pass. Updated APK is installed on the Honor without clearing app data.
- [ ] Recheck the remaining cover failure on the 1920×1080 HDMI display. The phone and external display are on and ADB-connected, but `dumpsys window` currently shows Honor Account Center/`LockScreenWindow`; do not launch or bypass the lockscreen. The current report still needs visual confirmation and exact affected-title diagnostics.
- [x] Read-only Honor diagnostics found four files in GameBox's `cache/box-art` (132,887–866,526 bytes). The app writes these files only after a cover downloads and decodes, so remote artwork loading has succeeded for at least four URLs on this hardware; this does not prove which Library titles they belong to or that covers render correctly on screen. HDMI remains active, but `LockScreenWindow` prevents safe visual acceptance.
- [x] Bundled starter-cover selection now tolerates recognized ROM release suffixes (region and language tags in parentheses/brackets), so exact starter titles still resolve their curated cover after import naming adds tags; unrelated title variants remain unmatched. Unit tests and app/instrumentation builds pass, and the APK is installed over the existing Honor app with data preserved. TV visual verification remains pending unlock.
- [x] Regular Library/collection cards now use `ContentScale.Fit` so portrait box art is shown in full on wide TV tiles; the wide Home hero retains `Crop` for its immersive background. Added unit coverage for this presentation policy; unit suite and app/instrumentation APK builds pass, updated APK installed on Honor with data preserved. Inspect on the unlocked TV before considering this visually accepted.
- [x] Wide Library/landscape game cards now pair a legible contained cover rail with the cropped ambient art backdrop, while retaining the focus glow, metadata, favorite state, and availability badge. This prevents portrait covers from disappearing into a panoramic crop on TV-sized tiles. Unit coverage for sizing/scaling, full unit suite, and APK builds pass; APK installed on Honor with user data preserved. TV visual acceptance remains pending unlock.
- [x] Added a Compose bounds regression ensuring landscape-card titles and availability badges stay to the right of the cover rail; instrumentation source compiles with unit tests and app assembly. It has not been executed on-device because the Honor remains locked, and API-emulator execution does not replace the requested TV acceptance pass.
- [x] External-display hosts now apply Compose safe-drawing insets on HDMI as well as phone displays. This preserves immersive full-screen drawing while honoring Android's reported persistent system-overlay bounds (no hard-coded TV overscan margin); targeted policy test, full unit suite, instrumentation/app APK builds pass. Updated APK installed on the Honor with data preserved; verify footer placement visually on HDMI after unlock.
- [x] Fixed artwork precedence for bundled/preloaded titles: real remote/library artwork now loads above the bundled cover, which remains immediately visible as an offline/loading fallback. Full unit suite and debug APK build pass; updated APK installed on the Honor with app data preserved. [ ] Visually confirm on the TV after unlock; current focus is `LockScreenWindow`, so no fresh hardware visual acceptance is claimed.
- [x] Normalized artwork URLs before validation and loading, fixing the case where padded provider URLs passed validation but failed at `URL.openConnection`. Added a focused normalization regression; full unit suite and debug/instrumentation APK builds pass, and the latest build is installed on the Honor with data preserved. TV recheck still awaits unlock.
- [x] Reworked portrait Library/collection tiles so the full-fit cover has a distinct artwork area and title/platform/status sit in a solid footer instead of obscuring the boxart. Full unit tests and debug/instrumentation APK builds pass; installed on Honor with app data preserved. [ ] Confirm cover/footer balance on 1920×1080 TV after unlock.
- [x] Added a Compose bounds regression for portrait cards: artwork ends before the info footer, and title/platform/status remain inside it. Instrumentation APK compiles; execution on the actual Honor/TV remains pending because the current foreground is `LockScreenWindow`.
- [x] Library/collection poster fallback now resolves against the stable provider title when the user edits the displayed title, preserving bundled covers without changing the visible title. Added a regression test; full unit suite and debug/instrumentation APK builds pass, and the update is installed on Honor with data preserved. TV visual acceptance still awaits unlock.
- [x] Increased poster title/platform typography to 13sp/10sp for couch readability and expanded the Compose bounds regression to ensure its footer stays within the tile. Unit tests and debug/instrumentation APK builds pass; installed on Honor with app data preserved. Visual acceptance still awaits unlock.
- [x] Split poster, standard landscape, and hero card rendering into focused composables, removing poster-only branches from the landscape card path. Full unit suite and debug/instrumentation APK builds pass; updated APK installed on Honor with data preserved. TV visual acceptance remains pending unlock.
- [x] Fixed the imported-library box-art lookup to use each game's canonical provider title rather than a user-edited display title; screenshot lookup follows the same stable title. Added regression coverage for a renamed PSP game. Full unit suite passes (728 tests, 0 failures/errors, 1 skipped), and debug app plus instrumentation APKs build successfully.
- [ ] Recheck remaining artwork failures visually on the Honor/TV. A fresh hardware query on 2026-10-03 found the phone keyguarded and only its built-in panel/virtual screen-sharing display (off); no HDMI display was enumerated. Do not claim TV acceptance until the user unlocks and reconnects the dock.
- [x] Removed eight unverified decorative illustrations from the Android resource tree after confirming they were still being packaged. They are preserved under `docs/archive/generated-starter-artwork/`; missing source artwork now gets a branded title monogram instead of misleading pseudo-boxart. Full unit tests and app/instrumentation APK assembly pass. The latest debug APK was installed on the connected Honor with `adb install -r` and app data preserved (package last updated 2026-10-03 22:24); visual verification still awaits unlock plus HDMI reconnection.
- [x] Localized the tracked-download browser's return action and download error messages, eliminating its `SetTextI18n` lint warning. Full verification now passes with 728 unit tests and `lintDebug` (15 warnings, 0 errors); the updated APK still needs physical TV flow review.
- [x] Replaced the tracked-download browser's platform-default `LinearLayout` toolbar with a GameBox-themed Compose toolbar, clear source hierarchy, focus highlight, and controller-aware/sound-enabled Back action. It includes the B/Back legend and accessibility semantics; the activity maps controller A to D-pad center. Added a toolbar focus/activation instrumentation test; app and instrumentation APKs compile. This test has not been executed yet.
- [x] Centralized controller-A-to-D-pad-center mapping across the main TV host and the tracked browser, with unit tests proving confirm mapping and preserving B/navigation keys. Latest complete local gate passes: 730 tests (0 failures, 1 skipped), app/instrumentation APK builds, and lint (15 warnings, 0 errors). Updated APK installed on Honor at 2026-10-03 22:42 with app data preserved. The current input/display check still reports no gamepad or HDMI and a locked handset, so hardware acceptance remains open.

## 2026-10-03 tracked-download follow-up

- [x] Added in-app handling for user-initiated `target=_blank`/popup navigation in the tracked source browser. Popup pages remain inside GameBox and share the HTTPS-only DownloadManager handoff, filename/platform validation, and automatic verified-import path instead of silently relying on the primary WebView.
- [x] Unit tests, debug APK, and instrumentation APK build successfully; `lintDebug` and `git diff --check` pass. Updated app installed on the unlocked Honor with app data preserved and GameBox Home relaunched on physical HDMI display 282 (1920×1080).
- [ ] Complete an end-to-end live download/import with a legally distributable test package on the physical TV. No copyrighted game binary was downloaded during this change; the device installation and display launch do not by themselves verify a website-triggered transfer.

## 2026-10-03 physical HDMI recheck

- [x] Reconnected Honor Magic6 Pro was unlocked, GameBox was foreground on the physical `HDMI Screen` (1920×1080, logical display 282), and the wireless controller was detected. Captured the real HDMI surface at `docs/evidence/hardware-run-2026-10-03/home-after-download-fix.png` (SurfaceFlinger display ID, not phone panel or Android Studio virtual display).
- [ ] Full-screen acceptance is still open: GameBox's own window requests 1920×1080 and the Android fullscreen request reports `window is already fully expanded`, but HONOR's separate `com.hihonor.desktop.systemui` `Dock` window remains visible at the bottom 43 px. `WindowInsetsController.hide(systemBars)` and a legacy immersive-sticky compatibility trial did not remove it; the trial was removed from source. Capture of the trial is retained at `docs/evidence/hardware-run-2026-10-03/immersive-compat-trial.png`.
- [ ] User approved temporarily disabling and restoring HONOR Global Taskbar. Tested the TV-visible HONOR Settings UI: searches for “Global taskbar”, “dock”, and “taskbar” did not reveal the system control (the “dock” result was Assistive Ball). No device setting was changed; the undocumented `game_dockbar_disable_mode` key was not written. Closed Settings and relaunched GameBox on HDMI; `docs/evidence/hardware-run-2026-10-03/gamebox-tv-after-restore.png` confirms GameBox is foreground again and the 43 px HONOR Dock remains. Next: locate the toggle through HONOR's documented Desktop Mode/Global Taskbar navigation or ask the user to expose its exact settings page; retest and restore the original state before claiming clean full-screen acceptance.

## 2026-10-03 Vimm results and artwork

- [x] Split configured-source results into distinct horizontally browsable rows, grouped by console, with per-row game counts. Confirmed on the physical 1920×1080 HDMI TV: an all-console “God of Wra” Vimm query displayed separate PS2, PS3, and further console sections (`docs/evidence/hardware-run-2026-10-03/gamebox-vimm-all-console-results.png`).
- [x] Search now tolerates close title spellings, including adjacent transposed letters, while preserving existing exact/prefix relevance ordering. On the Honor TV, “God of Wra” returned God of War titles; no game files were downloaded. Unit regression covers parser results and unrelated-title exclusion.
- [x] Result cards resolve missing covers through Libretro's exact `Named_Boxarts` archive lookup, without probing screenshots, and cache successful lookups. Confirmed genuine God of War cover art on the HDMI result rows. Titles with no reliable cover retain the branded fallback and stop showing an indefinite loading label.
- [x] Final debug APK installed with existing app data preserved. `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:lintDebug` pass. Physical-TV search evidence is in `docs/evidence/hardware-run-2026-10-03/gamebox-vimm-all-console-results.png`; the post-install foreground/home capture is `docs/evidence/hardware-run-2026-10-03/gamebox-vimm-final.png`.

## 2026-10-03 external fullscreen focus race

- [x] HONOR logs showed API 36 fullscreen requests rejected with “The window is not the top focused window” during desktop task transitions, leaving MainActivity in a 1152×691 freeform window. Removed the premature onResume request and added two bounded retries (500 ms, then 1.2 s) only for that focus-race error; other failures do not retry. Added unit coverage for the error filter and retry bound.
- [x] Installed and launched on the physical 1920×1080 HDMI display. HONOR accepted the retried request (`GameBoxDisplay: Fullscreen request accepted on display 282`), and the Activity config reports `mBounds=Rect(0,0 - 1920,1080)` with `windowingMode=fullscreen`. Capture: `docs/evidence/hardware-run-2026-10-03/gamebox-fullscreen-retry-test.png`.
- [x] Re-ran the app-owned DocumentsUI import-picker → Back → GameBox path on the retry build from the physical TV. Opened Cave Story's “Import your copy,” observed HONOR Downloads/SAF picker, pressed Back without selecting a document, and GameBox returned to its 1920×1080 surface with “No file selected”; the fullscreen API retry was accepted on display 282. Evidence: `docs/evidence/hardware-run-2026-10-03/gamebox-saf-picker-tv.png` and `docs/evidence/hardware-run-2026-10-03/gamebox-picker-return-tv.png`. No user file was selected or modified. HONOR's bottom Dock remains system-owned and visible, so “no system chrome” acceptance is still open.
- [x] `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:lintDebug` pass; instrumentation sources (including the clickable TV typo-correction test) compiled in the preceding build. The controller was detected on Home before the picker flow, but the post-picker status showed “Not connected”; physical button navigation was not exercised in this pass.

## 2026-10-04 live Honor HDMI follow-up

- [x] Re-ran the focused Vimm-search, configured-result, and Libretro-thumbnail unit tests and assembled the debug APK with Gradle 9.7.1 / Android Studio JBR. All requested tasks passed.
- [x] Reinstalled that APK on the unlocked Honor Magic6 Pro using `adb install -r` (existing app data preserved), launched GameBox on logical display 282, and captured Home and Store directly from the physical HDMI SurfaceFlinger display ID `4639578574838715141` at 1920×1080. Evidence: `docs/evidence/hardware-run-2026-10-04/honor-hdmi-installed-app.png` and `.../honor-hdmi-store.png`.
- [ ] TV clean-screen acceptance remains open: the GameBox content occupies the full 1920×1080 app bounds, but the separate HONOR desktop Dock remains visible along the bottom. A fresh Global Taskbar Settings attempt did not reach the toggle; no system setting was changed, so there was nothing to restore. The previously user-approved test/restore still needs a reliably identified system control.
- [x] Live Vimm search was exercised from the Store on the physical TV using GameBox's visible on-screen keyboard: “GOD OF WRA” produced the clickable “Did you mean ‘GOD OF WAR’?” correction; selecting it updated the query, and rerunning Vimm returned 9 results split into PS2 (2), PS3 (4), and PSP (3) rows. PS2 and PSP box art loaded; PS3 titles used the designed no-art fallback. Evidence: `honor-vimm-live-results.png`, `honor-vimm-live-results-lower-rows.png`, and `honor-vimm-corrected-results.png` under `docs/evidence/hardware-run-2026-10-04/`. No game binary was downloaded.
- [x] Opened one result and used Back to return to the grouped result rows (`honor-vimm-ghost-sparta-detail.png`, `honor-vimm-back-to-results.png`). Search results remain available even when the Vimm details request fails.
- [x] Opened the Chains of Olympus source page using “Open Vimm's Lair in GameBox” on HDMI; Vimm displayed the matching PSP vault page inside GameBox's full-screen tracked browser, and “Back to GameBox” returned to the same result detail. The download control was not activated. Evidence: `honor-vimm-source-browser.png` and `honor-vimm-back-to-gamebox.png`.
- [ ] Vimm detail hydration remains unresolved: on the Honor, actual GET requests to `https://vimm.net/vault/23719` returned HTTP 404 with both GameBox's and a Chrome-like user agent, while `https://vimm.net/vault/PSP/G` returned 200; `www.vimm.net` redirects to the apex host, which then returns 404. A same-origin cookie/referer probe did not change the result. The in-app WebView displayed a detailed Chains of Olympus page, so this may be a stale/cache or server-route discrepancy; no unsupported redirect/header workaround was added. Keep the exact result usable through the in-app source link and legal-copy import. Two same-title Ghost of Sparta PSP entries remain indistinguishable because detail hydration failed.
- [ ] Physical-controller acceptance remains open: GameBox reported “Not connected” in both Home and Store. On-screen search-key taps worked, but these were touch/ADB interactions, not a physical gamepad test; the prior ADB text-injection attempt did not produce a valid query and is not counted as keyboard acceptance.
- [x] GameBox returned to its normal Store state after testing; no game binary was downloaded, no user files were selected, and no phone data was cleared.
- [x] On 2026-10-04, dismissed the transient HONOR launcher context popup and recaptured the physical HDMI output at 1920×1080. The resulting GameBox Home capture is full-screen and shows no bottom dock; no Global Taskbar setting was changed. Evidence: `honor-hdmi-popup-dismissed.png`.
- [x] Android now enumerates the Bluetooth `Wireless Controller` as `KEYBOARD | GAMEPAD | JOYSTICK` on the Honor, and GameBox reflects that connected status. One ADB-injected DPAD_RIGHT moved focus from the default Home area to the Xbox quick-launch tile; this was only a software input check. Physical controller acceptance was later verified separately below using actual D-pad/A/B input.
- [x] Added optional region/year edition labels to configured-source result cards when the source provides that metadata, including accessibility descriptions. Focused `ConfiguredSourceResultTest`, `assembleDebug`, and `lintDebug` passed, and the updated APK was installed on the Honor with app data preserved. See `ConfiguredSourceResult.kt`, `DiscoveryGameComponents.kt`, and `ConfiguredSourceResultTest.kt`.
- [x] On the physical HDMI Home screen, removed the duplicated install-state text from installed/update poster subtitles; the availability badge already communicates that state, so cards now show the full console label instead of a clipped `console · installed`. Non-installed/actionable states retain their status. `GameCardArtworkScaleTest` and `assembleDebug`/`lintDebug` pass; updated APK installed over the existing app without clearing data. TV evidence: `honor-hdmi-poster-label-polish.png`.
- [x] Revalidated actual Bluetooth controller input on HDMI: `getevent` observed repeated `ABS_HAT0X/Y` transitions from the enumerated Wireless Controller while GameBox was foreground, and the resulting capture shows visible focus on a collection card. Evidence: `honor-hdmi-physical-controller-check.png`. The subsequent physical D-pad/A/B test below verified game-detail entry and back navigation.
- [x] After the Dock reappeared, used HONOR's visible GameBox context-menu action “Open in full screen mode” (no system settings changed). Closed the transient Settings floating window and verified the bottom Dock is gone in a fresh 1920×1080 HDMI capture; `dumpsys window` reports GameBox bounds `Rect(0, 0 - 1920, 1080)` and `windowingMode=fullscreen`. Evidence: `honor-hdmi-fullscreen-clean.png`.
- [x] On the physical Honor/TV, D-pad navigation moved focus to “Mario & Luigi: Partners in Time”; physical A opened its details page, and physical B returned Home with focus restored to that game card. `getevent` captured controller-axis and A/B button events; no key events were injected. Evidence: `honor-hdmi-physical-focus-after-navigation.png`, `honor-hdmi-controller-open-details.png`, and `honor-hdmi-controller-back-home.png`.
- [x] Cold-process test: `am force-stop com.gamebox.os` followed by launching MainActivity did preserve app data and 1920×1080 GameBox content, but the HONOR Dock returned. Reapplying the visible “Open in full screen mode” context action hid it again. Current clean result: `honor-hdmi-fullscreen-after-cold-launch-clean.png`; failure state: `honor-hdmi-after-cold-launch.png`.
- [ ] HONOR per-app fullscreen does not survive GameBox process termination/cold launch in this observed setup; determine whether a supported app/window API can re-request the mode automatically, or document the manual per-app action if the OS exposes no supported programmatic control. Do not alter undocumented system settings. Device-restart persistence also remains untested.
- [x] After cold launch and manual reapplication of HONOR full-screen mode, closed its transient Settings window. GameBox is left on the clean 1920×1080 TV Home with the Dock hidden; screenshot `honor-hdmi-fullscreen-after-cold-launch-clean.png`. No files, installed games, app data, or HONOR global settings were cleared or changed.

## 2026-10-04 physical Honor HDMI controller-search and Library follow-up

- [x] Exercised the actual Bluetooth controller on the TV output: D-pad moved focus to a game, A opened details, B returned Home, and X opened the game-search keyboard. A selected the focused `Q` key and filtered the Library; B dismissed the keyboard while preserving the query. The query was then cleared and all four titles returned. These are physical-controller observations, not injected key events.
- [x] Updated Library poster subtitles to show only the console/platform name, avoiding duplicated and clipped availability text; availability remains clearly shown by its badge. Added regression coverage for installed, update-available, importable, and missing-file states. Targeted unit test, debug assembly, and lint passed; the APK was installed over the existing Honor app data.
- [x] Rechecked the updated Library on the actual 1920×1080 HDMI output. All four games, their box art, console-only subtitles, availability badges, sidebar, and controller footer render; the HONOR Dock/taskbar is not visible in the current manually selected full-screen mode. Visual evidence: `docs/evidence/hardware-run-2026-10-04/honor-hdmi-library-subtitle-polish.png`.
- [ ] Full-screen mode is not persistent across force-stop/relaunch or app update on this HONOR desktop: the Dock returned, and selecting HONOR's per-app “Open in full screen mode” restored the borderless presentation. Do not claim cold-start persistence; re-test this behavior after a safe app/device lifecycle change and prefer an app-side supported fix if available.
## 2026-10-04 TV Library density and post-update hardware check

- [x] Reworked the wide Library shelf sizing for 1080p viewing: up to four poster tiles now use the column width (capped at 190dp), with a compact square card footprint so both Recently Played and All Games fit above the persistent controller footer. The compact/phone layout and landscape resume cards keep their existing sizing.
- [x] Added policy regression assertions for wide poster width bounds and height. `GameCardArtworkScaleTest`, `assembleDebug`, and `lintDebug` all pass.
- [x] Installed the APK on the Honor and visually accepted the resulting Library on the actual 1920×1080 HDMI display: all four covers in both shelves, titles, platform subtitles, sidebar, and controller footer fit without vertical clipping. Evidence: `docs/evidence/hardware-run-2026-10-04/honor-library-fullscreen-tv.png`.
- [x] Reapplied HONOR's per-app “Open in full screen mode” after updating the APK; final TV capture has no bottom Dock/taskbar or transient desktop window. This confirms manual recovery, not automatic fullscreen persistence after app update or cold start.

## 2026-10-04 Vimm details primary action

- [x] For Vimm-backed discovery details, promoted the tracked in-app Vimm browser into the hero’s primary action slot with the concise label “Download.” The action still tracks the supported browser download and returns it to the existing verification/import workflow; the duplicate lower-page browser action was removed.
- [x] Kept “Import your copy” available in the authorized-copy panel for Vimm-backed titles. Non-Vimm details retain the existing hero import action. Added instrumentation coverage for A activating Download instead of import and for manual import remaining reachable.
- [x] Full unit tests, Android-test Kotlin compilation, debug APK assembly, and lint pass. The build is installed on the Honor and the TV launcher was returned to the user-approved fullscreen mode.
- [ ] Vimm-specific details visual acceptance remains pending: on this run the configured Vimm search returned no Vimm result on the Honor, so the new Download CTA could not be reached by real catalog navigation. The neighboring catalog details route and its fallback UI were visible on TV.
- [ ] Physical Honor instrumentation execution is currently unreliable: the focused Compose test process crashed before reporting assertions. The connected-test runner then removed the app package; after reinstall, a read-only check found a fresh database and no imported ROM payloads (only empty `.keep` console directories). The current Library therefore no longer shows the earlier locally imported titles. Stop device-side tests that can uninstall/clear GameBox until its game files/library state can be restored or the user directs recovery; no recovery/import was attempted.

## 2026-10-04 reclaimed TV viewport

- [x] Removed the obsolete 20dp external-display bottom reserve from the GameBox shell. The HONOR Dock is hidden by the user's selected per-app fullscreen mode, so this reclaimed TV viewport height for content while keeping the GameBox controller footer in layout.
- [x] `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` passed. Installed the APK in place (no uninstall or app-data clearing) and manually restored HONOR per-app fullscreen mode after the OS brought the Dock back during update.
- [x] Verified the final Home screen on the actual 1920×1080 wired TV output with no Dock/taskbar or transient window. Evidence: `docs/evidence/hardware-run-2026-10-04/honor-bottom-space-final.png`.
- [ ] Vimm-specific Download action still needs visual acceptance from an actual Vimm result; the live Honor search returned no Vimm entry. The instrumented device runner remains off-limits until library/data recovery is addressed.

## 2026-10-04 physical TV emulator launch regression

- [x] Read-only inspection of the currently connected Honor and physical HDMI output found a RetroArch ANR over the GameBox details screen. Android reported input dispatch timeout at 02:15:26 while launching the imported Yu-Gi-Oh! Forbidden Memories `.cue` content URI from GameBox.
- [x] Confirmed the RetroArch task was placed on the 1920×1080 physical TV display but remained in a 1152×691 HONOR freeform window; the bottom system bar was visible. GameBox’s own MainActivity remained fullscreen underneath. Captured the actual TV state at `docs/evidence/hardware-run-2026-10-04/honor-current-tv-after-search.png`.
- [ ] Do not claim the controller B/search-dismiss test passed from this state: the keyboard was not open, and RetroArch’s Android ANR dialog obscured GameBox. Leave the dialog untouched until the user chooses Wait or Close app, since closing RetroArch may discard unsaved game progress.
- [ ] Diagnose the RetroArch hang and HONOR freeform/fullscreen handoff after the user resolves the ANR; preserve the current game/import data and avoid instrumentation runs that can uninstall GameBox.

## 2026-10-04 durable Vimm browser downloads and TV Download Manager

- [x] Replaced details-screen-scoped DownloadManager polling/import with persisted browser-transfer records and an application-lifetime coordinator. Leaving the Vimm page no longer cancels GameBox's progress tracking or automatic verified import; stored transfer IDs are reattached to Android DownloadManager after GameBox starts again.
- [x] Browser transfers now carry the selected Vimm game's identity/platform/artwork metadata, so completed files use the same verified import and source-removal workflow as manual imports.
- [x] Added browser-transfer progress/status rows and completed counts to GameBox's Download Manager, including queued, downloading, verifying/importing, imported, and attention-needed states.
- [x] Unit tests, Android-test Kotlin compilation, debug APK assembly, and lint pass. Installed over the existing app with `adb install -r`; no uninstall/data reset. Existing Yu-Gi-Oh! Library entry and app data remained present.
- [x] Recovered HONOR's approved per-app fullscreen presentation and visually checked GameBox's Download Manager on the physical 1920×1080 TV with no bottom Dock; final evidence: `docs/evidence/hardware-run-2026-10-04/honor-download-manager-final-installed.png`.
- [ ] No real Vimm transfer was started in this verification pass, so background completion/import and progress-row behavior still need an end-to-end run when Vimm returns a reachable result. Existing live search/network limitation remains.
- [ ] RetroArch's physical launch on this Honor previously produced an input-dispatch ANR and a floating window; closing its crashed process did not clear GameBox data. Emulator compatibility/fullscreen remains a separate unresolved hardware issue.
- [ ] The Bluetooth controller showed disconnected on the final TV capture; controller interaction was not tested in this pass.

## 2026-10-04 Home-to-Vimm entry point

- [x] Added a TV Home “Open Vimm Lair in GameBox” action alongside the game-discovery/recently-played section, using the previously unused right-side space. The empty Home welcome state also has a direct Store action, while authorized-copy import remains available elsewhere.
- [x] Kept the Vimm discovery details hero action labeled “Download”; its tracked in-app browser and Download Manager workflow are unchanged. Manual authorized-copy import remains available as a secondary option.
- [x] Added accessible naming and a Compose instrumentation assertion for the wide-TV Home action’s visibility and activation. `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` pass.
- [x] The Home action now enters the Store through the same focus path as controller Search, automatically opening the on-screen controller keyboard. Installed the latest build with `adb install -r` (no uninstall/data reset), tapped the shortcut on physical HDMI display 282, and verified the keyboard appeared over Store with Vimm’s configured search visible behind it. Capture: `artifacts/home-vimm-store-keyboard-tv.png`.
- [x] Left Store’s controller keyboard open after visual inspection, ready for a D-pad search; no query or download was started. App data and the installed Yu-Gi-Oh! entry remain intact.
- [x] After this update reset HONOR’s per-app fullscreen choice, the visible GameBox taskbar context menu was reopened and “Open in full screen mode” reapplied; a fresh physical HDMI capture confirmed the bottom Dock hidden. The controller reports disconnected, so physical-controller navigation was not rechecked in this pass.

## 2026-10-04 Download Manager visibility follow-up

- [x] Kept imported browser transfers in the main Download Manager list under “Completed browser downloads”; they now show “Added to Library” instead of disappearing from the active queue without a visible completion record.
- [x] Added `BrowserDownloadSectionsTest` to protect the active/completed grouping for queued, downloading, failed, and imported transfers.
- [x] `testDebugUnitTest`, Android-test Kotlin compilation, debug APK assembly, and lint pass.
- [x] Rechecked Download Manager on the physical 1920×1080 HDMI display after the in-place APK update; it rendered correctly and was empty (no transfer was running to validate background continuation/import live).
- [x] Reapplied HONOR’s visible per-app “Open in full screen mode” action and visually confirmed the bottom Dock and transient window-controls overlay are hidden on the HDMI output; evidence: `artifacts/download-manager-check-tv.png`. The Bluetooth controller currently reports disconnected.
- [ ] Still required: start a lawful/authorized browser transfer, leave the source/details page, and verify its persistent progress/completion/import row on the actual TV. The latest RetroArch crash buffer separately shows a native abort in RetroArch’s `hwuiTask0` graphics thread; emulator crash/fullscreen stability remains unresolved.

## 2026-10-04 TV Home safe-area fit and emulator crash audit

- [x] Reduced only the wide-TV Home vertical rhythm and card heights slightly; compact/phone dimensions are unchanged. On the physical HDMI Home screen, the full “Recently played” game tile now fits above the fixed controller footer without scrolling. Evidence: `artifacts/goal-home-current-tv.png`.
- [x] Installed over the existing Honor app without clearing data, ran `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` successfully, and restored HONOR’s per-app fullscreen mode afterward. The HDMI capture confirms the bottom Dock and transient window chrome are hidden.
- [x] Re-read the Honor crash buffer without relaunching RetroArch. Installed package is `com.retroarch` version `1.22.2_GIT`; repeated crashes include `FORTIFY: pthread_mutex_lock called on a destroyed mutex` in RetroArch `hwuiTask0`/`hwuiTask1`, with SIGABRT. Installed alternatives observed: PPSSPP, Dolphin, and Azahar; DuckStation is not installed. No game was relaunched and user save state was left untouched.
- [ ] Emulator stability remains unresolved; determine whether this repeated renderer-thread failure is specific to the installed RetroArch build/HONOR external-display mode before changing GameBox launch behavior. The Bluetooth controller remains disconnected during this pass.

## 2026-10-04 Library wording polish

- [x] Corrected visible singular count labels across Home, Library sections, Library Overview, and accessibility summaries (`1 game`, `1 favorite`, `1 hour`), with regression tests for singular/plural quantities.
- [x] `testDebugUnitTest`, Android-test Kotlin compilation, APK assembly, and lint pass; installed over the existing Honor app without clearing data.
- [x] On the physical HDMI display, both Recently Played and All Games now show “1 game”; artwork remains loaded, and the Dock was hidden again after installation. Capture: `artifacts/goal-library-singular-tv.png`.

## 2026-10-04 wide-TV save-tools layout

- [x] Reworked the game-details save tools into side-by-side “Save copy” and “Encrypted cloud copy” panels for wide TV; compact/phone mode retains the stacked layout. The local panel explicitly says GameBox does not alter the emulator’s live save.
- [x] Added a wide-panel layout assertion to `PerGameSaveCardTest`; unit tests, Android-test Kotlin compilation, APK assembly, and lint pass.
- [x] Installed over the existing Honor app without clearing data, opened the imported PS1 game's details on HDMI, and verified both panels on the physical 1920×1080 display. Did not activate Play or any save action. Restored the previously approved HONOR fullscreen setting; Dock/window chrome hidden in `artifacts/goal-save-panels-tv.png`.
- [ ] No physical controller was connected in this pass; save-action focus/navigation still needs controller verification. RetroArch was not launched because the device’s retained native crash history remains unresolved.

## 2026-10-04 download tracking recovery and RetroArch crash diagnosis

- [x] Fixed the browser-download coordinator's job-registration race by registering lazy tracking jobs before starting them, so fast completion cannot leave a stale completed job blocking later tracking.
- [x] If Android prunes a DownloadManager row after transfer completion, GameBox now recovers from its own non-empty staged file and continues verification/import instead of immediately marking the transfer failed. Added unit coverage for present, empty, and missing staged files.
- [x] Read the connected Honor's retained native crash buffer and package metadata without launching RetroArch. `com.retroarch` 1.22.2_GIT aborts in its own `hwuiTask0`/`hwuiTask1` renderer threads with `FORTIFY: pthread_mutex_lock called on a destroyed mutex`; this is an emulator-native failure, not a GameBox Java exception. No app data, game files, or saves were changed.
- [ ] Verify app-backgrounded download continuation and automatic import on the physical TV with a legal test package. No real transfer was started in this pass, and the staged-file recovery applies only once a transfer has completed; a genuinely failed/interrupted Android download still needs a retry path.
- [ ] RetroArch renderer instability remains unresolved; use an updated/stable emulator build or another compatible emulator to validate before claiming game-launch stability. Do not relaunch the crashing emulator on the user's imported title without explicit testing direction.

## 2026-10-04 physical TV artwork visibility polish

- [x] Reapplied HONOR's approved app-specific fullscreen mode after APK update. A fresh screenshot from the physical 1920×1080 HDMI panel confirms the Dock is hidden and the Recently Played tile fits above GameBox's controller footer; Bluetooth controller is currently disconnected. Evidence: `artifacts/gamebox-tv-fullscreen-new.png`.
- [x] Removed redundant availability pills from both poster and landscape game cards. Both card styles already show platform and install state in their dedicated information line; the overlays obscured cover art and repeated the same status.
- [x] Extended `PosterGameCardLayoutTest` to assert availability overlays are absent from both card styles while their captions retain platform/status and poster title/footer bounds remain non-overlapping.
- [x] `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` pass (742 unit tests; Android instrumentation compiled but was not executed). Installed over the Honor app with `adb install -r`, preserving app data; restored the previously approved per-app fullscreen setting after HONOR reset it during update.
- [x] Verified the final Home on physical 1920×1080 HDMI: Dock hidden; the collection poster and landscape Recently Played tile show unobstructed cover art, with status remaining in each card caption; the Recently Played card fits above GameBox's controller footer. Evidence: `artifacts/gamebox-tv-home-verified-fullscreen.png`.
- [ ] Physical controller is disconnected in this final capture; game-card focus/activation must be retested when connected. Do not launch RetroArch on the imported title while the emulator-native crash remains unresolved.

## 2026-10-04 sparse imported metadata polish

- [x] On the actual TV Game Details hero, an imported title with no release year displayed a literal “Unknown” beside its player count and file size. Omitted that unavailable metric while retaining known metadata.
- [x] Added unit coverage for a null release label and instrumentation coverage asserting sparse metadata does not render “Unknown” while player count and size remain visible.
- [x] `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` pass. Installed with `adb install -r` without clearing app data; HONOR fullscreen remained active.
- [x] Reopened Yu-Gi-Oh! Forbidden Memories details on the physical TV without activating Play. The unknown year placeholder is gone; known player-count and size metrics remain aligned. Evidence: `artifacts/gamebox-tv-details-final.png`.
- [ ] Physical controller is disconnected; confirm the Play action and focus traversal with the controller when available. Emulator stability remains separately unresolved.

## 2026-10-04 empty About panel and cached-description polish

- [x] The current imported PS1 details view has no description, region, or rating, so its About card contained only “No description is available…” and pushed the actual screenshot gallery down. Library details now omit that panel when no meaningful About content exists.
- [x] Library details now prefer an available cached provider description when the imported game record has none; region/rating-only About panels remain visible. Added unit tests for empty About suppression state and cached-description precedence.
- [x] `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` pass. Installed in place with app data preserved; restored HONOR's per-app fullscreen setting afterward.
- [x] On physical 1920×1080 HDMI, the empty About card is gone; the single screenshot gallery now starts directly below the hero and the wide local/cloud save panels move higher in the initial view. No game was launched and no save action was activated. Evidence: `artifacts/gamebox-tv-details-no-about.png`.
- [ ] The controller is disconnected and the details page remains vertically scrollable; continue checking remote navigation/focus when a controller is connected. RetroArch's renderer crash remains unresolved.

## 2026-10-04 wide-TV Store legibility pass

- [x] Increased the wide-TV Store's supporting copy, filter chips, configured-source labels/buttons, and featured-card description/action text for couch-distance readability. Compact/phone Store layout is unchanged.
- [x] Ran the full Android verification command from scratch: 743 unit tests (0 failures/errors, 1 skipped), Android-test Kotlin compilation, debug APK assembly, and lint all succeeded. Existing nullable-file warnings are confined to unrelated tests.
- [x] Installed in place on the connected Honor (no data reset), restored the previously approved per-app fullscreen mode, and visually inspected the real 1920×1080 HDMI output. Featured game, Vimm source action, source-library row, and fixed controller footer all remain visible; Dock/taskbar is hidden. Evidence: `artifacts/gamebox-store-font-review-final2.png`.
- [ ] Physical controller remains disconnected, so Store focus traversal remains unverified on hardware. No Vimm download was initiated; durable download continuation/import is still not verified end-to-end. RetroArch native renderer failure/ANR remains unresolved; it was not relaunched.

## 2026-10-04 full Vimm-preset console filters on TV

- [x] Expanded the Store rail from seven consoles plus Homebrew to all eighteen systems in the configured Vimm preset plus Homebrew, ordered to match the preset. WiiWare is kept distinct from Wii and does not fall through to the Homebrew discovery filter.
- [x] Added regression coverage for the complete preset filter set and normalized platform aliases. Console-specific empty featured art now uses the selected system label (including three-character acronyms such as NES); All-consoles fallback uses the intended GB monogram.
- [x] `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` succeeded; 744 unit tests, 0 failures/errors, 1 skipped. Installed over the existing Honor app without clearing data.
- [x] Verified on the physical 1920×1080 HDMI display: selecting NES updates the filter/title and keeps Search Vimm's Lair available; scrolling the rail reaches Xbox, Xbox 360, DS, 3DS, Switch, and Homebrew. Restored fullscreen (Dock hidden) and captured `artifacts/gamebox-store-console-rail-bottom-final.png`.
- [ ] Honor reports the controller as disconnected; physical controller navigation remains unverified. No actual Vimm transfer or RetroArch relaunch was attempted; download continuation/import and emulator-native renderer stability remain open.

## 2026-10-04 tracked-download manager shortcut

- [x] The discovery Details primary action used to become a disabled “Downloading…” control during an active Vimm transfer, with no direct way to inspect the queued job from that page. It now becomes “View Download” for queued/downloading/importing/failed transfers and “View Download Manager” after import; both open GameBox's Downloads destination instead of starting a duplicate source download.
- [x] Added a live badge to the header Downloads action (including compact layout) for active, paused, failed, and queued browser/remote transfers, making Download Manager status visible from the GameBox shell.
- [x] Routed compact-layout Downloads activation through the same controller-confirm handler as the TV layout. Updated the APK in place again, restored HONOR's per-app fullscreen mode, and confirmed Download Manager on the physical HDMI TV (1920×1080, bottom Dock hidden): `artifacts/gamebox-download-badge-tv-check.png`.
- [ ] Current Honor app storage contains no tracked browser-download record, so I could not confirm that the reported attempt passed through GameBox's persisted enqueue path. A real Vimm click-through is still needed to reproduce the site interaction and confirm DownloadManager enqueue on device.
- [ ] Honor exit history/logcat confirms repeated RetroArch native renderer failures (`pthread_mutex_lock` on a destroyed mutex in `hwuiTask*`) and ANRs; no current crash from this turn was retained, and GameBox cannot patch RetroArch's native renderer from its launch intent. Isolate by core/content and verify with an updated RetroArch build before treating it as resolved.
- [x] Propagated the navigation action from the Store route into discovery details and added a controller-A regression test for the active-download action. `testDebugUnitTest` (744 tests, 0 failures/errors, 1 skipped), Android-test Kotlin compilation, APK assembly, and lint succeeded.
- [x] Installed in place on the Honor without clearing user data and restored per-app fullscreen. Verified the physical TV Home remains borderless/fullscreen with the bottom Dock hidden: `artifacts/gamebox-fullscreen-quiet-check.png`.
- [x] Verified the updated header Downloads action opens Download Manager on the physical TV and that the installed app remains borderless/fullscreen with the Dock hidden. Capture: `artifacts/gamebox-download-badge-tv-check.png`.
- [ ] No tracked transfer was active, so the new Details-to-Manager action was not observed against a real browser download on the device, and Android-test cases were compiled but not run against the user app. This improves visibility, but does not yet prove the underlying interrupted-download resume/import behavior.
- [ ] Honor reports no connected controller. RetroArch stability and actual physical controller navigation remain open; RetroArch was not relaunched.

## 2026-10-04 featured Store artwork fallback on TV

- [x] Fixed the featured Store hero fallback to use the featured title rather than the selected console filter. With “All consoles” selected, Cave Story now renders its title-aware “CS” mark instead of the generic “GB” mark.
- [x] Added a Compose visual regression for the Cave Story fallback; Android-test Kotlin compilation, full unit suite (744 tests, 1 skipped), APK assembly, and lint all pass.
- [x] Installed in place on the Honor and verified the updated Store at 1920×1080 on physical HDMI, fullscreen with the HONOR Dock hidden. Capture: `artifacts/gamebox-store-title-fallback-tv.png`.
- [ ] Vimm’s live result/detail/download handoff and physical controller navigation remain unverified in this pass; the controller reports disconnected and no tracked browser transfer exists on the device.

## 2026-10-04 controller-search dialog on TV

- [x] Fixed a width-constraint ordering bug that let the controller-search dialog stretch to roughly 76% of the 1920px TV while the actual key grid stayed narrow and left-aligned. The panel now honors its 900dp cap, and the staggered key rows are centered within it.
- [x] Added a visual layout regression asserting the dialog respects its TV width cap. Unit tests pass (744 tests, 1 skipped), Android-test Kotlin compilation, APK assembly, and lint pass.
- [x] Installed in place on the Honor and verified the controller keyboard on the physical HDMI TV: the panel and keys are now centered and balanced, with fullscreen/Dock-hidden presentation preserved. Capture: `artifacts/gamebox-controller-search-tv.png`.
- [ ] Physical gamepad navigation is still unverified because Android currently reports no connected gamepad; keyboard open state was checked on hardware by touch, and instrumentation was compiled but not run on the user app.

## 2026-10-04 browser-download recovery and manager visibility

- [x] Failed GameBox-tracked browser transfers now have a retry action in Download Manager. It re-enqueues the same HTTPS URL and request metadata, stores the session cookie encrypted with Android Keystore, replaces the stale transfer row with the new tracked request, and recovers a completed staged file by retrying verification/import instead of downloading again.
- [x] Added a Compose regression test for the failed-transfer retry action. Full unit suite, Android-test Kotlin compilation, APK assembly, and lint pass; instrumentation tests were compiled only (not run against the user app).
- [x] Installed the verified build on the connected Honor without clearing data and verified Download Manager on the physical 1920×1080 HDMI output. The manager clearly shows zero active/queued/completed because no transfer was active on-device; HONOR per-app fullscreen was restored, with its Dock hidden. Evidence: `artifacts/gamebox-downloads-fullscreen-final.png`.
- [ ] Still need to reproduce with an authorized/legal download by leaving the source page during transfer, then verify the transfer continues and appears in Download Manager through automatic import. No binary was downloaded in this pass.
- [ ] RetroArch's previously captured SIGABRT remains an emulator-native destroyed-mutex failure in its `hwuiTask*` renderer threads. RetroArch was not relaunched; test a stable updated build/core separately before claiming game-launch stability.

## 2026-10-04 Download Manager empty-state TV polish

- [x] Rebalanced the wide-TV empty Download Manager state: replaced the short top-aligned strip with a large centered panel, clearer background-transfer and auto-import guidance, and a couch-readable Store action. Compact layout is unchanged; the Store action retains controller focus behavior.
- [x] Updated the empty-state controller-route instrumentation assertion. `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` pass; instrumentation compiled but was not run against the user's Honor app.
- [x] Installed in place on the Honor without clearing data and checked the final state on the real 1920×1080 HDMI display. The HONOR Dock is hidden, GameBox fills the display, the transient system TopBar auto-hides, and the empty-state card is balanced against the status rail. Capture: `artifacts/gamebox-downloads-empty-settled.png`.
- [ ] Controller is currently reported disconnected; physical focus movement and the retry action on a real failed transfer remain unverified. RetroArch's native renderer failure and a complete legal download-to-import hardware pass also remain open.

## 2026-10-04 game-details discovery action

- [x] Replaced the game-details "Import your copy" card with a prominent "Open Vimm Lair in GameBox" action. The card accurately directs owned-copy imports to the matching Store title's Details action; the Vimm-result details primary action continues to say "Download" when a tracked source transfer is supported.
- [x] Moved the discovery card directly below the game hero so it is reached before the screenshot gallery and is less likely to sit behind the controller footer on the 1080p layout. Added controller-A activation coverage for the card.
- [x] `testDebugUnitTest` passed (744 tests, 0 failures/errors, 1 skipped), `compileDebugAndroidTestKotlin`, `assembleDebug`, and `lintDebug` completed successfully. Android instrumentation was compiled only; it was not run against the user app.
- [ ] On-device HDMI verification is incomplete: the pre-reposition capture showed the card partly below the footer, prompting the move. At 06:34:52 local time Android logged the HDMI physical-display hot-unplug (`connected=false`) during the attempt to reopen TV mode; this confirms signal loss but not its cause. Do not treat the Android Studio virtual display as TV evidence; reseat HDMI and verify the revised card and fullscreen/Dock-hidden state.
- [ ] The latest Android input-device dump lists a USB optical mouse/keyboard and no Bluetooth gamepad. Reconnect the controller before physical button/focus acceptance; do not count the mouse's advertised joystick source as a gamepad.

## 2026-10-04 empty-library Home quick-start

- [x] Replaced the empty-Home copy that incorrectly directed owned-copy imports to Library. The onboarding now explains Store/Vimm discovery, the matching title's "Import your copy" action, and emulator/firmware prerequisites.
- [x] Added a couch-oriented wide quick-start layout with primary Vimm action and live controller, network, and storage readiness; compact mode stacks the guidance for handset width. Step titles/body copy use larger type for TV readability.
- [x] Expanded the guide to four steps: discover (discovery-only listings), import a copy you own, set the library folder/controller in Settings, and prepare an emulator/firmware. The wide layout presents steps as four readable cards; compact mode stacks them.
- [x] Clarified that listings are not install files, supported source links may start tracked downloads, and supported transfers are automatically verified/imported. The Compose regression checks this copy plus the Settings guidance and controller-A route.
- [x] The empty-library primary action now receives initial focus (including external-display mode, where the Android HOME prompt is intentionally hidden) and scrolls into view. The Compose regression requests focus and sends controller A to verify the Vimm route, while checking the owned-copy guidance and controller-ready status.
- [x] The 744-test JVM suite passes (0 failures/errors, 1 skipped); Android test sources compile, APK assembles, and lint passes after the focus, four-step guidance, and download/import-copy refinements.
- [x] Installed the current build in place on the physical Honor without clearing app data.
- [ ] Physical HDMI is still not enumerated, so the new layout has not been visually accepted at TV resolution. Do not use the Android Studio virtual display as a substitute; restore the wired output and verify the quick-start height, footer clearance, and controller focus on the actual TV.

## 2026-10-04 tracked-download failure visibility follow-up

- [x] Vimm browser download starts rejected before Android accepts a transfer now persist a `Needs attention` row when the download callback supplies valid game identity. Unsafe URLs and unsupported names are non-retryable; a safe request that fails to enqueue retains its URL for Retry. The details page receives the reason instead of only a transient toast.
- [x] The existing Download Manager renders failed browser attempts; the Retry action is only shown when a safe retry URL was retained. Successful transfers still use Android DownloadManager and the application-lifetime tracker/importer.
- [x] Verification: 744 JVM tests pass (0 failures/errors, 1 skipped), Android-test Kotlin sources compile, debug APK assembles, and `lintDebug` passes. Installed with `adb install -r` over the existing Honor app; app data was preserved. Instrumentation was not run.
- [x] Added an Android UI regression assertion that a rejected format remains visible with its exact reason and has no unsafe Retry button; Android-test source compilation passes. The test is not run on the Honor because device instrumentation has previously reset the user's app data.
- [ ] No physical HDMI display is currently enumerated (only the built-in display and Android Studio screen-share virtual display), so no TV UI or real Vimm-download handoff was exercised. This change cannot create a queue entry if the webpage never emits WebView's download callback; that remains the key live diagnostic.
- [ ] Honor's retained crash buffer contains RetroArch native `SIGABRT` entries on October 3, including `pthread_mutex_lock called on a destroyed mutex` in `hwuiTask*`; it does not contain a fresh crash timestamp for this report. RetroArch is version `1.22.2_GIT`. Do not claim the latest crash was fixed or relaunch it until a fresh crash can be captured safely.

## 2026-10-04 Home controller readiness shortcut

- [x] On the populated wide Home dashboard, the Controller status card now participates in controller focus and activates Settings. It has button semantics and an explicit accessible description; other status cards remain informational. This makes the disconnected-controller state actionable from the living-room landing screen.
- [x] Added a Home regression assertion for the Controller status shortcut. The 744-test unit suite, Android-test source compilation, debug APK assembly, and lint all pass. Instrumentation remains compile-only because the connected-test runner has previously removed/reset the user's app data.
- [x] Installed the verified APK in place on the Honor at 07:40 local time; `adb install -r` succeeded and did not clear app data.
- [ ] No physical HDMI display is currently enumerated; only the phone panel and Android Studio screen-sharing virtual display appear. Verify the controller status focus/action on the actual TV with the Bluetooth controller connected before marking the living-room path accepted.

## 2026-10-04 Home setup deep links

- [x] Home's first-run “Set up your room” action now opens Settings directly at Storage; the populated Home Controller status card opens Settings directly at Controllers. The wide Settings rail initially focuses the requested section while the settings pane scrolls to its anchor, keeping the action usable from a couch/controller.
- [x] Added save/restore coverage for the pending Settings-section request and consumption, plus Home action coverage. Verification passes: 745 JVM tests (0 failures/errors, 1 skipped), Android-test Kotlin compilation, debug APK assembly, and lint.
- [x] Added additional TV-navigation assertions that empty Home's Settings action invokes the Storage setup route and that the requested Controllers rail item can receive initial focus. `compileDebugAndroidTestKotlin` passes with these assertions; they were compiled only, not run on the data-bearing Honor.
- [x] Installed the build in place on the Honor at 07:46 local time with app data preserved. No instrumentation was run.
- [ ] TV behavior and focus on the requested Controllers/Storage rail item remain unverified because HDMI is still not enumerated; do not use screen-sharing virtual display as TV acceptance.

## 2026-10-04 wide Home quick-launch density

- [x] Wide Home quick-launch tiles now give their title one line while compact tiles retain two. The populated wide Home also uses slightly tighter vertical rhythm and card heights (hero 188dp, collection tiles 128dp, quick-launch minimum 92dp) to bring the Recently played row above the controller footer without changing the compact layout.
- [x] Added regression assertions for enlarged-text quick-launch labels, equal tile heights, activation, and Recently played staying inside a constrained 580dp TV content viewport. Android-test Kotlin compilation, APK build, lint, and JVM unit tests pass.
- [x] Installed this verified build over the existing Honor app without clearing its data; the app was not launched on the phone display.
- [x] Re-audited `artifacts/gamebox-tv-home-verified-fullscreen.png` at native 1920×1080: the Recently played card is fully visible (its lower edge sits above the footer); the earlier “clipped” note was inaccurate. The system Dock is separately present across the bottom edge in this older capture.
- [ ] The latest installed build has not been visually accepted on the actual TV: current ADB enumeration is only the 1312×2848 built-in panel plus Android Studio's virtual screen-share surface. Do not use the virtual display as TV evidence.

## 2026-10-04 bidirectional route motion

- [x] Replaced entry-only screen animation with paired route-aware enter/exit transitions: tabs move horizontally in the navigation direction, details/downloads move vertically, and outgoing content moves oppositely with a restrained fade/scale. Reduced-motion mode continues to render without animation.
- [x] Route-aware composition keeps the outgoing destination/details visible during the transition instead of dropping it immediately, while disabling its focus and semantics so controller/accessibility users cannot target stale controls. UI regressions now cover both stale outgoing controls and immediate route replacement when reduced motion is enabled. JVM tests (745, 1 skipped), Android-test compilation, APK assembly, and lint pass.
- [x] Installed the verified APK over the Honor app without clearing data; it was not opened on the phone panel.
- [ ] Physical HDMI is absent, so the new transition timing and controller focus handoff have not been accepted on the actual TV. Instrumentation was compiled but not run on the data-bearing Honor.
- [x] Follow-up audit found a shared-registry lifecycle race: an outgoing screen's `onDispose` could clear X/Y actions already registered by the incoming route. Controller-action registrations now carry owner leases; stale cleanup is ignored. Added a unit regression simulating overlapping outgoing/incoming registrations; 746 JVM tests pass (1 skipped), Android-test Kotlin compilation, APK build, and lint pass. Installed in place on Honor with data preserved.
- [x] Prevented outgoing animated routes from registering controller actions at all; incoming route retains the only active X/Y and Back owner during the transition. Added a Compose regression simulating an outgoing owner disposing after the incoming owner registers. Full unit/build/lint gates pass; the Android test is compiled only, not run on the data-bearing device. Latest APK installed in place with app data preserved.
- [ ] Current Android input diagnostics show `USB OPTICAL MOUSE Keyboard` advertising `KEYBOARD | DPAD | JOYSTICK`, not a confirmed physical gamepad; HDMI remains absent. Do not count this as controller/TV acceptance. Verify controller action handoff on the actual TV once the real Bluetooth controller and wired output are connected.
- [x] Read the mouse device's detailed Android mapper: it exposes only `GENERIC_1`, not standard stick/hat axes. GameBox's current controller predicate excludes that generic-axis mouse, so it should remain “Not connected”; this is a device classification check, not a live UI/controller acceptance test.

## 2026-10-04 browser download pause/re-entry recovery

- [x] A successful browser-download return now opens GameBox Download Manager immediately, so the newly queued transfer is visible without another navigation step.
- [x] Android `DownloadManager.STATUS_PAUSED` is now reflected as “Paused by Android” rather than misleadingly “Downloading”. Download Manager offers “Restart download” for paused transfers, reusing the stored HTTPS URL and request metadata; Android's own automatic retries remain unchanged.
- [x] Added Compose coverage for restarting a paused transfer. Unit tests, Android-test Kotlin compilation, debug APK assembly, and lint all pass; the APK was installed in place on Honor with `adb install -r`, preserving app data. No UI was launched on the handset.
- [ ] No physical HDMI display is enumerated, and GameBox has no persisted tracked-browser-download record on this device yet. The leave-page/download/import flow still needs a legal test transfer on TV; the current absence of a record means the WebView download callback has not been verified end-to-end here.
- [ ] Honor's retained crash buffer includes RetroArch's native `pthread_mutex_lock called on a destroyed mutex` in `hwuiTask*`; no fresh report-specific RetroArch crash was available. This is not a GameBox exception and remains unresolved pending controlled TV reproduction with a stable emulator build/core.

## 2026-10-04 starter artwork coverage

- [x] Reviewed the previous physical-TV Store capture: the bundled Cave Story and Celeste Classic tiles fell back to initials despite being featured starter titles. Added title-correct source artwork metadata from Libretro's official thumbnail archive (Cave Story) and Wikimedia Commons (Celeste Classic; the file is identified there as an MIT-licensed game screenshot). Kept the existing guard against borrowing VVVVVV/Celeste commercial art for similarly named games.
- [x] Added a starter-catalog regression asserting both exact artwork URLs and parsing through the authorized-catalog validator. Verification: 747 unit tests pass (0 failures/errors), Android-test Kotlin compilation, debug APK assembly, and lint pass. Installed the build in place on Honor with `adb install -r`; data was preserved and the app was not launched on the phone.
- [ ] The wired TV is not currently enumerated. Existing catalog rows may need Store > Refresh to merge this provider artwork before the visual difference appears; confirm Cave Story/Celeste Classic covers on the physical TV after HDMI returns.

## 2026-10-04 HONOR external-display Dock guidance

- [x] HONOR's official MagicOS 10 help confirms the global taskbar is controlled in Settings > Large screen features > Global Taskbar; the official global Dock guide also recommends searching Settings for “Global Taskbar”. GameBox cannot reliably hide this persistent system Dock using immersive window flags alone.
- [x] Added conditional couch-readable guidance to GameBox > Settings > Display, shown only on HONOR devices when a physical external display is detected. Android Studio virtual displays do not trigger it. Unit coverage checks HONOR/physical, HONOR/virtual, non-HONOR/physical, and no-display cases.
- [ ] Current device state still has no HDMI display. Once reconnected, verify this message on the TV, follow the exact system setting path, temporarily disable and restore the user's original Global Taskbar state, then visually verify GameBox's clean-screen layout on the TV.

## 2026-10-04 tracked browser download telemetry follow-up

- [x] Browser-download cards now show transferred/total bytes, percentage, and transfer speed when the server provides a total size. Unknown-length transfers show bytes received and speed alongside indeterminate progress instead of an unexplained bar. Added Compose regression coverage for both cases and JVM coverage for rate formatting.
- [x] New and retried DownloadManager requests explicitly allow metered and roaming networks so a transfer started by the user is not restricted to unmetered Wi-Fi. This can incur mobile-data charges.
- [x] Verification: `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, and `:app:assembleDebug` passed. The Android UI tests were compiled but not run on hardware.
- [x] Retrieved the stored 2026-10-03 Honor ANR: RetroArch 1.22.2 `RetroActivityFuture` timed out dispatching input; the main thread was blocked in ART JNI `CallObjectMethodV` while consuming an input event, with additional frames in RetroArch's native activity library. This is an emulator-native ANR, not a GameBox exception; the available report does not establish the exact trigger.
- [ ] TV output is not currently enumerated (only Honor built-in and Android Studio virtual displays). Do not install/launch for phone-screen validation; still verify a legal download leaving the source-details page and RetroArch launch/return on the physical TV with controller attached.

## 2026-10-04 featured Store hero artwork treatment

- [x] The wide Store featured-game hero now gives available box art a dedicated poster frame, separate from the cinematic background. The poster only appears at wide living-room breakpoints; compact screens keep their existing uncluttered hero. Added Compose coverage for the wide/compact behavior and a descriptive accessibility label.
- [x] Verification: 752 JVM tests (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, and debug APK assembly pass. The Compose interaction test is compiled but not run on the data-bearing Honor.
- [ ] Current device inventory still exposes only the Honor built-in screen and Android Studio's virtual sharing display; no physical TV or gamepad is connected. Verify poster sizing, artwork loading, focus behavior, and footer clearance on the HDMI TV before acceptance.

## 2026-10-04 physical-TV readiness recheck

- [x] Rechecked ADB after the Store hero pass: Honor is connected, but Android exposes only its 1312×2848 built-in display and the 800×1738 `studio.screen.sharing` virtual display. Input inventory contains a USB optical mouse and its keyboard/DPAD/JOYSTICK interface; no separate Bluetooth gamepad is enumerated. The virtual display and mouse must not count as TV/controller acceptance.
- [ ] Wait to launch the app until the wired TV appears as a physical external display and the controller is independently identified. Then install/launch on that display only and verify Home/Store/Details/Downloads focus, the new featured cover frame, fullscreen Dock behavior, download continuation, and game launch/return.

## 2026-10-04 tracked-download details guidance correction

- [x] Removed the inaccurate instruction telling users to keep the game-details page open while a supported transfer imports. The source guidance now explains that GameBox-tracked transfers continue in the background, appear in Downloads, and import automatically; other browser downloads still require manual import.
- [x] Added unit coverage for both tracked and untracked source guidance. Verification passes: 752 JVM tests (0 failures/errors, 1 skipped), Android-test Kotlin compilation, and debug APK assembly.
- [ ] Physical TV/controller acceptance is still pending: current ADB inventory has no physical external display or separate gamepad. Verify the real leave-details/download/auto-import flow on TV when the wired output returns.

## 2026-10-04 wide discovery-details cover treatment

- [x] Extracted the Store hero's poster treatment into a shared accessible cover component and added it to wide discovery-game details. The cinematic artwork remains the backdrop; the dedicated cover is only shown when box art exists and the display is wide enough, preserving compact layouts.
- [x] Android unit tests, Android UI-test compilation, and APK assembly passed for the shared Store/Details treatment. Visual sizing and focus traversal on the current build remain unverified on hardware.
- [ ] ADB still reports no wired TV and no separate gamepad. Verify the discovery details cover against real metadata/artwork on the physical 1920×1080 TV before acceptance.

## 2026-10-04 controller search reopen regression

- [x] Extended controller-keyboard instrumentation coverage for the full couch search loop: focus Search, press A to open, type a character, press B to close, preserve the query and return focus, then press A to reopen at the keyboard. Existing coverage still verifies D-pad traversal and B dismissal before shell-level Back.
- [x] JVM tests pass; the updated Android instrumentation sources compile and the debug APK assembles. The focus-restoration test is compiled but has not been run on hardware.
- [ ] Physical TV and a separate controller remain disconnected in ADB. Run this interaction with the actual Bluetooth gamepad on the TV once both return; do not validate on the phone/virtual screen.

## 2026-10-04 tracked-download detail progress clarity

- [x] Replaced the Details page's always-determinate zero-percent bar with a shared transfer progress component. Known lengths show bytes/percent; unknown lengths show indeterminate progress plus bytes received and speed; queued/importing remain indeterminate. Download Manager now uses the same component for consistent state presentation.
- [x] Unit tests, Android UI-test compilation, and APK assembly passed. Existing Compose progress tests compile against the shared row. The latest warning-only safe-call cleanup is included in the final incremental verification.
- [ ] No physical TV or separate gamepad is currently enumerated. Verify that unknown-length transfers visibly advance on Details and Download Manager during a legal TV download test.

## 2026-10-04 Home download-first onboarding

- [x] Reframed Home's second getting-started step around the more frequent Vimm flow: open Vimm Lair in GameBox, start a tracked download, and let GameBox verify/import it automatically; retained manual import for files already on-device. The prominent Home discovery action already opens Vimm in GameBox, and Vimm-backed game details already label the main action “Download.”
- [ ] Confirm the Home hierarchy and game-details action on the physical TV; do not launch on the phone or virtual display for acceptance.

## 2026-10-04 Home duplicate hero cleanup

- [x] Removed the repeated Continue Playing title from the secondary Home row. The row now shows other recently played titles, falls back to Explore games when no other recent title exists, and is omitted when the hero is the only game. Added regression coverage for these selections.
- [x] Verification: 754 JVM tests pass, Android UI-test sources compile, debug APK assembles, and Android lint passes.
- [ ] Fresh read-only ADB inventory finds only the 1312×2848 Honor built-in display and Android Studio's 800×1738 private virtual display; no physical TV display is available. Verify the corrected wide Home composition on TV before acceptance.

## 2026-10-04 shared controller button focus treatment

- [x] Added an animated GameBox gradient focus outline to shared filled, outlined, and text Material buttons. Existing controller activation/sound routing is unchanged, and the outline respects the reduced-motion setting.
- [x] Verification: unit tests, Android UI-test Kotlin compilation, debug APK assembly, and lint pass. Existing Details-action instrumentation covers controller-A activation on the shared primary button, but remains compiled-only without hardware execution.
- [ ] Final display inventory still exposes only the Honor built-in screen and Android Studio's private virtual display. Visually tune the focus outline and focus traversal using the Bluetooth controller on the physical TV before acceptance.

## 2026-10-04 TV header focus affordances

- [x] Applied the same animated gradient focus outline to the header Search and Download Activity icon buttons, in both wide and compact navigation. Expanded navigation instrumentation assertions to verify those actions receive focus while preserving controller-A activation coverage.
- [x] Verification: JVM tests, Android UI-test Kotlin compilation, debug APK assembly, and lint all pass.
- [ ] Current read-only ADB display inventory remains limited to the 1312×2848 built-in Honor panel and Android Studio's private 800×1738 virtual display. Do not launch there for acceptance; verify the ring's size/contrast and header traversal on the physical TV/controller.

## 2026-10-04 controller focus visibility coverage

- [x] Reused the Blueprint gradient focus ring on compact and wide header icon actions (Search and Download Activity). Navigation instrumentation now asserts those controls can take focus; existing tests continue to cover controller-A activation and D-pad tab movement.
- [x] Unit tests, Android UI-test Kotlin compilation, APK assembly, and lint pass.
- [ ] Latest ADB inventory still exposes only the built-in Honor screen and Android Studio's private virtual display. Physical-TV focus visibility and traversal remain unverified; no phone/virtual launch was used.

## 2026-10-04 accessible game-card playtime

- [x] Screen-reader game-card descriptions now announce natural playtime phrases, omitting zero units and handling singular units (for example “1 hour 1 minute played”). Visible compact card labels are unchanged.
- [x] Added duration edge-case tests. Verification: 755 JVM tests pass (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, APK assembly, and lint pass.
- [ ] Physical-TV visual/focus acceptance remains pending; latest ADB exposes only Honor's built-in panel and Android Studio's private virtual display.

## 2026-10-04 hardware checklist target correction

- [x] Corrected `docs/HARDWARE_VALIDATION.md` from the outdated Magic5 Pro reference to the actual deployment phone, Honor Magic6 Pro (`PGT-N19`), in the test intro, HDMI/controller checkpoint, and runtime-evidence instructions.
- [x] Fresh read-only ADB confirms the connected device reports product `PGT-N19`; its displays are the 1312×2848 built-in panel and Android Studio's private 800×1738 virtual display only.
- [ ] Physical HDMI/controller validation remains open. Do not launch on the phone or virtual display as a substitute for the user's TV target.

## 2026-10-04 import-cancel controller focus

- [x] Added the shared animated focus ring to the active-import Cancel button, and added an instrumentation regression for controller A focus/activation. This closes a raw Material TextButton exception in a long-running, user-controlled import flow.
- [x] Verification: 755 JVM tests pass (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, debug APK assembly, and lint pass. The new controller test is compiled but not run on hardware.
- [ ] Fresh ADB still reports only the built-in Honor panel and Android Studio virtual display; input devices include the USB mouse/keyboard but no independent Bluetooth gamepad. Validate Cancel focus and activation on the physical TV with the controller.

## 2026-10-04 Vimm search-correction controller action

- [x] Replaced the raw Vimm “Did you mean?” TextButton with the shared Blueprint text action, adding the couch-sized tap target, focus outline, UI click feedback, and explicit controller-A handling.
- [x] Added instrumentation for focusing and activating “Did you mean ‘God of War’?” with controller A. Verification: 755 JVM tests, Android UI-test Kotlin compilation, APK assembly, and lint all pass; instrumentation remains compiled-only.
- [ ] Current ADB still exposes no physical TV or independent controller. Verify the corrected-query selection on the actual TV when both are connected; do not count phone/virtual interaction.

## 2026-10-04 controller metadata-filter actions

- [x] Converted the Genre/Region/Language cycling control to the shared Blueprint text button, so each filter gets the visible focus ring, native click feedback, and explicit controller-A activation.
- [x] Added instrumentation verifying a focused Genre filter changes from “All” to its first value with controller A. Verification: 755 JVM tests pass (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, APK assembly, and lint pass; instrumentation is not hardware-run.
- [ ] Latest device evidence still shows only the Honor panel and Android Studio virtual display, with a USB mouse/keyboard but no separate Bluetooth controller. Verify filter traversal and activation on the physical TV when available.

## 2026-10-04 game-details tools controller actions

- [x] Replaced the raw OutlinedButtons for “Game settings & emulator” and “Edit metadata” with the shared Blueprint outlined actions, restoring animated focus indication, click feedback, and explicit A-button handling.
- [x] Added instrumentation covering focus and controller-A activation for both Details tools. Verification: 755 JVM tests pass (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, APK assembly, and lint pass; instrumentation is compiled-only.
- [ ] Physical-TV visual and controller acceptance is still pending: current ADB shows no external TV or independent gamepad. Verify Details tool focus/activation on the Honor connected to the TV when available.

## 2026-10-04 Vimm source and import action focus

- [x] Added the shared animated focus outline to configured discovery-source and external-link buttons. Converted manual copy and multi-disc import actions to the shared outlined-button wrapper, aligning touch, sound, focus, and controller-A behavior.
- [x] Extended the manual-copy test to assert controller focus and A activation; existing tests cover Vimm source focus/A, external-link A, and multi-disc picker A. Verification: 755 JVM tests, Android UI-test Kotlin compilation, APK assembly, and lint pass; hardware instrumentation remains pending.
- [ ] Fresh ADB still sees only the 1312×2848 Honor panel and Android Studio's private 800×1738 virtual display; only the USB mouse/keyboard, not a separate Bluetooth controller, is enumerated. Validate this browse→source→import focus flow on the actual TV.

## 2026-10-04 Store/Details primary action focus

- [x] Routed the Store featured-game “View Details” CTA through the shared Blueprint primary button and routed the compact Details primary action through the shared outlined button. Removed the redundant explicit focus target from the compact Material action to avoid a possible double stop in D-pad traversal.
- [x] Expanded Compose instrumentation to focus/activate the featured Details action and compact owned-copy import with controller A. Unit tests, Android UI-test Kotlin compilation, APK assembly, and lint pass; instrumentation is compiled-only.
- [ ] Current ADB exposes only the Honor internal display and Android Studio's private virtual display. Verify focus/highlight and one-stop traversal on the actual TV/controller before acceptance.

## 2026-10-04 persistent Vimm download tracking

- [x] Replaced the process-only browser-download observer with uniquely named WorkManager work. The Android Download Manager still owns the transfer, while WorkManager restores GameBox progress tracking and automatic verification/import after app process recreation or leaving the browser screen.
- [x] Active transfer identities are deduplicated before scheduling; an interrupted monitor periodically yields and retries without cancelling the underlying Android download. Added JVM regression coverage for active/terminal queue filtering and stable per-transfer work identities.
- [x] Verification: `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, and `:app:assembleDebug` pass.
- [ ] Reproduce a real Vimm download and process/app exit on the Honor connected to the physical TV; verify the GameBox Download Manager row persists, progress recovers, and the completed copy imports automatically. Current ADB exposes only the built-in Honor display, so no install/launch was attempted.
- [ ] RetroArch native crashes remain under investigation. HONOR crash records show repeated `pthread_mutex_lock called on a destroyed mutex` exits in `com.retroarch`; capture a fresh crash correlated with a specific game/launch before changing GameBox's intent contract.

## 2026-10-04 Library repair controller focus

- [x] Converted “Locate files”, “Forget”, and both missing-file confirmation actions to the shared Blueprint button wrappers, aligning animated focus, audio feedback, disabled states, and controller-A activation.
- [x] Added a controller regression that uses A to open the save-safe forget confirmation and A to cancel; the destructive confirmation is not activated. Verification: 757 JVM tests pass (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, APK assembly, lint, and `git diff --check` pass.
- [ ] Run the reimport/focus flow on the Honor connected to the physical TV; current device topology does not expose a TV display, so instrumentation remains compiled-only.

## 2026-10-04 metadata editor controller focus

- [x] Routed metadata match lookup, candidate selection, Save, Use provider metadata, and Cancel through the shared Blueprint buttons, preserving enabled states while adding the animated focus ring, UI feedback, and controller-A action contract.
- [x] Added a Compose instrumentation regression that focuses Cancel and activates it with controller A, asserting that the editor dismisses without saving. Verification: 757 JVM tests pass (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, APK assembly, lint, and whitespace checks pass.
- [ ] Test the editor and metadata-match navigation on the physical TV/controller. Fresh ADB still exposes only the Honor built-in screen and Android Studio's private virtual display; no app launch was made on either.

## 2026-10-04 wide app-hub controller polish

- [x] Routed the wide hub's Audio, Controllers, Open files, Desktop mode, and external-display actions through Blueprint outlined buttons.
- [x] Removed the nested interactive Launch button from the Moonlight hero so its animated card is one focus/activation target; retained a strong visual CTA and added a spoken button label that reflects installed/setup state.
- [x] Added an instrumentation regression for the hero's accessible label, single click action, controller focus, and A activation. Verification: 757 JVM tests pass (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, APK assembly, lint, and whitespace checks pass.
- [ ] Confirm wide-hub focus order and quick actions on the physical TV/controller. This remains unverified until the Honor exposes a real external display; do not substitute its built-in or private virtual screen.

## 2026-10-04 TV search keyboard focus and feedback

- [x] Added the shared animated gradient focus ring to the search trigger and individual keyboard keys. Controller-A and touch activation now use the standard GameBox click-feedback action, and held-A key repeats are consumed without repeatedly inserting characters.
- [x] Made keyboard focus scaling honor the reduced-motion setting and corrected a stale instrumentation expectation for the search field's spoken label.
- [x] Verification: 757 JVM tests pass (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, APK assembly, lint, and `git diff --check` pass.
- [ ] Physical-TV execution of D-pad traversal, A selection, B close, and reduced-motion appearance remains pending; fresh ADB reports only the built-in Honor screen and Studio's private virtual display, so no device app launch was attempted.

## 2026-10-04 PC host setup controller actions

- [x] Extracted the Moonlight host-probe action row and moved “Check and save host” and “Clear saved host” onto the shared Blueprint actions for consistent focus rings, click feedback, and controller-A activation.
- [x] Added a Compose instrumentation test that focuses and activates both actions using A while keeping the real settings repository untouched. Verification: 757 JVM tests pass (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, APK assembly, lint, and whitespace checks pass.
- [ ] Verify PC hub host setup on the physical TV/controller when the HONOR exposes an external display; current ADB still lists only built-in and private virtual displays.

## 2026-10-04 shared activation and TV search compatibility

- [x] Unified the TV search field and on-screen keys with the shared activation policy, adding D-pad Center and Enter in addition to gamepad A. Initial activation fires once; held-button repeats are consumed without duplicate actions.
- [x] Extended the search keyboard instrumentation sequence to use D-pad Center for both opening the keyboard and selecting a letter; added unit cases for activation keys and repeat handling.
- [x] Verification: 758 JVM tests pass (0 failures/errors, 1 skipped), Android UI-test Kotlin compilation, APK assembly, lint, and whitespace checks pass.
- [ ] Validate A, D-pad Center, and Enter behavior with the physical controller/TV setup. HONOR still exposes only its built-in panel and Android Studio's private virtual display; no device app launch was attempted.

## 2026-10-04 RetroArch disc-set handoff and TV polish

- [x] Traced the saved physical-TV RetroArch ANR capture to a multi-file PlayStation `.cue` launch. GameBox had verified companion tracks but exported only the descriptor to the filesystem path RetroArch consumes.
- [x] RetroArch now receives a temporary, uniquely named MediaStore folder containing the complete verified descriptor/track set with sibling filenames preserved; all exported rows are cleaned together when the game session returns. A missing/invalid RetroArch core contract is rejected before launching its native activity.
- [x] Added an Android integration regression with a synthetic CUE/BIN pair that asserts the descriptor's referenced sibling exists in the exported directory and that owned exports are cleaned. Instrumentation compiles but was not executed against user data or on the phone panel.
- [x] DownloadManager startup no longer removes a transfer when optional retry-cookie persistence fails; durable monitor scheduling is requested immediately after the tracked record is saved.
- [x] Increased only wide Home's major-section spacing from 6dp to 12dp; compact layout is unchanged.
- [x] Expanded the wide Download Manager empty state into a three-stage, controller-friendly choose/download/verified-library guide while preserving its focused Store action; compact zero-state remains unchanged.
- [x] Vimm/configured-source result rails now merge known console aliases and follow the default preset order instead of splitting shelves on display-name spelling or response ordering; added unit coverage for PS2 and GBA aliases.
- [x] Verification: 760 JVM tests, 0 failures/errors, 1 skipped; `compileDebugAndroidTestKotlin`, `assembleDebug`, `lintDebug`, and `git diff --check` pass.
- [ ] These changes have not been installed or run on the Honor. Fresh ADB exposes only the 1312×2848 built-in panel and Android Studio's private screen-share display; keep physical-TV launch, RetroArch ANR resolution, fullscreen behavior, controller navigation, and Vimm background-download/import acceptance open. The CUE companion omission is a concrete handoff defect and plausible cause of the captured ANR, but only a fresh hardware run can confirm the crash is resolved.

## 2026-10-04 background download resilience and RetroArch follow-up

- [x] Separated Android DownloadManager ownership from optional GameBox follow-up work: failure to save retry cookies or schedule the monitor can no longer remove a transfer the user already started.
- [x] Startup reconciliation now retries transient WorkManager scheduling failures per active transfer without terminating the collector; durable download records remain eligible for recovery on the next app start.
- [x] Verification: 760 JVM tests, 0 failures/errors, 1 skipped; Android UI-test Kotlin compilation, debug APK assembly, lint, and `git diff --check` pass.
- [ ] The app-specific Android DownloadManager transfer and GameBox's Download Manager row still need a lawful end-to-end transfer on the physical TV, including leaving the source page and confirming continued progress/import. The latest available device topology does not expose the TV, so this was not hardware-tested.
- [ ] RetroArch's previously captured crash remains unconfirmed as resolved: the saved report shows a native renderer-thread abort in RetroArch, and the complete CUE/BIN export fix is only a plausible trigger correction pending a controlled TV launch. No emulator was relaunched and no user save data was touched.

## 2026-10-04 physical display recheck

- [x] Rechecked the live Honor over ADB: only the 1312×2848 built-in panel and Android Studio's private 800×1738 screen-share display are enumerated; no physical HDMI/TV display is present.
- [x] Input inventory still shows the USB optical mouse and its keyboard companion advertising `KEYBOARD | DPAD | JOYSTICK`; no distinct physical controller is enumerated. Do not count that mouse interface as a controller.
- [ ] Physical-TV visual, controller, download-continuation, and emulator-launch acceptance remains open. No app was launched on the phone panel or virtual display in this recheck.

## 2026-10-04 first-run room setup guidance

- [x] Split empty-Home onboarding into separate storage and controller setup cards. The wide and compact layouts now explain each step independently and route to the matching Settings section (`Storage` or `Controllers`) instead of sending both tasks to Storage. Their sequence is now setup first, then discover, download, and emulator readiness.
- [x] Gave each of the five setup stages a distinct console-style icon/number treatment and reserved a consistent wide-card height so storage/controller actions sit in a stable place; compact cards remain vertically stacked.
- [x] Updated the Home dashboard interaction regression to assert the separate guidance and activate both Settings actions with simulated controller A input.
- [x] Verification: 760 JVM tests, 0 failures/errors, 1 skipped; Android UI-test Kotlin compilation, debug APK assembly, lint, and `git diff --check` pass.
- [ ] The five-card wide onboarding layout and focus traversal still need visual/controller acceptance on the physical TV. A post-build ADB check still exposes only the Honor built-in panel and Android Studio private virtual display, plus the USB mouse/keyboard; the app was not launched on either non-TV display.

## 2026-10-04 starter artwork loading polish

- [x] Changed Celeste Classic's starter-catalog art reference from Wikimedia's `Special:FilePath` redirect to its stable direct `upload.wikimedia.org` PNG URL. The source page identifies the image as a Celeste Classic screenshot and publishes the file under MIT terms; the catalog integrity test now guards the direct artwork URL.
- [x] Verification: 760 JVM tests, 0 failures/errors, 1 skipped; Android UI-test Kotlin compilation, debug APK assembly, lint, and `git diff --check` pass.
- [ ] Validate loaded art on the current debug APK over Honor/TV. Live ADB still enumerates no physical HDMI display or distinct game controller, so no device launch was made. The other title-art references and offline/missing-art fallback remain separate acceptance checks.

## 2026-10-04 Home status motion polish

- [x] The wide Home storage-usage bar now eases to refreshed capacity values with a restrained spring, while Reduce Motion makes updates snap immediately. The usage bar also exposes its current human-readable percentage to accessibility services; a Compose regression covers the label. Compact layouts and all other status-card behavior are unchanged.
- [x] Verification: 766 JVM tests, 0 failures/errors; Android-test Kotlin compilation, debug APK assembly, lint, and whitespace checks pass.
- [ ] Review the motion and couch-distance readability on the physical TV; the current HONOR connection exposes no external TV display, so this visual check remains open.

## 2026-10-04 modal action controller consistency

- [x] Replaced the raw Material text actions in Quick Actions, System Status, Profile Switcher, and the screenshot viewer with shared GameBox text buttons. These actions now share the animated focus ring, reduced-motion behavior, UI click feedback, and controller A activation contract.
- [x] Added a screenshot-viewer regression that opens an image and closes the modal using simulated controller A. Verification: 766 JVM tests pass; Android UI-test sources compile; lint and debug APK assembly pass.
- [ ] Execute the modal/controller flow on the Honor connected to the actual TV. Current ADB exposes only the built-in phone panel and Studio's private virtual display, neither of which is accepted as TV verification.

## 2026-10-04 remaining compact actions use shared focus feedback

- [x] Routed Compact Store refresh, the compact PC Hub's Android/DeX and external-display actions, and sanitized diagnostics export through the shared GameBox button components. The actions now inherit the same focus ring, reduced-motion behavior, sound setting, and controller activation used elsewhere.
- [x] Extracted the compact Store refresh control and added controller-A instrumentation coverage; the refresh action enters its disabled “Refreshing…” state after activation. Verification: 766 JVM tests pass, Android-test Kotlin compilation, lint, and debug APK assembly pass.
- [ ] Run these actions on the physical TV with the Bluetooth controller. Current ADB continues to report only the Honor's built-in screen and Android Studio's private virtual display; no TV acceptance is claimed.

## 2026-10-04 shared controller feedback for filter chips

- [x] Added `BlueprintFilterChip` and moved every production Material filter chip through it: Store console/installed filters, Library platform/genre/favorite/metadata filters, Downloads queue tabs, emulator selection, settings navigation, discovery-source type, display safe-area, and developer options. Selected state and existing enabled/accessibility behavior are preserved while adding the common animated focus treatment, reduced-motion support, click sound setting, and A activation.
- [x] Focused chips now automatically scroll into view, including chips in long horizontal console/filter rails. Added Compose regressions for controller-A selection and bringing an overflow chip into the viewport. Verification: 766 JVM tests pass, Android UI-test compilation, lint, and debug APK assembly pass.
- [ ] Run the filter navigation and selection sequence with the physical controller on the wired TV. ADB still exposes no physical TV display; the UI regression has only been compiled, not accepted on target hardware.

## 2026-10-04 Settings switch feedback and controller activation

- [x] Added `BlueprintSwitch` and migrated all eight Settings switches: unavailable shortcuts, per-app visibility, UI sounds, reduced motion, unmetered downloads, Windows Companion, discovery-source enablement, and focus diagnostics. They preserve checked/enabled semantics and now share animated focus, Reduce Motion, UI sound, and controller-A behavior.
- [x] Added Compose regressions for visible focus/A toggling and for disabled-switch protection. Verification: 766 JVM tests pass, Android UI-test compilation, lint, and debug APK assembly pass.
- [ ] Validate Settings switch traversal/toggling on the actual wired-TV output with the physical controller. Fresh ADB inventory still finds only the built-in display plus Studio's private virtual display and the USB mouse/keyboard; no gamepad or TV output is enumerated.

## 2026-10-04 uninstall acknowledgment controller safety

- [x] Added `BlueprintCheckbox` for the cloud-backup/local-backup acknowledgment in the content-removal confirmation. The control now has visible controller focus, sound/reduced-motion consistency, controller-A toggling, and an explicit accessible label.
- [x] Updated the Compose safety regression to activate the acknowledgment with A, verify that this first activation only enables the destructive confirm action, then use a separate A on “Uninstall content.” Verification: 766 JVM tests, Android UI-test compilation, lint, and debug APK assembly pass.
- [ ] The new sequence has not been run with a physical controller on the TV. Current HONOR topology still provides no TV display or distinct gamepad.

## 2026-10-04 bring focused settings toggles into view

- [x] `BlueprintSwitch` and `BlueprintCheckbox` now request bring-into-view when controller focus lands on them, so a setting below the fold or an overflow acknowledgment is revealed before activation.
- [x] Added a Compose regression for a focused below-the-fold switch scrolling into the viewport. Verification: 766 JVM tests pass, Android UI-test compilation, lint, and debug APK assembly pass.
- [ ] Confirm the scroll/focus handoff on the physical TV with the controller. TV and independent gamepad remain absent from current HONOR ADB inventory.

## 2026-10-04 background import progress in Download Manager

- [x] Persist importer stage, processed/total bytes, and file index/count in each tracked browser-download record. The Download Manager now replaces the generic spinner with determinate copy/verification/extraction progress when the importer reports a known total, and shows the active stage and file count.
- [x] Added a Compose regression covering extraction stage, multi-file progress, and percent/byte readouts. Verification: unit tests, Android UI-test Kotlin compilation, lint, debug APK assembly, and `git diff --check` pass.
- [ ] Run a legal archive download/import on Honor projected to the physical TV and verify progress stays live after leaving the source page. ADB currently exposes only the built-in Honor panel and Android Studio's private virtual display; no app launch was made on either in lieu of the TV.

## 2026-10-04 local import cancellation action consistency

- [x] Replaced the import-progress Cancel action's duplicated Material/focus wiring with the shared Blueprint text action. Touch, focus ring, UI sound, reduced-motion behavior, and A-button activation now follow one reusable path.
- [x] Existing Compose coverage checks both touch and controller-A cancellation. Physical-TV focus/readability acceptance remains open because the Honor currently reports no external display.

## 2026-10-04 Download Manager couch-distance readability

- [x] Raised Download Manager supporting/status/progress text from 9–10sp to 12sp, including browser transfer identity, import stage/byte readout, metric labels, empty-state step descriptions, and side-rail notes. Widened the wide-layout status rail from 198dp to 224dp so the clearer labels wrap less aggressively.
- [ ] Verify the resulting wide layout at native TV resolution and couch distance. Current Honor ADB exposes only the phone panel and Studio's private virtual display, so no substitute display was used.

## 2026-10-04 Blueprint-scale Home composition

- [x] Reproportioned wide Home around the supplied 1920×1080 reference using the observed Honor Desktop rendering scale (~1.5× logical dp): 212dp hero, 200dp status rail, 116×140dp collection cards, 198×105dp recently played cards, and 100dp minimum quick-launch tiles. Compact sizing remains separate.
- [x] Updated Compose layout regressions for the TV content viewport, hero action bounds, quick-launch minimum, and wide Home sizing. `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `lintDebug`, and `assembleDebug` passed (766 unit tests, 0 failures, 1 skipped).
- [x] Added an Android regression asserting that the visible external-display host carries `FLAG_KEEP_SCREEN_ON`; instrumentation Kotlin compilation passes. This guards the in-app awake behavior, not manual device unlock or keyguard dismissal.
- [ ] Re-capture and accept the corrected layout, focus visibility, and overscan on the physical HDMI TV. A trusted 1920×1080 HDMI display was confirmed and the prior APK was launched from the TV desktop; after installing the corrected APK, the Honor locked and the HDMI capture showed “Phone is locked, please unlock.” The corrected Home layout therefore still needs a live TV screenshot and review. Do not substitute phone-screen or emulator validation.
- [ ] Recheck HONOR's bottom dock/taskbar after unlock. The earlier live HDMI capture showed it overlaying roughly the bottom 43px and covering part of GameBox's controller footer; the dock component is `com.hihonor.hndockbar`. A secure setting named `game_dockbar_disable_mode` is present, but its effect was not verified or changed. Use HONOR's visible Global Taskbar control for the previously authorized temporary off/on comparison; restore the original setting afterward.

## 2026-10-04 live HDMI Home fit and controller recheck

- [x] Confirmed the unlocked HONOR Magic6 Pro is driving a trusted physical HDMI display at 1920×1080, 60Hz (Android display ID 298). Installed and launched the current debug build on that display; all acceptance observations in this entry use the HDMI capture, not the phone panel or emulator.
- [x] Tightened only wide Home's inter-section spacing from 8dp to 4dp and the wide recent-section title-to-card gap from 8dp to 4dp. Kept the Blueprint-scale hero, poster, quick-launch, and 198×105dp recent cards unchanged. The post-install HDMI screenshot shows the full recently played cards above the GameBox controller footer.
- [x] Verification: 766 JVM tests, 0 failures, 1 skipped; Android-test Kotlin compilation, lint, and debug APK assembly passed. Build installed successfully on the Honor.
- [ ] HONOR's bottom Dock/taskbar remains visible over the bottom ~43px of the HDMI capture despite GameBox's immersive-window request. The temporary Global Taskbar off/on comparison is still outstanding; don't alter the current system setting without confirming the visible control and original state.
- [ ] ADB's current input-device inventory contains the USB optical mouse and its keyboard companion, but no Bluetooth gamepad. GameBox correctly reports “Not connected”; reconnect the physical controller before acceptance of D-pad/A/B navigation or emulator mappings.

## 2026-10-04 compact console labels and multi-tab HDMI review

- [x] Added a compact card label alongside each Store console's full filter label. Home and library cards now resolve known platform aliases to readable short names such as PS1, GameCube, and GBA; the Store filter keeps its full “Game Boy Advance” name, and unknown platforms retain their original label.
- [x] Installed the updated build on the Honor and visually reviewed Home, Store, and Library using screenshots captured from the physical 1920×1080 HDMI output. The Home collection and recent cards no longer truncate long console names; Library's four-across poster rows and overview rail fit the TV viewport; Store vertical scrolling exposes the full source-game row and bottom actions above the fixed footer.
- [x] Verification: 767 JVM tests, 0 failures, 1 skipped; lint and debug APK assembly passed. `git diff --check` found no whitespace errors (Git emitted only existing CRLF normalization warnings).
- [ ] D-pad/A/B acceptance remains pending: Android currently lists the USB mouse and keyboard only, not the Bluetooth controller. The GameBox status correctly reads “Not connected.”
- [ ] The HONOR Dock/taskbar remains visible at the bottom of HDMI; the requested temporary off/on comparison still needs a verified Global Taskbar control and restoration of the original setting.

## 2026-10-04 game-details and screenshot-viewer HDMI review

- [x] On the physical HDMI display, opened Luigi's Mansion from Home and reviewed its game-details hero, installed status, Play Now action, Vimm Lair Download action, and screenshot rail. The primary actions and supporting metadata were legible at 1920×1080, with the persistent controller guide visible below.
- [x] Opened the screenshot preview on HDMI: the centered modal dims the background, preserves the game's screenshot aspect ratio, and exposes a visible Close action. Closed it and returned the TV session to Home without launching the emulator or starting a download.
- [ ] Physical controller confirmation remains pending because the current Honor input inventory still contains no Bluetooth gamepad; the current review used direct TV touch input only.
- [ ] HONOR's system Dock/taskbar is still visible along HDMI's bottom edge, separate from GameBox's fullscreen window policy.

## 2026-10-04 HONOR Dock control investigation

- [x] Checked HONOR's official Dock instructions. They describe searching Settings for “Global Taskbar” on a tablet or unfolded phone, but do not document auto-hide or wired-HDMI behavior; the guide is not sufficient to infer a safe toggle for this Magic6 Pro projection setup.
- [x] Reopened Settings on the HDMI display and attempted the documented search path. The input landed in Account Centre instead of a taskbar setting, so the overlay was closed without changing any system setting. GameBox was confirmed restored to the TV Home screen.
- [ ] Do not change the OEM Dock state until its exact control is visible and its current state can be recorded/restored. The Dock remains a known limitation to the requested edge-to-edge launcher presentation.

## 2026-10-04 Settings and Downloads TV layout review

- [x] Reviewed GameBox Settings and its Downloads section on the physical 1920×1080 HDMI output. The long settings pane scrolls independently while the category rail and controller footer stay fixed; the empty-download status, notification action, emulator adapter guidance, and HONOR Dock guidance remain visible/reachable.
- [x] Retried HONOR's documented Settings search at the visible search-field coordinates. Searching “Global taskbar” returns only “AI writing” and “AI Translate,” not a taskbar setting. No system setting was changed; Settings was closed and GameBox Home restored.
- [ ] The HONOR bottom Dock remains present, and no corresponding taskbar toggle is exposed by this device's current Settings search. Continue only through a verified projection/desktop-mode control or device firmware that exposes the documented setting.

## 2026-10-04 Download Manager HDMI layout review

- [x] Opened the header download-activity shortcut on the physical TV. Download Manager presents queue/completed tabs, four at-a-glance metrics, three completed browser-import records marked “Added to Library,” and a network/storage rail. At 1920×1080, labels and completion states are legible above the fixed footer.
- [x] Scrolled the right-hand Recent Completed rail and confirmed all three ready records can be brought into view. Returned the HDMI session to GameBox Home without starting or changing any transfer.
- [ ] A live transfer/progress-state layout still needs a lawful test download on the physical TV. The current queue is empty, so active, paused, retry, and import-progress states were not exercised in this visual review.
- [ ] Current Honor input enumeration still contains only the USB mouse/keyboard; physical gamepad navigation remains pending.

## 2026-10-04 PC Hub missing-app action on HDMI

- [x] Corrected the featured Moonlight card when the app is absent: the CTA now says “Get Moonlight” (rather than the misleading “Launch Moonlight”), its accessibility description explains that it opens an install page, and activation tries the Play Store listing with a web listing fallback. Other missing shortcuts keep their existing unavailable behavior.
- [x] Added JVM and Compose regressions for installed/missing action text, semantics, and activation. Verification: 768 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, debug APK assembly, and `git diff --check` passed.
- [x] Installed the build on the physical Honor Magic6 Pro and captured PC Hub from trusted HDMI display 298 at 1920×1080. The focused card visibly shows “Get Moonlight”; no store was opened during verification. Returned the session to Home afterward.
- [ ] The HONOR system Dock still overlays the bottom edge of the HDMI image, and its exact Global Taskbar control remains unavailable in Settings search. Physical Bluetooth-controller traversal also remains unverified while Android reports only the mouse/keyboard inputs.

## 2026-10-04 fullscreen handoff and live hardware recheck

- [x] Rechecked the actual Magic6 Pro by ADB: the trusted HDMI panel was present at 1920×1080/60Hz (Android display 298); the foreground was GameBox Home. The HONOR secure setting `game_dockbar_disable_mode` currently reads `2`; its meaning is undocumented in the surfaced official material, so it was not changed. The paired Bluetooth “Wireless Controller” is currently disconnected, and the only connected external input is the USB optical mouse/keyboard.
- [x] Exercised the legitimate in-app PC Hub “Show on TV” path. Android reported GameBox `ExternalDisplayActivity` as top resumed, and the immediate physical-screen capture showed GameBox Home on HDMI. Correction to the first capture diagnosis: HDMI had not disconnected. Its logical display ID changed from 298 to 310, while `screencap -d` requires the SurfaceFlinger/HWC display ID (the physical panel's ID was `4639578574838715141`); using the logical ID produced the earlier false “display not valid” result. A direct ADB start of `ExternalDisplayActivity` was rejected as expected because that activity is non-exported; no bypass was used.
- [x] Repeated a read-only active-display capture from the physical 1920×1080 HDMI panel, relaunched GameBox on logical display 310, and verified the Home screen there. No emulator or phone-panel image was used for this TV check.
- [ ] Keep the app visible on TV after the Show on TV transition and verify it does not return to HONOR's desktop launcher during idle/focus changes. The Activity was visible in the first capture but HONOR's desktop launcher later became top-resumed before GameBox was manually reopened.
- [ ] Reconnect the paired controller and validate D-pad/A/B on the live TV. Do not substitute synthesized keyevents, phone screen, or emulator input for physical-controller acceptance.

## 2026-10-04 suppress redundant external fullscreen requests

- [x] Device logs from the physical HDMI session showed one accepted external fullscreen request followed by a redundant request after HONOR had already expanded the window; HONOR rejected the second request with “The window is already fully expanded.” The external fullscreen guard now requests only while the focused external activity is in multi-window mode, matching the primary-display guard and still allowing focus-return retries if the shell restores a windowed task.
- [x] Added unit coverage for the external-display, focus, and multi-window conditions. Verification: `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `lintDebug`, and `assembleDebug` passed. Installed the APK on the Honor; live logs show the fullscreen request accepted on display 310 with no duplicate rejection during the observed launch, and HDMI capture shows the 1920×1080 Home UI.
- [ ] HONOR's bottom Dock remains visible over the lower edge even after its fullscreen request is accepted. The paired “Wireless Controller” is still disconnected; physical controller traversal remains pending.

## 2026-10-04 keep controller status readable above the HONOR Dock

- [x] The current physical 1920×1080 HDMI capture showed the persistent HONOR Dock clipping the second line of the wide controller footer's “Local player / Not connected” block. Changed the wide footer to one horizontal player/status row, preserving its controller-status accessibility description, and corrected the stale layout comment that assumed HONOR hides its Dock in fullscreen mode. No extra TV-safe-area inset was introduced.
- [x] Added a Compose layout assertion that profile and controller state share the compact footer row. Verification: `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `lintDebug`, and `assembleDebug` passed. Installed the build on the Honor and recaptured the actual HDMI panel; both “Local player” and “Not connected” now render on a single visible line above the Dock.
- [ ] The HONOR Dock itself remains visible as an OEM overlay. The paired Bluetooth “Wireless Controller” is still disconnected; D-pad/A/B acceptance awaits reconnection.

## 2026-10-04 full game names on wide Home cards

- [x] The fresh physical-HDMI Home capture showed “Yu-Gi-Oh! Forbidden Memories” ellipsized in both the collection poster and recently-played landscape card. Wide poster and landscape cards now allow a third title line where their dimensions can support it; narrow cards retain two lines and wide landscape card text is slightly reduced only in the 170–250dp width band.
- [x] Added unit coverage for the width-based line-count policy. Verification: 769 JVM tests pass, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly passed. Installed on the Honor and reviewed a direct 1920×1080 HDMI capture: the full game title now appears in both Home rows without a TV safe-area inset.
- [ ] Recheck long-name readability with the physical controller when it reconnects. The paired “Wireless Controller” remains Bluetooth-disconnected, so D-pad/A/B operation is still unverified.

## 2026-10-04 current Library layout on physical HDMI

- [x] Revalidated the installed title-wrap build on the Honor's physical trusted HDMI panel (1920×1080/60Hz, logical display 314; screenshot captured by SurfaceFlinger HWC display 1). The Library tab shows full “Yu-Gi-Oh! Forbidden Memories” titles in both Recently Played and All Games, with four-across cards, left filter rail, search, storage/overview rail, and controller legend all fitting in the TV viewport.
- [x] Current HDMI runtime still reports the paired “Wireless Controller” disconnected; the footer correctly displays “Not connected.” No emulator or phone-panel capture was used.
- [ ] Confirm D-pad focus movement, filter navigation, and card activation with the physical controller after reconnecting it.

## 2026-10-04 compact platform badges on Vimm results

- [x] A live 1920×1080 HDMI capture showed long source platform names wrapping vertically over Vimm box art in every result card. Replaced them with the Store's existing compact console labels (GBA, GBC, PS2, etc.) in a single-line, high-contrast badge; unknown platform names retain a one-line fallback.
- [x] Added unit coverage for compact labels and aliases. Verification: 770 JVM tests, 0 failures, 1 skipped; debug APK assembly succeeded. Installed the APK on the Honor Magic6 Pro and reran a read-only MARIO Vimm search on the physical HDMI display. The screen showed 65 results grouped by console, with the GBA cards' platform badge horizontal and legible; no game was opened or downloaded. Evidence: `tmp/gamebox-tv-vimm-compact-badges.png`.
- [ ] The physical Bluetooth controller is still disconnected, so gamepad focus/activation remains unverified; the HONOR Dock still overlays the TV's bottom edge.

## 2026-10-04 improve Vimm result-card readability on TV

- [x] On the physical HDMI screen, long Vimm titles were rendered directly over busy cover artwork, reducing legibility. Kept the artwork prominent in the upper part of each tile and moved the title, optional region/year, and details/import hint onto a high-contrast lower information panel. Compact platform badges remain at the top of the cover.
- [x] Verification: 770 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly passed. Installed on the Honor and reviewed the 65-result MARIO search on the actual 1920×1080/60Hz HDMI display; GBA result titles fit the lower panels and all visible cards retain distinct cover art. Evidence: `tmp/gamebox-tv-vimm-info-panels.png`.
- [x] Repeated a vertical scroll on HDMI and confirmed subsequent GBC, GameCube, and N64 shelves appear in order while the console rail and controller footer remain fixed. Evidence: `tmp/gamebox-tv-vimm-scroll-review.png`.
- [ ] Physical-controller navigation remains unverified. Current Bluetooth `ConnectionState` is `STATE_DISCONNECTED`; the earlier HID `Connected` message is a historical log entry, and Android's current joystick-capable input is the USB mouse/keyboard composite. HONOR's system Dock remains over the lower edge of the TV output.

## 2026-10-04 Vimm details action hierarchy and ambient hero

- [x] Removed the second Vimm `Download` button from the “Bring your game” import panel. The details hero now owns the single primary tracked Download action; manual `Import your copy` remains a separate fallback. Updated the source-backed hero description to explain both routes.
- [x] Applied a restrained 14dp blur to full-bleed detail-hero artwork, keeping the sharp box cover visible at the side. This makes low-resolution source screenshots read as ambient wallpaper rather than an oversized pixelated image.
- [x] Added an Android UI regression asserting one Download action alongside the manual-copy fallback. Verification: 770 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly passed. Installed and reviewed the Vimm Mario & Luigi detail page on the Honor's physical 1920×1080 HDMI screen without starting a download. Evidence: `tmp/gamebox-tv-vimm-detail-final.png`.
- [ ] The live Vimm result's detail metadata still returned “Source listing unavailable”; the tracked Download and manual-copy fallback remain visible. Verify the legitimate source-page handoff separately without initiating a copyrighted download. Physical controller remains disconnected and the HONOR Dock overlays the lower TV edge.

## 2026-10-04 clarify partial Vimm metadata failure

- [x] Reworded the detail-page warning to distinguish a successful search result from a failed enrichment request. It now says the result was found, identifies only the extra details as unavailable, and points to the existing Download and copy-import recovery actions instead of suggesting that the whole listing disappeared.
- [x] Updated the message regression. Verification: 770 JVM tests, 0 failures, 1 skipped; Android UI-test compilation, lint, and debug APK assembly passed.
- [x] Read-only check from the connected Honor to the previously referenced Vimm page `https://vimm.net/vault/23719` returned HTTP 404. This is evidence that this specific vault ID is unavailable now, not evidence of a redirect-handling or general metadata-parser failure; no game/download endpoint was opened.
- [ ] Reproduce detail hydration with a currently valid result ID before changing Vimm parsing. Latest display inventory shows only the Honor built-in panel and Studio virtual sharing display; physical HDMI is absent, so TV visual verification remains pending.

## 2026-10-04 animated controller focus ring

- [x] The shared Blueprint action modifier now fades its gradient focus ring over 140ms instead of snapping it on/off. The animation follows the existing reduced-motion preference (instant when enabled), while preserving the existing scale/press response and gradient focus colors.
- [x] Verification: 770 JVM tests pass, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass.
- [ ] Review focus-ring clarity and bounds on the physical 1920×1080 TV output, and verify with the Bluetooth controller after HDMI and controller are connected. The live display inventory still shows no HDMI panel, so no phone/virtual screenshot was used as a substitute.
- [ ] HDMI is currently absent from Android's display inventory, so this copy adjustment has not yet been reviewed on the TV. Continue physical-HDMI validation after projection reconnects; do not use the phone panel or Studio virtual display as substitute.

## 2026-10-04 PC and Media hub couch-distance labels

- [x] Raised the shortcut readiness labels and Moonlight hero eyebrow from 8sp to 10sp, hero supporting copy from 10sp to 12sp, and its call-to-action from 11sp to 12sp. This improves living-room readability without changing the tile/hero geometry or hierarchy.
- [x] Strengthened the shortcut-tile layout regression to require a minimum rendered status-label height while still checking the normal and enlarged-font layouts. Verification: 770 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass.
- [ ] Review the PC/Media hub at physical 1920×1080 and couch distance. HDMI remains absent from the live Android display inventory, so no phone or virtual-display visual review was substituted.

## 2026-10-04 Store rail couch-distance labels

- [x] Increased the Store console/filter rail's “FILTERS” heading and offline-catalog hint from 9sp to 11sp, keeping the heading's hierarchy distinct. Added a UI-test assertion that the offline hint remains reachable by scrolling the rail.
- [x] Verification: 770 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass.
- [ ] Review rail density and couch-distance readability on the physical TV and validate controller scrolling there. Current Android display inventory still shows only the built-in Honor display and Studio's virtual sharing display; the paired controller is not exposed as an input device.

## 2026-10-04 game-detail installation panel readability

- [x] Raised the wide game-detail hero's Installation heading and status/game-size rows from 9–10sp to 12sp; Play/Install labels are 12sp and the fallback instruction is 11sp. Kept the existing panel dimensions and primary focus/action path unchanged.
- [x] Verification: 770 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass.
- [ ] Check the taller text and action fit in the live game-details hero at TV resolution. The current Honor still exposes no physical HDMI display, so runtime visual acceptance remains pending.

## 2026-10-04 Vimm result-card supporting text

- [x] Raised the Vimm result-card platform badge, edition (region/year), and details/import hint from 9sp to 10sp while preserving the title-first hierarchy and current shelf dimensions.
- [x] Added a compact 156×132dp card UI regression for console label, game title, region/year, and the details/import hint. Verification: 770 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass. Instrumentation was not run on the phone in lieu of the requested TV test.
- [ ] Run the compact-card layout regression and review the actual Vimm shelf on physical 1920×1080 HDMI. Current display inventory still has no physical external display.

## 2026-10-04 filled-action contrast

- [x] Theme audit measured white labels on the original primary blue (`#5184FF`) at 3.45:1. Preserved that vivid color for focus/text accents and introduced a deeper filled-action blue (`#3267F2`) for Blueprint primary buttons, giving normal-size white button text approximately 4.82:1 contrast.
- [x] Added a contrast-ratio unit regression for the filled action color. Verification: 771 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass.
- [ ] Review the adjusted filled-button shade on the physical TV alongside the existing blue focus ring to ensure visual hierarchy remains clear. HDMI is still absent; no virtual/phone display is substituted.

## 2026-10-04 Home and top-bar secondary labels

- [x] Increased the Home quick-launch “PC Hub” subtitle and the GameBox OS/current-tab breadcrumb in the wide top bar from 9sp to 11sp. Main navigation labels, tile geometry, and focus behavior remain unchanged.
- [x] Verification: 771 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass. The existing quick-launch UI regression compiled but was not run against the phone screen.
- [ ] Review the breadcrumb width and quick-launch subtitle at physical TV resolution/couch distance. HDMI is absent from the latest Honor display inventory.

## 2026-10-04 discovery action guidance readability

- [x] Raised the “Import your copy”/release-year subtitle on discoverable game cards and the “VIMM LAIR” eyebrow in the game-details discovery card to 11sp so the next-step/discovery route is legible at living-room distance.
- [x] Strengthened the discovery-card controller test to require the import guidance to be displayed. Verification: 771 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass.
- [ ] Review the discovery cards on physical HDMI and run the controller-A navigation sequence on the TV. Honor still exposes no HDMI display or connected controller input.

## 2026-10-05 Vimm result-card controller focus

- [x] Vimm source-result cards now scale gently on focus/hover and animate their border width in line with the shared reduced-motion preference. Focus uses the blue-violet-magenta GameBox ring instead of a flat primary-blue border.
- [x] Added an Android UI regression that requests focus on a Vimm result card and activates it with controller A. Verification: 771 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass. Instrumentation was not run on the phone.
- [ ] Validate the animated focus ring and A-button activation on the physical TV. Latest ADB inventory shows only the Honor panel and Studio virtual display; input inventory has no separate Bluetooth gamepad (only the USB mouse/keyboard composite).

## 2026-10-05 consistent discovery-card focus motion

- [x] The regular catalog-discovery game cards now animate their gradient focus-border width over 120ms, matching Vimm result cards and honoring reduced motion. Their existing press/focus scale and controller-A activation are preserved.
- [x] Verification: 771 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass.
- [ ] Review discovery-row focus movement on physical HDMI with the paired controller. Fresh ADB still reports no HDMI output or distinct gamepad input.

## 2026-10-05 Home PC/Media shortcut focus polish

- [x] PC/Media shortcut tiles now animate the focused/hovered gradient ring width and elevation with the existing focus scale; reduced-motion mode remains instantaneous. The previously abrupt flat accent border is gone.
- [x] Added a controller-A regression for a focused Steam shortcut tile. Verification: 771 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass. No on-device instrumentation was run.
- [ ] Validate the shortcut focus/elevation on the physical TV with the controller. Latest HONOR state has no HDMI display or separately enumerated gamepad.

## 2026-10-05 PC/Media hub card focus motion

- [x] Larger PC/Media hub `ShortcutCard`s now use an animated 1–3dp gradient focus border and animate their elevation with the existing spring scale. Reduced-motion mode keeps these state changes immediate.
- [x] Added a controller-A regression for focusing and activating a Media-hub shortcut. Verification: 771 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass. Instrumentation was not run on the phone.
- [ ] SurfaceFlinger currently reports only the Honor's internal HWC display plus Studio's virtual share; no physical HDMI sink or Bluetooth gamepad appears. Review the new card treatment on the real TV after reconnection.

## 2026-10-05 cleaner ambient artwork fallback

- [x] The saved 1920×1080 TV Home capture showed fallback initials duplicated at full-bleed scale behind the same game's cover rail. Added an ambient-art mode that keeps the branded abstract gradients but suppresses oversized monograms; cover/poster artwork continues to show monograms when provider art is missing. Applied it to library-card backdrops, game-detail hero backdrops, and the Store featured hero to better match the blueprint's restrained ambient surfaces.
- [x] Added an Android UI regression proving ambient fallbacks omit the monogram while cover fallbacks retain it.
- [x] Strengthened the UI regression at the full wide-card composition: when both ambient and cover treatments use the same missing-art fallback, exactly one monogram remains on the cover rail.
- [ ] Verify the updated Home, game-detail, and Store hero visuals on the physical TV. Current ADB display inventory exposes only the Honor built-in panel and Android Studio's virtual sharing display, so no phone or virtual-display visual check is substituted.

## 2026-10-05 Home system-status couch readability

- [x] Raised the four compact Home system-status card labels from 10/12sp to 11/13sp and values from 11/14sp to 12/15sp. The cards keep their existing dimensions, truncation behavior, and status hierarchy while gaining a small but measurable TV-distance readability improvement.
- [x] Added an Android UI regression that measures both rendered line heights against the new minimums. Instrumentation compile and lint pass; physical 1920×1080 TV review remains pending because the current Android display inventory exposes no HDMI sink.

## 2026-10-05 Home connection-state indicators

- [x] Added small green/amber state pips for known connected/offline network and controller states, keeping unknown/loading labels neutral. Reserved indicator space in each card's value row so long controller names ellipsize before the indicator rather than pushing it outside the fixed TV sidebar.
- [x] Added JVM policy coverage and an Android layout regression for long controller labels, status-pip bounds, and readable text. Physical HDMI visual review remains pending; ADB currently lists only the Honor internal display and Studio virtual sharing display.
- [x] The network/controller pips now smoothly transition between healthy and attention colors, snapping immediately when the user's reduced-motion option is enabled.
- [x] Tightened the network-tone matcher so phrases such as “Not connected” and “Disconnected” can never be mistaken for a healthy state merely because they contain the word “connected”; regression coverage includes both negative forms.
- [x] Re-ran verification after the matcher fix: 773 JVM tests, 0 failures, 1 skipped; Android UI-test compilation, lint, and APK assembly all pass. Refreshed the in-place Honor install with app data retained and left GameBox unlaunched at the launcher.
- [x] Verification: 773 JVM tests, 0 failures, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass. Installed the debug APK in place on the Honor with app data retained and left GameBox unlaunched.
- [ ] Current post-install ADB state still has only HWC port 131 (the Honor internal panel) and Studio's private virtual display; no physical TV or separate gamepad is enumerated. Do not claim TV visual or controller acceptance until both appear.

## 2026-10-05 Library resume readiness

- [x] Unified the Library's locally playable-state rule and corrected “Ready to resume” so queued or missing-file titles cannot outrank an installed/update-available game in the summary. The wide resume shelf now also requires both a retained save and locally playable game content; recent-play history and retained recovery entries remain available in their own sections.
- [x] Added JVM coverage for queued/missing games not being selected for resume, updated the Android summary-card regression to expect the installed title, and added a wide-Library regression so missing game files cannot leave behind an empty “Ready to Resume” shelf.
- [x] Verification: 774 JVM tests, 0 failures, 0 errors, 1 skipped; Android UI-test Kotlin compilation, lint, and debug APK assembly pass. `git diff --check` passes for the changed code/tests. Instrumentation remains compiled, not run, because the physical TV output is absent.
- [ ] Visually review the Library and resume shelf on the physical TV with the Honor docked. Fresh ADB inventory shows only the Honor internal panel and Studio's private virtual display; do not substitute either for TV review.

## 2026-10-05 game-details couch readability

- [x] Increased secondary game-details action labels from 10sp to 12sp, metadata metrics from 12sp to 13sp, and wide hero status/install/play labels from 12sp to 13–14sp. This raises living-room readability while preserving the primary Play/Install action hierarchy and FlowRow navigation.
- [x] Added a rendered-height regression requiring the wide hero “Play Now” text to be at least 16dp tall. Verification: 774 JVM tests, 0 failures, 0 errors, 1 skipped; Android UI-test compilation, lint, and debug APK assembly pass.
- [ ] Review details typography and focus sequence on the physical TV with the paired controller. ADB currently sees only the Honor's internal panel and Studio's private virtual display; the physical TV/gamepad pass remains outstanding.

## 2026-10-05 Download queue scan hierarchy

- [x] Transfer rows now constrain long game titles to two lines and keep state visible in a compact text-and-indicator pill. Status tone distinguishes completed, failed/cancelled, paused, and in-progress states while the status word remains present so color is not the only signal.
- [x] Extended the controller-resume regression with a long title and explicit visibility checks for both the game title and paused state. Status tone now eases over 180ms on transfer-state changes and snaps immediately under reduced motion.
- [x] Audited existing interaction feedback: UI click sounds are user-toggleable, and D-pad directional navigation maps to Android navigation sounds. Verification: 774 JVM tests, 0 failures, 1 skipped; Android UI-test compilation, lint, APK assembly, and `git diff --check` pass.
- [ ] Review long titles, state-pill spacing, and focus on the physical TV. ADB still exposes only the Honor built-in panel and Studio virtual display.

## 2026-10-05 external-display system-overlay avoidance

- [x] The external-display content root now reserves the bottom navigation/taskbar inset reported by `WindowInsetsCompat.getInsetsIgnoringVisibility`, while the full-bleed backdrop still covers the complete surface. Phone layout and the user's separate TV overscan safe-area setting are unchanged. A fresh inset dispatch is requested after immersive-bar changes.
- [x] Added Compose layout coverage with a synthetic bottom inset and conversion checks for external-vs-phone surfaces, density scaling, invalid density, and nonpositive inset values.
- [x] Verification: 776 JVM tests, 0 failures, 0 errors, 1 skipped; Android UI-test compilation, lint, debug APK assembly, and `git diff --check` pass.
- [ ] Verify the Honor desktop dock's live inset and controller-footer/profile placement on the physical TV. ADB currently reports only the phone panel and Studio's private virtual display. This padding avoids overlap when reported; it does not claim to hide HONOR's system-owned dock or satisfy the OS-level taskbar requirement.

## 2026-10-05 controller footer couch readability

- [x] Increased the controller-hint glyphs from 20dp/10sp to 22dp/12sp, hint labels from 12sp to 13sp, legend labels from 12sp to 13sp, and detected-controller status from 11sp to 12sp; the wide footer minimum height is now 40dp.
- [x] Added wide-footer regressions for minimum rendered height of the A glyph, Select hint, and View/Status legend, alongside the existing fit/no-overlap assertion. Verification: 776 JVM tests, 0 failures, 1 skipped; Android UI-test compilation, lint, APK assembly, and `git diff --check` pass.
- [ ] Review footer density and overlap against HONOR's live system dock on the physical TV, then verify the hints against the connected controller. The current live inventory still shows no TV display or gamepad.

## 2026-10-05 wide navigation readability

- [x] Raised wide/compact header clocks from 13sp to 14sp, wide date from 10sp to 12sp, and primary tab labels from 12sp to 13sp. The wide header has ample horizontal room, so the increase keeps its existing tab layout and interaction model.
- [x] Added a wide-header rendered-height regression for the date and Home tab label. Verification: 776 JVM tests, 0 failures, 1 skipped; Android UI-test compilation, lint, APK assembly, and `git diff --check` pass.
- [ ] Confirm the date/time hierarchy and tab spacing on the actual TV at couch distance; ADB still exposes no physical external panel or gamepad.

## 2026-10-05 Settings controller-focus clarity

- [x] The wide Settings rail now gives the D-pad-focused row a distinct animated
  blue-violet outline and stronger fill even before selection; icon emphasis
  follows focus, hover, or selection. Settings action-row fill and outline-width
  transitions now animate consistently as well.
- [x] Both treatments honor Reduce motion by snapping their color and width
  changes. Existing section-navigation UI coverage verifies that a non-selected
  Settings row can receive focus and then be selected. Verification: 776 JVM
  tests, 0 failures, 0 errors, 1 skipped; Android UI-test Kotlin compilation,
  lint, debug APK assembly, and `git diff --check` pass.
- [ ] Review the focus contrast and motion on the physical TV with the Bluetooth
  controller. The Honor is connected to ADB, but current SurfaceFlinger inventory
  shows only its internal display and Studio's virtual display; input inventory
  shows a USB mouse/keyboard composite, not the gamepad.

## 2026-10-05 Settings rail couch readability

- [x] Increased wide Settings rail section labels from 11sp to 13sp and the
  SETTINGS eyebrow from 10sp to 12sp. The rail remains vertically scrollable,
  so the stronger type hierarchy does not depend on fitting every category in
  the available height.
- [x] Added Android UI layout assertions for the rendered heading and category
  label heights. Verification: 776 JVM tests, 0 failures, 0 errors, 1 skipped;
  Android UI-test Kotlin compilation, lint, debug APK assembly, and
  `git diff --check` pass. Instrumentation was not run because the physical TV
  display is absent, and phone-screen testing is not a substitute for it.
- [ ] Confirm the rail's font scale and scroll affordance on the real TV at
  couch distance.

## 2026-10-05 Store and Library filter-rail clarity

- [x] Increased shared console/library rail labels to 12sp, counts to 10sp,
  and row padding slightly; long platform names now ellipsize rather than
  clipping. Store filter headings/actions and metadata controls now use 12sp,
  and the explicit sort/clear targets grew from 30dp to 36dp. Library filter,
  sort, and offline-status labels were brought to the same scale.
- [x] Added a rendered-height assertion for the Store console label and section
  heading. Verification: 776 JVM tests, 0 failures, 0 errors, 1 skipped;
  Android UI-test Kotlin compilation, lint, debug APK assembly, and
  `git diff --check` pass. UI instrumentation remains unrun because HDMI is not
  enumerated; no phone-screen result is being used as a substitute.
- [ ] Validate truncation, filter-rail scrolling, and couch-distance readability
  on the physical TV with the controller.

## 2026-10-05 PC and Media shortcut status readability

- [x] Increased compact PC/Media shortcut titles to 13sp and readiness labels
  to 12sp. The wider Media cards now use the same 12sp readiness hierarchy,
  and the PC hero's section eyebrow is 12sp. This makes the most actionable
  state (“Ready” / “Setup required”) legible without relying on color alone.
- [x] Extended the compact and wide tile layout regressions to check rendered
  title and readiness-label heights. Verification: 776 JVM tests, 0 failures,
  0 errors, 1 skipped; Android UI-test Kotlin compilation, lint, debug APK
  assembly, and `git diff --check` pass. Instrumentation remains unrun pending
  the physical TV output.
- [ ] Review title/state balance and card clipping on the actual TV with the
  controller attached.

## 2026-10-05 Header search routing

- [x] The wide header's magnifying-glass action now uses the same Store search
  focus request as Home and game-details search paths. On controller-first
  layouts, this opens the on-screen keyboard; on text-entry layouts, it moves
  focus to the search field. Store navigation remains the destination.
- [x] Extended the controller-A header-search regression to assert the search
  request callback, not just a destination change. Verification: 776 JVM tests,
  0 failures, 0 errors, 1 skipped; Android UI-test Kotlin compilation, lint,
  debug APK assembly, and `git diff --check` pass.
- [ ] Verify search focus and keyboard visibility on the Honor/TV using its
  physical controller. The latest display/input inventory has no HDMI sink or
  gamepad registered.

## 2026-10-05 Store search focus priority

- [x] Prevented the delayed Store rail-autofocus action from stealing focus
  after an explicit search request. Search focus/keyboard now has priority over
  default focus when Store enters, and the search intent remains latched long
  enough for the next-frame rail focus check to respect it.
- [x] Added unit coverage for wide Store rail autofocus eligibility across
  search-requested, compact, and non-Store states. Verification: 777 JVM tests,
  0 failures, 0 errors, 1 skipped; Android UI-test Kotlin compilation, lint,
  debug APK assembly, and `git diff --check` pass.
- [ ] Verify the header → Store → focused search field/controller keyboard flow
  on the physical TV. Honor remains connected, but SurfaceFlinger lists only its
  built-in panel and Studio's virtual display; no gamepad is enumerated.

## 2026-10-05 Physical Honor TV fullscreen recovery

- [x] Reconnected to the actual Honor Magic6 Pro HDMI output: Android reports
  physical display 320 at 1920×1080 / 60 Hz. Relaunched GameBox on that display
  and verified `MainActivity` is resumed with full-display bounds.
- [x] Restored HONOR's visible per-app **Open in full screen mode** option from
  the Dock context menu. A fresh 1920×1080 capture shows the GameBox Home with
  the bottom Dock and transient desktop window controls absent. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-fullscreen-clean-home.png`.
- [x] Confirmed the two temporary UI-inspection XML files are absent from the
  phone. No user game files, app data, or global HONOR settings were changed.
- [x] Repeated a cold app-process test with `am force-stop` followed by a
  display-320 launch, without clearing GameBox data. The Dock returned even
  though GameBox's Activity still filled the 1920×1080 bounds. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-dock-after-cold-relaunch.png`.
- [x] Reapplied the supported per-app fullscreen menu action and verified the
  Dock disappeared again after transient window controls settled. GameBox was
  left on Home in that clean state. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-clean-after-manual-fullscreen.png`.
- [x] On the cold restart, the Android fullscreen request logged as accepted,
  but HONOR still displayed its Dock. After restoring the per-app mode, the
  final clean capture is preserved at
  `docs/evidence/hardware-run-2026-10-05/honor-tv-clean-after-cold-relaunch-restore.png`.
- [ ] Fullscreen does not survive this app cold restart on the current HONOR
  setup. The app's fullscreen request does not suppress the separate OEM Dock;
  investigate only supported HONOR controls/API options, and keep the manual
  per-app action as the recovery path meanwhile.
- [ ] The Home screenshot looks correct in this restored fullscreen state, but
  GameBox reports the controller as disconnected. Android lists the dock's
  `USB OPTICAL MOUSE Keyboard` HID with generic joystick source flags, but no
  separately identifiable gamepad input. HONOR Bluetooth lists a paired
  `Wireless Controller`, but its connection state is `STATE_DISCONNECTED`.
  Physical controller navigation and controller-only search remain unaccepted;
  injected gamepad events are not a substitute for that test.

## 2026-10-05 Library artwork recovery

- [x] Fixed exact-name thumbnail lookup for common combined-region releases.
  GameBox previously probed `(USA)` and `(Europe)` separately, missing valid
  archive names such as `Wario Land 4 (USA, Europe).png`. Added the combined
  `USA, Europe` variant to the existing shared Libretro thumbnail matcher; no
  new provider or bundled image was added. The official
  [Libretro GBA box-art index](https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy%20Advance/Named_Boxarts/)
  lists the matching cover.
- [x] Added a regression test for the GBA combined-region result. Verification:
  `testDebugUnitTest` passed (778 tests, 0 failures, 0 errors, 1 skipped),
  `lintDebug` passed, and `assembleDebug` passed.
- [x] Updated-installed the APK on the Honor without clearing app data. After
  startup enrichment, Wario Land 4's real cover appeared in both Home shelves
  and Library's Recently Played/All Games rows on the physical 1920×1080 HDMI
  display. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-home-artwork-fixed.png` and
  `docs/evidence/hardware-run-2026-10-05/honor-tv-library-artwork-fixed.png`.
- [x] Reapplied HONOR per-app fullscreen after the APK update, waited for its
  transient title controls to disappear, and left GameBox on Home with the Dock
  hidden. No user game/save files or global HONOR settings were changed.
- [ ] Audit the remaining placeholder covers by title/source; this exact-name
  correction fixes combined-region archive releases, not artwork absent from
  the provider or titles with different canonical naming.

## 2026-10-05 Physical TV Vimm results and emulator window check

- [x] Revalidated on the Honor's physical HDMI display (1920×1080 at 60 Hz).
  Searching `WARIO LAND 4` with the Game Boy Advance console selected returned
  two Vimm entries under a dedicated **Results by console → Game Boy Advance**
  row. Both cards displayed the expected Wario Land 4 cover art. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-vimm-results-by-console.png`.
- [x] Opened a result's details and verified the source artwork and imported
  state. The screen reported that Vimm's extra details could not be loaded; no
  new download was started. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-vimm-result-details.png`.
- [x] Verified Azahar's Android all-files app-op is `allow`. A direct launch on
  the TV initially appeared in HONOR's floating desktop window with the Dock
  visible; switching Azahar to full-screen hid the Dock. The transient window
  controls remain until dismissed. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-azahar-maximized-window.png`
  and `docs/evidence/hardware-run-2026-10-05/honor-tv-azahar-fullscreen.png`.
- [x] Tested the real GameBox → Dolphin launch with the imported Luigi's
  Mansion entry. Dolphin reached the game's intro/title sequence on the HDMI
  display. The initial launch showed HONOR's desktop window controls and Dock;
  choosing Dolphin's full-screen window action hid them after the transient
  toolbar timed out. Gameplay was pillarboxed correctly for 4:3 and had no
  touch-control overlay. A second GameBox launch reproduced the windowed
  startup, so fullscreen currently needs a manual per-session action. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-luigi-mansion-windowed-launch.png`
  and
  `docs/evidence/hardware-run-2026-10-05/honor-tv-luigi-mansion-dolphin-fullscreen.png`.
- [x] Repeated that A/B check on the physical HDMI output. GameBox's own
  per-app **Open in full screen mode** hides the Dock for the launcher but does
  not carry over to Dolphin's child `EmulationActivity`: a fresh launch again
  showed HONOR's titlebar and Dock. Tapping the emulator window's fullscreen
  control removed both; the next fresh launch returned to windowed mode. The
  game stayed correctly pillarboxed with no touch overlay. Android reports the
  containing task as HONOR's proprietary `hw-pc-freeform` mode even when its
  bounds are 1920×1080, so GameBox's Android fullscreen hint alone is not proof
  of a borderless TV surface. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-luigi-mansion-windowed-repeat.png`
  and
  `docs/evidence/hardware-run-2026-10-05/honor-tv-luigi-mansion-manual-fullscreen-repeat.png`.
- [x] Inspected Dolphin's GameCube Controller 1 profile. A Standard Controller
  profile is selected and has button/axis bindings under
  `Android/1/Wireless Controller Touchpad`; no settings were changed. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-dolphin-controller-map-upper.png`
  and `docs/evidence/hardware-run-2026-10-05/honor-tv-dolphin-controller-map-lower.png`.
- [ ] Investigate why Vimm's result-details hydration fails on the live device;
  the result row and fallback artwork work, but richer source metadata is still
  unverified. Do not download copyrighted content as part of this UI check.
- [ ] Make emulator launches reliably enter HONOR full-screen without requiring
  the user to press the window's full-screen control; Dolphin's GameBox launch
  still initially exposed the Dock. Android's public `requestFullscreenMode()`
  API can only change the calling/top activity; GameBox cannot call it on
  Dolphin's activity. The supported ActivityOptions fullscreen hint also did
  not change HONOR's `hw-pc-freeform` task mode. Continue looking for a supported
  HONOR handoff or emulator integration; do not claim automated fullscreen
  until a fresh launch proves it. Verify per-app behavior on each standalone
  emulator.
- [ ] Reconnect the physical controller and verify GameBox navigation plus
  emulator mappings. Android currently reports the paired `Wireless
  Controller` as `STATE_DISCONNECTED`; touchscreen-injected taps were used only
  for this UI inspection.
- [x] Restored GameBox's HONOR per-app **Open in full screen mode** after the
  emulator tests and left the actual TV on Home with the Dock hidden. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-home-after-emulator-test.png`.
- [x] After the repeated Dolphin launch check, returned to GameBox Home on the
  real 1920×1080 HDMI panel. The HONOR Dock is hidden; game artwork, Continue
  Playing, collection, Quick launch, Vimm entry, and controller status are
  visible in the launcher layout. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-home-after-rerun.png`.
- [x] Reviewed Home, Library, Store, Media, PC, and Settings on the unlocked
  physical TV after reconnecting ADB. The Library's offline count wrapped
  below the rail's initial viewport, so it is now a single aligned label/count
  row. The updated APK was installed with app data preserved; the row is fully
  visible on the first Library screen. PC connection controls are reachable by
  scrolling its content. Verification: 778 JVM tests passed (1 skipped),
  Android-test Kotlin compiled, APK assembled, and lint passed. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-library-layout-after-fix.png`.
- [x] Fullscreen Home on the current Honor HDMI run now shows the complete
  Recently played cards above the controller footer without scrolling. Wide
  PC Hub now puts Connection setup before Recent sessions; the host/IP field,
  port, and Check and save action are visible in the initial TV viewport.
  Verification: 778 JVM tests passed (1 skipped), Android-test Kotlin compiled,
  APK assembled, lint passed, and the updated APK was installed on the Honor.
  Evidence: `docs/evidence/hardware-run-2026-10-05/honor-tv-home-recent-row-fit.png`
  and `docs/evidence/hardware-run-2026-10-05/honor-tv-pc-setup-first-view.png`.
  The paired controller remains disconnected; no physical-input acceptance is
  claimed.
- [x] Supporting focus-path check on the same physical HDMI display: Android's
  injected `KEYCODE_DPAD_RIGHT` moved Home focus from the hero to the controller
  status card, and `KEYCODE_DPAD_LEFT` returned it to the hero. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-home-dpad-focus-injected.png`.
  This exercises the app's key-event/focus path only; the paired Bluetooth
  controller was still absent from Android input and its physical D-pad/A/B
  behavior remains unverified.
- [x] Opened Luigi's Mansion from the Home hero on the physical HDMI TV and
  inspected its installed-state details layout, Play Now action, Vimm Download
  panel, and screenshot gallery; returned to Home without launching the game.
  Evidence: `docs/evidence/hardware-run-2026-10-05/honor-tv-luigi-details-flow.png`.
  This was a visual/touch route check, not physical-controller acceptance.
- [x] On the physical TV, injected D-pad Right then Down moved Library focus
  from the Crash card to Wario Land 4 in Recently Played and then the matching
  All Games row. Injected A opened details; injected B dismissed the enlarged
  screenshot modal. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-library-a-opens-details-injected.png`
  and `docs/evidence/hardware-run-2026-10-05/honor-tv-details-screenshot-modal.png`.
  This verifies Android key dispatch and focus behavior only; the paired
  Bluetooth gamepad is still not enumerated, so physical controller acceptance
  remains outstanding.
- [x] On the physical TV, injected Android gamepad X opened Store's controller
  search keyboard, D-pad Right plus A inserted `W`, and results filtered to
  four matching titles. B closed the keyboard; the test query was then cleared
  via the on-screen keyboard and Store restored to all 12 titles. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-search-keyboard-injected.png`
  and `docs/evidence/hardware-run-2026-10-05/honor-tv-store-search-reset.png`.
  This validates the key-event path on the Honor display, not physical-gamepad
  input; the controller remains disconnected.
- [x] Rechecked Home, Library, Store, Media, PC, and Settings on the physical
  1920×1080 HDMI output. Primary controls remain above the GameBox footer:
  Home's hero/collection/quick launch, Library's recent/all-game rails, Store's
  source search, Media's app rail, and PC's host/port/save controls. Lower PC
  session content and Settings sections continue below the fold as scrollable
  content; Media's horizontally scrolling app rail exposes the next card edge.
  Captures: `docs/evidence/hardware-run-2026-10-05/honor-tv-ui-review-home.png`,
  `honor-tv-ui-review-library.png`, `honor-tv-ui-review-store.png`,
  `honor-tv-ui-review-media.png`, `honor-tv-ui-review-pc.png`, and
  `honor-tv-ui-review-settings.png`.
- [ ] The fresh process launch showed HONOR's system Dock over the GameBox
  footer even though GameBox requested fullscreen. Restoring HONOR's per-app
  **Open in full screen mode** removed it and returned the complete footer;
  this confirms the cold-start/manual-action limitation remains. GameBox cannot
  hide the OEM-owned Dock through its own window policy. The paired physical
  controller is still absent from Android input, so controller acceptance is
  open.
- [x] Inspected Luigi's Mansion's installed-game details on the actual HDMI TV:
  the full-width art hero, installation/status panel, Play Now action, Vimm
  Download entry, screenshot preview, and controller hints all fit on screen.
  Pressing Android's injected gamepad-B key returned to Home without launching
  the game. Evidence: `docs/evidence/hardware-run-2026-10-05/honor-tv-ui-detail-panel-review.png`.
  This tests the app key-dispatch path, not a physical controller.
- [x] Inspected Download Manager on the physical TV. The empty queue clearly
  explains that transfers continue in the background and verified files join
  Library, with Browse Store visible above the footer. Completed imports and
  storage/network status are readable in the Completed view. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-download-manager-empty-queue-review.png`
  and `docs/evidence/hardware-run-2026-10-05/honor-tv-download-manager-completed-review.png`.
  The queue was empty during this visual check; no transfer was started.
- [x] Validated GameBox's Quick Actions controller path on the HDMI display by
  sending gamepad-source Android events specifically to display 320: Start opens
  the overlay, D-pad moves focus to Downloads, A opens Download Manager, and B
  returns Home. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-gamepad-quick-actions.png`,
  `honor-tv-gamepad-quick-actions-focus.png`, and
  `honor-tv-gamepad-quick-actions-downloads.png`. These are display-targeted
  simulated inputs, not physical controller acceptance; Android still reports
  no connected gamepad. Temporary `/sdcard/tv-*.png` captures from this run
  were removed after saving project-side evidence.
- [x] Opened System Status with the display-targeted gamepad View/Select event
  and verified storage, network, controller, and device status are readable in
  the centered overlay; B closed it and returned to the launcher. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-system-status-overlay-review.png`.
  Input was simulated, and the overlay correctly reports the physical gamepad
  as not connected.
- [x] Tapped Home's Controller status card on the physical TV and verified it
  opens Settings directly at Controllers. The section clearly reports zero
  connected gamepads, explains pairing in Android, and exposes the Bluetooth
  controller settings action. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-controller-settings-review.png`.
- [ ] Continued the user-approved Azahar setup on the TV after confirming its
  all-files app-op is already allowed. Fullscreen hides the Dock after the
  transient window controls settle. Setup reaches a required user-data folder
  picker; Android disallows the storage root, so no folder was selected or
  created while its location is unconfirmed. No game or save files were
  touched. Await the user's folder choice before continuing to controller
  settings; return GameBox to Home in the meantime.
- [x] Retested Store search on the physical 1920×1080 HDMI display: the
  controller keyboard accepted the selected `W` after focus settled, the
  temporary `QW` query was cleared, all 12 Store titles returned, and Home was
  restored fullscreen with no HONOR Dock visible. Evidence:
  `docs/evidence/hardware-run-2026-10-05/honor-tv-search-reset-to-home.png`.
  Inputs were injected through Android, not a physical controller; GameBox
  reports the controller as not connected.
- [x] Moved Interface controls out of the Storage content and into the System
  group so the selected Storage section contains storage-related controls.
  Added a production-screen regression assertion. Added a dedicated Developer
  rail destination anchored to the diagnostics controls and extended the
  System → Developer → Display navigation checks. `assembleDebug`,
  `assembleDebugAndroidTest`, and all `testDebugUnitTest` tests passed; the
  instrumentation APK compiled but was not run on a phone/virtual display.
- [ ] Install and visually verify the Settings grouping on the physical HDMI
  display. The latest live check still exposes only the Honor panel and
  Studio's virtual display; no phone-screen or virtual-display substitution
  was used for acceptance.
- [x] Improved console-filter rail readability in Library and Store by showing
  the existing compact console labels (for example, GameCube, GBA, and GBC)
  while retaining full console names and item counts in accessibility labels.
  Added GBA/GBC mapping assertions and Store rail accessibility assertions.
  `assembleDebug`, `assembleDebugAndroidTest`, and `testDebugUnitTest` passed.
- [ ] Review the compact console rails and Settings grouping on the physical
  HDMI TV. Physical HDMI has since reconnected; review those specific screens
  remains pending (see the 2026-10-06 hardware entry below).
- [x] Added a Library Compose regression check that exercises the production
  rail with a Game Boy Advance title: “GBA” is visible and the complete console
  name plus count remains accessible. App build, instrumentation-test
  compilation, and JVM tests pass; the instrumentation test was not executed.
- [ ] Execute the updated Library/Store UI checks and inspect the new APK on the
  physical HDMI TV. HDMI is now available; automated checks and those specific
  screens remain unverified (see the 2026-10-06 hardware entry below).

### 2026-10-06 — Physical HDMI acceptance resumed

- [x] Reconfirmed the Honor's physical HDMI output is active at 1920×1080 and
  GameBox's `ExternalDisplayActivity` is rendering on that display. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-session-check.png`.
- [x] Restored a clean borderless TV surface for this session using HONOR's
  visible GameBox taskbar context-menu action “Open in full screen mode.” The
  Dock disappeared and the whole GameBox footer is visible in the 1920×1080
  capture. No Global Taskbar setting was changed. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-home-fullscreen-restored.png`.
  The OEM mode has previously reset on app relaunch/update, so persistence is
  still an open deployment gate. HONOR's separate Global Taskbar Settings
  search previously redirected to Account Center; do not use undocumented
  Settings-provider keys.
- [ ] Verify navigation with the physical Bluetooth controller. GameBox and
  Android currently report no connected gamepad. ADB D-pad injections did not
  establish physical-controller acceptance.
- [ ] Rebuild and run automated UI checks after the current UI changes. This
  check is now partly complete: cached Gradle 9.7.1 plus Android Studio's JBR
  ran `:app:testDebugUnitTest`, `:app:assembleDebug`, and
  `:app:assembleDebugAndroidTest` successfully. Instrumentation compiled but
  was not run; the temporary hardware-launch test ended with `Process crashed`
  and was removed rather than counted as a passing test.
- [x] Inspected Library and Store on physical HDMI. Scrolling Store exposed its
  catalog refresh action; refresh completed with “local install and play state
  preserved,” and Cave Story/Celeste Classic artwork appeared on their cards.
  Opened Cave Story details and verified the art-backed hero, install status,
  Vimm Download action, and metadata without starting a download. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-library-current.png`,
  `honor-tv-store-refresh-result.png`, and
  `honor-tv-store-cave-story-details.png`.
- [x] Installed the rebuilt `app-debug.apk` with replace mode (data preserved),
  relaunched GameBox from the HONOR app drawer on HDMI, and rechecked Home,
  Library, and Store on the latest build. Installed package update time is
  2026-10-06 19:02:54; Cave Story and Celeste Classic artwork remained visible.
  Evidence: `docs/evidence/hardware-run-2026-10-06/honor-tv-latest-install-launch.png`,
  `honor-tv-library-latest-build.png`, and `honor-tv-store-latest-build.png`.
- [x] Rechecked Home and Settings on the live 1920×1080 HDMI output. The Home
  dashboard shows Luigi's Mansion hero art, collection/recently-played cards,
  quick launch, and system status. Display-targeted DPAD_RIGHT focused the
  Controller status tile, A opened Settings → Controllers, and B returned to
  Home. This validates TV-rendered navigation with injected Android key events,
  not a physical controller. Evidence: `honor-tv-goal-continuation.png`,
  `honor-tv-focus-right.png`, `honor-tv-controller-status-open.png`, and
  `honor-tv-back-home-after-settings.png`.
- [x] Removed the HONOR Dock for the current session through the OEM's visible
  per-app full-screen action; the controller footer is now fully visible in
  `honor-tv-home-fullscreen-restored.png`. Cold-launch persistence remains open.
- [x] Reviewed Media and PC Hub on the active HDMI display. Media shows the
  app rail, installed-app cards, recent-app empty state, volume/network/controller
  status, and quick actions; the next partially visible app tile is the normal
  horizontally scrollable rail edge, not overlap from the status column. A
  display-targeted D-pad Down focused Plex, Right moved focus to Spotify, RB
  opened PC Hub, and B returned to Home. These are injected key events, not
  physical-controller acceptance. Evidence: `honor-tv-media-review.png`,
  `honor-tv-media-first-tile-focus.png`, `honor-tv-media-dpad-right.png`,
  `honor-tv-media-rb-test.png`, and `honor-tv-media-pc-back-home.png`.
- [x] Fixed Vimm/configured-source result cards with no box-art URL: they now
  render the existing title-aware gradient/monogram fallback instead of a
  blank poster. Added an instrumentation regression assertion for an artless
  result. `testDebugUnitTest`, `assembleDebug`, and
  `assembleDebugAndroidTest` passed. Installed the updated APK without clearing
  data and verified the fallback on the physical 1920×1080 HDMI TV in completed
  Vimm search results (10 titles; no download started). Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-vimm-results-artwork-fix.png`.
- [x] Reopened Luigi's Mansion details from the Home hero on the physical
  HDMI TV. The primary `Play Now` action received visible focus automatically;
  display-targeted D-pad Down also moved focus to that action. Did not activate
  it, since this title has a known Dolphin launch failure. This confirms the
  details focus path on this installed build; it does not verify a physical
  controller. Evidence: `docs/evidence/hardware-run-2026-10-06/resume-tv-focus-check.png`.
  Pressed B to return to Home after the check. Home evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-home-after-focus-check.png`.
  Later in this session, restored per-app fullscreen through HONOR's visible
  taskbar menu; the Dock is currently hidden, but relaunch persistence remains
  open. GameBox still reports the Bluetooth controller disconnected.
- [x] Reviewed the current Store on HDMI after restoring fullscreen: the
  console sidebar, featured Cave Story hero, Vimm search affordance, discovery
  empty state, imported-game card, and controller footer fit in the viewport.
  The current `CAVE` filter yields no discovery results while the imported
  Cave Story card remains visible. No download was started. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-store-review.png`.
  Returned to Home and confirmed the Dock remains hidden in this session:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-home-final-review.png`.
- [x] Revalidated the physical Honor after the previous turn: HDMI is still
  1920×1080, GameBox is on Home, and the Dock remains hidden without repeating
  the fullscreen action. Android's live `/dev/input/event*` list still contains
  no gamepad device, and GameBox shows “Not connected”; no physical-controller
  input is claimed. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-current-check.png`.
- [x] Marked the game-details hero title as a Compose accessibility heading and
  added a regression assertion in `GameDetailsHeroTest`; the app and
  instrumentation source compile. Updated the Honor in place without clearing
  data, opened Luigi's Mansion details on the physical 1920×1080 TV, verified
  the visible layout was unchanged, and returned to Home without activating
  Play Now. The assertion was not run on the phone display, and Android's
  UIAutomator XML dump does not expose Compose's heading property, so semantic
  runtime verification remains open. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-heading-build-details.png`,
  `honor-tv-heading-accessibility.xml`, and
  `honor-tv-heading-build-home-clean.png`.
- [x] The APK update reset HONOR per-app fullscreen, temporarily showing the
  Dock again. Reapplied the visible per-app fullscreen mode and restored the
  footer-clear 1920×1080 Home; no global taskbar setting changed. Cold-launch
  persistence remains unverified. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-heading-build-home-clean.png`.
- [x] Rechecked the live HDMI output at 1920×1080/60 and inspected Home,
  Luigi's Mansion Details, and Store on the latest installed build without
  launching the known-failing game or starting a download. Artwork and primary
  actions render correctly. The HONOR Dock is visible again and covers part of
  the controller footer; Android Settings search did not expose a safe
  per-app/global taskbar control, so no system setting was changed. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-hdmi-current.png`,
  `honor-hdmi-details-current.png`, and `honor-hdmi-store-current.png`.
  The physical controller is still absent, so controller acceptance remains
  open.
- [x] Fresh Honor/TV checkpoint: direct capture of the physical HDMI surface
  (1920×1080 at 60 Hz) shows GameBox Home with the HONOR Dock visible over the
  bottom edge, despite the app task being reported as fullscreen and the
  existing immersive `hideSystemBars` calls. The app-info page exposed no
  fullscreen switch, and long-pressing the visible Dolphin taskbar shortcut
  exposed only its pin action; no taskbar or app setting was changed. This live
  capture supersedes the earlier “Dock hidden” snapshot for current state.
  Evidence: `docs/evidence/hardware-run-2026-10-06/honor-hdmi-current-dock-visible.png`.
- [x] Checked Azahar 2126.1.2 on the Honor: `MANAGE_EXTERNAL_STORAGE` was
  already allowed. Its first-run wizard now requires choosing a User Folder;
  I left that selection untouched and did not open or change any ROMs/saves.
  Android's live input-device inventory currently has no gamepad, matching
  GameBox's “Not connected” status, so standalone-emulator button binding and
  physical-controller acceptance remain open until the Bluetooth controller
  is detected again.
- [x] Forced a fresh JVM test run: 778 tests, 0 failures, 0 errors, 1 skipped;
  `assembleDebug` and Android-test Kotlin compilation also succeed. `git
  diff --check` reports no whitespace errors (only existing LF/CRLF notices).
- [x] Installed the freshly rebuilt APK onto the Honor with `adb install -r`
  (no data clear). GameBox returned to Home on the physical TV with the same
  four installed games still present, but the Dock remained visible after
  installation and relaunch. Installed package reports versionCode 1,
  versionName 0.1.0. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-hdmi-latest-apk-dock-visible.png`.
- [x] Continued actual-HONOR testing after the Bluetooth controller reappeared.
  On the physical TV, controller B returned from Luigi's Mansion details to
  Library and RB switched Library to Store. Selected the built-in MIT-licensed
  Galaxy Patrol fixture with A; its details now report `installed`. Verified
  app-private content at `files/installed/retro/galaxy-patrol/content/` is
  49,168 bytes and its SHA-256 matches the bundled fixture; WorkManager reports
  the `AssetDownloadWorker` finished successfully. Download Manager Completed
  UI verification and gameplay launch remain pending. Current details screen
  is Galaxy Patrol; Azahar setup and Dolphin launch issues remain open. The
  HONOR Dock is still visible in the current external-display capture and
  fullscreen persistence remains unresolved.
- [x] Closed the authorized-homebrew tracking gap found in the HDMI flow:
  Galaxy Patrol now uses the shared persistent download history, restores its
  completed state from WorkManager after app restart, and records the fixture's
  exact byte count. Download Manager now shows Galaxy Patrol as Completed.
  Improved small-file formatting so the 49,168-byte game renders as `48.0 KB`
  instead of `0.0 MB`. Added lifecycle and formatter assertions. Build passed:
  779 JVM tests, 0 failures, 0 errors, 1 skipped; Android-test Kotlin
  compilation and `assembleDebug` also passed. Installed the APK in place on
  the Honor without clearing data. Captured the real HDMI 1920×1080 TV display:
  Completed count is 4 and Galaxy Patrol reads `48.0 KB of 48.0 KB`. Restored
  GameBox's per-app fullscreen setting from the TV taskbar; the Dock is now
  hidden. Current TV Download Manager evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-download-manager-galaxy-patrol-completed.png`.
  The current footer reports the controller disconnected after the APK update;
  physical B/RB navigation had worked earlier in this session. Gameplay launch
  and emulator readiness remain unverified; no existing user game was launched
  or removed.
- [x] Reconnected the Bluetooth controller and rechecked the actual HDMI
  surface after the APK update. Android exposes `Wireless Controller` as an
  enabled GAMEPAD/JOYSTICK on `/dev/input/event13`; GameBox's Download Manager
  footer now labels it `Wireless Controller` (instead of `Not connected`).
  The Honor HDMI output remains 1920×1080 and the per-app Dock is hidden. The
  completed Galaxy Patrol fixture still appears as `48.0 KB of 48.0 KB` in
  Download Manager. Evidence: `docs/evidence/hardware-run-2026-10-06/honor-tv-current.png`.
  A synthetic ADB D-pad event changed tab focus, but this is not counted as
  physical-controller acceptance. Follow-up physical test observed
  `ABS_HAT0X` transitions from `/dev/input/event13`; HDMI capture shows Home
  remains selected while focus is on the PC tab. Evidence:
  `docs/evidence/hardware-run-2026-10-06/honor-tv-physical-dpad-result.png`.
  User then pressed A, but that short event capture only retained analog-axis
  motion and the resulting Home/status-card capture does not prove tab
  activation. Physical D-pad focus movement is now verified; A activation and
  complete tab/game navigation remain open. No game was launched.
- [x] Fresh software validation on 6 October: forced the complete Android JVM
  test task (212 suites, 779 tests, 0 failures, 0 errors, 1 skipped), and
  `compileDebugAndroidTestKotlin` plus `assembleDebug` succeeded. The Windows
  companion console harness also passed with `dotnet run` on this PC. These are
  software checks only; they do not close physical-controller or gameplay
  acceptance. Subsequent HDMI testing observed a physical D-pad horizontal
  hat event and focus on PC while Home remained selected; activation still
  needs a clearly correlated physical button event.
- [x] Fixed an emulator-readiness gap for diagnostic/fixture titles whose
  catalog platform is generic but whose capability selects an emulator.
  Emulator settings now includes the explicit capability, and installed
  RetroArch variants share the launcher's existing package fallback policy.
  The updated APK was installed in place on Honor; the actual TV Game Settings
  panel reports RetroArch as installed. Full suite: 780 JVM tests, 0 failures,
  0 errors, 1 skipped; Android-test Kotlin compilation and APK assembly passed.
  Reapplied GameBox's per-app fullscreen option after install. The required
  FCEUmm core and real gameplay/save flow remain unverified; no game was
  launched. Evidence: `docs/evidence/hardware-run-2026-10-06/honor-tv-galaxy-emulator-settings.png`.
- [x] Cleared the blocking Android lint error in `GameDetailsHeroTest` by
  remembering its `FocusRequester` inside composition. `lintDebug` and
  `compileDebugAndroidTestKotlin` now pass; 20 existing lint warnings remain.

## 2026-10-09 — Vimm browser import observed on Honor HDMI

- [x] Fresh HDMI capture shows the previously active PSP browser transfer in
  Download Manager's **Completed browser downloads** section with **Added to
  Library**. The same page reports two completed browser imports. This verifies
  the user-initiated browser-download record and automatic import reached a
  visible completed state on Honor → TV. Evidence:
  `docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-import-completed-observed-2026-10-09.png`.
- [ ] Do not launch the commercial title to validate emulator support; use the
  redistributable Galaxy Patrol fixture for end-to-end gameplay checks.
- [ ] Physical gamepad navigation remains unverified: Android enumerates the
  Wireless Controller, but the passive input listener received no D-pad event
  during the observation window. The Downloads UI was not changed by test
  input. Repeat only when the user can press a physical control while the
  listener is actively attached.

## 2026-10-09 — Galaxy Patrol gameplay handoff on Honor HDMI

- [x] Installed the bundled MIT-attributed Galaxy Patrol fixture from its
  focused Store/Details action, then launched the focused **Play Now** CTA on
  the physical Honor HDMI display. Android resumed
  `com.retroarch/.browser.retroactivity.RetroActivityFuture`; the captured game
  image rendered `GAME OVER`, confirming the ROM/core activity ran. The paired
  controller's Linux input device emitted real D-pad hat and button events
  while RetroArch was foreground. This proves launch and input-device delivery,
  not button mapping/gameplay, because no specific game-state change was
  correlated to a particular physical button.
- [x] Set RetroArch to HONOR's per-app **Open in full screen mode**. The
  settled HDMI capture shows the game without the HONOR Dock or RetroArch touch
  layout. The taskbar menu later offered **Start in Windowed Mode**, confirming
  that per-app preference persisted. Evidence:
  `docs/evidence/hardware-run-2026-10-09/honor-tv-galaxy-patrol-fullscreen-no-overlay.png`.
- [x] Returned to the existing GameBox Galaxy Patrol details by using HONOR's
  normal RetroArch taskbar **Quit** item; GameBox was restored and RetroArch
  was no longer foreground. Evidence:
  `docs/evidence/hardware-run-2026-10-09/honor-tv-galaxy-patrol-returned-to-gamebox.png`.
- [ ] Android Back did not close RetroArch, and the user-confirmed physical B
  test on GameBox details was not received. Controller-only exit, button
  mapping, visible gameplay/input correlation, save state, uninstall/reinstall,
  and GameBox's own Dock-free cold launch remain open.

## 2026-10-09 — TV discovery-action focus recheck

- [x] On the external TV display, synthetic D-pad Center opened the already
  focused Galaxy Patrol details action; one synthetic D-pad Down moved visible
  focus from **Play Now** to the Vimm **Download** action, and D-pad Center
  opened Store with **Search Vimm's Lair** focused. No title or download was
  selected. Evidence:
  `docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-focus-synthetic-dpad.png`
  and
  `docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-search-entry-synthetic-a.png`.
- [x] Selecting the focused source entry opened GameBox's controller on-screen
  search keyboard, and synthetic B closed it back to Store. This validates the
  in-app text-entry/back route, not navigation or activation inside Vimm's
  webpage. Evidence:
  `docs/evidence/hardware-run-2026-10-09/honor-tv-search-keyboard-open-synthetic-a.png`
  and
  `docs/evidence/hardware-run-2026-10-09/honor-tv-search-keyboard-closed-synthetic-b.png`.
- [ ] This verifies GameBox's details-to-Store route only. Activation and
  controller focus inside the live Vimm WebView are still unverified; Android's
  current input inventory and GameBox footer both report the Bluetooth
  controller disconnected. Physical-controller acceptance must be rerun after
  it reconnects.
- [ ] The Honor instrumentation build required a one-worker, 3 GB Gradle heap
  after the default 2 GB build exhausted memory. Packaging then succeeded, but
  the test harness launched on the phone's default display rather than HDMI;
  the run was stopped and does not count as TV acceptance.

## 2026-10-10 — Synthetic gamepad Vimm search on Honor HDMI

- [x] Android's `input gamepad` source allowed a TV-display software-path check
  despite the physical controller being absent. GAMEPAD A opened Vimm search;
  D-pad/A entered `CAVE`, and A on Search completed a Vimm lookup across the
  configured consoles. The TV showed 10 results grouped by console. No result
  was opened and no game file was downloaded. Evidence:
  `docs/evidence/hardware-run-2026-10-10/honor-tv-vimm-search-keyboard-gamepad-a.png`,
  `docs/evidence/hardware-run-2026-10-10/honor-tv-vimm-keyboard-gamepad-typed-c.png`,
  `docs/evidence/hardware-run-2026-10-10/honor-tv-vimm-search-results-cave.png`.
- [ ] This uses synthetic GAMEPAD-source events, not the Bluetooth controller.
  It verifies GameBox's Store search path but not D-pad/A handling in the live
  WebView or activation of its Download control. Android still reports no
  connected controller.

## 2026-10-10 — Vimm WebView controller and fullscreen recheck

- [x] Wired both `BUTTON_A` and Android's `DPAD_CENTER` through the tracked
  browser's explicit focused-element activation path. On the connected Honor
  HDMI display, D-pad Up moved focus from Vimm's Download control to the
  harmless **More...** details link, and synthetic `DPAD_CENTER` expanded its
  metadata. B returned to GameBox with “Returned without starting a download.”
  No ROM transfer was started.
- [x] On this build, the browser activity shared the external TV task's
  `fullscreen` mode instead of opening as a floating freeform window. The
  HONOR Dock/taskbar still overlaid the bottom edge; its global setting was not
  changed. Android still reported the Bluetooth controller disconnected, so
  these checks used synthetic events rather than physical-controller input.
- [x] Full `:app:testDebugUnitTest` and
  `:app:compileDebugAndroidTestKotlin` passed after the changes; the debug APK
  was installed and exercised on the external TV display.
- [ ] Physical-controller acceptance and activation of the website's actual
  Download control remain unverified. Do not use a copyrighted title for a
  binary-download test; use an authorized redistributable homebrew fixture.

- [ ] Follow-up on the connected Honor: set HONOR's per-app **Open in full
  screen mode** for both GameBox and its tracked browser. The device later
  locked itself; an external-display capture then showed HONOR's lock screen,
  so settled dock-free rendering could not be reconfirmed. No global taskbar
  setting was changed. Unlock the phone before the next TV acceptance run.
- [ ] The WebView code and unit test confirm A/DPAD_CENTER dispatch to the
  focused element, and a harmless Vimm **More...** link responded on device.
  This does not prove the site's Download control reaches GameBox's tracked
  DownloadListener or starts a Download Manager record. Do not activate a
  copyrighted game download; reproduce with an authorized homebrew file/link.
- [x] Added visible D-pad Navigate, A Select, and B Back hints to the tracked
  source browser toolbar, with instrumentation assertions for all three. The
  updated source compiles; `:app:testDebugUnitTest` and
  `:app:compileDebugAndroidTestKotlin` pass. UI instrumentation and TV visual
  acceptance remain pending because the Honor is locked.
- [x] Applied the host's `FLAG_KEEP_SCREEN_ON` policy to the tracked browser
  too, so an active Vimm session does not fall back to the screen timeout. This
  does not defeat a manual lock. Rebuilt and installed `0.1.0` in place on the
  Honor at 03:01 UTC on 2026-10-10 without clearing app data; TV visual/runtime
  verification is still pending unlock.
- [x] Fixed tracked-browser popup handling so a user-initiated page replaces
  an existing popup instead of being rejected. This prevents an earlier
  source/ad popup from occupying the single slot and swallowing the next
  user-selected Download page. Automatic popups remain rejected. Unit tests,
  Android-test compilation, and APK packaging pass; the update was installed
  in place at 03:03 UTC without clearing data. Live Vimm verification remains
  pending unlock and a legal test link.
- [x] Updated controller A/Center activation to pass native Enter events through
  to WebView instead of calling JavaScript `element.click()`. The prior
  synthetic click could lose Chromium's user-gesture context, preventing a
  download link or `_blank` popup from following its normal tracked path. Unit
  coverage verifies key translation; an Android WebView regression exercises
  a focused `_blank` legal placeholder link and asserts trusted popup handling.
  Unit tests and Android-test compilation pass; instrumentation is not yet run
  on device. APK installed in place on Honor with data preserved.
- [ ] Physical-controller activation of Vimm's page Download control remains
  unverified. Earlier Honor HDMI captures do prove browser transfer progress
  continued after leaving the source page and later reached Completed / Added
  to Library; they do not prove controller-A activation or a lawful test
  payload. Do not repeat downloads of titles without verified redistribution
  rights.
- [x] On the charging Honor, enabled Android's reversible `stay_on_while_plugged_in`
  setting for AC and USB (`3`); it was previously disabled (`0`). The device
  reports AC+USB charging and the setting now reads back as `3`. This prevents
  idle sleep while docked/charging, but does not defeat a manual lock; unlock
  is still required for TV UI acceptance.

## 2026-10-10 — Windows companion validation

- [x] Confirmed .NET 8.0.17 runtime/SDK is installed. Release build of the WPF
  app and Windows core succeeded with zero warnings/errors; the executable
  companion test project then built successfully with one MSBuild worker.
- [x] Ran the Windows companion core test harness; it reported
  `GameBox Windows core tests passed.` The first sandboxed run was denied its
  isolated temp-file atomic move, so the same prebuilt harness was rerun with
  permission for its own unique temp fixture, which is deleted by the harness.

## 2026-10-10 — Companion request failure isolation

- [x] Unexpected route exceptions now return a generic HTTP 500 for that
  connection rather than unwinding the listener. Added a real-socket regression:
  an authenticated route throws, then a subsequent authenticated status request
  still succeeds. All 8 `CompanionHttpConnectionTest` cases and the full Android
  JVM suite pass. The debug APK was installed in place on the Honor at 03:17,
  preserving app data; device runtime validation is pending unlock.
- [x] Replaced companion HTTP with TLS and exact SHA-256 certificate pinning
  on Android and Windows. The connected Honor PGT-N19 passed the pinned status
  handshake and the plaintext-HTTP rejection check; Windows companion core
  tests pass. Pairing now requires copying the Honor's displayed certificate
  fingerprint as well as the pairing secret. Existing saved profiles without a
  fingerprint must be paired again.
- [ ] The request replay guard remains in-memory and resets when the service
  restarts; TLS protects the transport but does not make replay history durable.

## 2026-10-10 — Companion request replay guard

- [x] Added a random 128-bit nonce to the v2 HMAC authorization token for each
  Android and Windows companion request. Android accepts a nonce only once per
  service lifetime; repeated signed requests are rejected. Android socket/JVM
  tests and the Windows companion test harness pass.
- [x] Rebuilt and installed the debug APK in place on the connected Honor,
  preserving app data. `git diff --check` passes.
- [ ] Replay history is in-memory and resets when the companion service restarts;
  HTTP is still unencrypted, so captured traffic can expose request data and
  credentials. Do not expose the companion to untrusted networks until TLS and
  response authentication are implemented.
- [ ] TV/controller acceptance remains unverified while the Honor is locked and
  Android reports no connected gamepad. Vimm Download click-through and the
  Download Manager handoff still need a physical legal-homebrew test.

## 2026-10-10 — Vimm browser focus and companion TLS

- [x] Fixed Vimm's embedded-browser focus matcher to inspect visible text and
  accessibility labels independently (previously nonempty visible text masked
  the Download ARIA label); custom role-buttons are made keyboard-focusable.
  A representative WebView focusability instrumentation test passed on the
  physical Honor, and the connected test installed the updated app in place
  with data preserved. Physical controller activation and click-through on
  Vimm itself remain unverified because no controller was available.
- [x] Android companion TLS endpoint passed its focused instrumentation test on
  Honor PGT-N19, including certificate-pinned HTTPS and plaintext HTTP rejection.
  Windows companion core tests pass. The current APK is installed on Honor;
  TV/controller acceptance remains open because the external display is off and
  the controller is disconnected.

## 2026-10-10 — Blueprint goal and current hardware recheck

- [x] Revalidated the main software gate against the Blueprint: the critical
  remaining end-to-end proof is still authorized install → launch → real save
  backup → uninstall preserving saves → reinstall/restore. Keep mock/synthetic
  evidence separate from real provider, emulator, save, and hardware evidence.
- [x] Current Honor ADB state: the real external HDMI display is connected at
  1920×1080 (display 3); the Honor internal display is also present. The
  foreground remains NotificationShade/HONOR launcher, so GameBox is not
  currently visible. Android reports no GAMEPAD/JOYSTICK input. The in-place
  stay-awake setting remains `3` while charging; it does not override manual
  lock. Older roadmap entries saying HDMI is absent describe earlier sessions
  and are superseded by this snapshot, not rewritten as past observations.
- [x] Re-ran `:app:testDebugUnitTest`,
  `:app:compileDebugAndroidTestKotlin`, and `:app:assembleDebug`; all passed.
  Rebuilt the Windows companion test project (zero warnings/errors) and reran
  its test harness (`GameBox Windows core tests passed`).
- [x] `:app:lintDebug` also completes with zero errors. It reports 26 warnings:
  mostly SDK/dependency update notices, plus large-screen orientation/resizing
  guidance, two Compose modifier-order suggestions, obsolete mipmap qualifier,
  and KTX style suggestions. No dependencies or display policy were changed
  solely to silence warnings; those changes need compatibility review.
- [ ] Run the new native-Enter WebView regression and the user-facing Vimm
  controller activation on the wired TV after unlock, using only a currently
  reachable authorized homebrew test page/file. Prior captures already show
  persistent transfer progress and automatic import; do not treat them as
  physical-controller activation evidence.
- [ ] Attempted `TrackedSourceBrowserToolbarTest` on the connected Honor. The
  runner targeted external display 3, but the lock/notification shade remained
  foreground; both methods were reported as instrumentation-process crashes.
  Captured test/logcat output contains no actionable app/WebView exception, so
  classify this run as inconclusive (neither pass nor product regression).
  Unlock before retrying the single WebView activation test.

## 2026-10-10 — Vimm stale-vault diagnosis and recovery

- [x] Rechecked the previously referenced PSP vault page using read-only HTTPS
  requests: Vimm's `/vault/PSP/G` index returns HTTP 200 and still lists the
  title under ID `27934`, while both the old ID `23719` and listed ID `27934`
  return HTTP 404 with the generic PSP-vault page. A browser-like User-Agent
  receives the same 404. This explains the dead Download page for those stale
  entries; it is not evidence that controller input is broken.
- [x] Vimm transport now distinguishes HTTP 404 from redirects, server errors,
  and network failures. For a missing Vimm detail page, GameBox removes the
  dead direct URL, tells the user the listing is unavailable, keeps the
  console-category browse route, and leaves personal-copy import available.
  Other transport failures retain the existing retry/open-source fallback.
- [x] Added regressions for typed 404 handling, unavailable-listing guidance,
  and the category-search fallback. Android JVM tests, Android-test compilation,
  and APK packaging pass; the build was installed in place on the Honor with
  app data preserved.
- [x] Existing Honor HDMI evidence from 2026-10-09 shows one browser transfer
  active in GameBox Downloads after leaving the source page, then two completed
  browser imports marked Added to Library. This verifies that prior transfer
  and auto-import flow; source-content redistribution rights and physical
  controller activation were not verified by those captures.
- [ ] Revalidate the unavailable-listing notice and controller-A behavior on
  the unlocked 1920×1080 TV. Current Honor state remains lock shade with no
  GAMEPAD/JOYSTICK input.

## 2026-10-10 — Live Honor state recheck

- [x] Recovered the already-authorized ADB connection and checked the device
  without waking it or changing user data. Honor reports `Asleep`, display 0
  `OFF` at 1312×2848, and only the built-in display; the previously connected
  HDMI TV is no longer enumerated. Package `com.gamebox.os` remains installed
  at version `0.1.0`.
- [x] Android input exposes one `KEYBOARD | DPAD` source but no GAMEPAD or
  JOYSTICK source. This does not establish that the Bluetooth controller is
  connected or that its A button reaches the WebView.
- [x] Re-ran `:app:testDebugUnitTest` and
  `:app:compileDebugAndroidTestKotlin`; both passed. These validate the software
  path, not live Vimm activation or Download Manager behavior.
- [ ] TV acceptance remains pending until the Honor is awake/unlocked, HDMI is
  attached, and the physical controller is available. Use an authorized
  redistributable test download only; do not start a commercial game download.

## 2026-10-10 — Normalize controller keys for tracked WebView

- [x] The saved Honor TV capture shows the live Vimm **Download** control had
  focus, while the user's report is that activation did not work. GameBox now
  translates D-pad movement to Tab and controller A to D-pad Center, then sends
  translated events as `SOURCE_KEYBOARD`, preserving timing, repeat, device,
  scan code, flags, and meta state.
- [x] Strengthened `TrackedSourceBrowserToolbarTest` to begin with a
  `SOURCE_GAMEPAD` A event, apply the production translation, assert the
  resulting keyboard source, and retain the trusted `_blank` download-popup
  assertion. Unit tests, Android-test compilation, APK assembly, and lint pass.
  Installed the APK in place on the Honor; app data was not cleared.
- [x] Current PC gate: 792 Android JVM tests pass (one skipped), Android-test
  Kotlin compilation, APK assembly, and lint pass (0 errors; 24 warnings).
  The focused Android runner was attempted on the real Honor, but the device
  went asleep; one Compose test failed to initialize and the runner stalled
  before the WebView activation test completed. No live download was started.
- [ ] Run the focused WebView activation regression and an authorized
  redistributable Download Manager test on the awake Honor/TV. The latest live
  snapshot is asleep with the internal display off and HDMI disconnected, so
  this patch is not yet hardware-verified.

## 2026-10-10 — Exact game ownership during uninstall

- [x] Tightened `GameOwnedContentUninstaller` to accept only legacy
  `imports/<game>/...` and managed `imports/<known-console>/<game>/...`
  paths. A malformed manifest such as `imports/other/<game>/...` can no longer
  pass by placing the selected game's ID somewhere in another game's path.
- [x] Added a no-partial-deletion regression for that cross-game path. The
  focused uninstall tests and full 792-test JVM suite pass; APK and lint gates
  also pass. This protects recorded-path deletion, not external SAF providers.
- [ ] Recheck uninstall/reinstall with multiple imported games and retained
  saves on the awake Honor; the current device is asleep and HDMI disconnected.

## 2026-10-10 — Windows companion core verification

- [x] Found the installed .NET 8 SDK/runtime and ran the Windows core test
  harness from current source with Windows targeting enabled. All custom core
  checks passed. The harness's temporary files were directed into the writable
  workspace after its default `%TEMP%` path hit sandbox access denial.
- [ ] Windows companion UI and live Honor-to-PC companion flows are not covered
  by this core harness and still need separate end-to-end acceptance.

## 2026-10-10 — Resizable tracked browser for external display

- [x] Changed `TrackedSourceBrowserActivity` from explicitly non-resizable to
  resizable, matching the app's external-display host. The debug merged manifest
  confirms `android:resizeableActivity="true"`; a JVM manifest-contract test
  prevents regression.
- [x] All 791 Android JVM tests pass; Android-test Kotlin compilation, APK
  assembly, and lint pass (0 errors; 26 existing warnings). Installed the APK
  in place on the Honor, preserving app data.
- [ ] Verify the browser actually fills the Honor HDMI display and hides the
  window chrome/taskbar. The phone is still asleep and the physical TV is not
  enumerated, so manifest/build verification does not count as visual acceptance.

## 2026-10-10 — Partial uninstall state recovery across storage

- [x] Fixed a mixed-storage failure case in the Blueprint's uninstall/reinstall
  journey. If private files had already been removed and external SAF deletion
  then failed before its first file, the thrown error reported zero removals and
  could leave the game marked Installed. Removal failures now combine earlier
  and current-stage file counts before setting `MISSING_FILES` and reporting
  recovery progress.
- [x] Added a regression for aggregating files removed before a later storage
  failure. All 792 Android JVM tests pass; Android-test compilation, APK
  assembly, and lint pass (0 errors; 24 warnings). Installed the APK in place
  on the Honor with app data preserved.
- [ ] The SAF provider disconnect/failure case and the full save-preserving
  reinstall flow still need integration/physical validation; the Honor remains
  asleep with HDMI disconnected.

## 2026-10-10 — Honor state after focused instrumentation

- [x] Rechecked hardware before continuing: Honor is asleep, its built-in screen
  is off, the HDMI display is not enumerated, and no gamepad/joystick input is
  present. No wake/unlock action was issued.
- [x] Android reported the GameBox package absent after the focused test run.
  Reinstalled the current debug APK with `adb install -r` (no data-clear option)
- [x] Launched the reinstalled app without unlocking the phone. `am start -W`
  returned `Status: ok` / `LaunchState: COLD` in 2.5 s; `MainActivity` remained
  resumed and the GameBox process was alive. HONOR's NotificationShade is still
  above the app, so no UI acceptance is claimed. Starting the app initialized
  its databases/files directories; previous library/save preservation across
  the package gap cannot be confirmed from current state. No external game
  files were touched by these checks.
- [ ] Determine why the package was absent after instrumentation and verify
  install/test cycles preserve app-private data before using another Honor
  instrumentation run; resume the Blueprint hardware flow only after the phone
  is awake, HDMI is present, and the controller reconnects.

## 2026-10-10 — Remove unused TheGamesDB metadata scraper

- [x] Current runtime uses keyless Wikidata metadata and Libretro named box-art/
  screenshot lookups. Removed the unreferenced TheGamesDB per-game metadata
  client/parser and their obsolete parser/cancellation tests; generic provider
  candidate contracts now live independently of any one metadata source.
- [x] Corrected README's stale claim that TheGamesDB metadata enrichment is
  wired into runtime. The separate optional API-key catalog sync remains and
  may still provide fields to its own synced rows; finding a replacement sync
  source is still open rather than silently claiming this is fully migrated.
- [x] Android JVM suite passes 783 tests (0 failures, 1 skip); Android-test
  Kotlin compilation, lint, and APK assembly pass. Installed the verified APK
  in place on Honor with `adb install -r`; no data-clear flag was used.
- [ ] Phone is asleep with HDMI disconnected, so there is no TV visual check of
  existing covers/screenshots or runtime fallback behavior in this turn.

## 2026-10-10 — Vimm custom Download activation follow-up

- [x] Investigated the remaining report that the focused Vimm Download control
  would not activate. The tracked browser now maps controller A to Enter and
  equips matching custom ARIA role-buttons with Enter/Space click behavior;
  native buttons and links keep WebView's normal activation path.
- [x] Added unit coverage for the A-key mapping and a local WebView fixture for
  focusing and activating a custom role-button. Unit tests, Android-test Kotlin
  compilation, lint, APK packaging, and the focused WebView instrumentation
  passed; `adb install -r` updated the Honor app without clearing app data.
- [ ] Hardware acceptance is still pending: current ADB state is Asleep, the
  internal display is OFF, HDMI is not enumerated, and no Bluetooth gamepad is
  reported. The fixture test is not proof of activation on Vimm's live page or
  the wired TV.

## 2026-10-10 — Preserve active browser downloads past history limit

- [x] The durable browser-download store previously truncated to 100 records
  only when loading, which could silently discard an older active transfer
  after process restart. Writes and reads now keep all queued, downloading,
  paused, and importing records while capping only terminal history.
- [x] Added regressions proving old active jobs survive the 100-record limit
  and active jobs are never dropped even when they alone exceed that limit.
  Unit tests, Android-test Kotlin compilation, lint, and APK assembly pass; the
  build was installed on Honor with `adb install -r`.
- [ ] No process-kill/download-resume or TV acceptance was run in this turn:
  the Honor is asleep, HDMI is absent, and no physical controller is present.

## 2026-10-10 — Vimm controller focus and standalone NES default

- [x] Fixed the tracked browser's startup focus race: its WebView now requests
  focus when attached to the window, so controller navigation/activation is not
  sent to an unfocused view. The local Vimm-like WebView instrumentation test
  passes on the physical Honor.
- [x] Galaxy Patrol and NES platform defaults now select NES.emu instead of
  RetroArch; tests cover standalone defaults across the supported platform
  groups. The external emulator itself is not installed on Honor, so no live
  handoff/gameplay is claimed.
- [x] Full Android JVM suite passes (787 tests, 0 failures, 1 skipped), lint
  reports 24 warnings and 0 errors, and the debug APK builds. Restored the APK on Honor with
  `adb install -r` after the connected instrumentation runner left the package
  absent.
- [ ] Live Vimm Download acceptance and controller navigation remain pending:
  no physical controller input was available, and TV projection was not
  validated. The instrumentation used a local fixture, not Vimm's live page.
- [ ] Because the package was absent after instrumentation, the APK restore was
  a fresh install; survival of app-private state across that runner behavior is
  not confirmed. User ROM files were not touched by this test or restore.

## 2026-10-10 — Exercise Vimm controller activation through the real activity

- [x] Replaced the earlier fixture-only key test, which dispatched a synthetic
  JavaScript key event and did not cover the production activity's input path.
  A controller A on a focused Vimm custom Download control now activates it
  through a scoped WebView click; one `target=_blank` popup is allowed for that
  action and the allowance is consumed or expires after one second.
- [x] The production `TrackedSourceBrowserActivity` test passed on the Honor:
  a locally intercepted HTTPS Vimm fixture verified focus, GAMEPAD-A dispatch,
  the Download click, and in-app popup creation. No external page content or
  game binary was fetched.
- [x] Full JVM suite passes (787 tests, 0 failures, 1 skipped), Android-test
  Kotlin compilation and APK build pass; lint reports 24 warnings and 0 errors.
  The current app was installed with `adb install -r` before the direct runner;
  its package stayed installed through the test (same first-install timestamp).
- [ ] No physical controller or TV acceptance: Honor is Asleep and only its
  internal display is currently enumerated. Live Vimm behavior and actual
  DownloadManager tracking/import therefore remain unverified.

## 2026-10-10 — Restore browser focus after page navigation

- [x] Re-requested Android focus from the active WebView on each page-finished
  event before applying Vimm's Download-control focus. This covers pages and
  popup destinations where initial activity attachment focus was later lost.
- [x] Android JVM suite, Android-test Kotlin compilation, lint, and debug APK
  assembly passed (lint: 24 warnings, 0 errors). Updated the Honor APK in place
  and ran `TrackedSourceBrowserFocusTest` successfully (1 test); test uses a
  local Vimm-origin fixture and does not start a network download.
- [ ] Live Vimm Download and physical controller/TV acceptance remain pending:
  the Honor is asleep, no gamepad is connected, and TV output is not enumerated.

## 2026-10-10 — Re-run Windows companion core harness

- [x] Ran the Windows core console assertion harness with its actual entry
  point (`dotnet run`); it passes. `dotnet test` alone only reported a build
  because this project is an executable harness, not a test-SDK project.
- [x] Confirmed the first sandboxed run's atomic library-save denial is due to
  restricted temp-folder access: the same harness passes outside the sandbox.
  No product code changes were needed.
- [ ] Windows UI and live Honor-to-PC companion flows remain unverified; the
  Honor is asleep and no current TV/controller session is available.
- [ ] A fresh read-only ADB hardware check confirms only the 1312×2848 built-in
  panel (state OFF), no HDMI display, `mWakefulness=Asleep`, and no gamepad in
  Android's input-device inventory. TV acceptance cannot be inferred from the
  passing PC or fixture tests.
- [x] The WPF companion UI project builds with zero warnings/errors. Confirmed
  no saved paired-device profile exists, so startup would not contact the Honor.
- [ ] Attempted to launch the built WPF UI through the approved computer-use
  flow, but app approval timed out; visual/interaction acceptance was not run.

## 2026-10-10 — Physical HDMI legal install and WebView focus regression

- [x] Reworked `TrackedSourceBrowserFocusTest` so it retains the production
  `WebViewClient` and tests the real page-finished focus handler, controller-A
  activation, and in-app `about:blank` popup with a local Vimm-origin fixture.
  The first run exposed a test readiness race; the corrected test passes on the
  Honor (1 test). No live Vimm transfer or game binary was requested.
- [x] Confirmed the app and test package stayed installed after instrumentation,
  and that HDMI was active as Android display 10 at 1920×1080. Launched GameBox
  on the physical HDMI and verified Home, PC Hub, Store, Galaxy Patrol details,
  and its post-install state with TV captures under `tmp/gamebox-tv-*.png`.
- [x] Installed the bundled legal MIT Galaxy Patrol fixture through Store on
  HDMI; Details changed from “not installed” to “installed / Play Now.”
- [ ] TV Dock still overlays the bottom 40 px, including the controller footer,
  even in `ExternalDisplayActivity`; `hideSystemBars` and the app's fullscreen
  launch request do not suppress this HONOR system Dock. The Dock's long-press
  app menu offered only GameBox, Pin to Taskbar, and Quit, not the documented
  per-app fullscreen option. Reconcile the Settings guidance with the actual
  HONOR Dock flow; do not claim borderless TV acceptance yet.
- [x] Installed the official free NES.emu 1.5.85 nightly and added package
  visibility declarations for the six supported EX Plus Alpha emulators. This
  fixes Android package-visibility checks that falsely reported installed
  standalone emulators as missing. Unit tests and debug APK build pass.
- [ ] Physical controller is still reported disconnected. Screenshots confirm
  TV rendering only; they do not substitute for controller acceptance.

## 2026-10-10 — Vimm result fallback verified on HDMI

- [x] Reproduced Vimm result details losing their Download action when detail
  hydration returned HTTP 404. Kept the already-validated vault URL so the
  tracked in-app browser remains available for fallback browsing.
- [x] Updated and installed the Honor APK; JVM unit tests and debug APK build
  pass. Actual HDMI search returned 11 GBA Mario results with box art; opening
  one showed Download, and activating it opened `TrackedSourceBrowserActivity`.
- [ ] The stale result's vault URL currently resolves to Vimm's GBA vault index,
  not that game's details. Improve stale listing resolution; physical controller
  activation and DownloadManager/import remain unverified. No ROM was fetched.
- [ ] A rerun of the focused connected instrumentation test crashed the test
  process without a captured fatal exception; Gradle cleanup left GameBox
  uninstalled. Reinstalled the APK, which now opens first-run setup. Treat
  app-private library/settings restoration as at risk; external game files were
  not touched. Android reports no restore sets and no GameBox-owned media/files
  directory, so prior app-private state has not been recovered. Avoid another
  connected-test run until its uninstall behavior is understood. The module
  uses `ANDROIDX_TEST_ORCHESTRATOR`; no explicit `clearPackageData` option is
  set, so the uninstall appears to be connected-test cleanup after the runner
  crash rather than an intentional per-test data-clear setting.

## 2026-10-10 — Blueprint 1.0 acceptance checkpoint

The Blueprint's finish line is the repeatable browse → install → controller
launch → return → retain save → uninstall → reinstall slice, plus offline
library use and reliable recovery (Blueprint pp. 6, 17, 49–51, 75). GameBox has
the screens and substantial domain/test coverage, but the following release
gates remain open; implemented UI is not counted as hardware acceptance:

- [ ] Honor TV target is currently at fresh first-run setup. Android reports no
  backup set; prior app-private catalog/settings were not recovered. Do not
  choose a storage root or reimport files without an explicit location/recovery
  direction. External game files were not touched.
- [ ] Complete the legal-homebrew vertical slice on the Honor/TV: durable
  download and progress → verify/extract/import → Library → actual emulator
  launch/return → real save discovery/backup → content-only uninstall with save
  retained → reinstall and resume. The recent Vimm test stopped before a ROM
  transfer; Vimm's stale GBA listing currently redirects to its console index.
- [ ] Physical Bluetooth-controller acceptance (D-pad, A/B, search, dialogs,
  reconnect during app/emulator use). Android reports no connected controller;
  synthetic events and touch do not satisfy this gate.
- [ ] Verify each supported standalone emulator on HDMI for content handoff,
  persistent fullscreen behavior, hidden touch overlay, controller mapping, and
  safe return. Dolphin required HONOR's per-app fullscreen control; emulator
  launch/return and save retention remain incomplete across the target set.
- [ ] Re-run storage/download failure and offline cases without risking the
  Honor install: queue/process recovery, pause/resume/cancel, network loss, low
  storage, checksum/import failure, and launchability with network disabled.
- [ ] Preserve the user's deployment decision: Honor Magic6 Pro + wired dock +
  TV is the acceptance target; Android TV is not. Blueprint's S23/DeX references
  are superseded for deployment, while its controller-first and 1080p criteria
  remain applicable.

Automated JVM tests and APK assembly passed for the Vimm fallback change.
Connected instrumentation is still unreliable on this Honor: a focused browser
test crashed its instrumentation process during desktop-display activity
handoff. The debug app was reinstalled and returned to HDMI foreground; its
database files are present. Do not treat the failed run as a product-test pass.

## 2026-10-10 — Route stale Vimm results to current console listings

- [x] Corrected the 404 path to replace an expired `/vault/<id>` target with
  the Vimm platform/title browse route. The existing notice and Download action
  remain, so a result whose listing disappeared can still enter GameBox's
  tracked browser on the current category instead of reopening a dead ID.
- [x] `:app:testDebugUnitTest` and `:app:assembleDebug` pass after the change.
- [ ] Honor ADB currently cannot start its local server, so no live TV/UI
  verification or in-place APK install was possible. Physical controller and
  Vimm DownloadManager/import acceptance remain open.

## 2026-10-10 — Honor TV stale-result and legal-install pass

- [x] Reconnected with elevated ADB and verified the active wired TV output at
  1920×1080. Updated GameBox in place with `adb install -r`; first-run app data
  remained intact for this session.
- [x] On the TV, searched PSP Vimm results for “God of War”, saw three
  console-grouped results with box art, opened Chains of Olympus, and confirmed
  the removed-listing notice. Its Download action opened the tracked browser on
  the current PSP/G category; the current game title was listed and its detail
  page opened. Returned to GameBox without starting a site download. Captures:
  `tmp/honor-tv-vimm-search-live.png`,
  `tmp/honor-tv-vimm-stale-details.png`,
  `tmp/honor-tv-vimm-fallback-browser.png`, and
  `tmp/honor-tv-vimm-current-game-details.png`.
- [x] Installed the bundled legal 1 MB Galaxy Patrol fixture on HDMI. The game
  is shown as installed and verified, and its Library/game-details state is
  reachable. The Library contains one game, marked available offline; Downloads
  shows 48.0 KB of 48.0 KB, Completed, and Ready. Captures:
  `tmp/honor-tv-galaxypatrol-install.png`,
  `tmp/honor-tv-galaxypatrol-library.png`,
  `tmp/honor-tv-galaxypatrol-downloads.png`, and
  `tmp/honor-tv-galaxypatrol-completed.png`.
- [ ] Galaxy Patrol still uses the branded initials fallback in Library. Its
  old generated illustration was retired as unverified pseudo-box-art; provide
  approved original cover art before replacing the fallback.
- [x] Checked Play Now safely: GameBox stays in its details screen and reports
  the missing approved runtime `com.explusalpha.NesEmu`; that package is not
  installed on this Honor. Capture:
  `tmp/honor-tv-galaxypatrol-launch-status.png`. No emulator was purchased or
  installed.
- [ ] Physical controller gameplay, real save creation/backup, and
  content-only uninstall/reinstall remain unverified.
- [ ] Android reports no connected controller. TV touch navigation is not
  physical-controller acceptance. The HONOR Dock remains visible over the
  bottom of GameBox and obscures part of the controller-hint footer.
- [ ] No Vimm binary was fetched. Actual controller activation, durable download
  tracking, and automatic import remain open.

## 2026-10-10 — Standalone NES launch on wired Honor TV

- [x] Fixed Android 11+ package visibility for supported EX Plus Alpha
  runtimes and added a manifest regression test. `:app:testDebugUnitTest`
  and `:app:assembleDebug` pass; the merged APK contains all six declarations.
- [x] Installed free NES.emu 1.5.85 from the official EX Emulators nightly
  release on the Honor. GameBox Play Now now resolves the runtime instead of
  showing the missing-emulator warning.
- [x] Launched the bundled, legal Galaxy Patrol fixture from GameBox on the
  physical 1920×1080 HDMI display. The game rendered in NES.emu; closing the
  content and exiting NES.emu returned to GameBox. No commercial ROM was used.
- [ ] This was a touch-driven acceptance pass because Android reports the
  Bluetooth controller disconnected. NES.emu's touch controls and the HONOR
  Dock remain visible; fullscreen/controller-overlay acceptance is still open.
- [ ] Save creation/backup, content-only uninstall/reinstall, physical
  controller input, and actual Vimm Download activation/tracking/import remain
  unverified. No Vimm game binary was downloaded.

## 2026-10-10 — TV focus check for Vimm Download

- [x] Reopened the PSP result on the wired Honor TV. GameBox routed to Vimm's
  current PSP listing; the game detail page rendered its Download control, and
  GameBox's focus script visibly focused it. Safe D-pad Down/Up key injection
  moved focus to the adjacent control and back without starting a transfer.
- [ ] Android currently reports no connected gamepad/joystick (GameBox shows
  “Not connected”). Physical A-button activation and the tracked-download flow
  remain unverified. Did not activate Vimm's commercial game download.
- [ ] The HONOR Dock/taskbar is still visible over GameBox's TV footer.
- [x] Checked HONOR Settings search on the HDMI display for “Global taskbar”
  and “desktop”. Neither search exposed a Dock visibility control; Settings was
  closed without changing any device settings, and GameBox was restored to the
  TV foreground. The OS-owned Dock remains unresolved.
- [x] Inspected the Dock launcher menu as well; it exposes app search/launch,
  not taskbar visibility. Dismissed it without launching an app. The previously
  authorized hide/restore test cannot safely proceed until an explicit OEM
  visibility control is identified.

## 2026-10-10 — Vimm visible Download focus hardening

- [x] Updated the Vimm focus selector to reject controls hidden by an ancestor
  or with no rendered layout rectangle. The browser fixture now puts a hidden
  duplicate Download action before the visible one, guarding responsive
  desktop/mobile markup from focusing the invisible control.
- [x] JVM tests, debug APK assembly, and Android test Kotlin compilation pass.
- [x] Executed both production JavaScript snippets in a Node DOM fixture: the
  selector skips a hidden duplicate and focuses the visible custom Download
  control, and controller activation clicks that focused control. This is a
  script-level self-check, not WebView or hardware acceptance.
- [ ] The focused connected test crashed on the Honor during instrumentation
  activity handoff, so controller A activation is not hardware-verified. A
  direct AndroidJUnitRunner invocation reproduced the process death; Honor
  ActivityManager logged a system kill of the target process as the test opened
  its activity, before any assertion result. Reinstalled the existing debug
  APK after Gradle cleanup and restored GameBox to HDMI Home; Room database
  files remain present. No clear-data command or game-file operation was run.
- [ ] Physical A-button activation, real Vimm transfer tracking/import, and
  the full hardware regression remain open; no download was started.

## 2026-10-10 — Retire legacy TheGamesDB sync

- [x] Removed TheGamesDB transport, parser, sync, provider-health and API-key
  access from active app code. Configured JSON discovery and Vimm discovery
  remain available. Room's legacy platform-ID fields are retained for upgrade
  compatibility, and any previously encrypted API key remains stored but is
  no longer read by GameBox.
- [x] Updated metadata documentation and removed the legacy-specific tests.
  Full local gate passed: 771 JVM tests (0 failures/errors, 1 skipped), debug
  app and Android-test APK assembly, lint (0 errors; 24 warnings).
- [x] Installed the APK on the physical Honor with `adb install -r`, preserving
  app data. GameBox launched on HDMI display 10 at 1920×1080; no startup
  exception appeared in the inspected logcat window.
- [ ] Controller activation and actual authorized-download tracking/import
  remain unverified; Android reports the controller disconnected. Metadata
  source availability and Switch artwork coverage also remain open.

## 2026-10-10 — Preserve browser activation for Vimm Download

- [x] Changed controller A on Vimm pages from asynchronous JavaScript
  `.click()` to WebView's native Enter key path, retaining a short popup window
  so `target=_blank` actions stay inside GameBox. Updated the Android HTML
  fixture to exercise a focused Download link with a popup.
- [x] Full local Android build gate passed: 771 JVM tests (1 skipped), app and
  instrumentation APKs assembled, lint 0 errors. Installed the app in-place on
  the Honor and launched GameBox on its 1920×1080 HDMI display.
- [ ] The focused Honor instrumentation run was stopped by the OS while it
  handed off the target activity (`ActivityManager` reports “stop com.gamebox.os
  due to start instr”), before returning assertions. GameBox was restored to
  HDMI Home. No live Vimm Download was activated; hardware/controller acceptance
  remains open.

## 2026-10-10 — Honor TV synthetic Vimm navigation check

- [x] On the physical 1920×1080 HDMI surface, synthetic D-pad/A events opened
  Home → Store → Vimm search. A selected the initially focused Q key; B exited
  the keyboard and the search completed with 19 titles grouped by console and
  visible cover art. The source reported failures for GameCube, NES, PSP, SNES,
  Wii, and WiiWare.
- [x] Opened a result's details and activated its focused GameBox **Download**
  action. It opened `TrackedSourceBrowserActivity` on the TV. For the stale
  Quantum Theory listing, Vimm showed its PS3 Q results page; the app details
  correctly warned that the listing was no longer available.
- [x] D-pad Down advanced focus through Vimm's native page links (the first
  focused link was “Emulation Lair”; after 20 steps focus was “PlayStation”).
  A on that focused site-navigation link returned to Vimm's homepage, not a
  game row. This explains that test navigation and does not verify Download
  activation.
- [ ] No website Download control was activated. The exact God of War page's
  automatic focus and the native A → Download behavior remain unverified; the
  physical controller is still disconnected. The Honor test runner was killed
  during activity handoff before assertions, and no transfer was started.

## 2026-10-10 — Vimm result-to-detail focus verified on Honor TV

- [x] Added a fallback in the existing Vimm focus script: when a page has no
  Download control yet, focus its first visible numeric `/vault/<id>` result.
  The Download control still takes precedence on game-detail pages. Extended
  the Android fixture through result activation and the resulting detail page.
- [x] Installed the rebuilt APK on the Honor and replayed the Store search on
  HDMI with synthetic key events. The Vimm query page opened with its first game
  row visibly focused; synthetic A opened that row, Cloudflare completed its
  human check, and the game's Vimm detail page loaded with its 3.53 GB Download
  button visibly focused. Evidence: `tmp/honor-vimm-tv-first-result-focused.png`
  and `tmp/honor-vimm-tv-detail-after-check.png`.
- [ ] Did not activate the website Download control or start a copyrighted
  transfer. Physical-controller A and durable download/import acceptance remain
  open; the Bluetooth controller still reports disconnected. Focused Honor
  instrumentation remains unavailable because the OS kills the target process
  during test activity handoff.

## 2026-10-10 — Vimm controller confirm compatibility

- [x] Unified Vimm’s popup permission and WebView refocus for both controller
  confirm keycodes (`BUTTON_A` and `DPAD_CENTER`); extended the Honor browser
  fixture to exercise D-pad confirm on a Download link.
- [x] Unit tests (771, 0 failures, 1 skipped), debug and instrumentation APK
  builds, and lint passed. Updated APK installed in place on the Honor; GameBox
  reopened on the TV without clearing app data.
- [ ] Physical-controller acceptance and actual Vimm Download activation remain
  unverified because the Bluetooth controller is disconnected. No transfer was
  started; the instrumentation fixture compiled but was not run on-device.

## 2026-10-10 — Vimm Download focus made visible on Honor TV

- [x] Added a high-contrast yellow focus outline to the focused control in
  Vimm's embedded browser; the existing auto-focus still prioritizes the
  visible Download control over result links. The local browser fixture now
  asserts that the outline is present.
- [x] Rebuilt and installed the APK in place. On the physical Honor HDMI
  display, reopened GameBox, searched Vimm for `Q`, opened Quake III: Arena's
  Vimm detail page, and verified that the website Download button is visibly
  outlined in yellow. Evidence: `tmp/honor-tv-vimm-focused-download.png`.
- [ ] Did not activate the commercial-game Download link. The physical
  controller remains disconnected, so confirm-button acceptance is still
  unverified. The attempted connected-device test task ran the broad suite
  rather than the intended single class and the instrumentation process crashed;
  no instrumentation assertions are counted as passed.

## 2026-10-10 — Honor instrumentation runner limitation

- [x] Retried with Gradle's explicit `--project-prop` filter; output confirmed
  that exactly one `TrackedSourceBrowserFocusTest` case was scheduled. HONOR's
  activity manager again killed `com.gamebox.os` with “stop com.gamebox.os due
  to start instr,” and the instrumentation case failed before assertions.
- [x] The runner removed the target package. Reinstalled the APK and relaunched
  GameBox through the TV app drawer; the TV now shows first-run setup, confirming
  app-private preferences/database state did not survive the test-runner
  uninstall. The shared `/storage/emulated/0/GameBoxOS` tree still exists and
  contains `Emulators/Azahar`; no files were found under that managed root.
- [ ] Do not run HONOR instrumentation again without a non-destructive test
  strategy. Physical-controller acceptance and setup/library restoration remain
  open; user files elsewhere in shared storage were not inventoried or changed.

## 2026-10-10 — Authorized homebrew install-to-launch on Honor TV

- [x] Reinstalled the app after the destructive HONOR instrumentation attempt
  and installed the bundled MIT-licensed Galaxy Patrol fixture from Store. Its
  detail page showed `installed` and offered **Play Now**.
- [x] Launched Galaxy Patrol on the physical 1920×1080 TV display. NES.emu
  opened the game and rendered the live play screen. Evidence:
  `tmp/honor-tv-galaxy-patrol-launch.png`.
- [ ] The HONOR taskbar remained visible over both the GameBox details page and
  the emulator. NES.emu also displayed its on-screen touch controls. Verify and
  address fullscreen/control behavior separately; the Bluetooth controller was
  disconnected, so hiding touch controls conditionally and physical input were
  not verified. No uninstall/reinstall or save-retention step was attempted.
- [ ] Vimm's website Download activation and actual download tracking/import
  remain unverified; no copyrighted title was downloaded.

## 2026-10-10 — Honor fullscreen and emulator return follow-up

- [x] Enabled HONOR Desktop Mode's **Open in full screen mode** for GameBox.
  The bottom Dock disappeared on the Galaxy Patrol details page and remained
  hidden after exiting NES.emu back to GameBox. Evidence:
  `tmp/honor-tv-gamebox-fullscreen.png` and
  `tmp/honor-tv-gamebox-fullscreen-return-final.png`.
- [x] Opened NES.emu's on-screen input settings and confirmed **Use Virtual
  Gamepad: Auto**. With no Bluetooth controller connected, its touch controls
  remain visible; NES.emu's documented Auto mode is the appropriate setting to
  hide them when a controller is detected. No mapping was changed.
- [x] HONOR's **Open in full screen mode** hid the Dock for the active NES.emu
  window; the legal sample continued rendering and the app returned to GameBox
  through NES.emu's normal Exit flow. Evidence:
  `tmp/honor-tv-nes-active-fullscreen.png` and
  `tmp/honor-tv-galaxy-patrol-gameplay.png`.
- [ ] A fresh GameBox → NES.emu handoff still showed the Dock, even after the
  per-app fullscreen action had been used in an earlier NES.emu session. The
  active-window workaround is verified, but automatic fullscreen on each
  emulator launch is not. Physical controller detection, gameplay input, and
  touch-control auto-hide remain unverified until the controller reconnects.
- [ ] Save creation/retention, uninstall/reinstall, Vimm download activation,
  and tracked import remain open. No copyrighted download was activated.

## 2026-10-10 — Honor overrides emulator fullscreen launch request

- [x] Captured Android's actual launch trace for Galaxy Patrol → NES.emu on
  display 10. ActivityTaskManager logged the launch display and fullscreen task
  area (`task-display-area-windowing-mode=1`), but the resulting NES.emu task
  was assigned HONOR's `hw-pc-freeform` mode and its Dock remained visible.
  Evidence: `tmp/honor-tv-nes-launch-mode.png` and the device's
  ActivityTaskManager/dumpsys output from this run.
- [x] Confirmed the platform limit: Android's supported
  `Activity.requestFullscreenMode()` is called by the Activity whose window is
  being changed. GameBox cannot invoke it on NES.emu's Activity.
- [ ] Automatic third-party emulator fullscreen remains an HONOR Desktop Mode
  limitation. The per-window **Open in full screen mode** action hides the Dock
  for the running emulator; GameBox itself now returns fullscreen with the Dock
  hidden, but HONOR still displays its top window-control strip. Do not claim
  automatic, borderless emulator fullscreen is fixed unless it is reproduced
  after a fresh launch without that manual action or shell chrome.

## 2026-10-10 — Authorized content uninstall and reinstall on Honor TV

- [x] Confirmed the GameBox details view showed one installed content file
  (49,168 bytes) and no managed save files before removal. The uninstall dialog
  explicitly said saves, backups, metadata, favorites, and history are retained
  and external emulator files are not touched.
- [x] Confirmed uninstall on the TV. GameBox reported one content file removed;
  `run-as` verified the exact file disappeared from its app-private managed
  install path. The Galaxy Patrol record remained available and showed
  `not installed`.
- [x] Reinstalled the same bundled MIT-licensed fixture from its details page.
  GameBox reported verified, ready-to-play content; `run-as` confirmed the
  file returned with the trusted catalog SHA-256
  `97c1757ffd6a5bc1a591809b2b0f8988741f61f6abd82889c148ecae8a2f471f`.
  Evidence: `tmp/honor-tv-game-details-scroll-1.png`,
  `tmp/honor-tv-galaxy-patrol-uninstall-confirm.png`,
  `tmp/honor-tv-galaxy-patrol-uninstalled.png`, and
  `tmp/honor-tv-galaxy-patrol-reinstalled.png`.
- [x] Opened Download Manager on the TV after reinstall. The queue showed 0
  active/queued jobs, and **Completed** showed Galaxy Patrol as completed and
  ready (48.0 KB of 48.0 KB). This verifies the legal fixture's completed-item
  display and the Details → Downloads navigation, not a live remote transfer.
  Evidence: `tmp/honor-tv-downloads-after-reinstall.png` and
  `tmp/honor-tv-downloads-completed.png`.
- [ ] This proves managed-content removal/reinstall and catalog integrity only.
  No managed save existed before removal, so preservation of real game progress
  remains unverified; Vimm download tracking/import and physical-controller
  acceptance also remain open.

## 2026-10-10 — TV artwork fallback and Vimm button focus check

- [x] Added an original bundled Galaxy Patrol cover fallback for the legal
  fixture when no catalog art URL is available. The cover renders on the
  Honor's 1920×1080 HDMI display in the Home hero and collection card.
  Evidence: `tmp/honor-tv-library-galaxy-patrol-art.png`.
- [x] Targeted artwork unit test, debug APK build, and `lintDebug` completed
  successfully; installed the APK in place without clearing app data.
- [x] Opened the live Vimm WiiWare Cave Story listing (view-only). The actual
  website Download button was visibly focused with GameBox's yellow focus
  outline. Evidence: `tmp/honor-tv-vimm-cave-story-page.png`.
- [ ] Did not activate the website Download button: this would start a
  copyrighted WiiWare transfer. Android now enumerates the Sony Wireless
  Controller as a Bluetooth GAMEPAD/JOYSTICK. A later raw-event capture
  confirmed physical input while Dolphin was foregrounded (see follow-up
  below). Existing local WebView fixture tests and visible focus do not prove a
  live download is tracked or imported end-to-end. On return,
  Download Manager displayed Cave Story as completed and added to Library; this
  pass did not initiate that transfer, and its origin was not determined.
- [ ] The bottom HONOR Dock reappeared after the in-place app update; reapply
  the per-app **Open in full screen mode** setting and check whether it persists
  through future updates.

## 2026-10-10 — Physical controller events reach Dolphin on Honor TV

- [x] With HDMI display 10 active, Android's raw input monitor recorded real
  Sony Bluetooth controller D-pad, face-button, and shoulder events. The
  foreground app was Dolphin, not GameBox.
- [x] Passive TV capture showed Cave Story's in-game save-file prompt with
  **Yes** focused. This confirms real controller navigation reached the
  emulator/game; it does not prove the GameBox shell's key handling.
- [ ] Did not confirm the save prompt or change this game's state. The
  controller-only GameBox screen flow and save-retention/uninstall/reinstall
  acceptance still need an authorized homebrew title on actual hardware.
  Evidence: `tmp/honor-tv-physical-controller.png` and
  `tmp/honor-tv-dolphin-current-state.png`.

## 2026-10-10 — Full Android gate and Windows test-runner check

- [x] Current Android source passed `:app:testDebugUnitTest`,
  `:app:assembleDebug`, and `:app:lintDebug`: 772 JVM tests, 0 failures, 0
  errors, 1 skipped (`DirectorySaveAdapterTest.ignoresLinkedDirectoriesFilesAndStagingArtifacts`,
  because this Windows host lacks symbolic-link privileges); debug APK
  assembled; lint completed successfully.
- [x] The Honor stayed connected to the physical 1920×1080 HDMI display, with
  Dolphin still foregrounded. The newly assembled APK was not installed, so
  the running game/session was not interrupted.
- [x] `.NET 8.0.314` and runtime `8.0.17` are installed. The standalone
  `GameBox.Windows` WPF project builds in Release with workload resolution
  disabled (0 warnings/errors).
- [ ] `GameBox.Windows.Core.Tests` did not build/run: MSBuild's workload
  resolver reports a missing `Microsoft.NET.SDK.WorkloadAutoImportPropsLocator`
  SDK, while disabling the resolver still causes the project-reference target
  evaluation to fail without diagnostics. `dotnet workload list` is also
  blocked by service-control-manager access denied. No project files were
  changed for this environment issue.
- [ ] Windows companion self-check, on-Honor GameBox controller navigation,
  authorized-homebrew save retention, and TV Dock persistence remain open.

## 2026-10-10 — Vimm Download controller activation follow-up

- [x] Confirmed the Honor remains connected to HDMI. The focused local browser
  instrumentation test ran on the PGT-N19; it uses a fake local Download
  button and does not open Vimm or transfer a game.
- [x] Updated the browser instrumentation fixture to use a native `input`
  Download button and controller A, matching the TV capture more closely. The
  updated test source compiles; targeted gamepad-navigation and Vimm-source JVM
  tests pass. On-device instrumentation initially exposed an overly strict
  `3px` CSS assertion (Honor reports `2.85714px`); the assertion now checks a
  visible focus outline with device-scale tolerance. The rerun passed on the
  Honor, including controller-A activation and the in-app popup assertion.
- [ ] The reported inability to activate Vimm's Download action remains
  unresolved on the exact live Vimm page. The live page's Download control was
  visibly focused in the earlier TV capture, but it was not activated because
  that would start a copyrighted game transfer.
- [ ] The test process ended Dolphin's prior Cave Story session and returned
  the emulator to its main screen; I reopened Dolphin, and its main activity is
  now foregrounded. The in-game prompt/save state was not confirmed or changed
  by us; verify the game can be resumed before claiming that session was
  preserved.

## 2026-10-10 — Honor test-runner recovery and full Android gates

- [x] Full current-tree Android gates passed: 772 JVM tests (0 failures, 0
  errors, 1 skipped for Windows symlink privilege), `assembleDebug`, and
  `lintDebug`. The focused Honor WebView fixture test also passed controller-A
  activation and the in-app popup check.
- [x] The Honor instrumentation runner removed `com.gamebox.os` after the test.
  Verified with `pm path` returning no package and `run-as` reporting unknown
  package. Reinstalled the built debug APK and launched GameBox; the current
  Home screen renders, and Galaxy Patrol is shown as **not installed**. This
  indicates the earlier app-private installed game content was not preserved
  through instrumentation. Evidence: `tmp/honor-after-instrument-reinstall.png`.
- [x] The dedicated `/storage/emulated/0/GameBoxOS` directory still contains
  `Emulators/Azahar`; it was not modified. No clear-data command was issued.
- [ ] Do not run Honor instrumentation again until it can be isolated from the
  user's installed package/data. Do not claim app-private content or database
  restoration from the successful WebView fixture run.
- [ ] Current Honor state is GameBox Home on the built-in 1312×2848 display;
  HDMI is not active and Android/GameBox report no controller connected. TV
  fullscreen/controller acceptance and restoring the legal Galaxy Patrol
  content remain open.

## 2026-10-10 — Isolate debug instrumentation package from user install

- [x] Added `.debug` application-ID/version suffix to the debug build, keeping
  normal/release `com.gamebox.os` separate from `com.gamebox.os.debug` used by
  instrumentation. Updated the provider-authority test and CI's explicit
  instrumentation package and screenshot path.
- [x] Verified packaged IDs with Android `aapt`: app is
  `com.gamebox.os.debug`; test runner is `com.gamebox.os.debug.test`.
- [x] Current-tree Android JVM tests, app/test APK assembly, and lint pass:
  772 tests, 0 failures, 0 errors, 1 Windows symlink-permission skip.
- [ ] Did not run the isolated variant on the Honor yet: HDMI is disconnected
  and the user specified TV-first visual acceptance. Package separation is
  build-verified but still needs a device check before relying on it as the
  complete instrumentation safety fix.

## 2026-10-10 — Windows companion self-check repaired and executed

- [x] The Windows self-check was a console executable, so `dotnet test` had
  reported success without executing tests. Its only WPF-app dependency was
  `PairingProfileStore`; moved that UI-independent DPAPI store into the Core
  library and removed the WPF project reference from the self-check project.
  MainWindow and the self-check already reference Core.
- [x] Ran the actual harness with `dotnet run`; it printed
  `GameBox Windows core tests passed.` The first run exposed that sandbox Temp
  is read-only, so the successful reruns used process-only `TEMP`/`TMP` paths
  under the writable workspace. No project data is retained by the harness.
- [x] Rebuilt `GameBox.Windows` Release after the move: 0 warnings, 0 errors.
- [ ] Companion live-network, Windows UI, and Honor-to-PC pairing/content/save
  flows still require the connected target hardware and companion host.

## 2026-10-10 — Dolphin Wii Remote controller mapping

- [x] Read the Honor's Dolphin configs. The existing GameCube profile already
  used the Sony Bluetooth controller device; `WiimoteNew.ini` had only phone
  motion-sensor/rumble bindings, despite selecting a Nunchuk extension.
- [x] Backed up the original Wii Remote config to
  `tmp/honor-dolphin-WiimoteNew-before-map.ini`. Added controller A/B/1/2,
  minus/plus, D-pad, Nunchuk C/Z/stick, and right-stick IR-pointer bindings,
  retaining the existing motion and rumble bindings. A local INI self-check
  confirmed unique keys, valid lines, and the saved controller device path.
- [x] Stopped Dolphin while it was backgrounded at its main screen, pushed only
  `Config/WiimoteNew.ini`, then pulled it back; its SHA-256 matches the prepared
  file. The rollback copy remains available.
- [ ] Bluetooth controller is disconnected and HDMI is inactive, so actual
  Wii gameplay, Nunchuk behavior, pointer calibration, and reconnect behavior
  have not been tested. Motion-dependent titles may need per-game mappings;
  verify the mapped buttons with a legal homebrew title on TV before claiming
  Dolphin is controller-ready.

## 2026-10-10 — Vimm custom Download control and current verification

- [x] Expanded Vimm's browser focus helper to include visible custom clickable
  elements without semantic button roles, and give those elements Enter/Space
  activation. The instrumentation fixture now uses a custom clickable `div`.
- [x] A Node-based DOM stub verified focus selection, hidden duplicate
  filtering, and Enter-to-click behavior. Android's 772 JVM tests pass (0
  failures, 1 Windows symlink-permission skip), `assembleDebugAndroidTest`
  compiles the changed fixture, and `lintDebug` succeeds with 16 warnings.
- [x] Traced the in-app Store search → Vimm result details → tracked browser
  route: it passes the stable source-derived game ID, title, platform ID, and
  available metadata required by Download Manager/import. Missing metadata is
  guarded with a visible message, but is not expected on this details route.
- [x] Re-ran the Windows Core self-check (`GameBox Windows core tests passed`)
  and Release WPF build (0 warnings/errors).
- [x] `assembleRelease` also succeeds; package metadata is
  `com.gamebox.os` / version `0.1.0` / code `1`.
- [ ] The locally assembled production-variant APK is unsigned because
  signing inputs are not configured in this workspace. The protected CI
  signing workflow remains the only production-signing path; do not distribute
  this local APK.
- [x] Ran the isolated `AuthorizedRomImporterStorageFailureTest` on the
  physical PGT-N19. It passed; afterward `com.gamebox.os` remained installed,
  `com.gamebox.os.debug` was absent, and MainActivity remained foregrounded.
- [ ] TV/controller UI acceptance remains pending: HDMI and the Bluetooth
  controller are inactive, and no visible UI test was run on the phone. When
  the TV is available, run the custom-control fixture on the isolated debug
  package before claiming live Vimm activation is fixed.

## 2026-10-10 — Honor download-queue persistence check

- [x] Ran `DownloadRepositoryIntegrationTest.immediateUpdatesSurviveBeforeFirstObservedRow`
  on the physical PGT-N19. It passed and verified queued state transitions reach
  the in-memory Room table in order while observers have not yet received the
  first row; this does not verify persistence across process/device restart.
- [x] Confirmed the normal `com.gamebox.os` release package remained installed;
  the isolated `.debug` and test-runner packages were removed after the run.
- [ ] HDMI and controller remain unavailable, so TV navigation and live Vimm
  Download activation are still unverified.

## 2026-10-10 — Honor database migration matrix

- [x] Ran `ReleasedSchemaMigrationMatrixTest.everyReleasedSchemaUpgradesToCurrentWithoutDataLoss`
  on the PGT-N19. All released schema versions 1–13 migrated to the current
  schema with the sentinel game row and required tables intact; the test's
  uniquely named databases were removed afterward.
- [x] Confirmed `com.gamebox.os` remained installed and no `.debug` or test
  runner package remained.
- [ ] This database test does not validate the real installed database, app
  restart recovery, HDMI UI, or physical-controller navigation.

## 2026-10-10 — Honor content-removal safety checks

- [x] Ran all three `GeneralContentRemovalTest` cases on the PGT-N19. Synthetic
  multi-file uninstall/reimport retained save bytes and play metadata, rejected
  a symbolic-link deletion targeting another game's file, and failed closed
  when a save backup could not be created.
- [x] The tests used unique trees under the isolated `.debug` app cache and
  removed their fixtures. The normal `com.gamebox.os` package remained
  installed; no instrumentation packages remained.
- [ ] This verifies synthetic app-managed files, not the user's real library
  or a real emulator save on the TV setup.

## 2026-10-10 — Honor reimport identity safety

- [x] `ReimportIdentityIntegrationTest.identityIsCheckedBeforeReplacingExistingFiles`
  passed on the PGT-N19: a mismatched source was rejected without replacing
  installed bytes or save data, and a matching source restored the game while
  retaining synthetic progress. Temporary test files were removed.
- [x] The release package remained installed; the isolated instrumentation
  packages were removed after the run.
- [ ] This synthetic import test does not validate user-selected SAF files,
  emulator launch, or a real game/save on TV.

## 2026-10-10 — Tracked-download store reload regression

- [x] Added `BrowserDownloadStorePersistenceTest` and ran it on the PGT-N19.
  A tracked record's game/platform metadata and byte-progress state reload from
  app preferences after constructing a fresh store instance.
- [x] The test asserts it runs only in `com.gamebox.os.debug` and restores the
  previous preference value. The on-device test passed; the normal release
  package remained installed.
- [ ] Recreating the store is not a process-death or Android DownloadManager
  end-to-end test; the TV/controller transfer flow remains unverified.

## 2026-10-10 — Browser download recovery unit gate

- [x] Freshly reran all seven `BrowserDownloadRecoveryTest` cases: active
  monitor selection, stable work names, completion resumption states, retention
  limits, and non-empty staged-file recovery all passed (0 failures/errors).
- [ ] These policy/file tests do not exercise a live Android DownloadManager
  transfer, process death, or automatic import on the physical TV.

## 2026-10-10 — Honor 7-Zip runtime extraction

- [x] `SevenZipRuntimeTest.packagedSevenZipRuntimeExtractsSyntheticPspPayload`
  passed on the PGT-N19. The production APK runtime extracted a generated 7z
  archive and returned the expected synthetic ISO bytes; the test cache fixture
  was cleaned up, and the release app remained installed.
- [ ] This verifies 7z/LZMA runtime extraction only; SAF selection, full import
  registration, source-archive deletion, and TV launch remain separate checks.

## 2026-10-10 — Synthetic 7-Zip import and source cleanup

- [x] Added `SevenZipImportLifecycleTest` for the shared import workflow used by
  tracked downloads and manual copy-import. On the PGT-N19 it verified a valid
  generated PSP 7z archive becomes the registered `.iso` under the managed
  console directory and the source archive is removed after registration.
- [x] The same test supplied a corrupt archive and confirmed import failure
  leaves the source untouched and does not register a game. Test data stayed in
  a unique `.debug` cache tree; the release package remained installed.
- [ ] This synthetic file-URI test does not cover SAF-provider deletion or the
  live DownloadManager/TV/controller route.
- [x] Added a policy regression covering all 18 configured Vimm preset console
  names: each accepts `.7z` as an archive while excluding the container from
  registered game-file extensions. All five `RomImportPolicyTest` cases pass.

## 2026-10-10 — HDMI and controller reconnected; TV locked

- [x] ADB reports physical HDMI display 11 at 1920×1080 and a Sony controller
  input device (vendor 1356/product 1476). This confirms enumeration only, not
  successful GameBox navigation.
- [x] Captured HDMI via its SurfaceFlinger display ID. The TV currently shows
  HONOR's “Phone is locked, please unlock” screen; GameBox's MainActivity task
  is present but not visible. The temporary screenshot file was removed from
  the phone after capture.
- [ ] Do not bypass the device lock. Resume TV-only UI/controller testing after
  the user unlocks the Honor; then verify the app is foreground on HDMI.

## 2026-10-10 — Standalone emulator coverage audit

- [x] Compared `EmulatorCapabilityRegistry.optionsFor` with the 18-console
  Vimm default preset. Four platforms currently have no standalone route:
  PS3, Xbox, Xbox 360, and Switch. Existing behavior reports unsupported rather
  than silently routing them to RetroArch.
- [x] Honor package inventory found only PPSSPP, RetroArch, Azahar, Dolphin, and
  NES.emu among the checked emulator families; other configured standalone
  targets are not installed on this device.
- [x] Primary-source check found the RPCS3 Android port is archived and
  discontinued; the Xenia Android port is archived/EOL; the original-Xbox
  Android candidate X1 BOX has unverified distribution/launch integration and
  requires user-supplied system files; Strato is explicitly experimental.
- [x] `:app:testDebugUnitTest --tests com.gamebox.os.LaunchAdapterTest` passes
  (2026-10-10). This validates adapter selection logic only, not emulator
  installation or gameplay on the Honor/TV.
- [ ] Do not add package defaults or download/install APKs until each candidate
  has a verifiable maintained source, package identity, launch contract, and
  legal BIOS/firmware setup. Test supported packages on HDMI after unlock.

## 2026-10-10 — Fresh TV and controller regression pass

- [x] Honor is unlocked; HDMI remains active at 1920×1080 and Android still
  enumerates the Sony controller (vendor 1356/product 1476). Enumeration is
  not proof of GameBox input delivery.
- [x] Full `:app:testDebugUnitTest` passes: 773 tests, 0 failures, 0 errors,
  1 skipped. Browser controller mapping and launch adapter focused tests pass;
  Android instrumentation test Kotlin compilation also passes.
- [ ] No live UI was driven: Android currently reports the HDMI display cannot
  host tasks, HONOR's desktop-shell activity is foreground on HDMI, and
  GameBox's MainActivity is hidden on display 0. Avoid launching a test Activity
  onto the phone display. Re-establish the GameBox HDMI task, then verify the
  Vimm Download control with the physical controller and a legal homebrew flow.
- [ ] Do not rerun Honor instrumentation until its uninstall/data-loss behavior
  is fixed; a prior run removed app-private state. No app data was changed in
  this pass.

## 2026-10-10 — Blueprint acceptance and Android build gates

- [x] Re-read the Blueprint's completion definition, controller focus model,
  test matrix, vertical-slice spec, release checklist, and priority order.
  Adapt the hardware target to the user's later decision: Honor Magic6 Pro
  docked to HDMI, not Samsung DeX or Android TV.
- [x] Current `:app:assembleDebug`, `:app:lintDebug`, and
  `:app:compileDebugAndroidTestKotlin` all pass. Lint reports 0 errors and 16
  warnings (dependency/target-SDK freshness, Compose modifier ordering,
  orientation hints, and KTX suggestions).
- [x] Tried the explicit HDMI-only launch of `ExternalDisplayActivity` on
  display 11. Android rejected the shell launch because the Activity is
  non-exported; no app was launched on the phone and no user data changed.
- [ ] Resume the vertical-slice and controller acceptance only from GameBox's
  own UI using the PC Hub external-display action. Then validate controller
  navigation, legal homebrew download/import, emulator launch/return, real save
  retention, and content-only uninstall/reinstall on the TV. Do not use
  phone-display UI or repeat destructive instrumentation against production.

## 2026-10-10 — Companion build and lock-state recheck

- [x] Fresh Windows Core assertion harness passes when its temporary files are
  redirected to the repository's writable `tmp` directory. The default temp
  location is denied by this execution sandbox; no source/config workaround
  was added.
- [x] Current Windows WPF companion Release build succeeds with 0 warnings and
  0 errors.
- [ ] Honor has relocked again (`isKeyguardShowing=true`). HDMI is present at
  1920×1080, but GameBox remains hidden on display 0 and the external-display
  host cannot be launched by ADB because it is non-exported. Do not bypass the
  lock or start UI on the phone; resume TV flow after unlock and in-app handoff.

## 2026-10-10 — TV vertical-slice retest

- [x] HDMI capture confirms GameBox at 1920×1080 with Galaxy Patrol details
  still installed. The TV is active; the visible 43 px HONOR dock still overlays
  GameBox's bottom controller legend.
- [x] Synthetic Android gamepad-source events moved focus Home → Library →
  Store, opened and closed the Vimm search keyboard (A/B), and opened Galaxy
  Patrol details. These are software-path checks only; the user chose to
  continue without a physical-controller acceptance test, and GameBox still
  reported the controller disconnected.
- [x] Installed the bundled MIT-licensed Galaxy Patrol fixture, launched it
  through the selected NES.emu standalone route on HDMI, and used the
  emulator's Exit action to return to GameBox. This verifies the local install
  → launch → return path, not gameplay compatibility or a Vimm transfer.
- [ ] NES.emu displayed its touch-control overlay while running; the HONOR dock
  also remained visible. Resolve emulator overlay/fullscreen and dock behavior
  on the TV, without treating a full-size app window as immersive fullscreen.
- [ ] Vimm's Download button was visible on details, but no real download was
  initiated. Validate focus/activation, persistent Download Manager tracking,
  import, and cleanup using only a legal homebrew source.
- [ ] Global Taskbar was not toggled. The approved TV-only Settings attempt
  opened HONOR Account Centre over GameBox instead; the ADB server then became
  unavailable. No HONOR settings were changed. Retry only when the exact
  setting and original state can be verified and restoration is assured.
- [ ] User reports they cannot activate Download on Vimm. Existing source
  auto-focuses a download-like control and maps D-pad/A into WebView Tab/Enter;
  `TrackedSourceBrowserFocusTest` covers a synthetic Vimm-shaped page, not the
  live site or the Honor controller. Reproduce the failure stage on TV before
  changing the WebView behavior. Current ADB connection is unavailable, and
  local Gradle startup is denied access to its native-platform lock file.

## 2026-10-10 — TV connection restored; Vimm issue narrowed

- [x] Reconnected to the authorized Honor over ADB. The device is unlocked;
  HDMI is active as Android display 11 at 1920×1080, and GameBox is foreground
  on that display. Closed the leftover Settings overlay without changing
  settings. A TV-only capture confirms GameBox is back on screen.
- [x] Android's current input-device inventory does not include the Sony
  controller; GameBox also displays “Not connected.” No physical-button result
  is claimed. The TV Store's Vimm search field currently has focus.
- [x] `:app:testDebugUnitTest --tests
  com.gamebox.os.input.GamepadBrowserNavigationTest` and
  `:app:compileDebugAndroidTestKotlin` both pass using Gradle 9.7.1. The
  instrumented live-WebView focus test was not run because the installed app's
  prior instrumentation path risked erasing private data.
- [ ] User reports Download cannot be activated on Vimm. No actual Vimm game
  page was reached in this pass, so focus, activation, popup handling, and
  transfer tracking remain unverified against the live site. No download was
  initiated. Reproduce on a legal homebrew result and preserve installed app
  data when selecting a test method.

## 2026-10-10 — Vimm download-control matching hardening

- [x] Broadened the WebView's controller focus selector from a short list of
  exact Download labels to visible interactive labels beginning with
  “Download”; the fallback to a vault result link remains for pages without a
  labeled action. Updated the local Vimm-shaped instrumentation fixture to use
  “Download this game,” which the prior selector missed.
- [x] Full Android unit suite passes: 773 tests, 0 failures, 0 errors, 1
  skipped. Android instrumentation test Kotlin compilation and Debug APK
  assembly pass. `git diff --check` passes.
- [x] Replaced the installed package with `adb install -r` (no uninstall or
  data clear) and confirmed GameBox MainActivity remains foreground on HDMI.
- [ ] The controller is still absent from Android's input-device list. The
  updated instrumented WebView test was compiled but not executed; a live Vimm
  Download activation and transfer/import remain unverified. No download was
  initiated.

## 2026-10-10 — Live Vimm route and TV focus verification

- [x] Reproduced the complete metadata-only flow on the Honor's HDMI display:
  searched Vimm for the user-owned PSP title, confirmed one result with box art,
  opened its GameBox details, and used GameBox's Download action.
- [x] Fixed the result-hydration failure path to retain the result's exact
  `/vault/{id}` URL instead of replacing it with a PSP letter-index page. The
  detail notice now says GameBox could not load extra details, rather than
  incorrectly claiming Vimm removed the listing.
- [x] Confirmed on TV that GameBox Download now opens the exact Vimm title page.
  Non-activating D-pad events visibly move the yellow focus between Vimm's
  Download and Play Online controls; focus can return to Download. The actual
  site Download button was not activated.
- [x] The installed test target was `com.gamebox.os.debug` (version 0.1.1-debug);
  the separate release package `com.gamebox.os` (0.1.0) was not overwritten.
  Existing Android unit suite (773 tests, 1 skipped), Android-test Kotlin
  compilation, and Debug APK assembly passed for the fix; `git diff --check`
  passes.
- [ ] Physical controller activation, Vimm transfer tracking/import, and
  emulator launch remain unverified. Android currently reports no connected
  controller. No copyrighted game file was requested or downloaded.

## 2026-10-10 — HDMI legal vertical-slice retest

- [x] Re-read the Blueprint's 1.0 definition, controller model, integration lab,
  release checklist, and first-vertical-slice requirements. The final deployment
  target remains the user's Honor Magic6 Pro over HDMI, superseding the
  Blueprint's original Samsung S23/DeX assumption.
- [x] On HDMI display 11 (1920x1080), installed the bundled legal Galaxy Patrol
  fixture using `com.gamebox.os.debug`. GameBox reported the game installed;
  Download Manager reported one completed item, “Galaxy Patrol — Ready,” and
  zero queued/active downloads.
- [x] Launched the game from GameBox through NES.emu 1.5.85 on the TV. The
  emulator became foreground on display 11. Exiting NES.emu returned to the
  same GameBox details screen, which recorded the return/time-away state.
- [x] Used GameBox's normal uninstall dialog. It reported one 49,168-byte
  content file, with saves/backups/metadata/favorites/history retained and
  external emulator files untouched. The uninstall result reported one file
  removed; reinstall then succeeded and the details showed verified/ready to
  play again. No managed save file existed, so save retention was not proven.
- [x] Temporarily changed NES.emu “Hide OS Navigation” from Off to On for a
  TV-only comparison, observed no removal of the HONOR desktop taskbar or the
  virtual gamepad, then restored the setting to Off and verified restoration.
  Virtual Gamepad remains Auto; its on-screen controls were visible after the
  touch-based launch, so physical-controller behavior remains untested.
- [ ] The HONOR dock/taskbar still overlays the bottom of GameBox and NES.emu.
  Read-only Settings inspection did not reveal a matching taskbar setting on
  this Magic6 Pro; no system setting was changed. Find the device-specific
  desktop-mode control or document the OS limitation before acceptance.
- [ ] Physical Xbox/PlayStation/Bluetooth input, a real managed save/restore,
  Vimm download activation/tracking/import, background-resume and network-loss
  downloads, emulator coverage beyond NES.emu, and the Blueprint soak test
  remain open. GameBox continued to report “Not connected.”

## 2026-10-10 — Vimm Download button activation fix

- [x] User reports they cannot press Download on Vimm. The controller focus
  matcher only accepted “Download” and a few exact short labels, missing longer
  actions such as “Download this game.” Broadened it to visible actionable
  labels beginning with “Download”; updated the browser instrumentation fixture
  to exercise that label.
- [x] Full Android unit suite passes: 773 tests, 0 failures, 1 skipped.
  Android-test Kotlin compilation and git diff --check also pass using the
  cached Gradle 9.7.1 distribution and Android Studio Java.
- [x] Later Honor HDMI verification confirmed the live Vimm Download control
  receives a visible focus outline; see “Honor TV acceptance of Vimm focus fix.”
- [ ] Instrumentation and actual Vimm activation remain unverified. The
  physical controller is disconnected, so no real hardware-A test was made and
  no download was triggered.

## 2026-10-10 — Honor TV acceptance of Vimm focus fix

- [x] Built and installed com.gamebox.os.debug version
  0.1.1-tvtest-debug, versionCode 3, using adb install -r; existing debug
  app data was preserved and release package com.gamebox.os was untouched.
- [x] Verified the Store → PSP → Vimm search flow on HDMI display 11. A
  controller-source D-pad/A sequence moved focus and entered a character;
  controller-source B closed the custom keyboard. A metadata search for
  “God of War” returned three PSP results with box art, including the exact
  user-selected “God of War: Chains of Olympus” details.
- [x] Opened GameBox’s Download route to the Vimm title page on HDMI. The
  actual Vimm Download control visibly received GameBox’s yellow focus outline.
  Returned to GameBox using its TV toolbar. Neither A nor touch activated the
  site Download action; no file was requested.
- [x] Checked Download Manager afterward on HDMI: 0 active, 0 queued/paused,
  and the existing single completed Galaxy Patrol item remained unchanged.
- [ ] Physical controller confirmation and the actual Vimm download → manager
  tracking → import flow remain unverified. The controller is disconnected;
  no copyrighted file was downloaded. HONOR dock is still visible over the
  bottom edge of GameBox.

## 2026-10-11 — Honor TV PSP import and launch slice

- [x] Used the existing user-provided God of War: Chains of Olympus `.7z`
  copy from `D:\GameBoxOS\games` in GameBox's Import your copy flow on HDMI
  display 11. The selected device staging folder was empty after import;
  GameBox extracted the PSP ISO into its managed PSP library and removed the
  staged archive. The PC source archive was left untouched.
- [x] Verified the imported PSP title and artwork in Library, then launched
  it from the game's details screen. PPSSPP received GameBox's content URI and
  booted the game to its title menu on the TV. Returned focus to GameBox.
- [x] Import progress was visible during the operation. No new Vimm download
  was started and no save data was created or changed.
- [ ] PPSSPP still appears with a window/title bar and the HONOR desktop dock
  visible. Fullscreen handoff/taskbar hiding is not accepted. Physical
  controller input, gameplay/save/restore, and the real Vimm download →
  Download Manager → import path remain unverified.

## 2026-10-11 — Honor controller-download instrumentation attempt

- [ ] The focused `TrackedSourceBrowserFocusTest` did not complete on the
  Honor. Gradle first rejected the default versionCode 1 as a downgrade; the
  retry with versionCode 4 started the test, but HONOR Desktop Mode force-stopped
  `com.gamebox.os.debug` during the instrumentation activity/display relaunch
  and the instrumentation process crashed before reporting any assertion.
- [x] Restored the still-installed production `com.gamebox.os` app to the TV.
  The user's original `D:\GameBoxOS\games\God of War - Chains of Olympus
  (USA).7z` remains present on the PC.
- [ ] `com.gamebox.os.debug` is no longer installed after the failed test run;
  its device-private imported-game test state is therefore unavailable. Do not
  rerun this instrumentation route on the Honor without isolating its activity
  from HONOR's display-relaunch/force-stop behavior.

## 2026-10-11 — Honor TV legal install and controller launch retest

- [x] Installed the current source as `com.gamebox.os.debug`, versionCode 4
  (`0.1.2-focus-test-debug`), alongside the still-installed production package.
  This was a fresh debug profile after the previous instrumentation failure;
  no release-package data was changed.
- [x] On HDMI display 11, used controller-source D-pad/A input to move between
  Home, Store, Library, and Downloads. D-pad Down visibly focused Galaxy
  Patrol's Install game button; A activated it.
- [x] Download Manager then showed Galaxy Patrol Ready, 1 completed, 0 active,
  and 0 queued/paused. Library showed Galaxy Patrol installed with artwork and
  one available-offline title.
- [x] Used controller-source A and D-pad to reach Play Now and start the legal
  Galaxy Patrol fixture through NES.emu 1.5.85. The emulator showed the game on
  HDMI; Android Back returned to GameBox, where the title appeared under
  Recently Played with a 5-minute session. No save was created or changed.
- [ ] In-game D-pad movement was not visually confirmed. NES.emu's on-screen
  controls and the HONOR desktop dock remained visible. Physical controller,
  save/restore, Vimm-download tracking, and the PSP reimport into this fresh
  debug profile remain open. The Honor instrumentation test is still unsafe to
  run because its external-display activity launch force-stopped the target app.

## 2026-10-11 — Honor TV uninstall/reinstall lifecycle

- [x] From Galaxy Patrol details, GameBox reported no managed save copy and
  no managed save files. The uninstall confirmation named exactly one content
  file (49,168 bytes), said saves/backups/metadata/favorites/history are kept,
  and said external emulator files are untouched.
- [x] Confirmed the normal in-app uninstall. GameBox reported one file removed,
  retained the listed metadata/history, and changed content status to not
  installed. Reinstalled the same legal fixture using Reinstall game; details
  again report installed and verified/ready to play.
- [ ] This validates content removal and reinstall for a no-save game only; it
  does not prove preservation/restoration of an actual emulator save artifact.

## 2026-10-11 — Honor TV Vimm controller-focus retest

- [x] On the real Honor TV, searched Vimm for “Mario” from the Store's
  controller keyboard. GameBox returned 65 grouped results across consoles;
  the GBA row displayed box art and opened the Mario & Luigi: Superstar Saga
  detail page.
- [x] Opened the source page in GameBox. Vimm's actual Download button showed
  the yellow focus outline. A synthetic D-pad Down moved that outline to the
  next action (“Play Online”), confirming directional focus movement; B
  returned to GameBox and displayed “Returned without starting a download.”
- [x] No file was requested, no download/import state was created, and the
  production app data was not changed. Physical-controller confirmation and
  Download Manager tracking/import still need a legal authorized fixture. The
  manager remained at 0 active, 0 queued/paused, and 1 completed (Galaxy
  Patrol).
- [ ] HONOR's bottom dock/taskbar appeared over the tracked Vimm browser
  activity, although no window title bar was visible. Fullscreen mode therefore
  needs another device-specific check for this activity; real-controller A
  remains untested because Android reports it disconnected.

## 2026-10-11 — Honor TV synthetic gamepad gameplay and return

- [x] On the physical 1920x1080 Honor TV, explicit gamepad-source D-pad Right
  moved focus from the Library filter to Search; D-pad Down focused Galaxy
  Patrol, and gamepad A opened its details and launched Play Now in NES.emu
  1.5.85.
- [x] In the legal MIT homebrew, gamepad Start entered active gameplay, D-pad
  Up moved the ship to the top edge, and D-pad Down moved it back into the
  playfield. This verifies the emulator receives directional gamepad events;
  it is synthetic ADB input, not a paired physical controller test.
- [x] Used NES.emu's System Actions → Close Content confirmation → Exit menu
  to return to GameBox without force-stopping the emulator. GameBox regained
  the Galaxy Patrol detail screen.
- [ ] NES.emu's touch controls and HONOR's bottom dock were visible throughout
  the game session. Keep touch controls available while no physical controller
  is connected; verify controller-aware auto-hide and per-app fullscreen after
  reconnecting the real pad. No save-state or real managed-save round trip was
  tested in this session.

## 2026-10-11 — Honor TV per-app fullscreen follow-up

- [x] Re-applied HONOR's app-specific “Open in full screen mode” setting to
  GameBox and its tracked Vimm browser activity. After the browser was active,
  its own taskbar context menu still offered the setting; enabling it removed
  the HONOR dock/caption, and returning with synthetic B kept GameBox dock-free.
- [x] Applied the same setting to NES.emu. On the 1920x1080 TV display the
  HONOR dock disappeared, but NES.emu's own red title bar remained. This is
  app-drawn chrome; the device fullscreen toggle cannot remove it.
- [x] Used explicit gamepad-source synthetic D-pad/A to focus and activate
  NES.emu's own Exit menu item, returning without force-stopping the emulator.
- [ ] This does not establish physical-controller behavior or configure every
  emulator. NES.emu's touch overlay was not changed; Dolphin, Azahar, DuckStation,
  and other emulator-specific fullscreen/control settings remain to be checked
  individually. No ROM or Vimm download was started.

## 2026-10-11 — Vimm Download activation follow-up

- [x] Reconnected to the Honor TV and confirmed the installed GameBox returns
  normally from NES.emu. Opened Store from Home for a safe UI-only check; the
  existing MARIO query displayed zero results and the app still reports the
  controller disconnected. No game page or Download action was opened.
- [x] Ran `:app:testDebugUnitTest --tests
  com.gamebox.os.input.GamepadBrowserNavigationTest` and compiled
  `:app:compileDebugAndroidTestKotlin`; both succeeded with Gradle 9.7.1.
- [ ] This does not validate the live Vimm site, a real browser transfer, or
  activation by a paired controller. The existing browser instrumentation
  fixture only proves focus/Enter click and popup creation; it does not assert
  DownloadManager enqueue, durable tracking, or import. Do not treat it as an
  end-to-end Vimm-download pass. Keep the Honor instrumentation activity test
  deferred because prior execution force-stopped the debug app in Desktop Mode.

## 2026-10-11 — Android and Windows software verification

- [x] Full `:app:testDebugUnitTest` passed. Focused browser navigation test and
  `:app:compileDebugAndroidTestKotlin` also passed.
- [x] `:app:lintDebug` passed with 0 errors and 24 warnings (dependency
  freshness/style notices, fixed-orientation advisories, and SDK/resource
  notices); no lint warning was silently treated as a test failure.
- [x] `:app:assembleRelease` completed successfully; this verifies release
  compilation, resource processing, lintVital, and APK assembly. Its artifact is
  `app-release-unsigned.apk`; it is not a signed production release and was not
  installed on the phone.
- [x] The Windows core self-test executable printed
  `GameBox Windows core tests passed.` `dotnet test` alone only built this
  executable project, so the self-test was run explicitly. Its first sandboxed
  attempt hit temp-directory access denial; the same test passed with ordinary
  Windows permissions and its own unique temp directory.
- [ ] Android instrumentation/UI tests, Windows desktop UI, physical gamepad,
  and the full legal download → Download Manager → import → launch → save/
  restore → uninstall/reinstall hardware journey remain separate acceptance
  gates. Release signing/provenance remains open.

## 2026-10-11 — Windows desktop build and discovery lookup audit

- [x] Built `windows/GameBox.Windows/GameBox.Windows.csproj` successfully with
  0 warnings and 0 errors. The Windows core self-test executable also passes
  (see the preceding verification entry).
- [x] Audited `CatalogDiscoveryRepository.observeGame()` after noticing the
  interface's default `null` flow. This is an optional interface fallback, not
  the production path: `RoomCatalogDiscoveryRepository` overrides it with the
  Room DAO flow, and GameDetails subscribes to the injected repository. No
  production stub/fix was justified by this finding.
- [ ] During this pass the Honor's external TV display disappeared from
  SurfaceFlinger; no phone-screen substitute was used. Physical TV/controller,
  live source transfer, Windows desktop runtime behavior, and Android
  instrumentation remain unverified.

## 2026-10-11 — Require real discovery-detail lookup implementations

- [x] Removed the interface's silent `observeGame() = null` fallback. The
  production Room implementation was already correct; making exact detail
  lookup mandatory prevents another repository implementation from silently
  dropping cached metadata used by GameDetails.
- [x] Expanded `DiscoveryDetailLookupTest` to reopen a 301-row database and
  query `RoomCatalogDiscoveryRepository` directly, verifying the exact title,
  platform, region, screenshots mapping, and absent-ID behavior.
- [x] Full Android JVM tests passed, Android instrumentation Kotlin compiled,
  and `:app:assembleRelease` succeeded after the contract/test change.
- [ ] The Room instrumentation test was not executed because no emulator is
  available and the Honor TV display is disconnected. Re-run that test on an
  isolated Android test target before treating its runtime path as accepted.

## 2026-10-11 — Verify requested default Vimm console preset

- [x] Confirmed the default preset contains all 18 requested names, settings
  use it when the preference is absent, and an explicitly saved empty source
  list stays empty rather than being silently repopulated.
- [x] Ran `VimmLairDiscoverySourceTest`, `SettingsPreferenceMapperTest`, and
  `StoreConsoleTest`; all passed. The configured preset retains 3DS and Switch,
  while actual Vimm searches skip them because no verified vault slug mapping
  exists; the UI explains that limitation.
- [ ] This is software-level configuration evidence only. Live Vimm browsing,
  real controller selection, and Download Manager/import acceptance remain
  pending on the TV.

## 2026-10-11 — Honor TV Vimm search and browser handoff

- [x] On the actual 1920x1080 Honor HDMI display, opened Store's Vimm source;
  the blank preset search returned 628 results grouped by console, with box
  art on metadata-backed results. The Store keyboard searched `MARIO` and the
  Vimm source returned 65 matches, also grouped by console with artwork.
- [x] Opened a result, navigated with D-pad events to focus `Download`, and
  activated it with a synthetic A event. GameBox opened its tracked Vimm
  browser; returning to GameBox showed the expected no-download-started
  message. Download Manager stayed at 0 queued/active and retained its one
  existing completed Galaxy Patrol install.
- [x] The website accepted a synthetic D-pad event, but this only moved focus
  to a rating cell; game-row selection and the website's own download control
  were not tested. No ROM was downloaded. Android still reports the physical
  controller disconnected, so physical-controller acceptance remains open.
- [x] HONOR's per-app `Open in full screen mode` option was active for GameBox
  after the browser was opened; the dock/taskbar disappeared and remained
  hidden after returning to GameBox Home.
- [ ] The selected Mario & Luigi result opened Vimm's GBA `M` listings page,
  not a verified title detail page; its result notice also said Vimm no longer
  had the listing. Confirm/repair title-specific source URLs with a currently
  available legal homebrew listing. No copyrighted game file was requested.

## 2026-10-11 — Separate stale Honor build behavior from current source

- [x] Read the actual Vimm WebView URL through its existing local debug socket:
  the installed app ended at `https://vimm.net/vault/GBA/M`. The installed
  package is version `0.1.0`, last updated 2026-10-10; it predates the current
  checkout's unavailable-detail handling, so this is not proof of current
  source URL behavior.
- [x] Current source builds each result link from its Vimm vault ID and keeps
  that link when detail hydration returns 404. Focused
  `VimmLairDiscoverySourceTest`, `ConfiguredSourceResultTest`, and
  `DiscoveryHttpsCancellationTest` all pass.
- [x] Added a fullscreen re-request to
  `TrackedSourceBrowserActivity.onResume()` to match the launcher's resume
  behavior. The focused unit tests and Android test Kotlin compilation pass.
- [ ] Could not install/verify this source on Honor: the existing production
  app is signed with a key unavailable in this environment, so replacing it
  with the debug/release build is unsafe. Re-test browser fullscreen and the
  title-specific Vimm page after a signed build is available. No game file was
  downloaded; the physical controller still reports disconnected.

## 2026-10-11 — Honor TV legal install and emulator launch slice

- [x] On the actual 1920x1080 Honor external display, installed the included
  1 MB Galaxy Patrol homebrew using the separate `0.1.0-debug` build. Its
  detail page changed to `installed`, and Library showed it as available
  offline.
- [x] Opened its details from Library and launched it through NES.emu. The
  emulator reached the running game screen; returning to GameBox restored the
  production Home screen. The production Home capture is full-screen with no
  HONOR dock/taskbar.
- [ ] NES.emu still displays its touch-controller overlay while the game runs;
  hiding that overlay and verifying real Bluetooth gamepad input remain open.
  The game was launched but not played through a complete level or save/restore
  cycle. Vimm's website download button was not activated and no ROM was
  downloaded.

## 2026-10-11 — Honor TV fullscreen, return, and test-content cleanup

- [x] Repeated the Galaxy Patrol launch on the actual 1920x1080 Honor TV output.
  Enabling HONOR's per-app `Open in full screen mode` for NES.emu removed the
  floating window chrome and bottom Dock; the game occupied the full TV screen.
- [x] Confirmed NES.emu rendered the running game. Its on-screen touch controls
  remain visible; Android reported the Bluetooth controller disconnected, so
  hiding the only available input would make this test unusable.
- [x] Used NES.emu's own close-content and exit actions to return to GameBox,
  then used GameBox's uninstall confirmation for this QA-only fixture. The UI
  reported one content file removed, and the debug app's installed-content tree
  was empty afterward. No existing games, ROMs, saves, or emulator apps were
  touched.
- [ ] Still pending: physical controller acceptance, hiding emulator touch
  controls only when a gamepad is connected, a full gameplay/save cycle, and a
  successful generic user-owned import plus live Vimm download tracking. No
  copyrighted game was downloaded.
