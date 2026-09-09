# Per-game save controls — integration in progress

The local controller accepts a validated game ID and derives initial/cloud save
paths from it. Observed records outside that game's path are excluded before
operations can use them. Legacy fixture uninstall methods reject non-fixture IDs.
The application container can create a game-bound controller on an IO dispatcher,
using the caller's coroutine lifetime rather than retaining every visited game.

Two pure identity tests pass locally. A new Android test exercises two controllers
with separate imports, backup/restore, corrupt cross-game records and synthetic
save bytes. It has not run yet. Controller and container edits are not yet compiled
by Android tooling yet. Game details now receive the per-game factory and show a
managed-copy panel with import, backup, export and confirmed restore. Added Compose
tests cover confirmation, cancellation and disabled controls; execution is pending.
The integration is being published as a draft for validation, not marked complete.

This is managed single-file save handling, not emulator-private save access.
Emulator integration, multi-file snapshots, operation cancellation/serialization,
restore confirmation and provider round-trip validation remain required before
general save management can be considered complete.

