# Nuvio TV integration plan

## Target

Replace GameBox OS's custom Live TV and TV Mode with Nuvio-based TV features inside the GameBox experience. Keep Gaming and TV as clear first-class areas, with a shared GameBox identity, settings entry, display behavior, and controller navigation.

## Current repository evidence

- GameBox's TV surface is primarily `TvModeScreen.kt` and `tvmode/TvModeApps.kt`. `GameBoxApp.kt` selects TV Mode for an external display and wires its back, remote text, settings, playlist, favorites, and channel callbacks.
- TV settings are stored in GameBox DataStore: installed-app favorites, playlist URI, favorite channel keys, last channel, and home sections. A replacement must migrate or deliberately preserve these values before removing their keys.
- The project currently builds one Android app (`:app`) and a companion remote (`:companion-remote`). It already has Media3 playback dependencies, but that does not provide Nuvio's repositories, lifecycle, guide, player, or setup flows.
- No root `LICENSE` is tracked. GameBox is public, and the repository's tracked history currently identifies the project owner's two GitHub author identities. Confirm contributor rights and document the licensing choice before distributing a combined derivative.
- Nuvio's current `dev` branch declares GPL-3.0. Its repository tree contains roughly 4,600 paths and its reported checkout size is about 596 MB. It is a complete Android TV launcher, not a small drop-in Live TV library.
- Nuvio's app build uses namespace/application ID `com.nuvio.tv`, compile/target SDK 36, min SDK 24, AGP 8.13.2 and Kotlin 2.3.0. GameBox currently uses app ID `com.gamebox.os`, min SDK 26, AGP 9.3.0 and Kotlin 2.3.21.
- Nuvio includes additional modules and bundled native/media artifacts, including VPN modules, MediaServiceCore modules, FFmpeg decoding, and player libraries. Its manifest also declares numerous permissions and components. Importing the entire launcher without a feature and permission review would add substantial size and behavior beyond GameBox's TV requirements.
- A separate Nuvio 1.2.4 APK was previously installed on the HONOR HDMI display for a compatibility trial. MagicOS presented it as a floating window; fullscreen, return-to-GameBox, physical controller input, and playback were not verified. That binary trial is not evidence that source integration works.
- The source baseline is pinned for this investigation to commit `b7028012fbc19379974a005ae3ae79ac6787822d` on Nuvio `dev` (24 November 2026).
- A standalone upstream `fullDebug` build succeeded from that commit after using Temurin 17, the pinned Go 1.27.3 compiler, Android NDK 29, and a temporary debug-signing adjustment in the ignored clone. The test-build universal APK was 378,028,495 bytes and contains multiple ABIs, so this number is not a release-size comparison. The build took 10m31s and emitted extensive Kotlin warnings plus D8 startup-profile warnings; treat it only as a source-build baseline.
- The generated APK requests permissions beyond GameBox's current TV flow, including package installation/update, secure settings, TV-provider read/write, microphone, exact alarms, external storage/audio, phone state, and foreground-service permissions. The merged manifest must be reduced to the permissions required by retained features, not copied wholesale.
- GameBox now has a root GPL-3.0-only `LICENSE` and the README links to it. This is the project-level license decision for the planned combined source; individual dependencies, native binaries, images, and datasets still require a separate release inventory and their own notices.
- The source-level host integration combines GameBox's Kotlin sources with Nuvio's full app, routes the Nuvio drawer to GameBox Gaming, and keeps the Gaming/TV destinations in one host activity. GameBox branding and launcher artwork are applied in the integration patch; required upstream attribution remains.
- The integration is reproducible from `integration/nuvio/Build-GameBox.ps1` plus `integration/nuvio/gamebox-nuvio.patch`, pinned to the upstream revision above. The patch applies cleanly to a fresh checkout. The arm64 `fullDebug` APK rebuilt successfully on 25 November 2026 with Temurin 17 in 5m18s. Gradle reports upstream Kotlin and D8 startup-profile warnings; the build completes.
- The debug APK was installed earlier on the HONOR as `com.gamebox.os.nuviointegration`, without clearing its data. This is a parallel test package; the existing GameBox app has not been replaced. ADB now lists no connected device. The phone answers on the local network and mDNS advertises wireless debugging, but its advertised ADB TLS port refuses connections. The merged UI and physical controller behavior therefore remain unverified on the HDMI TV.

## Implementation sequence

### 1. Lock down the upstream baseline and obligations

- Pin the exact Nuvio commit to integrate and record the upstream URL, branch, version, and license.
- Inventory source, assets, native libraries, generated files, bundled libraries, and licenses for the selected Nuvio features. Do not assume the top-level GPL notice covers every bundled component or asset.
- The project-level license is now GPL-3.0-only. Complete the release-specific dependency/asset audit, preserve upstream attribution and notices, and provide the corresponding source and build instructions. Do not publish a combined binary until the full inventory is resolved.
- Inspect Nuvio's TV-specific build flavors and feature boundaries before choosing a host application. Prefer retaining only the dependencies and permissions needed for the selected TV experience.

### 2. Prove the integration shape with a disposable build spike

- Determine whether Nuvio exposes Live TV as reusable Compose/navigation modules or whether its player, repositories, dependency injection, and application services are tied to its launcher `Application` and `MainActivity`.
- Compare two source-level prototypes: embed the Nuvio TV flow in GameBox's app, or use Nuvio's application as the host and port GameBox's navigation/library as a module. The accepted result must be one installed GameBox-branded experience, not two independently launched apps.
- Build the smallest Nuvio-backed screen inside the selected host. Verify startup, navigation, one playlist setup, channel selection, playback, Back, and return before beginning large-scale migration.
- Record dependency conflicts, APK size, startup cost, required permissions, account/server dependencies, and any upstream changes needed to support GameBox navigation.
- Upstream standalone source build is proven. The next spike must test combining the upstream Hilt `NuvioApplication` lifecycle with GameBox's `GameBoxApplication`, then bring up the Live TV guide from a GameBox-owned TV entry inside one APK. Keep all experiments in an isolated branch/worktree because the current checkout contains unrelated uncommitted work.
- Combined Kotlin compilation and arm64-only APK packaging, including the Application inheritance prototype, now pass in the ignored clone. Commons IO resolves to a single compatible Maven version. The connected HONOR reports `arm64-v8a`, with an active external display at 1920×1080. The spike still takes Nuvio as host and contains both old TV surfaces; do not copy it wholesale into GameBox.
- A 151 MB arm64 debug APK with package ID `com.gamebox.os.nuviointegration` is installed alongside the existing apps on the connected HONOR PGT-N19. The combined app's Gaming destination renders GameBox Home and its game cards on the 1920×1080 HDMI display. The initial artwork crash was fixed in the ignored prototype by replacing its Kotlin resource bridge with Java static resource fields; a fresh logcat check after the latest navigation showed no `FATAL EXCEPTION`, `NoSuchMethodError`, or AndroidRuntime failure.
- On the HDMI display, tapping GameBox's top-right TV control navigated to Nuvio's Live TV guide in the same app task. The guide reached its expected empty state (“No playlists yet”) and offered the Live TV settings action. No playlist is configured, so channel playback remains untested. An Android Back key event did not visibly return from the guide, so the return route remains unverified and needs investigation.
- HONOR Desktop displays the app at the full 1920×1080 content size after manual maximize, but the system dock remains visible. Initial launch also opened in a floating desktop window; automatic borderless fullscreen is unresolved. The Gaming screenshot showed low-contrast dark text against the dark GameBox background, so theme integration needs correction before UI acceptance.
- The source-level Gaming-to-TV prototype compiles and packages in the ignored clone, but has not been adopted into the tracked GameBox app. It adds Gaming to Nuvio's drawer, renders GameBox's `GameBoxApp`, and routes its TV control to Nuvio Live TV. Controller focus, back navigation, automatic fullscreen, app branding, settings, authorized playback, and existing-data migration still need verification.
- Branding work in the ignored integration clone now applies GameBox's dark navy and blue/purple palette to the TV UI, changes the shared wordmark and launcher icon/banner to GameBox, and labels the two main destinations `GameBox Gaming` and `GameBox TV`. Visible strings in the app have been rebranded; upstream attribution and license notices remain in the credits screen as required. These edits are not yet in the tracked GameBox application.
- On 25 November 2026, an ADB check found the HONOR PGT-N19 connected. The integration clone could not be rebuilt for this branding pass: Android Studio's bundled Java 25 is newer than the Kotlin build-script parser in this checkout, which fails while parsing `25.0.2`. No new APK was installed and the changed branding was not visually verified on the TV in this pass. Re-run the arm64 `fullDebug` build with a compatible JDK before hardware acceptance.

### Latest acceptance check — 26 November 2026

- Rebuilt the pinned integration with Temurin 17 as an arm64 `fullDebug` APK and installed it over `com.gamebox.os.nuviointegration` without clearing app data.
- On the physical HONOR PGT-N19 HDMI display at 1920×1080, the controller was reported as connected. A physical D-pad Up moved focus from Home to GameBox Gaming; an ADB-generated confirm event opened Gaming in the same app task. Physical A-button activation remains unverified.
- Gaming now uses the shared dark navy and blue/purple colors, larger TV-style hero/card radii, and left content inset so the host navigation rail no longer covers the GameBox logo or hero text. The GameBox home screen and controller status were visually checked on the TV.
- HONOR still opens the activity as a desktop window after a cold launch. Manually maximizing it hides the bottom dock; automatic borderless startup is still unresolved.
- This pass did not configure or play an IPTV source, verify GameBox-to-TV playback/Back, migrate old TV settings, remove the old GameBox TV Mode implementation from the tracked app, or complete the dependency and license inventory. Do not treat the combined app as production-ready yet.

### 3. Build the shared GameBox experience

- Add clear Gaming and TV destinations in GameBox navigation. TV must open within the same app task and use the GameBox name, icons, colors, typography, and user-facing copy while retaining required Nuvio notices.
- Reuse Nuvio's supported Live TV setup, playlist, guide, channel browsing, and playback behavior. Expose optional TV features only after identifying their service, account, permission, and storage requirements.
- Make focus and controller movement predictable across the Gaming/TV switch, dialogs, search, channel lists, player controls, and Android Back. Support touch and remote text input without trapping focus.
- Preserve the external-display-first behavior on the HONOR phone connected to HDMI. Verify fullscreen sizing, no floating window, Home/recents behavior, and return from playback on that hardware.

### 4. Migrate state and remove the duplicate implementation

- Define a migration for the existing playlist URI, channel favorites, last channel, app favorites, and home-section preferences. Preserve data that Nuvio can import; otherwise explain the one-time outcome and keep a recoverable backup for user-owned playlist configuration.
- Switch the GameBox TV route only after the Nuvio-backed route passes the prototype gates.
- Remove the old `TvModeScreen`, custom channel/EPG/parser/playback code, obsolete TV-specific preferences and UI, and their tests/assets/dependencies once no callers remain.
- Retain the companion remote only where it serves shared Gaming/TV app navigation or text entry; remove old TV-specific command assumptions that Nuvio does not use.
- Update documentation, UAT, dependency notices, and release notes to describe the new setup and any provider/account requirements.

### 5. Verify and release

- Run the Android unit, instrumentation, lint, dependency/license, and release-build checks that cover the changed surface. Verify that GameBox gaming flows remain intact.
- On the HONOR Magic6 Pro connected to the TV, test fresh setup, playlist and guide configuration, channel browsing, successful playback from an authorized source, playback error recovery, controller focus, physical input when the controller is available, Back, app relaunch, and switching between Gaming and TV.
- Use an emulator for development checks only when the physical device or TV is unavailable. Clearly label emulator-only results and keep hardware acceptance open until verified.
- Measure final APK size and cold-start behavior against the baseline. Review all added permissions and services and remove any not required by the integrated features.
- Record actual pass/fail/block status. Do not mark the work production-ready while licensing, playback, hardware navigation, migration, or release verification remains incomplete.

## Initial acceptance gates

1. A single GameBox installation presents Gaming and TV without launching the separate Nuvio app.
2. TV setup, channel browsing, guide display, and authorized playback work in the chosen host.
3. Back and controller focus return to the expected GameBox destination on the HONOR-to-TV display.
4. Existing TV configuration is migrated or preserved with a documented outcome.
5. The old duplicate TV implementation is removed only after replacement coverage is demonstrated.
6. Licensing, notices, source availability, permissions, dependencies, APK impact, and release status are documented and verified.
