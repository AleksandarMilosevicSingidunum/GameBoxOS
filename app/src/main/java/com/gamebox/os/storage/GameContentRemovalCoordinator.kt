package com.gamebox.os.storage

import android.content.Context
import com.gamebox.os.content.GameMutationGate
import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.local.SaveRecordDao
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.download.AuthorizedHomebrewDownload
import com.gamebox.os.content.GameContentPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

/** Coordinates save-safe, per-game content removal and its external-file boundary. */
internal class GameContentRemovalCoordinator(
    context: Context,
    private val saveRecordDao: SaveRecordDao,
    private val gameRepository: GameRepository,
    private val gameId: GameId,
    private val savesRoot: File,
    private val backupService: SaveBackupService,
    private val externalContent: ExternalLibraryContentStore?,
    private val uploadCloudSave: suspend (relativePath: String, updatedAtMillis: Long) -> Unit,
    private val safeCloudError: (Throwable) -> String,
) {
    private val applicationContext = context.applicationContext

    fun preview(game: Game): ContentRemovalPreview {
        require(game.id == gameId) { "Content controller does not belong to this game" }
        val manifest = contentManifest(game)
        val internal = GameOwnedContentUninstaller(applicationContext.filesDir).preview(manifest)
        val externalPaths = externalOnlyPaths(manifest)
        val external = if (externalPaths.isEmpty()) ContentRemovalPreview(0, 0) else {
            requireNotNull(externalContent) {
                "External game content is recorded but no external library is configured"
            }.preview(game.id.value, externalPaths)
        }
        return ContentRemovalPreview(internal.bytes + external.bytes, internal.files + external.files)
    }

    suspend fun uninstall(game: Game, allowWithoutCloudBackup: Boolean): String {
        val saveOperationKey = savesRoot.canonicalPath + ":" + gameId.value
        require(SaveOperationGate.acquire(saveOperationKey)) {
            "A save operation is already running for this game"
        }
        return try {
            GameMutationGate.withGameLock(game.id.value) {
                withContext(Dispatchers.IO + NonCancellable) {
                    require(game.id == gameId) { "Content controller does not belong to this game" }
                    val current = requireNotNull(gameRepository.game(game.id)) {
                        "Game is no longer in the library"
                    }
                    require(current.state in setOf(
                        InstallState.INSTALLED,
                        InstallState.UPDATE_AVAILABLE,
                        InstallState.MISSING_FILES,
                    )) { "Wait for installation or download operations to finish" }

                    val manifest = contentManifest(current)
                    require(manifest == contentManifest(game)) {
                        "Game content changed; reopen the confirmation"
                    }
                    val saveRecord = saveRecordDao.getByGameId(game.id.value)
                    val backupCreated = saveRecord?.let { record ->
                        requireSavePathForGame(game.id.value, record.relativePath)
                        val backupResult = backupService.createBackup(record.relativePath)
                        require(backupResult == BackupResult.SUCCESS) {
                            backupResultMessage("Pre-uninstall backup", backupResult).message
                                ?: "Pre-uninstall backup failed; content was not removed"
                        }
                        true
                    } ?: false
                    val cloudBackupCreated = saveRecord?.let { record ->
                        val upload = runCatching {
                            uploadCloudSave(record.relativePath, record.updatedAtMillis)
                        }
                        requireCloudBackupOrAcknowledgement(
                            uploadSucceeded = upload.isSuccess,
                            allowWithoutCloudBackup = allowWithoutCloudBackup,
                            failureReason = upload.exceptionOrNull()?.let(safeCloudError)
                                ?: "operation failed safely",
                        )
                    } ?: false

                    val externalPaths = externalOnlyPaths(manifest)
                    // Resolve every required external file before deleting any internal content.
                    if (externalPaths.isNotEmpty()) {
                        requireNotNull(externalContent) {
                            "External game content is unavailable; reconnect the selected library and retry"
                        }.preview(game.id.value, externalPaths)
                    }
                    var removedBeforeFailure = 0
                    try {
                        val internalRemoved = GameOwnedContentUninstaller(applicationContext.filesDir)
                            .uninstall(manifest)
                        removedBeforeFailure = internalRemoved
                        val externalRemoved = if (externalPaths.isEmpty()) 0 else {
                            requireNotNull(externalContent).uninstall(game.id.value, externalPaths)
                        }
                        removedBeforeFailure += externalRemoved
                        gameRepository.setInstallStateAndAwait(game.id, InstallState.NOT_INSTALLED)
                        buildString {
                            append("$removedBeforeFailure content file(s) removed. ")
                            if (backupCreated) append("Verified save backup created. ")
                            if (cloudBackupCreated) append("Cloud save uploaded and verified. ")
                            else if (saveRecord != null) {
                                append("Cloud backup unavailable; explicitly continued with verified local backup. ")
                            }
                            append("Saves, backups, metadata and history retained.")
                        }
                    } catch (error: ContentRemovalFailed) {
                        val removedFiles = error.totalRemovedFiles(removedBeforeFailure)
                        if (removedFiles > 0) {
                            gameRepository.setInstallStateAndAwait(game.id, InstallState.MISSING_FILES)
                        }
                        throw IllegalStateException(
                            "Content removal stopped after $removedFiles file(s). " +
                                "Saves were not touched; retry to remove remaining content.",
                            error,
                        )
                    }
                }
            }
        } finally {
            SaveOperationGate.release(saveOperationKey)
        }
    }

    private fun externalOnlyPaths(manifest: ContentRemovalManifest): List<String> =
        manifest.relativePaths.filter { path ->
            path.startsWith("installed/") && !applicationContext.filesDir.resolve(path).isFile
        }.map { it.removePrefix("installed/") }

    private fun contentManifest(game: Game): ContentRemovalManifest {
        val primary = game.localContentRelativePath
        val paths = if (primary != null) {
            (listOf(primary) + game.localContentFiles.map { it.relativePath })
                .distinct()
                .map { "imports/$it" }
        } else if (game.id.value == "galaxy-patrol") {
            listOf("installed/" + AuthorizedHomebrewDownload.RELATIVE_PATH)
        } else {
            require(game.expectedSha256 != null) { "No managed game content is recorded" }
            listOf(
                "installed/" + GameContentPolicy.describe(
                    game.id.value,
                    game.platform,
                    game.sourceUrl,
                ).relativePath,
            )
        }
        return ContentRemovalManifest(game.id.value, paths)
    }
}
