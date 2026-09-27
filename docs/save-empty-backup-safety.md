# SAVE-14: empty backup protection

Creating a managed backup now rejects an empty staged save before publishing, retaining the previous backup and checksum. Restore and export reject zero-length and oversized stored backups even if their checksum is valid. Backup creation and restore clean their staging files in finally blocks.

The SaveBackupService coverage now includes preservation of the last good backup and
cleanup of incomplete snapshot generations. The current Android JVM suite passes with
514 tests, zero failures, and one explicit Windows symlink-capability skip. New managed
backups use a single atomic snapshot file; legacy payload-plus-checksum backups remain
readable and migrate on the next successful write. This still does not prove emulator
save compatibility or real cloud endpoint acceptance.
