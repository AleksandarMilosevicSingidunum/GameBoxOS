# Audit implementation follow-up

Baseline: 9 September 2026 requirement audit, main `1504046a036f3a2a7e044b08bd62b6cb059b927e`.
Keep implementation, production integration, automated validation, live-service/emulator
validation and hardware acceptance separate. The original audit is a frozen baseline,
not a claim that subsequent branch work is already on main.

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

## Next critical-path work

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
await CI; no local Android SDK is available. This is not yet a complete
launch lifecycle: persistent sessions, large-file frame measurements
and live emulator gameplay remain pending. Intent dispatch is not proof of gameplay.

1. One real emulator game/save/return/uninstall/reinstall proof, without synthetic-save claims.
2. General game-ID-based content-only uninstall and actual emulator-save ownership.
3. Background launch validation and durable play-session recovery.
4. Authenticated provider/download/volume integration and core emulator setup.
5. Unified rich details, complete controller behavior and populated Blueprint visual acceptance.
6. Database upgrade matrix, monotonic release version codes, signing and rollback rehearsal.

Physical hardware gates stay pending and do not prevent independent software work.
