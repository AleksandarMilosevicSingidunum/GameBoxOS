# Database migration backups

Before Room opens an existing `gamebox.db` whose SQLite `user_version` is older than the current application schema, GameBox creates a durable app-private snapshot under `files/database-backups`.

The snapshot includes the database and an existing write-ahead log. Files are copied into a private staging directory, flushed to storage, and exposed only after the directory rename succeeds. Startup fails closed if a required backup cannot be committed, preventing an unprotected schema migration. The two newest committed snapshots are retained.

Current-schema, missing, and invalid/empty database files do not produce redundant snapshots. This implements the automated backup portion of Blueprint DATA-11. A signed downgrade rehearsal and user-facing recovery procedure remain tracked by SYS-26 and REL-14.
