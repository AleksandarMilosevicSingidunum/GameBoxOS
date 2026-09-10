# SAVE-14: empty backup protection

Creating a managed backup now rejects an empty staged save before publishing, retaining the previous backup and checksum. Restore and export reject zero-length and oversized stored backups even if their checksum is valid. Backup creation and restore clean their staging files in finally blocks.

Nine SaveBackupService tests passed locally, including preservation of the last good backup and current save. Project/Android validation is pending. This does not prove emulator save compatibility or crash-atomic replacement of the backup plus checksum pair; both remain separate acceptance work.
