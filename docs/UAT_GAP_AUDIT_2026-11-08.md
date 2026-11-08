# GameBox OS full UAT / QA gap audit

**Date:** 8 November 2026
**Scope:** Android gaming flows, standalone emulators, TV Mode, Live TV, controller and companion Remote, downloads/import/library/storage, Windows companion, and release readiness.
**Method:** Five parallel read-only reviews of feature areas and existing evidence, reconciled against `docs/UAT.md`, the legal homebrew matrix, TV/emulator matrix, source and current HONOR HDMI observations. This is an evidence audit, not a claim that every individual feature was executed end to end.

## Executive result

GameBox has substantial implemented functionality and useful unit/instrumentation coverage, but it is **not production-ready**. The highest impact confirmed failures are: no tested physical gamepad acceptance, HONOR's bottom Dock remains on GameBox, app handoff can retain OEM window chrome, and the tested Serbian 1080p IPTV candidates produced no decoded frames (10 failures, 0 passes). Key end-to-end workflows—including browser download to import, full game lifecycle, second-phone Remote, HDMI reconnection, and Windows companion—remain unverified. A feature existing in code or passing a unit test is not treated as a UAT pass.

The existing UAT checklist currently has **122 cases: 5 Pass, 22 Partial, 2 Fail, 10 Blocked, and 83 Not run**. The two failed rows are TV-06 (OEM Dock/fullscreen) and TV-08 (app handoff/fullscreen). The checklist counts historical recorded test cases; the category tables below also identify material acceptance gaps not represented by a single checklist row.

The five reviews covered: (1) games, console/emulator coverage; (2) discovery, downloads, import, library, metadata; (3) TV Mode and Live TV; (4) controller, Remote, Windows companion; (5) release, installation, migration, security, and cross-cutting QA. Conflicts were resolved in favor of dated target-device evidence and explicit limitations in the repository's acceptance documents.

**Live TV evidence update (8 November):** Ten distinct public international broadcaster feeds were subsequently played in GameBox on the HONOR HDMI output; all ten showed moving video and the in-app player reported decoded sizes of at least 1920×1080. This proves the current player path for those feeds, not Serbian channel continuity. F-01 remains open specifically for Serbian broadcasts because RTS 1 and Prva still failed to decode during the earlier device run. Detailed channel-by-channel observations and local captures are in the 8 November section of `docs/UAT.md`.

## Confirmed failures / highest priority

| ID | Severity | Feature | Evidence and impact |
| --- | --- | --- | --- |
| F-01 | Critical | Serbian 1080p Live TV | Ten RS playlist entries labelled 1080p were opened on the HONOR HDMI output; none yielded a decoded frame. Failures included DNS/NXDOMAIN, HTTP 403/bad status, network connection errors, and cleartext rejection. Playlist labels are not proof of video resolution. See dated evidence in `docs/UAT.md` and `docs/livetvserbia-evaluation.md`. |
| F-02 | High | Fullscreen / no Dock | GameBox fills the 1920×1080 HDMI display, but HONOR's separate 43 px Dock overlays it after fresh launch/update. Selecting HONOR's “Open in full screen mode” hides Dock only for a tested emulator session; it returns after relaunch. |
| F-03 | High | Emulator app handoff fullscreen | On HONOR, standalone app launch can use MagicOS `hw-pc-freeform`; title/caption chrome or Dock remains, and user interaction with the OEM fullscreen control has been required. Automatic borderless fullscreen is not established. |
| F-04 | High | Vimm search coverage | Vimm has no vault mapping for 3DS/Switch. The requested 18-console preset remains intact, but search is now disabled when the selected console has no mapping. Those consoles remain unsupported by Vimm search; the UI no longer launches a known-empty search. |
| F-05 | High | Game controller acceptance | Physical controller was not active as an Android GAMEPAD/JOYSTICK during the current run. Synthetic ADB/Remote evidence and isolated game observations do not prove physical D-pad, confirm/back, keyboard, or gameplay controls. |
| F-06 | High | Emulator compatibility | N64 Gopher64 showed rendering corruption; Mupen64Plus AE hit renderer/startup trouble. JustGBA fallback crashed with SIGSEGV. Dolphin launched without a configured game, so Luigi's Mansion is not validated. |
| F-07 | High | Serbian broadcast continuity | RTS 1 and Prva direct streams repeatedly failed on this HONOR/network. A working TDI Radio TV stream and authorized 1080p VOD prove the player path only, not Serbian TV service availability. |

## Partial or blocked functionality

### Game discovery, download, import, and library

| Area | Status | Missing proof / observed limitation |
| --- | --- | --- |
| Vimm preset and console filters | Partial | The requested 18-console preset remains present. Search is disabled for unsupported mappings, including 3DS/Switch. Current full TV acceptance for mapped-console search, details, art, and download was not completed. |
| Search correction, result grouping, artwork, details | Partial | Parser/UI logic and tests exist, but end-to-end live Vimm result accuracy, artwork load, focus, and navigation across the full preset were not accepted on TV. |
| Vimm download handoff | Not run | No legal downloadable Vimm test item completed the whole browser → tracked download → durable manager → verified file → import path. Do not use copyrighted ROMs for acceptance. |
| Download manager / durable transfer | Partial | Queue visibility and recovery logic have unit coverage. New real transfer, pause/resume, cancel, server range mismatch, checksum failure, low storage, network loss, process death, and completed-item cleanup were not all device exercised. An interrupted IMPORTING record now has regression coverage, but no actual process-kill boundary test. |
| Archive import (ZIP/7z) | Partial | Focused archive/import tests now verify cancellation checks during scanning/extraction, partial output cleanup, and source archive preservation (20/20 focused tests passed). Representative SAF-picked ZIP/7z imports, progress/cancel UX, multi-disc sets, duplicate handling, source deletion, and post-crash cleanup are not all verified on hardware. |
| Move vs copy and folder structure | Partial | Managed destination and cleanup paths exist; complete byte/hash proof across supported source providers, failed move rollback, and all-console directory coverage remain open. |
| Library search, sort, filters, favorites, recents | Not run | The complete acceptance set has no current target-device run; individual rendering/state tests are not full TV operation evidence. |
| Metadata, box art, screenshots | Partial | Providers and cache/fallback code exist. Match quality, missing art, stale metadata, source failure, rate limits, and results across the entire imported library were not reviewed end to end. |
| Uninstall / preserve source / saves | Partial | One Pong-NDS payload removal (403,520 bytes) was observed with external source retained. A managed save was not present to prove preservation; exact file and save behavior across console profiles is not accepted. |
| External storage / migration | Not run | SAF permissions, migration plan, byte/hash verification, disconnect/reconnect, rollback, and recovery were not run as a complete acceptance workflow. |

### Console and emulator coverage

The following classifications describe available evidence, not marketing support. “Partial” means some rendering, handoff, or exit was observed but required controls/save/fullscreen/lifecycle steps remain open. Console rows in the existing 18-console matrix are not filled as full acceptance passes.

| Console / platform | Current emulator path | UAT result |
| --- | --- | --- |
| Atari 2600 | Atari2600.emu | Not run per-console |
| Dreamcast | Redream | Partial: MIT Chroma Circuit rendered; physical A advanced once; import/uninstall evidence exists. Save/restore, full controls, fullscreen, and full reinstall lifecycle unverified. |
| Game Boy | GBC.emu | Not run separately |
| Game Boy Color | GBC.emu | Partial: RayTracer rendered; repeatable physical D-pad scene change was not established. |
| Game Boy Advance | GBA.emu; JustGBA fallback | Partial: MIT @rkanoid gameplay rendered; Start and D-pad Right observations; JustGBA fallback SIGSEGV. Physical confirm/menu/save/restore not established. |
| Game Gear | MD.emu | Not run |
| Genesis / Mega Drive | MD.emu | Not run |
| Master System | MD.emu | Not run |
| NES | NES.emu | Partial: legal MIT Galaxy Patrol installed, rendered on HDMI, and returned to the same details screen. Inputs/save not verified; Dock/fullscreen not persistent. |
| SNES | Snes9x EX+ | Partial: MIT Gothicvania title screen/return and source preservation observed; gameplay controls/save not verified. |
| Nintendo 64 | Gopher64 / Mupen64Plus AE | Fail/Partial: Gopher64 rendering corruption; Mupen startup/renderer issue. No successful controlled gameplay/save acceptance. |
| Nintendo DS | melonDS | Partial/uncertain: Pong-NDS has inconsistent boot/hash notes in historical evidence; resolve fixture identity and boot record, then test physical controls/save. |
| Nintendo 3DS | Azahar | Partial: BlockBlast board rendered after display-targeted touch; physical controls/save/install lifecycle unverified. |
| GameCube | Dolphin | Not run: Dolphin opens but no authorized test game was configured. Luigi's Mansion does not have a successful launch result. |
| Wii | Dolphin | Not run: no authorized Wii fixture/gameplay acceptance. |
| WiiWare | Dolphin/mapping | Not run: mapping exists; no WAD/game tested. |
| PlayStation 1 | DuckStation | Not run: setup observed; legal game gameplay, controls, save and lifecycle not tested. |
| PlayStation 2 | ARMSX2 | Not run: installation/adapter only; no gameplay/TV acceptance. |
| PSP | PPSSPP | Partial: title menu from user-provided commercial archive observed; not a legal acceptance fixture. No claim of lawful coverage, physical controls, or saves. |
| Xbox | No verified Android emulator route | Unsupported/unverified |
| Xbox 360 | No verified Android emulator route | Unsupported/unverified |
| PlayStation 3 | No verified Android emulator route | Unsupported/unverified |
| Nintendo Switch | No verified Android emulator route | Unsupported/unverified |

No console currently has the complete verified chain: legal fixture install/import → physical controller gameplay → managed save create and restore → exit/details return → content-only uninstall → source/save preservation → reinstall → fullscreen TV operation.

### TV Mode, Live TV, app hub, and remote

| Area | Status | Remaining gap |
| --- | --- | --- |
| TV dashboard / focus navigation | Partial | Dashboard renders on real 1080p HDMI and synthetic D-pad focus works. Physical gamepad and complete focus graph/remote input are not accepted. |
| TV fullscreen and safe area | Fail | HONOR Dock overlays the screen; app immersive flags do not control separate OEM Dock. Emulator fullscreen choice did not persist after relaunch. |
| HDMI disconnect/reconnect / launcher | Not run | No current controlled acceptance proving GameBox restoration, focus recovery, and normal HONOR desktop switch after HDMI cycles. |
| Quick launch / More apps / app return | Partial | Some pinned-app and Back return paths worked. Arbitrary installed apps, repeated return, actual fullscreen, and TV-mode handoff are not accepted. |
| App in-app presentation | Partial | Some services are web-backed and remain in GameBox; DRM/service restrictions and playback do not support treating every app as in-app. Full matrix for Netflix, Prime, YouTube, EON/RTS Planet, VLC and installed native apps not completed. |
| Live TV playlist / deduplication / favorites | Partial | Same guide ID entries with quality suffixes now merge into one channel while retaining distinct stream headers. On HDMI, All channels displayed 50 entries; selecting Travel filtered to RTG int., All categories restored the list. Favorites persist; broader duplicate/favorite/navigation coverage remains open. |
| Channel sort by Serbian popularity | Partial | Ranking covers a selected set of national channels rather than a measured comprehensive Serbian popularity order. |
| EPG / now-next | Partial | Parser and boundary logic have tests; one RTS 1 programme boundary advanced correctly. A guide status/retry action now explains failures and retries without discarding the channel list. The current in-place HDMI build showed guide data, but the failure/retry path was not forced on hardware; EPG metadata does not prove stream playback. |
| Stream quality | Fail for tested Serbian HD sample | Ten RS entries tagged 1080p yielded zero decoded frames. Other source tags are not a substitute for decoded 1080p verification. |
| Stream errors and retry | Partial | Error classification has unit coverage. Retry now restarts the candidate sequence, but no successful alternate Serbian broadcast was verified. Header-required/RTSP sources and runtime DNS/cleartext/timeout recovery remain open. |
| Companion Remote pairing and remembered reconnect | Partial | Same-HONOR remembered reconnection and local route behavior were observed; complete second-phone pairing, reconnect, revoke/re-pair, authentication, and failure UX are not acceptance-tested end to end. |
| Remote command routing | Partial | Unit coverage verifies target priority/isolation and tracked-browser routing. Separate-phone concurrency and real WebView Download/Back navigation were not executed on HONOR. |
| Remote text/search | Partial | Text entry from the Remote app on the host HONOR was verified; second-phone use and complete search/app/channel flows remain open. |

### Controller, accessibility, and interaction

- **Physical controller: Blocked/Not accepted.** Latest inventory had no active GAMEPAD/JOYSTICK source. Historical events include minor stick movement and synthetic events, but do not establish correct physical D-pad buttons.
- Confirm (A), Back (B), Start/Menu, shoulders, analog sticks, keyboard entry/exit, in-game mappings, focus restoration after dialogs, and controller operation in each emulator remain partial or unrun.
- Accessibility semantics, reduced-motion behavior, text scaling, contrast/focus visibility, and TalkBack acceptance are not comprehensively run on target TV.
- Controller footer and on-screen keyboard tests in instrumentation are implementation coverage, not physical-input UAT.
- Shell key-repeat handling now ignores held L1/R1/Menu repeats while preserving D-pad repeat; the physical button path remains blocked pending an active controller.

### Windows Companion

All current `WIN-01` through `WIN-10` cases in `docs/UAT.md` remain **Not run** as production UAT. The scope includes clean start/discovery, local/Steam/GOG/Epic library scanning, edits/launch, safe file management, shortcut creation/removal, backup/restore, pinned/authenticated Android pairing, managed-save transfer/conflicts, catalog sync, and ZIP/MSIX contents/signing/rollback. Earlier development tests/builds do not replace a supported-runtime clean-machine and real-service acceptance run.

### Release, update, security, resilience, and operations

- Production signing, signed provenance, Play/internal distribution, update-channel installation, upgrade from supported user data, and rollback rehearsal are not complete. Android release assembly is currently unsigned in the local developer path.
- Windows signed distribution and rollback are not verified. Android versionCode/tag strategy means “publish the old tag” alone must not be assumed to be a valid downgrade; verify the documented install/rollback workflow on a seeded device before release.
- Full database migration matrix on a clean seeded device, backups, diagnostics sanitization review, permission denial/regrant, offline/low-space behavior, crash/process death recovery, and external-storage disconnect are not a completed release run.
- Sustained charging/thermal soak with the final dock, external display, gameplay, storage, and network workload is not complete.
- No production sign-offs recorded in `docs/UAT.md` / release checklist.

## Implemented fixes in the active worktree (not release acceptance)

- Startup reconciliation reschedules durable manual imports left in `IMPORTING`; regression coverage also preserves an active WorkManager chain.
- Live TV Retry resets to the primary candidate so the alternate sequence can run again.
- The tracked source browser registers as the active Remote target on its display; authenticated Select/Back route to browser actions/history.
- The above changes have focused regression coverage; the full Android JVM suite passed in the current run. Browser Remote instrumentation was not run on HONOR. Do not infer hardware acceptance from these fixes.

### Follow-up fixes from ten-agent implementation pass — 8 November 2026

- Archive import cancellation now checks during ZIP/7z enumeration and extraction; cancellation removes partial output and preserves the original archive. Focused import suites passed 20/20.
- Browser download startup failures from secure-cookie storage or Android user-initiated job scheduling now remain as visible failed/retryable records with URL and ID retained. Covered in the combined 899-test Android JVM pass.
- Vimm keeps the requested 18-console preset, but disables search for consoles without a known vault mapping. Typo correction now prefers a uniquely most common result title token and skips ties.
- Live TV groups duplicate same-guide-ID quality variants and retains each stream/header alternative. TV Mode adds guide failure status/retry and all-channel category filters; HDMI filtering was observed with display-targeted input.
- Controller shell actions ignore held-key repeats without swallowing D-pad focus repeats. No physical controller was active during the HDMI run.
- The crashing JustGBA GBA adapter was removed from launch choices; GBA.emu remains the supported path. Box-art candidate lookup uses the canonical provider title after a display rename.
- A revoked remembered Remote token now fails directly rather than broadcasting a needless discovery request. Windows Steam discovery now respects the registered custom Steam library path.
- Integrated checks: Android JVM 899 passed / 0 failed / 1 skipped; companion Remote 13/13; app lint passed; debug, Android instrumentation APK, and unsigned release builds passed. Windows core self-check passed and WPF build completed with 0 warnings/errors.
- Installed the Android debug build in place on HONOR and verified GameBox TV Home plus the All channels category filter on 1920×1080 HDMI. Captures are in ignored local `tmp/` evidence; this was not a physical gamepad or live-stream pass.

## Exit gates and recommended order

1. Restore physical gamepad availability; execute physical Home/TV focus, search keyboard, download controls, and per-emulator input mapping on HDMI.
2. Fix or formally accept HONOR Dock and standalone-app fullscreen limitations; verify fresh-launch and relaunch on the TV.
3. Stabilize the legal Serbian Live TV source; require decoded frames, measured resolution, repeat/retry, and EPG on at least ten selected 1080p channels before calling HD coverage passed. Current result is 0/10.
4. Run one legal homebrew lifecycle for every platform advertised as supported. Hide/remove unsupported Vimm/platform entries until a real compatible path exists; separately test each emulator's launch, physical controls, save/restore, exit, and fullscreen.
5. Execute download → durable manager → import ZIP/7z → verified destination/hash → archive cleanup → library metadata/art → play/save → uninstall/preserve source and saves; force process death/network/storage failures at safe boundaries.
6. Complete the HDMI reconnect/normal desktop switch, second-phone Remote pairing and remembered reconnect/revocation, and Remote Vimm browser flow.
7. Run Windows `WIN-01`–`WIN-10`, upgrade/migration, security/privacy, thermal/soak, signed release, and rollback rehearsal. Record evidence and owners in UAT; do not sign off with Blocked/Not run cases.

## Evidence boundaries

The current HDMI capture and device inventory are dated 8 November 2026. It shows TV Mode, retained RS playlist/favorites/last-watched state, unavailable guide, HONOR Dock, and no active controller. Existing repository captures and logs are referenced from `docs/UAT.md`; they are not copied here. Some earlier notes conflict (notably DS fixture/hash/boot and older NES lifecycle claims), so the current matrix deliberately marks those rows partial until reconciled. No copyrighted game download was performed for this audit.
