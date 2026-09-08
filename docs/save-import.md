# Managed save import

The Galaxy Patrol Details screen offers **Import save file** when GameBox has no
managed save record. Select a non-empty save file using Android's document picker.
The import accepts up to 16 MiB, copies into GameBox-owned storage, and leaves the
selected original unchanged. It refuses to replace an existing managed save.

The managed destination is `files/saves/galaxy-patrol/save.dat` inside the app's
private data directory. A successful import records its size and enables the
existing backup operations. Content-only uninstall retains this save. Backup
import and Restore are separate actions; importing a backup is not itself a
request to overwrite the active save.

This does **not** automatically find or configure RetroArch's save directory.
The file must be compatible with the emulator/core you use. A file being accepted
only proves that it was safely copied; it does not prove emulator compatibility,
gameplay, or that the emulator will read GameBox's private copy. Automated
lifecycle tests use synthetic bytes through the import API to check storage and
backup behavior, not to establish emulator compatibility.

If import fails, verify that the document is readable, non-empty, within the size
limit, and that no managed save already exists. Existing saves are not overwritten
by this action. Use deliberate backup/restore operations for replacement.

This flow is currently scoped to Galaxy Patrol. General per-game emulator save
discovery and synchronization remain separate implementation/validation work.
