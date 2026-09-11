# Atomic managed save snapshots

Managed backup creation and import publish payload and SHA-256 digest in one bounded snapshot file using a required atomic replacement. The file is flushed before publication. Interrupted, empty and oversized writes retain the previous snapshot and remove temporary files. Restore and export validate the complete snapshot before returning payload bytes.

Legacy payload plus `.sha256` backups remain readable. The next successful backup migrates that path to the atomic format; a leftover legacy sidecar is ignored when the atomic header is present.

Multi-artifact coordination preflights every discovered source. Known missing, empty or oversized artifacts abort the batch before any backup changes. Per-artifact results remain visible and the prior manifest is retained. If writes begin and the process stops between separate artifact publications, the set is not yet transactional; generation-based recovery remains open.

Sixteen combined snapshot, service and manifest-wiring tests pass locally. Full project validation is pending. This does not prove real emulator save compatibility.
