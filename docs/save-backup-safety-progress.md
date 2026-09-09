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

Still required: general per-game UI/controller wiring, emulator-owned save access,
snapshot transaction recovery, shared mutation coordination and real emulator
save round-trip validation. Hardware acceptance remains separately deferred.

