package com.gamebox.os.storage

interface SaveSyncOperations {
    fun upload(revision: SaveRevision): SaveSyncResult
    fun download(revision: SaveRevision): SaveSyncResult
    fun preserveConflict(local: SaveRevision, remote: SaveRevision): SaveSyncResult
}

class SaveSyncService(
    private val resolver: SaveSyncResolver = SaveSyncResolver(),
    private val operations: SaveSyncOperations
) {
    fun synchronize(local: SaveRevision?, remote: SaveRevision?): SaveSyncResult {
        return when (val decision = resolver.resolve(local, remote)) {
            is SaveSyncResult.Uploaded, is SaveSyncResult.Downloaded, SaveSyncResult.UpToDate -> decision
            is SaveSyncResult.Conflict -> operations.preserveConflict(decision.local, decision.remote)
        }
    }
}