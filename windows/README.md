# GameBox Windows Companion

This optional native .NET 8/WPF companion complements the Android/DeX GameBox OS product.

## Implemented capabilities

- Register local EXE, LNK/URL shortcut, BAT, and CMD launch targets with duplicate-path protection.
- Discover bounded Start Menu/desktop shortcuts and installed Steam games across primary and configured secondary Steam libraries, generating local protocol shortcuts under GameBox app data.
- Discover installed Epic Games from bounded launcher manifests while rejecting traversal, missing executables, malformed JSON, and oversized metadata.
- Create persistent Moonlight streaming sessions with validated host/application arguments while leaving pairing and credentials in Moonlight.
- Search and filter by favorites, availability, and platform.
- Sort by favorites/title or recent play history.
- Edit validated titles, platforms, and command-line arguments without changing game identity or executable paths.
- Show launch targets in Explorer, relocate missing targets, and disable launch actions when files are unavailable.
- Record localized last-played status and clear it without changing favorites, launch settings, or game files.
- Persist the library atomically under `%LOCALAPPDATA%\GameBoxOS`.
- Export normalized JSON backups and restore validated backups with explicit confirmation.
- Enrich existing local entries from a bounded HTTPS catalog while preserving local launch trust.
- Reject insecure endpoints, redirects, embedded credentials, oversized responses, invalid timeouts, and unsafe launch extensions.

Enter plays the selected game; Ctrl+F searches; Ctrl+O adds a game.

The companion never downloads game content, deletes installed files, reads Android save data, or stores streaming credentials. Moonlight remains a user-installed runtime; GameBox stores only its executable path and validated stream arguments.

## Remaining Windows work

Additional PC-runtime integrations, installer packaging/signing, and physical Windows acceptance testing remain before the companion is considered complete.

CI publishes the self-contained ZIP with a SHA-256 file and a validated JSON provenance manifest containing the exact artifact name, hash, byte size, runtime, self-contained flag, and source commit.

## Build and test

```powershell
dotnet run --project windows/GameBox.Windows.Core.Tests/GameBox.Windows.Core.Tests.csproj -c Release
dotnet publish windows/GameBox.Windows/GameBox.Windows.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
```
