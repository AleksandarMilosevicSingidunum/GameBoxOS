# GameBox Windows Companion

This optional native .NET 8/WPF companion complements the Android/DeX GameBox OS product.

## Implemented capabilities

- Register local EXE, LNK/URL shortcut, BAT, and CMD launch targets with duplicate-path protection.
- Discover bounded Start Menu/desktop shortcuts and installed Steam games across primary and configured secondary Steam libraries, generating local protocol shortcuts under GameBox app data.
- Discover installed Epic Games from bounded launcher manifests while rejecting traversal, missing executables, malformed JSON, and oversized metadata.
- Create persistent Moonlight streaming sessions with validated host/application arguments while leaving pairing and credentials in Moonlight.
- Search and filter by favorites, availability, and platform.
- Connect to a paired GameBox device, browse its library, update a title's favorite state, upload/download a selected managed save, and send a user-selected legally owned game copy to an existing catalog title.
- Remember a successfully authenticated device across restarts. Host and port are stored in the bounded local profile; the 256-bit pairing secret is encrypted with Windows DPAPI for the current user and can be explicitly forgotten without changing Android.
- Automatically reconnect and refresh the paired library after startup with three bounded exponential-backoff attempts for transient LAN failures. Authentication rejection and invalid protocol/data fail immediately; caller cancellation stops before another request.
- Verify save SHA-256 on both platforms, limit saves to 16 MiB, support cancellation, and preserve a different Android save before replacement.
- Stream owned game copies up to 64 GiB without buffering them in memory. Android authenticates the declared checksum before reading the body, stages with an inactivity timeout, verifies SHA-256 again, enforces the selected console's format allowlist, and atomically registers the installed content. A failed or cancelled transfer retains no partial content; a failed replacement restores the previous copy.

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

Live Windows↔Android large-file, cancellation, DPAPI reconnect, disk-full, and replacement acceptance testing remains pending, along with additional PC-runtime integrations, installer packaging, and physical Windows acceptance testing. Authenticode signing and signed release upload are prepared through the protected `release-windows-production.yml` workflow.

CI publishes the self-contained ZIP with a SHA-256 file and a validated JSON provenance manifest containing the exact artifact name, hash, byte size, runtime, self-contained flag, and source commit. Validation also requires exactly one non-empty `GameBox.Windows.exe` in the archive and verifies the published executable before upload.

## Build and test

```powershell
dotnet run --project windows/GameBox.Windows.Core.Tests/GameBox.Windows.Core.Tests.csproj -c Release
dotnet publish windows/GameBox.Windows/GameBox.Windows.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
```


## Signed Windows release

Configure the protected repository secrets `GAMEBOX_WINDOWS_PFX_BASE64` and `GAMEBOX_WINDOWS_PFX_PASSWORD`, create a stable `vMAJOR.MINOR.PATCH` tag, and run **Publish signed Windows companion**. The workflow checks out the exact tag, runs tests, signs and verifies `GameBox.Windows.exe`, packages it, validates signed provenance, and uploads the ZIP, SHA-256, and manifest to that release. Development CI artifacts explicitly declare `authenticodeSigned: false`.

