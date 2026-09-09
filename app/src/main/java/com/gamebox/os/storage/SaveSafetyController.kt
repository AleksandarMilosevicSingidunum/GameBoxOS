package com.gamebox.os.storage

import android.content.Context
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gamebox.os.catalog.AwsSignatureV4Signer
import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.local.SaveRecordDao
import com.gamebox.os.data.local.SaveRecordEntity
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.Game
import com.gamebox.os.content.GameContentPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import com.gamebox.os.domain.InstallState
import com.gamebox.os.download.AssetDownloadWorker
import com.gamebox.os.download.AuthorizedHomebrewDownload
import com.gamebox.os.save.CloudSaveEndpointPolicy
import com.gamebox.os.save.CloudSaveEnvelopeCodec
import com.gamebox.os.save.CloudSaveProvider
import com.gamebox.os.save.CloudSaveSyncRequest
import com.gamebox.os.save.HttpsCloudSaveTransportClient
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.flow.first

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

fun backupResultMessage(action: String, result: BackupResult): SaveOperation = when (result) {
    BackupResult.SUCCESS -> SaveOperation("$action completed")
    BackupResult.SOURCE_MISSING -> SaveOperation("$action failed: save file is missing", false)
    BackupResult.BACKUP_MISSING -> SaveOperation("$action failed: backup is missing", false)
    BackupResult.CHECKSUM_MISMATCH -> SaveOperation("$action refused: backup checksum mismatch", false)
    BackupResult.SIZE_LIMIT_EXCEEDED -> SaveOperation("$action refused: selected file exceeds 16 MiB", false)
    BackupResult.CROSS_GAME_PATH -> SaveOperation("$action refused: artifact belongs to another game", false)
}

interface SaveSafetyController {
    fun observeBusy(): StateFlow<Boolean> = MutableStateFlow(false)
    fun observeState(): StateFlow<SaveSafetyState>
    fun importInitialSave(uri: Uri)
    fun uninstallPreview(): UninstallConfirmation
    fun uninstallTestContent()
    fun contentRemovalPreview(game: Game): ContentRemovalPreview
    suspend fun uninstallContent(game: Game): String
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
) : SaveSafetyController {
    init { requireSaveGameId(gameId.value) }
    private val applicationContext = context.applicationContext
    private val savesRoot = applicationContext.filesDir.resolve("saves")
    private val initialSaveImporter = InitialSaveImporter(savesRoot)
    private val backupService = SaveBackupService(
        savesRoot,
        applicationContext.filesDir.resolve("save-backups")
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

    override fun contentRemovalPreview(game: Game): ContentRemovalPreview =
        GameOwnedContentUninstaller(applicationContext.filesDir).preview(contentManifest(game))

    override suspend fun uninstallContent(game: Game): String = withContext(Dispatchers.IO + NonCancellable) {
        val current = requireNotNull(gameRepository.game(game.id)) { "Game is no longer in the library" }
        require(current.state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE, InstallState.MISSING_FILES)) {
            "Wait for installation or download operations to finish"
        }
        val manifest = contentManifest(current)
        require(manifest == contentManifest(game)) { "Game content changed; reopen the confirmation" }
        try {
            val removed = GameOwnedContentUninstaller(applicationContext.filesDir).uninstall(manifest)
            gameRepository.setInstallStateAndAwait(game.id, InstallState.NOT_INSTALLED)
            "$removed content file(s) removed. Saves, backups, metadata and history retained."
        } catch (error: ContentRemovalFailed) {
            if (error.removedFiles > 0) gameRepository.setInstallStateAndAwait(game.id, InstallState.MISSING_FILES)
            throw IllegalStateException("Content removal stopped after ${error.removedFiles} file(s). Saves were not touched; retry to remove remaining content.", error)
        }
    }

    private fun contentManifest(game: Game): ContentRemovalManifest {
        val primary = game.localContentRelativePath
        val paths = if (primary != null) {
            (listOf(primary) + game.localContentFiles.map { it.relativePath }).distinct().map { "imports/$it" }
        } else if (game.id.value == "galaxy-patrol") {
            listOf("installed/" + AuthorizedHomebrewDownload.RELATIVE_PATH)
        } else {
            require(game.expectedSha256 != null) { "No managed game content is recorded" }
            listOf("installed/" + GameContentPolicy.describe(game.id.value, game.platform, game.sourceUrl).relativePath)
        }
        return ContentRemovalManifest(game.id.value, paths)
    }

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
                val cloud = cloudAccess()
                val saveFile = resolveSave(relativePath)
                require(saveFile.isFile) { "Save file is missing" }
                require(saveFile.length() <= CloudSaveEnvelopeCodec.MAX_RAW_PAYLOAD_BYTES) {
                    "Save exceeds the 15 MiB cloud payload limit"
                }
                val payload = saveFile.readBytes()
                val envelope = CloudSaveEnvelopeCodec.encode(
                    gameId.value,
                    state.value.updatedAtMillis.coerceAtLeast(0L),
                    payload,
                )
                val request = CloudSaveSyncRequest(
                    gameId = gameId.value,
                    endpoint = cloud.endpoint,
                    payloadBytes = envelope.size.toLong(),
                    credentialKey = "cloud-save",
                    expectedSha256 = CloudSaveEnvelopeCodec.sha256(envelope),
                )
                cloud.client.upload(request, envelope, cloud.credentials)
            }
            operation.value = result.fold(
                onSuccess = { SaveOperation("Cloud save uploaded and verified") },
                onFailure = { SaveOperation("Cloud upload failed: " + safeCloudError(it), false) },
            )
        }
    }

    override fun downloadCloudSave() {
        launchSaveOperation {
            val relativePath = state.value.relativePath ?: (gameId.value + "/save.dat")
            operation.value = SaveOperation("Downloading and verifying cloud save…")
            val result = runCatching {
                val cloud = cloudAccess()
                val request = CloudSaveSyncRequest(
                    gameId = gameId.value,
                    endpoint = cloud.endpoint,
                    payloadBytes = 0L,
                    credentialKey = "cloud-save",
                )
                val encoded = cloud.client.download(request, cloud.credentials)
                val remote = CloudSaveEnvelopeCodec.decode(gameId.value, encoded)
                val saveFile = resolveSave(relativePath)
                val localPayload = saveFile.takeIf(File::isFile)?.readBytes()
                val conflictPreserved = localPayload != null &&
                    CloudSaveEnvelopeCodec.sha256(localPayload) != remote.payloadSha256
                if (conflictPreserved) preserveConflict(localPayload)
                val importResult = backupService.importBackup(
                    relativePath,
                    ByteArrayInputStream(remote.payload),
                    CloudSaveEnvelopeCodec.MAX_RAW_PAYLOAD_BYTES.toLong(),
                )
                require(importResult == BackupResult.SUCCESS) {
                    backupResultMessage("Cloud download", importResult).message ?: "Cloud save import failed safely"
                }
                require(backupService.restore(relativePath) == BackupResult.SUCCESS) { "Cloud save restore failed safely" }
                saveRecordDao.upsert(
                    SaveRecordEntity(
                        gameId.value,
                        relativePath,
                        remote.updatedAtMillis,
                        remote.payload.size.toLong(),
                    )
                )
                conflictPreserved
            }
            operation.value = result.fold(
                onSuccess = { conflictPreserved ->
                    SaveOperation(
                        if (conflictPreserved) "Cloud save restored; previous local copy preserved"
                        else "Cloud save downloaded and verified"
                    )
                },
                onFailure = { SaveOperation("Cloud download failed: " + safeCloudError(it), false) },
            )
        }
    }

    override fun uninstallPreview(): UninstallConfirmation {
        require(gameId.value == "galaxy-patrol") { "Use the general content-removal flow for this game" }
        val contentFile = applicationContext.filesDir
            .resolve(AssetDownloadWorker.INSTALL_ROOT)
            .resolve(AuthorizedHomebrewDownload.RELATIVE_PATH)
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
        require(gameId.value == "galaxy-patrol") { "Use the general content-removal flow for this game" }
        launchSaveOperation {
            val result = runCatching {
                FileContentUninstaller(
                    applicationContext.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT)
                ).uninstall(AuthorizedHomebrewDownload.RELATIVE_PATH)
            }
            if (result.isFailure) {
                operation.value = SaveOperation("Uninstall failed safely", false)
                return@launchSaveOperation
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

    private suspend fun cloudAccess(): CloudAccess {
        require(networkAvailable()) { "Network is offline; try again when connected" }
        val settings = settingsRepository.settings.first()
        val provider = runCatching { CloudSaveProvider.valueOf(settings.cloudSaveProvider.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Cloud save provider is invalid") }
        require(settings.cloudSaveEndpoint.isNotBlank()) { "Configure a cloud save endpoint in Settings" }
        val endpoint = CloudSaveEndpointPolicy.objectUri(settings.cloudSaveEndpoint, gameId.value)
        val region = CloudSaveEndpointPolicy.requireRegion(provider, settings.cloudSaveRegion)
        val credentials = settingsRepository.cloudSaveCredentials(provider.name)
            ?: throw IllegalArgumentException("Configure cloud save credentials in Settings")
        val signer = if (provider == CloudSaveProvider.S3) AwsSignatureV4Signer(region) else null
        return CloudAccess(endpoint, credentials, HttpsCloudSaveTransportClient(s3Signer = signer))
    }

    private fun resolveSave(relativePath: String): File {
        requireSavePathForGame(gameId.value, relativePath)
        val root = savesRoot.canonicalFile
        val file = File(root, relativePath).canonicalFile
        require(file.path.startsWith(root.path + File.separator)) { "Save path escaped app storage" }
        return file
    }

    private fun preserveConflict(payload: ByteArray) {
        val root = applicationContext.filesDir.resolve("save-conflicts").canonicalFile
        val directory = root.resolve(gameId.value).canonicalFile
        require(directory.path.startsWith(root.path + File.separator))
        check(directory.mkdirs() || directory.isDirectory)
        val destination = directory.resolve("local-${System.currentTimeMillis()}.save")
        val partial = directory.resolve(destination.name + ".partial")
        partial.writeBytes(payload)
        runCatching {
            Files.move(
                partial.toPath(), destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
            )
        }.getOrElse {
            Files.move(partial.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun networkAvailable(): Boolean {
        val manager = applicationContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun safeCloudError(error: Throwable): String =
        error.message?.replace(Regex("https://\\S+"), "remote endpoint")?.take(180)
            ?: "operation failed safely"

    private data class CloudAccess(
        val endpoint: java.net.URI,
        val credentials: com.gamebox.os.catalog.CatalogCredentials,
        val client: HttpsCloudSaveTransportClient,
    )

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
