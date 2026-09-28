# Legal homebrew test matrix

This matrix is for validating GameBox's import, metadata, installation, emulator
handoff, input, save, uninstall, reinstall, and restore flow. It is not a game
distribution list. A fixture may be used only when its upstream project explicitly
permits the binary/source use, or when the binary is built locally from a repository
with a compatible license. GameBox must record the upstream URL, license, build or
release identifier, and SHA-256 before using a fixture in an acceptance run.

## Ready candidates

| Platform family | Candidate | Format / adapter | License evidence | Validation status |
| --- | --- | --- | --- | --- |
| NES / Famicom | Galaxy Patrol | `.nes` / RetroArch FCEUmm | Existing repository MIT-attributed fixture | Already validated on Honor and API-35 through gameplay, input, save-state, uninstall, reinstall, and restore |
| Game Boy | Bustfree! | `.gb` / approved RetroArch core | MIT in the upstream repository | Candidate; obtain a release or build locally, then pin checksum |
| Game Boy Color | Brickster | `.gbc` / approved RetroArch core | RetroBrews states the listed ROMs are approved for free distribution on its site/project | Physical Honor flow completed through Store → Cave Story → Import your copy → DocumentsUI; app-private import registered as `Game Boy Color` in Library, 32,768 bytes, SHA-256 `24e44e274f8ee77a838d0fc58a607521021b3de122a968f359994c6f7c773a45`; official ARM64 Gambatte core was sideloaded through RetroArch's documented debug path, then GameBox launch rendered the real Brickster session, accepted input, saved and loaded a Gambatte state, cleaned its temporary handoff copy on return, and completed content-only uninstall → re-import → relaunch → state restore on Honor |
| Game Boy Advance | Solar Guard | `.gba` / approved RetroArch core | GPL-3.0 for source and original assets; Open Game Art attribution remains required | Candidate; build/release and asset notices must be captured |
| Nintendo 64 | FissionFailure64 | `.z64` / M64Plus FZ or RetroArch | Upstream repository is marked MIT and describes the ROM as N64brew homebrew | Candidate; validate the release checksum and M64Plus FZ handoff |
| Dreamcast | Drift Los Angeles | `.cdi` / Flycast | Upstream repository states original projects are MIT; KallistiOS attribution applies | Candidate; validate CDI release and Flycast launch/save behavior |
| Sega Genesis / Mega Drive | Scorpion Illuminati | Genesis ROM / RetroArch | Project site states Artistic License | Candidate; verify the downloadable artifact and checksum before import |
| Nintendo DS | DScraft / DS-Homebrew ecosystem | `.nds` / melonDS | DS-Homebrew documents DScraft as homebrew; obtain the author's license and release checksum before use | Source lead only; no binary approved yet |
| SNES / Super Famicom | SNES Studio generated sample | `.sfc` / Snes9x | Project documents a reproducible ROM build and CC0 sample art; verify the generated game/source license | Build lead only; generate locally before acceptance |
| Game Boy Advance | RetroBrews GBA collection | `.gba` / mGBA | The project states listed ROMs are approved for free distribution on its site; verify the individual game's license and checksum | Source lead only; no binary approved yet |

## Source-led candidates

| Platform family | Source | Why it is useful | Remaining gate |
| --- | --- | --- | --- |
| PSP | [PSP Homebrew Database](https://github.com/pspdev/homebrew) | The catalog records per-entry author, source, license, release URL, and checksums for EBOOT packages | Select a game entry whose record has a clear license and a compatible release; then test PPSSPP homebrew-folder handling |
| Nintendo 3DS | [Mars3DS](https://github.com/BotRandomness/Mars3DS) | A native 3DS homebrew shooter with documented `.3dsx` build output and optional CIA packaging | The repository page does not provide enough license evidence for a bundled fixture; verify license and build/release artifact first |
| Wii | [SuDokuL / Open Shop Channel](https://oscwii.org/library/app/SuDokuL) | Provides a documented homebrew download and `boot.dol` for Wii/vWii | Confirm the developer's license and whether the Dolphin handoff should treat it as an application rather than a game |
| PSP smoke test | [hello-world-psp](https://github.com/filfreire/hello-world-psp) | Small EBOOT build that exercises PSP homebrew packaging and PPSSPP launch without commercial assets | It is an application smoke fixture, not a gameplay/save fixture; use only for handoff diagnostics unless a game fixture is selected |

## Not yet approved for acceptance binaries

PS1, PS2, PS3, GameCube, Switch, Xbox, PC Engine, WonderSwan, Neo Geo, arcade, and
other supported families still need a specific homebrew/open-source project with a
clear license and a reproducible binary or build recipe. A source port that requires
commercial game assets is not a legal fixture by itself. A decompilation or engine
repository that says “bring your own ROM/assets” remains a source-only candidate.

## Acceptance procedure

1. Select a candidate with explicit license and source/build evidence.
2. Build or download only the permitted release into a temporary fixture directory;
   do not modify the user's `games/` directory without explicit authorization.
3. Record the exact artifact name, upstream revision/release, license notice, and
   SHA-256 in the audit.
4. Run the intended GameBox journey: import or authorized download, metadata/artwork,
   install verification, emulator detection/configuration, launch, input, save, exit,
   return, content-only uninstall, reinstall, and save restoration.
5. Report emulator-family and hardware results separately. A successful import or
   intent dispatch is not gameplay or save-compatibility evidence.

The source leads above are deliberately not acceptance binaries. They identify where
to obtain or build a legal fixture while preserving the approval gate for per-game
license evidence, exact artifact, and checksum. No commercial ROMs are needed for
the GameBox flow tests.
