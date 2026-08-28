package com.gamebox.os.storage

import android.content.Context
import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.local.SaveRecordDao
import com.gamebox.os.data.local.SaveRecordEntity
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.download.AssetDownloadWorker
import com.gamebox.os.download.AuthorizedTestDownload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class SaveSafetyState(
    val saveRecordPresent: Boolean = false,
    val relativePath: String? = null,
    val sizeBytes: Long = 0L,
    val backupPresent: Boolean = false
)

interface SaveSafetyController {
    fun observeState(): StateFlow<SaveSafetyState>
    fun createTestSaveRecord()
    fun uninstallTestContent()
    fun backupSave()
    fun restoreSave()
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
    private val state = saveRecordDao.observe(gameId.value)
        .map { record ->
            SaveSafetyState(
                saveRecordPresent = record != null,
                relativePath = record?.relativePath,
                sizeBytes = record?.sizeBytes ?: 0L,
                backupPresent = record?.relativePath?.let {
                    runCatching { backupService.hasBackup(it) }.getOrDefault(false)
                } ?: false
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, SaveSafetyState())

    override fun observeState(): StateFlow<SaveSafetyState> = state

    override fun createTestSaveRecord() {
        scope.launch {
            val relativePath = "retro-test/save.dat"
            val root = applicationContext.filesDir.resolve("saves").canonicalFile
            val saveFile = File(root, relativePath).canonicalFile
            require(saveFile.path.startsWith(root.path + File.separator))
            check(saveFile.parentFile?.mkdirs() != false || saveFile.parentFile?.isDirectory == true)
            if (!saveFile.exists()) saveFile.writeText("SAVE")
            saveRecordDao.upsert(
                SaveRecordEntity(
                    gameId = gameId.value,
                    relativePath = relativePath,
                    updatedAtMillis = System.currentTimeMillis(),
                    sizeBytes = saveFile.length()
                )
            )
        }
    }

    override fun backupSave() {
        scope.launch {
            val relativePath = state.value.relativePath ?: return@launch
            if (backupService.createBackup(relativePath) == BackupResult.SUCCESS) {
                saveRecordDao.upsert(
                    SaveRecordEntity(
                        gameId.value,
                        relativePath,
                        System.currentTimeMillis(),
                        savesRoot.resolve(relativePath).length()
                    )
                )
            }
        }
    }

    override fun restoreSave() {
        scope.launch {
            val relativePath = state.value.relativePath ?: return@launch
            if (backupService.restore(relativePath) == BackupResult.SUCCESS) {
                saveRecordDao.upsert(
                    SaveRecordEntity(
                        gameId.value,
                        relativePath,
                        System.currentTimeMillis(),
                        savesRoot.resolve(relativePath).length()
                    )
                )
            }
        }
    }

    override fun uninstallTestContent() {
        scope.launch {
            FileContentUninstaller(
                applicationContext.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT)
            ).uninstall(AuthorizedTestDownload.RELATIVE_PATH)
            gameRepository.setInstallState(gameId, InstallState.NOT_INSTALLED)
        }
    }
}
