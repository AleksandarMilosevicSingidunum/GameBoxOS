# Audit implementation follow-up

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
