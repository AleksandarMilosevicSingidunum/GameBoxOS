# Android Blueprint UI pass — 7 September 2026

This pass uses the Blueprint's Home, Library, Store, Details, Downloads, Media,
PC Hub and Settings mockups as layout references. It is not a claim of pixel
identity: available games/artwork, Android system insets, font scaling and phone
aspect ratios necessarily change the rendered result.

## Changes

- Neutral near-black surfaces, smaller glass panels and blue-to-purple focus borders.
- Compact header, live clock, actionable catalog/download shortcuts and controller hints.
- Home hero beside separate device panels; portrait collection beside quick launch;
  short landscape game strip below.
- Narrow console rails, actual cached counts, working favorites/sort/installed filters,
  denser Store/Library covers and real storage/save statistics.
- Artwork-first game details; emulator settings are expandable. Discovered-game
  details adapt to phones and screenshots can be enlarged.
- Full-opacity artwork with bounded decoding, memory/disk cache and original geometric
  placeholders when artwork is unavailable. Placeholders are not official cover art.
- Media/PC launcher tiles retain their scalable brand marks. Status and sessions
  reflect available data; invented watch history, streaming titles, host IPs,
  player levels and points have been removed.
- Phone safe-area padding, large-display scaling and scrollable console/settings rails.
- Unified mouse hover, touch press and keyboard/controller click sources.

## Verification

The Android workflow builds the APK and runs unit/instrumentation tests.
Its separate phone and DeX visual jobs install the production APK on API 35
emulators and capture the real MainActivity, game details and all seven tabs.
Screenshots and instrumentation/logcat output are published as
`gamebox-ui-phone-<sha>` and `gamebox-ui-dex-<sha>` artifacts.

Profile sizes: phone 1080 × 2400 at 420 dpi; DeX 1920 × 1080 at 160 dpi.
Visual capture is not equivalent to a pixel-difference test or physical DeX,
controller, emulator-launch or streaming acceptance testing.

## Playing games

Open a game in Library and choose Play (Play Now on the wide details panel).
The game must be installed/imported and its compatible emulator configured in
Game settings & emulator. TheGamesDB provides discovery metadata and images,
not commercial game downloads. A debug APK does not include emulator cores,
console firmware, keys or commercial game files.
