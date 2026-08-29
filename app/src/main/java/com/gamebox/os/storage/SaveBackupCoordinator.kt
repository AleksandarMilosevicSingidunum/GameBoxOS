package com.gamebox.os.storage

data class SaveArtifactBackupResult(
    val artifact: SaveArtifact,
    val result: BackupResult,
)

data class GameSaveBackupResult(
    val gameId: String,
    val artifacts: List<SaveArtifactBackupResult>,
    val message: String? = null,
) {
    val successfulCount: Int get() = artifacts.count { it.result == BackupResult.SUCCESS }
    val failedCount: Int get() = artifacts.size - successfulCount
}

class SaveBackupCoordinator(
    private val registry: SaveAdapterRegistry,
    private val backupService: SaveBackupService,
) {
    fun backup(platform: String, gameId: String): GameSaveBackupResult {
        val adapter = registry.adapterFor(platform)
            ?: return GameSaveBackupResult(
                gameId = gameId,
                artifacts = emptyList(),
                message = "Save backup is not configured for platform: " + platform,
            )

        return runCatching { adapter.discover(gameId) }.fold(
            onSuccess = { artifacts ->
                GameSaveBackupResult(
                    gameId = gameId,
                    artifacts = artifacts.map { artifact ->
                        SaveArtifactBackupResult(
                            artifact = artifact,
                            result = backupService.createBackup(artifact.relativePath),
                        )
                    },
                )
            },
            onFailure = { error ->
                GameSaveBackupResult(
                    gameId = gameId,
                    artifacts = emptyList(),
                    message = error.message ?: "Save discovery failed",
                )
            },
        )
    }
}
