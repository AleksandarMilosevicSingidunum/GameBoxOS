# Production critical path — 8 September 2026

Implementation and validation are tracked separately. Green UI/build checks do
not prove working emulators, providers, saves or release signing.

| Priority | Work | Current evidence / next gate |
| --- | --- | --- |
| P0 | Honest first-run library and installation state | Merged in PR #267 after Android unit/instrumentation, phone/DeX screenshot and Windows build checks passed. Catalog claims no longer create installed or queued games. |
| P0 | One complete authorized game journey | Galaxy Patrol is bundled and hash-pinned. The connected Honor Magic6 Pro now proves GameBox install, real RetroArch/FCEUmm gameplay, controller-key dispatch, emulator save-state creation and restore, return, content-only uninstall, reinstall and relaunch. API-35 remains the reproducible emulator check; managed GameBox save synchronization is a separate gate. |
| P0 | Reproducible signed release and upgrade | Debug APK, API-35 UI tests, and a real API-35 `MainActivity` startup pass locally. Production signing, clean install/upgrade and rollback validation remain separate gates. |
| P1 | Remove simulated installation controls | Merged and automated checks passed in PR #267. Manual completion APIs removed; fake repository moved to unit-test sources. Details pause/resume uses the real download controller; in-progress actions open Downloads. Unavailable-source jobs report recovery guidance rather than simulate progress. |
| P1 | Durable download updates | PRs #269–#271 merged after their checks passed. Ordered Room writes read persisted rows, startup waits for database records, work identity distinguishes fast reinstalls, and remote recovery verifies current content while preserving uninstall state. Live provider transfer/recovery still needs separate evidence. |
| P1 | General game lifecycle | The Galaxy Patrol/RetroArch lifecycle is validated on the Honor and API-35; full supported-console gameplay, adapter-specific save ownership and physical external-controller validation remain pending. |
| P1 | Real providers | TheGamesDB transport/enrichment exists; live authenticated discovery/media, limits and failures require provider evidence. |
| P1 | Windows communication journey | Debug Android↔Windows pairing, authentication, configuration, managed-save transfer, content transfer, gameplay handoff and save-safe cleanup now have one complete Honor run; large-file, cancellation, disk-full, replacement, routed-network, signed MSIX/install and physical Windows acceptance remain separate gates. |
| P2 | UI interaction and populated-state coverage | PR #266 passed unit, instrumentation and real phone/DeX screenshot jobs. Populated provider, active downloads, large fonts and physical controller flows need broader coverage. |
| P2 | Hardware and external services | Physical DeX/controller/storage/thermal checks and authenticated cloud endpoints must be validated when resources are available; they do not block independent software work. |

Fresh physical Honor↔Windows checkpoint (27 September 2026): after reinstalling the
debug APK, the Windows Companion was re-enabled through the phone's Network settings.
The repository .NET 8 probes then verified UDP discovery, authenticated `GameBox / ready /
protocol 1` status, an 8-entry library response, and the authenticated configuration
round-trip. The clean reinstall intentionally reports all catalog entries as
`NOT_INSTALLED`; this evidence is separate from the earlier physical Galaxy Patrol
gameplay and managed-save mutation rehearsals.

The same session then used the repository's explicit live-save probe to upload and
download a 23-byte `galaxy-patrol` managed-save payload with matching SHA-256; the
authenticated library response reported `savePresent=true`. This is real managed-save
transport evidence, separate from content installation and emulator-owned save-state
continuity.

The repository's live-content probe also streamed the legal Galaxy Patrol fixture to
the Honor, and Android confirmed the 49,168-byte file and matching SHA-256. The
authenticated library response changed Galaxy Patrol to `INSTALLED` while retaining
`savePresent=true`; a direct app-sandbox hash/length check matched the client response.
The same imported file then launched through the Store details page into the real
RetroArch/FCEUmm activity on the Honor; controller key events were accepted and RetroArch
reported `Saved state to slot: 0`. RetroArch Close Content and Quit returned to GameBox.
GameBox's supported content-only uninstall confirmation retained saves/backups/metadata,
history and external emulator files while removing the 49,168-byte ROM. The final details
screen reported `Game content · not installed` and `1 managed save files · 0.0 MB`; direct
app-sandbox checks confirmed the ROM was absent while `files/saves/galaxy-patrol/save.dat`
remained with SHA-256 `17c75cabe91d5339d4237d4ccfca3a20d4eb78ff357355a274df8162cdac3c24`.
This closes the physical companion-import → gameplay → save-safe cleanup slice; cloud,
other adapters and external hardware remain separate gates.

The Windows probe then repeated the same authenticated content upload on the Honor with
the identical checksum. Both confirmations were 49,168 bytes with SHA-256
`97c1757ffd6a5bc1a591809b2b0f8988741f61f6abd82889c148ecae8a2f471f`, and the library
continued to report `INSTALLED` plus `savePresent=true`. The test ROM was removed through
the supported uninstall flow afterward, leaving the managed save intact. This closes the
live identical-replacement/no-op slice while leaving maximum-size, long-running
cancellation, disk-full, and routed-network acceptance open.

The repeatable Windows harness also streamed a temporary 6,293,504-byte authorized fixture
through the real Honor companion route. Client and Android agreed on SHA-256
`41ff91ad3ec898d9b75c28410f9a5137fe8f462898c9d0960a38a972bcd437d2`; supported uninstall
removed the temporary import and retained the managed save. This closes a bounded larger-
body streaming check, while maximum-size, long-running cancellation, disk-full and routed-
network acceptance remain open.

The live cancellation probe also started a 25,174,016-byte upload and cancelled it after
100 ms. Android retained no imported or `.companion-partial` file, and the managed save
remained intact. Long-running cancellation and staging cleanup are now live evidence;
maximum-size, disk-full and routed-network acceptance remain open.

The catalog repair changes database state only for six known legacy demo IDs
without local content references or a real download source. It does not delete
files, save records, favorites, history or emulator preferences. Imported copies,
real remote downloads, Galaxy Patrol and unrelated user records are excluded.

Catalog manifests retain the legacy initialState field for schema compatibility,
but it cannot install or enqueue games. New entries always start NOT_INSTALLED;
verified local operations remain authoritative for subsequent state.

Official adapter checkpoint (27 September 2026): RetroArch 1.22.2's universal Android
APK was installed on `GameBox_API35`, initialized successfully, and its own Online
Updater installed the `Nintendo - NES / Famicom (FCEUmm)` core. This proves the emulator
frontend and required core are available through a reproducible legal distribution path.

Live API-35 critical-path checkpoint (27 September 2026): GameBox installed its bundled,
hash-verified Galaxy Patrol content and reported a successful verified-asset worker. Play
opened RetroArch's real `com.retroarch/.browser.retroactivity.RetroActivityFuture`, and
the visible game screen was rendered by FCEUmm (`1.22.2 - FCEUmm ((SVN) 7a542da)`).
Hardware-key events were dispatched while the session was running. RetroArch created
`/sdcard/RetroArch/states/FCEUmm/galaxy-patrol.state` and its preview, then Close Content
and Quit returned to GameBox. The content-only uninstall removed the installed game file
while the emulator state remained; reinstall restored the app-private `.nes` file and a
second Play relaunched the real RetroArch activity. Loading slot 0 after reinstall showed
RetroArch's `Loaded state from slot: 0` confirmation. Logcat also recorded execution of
`/data/data/com.retroarch/cores/fceumm_libretro_android.so`.

This proves the authorized API-35 emulator lifecycle, including emulator-owned save-state
continuity. It does not prove managed GameBox save import/cloud synchronization, a physical
Honor emulator adapter, every supported console group, or signed release behavior.

Physical Honor Magic6 Pro critical-path checkpoint (27 September 2026): the official
RetroArch 1.22.2 ARM64 package launched on the connected `PGT-N19` device, and the
FCEUmm core was installed through its Online Updater. GameBox then installed Galaxy
Patrol, opened the real `RetroActivityFuture`, rendered the game, accepted controller
key events, and created `/sdcard/RetroArch/states/FCEUmm/galaxy-patrol.state`. Close
Content and Quit returned to GameBox. The product's uninstall confirmation removed the
49,168-byte content file while explicitly retaining saves and external emulator files;
the state file remained. Reinstall restored the app-private `.nes` file, relaunch opened
RetroArch again, and loading slot 0 displayed `Loaded state from slot: 0`.

This is physical-device gameplay and emulator-save continuity evidence on the Honor. It
does not yet cover a separately connected external controller, external-display/DeX
soak, managed GameBox save import/cloud synchronization, or other adapter families.

PR #268's real WorkManager/Room/files lifecycle test passed without skipping and
was merged. It covers hash-verified installation, synthetic save backup,
content-only uninstall, reinstall and restore. It does not validate emulator
gameplay or emulator-produced saves.

The follow-up startup fix waits for Galaxy Patrol's database row before applying
restored WorkManager state. Its regression extends the lifecycle test by delaying
library availability while observing actual completed work. Subsequent runs exposed
a fast-reinstall failure: verified content and WorkManager success coexisted with
a QUEUED library row. PR #270 now retains work identity in observable download
state and serializes installation-state writes. Commit 008fd131 passed all 12
API-35 instrumentation tests with zero skips/failures; this is automated lifecycle
evidence, not proof of RetroArch gameplay or emulator-produced saves.

PR #271 additionally defers remote-work reconciliation until both database rows
exist and preserves a completed download's later uninstall across controller
restart. Its injected-work regression is not a live provider transfer test.
PR #272 resolves the actual MediaStore export path rather than guessing a filename
that Android may rename, and marks missing/altered launch content for reinstall
without invalidating valid content when emulator setup fails. PRs #271 and #272
are merged after all five checks passed on each; their API-35 instrumentation runs
completed 13 tests each. MediaStore export validation does not prove RetroArch
execution or resolve every possible black-screen cause.

PR #273 is merged after build, unit, instrumentation and phone/DeX screenshot
checks passed. It replaces production synthetic save creation with a real
document-picker import, rejecting empty/oversized files and existing-save
overwrites. The lifecycle test imports test-only bytes through that API before
backup, uninstall, reinstall and restore. See [managed save import](save-import.md)
for the current Galaxy Patrol scope and the separate emulator synchronization gap.
