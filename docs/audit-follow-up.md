# Audit implementation follow-up

Baseline: 9 September 2026 requirement audit, main `1504046a036f3a2a7e044b08bd62b6cb059b927e`.
Keep implementation, production integration, automated validation, live-service/emulator
validation and hardware acceptance separate. The original audit is a frozen baseline,
not a claim that subsequent branch work is already on main.

## Current implementation checkpoint (27 September 2026)

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
Wi-Fi transport now passes the complete 60-test debug instrumentation suite with zero
failures and zero skips. The earlier failure exposed a production focus defect: the root
Compose container handled controller key events but did not request focus on a physical
phone. The root now owns an initial focus target. This is physical UI/controller evidence,
not emulator gameplay or external-controller hardware evidence.

Companion JSON responses now share control-character escaping for device names and
transferred filenames, with JVM coverage for status serialization. This keeps the LAN
protocol parseable for valid user metadata without expanding the content-transfer trust
boundary. Companion content replacement now also recovers an orphaned previous copy
before retrying, preserving the old installed content when a staged replacement fails.
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
The Windows core executable test harness passes locally, and now includes the documented
read-only `--live-companion <host> <port> <pairing-secret>` probe for repeatable LAN
status/library validation, plus a separate `--live-companion-discovery <host> <port>`
probe for nonce-correlated UDP discovery and an idempotent
`--live-companion-configuration <host> <port> <pairing-secret>` configuration
round-trip probe. Its unknown-length HTTP test
content was made compatible with the .NET 8 `Task` override contract by converting the
stream write `ValueTask` to a `Task`.

The protected Android production workflow now runs the release-manifest unit tests,
creates a schema-compatible APK provenance manifest from the signed artifact, requires
the distinct rollback tag in that manifest, and uploads the manifest alongside the APK
and checksum. It also verifies the generated checksum file before publication. Local
Python is not installed on this Windows host, so the new script's automated execution
remains a CI-side validation rather than local evidence.

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

The local Android JVM suite then exposed a host-capability failure in
`DirectorySaveAdapterTest`: Windows symbolic-link creation is denied without the
required privilege. The test now classifies that specific capability absence as a
skip while retaining full symlink coverage on capable CI hosts. The complete local
suite passes with 503 tests, zero failures, and one explicit privilege-based skip;
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
- Preserves the existing `X-GameBox-Authorization` protocol used by Windows.

Local validation: 8 JUnit tests passed using an isolated Kotlin 1.9.24/JVM 8 toolchain:
`CompanionHttpConnectionTest`, `CompanionProtocolTest`, `CompanionStatusRouteTest`.
The connection test opens actual loopback sockets, does not half-close the client,
and checks 401, incomplete-request 408, malformed-request 400, then authenticated 200
on the same listening socket. Parser tests cover truncation, line endings, invalid
and duplicate headers, line/head/header-count limits and unsupported request bodies.

Merged in PR #276 after Android unit/build, instrumentation, phone/DeX screenshots
and Windows build checks passed. Pending: real Android service lifecycle/start-stop
coverage and the Windows client against the Android device endpoint. This change
does not implement file transfer/synchronization, durable pairing discovery, or
general save management, and does not close the full EXT-14 acceptance gate.


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
and live emulator gameplay remain pending. Intent dispatch is not proof of gameplay.

Emulator readiness now automatically selects the first installed approved adapter when
no per-game emulator choice has been saved, while preserving an explicit approved
selection and its missing-selection guidance. JVM coverage exercises the installed
alternative path; this improves software detection/configuration behavior but does not
prove a real emulator launch or gameplay.

1. One real emulator game/save/return/uninstall/reinstall proof, without synthetic-save claims.
2. Actual emulator-save ownership and production-emulator validation across the supported adapter patterns.
3. Live provider/download/volume validation against real configured endpoints and storage.
4. Physical controller and Honor Magic5 Pro external-display recovery/soak acceptance.
5. Signed production build, clean upgrade, update-channel and rollback rehearsal.
6. Windows Companion production-signed LAN session; the live debug mutation cleanup
   rehearsal is complete, with the retained save/backup state documented above.

The released-schema migration matrix and monotonic Android version-code generation are now
implemented and exercised by automated CI. They remain prerequisites for the signed
upgrade/rollback rehearsal rather than open implementation gaps.

Physical hardware gates stay pending and do not prevent independent software work.
