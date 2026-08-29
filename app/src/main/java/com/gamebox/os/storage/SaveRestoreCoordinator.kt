package com.gamebox.os.storage

data class SaveArtifactRestoreResult(
    val relativePath: String,
    val result: BackupResult,
)

data class GameSaveRestoreResult(
    val gameId: String,
    val artifacts: List<SaveArtifactRestoreResult>,
    val message: String? = null,
) {
    val successfulCount: Int get() = artifacts.count { it.result == BackupResult.SUCCESS }
    val failedCount: Int get() = artifacts.size - successfulCount
}

class SaveRestoreCoordinator(
    private val backupService: SaveBackupService,
    private val manifestStore: SaveSnapshotManifestStore? = null,
) {
    fun restoreLatest(gameId: String): GameSaveRestoreResult {
        val store = manifestStore
            ?: return GameSaveRestoreResult(gameId, emptyList(), "Snapshot manifest store is not configured")
        val manifest = store.load(gameId)
            ?: return GameSaveRestoreResult(gameId, emptyList(), "No save snapshot is available")
        return restore(gameId, manifest.relativePaths)
    }

    fun restore(gameId: String, relativePaths: List<String>): GameSaveRestoreResult {
        require(gameId.isNotBlank()) { "gameId cannot be blank" }
        val uniquePaths = relativePaths.distinct()
        return GameSaveRestoreResult(
            gameId = gameId,
            artifacts = uniquePaths.map { path ->
                SaveArtifactRestoreResult(path, backupService.restore(path))
            },
        )
    }
}
