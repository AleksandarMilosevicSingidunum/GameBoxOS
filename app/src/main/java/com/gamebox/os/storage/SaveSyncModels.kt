package com.gamebox.os.storage

data class SaveRevision(
    val gameId: String,
    val checksum: String,
    val updatedAtMillis: Long,
    val sizeBytes: Long
)

enum class SaveConflictStrategy { KEEP_LOCAL, KEEP_REMOTE, KEEP_BOTH }

sealed interface SaveSyncResult {
    data object UpToDate : SaveSyncResult
    data class Uploaded(val revision: SaveRevision) : SaveSyncResult
    data class Downloaded(val revision: SaveRevision) : SaveSyncResult
    data class Conflict(val local: SaveRevision, val remote: SaveRevision) : SaveSyncResult
}