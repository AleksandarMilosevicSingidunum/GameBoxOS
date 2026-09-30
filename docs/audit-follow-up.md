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
shared host now translates `BUTTON_A` to the Android TV confirmation key, and
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
