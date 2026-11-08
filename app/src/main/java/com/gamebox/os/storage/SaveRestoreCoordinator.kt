package com.gamebox.os.storage

import java.util.UUID

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
        val saveAdapter = adapter
        val pathsToRestore = if (saveAdapter == null) {
            manifest.relativePaths
        } else {
            val existingPaths = runCatching { saveAdapter.discover(gameId).mapTo(mutableSetOf()) { it.relativePath } }
                .getOrElse {
                    return GameSaveRestoreResult(
                        gameId,
                        emptyList(),
                        "Live emulator saves could not be checked; current progress was left untouched",
                    )
                }
            manifest.relativePaths.filterNot(existingPaths::contains)
        }
        if (pathsToRestore.isEmpty() && saveAdapter != null) {
            return GameSaveRestoreResult(
                gameId,
                emptyList(),
                "Live emulator saves already exist; automatic restore was skipped to protect current progress",
            )
        }
        return restore(gameId, pathsToRestore, manifest.generation)
    }

    fun restore(gameId: String, relativePaths: List<String>): GameSaveRestoreResult {
        return restore(gameId, relativePaths, generation = null)
    }

    private fun restore(gameId: String, relativePaths: List<String>, generation: String?): GameSaveRestoreResult {
        require(gameId.matches(Regex("[A-Za-z0-9._-]+"))) { "gameId contains unsupported characters" }
        require(generation == null || generation.matches(Regex("[A-Za-z0-9._-]+"))) { "Snapshot generation is invalid" }
        val uniquePaths = relativePaths.distinct()
        val prefix = gameId + "/"
        val prepared = uniquePaths.map { path ->
            val normalized = path.replace('\\', '/')
            val safeForGame = normalized.startsWith(prefix) &&
                !normalized.split('/').any { it == ".." || it.isBlank() }
            val backupPath = generation?.let { ".snapshots/$it/$normalized" } ?: normalized
            Triple(path, normalized, if (safeForGame) backupService.validateBackup(backupPath) else BackupResult.CROSS_GAME_PATH)
        }
        if (prepared.any { it.third != BackupResult.SUCCESS }) {
            val failed = prepared.first { it.third != BackupResult.SUCCESS }.third
            return GameSaveRestoreResult(
                gameId,
                prepared.map { (path, _, result) ->
                    SaveArtifactRestoreResult(path, if (result == BackupResult.SUCCESS) BackupResult.SNAPSHOT_ABORTED else result)
                },
                "Restore stopped before changing live saves because a backup could not be verified ($failed)",
            )
        }
        val transactionId = UUID.randomUUID().toString()
        val transactionPaths = prepared.associate { (_, normalized, _) ->
            normalized to ".restore-transactions/$transactionId/$normalized"
        }
        val saveAdapter = adapter
        val existingPaths = try {
            saveAdapter?.discover(gameId)?.mapTo(mutableSetOf()) { it.relativePath }.orEmpty()
        } catch (_: Exception) {
            return GameSaveRestoreResult(gameId, prepared.map { (path, _, _) ->
                SaveArtifactRestoreResult(path, BackupResult.SNAPSHOT_ABORTED)
            }, "Live saves could not be inspected; restore was not started")
        }
        val existed = mutableMapOf<String, Boolean>()
        for ((_, normalized, _) in prepared) {
            val wasPresent = if (saveAdapter != null) normalized in existingPaths else {
                when (runCatching {
                    backupService.stageCurrentSave(normalized, transactionPaths.getValue(normalized))
                }.getOrDefault(BackupResult.SNAPSHOT_ABORTED)) {
                    BackupResult.SUCCESS -> true
                    BackupResult.SOURCE_MISSING -> false
                    else -> {
                        backupService.discardRestoreTransaction(transactionId)
                        return GameSaveRestoreResult(gameId, prepared.map { (itemPath, _, _) ->
                            SaveArtifactRestoreResult(itemPath, BackupResult.SNAPSHOT_ABORTED)
                        }, "Current saves could not be protected; restore was not started")
                    }
                }
            }
            existed[normalized] = wasPresent
            if (saveAdapter != null && wasPresent) {
                val artifact = SaveArtifact(gameId, normalized, 0L, 0L)
                val staged = runCatching {
                    backupService.stageRestoreSource(
                        transactionPaths.getValue(normalized),
                        saveAdapter.openArtifact(artifact),
                    )
                }.getOrDefault(BackupResult.SNAPSHOT_ABORTED)
                if (staged != BackupResult.SUCCESS) {
                    backupService.discardRestoreTransaction(transactionId)
                    return GameSaveRestoreResult(gameId, prepared.map { (itemPath, _, _) ->
                        SaveArtifactRestoreResult(itemPath, BackupResult.SNAPSHOT_ABORTED)
                    }, "Current saves could not be protected; restore was not started")
                }
            }
        }

        val attempted = mutableListOf<Triple<String, String, String>>()
        var failure: Pair<String, String>? = null
        for ((path, normalized, _) in prepared) {
            attempted += Triple(path, normalized, transactionPaths.getValue(normalized))
            val backupPath = generation?.let { ".snapshots/$it/$normalized" } ?: normalized
            val result = runCatching {
                if (saveAdapter != null) {
                    val artifact = SaveArtifact(gameId, normalized, 0L, 0L)
                    backupService.restoreTo(backupPath) { source -> saveAdapter.restoreArtifact(artifact, source) }
                } else backupService.restore(backupPath, normalized)
            }.getOrElse { BackupResult.SNAPSHOT_ABORTED }
            if (result != BackupResult.SUCCESS) {
                failure = path to result.name
                break
            }
        }
        if (failure == null) {
            backupService.discardRestoreTransaction(transactionId)
            return GameSaveRestoreResult(gameId, prepared.map { (path, _, _) ->
                SaveArtifactRestoreResult(path, BackupResult.SUCCESS)
            })
        }

        val rollbackFailures = attempted.asReversed().mapNotNull { (_, normalized, stagedPath) ->
            runCatching {
                if (saveAdapter != null) {
                    val artifact = SaveArtifact(gameId, normalized, 0L, 0L)
                    if (existed[normalized] == true) {
                        backupService.openRestoreSource(stagedPath).use { source ->
                            saveAdapter.restoreArtifact(artifact, source)
                        }
                    } else saveAdapter.deleteArtifact(artifact)
                } else if (existed[normalized] == true) {
                    check(backupService.restoreStagedSave(stagedPath, normalized) == BackupResult.SUCCESS)
                } else backupService.removeSave(normalized)
            }.exceptionOrNull()?.let { normalized }
        }
        if (rollbackFailures.isEmpty()) backupService.discardRestoreTransaction(transactionId)
        return GameSaveRestoreResult(
            gameId,
            prepared.map { (path, _, _) -> SaveArtifactRestoreResult(path, BackupResult.SNAPSHOT_ABORTED) },
            "Restore failed for ${failure.first} (${failure.second}); " + if (rollbackFailures.isEmpty()) {
                "previous live saves were restored"
            } else {
                "rollback failed for ${rollbackFailures.joinToString()}; recovery data was retained"
            },
        )
    }
}
