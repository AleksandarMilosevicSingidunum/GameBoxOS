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
    private val manifestStore: SaveSnapshotManifestStore? = null,
    private val nowMillis: () -> Long = System::currentTimeMillis,
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
                val results = artifacts.map { artifact ->
                    SaveArtifactBackupResult(
                        artifact = artifact,
                        result = backupService.createBackup(artifact.relativePath),
                    )
                }
                val successfulPaths = results
                    .filter { it.result == BackupResult.SUCCESS }
                    .map { it.artifact.relativePath }
                if (successfulPaths.isNotEmpty()) {
                    manifestStore?.save(
                        SaveSnapshotManifest(
                            gameId = gameId,
                            createdAtMillis = nowMillis(),
                            relativePaths = successfulPaths,
                        ),
                    )
                }
                GameSaveBackupResult(gameId = gameId, artifacts = results)
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
