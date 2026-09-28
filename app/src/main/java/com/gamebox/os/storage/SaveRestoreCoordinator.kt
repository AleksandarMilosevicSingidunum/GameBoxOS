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
    private val adapter: BidirectionalSaveAdapter? = null,
) {
    fun restoreLatest(gameId: String): GameSaveRestoreResult {
        val store = manifestStore
            ?: return GameSaveRestoreResult(gameId, emptyList(), "Snapshot manifest store is not configured")
        val manifest = store.load(gameId)
            ?: return GameSaveRestoreResult(gameId, emptyList(), "No save snapshot is available")
        return restore(gameId, manifest.relativePaths, manifest.generation)
    }

    fun restore(gameId: String, relativePaths: List<String>): GameSaveRestoreResult {
        return restore(gameId, relativePaths, generation = null)
    }

    private fun restore(gameId: String, relativePaths: List<String>, generation: String?): GameSaveRestoreResult {
        require(gameId.matches(Regex("[A-Za-z0-9._-]+"))) { "gameId contains unsupported characters" }
        require(generation == null || generation.matches(Regex("[A-Za-z0-9._-]+"))) { "Snapshot generation is invalid" }
        val uniquePaths = relativePaths.distinct()
        val prefix = gameId + "/"
        return GameSaveRestoreResult(
            gameId = gameId,
            artifacts = uniquePaths.map { path ->
                val normalized = path.replace('\\', '/')
                val safeForGame = normalized.startsWith(prefix) &&
                    !normalized.split('/').any { it == ".." || it.isBlank() }
                SaveArtifactRestoreResult(
                    path,
                    if (safeForGame) {
                        val backupPath = generation?.let { ".snapshots/$it/$normalized" } ?: normalized
                        if (adapter != null) {
                            val artifact = SaveArtifact(gameId, normalized, 0L, 0L)
                            backupService.restoreTo(backupPath) { source ->
                                adapter.restoreArtifact(artifact, source)
                            }
                        } else {
                            backupService.restore(backupPath, normalized)
                        }
                    } else BackupResult.CROSS_GAME_PATH
                )
            },
        )
    }
}
