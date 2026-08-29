package com.gamebox.os.storage

import android.content.Context
import android.net.Uri
import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.local.SaveRecordDao
import com.gamebox.os.data.local.SaveRecordEntity
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.download.AssetDownloadWorker
import com.gamebox.os.download.AuthorizedTestDownload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class SaveSafetyState(
    val saveRecordPresent: Boolean = false,
    val relativePath: String? = null,
    val sizeBytes: Long = 0L,
    val backupPresent: Boolean = false,
    val operationMessage: String? = null,
    val operationSuccessful: Boolean = true
)

data class SaveOperation(val message: String? = null, val successful: Boolean = true)

fun backupResultMessage(action: String, result: BackupResult): SaveOperation = when (result) {
    BackupResult.SUCCESS -> SaveOperation("$action completed")
    BackupResult.SOURCE_MISSING -> SaveOperation("$action failed: save file is missing", false)
    BackupResult.BACKUP_MISSING -> SaveOperation("$action failed: backup is missing", false)
    BackupResult.CHECKSUM_MISMATCH -> SaveOperation("$action refused: backup checksum mismatch", false)
    BackupResult.SIZE_LIMIT_EXCEEDED -> SaveOperation("$action refused: selected file exceeds 16 MiB", false)
}

interface SaveSafetyController {
    fun observeState(): StateFlow<SaveSafetyState>
    fun createTestSaveRecord()
    fun uninstallPreview(): UninstallConfirmation
    fun uninstallTestContent()
    fun backupSave()
    fun restoreSave()
    fun exportBackup(uri: Uri)
    fun importBackup(uri: Uri)
}

class DefaultSaveSafetyController(
    context: Context,
    private val saveRecordDao: SaveRecordDao,
    private val gameRepository: GameRepository,
    private val scope: CoroutineScope
) : SaveSafetyController {
    private val applicationContext = context.applicationContext
    private val gameId = GameId("retro-test")
    private val savesRoot = applicationContext.filesDir.resolve("saves")
    private val backupService = SaveBackupService(
        savesRoot,
        applicationContext.filesDir.resolve("save-backups")
    )
    private val operation = MutableStateFlow(SaveOperation())
    private val state = combine(saveRecordDao.observe(gameId.value), operation) { record, current ->
        SaveSafetyState(
            saveRecordPresent = record != null,
            relativePath = record?.relativePath,
            sizeBytes = record?.sizeBytes ?: 0L,
            backupPresent = record?.relativePath?.let {
                runCatching { backupService.hasBackup(it) }.getOrDefault(false)
            } ?: false,
            operationMessage = current.message,
            operationSuccessful = current.successful
        )
    }.stateIn(scope, SharingStarted.Eagerly, SaveSafetyState())

    override fun observeState(): StateFlow<SaveSafetyState> = state

    override fun createTestSaveRecord() {
        scope.launch {
            runCatching {
                val relativePath = "retro-test/save.dat"
                val root = savesRoot.canonicalFile
                val saveFile = File(root, relativePath).canonicalFile
                require(saveFile.path.startsWith(root.path + File.separator))
                check(saveFile.parentFile?.mkdirs() != false || saveFile.parentFile?.isDirectory == true)
                if (!saveFile.exists()) saveFile.writeText("SAVE")
                saveRecordDao.upsert(record(relativePath, saveFile.length()))
            }.onSuccess {
                operation.value = SaveOperation("Test save created")
            }.onFailure {
                operation.value = SaveOperation("Unable to create test save", false)
            }
        }
    }

    override fun backupSave() = runBackupAction("Backup") { relativePath ->
        backupService.createBackup(relativePath)
    }

    override fun restoreSave() = runBackupAction("Restore") { relativePath ->
        backupService.restore(relativePath)
    }

    override fun exportBackup(uri: Uri) {
        scope.launch {
            val relativePath = state.value.relativePath ?: return@launch noSave("Export")
            val result = runCatching {
                applicationContext.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    backupService.exportBackup(relativePath, output)
                } ?: BackupResult.BACKUP_MISSING
            }.getOrElse {
                operation.value = SaveOperation("Export failed: document could not be written", false)
                return@launch
            }
            operation.value = backupResultMessage("Export", result)
        }
    }

    override fun importBackup(uri: Uri) {
        scope.launch {
            val relativePath = state.value.relativePath ?: return@launch noSave("Import")
            val result = runCatching {
                applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                    backupService.importBackup(relativePath, input)
                } ?: BackupResult.BACKUP_MISSING
            }.getOrElse {
                operation.value = SaveOperation("Import failed: document could not be read", false)
                return@launch
            }
            if (result == BackupResult.SUCCESS) {
                saveRecordDao.upsert(record(relativePath, savesRoot.resolve(relativePath).length()))
            }
            operation.value = backupResultMessage("Import", result)
        }
    }

    override fun uninstallPreview(): UninstallConfirmation {
        val contentFile = applicationContext.filesDir
            .resolve(AssetDownloadWorker.INSTALL_ROOT)
            .resolve(AuthorizedTestDownload.RELATIVE_PATH)
        val artifacts = buildList {
            add(
                StoredArtifact(
                    gameId,
                    "internal://installed-content",
                    ArtifactKind.GAME_CONTENT,
                    contentFile.takeIf { it.isFile }?.length() ?: 0L
                )
            )
            state.value.relativePath?.let {
                add(
                    StoredArtifact(
                        gameId,
                        "internal://save-data",
                        ArtifactKind.SAVE_DATA,
                        state.value.sizeBytes.coerceAtLeast(0L)
                    )
                )
            }
        }
        return UninstallPlanner().plan(gameId, artifacts).toConfirmation()
    }

    override fun uninstallTestContent() {
        scope.launch {
            val result = runCatching {
                FileContentUninstaller(
                    applicationContext.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT)
                ).uninstall(AuthorizedTestDownload.RELATIVE_PATH)
            }
            if (result.isFailure) {
                operation.value = SaveOperation("Uninstall failed safely", false)
                return@launch
            }
            gameRepository.setInstallState(gameId, InstallState.NOT_INSTALLED)
            operation.value = SaveOperation(
                if (result.getOrDefault(false)) "Content removed; save retained"
                else "Content already absent; save retained"
            )
        }
    }

    private fun runBackupAction(
        action: String,
        block: (String) -> BackupResult
    ) {
        scope.launch {
            val relativePath = state.value.relativePath ?: return@launch noSave(action)
            val result = runCatching { block(relativePath) }.getOrElse {
                operation.value = SaveOperation("$action failed safely", false)
                return@launch
            }
            if (result == BackupResult.SUCCESS) {
                saveRecordDao.upsert(record(relativePath, savesRoot.resolve(relativePath).length()))
            }
            operation.value = backupResultMessage(action, result)
        }
    }

    private fun noSave(action: String) {
        operation.value = SaveOperation("$action failed: no save record", false)
    }

    private fun record(relativePath: String, sizeBytes: Long) = SaveRecordEntity(
        gameId.value,
        relativePath,
        System.currentTimeMillis(),
        sizeBytes
    )
}
