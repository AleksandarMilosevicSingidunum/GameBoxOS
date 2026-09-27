# Atomic managed save snapshots

Managed backup creation and import publish payload and SHA-256 digest in one bounded snapshot file using a required atomic replacement. The file is flushed before publication. Interrupted, empty and oversized writes retain the previous snapshot and remove temporary files. Restore and export validate the complete snapshot before returning payload bytes.

Legacy payload plus `.sha256` backups remain readable. The next successful backup migrates that path to the atomic format; a leftover legacy sidecar is ignored when the atomic header is present.

Multi-artifact coordination preflights every discovered source and publishes each complete
set under a unique generation directory. The manifest is atomically replaced only after
every generation artifact succeeds; restore follows the active generation, so a process
stop between individual artifact writes leaves the prior complete generation selected.
If an artifact or manifest publication fails, the newly-created generation is discarded
without touching the active generation. Legacy version-1 manifests remain readable and continue to use their original paths.

Generation, legacy-compatibility, combined snapshot, service and manifest-wiring tests
pass in the Android JVM suite. This does not prove real emulator save compatibility.
