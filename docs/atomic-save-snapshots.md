# Atomic managed save snapshots

Managed backup creation and import publish payload and SHA-256 digest in one bounded snapshot file using a required atomic replacement. The file is flushed before publication. Interrupted, empty and oversized writes retain the previous snapshot and remove temporary files. Restore and export validate the complete snapshot before returning payload bytes.

Legacy payload plus `.sha256` backups remain readable. The next successful backup migrates that path to the atomic format; a leftover legacy sidecar is ignored when the atomic header is present.

Multi-artifact coordination now reports individual failures without throwing out of the operation. A new manifest is published only when every discovered artifact succeeds; otherwise the previous complete manifest is retained.

Sixteen combined snapshot, service and manifest-wiring tests pass locally. Full project validation is pending. This does not provide a transaction across all artifact files and their manifest, nor prove real emulator save compatibility.
