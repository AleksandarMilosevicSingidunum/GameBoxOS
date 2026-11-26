# GameBox + Nuvio source integration

This integration layers GameBox Gaming into the pinned Nuvio TV host and applies GameBox branding to the combined app. The standalone upstream project remains separately licensed under GPL-3.0; retain its copyright and third-party notices when distributing a derivative.

## Rebuild

On Windows with Java 17 and Android SDK installed, run from the project root:

```powershell
.\integration\nuvio\Build-GameBox.ps1
```

To install the debug build on an ADB-connected device:

```powershell
.\integration\nuvio\Build-GameBox.ps1 -Install
```

The script checks out Nuvio revision `b7028012fbc19379974a005ae3ae79ac6787822d`, applies `gamebox-nuvio.patch`, and builds an arm64 test APK. It leaves the checkout under ignored `tmp\nuvio-upstream`. The patch intentionally excludes ROM/download fixture files. Use `-Checkout <path>` to provide another clean checkout at the same pinned revision.

## Current status

- [x] GameBox Gaming sources are integrated into the Nuvio host app.
- [x] GameBox and TV entry points share GameBox branding.
- [x] Debug APK builds successfully with Temurin Java 17.
- [x] Debug APK installed on HONOR without clearing app data.
- [x] Updated GameBox Gaming layout captured on the HONOR HDMI display at 1920×1080. Gaming hero shapes, shared corner tokens, GameBox theme, and left-side content inset now follow the TV shell more closely.
- [x] Physical controller check: with Home selected, D-pad Up moved focus to GameBox Gaming. An ADB-generated confirm/center event then opened Gaming in the same app. The HONOR reports the Wireless Controller connected; a physical A-button activation remains to be checked.
- [ ] Verify GameBox-to-TV playback and return behavior with an authorized playlist. No channel source was configured for this check.
- [ ] Verify automatic borderless launch. HONOR still starts the app in a desktop window; maximizing it hides the bottom dock, but this is currently a manual system action.
- [ ] Complete the migration, old TV implementation removal, permissions/dependency/license review, and release build. These remain open until the single-app flow passes on hardware.

The install used package `com.gamebox.os.nuviointegration` to avoid replacing the existing GameBox installation before TV acceptance. This is a test package, not the final production package.
