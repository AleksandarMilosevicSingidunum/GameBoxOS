# GameBox + Nuvio source integration

This integration layers GameBox Gaming into the pinned Nuvio TV host and applies GameBox branding to the combined app. The standalone upstream project remains separately licensed under GPL-3.0; retain its copyright and third-party notices when distributing a derivative.

## Rebuild

On Windows, install Temurin/OpenJDK 17, Go 1.21 or newer, Android SDK Platform 36, and Android NDK `29.0.14206865`. Gradle downloads the pinned Go 1.27.3 toolchain and Go modules on the first native build, so network access is required. If Go is installed outside `PATH`, set `NUVIO_GO` to the full path of `go.exe`.

Then run from the project root:

```powershell
.\integration
uvio\Build-GameBox.ps1
```

To install the debug build on an ADB-connected device:

```powershell
.\integration
uvio\Build-GameBox.ps1 -Install
```

The script checks out Nuvio revision `b7028012fbc19379974a005ae3ae79ac6787822d`, applies `gamebox-nuvio.patch`, and builds an arm64 test APK. It leaves the checkout under ignored `tmp
uvio-upstream`. The patch intentionally excludes ROM/download fixture files. Use `-Checkout <path>` to provide another clean checkout at the same pinned revision.

## Current status

- [x] GameBox Gaming sources are integrated into the Nuvio host app.
- [x] The shared sidebar is simplified to Gaming, Home, TV, and Settings. Gaming opens its overview directly; TV opens the live guide directly, with its secondary pages grouped underneath while browsing TV. Inside Gaming, the first tab is labeled Overview to avoid a second ambiguous Home; its remaining tabs keep Library, Store, Downloads, Media, PC Hub, and Gaming Settings together.
- [x] The obsolete GameBox TV Mode screens and controller keyboard are removed. The shared D-pad search keyboard remains available to GameBox Library and Store under a neutral component name.
- [x] Debug APK builds successfully with Temurin Java 17.
- [x] Verify the pinned integration patch applies and a fresh checkout builds the full arm64 debug APK, including the native VPN code.
- [x] Latest fresh-checkout APK installed to the separate GameBox test package on HONOR without clearing app data.
- [ ] Verify the live TV/Gaming UI on the HDMI display after the latest clean-checkout install (`1.2.0-test.474694+beta.3`): the phone is currently locked and the HDMI output shows HONOR's unlock prompt, so this install could not be visually accepted.
- [x] Updated GameBox Gaming layout captured on the HONOR HDMI display at 1920×1080. Gaming hero shapes, shared corner tokens, GameBox theme, and left-side content inset now follow the TV shell more closely.
- [x] HDMI navigation smoke check on the installed merged build: D-pad Up from Home focused Gaming, confirm opened the Gaming overview, and selecting TV opened the live guide directly. TV's secondary pages remained available under TV. These checks used ADB-generated keys, not the physical controller.
- [ ] Verify D-pad focus movement between GameBox tabs and physical-controller confirm input. The controller was reported disconnected during this check, and ADB-generated key events did not fully verify tab traversal.
- [x] ADB D-pad/confirm events opened the TV guide from the compact shared menu on the latest build; the guide rendered its channel list on the HDMI display. This did not verify stream playback or physical-controller input.
- [ ] Verify GameBox-to-TV playback and return behavior with an authorized playlist. No channel source was configured for this check.
- [x] HONOR accepted Android's fullscreen request on HDMI. The merged activity now has fullscreen 1920×1080 task bounds and the window caption is gone.
- [ ] Hide the remaining 43 px HONOR Dock overlay. Its “Global Taskbar” control is not listed by Settings search on this Magic6 Pro, so the vendor setting remains unchanged. A test combining deprecated immersive flags with `WindowInsetsController` did not hide the Dock and visibly clipped the page, so those flags were removed.
- [ ] Verify migration of saved playlists and channel choices from the retired GameBox TV Mode, including file-picker URI access on an upgrade install. The migration code compiles, but the focused preference test class currently fails on this Windows host when AndroidX DataStore renames its temporary preference file; all five cases in that class fail, including its existing tests.
- [ ] Complete permissions/dependency/license review and run a signed GameBox release build. Release signing now requires GameBox-owned credentials supplied through `GAMEBOX_RELEASE_*`; no upstream signing defaults are used.

The install used package `com.gamebox.os.nuviointegration` to avoid replacing the existing GameBox installation before TV acceptance. This is a test package, not the final production package.
