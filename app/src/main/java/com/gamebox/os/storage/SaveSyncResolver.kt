package com.gamebox.os.storage

class SaveSyncResolver(
    private val strategy: SaveConflictStrategy = SaveConflictStrategy.KEEP_BOTH
) {
    fun resolve(local: SaveRevision?, remote: SaveRevision?): SaveSyncResult {
        if (local == null && remote == null) return SaveSyncResult.UpToDate
        if (local == null) return SaveSyncResult.Downloaded(remote!!)
        if (remote == null) return SaveSyncResult.Uploaded(local)
        require(local.gameId == remote.gameId) { "save revisions must belong to the same game" }
        if (local.checksum == remote.checksum) return SaveSyncResult.UpToDate
        return when (strategy) {
            SaveConflictStrategy.KEEP_LOCAL -> SaveSyncResult.Uploaded(local)
            SaveConflictStrategy.KEEP_REMOTE -> SaveSyncResult.Downloaded(remote)
            SaveConflictStrategy.KEEP_BOTH -> SaveSyncResult.Conflict(local, remote)
        }
    }
}