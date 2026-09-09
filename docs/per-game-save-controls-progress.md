# Per-game save controls — integration in progress

The local controller accepts a validated game ID and derives initial/cloud save
paths from it. Observed records outside that game's path are excluded before
operations can use them. Legacy fixture uninstall methods reject non-fixture IDs.
The application container can create a game-bound controller on an IO dispatcher,
using the caller's coroutine lifetime rather than retaining every visited game.

Two pure identity tests pass locally. A new Android test exercises two controllers
with separate imports, backup/restore, corrupt cross-game records and synthetic
save bytes. The Android unit/build check and Windows build passed at c683dbf;
Android instrumentation and screenshot checks were still running when checked.
Game details now receive the per-game factory and show a
managed-copy panel with import, backup, export and confirmed restore. Added Compose
tests cover confirmation, cancellation and disabled controls; execution is pending.
The integration is being published as a draft for validation, not marked complete.

Follow-up: a process-local game/root guard rejects overlapping save operations,
including operations from newly created controllers. The panel disables actions
while its controller is busy. Once started, file work and record updates continue
on IO despite panel disposal; completion releases the guard even if the parent
was cancelled before work began. This is not a durable file/database transaction
and does not survive process death. One gate unit test passes locally; UI busy-state
and controller lifecycle integration evidence are still pending.

Added a deterministic lifecycle test that pauses the real Room record write after
import, cancels the original panel scope, verifies a reopened controller cannot
overlap it, then releases the write and verifies the record, bytes and subsequent
backup. Existing tests now await operation completion before starting another
action. The guard is released before clearing the busy indicator, so an enabled
button cannot race a still-held guard. These follow-up Android tests require CI
execution; no local Android SDK is available. This is not process-kill evidence.

This is managed single-file save handling, not emulator-private save access.
The panel now also exposes importing a newer backup, with a pre-picker warning
that the stored backup is replaced and a separate confirmed restore to apply it.
Picker callbacks retain their original controller identity and reject delivery
to a different game's controller or while actions are unavailable. Added UI
warning/cancel coverage and an Android import-then-restore isolation assertion;
these new tests await execution. File ownership/format cannot be inferred from
arbitrary save bytes, and the UI explicitly states that limitation.
Emulator integration, multi-file snapshots, durable recovery and
provider round-trip validation remain required before
general save management can be considered complete.
