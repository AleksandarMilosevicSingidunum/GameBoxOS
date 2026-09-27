# Backup destination validation

SAVE-14 follow-up: validate that an existing backup and checksum destination are files before creation or import changes backup bytes. A checksum path that is a directory previously allowed replacement of the old backup followed by a write failure. Both entry points now reject that condition first.

The SaveBackupService coverage now includes preservation of old bytes for create and
import when the checksum destination is a directory, plus scoped cleanup of incomplete
snapshot generations. The current Android JVM suite passes with 515 tests, zero
failures, and one explicit Windows symlink-capability skip. New writes publish one
atomic snapshot file; legacy payload-plus-checksum paths remain readable for migration.
This still does not prove real emulator save compatibility or cloud endpoint acceptance.
