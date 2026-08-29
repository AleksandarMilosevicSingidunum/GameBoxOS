package com.gamebox.os.storage

data class SaveArtifactRestoreResult(
    val relativePath: String,
    val result: BackupResult,
)

data class GameSaveRestoreResult(
    val gameId: String,
    val artifacts: List<SaveArtifactRestoreResult>,
) {
    val successfulCount: Int get() = artifacts.count { it.result == BackupResult.SUCCESS }
    val failedCount: Int get() = artifacts.size - successfulCount
}

class SaveRestoreCoordinator(private val backupService: SaveBackupService) {
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
