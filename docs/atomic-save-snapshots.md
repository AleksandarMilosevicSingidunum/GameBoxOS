# Atomic managed save snapshots

Managed backup creation and import now publish payload and SHA-256 digest in one bounded snapshot file using a required atomic replacement. The file is flushed before publication. Interrupted, empty and oversized writes retain the previous snapshot and remove temporary files. Restore and export validate the complete snapshot before returning payload bytes.

Legacy payload plus `.sha256` backups remain readable. The next successful backup migrates that path to the atomic format; a leftover legacy sidecar is ignored when the atomic header is present.

Fourteen isolated snapshot/service tests pass locally, including interrupted writes, corruption/truncation, legacy restore and migration. Full project validation is pending. This validates managed single-file backup publication, not multi-artifact transactional snapshots or real emulator save compatibility.
