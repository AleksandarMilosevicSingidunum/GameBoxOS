# Released-schema migration testing

Android instrumentation constructs every released database schema version from 1 through
13, seeds a sentinel game, and opens it with the production Room database and complete
migration registry. The current database version is 14. Room validates the resulting
version-14 schema; the test also verifies sentinel preservation, default values added by
later migrations, current tables, and the final SQLite `user_version`.

The matrix uses the production migration chain:

`1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12 → 13 → 14`.

For every starting version from 1 through 13, the test constructs that historical state
from the version-1 schema using the migrations that had shipped by that point, then opens
the database through Room with the full current registry. This continuously exercises
every supported upgrade suffix without destructive fallback.

The current matrix covers the software migration path and sentinel preservation for
DATA-09, DATA-10, DATA-11, and the software portion of REL-02. The migration-backup
manager separately snapshots older on-device databases before Room migration.

This automated matrix does **not** prove a signed APK upgrade/downgrade on a real device.
Production signing, APK upgrade rehearsal, rollback behavior, and user recovery remain
separate release evidence.
