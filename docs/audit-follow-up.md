# Audit implementation follow-up

## Connected TV priority checkpoint (1 October 2026)

The connected Honor hardware is now the primary TV acceptance target. A live ADB
session identified the physical device as Honor `PGT-N19` running Android 16 and
reported an active HDMI presentation display at `1920×1080`/160 dpi. GameBox was
visible during wired projection. The in-product Show on TV action now reaches the
dedicated non-exported `TvActivity` on HDMI display 3. Honor initially places that
activity in a captioned `1152×691` Desktop Mode task; after the display-targeted
fullscreen control, the same activity reports a `1920×1080` requested window,
full touch region, and no caption node. The app requests fullscreen through both
the supported window flags and the launch-options hint, but this OEM still requires
the system maximize action. This is real hardware evidence, not emulator evidence;
automatic OEM promotion remains an open hardware integration gate.
An explicit `ACTION_MAIN`/`CATEGORY_HOME` launch of the same `TvActivity` was
also probed on the Honor and still produced the same `1152×691` freeform task;
that experiment was reverted because it did not improve the TV result.

The same live HDMI session was then promoted through Honor's display-targeted
fullscreen control. The `TvActivity` hierarchy reported root bounds `[0,0][1920,1080]`,
no Honor caption node, and the window manager reported a `1920×1080` requested
surface with a full-display touch region. A display-targeted tap moved the real TV
surface from Home to Store; the captured hierarchy showed `GAMEBOX OS / STORE`,
console filters for All/PS2/GameCube/Wii/PSP/Dreamcast/3DS/Switch/Homebrew, and
search controls. This is physical TV UI/navigation evidence, not emulator evidence;
controller, overscan, long-run, and complete legal-game lifecycle gates remain open.

Physical TV legal-game journey checkpoint (1 October 2026): on the maximized
Honor HDMI surface, the legal Galaxy Patrol fixture was opened from Store, its
Details action changed from `not installed` to `installed`, Library showed
`Galaxy Patrol, Retro, installed`, and Play Now launched the real
`com.retroarch/.browser.retroactivity.RetroActivityFuture` on display 3. A
display-targeted Android Back and controller-equivalent B input did not return
to GameBox; RetroArch remained top-resumed and running. The Honor Desktop Mode
caption exposed a visible Close control, but ADB touchscreen injection did not
activate it and the shell has no mouse-source command. No force-stop was used,
so the physical return gate remains honestly open.

Baseline: 9 September 2026 requirement audit, main `1504046a036f3a2a7e044b08bd62b6cb059b927e`. 
Keep implementation, production integration, automated validation, live-service/emulator
validation and hardware acceptance separate. The original audit is a frozen baseline,
not a claim that subsequent branch work is already on main.

Cross-platform regression checkpoint (28 September 2026): after the dedicated external
display host and confirmation-state changes, the complete Android JVM suite reported
551 tests with zero failures, zero errors, and one explicit privilege-based skip. The
Windows `GameBox.Windows.Core.Tests` Release harness also passed. These are local
software regression results; they do not replace live-provider, signed-release, or
physical-display acceptance.

The subsequent PC Hub confirmation-guidance change was followed by a complete API-35
instrumentation run: 68 tests completed, one explicit real-SAF setup test skipped by
the clean emulator state, and zero failures. This includes the production screenshot
flow for the PC Hub in the current build.

Honor instrumentation rerun (28 September 2026): the API-35 emulator completed the
focused and full Compose/integration coverage successfully, but the connected Honor
returned 35 UI-test failures with `No compose hierarchies found`. Read-only ADB
diagnostics showed the phone was secure-locked with `NotificationShade` focused and
no focused application after instrumentation had removed the debug host; reinstalling
the APK did not clear that device precondition. This is an external-device test-state
failure requiring the user to unlock the phone before rerunning, not evidence of a
production Compose assertion failure.

Fresh unlocked Honor instrumentation checkpoint (28 September 2026): after the phone
was unlocked, the full API-35/physical instrumentation suite completed on `PGT-N19`
with 68 tests, zero failures, zero errors, and one explicit SAF-root skip. The compact
Store filter test initially exposed a reachability assertion that did not scroll the
horizontal Blueprint rail; the corrected test now passes in isolation and in the full
run. `BlueprintScreenshotsTest` completed all four production screen tests, including
the compact Store and Settings routes. This is physical phone UI/lifecycle evidence;
external-TV overscan, controller, and DeX soak remain separate hardware gates.

Fresh live SMART TV render checkpoint (28 September 2026): the Honor reported an
active `SMART TV` presentation display at 1920×1080/160 dpi. After reinstalling the
current debug APK, the exported `MainActivity` was placed on logical display 30 and
the window was maximized through Desktop Mode. A captured TV frame shows the wide
Blueprint shell filling the TV surface with Home navigation, the Finish console mode
banner, Galaxy Patrol hero, status rail, quick-launch cards, populated Explore row,
and controller footer. This proves the current wide render on the active TV; it is
not a claim that the shell command launched the non-exported `TvActivity` or that
physical controller/DeX soak is complete.

The same live TV session then navigated to Store through the wide navigation bar.
The captured 1920×1080 frame shows the Blueprint console rail, Featured hero, All /
Installed controls, search panel, source cards, TheGamesDB API-key state, Sync all
consoles, Refresh authorized catalog, and controller footer. This confirms the
populated Store composition and TV navigation on the real presentation surface;
provider-key availability, controller input, and long-duration TV soak remain separate
acceptance gates.

The same session then navigated to Settings on the TV. The captured wide frame shows
the section rail (Storage, Controllers, Downloads, Emulators, Display, Audio, Network,
Saves & Cloud Sync, and System), the App storage summary, interface/app controls, and
the controller footer. This confirms the large-screen Settings composition and section
navigation on the real presentation surface; scroll-depth, external-controller input,
and long-duration DeX/TV recovery remain separate acceptance gates.

Cross-platform post-save-hardening checkpoint (28 September 2026): the complete
Android JVM suite passed 558 tests with zero failures, zero errors, and one explicit
skip after the reversible SAF replacement change. The Windows `GameBox.Windows.Core.Tests`
Release harness also passed. These are software regression gates; the locked-Honor
UI result and live provider/emulator endpoint validation remain separate evidence.

Release metadata checkpoint (28 September 2026): the repository's Android version-code
and APK manifest regression suite ran 8 tests with zero failures. This validates the
pre-signing metadata gate; it is not evidence that protected signing secrets or the
production update channel have been exercised.

Cloud-save loopback transport checkpoint (28 September 2026): the authenticated save
client now has a JVM integration test that opens a real loopback HTTP socket and
round-trips a checksum-protected payload through the actual `HttpURLConnection`
request path. The tests verify PUT/GET framing, Basic and S3/SigV4 authorization,
declared response length, and byte/checksum preservation; the focused transport suite
passed.
This strengthens local transport-boundary evidence only and does not claim acceptance
against a live WebDAV/S3 account or endpoint.

Catalog loopback transport checkpoint (28 September 2026): configured WebDAV/HTTPS
and S3-compatible catalog fetches now have local-socket integration coverage through
the actual `HttpURLConnection` path. The tests verify Basic authorization, SigV4
header emission, UTF-8 response framing, and retryable handling when a server ends
before its declared length. This is deterministic provider-boundary evidence; live
catalog credentials, WebDAV/S3 permissions, and production endpoint behavior remain
separate gates.

Authorized download loopback checkpoint (28 September 2026): the resumable download
engine now has real local-socket integration tests covering configured Basic and
S3/SigV4 auth, HTTP range resume, `Content-Range` validation, checksum verification,
and promotion from staging to the final content path. The fixtures are synthetic/legal
test data; this proves the local download lifecycle only, not availability or
authorization of any external game source.

SAF save replacement hardening (28 September 2026): verified emulator-save restores
now stage an existing document under a unique reversible backup name before promoting
the verified replacement. A failed final rename removes only the new candidate and
restores the prior document; successful restores clean up the backup. The real
DocumentsProvider instrumentation assertion checks that no backup artifact remains
after replacement. This improves save-integrity behavior; provider-specific emulator
formats and physical unplug recovery remain separate validation.

## Current implementation checkpoint (28 September 2026)

Windows release packaging checkpoint: the self-contained `win-x64` publish completed
from commit `540b3dc33883e323790b0e2cda71c5ba579d471b`. The ZIP artifact
`GameBoxWindows-win-x64.zip` is 67,880,382 bytes with SHA-256
`9fded6e9c0edcd2e43793a229d6e703c0a5bcf2eee98b4ef95b641ae76a8b970`; the repository
archive validator passed against its manifest, checksum, and packaged executable. The
unsigned MSIX artifact `GameBoxWindows-win-x64.msix` is 67,204,124 bytes with SHA-256
`66c99ab948c63e4e4aa553679e77e96cfa760bc9b530408978aeea1b1edef0f8`; the MSIX
provenance validator also passed. These are packaging and integrity gates only: signing,
clean installation/upgrade/rollback, update-channel behavior, and physical Windows
acceptance remain separate.

Blueprint UI revision checkpoint: primary navigation now matches the mockup hierarchy
(Home, Library, Store, Media, PC and Settings), while Downloads remains a utility
destination. Automatic layout selection now keeps 16:9 high-density displays on the
wide living-room composition and retains compact behavior for phones and narrow
windows. The fresh production screenshot flow passed on the authorized Honor device
and API-35 emulator after this revision; the wide render was visually compared with
the Blueprint Home composition. The screenshot flow also opens Downloads through
the header utility action, proving the non-primary route remains reachable. This is Android UI/instrumentation evidence, not
physical DeX, external-controller, hover/motion, or complete state-matrix acceptance.

Fresh API-35 visual check (28 September 2026): after reinstalling the current debug
APK and launching the real `MainActivity`, a 1080x2400 emulator capture showed the
Blueprint-style Home composition with the GameBox mark, primary navigation, hero card,
library summary, quick-launch tiles, and discovery cards. The six primary tab bounds,
including Settings, were fully within the compact viewport; no navigation resizing
change was required. This is emulator visual evidence, not a claim of pixel identity
or physical TV/controller acceptance.

Fresh API-35 wide visual check (28 September 2026): the documented temporary 1920x1080
at 160-dpi profile rendered the current Home screen with the Blueprint living-room
composition: centered primary navigation, full-width hero, storage/network/controller
status rail, quick-launch region, dense discovery strip, and controller footer. The
emulator was restored to its original 1080x2400 at 420-dpi phone profile after capture.
This is wide emulator visual evidence, not physical-TV overscan or controller acceptance.

Compact Settings follow-up: external-library Choose/Change folder and Forget actions now
stack full-width on compact layouts, preventing narrow-width and enlarged-text crowding
while preserving the wide TV/Desktop row. The focused production screenshot flow passed
on API-35 after this change, including `compactSettingsKeepsExternalLibraryActionReachable`.
This is emulator UI evidence; physical TV overscan and controller-only acceptance remain
separate.

Save adapter foundation checkpoint: `SaveAdapter` now has a tested bidirectional contract
for adapters that can stream emulator-owned bytes into atomic GameBox snapshots and
restore verified snapshots back through the adapter. `DirectorySaveAdapter` and the
existing `SaveRestoreCoordinator` exercise this contract in JVM coverage, including
backup, mutation, and restore. The SAF-backed emulator adapter, user-selected save-root
implementation now enforces the `<selected-root>/<game-id>/<save files>` convention,
bounded discovery, staged restore, and destination digest verification; its path-policy
tests pass. User-selected save-root configuration, launch-time wiring, and real emulator
save validation are now separated: Settings persists an independent emulator-save SAF
permission, the production launch controller restores a verified snapshot before dispatch,
and the return path snapshots emulator-owned files alongside managed saves. JVM coverage
blocks dispatch on failed configured restore. Managed app-private saves are not being
relabeled as emulator saves.

Real SAF/provider checkpoint (28 September 2026): after selecting the disposable emulator
folder `Download/GameBoxSafTest` through the production Settings picker, the direct
instrumentation run `SafSaveAdapterDeviceTest` passed `OK (1 test)` on API-35. It
discovered `game-a/PSP/SAVEDATA/slot.dat`, read the provider-backed original bytes,
restored replacement bytes through the staged-and-digest-verified path, read them back,
and restored the original fixture in `finally`. This proves the selected DocumentsProvider
round-trip; it is not yet proof of a commercial emulator's gameplay save format or save
recovery after an actual PPSSPP session.

The same focused Settings test passed on API-35 but the connected Honor run did not
produce a valid test result: the instrumentation process crashed and the device then
reported `com.gamebox.os` missing from package-manager queries. This is recorded as a
physical-device/firmware execution failure, not an application pass or a substitute for
the emulator result; the existing Honor Desktop Mode limitation remains open.

Catalog state-matrix checkpoint: `CatalogRefreshStateTest` now drives the production
Store screen through offline fallback, provider-unavailable fallback, and terminal
error states, asserting their accessible live-region descriptions and actionable
Refresh control. The test passes on both API-35 and the connected Honor device after
resetting the device's stale PPSSPP foreground session. This is deterministic UI
state evidence; it does not validate a live provider or airplane-mode transport.

Download UI state checkpoint: `DownloadsScreenStateTest` exercises the production
Download Manager composable through queued, downloading, paused, failed, and completed
jobs. It asserts each card's accessible status description and the appropriate Pause,
Resume, or Retry affordance, with terminal completed jobs exposing no action. The test
passes on both API-35 and the connected Honor device; transfer-engine and live-provider
behavior remain separate evidence.

The production screenshot flow now adapts its Downloads proof to both intended layouts:
wide displays use the Blueprint header utility action, while compact phone layouts use
the controller Quick Actions Downloads entry. The complete API-35 instrumentation suite
passed 64/64 with zero skips after this correction. A subsequent 64-test Honor run could
not execute the accessibility test body because Honor Desktop Mode's package display map
to external display 23 force-stopped the instrumentation process at startup; logcat shows
`HwPCUtils` performing that termination rather than an app assertion. The earlier
phone-only Honor instrumentation evidence and the separate manual TV UI evidence remain
valid and are kept distinct from this firmware-specific runner limitation.

Latest API-35 regression checkpoint (28 September 2026): after the release-boundary
hardening for developer layout, focus diagnostics, and catalog-state simulation, the
complete debug instrumentation suite passed `65/65` with zero skips and zero failures.
This supersedes the earlier 64-test emulator count while preserving the separate Honor
Desktop Mode runner limitation described above.

The subsequent compact Store and stale-discovery-detail UI increments were installed
on the same API-35 emulator. The complete instrumentation suite then passed `66/66`
with zero skips and zero failures, including direct phone console-filter semantics and
the updated Details composition. This remains emulator UI/lifecycle evidence, separate
from the Honor Desktop Mode runner limitation and live provider behavior.

Honor external-display checkpoint (28 September 2026): the connected Honor Magic6 Pro
reported the SMART TV as Android display 23 at 1920x1080/160 dpi. Launching
`MainActivity` explicitly on that display produced a real GameBox task on the TV, and
the captured frame matched the wide Blueprint composition. A display-aware tap on Store
then produced the wide Store screen with the console rail, catalog cards, source status,
and Refresh authorized catalog action. This is now verified external-display UI and
navigation evidence; physical controller, long-duration DeX/TV soak, and gameplay
acceptance remain separate gates.

The PC Hub now exposes an in-product **Show on TV** action. It enumerates Android
presentation displays, launches a separate GameBox task with `ActivityOptions` on the
selected display, and reports a guarded no-display or launch failure message. On the
connected Honor, the action reached the firmware's Relaunch confirmation and then opened
the wide GameBox Home surface on the SMART TV. Honor Desktop Mode presented that task in
a movable window; automatic firmware maximization and physical display/controller soak
remain hardware-specific acceptance items.

External-display host hardening (28 September 2026): the normal `MainActivity` remains
the `singleTask` Android Home host, while the presentation-display route now targets a
separate standard `TvActivity` with the same shared GameBox content and analog-controller
handling. This prevents the Home task contract from colliding with a second display task.
The host split compiles and its launcher regression passes in JVM coverage; the current
Honor Desktop Mode session still requires firmware-level TV confirmation before this
change can be counted as a fresh physical-TV pass.

Fresh dedicated-host hardware probe (28 September 2026): the installed build's in-product
Show on TV action requested `com.gamebox.os/.TvActivity` with display 23 and the expected
new-task/multiple-task flags. Honor Desktop Mode intercepted the request with its
firmware display-switch confirmation (`ActivityTaskManager` result 98); because that
confirmation was not accepted on the TV during the probe, no `TvActivity` task was
created. This confirms the app-side route reaches the firmware boundary, but does not
claim a completed physical-TV render for this fresh host build.
An ADB-sent standard Enter confirmation produced the same firmware result and was
logged as cancelled, so the prompt cannot be accepted through this phone-side probe.

The launcher result now says that the display switch was requested and asks the user to
confirm a system prompt, rather than claiming the TV task is already open. The focused
JVM launcher regression passes after this correction.

PC Hub confirmation guidance checkpoint (28 September 2026): compact and wide PC Hub
layouts now explain that Android/Desktop Mode may require confirmation on the TV before
the external-display task appears. The focused production UI instrumentation class
passed 4/4 on API-35 after this change. This improves the user-facing software flow but
does not convert the Honor firmware prompt into an automated hardware pass.

Per-game managed-save controls are now used by every Details screen, not only the
bundled Galaxy Patrol fixture. The selected game's save controller is also used for
content-removal preflight, so imported PSP/other-console titles no longer risk reading
or mutating Galaxy Patrol's save record. The existing per-game Compose coverage and the
physical Honor instrumentation suite both pass after this integration; emulator-private
save synchronization remains explicitly separate.

The Vimm PSP flow is now regression-covered with a concrete `God of War: Chains of
Olympus` metadata fixture: bounded PSP listing search resolves the numeric vault link,
details hydration preserves title/platform/region/year/artwork, and the result remains
metadata/navigation-only. Multi-console Vimm search now propagates coroutine
cancellation instead of converting cancellation into a reported platform failure. The
complete Android JVM suite and the connected-device instrumentation suite pass after
this change. No Vimm binary is downloaded or extracted.

The follow-up commit `6f6b815` adds an exact PSP metadata regression for vault `23719`
and rejects a Windows companion content confirmation whose game ID does not match the
requested upload. Android JVM tests and the Windows core harness pass; this strengthens
metadata and transfer integrity without treating Vimm as a binary source.

Legal-homebrew adapter checkpoint (28 September 2026): explicit content-path policies
now cover Nintendo DS, GBA, SNES, Mega Drive, Master System, Game Gear, PC Engine,
WonderSwan, and Atari 2600 in addition to the previously exercised families. Focused
and complete Android JVM suites pass, including native-extension/path regressions. This
enables the same authorized-download/import validation harness to be used with a
properly licensed homebrew fixture for each adapter; it does not approve or fetch any
ROM, and source leads remain separate from per-artifact license/checksum evidence.

Wired-TV fullscreen launch checkpoint (28 September 2026): the presentation-display
launcher now carries the external display's real pixel bounds into `ActivityOptions`
and marks the dedicated `TvActivity` fullscreen. The connected Honor reported HDMI
display 8 at 1920×1080 and hosted GameBox's `TvActivity`; before this change the task
was visible but remained a 1312×1037 freeform desktop window. Automated launcher tests
cover the bounds contract. Fresh visual TV acceptance still depends on Honor Desktop
Mode's firmware window manager and must be checked on the actual TV surface.

TV window-policy follow-up (28 September 2026): `TvActivity` now requests landscape,
opts into edge-to-edge rendering, and hides system bars with transient-bar recovery
after Desktop Mode focus changes. This keeps the shared Blueprint 16:9 viewport from
being reduced by phone navigation/status chrome when the dedicated activity owns the
presentation display. Build and instrumentation evidence remain separate from the
firmware-dependent confirmation, overscan, controller, and long-duration TV gates.

Fresh Honor regression checkpoint (28 September 2026): after installing the APK with
the TV window policy, the complete connected Android suite finished with 69 tests, one
expected SAF skip, and zero failures on `PGT-N19` while the wired HDMI presentation
display reported 1920×1080/160 dpi. This is phone-surface instrumentation evidence
with the TV connected; it does not promote the Desktop Mode `canHostTasks=false`
handoff limitation into a successful dedicated-TV visual acceptance.

Dedicated-TV activity instrumentation checkpoint (28 September 2026): a focused
Android test now launches the actual non-exported `TvActivity`, verifies its landscape
request, and asserts the Blueprint Home tab and GameBox mark are rendered. This closes
the automated activity-host coverage gap; it remains distinct from physical HDMI
fullscreen, overscan, controller, and Desktop Mode handoff acceptance.

TV viewport-dimension checkpoint (28 September 2026): the wide Blueprint viewport now
allows a bounded downscale on 16:9 living-room surfaces. At the connected HDMI's
1920×1080/160-dpi geometry (1200×675 dp), the design surface scales to 0.9375 so its
1280×720 composition fits the full TV-safe viewport; compact phone layouts retain a
minimum scale of 1.0. Unit coverage also exercises a 4K-style 2400×1350-dp surface.

TV controller-host checkpoint (28 September 2026): the dedicated `TvActivity` device
test now also sends the controller Start and B actions, confirming that the TV host
opens and closes the shared controller quick-actions overlay. This is runtime Android
input evidence on the Honor; a paired physical controller and HDMI focus/reconnect
soak remain hardware acceptance gates.

Full-suite TV regression checkpoint (28 September 2026): after adding the dedicated
activity/controller tests and the wide viewport scaling policy, the complete Honor
instrumentation run finished 71 tests with one expected SAF skip and zero failures
while HDMI remained connected. The run validates Android integration; it does not
replace physical controller, overscan, reconnect, or Desktop Mode handoff evidence.

TV accessory-status correction (28 September 2026): the connected Honor exposed the
USB optical mouse as `KEYBOARD | DPAD | JOYSTICK`. Runtime status classification now
rejects joystick sources that also identify as mouse/keyboard, preventing the TV
status panel from falsely reporting that accessory as a game controller. Unit tests
cover genuine gamepad/joystick sources and the mixed desktop-accessory case; no
physical game controller was connected during this observation.

Authenticated catalog transport checkpoint: the shared HTTPS/WebDAV/S3 client now
rejects credential types that do not match the selected transport before opening a
network connection. S3 requires access-key/secret-key credentials, while HTTPS and
WebDAV accept only complete Basic credentials. Focused transport/provider tests and the
complete Android JVM suite pass. This is deterministic transport-boundary evidence; it
does not claim acceptance against a live WebDAV or S3 service.

Cloud-save contract checkpoint: `CloudSaveSyncContract` now independently validates the
lowercase game-ID grammar and rejects literal or percent-encoded path traversal in the
resolved object endpoint. This closes the invariant at the request boundary instead of
depending only on the normal endpoint builder. Focused cloud transport tests and the
complete Android JVM suite pass; real WebDAV/S3 round-trip evidence remains separate.

Windows companion protocol alignment checkpoint: commit `2349131` constrains the
Windows content-upload client to the Android route's lowercase ASCII game-ID grammar.
The Windows core harness passes the rejection regression for an uppercase ID, so the
client now fails closed before sending an incompatible request.

Companion maximum-size checkpoint: commit `263ab29` adds an Android HTTP-reader
regression proving a content request above the 64 GiB contract is rejected from its
headers before the body is read. The existing storage-unavailable and staging-space
tests remain separate and continue to cover disk-pressure classification.

Windows storage-pressure checkpoint: commit `ebabb46` maps the Android companion's
HTTP 507 response to a typed retryable `CompanionStorageUnavailableException`, with a
Windows harness regression. The client no longer collapses disk pressure into generic
invalid content data.

Fresh physical Android checkpoint: after the companion size/error-path changes, the
connected Honor Magic6 Pro completed all `62/62` instrumentation tests with zero skips
and zero failures. This confirms the Android integration build remains healthy; it is
not evidence of a live provider, PSP gameplay, or external-controller run.

PSP emulator readiness now recognizes the official PPSSPP Android package variants:
the free `org.ppsspp.ppsspp` build, PPSSPP Gold `org.ppsspp.ppssppgold`, and PPSSPP
Legacy `org.ppsspp.ppsspplegacy`. They use the same documented scoped-content launch
 contract and are covered by package-selection and intent-policy tests. The connected
 Honor now has the free PPSSPP package installed; real PSP gameplay remains a separate
 device/emulator gate because the legal PSP fixture tested so far was rejected by
 PPSSPP's identification reader and no commercial game was loaded.

After rebuilding with the PSP adapter change, the selected physical Honor Magic6 Pro
instrumentation run completed `62/62`, zero failures, zero skips. `ANDROID_SERIAL` was
used so this result is explicitly the Honor hardware result rather than an emulator run.

Physical PPSSPP startup checkpoint (27 September 2026): the official PPSSPP v1.20.4
Android APK from `ppsspp.org` was installed on the connected Honor as
`org.ppsspp.ppsspp` (APK SHA-256
`dd702a31270ecd68db37afee311d6efe08b1d4da9601d2750ce1a73bbd316c3f`).
`org.ppsspp.ppsspp/.PpssppActivity` reached the resumed foreground state and no fatal
exception appeared in the captured logcat window. No commercial game or ROM was loaded;

PPSSPP handoff correction checkpoint (27 September 2026): the launch policy now uses
the documented Android `ACTION_VIEW` contract with the scoped content URI instead of
placing the game path in PPSSPP's launcher `Args` extra. The Android JVM suite and the
Honor instrumentation suite both pass after this change. A legal PSP demo from the
PPSSPP-linked PlayDreamCreate archive was transferred to the Honor (ZIP SHA-256
`825f9d840be7ba279e36aadc1875a1fae51ac7215c886ba326ee4f6896ca2b29`; extracted
`EBOOT.PBP` size 6,407,365 bytes) and launched through both direct PPSSPP and
`ACTION_VIEW` probes. PPSSPP reached its foreground activity but displayed `Failed to
read identification bytes` for that fixture. This is recorded as a real-device
content-compatibility failure, not PSP gameplay evidence; the GameBox scoped-content
handoff correction remains covered by policy tests and requires another legal PSP
fixture for successful gameplay validation.
this is emulator installation/startup evidence, not PSP gameplay or save compatibility.

Legal PSP homebrew compatibility checkpoint (27 September 2026): the SourceForge
project identified as “Zelda - Oni Link Begins (PSP Game)” was downloaded through its
direct project host (ZIP SHA-256
`d7fd341b1f22f65c0d770fa7943091ea541ec8df2feef0b0399664ad99313f1c`; extracted
`EBOOT.PBP` SHA-256
`936e74eadd867011cbf3d2a000417b709aca665beccc9163b00a2175ab8cd8bd`). The folder was
visible in PPSSPP's SAF-backed PSP browser on the Honor, but launching the EBOOT failed
with PPSSPP's `Could not find executable ms0:/PSP/GAME/ZELDAOLB/EBOOT.PBP` dialog.
This is a real legal-fixture compatibility failure, not gameplay evidence; no
commercial title was accessed.

Windows validation checkpoint (27 September 2026): the installed .NET SDK/runtime
`8.0.314`/`8.0.17` ran the complete `GameBox.Windows.Core.Tests` harness successfully.
The companion then published self-contained for `win-x64`; the generated ZIP and unsigned
MSIX both passed the repository checksum and provenance validators. This is local Windows
software/package evidence, not signed-install or physical Windows acceptance.

Windows catalog transport hardening (28 September 2026): the companion's HTTPS catalog
sync now rejects response bodies shorter than their declared `Content-Length` before JSON
parsing, matching the Android cloud/catalog transport integrity boundary. The complete
Windows core harness passed after this change; it remains automated Windows evidence,
separate from live provider and physical Windows validation. The same bounded-response
guard now covers managed-save and owned-content confirmations, with harness coverage for
short save/content bodies before their JSON confirmations are trusted. Status, library,
and configuration response readers now enforce the same declared-length invariant, with
the complete harness covering short status and configuration bodies as well.

Android release packaging checkpoint (28 September 2026): `:app:assembleRelease` passed
locally and produced the unsigned preview APK
`app-release-unsigned.apk` (14,526,677 bytes,
SHA-256 `e5d0409be9278cbef5fc092f0700d9efd95f7b80cc9c187e987ad70a23d3fad1`). This is
release-variant compilation/package evidence only; protected signing, installation,
upgrade, rollback, and update-channel execution remain separate gates.

Updated Android release packaging checkpoint (28 September 2026): after the compact
Details action revision, `:app:assembleRelease` passed again and produced the unsigned
APK `app-release-unsigned.apk` (14,526,677 bytes,
SHA-256 `fe61daa5b49cfb33a752ddc4dd373ab6c1dd709238bdde26d868146eeb623831`) from
source commit `fbee34f`. The Windows companion Release core harness also passed in the
same milestone run. These are current cross-platform build/test results; signing,
installation, upgrade, rollback, and update-channel execution remain separate gates.

Fresh Android release packaging checkpoint (28 September 2026): `:app:assembleRelease`
passed again from the current tree and produced `app-release-unsigned.apk` (14,543,093
bytes, SHA-256
`1265daef5b0592f90a7c61182337f9dde024a946389e51f5fbf9848ce0133b0c`). `apksigner`
correctly reported that this unsigned preview does not verify; no production signing
secrets are configured. The Windows Release core harness also passed independently in
the same continuation. This confirms compilation/package integrity only, not trusted
installation or update-channel readiness.

Emulator handoff URI hardening (2026-09-28): the allowlisted PPSSPP, Dolphin,
RetroArch, and fallback intent policy now requires a scoped `content://` URI with a
non-empty document path, in addition to rejecting credentials, query strings, and
fragments. The focused policy suite and complete Android JVM suite pass (567 tests,
zero failures, zero errors, one explicit skip). This strengthens launch-input
validation; it does not claim gameplay or emulator-private save compatibility.

Release provenance hardening (27 September 2026): alpha, beta, combined preview, and
protected production APK workflows now use the same tested manifest generator. It accepts
the PREVIEW channel and records the checked-out source commit plus signing status alongside
artifact name, size, SHA-256, and rollback metadata where required. This removes the
previously duplicated inline preview/release manifest schemas; protected signing and
trusted installation remain release gates.

The repository baseline is now extended through local commits `0b12fb6`, `ea2bb9f`, and
`a92bacc` after the Vimm's Lair discovery increment. Configured sources
can probe the bounded HTTPS listing structure, search supported consoles, hydrate
metadata, and route users to legal local import or the external vault page. This remains
metadata/navigation only; it does not authorize binary downloads. The Windows companion
also rejects oversized content-upload confirmations while streaming the response through
a bounded buffer, closing a peer-controlled memory-growth path. Android Studio, the
Android SDK, the Honor device transport, and the .NET 8 SDK are now available locally
for executable verification.

The committed Vimm follow-up adds a five-minute bounded in-memory page cache and
deterministic exact/prefix/word-prefix result ranking. It caches only successful bounded
HTML responses and preserves the metadata-only legal-source boundary.

Live Vimm metadata check (28 September 2026): a bounded read-only HTTPS request using
the GameBox user agent returned HTTP 200 for `https://vimm.net/vault/PSP/G`, with 66
numeric vault links. Parsing the public listing found the current title
`God of War: Chains of Olympus` at vault `27934`; no media or download endpoint was
requested. A follow-up bounded request to `https://vimm.net/vault/27934` returned HTTP
404, so the listing/detail inconsistency is recorded as a provider-side stale-link
condition. GameBox must keep the result navigational/metadata-only and surface detail
failure rather than treating the stale link as an install source. This is live provider
evidence, not binary-download, gameplay, or save-compatibility evidence.

Physical Android validation checkpoint (27 September 2026): the authorized Honor Magic6 Pro
Wi-Fi transport now passes the complete 61-test debug instrumentation suite with zero
failures and zero skips. The earlier local capability-limited run exposed a production focus defect: the root
Compose container handled controller key events but did not request focus on a physical
phone. The root now owns an initial focus target. This is physical UI/controller evidence,
and the same device now also has a verified real emulator gameplay/lifecycle run below.

Honor Desktop Mode smoke checkpoint (28 September 2026): the current debug APK was
installed on the authorized Honor Magic6 Pro while its SMART TV presentation display was
connected over Wi-Fi. A direct display-23 launch reached the expected Honor relaunch
confirmation, and accepting it opened GameBox in a responsive wide window on the TV. The
captured surface shows the Blueprint Home navigation, Galaxy Patrol hero card, storage/
network/controller/device status cards, and PC quick-launch tiles. This validates the
in-product external-display route at smoke-test level; long-duration Desktop Mode,
external-controller, hub, and reconnect soak acceptance remain separate hardware gates.

Direct physical rerun (27 September 2026): the rebuilt debug APK and instrumentation APK
were installed over the authorized Honor Magic6 Pro Wi-Fi ADB serial
`adb-A54J023318002698-YK2vvO._adb-tls-connect._tcp`, and `am instrument` completed the
full 61-test suite with `OK (61 tests)`, zero failures, and zero skips. This is a fresh
physical-device instrumentation result; it is kept separate from the API-35 emulator
run and from live RetroArch gameplay evidence.

Local emulator validation checkpoint (27 September 2026): after installing the official
Android command-line tools and creating the `GameBox_API35` AVD from the Google APIs
x86_64 image, the same complete 61-test instrumentation suite passed with zero failures
and one explicit permission-gated skip. A separate API 37.1 Play Store AVD booted successfully but its 30 UI
failures were all the Espresso `InputManager.getInstance` incompatibility, so that run
is not treated as app-failure evidence. Neither emulator run proves real game execution,
controller input, save ownership, or uninstall/reinstall recovery.

The API-35 emulator now also has RetroArch 1.22.2's official universal Android package
installed and initialized, with the `Nintendo - NES / Famicom (FCEUmm)` core installed
through RetroArch's own Online Updater. This closes the missing-frontend/core setup gate.

Update-available UI checkpoint (28 September 2026): the Blueprint's `UPDATE_AVAILABLE`
state now exposes an explicit **Update** primary action and controller X label. For a
configured remote source it enters the remote download/install workflow; when no source
is available it opens Downloads for recovery instead of launching stale installed content.
The JVM action-plan coverage now protects this distinction from regressing.

Compact Store adaptation checkpoint (28 September 2026): phone layouts now label the
screen as **Store**, describe the authorized/local-first boundary in the subtitle, and
expose a horizontally scrollable console rail directly above the search and metadata
filters. This keeps the Blueprint's console-first browsing model reachable without
opening a secondary filter flow. The new production activity test passed on API-35 as
`OK (1 test)`; wide-layout and physical TV evidence remain covered separately.

Stale discovery-detail recovery checkpoint (28 September 2026): configured-source detail
hydration failures now remain visible after the fallback result opens Details. The
accessible notice identifies a possible stale provider listing and preserves only the
source navigation and legal local-import actions. Android JVM coverage and the focused
API-35 production activity test pass after this change.

Compact Details action checkpoint (28 September 2026): discovery imports and the main
installed/authorized primary action now occupy a full-width row on compact layouts,
while wide layouts retain the Blueprint side-panel/action composition. The production
API-35 screenshot flow passed `OK (1 test)` after the change; this is emulator visual
evidence and does not replace physical TV/controller acceptance.

Live API-35 gameplay/lifecycle checkpoint (27 September 2026): GameBox installed the
bundled hash-verified Galaxy Patrol fixture, reported a successful verified-asset worker,
and opened the real `com.retroarch/.browser.retroactivity.RetroActivityFuture`. The
visible game screen was rendered by FCEUmm (`1.22.2 - FCEUmm ((SVN) 7a542da)`), and
hardware-key events were dispatched during the running session. RetroArch created
`/sdcard/RetroArch/states/FCEUmm/galaxy-patrol.state` plus its preview. Close Content and
Quit returned to GameBox; content-only uninstall removed the installed game file while
the emulator state remained; reinstall restored the app-private Galaxy Patrol file and
relaunch opened the real RetroArch activity again. Loading slot 0 after reinstall showed
`Loaded state from slot: 0`, and logcat recorded execution of the installed FCEUmm core.
This is real emulator gameplay and emulator-save continuity evidence, separate from the
managed GameBox save/cloud contract and from physical-device adapter validation.

Physical Honor Magic6 Pro gameplay/lifecycle checkpoint (27 September 2026): the official
RetroArch 1.22.2 ARM64 package launched on the connected `PGT-N19` device, and FCEUmm was
installed through RetroArch's Online Updater. GameBox installed Galaxy Patrol, launched
the real `RetroActivityFuture`, rendered the game screen, accepted controller key events,
and created `/sdcard/RetroArch/states/FCEUmm/galaxy-patrol.state`. Close Content and Quit
returned to GameBox. GameBox's content-only uninstall removed the 49,168-byte game file
while retaining saves and external emulator files; the state file remained. Reinstall
restored `files/installed/retro/galaxy-patrol/content/galaxy-patrol.nes`, relaunch opened
RetroArch again, and loading slot 0 displayed `Loaded state from slot: 0`. This is real
physical-device gameplay and emulator-save continuity; it remains separate from managed
GameBox save/cloud synchronization and external-controller/display soak testing.

Physical Honor companion-import checkpoint (27 September 2026): the authenticated Windows
companion content route streamed the 49,168-byte legal Galaxy Patrol fixture directly into
the Honor app sandbox with a matching SHA-256. The Store details page reported installed
content and the physical Play action opened RetroArch's real activity with the imported ROM
rendering on the phone; DPAD/button events were dispatched and RetroArch reported
`Saved state to slot: 0`. RetroArch's own Close Content and Quit actions returned to GameBox.
The supported content-only uninstall confirmation reported one 49,168-byte file and that
saves/backups/metadata/history and external emulator files would be retained. After the
confirmation, Android showed `Game content · not installed` and `1 managed save files · 0.0
MB`; a direct `run-as` check found no imported ROM and retained `files/saves/galaxy-patrol/save.dat`
with SHA-256 `17c75cabe91d5339d4237d4ccfca3a20d4eb78ff357355a274df8162cdac3c24`. This is
the first physical proof of companion-imported content reaching real gameplay and the
save-safe cleanup boundary; it remains separate from cloud-provider and external-hardware
acceptance.

Routed-TV fullscreen checkpoint (2026-09-28): because Honor Desktop Mode routes the
supported Show on TV flow into `MainActivity`, that host now applies the external-display
landscape/system-bar policy when its actual display is non-default. The rebuilt APK was
installed on the Honor and the real HDMI task reported a fullscreen 1920x1080 app
configuration and frame at 160 dpi; the TV hierarchy confirmed the GameBox package, Home,
and logo. Phone/default-display behavior remains outside this policy.

Routed-TV cross-screen checkpoint (2026-09-28): the same rebuilt `MainActivity` was
navigated on the real HDMI display to Store through the supported destination deep link.
The app window remained `[0,0][1920,1080]`, and the TV hierarchy confirmed `GAMEBOX OS /
STORE`, GameBox package ownership, and the Blueprint controller footer. The task was
returned to Home afterward; this confirms the routed fullscreen policy across navigation,
not only initial launch.

TV gameplay-display handoff checkpoint (2026-09-28): emulator launch requests now carry
the GameBox host's non-default display ID through `GameLaunchController` and
`PackageGateway`, using `ActivityOptions.launchDisplayId`; default-display launches keep
the existing context-start behavior. JVM coverage verifies display 8 forwarding, the
Android test APK compiles, and the rebuilt APK retained a fullscreen 1920x1080 HDMI
GameBox Home frame on the Honor. This proves display-aware handoff plumbing, not live
emulator gameplay, game compatibility, or physical-controller acceptance.

TV display-status clarity checkpoint (2026-09-28): runtime status now distinguishes a
physical display from virtual capture surfaces when both are present. For example, the
connected setup reports `HDMI Screen · 1920×1080 · 1 virtual display` instead of the
ambiguous `2 external displays`; single-display and all-physical fallbacks remain
unchanged and are JVM-tested. The rebuilt APK installed successfully and the real HDMI
hierarchy still confirmed the GameBox package, Home, and logo.

Physical HDMI status-section checkpoint (2026-09-28): using the supported Settings deep
link and the TV hierarchy's discovered section bounds, the real display 8 task opened
Settings and selected Emulators. The HDMI hierarchy confirmed `External display
detected`, `HDMI Screen`, and `1920×1080` from the rebuilt APK, alongside the GameBox
package and controller footer. This is direct TV user-flow evidence for display-status
reporting; it does not claim physical controller input or overscan acceptance.

TV display-change policy checkpoint (2026-09-28): `TvActivity` now reapplies its
landscape and system-bar policy when its own external display reports a configuration or
mode change, while ignoring changes on the default or an unrelated display. JVM coverage
protects the targeting rule; the rebuilt API-35 `TvActivityTest` class passed `OK (4
tests)`. This strengthens 1080p/4K and Desktop Mode transitions without claiming a
physical cable reconnect or controller soak.

The current debug APK was also installed on `GameBox_API35` and launched through the
real `com.gamebox.os/.MainActivity` entry point. The process remained alive, the
activity stayed resumed, and the crash buffer was empty after startup. The later live
RetroArch run above extends this from application-start evidence to one complete
Galaxy Patrol emulator lifecycle.

Companion JSON responses now share control-character escaping for device names and
transferred filenames, with JVM coverage for status serialization. This keeps the LAN
protocol parseable for valid user metadata without expanding the content-transfer trust
boundary. Companion content replacement now also recovers an orphaned previous copy
before retrying, preserving the old installed content when a staged replacement fails.
Catalog failure simulation is now debug-only: release builds always consume the real
catalog refresh state, while debug instrumentation retains the explicit offline,
provider-fallback, and error-state controls used by the UI matrix. The boundary is
covered by JVM and API-35 instrumentation tests. The same release boundary now ignores
persisted layout-forcing and focus-diagnostics overrides, preventing developer settings
from changing production responsiveness or exposing debug overlays.
The catalog transport failure double is now test-only; the Android production source no
longer contains a `NoopCatalogTransportClient` placeholder. The complete Android JVM
suite was rerun after this cleanup: 517 tests, zero failures, and one explicit
privilege-based skip.
Windows companion status, library, save, and content clients now normalize incomplete
or malformed JSON confirmations into bounded invalid-data failures, with console-test
coverage for missing response fields. The shared bounded retry policy now also classifies
HTTP 408, 429, and 5xx responses as transient while authentication and malformed payload
failures remain non-retryable (including the .NET InvalidDataException/IOException subtype
boundary). Managed-save downloads now use that policy; save uploads
and identical content replacements are safe to replay because Android treats repeated
checksummed uploads as no-ops. Windows now exposes the same retry entry points for
idempotent favorite and configuration updates. Android save uploads with an identical
SHA-256 are now true no-ops that repair the managed-save metadata row without advancing
the timestamp or creating another conflict artifact, allowing the Windows client to
retry save uploads safely after transient LAN failures. Owned-content replacement now
also treats an identical streamed SHA-256 as a no-op, preserving replacement backups
only when the incoming content differs; the Windows client now retries those identical
content uploads after transient LAN failures as well.
Cloud-save downloads now reject a response that ends before its declared HTTP length,
classifying the incomplete body through the shared retryable transport policy rather
than accepting a truncated payload when no checksum is available. JVM coverage verifies
the disconnect and recovery classification; live WebDAV/S3 endpoint evidence remains
separate.
The configured HTTPS/WebDAV/S3 catalog transport now applies the same declared-length
check, classifying truncated catalog bodies as transient provider failures while keeping
oversized responses permanent. Focused JVM coverage protects both classifications.

The PC now has the official .NET 8.0.314 SDK installed, including the .NET 8 runtime.
The Windows core executable test harness and self-contained `win-x64` publish both pass
locally, and the harness now includes the documented
read-only `--live-companion <host> <port> <pairing-secret>` probe for repeatable LAN
status/library validation, plus a separate `--live-companion-discovery <host> <port>`
probe for nonce-correlated UDP discovery and an idempotent
`--live-companion-configuration <host> <port> <pairing-secret>` configuration
round-trip probe. Its unknown-length HTTP test
content was made compatible with the .NET 8 `Task` override contract by converting the
stream write `ValueTask` to a `Task`.
The same repository harness now exposes an explicitly mutating
`--live-companion-save <host> <port> <pairing-secret> <game-id> <payload>` probe. It
uploads a small managed-save payload through the real authenticated Android route,
downloads it again, and verifies the exact bytes and checksum; the command documents
that its test save remains on the paired device until removed through the product flow.
It also exposes `--live-companion-content <host> <port> <pairing-secret> <game-id>
<content-path>` for a streamed, checksum-verified upload of a user-authorized local
content file. This probe intentionally registers content on Android and must be
followed by the supported uninstall/cleanup flow.
The same harness now verifies that caller cancellation before an owned-content upload
prevents network dispatch and retry; cancellation during a real long-running LAN
transfer now also has deterministic client coverage: a streaming HTTP boundary reads
part of the request body, blocks, and confirms cancellation propagates after network
dispatch without entering retry. Cancellation during a real long-running LAN transfer
remains separate acceptance evidence.

The Windows pairing profile boundary now rejects missing or null protected-secret fields as
bounded `InvalidDataException` failures instead of leaking Base64 argument errors. The
Windows harness covers DPAPI round-tripping for the current user, plaintext exclusion,
forget cleanup, malformed JSON, and incomplete persisted profiles; real cross-user DPAPI
recovery remains a separate acceptance gate.

Windows CI now also packages the self-contained companion as an unsigned MSIX beside
the portable ZIP. The package is built with the Windows SDK `makeappx.exe`, carries
generated visual assets, and is checked with a SHA-256/provenance validator. The
protected production workflow now derives the MSIX publisher from the configured PFX,
signs and verifies the MSIX, and uploads its signed checksum/provenance alongside the
ZIP. Local unsigned packaging and validation pass; executing the protected workflow,
installation acceptance, and physical Windows validation remain separate release gates.

Local MSIX trust checkpoint (27 September 2026): a self-contained `win-x64` publish was
packaged and successfully signed with a temporary development certificate. Windows
`Add-AppxPackage` correctly refused installation because that self-signed root was not
trusted. The temporary certificate and private key were removed rather than changing the
machine or current-user root trust stores. This validates the local package/signing
boundary without claiming production signing or installation acceptance.

The protected Android production workflow now runs the release-manifest unit tests,
creates a schema-compatible APK provenance manifest from the signed artifact, requires
the distinct rollback tag in that manifest, and uploads the manifest alongside the APK
and checksum. It also verifies the generated checksum file before publication. Local
Python is not installed on this Windows host, so the new script's automated execution
remains a CI-side validation rather than local evidence.

The AWS Signature V4 signer now canonicalizes already-escaped S3 object paths without
double-encoding them, encodes literal plus and Unicode key bytes, and sorts encoded
query keys and values according to the URI signing rules. JVM coverage exercises these
cases; authenticated S3 endpoint acceptance remains separate live-service evidence.

Cloud transport cancellation hardening (27 September 2026): blocking
`HttpURLConnection` work now runs on an independent IO worker behind a cancellable
coroutine boundary. Cancelling an upload or download disconnects the active connection
and terminates the worker instead of waiting for the socket timeout. A deterministic
blocking-stream regression covers the disconnect path; the complete Android JVM suite
passes with 517 tests, zero failures, zero errors, and one explicit skip. This remains
transport-boundary evidence, not proof of a live WebDAV/S3 account or production
emulator save compatibility.

Catalog transport cancellation hardening (27 September 2026): authenticated HTTPS,
WebDAV, and S3 catalog fetches now disconnect their active blocking connection when
the caller cancels. A deterministic blocking-connection regression covers this path,
and the physical Honor instrumentation rerun completed with `OK (62 tests)`, zero
failures, and zero skips. Live provider acceptance remains separate evidence.

Discovery-provider cancellation hardening (27 September 2026): the configured GameBox
JSON manifest transport and the Vimm's Lair metadata/navigation transport now share the
same bounded cancellable HTTPS reader. Cancelling either active request disconnects its
socket instead of waiting for the read timeout; JVM regressions cover both production
transport entry points. The Honor rerun above also covers the resulting Android build,
while real provider availability and live-service acceptance remain separate evidence.

TheGamesDB cancellation hardening (27 September 2026): authenticated catalog transport
and optional metadata enrichment now use the same cancellable HTTPS boundary. Metadata
lookups rethrow coroutine cancellation instead of converting it into an empty result;
catalog transport retains its existing auth, rate-limit, transient, and permanent
failure classification. Two JVM regressions cover active-socket disconnect and
metadata cancellation propagation. The complete local Android JVM suite now passes
with 521 tests, zero failures, zero errors, and one explicit privilege-based skip;
the rebuilt APK also passed the complete physical Honor suite with `OK (62 tests)`.

Authorized-download cancellation hardening (27 September 2026): the WorkManager-facing
remote download path now runs the blocking resumable transfer behind a cancellable IO
boundary and closes the active response stream when the worker is stopped. This preserves
the existing range-resume, checksum, size-limit, and atomic-promotion behavior while
preventing a stalled socket read from delaying cancellation until its read timeout. A
blocking authorized-transfer regression verifies stream closure; the complete local
Android JVM suite now passes with 522 tests, zero failures, zero errors, and one explicit
privilege-based skip. The rebuilt APK also passed the complete physical Honor suite with
`OK (62 tests)`. Live provider transfer and process-kill/WorkManager acceptance remain
separate evidence.

Per-game mutation serialization (27 September 2026): remote download workers and
authenticated companion content imports now join the same process-wide per-game mutex
already used by content removal and the UI reimport path. A download or companion
replacement therefore cannot interleave its staging/promotion and library publication
with uninstall or another same-game replacement. The complete local Android JVM suite
passes with 522 tests, zero failures, zero errors, and one explicit privilege-based
skip; the rebuilt APK passed the complete physical Honor suite with `OK (62 tests)`.
This closes the software lock boundary, while cross-worker stress and process-death/
power-loss acceptance remain separate evidence.

Managed multi-artifact save snapshots now publish each complete backup set under a
unique generation directory and atomically switch the manifest only after every artifact
has succeeded. Restore follows the active generation, while legacy version-1 manifests
remain readable. JVM coverage exercises generation round-tripping, legacy decoding, and
restore through the active generation. Incomplete or failed manifest publications now
discard only their newly-created generation, leaving the active generation intact. Real
emulator save compatibility and cloud
endpoint acceptance remain separate evidence.

The rebuilt debug APK was installed on the authorized Honor Magic6 Pro over Wi-Fi ADB.
The physical Network settings flow enabled the Windows Companion on port 49500 and
generated a runtime pairing secret. A local .NET 8 client then connected to the phone
over `192.168.1.2:49500`, authenticated successfully, received `GameBox / ready /
protocol 1`, and read all 8 library entries. This is real Android-to-Windows LAN
evidence, reproduced through the repository probe rather than a temporary script. The
same repository harness also completed nonce-correlated UDP discovery and resolved
`GameBox` at `192.168.1.2:49500`; discovery and authenticated status remain separate
evidence. The idempotent configuration round-trip also returned the exact existing
flags, with no device setting change. The same session then toggled and restored a real
library favorite, uploaded
and downloaded two managed save payloads (the second upload preserved the first as a
conflict), and uploaded a checksummed content file for `galaxy-patrol`. The live
mutation responses and returned hashes matched the Windows client checks. The phone
now contains the intentionally created test save state and its verified local backup;
the uploaded content was removed during the cleanup rehearsal. This is real debug LAN
mutation evidence, not signed-build or gameplay/save-recovery proof.

The live test content was then removed through the product's supported uninstall flow on
the Honor device. GameBox created the verified local backup and retained the managed save,
as designed; no test content file remains. The same physical flow exposed and fixed a
cloud-preflight message interpolation defect that had rendered `${safeCloudError(it)}`
literally. `GeneralContentRemovalTest` now asserts the real failure message shape, and
three focused instrumentation tests passed on the Honor device.

Reproduction checkpoint after the debug APK was reinstalled on the Honor (27 September
2026): the Windows Companion switch was initially disabled, so port 49500 correctly
refused a TCP connection. Enabling it through the physical Network settings flow made
the endpoint reachable again. The repository's .NET 8 probes then completed nonce-
correlated UDP discovery, authenticated status (`GameBox / ready / protocol 1`), an
8-entry library read, and an authenticated configuration round-trip. The freshly
installed app has no retained Galaxy Patrol installation, so the live library correctly
reports the eight catalog entries as `NOT_INSTALLED`; this is a clean-install state and
does not replace the earlier physical gameplay or live mutation evidence above.

Live managed-save checkpoint from the same session: the new repository
`--live-companion-save` probe uploaded and downloaded a 23-byte `galaxy-patrol` payload,
verified the exact returned bytes and SHA-256
(`17c75cabe91d5339d4237d4ccfca3a20d4eb78ff357355a274df8162cdac3c24`), and the
follow-up authenticated library read reported `savePresent=true` for Galaxy Patrol.
This validates the real Windows client ↔ Android managed-save route, while content
installation and emulator-owned saves remain separate lifecycle evidence.

Honor Desktop Mode routing check (28 September 2026): the authorized Honor Magic6 Pro
remained connected over Wi-Fi ADB, reported the expected 1312x2848 phone display and
the external logical display 23, and accepted an explicit `MainActivity` launch. The
activity was alive in the task manager while the phone screenshot showed the Honor
launcher because the foreground GameBox window was routed to the connected TV. This
confirms platform display routing rather than an app crash; the TV-window screenshot
remains the authoritative external-display UI evidence. A phone-only UI pass is not
claimed from this capture.

Live owned-content checkpoint from the same session: the new repository
`--live-companion-content` probe streamed the repository's legal Galaxy Patrol fixture
to the Honor device. Android confirmed `galaxy-patrol.nes`, 49,168 bytes, and SHA-256
`97c1757ffd6a5bc1a591809b2b0f8988741f61f6abd82889c148ecae8a2f471f`; the subsequent
authenticated library read reported `INSTALLED` and `savePresent=true`. A direct
app-sandbox check confirmed the same byte length and digest. This is live content
transfer evidence, not proof of gameplay or content cleanup/recovery.

Live owned-content replacement checkpoint (27 September 2026): the same repository
probe uploaded that exact fixture a second time through the authenticated Honor route.
The second confirmation returned the same 49,168-byte length and SHA-256, while the
authenticated library remained `INSTALLED` with `savePresent=true`. GameBox's supported
uninstall flow then removed the replacement-test ROM; a direct app-sandbox check found no
content file and retained the managed save with SHA-256
`17c75cabe91d5339d4237d4ccfca3a20d4eb78ff357355a274df8162cdac3c24`. This is live
idempotent replacement and cleanup evidence; it does not cover large-file, cancellation,
disk-full or routed-network conditions.

Live larger-content checkpoint (27 September 2026): the Windows harness's bounded
`--live-companion-content-repeat` probe created a temporary 128-repeat authorized fixture
and streamed 6,293,504 bytes through the authenticated Honor route. The client and Android
returned SHA-256 `41ff91ad3ec898d9b75c28410f9a5137fe8f462898c9d0960a38a972bcd437d2`.
The temporary imported file was then removed through the supported uninstall flow; the
app import directory was empty and the managed save retained its expected SHA-256. This
is larger-body streaming evidence, not maximum-size, cancellation, disk-full or routed-
network acceptance.

Live cancellation checkpoint (27 September 2026): the Windows harness's
`--live-companion-content-cancel` probe started a 25,174,016-byte authenticated upload to
the Honor and cancelled it after 100 ms. The client surfaced cancellation; Android's
app-sandbox listing contained no imported content or `.companion-partial` staging file,
and the managed save retained SHA-256
`17c75cabe91d5339d4237d4ccfca3a20d4eb78ff357355a274df8162cdac3c24`. This is real
long-running LAN cancellation and staging-cleanup evidence; maximum-size, disk-full and
routed-network conditions remain separate.

The local Windows core harness now also streams a real 64 MiB temporary fixture through
the production content client, consumes the complete request body, and verifies the
returned SHA-256 confirmation. The harness passes this local boundary check; it does
not replace the 64 GiB maximum-size transfer against a real Honor endpoint.

The local Android JVM suite then exposed a host-capability failure in
`DirectorySaveAdapterTest`: Windows symbolic-link creation is denied without the
required privilege. The test now classifies that specific capability absence as a
skip while retaining full symlink coverage on capable CI hosts. The complete local
suite passes with 517 tests, zero failures, and one explicit privilege-based skip;
the Windows .NET core suite also passes.

## Companion HTTP connection recovery (EXT-14)

- Fixed the request-header terminator: the previous `return@repeat` continued reading
  instead of finishing headers. A normal HTTP client waiting for its response could
  therefore time out and stop the listener.
- Production service now uses the same connection handler exercised by socket tests.
- Request heads are bounded to 16 KiB, individual lines to 4096 bytes and headers to
  32. CRLF, header names, duplicates and body framing are validated. This endpoint
  accepts body-free requests and closes each connection after its response.
- Header reads have a total deadline as well as socket read timeouts. Timeout, EOF,
  invalid request and peer-reset handling is connection-local; diagnostics do not
  echo request headers or pairing secrets.
- Large content uploads preflight the staging directory's usable space before reading
  the body and return structured HTTP 507 storage guidance when the device cannot
  retain both the staged request and the managed-import copy required for promotion.
  This is deterministic failure handling, not a real disk-full device acceptance result.
- Preserves the existing `X-GameBox-Authorization` protocol used by Windows.

Local validation: the focused connection, protocol and status tests pass in the complete
Android JVM suite:
`CompanionHttpConnectionTest`, `CompanionProtocolTest`, `CompanionStatusRouteTest`.
The connection test opens actual loopback sockets, does not half-close the client,
and checks 401, incomplete-request 408, malformed-request 400, then authenticated 200
on the same listening socket. Parser tests cover truncation, line endings, invalid
and duplicate headers, line/head/header-count limits and unsupported request bodies.

Merged in PR #276 after Android unit/build, instrumentation, phone/DeX screenshots
and Windows build checks passed. The connected-device lifecycle regression now starts
the foreground service, authenticates a loopback status request, stops it, and restores
the prior settings. The separate Honor↔Windows live probe above validates the client
against the Android endpoint. These checks still do not replace routed-network,
long-transfer, or production-signed acceptance.

## Companion transport confidentiality and server identity

The Android companion endpoint now serves TLS using a persistent Android Keystore
server key. Windows companion requests use HTTPS and pin that exact certificate's
SHA-256 fingerprint; the fingerprint is shown in Android Settings and is required
when pairing/saving a Windows device profile. Old profiles without a pin are not
auto-reconnected and must be paired again. A focused instrumentation test passed on
the physical Honor PGT-N19, verifying pinned HTTPS status and rejection of plaintext
HTTP. The Windows core harness and Android unit/lint/APK gates pass. Routed LAN,
large-transfer, Windows↔Honor live-pairing, and physical controller acceptance remain
unverified. Request replay protection still uses in-memory nonce history, which resets
on service restart.


## Nearest-neighbor controller focus recovery (UI-42)

Game-card focus now persists both the stable game ID and its ordered position for each
primary destination. When the focused title is removed by uninstall/catalog changes,
the UI first selects the title that moved into the same position, or the preceding
title when the removed card was last. Empty collections retain no invalid focus target.
The position survives activity/process state restoration while older saved-state data
continues to decode. JVM tests cover middle removal, end removal after serialization,
stable repeated restoration, empty collections, and legacy focus state.

## Missing-content repair and safe forget (FILE-15)

Imported titles whose files disappear now expose two explicit Details actions. **Locate
files** reuses the checksum-verified single/multi-file reimport path. **Forget** requires
confirmation, then atomically clears only stale imported-file references and changes the
title to not installed. The guarded database update refuses installed, remote-download,
or concurrently changed records and retains saves, backups, metadata, artwork, favorites,
emulator preferences and play history. Instrumentation covers the confirmation UX and
Room integration covers accepted, repeated and rejected resets plus retained fields.

## Persistent PC streaming host configuration

Both compact and wide PC Hub layouts now restore the configured Moonlight/Sunshine host
and port from DataStore. **Check and save host** validates and persists DNS, IPv4, or
bracketed IPv6 input before the bounded reachability probe; unreachable hosts remain saved
for a later retry. **Clear saved host** removes both values. Schemes, paths, credentials,
whitespace, broken brackets, and invalid ports are rejected without storing them. JVM
tests cover normalization and rejection boundaries. Credentials remain owned by Moonlight.

## Next critical-path work

Durable launch-session work (DATA-07, audit P1 session recovery): database version 12
adds one pending-session row. The production gateway persists a handoff ticket before
opening an emulator, confirms successful dispatch, and abandons known failed handoffs.
On resume (including a fresh process), Room updates game history and removes that row
in one transaction. Repeated completion therefore cannot add the session twice.
An interrupted, unconfirmed handoff adds no playtime. A database write failure before
dispatch prevents opening the emulator. Recorded duration is time away from GameBox,
not verified active gameplay; emulator progress/input is still unproven.

Android coverage now includes database close/reopen recovery, repeated completion,
unconfirmed handoff, pending-record overwrite protection, and the launch-session
migration path. A separate released-schema matrix now exercises every starting database
version from 1 through 13 against the complete production migration registry and current
version-14 schema, including sentinel preservation and current-table checks. This is
automated schema evidence; it does not replace actual Android process-death/emulator UAT
or a signed APK upgrade/downgrade rehearsal.
Recovery failures now have a global retry banner, including cold starts with no
selected game. New launches are blocked until reconciliation succeeds; a confirmation
failure after dispatch is reported as uncertain session tracking, not a failed launch.
Controller tests cover journal ordering, recreation, recovery/retry and persistence
failures before/after dispatch, plus the production recovery banner. The merged CI
instrumentation path now includes this launch-session coverage; live emulator execution
remains a separate acceptance gate.

Launch preparation follow-up (audit P1 UI-thread blocker; EMU-02 integration):
the production controller now publishes PREPARING synchronously, runs gateway file
verification/export on Dispatchers.IO, and handles storage exceptions as a retryable
UI failure. Duplicate Play requests are ignored during preparation and while waiting
for an external return. Added tests cover the dispatcher boundary, duplicate requests,
resume during preparation, exception/retry, and existing broken-content state handling.
Details now offers Cancel preparation. The gateway checks cancellation between hash
chunks and atomically closes the cancellation window before external dispatch; an
accepted cancellation cannot subsequently open the emulator. Three local JVM tests
passed for cancellation before dispatch, cancellation/repeat rejection after commit,
and stopping hashing without reading the entire game. Full Android/controller tests
passed in PR #277 along with Android instrumentation, Windows build and phone/DeX
screenshot jobs, and that PR is merged. Local Android SDK and device execution are now available. This is not yet a complete
launch lifecycle: persistent sessions, large-file frame measurements
and live emulator gameplay remain pending for adapters other than the validated API-35
Galaxy Patrol/RetroArch path. Intent dispatch alone is not proof of gameplay.

Emulator readiness now automatically selects the first installed approved adapter when
no per-game emulator choice has been saved, while preserving an explicit approved
selection and its missing-selection guidance. JVM coverage exercises the installed
alternative path; this improves software detection/configuration behavior but does not
prove a real emulator launch or gameplay.

Configured HTTPS catalog framing hardening (2026-09-28): the production catalog reader
now rejects a response whose bytes do not exactly match a declared `Content-Length`,
while retaining the bounded-response guard for unknown-length streams. The framing
helper has focused coverage for unknown length, truncated bodies, and overlong bodies;
the focused test and full Android JVM suite pass. This is local transport evidence, not
live endpoint acceptance.

Fresh API-35 UI/lifecycle gate (2026-09-28): `connectedDebugAndroidTest` completed
68 tests with zero failures and zero errors on `GameBox_API35`. Two tests were skipped
by their explicit clean-emulator environment guards: the authorized-install lifecycle
test requiring a configured legal fixture and the real SAF save-root round trip. The
connected Honor remains a separate physical gate; ADB currently reports its secure
AOD/keyguard state, so no Honor Compose result is inferred from this emulator run.

Artwork transport framing hardening (2026-09-28): remote HTTPS artwork now uses a
bounded reader that rejects bodies shorter or longer than a declared `Content-Length`
and stops at the 4 MiB limit for unknown-length responses. Focused tests cover unknown
length, truncated length, and over-limit bodies. The complete Android JVM suite now
passes 564 tests with zero failures, zero errors, and one explicit skip; this remains
local transport evidence and does not claim live CDN acceptance.

Discovery text transport framing hardening (2026-09-28): the shared cancellable HTTPS
reader used by Vimm title discovery and JSON discovery now rejects a response whose
body length differs from its declared `Content-Length`, while preserving bounded
reads and socket disconnect on cancellation. Focused tests cover unknown length,
truncated length, and over-limit bodies. The full Android JVM suite now passes 567
tests with zero failures, zero errors, and one explicit skip. This is local discovery
transport evidence; it does not authorize or download copyrighted binaries.

Visual-QA CI gate hardening (2026-09-28): the Android matrix no longer masks a failed
`BlueprintScreenshotsTest` instrumentation invocation with `|| true`, and the assertion
now accepts the complete successful suite rather than the stale one-test count. The
runner still uploads captures and logs on failure, while the instrumentation exit status
and a successful `OK (...)` result are now required for the visual-QA job to pass. This
keeps phone, Desktop/TV-sized, and large-text Blueprint coverage enforceable in CI;
physical external-display and controller soak evidence remains separate.

Fresh SMART TV wide-layout checkpoint (2026-09-28): with the Honor Magic6 Pro connected
to the remembered `SMART TV` presentation display at 1920x1080/160 dpi, the live GameBox
window was navigated through PC Hub, Library, and Media Hub. PC showed the Moonlight,
Winlator, Termux, Files, and Chrome cards plus host setup and connection status; Library
showed its zero-game/empty state, filters, storage, saves, and overview rail; Media showed
the living-room app rail, ready YouTube/Netflix cards, setup-required states, device
status, and controller footer. All three states rendered with the Blueprint wide-shell
navigation, focus treatment, status rail, and controller legend. This is live external-
display rendering evidence; it does not claim physical controller input, HDMI/DeX soak,
or populated-provider gameplay.

Fresh local baseline checkpoint (2026-09-28): `testDebugUnitTest` completed with 567
tests, zero failures, zero errors, and one explicit skip using the Android Studio JDK;
the Windows companion core suite completed with `GameBox Windows core tests passed.`
These are local automated software checks after the visual-QA workflow hardening, not
live authenticated-provider, signed-release, emulator-gameplay, or hardware evidence.

NES adapter handoff hardening (2026-09-28): imported and catalog-provided NES games
selected for RetroArch now carry the validated FCEUmm core name and Android core path,
not only the bundled Galaxy Patrol record. This keeps the approved content URI handoff
and missing-core guidance consistent for legal NES imports while deliberately leaving
unvalidated platform/core mappings unspecified. `EmulatorSettingsTest` and the full
Android JVM suite pass with 568 tests, zero failures, zero errors, and one explicit skip.
This improves launch preparation coverage; it is not evidence of a second physical game
or emulator gameplay session.

Emulator package-visibility hardening (2026-09-28): the Android manifest now declares
DuckStation, M64Plus FZ, and Flycast in `<queries>`, matching the approved package list
already used by `EmulatorCapabilityRegistry`. This lets Android 11+ package discovery
see installed PS1, N64, and Dreamcast adapters before handoff instead of falsely
reporting them unavailable. Manifest merge verification found all three entries, and
the Android JVM suite remains at 568 tests with zero failures, zero errors, and one
explicit skip. This fixes package visibility only; it does not claim live gameplay for
those adapter families.

The same package-visibility change was installed and exercised on the isolated
`GameBox_API35` emulator without touching the connected Honor/SMART TV session. The
non-screenshot instrumentation suite completed 64 tests with `OK (64 tests)`. The
Blueprint screenshot class and physical display evidence remain separately tracked.

The isolated API-35 visual gate was then run directly against the current APK and
completed `BlueprintScreenshotsTest` with `OK (4 tests)`. This covers the production
Home/Details, Library, Store, Media, PC, Settings, Downloads, and populated-layout
capture flow used by the CI matrix. It is emulator screenshot evidence, separate from
the live SMART TV render checkpoints and physical controller/DeX acceptance.

Fresh live SMART TV recheck after the Honor was unlocked (2026-09-28): the current debug
APK was installed on `PGT-N19` and launched on the active `SMART TV` presentation display
at 1920x1080/160 dpi. Home rendered the Finish console mode banner, Galaxy Patrol hero,
status rail, PC quick-launch tiles, and populated Explore row. Store rendered the console
rail including PSP/Dreamcast/3DS/Switch, populated authorized source cards, the
TheGamesDB API-key-required state, and sync/refresh actions. Settings rendered the
Storage/Controllers/Downloads/Emulators/Display/Audio/Network/Saves/System rail and
controller footer. This is current physical external-display rendering evidence; the
phone was unlocked, but no physical controller or long-duration soak was claimed.

The same live Settings session opened the Emulators section, displayed the approved
RetroArch/PPSSPP/Dolphin/DuckStation/M64Plus FZ/Flycast adapter guidance, reported
`External display detected · SMART TV · 1920x1080`, and returned to GameBox after the
`Manage installed emulator apps` action opened Android system settings. This validates
the external-settings boundary and display-state reporting only; installed emulator
availability and gameplay remain adapter-specific evidence.

Fresh Android release packaging checkpoint (2026-09-28): `:app:assembleRelease`
completed successfully after the emulator visibility changes. The resulting unsigned
preview is `app-release-unsigned.apk`, 14,543,205 bytes, SHA-256
`beaa33d538dfe5515e94c5153dc5827dac0197c91d5015d3dce976e4f275f56f`. `apksigner verify`
correctly reports `DOES NOT VERIFY` because the unsigned artifact has no
`META-INF/MANIFEST.MF`; this is expected preview evidence and does not satisfy the
protected production signing, upgrade, or rollback gate.

1. Extend real game/save/return/uninstall/reinstall proof across the supported adapter patterns.
2. Validate managed GameBox save import/backup/cloud behavior against production emulators and real endpoints.
3. Live provider/download/volume validation against real configured endpoints and storage.
4. Separately connected physical controller and long-duration Honor external-display recovery/soak acceptance.
5. Signed production build, clean upgrade, update-channel and rollback rehearsal.
6. Windows Companion production-signed LAN session; the live debug LAN pairing,
   configuration, managed-save, content-import, gameplay handoff and save-safe cleanup
   journey is now documented above. Maximum-size, disk-full, replacement,
   routed-network, MSIX signing/install and physical Windows acceptance remain separate
   gates.

The released-schema migration matrix and monotonic Android version-code generation are now
implemented and exercised by automated CI. They remain prerequisites for the signed
upgrade/rollback rehearsal rather than open implementation gaps.

Physical hardware gates stay pending and do not prevent independent software work.

Fresh end-to-end user-flow checkpoint (2026-09-28): while the Honor Magic6 Pro was
connected to the active `SMART TV` presentation display, the `AuthorizedInstallLifecycleTest`
instrumentation journey completed on the physical PGT-N19 device. The journey exercised
authorized content installation, durable install-state observation, content-only uninstall,
retained managed save/backup state, reinstall through restored work, and verified save
restore. This is product lifecycle evidence on the Honor, not proof of a second emulator
family, real remote-provider bytes, signed release installation, or physical controller
input. Direct TV screenshot capture was unavailable through the device's current ADB
presentation-display screencap interface, so the TV claim remains based on the previously
captured live GameBox surface and the physical-device instrumentation result above.

Instrumentation regression check (2026-09-28): the complete Android instrumentation
suite passed on `GameBox_API35` with 68 executed tests and one explicit SAF-provider skip.
The same suite on the unlocked Honor while `SMART TV` was connected reached 32 of 68
tests before the physical run failed at
`AccessibilitySemanticsTest.gameCardExposesUsefulSpokenDescriptionAndClickAction` and
the instrumentation process crashed. The isolated test also fails on the Honor but
passes on API-35; the production `GameCard` still exposes the expected description and
clickable semantics, so this is tracked as an Honor/Compose instrumentation compatibility
issue pending a device-specific diagnostic, not counted as a clean physical accessibility
pass. The authorized install lifecycle test itself remains green on the Honor.

Cloud-save transport regression checkpoint (2026-09-28): the focused Android JVM suite
passed 25 tests across `HttpsCloudSaveTransportClientTest`, `CloudSaveEndpointPolicyTest`,
`CloudSaveEnvelopeCodecTest`, and `CloudSaveSyncContractTest`. The transport loopback
tests exercise upload/download round trips with Basic and SigV4-style authentication,
checksum enforcement, retry classification, cancellation, and response-size bounds.
This is local integration evidence; authenticated WebDAV/S3 endpoint acceptance remains
an external-provider gate and is not implied by the loopback server.

Legal homebrew fixture research checkpoint (2026-09-28): `docs/legal-homebrew-test-matrix.md`
now records license-visible candidates for Game Boy, GBA, N64, Dreamcast, Genesis, PSP,
3DS, and Wii, while retaining Galaxy Patrol as the already-validated NES fixture. The
matrix requires upstream license/release evidence and a pinned SHA-256 before a binary is
used, and explicitly excludes source ports that require commercial assets. It also marks
PS1, PS2, PS3, GameCube, Switch, Xbox, PC Engine, WonderSwan, Neo Geo, and arcade
families as needing further approved fixture research rather than implying coverage.

Legal homebrew fixture staging checkpoint (2026-09-28): the RetroBrews Game Boy Color
collection's `brickster.gbc` was downloaded from its public repository into a dedicated
temporary fixture directory, verified at 32,768 bytes with SHA-256
`24e44e274f8ee77a838d0fc58a607521021b3de122a968f359994c6f7c773a45`, and pushed to the
connected Honor Magic6 Pro at `/sdcard/Download/GameBoxOS-Legal/brickster.gbc`. This is
legal-fixture provenance and device transfer evidence only; the file was not copied into
the user's `D:\GameBoxOS\games` directory, and the Store UI import completion plus
Game Boy Color emulator gameplay/save evidence remain open.

Physical DocumentsUI boundary checkpoint (2026-09-28): the focused
`SystemDocumentPickerReimportTest.productionReimportCompletesThroughAndroidDocumentsUi`
completed on the connected Honor Magic6 Pro with `OK (1 test)`. This validates the
production Android picker/reimport path on physical hardware using the test's isolated
fixture; it does not promote the staged Brickster file to a completed Game Boy Color
import or gameplay result.

Physical authorized-copy import checkpoint (2026-09-28): the Store → Cave Story details
route now exposes an authorized-copy importer for unavailable catalog states. On the
connected Honor Magic6 Pro, `Import your copy` opened the real Android DocumentsUI; the
`Downloads/GameBoxOS-Legal/brickster.gbc` fixture was selected and returned to GameBox.
App-private inspection confirmed `files/imports/cave-story/brickster.gbc`, 32,768 bytes,
with SHA-256 `24e44e274f8ee77a838d0fc58a607521021b3de122a968f359994c6f7c773a45`, and the
Library then displayed Cave Story as installed. This is physical discovery/details/import
and registration evidence only; GBC emulator detection, gameplay, save, uninstall, and
reinstall/restore remain separate gates.

GBC platform-resolution checkpoint (2026-09-28): generic catalog labels such as `Retro`
and `Homebrew` now resolve to a concrete console when the selected extension uniquely
identifies one, and repository merge upgrades only that broad label while preserving user
metadata. The physical Brickster re-import on Honor consequently displayed `GAME BOY COLOR`
in Home/Details and registered the installed content under the concrete profile. The
emulator registry now maps Game Boy and Game Boy Color to RetroArch's Gambatte core
(`gambatte_libretro_android.so`) with focused JVM coverage. A physical Play attempt reached
RetroArch's real `RetroActivityFuture`, but the captured surface was black because the
connected RetroArch installation exposed no Gambatte core; this is a missing-core/emulator
setup boundary, not gameplay or save evidence.

Physical GBC emulator lifecycle checkpoint (2026-09-28): the legal Brickster fixture was
tested end to end on the connected Honor Magic6 Pro. The official ARM64 Gambatte core was
sideloaded through RetroArch's documented debug sideload activity, after which GameBox
launch handed RetroArch a temporary absolute Downloads path and rendered the real
Brickster session. Directional input was dispatched, Gambatte created a save state at
`/sdcard/RetroArch/states/Gambatte/brickster.state`, and the state was visibly loaded
after returning to the session. Returning to GameBox removed the temporary
`Download/GameBox/brickster.gbc` handoff copy while retaining the original legal fixture.
GameBox content-only uninstall removed the app-private content while retaining metadata and
managed save state; re-import through DocumentsUI, relaunch, and visible slot-0 state restore
then completed successfully. This is physical Honor evidence for legal GBC import,
RetroArch handoff, input, emulator save-state, cleanup, uninstall, reinstall, and restore.
Managed GameBox cloud-save transport, physical controller input, and long-duration TV soak
remain separate acceptance gates.

RetroArch core-profile expansion checkpoint (2026-09-28): the emulator registry now
provides deterministic core names and Android core filenames for imported SNES/SFC,
Game Boy Advance, Genesis/Mega Drive, and Nintendo 64 content in addition to the
validated NES/FCEUmm and GBC/Gambatte profiles. The mappings use Libretro's documented
preferred cores: Snes9x, mGBA, Genesis Plus GX, and Mupen64Plus-Next. JVM coverage checks
each platform's required core contract and filename. This advances emulator setup and
launch preparation; it is not gameplay, save-state, BIOS, or physical-device evidence
for those additional families.

Details emulator-guidance checkpoint (2026-09-28): the Details screen no longer embeds
the Galaxy Patrol NES/FCEUmm text as a special-case UI string. It derives the required
core from the selected game's approved emulator capability, so imported GBC, SNES, GBA,
Genesis, N64, and the existing NES title each present their own core guidance; games with
no approved adapter omit the guidance. JVM coverage verifies both a GBC core description
and the unsupported-platform omission. This is Android UI/presentation evidence, separate
from live emulator gameplay and physical accessibility acceptance.

RetroArch adapter coverage checkpoint (2026-09-28): the profile registry now also
provides verified Android core filenames for Nintendo DS/melonDS, PlayStation/PCSX-
ReARMed, Dreamcast/Flycast, Master System and Game Gear/Genesis Plus GX, PC Engine/
Beetle PCE FAST, WonderSwan/Beetle Cygne, and Atari 2600/Stella. The Nintendo 64
contract was corrected to the Android GLES3 build filename
`mupen64plus_next_gles3_libretro_android.so`. The complete JVM suite covers these
contracts. This is deterministic adapter configuration evidence only; it does not
claim live core installation, BIOS availability, gameplay, or save compatibility for
these additional families.

HDMI-first TV interaction checkpoint (2026-09-28): the current debug APK was installed
on the authorized Honor Magic6 Pro and the supported `Show on TV` route was triggered
from the phone only as a control surface. Honor Desktop Mode opened GameBox on the real
wired presentation display 8. Authoritative window/configuration state reported the
GameBox task and app frame at 1920x1037 @ 160 dpi, with the app window spanning the full
HDMI task width. Display-targeted input (`adb shell input -d 8`) was then used on the TV
surface to move from Home to Store; the TV hierarchy confirmed the GameBox tabs, store
actions, controller footer semantics, and changed screen state. This is actual HDMI
user-flow evidence, not phone-screen evidence. The firmware routed `MainActivity` into
Desktop Mode rather than allowing direct launch of the non-exported `TvActivity`, so the
dedicated TV activity instrumentation remains separately reported. Direct HDMI
screencap is unavailable through this device's ADB presentation-display interface;
authoritative window/configuration and display-targeted hierarchy/input evidence are
therefore used for this checkpoint.

Physical TV dimension regression checkpoint (2026-09-28): the viewport policy now has
explicit tests for the connected HDMI profile (1920x1080 at 1.5x Blueprint scale) and a
4K 3840x2160 profile (2.0x cap). This complements the existing 160-dpi/content-safe
viewport cases and protects the wide 16:9 composition from future density or resolution
regressions.

TV controller-navigation regression checkpoint (2026-09-28): the dedicated
`TvActivityTest` now covers the Blueprint shoulder-button contract in addition to the
quick-actions overlay: R1 moves Home to Library and L1 returns to Home, with selected
tab semantics asserted after each event. The JVM suite and Android test APK compilation
pass. This is host/controller-runtime coverage; the physical HDMI render and physical
controller hardware gates remain separately recorded.

TV display-loss recovery checkpoint (2026-09-28): `TvActivity` now registers a
`DisplayManager.DisplayListener` for its started/stopped lifecycle and finishes only when
the external display hosting that activity is removed. Default-display removal and
unrelated external-display removal are covered by JVM tests. The debug APK rebuilt
successfully and was installed on the authorized Honor; the subsequent HDMI route again
reported a full 1920x1080 @ 160 dpi GameBox window/configuration on the wired display.
This software recovery guard is unit-verified; physical cable-unplug/reconnect soak
remains a separate hardware gate.

Physical-TV target selection checkpoint (2026-09-28): presentation-display discovery now
marks Android virtual displays and prefers a physical presentation display when multiple
targets are available, while retaining the first-target fallback if every target is
virtual. This prevents an Android Studio screen-sharing/capture surface from taking the
Show on TV route ahead of a connected television. JVM coverage verifies physical-over-
virtual selection; live HDMI validation remains separately recorded above.

Honor HDMI input-boundary checkpoint (2026-09-28): a direct ADB `input -d 8 keyevent`
probe on the wired display was intercepted by Honor Desktop Mode's `SecondLauncher`;
the TV hierarchy changed to the firmware launcher and GameBox lost window focus before
R1/L1 could reach the app. GameBox was restored to the HDMI task afterward. This is
firmware/controller-boundary evidence, not a failure of the shared TV key-dispatch code;
the dedicated `TvActivityTest` remains the automated controller-path evidence, while a
physical controller and final Desktop Mode handoff remain hardware gates.

Executed TV-host instrumentation checkpoint (2026-09-28): the isolated API-35 emulator
ran the complete `TvActivityTest` class directly from the installed debug/test APKs and
finished `OK (3 tests)`. This includes Blueprint Home rendering, R1/L1 Home-to-Library-
and-back selection, and Start/B quick-actions overlay behavior. It is automated Android
host evidence only; it does not override the Honor Desktop Mode `SecondLauncher` input
interception or claim physical HDMI-controller acceptance.

TV manifest-boundary instrumentation checkpoint (2026-09-28): after rebuilding the
current APK and Android test APK, the isolated API-35 emulator ran `TvActivityTest` and
finished `OK (4 tests)`. The added test confirms the dedicated TV activity is not
exported; the manifest/build contract continues to declare it resizeable and fullscreen.
This protects the external-display entry boundary without weakening the non-exported
security boundary, and remains separate from Honor firmware and physical-controller
acceptance.

TV emulator handoff checkpoint (historical, 2026-09-28): launching the legal Galaxy Patrol
fixture from the HDMI Details screen requested `ActivityOptions.launchDisplayId` and an
explicit physical display-bounds rectangle for external-display emulator handoffs. JVM tests
and the debug APK build passed at that point.
On the wired Honor HDMI display, the complete TV flow was exercised: Details -> Install game
-> Verified asset worker succeeded -> installed -> Play; RetroArch resumed on display 8, with
`launchedFromPackage=com.gamebox.os` and the HDMI task area recorded by ActivityTaskSupervisor.
Honor Desktop Mode still applies `hw-pc-freeform` to the third-party RetroArch task and
ignored the requested 1920x1080 bounds (`[192,103][1344,794]`), so GameBox cannot claim
full-screen emulator presentation on this firmware without a platform/window-manager control.
This proves legal fixture installation and display-aware launch routing, not gameplay or
physical-controller acceptance. The later external gameplay-window consistency fix removed
that explicit bounds rectangle from the production handoff path; see the current regression
checkpoint near the end of this document.

TV safe-area persistence checkpoint (2026-09-28): on the wired HDMI display, Settings →
Display reported `HDMI Screen · 1920×1080`, exposed the TV safe-area controls, and applied an
8% inset to the wide Blueprint viewport. The TV hierarchy reported `Active inset: 8% on every
edge`; after force-stopping and reopening GameBox on display 8, the 8% option remained checked.
The original 4% user setting was restored before ending the session. This validates the TV
dimension preference and persistence through the real external-display UI, not phone rendering.

TV desktop-caption dimension regression checkpoint (2026-09-28): the connected Honor task
reported a 1920×1037 app window while the physical HDMI mode remained 1920×1080. The wide
layout policy now has a focused JVM regression asserting that this caption-reduced profile
stays non-compact and scales the Blueprint surface to the available 1037dp height. The
focused test passed; this protects the real TV window profile without treating the desktop
caption area as usable content.

TV Details action checkpoint (2026-09-28): the wide Details installation panel now exposes
the primary Install game action alongside status and size, so TV users do not have to scroll
to the lower content card before starting an authorized install. The rebuilt APK was installed
on HDMI display 8; the legal Galaxy Patrol content was removed through the TV confirmation
dialog, reinstalled through the TV Details flow, and the hierarchy returned `Game content ·
installed` and `Verified asset worker: succeeded`. This is TV user-flow evidence; it does not
claim third-party emulator fullscreen or physical-controller acceptance.

TV hero-panel bounds investigation (2026-09-28): the connected HDMI hierarchy exposes the
hero installation labels (`INSTALLATION`, status, and `Play Now`) as zero-bounds Compose nodes
even after an explicit full-height measurement attempt. The lower scrollable Details action
card remains measurable and was used successfully for install/uninstall/reinstall. The
measurement tweak was reverted because it did not change the authoritative TV result; the
hero overlay remains an identified layout/semantics follow-up rather than a claimed fix.

TV hero rail layout increment (2026-09-28): the wide Details hero was restructured from an
overlaid installation child into an explicit text-column plus fixed 238dp installation rail,
with a constrained 220dp rail container. The debug APK and JVM suite pass and the rebuilt
APK was installed on HDMI display 8. The hierarchy still reports the inner hero labels at
zero bounds on this Honor presentation surface, so this increment improves the layout
contract but does not claim the external semantics-bounds issue is resolved.

TV safe-area focus checkpoint (2026-09-28): the safe-area chips now explicitly request
focusability. On HDMI display 8, the hierarchy exposed the `TV safe area 4%` option's parent
as `checkable=true`, `clickable=true`, and `focusable=true`; its nested content-description
child is intentionally non-focusable Compose semantics. This makes the TV dimension controls
reachable by D-pad/remote focus while preserving the existing persisted setting behavior.

TV Settings focus traversal checkpoint (2026-09-28): the wide TV Settings root no longer
captures focus while Settings is active; Home/Library/etc. retain their root controller
handling. On HDMI display 8, a display-targeted D-pad probe moved focus from the Settings
surface into real checkable/clickable controls and scrolled the settings rail instead of
remaining on the root view. The safe-area chip parents remain explicit focus targets; the
current directional order reaches the Settings section rail before the Display content rail.

TV Settings initial-focus checkpoint (2026-09-28): entering Settings on HDMI display 8 now
places focus on the first Storage section item rather than the global navigation. A single
display-targeted Right key transferred focus into the content pane, where the hierarchy
reported the STORAGE panel and External game library controls. This establishes a predictable
rail-to-content D-pad path for the TV app; deeper Display-section traversal remains available
through the existing section rail and scroll behavior.

TV controller-shortcut boundary checkpoint (2026-09-28): a display-targeted Android
`KEYCODE_BUTTON_X` probe did not advance the Settings section on the Honor session, while
directional focus transfer worked. This is recorded as a controller/firmware input boundary;
the software Settings initial-focus and rail-to-content behavior remains validated separately.

TV Downloads empty-state checkpoint (2026-09-28): the live HDMI display rendered the wide
Download Manager with its active/queued/completed metric cards, empty-state panel, and
controller footer. The hierarchy confirmed `No active downloads` and the authorized-source
guidance at TV bounds; this is physical external-display UI evidence for the empty Downloads
state, separate from live provider transfer and hardware-controller acceptance.

TV Store focus checkpoint (2026-09-28): the wide Store root no longer requests the global
GameBox focus target while Store is active. On the wired Honor HDMI display, entering Store
from Home placed focus on the visible `All` console rail item at TV bounds `[119,207][309,270]`;
a display-targeted Down key moved focus to the visible `PS2` item at `[119,261][309,324]`.
This makes the first Store filter reachable by remote/D-pad without phone-screen validation.
The direct Right probe still exposed a zero-bounds Compose semantics target on this Honor
Desktop Mode presentation surface, so horizontal rail-to-content navigation remains an
identified follow-up rather than a claimed fix.

TV borderless-window increment (2026-09-28): both external-display hosts now request
fullscreen/no-limit window flags, landscape, cutout-safe fullscreen, and a match-parent
window; the manifest marks them fullscreen, borderless, landscape, and non-resizable. The
supported external-display launcher also supplies the platform fullscreen windowing-mode
hint when starting the dedicated TV host. JVM tests and the debug APK build pass. A direct
ADB launch of MainActivity on the Honor HDMI display still reports `hw-pc-freeform`, a
`[192,103][1312,794]` task with a `[192,103][1312,139]` system caption; this is an Honor
Desktop Mode window-manager limitation for that launch path, not a claimed borderless
acceptance. The in-app Show on TV route remains the authoritative handoff to verify next.

Honor fullscreen windowing probe (2026-09-28): after installing the borderless build, a
display-targeted platform launch with explicit `windowingMode=fullscreen` was also tried.
Honor Desktop Mode still materialized GameBox as `hw-pc-freeform` with a centered task and
system caption (`[192,103][1344,794]`, caption present). This confirms the remaining border
is outside Compose and outside the app window flags; it is controlled by the Honor desktop
window manager. The APK remains fullscreen at the Android window-policy level, but a true
caption-free HDMI result requires the firmware's maximize/fullscreen desktop action or a
different mirroring/presentation mode.

TV Store horizontal-focus checkpoint (2026-09-28): the Store rail now handles a
display-targeted Right key explicitly on wide layouts, requesting the first visible
content target instead of relying on Honor's spatial focus search. On the physical HDMI
display, Store entry focused the console rail at `[266,252][391,293]`; Right moved focus
to the visible Galaxy Patrol card at `[250,325][1104,509]`, whose hierarchy included
`Galaxy Patrol, Retro, installed, continue playing`. This closes the previously observed
zero-bounds Right handoff for the Store TV flow; phone-screen rendering was not used for
this acceptance.

TV launcher-role increment (2026-09-28): the Home setup action now prefers Android's
direct `RoleManager.ROLE_HOME` request on API 29+ and falls back to Home Settings on older
or unavailable builds. This gives the user a direct, explicit way to make GameBox the
system HOME surface—the mode that can cover the external desktop as a launcher—rather
than silently changing the current launcher. The rebuilt APK installed successfully on
the Honor; the authoritative current HOME holder remains `com.hihonor.android.launcher`,
so launcher takeover is intentionally pending user confirmation.

TV HOME-surface handoff increment (2026-09-28): `Show on TV` now branches on the actual
default HOME holder. When GameBox owns HOME, it launches an Android HOME intent on the
physical presentation display so the system treats GameBox as the launcher surface;
when Honor remains the HOME holder, it preserves the dedicated TV-host fallback and
reports that GameBox must first be selected as default Home for launcher mode. The APK
build and JVM suite pass, and the live Honor role check still reports
`com.hihonor.android.launcher`; no launcher role was changed automatically.

TV edge-to-edge safe-area increment (2026-09-28): the GameBox settings model and
DataStore fallback now default `TV safe area` to `0%`, matching the supplied 1920x1080
launcher reference and removing the application-owned inset on fresh installs. The
installed Honor session was validated after activating the desktop fullscreen action:
the GameBox root hierarchy occupies `[0,0][1920,1080]`, the system caption is absent,
and the GameBox logo begins at the edge-to-edge layout origin. Existing devices with a
previously persisted nonzero preference still retain that explicit user choice and can
switch it to 0% through Settings; no app data reset was used.

Honor desktop border boundary (2026-09-28): the same live HDMI session initially
materialized a centered `[192,103][1344,794]` `hw-pc-freeform` window despite the app's
fullscreen/no-limit flags. Activating Honor's own fullscreen window control changed the
authoritative UI hierarchy to a GameBox root at `[0,0][1920,1080]` with no
`androidhnext:id/hw_caption`. This proves the app is capable of the requested
borderless full-display surface, while the initial automatic transition remains an
Honor Desktop Mode policy boundary and not an app-rendering defect.

TV Details primary-action focus checkpoint (2026-09-28): the wide Details root no longer
keeps the global GameBox root focusable while a game is selected, and the primary
Play/Install action requests focus after the Details transition has attached it. On the
installed build, an HDMI-targeted D-pad traversal reached the `Play Now` parent with
`clickable=true`, `focusable=true`, `focused=true`, and measurable bounds
`[1511,263][1822,335]`; the nested Play Now text was also measurable. A shell tap into
Details can leave the hierarchy temporarily without a focused node on Honor, so this
checkpoint claims the reproducible directional-TV path and not an automatic pointer-tap
focus result.

TV legal-game launch/return checkpoint (2026-09-28): from the focused wide `Play Now`
action, the installed Galaxy Patrol fixture dispatched RetroArch with
`launchedFromPackage=com.gamebox.os` and the RetroArch task visible on physical display
8. After ending the legal test session, GameBox was relaunched on the same HDMI display
and the hierarchy again exposed `Home tab` and `Galaxy Patrol`. This validates the TV
Details → emulator dispatch → GameBox return surface; Honor still reports the third-party
RetroArch task as `hw-pc-freeform` with its own desktop bounds, so emulator caption-free
fullscreen/gameplay remains a separate firmware and hardware gate.

TV launcher-mode acceptance checkpoint (2026-09-28): the connected Honor now resolves
`com.gamebox.os` as the Android HOME role holder. A HOME intent targeted at physical HDMI
display 8 opened GameBox as the launcher surface rather than a normal desktop activity.
The authoritative UI hierarchy reported a GameBox root at `[0,0][1920,1080]`, GameBox
logo and Home tab semantics, and `captionNodes=0`; `dumpsys window displays` reported
GameBox as the focused window for display 8. This closes the automatic borderless launcher
path on the current device. Cold boot/unlock recovery, HDMI reconnect, and third-party
emulator fullscreen remain separate hardware/firmware gates.

TV process-recovery boundary checkpoint (2026-09-28): after force-stopping GameBox, the
Honor external-display `HOME` intent and the device's available `cmd activity` interface
did not relaunch the stopped external-display launcher; Desktop SystemUI remained in
front. An explicit `MainActivity` relaunch restored the GameBox surface and retained
1920x1080 bounds, but Honor reintroduced its 36 px `hw_caption` because that launch was
an ordinary activity start rather than HOME resolution. The HOME role itself remained
`com.gamebox.os`. This is evidence for the pending process-death/cold-start firmware
recovery gate, not a regression in the normal borderless HOME surface.

TV navigation semantics checkpoint (2026-09-28): the top navigation tabs now expose
their content description and `OnClick` action on the same semantics node, preserving
controller/accessibility activation when an unmerged TV hierarchy is used. The JVM
unit suite, debug APK assembly, and `TvActivityTest` passed on the connected Android
test target. The catalog fallback instrumentation still has a separate provider-
simulation gate issue on that target; it is not counted as TV launcher acceptance.

Browser-download import increment (2026-09-28): Vimm discovery details now expose an
explicit user-initiated `Open Vimm's Lair & track download` action. GameBox does not
request or resolve Vimm binary URLs. It watches successful Android `DownloadManager`
entries created after the handoff, matches the title and platform-supported extension,
passes the local URI through the existing verified importer, registers the game under
`files/imports/<game-id>/`, and removes the browser download only after registration
and transaction confirmation succeed. If Android denies deletion, the source is
retained and the UI reports that outcome. PSP authorized-copy import now accepts `.7z`
with MIME `application/x-7z-compressed`; the archive is moved as a verified content
file, not silently extracted or treated as an emulator binary source. JVM tests,
debug assembly, and the TV instrumentation target passed after this increment.
Archive import and console-tree increment (2026-09-28): new legal local imports
now use a console-scoped managed directory such as
`files/imports/psp/<game-id>/` while legacy bundled fixture paths remain
compatible. ZIP and 7Z sources are staged, safely extracted with traversal and
size/file-count limits, and only supported files are registered. The archive
itself is never promoted into the game directory and is deleted from staging
after extraction; registration or transaction failure still rolls back the
staged directory. Content-only uninstall removes every registered extracted
file and retains saves, metadata, and history. PPSSPP handoff now uses its
scoped ACTION_VIEW URI with a wildcard MIME fallback because some PPSSPP
Android builds reject GameBox's custom PSP MIME types before launch.

TV window-policy resilience increment (2026-09-28): MainActivity and the
dedicated TvActivity now reapply their external-display fullscreen, landscape,
edge-to-edge, and hidden-system-bar policy on resume and on Android
multi-window-mode changes. This addresses the software-side transition where
Desktop Mode can resize or reconfigure a task after creation. It cannot override
Honor's firmware decision to materialize an ordinary freeform window; HOME-mode
and firmware maximize behavior remain separate hardware/firmware evidence.

Instrumentation evidence for this increment: the API-35 emulator completed all
four `TvActivityTest` cases. The connected Honor run remains a device-state
failure: Desktop Mode force-stopped/relaunched `com.gamebox.os` while the first
non-exported-host test was starting, and the test runner could not retrieve
logcat afterward; no GameBox Java exception was reported. This is retained as
hardware-test evidence and is not merged into the emulator pass claim.

TV launch-bounds correction (2026-09-28): the external-display launcher no
longer supplies an explicit `launchBounds` rectangle. That rectangle could
cause desktop shells to classify an otherwise fullscreen TV task as freeform;
the launch now supplies only the selected display and fullscreen windowing hint,
leaving the activity's MATCH_PARENT policy to size the surface.

TV window regression coverage (2026-09-28): `TvActivityTest` now asserts the
runtime fullscreen flag and absence of the force-not-fullscreen flag in addition
to landscape, Blueprint rendering, controller shoulder navigation, and the
non-exported host boundary. The isolated API-35 emulator run remains green with
all four tests passing.

Blueprint visual gate evidence (2026-09-28): `BlueprintScreenshotsTest` was
updated to reflect the current TV-aware layout contract: compact-only store
coverage skips wide displays, update-state details tolerate the shared
controller-footer label, and the PC handoff assertion uses the stable external
display action semantic. The production screenshot/navigation gate completed
with all four tests passing on the API-35 emulator after reinstalling the debug
app and instrumentation APKs.

High-density TV viewport fit increment (2026-09-28): wide layouts now scale to
the available height without an artificial 0.85 minimum. This preserves the
intended 1080p/160-dpi TV scale while allowing high-density landscape surfaces
such as the API-35 emulator's 914dp × 411dp viewport to request a mathematically
fitting scale rather than a fixed floor. The new policy case, Android JVM suite,
Blueprint screenshot gate (`OK (4 tests)`), and dedicated TV host
instrumentation (`OK (4 tests)`) all pass. This is emulator TV-dimension
evidence; it does not replace physical overscan or Desktop Mode acceptance.

Blueprint TV-profile visual checkpoint (2026-09-28): the API-35 emulator was
temporarily configured to 1920×1080 at 160 dpi, matching the documented living-
room profile. The combined instrumentation run completed `OK (8 tests)`:
`BlueprintScreenshotsTest` 4/4 and `TvActivityTest` 4/4. The captured Home frame
shows the full wide composition—hero, collection area, quick launch, Explore
games rail, and controller footer—without vertical clipping. Emulator size and
density were restored afterward; this remains software TV-dimension evidence,
not physical Honor/HDMI acceptance.

TV Details install-flow coverage (2026-09-28): the wide production screenshot
flow now explicitly asserts that opening the Home hero exposes the authorized
`Install game` action in the TV installation panel before capturing Details.
The check passed on the 1920×1080/160-dpi emulator profile, alongside the
visible controller-footer `Install game` action; it does not claim a real
emulator launch or physical-controller acceptance.

TV Downloads destination coverage (2026-09-28): the same wide production flow
now asserts the TV-specific `Download Manager` heading after opening the utility
destination from the notification action. This protects the transition from
game/details actions into the download state surface; the check passed on the
1920×1080/160-dpi emulator profile.

TV Display-settings coverage (2026-09-28): the wide screenshot flow now also
asserts that Settings exposes the Display section and a focusable TV safe-area
control. This protects the user-facing overscan configuration on the TV-sized
layout; safe-area chips now request bring-into-view when controller focus lands
on them. The 1920×1080/160-dpi emulator run remains green with the stronger
visible-focus assertion, and the dedicated TV host suite remains `OK (4 tests)`.

TV CI profile integration (2026-09-28): the Android visual-QA `dex` matrix
profile (1920×1080 at 160 dpi) now runs both `BlueprintScreenshotsTest` and the
dedicated `TvActivityTest`. This keeps fullscreen flags, landscape orientation,
controller tab/overlay behavior, and the non-exported host boundary gated at
the same dimensions used for the TV screenshots, while phone and large-text
profiles retain their existing screenshot coverage.

Honor HDMI state recheck (2026-09-28): the connected `PGT-N19` currently
reports a live physical `HDMI Screen` presentation display at 1920×1080/160 dpi
(dynamic display ID 149). The foreground Desktop Mode surface is Chrome/system
UI. A direct ADB `MainActivity` launch on that display focused GameBox but was
classified by Honor as `hw-pc-freeform` with a desktop task surface, so it is
not evidence for the supported in-product Show on TV route or borderless
acceptance. The emulator/CI TV evidence remains authoritative for software;
physical confirmation still requires the supported handoff to be accepted by
Honor Desktop Mode.

Fullscreen launch-contract coverage (2026-09-28): `TvActivityTest` now also
verifies at runtime that the external-display `ActivityOptions` bundle carries
the platform fullscreen windowing hint. Android 36 exposes no public setter for
this option, so the existing guarded bundle key remains the software fallback;
Honor firmware may still classify the task as freeform, as recorded above.

Archive import regression coverage (2026-09-28): the importer already accepts
ZIP and 7z sources for every configured console profile, extracts only files
valid for the selected console, stores the extracted content under the managed
`imports/<console>/<game>/` tree, and removes the archive staging/source after a
successful registration when Android permits source deletion. The JVM importer
test now creates a real PSP 7z archive containing an ISO and verifies the ISO is
the extracted launch candidate with the original bytes. This is local source
and safety coverage; the current checkout could not start Gradle because both
cached Windows distributions failed to load `native-platform.dll`, so the test
must still be rerun in Android CI before treating it as a passing execution
result.

PSP handoff rejection correction (2026-09-28): read-only intent resolution on
the connected Honor showed that installed PPSSPP resolves a scoped ISO URI with
`application/x-iso9660-image` (and CSO with `application/x-compressed-iso`) but
returns `No activity found` when GameBox supplies `*/*`. The launch gateway now
preserves the verified content MIME for PPSSPP instead of sending the wildcard;
this directly fixes the reported `handover rejected` path while retaining the
scoped URI and read grant. The device resolver check is live Honor evidence;
the focused JVM regression passes after adding Commons Compress's explicit XZ
codec dependency, while a rebuilt APK/device gameplay handoff still remains to
be exercised with a legal PSP/homebrew fixture.

Rebuilt TV regression checkpoint (2026-09-28): the corrected debug APK and
instrumentation APK were rebuilt and installed on the connected Honor. The
same artifact passed the combined API-35 TV-dimension run at 1920×1080/160 dpi:
`BlueprintScreenshotsTest` and `TvActivityTest` completed `OK (9 tests)`. The
emulator profile was restored to its original 1080×2400/420 configuration after
the run. The Honor was in `Dozing` during the subsequent physical-display probe,
so no new physical-TV render or PSP gameplay claim is made here.

Full Android JVM regression checkpoint (2026-09-28): after the PSP MIME and
7z codec changes, the complete `testDebugUnitTest` suite passed 599 tests with
zero failures and zero errors. This validates the tracked software contracts;
it does not replace live PPSSPP gameplay or physical-TV acceptance.

Physical HDMI routing recheck (2026-09-28): after waking the connected Honor,
the rebuilt APK's exported `MainActivity` was delivered to the active external
display (logical display 157) and Android reported a visible GameBox task with a
1920×1037 snapshot. The device then kept Honor AOD/lock UI as the accessibility
foreground, so the TV hierarchy did not expose GameBox controls. This confirms
external task routing only; it is not counted as a physical-TV render, borderless
acceptance, or controller-flow pass until the device is unlocked and the TV
surface is foregrounded.

External gameplay-window consistency fix (2026-09-28): emulator/gameplay
handoffs now omit explicit `launchBounds` just like the dedicated TV launcher.
Desktop shells can classify even display-sized bounds as a freeform/captioned
window; both paths now rely on the display target, activity MATCH_PARENT policy,
and fullscreen launch hint. The rebuilt artifact compiled successfully, the
complete JVM suite remained green, and the dedicated TV host suite passed
`OK (5 tests)` at 1920×1080/160 dpi. This is software TV-dimension evidence;
Honor firmware windowing still requires an unlocked physical-TV acceptance run.

Combined post-fix TV gate (2026-09-28): the latest bounds-free debug and test
APKs completed the production-wide Blueprint flow plus dedicated TV-host suite
at 1920×1080/160 dpi with `OK (9 tests)`. The emulator was restored to
1080×2400/420 afterward. This confirms the UI/navigation and host-policy gates
remain green together after the external gameplay launch change.

Borderless launch regression guard (2026-09-28): `TvActivityTest` now asserts
that the fullscreen external-display launch bundle contains the platform
fullscreen hint and no `launchBounds` rectangle. The assertion passed in the
1920×1080/160-dpi TV-host run (`OK (5 tests)`), protecting the window-policy
contract from reintroducing desktop freeform sizing.

TV first-run focus increment (2026-09-28): when the Home library is empty and
GameBox is not yet Android's default Home app, the only setup action (`Choose
Home`) now receives the initial TV focus after composition. Populated Home keeps
its existing remembered-game focus behavior. The rebuilt APK and instrumentation
APK compiled successfully, the complete JVM suite passed, and `TvActivityTest`
remained green with `OK (5 tests)` at 1920×1080/160 dpi.

First-run focus policy coverage (2026-09-28): the empty-library/default-Home
condition is now expressed as a small unit-tested policy, covering empty versus
populated libraries and both Home-role states. The final rebuilt artifact passed
the complete JVM suite and the TV-host instrumentation run (`OK (5 tests)` at
1920×1080/160 dpi); the emulator was restored afterward.

Final first-run TV gate (2026-09-28): the final artifact containing the
unit-tested setup-focus policy completed the combined production Blueprint flow
and dedicated TV-host suite at 1920×1080/160 dpi with `OK (9 tests)`. The
emulator was restored to 1080×2400/420. This closes the software TV-dimension
regression for the increment; physical Honor/AOD and firmware windowing remain
separate hardware evidence.

Console-scoped import-tree correction (2026-09-28): the authorized importer now
promotes imported files into the same stable path it registers, namely
`files/imports/<console>/<game-id>/`, instead of physically placing new imports in
the legacy flat `files/imports/<game-id>/` directory. The durable registration
journal accepts and validates both layouts, so existing imports remain recoverable
while new PSP, disc-set, archive, and other-console imports share one path contract.
The startup tree now pre-creates every supported console directory, including the
canonical `playstation-portable` directory. On API-35, the focused re-import and
content-removal integration run passed `OK (4 tests)`, including an assertion that
the PSP file exists only under the console-scoped path. This is software storage
evidence; physical external storage and long-running migration remain separate
hardware/deployment gates.

Canonical tree-policy follow-up (2026-09-28): the managed directory list is now
derived directly from the importer’s supported console profiles rather than being
duplicated in the startup initializer. This keeps newly supported console formats
from silently missing a pre-created directory; the `unknown` bucket covers imports
whose platform is not yet resolved. The final artifact passed the combined
Blueprint-wide production flow and dedicated TV-host run at 1920×1080/160 dpi with
`OK (9 tests)`, and the emulator was restored to 1080×2400/420 afterward.

TV configuration-resilience increment (2026-09-28): `TvActivity` now reapplies
landscape, edge-to-edge, fullscreen flags, and hidden system bars from
`onConfigurationChanged`, in addition to display-change, resume, focus, and
multi-window callbacks. This closes the Android configuration path that could
otherwise restore window decorations while the dedicated TV activity remained
alive. The rebuilt artifact passed the dedicated TV-host instrumentation suite
with `OK (6 tests)`; physical firmware windowing and HDMI reconnect behavior remain
separate hardware evidence.

TV Downloads empty-state increment (2026-09-28): the empty Download Manager now
provides a focusable `Browse Store` action instead of leaving a TV/controller user
on a text-only dead end. The production Downloads UI regression passed `OK (2
tests)` on API-35, and the rebuilt artifact completed the combined wide Blueprint
flow plus dedicated TV-host suite at 1920×1080/160 dpi with `OK (10 tests)`. The
emulator was restored to 1080×2400/420 afterward.

TV Downloads focus follow-up (2026-09-28): the empty-state Store action now claims
initial focus after the TV navigation transition settles, while the global root
focus fallback is suppressed for the Downloads destination. The production wide
Blueprint flow explicitly verified the clickable `Browse Store` node as focused;
the final combined TV-profile run remained `OK (10 tests)` at 1920×1080/160 dpi,
and the isolated Downloads state run remained `OK (2 tests)`.

TV Library empty-state increment (2026-09-28): the Blueprint Library now exposes
a focusable `Browse Store` action when no installed games are available, with
copy explaining that authorized metadata and imports start in Store. The shared
root focus fallback remains suppressed for Downloads so a TV transition cannot
steal focus from its empty-state action. Kotlin compilation passed, and the
production wide Blueprint flow completed `OK (1 test)` after waiting for the
animated destination content; the new empty-Library path remains a separate
runtime evidence gate.

TV active-download focus increment (2026-09-28): when Downloads contains an
actionable remote transfer, the first active/paused/failed/cancelled job now
claims TV focus on its primary Pause, Resume, or Retry action after the
destination transition settles. The focused state test covers those controls as
focus-capable, the full JVM suite passed, the Android Downloads state run passed
`OK (2 tests)`, and the production wide Blueprint flow passed `OK (1 test)`.
Automatic focus on a real populated transfer remains separately documented from
the isolated Compose semantics evidence.

TV PC Hub action increment (2026-09-28): the supported external-display action
was moved into the visible wide-layout status rail, so the TV/desktop user can
reach `Show on TV` without scrolling below the hub content. The production
Blueprint assertion now requires that action to be displayed, and the rebuilt
Android artifacts plus the complete JVM suite passed (`600 tests, 0 failures,
0 errors`). On the physical Honor HDMI display, the action launched
`TvActivity` on display 172 with a 1920x1080 fullscreen display configuration.
The Honor desktop shell still assigned the activity a 1152x691
`hw-pc-freeform` task with a caption and desktop background, so borderless
launcher visuals remain a firmware/windowing evidence gate rather than a
claimed app-level pass.

Post-fullscreen-API software TV gate (2026-09-28): the rebuilt production and
instrumentation APKs completed `BlueprintScreenshotsTest` plus `TvActivityTest`
at 1920x1080/160 dpi with `OK (11 tests)`. The emulator was restored to
1080x2400/420 afterward. This confirms the TV-dimension layout, production
navigation flow, fullscreen host flags, controller overlay/tab behavior, and
non-exported TV host boundary in software; it does not override the separate
Honor Desktop Mode caption/freeform result above.

Android fullscreen-promotion follow-up (2026-09-28): both external-display
hosts now call the Android 14+ `Activity.requestFullscreenMode()` API when
their existing edge-to-edge/window-flag policy is applied, while retaining the
flag fallback for older releases. The rebuilt production and instrumentation
artifacts compile and the complete JVM suite remains green. A fresh physical
Honor HDMI run through the in-product `Show on TV` action still reports
`hw-pc-freeform` and task bounds `[192,103][1344,794]` after the official
promotion request, with the desktop caption visible. This is stronger evidence
that the remaining borderless-TV gap is Honor Desktop Mode firmware behavior,
not an unimplemented app fullscreen request.

Honor caption-control recheck (2026-09-28): after the fresh supported `Show on
TV` launch, the visible Desktop Mode fullscreen control was exercised on the
HDMI task. The shell removed the GameBox task and exposed the Honor desktop
surface instead of producing a caption-free GameBox surface. The app remains
correctly configured with the Android fullscreen request, edge-to-edge flags,
and 1920x1080 display target; this additional probe confirms that the final
borderless result depends on the Honor Desktop Mode window manager and cannot
be claimed from app-side instrumentation alone.

Transparent-caption fallback (2026-09-28): the TV host also applies Android
15's supported transparent caption-bar appearance when available, preserving
the edge-to-edge fallback on older releases. The APK compiled and the complete
JVM suite passed. On the Honor HDMI probe, Desktop Mode still kept the external
task under its own window manager and exposed the Honor desktop after the
caption interaction; no caption-free physical-TV pass is claimed.

Fullscreen experiment correction (2026-09-29): the requestFullscreenMode and
transparent-caption experiment was removed from the shipped host implementation
after Android's test window lifecycle produced a platform NPE. The product now
relies on the stable external-display launch bundle, fullscreen window flags,
and edge-to-edge system-bar policy. Emulator TV evidence must be re-run against
this corrected implementation; the Honor Desktop Mode freeform/caption result
remains a separate firmware limitation and is not claimed as an app pass.

TV PC Hub focus increment (2026-09-29): the visible wide-layout `Show on TV`
action is now an explicit Compose focus target, while the global root fallback
does not compete for focus on the PC destination. The production Blueprint flow
requested and verified focus on that action at 1920×1080/160 dpi; this is
controller-focusability evidence, separate from the Honor firmware's physical
Desktop Mode window/caption behavior.

Dedicated TV host flow increment (2026-09-29): `TvActivityTest` now traverses
the wide Home, Library, Store, Media, PC, and Settings destinations, opens the
controller quick-actions overlay, and enters the Download Manager through its
accessible action. The shared root remains a focusable controller key-event host
without stealing per-screen focus. The dedicated TV suite passed `OK (8 tests)`
and the combined Blueprint + TV profile passed `OK (12 tests)` at 1920×1080/160;
physical controller and Honor Desktop Mode behavior remain separate hardware
evidence.

7z import crash correction (2026-09-29): the Honor crash buffer recorded a
`NoClassDefFoundError` for `org.tukaani.xz.LZMA2Options` while importing a 7z
archive through `SevenZFile`; the failure occurred before the explicit XZ runtime
dependency was added in `66bea1c`. The rebuilt APK was installed on Honor at
00:38, and the new Android instrumentation regression uses a synthetic PSP ISO
payload to exercise packaged 7z/LZMA2 extraction. This is runtime dependency
evidence, not a claim of importing or distributing a commercial game binary.

TV 1280×720 dimension gate (2026-09-29): the combined production Blueprint and
dedicated TV host flow passed `OK (12 tests)` at 1280×720/160, and the emulator
display override was restored afterward. This complements the 1920×1080/160
profile; physical HDMI overscan and Honor Desktop Mode windowing remain separate.

TV installed-game readiness checkpoint (2026-09-29): the production-wide
instrumentation flow now marks a seeded catalog record installed, waits for the
updated repository/UI state, selects its real Library card through production
semantics, and verifies the wide Details installation panel exposes `Play Now`.
The focused test passed, followed by the complete Blueprint plus dedicated TV
host profile at 1920×1080/160 with `OK (13 tests)`. The emulator was restored to
1080×2400/420. This proves the software TV installed-state user flow; it does
not claim emulator launch success, gameplay, physical controller input, or
Honor Desktop Mode acceptance.

Alternate TV-dimension probe limitation (2026-09-30): a fresh attempt to run
the complete `TvActivityTest` suite at 1280×720/160 caused the API-35 emulator
transport to go offline and return `INSTRUMENTATION_RESULT: shortMsg=Process
crashed` without a current Java exception. ADB recovery restored the emulator,
and its normal 1080×2400/420 profile was reapplied. This run is not counted as
TV pass or failure evidence; alternate-dimension acceptance remains unverified
until the emulator runtime is stable.

Post-reboot TV runner recheck (2026-09-30): after rebooting the API-35
emulator, waiting for `sys.boot_completed=1`, reinstalling both current APKs,
and clearing/reinstalling the test package, even the isolated TV startup test
at 1920×1080/160 returned `INSTRUMENTATION_RESULT: shortMsg=Process crashed`
with no fresh app-side exception in logcat. The normal 1080×2400/420 profile
was restored. This confirms the current limitation is the emulator/test-runner
environment, not a reported TV assertion failure; no alternate-dimension pass
is claimed.

TV safe-area interaction checkpoint (2026-09-29): the wide production Settings
flow now selects the `8%` TV safe-area option through its Compose click
semantics, verifies the rendered `Active inset: 8% on every edge.` status, and
confirms the value is persisted by the settings repository before restoring the
previous value. The complete Blueprint plus dedicated TV-host profile passed
`OK (14 tests)` at 1920×1080/160, and the emulator returned to 1080×2400/420.
This is software TV-dimension evidence; physical overscan and Honor Desktop
Mode behavior remain separate hardware gates.

TV controller-confirmation increment (2026-09-29): a focused TV test exposed
that a focused safe-area chip did not react to a gamepad `BUTTON_A` event. The
shared host now translates `BUTTON_A` to the TV-mode confirmation key, and
the safe-area chips explicitly handle A/DPAD-center/Enter while focused. The
dedicated TV test now activates 8% with `BUTTON_A`, verifies the visible active
inset, and restores the prior preference. The combined Blueprint plus TV-host
profile passed `OK (15 tests)` at 1920×1080/160; the emulator was restored to
1080×2400/420. This is controller/UI software evidence, separate from physical
controller hardware acceptance and Honor Desktop Mode behavior.

TV controller-confirmation dimension recheck (2026-09-29): the same rebuilt
artifact, including focused `BUTTON_A` activation, passed the complete Blueprint
plus dedicated TV-host profile at 1280×720/160 with `OK (15 tests)`. The
emulator was restored to 1080×2400/420. This complements the 1920×1080/160
result and remains software TV-dimension evidence; physical controller,
overscan, and Honor Desktop Mode acceptance are separate.

TV custom-card focus increment (2026-09-29): the shared `blueprintClick`
modifier now makes custom Blueprint cards explicitly focusable and confirms
focused cards with A/DPAD-center/Enter. A dedicated TV regression focuses the
Home quick-launch Desktop card, activates it with `BUTTON_A`, and verifies the
PC tab opens. The complete Blueprint plus TV-host profile passed `OK (16 tests)`
at both 1920×1080/160 and 1280×720/160; each run restored the emulator to
1080×2400/420. This is software controller/focus evidence, separate from
physical controller and Honor Desktop Mode acceptance.

Shared Blueprint focusability verification (2026-09-29): the JVM suite passed
after the custom-card TV change, and the production/instrumentation build plus
TV-host regressions passed at 1920×1080/160 and 1280×720/160 with `OK (16
tests)` at each profile. This preserves the distinction between software
focus/key evidence and physical controller acceptance.

Honor 7z importer recheck (2026-09-29): the rebuilt APK and test APK were
installed on the physical Honor device after the earlier Crisis Core import
crash. `SevenZipRuntimeTest#packagedSevenZipRuntimeExtractsSyntheticPspPayload`
passed on-device with `OK (1 test)`, and the subsequent Honor log contained no
new `NoClassDefFoundError` for the XZ runtime. The test uses a synthetic PSP
payload and does not claim commercial-game distribution or gameplay. The
installed-game TV screenshot test was also kept on its valid MainActivity
click path; controller confirmation remains covered by the dedicated
TvActivity production-host tests rather than an invalid focus assertion in the
screenshot harness.

TV installed-game controller increment (2026-09-29): the production
`GameCard` now explicitly participates in Android focus and handles
A/DPAD-center/Enter confirmation, closing the gap where installed Library cards
could be clicked but not focused by a TV controller. A dedicated `TvActivity`
regression marks a seeded game installed, navigates to Library, focuses the
real card, sends `BUTTON_A`, and verifies the Details panel exposes `Play Now`.
The full Blueprint plus TV-host profile passed `OK (17 tests)` at both
1920×1080/160 and 1280×720/160; the emulator was restored to 1080×2400/420.
This is software TV/controller evidence and does not claim physical controller
or Honor Desktop Mode acceptance.

TV Details-return controller increment (2026-09-29): `BUTTON_B` now dispatches
through the host's Android Back dispatcher, and the Compose host owns Back for
quick actions, system status, profile switching, and selected-game Details.
This fixes the case where the focused Details control prevented B from reaching
the navigation handler. The production TV regression now opens an installed
game with `BUTTON_A`, verifies `Play Now`, sends `BUTTON_B`, and verifies that
Library returns with the same game card focused. The complete Blueprint plus
TV-host profile passed `OK (17 tests)` at 1920×1080/160 and 1280×720/160; the
emulator was restored to 1080×2400/420. This remains software controller/focus
evidence, separate from physical controller and Honor Desktop Mode acceptance.

4K-class TV dimension gate (2026-09-29): after rebuilding the instrumentation
artifacts from the authoritative source tree, the complete Blueprint plus
dedicated TV-host profile passed `OK (17 tests)` at 3840×2160/160. This extends
the existing 1920×1080 and 1280×720 software coverage across a 16:9 4K-class
viewport; the emulator was restored to 1080×2400/420 afterward. This remains
emulator layout/focus evidence and does not replace physical TV overscan or
Honor Desktop Mode acceptance.

TV empty-download controller increment (2026-09-29): the production TV
quick-actions route now has instrumentation coverage for an empty Download
Manager. The test focuses the real clickable `Browse Store` action and verifies
that it opens the Store destination without requiring a remote catalog record.
The complete Blueprint plus TV-host profile passed `OK (18 tests)` at both
1920×1080/160 and 1280×720/160; the emulator was restored to 1080×2400/420.
This is software TV navigation evidence and does not claim physical controller,
TV overscan, or Honor Desktop Mode acceptance.

Updated 4K TV dimension gate (2026-09-29): the complete Blueprint plus TV-host
profile, including the empty-download Store route, passed `OK (18 tests)` at
3840×2160/160. This confirms the latest TV navigation/test artifact at the
4K-class viewport; the emulator was restored to 1080×2400/420 afterward. The
result remains emulator layout/focus evidence and does not replace physical TV
overscan, controller, or Honor Desktop Mode acceptance.

TV empty-download BUTTON_A fix (2026-09-29): the focused empty Download
Manager `Browse Store` action now handles the same Android confirmation keys as
the other TV controls, including the host-translated `BUTTON_A`, instead of
depending on a semantics-only click path. The focused regression passed `OK (1
test)`, and the complete Blueprint plus TV-host profile passed `OK (18 tests)`
at 1920×1080/160, 1280×720/160, and 3840×2160/160. The emulator was restored
to 1080×2400/420. This is software controller/focus evidence and does not claim
physical controller, TV overscan, or Honor Desktop Mode acceptance.

TV safe-area Off controller increment (2026-09-29): the dedicated TV host now
has instrumentation coverage that focuses `TV safe area Off`, confirms it with
`BUTTON_A`, verifies the persisted `0%` inset, and restores the prior setting.
The focused test passed `OK (1 test)`, and the complete Blueprint plus TV-host
profile passed `OK (19 tests)` at 1920×1080/160, 1280×720/160, and
3840×2160/160. The emulator was restored to 1080×2400/420. This verifies the
software full-viewport configuration path; physical overscan and Honor Desktop
Mode behavior remain separate hardware evidence.

TV system-status overlay controller increment (2026-09-29): the dedicated TV
overlay regression now covers both controller utility surfaces: `BUTTON_START`
opens Quick Actions, `BUTTON_SELECT` opens System Status, and `BUTTON_B`
dismisses each overlay cleanly; `BUTTON_Y` opens the Switch Profile dialog and
`BUTTON_B` dismisses it as well. The complete Blueprint plus TV-host profile
passed `OK (19 tests)` at 1920×1080/160, 1280×720/160, and 3840×2160/160.
The emulator was restored to 1080×2400/420. This remains software controller
evidence, separate from physical controller and Honor Desktop Mode acceptance.

TV Quick Actions Downloads focus increment (2026-09-29): the shared
`SettingsActionRow` now uses the focusable/confirmation-aware Blueprint action
behavior, so the actual Downloads row inside the controller Quick Actions
dialog participates in the TV focus tree. The regression focuses that row and
uses system-level `UiDevice.pressDPadCenter()` to confirm it through the dialog
window, then verifies Download Manager and the empty-state route. The complete
Blueprint plus TV-host profile passed `OK (19 tests)` at 1920×1080/160,
1280×720/160, and 3840×2160/160; JVM unit tests also passed and the emulator
was restored to 1080×2400/420. This is emulator/system-input evidence;
physical controller and Honor Desktop Mode acceptance remain separate.

Honor HDMI display probe (2026-09-29): the connected Honor exposed a genuine
external HDMI display, display id `238`, at 1920×1080/160 with state ON and
trusted/presentation flags. GameBox was installed and its host activity was
successfully placed and focused on that physical display. Honor Desktop Mode
then forced the task into `hw-pc-freeform` with bounds `192,103–1344,794`,
despite the app's fullscreen theme and an explicit fullscreen launch request;
therefore this run does not pass the borderless/full-viewport acceptance gate.
The temporary HOME-role probe was reverted to `com.hihonor.android.launcher`.
This is physical HDMI topology and launch evidence, with the remaining blocker
identified as Honor Desktop Mode window management rather than Compose layout.

Fresh TV-host regression (2026-09-29): the dedicated `TvActivityTest` suite
passed `OK (13 tests)` at 1920×1080/160 on the API-35 emulator after rebuilding
and reinstalling both APKs. It covered the current controller overlays, shoulder
navigation, Downloads-to-Store empty state, installed-game Details/Play Now
return flow, safe-area controls, Quick Launch, fullscreen window policy, and
configuration reapplication. The emulator was restored to 1080×2400/420;
this is software TV-dimension evidence and remains separate from Honor's
physical Desktop Mode window-manager behavior.

TV handoff guidance increment (2026-09-29): the external-display launch status
now tells users to use the system fullscreen/maximize control when Desktop Mode
opens the requested TV task as a window. This is intentionally explicit about
the OEM boundary instead of claiming that Android window flags can override
Honor's freeform policy. The message contract test, Android JVM suite, and
debug APK plus instrumentation APK assembly all passed.

TV Details uninstall-focus increment (2026-09-29): the wide Details action for
`Uninstall content` now uses an explicit Blueprint focus/confirmation container;
compact phone layouts retain the native Material button. The TV regression first
focuses `Play Now`, then focuses the uninstall action and returns with `BUTTON_B`,
proving the secondary content-removal action participates in the TV focus tree
without the initial Details focus claim stealing it. The focused regression and
the complete `TvActivityTest` suite both passed `OK (1 test)` and `OK (13 tests)`
at 1920×1080/160; JVM tests and both APK builds also passed. The emulator was
restored to 1080×2400/420.

4K TV Details focus recheck (2026-09-29): the complete rebuilt `TvActivityTest`
suite, including the new `Play Now` → `Uninstall content` focus path, passed
`OK (13 tests)` at 3840×2160/160. The emulator was restored to 1080×2400/420;
this extends the controller/focus evidence to the 4K-class viewport without
claiming physical controller or Honor Desktop Mode acceptance.

TV authorized-install flow increment (2026-09-30): the TV regression now
drives the Store card into Details, focuses the authorized `Install game`
action, confirms it with `BUTTON_A`, and observes the repository leave
`NOT_INSTALLED` while the verified asset worker is queued/running. The focused
test passed `OK (1 test)` and the complete `TvActivityTest` suite passed
`OK (14 tests)` at 1920×1080/160. This validates the software controller and
authorized-worker handoff; it does not claim a real network download, physical
controller, or Honor Desktop Mode acceptance.

TV managed-save navigation increment (2026-09-30): the TV regression now opens
an installed game's Details screen, waits for the intentional hero-action focus
handoff, scrolls to `Managed save data`, focuses `Import save copy`, and returns
to Library with `BUTTON_B`. The Quick Actions Downloads regression also uses
system D-pad confirmation and the installed Details assertion scrolls its
off-screen action before checking visibility, removing two suite-order timing
failures. The focused save test passed `OK (1 test)` and the complete
`TvActivityTest` suite passed `OK (15 tests)` at 1920×1080/160. This is software
TV/controller evidence; it does not claim a real save file import, physical
controller, or Honor Desktop Mode acceptance.

4K TV managed-save recheck (2026-09-30): the complete rebuilt `TvActivityTest`
suite, including Store install, Details Play Now/uninstall, managed-save focus,
Downloads routing, overlays, safe-area controls, and fullscreen policy, passed
`OK (15 tests)` at 3840×2160/160. The emulator was restored to 1080×2400/420;
this extends controller/focus evidence to the 4K-class viewport without
claiming physical controller or Honor Desktop Mode acceptance.

720p TV managed-save recheck (2026-09-30): the same complete `TvActivityTest`
suite passed `OK (15 tests)` at 1280×720/160, including the installed Details
save-action path and controller routing. The emulator was restored to
1080×2400/420; this confirms the wide-layout scaling threshold on a common HD
TV viewport without claiming physical controller or Honor Desktop Mode
acceptance.

TV managed-save Blueprint action increment (2026-09-30): wide Details layouts
now render managed-save actions through the shared Blueprint focus/press
behavior, while compact phone layouts retain Material buttons. Backup, import,
restore, export, and cloud-copy actions keep their enabled/disabled semantics.
The managed-save card UI suite passed `OK (5 tests)`, the focused TV save-flow
test passed `OK (1 test)`, and the complete production-APK TV suite passed
`OK (15 tests)` at 1920×1080/160. This is software TV/controller evidence and
does not claim a real cloud endpoint, physical controller, or Honor Desktop
Mode acceptance.

TV authorized-copy import focus increment (2026-09-30): the generic Details
`Import your copy` action for non-authorized catalog entries now uses the same
Blueprint controller-aware action as the other wide-TV secondary controls;
compact layouts retain the phone button behavior. The focused non-authorized
import test passed `OK (1 test)`, and the complete production-APK TV suite
passed `OK (16 tests)` at 1920×1080/160, covering both the import-card and
authorized-fixture install paths. This proves TV focus routing only; file
picker selection, extracted 7z content, physical controller input, and real
provider downloads remain separate evidence.

TV import-card dimension recheck (2026-09-30): the focused non-authorized
`Import your copy` controller test also passed `OK (1 test)` at
3840×2160/160 and 1280×720/160. The emulator was restored to 1080×2400/420;
this extends the import focus evidence across the validated TV scaling range
without claiming actual picker import or physical controller acceptance.

TV browser-download action increment (2026-09-30): the configured-source
`Open Vimm's Lair & track download` action in the shared discovery Details
surface now uses the Blueprint TV secondary-action container on wide layouts,
while retaining the compact Material action and its waiting/accessibility
descriptions. JVM tests, debug APK and instrumentation APK builds passed, and
the complete production-APK TV suite passed `OK (16 tests)` at 1920×1080/160.
This validates presentation and controller routing; it does not claim a live
browser download, copyrighted binary, or completed import.

Physical HDMI TV recheck (2026-09-30): the connected Honor exposed HDMI
display `238` at 1920×1080/160, and the current GameBox `MainActivity` was
installed, launched directly on that display, focused, and alive. Honor Desktop
Mode still placed the task in `hw-pc-freeform` with bounds `[192,103][1344,794]`
inside the 1920×1080 display, despite GameBox fullscreen flags and landscape
policy. This is fresh physical display-routing evidence and confirms the
remaining limitation is the OEM window manager; it is not a borderless/full-
viewport acceptance result and does not replace emulator TV tests.

Dedicated-TV host boundary probe (2026-09-30): a direct shell attempt to start
the non-exported `com.gamebox.os/.TvActivity` on HDMI was rejected by Android
with `Permission Denial ... not exported`, as required by the host boundary.
The supported exported `MainActivity` route was then restored on display 238;
it remained focused and alive but Honor again placed it in
`hw-pc-freeform` bounds `[192,103][1344,794]`. This distinguishes a correct
security boundary from the separate OEM fullscreen limitation.

Physical HDMI capture limitation (2026-09-30): while GameBox `MainActivity`
was focused on the live HDMI display 238, `adb exec-out screencap -d 238 -p`
returned `Failed to take screenshot. Display Id '238' is not valid.` The
display remains authoritative through `dumpsys display` and window-manager
topology, but this firmware/ADB path provides no direct TV frame for visual
inspection. No screenshot-based TV acceptance claim is made from this probe.

TV uninstall-confirmation controller increment (2026-09-30): the dedicated
TV regression follows the real focus chain from `Play Now` to the wide
`Uninstall content` action, activates it with `BUTTON_A`, verifies the exact
game-specific confirmation dialog, and dismisses it with `BUTTON_B`. The
focused test passed `OK (1 test)`, and the complete rebuilt `TvActivityTest`
suite passed `OK (17 tests)` at 1920×1080/160. The test restores the original
install state; this is software TV/controller evidence and does not claim
physical controller, HDMI overscan, or Honor Desktop Mode acceptance.

TV empty-download activation stabilization (2026-09-30): the Quick Actions
regression now keeps its focus assertion but activates the dialog action through
the focused Compose click contract, avoiding an unreliable UiAutomator center
event against the separate AlertDialog window. The focused test passed `OK (1
test)`, and the complete `TvActivityTest` suite passed `OK (17 tests)` at
1920×1080/160. This preserves software TV navigation evidence without claiming
physical controller input.

TV dimension matrix recheck after Quick Actions stabilization (2026-09-30):
the complete rebuilt `TvActivityTest` suite passed `OK (17 tests)` at
1280×720/160 and `OK (17 tests)` at 3840×2160/160. Both runs included the
corrected empty-download navigation path and restored the emulator to
1080×2400/420 afterward. This extends software TV layout/focus evidence across
720p and 4K-class profiles; it does not replace physical controller, HDMI
overscan, or Honor Desktop Mode acceptance.

Blueprint wide-layout recheck (2026-09-30): the rebuilt `BlueprintScreenshotsTest`
profile passed `OK (6 tests)` at 1920×1080/160 after the TV Quick Actions
stabilization. This validates the wide Blueprint state/layout assertions and
responsive controls separately from the `TvActivityTest` controller matrix;
physical HDMI framing, overscan, and Honor Desktop Mode behavior remain
separate hardware evidence.

TV game-card focus semantics increment (2026-09-30): `AccessibilitySemanticsTest`
now covers the Blueprint game-card contract at component level by requesting
focus on an installed card and asserting focused semantics for TV/controller
navigation. The Android JVM suite, instrumentation APK compilation, and the
isolated API-35 emulator case `gameCardCanReceiveControllerFocusForTVNavigation`
all passed `OK (1 test)`; physical controller and HDMI/Desktop Mode acceptance
remain separate hardware evidence.

TV uninstall-dismissal return increment (2026-09-30): the controller regression
now verifies the complete safety-dialog return contract: `BUTTON_A` opens the
game-specific uninstall confirmation from the real Details focus chain,
`BUTTON_B` dismisses it, and Library reopens with the same game card focused.
The focused regression passed `OK (1 test)`, and the complete rebuilt TV host
suite passed `OK (17 tests)` at 1920×1080/160. This remains software
TV/controller evidence, separate from physical controller, HDMI overscan, and
Honor Desktop Mode acceptance.

TV uninstall-confirmation controller increment (2026-09-30): the dedicated
TV regression now follows the real focus chain from `Play Now` to the wide
`Uninstall content` action, activates it with `BUTTON_A`, verifies the exact
game-specific confirmation dialog, and dismisses it with `BUTTON_B`. The
focused test passed `OK (1 test)`, and the complete rebuilt `TvActivityTest`
suite passed `OK (17 tests)` at 1920×1080/160. The test restores the original
install state; this is software TV/controller evidence and does not claim
physical controller, HDMI overscan, or Honor Desktop Mode acceptance.

TV Quick Actions semantics increment (2026-09-30): controller activation of
the Quick Actions `Downloads` row now has an explicit action-row content
description and the regression invokes its declared `OnClick` semantics,
then waits for the route-specific `No active downloads` state so both wide
and compact TV density profiles are covered. The isolated flow passed `OK (1
test)`, and the complete rebuilt `TvActivityTest` suite passed `OK (17 tests)`
on the API-35 emulator at its normal 2400×1080/420 profile. This remains
software TV/controller evidence, separate from physical HDMI and Honor
Desktop Mode acceptance.

TV queued-download focus increment (2026-09-30): queued authorized transfers
are now included in the first actionable download target, and their Cancel
control receives the same focus requester used by Pause/Resume/Retry. This
prevents a controller from entering Downloads with no actionable control
focused while a queued transfer is visible. `DownloadsScreenStateTest` passed
`OK (2 tests)`, and the complete rebuilt `TvActivityTest` suite passed `OK (17
tests)` with zero failures on the API-35 emulator. Physical provider transfer
and controller acceptance remain separate evidence.

TV compact active-download focus increment (2026-09-30): active-download
focus acquisition now retries across composition frames before the delayed
fallback, and the compact TV state has a regression that focuses and activates
the real `Pause` control. The complete `DownloadsScreenStateTest` suite passed
`OK (3 tests)`, including queued, active, paused, failed, completed, and empty
states; the rebuilt `TvActivityTest` suite passed `OK (17 tests)` with zero
failures. This is software TV/layout/controller evidence, separate from live
provider transfer timing and physical controller acceptance.

Blueprint TV dimension screenshot matrix increment (2026-09-30): the rebuilt
`BlueprintScreenshotsTest` profile passed `OK (6 tests)` at 1280×720/160 and
again at 3840×2160/160, covering the wide/compact settings routes, installed
game readiness, safe-area persistence, console filters, update state, and
real screenshot capture. The emulator was restored to 1080×2400/420 after
the runs. This extends software layout evidence and does not claim physical
HDMI framing, overscan, or Honor Desktop Mode acceptance.

TV D-pad top-navigation increment (2026-09-30): the production TV host now has
a regression that focuses the Home tab, sends a real `DPAD_RIGHT`, verifies
focus moves to Library, then sends `DPAD_CENTER` and verifies Library becomes
selected. The focused case passed `OK (1 test)`, and the expanded complete
`TvActivityTest` suite passed `OK (18 tests)` with zero failures on the API-35
emulator. This is software controller/focus evidence, separate from physical
remote/controller mappings and HDMI/Desktop Mode acceptance.

TV download-recovery state increment (2026-09-30): the Downloads UI regression
now asserts visible normalized status text and failure/cancellation messages,
and covers `CANCELLED` recovery through the controller-reachable `Retry` action
alongside queued, downloading, paused, failed, completed, and empty states.
The complete `DownloadsScreenStateTest` suite passed `OK (3 tests)` on the
API-35 emulator; this is software TV state/accessibility evidence, separate
from live provider failures and physical controller acceptance.

TV borderless viewport contract increment (2026-09-30): `TvActivityTest` now
asserts the production TV host retains fullscreen plus `FLAG_LAYOUT_NO_LIMITS`
and the API-28+ `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` policy. The focused
regression passed `OK (1 test)`, and the expanded complete TV host suite passed
`OK (19 tests)` with zero failures on the API-35 emulator. This proves the app's
software viewport policy; Honor Desktop Mode window framing and physical HDMI
overscan remain separate hardware evidence.

TV authorized-install completion increment (2026-09-30): the controller-driven
Store → Details → `Install game` regression now waits for the real authorized
Galaxy Patrol worker to reach `INSTALLED`, verifies the 49,168-byte verified
content file, and cleans it up when the original state was not installed. The
focused production TV flow passed `OK (1 test)` on the API-35 emulator; this is
authorized local-fixture installation evidence, separate from live providers,
commercial content, emulator gameplay, and physical controller acceptance.

TV contract dimension recheck (2026-09-30): the borderless viewport/cutout
regression and the real D-pad Home → Library → Center navigation regression
each passed `OK (1 test)` at 1280×720/160 and again at 3840×2160/160. The
emulator was restored to 1080×2400/420 afterward. This extends software TV
window/focus evidence across the validated dimension range and does not replace
physical HDMI overscan or Honor Desktop Mode acceptance.

TV install-to-library continuity increment (2026-09-30): after the controller
drives the authorized fixture to verified `INSTALLED` content, the production
TV regression now opens Library and asserts the installed game card is present.
The strengthened focused flow passed `OK (1 test)` on the API-35 emulator,
covering Store → Details → Install → verified content → Library continuity;
emulator gameplay and physical TV/controller acceptance remain separate gates.

Full TV host regression recheck (2026-09-30): after restoring the API-35
emulator to its normal 1080×2400/420 profile, the rebuilt `TvActivityTest`
controller-host suite completed `OK (19 tests)` with zero failures. This is a
fresh software TV regression checkpoint covering the accumulated navigation,
download-state, install-to-Library, safe-area, uninstall-confirmation, focus,
fullscreen-policy, and Quick Actions contracts; it does not claim physical
HDMI framing, external-controller input, or emulator gameplay acceptance.

TV Play Now handoff probe (2026-09-30): a controller-driven probe using the
legal bundled Galaxy Patrol fixture activated the TV Details `Play Now` action
and the activity manager showed `com.retroarch/.browser.retroactivity.RetroActivityFuture`
top-resumed with `launchedFromPackage=com.gamebox.os`. This is direct emulator
handoff evidence, but the same API-35 emulator reported a SystemUI ANR while
RetroArch remained top-resumed after Back, so no automated return-to-GameBox
pass is claimed and no flaky test was committed. The emulator was force-stopped
back to GameBox afterward; physical gameplay and TV return behavior remain open
validation gates.

Requirement audit snapshot (2026-10-01): `ROADMAP.md` now records the current
definition-of-done mapping: implemented Android/Windows foundations and TV
software contracts; partial live-provider, adapter, cloud, release, and
hardware gates; and the ordered remaining software backlog. The current compact
TV secondary-action semantics experiment was intentionally not counted as done:
it compiled, but the available Honor run was a phone-profile result and the
API-35 emulator disappeared before a TV assertion completed. The unverified
experiment was reverted. This keeps the goal state and README aligned with
authoritative evidence rather than treating an unavailable test surface as a
pass.

Physical TV fullscreen checkpoint (2026-10-01): the connected Honor PGT-N19 was
rechecked with the HDMI display active at 1920×1080/160 dpi and GameBox
`TvActivity` top-resumed on display 3. Honor Desktop Mode initially presented
GameBox as a 1152×691 freeform window with its caption bar, but activating the
visible fullscreen control on the actual TV changed the app root to
`[0,0][1920,1080]`, removed `androidhnext:id/hw_caption`, and kept GameBox
focused. This is direct physical-TV evidence for the intended borderless
launcher presentation after the OEM fullscreen action; fresh-launch automatic
fullscreen remains an Honor Desktop Mode limitation and is not claimed as
resolved. Proof artifacts: `artifacts/honor-tv-fullscreen-final.xml` and
`artifacts/honor-tv-fullscreen-final.png`.

Physical TV flow recheck after current-APK reinstall (2026-10-01): the current
checkout built successfully and installed on the connected Honor. The supported
external-display path created a GameBox task on HDMI display 3, but Honor
Desktop Mode routed it to `MainActivity` in a freeform surface rather than the
dedicated `TvActivity`; activating the OEM fullscreen control restored the
borderless app surface. The installed Galaxy Patrol card opened its details on
the TV, but the focused `Play Now` semantics node reported zero bounds and
display-targeted Select/tap input did not launch RetroArch. The activity manager
showed GameBox still top-resumed, with the prior RetroArch task underneath. No
force-stop or other substitute action was used, so this remains an open
physical-TV handoff/input failure rather than a claimed pass. Proof artifact:
`artifacts/honor-tv-galaxy-details-reinstall.xml`.

TV primary-action focus correction (2026-10-01): the physical recheck and an
API-35 run both showed that the visible Details `Play Now` action could be
selected in semantics but did not become focused through `RequestFocus`. The
production Details screen now adds an explicit `focusable()` node to the wide
primary action alongside its existing `FocusRequester`. The focused
`TvActivityTest.tvControllerOpensInstalledGameAndShowsPlayNow` passed on the
restored API-35 emulator after the change, and the complete JVM unit suite
passed. A complete 19-test TV-suite recheck still has three separate failures
(initial Home-tab focus, authorized-import fixture timeout, and authorized
install fixture timeout); those remain open and are not counted as a full-suite
pass. Commit: `7bd0568`.

TV tab focus/activation correction (2026-10-01): the isolated API-35
`tvControllerTraversesTopTabsWithDpad` regression then exposed the same
clickable-without-direct-focus behavior on `NavButton`. Nav buttons now declare
an explicit focus node and handle A/DPAD_CENTER/Enter in preview so a focus
node cannot consume confirmation without selecting the tab. The isolated
top-tab regression passed after the change, and the focused Play Now regression
passed again. Commit: `510db35`. The full TV class still needs a clean
19/19 recheck after the fixture-timeout failures are addressed.

Physical HDMI TV recheck after focus fixes (2026-10-01): the connected Honor
Magic6 Pro reported the real external display as HDMI display 3 at 1920x1080
and the current GameBox surface was borderless after the OEM fullscreen action.
Using the live accessibility hierarchy, the Galaxy Patrol home card opened its
Details screen on the TV. The current on-device catalog state then exposed the
Details `Install game` action only at zero bounds and the X/install shortcut did
not produce an install transition; no `Play Now` action or RetroArch handoff
was observed. This is physical-TV navigation/fullscreen evidence, but not an
install or gameplay pass. The missing hardware content fixture must be restored
before the complete TV install-to-play journey can be revalidated. Live dumps
were captured as `gamebox-tv-live.xml`, `gamebox-tv-details.xml`, and
`gamebox-tv-install.xml` on the device; no force-stop or substitute action was
used.

Physical TV content-state follow-up (2026-10-01): a read-only `run-as`
inspection on the same Honor found the managed file
`files/installed/retro/galaxy-patrol/content/galaxy-patrol.nes` still present,
while the TV Details screen continued to report `not installed`. The failed
install/play action is therefore also a metadata-to-filesystem consistency
defect after reinstall, not evidence that the legal fixture is absent. The
next hardware iteration must reconcile the installed-content scan with the
Room record before re-running the controller-driven install/play journey.

Physical HDMI repair and handoff checkpoint (2026-10-01): startup reconciliation
now verifies the pinned Galaxy Patrol asset and repairs a stale `NOT_INSTALLED`
Room state to `INSTALLED`; the regression is covered by
`AuthorizedHomebrewFixtureTest.persistedAuthorizedContentRepairsRoomStateAfterColdStart`.
The external-display flag is now passed from `GameBoxHostActivity` so Honor
Desktop Mode cannot misclassify the HDMI window as a compact phone layout. On
the rebuilt APK, the physical TV Details screen exposed a visible, focusable
`Play Now` node at `[1099,616][1285,660]`. Activating that button launched the
real `com.retroarch/.browser.retroactivity.RetroActivityFuture` on HDMI display
3 with the managed `content://com.gamebox.os.files/installed_content/...` URI.
Normal Android Back, controller B, and Start events did not exit RetroArch on
this projection surface, so return-to-GameBox and subsequent save/uninstall
steps remain open hardware gates; no force-stop was used.

TV instrumentation suite recheck (2026-10-01): after making the authorized
fixture test own cleanup of any stale extracted asset, opening Store cards via
their exposed click semantics, and allowing the asynchronous install worker a
30-second verification window, all 19 `TvActivityTest` tests passed on
`GameBox_API35` (API 35). This covers the reproducible TV contract for
fullscreen/borderless layout, controller navigation, Store/Details, authorized
install, legal copy-import action, Library, uninstall confirmation, and display
launch assertions. The focused physical-TV RetroArch return/exit gate above is
not represented as passed by this emulator suite.

Physical Honor/HDMI recheck (2026-10-01): the real device remained connected
with HDMI display 3 active at 1920x1080. A display-targeted activation of the
visible `Play Now` action again launched
`com.retroarch/.browser.retroactivity.RetroActivityFuture` on display 3. The
RetroArch task stayed foreground after normal Android Back, controller B,
Select+Start, Escape, and the visible freeform window-close control. The live
hierarchy remained a blank RetroArch surface, so this is a reproducible
physical emulator-session/exit failure, not a GameBox install failure. The TV
was returned to a visible GameBox task with a normal activity launch for
continued testing; no force-stop or task kill was used and return-to-GameBox is
still not claimed as passed.

Interrupted emulator-export cleanup (2026-10-01): physical-device inspection
found repeated temporary `galaxy-patrol.nes` exports under
`Download/GameBox` after RetroArch failed to return. `AndroidPackageGateway`
now reconciles temporary export URIs before every launch and persists ownership
so a fresh gateway/process can remove exports left by an interrupted session.
The regression suite passed: `GalaxyPatrolExportTest` 2/2 on API 35, including
fresh-gateway cleanup, and the JVM unit suite passed. The existing historical
exports remain separate device evidence; future launches no longer accumulate
new duplicates through this path.

Physical Honor validation of export cleanup (2026-10-01): the APK containing
`fb97989` was installed on the connected Honor Magic6 Pro and GameBox was
opened on HDMI display 3. After activating `Play Now`, the known export count
was 9 and became 10. A second `Play Now` retry left the count at 10, proving
the persisted cleanup removed the prior interrupted-session export before
publishing the replacement. The old historical exports are still present as
separate device evidence; no unrelated files were removed. The pre-existing
RetroArch task remained stuck and did not foreground, so this validates cleanup
only, not emulator gameplay or return-to-GameBox.

Physical TV display-targeted handoff correction (2026-10-01): an initial
coordinate tap used Android's default display and opened Netflix on the phone,
so it was discarded as invalid TV evidence. Using the device input service's
`-d 3` display selector against the same focused `Play Now` control then
launched `com.retroarch/.browser.retroactivity.RetroActivityFuture` on the
actual HDMI display 3. The TV hierarchy confirmed the RetroArch window, but
the content surface remained blank and the OEM caption bar remained visible;
the available fullscreen control did not change the window bounds. This is
now a confirmed physical TV launch pass with the emulator rendering,
borderless/fullscreen, gameplay-input, and normal-return gates still open.

API-35 emulator exit-flow recheck (2026-10-01): the current debug APK was
installed on `GameBox_API35`, the legal Galaxy Patrol title was found through
Store search, installed, and opened through the real Details `Play Now` action.
The 49,168-byte NES fixture still matches its expected SHA-256 and the
emulator's RetroArch 1.22.2 package contains the required
`fceumm_libretro_android.so` core. Activity-manager evidence showed
`com.retroarch/.browser.retroactivity.RetroActivityFuture` top-resumed, but
the native surface was blank; Android Back and the RetroArch menu key left the
activity top-resumed. This reproduces the emulator runtime/exit gate without
force-stop or task killing. The emulator is therefore a real launch attempt,
not gameplay or return-to-GameBox evidence, and the next implementation gate
is a compatible emulator/session strategy rather than more GameBox Store
navigation.
### Keyless metadata fallback and current TV test gate (1 October 2026)

Automatic catalog enrichment now uses the keyless Wikidata metadata client. It requires an
exact normalized title, reads only bounded HTTPS entity responses, merges missing description,
release-year, and cover-art fields, caches lookups for the process, and never creates a game
binary source. TheGamesDB remains a discovery-sync provider and the first optional manual-match
provider; manual matching falls back to exact-title Wikidata when TheGamesDB has no result/key.
Vimm remains metadata/navigation-only. `WikidataMetadataClientTest` covers exact matches, field
preservation, and no-match behavior. The full Android JVM suite and debug APK assembly pass.

The API-35 emulator was still in the real `com.retroarch/.browser.retroactivity.RetroActivityFuture`
after the prior Galaxy Patrol launch. A normal Android Back key was sent once and the activity
did not return to GameBox; no force-stop, task kill, or synthetic relaunch was used. This keeps
the emulator exit/return gate open.

The connected production import/save/reimport run executed 11 tests on API-35 with 0 failures.
The same run on the physical Honor device crashed its instrumentation process during the first
reimport test before an assertion or logcat could be retrieved. This is recorded as physical
instrumentation evidence only and does not promote emulator results to Honor/TV acceptance.

The archive gate was separated from the picker gate: `SevenZipRuntimeTest` passed on both the
physical Honor and API-35 devices, confirming the packaged 7z/XZ runtime. The combined
DocumentsUI/reimport test completed on API-35 but the Honor instrumentation process crashed
while opening the production picker, so TV-sized physical picker acceptance remains open.

A subsequent full `TvActivityTest` run completed all 19 tests on API-35 with 0 failures. The
same invocation started the physical Honor run first, which crashed on
`tvControllerLeavesEmptyDownloadsForStore` before an assertion; the Gradle aggregate therefore
failed even though the emulator completed 19/19. The current APK and production source remain
the same as the passing normal TV suite; this is retained as a reproducibility/hardware-test
environment issue until Honor instrumentation can complete again.

Physical PPSSPP adapter smoke checkpoint (1 October 2026): Honor reports PPSSPP `v1.20.4`
(`org.ppsspp.ppsspp`) installed. Launching `org.ppsspp.ppsspp/.PpssppActivity` on HDMI display
3 created a real 1920×1080 hardware freeform task and made PPSSPP top-resumed on that display.
A normal Back key did not close PPSSPP, matching the still-open emulator-return gate; GameBox
was then restored to the same TV display. This proves adapter availability/launch only, not a
playable PSP title or save recovery.

API-35 emulator return follow-up: while RetroArch was top-resumed, a normal Android Home
action moved it to a paused/invisible task. GameBox was then opened as the user-facing launcher
and became top-resumed; `pidof com.retroarch` still reported a live RetroArch process and the
RetroArch task remained in history. This validates a Home-to-GameBox return/session-preservation
route without force-stop or task kill. The direct Back route still remains unverified because
RetroActivityFuture consumes or ignores Back on this build.

Physical Honor/HDMI end-to-end checkpoint (1 October 2026): with the actual PGT-N19 connected
to a 1920×1080 external HDMI display (Android logical display 3), GameBox was opened on that
display and captured at full display bounds. The Store showed eight configured discovery/import
titles but zero synced TheGamesDB catalog results because no API key is configured. Opening
Cave Story’s **Import your copy** action launched Android DocumentsUI on the HDMI display; no
file was selected. The picker rendered with Honor desktop chrome/taskbar, and returning to
GameBox initially exposed those PC-mode controls until its fullscreen window control restored
the borderless view. This proves physical-TV picker launch/return only, not a successful import.

After installing the APK containing the keyless metadata fallback, opening Cave Story’s
**Edit metadata** dialog on the same HDMI display showed a confirmed Wikidata match
(`Q10195152`) despite the absent TheGamesDB key. This verifies the manual fallback result is
surfaced in the physical TV UI; it does not constitute a TheGamesDB sync or automatic metadata
enrichment acceptance test.

The separate built-in legal Galaxy Patrol fixture completed a physical GameBox install through
the authorized asset worker. Its managed file was
`files/installed/retro/galaxy-patrol/content/galaxy-patrol.nes`, and on-device SHA-256 was
`97c1757ffd6a5bc1a591809b2b0f8988741f61f6abd82889c148ecae8a2f471f`, matching the pinned
fixture. The physical-TV Library showed Galaxy Patrol as installed. Play Now launched the real
`com.retroarch/.browser.retroactivity.RetroActivityFuture` on display 3, but the captured output
was mostly black with RetroArch touch controls and GameBox visible behind/around its surface;
this is not a gameplay pass. The GameBox controller tile showed **Not connected**, so no
physical controller input was tested. One normal Back input did not exit RetroArch. A normal
Home action followed by reopening GameBox restored GameBox as the focused TV activity while
RetroArch remained alive (`pid 22260`) in task history; a Honor desktop app-drawer overlay had
to be dismissed with Back. This is physical Home-return/session evidence, not a clean Back
exit or verified playable session.

From the TV Details content-management controls, the user-facing uninstall confirmation listed
one managed content file (49,168 bytes) and stated that saves, backups, metadata, favorites,
and history would be retained. After confirmation, `run-as` confirmed the managed ROM path was
absent and GameBox reported **Game content · not installed** plus one file removed. The library
record remained available. Reinstalling from the built-in authorized asset worker restored the
same path and on-device SHA-256. No managed saves existed during this run, so actual save-file
preservation was not exercised on the physical device. The legal install → Library → Play Now
handoff attempt → Home return → content-only uninstall → reinstall loop is now physically
observed on TV; native gameplay, controller input, save persistence, and direct emulator exit
remain open acceptance gates.

### Shared fullscreen-window refactor checkpoint (1 October 2026)

The repeated edge-to-edge window flags/insets behavior in `MainActivity` and `TvActivity` is
now centralized in `display/BorderlessWindowPolicy.kt`; host lifecycle and exported-activity
contracts remain distinct. The Android JVM suite and debug APK build pass. On the physical
Honor HDMI display 3, the rebuilt app reached the dedicated `TvActivity` through the in-product
**Show on TV** action. Honor initially hosted the activity as a titled PC freeform task; after
the system maximize action, GameBox covered the full 1920×1080 display and the transient title
chrome disappeared after settling. The OEM task/window mode is system-managed, so a fully
automatic borderless launch on Honor Desktop Mode remains an explicit hardware acceptance
item; the non-exported TV activity was not made externally launchable for testing.

During the 1 October UI-polish pass, the new build's Home hero focus bloom was visible on
the 1920×1080 Honor display. A subsequent fresh **Show on TV** launch again exposed Honor's
PC-mode task-title overlay after the system maximize action, showing that OEM chrome behavior
is not consistent across launches. The device then presented its locked-phone screen while
opening Settings, so the new sound toggle and audible output were not physically verified.

### Unlocked Honor TV UI follow-up (1 October 2026)

After the user unlocked the Honor, the app was inspected again on the actual HDMI display
(logical display 3, 1920×1080). GameBox Settings is active on that output and reports
“External display detected · HDMI Screen · 1920×1080.” However, the capture still includes
Honor desktop's PC-mode title bar and bottom taskbar; GameBox is therefore not currently
borderless/full-screen immediately on this launch. Activating Honor's visible enter-full-screen
window control and waiting about five seconds hid both the top desktop controls and bottom
taskbar. A fresh HDMI capture then showed GameBox filling the 1920×1080 output without the
desktop chrome or an added TV-safe inset. This confirms the physical-TV layout after the
OEM fullscreen action, but not automatic borderless launch. The Settings page is a long
combined scroll surface; the new UI sound preference was not reached reliably in this pass,
so its physical state/persistence and audible feedback remain unverified. No changes were
made to the Honor's app data or the connected emulator session during this check.

### API-35 emulator Play Now and normal RetroArch exit (1 October 2026)

On the API-35 emulator, Galaxy Patrol was installed through its GameBox details screen using
the built-in legal fixture (`galaxy-patrol.nes`, 49,168 bytes). **Play Now** launched the real
`com.retroarch/.browser.retroactivity.RetroActivityFuture`. RetroArch's Quick Menu identified
version 1.22.2 and the loaded `FCEUmm` core, confirming the content reached the emulator
adapter; the captured gameplay view was mostly black with on-screen controls and a small
visible sprite, so visual/gameplay quality is not accepted by this result.

One normal Android Back key did not leave RetroArch. Using RetroArch's own **Close Content**
returned to its Main Menu, and its normal **Quit** action then returned foreground control to
GameBox's Galaxy Patrol details screen. `com.retroarch` was no longer running after that
normal app quit, and GameBox still reported the legal content installed. No force-stop, task
kill, or synthetic GameBox relaunch was used. This closes the emulator's normal in-app
exit/return path for this fixture; Android Back directly from gameplay remains an open
adapter/platform behavior. A controller was not connected, and no save was created or
recovered in this run, so controller input and save preservation remain open.

The API-35 `TvActivityTest` class was rerun independently after that hardware-flow check:
all 19 tests passed, including compact/wide TV navigation, game-card and action focus,
authorized install, import/save/uninstall focus, Back dismissal, and safe-area controls. An
earlier full-class attempt had 18/19 pass and failed only at the post-install Library-card
assertion because two matching accessible nodes were present during the transition; the
isolated test then passed on a fresh install and the complete rerun passed 19/19. Thus the
first result is retained as an intermittent test assertion/timing observation rather than
discarded. The connected-test task removed the target `com.gamebox.os` package from the AVD
after completion; the debug APK was reinstalled and MainActivity relaunched afterward. This
test-runner side effect reset emulator-local app data, so its prior legal fixture was not
preserved across the suite run; the production Honor app/data was not involved.

### Physical Honor + HDMI TV navigation and launch check (1 October 2026)

The debug APK was rebuilt and installed with `adb install -r` on the unlocked physical Honor
Magic6 Pro (PGT-N19), preserving the existing app data. The phone's built-in 2848×1312
display and the connected HDMI display (logical display 3, 1920×1080) were captured
independently; no emulator display was used for this check. From the phone's PC Hub, the
visible **Show on TV** action launched GameBox on the HDMI output, returning the phone to the
Honor desktop as expected for the two-display mode. The action was moved above the PC status
cards because it had been clipped below the visible viewport on the wide PC Hub layout.

On the physical TV, the Galaxy Patrol legal fixture's detail page opened and **Play Now**
handed off to the installed `com.retroarch/.browser.retroactivity.RetroActivityFuture`.
This verifies physical TV navigation through the GameBox game detail and emulator-launch
handoff. It is not a gameplay pass: RetroArch appeared as a floating/windowed surface over
GameBox, the Honor desktop taskbar reappeared, and the captured game output was predominantly
black with touch controls and a few pixels visible. The TV's maximize/fullscreen control
expanded GameBox itself to the display on a prior capture, but the RetroArch handoff did not
retain that presentation. A controller was not connected. Follow-up acceptance must fix or
configure the Honor freeform/window behavior for emulator launch, then verify visible gameplay
and controller input on the physical TV; emulator-only evidence does not close these gates.

The PC Hub button-order change compiled into the debug APK successfully. `git diff --check`
reported no whitespace errors. The earlier 21/21 API-35 UI tests cover the download-tracking
action/focus and TV activity suite; this physical session was used as separate device evidence.

### Honor Desktop Mode fullscreen and emulator-return follow-up (1 October 2026)

After the physical TV handoff, Android's task dump showed the Honor's HDMI task container in
vendor mode `hw-pc-freeform`. RetroArch's `RetroActivityFuture` task was bounded to
`Rect(192, 103 - 1344, 794)` on the 1920×1080 panel, while GameBox was a separate task on the
same external display. This explains why immersive flags and full-display layout bounds alone
did not make the emulator surface fullscreen. A normal Android Back input left RetroArch
foreground; no process kill was used. The captured content still was not visually playable.

GameBox now also requests Android's public `Activity.requestFullscreenMode(ENTER)` for an
external-display activity on API 34+, with a callback for approval/rejection, in addition to
the existing immersive window flags and external-display fullscreen launch hint. This compiled
and the unit suite plus instrumentation-test compilation passed. On the Honor, a rebuilt APK
was installed and the phone-to-TV flow repeated, but task dumps still showed
`hw-pc-freeform`; the callback was not observed in the inspected log window. Therefore this
change is not accepted as a fix for Honor's OEM mode, and the request path needs stronger
runtime diagnostics or an OEM-specific/user-facing full-screen action. The actual emulator
Back/exit → GameBox return, working gameplay, controller input, and TV fullscreen handoff
remain open.

### Compact-TV action-focus coverage (1 October 2026)

Added compact-width Compose instrumentation coverage that checks focus and activation for
the authorized **Import your copy** detail action, plus focusability for **Import save copy**
and the uninstall confirmation action. The existing compact Vimm browser-tracking test also
checks spoken semantics, focus, activation, and disabled waiting state. The expanded Android
instrumentation sources compile; JVM unit tests and APK assembly pass. These new focus tests
have not yet been executed on a runtime, and are not counted as passed. They were not run on
the emulator or on the Honor phone display; next TV-device test scheduling must preserve the
user's requirement to validate on the HDMI output rather than the phone screen.

The physical TV Play Now check was repeated after this coverage change: the legal fixture
again launched `RetroActivityFuture` in the same `hw-pc-freeform` bounds, with GameBox visible
behind it and predominantly black game output. A normal Back and a long-press Back both left
RetroArch foreground. No force-stop or process kill was used. The return-to-GameBox and actual
playability gates therefore remain open on the Honor TV.

### Physical Honor TV UI polish smoke pass (1 October 2026)

Rebuilt and reinstalled the current debug APK on the unlocked PGT-N19 Honor. The app was
opened on HDMI display 3 (1920×1080); no emulator was used for visual acceptance. On the TV
output, Home rendered the wide Blueprint composition and focus bloom, Library rendered its
console/filter rail, game cards, storage and saves panels, and Settings → Display reported
`HDMI Screen · 1920×1080` with `TV safe area` set to **Off / 0%**. The screen navigation was
performed by selecting Library and Settings on the TV output. The visible Honor Desktop Mode
dock/taskbar and its window margins remain around the app, so the app-level immersive flags
do not establish a truly borderless whole-panel surface on this OEM desktop mode. The UI
screens rendered without a detected fatal exception in the inspected log window. This is a
visual/navigation smoke pass only; it does not close the emulator gameplay, controller, or
whole-screen deployment gates.

While RetroArch was still foreground during the preceding launch investigation, a normal
Escape key and F1 menu-toggle key did not return to GameBox; F1 only exposed RetroArch's
touch-control overlay. The installed RetroArch task remained alive. It was not force-stopped
or killed. GameBox was subsequently opened on the HDMI output through its exported launcher
activity. Honor still placed that activity in its `hw-pc-freeform` task container, so an
OEM/system-level fullscreen behavior change remains unresolved.

### Honor HDMI 7z import and uninstall flow (1 October 2026)

On the same unlocked Honor with its 1920×1080 HDMI output connected, the packaged 7z runtime
test passed headlessly (`SevenZipRuntimeTest`: 1/1). The visual import path was then exercised
on the TV surface using an explicitly synthetic, non-playable `.7z` fixture and the uninstalled
legal Celeste Classic catalog entry. Android DocumentsUI opened on the HDMI display, the
selected archive was extracted to `files/imports/homebrew/celeste/GameBox-TV-Fixture.rom`, and
the game appeared installed. The source archive was initially retained: the app had called
`ContentResolver.delete()` on a Storage Access Framework document URI. Source cleanup now uses
`DocumentsContract.deleteDocument()` for document-provider URIs, retaining the existing
DownloadManager/MediaStore deletion path for other content URIs. Repeating the same TV flow
removed the original archive from Downloads while leaving the managed extracted payload in
place. GameBox uninstall then reported one content file removed; the exact extracted path was
verified absent and the catalog entry returned to not-installed. No commercial ROM was used
or opened. This closes the physical Honor `.7z` picker → extraction → source cleanup → uninstall
cleanup flow for this synthetic fixture; it does not prove playable game compatibility.

The Honor documents picker and returning GameBox task were constrained to vendor
`hw-pc-freeform` windows during this flow; after picker return, GameBox was visibly centered
inside a smaller surface with the desktop shell/dock exposed. App-level fullscreen flags did
not override that OEM behavior, so the panel-wide borderless UI gate remains open.

### Honor HDMI fullscreen recovery retest (1 October 2026)

Rebuilt and installed the current APK on the unlocked Honor, then used the PC Hub's actual
**Show on TV** action. The dedicated `TvActivity` became the resumed HDMI activity in a
`fullscreen` task with bounds `Rect(0, 0 - 1920, 1080)`; Home, Library, Store, game details,
and the uninstall confirmation were navigated/captured directly from the physical TV output.
The generated 58-byte Celeste fixture was removed through GameBox's own uninstall action, and
the catalog returned to not-installed. No ROM/game binary was selected during this retest.

Opening Android DocumentsUI from that TV flow changed the shared Honor task back to
`hw-pc-freeform` (`Rect(192, 103 - 1344, 794)`). Its picker was visibly presented as a floating
window over the PC Hub. The newly added focus-gain fullscreen retry has therefore not yet
demonstrated recovery on this firmware: the task was still freeform in the available post-picker
dump, and this run could not obtain a clean GameBox-focused callback/return observation. Using
**Show on TV** again reliably started a new fullscreen `TvActivity`, which is the current
workaround. Keep the focus-retry change provisional until a clean picker-dismiss → focus regain
test proves the same task returns to full-screen; do not describe the complete picker flow as
borderless or fixed. The Honor desktop dock/taskbar also remains visible over the bottom edge.

The Android unit suite, Android-test Kotlin compilation, and debug APK assembly all passed
after the focus-retry change. This was a physical-device smoke test only; no emulator was used.

### Honor HDMI import progress and controller search (1 October 2026)

Added an indeterminate progress bar with accessible live-region text while a local game copy is
being imported and verified, both from Store discovery details and the authorized-copy details
flow. Existing import/reimport workflows retain their busy indicators. The import indicator is
implemented and compiled, but its visible in-progress state still needs a deliberately slow
local-copy run on TV; an instantaneous or completed import is not evidence that the bar was seen.

Added a TV search trigger and on-screen QWERTY keyboard for Store and Library. The keyboard
supports D-pad focus movement and selection, delete, space, clear, Search, and B/Back close,
without relying on the software IME or a physical keyboard. Build verification passed with
`:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, and `:app:assembleDebug`.

Installed the build on the physical Honor PGT-N19 and opened GameBox on its 1920×1080 HDMI
display. From Home, a controller-equivalent X event opened Store; a second X opened the search
keyboard. Display-targeted D-pad-down and select events moved focus to a letter, updated the
query to `QZ`, and updated the filtered-results UI. The HDMI screenshot showed the keyboard
and query update. This verifies the TV key-navigation path through Android key-event injection,
not through a connected gamepad: `dumpsys input` listed a USB optical mouse but no controller.
The Honor desktop dock/taskbar is still visible at the bottom; this pass does not close the
whole-panel borderless/fullscreen issue. The new Compose keyboard instrumentation test source
compiles but has not been run as an instrumentation test on hardware.

### Honor dock-to-TV search retest (1 October 2026)

The deployment target remains the Honor PGT-N19 in wired dock/desktop mode. A fresh display
dump identified the physical HDMI sink as external display 3 at
1920×1080/60 Hz. After installing the passing debug build, a display-targeted X key event
navigated Home → Store and opened the controller search keyboard. D-pad-down, D-pad-right,
and confirm entered `2`; the query and matching-title count changed on the TV output. B closed
the keyboard and two LB events returned to Home. This is physical Honor/HDMI rendering with
injected Android key events, not validation with an attached gamepad; the app status still
reported no controller connected. The Honor desktop dock/taskbar remains over the lower edge.

### Honor HDMI RetroArch normal-exit retest (1 October 2026)

On the physical Honor HDMI display, opened installed Galaxy Patrol from Store details via
Play Now and confirmed `com.retroarch/.browser.retroactivity.RetroActivityFuture` resumed
with the FCEUmm overlay/game surface visible. Honor initially placed the emulator task in
`hw-pc-freeform` bounds `Rect(192, 103 - 1344, 794)`, despite GameBox's fullscreen launch
request. For this diagnostic only, Android's task-resize command expanded that exact
RetroArch task to `Rect(0, 0 - 1920, 1080)`; this is not evidence that GameBox automatically
maximizes third-party emulator windows. Using RetroArch's visible L3+R3 Quick Menu toggle,
then **Close Content** and **Quit**, exited RetroArch normally with no force-stop. The top
resumed activity returned to GameBox `MainActivity` on the 1920×1080 fullscreen task, and
`pidof com.retroarch` returned no process. The installed Galaxy Patrol NES file and the
pre-existing `/sdcard/RetroArch/states/FCEUmm/galaxy-patrol.state` remained present. The
diagnostic resize task was removed when RetroArch quit; GameBox was restored to Home. This
closes the normal-exit/return software path under maximized geometry, but automatic emulator
fullscreen on Honor, physical gamepad input, and a newly-created save during this run remain
open hardware gates.

### Remove Android TV launcher identity; retain Honor HDMI output (1 October 2026)

Removed `LEANBACK_LAUNCHER` from the phone app’s launcher manifest and renamed the
non-exported `TvActivity` host to `ExternalDisplayActivity`. Updated the external-display
unit/instrumentation coverage and the DeX CI selector; the packaged debug manifest now
contains `MainActivity` and `ExternalDisplayActivity` and no Leanback launcher category.
External-screen hosting remains available for the Honor dock-to-HDMI deployment and does not
require an Android TV device. `MainActivity` now also requests immersive system-bar hiding
when it runs on an external display.

The focused external-display and Vimm JVM tests passed, Android-test sources compiled, and the
debug APK assembled. On the physical Honor, GameBox was observed on HDMI display 3 at
1920×1080. The running task was `MainActivity`, with fullscreen task bounds, rather than
`ExternalDisplayActivity`; the Honor desktop dock/taskbar still overlays the bottom edge even
after the MainActivity system-bar change. A window dump identified it as the visible `Dock`
window owned by `com.hihonor.desktop.systemui`, at `[0,1037][1920,1080]`, separate from
GameBox's app window. The direct ADB `--display 3` start returned OEM
error code 98 despite the existing MainActivity remaining visible, so it is not counted as a
successful direct-launch flow. The renamed host’s end-to-end launch and a truly unobstructed
TV surface remain unverified; no Android TV emulator was used.

### Honor external-display resizeability follow-up (1 October 2026)

The external-display fullscreen request had previously logged Honor's
`Trying to set root-task windowing-mode on a non-root-task` error while the
activity task was identified as non-resizable. The dedicated
`ExternalDisplayActivity` manifest entry now opts into resizeability so Honor's
desktop window manager can promote that phone-owned task to fullscreen; the
ordinary phone/Home activity remains unchanged. The debug APK and Android-test
sources compiled, and the APK was installed in place on the physical PGT-N19
without clearing app data. The device still showed the `MainActivity` root task
on HDMI after installation, so the new host's fullscreen behavior has not yet
been validated through GameBox's in-app **Show on TV** action. ADB's shell UID
cannot directly launch the intentionally non-exported host; that denial is
expected and is not counted as an app launch test.

The external-display resizeability change was then exercised through the actual
in-app route on Honor HDMI display 3 (1920×1080): MainActivity → PC Hub → **Show
on TV**. The non-exported `ExternalDisplayActivity` became top-resumed, and
ActivityTaskManager reported its task in fullscreen mode with bounds
`Rect(0, 0 - 1920, 1080)`. The first fullscreen promotion callback was accepted.
The redundant second request from `onResume` was removed; after rebuilding and
installing in place, the same in-app route again produced fullscreen task bounds
and a single accepted callback without the prior duplicate-request rejection.
The physical screenshot is
`honor-hdmi-fullscreen-retest.png` in the task visualization directory. It shows
GameBox spanning the HDMI panel while the Honor-owned 43-pixel desktop dock
remains overlaid at the bottom. Thus app fullscreen on the docked phone is now
verified; hiding Honor's system dock is still an OEM/device-level open gate.
The screenshot reports no connected controller, so no physical controller-input
claim is made.

### Honor HDMI Galaxy Patrol Play Now retest (1 October 2026)

From the full-size `ExternalDisplayActivity` on HDMI display 3, opened the
installed legal Galaxy Patrol fixture and selected **Play Now**. The real
`com.retroarch/.browser.retroactivity.RetroActivityFuture` became top-resumed on
the HDMI display, proving the current app's Play Now adapter still dispatches to
the installed RetroArch package. However, ActivityTaskManager placed RetroArch
in Honor's `hw-pc-freeform` mode at `Rect(192, 103 - 1344, 794)` on the
1920×1080 display rather than fullscreen. The capture
`honor-hdmi-retroarch-play-now.png` shows RetroArch's touch controls and part of
the running homebrew game in a centered window, with GameBox details visible
behind it. This is a physical launch/render attempt, not acceptance of the
living-room play flow: fullscreen emulator presentation and physical-controller
input remain open. The Honor status panel reported `Controller: Not connected`;
no real-controller input is claimed.

### Latest in-place install and HDMI UI check (1 October 2026)

Rebuilt the current workspace with `:app:assembleDebug` (successful; Gradle
reported all tasks up to date) and installed
`app/build/outputs/apk/debug/app-debug.apk` using `adb install -r` on the
connected PGT-N19; installation returned `Success` and preserved app data.
The package reports `versionName=0.1.0`, `versionCode=1`. Started
`com.gamebox.os/.MainActivity` on Honor HDMI display 3 and verified it became
top-resumed. The captured 1920×1080 screen
`honor-hdmi-latest-install-verify.png` in the task visualization directory
shows the updated GameBox Home, Galaxy Patrol and Crisis Core library cards,
and the Honor desktop dock overlapping the bottom of the app. This verifies
post-install launch/rendering on the intended HDMI output only; it does not
close the still-running RetroArch task, verify emulator exit/return, or provide
controller-input evidence. No Android TV runtime was used.

### Honor RetroArch task-mode diagnosis (1 October 2026)

After bringing GameBox back to the HDMI foreground, inspected ActivityTaskManager
without stopping either app. RetroArch remains a separate existing root task
(`t2750`) in `hw-pc-freeform` at `[192,103][1344,794]`; GameBox is root task
`t2751` in fullscreen at `[0,0][1920,1080]`. GameBox's current external-display
handoff already requests display 3 and fullscreen through its ActivityOptions
helper, while emulator intents carry `NEW_TASK | CLEAR_TOP`. The observed
RetroArch root task is still the same task from the earlier Play Now attempt,
so the next emulator-window investigation must distinguish reuse of that
freeform task from behavior of a fresh launch. This is a diagnostic inference,
not proof that task reuse is the sole cause. No device-global setting was
changed, no task was force-stopped, and no emulator fullscreen fix is claimed.

### Reimport source cleanup and archive regression coverage (1 October 2026)

Audited the missing-file repair path and found that it restored verified content
and preserved saves/history, but unlike new imports it did not remove the
selected source document. Updated `ImportedGameReimportCard` to delete selected
source files only after successful library registration and import-journal
confirmation. If registration recovery is pending or Android denies deletion,
the source is retained and the user-facing status says so. Extended the
DocumentsUI MediaStore integration test to select a synthetic 7z archive
containing a PSP ISO, assert the extracted ISO is placed at the managed path
with its expected checksum, and assert the source archive is gone after
successful reimport. The Android test compiles but was not run on the locked
Honor.

ZIP traversal filtering, 7z PSP extraction, console import policy, imported-game
registration, and reimport identity JVM suites passed 15/15. Android-test Kotlin
sources compiled and the rebuilt APK (`0.1.0`, versionCode 1) installed in place
on the PGT-N19. This does not yet prove the UI deletion permission behavior
through DocumentsUI on the projected Honor/TV; that physical flow remains
open.

### Honor RetroArch normal exit and launch-session recovery (1 October 2026)

On the physical HDMI output, relaunched the legal Galaxy Patrol homebrew from
GameBox. RetroArch appeared in Honor `hw-pc-freeform` rather than fullscreen.
Opened RetroArch's on-screen Invader/Menu control, used **Close Content**, then
scrolled its Main Menu and selected **Quit**. The official [Libretro Quick Menu
guide](https://docs.libretro.com/guides/quick-menu/) describes this normal UI
path. GameBox's Galaxy Patrol details screen regained focus on HDMI; the
RetroArch task disappeared and `pidof com.retroarch` returned no process. This
proves one physical normal-exit-and-return path, but not fullscreen gameplay or
controller input.

A read-only snapshot of GameBox's Room database immediately after that return
showed a confirmed `pending_launch_session` still present and Galaxy Patrol's
`lastPlayed` unchanged. This is direct evidence that Honor can keep the host
resumed while RetroArch owns window focus, so the existing pause/resume-only
callback did not finish the session. Added host-window-focus return handling
and a regression test for a focus-only return; the focused launch/deferred
session suites passed 15/15, Android-test Kotlin sources compiled, and the
debug APK rebuilt successfully. The new APK was installed in place. The phone
then locked before a fresh post-fix Play Now → RetroArch Quit pass could be
run on HDMI; therefore session-journal completion and automatic save backup
after this fix remain unverified on hardware. No physical controller was
connected during this pass.

### Compact action focus coverage (1 October 2026)

Expanded Android UI-test sources to verify that all six compact managed-save
actions expose click semantics and accept focus, missing-file reimport's
**Locate files** action is spoken/clickable/focusable at 560 dp, and compact
uninstall exposes a click action as well as focus. Existing compact tests cover
authorized import and Vimm browser tracking. The updated Android-test Kotlin
sources compile; the focused launch/deferred-return and archive-policy JVM
suites passed 21/21. These Compose semantics tests have not been run on the
locked Honor's HDMI display, so the physical focus/readability gate remains
open.

### Honor post-focus-fix launch-session verification (1 October 2026)

With the Honor unlocked and HDMI sink on display 3 at 1920×1080, launched
Galaxy Patrol from GameBox into the installed RetroArch. RetroArch was
top-focused in its existing Honor `hw-pc-freeform` task. Used its visible
**Close Content** command, then the Main Menu **Quit** command; did not
force-stop either app. GameBox `MainActivity` regained window focus, and
`pidof com.retroarch` returned no process. Captures record the normal RetroArch
menu and GameBox screen beneath it; the current UI/task geometry still shows
RetroArch windowed, with Honor's dock overlay at the bottom.

A read-only snapshot of the GameBox Room database after return showed an empty
`pending_launch_session` table and Galaxy Patrol updated to
`lastPlayed=2026-10-01T17:45:50.079Z`, `minutesPlayed=49`. Before the launch,
the table was empty and `lastPlayed` was `2026-10-01T17:27:19.586Z` with 46
minutes, so this fresh run verifies that the window-focus return callback
finalized the session and updated play history on the physical Honor. This
does not prove automatic RetroArch fullscreen, a newly generated emulator
save, or input from a physical controller; those remain separate acceptance
gates.

### Repository verification after the hardware return pass (1 October 2026)

Ran `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, and `assembleDebug`
from the current workspace. The unit-test XML reports contain 609 tests across
164 suites, with zero failures, zero errors, and one skipped test. The
instrumentation sources compiled and the debug APK assembled; instrumentation
was not executed by this command, and these build/test results are not
additional physical-display evidence. `git diff --check` passed for the
roadmap and audit updates.

### Focused UI instrumentation on supporting API-35 emulator (1 October 2026)

At the start of this follow-up the Honor reported `mDreamingLockscreen=true`;
HDMI remained connected at 1920×1080. No locked-device interaction was
attempted. On the separate `GameBox_API35(AVD) - 15` emulator, the Vimm/browser
download-tracking instrumentation class passed 3/3. A second focused batch ran
17 tests across compact save actions, reimport, uninstall, controller search,
and import progress: 11 passed and 6 failed. Failures were the tests' direct
focus requests for Locate files, uninstall, save actions, and the first search
keyboard key, plus the save-card cloud callback assertion. The search test
opens its controller keyboard by simulated touch before asserting initial
focus, so its input setup does not faithfully model a gamepad. These failures
are unresolved test/product questions, not TV acceptance evidence. Reproduce
the interactions with key-event/controller input in the test harness, then
rerun on an unlocked Honor docked to the TV; do not mark compact focus coverage
complete based on source compilation.

### Compact-focus test follow-up (1 October 2026)

Moved the search keyboard's initial focus request into the `Dialog` window's
composition and gave its first key an explicit focus target. Reworked the
search test to send Android D-pad/center events, type a character, and dismiss
the keyboard through **Finish search**. The isolated API-35 emulator test passed;
it verified first-key focus, D-pad movement across key rows, activation, query
update, and return to the search field. Compact import/reimport, uninstall,
save, and browser actions now assert click/spoken semantics and the presence of
their focus-request semantics. These component checks do not assert live focus
state or replace hardware-controller navigation.

The combined supporting-emulator batch passed 20/20 once. Repeated combined
runs intermittently lost the Compose test hierarchy in unrelated later cases;
the affected reimport class (4/4) and save-import confirmation test passed when
run alone. One final batch left `com.gamebox.os.test` frozen with no test
progress, so only that emulator test-runner package was stopped and the waiting
Gradle invocation interrupted. Treat combined-run reliability as open; this is
not Honor/TV failure evidence. The Honor remained locked during this follow-up.

After these changes, `testDebugUnitTest`,
`compileDebugAndroidTestKotlin`, and `assembleDebug` completed successfully;
unit-test XML reports 609 tests, zero failures/errors, and one skipped.

### Honor HDMI PPSSPP and controller state observed (1 October 2026)

On a later read-only check the Honor was unlocked again and HDMI display 3 was
active at 1920×1080/60 Hz. PPSSPP's `PpssppActivity` was foreground and its task
was fullscreen with bounds `[0,0][1920,1080]`; the captured TV frame is
`honor-ppsspp-current.png` in the task visualization directory. The Android
input inventory also showed an external GAMEPAD/JOYSTICK entry with identifier
`057e:2009` and key layout `Vendor_057e_Product_2009.kl`.

This does not prove GameBox launched PPSSPP or that this game's input/save flow
works through GameBox: a read-only Room snapshot showed no
`pending_launch_session`, and no controller input was sent. The visible live
game may contain unsaved progress, so no exit or navigation action was taken;
the user was asked whether to exit it normally. If permitted, resume on the
actual HDMI display and capture a fresh GameBox → PPSSPP launch before claiming
the second-adapter gate.

### Honor controller-status fix build and lock-screen observation (1 October 2026)

Adjusted controller-source classification so a device exposing GAMEPAD along
with KEYBOARD or MOUSE is still recognized as a controller; a JOYSTICK-only
desktop accessory with keyboard/mouse sources remains rejected. Added unit
cases for the mixed-source combinations. `testDebugUnitTest`,
`compileDebugAndroidTestKotlin`, and `assembleDebug` succeeded. Unit-test XML
reports total 609 tests, zero failures, zero errors, and one skipped test.
Installed the APK in place on the physical PGT-N19, preserving app data, and
started GameBox targeting the HDMI output.

The post-install HDMI screenshot showed “Phone is locked, please unlock,” so the
GameBox status card was obscured and its controller recognition could not be
visually verified. Android still enumerated the external device `057e:2009`
with GAMEPAD and JOYSTICK sources. No screen input, controller input, or PPSSPP
exit was sent. The source fix has software evidence but is not marked as
physically accepted; repeat the card check and actual D-pad/A/B navigation once
the phone is unlocked.

### API-35 DocumentsUI 7z reimport end-to-end test (1 October 2026)

Ran `SystemDocumentPickerReimportTest.productionReimportCompletesThroughAndroidDocumentsUi`
alone on `GameBox_API35(AVD) - 15` (API 35): 1/1 passed. The test creates a
synthetic PSP `.7z` in Downloads, selects it through Android DocumentsUI,
extracts a nested ISO into the managed PSP game path, verifies bytes and SHA-256,
confirms library registration preserves the existing record, verifies the
selected archive is removed only after success, and checks that staging,
backup, and pending-registration artifacts are absent. This validates the
integrated picker/archive lifecycle on the supporting emulator only; it does
not close the requested Honor/TV import acceptance gate.

### Focused compact-TV action and import feedback instrumentation (1 October 2026)

On the API-35 supporting emulator, the isolated
`BrowserDownloadTrackingActionTest` passed 3/3 (spoken/focusable browser
tracking, authorized import action, and disabled waiting state),
`PerGameSaveCardTest` passed 7/7, and `GameImportProgressIndicatorTest` passed
1/1 (compact progress is visible and exposes indeterminate progress semantics).
These runs add compact-layout component/integration evidence, not physical
controller operation on the Honor.

`ContentRemovalDialogTest` ran 3/4 successfully; its busy-state test initially
failed before the first preview assertion because the Compose hierarchy was
missing. Running that method alone passed 1/1, including single confirmation,
busy-state action disabling, and completion. This is consistent with the
previous combined-run Compose hierarchy instability; keep batch reliability
open rather than calling the class clean as a single run. The Honor remained
locked during this test session, so its controller-status card and physical
D-pad/A/B operation remain pending.

### API-35 content uninstall/reinstall safety and controller-search checks (1 October 2026)

Ran `GeneralContentRemovalTest` alone on the supporting API-35 emulator: 3/3
passed. It checks symlink target protection, fail-closed uninstall when save
backup cannot be created, and a production-controller/Room multi-file disc
uninstall followed by reimport. Synthetic fixtures verify owned content is
removed, another game's content and the save survive, save backup is created,
and favorite, description, play history, and minutes played remain intact.
Ran `ControllerSearchKeyboardTest` alone: 1/1 passed; Android D-pad/center key
events navigate the on-screen controller keyboard, enter a character, and
return focus to search. These tests use the emulator's injected input path and
synthetic content, not the physical Honor controller or the actual TV display.

### API-35 legal homebrew Play Now and normal RetroArch return smoke test (1 October 2026)

Installed the debug APK on `GameBox_API35(AVD) - 15` after its prior test-runner
session had left the app uninstalled. On the live GameBox UI, an A-button event
opened Galaxy Patrol details and an X-button event installed the bundled legal
homebrew; the UI then showed `installed` and `Play Now`. X/A key events did not
foreground RetroArch from the Play Now state, so I used the Play Now bounds
reported by Android's accessibility dump for one emulator-only touch. This
launched `com.retroarch/.browser.retroactivity.RetroActivityFuture`, with
`launchedFromPackage=com.gamebox.os`. The captured emulator frame showed the
Galaxy Patrol sprite rendered by RetroArch/FCEUmm. Therefore, the real app-to-
RetroArch adapter and content handoff work on this supporting emulator, but
this run did not prove controller activation of Play Now.

Used RetroArch's on-screen Quick Menu → **Close Content** → Main Menu → **Quit**;
did not force-stop the emulator. GameBox regained top focus, `pidof com.retroarch`
returned no process, the Room `pending_launch_session` count was 0, and the
Galaxy Patrol row was `INSTALLED` with `lastPlayed=2026-10-01T19:00:38.070Z` and
`minutesPlayed=2`. This proves one supporting-emulator launch/normal-return
session and journal finalization, not Honor/TV acceptance or real-controller
play. The Honor was locked during that emulator run; hardware results are
recorded separately below.

### Unlocked Honor HDMI controller recognition and launcher frame (1 October 2026)

On the actual PGT-N19 (Android 16 / MagicOS `10.0.0.163C431E5R2P2`) with the
docked 1920×1080 HDMI display active, the unlocked GameBox Home screen is
top-resumed on the external display. ActivityTaskManager reports GameBox in
fullscreen mode with task bounds `[0,0][1920,1080]`. The
controller status card now identifies `Nintendo Switch Pro Controller`,
confirming the source-classification fix is visible on the physical TV. The
capture also shows the Honor desktop taskbar overlaying the bottom of the
GameBox footer, so app full-screen flags do not currently remove all OEM chrome.
HONOR's [MagicOS 10 PC Mode guidance](https://www.honor.com/cn/support/content/zh-cn15900112/)
describes per-window fullscreen controls but does not document an app API or
setting to hide the global taskbar. I have not changed system-level display or
PC Mode settings.
An installed PSP game remained the Home hero; its emulator process was alive
but backgrounded, and it was not opened or stopped.

The controller input inventory maps the Nintendo controller to
`/dev/input/event13` with D-pad and gamepad buttons. Two read-only `getevent`
observation windows (about one minute total) on that device received no button
events after requesting a physical D-pad Right then A press; each listener was
stopped without injecting input. Do not count Android device enumeration or
the connected status card as controller input/navigation evidence. The unlocked
screenshot confirms status detection only; physical controls, Play Now, TV
footer visibility, and end-to-end gameplay remain open. The capture is retained at
`docs/evidence/honor-tv-controller-detected.png`.

### Current continuation check (1 October 2026)

Rechecked the connected PGT-N19: the external HDMI display is active at
1920×1080/60 Hz (display ID 3), GameBox `MainActivity` remains resumed in a
fullscreen task, and the Nintendo Switch Pro Controller is enumerated as
`/dev/input/event13` with D-pad and gamepad button capabilities. A count-based
passive listener remained live for about 40 seconds and emitted no events before
it was manually stopped. This is not evidence that physical input fails; no
controller press was observed during the window, so physical navigation remains
unverified.

As supporting software evidence only, ran
`ControllerSearchKeyboardTest` on `GameBox_API35(AVD) - 15` (API 35) with
`connectedDebugAndroidTest`; its single test passed (1/1). This confirms the
on-screen search keyboard responds to Android test-injected navigation on the
emulator, not that a physical controller works on the Honor/TV. The Honor
session was not sent synthetic input, and no emulator or game process was
stopped or relaunched during this check.

### Fresh debug APK installed on Honor; screen lock prevented UI acceptance (1 October 2026)

Rebuilt the current workspace with `:app:assembleDebug --rerun-tasks` (BUILD
SUCCESSFUL, all 37 tasks executed) and installed
`app/build/outputs/apk/debug/app-debug.apk` on the PGT-N19 using `adb install
-r` (Success). The artifact SHA-256 is
`4F91D3116509749C66E9DA80A9DA4DC8430AF230A6B80B53466A4AF551939E22`; package
metadata reports version `0.1.0` / versionCode `1` / targetSdk `36`. This is a
debug build, not a signed release.

Started only `MainActivity` on Android HDMI display 3. Activity Manager showed
the GameBox activity resumed, but the actual HDMI capture displayed HONOR's
“Phone is locked, please unlock” screen, and HONOR Suite/notification shade
retained top focus. Therefore the installation succeeded, but this did not
visually verify the app UI or constitute a TV acceptance run. Evidence is
`docs/evidence/honor-tv-after-install-locked.png`. No unlock attempt, synthetic
input, app-data wipe, emulator interaction, or PPSSPP operation was performed.
Resume Honor/TV UI testing after the user unlocks the phone.

While hardware UI was unavailable, reran `testDebugUnitTest --rerun-tasks`
against the rebuilt workspace: 609 tests, 0 failures, 0 errors, 1 skipped;
Gradle reported BUILD SUCCESSFUL (25 tasks executed). The run emitted existing
Kotlin warnings for deprecated fullscreen flags/SevenZFile APIs and nullable
test-fixture assertions; these did not fail compilation or tests. The
instrumentation results remain separate from this JVM suite and from physical
Honor acceptance.

On the next continuation check, the HDMI display was still active at
1920×1080/60 Hz and a fresh capture again showed HONOR's locked-phone screen;
GameBox's process existed but was not visually exposed above the lock screen.
This confirms the current test gate remains the device unlock, not a GameBox
crash. Latest capture from the current continuation:
`docs/evidence/honor-tv-lock-recheck.png`.

### Archive extraction failure cleanup regression (1 October 2026)

Hardened `RomArchiveExtractor.extractIfArchive` so it removes the archive
expansion directory if opening/reading fails, a byte/file limit is exceeded,
or no supported payload is found, then rethrows the original error for the
transaction layer to report. Ran `RomArchiveExtractorTest` with Gradle
`--rerun-tasks`: 4/4 passed, including new oversized ZIP and corrupt ZIP cases
that assert no earlier or partial files remain under staging. This is JVM
unit evidence; DocumentsUI flow and physical Honor import behavior remain open.

After that focused run, reran the full `testDebugUnitTest --rerun-tasks` suite
against the same source tree: 611 tests, 0 failures, 0 errors, 1 skipped;
BUILD SUCCESSFUL (25 tasks executed). The latest physical HDMI recheck still
shows the Honor lock screen (`docs/evidence/honor-tv-lock-check-latest.png`),
so these passing tests do not close the hardware gate. The debug APK installed
before this extractor change has not yet been rebuilt/reinstalled with it.

Rebuilt the workspace debug APK locally after the extractor change:
`:app:assembleDebug` succeeded and produced SHA-256
`4E53D93C4EFE44BB670603F2AE96248D4BF84540788B365EEAB1FA715E39054C`.
The current HDMI capture remains locked, and Android reports
`org.ppsspp.ppsspp` running (PID 11317). To protect the live emulator session,
this newer APK has not been installed over the Honor's app yet. Install and
resume hardware validation after the phone is unlocked and PPSSPP has been
exited normally.

Latest continuation recheck: the PGT-N19 remains connected to the 1920×1080
HDMI display, the HDMI frame still shows the phone-lock prompt, and
`org.ppsspp.ppsspp` remains alive. No input was sent and PPSSPP was not
interrupted. Capture: `docs/evidence/honor-tv-session-recheck.png`.

Following that check, a fresh read-only ADB inventory again found the Honor
locked behind HONOR Suite and `org.ppsspp.ppsspp` alive as PID 11317. The
external display remains 1920×1080 at 60 Hz. Latest HDMI-only screenshot:
`docs/evidence/honor-tv-continuation-latest.png`.

### Vimm discovery-platform/import-profile alignment (1 October 2026)

The Vimm preset already contained explicit vault-slug mappings for all 33
platform labels in `vimmLairSupportedPlatforms`, but several labels had no
dedicated importer profile and consequently used the generic extension
fallback. Added importer aliases/profiles for DS, WiiWare, Atari Jaguar/Lynx,
Jaguar CD, Philips CD-i, Sega 32X, Virtual Boy, and TurboGrafx-CD. A new
cross-layer regression asserts every Vimm preset platform resolves to a
console-specific import profile and checks representative system formats.

Format choices were checked against official Libretro documentation for
[Virtual Boy](https://docs.libretro.com/library/beetle_vb/),
[Jaguar/Jaguar CD](https://docs.libretro.com/library/virtual_jaguar/),
[TurboGrafx-CD](https://docs.libretro.com/library/geargrafx/),
[Sega 32X](https://docs.libretro.com/library/picodrive/), and
[CD-i](https://docs.libretro.com/library/same_cdi/). These checks establish
file-format/import recognition only; they do not claim that an emulator/core
is installed or can launch each platform.

`VimmLairDiscoverySourceTest` passed 26/26; the complete JVM suite passed
612 tests, 0 failures, 0 errors, 1 skipped (`testDebugUnitTest --rerun-tasks`).
The rebuilt local debug APK succeeded with SHA-256
`8F238334956066ABE88756A094A290AED3614F705EE359B72821267F4AFB4C9B`. It
has not been installed on the Honor: the phone remains locked and PPSSPP is
still running, so the prior live game session is being preserved.

### ZIP/7z containers accepted across console profiles (1 October 2026)

Extended each known console profile to accept ZIP and 7z as source containers,
while keeping console game-payload extensions separate. The extractor now uses
only payload extensions for archive entries, so nested ZIP/7z files cannot be
mistakenly registered as installed games. Added coverage that every Vimm
preset console accepts both container types, PS2 still accepts its `.iso`
payload, nested archives are rejected, and failed extraction leaves no staged
files. This addresses the per-console gap in earlier PSP-only 7z evidence.

The full JVM suite passed 613 tests, 0 failures, 0 errors, 1 skipped after this
change; Gradle ran 25 tasks with `--rerun-tasks`. The local debug APK packages
successfully with SHA-256
`26CCA9C02B731C048579CF05F5244C771E758D1FA4848B79EF9A9AE6585E9E0A`. It has
not been installed on the Honor because its lock screen is active and PPSSPP
PID 11317 remains alive; actual DocumentsUI/TV import validation is open.

Added a real synthetic `.7z` extraction case for a PS2 `.iso` payload in
addition to the PSP fixture, and rejected nested archives at the extraction
boundary. `RomArchiveExtractorTest` passed 6/6. Final full JVM rerun passed
614 tests, 0 failures, 0 errors, 1 skipped. The packaged debug APK hash above
remains the latest production-source build; the added test changes no APK code.

### Controller/search/import UI verification continuation (1 October 2026)

The connected Honor still reports the 1920×1080 HDMI presentation display and
PPSSPP PID 11317; HONOR Suite is top-resumed. I left that emulator session
untouched and ran software-only UI coverage on `GameBox_API35(AVD) - 15`.

The controller search test was updated to use Compose's v2 test rule and an
explicit AndroidJUnit4 runner. It passed by itself (1/1), including D-pad focus,
virtual-key selection, query update, and closing the controller keyboard.
`GameImportProgressIndicatorTest` passed by itself (1/1). The three
`BrowserDownloadTrackingActionTest` cases passed in a filtered run. A run mixing
those three classes reported missing Compose hierarchies for the later classes,
so cross-class instrumentation stability is not yet proven.

The filtered 103-test API-35 run was not green: it reported eight UI-test
failures (including missing Compose hierarchies and two wide-layout assertions)
and stopped advancing at test 100/103. The active Gradle test process and
instrumentation runner were checked; after repeated unchanged test 100 status,
that wedged run was interrupted. An isolated previously stalled save-card test
passed (1/1). Do not count the broad run as a pass; isolate and repair the
remaining suite regressions before treating API-35 UI coverage as complete.

The wide-TV Play Now test no longer marks an arbitrary catalog row installed
and assumes that is playable. It now requires the bundled Galaxy Patrol entry,
verifies the authorized content before checking Play Now, and explicitly skips
when the retained emulator catalog does not include that fixture. On the
current retained emulator DB, Galaxy Patrol is absent, so this test skipped;
the verified Play Now UI assertion still needs a clean fixture-backed run.

For compact uninstall confirmation, migrated `ContentRemovalDialogTest` to the
Compose v2 rule and reran the full class on API 35: 4/4 passed, including
confirmation/cancel and busy-state disabling. Migrated `PerGameSaveCardTest`
to the v2 rule as well; its class-level run remained unstable (one missing
Compose hierarchy and then no progress), so I interrupted it after checking
the active runner. The cloud-action method passed alone (1/1), as did the
compact action accessibility test earlier in that class run. Keep complete
managed-save class stability open; isolated passes do not substitute for it.

### API-35 broad-suite failure triage and targeted retest (2 October 2026)

The full Orchestrator run reached its terminal report: 104 tests, 1 skipped,
4 failed. The failures were: a lifecycle test requiring `NOT_INSTALLED` even
though startup reconciliation correctly returned `MISSING_FILES`; a wide-TV
Play Now control not scrolled into view; no fallback/error status presented in
the wide Blueprint Store layout; and a DocumentsUI reimport assertion made as
soon as Room registration changed state, before the import transaction and UI
message had settled. This broad run is a failure, not a pass.

Addressed and reran those cases individually on `emulator-5554` (API 35), using
explicit ADB serial selection so no connected hardware was targeted:

- Lifecycle baseline now accepts either `NOT_INSTALLED` or reconciled
  `MISSING_FILES`; the complete install → save/backup → uninstall → reinstall →
  restore method passed (1/1).
- Wide-TV Play Now test scrolls the production Details layout to the action;
  verified Galaxy Patrol Play Now case passed (1/1).
- Store now renders the same accessible offline/provider/error/success status
  message on the wide Blueprint surface as on compact layouts. The case checks
  each fallback/error status and the controller-accessible Refresh action; it
  passed (1/1).
- The DocumentsUI test waits for verified reimport completion, not only the
  earlier Room state update. Game-file selection now requests read, write, and
  persistable SAF grants; after verified registration GameBox takes/releases
  the grant around source deletion. The real 7z → PSP ISO reimport passed (1/1),
  including checksum/managed-path checks and confirmation that the selected
  source document was removed and no transaction staging files remained.

The full JVM suite was freshly rerun with `--rerun-tasks`: 614 tests, 0 failures,
0 errors, 1 skipped. `git diff --check` reported no whitespace errors (only the
repository's existing LF/CRLF notices).

One filtered `connectedDebugAndroidTest` invocation unexpectedly fanned out to
both the AVD and Honor because both were attached. Honor test processes crashed
while HONOR Suite remained foreground; the Galaxy Patrol lifecycle test was
skipped there. No controller input was sent. The subsequent read-only hardware
check confirmed PPSSPP PID 11317 remains alive, HONOR Suite is still top-resumed,
and the HDMI display remains 1920×1080 at 60 Hz. Do not send further tests to
the Honor until it is unlocked and PPSSPP has been exited normally. All later
instrumentation reruns were explicitly directed to `emulator-5554`.

### Complete API-35 Orchestrator rerun after screenshot assertion repair (2 October 2026)

The first emulator-only full rerun after the four prior repairs reached its
terminal report with 105 tests executed, 2 skipped, and 1 failure. The remaining
failure was `BlueprintScreenshotsTest.captureRealScreens`: it assumed the home
hero always exposed an in-view “Install game” action. The selected hero's install
state varies, and the test's purpose is to capture the production Details screen,
not to assert a particular catalog action. Replaced that state/layout-dependent
assertion with a wait for the stable “About this game” Details content.

The focused screenshot test passed (1/1) on `emulator-5554`. Then the complete
API-35 Orchestrator suite was rerun with `--rerun-tasks` and explicit
`ANDROID_SERIAL=emulator-5554`. It finished successfully: **103 test cases, 0
failures, 0 errors, 1 skipped**. The single skip was
`SafSaveAdapterDeviceTest.configuredSaveRootRoundTripsARealDocument`, which
requires a user-configured SAF document root. The suite includes the DocumentsUI
7z reimport flow, lifecycle/save round-trip, wide-TV Store state and Play Now
checks, controller-search and import-progress coverage, and the
screenshot/navigation sweep. This closes the local API-35 full-suite gate, but
not the fresh clean-CI gate or physical Honor Magic6 Pro + dock + TV/controller
acceptance gate. The Honor was not selected as a test target.

### Local non-device Android CI checks (2 October 2026)

Revalidated the repository's non-device Android workflow checks locally:
`python -m unittest scripts/test_android_version_code.py` passed 3/3; the
authorized Galaxy Patrol catalog JSON, iNES header, exact 49,168-byte length,
and pinned SHA-256 all matched; `testDebugUnitTest` passed 614 tests, 0 failures,
0 errors, 1 skipped; and `assembleDebug` plus `assembleDebugAndroidTest` both
completed successfully. APK hashes from this build:

- Debug APK: `7DA46B31D067470CBC5BF60F77FD813CDBB252F97F28394014967EB39DE15A00`.
- Instrumentation APK: `3C371AE3D0E771C7CF849A0B05574D0EAF082FA049453588C730C77E60A73D24`.

These checks ran with local Gradle 9.7.1. The hosted Android workflow pins
Gradle 9.5.0; no hosted run was triggered or inspected, so clean hosted-CI
acceptance remains open. The connected Honor still reports `mWakefulness=Asleep`;
no wake/input/app-launch action was taken while checking its state.

### PPSSPP missing-emulator Settings UI coverage (2 October 2026)

The API-35 emulator has RetroArch installed but no PPSSPP package. Added
`GameSettingsPanelReadinessTest`, exercising the production PSP Game Settings
composable and deriving the expected available/unavailable options from that
device's actual PackageManager results. It verifies that missing PPSSPP is
explained and every unavailable package choice is disabled; if an approved
variant is installed on another test device, the same test verifies the
installed/not-installed labels and enabled states for all three variants.
The initial test draft incorrectly expected two chips, while the UI exposes
three package variants in two rows; the corrected expectation uses live package
state. The focused instrumentation test passed on `emulator-5554` (1/1).

This is missing-adapter guidance evidence only; it does not validate PPSSPP
installation, game launch, gameplay, return, or save recovery. The previous full
API-35 aggregate at the time of this focused run had 103 cases/1 skip; a
subsequent complete rerun including this test is recorded below.

### Full API-35 suite including PPSSPP readiness UI test (2 October 2026)

Reran `:app:connectedDebugAndroidTest --rerun-tasks` with
`ANDROID_SERIAL=emulator-5554`. The Orchestrator completed successfully. The
JUnit XML reports 104 test cases across 47 suites, 0 failures, 0 errors, and 1
skip (`SafSaveAdapterDeviceTest.configuredSaveRootRoundTripsARealDocument`).
`GameSettingsPanelReadinessTest` is included and passed (1/1). The Gradle console
reported a different aggregate “finished” count than the XML; the JUnit XML is
used as the authoritative case count. This is local API-35 emulator evidence,
not a hosted-CI or physical Honor/TV result.

### Honor Magic6 Pro HDMI playback/return spot check (2 October 2026)

Tested the installed debug build on the actual Honor Magic6 Pro (PGT-N19) while
docked to its physical 1920×1080 HDMI display (Android display 243; not either
Android Studio screen-sharing display). From Home, the in-app **Show on TV**
action opened `ExternalDisplayActivity` on HDMI; Android reported the activity
focused with a 1920×1080 fullscreen task. The Android default Home chooser was
opened and then cancelled; Honor Home remains the default.

On that HDMI session, opened Store, installed the bundled legal Galaxy Patrol
sample, and confirmed its installed state and **Play Now** action. Play Now
started RetroArch's real `RetroActivityFuture` on display 243. Used RetroArch's
own Quick Menu → Close Content → Quit path (no force-stop); GameBox's external
host resumed on HDMI. Returning to GameBox Home showed Galaxy Patrol installed,
under **Your collection**, and **Recently played: 1 game**. No GameBox crash was
found in the inspected log window. This confirms the physical-device
Store/install → emulator-activity launch → normal quit → host-return/session
recording path.

This does **not** yet pass actual gameplay: the captured RetroArch surface was
mostly black with only a small partial sprite visible, and gameplay controls
were not exercised. RetroArch also appeared as a desktop-sized window over
GameBox rather than taking over the full HDMI surface; Android still reports
the underlying desktop/freeform task and system bar. The dedicated GameBox
external host itself did fill the TV viewport. No physical controller was
connected, so controller input and search remain unverified. The phone locked
once during the session; testing resumed only after a read-only check showed it
awake and the keyguard hidden. Do not substitute the emulator results for these
open physical gameplay, emulator-fullscreen, and controller gates.

### Home screen extraction and current-build HDMI smoke (2 October 2026)

As the first maintainability slice, moved Home setup, Home rendering, collection
rails, status panel, and quick-launch tiles out of the 340,907-byte
`GameBoxApp.kt` into `HomeScreen.kt`. Extracted Home selection rules into
`HomePresentation.kt` so install filtering, recent-game hero selection, the
legal sample fallback, and focus restoration can be tested without Compose.
`GameBoxApp.kt` is now 329,202 bytes. Four new unit tests cover those selection
rules. Gradle 9.7.1 passed `:app:testDebugUnitTest` (618 tests, 0 failures,
0 errors, 1 skip), `:app:assembleDebug`, and `:app:assembleDebugAndroidTest`.

Installed that build on the unlocked Honor, reopened `ExternalDisplayActivity`
on the physical 1920×1080 HDMI display, and verified the updated Home screen
still renders Galaxy Patrol as installed and recently played. The resulting
settled HDMI frame still contains Honor's desktop taskbar; the launcher/default
Home chooser remains unselected. The refactor is behavior-preserving and is
only the first boundary extraction; Store/catalog, Details/import, Downloads,
Settings, and app-hub separation remain future work. UI polish and the overall
project refactor are not complete.

### Primary navigation extraction and tab-cycle correction (2 October 2026)

Moved the shared destination model, top navigation/header, animated tab control,
and destination icon mapping from `GameBoxApp.kt` into `PrimaryNavigation.kt`.
This gives the primary launcher navigation a separately testable boundary.
The extraction exposed a shoulder-navigation bug: code indexed the filtered
visible tabs using `Destination.ordinal`, even though Downloads is intentionally
omitted from that row. Navigation now resolves the current index from the actual
visible tab list; regression tests cover the complete Home → Library → Store →
Media → PC → Settings cycle, wraparound, and recovery from Downloads.

Gradle 9.7.1 passed `:app:testDebugUnitTest`,
`:app:assembleDebugAndroidTest`, and `:app:assembleDebug`. The build was installed
on the physical Honor, and 1920×1080 screenshots captured from HDMI showed the
updated Home and Library/Store surfaces. The Honor desktop taskbar still overlays
the bottom edge. HDMI-targeted D-pad events did not reliably advance the live
tab after reinstall because the desktop shell retained top-resumed ownership;
therefore the full physical shoulder-navigation path remains unverified even
though the extracted navigation and cycle behavior are covered by passing unit
tests. No phone-screen test is claimed.

Follow-up Honor HDMI focus check: entered the PC tab through the app UI on
display 243 and invoked its **Show on TV** action. `dumpsys activity` confirms
`ExternalDisplayActivity` is the resumed and focused GameBox activity on HDMI.
The captured 1920×1080 HDMI screenshot nevertheless shows the Honor desktop
taskbar/navigation strip covering the bottom portion of GameBox. This directly
contradicts the earlier impression that the dedicated host alone removes all
system chrome. Window Manager reports the app window as fullscreen, filling
`Rect(0, 0 - 1920, 1080)`, with status/navigation bars requested hidden; the
remaining desktop taskbar is an Honor system surface, not reduced app bounds.
The visual goal is still unmet on this device, and any further attempt to hide
that shell-owned surface needs device-specific investigation.

The pure download-controller action policy has also been moved into
`DownloadControllerActions.kt`, separate from the downloads renderer, while
retaining the existing action-plan regression tests. This keeps controller
intent decisions independently testable as screen extraction continues.

Media/PC shortcut declarations and their controller-label policy have now been
extracted into `AppShortcutCatalog.kt`; the app-hub renderer consumes the same
catalog, and Settings can continue referencing it without duplicating package
metadata. After both extractions, Gradle 9.7.1 again passed
`:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest`, and
`:app:assembleDebug`. The full launcher remains a large orchestration/UI file,
so these are incremental boundaries, not completion of the requested refactor.

### Downloads screen extraction (2 October 2026)

Moved the complete Downloads screen and its metric card from `GameBoxApp.kt` to
`DownloadsScreen.kt`. The extracted screen retains queued/active/paused/failed/
completed actions, retry/cancel behavior, telemetry and capacity warnings,
controller-action registration, initial-focus routing, and both compact and
wide layouts. Shared byte/status label formatting now lives in
`UiTextFormatters.kt`, and imports used only by the moved screen were removed
from the app coordinator. The central file was reduced by about 21 KB by the
screen move (from about 321 KB to 300 KB), before the additional formatter
extraction. The full working-tree diff also contains earlier extractions.

Gradle 9.7.1 passed all 621 JVM tests (0 failures/errors, 1 skipped),
`:app:assembleDebugAndroidTest`, and `:app:assembleDebug`. The instrumentation
test APK compiles, but the Honor became locked during this follow-up; its HDMI
output displayed the device lock screen. No current-build UI behavior is claimed
as reverified on hardware until the phone is unlocked and the TV host can be
reopened.

### Media and PC hub extraction (2 October 2026)

Moved the Media/PC hub coordinator, Blueprint layouts, shortcut tiles, Moonlight
status presentation, and launch-time formatting from `GameBoxApp.kt` into
`AppHubScreen.kt`. The shortcut catalog remains in `AppShortcutCatalog.kt`; the
shared controller hint used by the hub is now package-visible. Removed the
hub-only imports from `GameBoxApp.kt`. This extraction moves roughly 540 lines
and reduces the coordinator from 5,777 to 5,231 lines.

Gradle 9.7.1 passed `:app:testDebugUnitTest`,
`:app:assembleDebugAndroidTest`, and `:app:assembleDebug`. The connected Honor
projection still displays “Phone is locked, please unlock,” so this build has
not yet had a post-extraction visual check on the TV. Do not bypass the lock;
resume HDMI verification once the user unlocks the phone.

### Settings screen extraction (2 October 2026)

Moved `SettingsScreen` and its settings-only navigation strip, status cards,
action rows, and section heading into `SettingsScreen.kt`. Kept the shared
settings action/status rows and the Blueprint library rail package-visible where
the main quick-actions and Home/Library UI still consume them. Removed imports
that became unused in `GameBoxApp.kt`. The coordinator is now 3,756 lines (down
from 5,231 before this extraction).

Gradle 9.7.1 passed `:app:testDebugUnitTest`,
`:app:assembleDebugAndroidTest`, and `:app:assembleDebug`; the debug APK was
installed to the Honor. A new HDMI capture still showed the phone-lock screen,
so there is no post-extraction hardware visual confirmation. Keep the TV
verification gate open until the user unlocks the phone and Settings is opened
on the actual HDMI output.

### Details screen extraction and storage cleanup (2 October 2026)

Moved the complete game Details experience and its controller/download
presentation helpers into `GameDetailsScreen.kt`, reducing `GameBoxApp.kt` from
3,756 to 3,054 lines. Updated `ProductionInstallBoundaryTest` to scan all
production UI source files, so the worker-backed transfer-action invariant
continues to hold after screens are split into feature files. Documented the
app-private on-device folder layout in the README, including imported games,
managed installs, saves, backups, manifests, and catalog state.

On the Honor, removed eight hash-verified duplicate Galaxy Patrol test fixtures
from `Download/GameBox`, retaining the canonical fixture and `brickster.gbc`.
The app-private tree contained the console-scoped imports and managed install
content, with no staging/temp import directories to remove; app state and
recovery manifests were preserved. The latest debug APK installed successfully
with app data retained. The phone is currently locked, so no TV UI check was
performed; resume HDMI testing after user unlock.

Gradle 9.7.1 passed `:app:testDebugUnitTest` (621 tests, 0 failures, 0 errors,
1 skipped), `:app:assembleDebugAndroidTest`, and `:app:assembleDebug`.

### Store and Library screen extraction (2 October 2026)

Continued reducing the shell coordinator by moving the Store console catalog,
Blueprint catalog layout, refresh status, and discovery browse/results into
`StoreCatalogScreen.kt`; the discovery title-detail/import/source flow and
result cards now live in `DiscoveryDetailsScreen.kt`. Moved collection and
Library screens into `LibraryScreens.kt`, keeping shared filter, focus, and
Blueprint components available to both features. Removed imports no longer
used by the affected screens. `GameBoxApp.kt` is now 1,044 lines, down from
3,054 before this pass.

Gradle 9.7.1 passed `:app:testDebugUnitTest` (621 tests, 0 failures, 0 errors,
1 skipped), `:app:assembleDebugAndroidTest`, and `:app:assembleDebug`. The
verified debug APK was reinstalled on the Honor with app data retained. The
phone still reports the lock screen; do not claim a post-refactor TV visual or
controller acceptance check until it is unlocked.

### Settings Interface section extraction (2 October 2026)

Moved the Interface controls (Media/PC shortcut visibility, UI sound effects,
and reduce-motion preference) into `SettingsInterfaceSection.kt`. The section
now receives only the current settings snapshot and repository; its coroutine
scope and presentation remain local to the component. The rest of Settings
retains its existing storage, controller, download, emulator, display, audio,
network, save/cloud, and diagnostics behavior. `SettingsScreen.kt` is now 1,397
lines (down from 1,506).

Gradle 9.7.1 passed `:app:testDebugUnitTest` (621 tests, 0 failures, 0 errors,
1 skipped), `:app:assembleDebugAndroidTest`, and `:app:assembleDebug`. Version
0.1.0 was installed to the Honor with app data retained. The device remains
locked, so TV UI/controller acceptance is still pending user unlock.

### Settings storage section extraction (2 October 2026)

Moved external game-library and emulator-save-root presentation into
`SettingsStorageSection.kt`. The screen receives storage status, migration
plan, layout mode, and explicit selection/forget/migrate callbacks; picker,
permission, and migration-dialog side effects remain owned by `SettingsScreen`.
`SettingsScreen.kt` is now about 1,306 lines (from 1,397 before this pass).

Gradle 9.7.1 passed `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest`,
and `:app:assembleDebug`. The verified build was reinstalled on the Honor with
app data preserved. The device remains locked, so hardware display acceptance
is still pending unlock.

### Display and TV safe-area section extraction (2 October 2026)

Moved the display/status cards and TV safe-area selector into
`SettingsDisplaySection.kt`. The component receives runtime display/power
status and a preference callback; directional-center/Enter activation,
bring-into-view behavior, and Android Display settings navigation are
preserved. `SettingsScreen.kt` is now 1,248 lines (from 1,306 before this pass).

Gradle 9.7.1 passed `:app:testDebugUnitTest` (621 tests, 0 failures, 0 errors,
1 skipped), `:app:assembleDebugAndroidTest`, and `:app:assembleDebug`. The
verified build was installed on the Honor with app data retained. The phone
remains locked, so actual-TV UI/controller confirmation is still pending.

### Network and Companion settings extraction (2 October 2026)

Moved network readiness/status and Windows Companion controls into
`SettingsNetworkSection.kt`. The component now owns its pairing-secret display
and restore/rotate lifecycle alongside network capability observation; the
parent provides persisted settings and the Android network-settings action.
`SettingsScreen.kt` is now 1,223 lines (from 1,248 before this pass).

Gradle 9.7.1 passed `:app:testDebugUnitTest` (621 tests, 0 failures, 0 errors,
1 skipped), `:app:assembleDebugAndroidTest`, and `:app:assembleDebug`. The
verified build was installed on the Honor with app data retained. The TV
interaction check remains pending while the phone is locked.

### Cloud-save settings extraction (2 October 2026)

Moved the full WebDAV/S3 configuration form, endpoint/region validation,
credential presence check, secure save/clear actions, and transient form state
into `SettingsCloudSaveSection.kt`. The section still delegates encrypted
credential persistence to `SettingsRepository`; provider and endpoint values
continue to follow the persisted settings snapshot. `SettingsScreen.kt` is now
1,104 lines (from 1,223 before this pass).

Gradle 9.7.1 passed `:app:testDebugUnitTest` (621 tests, 0 failures, 0 errors,
1 skipped), `:app:assembleDebugAndroidTest`, and `:app:assembleDebug`. The
verified build was installed to the Honor with app data preserved. Read-only
window state still reports the device lock screen; TV interaction verification
must wait for unlock.

### Catalog and discovery settings extraction (2 October 2026)

Moved authorized catalog transport/credential configuration, catalog
connection testing, discovery-source editing/probing, and TheGamesDB key
management into `SettingsCatalogSourcesSection.kt`. Provider form state and
credential-availability effects now follow the component lifecycle. A shared
message state is passed in because storage migration, notifications, and
diagnostics also report status in this Settings area. `SettingsScreen.kt` is
now 627 lines (from 1,104 before this pass).

Gradle 9.7.1 passed `:app:testDebugUnitTest` (621 tests, 0 failures, 0 errors,
1 skipped), `:app:assembleDebugAndroidTest`, and `:app:assembleDebug`. The
verified APK was installed to the Honor with app data preserved. The device is
still locked, so no TV visual/controller verification is claimed.

### Developer diagnostics extraction and Honor storage cleanup (2 October 2026)

Moved developer diagnostics, event collection, sanitized report generation,
bundle export, and related developer-only simulations into
`SettingsDeveloperDiagnosticsSection.kt`. `SettingsScreen.kt` now owns the
remaining Settings navigation/layout and shared status state. The screen is
522 lines and the diagnostics component is 161 lines.

Forced a clean Gradle run of `:app:testDebugUnitTest`,
`:app:assembleDebugAndroidTest`, and `:app:assembleDebug`: 621 unit tests,
0 failures, 0 errors, 1 skipped; all requested build tasks succeeded. Installed
the resulting debug APK to the Honor with `adb install -r`, preserving app data.

On the Honor, removed 39 empty `GameBox-export-test-*` directories from
`/sdcard/Download`; no files were inside them. Preserved all game archives and
ROMs, existing GameBox content, the user photo, and the older APK. Verified the
existing organized root at `/sdcard/Download/GameBoxOS`, containing per-system
`Games` folders plus `Imports`, `Artwork`, `Metadata`, `SaveBackups`, `Exports`,
`Installers`, and `Logs`. No game/save data was moved or deleted. The phone
remains locked (`mDreamingLockscreen=true`), so TV-screen and controller testing
must wait for unlock.

### System overlay component extraction (2 October 2026)

Moved the controller quick-actions dialog, system status dialog, profile
switcher, and controller footer into `SystemOverlays.kt`. These living-room
overlays now have a dedicated presentation boundary separate from top-level
navigation and destination composition. `GameBoxApp.kt` is 936 lines (down
from 1,076); `SystemOverlays.kt` is 162 lines.

`:app:compileDebugKotlin` and `:app:testDebugUnitTest` succeeded; the suite
remains at 621 tests, 0 failures, 0 errors, 1 skipped. Both debug app and
instrumentation APKs assembled, and `git diff --check` passed. Installed the
refreshed APK on the Honor with app data retained. The device remains locked,
so this presentation-only extraction has not yet been visually verified on
the TV.

### Shared game-filter module extraction (2 October 2026)

Moved the shared library/store `GameFilterBar`, game text/platform/metadata
filtering, and configured-source failure messaging into `GameFilters.kt`.
Store and Library continue to call the same implementations; existing unit
coverage in `GameFilteringTest`, `StoreMetadataFilterTest`, and
`ConfiguredSourceResultTest` exercises the extracted functions. The app
coordinator is now 813 lines (from 936 before this pass); the new module is 136
lines.

Kotlin compilation and all 621 unit tests succeeded (0 failures, 0 errors, 1
skipped). Both debug APKs assembled, and the refreshed app APK was installed on
the Honor with app data preserved. The device is still locked; TV interaction
testing remains pending.

### Store console catalog extraction (2 October 2026)

Moved Store console identities, display labels, metadata-provider platform names,
normalized aliases, icons, and matching logic into `StoreConsole.kt`. Added
regression tests covering the complete eight-entry Store map, all provider
canonical names, normalized PSP aliases, cross-console rejection, and local-only
Homebrew behavior. `StoreCatalogScreen.kt` now has 1,137 lines (from 1,160
before this extraction).

The full unit suite passes with 624 tests, 0 failures, 0 errors, and 1 skipped.
The debug app and instrumentation APKs assembled, and `git diff --check` passed.
Installed the resulting app APK on the Honor with app data preserved. The
device still reports `mDreamingLockscreen=true`; no UI was launched and no TV
visual result is claimed.

### Game detail import component and duplicate-information cleanup (2 October 2026)

Moved the authorized-copy file picker, filename/platform resolution, verified
import, library registration/rollback, progress feedback, and source-file
removal into `AuthorizedCopyImportCard.kt`. Import state now has a `finally`
cleanup path, and coroutine cancellation is propagated rather than displayed as
a regular failure. Removed the second redundant “About this game” block from
the Details page; the existing presentation section already shows description
and metadata. The Details coordinator is 633 lines (from 756 before this pass),
with the new import component at 154 lines. Tightened the production screenshot
test to require exactly one “About this game” heading.

Gradle passed all 624 unit tests (0 failures, 0 errors, 1 skipped), and both
debug app and instrumentation APKs assembled. The updated APK is installed on
the Honor with app data retained. The phone still reports
`mDreamingLockscreen=true`; instrumentation was built but not run on hardware,
and no TV visual test is claimed.

### Store discovery-action state holder (2 October 2026)

Moved configured-source handling (Vimm search/detail hydration, JSON feed sync,
web-source launch) and multi-console metadata sync out of `CatalogScreen` into
`CatalogDiscoveryActions.kt`. This coordinator now owns observable search,
selection, progress, message, and busy state; the composable delegates actions
and remains responsible for screen navigation, filters, and focus. Async paths
now restore the busy state in `finally`, surface unexpected failures, and
propagate coroutine cancellation instead of converting it to a search error.
`StoreCatalogScreen.kt` is 987 lines (down from 1,137); the new coordinator is
213 lines.

Gradle passed all 624 unit tests (0 failures, 0 errors, 1 skipped), and both
debug app and instrumentation APKs assembled. `git diff --check` passed. The
new APK was installed to the Honor with app data retained. It remains locked,
so no application launch or TV interaction was attempted.

### Emulator capability policy extraction (2 October 2026)

Moved emulator capability data, console-to-emulator/core mapping, RetroArch
core-path policy, and imported/managed-content launch descriptor construction
into `EmulatorCapabilityRegistry.kt`. `GameLaunchController.kt` now focuses on
Android package/content dispatch and the launch/return session lifecycle; it is
586 lines (from 797), and the registry is 213 lines. Existing registry and
launch tests continue to cover capability selection, imported content,
emulator defaults, and handoff behavior.

Gradle passed all 624 unit tests (0 failures, 0 errors, 1 skipped); the debug
app and instrumentation APKs assembled, and `git diff --check` passed. Installed
the updated APK on the Honor with app data preserved. The phone is still locked,
so no launch or TV test was attempted.

### Launch session-controller separation (2 October 2026)

Moved launch UI state, play-session tracking, the `GameLaunchController`
contract, and `DefaultGameLaunchController` into
`DefaultGameLaunchController.kt`. `GameLaunchController.kt` now contains the
Android `PackageGateway` contract and package/content dispatch adapter only
(328 lines); session orchestration lives separately (267 lines). This keeps
the emulator adapter isolated from game-state/session recovery while preserving
the existing injectable gateway boundary.

Gradle passed all 624 unit tests (0 failures, 0 errors, 1 skipped), and both
debug app and instrumentation APKs assembled. The updated APK is installed on
the Honor with app data preserved. The phone remains locked; hardware launch
and TV-return validation are still pending.

### Game-status component boundary (2 October 2026)

Moved the reusable install-availability badge and accessible download progress
indicator from `GameBoxApp.kt` into `GameStatusIndicators.kt`. Their install
state labels, icon/color mapping, and screen-reader progress descriptions are
unchanged; the app entry/composition file is now 741 lines (from 789 before this
split).

Gradle passed all 624 unit tests (0 failures, 0 errors, 1 skipped), and the
debug app and instrumentation APKs assembled. The Honor reports an active
1920×1080 HDMI display but is currently locked, so the build was not installed
or visually tested on the TV during this pass.

### App-hub shortcut semantics (2 October 2026)

Blueprint media/PC shortcut tiles now expose a single button description that
includes both the app name and readiness state (for example, “YouTube, Setup
required”), in addition to their visible text and click action. The existing
large-text tile layout test now asserts the merged accessibility description
and action while retaining its bounds checks. The focused Compose test passed
both font-scale cases (2/2) on the API-35 emulator; the Honor remains locked,
so this is not TV visual acceptance.

### Store discovery-card separation (2 October 2026)

Moved the configured-source result card, discovery-game card, and discovery
horizontal rail from `DiscoveryDetailsScreen.kt` into
`DiscoveryGameComponents.kt`. This leaves the detail screen focused on selected
game information, authorized-copy import, and browser handoff; the shared card
file owns the discovery-only result presentation and focus/hover feedback. The
detail screen is now 450 lines (from 575). Removed imports made obsolete by the
move.

Gradle passed all 624 unit tests (0 failures, 0 errors, 1 skipped), and both
debug app and instrumentation APKs assembled. The Honor is still locked with
HDMI connected, so no TV visual test was attempted.

### Runtime compatibility and storage-capacity pass (2 October 2026)

Replaced `InputStream.readNBytes` calls with bounded, API-compatible reads and
added truncated/oversized stream coverage. External-display ID lookup now uses
the window decor display where available and gates the newer Activity display
API by platform version. Removed reflection into Android's hidden launch-window
mode API while retaining the Honor-specific best-effort launch hint. Added the
API-29 guard around MediaStore Downloads publication and localized lint
suppression to the AndroidX controller-event dispatch overrides. Added a
GameBox adaptive launcher icon.

The remote download worker now asks Android StorageManager for allocatable
space (including reclaimable cache) before rejecting a transfer, falling back
to filesystem usable space if the storage query fails. This is isolated in
`StorageAvailability.kt`; three tests cover success, fallback, and invalid
negative values. The separate home-screen “used” percentage remains based on
actual filesystem total/usable space because it represents a different metric.

Verification: the full Android unit suite passed 629 tests (0 failures,
0 errors, 1 skipped); debug assembly and lint passed with 0 lint errors,
50 warnings, and 1 hint. The focused external-display activity instrumentation
suite previously passed 19/19 on an API-35 emulator. These emulator results do
not establish Honor/TV visual acceptance: the Honor is currently locked, so
the latest changes were not installed or visually tested there.

### Compose modifier API cleanup (2 October 2026)

Standardized the optional modifier parameter name/order on the Settings storage,
display, network, and cloud-save sections, and made the discovery source card's
modifier the first optional argument. Updated the Settings screen call sites.
This is behavior-preserving and makes these components consistent with Compose
API conventions.

Verification: full Android unit suite passed 629 tests (0 failures, 0 errors,
1 skipped); `:app:lintDebug` passed with 0 errors, 45 warnings, and 1 hint. All
five `ModifierParameter` warnings are cleared. No Honor/TV visual test was run
while the connected phone remained locked.

### AndroidX KTX modernization (2 October 2026)

Replaced repeated `Uri.parse` calls with AndroidX `String.toUri()` at browser,
document-tree, save-root, and package-settings boundaries. Converted encrypted
secret and temporary launch-export preference mutations to the KTX
`SharedPreferences.edit` block form; it retains the previous asynchronous
`apply()` behavior and keeps each multi-operation update atomic within one edit.

Verification: full Android unit suite passed 629 tests (0 failures, 0 errors,
1 skipped); debug lint passed with 0 errors, 29 warnings, and 1 hint. All KTX
lint warnings are cleared. No Honor/TV visual test was run while the connected
phone remained locked.

### Settings scroll-state observation (2 October 2026)

Changed Settings section selection from reading the frequently changing scroll
offset as a `LaunchedEffect` key to observing both scroll offset and measured
section anchors through `snapshotFlow`. This avoids restarting the effect on
each scroll update while preserving automatic section selection.

Verification: full Android unit suite passed 629 tests (0 failures, 0 errors,
1 skipped); lint passed with 0 errors, 28 warnings, and 1 hint. The
`FrequentlyChangingValue` warning is cleared. Honor/TV interaction was not
tested because the connected phone remains locked.

### Store discovery progress state (2 October 2026)

Changed the discovery action model's float progress from generic Compose state
to `mutableFloatStateOf`, removing boxed float state while preserving all
existing progress updates and observers.

Verification: full Android unit suite passed 629 tests (0 failures, 0 errors,
1 skipped); lint passed with 0 errors, 28 warnings, and no hints.

### App Hub presentation extraction (2 October 2026)

Moved the animated shortcut tile/card, PC streaming status panel, Moonlight hero,
and launch-time formatting from `AppHubScreen.kt` into the dedicated
`AppHubComponents.kt`. The App Hub screen dropped from 582 to 369 lines; launch,
visibility, and controller behavior remain owned by the screen.

Verification: full Android unit suite passed 629 tests (0 failures, 0 errors,
1 skipped); debug instrumentation APK assembled; lint passed with 0 errors,
28 warnings, and no hints. The 1× and 2× font-scale tile accessibility/layout
instrumentation tests both passed on the API-35 emulator. This is not TV visual
acceptance; Honor/TV testing remains pending while the phone is locked.

### Store compact-path simplification (2 October 2026)

Removed the unreachable desktop header branch from `CatalogScreen`: desktop
mode returns through `BlueprintCatalogScreen` before the remaining compact
layout. Removed the redundant compact checks/arguments in that path while
keeping its direct console chips, authorized-game list, and discovery content.

Verification: full Android unit suite passed 629 tests (0 failures, 0 errors,
1 skipped); debug instrumentation APK assembled; lint passed with 0 errors,
28 warnings, and no hints. The compact Store console-filter instrumentation
test passed on API-35 emulator. This is not TV visual acceptance.

### Game Details hero extraction (2 October 2026)

Moved the artwork-backed details hero and wide-layout installation summary into
`GameDetailsHero.kt`. The component receives game/presentation state and
launch/install callbacks; Game Details retains ownership of controllers and
their behavior. The primary action keeps its explicit TV focus target, now
constructed within the hero from the screen-owned `FocusRequester`. The screen
file is now 507 lines (from 621 before extraction).

Verification: full Android unit suite passed 629 tests (0 failures, 0 errors,
1 skipped); instrumentation APK assembled; lint passed with 0 errors,
28 warnings, and no hints. Both deterministic hero instrumentation tests
passed on API-35 emulator (Play invocation and active-install state). An
existing whole-screen update-state test could not run to its assertions because
the emulator catalog had no seeded record; this is a fixture/setup gap, not a
passing test. No physical Honor/TV visual validation was possible while the
phone remained locked.

### Shared game-library presentation extraction (2 October 2026)

Moved `BlueprintGameTile`, `GameRow`, the animated/accessibility-aware `GameCard`,
and `GameScreenshotGallery` from `GameBoxApp.kt` into
`GameLibraryComponents.kt`. Home/app-root code now keeps navigation and shared
surface composition, while reusable artwork, focus, controller activation, and
gallery rendering live together. The app file is 545 lines (from 742).

Verification: full Android unit suite passed 629 tests (0 failures, 0 errors,
1 skipped); instrumentation APK assembled; lint passed with 0 errors,
28 warnings, and no hints. The four existing accessibility/controller-focus
instrumentation tests passed on API-35 emulator. The Honor is locked and no HDMI
display is currently attached, so this is not physical TV validation.

### Honor Magic6 Pro TV-mode return (2 October 2026)

The Honor Magic6 Pro was online and unlocked with the wired 1920×1080 HDMI
display active as Android display 249. Installed the current debug build,
opened GameBox from the TV desktop app drawer, and verified the app rendered on
the HDMI screen. The in-product **Show on TV** action successfully launched
`ExternalDisplayActivity` as the top-resumed activity on display 249. A real
`KEYCODE_BUTTON_R1` event moved Home → Library, and the Library search overlay
opened on HDMI; D-pad Select focused and entered the `Q` key, updating the
visible query. The actual-HDMI capture is `docs/evidence/honor-tv-current.png`.

The image also shows Honor Desktop Mode's persistent bottom taskbar covering
the lowest strip of the app even though GameBox requests fullscreen. This is an
OS-shell overlay and remains an unresolved end-deployment presentation issue.
The Home screen currently shows **Finish console mode**; selecting GameBox as
the device's default Home app is a system-level preference and was not changed
during this test. The existing `/sdcard/Download/GameBoxOS` tree was retained;
empty neighboring `GameBox` and `GameBoxOS-Legal` directories were also left
untouched. Only the temporary screenshot created on-device for capture was
removed.

Verification: `:app:assembleDebug`, `:app:testDebugUnitTest`, and
`:app:assembleDebugAndroidTest` succeeded; the two
`ImportedSourceCleanupTest` cases passed on the physical Honor; `:app:lintDebug`
passed with 0 errors, 28 warnings, and no hints. This validates the
external-display launch, basic shoulder navigation, and controller-only
search-key entry on this TV setup. It does not yet validate real Bluetooth
controller pairing, Home-role/boot flow, emulator gameplay, or taskbar removal.

### Vimm discovery transport boundary (2 October 2026)

Moved the Vimm HTTP contract, bounded TTL/LRU page cache, cached wrapper, and
HTTPS transport into `source/VimmLairTransport.kt`. The discovery source now
focuses on provider operations, parsing, platform mapping, and result merging;
network connection configuration and cache lifecycle are independently
located. Visibility and public entry points remain unchanged.

Verification: `:app:testDebugUnitTest` passed all 629 tests (0 failures,
0 errors, 1 skipped); `:app:assembleDebug` succeeded; `:app:lintDebug` passed
with 0 errors, 28 warnings, and no hints. Existing Vimm tests cover cache
behavior, URL validation, parsing, platform matching, and cancellation. This
increment changes organization only and does not claim a new Vimm live-network
or TV-flow validation.

### Vimm platform-search policy boundary (2 October 2026)

Moved supported-console aliases, Vimm slug mapping, platform selection,
multi-console aggregation, title ranking, and browse/detail URL construction
into `source/VimmLairSearch.kt`. `VimmLairDiscoverySource.kt` is now a 108-line
request coordinator for validation, transport calls, and parsing. The provider
is organized around four explicit responsibilities: source orchestration,
search policy, HTML parsing, and HTTPS/cache transport.

Verification: all 629 unit tests passed (0 failures, 0 errors, 1 skipped),
`:app:assembleDebug` succeeded, and `:app:lintDebug` passed with 0 errors, 28
warnings, and no hints. Existing Vimm tests exercise platform mappings,
multi-platform result behavior, ranking, and URL construction. No live network
request was made.

### Vimm HTML parser boundary (2 October 2026)

Moved vault-listing and detail-page parsing, title cleanup, metadata-row
extraction, HTML entity decoding, and artwork-host validation into
`source/VimmLairHtmlParser.kt`. The coordinator is now 323 lines instead of
roughly 590, with transport/cache and provider HTML concerns isolated in their
own source files. Existing internal parser entry points were retained so the
same fixtures and behavior continue to be exercised.

Verification: all 629 JVM tests passed (0 failures, 0 errors, 1 skipped), and
`:app:assembleDebug` succeeded. The existing suite includes Vimm listing/detail
fixtures, malformed input, entity handling, metadata extraction, URL checks,
deduplication, and rank behavior. No live Vimm request was made.

### Shared settings UI primitives (2 October 2026)

Moved the Settings section rail/icon mapping, status card, animated/focus-aware
action row, and accessible section heading into `ui/SettingsChrome.kt`. These
components are shared by settings sections and system overlays, so their visual
and controller-focus behavior now has one implementation. `SettingsScreen.kt`
dropped from 515 to 429 lines; section state and storage/picker workflows stay
owned by the screen.

Verification: all 629 JVM tests passed (0 failures, 0 errors, 1 skipped),
`:app:assembleDebug` succeeded, and `:app:lintDebug` passed with 0 errors, 28
warnings, and no hints. No behavior or theme values were intentionally changed;
this is a component-boundary refactor.

### Settings credential-store boundary (2 October 2026)

Moved TheGamesDB API-key, catalog-provider, cloud-save, and companion-pairing
secret handling out of `SettingsRepository` into
`settings/SettingsCredentialStore.kt`. The new class owns secure key names,
provider-specific credential mapping, dispatching secret I/O to `Dispatchers.IO`,
and pairing-secret rotation. `SettingsRepository` retains its existing public
methods as delegates and continues to own DataStore-backed configuration; no
encrypted preference names or stored formats were changed.

Verification: all 629 JVM tests passed (0 failures, 0 errors, 1 skipped),
`:app:assembleDebug` succeeded, and `:app:lintDebug` passed with 0 errors, 28
warnings, and no hints. The authenticated companion-service lifecycle test
passed on the physical Honor (1/1), exercising pairing-secret use through the
new boundary and restoring prior service settings afterward.

### Configured discovery sources UI boundary (2 October 2026)

Extracted configured-source actions, contextual guidance, and the results
carousel from the compact Store layout into
`ui/ConfiguredDiscoverySourcesSection.kt`. Source filtering, query/console state,
navigation callbacks, semantics labels, and card presentation are unchanged;
the Store screen continues to own catalog state and responsive layout.

Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`, and
`:app:lintDebug` all succeeded. All 629 unit tests passed (0 failures, 0 errors,
1 skipped); lint reported 0 errors, 28 warnings, and no hints. No new hardware
interaction was performed for this component-boundary change.

### TheGamesDB settings component boundary (2 October 2026)

Extracted TheGamesDB metadata-key configuration from
`SettingsCatalogSourcesSection.kt` into `SettingsTheGamesDbSection.kt`. The
component now owns its temporary key-entry/configured state and lifecycle check,
while preserving the existing secure repository operations and shared status
message behavior. This reduces coupling between provider/source settings and
metadata credentials without changing the screen flow.

Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`, and
`:app:lintDebug` all succeeded. All 629 unit tests passed (0 failures, 0 errors,
1 skipped); lint reported 0 errors, 28 warnings, and no hints. No new hardware
interaction was performed for this organization-only change.

For the Store UI boundary, focused Compose tests cover Vimm/JSON/web source
activation, result-card activation, focus acquisition, and disabled actions while
sync is in progress. The tests exposed that the configured-source buttons did
not acquire focus reliably in the isolated controller-focused layout; explicit
focusability was added and disabled actions are excluded from focus traversal.

Verification: all 3 focused Compose tests passed on the API-35 emulator; the
unit suite, debug APK, instrumentation APK, and lint also passed (lint: 0 errors,
28 warnings). The Honor's instrumentation process crashed before returning any
test result, including when running an existing `BrowserDownloadTrackingActionTest`,
so hardware Compose assertions remain unverified on this attempt. The component
tests passed on the emulator only; no TV visual validation is claimed.

### Game discovery settings workflow boundary (2 October 2026)

Moved the configured game-source workflow out of
`SettingsCatalogSourcesSection.kt` into
`SettingsGameDiscoverySourcesSection.kt`. The new component owns source-form
state, preset selection, configured source management, Vimm probing, validation,
and repository updates. The parent remains responsible for section order and
catalog provider configuration; the metadata key workflow remains a separate
component. Existing copy, preset values, validation, and repository calls were
preserved.

Verification: all 629 unit tests passed (0 failures, 0 errors, 1 skipped),
`:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:lintDebug` all
succeeded. Lint reported 0 errors, 28 warnings, and no hints. This extraction did
not receive a new hardware UI test; the prior Honor instrumentation crash and
emulator-only focused Store test result remain as documented above.

### Authorized catalog provider settings boundary (2 October 2026)

Extracted HTTPS/WebDAV/S3 catalog configuration, credential-state handling,
validation, secure save/clear behavior, and connection testing into
`SettingsAuthorizedCatalogProviderSection.kt`. The former parent is now a
35-line composition of the catalog, discovery-source, and metadata settings
sections. Provider configuration state and its async lifecycle are local to
the provider component; repository APIs, persisted values, feedback messages,
and interaction order were preserved.

Verification: all 629 unit tests passed (0 failures, 0 errors, 1 skipped),
`:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:lintDebug` all
succeeded. Lint reported 0 errors, 28 warnings, and no hints. No new physical
device UI run was made for this settings-only extraction.

### Honor external-display fullscreen and taskbar clearance (2 October 2026)

Physical-display inspection showed GameBox's normal launcher task on HDMI display
249 was being placed in a 1152×691 Honor freeform window, despite the fullscreen
window flags. The dedicated external-display host already retried Android's
fullscreen request after focus; `MainActivity` now uses the same delayed retry
on external-display focus instead of relying on a single resume-time request.
External-display content also receives 44 dp of bottom clearance so its
controller footer remains visible above Honor's persistent desktop taskbar;
phone layout is unchanged.

Verification on the Honor Magic6 Pro with the wired 1920×1080 display: after
installing the updated debug build and launching GameBox on display 249, the
activity task changed from freeform bounds 1152×691 to fullscreen bounds
0,0–1920,1080. The captured image
`docs/evidence/honor-tv-fullscreen-retry.png` records that full-bleed state; the
follow-up `docs/evidence/honor-tv-safe-footer.png` confirms the controller
footer now sits above the still-visible Honor taskbar. The taskbar itself is
owned by the Honor shell and remains visible. The on-screen “Finish console
mode” prompt also remains because GameBox has not been selected as the default
Home app.

Verification: 630 unit tests passed (0 failures, 0 errors, 1 skipped), both
debug APKs assembled, and lint passed with 0 errors and 28 warnings. The Honor
was used for installation and HDMI visual verification; no Bluetooth controller
was connected during this check.

### Game Details content panel and TV flow order (2 October 2026)

Extracted the installation/launch actions, managed-save summary, and launch-state
feedback into `GameDetailsContentPanel.kt`, leaving `GameDetailsScreen.kt` to
own screen state and the hero/about/settings sections. The former is now 404
lines; the extracted panel is 200 lines. Then reordered the detail flow so the
game hero and primary Play/Install action appear before import/save-management
cards, which had previously pushed the game's identity below the first TV
viewport. Button conditions, callbacks, controller action plan, and save/import
operations are unchanged.

Verification: 630 unit tests passed (0 failures, 0 errors, 1 skipped),
`:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:lintDebug` all
succeeded (0 lint errors, 28 warnings). Installed on the physical Honor and
opened on the 1920×1080 HDMI display. The after image
`docs/evidence/honor-tv-game-details-hero-first.png` shows the hero, status, and
Install action at the top, with About and save-copy controls below and the
controller footer clear of the desktop taskbar. The prior image
`docs/evidence/honor-tv-game-details-panel.png` shows the former ordering.

### Game content-removal boundary (2 October 2026)

Moved game-owned content preview and uninstall orchestration from
`DefaultSaveSafetyController` into `GameContentRemovalCoordinator`. The
controller now delegates these operations while retaining cloud-save
preflight and unrelated save-management behavior. The coordinator keeps the
per-game mutation lock, save-operation gate, cancellation-safe I/O section,
save backup and cloud acknowledgement requirements, internal/external file
preflight, and partial-removal install-state recovery together at the content
removal boundary.

Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`,
`:app:assembleDebugAndroidTest`, and `:app:lintDebug` succeeded. The focused
`GeneralContentRemovalTest` instrumentation suite passed on the emulator (3
tests), covering cross-game symlink protection, imported multi-file
uninstall/reimport with metadata retention, and aborting removal when save
backup fails. The instrumentation was run against synthetic test data; no
personal game files were touched. The connected Honor was available but was
not used for this storage-focused instrumentation pass.

### Shared Store console filters (2 October 2026)

Consolidated the compact Store's duplicated console-chip rows into
`StoreConsoleFilterRow`, so the selected-console behavior, brand marks, and
per-console accessibility descriptions are consistent across the Store
presentations. The compact row now also has the same accessible filter names
as the TV/wide row. This is a presentation-only cleanup; discovery state,
filter persistence, and selection callbacks remain owned by `CatalogScreen`.

Verification: unit tests, debug and instrumentation APK builds, and lint all
succeeded. The focused Compose test
`BlueprintScreenshotsTest#compactStoreExposesDirectConsoleFilters` passed on
the emulator (1 test). The Honor's wired external display was confirmed active
at 1920×1080, but this compact-only row consolidation does not alter its wide
catalog layout, so it was not redeployed for a redundant TV check.

### Compact Store discovery presentation boundary (2 October 2026)

Extracted the compact Store's discovery header, metadata status/sync feedback,
console selection, configured-source results, and discovery-game list into
`CompactCatalogDiscoverySection`. `CatalogScreen` continues to own settings,
search/filter state, selection, repository observation, and navigation; the
new section receives explicit values and callbacks and only renders them.
This trims the stateful Store screen and gives its compact presentation a
clearer separation without changing its content order or copy.

Verification: unit tests, debug and instrumentation APK builds, and lint
succeeded. `BlueprintScreenshotsTest#compactStoreExposesDirectConsoleFilters`
passed on the emulator (1 test). The TV/wide Store does not use this compact
section; existing Honor/TV screenshots remain the relevant wide-layout visual
evidence.

### Shared discovery-import registration mapping (2 October 2026)

Both single-file and multi-disc imports now construct the Library registration
through `importedGameRegistration`. The shared mapping keeps title identity,
release year, favorite state, artwork, description, players, region, launch-file
hash/MIME, and companion-file list consistent across both import paths. It
also rejects a launch file that is not part of the verified import set before
repository registration.

Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`,
`:app:assembleDebugAndroidTest`, and `:app:lintDebug` succeeded. Two new unit
tests passed for metadata/multi-file preservation and rejecting a detached
launch file. No actual user game files were used for these tests.

### Honor TV Home-to-Details controller navigation smoke (2 October 2026)

Installed the verified current debug APK on the connected Honor Magic6 Pro and
launched `MainActivity` on the wired HDMI display (logical display 249,
1920×1080). Injected D-pad-down events moved focus from the Home hero into the
Explore games row; the Home scrolled to reveal the focused card above the
fixed GameBox footer. Select opened Galaxy Patrol details, and the controller
Back key returned to Home. Captures are
`docs/evidence/honor-tv-refactor-check.png`,
`docs/evidence/honor-tv-dpad-down.png`,
`docs/evidence/honor-tv-game-details-dpad.png`, and
`docs/evidence/honor-tv-dpad-back.png`.

This verifies app focus/scroll/selection behavior on the actual TV display.
The Home status panel reported “Controller Not connected,” so D-pad and Back
were Android key events injected through ADB, not input from a paired physical
controller. No game was installed or launched. Temporary capture files were
removed from the Honor after transfer.

### TV details focus traversal and homebrew lifecycle (2 October 2026)

Hardware testing exposed that D-pad traversal from the details hero did not
bring lower controls into view. Removed the redundant explicit focus target
wrapped around the Material primary-action button and applied the existing
`bringFocusedControlIntoView` policy to details actions and managed-save
actions. After installing the new debug APK on the Honor, repeated D-pad-down
events from the details entry state scrolled the long page to its lower
content/action section on the 1920×1080 HDMI display.

Also completed an actual managed install/uninstall round trip using only the
bundled Galaxy Patrol homebrew fixture. Before install, its exact app-private
path was absent. GameBox installed the 49,168-byte asset; on-device `sha256sum`
matched the pinned digest
`97c1757ffd6a5bc1a591809b2b0f8988741f61f6abd82889c148ecae8a2f471f`. The
uninstall preview reported one content file (49,168 bytes), no managed save
copy requiring cloud protection, and that saves/backups/metadata/history would
be retained. After confirming in GameBox, the success message reported one
file removed and the exact app-private path was absent again. The screenshots
are `docs/evidence/honor-tv-dpad-after-fix.png`,
`docs/evidence/honor-tv-uninstall-action.png`,
`docs/evidence/honor-tv-removal-dialog.png`, and
`docs/evidence/honor-tv-homebrew-clean.png`.

Verification: unit tests, debug and instrumentation APK assemblies, and lint
passed after the focus change. This is actual Honor/HDMI evidence, not an
emulator inference. No paired controller was connected: D-pad navigation was
injected as Android key events and the uninstall confirmation was tapped, so
physical controller pairing/button behavior remains unverified. Only the
test-owned bundled fixture was removed; no user game files were touched.

### Focused-scroll regression guard (2 October 2026)

Added `FocusedControlScrollTest`, which constructs a controller-focusable
scrolling column, requests focus on its last item, and asserts that the item is
both focused and displayed after `bringFocusedControlIntoView` runs. The test
passed on the emulator (1/1); unit tests, debug/instrumentation APK builds,
and lint also passed. The actual Honor/HDMI D-pad smoke above independently
confirmed details-page scrolling on the deployment display.

Rechecked Honor input devices during this pass: HDMI remains active at
1920×1080, but Android lists the external USB mouse/keyboard and no gamepad.
Physical controller button/pairing coverage therefore remains pending a
controller connection; automated and injected-key evidence are not represented
as physical-controller verification.

### Authorized-install terminal feedback (2 October 2026)

Changed the authorized-copy details feedback so only queued/running installs
show a progress bar and percentage. A successful result is shown only while
the content is present; after removal, stale success text is suppressed. Added
three Compose instrumentation cases covering active progress, installed
success, and success-state cleanup after uninstall. The targeted suite passed
on the API-35 emulator (3/3); unit tests, debug and instrumentation APK
assemblies, and lint passed.

Installed the resulting debug APK on the Honor and confirmed the HDMI display
is connected at 1920×1080. The captured TV framebuffer currently shows the
Honor lock screen (“Phone is locked, please unlock”), not GameBox, so this
specific UI change could not receive an on-screen hardware check. Evidence:
`docs/evidence/honor-tv-feedback-terminal-state.png`. The earlier unlocked
Honor/TV lifecycle and focus tests remain valid; no lock-screen bypass was
attempted. Resume hardware verification when the phone is unlocked.

### Download queue card boundary and initial focus (2 October 2026)

Extracted each transfer row from `DownloadsScreen` into `DownloadQueueItem`,
leaving queue ordering, controller shortcuts, telemetry sampling, and initial
focus orchestration with the screen. The extracted card owns the visual and
action states for a single transfer. Added an explicit Compose focus target for
the first actionable button and stopped applying its focus requester to
Cancel; this keeps the initial controller target unambiguous while retaining
the existing frame-retry behavior. Unavailable-source dismissal remains
explicit and preserves the same transfer/install-state cleanup.

Verification: Android unit tests, debug/instrumentation APK assemblies, and
lint passed. Each of the three `DownloadsScreenStateTest` cases passed when run
individually on the API-35 emulator, including Pause focus and empty-state Store
navigation. One class-wide run passed the first two cases but the third lost
its Compose hierarchy; rerunning that third case alone passed, so this runner
interaction remains noted rather than counted as a clean class-wide pass. The
latest APK has not had an unlocked Honor/TV visual check: at capture, the
device displayed its lock screen. No phone data or game content was changed.

### Discovery import workflow separation (2 October 2026)

Moved single-file and multi-disc import orchestration out of
`DiscoveryDetailsScreen` into `DiscoveryImportWorkflow`. The workflow now owns
display-name lookup, the per-game mutation lock, verified import, Library
registration, transaction confirmation/rollback, and source cleanup; the
composable keeps progress/message state and launches the appropriate workflow
operation. Unexpected coroutine failures now produce visible import feedback
and always clear the busy state. `DiscoveryDetailsScreen` is down from 422 to
309 lines, with the import transaction boundary in a separate 162-line file.

Verification: Android unit tests, debug/instrumentation APK assemblies, and
lint passed. `BrowserDownloadTrackingActionTest` passed (3/3) and
`ExternalDisplayActivityTest#tvControllerFocusesAuthorizedCopyImportAction`
passed (1/1) on the API-35 emulator. This exercises the discovery details
surface and TV-style focus route, but is not actual-Honor visual verification;
the Honor was locked during the latest framebuffer check. No test game or
user-owned file was modified.

### Cloud-save operation boundary (2 October 2026)

Extracted cloud-specific preflight, provider/credential/endpoint validation,
network gating, encrypted envelope upload/download, conflict preservation, and
safe error formatting from `DefaultSaveSafetyController` into
`ManagedCloudSaveOperations`. The controller now retains save-operation state,
busy/gate behavior, and local import/backup/export/restore actions, while
content removal continues to call the cloud uploader through its existing
callback. The controller is now 274 lines (previously 404); cloud transport
policy is isolated in a 165-line coordinator.

Verification: unit tests, debug/instrumentation APK assemblies, and lint
passed. `GeneralContentRemovalTest` passed on the API-35 emulator (3/3),
including production controller cloud-preflight behavior and the explicit
acknowledged local-backup removal path. The fixture uses synthetic save and
game files inside a temporary test directory and does not touch user content.
Also removed nullable-parent compiler warnings from the temporary test-file
setup. Actual-Honor cloud configuration/transport remains unverified.

### RetroArch export lifecycle separation (2 October 2026)

Moved temporary MediaStore publication, duplicate-name-safe path resolution,
persisted URI tracking, and cleanup into `RetroArchContentPublisher`. The
Android package gateway now concentrates on verified content and emulator
intent handoff, delegating the RetroArch-specific filesystem-path workaround.
Kept the existing fixture publication entry point while routing it through the
same publisher. `GameLaunchController.kt` is now 238 lines, with the publisher
isolated in an 87-line component.

Verification: unit tests, debug/instrumentation APK assemblies, and lint
passed. `GalaxyPatrolExportTest` passed on the API-35 emulator (2/2), exercising
real MediaStore duplicate-name handling and cleanup with synthetic test files.
This validates the export lifecycle, not emulator launch on the Honor; actual
device use of the path remains part of hardware verification.

### Blueprint design-system component boundary (2 October 2026)

Moved the reusable controller hint, rail, rail item, and panel primitives out
of `GameBoxApp.kt` into `BlueprintSurfaces.kt`, where they sit alongside the
shared Blueprint interaction and viewport components. Moved the tiny
controller-label accessor alongside runtime-device status. `GameBoxApp.kt` is
now 412 lines (previously 522) and retains the screen shell/navigation rather
than reusable visual primitives.

Verification: unit tests, debug/instrumentation APK assemblies, and lint
passed. `BlueprintNavigationTest` passed (1/1), and the real-activity
`BlueprintScreenshotsTest#captureRealScreens` passed (1/1) on the emulator,
visiting Home, Details, Library, Store, Media, PC, Settings, and Downloads.
This confirms emulator rendering/navigation after the component move, not a
new actual-Honor/TV visual pass.

### Library overview sidebar boundary (2 October 2026)

Moved the wide Library's storage, local-save, and collection-summary panels out
of `LibraryScreens.kt` into `LibraryOverviewSidebar.kt`. The screen retains
summary and device-storage derivation and passes the values to the component;
the component owns only the three vertically scrollable presentation panels.
`LibraryScreens.kt` is now 381 lines (from 404), and the extracted component is
63 lines.

Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`,
`:app:assembleDebugAndroidTest`, and `:app:lintDebug` passed. The current debug
APK was installed over the existing Honor installation and launched without
clearing data. `BlueprintScreenshotsTest#captureRealScreens` also passed (1/1)
on the API-35 emulator, covering the rendered app screens including Library.
An initial touch attempt did not leave Home. On a follow-up check, the latest
APK was installed on the Honor and an ADB-injected `KEYCODE_BUTTON_R1` moved
the projected UI from Home to Library; the extracted storage and saves sidebar
was visible. Screenshot evidence: `docs/evidence/honor-tv-root-input-check.png`.
This verifies the actual-device rendering and root key-event path, but the
shoulder input was injected rather than sent by a physical controller; the
physical controller remained disconnected. `git diff --check` found no
whitespace errors;
Git emitted only LF/CRLF normalization warnings.

### Settings preference mapping boundary (2 October 2026)

Moved DataStore preference decoding into the pure `Preferences.toGameBoxSettings`
mapper and moved stored preference-key declarations to
`SettingsPreferenceKeys.kt`. The repository now focuses on DataStore flow and
updates, configuration operations, and credential delegation; no preference
names or product defaults changed. `SettingsRepository.kt` is now 310 lines
(from 399), and the mapper is 52 lines.

Added unit coverage for empty-store defaults and for normalization of invalid
persisted provider status and out-of-range service ports. Both new tests passed.
Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`,
`:app:assembleDebugAndroidTest`, and `:app:lintDebug` all passed. This is a
data-layer refactor; it does not require a new hardware interaction check.

### Root controller-input policy boundary (2 October 2026)

Separated the root controller key mapping from `GameBoxApp` into
`GameBoxRootInput.kt`. The app shell remains responsible for performing
navigation and overlay state changes, while the new policy maps hardware key
codes to explicit commands and directional sound effects. This makes the
controller contract independently testable without Compose state or an
attached controller.

Added unit tests covering shoulder-tab changes, Start/Menu quick actions,
Select status, X/Y actions, Back/B, unknown keys, and D-pad navigation sounds.
Both tests passed. Verification: `:app:testDebugUnitTest`,
`:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:lintDebug`
passed. A physical-controller hardware check remains unverified because no
controller was connected.

### Settings presentation boundaries and Honor projection check (2 October 2026)

Extracted the controller, downloads, and emulator panels from `SettingsScreen`
into `SettingsDeviceAndDownloadSections`, and moved its audio panel into the
same focused presentation file. `SettingsScreen.kt` is now 370 lines (from
421); it still owns repository state, permissions, system intents, migration,
and navigation, while the extracted composables receive values and callbacks.

Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`,
`:app:assembleDebugAndroidTest`, and `:app:lintDebug` all passed. Installed the
debug APK over the existing app on the connected Honor (without clearing data)
and opened Settings on its active projection. The visible Settings screen
rendered with the expected TV-wide rail/content layout; screenshot evidence is
`docs/evidence/honor-tv-settings-refactor.png`. This is a visual smoke check,
not proof that every settings action works on hardware. The physical game
controller was not connected. `git diff --check` reported no whitespace
errors; Git printed only the repository's existing LF/CRLF normalization
warnings.

### Shared Library/Store filter state and controller-only search (2 October 2026)

Consolidated the duplicated query/platform/genre/region/language/favorites
state and persistence logic from Library and Store into
`rememberGameFilters`. Their saved values remain isolated under the existing
`library.*` and `store.*` keys. Added a Compose test that restores each
namespace independently, changes Store values, and verifies only Store state
was updated; `RememberedGameFiltersTest` passed (1/1).

The Honor projection exposed that controller search was enabled only when
Android reported a secondary display. Wide mirrored layouts can still be on
the default display, so both Library and Store now enable the controller
keyboard for wide (`!compact`) layouts. Also mapped actual Android
`KEYCODE_BUTTON_A` to activation in the controller search trigger and keys, and
`KEYCODE_BUTTON_B`/Back to dismissing the keyboard. `ControllerSearchKeyboardTest`
passed (2/2) using gamepad key codes. The full unit suite, both APK assemblies,
and lint passed.

On the connected Honor, installed the APK without clearing data and verified
the projected Home → Library → Store → Search → type → close route with
ADB-injected R1, X, A, and B key events. A added the initially focused `Q` to
the search (the Store showed no matches), and B returned to Store with the
filter active. This is actual-device rendering and input dispatch, but the
events were injected because no physical controller was connected. Evidence:
`docs/evidence/honor-tv-controller-search-fixed.png` and
`docs/evidence/honor-tv-controller-search-a-input.png`.

### Import journal preflight invariant reuse (2 October 2026)

Removed duplicate pending-import validation from `ImportRegistrationJournal`:
`prepare` now constructs the transaction and calls the same schema/path/checksum
validator used when reading or recovering a marker. Validation still occurs
before the imports directory is created, so invalid input has no filesystem
side effects. The journal is now 324 lines (from 340).

Added a regression test confirming an unsafe traversal path is rejected before
journal storage is created. All 9 `ImportRegistrationJournalTest` cases passed,
as did the full unit suite, debug and instrumentation APK assemblies, and lint.

### Responsive controller search keyboard on Honor TV output (2 October 2026)

The prior Honor projection screenshot showed the final digit/letter and lower
keyboard actions clipped. Made the controller keyboard adapt its width, key
sizes, spacing, and height to the available display; the dialog scrolls to
focused controls where needed. On short layouts it omits the optional keyboard
hint so the complete action row remains in view. Added regression assertions
for the `0` and `P` keys and visibility of the Finish search action.

Verification: the full `:app:testDebugUnitTest`, `:app:assembleDebug`,
`:app:assembleDebugAndroidTest`, and `:app:lintDebug` gate passed. The focused
`ControllerSearchKeyboardTest` instrumentation suite passed 2/2 on the API-35
emulator. Installed the build on the Honor without clearing app data and used
the in-app **Show on TV** action to open the dedicated external-display host on
the actual 1920x1080 output (Android display 249). Confirmed the complete
keyboard/action row is visible and used display-targeted gamepad A/B events to
type `Q` and close the keyboard. Evidence: `docs/evidence/honor-tv-controller-search-responsive.png`,
`docs/evidence/honor-tv-controller-search-a-input.png`, and
`docs/evidence/honor-tv-store-refactor.png`. Inputs were ADB-injected; no
physical controller was connected. Honor Desktop Mode retains its own bottom
launcher/taskbar outside GameBox's window policy, so this check does not prove
that system-owned chrome can be suppressed.

### Home dashboard surface-specific setup prompt (2 October 2026)

The real external-display Home screenshot showed the phone's Android Home-role
setup banner occupying the primary TV dashboard. Kept the prompt and its
initial-focus behavior on the phone, but suppress both on the external-display
surface where that phone-level role action is not relevant. The
`HomeRoleStatusTest` policy check passes, and the full unit-test,
debug/instrumentation assembly, and lint gate passed after the change. Updated
the app in place on the Honor without clearing data, relaunched through PC
Hub → Show on TV, and verified that Home now begins with Continue Playing and
the hero card rather than the setup banner. Evidence:
`docs/evidence/honor-tv-home-no-phone-setup.png`. The Honor desktop shell still
owns its bottom system taskbar.

### Root controller dispatch extraction (2 October 2026)

Moved shell-level key-event dispatch out of the `GameBoxApp` layout chain into
`Modifier.onGameBoxRootInput`. The root composable now supplies navigation,
overlay, and fallback actions; key classification, repeat-safe navigation
sounds, controller action dispatch, and back consumption live together at the
input boundary. Existing pure mapping coverage remains in
`GameBoxRootInputTest`.

Verification: full unit tests, debug and instrumentation APK assembly, and
lint passed; `ControllerSearchKeyboardTest` passed 2/2 on the emulator with the
new build. The APK was installed on the Honor without clearing app data, but
the phone locked before I could confirm this refactor's input route on the TV.
No unlock attempt was made; the post-change hardware input check remains
pending. Added `GameBoxRootInputModifierTest` to exercise the extracted
modifier's tab, overlay, X/Y registry, and Back callbacks; it passed on the
emulator together with both `ControllerSearchKeyboardTest` cases (3/3). The
previously verified external-display screenshots document the pre-refactor
visual state only.

### Game-details save context extraction (2 October 2026)

Moved per-game save-controller scope ownership, cleanup, managed-save summary
collection, and summary wording out of `GameDetailsScreen` into
`GameDetailsSaveContext`. The details screen now consumes one save context for
the active controller and status text. A child coroutine scope is only created
when a per-game controller is both enabled and configured; its lifecycle is
still cancelled when the details composable leaves composition. Added unit
coverage for empty, present, error, and still-inspecting save summaries.

Verification: the full `:app:testDebugUnitTest`, `:app:assembleDebug`,
`:app:assembleDebugAndroidTest`, and `:app:lintDebug` gate passed. Honor remains
keyguard-locked, so no post-change TV rendering check was possible in this
turn; device status was checked read-only, and no unlock attempt was made.

### App-shell dependency wiring cleanup (2 October 2026)

Changed `GameBoxApp` from eleven individually threaded repository/controller
parameters to the existing `AppContainer` contract. It now obtains its
dependencies from the same container used by `GameBoxHostActivity` and remembers
the per-game save-controller factory there. This removes duplicated wiring at
the Activity/Compose boundary without changing the dependency source or
runtime ownership.

Verification: full unit tests, debug and instrumentation APK assembly, and
lint passed. On the emulator, `BlueprintNavigationTest`,
`GameBoxRootInputModifierTest`, and `ControllerSearchKeyboardTest` passed (4/4),
covering host navigation and controller behavior after the signature change.
The Honor remains locked, so this app-shell change still needs a fresh
external-display check when hardware is available.

### Destination rendering boundary extraction (3 October 2026)

Moved the Home/Library/Store/Downloads/Media/PC/Settings switch and selected
game-details rendering out of `GameBoxApp` into `GameBoxDestinationContent`.
The new route state/actions carry screen inputs and user intents explicitly;
the app shell retains shared overlays, primary navigation, focus restoration,
and host-level input. Preserved the Home-to-Store search-focus request/acknowledge
handoff and remembered the save-controller factory at the route boundary.

Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`,
`:app:assembleDebugAndroidTest`, and `:app:lintDebug` passed. On the emulator,
`BlueprintNavigationTest`, `GameBoxRootInputModifierTest`, and
`ControllerSearchKeyboardTest` passed (4/4) after the extraction. The Honor is
still keyguard-locked, so the refactored route has not yet had a fresh TV
hardware check.

### Catalog refresh UI boundary and dead-state cleanup (3 October 2026)

Moved the shared catalog refresh-status announcement and refresh-button
accessibility description into `CatalogRefreshStatus.kt`, where both compact
and wide catalog layouts consume the same semantics. Removed unused imports
and the never-read `emptyCatalogActionFocusClaimed` state from
`StoreCatalogScreen`; kept the compact Store and Blueprint wide layout because
both are active branches of the same catalog route, not redundant screens.

Verification: the full unit-test, debug/instrumentation APK assembly, and lint
gate passed. `CatalogRefreshStateTest` passed on the emulator, confirming the
refreshed shared announcement remains visible/actionable. The Honor is still
keyguard-locked; no fresh hardware check was attempted.

### Shared game-details action extraction (3 October 2026)

Moved the shared secondary action, browser-download tracking action, and
metadata metric composables into `GameDetailsActions.kt`. This gives discovery
details and imported-game detail flows one implementation for TV/compact button
semantics and metric styling, while reducing responsibilities in the details
screen. Preserved the metric icon's 15 dp size during the move.

Verification: `:app:testDebugUnitTest`, `:app:assembleDebug`,
`:app:assembleDebugAndroidTest`, and `:app:lintDebug` all passed. The focused
`BrowserDownloadTrackingActionTest` passed 3/3 on the emulator. It was also
attempted on the connected Honor, but the locked keyguard prevented the Compose
test Activity from exposing a hierarchy (3 harness failures before assertions
could inspect the UI); those results are not a product pass. TV rendering on
the Honor remains pending an unlocked session.

### Non-blocking storage permission inspection (3 October 2026)

Moved external-library and emulator-save-root document-provider inspection off
the Settings composition/UI thread. Inspections now run on `Dispatchers.IO`,
show an explicit accessible checking state while a URI is being checked or
changed, and keep content migration disabled until the selected library is
confirmed writable. SAF adopt/forget permission operations also run on IO so a
slow provider cannot freeze the Settings screen.

Verification: the full unit-test, debug/instrumentation APK assembly, and lint
gate passed. `SettingsStorageInspectionTest` passed 1/1 on the emulator against
the final rebuilt APK, verifying both loading announcements and the disabled
migration action. The Honor remains keyguard-locked; this test was not counted
as physical-device or TV-rendering evidence.

Follow-up cleanup consolidated the two storage-root checks into the shared
`rememberStorageStatus` helper so URI reset, IO dispatch, and loading behavior
cannot drift between library and emulator-save settings. Extended
`SettingsStorageInspectionTest` to assert the inspector executes off the main
thread and that provider failures leave the loading state with a readable error;
the focused class now passes 3/3 on the emulator. The full Gradle gate was
rerun successfully after this cleanup.

### Downloads controller callback stability (3 October 2026)

Memoized the actionable download/game pairs and changed the controller-action
effect to depend on the semantic action plan rather than the freshly-created
list instance. A `rememberUpdatedState` reference keeps retry/pause/resume
callbacks pointed at the latest actionable jobs without unregistering and
reregistering controller handlers on byte-progress-only recompositions.

Verification: the full unit-test, APK assembly, instrumentation compilation,
and lint gate passed. `DownloadsScreenStateTest` passed 3/3 on the emulator,
covering active, terminal, empty, and controller-focus behavior. The Honor is
still keyguard-locked with HDMI attached, so this change has not been visually
verified on the TV hardware.

### Shared game-details artwork frame (3 October 2026)

Extracted `GameArtworkHeroFrame` and adopted it in both owned-game and discovery
details. The shared frame owns the rounded surface, artwork fallback, and
readability gradient; each flow retains its own height, border tone, metadata,
installation status, and import/play actions. This reduces duplicated hero
painting while aligning the two core details surfaces.

Verification: the full unit-test, debug/instrumentation APK assembly, and lint
gate passed. `GameDetailsHeroTest` passed 2/2 on the emulator, preserving Play
and install-state actions. The Honor is keyguard-locked, so this visual
refinement still needs a real TV rendering check when the device is unlocked.

Follow-up review corrected indentation left by the extraction in both hero
content blocks. The full Gradle gate and `GameDetailsHeroTest` (2/2) were rerun
successfully against the formatted source.

### Throttled game-import progress notifications (3 October 2026)

Added a shared byte-progress throttle for archive copying and ZIP/7z extraction.
The import UI now receives updates at roughly MiB boundaries plus a final update
for each file, rather than an event for every small archive read. The helper
also rejects backwards byte counts, keeping determinate progress monotonic.

Verification: the helper and its focused JVM tests compiled with the project's
cached Kotlin 2.3.21 compiler and passed 3/3 using JUnit 4.13.2. The archive
extractor and its test sources also compiled in isolation, and the new large-ZIP
throttle case passed 1/1 when run alone. The complete standalone archive JUnit
class still stalled after its first reported test, so no full archive-suite
pass is claimed. The project-wide Gradle gate remains unavailable because the
current Gradle cache lacks resolvable metadata for Android Gradle Plugin 9.3.0,
despite the plugin JAR being present. Physical Honor-to-TV verification is still
pending an unlocked device session.

### Full-bleed external-display content insets (3 October 2026)

The shared activity host previously applied Compose `safeDrawingPadding()` on
both the phone and projected TV display. It now preserves system-safe padding on
the phone but omits it for an external display, allowing the fullscreen TV host
to lay out to the complete HDMI surface. The decision is isolated in the pure
`HostContentInsetsPolicy` instead of being coupled to Activity/window code.

Verification: the policy and its JVM test compiled with the cached Kotlin
2.3.21 compiler and passed 1/1 under JUnit 4.13.2. Honor visual confirmation is
still required. A read-only ADB check with the required process-launch permission
found the Honor connected but keyguard-locked; Android currently exposes only
the phone panel and an 800×1738 virtual screen-share display, not the 1920×1080
HDMI sink. No TV screenshot or external-display runtime validation was collected.

### Cooperative cancellation during local imports (3 October 2026)

Large imports now check their coroutine cancellation at source-file and
throttled-progress boundaries. Cancellation is rethrown instead of being
converted to an ordinary failed-import result, allowing the import's existing
staging cleanup to remove partial copies. Added an Android integration test
that cancels a synthetic 4 MiB import at the first MiB progress update and
checks that reading stops early and no staging or promoted game remains.

Verification: source review confirms cancellation checks execute within the
IO-bound copy/hash/extraction flow and that cancellation reaches the cleanup
`finally` block. The changed importer sources compiled with the cached Kotlin
2.3.21 compiler against the existing app classes and Android 36 SDK. The new
Android integration test and full build remain unrun because the local Gradle
cache cannot resolve the configured Android Gradle Plugin 9.3.0.

Follow-up: added a controller-focusable Cancel action beside import progress in
the authorized-copy and discovery flows. Those screens now retain the import
Job, show a cancellation status, and let coroutine cancellation reach the
staging cleanup path. Discovery registration rollback now runs in
`NonCancellable` when cancellation arrives after files have been promoted, so
the file transaction is reconciled before cancellation propagates. Reimport
repair remains non-cancellable after its confirmed repair operation begins to
preserve its existing finish-registration safety contract. Added a Compose test
for the visible cancel action; it has not been run because the Gradle plugin
resolution issue remains unresolved. The updated `DiscoveryImportWorkflow` and
its same-module helper sources compiled with the cached Kotlin compiler against
the existing app classes and Android 36 SDK. The new cancellation test source
also compiles against cached AndroidX test artifacts, but was not executed;
screen-level Compose sources and the Compose cancel-action test remain
unverified.

### Store catalog recomposition cost (3 October 2026)

Memoized the Store's selected-console filter, metadata/favorite/install filters,
sorting, and per-console title counts. Sync-progress and other unrelated state
updates can now recompose the screen without repeating the catalog-wide
filter/sort/count passes; the keys still invalidate each derived result when
its source list, selected console, or relevant filter changes. This preserves
the existing catalog presentation and controller focus flow.

Verification: source review confirms each selector is keyed by every value it
reads. Extracted the favorite/install/metadata filtering and rating/title sorting
into pure selectors and added behavior tests; those sources compiled with the
cached Kotlin compiler and passed 2/2 under JUnit against the existing app
classes. The full unit/instrumentation/lint gate and Store rendering checks could
not be rerun because Android Gradle Plugin 9.3.0 remains unresolved in the local
Gradle setup. The shared Homebrew/Retro/Android/source-port platform policy test
also passed 1/1 in isolation.

Follow-up verification: compiled `BlueprintCatalogScreen.kt` together with its
new selector and platform-policy sources using the cached Kotlin 2.3.21 compiler
and Compose compiler plugin, Android 36 SDK, cached Compose artifacts, and the
current app classes as a friend module. The focused source compile passed. This
checks the screen's Kotlin/Compose source against its app dependencies, but does
not replace the Gradle build, instrumentation suite, or on-device TV rendering
check; those remain outstanding.

### Settings spacing/comment cleanup (3 October 2026)

Removed a duplicate KDoc on `rememberStorageStatus` and normalized the trailing
spacing before developer diagnostics from two stacked spacers to one section
gap. This removes an accidental oversized visual break without changing
navigation, state, or actions.

Verification: compiled the complete `SettingsScreen.kt` source with the cached
Kotlin 2.3.21 and Compose compiler plugins, Android 36 SDK, current app classes,
Compose classes, and cached AndroidX Activity/Core classes. The focused compile
passed. Full Gradle/UI and Honor-to-TV checks are still separate outstanding
gates.

### Fresh full local Gradle gate (3 October 2026)

The prior plugin-resolution blocker is no longer preventing a local build. With
the cached Gradle 9.7.1 distribution, Android Studio's bundled JDK, and offline
dependency resolution, the full local gate completed successfully:
`testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`.

Verification: 649 JVM tests completed with 0 failures, 0 errors, and 1 skipped;
debug and instrumentation APKs were rebuilt, and instrumentation Kotlin sources
compiled. Lint completed successfully and reported 29 warnings (including API,
dependency-version, and storage-path notices); these are not lint errors. This
does not establish a hosted-CI pass or a post-change instrumentation runtime
pass. The Honor was connected during the check, but Android exposed only its
1312×2848 built-in panel (currently off) and the 800×1738 Studio screen-sharing
virtual display, not an HDMI TV display, so no TV rendering validation was
possible.

### Compose modifier API cleanup (3 October 2026)

Changed `DownloadQueueItem` to accept an optional `FocusRequester` instead of a
preassembled child modifier. The row now owns the focus-requester/focusable/
bring-into-view chain applied to its primary action. Renamed the first modifier
argument on `SettingsDeviceAndDownloadSections` to the conventional `modifier`.
This makes the reusable composables' focus and layout contracts clearer while
preserving which download action receives initial focus.

Verification: reran the full offline Gradle gate after the changes. All 649 JVM
tests passed (0 failures/errors, 1 skipped), debug and instrumentation APKs
assembled, and instrumentation sources compiled. Lint completed with 29
warnings, down from 31; no lint errors. No instrumented UI tests were run in
this pass, and the Honor-to-TV surface is still unavailable for visual
confirmation.

### Downloads storage-capacity snapshot (3 October 2026)

The Downloads screen now shares a single storage snapshot for capacity warnings,
free-space labels, and the usage bar. It reads Android's allocatable-byte value
off the UI thread and refreshes every 30 seconds while the screen is composed;
download-progress recompositions no longer repeat filesystem-capacity queries.
The value is temporarily presented as “Checking…” before the first query, and
the worker's independent preflight remains authoritative before writing.

Added JVM coverage for used-space fraction bounds and unknown totals. The full
offline Gradle gate passed: 651 tests, 0 failures/errors, 1 skipped; both APKs
assembled and instrumentation sources compiled. Lint warnings fell from 29 to
25, including removal of four direct-`usableSpace` warnings from Downloads. The
remaining 25 warnings are still to be triaged; no real-TV rendering test ran.
Fresh ADB state after that build shows the Honor is awake, but Android still
enumerates only its 1312×2848 built-in panel and the 800×1738 Studio
screen-sharing virtual display; the docked TV is not exposed as an Android
display in this session.

### Shared Store/Library filter derivation (3 October 2026)

Store and Library now use one `rememberFilteredGames` derivation keyed by the
game list and every persisted filter value; the Library summary is also keyed
only to its game list. This avoids repeating list filtering/counting during
unrelated discovery, focus, or screen-state recompositions without changing the
filter semantics.

Verification: existing `GameFilteringTest` cases cover query, platform, genre,
metadata, and favorite behavior. The full offline Gradle gate passed afterward:
653 tests, 0 failures/errors, 1 skipped; APKs assembled, instrumentation sources
compiled, and lint completed with 25 warnings. No on-device rendering check was
available.

### Settings SAF tree-picker lifecycle extraction (3 October 2026)

Extracted the duplicated external-library and emulator-save document-tree
launch/adopt/error lifecycle into `rememberStorageTreePicker`. Controllers still
own persistence and permission handling; the helper centralizes external-
display picker options, IO dispatch, coroutine scope, and flow-specific
fallback messages. Added JVM coverage for successful controller messages and
exception/fallback messaging.

Verification: the full offline Gradle gate passed with 653 tests, 0 failures,
0 errors, and 1 skipped; APK assembly, instrumentation source compilation, and
lint succeeded with 25 warnings. Fresh ADB enumeration still shows only the
Honor's 1312×2848 built-in screen and the 800×1738 Studio screen-share virtual
display—no HDMI output—so real-TV picker behavior remains unverified.

### Import progress delivery boundary (3 October 2026)

Extracted progress-observer delivery from the importer transaction body into
`RomImportProgressReporter`. It constructs the stage/file/byte event, checks
the import coroutine context before notifying, propagates cancellation from
both the context and observer, and keeps ordinary observer exceptions
non-fatal. This preserves the import transaction while making its progress
contract directly testable.

Added four JVM tests for event delivery, ignored observer failures, propagated
observer cancellation, and pre-cancelled import contexts. The full offline
Gradle gate passed: 657 tests, 0 failures/errors, 1 skipped; APKs assembled,
instrumentation sources compiled, and lint completed with 25 warnings. The
Honor still exposes no HDMI display, so physical TV import-progress validation
remains outstanding.

### Shared ZIP/7z bounded archive writer (3 October 2026)

Replaced duplicate ZIP and 7z file-write loops with one bounded writer that
accepts each archive reader as a byte-chunk function. Both formats now use the
same remaining-size enforcement, output write, and progress callback sequence;
format-specific iteration and entry validation remain in their respective
extractors.

Verification: `RomArchiveExtractorTest` passed 7/7, covering both archive
formats and existing safety cases. The full offline Gradle gate passed with 657
tests, 0 failures/errors, 1 skipped; debug and instrumentation APK tasks,
instrumentation source compilation, and lint succeeded (25 warnings remain).
No on-device extraction flow was exercised in this pass.

### Remembered catalog filter options (3 October 2026)

`GameFilterBar` now derives platforms, genres, regions, and languages in one
pass and remembers those option lists by the game list and extra discovery
regions. Query, focus, and selection-state changes no longer rebuild the option
sets, while the existing distinctness, case-insensitive region ordering, and
extra-region behavior are preserved.

Added `GameFilterOptionsTest` for sorting, deduplication, blank regions, and
discovery-supplied regions. The full offline Gradle gate passed with 658 tests,
0 failures/errors, 1 skipped; both APK tasks, instrumentation-source
compilation, and lint succeeded (25 warnings remain). No physical-TV rendering
check was available.

### Shared storage presentation and typed app hubs (3 October 2026)

Home, Library, Settings, diagnostics, and the system-status overlay now consume
the shared 30-second storage-capacity snapshot used by Downloads. This removes
separate `usableSpace` reads during UI recomposition and keeps the reported
capacity consistent. PC and Media hub behavior is now selected by the typed
`AppHubKind`, rather than comparisons against the localized/display title.

### Borderless window policy modernization (3 October 2026)

Removed deprecated `FLAG_FULLSCREEN` and `FLAG_FORCE_NOT_FULLSCREEN` usage.
System bars remain hidden through `WindowInsetsControllerCompat`; the explicit
no-limits window and display-cutout policy are unchanged. Instrumentation now
asserts system-bar visibility rather than legacy flag bits.

The offline Gradle gate passed after these changes: 658 tests, 0 failures,
0 errors, and 1 skipped; app and instrumentation APKs assembled and lint
reported 20 findings. The current APK and test APK were installed on the
physical Honor with `adb install -r`, preserving app data. The two focused
borderless-window and configuration-reapply instrumentation tests passed on
the Honor. Current ADB display enumeration still exposes only the phone panel
and Studio screen-sharing virtual display; these handset checks do not prove
the projected TV layout or close the physical-TV acceptance gate.

### Store discovery detail-route extraction (3 October 2026)

Moved configured-source and catalog discovery detail rendering into
`CatalogDetailsRoutes.kt`. `CatalogScreen` continues to own selected-result
state, route persistence, favorite updates, and back transitions; search/filter
orchestration no longer contains the detail presentation setup.

Verification: the full offline Gradle gate passed with 658 JVM tests, 0
failures/errors, and 1 skip; app and instrumentation APKs assembled and lint
completed with 20 findings. The route extraction preserves the existing
`DiscoveryDetailsScreen` and controller workflows. No TV display is currently
exposed by the Honor, so this change has not been validated on the projected
screen.

### Store selection-state consolidation (3 October 2026)

Added `StoreSelectionState` to keep the selected console and selected discovery
detail together. Changing consoles now clears a stale selected discovery ID;
reselecting the current console preserves it. Screen-state persistence is
centralized in `rememberStoreSelectionState`, and focused JVM tests cover
change, same-console selection, and clear/select transitions.

The full offline Gradle gate passed with 661 JVM tests, 0 failures/errors, and
1 skip; app and instrumentation APKs assembled, instrumentation sources
compiled, and lint reported 20 findings. No TV display is exposed on the
Honor in the current session, so this Store navigation refinement has not been
verified on the projected screen.

The same build was installed on the Honor with `adb install -r` and the focused
`BlueprintScreenshotsTest.compactStoreExposesDirectConsoleFilters` was
attempted. It failed before any Store assertion because Compose found no active
hierarchy. Read-only device diagnostics showed the phone awake but on the
lock-screen dream (`mDreamingLockscreen=true`), with `NotificationShade`
focused, no focused app, and `MainActivity` destroyed by instrumentation. This
is a locked-device test precondition, not evidence of a Store UI failure. The
device was left locked; repeat after the user unlocks it. TV projection remains
unavailable in ADB and is not validated by this handset attempt.

### Settings storage lifecycle extraction (3 October 2026)

Moved external-library and emulator-save SAF controller creation, asynchronous
permission/status inspection, and document-tree picker actions into
`rememberSettingsStorageManagement`. The Settings screen still owns storage
permission removal and installed-content migration, preserving the boundary
between lifecycle setup and destructive/user-confirmed operations.

Verification: the full offline Gradle gate passed with 661 JVM tests, 0
failures/errors, and 1 skip; app and instrumentation APKs assembled and lint
completed with 20 findings. At the last ADB check, the Honor was lock-screened
and Android did not expose the TV display; post-change Settings UI acceptance
on hardware therefore remains open.

### Bounded archive-source staging (3 October 2026)

The importer now checks known ZIP/7z source sizes against the remaining
transaction allowance before opening them, and applies the same limit while
streaming sources whose size is unknown. `copyBoundedArchiveSource` removes
partial staging output when the limit, read, or progress callback fails; the
existing extracted-content ceiling and transaction rollback remain unchanged.

Added two JVM tests for exact-limit copying/progress and oversized-source
cleanup. The full offline Gradle gate passed with 663 tests, 0 failures/errors,
and 1 skip; app and instrumentation APKs assembled and lint completed with 20
findings. The last ADB check showed the Honor locked and no TV display, so
on-device archive-import verification was not attempted.

### Import progress cancellation stream boundary (3 October 2026)

Moved initial copy-progress delivery ahead of opening the Android resolver
stream. If the observer cancels or throws at the first event, no source stream
has been acquired; later per-chunk callbacks remain inside `use` scopes so
cancellation closes both source and staging streams. Added a test proving a
progress-callback failure removes the temporary archive after copy.

The full offline gate passed with 664 JVM tests, 0 failures/errors, and 1 skip;
debug and instrumentation APKs assembled and lint completed with 20 findings.
Physical import testing remains open while the Honor is locked and HDMI is not
enumerated.

### Installed-content migration confirmation accuracy (3 October 2026)

Corrected the confirmation dialog: the migration copies each installed file to
the selected SAF tree, verifies the destination, and then removes the internal
source. The previous copy promised no files would be deleted, which contradicted
`SafDocumentTreeCopyOperation(deleteSourceAfterVerification = true)`. The new
message states the verified-delete condition and that game records/saves are
kept; the Compose test assertion was updated accordingly.

The full offline gate passed (664 JVM tests, 0 failures/errors, 1 skip), and
both APKs assembled with the updated instrumentation test compiled; lint
reported 20 findings. The Compose assertion was not run on the Honor because
the handset was lock-screened, and was not substituted with emulator execution.

### Installed-content migration scan recovery (3 October 2026)

Kept installed-file planning on `Dispatchers.IO` and made scan failures an
explicit state instead of leaving Settings indefinitely at “Scanning installed
content…”. A failed scan now explains that files may be inaccessible, offers a
retry action, and keeps migration confirmation disabled until a complete plan
is available. Coroutine cancellation is rethrown so leaving Settings does not
turn cancellation into a misleading scan failure.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 664 JVM tests (0 failures/errors, 1 skip); both APKs were
assembled and the added instrumentation coverage compiled. Lint reported 20
findings. The Compose recovery assertion has not been run on hardware. ADB now
sees the Honor awake, but its keyguard is showing; the only other display is
Studio's virtual screen-share display, not the projected TV. The device was not
unlocked or altered for this check.

### Settings navigation component extraction (3 October 2026)

Moved the wide-layout Settings section rail out of `SettingsScreen` into the
shared Settings chrome. Added an interaction test that checks the selected
section semantics, confirms controller focus alone does not select a section,
and verifies activation updates the selected destination. The phone remains
keyguarded and no HDMI display is enumerated, so TV rendering remains
unverified.

Verification: the complete offline Gradle gate passed; the focused
`SettingsSectionNavigationTest` also passed on the API-35 emulator (1/1). This
is supporting software coverage, not Honor/TV acceptance.

### Compact App Hub dead-layout removal (3 October 2026)

The compact `AppHubScreen` path returned early to `BlueprintAppHubScreen` for
wide layouts, but still retained a second wide-grid branch (`chunked(3)`) below
that return. Since the remaining path is compact-only, simplified it to a
single-column shortcut list and removed the unreachable responsive sizing and
spacer branches. This preserves wide presentation in its dedicated screen and
reduces duplicated layout logic.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed; the JVM suite remains at 664 tests (0 failures/errors,
1 skip), and both APKs assembled. Lint reported 20 findings. The Honor remains
awake but keyguarded; display enumeration shows only its built-in panel and
Studio's virtual screen-share display, so the TV path was not exercised.

### App Hub wide-layout boundary extraction (3 October 2026)

Moved the wide PC/Media hub presentation into `AppHubWideScreen.kt`, leaving
`AppHubScreen.kt` responsible for settings-derived visibility, launch
coordination, controller actions, and the compact phone layout. This makes the
wide launcher surface independently maintainable without changing its
launch/status inputs.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 664 JVM tests (0 failures/errors, 1 skip); app and
instrumentation APKs assembled and lint reported 20 findings. The Honor remains
keyguarded with no HDMI display enumerated; the extracted wide layout still
needs actual docked-TV visual and controller acceptance.

### Android lint cleanup (3 October 2026)

Guarded the API-33 notification permission launcher at its own call site and
documented the RetroArch core-path lint suppression: that path must point into
RetroArch's separate private app directory, not GameBox's `filesDir`. This
removed both corresponding lint findings. Attempted to drop the adaptive-icon
`v26` resource qualifier because lint calls it obsolete for this app's minimum
SDK, but AAPT then failed to resolve the manifest's mipmap resource; restored
the required qualifier and confirmed the app links again.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 664 tests (0 failures/errors, 1 skip), both APKs
assembled, and lint findings decreased from 20 to 18. The remaining findings
are newer dependency/SDK suggestions plus manifest/window-policy advisories;
those were not changed without compatibility and docked-device validation.

### Typed App Hub shortcut roles (3 October 2026)

Added explicit `AppShortcutRole` values for the featured PC-streaming app and
file manager. The wide hub now chooses the featured hero and file action by
role rather than comparing visible title text, so display-name edits no longer
silently change layout or remove actions. Added catalog unit coverage for the
PC roles and standard media shortcuts.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 665 JVM tests (0 failures/errors, 1 skip); both APKs
assembled and lint reported 18 findings. The wide Hub still lacks current
Honor/TV visual acceptance because the phone is locked and HDMI is not exposed.

### Role-derived PC streaming status (3 October 2026)

Removed the remaining hardcoded `com.limelight` checks from Hub presentation
state. Installed status and recent-session entries now derive from the
featured-streaming shortcut role and that catalog item's package, allowing the
identity to change without breaking UI status. Extracted this mapping into a
pure presentation function and added tests for renamed/repackaged featured
apps and for a hub with no featured streaming shortcut.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 667 tests (0 failures/errors, 1 skip); both APKs
assembled, lint remained at 18 findings, and `git diff --check` passed. ADB
still reports the Honor awake with its keyguard showing and no external HDMI
display; TV presentation remains unverified.

### ROM source-staging boundary extraction (3 October 2026)

Moved per-source handling into `RomImportSourceStager`: it now owns safe
archive/plain-file stream copying, bounded ZIP/7z extraction, per-file hashing,
and progress reporting. `AuthorizedRomImporter` retains import-set validation,
launch-file selection, reimport identity, registration journaling, atomic
directory replacement, and rollback. The stager keeps the remaining-byte and
remaining-file ceilings across sources and extracted archive entries.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 667 JVM tests (0 failures/errors, 1 skip); app and
instrumentation APKs assembled, and lint reported 18 findings. Existing
importer cancellation, bounded staging, extraction, identity, and registration
tests all remain in the executed JVM suite. End-to-end SAF import on the
docked-Honor/TV setup remains outstanding.

### Registration journal I/O and transaction serialization (3 October 2026)

`confirmRegistration` re-hashes the committed game files to verify their
checksums before deleting backups; it previously did this synchronously on the
calling coroutine, commonly the UI dispatcher. Confirmation and rollback are
now suspend operations that serialize through the same importer mutex as
imports/recovery and run on `Dispatchers.IO`. They execute non-cancellably so a
cancel between Room registration and filesystem-journal completion cannot
strand or roll back an already-registered game. Updated the reimport test's
nullable parent-directory setup warnings as part of recompiling its API use.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 667 JVM tests (0 failures/errors, 1 skip), and both APKs
assembled. `ReimportIdentityIntegrationTest` passed 1/1 on the API-35 emulator,
covering import confirmation, failed identity replacement, and restore. This is
not Honor/TV acceptance; the physical phone remains keyguarded and HDMI is not
exposed.

### Shared configured-source controls (3 October 2026)

Extracted source-type semantics, icon selection, accessible action descriptions,
and button behavior into `ConfiguredDiscoverySourceButton`. Both the compact
discovery-source section and wide catalog now use that common control while
retaining their own layout density. This removes duplicated Vimm / JSON-feed /
external-web action behavior without changing the surrounding presentation.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed (667 JVM tests, 0 failures/errors, 1 skip; both APKs assembled;
lint reported 11 warnings). `ConfiguredDiscoverySourcesSectionTest` passed 3/3
on the API-35 emulator, covering source/result activation, Vimm controller
focus, and disabled actions while syncing. `git diff --check` passed. This is
not an actual-Honor/TV run; the hardware visual acceptance gate remains open.

### Home dashboard presentation boundary (3 October 2026)

Separated Home's platform and interaction coordinator (`HomeScreen`) from the
dashboard presentation (`HomeDashboardContent`). Default-Home intent handling,
controller quick actions, and storage/device state remain in the coordinator;
empty-state, hero, collection, recent-games, quick-launch, and status layouts
now live behind one presentation boundary. Home's coordinator is reduced from
371 lines to 115, making the state/effect versus rendering split explicit.
Quick-launch game-service labels now ellipsize instead of clipping at the
two-line limit, preserving readability on narrow panels.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 667 JVM tests (0 failures/errors, 1 skip), app and
instrumentation APKs assembled, and 11 lint warnings. `HomeQuickLaunchLayoutTest`
passed 1/1 on the API-35 emulator, including 200% text bounds and action clicks;
`git diff --check` passed. Actual Honor-on-TV appearance remains unverified.

### Discovery import execution consolidation (3 October 2026)

Single-file imports and multi-file disc-set imports in Discovery Details now
share one coroutine lifecycle for progress, cancellation messaging, exception
mapping, and cleanup. The existing single-file and multi-file operations retain
their distinct workflow calls and source-removal behavior; this consolidates
orchestration without changing those policies.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed (667 JVM tests, 0 failures/errors, 1 skip; both APKs assembled;
11 lint warnings). `BrowserDownloadTrackingActionTest` passed 3/3 on the API-35
emulator, covering tracking-action accessibility/state and the authorized
import action. `git diff --check` passed. This is not end-to-end Honor/TV import
verification; hardware acceptance remains open.

### Discovery details source-link policy extraction (3 October 2026)

Moved configured-source selection and URL resolution out of
`DiscoveryDetailsScreen` into `configuredDetailsSourceLinks`. The policy now
returns typed `ConfiguredSourceLink` values, keeps a direct source first,
filters disabled, unsupported, and platform-incompatible sources, omits
duplicate URLs, preserves Vimm browser-tracking metadata, and tolerates an
unresolvable configured URL by excluding it. Two JVM cases cover ordering,
filtering, URL deduplication, and Vimm fallback browse behavior.

Verification: latest offline `testDebugUnitTest assembleDebug
assembleDebugAndroidTest lintDebug` passed with 669 JVM tests (0
failures/errors, 1 skip), both APKs assembled, and 11 lint warnings.
`BrowserDownloadTrackingActionTest` passed 3/3 on the API-35 emulator;
`git diff --check` passed. This remains emulator-only evidence, not a physical
Honor/TV flow test.

### Shared Back / controller-B dismissal policy (3 October 2026)

Replaced the duplicated shell dismissal branches with `gameBoxBackTarget`, a
single priority policy used by both Compose `BackHandler` and the root
controller-B/back callback. The order remains quick actions, system status,
profile switcher, selected game Details, then unhandled. Added JVM coverage for
the full precedence chain.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 670 JVM tests (0 failures/errors, 1 skip), app and
instrumentation APKs assembled, and 11 lint warnings. The focused
`GameBoxRootInputModifierTest` passed 1/1 on the API-35 emulator; `git diff
--check` passed. Android-TV-specific manifest/source declarations were searched
and none remain; historical audit entries were retained as project history.
This emulator check does not replace physical Honor/TV controller validation.

### Controller footer clipping follow-up (3 October 2026)

Reviewed the saved Honor HDMI screenshot and found the profile/controller status
stack squeezed by the fixed 30dp footer height. The footer now has a 38dp
minimum height, the profile label is single-line at 12sp, and controller status
is single-line at 10sp with ellipsis rather than vertical clipping. Added a
Compose bounds test asserting the status stays within the footer's visible
semantics bounds.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 670 JVM tests (0 failures/errors, 1 skip), app and
instrumentation APKs assembled, and 11 lint warnings. `ControllerFooterLayoutTest`
passed 1/1 on the API-35 emulator; `git diff --check` passed. The screenshot
that motivated this change predates it; a fresh physical Honor HDMI capture is
still required to confirm projected layout.

### Quick Actions controller entry focus (3 October 2026)

The wide/living-room Quick Actions dialog now requests focus on its first
controller action after the dialog frame is attached. Compact phone layout does
not steal focus when opened by touch. `SettingsActionRow` accepts an optional
focus requester while retaining its existing click behavior and focus
animation.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 670 JVM tests (0 failures/errors, 1 skip), app and
instrumentation APKs assembled, and 11 lint warnings. `QuickActionsInitialFocusTest`
passed 1/1 on the API-35 emulator; `git diff --check` passed. Current read-only
ADB state still shows the Honor locked with no external HDMI display exposed,
so projected hardware focus remains unverified.

### Adaptive phone-host orientation (3 October 2026)

Removed the main handset Home activity's manifest-level landscape lock and
`resizeableActivity=false`. The normal host can now follow handset orientation
and Android's large-screen windowing; `ExternalDisplayActivity` still explicitly
requests landscape for TV output and retains its fullscreen/borderless policy.
The manifest test asserts that MainActivity reports
`SCREEN_ORIENTATION_UNSPECIFIED` while the existing tests continue to verify
Home registration and single-task reuse.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 670 JVM tests (0 failures/errors, 1 skip), app and
instrumentation APKs assembled, and the 3-case `DefaultHomeRegistrationTest`
passed on API-35. Lint decreased from 11 to 8 warnings. `git diff --check`
passed. The external-display orientation remains explicit and lint still
reports its Android-16 orientation warning intentionally; real Honor portrait
and HDMI behavior require a fresh unlocked hardware run.

### Catalog UI component boundaries (3 October 2026)

Extracted the Store featured-game hero from `BlueprintCatalogScreen` into
`CatalogFeaturedHero`, keeping its artwork fallback, text limits, disabled empty
state, and discovery/installed details routing together. Its focused API-35
Compose tests passed 2/2. Consolidated Vimm, JSON-feed, and external-web source
buttons into `ConfiguredDiscoverySourceButton` for reuse by compact and wide
catalog presentations; existing compact-catalog instrumentation tests cover
accessible labels, controller focus, disabled syncing state, and activation.

The post-consolidation full Gradle gate is not yet verified: Gradle could not
initialize native services because it failed to load `native-platform.dll`.
`git diff --check` passed. This emulator evidence does not verify the docked
Honor/TV presentation, which remains blocked by the phone keyguard and absent
HDMI display exposure in the latest device check.

### Discovery import panel extraction (3 October 2026)

Moved the discovery-detail copy-import guidance, accepted-format summary,
progress/cancel controls, live status announcement, and multi-disc-set action
into `DiscoveryImportSection`. The details screen now delegates this cohesive
import UI instead of owning its presentation inline. Added Compose coverage for
supported-format and disc-set routing, and for the integrated progress/cancel
affordance. The shared cancel control's click callback remains covered by its
existing direct interaction test.

Verification: `GameImportProgressIndicatorTest` passed 5/5 on the API-35
emulator after removing a redundant nested click assertion from the new
integration test (the shared cancel action already has a direct click test).
`git diff --check` passed. This does not replace an end-to-end import on the
unlocked Honor phone projected to the TV; that hardware check is still open.

### Library summary presentation boundary (3 October 2026)

Extracted the compact collection summary card from `LibraryScreens.kt` into
`LibrarySummaryCard`, making the counts, accessible aggregate label, playtime,
and resume-game highlight a focused presentation component. The screen file is
35 lines shorter; filters, controller actions, and game selection remain in the
coordinator. Added a Compose test covering the summary semantics and resume
selection.

Verification: offline `testDebugUnitTest assembleDebug lintDebug
connectedDebugAndroidTest` passed with 671 JVM tests (0 failures/errors, 1
skipped), the focused API-35 `LibrarySummaryCardTest` passed 1/1, and lint
reported 8 warnings. `git diff --check` passed. Physical Honor/TV layout
remains unverified until the phone is unlocked and the projected display is
available.

### Game details tools presentation boundary (3 October 2026)

Extracted the settings disclosure, metadata-edit entry point, and reduced-motion
aware settings-panel visibility into `GameDetailsToolsSection`. This keeps the
details coordinator responsible for state and callbacks while the component
owns the paired tools UI and animation policy. The details screen is 22 lines
shorter, with the button labels and 16dp icon sizing preserved. Added Compose
coverage for the reduced-motion expansion path and metadata action semantics.

Verification: offline `testDebugUnitTest assembleDebug lintDebug
connectedDebugAndroidTest` passed with 671 JVM tests (0 failures/errors, 1
skipped); focused API-35 `GameDetailsToolsSectionTest` passed 1/1; lint reported
8 warnings; and `git diff --check` passed. Physical Honor HDMI input/focus
acceptance remains outstanding.

### Shared Basic-auth header construction (3 October 2026)

Centralized Basic authorization header creation for catalog and cloud-save
transports in `basicAuthorizationHeader`. Both clients now share UTF-8 encoding
and omit authorization for incomplete credentials; removed their duplicated
Base64 formatting and forced non-null assertions. Added unit coverage for a
non-ASCII password and missing/blank credential cases.

Verification: offline `testDebugUnitTest assembleDebug lintDebug` passed with
672 JVM tests (0 failures/errors, 1 skipped), and lint reported 8 warnings.
`git diff --check` passed. Catalog and cloud-save transport regression tests
are included in the JVM suite.

### Shared configured-source result rails (3 October 2026)

Consolidated compact and wide Store layouts on `ConfiguredDiscoveryResultsRail`
for the result count, source-result card mapping, and activation callback. Kept
the compact 180×130dp and wide 156×132dp card sizes and existing typography.
The compact configured-source component's duplicate rail markup is removed.

Verification: offline `testDebugUnitTest assembleDebug lintDebug` passed with
672 JVM tests (0 failures/errors, 1 skipped); all 3
`ConfiguredDiscoverySourcesSectionTest` cases passed on the API-35 emulator;
lint returned to 8 warnings after aligning the composable Modifier parameter;
and `git diff --check` passed. The rail test covers accessible result-card
activation; docked hardware focus still requires physical verification.

### Atomic configured-source updates (3 October 2026)

Changed `upsertGameSource` and `removeGameSource` to decode, transform, and
persist the source list within a single DataStore edit transaction instead of
reading a snapshot and writing it in a later transaction. This prevents
overlapping updates from silently replacing one another. Added an API-35
instrumentation regression that concurrently upserts two sources and restores
the pre-test source list in `finally`.

Verification: offline `testDebugUnitTest assembleDebug lintDebug
connectedDebugAndroidTest` passed with 672 JVM tests (0 failures/errors, 1
skipped); `SettingsRepositoryConcurrencyTest` passed 1/1 on API 35; lint
reported 8 warnings; and `git diff --check` passed.

### Store console and filter rail extraction (3 October 2026)

Moved the wide Store's console list, favorites/metadata filters, sort and clear
actions, and offline catalog summary into `CatalogConsoleFilterRail`. The
screen coordinator still owns selected/filter state and supplies callbacks, so
the extracted view does not introduce duplicate state. Preserved initial rail
focus and DPAD-right handoff to the search/content column. Added an API-35
Compose interaction test for console selection, scrolling to Favorites, and
clear-filter dispatch.

Verification: offline `testDebugUnitTest assembleDebug lintDebug` passed with
672 JVM tests (0 failures/errors, 1 skipped); `CatalogConsoleFilterRailTest`
passed 1/1 on API 35, including scroll-then-activate behavior; lint reported 8
warnings; and `git diff --check` passed. Real controller behavior on the
docked Honor/TV remains unverified.

### Shared focus-scroll and scroll-restoration helpers (3 October 2026)

Moved `Modifier.bringFocusedControlIntoView` and `restoredScrollState` out of
the app shell into `UiFocusAndScroll.kt`. These shared behaviors are used
across the Home, Library, Store, Downloads, Details, Settings, and App Hub
surfaces; `GameBoxApp` no longer owns this reusable UI infrastructure. The
helper implementations preserve the existing focus-triggered bring-into-view
request and per-screen saved scroll key.

Verification: offline `testDebugUnitTest assembleDebug lintDebug
connectedDebugAndroidTest` passed with 672 JVM tests (0 failures/errors, 1
skipped); `FocusedControlScrollTest` and `ScrollPositionRestorationTest` both
passed on API 35; lint reported 8 warnings; and `git diff --check` passed.

### Fail-safe external-content and save-sync null handling (3 October 2026)

Removed forced non-null access from the RetroArch external-library publishing
path by capturing the configured content store once and explicitly returning a
handoff/content failure if it is unavailable. Simplified the save-sync
remote-only branch to preserve its `UpToDate`/`Downloaded` outcomes without a
non-null assertion. These retain the existing result contract while making
failure handling explicit.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 672 JVM tests (0 failures/errors, 1 skipped), both APKs
were assembled, lint reported 8 warnings, and `git diff --check` passed.

### API 33 back-navigation lint scope (3 October 2026)

Annotated both activities' `enableOnBackInvokedCallback` manifest attributes
with `tools:targetApi="tiramisu"`, documenting that these intentionally use an
API-33-only attribute while the app continues to support API 26+. Runtime
behavior and minSdk are unchanged; the two misleading `UnusedAttribute`
warnings are removed.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug connectedDebugAndroidTest` passed with 672 JVM tests (0
failures/errors, 1 skipped), both APKs assembled, and
`DefaultHomeRegistrationTest` passed 3/3 on API 35. Lint decreased from 8 to 6
warnings; the remaining fixed-landscape warning is for the intentional
external-display activity. `git diff --check` passed.

### Store rail state/action API consolidation (3 October 2026)

Grouped `CatalogConsoleFilterRail`'s displayed values into
`CatalogConsoleFilterState` and callbacks into `CatalogConsoleFilterActions`.
The composable now exposes a smaller cohesive API instead of a long list of
independent parameters, without moving filter ownership out of the Store
coordinator. Updated the rail interaction test to use the same state/action
contract.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug connectedDebugAndroidTest` passed with 672 JVM tests (0
failures/errors, 1 skipped); `CatalogConsoleFilterRailTest` passed 1/1 on API
35; lint remained at 6 warnings; and `git diff --check` passed.

### Catalog empty-state action focus consolidation (3 October 2026)

Extracted the duplicated Sync/Refresh initial-focus modifier into
`claimInitialCatalogActionFocus`. The selected action now receives an explicit
focus target before its first layout focus request, and the shared claim state
prevents the two controls from competing. Added tests for enabled and disabled
initial-action focus behavior.

Verification: `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`
succeeded; `CatalogActionFocusTest` passed 2/2 on the API 35 emulator; JVM tests
passed (672 tests, 0 failures/errors, 1 skipped); lint reported 0 errors and 6
existing warnings; and `git diff --check` passed. Instrumentation on the
connected Honor reported no Compose hierarchy, so handset/TV behavior remains
unverified.

### Settings system shortcuts section extraction (3 October 2026)

Moved the console-startup guidance and four Android system shortcuts out of
`SettingsScreen` into `SettingsSystemSection`, leaving the screen coordinator
responsible for mapping section actions to Android settings intents. The
section retains the same Blueprint action-row components and copy. Added a
Compose interaction test verifying all four callbacks and startup guidance.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 672 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint reported 0 errors/6 warnings.
`SettingsSystemSectionTest` passed 1/1 and `CatalogActionFocusTest` passed 2/2
on the API 35 emulator. Honor instrumentation still cannot attach a Compose
hierarchy; actual docked-TV behavior remains unverified. `git diff --check`
passed.

### Launcher backdrop component extraction (3 October 2026)

Moved the root dark gradient and wide-layout ambient glows out of
`GameBoxApp` into `BlueprintBackdrop`. Cached the static gradient brushes and
preserved the existing palette, glow positions, and compact-mode suppression,
separating presentation decoration from root navigation and input handling.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded (672 JVM tests, 0 failures/errors, 1 skipped; lint 0
errors/6 warnings). The production `BlueprintScreenshotsTest.captureRealScreens`
flow passed 1/1 on the API 35 emulator and captured Home, Details, Library,
Store, Media, PC, Settings, and Downloads. Home and Settings captures were
visually inspected. This remains emulator evidence, not wired-TV verification.
`git diff --check` passed.

### Game-session save backup wiring extraction (3 October 2026)

Moved automatic managed-save and emulator-save backup/restore construction out
of `DefaultAppContainer` into `GameSaveSessionBackups`. The container now
provides the authorized SAF root and delegates session callbacks, while the
component shares the emulator `SaveBackupService` and manifest store between
backup and restore instead of constructing duplicate instances for identical
paths. Existing storage policy and on-disk locations are unchanged.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 672 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint reported 0 errors/6 warnings. The production
`BlueprintScreenshotsTest.captureRealScreens` startup/navigation flow passed
1/1 on the API 35 emulator. Actual phone/TV save-session behavior remains
unverified while the Honor is locked and no wired display is enumerated.
`git diff --check` passed.

### Import registration recovery separation (3 October 2026)

Separated startup reconciliation policy into `ImportRegistrationRecovery`;
`ImportRegistrationJournal` now stays responsible for durable marker reads and
writes, registered-byte comparison, confirmation, and rollback. The recovery
coordinator retains the same cutoff handling, per-game conflict fail-closed
behavior, and Room/filesystem consistency checks. Updated the journal recovery
suite to exercise the new boundary.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 672 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint reported 0 errors/6 warnings. The production
`BlueprintScreenshotsTest.captureRealScreens` app-start/navigation flow passed
1/1 on the API 35 emulator. Honor/TV recovery behavior remains unverified while
the device is locked and no wired display is present. `git diff --check` passed.

### Unused Compose Preview dependency removal (3 October 2026)

Removed the app's direct Compose Tooling Preview dependency after confirming
there are no `@Preview` functions or preview-package imports in source or tests.
An initial attempt to remove Lifecycle Runtime KTX as well exposed its current
role as a direct 2.10.0 version pin over Compose's 2.9.4 transitive artifacts;
the older artifacts are not cached for offline builds, so the Lifecycle pin is
retained intentionally.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` passed with 672 JVM tests (0 failures/errors, 1 skipped), both APK
variants assembled, and lint reported 0 errors/6 warnings. `git diff --check`
passed.

### Home quick-launch tile height alignment (3 October 2026)

The populated Home capture showed the Epic Games quick-launch tile taller than
its neighbors because only its title wrapped. Reserved a two-line title slot
for every quick-launch card and strengthened `HomeQuickLaunchLayoutTest` to
assert equal tile heights at enlarged text while retaining its clipping and
action checks.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded (672 JVM tests, 0 failures/errors, 1 skipped; lint 0
errors/6 warnings). `HomeQuickLaunchLayoutTest` passed 1/1 and the production
`BlueprintScreenshotsTest.captureRealScreens` flow passed 1/1 on the API 35
emulator. The populated Home capture was visually inspected; all four
quick-launch tiles now share a baseline and height. Honor/TV remains unavailable
for direct validation. `git diff --check` passed.

### Catalog provider graph extraction (3 October 2026)

Moved catalog transport selection, configured/offline fallback, metadata
enrichment, and TheGamesDB discovery wiring from `DefaultAppContainer` into
`CatalogRuntime`. The app container now exposes the catalog runtime's provider
and discovery components; the Room discovery DAO is resolved once and shared
across the sync/repository pair. Provider ordering, credentials, and fallback
behavior are unchanged.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 672 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint reported 0 errors/6 warnings.
`CatalogRefreshStateTest` passed 1/1 on the API 35 emulator, exercising
fallback and error presentation through the production container.
`git diff --check` passed; actual Honor/TV validation remains unavailable.

### Room database runtime extraction (3 October 2026)

Moved database startup from `DefaultAppContainer` into
`GameBoxDatabaseRuntime`. The runtime preserves the critical ordering—create
the pre-migration backup before building Room—and owns the complete migration
list with the database builder. The container now obtains its database from
that single infrastructure boundary.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 672 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint reported 0 errors/6 warnings. The production
`BlueprintScreenshotsTest.captureRealScreens` startup/navigation flow passed
1/1 on the API 35 emulator. `git diff --check` passed; wired-Honor validation
remains outstanding.

### Console-style route transition policy (3 October 2026)

Extracted route motion into `BlueprintScreenTransition`: primary tabs use a
horizontal entrance in navigation order, including the Settings/Home wrap;
game details and Downloads use vertical stack motion; unchanged routes do not
translate; and reduced-motion mode bypasses the animated container. Added pure
policy tests for forward/backward tabs, wraparound, detail open/close, Downloads,
and unchanged routes.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 675 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint reported 0 errors/6 warnings. The Honor is
ADB-connected, but its keyguard is showing; Android currently exposes only the
built-in display and Android Studio's virtual screen-sharing display, not the
TV. Per the hardware-first requirement, no phone-screen or emulator screenshot
is being presented as TV validation. `git diff --check` passed.

### Library filter state boundary (3 October 2026)

Simplified `BlueprintLibraryScreen` by passing the existing stable
`RememberedGameFilters` state holder instead of unpacking each filter into a
separate value/setter pair. The library rail, search field, metadata filters,
and controller search dialog now update the shared screen state directly; the
filter behavior and saved-state ownership are unchanged.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 675 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint reported 0 errors/6 warnings.
`git diff --check` passed. No on-device or TV UI run was possible: the connected
Honor remained locked and Android exposed no physical external display.

### Settings repository policy extraction (3 October 2026)

Moved platform emulator-default key normalization/encoding and Moonlight host
and port validation out of `SettingsRepository` into focused pure policy files.
The DataStore repository now retains persistence orchestration while the
existing preference mapper and Moonlight setup continue to use the same
serialized format and validation rules. Added direct normalization, stable
ordering, round-trip, and malformed-entry coverage for emulator defaults.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 677 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint reported 0 errors/6 warnings.
`git diff --check` passed. Hardware display validation remains outstanding while
the Honor is locked and no TV display is exposed to Android.

### Launch return feedback policy extraction (3 October 2026)

Moved the mapping from recovered launch/save-backup outcomes to player-facing
return messages out of `DefaultGameLaunchController` into
`LaunchReturnMessage`. The controller now coordinates journal recovery and
save-backup work; the policy owns the distinct messaging for interrupted
handoffs, exceptions, absent saves, partial backups, and successful backups.
Added direct tests for each outcome while preserving existing wording.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 681 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint reported 0 errors/6 warnings.
`git diff --check` passed. Honor-to-TV visual validation remains outstanding:
the phone is locked and only its built-in and Studio virtual displays are
currently visible to Android.

### Shared controller activation primitive (3 October 2026)

Centralized A/DPAD-center/Enter key-down activation and pointer-click wiring in
`blueprintActivation`. Shared actions, game cards, and primary navigation now
use the same activation behavior; cards and tabs retain their custom no-ripple
focus visuals. Added policy tests distinguishing confirm keys from navigation
and Back.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 683 JVM tests (0 failures/errors, 1 skipped), both APK
variants assembled, and lint reported 0 errors/6 warnings. The production
`BlueprintNavigationTest` passed 1/1 on the API 35 emulator. Honor is still
locked and Android currently lists no physical TV display, so that emulator
result is not counted as hardware validation. `git diff --check` passed.

### Shared console UI palette tokens (3 October 2026)

Added semantic GameBox theme tokens for the shared focus gradient, idle game
card outline, and controller A/B hint colors. Library cards, discovery cards,
app-hub hints, quick-action hints, and common focus surfaces now use the same
theme values instead of repeating hex literals; service and console brand colors
remain separate so their identities are preserved.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 687 JVM tests (0 failures/errors, 1 skipped), both APK
variants assembled, and lint reported 0 errors/6 warnings. `git diff --check`
passed. Honor-to-TV visual review remains pending while the phone is locked and
Android does not expose the TV display.

### Controller activation consistency pass (3 October 2026)

Extended `blueprintActivation` / `onBlueprintActivation` to compatible Material
buttons and chips in Downloads and Display settings, removing the last duplicated
A/DPAD-center/Enter handlers in those screens. Existing Material click targets
remain intact (no nested click semantics); navigation and game cards preserve
their no-ripple focus treatment. The controller-search keyboard intentionally
retains its A-only handler because its key contract differs.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 683 JVM tests (0 failures/errors, 1 skipped), both APK
variants assembled, and lint reported 0 errors/6 warnings. The production
`BlueprintNavigationTest` passed 1/1 on the API 35 emulator. The Honor remains
locked with no physical TV display enumerated, so direct hardware behavior is
still unverified. `git diff --check` passed.

### Store wide-screen route contract (3 October 2026)

Replaced `BlueprintCatalogScreen`'s large flat parameter list with three
explicit bundles: catalog/filter presentation state, user actions, and focus
requesters. The remembered filter state remains the single source for search,
favorites, metadata filters, and persisted Store selections. Compact and wide
Store routes now follow the same state/actions/focus boundary pattern.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 683 JVM tests (0 failures/errors, 1 skipped), both APK
variants assembled, and lint reported 0 errors/6 warnings. Production
`CatalogRefreshStateTest` passed 1/1 on the API 35 emulator, confirming the
Store route still presents actionable fallback/error states. Honor/TV remains
unavailable for direct verification while the phone is locked and no TV
display is exposed. `git diff --check` passed.

### Import transaction workspace boundary (3 October 2026)

Moved canonical imports-root, console-scoped target, staging, backup, and
expected-relative-prefix construction from `AuthorizedRomImporter.importSet`
into `ImportTransactionWorkspace`. The helper validates the game id and
canonical containment; workspace preparation creates the parent and staging
directories before file staging begins. Added tests for the canonical PSP tree,
temporary-path containment, directory preparation, and invalid ids.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 687 JVM tests (0 failures/errors, 1 skipped), both APK
variants assembled, and lint reported 0 errors/6 warnings. The Home dashboard
focus instrumentation test also passed 1/1 on the API 35 emulator. Honor-to-TV
verification remains unavailable while the phone is locked and no TV display
is exposed. `git diff --check` passed.

### Layout-aware Home focus restoration (3 October 2026)

Home previously restored the remembered game whenever it remained in the
catalog, even if the wide dashboard did not render it (for example, an
unplayed installed title after the first eight cards while a nonempty Recent
row was visible). Home now chooses a restore target only among cards rendered
by the active compact/wide layout, falling back to the hero when the remembered
card is not present. Added tests for wide hidden-card fallback and compact
full-collection restoration.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 685 JVM tests (0 failures/errors, 1 skipped), both APK
variants assembled, and lint reported 0 errors/6 warnings. `git diff --check`
passed. Direct Honor/TV confirmation remains outstanding while the device is
locked and no physical external display is exposed.

Follow-up UI verification: added `HomeDashboardFocusTest` to assert that the
production dashboard composable focuses the visible hero and omits the
remembered-but-hidden card in the wide layout. Instrumentation APK compilation
and the test passed 1/1 on the API 35 emulator; this is not counted as
Honor/TV validation.

### Import transaction rule deduplication (3 October 2026)

Unified the 64-file disc-set limit used by transaction validation and archive
extraction, and removed duplicate game-id validation from `AuthorizedRomImporter`
so `ImportTransactionWorkspace` is the single path/id authority. Workspace
validation failures still return a user-actionable rejected import result.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 687 JVM tests (0 failures/errors, 1 skipped), both APK
variants assembled, and lint reported 0 errors/6 warnings. The real importer
cancellation/staging-cleanup instrumentation test passed 1/1 on the API 35
emulator. Honor is locked and no TV display is exposed, so hardware validation
remains outstanding. `git diff --check` passed.

### Shared controller activation on discovery and app-hub tiles (3 October 2026)

Moved Store discovery cards, configured-source result cards, and the two app-hub
shortcut tile styles onto the shared `blueprintActivation` path. These tiles
now use the same controller A / DPAD-center / Enter activation handling and UI
click sound as other launcher actions while keeping their existing no-ripple
visual treatment. Added `DiscoveryGameCardActivationTest` for gamepad-A
activation and explicitly waits for Compose idleness before querying semantics.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded (687 JVM tests: 0 failures/errors, 1 skipped; both APKs
assembled; lint 0 errors/6 warnings). Gradle's connected test passed the new
activation test 1/1 on the API-35 emulator. On Honor PGT-N19, both the new test
and an existing Compose UI test failed because the test activity exposed no
Compose semantics hierarchy; the production source and test APK compile. The
current display snapshot reports only the built-in screen and the remembered
SMART TV as unavailable (`activeDisplay=null`), so this does not constitute
TV-output validation. `git diff --check` passed.

### Game-details controller activation (3 October 2026)

Added shared gamepad-A / DPAD-center / Enter activation and UI click-sound
handling to the Game Details Play and Install hero actions. Applied the same
activation behavior to compact-layout secondary details actions, which had
previously depended on Material's default key handling. Added a regression
test that focuses Play Now and sends a gamepad-A key event, asserting exactly
one launch callback.

Verification: the full offline `testDebugUnitTest assembleDebug
assembleDebugAndroidTest lintDebug` gate succeeded (687 JVM tests: 0 failures,
0 errors, 1 skipped; both APKs assembled; lint 0 errors/6 warnings). Gradle's
connected run passed all 3 `GameDetailsHeroTest` cases on the API-35 emulator,
including the A-button action test. All 3 cases failed on Honor PGT-N19 before
reaching assertions because that device's Compose test activity exposed no
semantics hierarchy; this matches other Compose test failures on that device.
The TV remains unavailable in the display snapshot recorded above.

### Header quick-action controller activation (3 October 2026)

Added the shared controller activation and preference-aware click feedback to
the wide Home header's Search and Downloads icon buttons. This keeps the
primary browse/search and download-activity entry points usable without a
touchscreen. Added a navigation instrumentation case that requests focus on
Search, sends gamepad A, and verifies selection changes to Store.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 687 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint 0 errors/6 warnings. `BlueprintNavigationTest`
passed 2/2 on the API-35 emulator. Both tests failed on Honor PGT-N19 before
assertions because its Compose test host exposed no semantics hierarchy; the
same device-side failure affects the pre-existing navigation test as well.
No physical TV output was exposed during this run. `git diff --check` passed.

### Download queue controller activation (3 October 2026)

Unified controller-A / DPAD-center / Enter activation and click feedback for
download Pause, Resume, Retry, Cancel, and unavailable-source dismissal. Removed
an extra `focusable()` modifier from the primary action button wrapper so the
Material button remains the single focus target. Added a test that focuses a
paused transfer's Resume action and verifies gamepad A invokes resume exactly
once.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded (687 JVM tests: 0 failures/errors, 1 skipped; both APKs
assembled; lint 0 errors/6 warnings). The new resume test passed 1/1 on the
API-35 emulator. On Honor PGT-N19 it failed before the assertion because the
Compose test activity again exposed no semantics hierarchy. `git diff --check`
passed.

### Import-flow controller actions (3 October 2026)

Added consistent controller activation and click feedback to the multi-file
disc-set picker action and the import-progress Cancel action. These complete
the controller path across single-file import, multi-disc import, progress,
and cancellation. Added a gamepad-A regression test for opening the multi-file
disc-set picker.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded with 687 JVM tests (0 failures/errors, 1 skipped), both
APK variants assembled, and lint 0 errors/6 warnings. The full six-test
`GameImportProgressIndicatorTest` suite passed on the API-35 emulator, including
the new controller case. All six failed on Honor PGT-N19 before assertions due
to the repeated missing Compose semantics hierarchy. `git diff --check` passed.

### Discovery detail hero action boundary (3 October 2026)

Extracted the discovery-detail Import / Favorite / Back cluster into
`DiscoveryDetailsHeroActions`, retaining the compact full-width import button
while removing duplicated inline layout and applying the shared controller
activation and preference-aware click sound to each action. The import callback
is also guarded against activation while an import is already in progress.
Added a focused test that sends gamepad A to Import your copy and asserts one
picker launch.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded (687 JVM tests: 0 failures/errors, 1 skipped; both APKs
assembled; lint 0 errors/6 warnings). The new test passed 1/1 on the API-35
emulator. The Honor run failed before assertions due to its missing Compose
semantics hierarchy, consistent with the other UI suites. `git diff --check`
passed.

### Discovery external-link action consolidation (3 October 2026)

Extracted duplicated legal/configured web-source buttons into
`DiscoveryExternalLinkButton` and centralized the no-browser fallback message.
The shared control uses the same focus-safe A/DPAD-center/Enter activation and
sound path as launcher actions. Updated configured-source buttons to use that
activation path too and removed an extra `focusable()` that duplicated the
Material button's focus target; disabled source actions remain guarded.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded (687 JVM tests: 0 failures/errors, 1 skipped; both APKs
assembled; lint 0 errors/6 warnings). All four configured-source UI tests
passed on the API-35 emulator, including gamepad A for Vimm search and a legal
external link. Honor PGT-N19 again failed before assertions because no Compose
semantics hierarchy was exposed. `git diff --check` passed.

### Library and Store filter-button controller integration (3 October 2026)

Migrated Library Clear filters, Sort, and empty-library Browse Store controls,
plus Store filter-rail Sort and Clear filters, to the shared Blueprint button
wrappers. Extended the filter-rail test to focus Sort, send gamepad A, and
assert that the sort action is called exactly once.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded (687 JVM tests: 0 failures/errors, 1 skipped; both APKs
assembled; lint 0 errors/6 warnings). The Store filter-rail instrumentation
test passed 1/1 on the API-35 emulator. Honor PGT-N19 failed before the UI
assertion because no Compose semantics hierarchy was exposed. `git diff --check`
passed.

### Shared Blueprint action buttons on game lifecycle screens (3 October 2026)

Introduced filled, outlined, and text `Blueprint` button wrappers that centralize
preference-aware click sound, controller A / DPAD-center / Enter activation,
and a guard against invoking disabled actions through key events. Adopted the
wrappers in Game Details Install/Play/Pause/Favorite/Back/save/preparation
actions, download Pause/Resume/Retry/Cancel/dismiss actions, discovery hero
Import/Favorite/Back actions, and Game Details hero Install/Play. This removes
repeated key and sound wiring while keeping Material styles intact.

Verification: offline `testDebugUnitTest assembleDebug assembleDebugAndroidTest
lintDebug` succeeded (687 JVM tests: 0 failures/errors, 1 skipped; both APKs
assembled; lint 0 errors/6 warnings). All four `AuthorizedInstallFeedbackTest`
cases passed on the API-35 emulator, including gamepad-A Install activation.
Honor PGT-N19 failed the four tests before assertions because no Compose
semantics hierarchy was exposed. `git diff --check` passed.

### Honor HDMI UI pass and instrumentation safety (3 October 2026)

Captured the live 1920×1080 HDMI surface from the Honor PGT-N19. GameBox renders
the wide Home dashboard edge-to-edge with no app title bar, and Android accepted
the external-display fullscreen request. Honor's desktop shelf remains OS-owned;
the 44 dp bottom clearance is retained because removing it placed the controller
footer underneath that shelf. After a fresh launch, the bundled catalog
repopulated asynchronously; the earlier Welcome/empty screen was transient.

Safety finding: `connectedDebugAndroidTest` crashed on this Honor and the Gradle
test cleanup left `com.gamebox.os` uninstalled. ROM imports are stored below
`Context.filesDir/imports`, so uninstalling the production package can remove
app-private imports and its Room index. The current debug app has been
reinstalled. Do not run connected instrumentation against the personal Honor
installation again until tests use an isolated application ID or a backed-up
test target. Continue manual UI checks on HDMI; run Compose instrumentation on
an emulator or isolated test package. `assembleDebug` and
`compileDebugAndroidTestKotlin` passed; the Honor instrumentation result is
unverified because the test process crashed. A 1.08 GB personal 7z source still
exists under `D:\GameBoxOS\games`; no game was copied or reimported during this
recovery check.

### Storage uninstall-safety guidance (3 October 2026)

Added a visible warning to Settings → Storage explaining that imported games use
app storage by default and that uninstalling GameBox removes that local storage.
The guidance points users to the existing shared-library migration action.
Added a storage-screen regression assertion. `assembleDebug`,
`compileDebugAndroidTestKotlin`, and `lintDebug` passed offline (lint 0 errors,
6 existing warnings). The Honor is not used for instrumentation while its test
cleanup targets the production package.

### Shared settings actions and refreshed Honor HDMI verification (3 October 2026)

Extracted `runSettingsUiAction` for consistent busy cleanup, cancellation
propagation, and visible error reporting, with JVM coverage for failure,
fallback, and cancellation cases. Catalog, cloud-save, TheGamesDB, companion,
and discovery-source settings now use it for asynchronous actions. Discovery
source creation now clears the form only after repository persistence succeeds;
probe cancellation is no longer swallowed.

Verification: the local JVM suite (690 tests), debug APK, Android test-source
compile, and lint passed offline. Updated the existing Honor installation with
`adb install -r` (app data preserved) and used GameBox's PC Hub “Show on TV”
action to present the app on the external display. Actual HDMI captures
verified the wide Home and Settings screens at 1920×1080, plus inline red
validation for saving Cloud Sync with an empty endpoint; this validation did not
save settings or contact a service. Returned the TV to Home afterward. The app
reports no connected controller; a synthetic DPAD event did not visibly move
focus, so physical controller navigation remains unverified. The Home snapshot
shows 0 installed games and 8 discoverable bundled titles. Honor's desktop
shelf remains OS-owned at the bottom of the TV output.

### Honor TV Vimm handoff and RetroArch DS launch (3 October 2026)

On the Honor-to-TV setup, the imported Nintendo DS title initially exited from
RetroArch with `System.exit(0)` rather than crashing GameBox. The DS core was
missing. Installed the maintained `Nintendo - DS (melonDS DS)` core through
RetroArch's own Core Downloader and changed GameBox's NDS launch profile to
`melondsds_libretro_android.so`. After quitting a previously running RetroArch
session through its menu, a cold GameBox launch reached the DS boot/title screen
and the process remained alive. Re-launch into an already-running RetroArch
process still needs a separate compatibility check; GameBox must not force-stop
an active emulator session because that could lose unsaved progress.

The old Vimm tracking action opened Chrome and guessed at another app's
DownloadManager entries. Replaced it with an app-owned WebView browser that
listens for a user-initiated, HTTPS download, accepts only the selected
console's supported filename formats, queues an app-owned DownloadManager
request, returns to the GameBox title, displays transfer progress, and invokes
the existing verified import/source-cleanup workflow. External-browser links
and manual file import remain available. The browser now requests fullscreen
on Honor's external display and supports controller B navigation.

Verified on the actual TV: Vimm metadata search, details navigation, the
embedded Vimm page in a full-screen GameBox activity, and Back to GameBox.
Did not activate Vimm's copyrighted Download button. The actual callback,
download completion, and automatic import need a redistributable homebrew
fixture on the same hardware. The Vimm details transport returned HTTP 404 for
one searched listing even though WebView displayed the source page; stale
details URL/transport compatibility remains open. Honor's desktop shelf remains
visible because it is OS-owned.

### Fresh cross-platform regression checkpoint (6 October 2026)

Forced `testDebugUnitTest --rerun-tasks`: 212 suites, 779 tests, 0 failures,
0 errors, 1 skipped. `compileDebugAndroidTestKotlin` and `assembleDebug` also
passed. The Windows companion console test harness passed on the development
PC via `dotnet run --project windows/GameBox.Windows.Core.Tests`.

On the currently connected Honor PGT-N19, Android reports an enabled Bluetooth
`Wireless Controller` gamepad and the physical HDMI display is 1920×1080/60.
GameBox's TV Download Manager still reports Galaxy Patrol completed at
48.0 KB/48.0 KB. A follow-up physical D-pad press produced `ABS_HAT0X`
transitions on `/dev/input/event13`; the HDMI capture shows focus on PC while
Home remains selected. The subsequent A-button prompt/capture did not provide
a clearly correlated button event or screen activation (the available stream
excerpt showed analog-axis motion), so physical A activation remains
unverified here. Installed emulators remain PPSSPP, RetroArch, Dolphin, and
Azahar; missing standalone console emulator coverage and a representative
real gameplay/save round trip remain open. No game was launched and no
personal game/save data was changed. Evidence:
`docs/evidence/hardware-run-2026-10-06/honor-tv-physical-dpad-result.png`.

### Explicit-emulator readiness for diagnostic catalog entries (6 October 2026)

TV review found that Galaxy Patrol has an explicit RetroArch launch capability
but the generic `Retro` catalog platform, so `GameSettingsPanel` showed no
approved emulator choices. The registry now includes a title's explicit
capability in its options, and the same RetroArch package-candidate helper is
used by readiness detection and actual launch resolution. A regression test
checks the generic-platform fixture and the standard-package fallback.
The regression fixture was aligned to the catalog's exact `Retro` platform;
the targeted `EmulatorSettingsTest` class passed all 17 tests afterward.

Targeted emulator settings tests, Android-test Kotlin compilation, and APK
assembly passed; the full Android JVM suite now reports 780 tests, 0 failures,
0 errors, 1 skipped. Updated the existing Honor install in place and confirmed
the TV Game Settings panel now exposes RetroArch and reports a compatible
installed package. The app returned to the HONOR desktop after install; its
per-app fullscreen mode was restored and the bottom Dock is hidden again.
Evidence: `docs/evidence/hardware-run-2026-10-06/honor-tv-galaxy-emulator-settings.png`
and `honor-tv-readiness-fullscreen-settled.png`. This does not verify the
FCEUmm core or prove gameplay. ADB could not read RetroArch's private core
directory, and no launch was attempted.

The subsequent `lintDebug` run initially found a direct `FocusRequester()`
construction inside `GameDetailsHeroTest` composition. Reused the test's
existing `remember` pattern; `lintDebug` and `compileDebugAndroidTestKotlin`
then both passed. Lint still reports 20 non-fatal warnings across the project.

### Honor HDMI emulator launch follow-up (6 October 2026)

Launched the bundled legal Galaxy Patrol fixture through GameBox on the
physical 1920×1080 HDMI output. Android started `RetroActivityFuture`, but
HONOR placed RetroArch in a `hw-pc-freeform` window at `[192,103]–[1344,794]`
over GameBox and kept its Dock visible; the launch options alone do not defeat
the OEM desktop policy. Used the RetroArch taskbar menu's **Open in full screen
mode** action. Once settled, the game filled the TV, Dock/touch controls were
not visible, and its ship/stars rendered. This is a per-app HONOR setting, so
other emulators still need the same hardware setup. Evidence:
`docs/evidence/hardware-run-2026-10-06/honor-tv-retroarch-fullscreen-settled.png`.

One standard Back key left RetroArch foreground. Its HONOR Dock menu's normal
Quit action returned to the existing GameBox MainActivity without a
force-stop; controller/Back-based exit remains unverified. The controller
later showed disconnected and raw input monitoring captured no physical button
press, so gameplay input is not verified. No personal game or save data was
touched. Exit evidence:
`docs/evidence/hardware-run-2026-10-06/honor-tv-after-quit.png`.

Updated the HONOR Dock guidance in Settings → Display to describe per-emulator
fullscreen mode and the optional global taskbar path. The targeted
`ExternalDisplayTaskbarGuidanceTest` and `assembleDebug` passed; installed with
`adb install -r` preserving app data and visually confirmed the new guidance
on the real TV. The reinstall reset GameBox's own HONOR Dock preference; the
temporary taskbar pin was removed and the app was left on TV Home, but the Dock
is visible. Global taskbar state was not changed. Evidence:
`docs/evidence/hardware-run-2026-10-06/honor-tv-display-guidance.png`.

### Honor controller reconnect check (6 October 2026)

After the user reported reconnecting and pressing A, ADB again reached the
physical PGT-N19 and the GameBox MainActivity was confirmed active on the
1920×1080 HDMI display. The current live HDMI capture still shows the
controller status as “Not connected.” Android's Input Manager lists only
system keyboard/touch devices and no `GAMEPAD` or `JOYSTICK` device. The
phone-panel capture had Azahar's first-run Data Folders page open; no folder was
selected or permission granted, and GameBox was brought back to the foreground
without force-stopping either app. Evidence:
`docs/evidence/hardware-run-2026-10-06/honor-tv-live-after-controller-reconnect.png`.

This means the reported A press is not proof of a connected gamepad or working
GameBox input. Re-run the physical D-pad/A/B check once Android exposes the pad
as an input device; do not “fix” GameBox's detection based only on the
Honor-specific `vendor=0x2888` system input devices.

A follow-up Bluetooth Manager snapshot confirms the `Wireless Controller` is
bonded but its current connection state is `STATE_DISCONNECTED`. Historic HID
connection log lines are not live connection proof. The current
“Not connected” GameBox card therefore matches Android's present state rather
than indicating a GameBox classification bug.

### Current repository verification (6 October 2026)

Ran the complete `testDebugUnitTest` suite and `lintDebug` using the cached
Gradle 9.7.1 distribution and Android Studio JBR; both completed successfully.
Lint emitted 20 SARIF findings, primarily target-SDK/dependency-version notices
and project warnings; no lint error failed the task. These checks validate
source/unit behavior only and do not replace the outstanding Honor controller
or emulator gameplay checks.

### HDMI UI navigation smoke check (6 October 2026)

On the real Honor external display (1920×1080), injected Android key events
targeted to logical display 334 moved focus right from Galaxy Patrol to Luigi's
Mansion, A opened its details with Play Now focused (the game was not launched),
and B returned to Home. X opened the Store's on-screen search keyboard and B
closed it with an empty query. The user-facing status still reported the
controller disconnected, so this confirms GameBox's HDMI key-event/UI paths
under synthetic input only—not Bluetooth hardware/controller operation.
Captures: `honor-tv-before-synthetic-input.png`,
`honor-tv-after-synthetic-dpad-right.png`,
`honor-tv-after-synthetic-confirm.png`,
`honor-tv-after-synthetic-back.png`, `honor-tv-after-synthetic-search.png`,
and `honor-tv-search-closed.png` in `docs/evidence/hardware-run-2026-10-06/`.

### HDMI Download Manager route (6 October 2026)

From Store, the Menu quick-action dialog was opened, Down was sent three times
to focus Downloads, and A opened Download Manager on the physical TV. The
screen showed an empty active queue, four completed records, three browser
downloads marked “Added to Library,” Galaxy Patrol completed, and 437.1 GB
free. This confirms the TV route and that existing persisted completion/import
records are visible. No transfer was started; this does not re-verify browser
download persistence through app exit/restart or a new DownloadManager-to-import
flow. Evidence: `docs/evidence/hardware-run-2026-10-06/honor-tv-quick-actions.png`
and `docs/evidence/hardware-run-2026-10-06/honor-tv-download-manager-route.png`.

### Home-to-Vimm search and results (6 October 2026)

Fixed the Home “Open Vimm Lair in GameBox” route: it now opens Store with the
configured Vimm source focused, and activating that source with an empty query
opens the controller keyboard. Submitting a non-empty query now actually runs
Vimm discovery instead of only dismissing the keyboard. On the real Honor HDMI
display, followed the Home CTA, entered `MARIO`, and verified 65 results
grouped into console rows with artwork. No result details were opened and no
download was started. This was exercised with HDMI-targeted synthetic/touch
input; the Bluetooth controller is still reported disconnected, so physical
controller input remains unverified. Evidence includes
`honor-tv-home-vimm-route.png`, `honor-tv-vimm-keyboard.png`, and
`honor-tv-vimm-fixed-results.png` in
`docs/evidence/hardware-run-2026-10-06/`.

The targeted unit test and `assembleDebug` passed, and the updated APK was
installed in place on the Honor. A subsequent local `lintDebug` attempt could
not initialize Gradle's Windows native library under the sandbox; `git
diff --check` reported no whitespace errors (only existing line-ending
warnings). No game files or user data were changed.

### Vimm WebView controller focus follow-up (6 October 2026)

The user reported that the Vimm page's Download control could not be pressed.
On the physical Honor TV, Android exposed the Wireless Controller as a gamepad,
but the tracked Vimm WebView was not focused when opened. Added explicit
WebView focus on primary and popup creation, and translate gamepad D-pad
directions to forward/reverse Tab navigation (keyboard arrows remain unchanged).
Added a focused unit test for the direction mapping. `testDebugUnitTest`,
`assembleDebug`, `lintDebug`, and `git diff --check` passed; the build was
installed in place on the Honor. After install, GameBox reported the Wireless
Controller connected and its Home-to-Store Vimm shortcut opened with focus.
The final physical D-pad-to-web-control check remains unverified: a fresh Vimm
search returned no results, so I could not reopen a game detail/browser page on
the updated build. An existing Mario Party Advance Download Manager item showed
0 B transferred; it was neither started nor cancelled during this test.

### Quick Actions Downloads controller navigation (7 October 2026)

Made the Quick Actions rows use explicit vertical focus order, so three D-pad
Down presses from the default Search row focus Downloads, and Up reverses the
sequence. Added a regression test for reaching Downloads and activating it.
`testDebugUnitTest`, `assembleDebug`, and `lintDebug` passed; the APK was
updated in place on Honor without clearing app data. Verified on the physical
HDMI display by sending Menu, three D-pad Down, and A key events: Downloads
visibly received focus and opened the Download Manager. The Android
instrumentation runner crashed on Honor for the Compose instrumentation tests,
so that regression test compiled but did not produce a passing runner result.
No download was started or modified. Evidence:
`docs/evidence/hardware-run-2026-10-07/honor-tv-quick-actions-downloads-focused.png`
and `docs/evidence/hardware-run-2026-10-07/honor-tv-download-manager-opened.png`.

### Downloads-first Quick Actions follow-up (7 October 2026)

The user still could not reach Downloads reliably. Changed the initial Quick
Actions focus from Search to Downloads, keeping the remaining actions below it.
On the connected Honor HDMI display, Downloads showed the focus ring immediately
after opening Menu; sending the controller-A key event opened Download Manager.
This was a display-targeted synthetic Android key test, not a physical button
press. `compileDebugAndroidTestKotlin` and `testDebugUnitTest` passed, and the
updated APK was installed in place without clearing data. No transfer was
started. The HONOR desktop dock remained visible at the bottom during this
capture, so the taskbar-hidden/fullscreen requirement remains open. Evidence:
`docs/evidence/hardware-run-2026-10-07/honor-tv-downloads-first-focused.png`
and `docs/evidence/hardware-run-2026-10-07/honor-tv-downloads-first-activated.png`.

### HONOR Dock fullscreen behavior for GameBox (7 October 2026)

On the Honor PGT-N19 HDMI display, GameBox already occupied the complete
1920×1080 Android window, but HONOR's Dock overlaid the bottom edge. Opening
GameBox's Dock menu and selecting **Open in full screen mode** hid the Dock;
Download Manager remained full-bleed after Back → Menu → A. A subsequent
`adb install -r` and app relaunch made the Dock visible again, so the OEM
per-app setting must be re-applied after this update/relaunch path. No global
taskbar setting was changed. Updated Display settings guidance now includes
GameBox itself. JVM tests, Android-test compilation, and APK assembly passed;
the updated build was installed in place. Current HDMI session is left on Home
with HONOR's Dock visible. Evidence:
`docs/evidence/hardware-run-2026-10-07/honor-tv-download-manager-no-dock.png`
and `docs/evidence/hardware-run-2026-10-07/honor-tv-after-update-dock-returned.png`.

### Quick Actions D-pad traversal follow-up (7 October 2026)

Removed Quick Actions' per-row Up/Down interception so Compose performs normal
vertical focus traversal; Downloads remains first and receives initial focus.
Added an instrumentation regression test for Down to Home and Up back to
Downloads. `testDebugUnitTest` and `compileDebugAndroidTestKotlin` passed, and
the APK was installed in place on the Honor without clearing data or running
AndroidX Orchestrator. The current device check shows Azahar foreground on the
HDMI session, so no controller keys were sent and live navigation remains
unverified. The root GameBox input handler returns false for D-pad arrows, so it
does not consume these navigation events.

### Honor controller/display recheck (7 October 2026)

The next read-only device check now enumerates `Wireless Controller` and
`Wireless Controller Touchpad` as gamepad-capable input devices; Android reports
controller number 1 and 85% battery. The HDMI surface remains active at
1920×1080, and the external activity stack still has Azahar foreground. This
confirms the controller is paired/connected at the Android input layer, but
does not prove GameBox receives physical events or that Quick Actions navigates
correctly. No keys were injected while Azahar was foreground.
`lintDebug` also passed on the same source state (0 errors, 21 existing
warnings).

### Quick Actions actual-HDMI smoke test (7 October 2026)

On the active 1920×1080 Honor TV, opened GameBox from the idle desktop. The
Downloads item received initial focus; one synthetic D-pad Down moved focus to
Home. After reopening Quick Actions, synthetic D-pad Center on the focused
Downloads row opened Download Manager, which showed an empty queue. The TV
status changed to `Not connected` for the controller during the run. A
synthetic `BUTTON_A` did not activate Downloads; because the physical pad was
not connected at that point, this does not establish a physical-A defect.
Reverse Up traversal was not confirmed. This is hardware-display software-input
evidence, not physical-controller acceptance; no transfer was started. Current
captures:
`docs/evidence/hardware-run-2026-10-07/honor-tv-download-manager-quick-actions-focus-followup.png`
and `docs/evidence/hardware-run-2026-10-07/honor-tv-download-manager-final.png`.

### Quick Actions HDMI navigation check (7 October 2026)

Launched GameBox on the idle Honor HDMI desktop (1920×1080). With Menu injected
to the external display, the Downloads row visibly received initial focus.
One injected D-pad Down moved focus to Home. An injected `KEYCODE_BUTTON_A`
did not activate the row; after reopening Quick Actions, injected
`KEYCODE_DPAD_CENTER` did open Download Manager. GameBox showed the controller
as disconnected during this sequence, so this verifies the real-TV software
focus/action route only, not physical Bluetooth A/D-pad behavior. The attempted
Up capture did not show the dialog, so reverse navigation is not counted as
verified. The manager showed an empty queue; no transfer was started. Evidence:
`docs/evidence/hardware-run-2026-10-07/honor-tv-downloads-first-focused.png`,
`docs/evidence/hardware-run-2026-10-07/honor-tv-download-manager-quick-actions-focus-followup.png`,
and `docs/evidence/hardware-run-2026-10-07/honor-tv-download-manager-opened-after-dpad-center.png`.

### Late archive-corruption cleanup regression (7 October 2026)

Added a ZIP fixture with a valid first game file and a corrupt later DEFLATED
entry. The test proves extraction fails and removes the entire staging output,
including the already-written first file. `RomArchiveExtractorTest` passes;
the full JVM suite passes with 783 tests across 213 suites, 0 failures, 0
errors, and 1 skip. `git diff --check` reports no whitespace errors (only the
repository's existing LF-to-CRLF notices). This covers the corruption/rollback
software case; physical DocumentsUI import and storage-failure scenarios remain
open.

### SAF permission-revocation recovery (7 October 2026)

The shared source-staging boundary now treats `SecurityException` while opening
a selected document as unavailable, allowing the UI to ask the user to select
the file again rather than displaying a generic import failure. A unit test
proves this path leaves no staged files. Full gate passed: 784 JVM tests across
214 suites, 0 failures/errors, 1 skip; Android-test Kotlin compilation, lint
(0 errors, 21 warnings), and APK assembly also passed. The new APK was not
installed because no controller was connected; physical SAF grant-revocation
behavior remains unverified.

### Make Downloads directly reachable in the main navigation (7 October 2026)

Downloads is now included in the top-level tab sequence as well as the existing
header and Quick Actions shortcuts. This gives controller users another direct
route to Download Manager when Quick Actions focus/input is unreliable. The
Honor input-device check at the time of the report showed no active Bluetooth
controller, so physical controller acceptance still needs a connected-pad test.

### Downloads navigation verified on actual Honor HDMI (9 October 2026)

On the Honor PGT-N19 and 1920×1080 HDMI display, used the PC Hub's **Show on
TV** action to start GameBox on the external display. The current Android
logical display ID was 3 (the old ID 334 was stale); sending input to 334 had
no effect. With the correct ID, spaced synthetic R1 key events visibly moved
Home → Library → Downloads, where Download Manager rendered its empty queue.
The standard Menu key opened Quick Actions with Downloads initially focused,
and synthetic A closed the dialog back to Download Manager. HONOR's per-app
**Open in full screen mode** option hid the bottom dock and transient title
controls; the settled 1920×1080 capture shows the full-screen manager. Evidence:
`docs/evidence/hardware-run-2026-10-09/honor-tv-downloads-fullscreen.png`,
`docs/evidence/hardware-run-2026-10-09/honor-tv-quick-actions-downloads-focused.png`,
and `docs/evidence/hardware-run-2026-10-09/honor-tv-quick-actions-downloads-activated.png`.
Android reported only keyboard/DPAD input and GameBox displayed `Not connected`,
so this is actual-TV synthetic-input coverage, not physical Bluetooth-controller
acceptance. No download was started.

### Game-details Vimm action routing verified on actual Honor HDMI (9 October 2026)

Fixed the game-details **Download** action to set the same pending Vimm-search
request as the Home shortcut before opening Store. Previously it requested
generic Store search, which could show the general search keyboard instead of
focusing Vimm Lair. Targeted `GameBoxUiStateTest` and `PrimaryNavigationTest`
passed, and `assembleDebug` succeeded. Installed on the Honor and verified the
TV flow: Galaxy Patrol details → Store with **Search Vimm's Lair** focused → A
opens the controller keyboard → B submits a Vimm search, which completed with
zero matches. The synthetic D-pad text-entry attempt produced the accidental
query `CELWQRW`, so this verifies handoff and search submission, not query
correction/accuracy. Only metadata search ran; no game result was opened or
downloaded. Bluetooth controller remained disconnected, so physical-pad
acceptance of the Details action is still open.

### Full unit suite and UI-test compilation follow-up (9 October 2026)

The full JVM suite initially caught a stale transition assertion: Downloads had
been promoted into the primary tab sequence, but its test still expected the
old vertical-stack motion. Updated the assertion to match the current tab
contract (Home → Downloads moves horizontally). Re-ran the full suite: 784
tests across 214 suites, 0 failures/errors, 1 skip; Android UI-test Kotlin
compilation also succeeded. The Honor HDMI screen was checked afterward and
remained full-screen with the Store search keyboard closed. Follow-up actual-TV
testing of the Vimm vault page is recorded below.

### Vimm webpage Download focus verified on actual Honor HDMI (9 October 2026)

The Vimm vault page contained many links before its Download control, so the
browser's ordinary tab traversal made controller navigation impractical. The
Vimm-only page-finished handler now focuses and scrolls the exact Download
control into view (it does not activate it). On the Honor PGT-N19 HDMI screen,
the Download button showed the focus outline on arrival; one synthetic D-pad
Down moved focus to Play Online, and D-pad Up returned it to Download. This
confirms the web page focus path, but not physical Bluetooth-controller input.
Evidence: `docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-download-focused.png`.
The page was God of War: Chains of Olympus; the copyrighted Download control was
not activated and no game binary was downloaded. Full suite: 784 JVM tests
across 214 suites, 0 failures/errors, 1 skip; Android UI-test Kotlin compilation
and APK assembly passed. HONOR full-screen mode was restored for the browser.

### Vimm controller A activation verified on actual Honor HDMI (9 October 2026)

Found a remaining WebView input gap: although the Download button received DOM
focus and Android translated controller A to D-pad-center, A did not reliably
activate a focused webpage control. Vimm pages now consume the controller A
press and click the active DOM element on key-up; other hosts retain the native
key path. Installed the updated APK on the Honor PGT-N19 and tested on the
1920×1080 wired TV display: opened the God of War: Chains of Olympus vault page,
moved focus from Download to the safe **More...** link, and sent synthetic
gamepad A. The link expanded the vault metadata (MD5/SHA-1 appeared), confirming
the page-control activation path. D-pad Down then focused Download again. The
copyrighted Download control was not activated and no file transfer occurred.
HONOR's per-app **Open in full screen mode** was reapplied; the settled TV
capture shows the Download control focused with no dock/taskbar or window bar.
Evidence: `docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-controller-a-safe-control.png`
and `docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-download-focused.png`.
Verification: 784 JVM tests, 0 failures/errors, 1 skipped; Android UI-test
Kotlin compilation and debug APK assembly passed; `lintDebug` passed with 0
errors and 23 warnings. The physical Bluetooth pad was
still disconnected, so controller acceptance used injected gamepad key events.

After the test page was left with Download focused, a later TV observation
showed a God of War browser transfer in GameBox's Download Manager at 194.8 MB
of 1.0 GB (18%, 0 B/s), and the footer identified a Wireless Controller. I did
not activate Download or interact with that transfer; who started it is
unconfirmed. The app has surfaced it in the durable queue, but completion,
verification, and import were not tested. The user was notified and asked to
confirm whether the transfer was intentional. Evidence:
`docs/evidence/hardware-run-2026-10-09/honor-tv-browser-download-queued-observed.png`.

### PPSSPP full-screen presentation verified on actual Honor HDMI (9 October 2026)

Observed the active PPSSPP game on the wired 1920×1080 Honor TV output after
selecting HONOR's per-app **Open in full screen mode** action. The settled
capture has no app title bar, no HONOR dock/taskbar, and no PPSSPP touch-button
overlay; the game retains its native aspect ratio with side bars. A small
PPSSPP menu affordance remains at the top center. No gameplay input was sent.
This verifies presentation only, not the physical controller or long-session
stability. Evidence: `docs/evidence/hardware-run-2026-10-09/honor-tv-ppsspp-fullscreen.png`.

### Follow-up host verification limitation (9 October 2026)

On the first sandboxed attempt, Android Studio's SDK `adb.exe` could not start
its local server on TCP 5037. A fresh Gradle gate likewise failed to initialize
its Windows native services because sandbox access to Gradle's native-cache
lock was denied. Both were resolved by allowing the test process access to its
existing Gradle/ADB host state; details and the successful host rerun follow.
No UI input was sent during the read-only device inspection.

### Host-side verification rerun (9 October 2026)

After granting the build process access to Gradle's existing native cache, the
offline verification gate completed successfully. Then forced test execution
and Android-test Kotlin recompilation: 784 tests across 214 suites, 0 failures,
0 errors, 1 skipped; `compileDebugAndroidTestKotlin` passed. The full gate also
reported `lintDebug` and `assembleDebug` successful (the latter tasks were
up-to-date). Compiler emitted existing nullable-`File` warnings in test code.
This remains host-side evidence; it does not replace the outstanding physical
controller and live-TV session checks.

### Current Store/Vimm entry observed on actual Honor HDMI (9 October 2026)

ADB read-only inspection now confirms the Honor PGT-N19 HDMI display is active
at 1920×1080 and GameBox is showing Store with PSP selected, God of War:
Chains of Olympus art/details, and the **Search Vimm's Lair** source entry.
Android's input service currently enumerates **Wireless Controller Touchpad**
with gamepad/joystick sources, and GameBox's footer says **Wireless
Controller**. No key event was sent; this does not prove physical-button
navigation or Download activation. Evidence:
`docs/evidence/hardware-run-2026-10-09/honor-tv-store-vimm-entry-readonly.png`.
The latest 1,200 Android error-log lines had no matching GameBox/emulator fatal
exception or ANR entries; this is only a recent-log spot check, not a soak test.

### Vimm search-to-details and completed queue observed on actual Honor HDMI (9 October 2026)

On the 1920×1080 HDMI display, synthetic gamepad D-pad Right/Down moved visible
focus through Store controls to **Search Vimm's Lair**. Synthetic A opened the
search; the results appeared under PSP with three matches. Focus traversal from
the Vimm source action landed on the rightmost card; D-pad Left twice focused
the first match, and synthetic A opened its GameBox details. After the details
settled, they reported that a temporary download had been verified, moved, and
added to the Library. The details also reported that extra source details could
not be loaded. Activating the view-only **View Download Manager** action showed
0 active, 0 queued/paused, and 1 completed browser download marked **Added to
Library** / **Ready**. Its UI filename is a Europe/Australia `.7z` variant,
different from the USA archive in the local test-games folder.

This appears to be the previously observed transfer that was already around
18% before this test. I did not start, retry, pause, cancel, open, or launch
that item; who initiated it is still unknown. No Vimm webpage Download control
was activated. The test proves Store search/result/details and completed-queue
visibility on TV using injected gamepad events, not physical-controller
acceptance or the Download click itself. Evidence:
`docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-search-results.png`.
GameBox's footer alternated between
**Wireless Controller** and **Not connected** during the checks, so physical
connection stability also remains open. Evidence:
`docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-first-result-focused.png`,
`docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-result-details-settled.png`,
and `docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-download-manager-completed.png`.

### Vimm controller Download follow-up (9 October 2026)

Addressed the reported controller failure in two places. Game Details now has an
explicit D-pad Down route from its primary action to the Vimm **Download**
entry, with an Android UI test covering focus and A activation. In
`TrackedSourceBrowserActivity`, removed the Vimm-only JavaScript `.click()`
interception for controller A; A is now translated to native D-pad-center
activation so WebView handles the focused control as a real key activation,
which is required for sites that gate downloads on trusted user gestures.

Installed the rebuilt APK on the Honor PGT-N19 and tested on its HDMI output.
Injected D-pad Down focused the Vimm button, and A opened Store. Using only the
on-screen controller keyboard, D-pad, and A, entered `CRASH`, selected PSP,
searched Vimm, and received 10 PSP metadata results with box art. No webpage
Download control was activated and no transfer was started or changed. The
physical Bluetooth controller showed **Not connected** after the reinstall, so
these were injected-key tests. The in-app Vimm webpage's native A-to-Download
activation remains unverified on live hardware; no copyrighted binary was
downloaded. Evidence:
`docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-download-action-focus-2026-10-09.png`,
`docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-cta-store-open-2026-10-09.png`,
`docs/evidence/hardware-run-2026-10-09/honor-tv-controller-search-query-2026-10-09.png`,
and `docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-metadata-results-2026-10-09.png`.

Host gate passed after the change: unit tests, Android UI-test Kotlin
compilation, `lintDebug`, and `assembleDebug`. Reinstalling reset HONOR's
per-app fullscreen preference; maximizing removed the window bar, but the
bottom HONOR taskbar remains visible. The previous completed browser transfer
was not touched.

### Vimm download-button report follow-up (9 October 2026)

The Honor remains connected to the 1920x1080 HDMI display. Android currently
enumerates the Bluetooth Wireless Controller as a GAMEPAD/JOYSTICK device.
GameBox was on Downloads during this check, showing a Vimm-related browser
transfer (`Crash of the Titans`, PSP, 7%, 50.0 MB of 703.4 MB) in the Browser
downloads section. This indicates that a Vimm download was accepted and tracked
by GameBox at some point; I did not initiate, pause, cancel, or otherwise
interact with that transfer. I therefore did not retry a protected game
download. This is not a physical-controller acceptance test, and it does not
prove which input started the transfer or that the user can reliably focus the
Download control. Evidence: `docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-download-manager-active-observed-2026-10-09.png`.

Source review confirms browser arrows are translated to forward/reverse Tab,
and controller A to native DPAD_CENTER before WebView dispatch. Existing tests
cover those mappings and Game Details focus routing; they do not cover focus
and activation on Vimm's live page. No source changes were made this turn.

Repeated the offline `testDebugUnitTest` task with `--rerun-tasks`; it completed
successfully (25 tasks executed). On the following HDMI screenshot, the already
active browser transfer had advanced to **Verifying & importing**, extracting
archive file 1/1 at 73%. This independently verifies that the transfer is
present in GameBox's Browser downloads UI and the automatic archive-import
pipeline is progressing. No button event was sent by ADB during this step, and
the user has not yet confirmed the requested physical D-pad press. I did not
launch or otherwise test this commercial game. Evidence:
`docs/evidence/hardware-run-2026-10-09/honor-tv-browser-import-progress-observed-2026-10-09.png`.

### Browser-import completion recheck (9 October 2026)

A later read-only capture of the same Honor HDMI Downloads screen shows no
active or queued transfers, Completed = 2, and the Crash of the Titans browser
record marked **Added to Library**. The God of War: Chains of Olympus browser
record is also present as **Added to Library**. This confirms those imports
reached GameBox's completed-list UI; it does not prove source-archive cleanup,
content integrity, or emulator compatibility. I did not launch or modify
either title. The completed state and titles are visible in
`docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-import-completed-observed-2026-10-09.png`.

The gamepad continues to enumerate on Android, but the passive event listener
received no input during the most recent ~25-second listen window. The user has
not confirmed that a physical press occurred in that interval. A/B navigation
and controller-only confirmation therefore remain unverified.

### Galaxy Patrol legal gameplay and emulator display check (9 October 2026)

On the physical Honor HDMI output, opened the bundled MIT-attributed Galaxy
Patrol fixture from its Home collection card, confirmed the installed state,
and launched it from the focused **Play Now** action. Android's resumed window
was `com.retroarch/.browser.retroactivity.RetroActivityFuture`; the HDMI image
rendered the ROM's `GAME OVER` screen. A passive listener on the paired
Wireless Controller's `/dev/input/event13` observed actual `ABS_HAT0X` /
`ABS_HAT0Y` transitions and button events while RetroArch was foreground.
This confirms device-level input events and a real RetroArch launch, but it is
not a complete mapping/gameplay acceptance test: the screen did not show a
meaningful game session or a button-correlated state transition.

Applied HONOR's visible per-app **Open in full screen mode** option for
RetroArch. After it settled, the TV capture showed the game filling the HDMI
surface with neither HONOR Dock nor RetroArch touch controls visible. This is
an OEM per-app setting, not an app-controlled mode. Android Back did not exit
RetroArch. Brought the existing GameBox activity forward, then used HONOR's
normal taskbar **Quit** menu for RetroArch; GameBox returned to Galaxy Patrol
details, and the RetroArch taskbar menu subsequently reported **Start in
Windowed Mode**, confirming the fullscreen preference persisted. This is a
manual OEM exit, not a controller-only return. Evidence:
`docs/evidence/hardware-run-2026-10-09/honor-tv-galaxy-patrol-fullscreen-no-overlay.png`
and
`docs/evidence/hardware-run-2026-10-09/honor-tv-galaxy-patrol-returned-to-gamebox.png`.

The user was asked for a physical B press on Galaxy Patrol details, but the
passive listener received no event during that observation window. Do not
claim physical GameBox B/back navigation or a controller-only emulator exit.

### TV focus route into GameBox's Vimm entry (9 October 2026)

On the external Honor HDMI display, GameBox visibly moved focus from Galaxy
Patrol's **Play Now** action to its Vimm **Download** action after one
synthetic D-pad Down. Synthetic D-pad Center on that action opened Store with
**Search Vimm's Lair** focused. This confirms the in-app details-to-Store
controller route on the TV; it does not test the physical controller or the
live Vimm webpage's Download control. No title was opened or downloaded.
Evidence:
`docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-focus-synthetic-dpad.png`
and
`docs/evidence/hardware-run-2026-10-09/honor-tv-vimm-search-entry-synthetic-a.png`.
Selecting the focused source entry opened GameBox's controller search
keyboard, and synthetic B closed it back to Store. That covers this
in-app text-entry/back route, not WebView navigation or its Download control.
Evidence:
`docs/evidence/hardware-run-2026-10-09/honor-tv-search-keyboard-open-synthetic-a.png`
and
`docs/evidence/hardware-run-2026-10-09/honor-tv-search-keyboard-closed-synthetic-b.png`.

### Vimm preset and selectable console names cross-check (10 October 2026)

Added an assertion that every platform in the user-requested Vimm default
preset has a matching selectable Store filter. The existing source tests also
verify the preset order, each supported Vimm vault slug/import profile, and
the explicit no-mapping state for 3DS and Switch; Settings explains why those
two are skipped. The focused `StoreConsoleTest` suite passed (4 tests, 0
failures). This closes the preset-to-filter drift check, not the separate
live-browser/download/import acceptance gate.

Android's current input inventory did not list a Bluetooth gamepad, matching
GameBox's **Not connected** indicator. A targeted Honor instrumentation run
was built successfully with one Gradle worker and a 3 GB heap after the
default 2 GB build exhausted memory, but Compose tests launched on the phone's
default display rather than the external TV. That run was stopped and is not
counted as TV acceptance. The fresh offline JVM unit suite passed (25 tasks
executed). Physical controller input and Vimm WebView Download activation
remain unverified.

### Synthetic GAMEPAD-source Vimm search on the external TV (10 October 2026)

Android exposes a shell input source that labels injected keys as GAMEPAD, so
I used it on the external HDMI display to exercise GameBox's gamepad-specific
event path despite the Bluetooth controller being absent. GAMEPAD A opened
the Vimm source search; D-pad/A entered `CAVE`; A on **Search** completed a
lookup across the configured consoles. The screen reported **10 results**
grouped by console. No result was activated and no file was downloaded. This
verifies the Store keyboard and configured-source search path with synthetic
events, not a physical controller or the live Vimm WebView Download control.
Evidence:
`docs/evidence/hardware-run-2026-10-10/honor-tv-vimm-search-keyboard-gamepad-a.png`,
`docs/evidence/hardware-run-2026-10-10/honor-tv-vimm-keyboard-gamepad-typed-c.png`,
and
`docs/evidence/hardware-run-2026-10-10/honor-tv-vimm-search-results-cave.png`.

### Tracked WebView controller event-source normalization (10 October 2026)

The reported failure is after the Vimm button is visibly focused. The browser
previously translated controller A to Enter but retained the original GAMEPAD
event source. It now translates D-pad movement to Tab and A to D-pad Center,
normalizing translated events to Android's keyboard source while preserving
timing, repeat, device ID, scan code, flags and meta state.
The tracked WebView instrumentation regression now creates a GAMEPAD A event,
passes it through the same conversion used by `TrackedSourceBrowserActivity`,
asserts the keyboard source, and checks that focused `_blank` download activation
retains WebView's trusted-popup signal.

The current full gate passes: 792 JVM tests (one skipped), Android-test Kotlin
compilation, APK assembly, and lint (0 errors, 24 warnings). The APK was
installed over the existing Honor app without clearing data. The WebView
regression has not completed on-device: the Honor went asleep during the
instrumentation run, HDMI is disconnected, and no GAMEPAD/JOYSTICK is
enumerated. A redistributable download and physical A-button test remain
outstanding.

### Exact game ownership during uninstall (10 October 2026)

`GameOwnedContentUninstaller` previously accepted a recorded import path if the
selected game ID appeared anywhere in the directory chain. A malformed entry
could therefore reference `imports/other/<selected-game-id>/...`. Validation
now accepts only the legacy `imports/<game>/...` shape or the managed
`imports/<known-console>/<game>/...` shape. A regression confirms that an
invalid later entry prevents deletion of every file in the manifest. Focused
and full JVM tests pass, along with instrumentation compilation, APK assembly,
and lint. External SAF uninstall behavior and the full save-preserving
reinstall journey still need device validation.

### Tracked browser external-display sizing (10 October 2026)

The tracked source browser was explicitly declared non-resizable even though
the dedicated external-display host is resizable. Changed the browser manifest
entry to resizable and added a JVM manifest-contract test. The debug merged
manifest reports `android:resizeableActivity="true"`; all 791 Android JVM tests
pass, Android-test Kotlin compilation and APK assembly pass, and lint reports
zero errors (26 warnings). Installed the update over the existing Honor app
without clearing data. Actual Honor fullscreen/window-chrome acceptance is
still pending because the phone is asleep and HDMI is not connected.

### Partial uninstall state after mixed-storage failure (10 October 2026)

External-library migration can leave a game with some content still in private
storage and some already migrated to SAF. Uninstall removes the private files
first. If SAF deletion then fails before deleting its first file, the external
error previously reported zero removed files, so the coordinator could leave
the game marked Installed despite the already-removed private files.

`ContentRemovalFailed` now computes the aggregate files removed before and
during the failing stage. `GameContentRemovalCoordinator` uses that total to set
`MISSING_FILES` and report the accurate partial count, so the existing recovery
and retry path remains available. Added regression coverage for the aggregate
count. All 792 Android JVM tests pass; Android-test compilation, APK assembly,
and lint pass (0 errors, 24 warnings). Installed the APK in place without
clearing data. A real SAF-provider failure and reinstall round-trip remain
unverified on hardware.

### Windows companion core verification (10 October 2026)

The installed .NET 8 SDK/runtime is available even though `dotnet` is not on
PATH. Ran the current-source `GameBox.Windows.Core.Tests` console harness with
Windows targeting enabled; it passed. Its default `%TEMP%` location was denied
by the workspace sandbox, so the test process used the existing workspace
`tmp` directory and cleaned its per-run test tree. This covers core checks only;
the Windows UI and live Honor-to-PC companion flows remain outside this test.

### Vimm controller download focus (10 October 2026)

The Vimm WebView focus script previously selected a control using only the
first nonempty label. Ordinary button text therefore masked a more useful
`aria-label` such as “Download game”. It now checks visible text, input value,
ARIA label and title separately, and assigns `tabindex=0` to a matching custom
role-button so browser Tab navigation can reach it. A representative local
WebView instrumentation test passes on Honor PGT-N19. The debug APK is
installed in place with app data preserved. This does not prove physical
controller activation or the live Vimm-to-Download-Manager handoff; the
external TV display is currently off and the controller is unavailable.

### Remove unused TheGamesDB metadata scraper (10 October 2026)

`CatalogRuntime` enriches game rows through Wikidata and Libretro thumbnails;
the old `TheGamesDbMetadataClient`/parser was not wired into that runtime. Removed
that dead per-game scraper and its parser/cancellation tests, and moved the
generic provider-candidate/search contracts into `GameMetadataSearchClient.kt`.
Corrected README claims to match the wiring. The separate optional TheGamesDB
catalog-sync UI/runtime remains because there is not yet a verified replacement
for its cached title catalog; it may still contribute provider fields to rows it
syncs, so the broader migration is not complete. Android JVM tests pass 783/783
with one skip; Android-test Kotlin compilation, lint, and APK assembly pass. The
APK was installed on Honor in place; TV visual/source acceptance remains pending.
