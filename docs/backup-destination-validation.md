# Backup destination validation

SAVE-14 follow-up: validate that an existing backup and checksum destination are files before creation or import changes backup bytes. A checksum path that is a directory previously allowed replacement of the old backup followed by a write failure. Both entry points now reject that condition first.

Ten local SaveBackupService tests passed, including preservation of old bytes for create and import when the checksum destination is a directory. Project validation pending. This is not a transaction across the backup and checksum files; interrupted paired publication remains open.
