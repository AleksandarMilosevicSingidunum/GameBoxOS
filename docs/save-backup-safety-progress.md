# Save backup path validation

Backup, restore, import and export reject traversal components, backslashes,
alternate-stream syntax, NULs and symbolic-link path components. Temporary files
and checksum sidecars use the same validation. Sidecars are checked before backup
replacement. This closes static cross-game path and link cases; it is not an
atomic defense against concurrent filesystem mutation or a complete save system.

Seven local JUnit tests passed on rerun, covering existing backup/restore/import
behavior and invalid paths. The first broader Windows run had an AccessDeniedException
at atomic file replacement; an unchanged rerun passed. The cause is not established.
An Android regression test covers linked backup, checksum, import-staging and
restore-staging paths; its CI execution is pending.

Completed-session recovery now automatically discovers managed save artifacts,
creates checksum-verified atomic backups, and publishes a per-game snapshot manifest.
A failed or incomplete automatic snapshot retains the prior complete snapshot and
surfaces a retry message; interrupted/unconfirmed handoffs do not trigger backup.
This advances SAVE-04/SAVE-09 for GameBox-managed saves.

Still required: emulator-owned save access outside GameBox private storage and real
emulator save round-trip validation. Hardware acceptance remains separately deferred.

