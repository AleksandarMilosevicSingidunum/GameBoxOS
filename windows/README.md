# GameBox Windows Companion

This optional native .NET 8/WPF companion complements the Android/DeX GameBox OS product.

## Implemented capabilities

- Register local EXE, LNK/URL shortcut, BAT, and CMD launch targets with duplicate-path protection.
- Discover bounded Start Menu/desktop shortcuts and installed Steam games across primary and configured secondary Steam libraries, generating local protocol shortcuts under GameBox app data.
- Discover installed Epic Games from bounded launcher manifests while rejecting traversal, missing executables, malformed JSON, and oversized metadata.
- Create persistent Moonlight streaming sessions with validated host/application arguments while leaving pairing and credentials in Moonlight.
- Search and filter by favorites, availability, and platform.
- Discover enabled GameBox devices over bounded nonce-correlated UDP broadcast across active IPv4 LAN interfaces, choose a returned device, then authenticate with its pairing secret. Discovery exposes only device name, address, companion port, and protocol identity.
- Connect to a paired GameBox device, browse its library, update a title's favorite state, securely synchronize user-visible interface/download preferences, upload/download a selected managed save, and send a user-selected legally owned game copy to an existing catalog title.
- Remember a successfully authenticated device across restarts. Host and port are stored in the bounded local profile; the 256-bit pairing secret is encrypted with Windows DPAPI for the current user and can be explicitly forgotten without changing Android.
- Automatically reconnect and refresh the paired library after startup with three bounded exponential-backoff attempts for transient LAN failures, including HTTP 408, 429, and 5xx responses. Idempotent favorite and configuration updates expose the same bounded retry behavior. Authentication rejection, malformed protocol/data, and caller cancellation fail immediately.
- Read and atomically apply reduced-motion, unavailable-game, unavailable-shortcut, and unmetered-download settings through authenticated `/v1/config` requests. Configuration flags are checksum-bound to the request signature and Android echoes the committed snapshot for verification.
- Verify save SHA-256 on both platforms, limit saves to 16 MiB, support cancellation, retry idempotent downloads and save uploads, make identical uploads true no-ops, and preserve a different Android save before replacement. Identical owned-content replacements are also safe to retry; different content preserves the prior copy before replacement.
- Stream owned game copies up to 64 GiB without buffering them in memory. Android authenticates the declared checksum before reading the body, stages with an inactivity timeout, verifies SHA-256 again, enforces the selected console's format allowlist, and atomically registers the installed content. Identical streamed replacements are no-ops, so the client can retry transient uploads safely; a failed or cancelled transfer retains no partial content, and a different-content replacement restores the previous copy on failure.
- Produce a reproducible unsigned MSIX installer package beside the portable ZIP, with generated package assets, SHA-256 checksum, and provenance validation. Production package signing remains a separate release gate.

- Sort by favorites/title or recent play history.
- Edit validated titles, platforms, and command-line arguments without changing game identity or executable paths.
- Show launch targets in Explorer, relocate missing targets, and disable launch actions when files are unavailable.
- Record localized last-played status and clear it without changing favorites, launch settings, or game files.
- Persist the library atomically under `%LOCALAPPDATA%\GameBoxOS`.
- Export normalized JSON backups and restore validated backups with explicit confirmation.
- Enrich existing local entries from a bounded HTTPS catalog while preserving local launch trust.
- Reject insecure endpoints, redirects, embedded credentials, oversized responses, invalid timeouts, and unsafe launch extensions.

Enter plays the selected game; Ctrl+F searches; Ctrl+O adds a game.

The companion never searches for or downloads game content from third-party sources. Game transfer begins only after the user selects and confirms a local legally owned file. It does not delete Windows files or store streaming credentials. Moonlight remains a user-installed runtime; GameBox stores only its executable path and validated stream arguments.

## Remaining Windows work

The connected Honor debug session has validated Windows↔Android UDP discovery,
authenticated status/library access, idempotent configuration round-trip, favorite
mutation, managed-save upload/download/conflict preservation, and a checksummed content
upload through the repository probe. Remaining acceptance includes routed/VLAN/firewall
coverage, large-file and cancellation behavior, disk-full and replacement testing,
DPAPI reconnect on a real paired profile, additional PC-runtime integrations, MSIX
signing/install acceptance,
packaging, and physical Windows acceptance testing. Authenticode signing and signed
release upload are prepared through the protected `release-windows-production.yml`
workflow.

The local core harness verifies both cancellation before an owned-content upload
(before hashing/network dispatch) and cancellation after a streaming request has
started; neither path enters retry logic. This is deterministic client evidence.
Cancellation during a real long-running LAN transfer still requires live acceptance.

Android content-upload preflight now reserves space for both the request staging file
and the managed-import copy, preventing a transfer that cannot complete promotion.
The Android JVM regression suite covers rejection before any request-body bytes are
read; this remains deterministic storage evidence rather than a physical disk-full run.

CI publishes the self-contained ZIP and unsigned MSIX package with SHA-256 files and
validated JSON provenance manifests containing the exact artifact name, hash, byte size,
runtime, package format where applicable, self-contained flag, and source commit.
Validation requires exactly one non-empty `GameBox.Windows.exe` in the ZIP and verifies
both published package formats. The MSIX is a development artifact until signed with
the protected production certificate workflow.

## Build and test

```powershell
dotnet run --project windows/GameBox.Windows.Core.Tests/GameBox.Windows.Core.Tests.csproj -c Release
dotnet publish windows/GameBox.Windows/GameBox.Windows.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
powershell -File scripts/package-windows-msix.ps1 -PublishDirectory artifacts/GameBoxWindows -OutputPath artifacts/GameBoxWindows-win-x64.msix
```

For an explicitly requested live LAN read-only check, provide the Android device's
address, companion port, and pairing secret at runtime; the secret is not persisted
or printed:

```powershell
dotnet run --project windows/GameBox.Windows.Core.Tests/GameBox.Windows.Core.Tests.csproj -c Release -- --live-companion 192.168.1.2 49500 $pairingSecret
```

This validates authenticated device status and library retrieval. Save/content and
favorite mutations remain explicit client operations and are not run by the default
probe.

To validate unauthenticated LAN discovery separately, provide the expected device
address and Companion port:

```powershell
dotnet run --project windows/GameBox.Windows.Core.Tests/GameBox.Windows.Core.Tests.csproj -c Release -- --live-companion-discovery 192.168.1.2 49500
```

The discovery probe requires the Android device and Windows PC to permit the bounded
UDP broadcast on port 49499; it does not expose or require the pairing secret.

To validate authenticated configuration read/write without changing settings, use the
idempotent round-trip probe:

```powershell
dotnet run --project windows/GameBox.Windows.Core.Tests/GameBox.Windows.Core.Tests.csproj -c Release -- --live-companion-configuration 192.168.1.2 49500 $pairingSecret
```

It reads the current flags and writes those exact values back, verifying the returned
confirmation while keeping the device configuration unchanged.

For an explicit live managed-save transfer check, supply an existing GameBox game ID
and a small probe payload. The command uploads through the authenticated Android route,
downloads the resulting envelope, and verifies the returned bytes and SHA-256. It may
create or replace that game's managed save, so use it only when you intend to leave test
state on the paired device:

```powershell
dotnet run --project windows/GameBox.Windows.Core.Tests/GameBox.Windows.Core.Tests.csproj -c Release -- --live-companion-save 192.168.1.2 49500 $pairingSecret galaxy-patrol "GameBox live save probe"
```

The pairing secret is supplied only at runtime and is not printed by the probe. Remove
the resulting test save through the product's supported save/content cleanup flow after
validation.


## Signed Windows release

Configure the protected repository secrets `GAMEBOX_WINDOWS_PFX_BASE64` and `GAMEBOX_WINDOWS_PFX_PASSWORD`, create a stable `vMAJOR.MINOR.PATCH` tag, and run **Publish signed Windows companion**. The workflow checks out the exact tag, runs tests, signs and verifies `GameBox.Windows.exe`, derives the MSIX publisher from the same certificate, builds and signs the MSIX, verifies both signed formats, and uploads the ZIP/MSIX packages with SHA-256 files and manifests to that release. Development CI artifacts explicitly declare `authenticodeSigned: false`.

