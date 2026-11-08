package com.gamebox.os.importer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Runs a picker-accepted import independently of the details screen and app process. */
class ManualRomImportWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_IMPORT_ID, 0L)
        val app = applicationContext.applicationContext as? GameBoxApplication ?: return Result.failure()
        val store = app.browserDownloadStore
        val record = store.get(id)?.takeIf { it.manualImport } ?: return Result.failure()
        val uris = record.sourceUris.map(Uri::parse)
        try {
            store.update(id) { it.copy(status = BrowserDownloadStatus.IMPORTING, error = null) }
            require(uris.isNotEmpty() && record.sourceNames.size == uris.size) { "Selected file list is incomplete" }
            val game = DiscoveryGame(
                id = GameId(record.gameId),
                title = record.title,
                platformId = record.platformId,
                region = record.region,
                releaseDate = record.releaseDate,
                description = record.description,
                players = record.players,
                rating = record.rating,
                coverUrl = record.coverUrl,
                backgroundUrl = record.backgroundUrl,
                logoUrl = record.logoUrl,
                screenshots = emptyList(),
                favorite = record.favorite,
            )
            val workflow = DiscoveryImportWorkflow(
                applicationContext,
                app.container.authorizedRomImporter,
                app.container.gameRepository,
                game,
                record.platformName,
            )
            val updateProgress: (RomImportProgress) -> Unit = { progress ->
                store.update(id) {
                    it.copy(
                        status = BrowserDownloadStatus.IMPORTING,
                        importStage = progress.stage.label,
                        importBytesProcessed = progress.bytesProcessed,
                        importTotalBytes = progress.totalBytes,
                        importFileIndex = progress.fileIndex,
                        importFileCount = progress.fileCount,
                    )
                }
            }
            val outcome = if (uris.size == 1) {
                workflow.importSingle(
                    uris.single(),
                    record.displayName,
                    removeSourceAfterSuccess = shouldRemoveImportedArchive(record.displayName),
                    onProgress = updateProgress,
                )
            } else {
                workflow.importDiscSet(
                    uris.mapIndexed { index, uri -> RomImportSource(uri, record.sourceNames[index]) },
                    updateProgress,
                )
            }
            store.update(id) {
                it.copy(
                    status = if (outcome.succeeded) BrowserDownloadStatus.IMPORTED else BrowserDownloadStatus.FAILED,
                    error = outcome.message.takeUnless { outcome.succeeded },
                )
            }
            return if (outcome.succeeded) Result.success() else Result.failure()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                store.update(id) {
                    it.copy(status = BrowserDownloadStatus.CANCELLED, error = "Import cancelled; no incomplete game was added.")
                }
                releaseManualImportAccess(applicationContext, record)
            }
            throw cancelled
        } catch (error: Exception) {
            store.update(id) {
                it.copy(status = BrowserDownloadStatus.FAILED, error = error.message?.take(180) ?: "Import failed")
            }
            return Result.failure()
        } finally {
            if (!isStopped) releaseManualImportAccess(applicationContext, record)
        }
    }

    companion object {
        const val KEY_IMPORT_ID = "manual_import_id"
        fun workName(id: Long) = "gamebox-manual-import:$id"
    }
}

internal fun enqueueManualRomImport(
    context: Context,
    store: BrowserDownloadStore,
    game: DiscoveryGame,
    platformName: String,
    uris: List<Uri>,
    sourceNames: List<String>,
    grantFlags: Int,
): BrowserDownloadRecord {
    require(uris.isNotEmpty() && uris.size == sourceNames.size) { "Select one or more valid files" }
    require(grantFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) { "Android did not grant persistent read access" }
    val id = -java.util.UUID.randomUUID().leastSignificantBits.ushr(1).coerceAtLeast(1L)
    val record = BrowserDownloadRecord(
        downloadId = id,
        stagedName = sourceNames.first(),
        displayName = if (sourceNames.size == 1) sourceNames.first() else "${sourceNames.size} selected files",
        gameId = game.id.value,
        title = game.title,
        platformName = platformName,
        platformId = game.platformId,
        region = game.region,
        releaseDate = game.releaseDate,
        description = game.description,
        players = game.players,
        rating = game.rating,
        coverUrl = game.coverUrl,
        backgroundUrl = game.backgroundUrl,
        logoUrl = game.logoUrl,
        manualImport = true,
        sourceUris = uris.map(Uri::toString),
        sourceNames = sourceNames,
        sourceGrantFlags = grantFlags,
        createdAtMillis = System.currentTimeMillis(),
        favorite = game.favorite,
    )
    val granted = mutableListOf<Uri>()
    try {
        uris.forEach { uri ->
            context.applicationContext.contentResolver.takePersistableUriPermission(uri, grantFlags)
            granted += uri
        }
        store.add(record)
        enqueueManualRomImportWork(context.applicationContext, id)
        return record
    } catch (error: Exception) {
        store.get(id)?.let { stored ->
            runCatching { store.update(id) { it.copy(status = BrowserDownloadStatus.FAILED, error = error.message) } }
            releaseManualImportAccess(context, stored.copy(sourceUris = granted.map(Uri::toString)))
        } ?: granted.forEach { uri -> runCatching {
            context.applicationContext.contentResolver.releasePersistableUriPermission(uri, grantFlags)
        } }
        throw error
    }
}

internal fun enqueueManualRomImportWork(context: Context, id: Long) {
    val request = OneTimeWorkRequestBuilder<ManualRomImportWorker>()
        .setInputData(workDataOf(ManualRomImportWorker.KEY_IMPORT_ID to id))
        .addTag("gamebox-manual-rom-import")
        .build()
    WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
        ManualRomImportWorker.workName(id), ExistingWorkPolicy.KEEP, request,
    )
}

internal fun cancelManualRomImport(context: Context, store: BrowserDownloadStore, record: BrowserDownloadRecord) {
    if (!record.manualImport || record.status !in ACTIVE_BROWSER_DOWNLOAD_STATUSES) return
    WorkManager.getInstance(context.applicationContext)
        .cancelUniqueWork(ManualRomImportWorker.workName(record.downloadId))
        .result.addListener({
            if (store.get(record.downloadId)?.status in ACTIVE_BROWSER_DOWNLOAD_STATUSES) {
                store.update(record.downloadId) {
                    it.copy(status = BrowserDownloadStatus.CANCELLED, error = "Import cancelled; no incomplete game was added.")
                }
                releaseManualImportAccess(context, record)
            }
        }, java.util.concurrent.Executor { it.run() })
}

internal fun releaseManualImportAccess(context: Context, record: BrowserDownloadRecord) {
    if (record.sourceGrantFlags == 0) return
    record.sourceUris.map(Uri::parse).forEach { uri -> runCatching {
        context.applicationContext.contentResolver.releasePersistableUriPermission(uri, record.sourceGrantFlags)
    } }
}
