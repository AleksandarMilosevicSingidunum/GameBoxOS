# Audit implementation follow-up

Baseline: 9 September 2026 requirement audit, main `1504046a036f3a2a7e044b08bd62b6cb059b927e`.
Keep implementation, production integration, automated validation, live-service/emulator
validation and hardware acceptance separate. The original audit is a frozen baseline,
not a claim that subsequent branch work is already on main.

## Current implementation checkpoint (27 September 2026)

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

Windows companion protocol alignment checkpoint: commit `2349131` constrains the
Windows content-upload client to the Android route's lowercase ASCII game-ID grammar.
The Windows core harness passes the rejection regression for an uppercase ID, so the
client now fails closed before sending an incompatible request.

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

Physical Android validation checkpoint (27 September 2026): the authorized Honor Magic6 Pro
Wi-Fi transport now passes the complete 61-test debug instrumentation suite with zero
failures and zero skips. The earlier local capability-limited run exposed a production focus defect: the root
Compose container handled controller key events but did not request focus on a physical
phone. The root now owns an initial focus target. This is physical UI/controller evidence,
and the same device now also has a verified real emulator gameplay/lifecycle run below;
separately connected external-controller and external-display evidence remain pending.

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

1. Extend real game/save/return/uninstall/reinstall proof across the supported adapter patterns.
2. Validate managed GameBox save import/backup/cloud behavior against production emulators and real endpoints.
3. Live provider/download/volume validation against real configured endpoints and storage.
4. Separately connected physical controller and Honor external-display recovery/soak acceptance.
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
