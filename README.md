# GameBox OS

GameBox OS is a controller-first Android living-room shell for a docked phone. It presents one coherent interface for a local game library, authorized or homebrew catalog, media launchers, streaming tools, and Samsung DeX.

The current 0.1 day-zero shell includes:

- Kotlin and Jetpack Compose
- 16:9 landscape-first layout
- controller-focusable navigation and game cards
- Home, Library, and Store mock screens
- explicit install-state model and safe fixture content
- verified app-private test installation with WorkManager
- save-safe content uninstall, reinstall, and user-controlled backup import/export
- allowlisted emulator handoff with checksum re-verification
- persistent catalog, download, save, and play-session state
- automated unit tests and debug APK builds

## Run

Open the repository root in the current stable Android Studio, install Android SDK 36, sync Gradle, and run the app on an emulator or Galaxy A53. Pair a controller and verify that core navigation works without touch.

Every CI run also produces a 14-day `gamebox-os-debug-<commit>` artifact containing the unsigned debug APK and its SHA-256 checksum. Use debug artifacts for development testing only; release signing is intentionally not configured yet.

GameBox is an Android and DeX shell, not a custom ROM or emulator. Sources are limited to user-owned backups, homebrew, freeware, and otherwise authorized content.
