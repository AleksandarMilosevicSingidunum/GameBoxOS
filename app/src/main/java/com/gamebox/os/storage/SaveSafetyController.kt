package com.gamebox.os.storage

import android.content.Context
import android.net.Uri
import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.local.SaveRecordDao
import com.gamebox.os.data.local.SaveRecordEntity
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.Game
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SaveSafetyState(
    val saveRecordPresent: Boolean = false,
    val relativePath: String? = null,
    val sizeBytes: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val backupPresent: Boolean = false,
    val operationMessage: String? = null,
    val operationSuccessful: Boolean = true
)

data class SaveOperation(val message: String? = null, val successful: Boolean = true)

enum class CloudBackupPreflightStatus { NOT_REQUIRED, READY, ACKNOWLEDGEMENT_REQUIRED }

data class CloudBackupPreflight(
    val status: CloudBackupPreflightStatus,
    val message: String,
) {
    val requiresAcknowledgement: Boolean
        get() = status == CloudBackupPreflightStatus.ACKNOWLEDGEMENT_REQUIRED
}

class CloudBackupAcknowledgementRequired(message: String) : IllegalStateException(message)

internal fun requireCloudBackupOrAcknowledgement(
    uploadSucceeded: Boolean,
    allowWithoutCloudBackup: Boolean,
    failureReason: String,
): Boolean {
    if (!uploadSucceeded && !allowWithoutCloudBackup) {
        throw CloudBackupAcknowledgementRequired(
            "Cloud backup failed: $failureReason. Content was not removed. " +
                "Review the warning and explicitly continue with the verified local backup."
        )
    }
    return uploadSucceeded
}

fun backupResultMessage(action: String, result: BackupResult): SaveOperation = when (result) {
    BackupResult.SUCCESS -> SaveOperation("$action completed")
    BackupResult.SOURCE_MISSING -> SaveOperation("$action failed: save file is missing", false)
    BackupResult.BACKUP_MISSING -> SaveOperation("$action failed: backup is missing", false)
    BackupResult.CHECKSUM_MISMATCH -> SaveOperation("$action refused: backup checksum mismatch", false)
    BackupResult.SIZE_LIMIT_EXCEEDED -> SaveOperation("$action refused: selected file exceeds 16 MiB", false)
    BackupResult.CROSS_GAME_PATH -> SaveOperation("$action refused: artifact belongs to another game", false)
    BackupResult.SNAPSHOT_ABORTED -> SaveOperation("$action stopped: another save artifact failed validation", false)
}

interface SaveSafetyController {
    fun observeBusy(): StateFlow<Boolean> = MutableStateFlow(false)
    fun observeState(): StateFlow<SaveSafetyState>
    fun importInitialSave(uri: Uri)
    fun contentRemovalPreview(game: Game): ContentRemovalPreview
    suspend fun cloudBackupPreflight(game: Game): CloudBackupPreflight = CloudBackupPreflight(
        CloudBackupPreflightStatus.NOT_REQUIRED,
        "No managed save copy requires cloud protection.",
    )
    suspend fun uninstallContent(game: Game): String
    suspend fun uninstallContent(game: Game, allowWithoutCloudBackup: Boolean): String = uninstallContent(game)
    fun backupSave()
    fun restoreSave()
    fun exportBackup(uri: Uri)
    fun importBackup(uri: Uri)
    fun uploadCloudSave()
    fun downloadCloudSave()
}

class DefaultSaveSafetyController(
    context: Context,
    private val saveRecordDao: SaveRecordDao,
    private val gameRepository: GameRepository,
    private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
    private val gameId: GameId = GameId("galaxy-patrol"),
    private val externalContent: ExternalLibraryContentStore? = null,
) : SaveSafetyController {
    init { requireSaveGameId(gameId.value) }
    private val applicationContext = context.applicationContext
    private val savesRoot = applicationContext.filesDir.resolve("saves")
    private val initialSaveImporter = InitialSaveImporter(savesRoot)
    private val backupService = SaveBackupService(
        savesRoot,
        applicationContext.filesDir.resolve("save-backups")
    )
    private val cloudSaveOperations = ManagedCloudSaveOperations(
        context = applicationContext,
        saveRecordDao = saveRecordDao,
        settingsRepository = settingsRepository,
        gameId = gameId,
        savesRoot = savesRoot,
        backupService = backupService,
    )
    private val contentRemoval = GameContentRemovalCoordinator(
        context = applicationContext,
        saveRecordDao = saveRecordDao,
        gameRepository = gameRepository,
        gameId = gameId,
        savesRoot = savesRoot,
        backupService = backupService,
        externalContent = externalContent,
        uploadCloudSave = cloudSaveOperations::upload,
        safeCloudError = cloudSaveOperations::safeError,
    )
    private val operation = MutableStateFlow(SaveOperation())
    private val busy = MutableStateFlow(false)
    override fun observeBusy(): StateFlow<Boolean> = busy

    private fun launchSaveOperation(block: suspend () -> Unit) {
        val key = savesRoot.canonicalPath + ":" + gameId.value
        if (!SaveOperationGate.acquire(key)) {
            operation.value = SaveOperation("A save operation is already running for this game", false)
            return
        }
        busy.value = true
        val job = scope.launch {
            // Finish file publication and its database record together when the panel closes.
            withContext(Dispatchers.IO + NonCancellable) {
                try { block() }
                catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    operation.value = SaveOperation("Save operation failed; check storage and retry", false)
                }
            }
        }
        job.invokeOnCompletion {
            SaveOperationGate.release(key)
            busy.value = false
        }
    }
    private val state = combine(saveRecordDao.observe(gameId.value), operation) { sourceRecord, current ->
        val record = sourceRecord?.takeIf {
            it.gameId == gameId.value && runCatching {
                requireSavePathForGame(gameId.value, it.relativePath)
            }.isSuccess
        }
        SaveSafetyState(
            saveRecordPresent = record != null,
            relativePath = record?.relativePath,
            sizeBytes = record?.sizeBytes ?: 0L,
            updatedAtMillis = record?.updatedAtMillis ?: 0L,
            backupPresent = record?.relativePath?.let {
                runCatching { backupService.hasBackup(it) }.getOrDefault(false)
            } ?: false,
            operationMessage = current.message,
            operationSuccessful = current.successful
        )
    }.stateIn(scope, SharingStarted.Eagerly, SaveSafetyState())

    override fun observeState(): StateFlow<SaveSafetyState> = state

    override fun contentRemovalPreview(game: Game): ContentRemovalPreview = contentRemoval.preview(game)

    override suspend fun cloudBackupPreflight(game: Game): CloudBackupPreflight =
        cloudSaveOperations.preflight(game)

    override suspend fun uninstallContent(game: Game): String = contentRemoval.uninstall(game, false)

    override suspend fun uninstallContent(game: Game, allowWithoutCloudBackup: Boolean): String =
        contentRemoval.uninstall(game, allowWithoutCloudBackup)

    override fun importInitialSave(uri: Uri) {
        launchSaveOperation {
            runCatching {
                val relativePath = gameId.value + "/save.dat"
                val bytes = applicationContext.contentResolver.openInputStream(uri)?.use {
                    initialSaveImporter.importNew(relativePath, it)
                } ?: error("Selected document could not be opened")
                saveRecordDao.upsert(record(relativePath, bytes))
            }.onSuccess {
                operation.value = SaveOperation("Save imported into GameBox storage; emulator save synchronization must be configured separately")
            }.onFailure {
                operation.value = SaveOperation("Save import failed; existing save retained. Select a non-empty save up to 16 MiB, or use restore if a save already exists.", false)
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
        launchSaveOperation {
            val relativePath = state.value.relativePath ?: return@launchSaveOperation noSave("Export")
            val result = runCatching {
                applicationContext.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    backupService.exportBackup(relativePath, output)
                } ?: BackupResult.BACKUP_MISSING
            }.getOrElse {
                operation.value = SaveOperation("Export failed: document could not be written", false)
                return@launchSaveOperation
            }
            operation.value = backupResultMessage("Export", result)
        }
    }

    override fun importBackup(uri: Uri) {
        launchSaveOperation {
            val relativePath = state.value.relativePath ?: return@launchSaveOperation noSave("Import")
            val result = runCatching {
                applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                    backupService.importBackup(relativePath, input)
                } ?: BackupResult.BACKUP_MISSING
            }.getOrElse {
                operation.value = SaveOperation("Import failed: document could not be read", false)
                return@launchSaveOperation
            }
            if (result == BackupResult.SUCCESS) {
                saveRecordDao.upsert(record(relativePath, savesRoot.resolve(relativePath).length()))
            }
            operation.value = backupResultMessage("Import", result)
        }
    }

    override fun uploadCloudSave() {
        launchSaveOperation {
            val relativePath = state.value.relativePath ?: return@launchSaveOperation noSave("Cloud upload")
            operation.value = SaveOperation("Uploading encrypted-credential cloud save…")
            val result = runCatching {
                cloudSaveOperations.upload(relativePath, state.value.updatedAtMillis)
            }
            operation.value = result.fold(
                onSuccess = { SaveOperation("Cloud save uploaded and verified") },
                onFailure = { SaveOperation("Cloud upload failed: " + cloudSaveOperations.safeError(it), false) },
            )
        }
    }

    override fun downloadCloudSave() {
        launchSaveOperation {
            val relativePath = state.value.relativePath ?: (gameId.value + "/save.dat")
            operation.value = SaveOperation("Downloading and verifying cloud save…")
            val result = runCatching { cloudSaveOperations.downloadAndRestore(relativePath) }
            operation.value = result.fold(
                onSuccess = { conflictPreserved ->
                    SaveOperation(
                        if (conflictPreserved) "Cloud save restored; previous local copy preserved"
                        else "Cloud save downloaded and verified"
                    )
                },
                onFailure = { SaveOperation("Cloud download failed: " + cloudSaveOperations.safeError(it), false) },
            )
        }
    }

    private fun runBackupAction(
        action: String,
        block: (String) -> BackupResult
    ) {
        launchSaveOperation {
            val relativePath = state.value.relativePath ?: return@launchSaveOperation noSave(action)
            val result = runCatching { block(relativePath) }.getOrElse {
                operation.value = SaveOperation("$action failed safely", false)
                return@launchSaveOperation
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

    private fun record(relativePath: String, sizeBytes: Long): SaveRecordEntity {
        requireSavePathForGame(gameId.value, relativePath)
        return SaveRecordEntity(
        gameId.value,
        relativePath,
        System.currentTimeMillis(),
        sizeBytes
    )
    }
}
