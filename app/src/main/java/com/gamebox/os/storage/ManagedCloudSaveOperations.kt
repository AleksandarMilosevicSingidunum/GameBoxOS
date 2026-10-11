package com.gamebox.os.storage

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gamebox.os.catalog.AwsSignatureV4Signer
import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.data.local.SaveRecordDao
import com.gamebox.os.data.local.SaveRecordEntity
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.save.CloudSaveEndpointPolicy
import com.gamebox.os.save.CloudSaveEnvelopeCodec
import com.gamebox.os.save.CloudSaveProvider
import com.gamebox.os.save.CloudSaveSyncRequest
import com.gamebox.os.save.HttpsCloudSaveTransportClient
import com.gamebox.os.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.net.URI

/** Cloud-specific connectivity, credential, encryption, conflict, and transport policy. */
internal class ManagedCloudSaveOperations(
    context: Context,
    private val saveRecordDao: SaveRecordDao,
    private val settingsRepository: SettingsRepository,
    private val gameId: GameId,
    private val savesRoot: File,
    private val backupService: SaveBackupService,
) {
    private val applicationContext = context.applicationContext

    suspend fun preflight(game: Game): CloudBackupPreflight = withContext(Dispatchers.IO) {
        require(game.id == gameId) { "Content controller does not belong to this game" }
        val record = saveRecordDao.getByGameId(game.id.value)
            ?: return@withContext CloudBackupPreflight(
                CloudBackupPreflightStatus.NOT_REQUIRED,
                "No managed save copy requires cloud protection.",
            )
        requireSavePathForGame(game.id.value, record.relativePath)
        runCatching { cloudAccess() }.fold(
            onSuccess = {
                CloudBackupPreflight(
                    CloudBackupPreflightStatus.READY,
                    "Cloud backup is configured and will be verified before content removal.",
                )
            },
            onFailure = {
                CloudBackupPreflight(
                    CloudBackupPreflightStatus.ACKNOWLEDGEMENT_REQUIRED,
                    "Cloud backup is unavailable: ${safeError(it)}. A verified local backup is still required.",
                )
            },
        )
    }

    suspend fun upload(relativePath: String, updatedAtMillis: Long) {
        val cloud = cloudAccess()
        val saveFile = resolveSave(relativePath)
        require(saveFile.isFile) { "Save file is missing" }
        require(saveFile.length() <= CloudSaveEnvelopeCodec.MAX_RAW_PAYLOAD_BYTES) {
            "Save exceeds the 15 MiB cloud payload limit"
        }
        val envelope = CloudSaveEnvelopeCodec.encode(
            gameId.value,
            updatedAtMillis.coerceAtLeast(0L),
            saveFile.readBytes(),
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

    /** Downloads, authenticates, backs up, and restores a cloud save; returns true if local data was preserved. */
    suspend fun downloadAndRestore(relativePath: String): Boolean {
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
        if (conflictPreserved) preserveConflict(requireNotNull(localPayload))

        val importResult = backupService.importBackup(
            relativePath,
            ByteArrayInputStream(remote.payload),
            CloudSaveEnvelopeCodec.MAX_RAW_PAYLOAD_BYTES.toLong(),
        )
        require(importResult == BackupResult.SUCCESS) {
            backupResultMessage("Cloud download", importResult).message ?: "Cloud save import failed safely"
        }
        require(backupService.restore(relativePath) == BackupResult.SUCCESS) {
            "Cloud save restore failed safely"
        }
        saveRecordDao.upsert(
            SaveRecordEntity(
                gameId.value,
                relativePath,
                remote.updatedAtMillis,
                remote.payload.size.toLong(),
            ),
        )
        return conflictPreserved
    }

    fun safeError(error: Throwable): String =
        error.message?.replace(Regex("https://\\S+"), "remote endpoint")?.take(180)
            ?: "operation failed safely"

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

    private fun resolveSave(relativePath: String): File =
        resolveGameSave(savesRoot, gameId.value, relativePath)

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
                partial.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
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

    private data class CloudAccess(
        val endpoint: URI,
        val credentials: CatalogCredentials,
        val client: HttpsCloudSaveTransportClient,
    )
}
