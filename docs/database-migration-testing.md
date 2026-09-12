# Released-schema migration testing

Android instrumentation now constructs every released database schema version from 1 through 11, seeds a sentinel game, and opens it with the production Room database and complete migration registry. Room validates the resulting version-12 schema; the test also verifies sentinel preservation, defaulted fields, current tables, and the final SQLite `user_version`.

The historical states are derived by creating the original version-1 schema and applying the same explicit migration sequence that shipped each subsequent version. This continuously exercises every supported upgrade suffix without destructive fallback. Together with pre-migration snapshots, this provides automated evidence for DATA-09, DATA-10, DATA-11, and the software portion of REL-02. Signed APK upgrade/downgrade rehearsal remains separate release evidence.
