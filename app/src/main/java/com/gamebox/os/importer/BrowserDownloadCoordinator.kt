package com.gamebox.os.importer

import android.app.DownloadManager
import android.content.Context
import android.app.DownloadManager.Request
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.GameId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/** Schedules durable monitoring of DownloadManager transfers and completes authorized-copy imports. */
class BrowserDownloadCoordinator(
    context: Context,
    private val store: BrowserDownloadStore,
    private val importer: AuthorizedRomImporter,
    private val repository: GameRepository,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(DownloadManager::class.java)
    private val workManager = WorkManager.getInstance(appContext)

    fun start() {
        scope.launch(Dispatchers.IO) {
            store.records
                .map { records ->
                    activeBrowserDownloadIds(records) to recoverableManualImportIds(records)
                }
                .distinctUntilChanged()
                .collect { (downloadIds, manualImportIds) ->
                    // A transient WorkManager/database error for one item must not
                    // kill this collector: later state changes and newly queued
                    // transfers still need durable monitoring.
                    downloadIds.forEach { downloadId ->
                        syncDownloadManagerState(downloadId)
                        scheduleMonitorWithRetry(downloadId)
                    }
                    manualImportIds.forEach { importId ->
                        scheduleManualImportWithRetry(importId)
                    }
                }
        }
    }

    /** Schedule monitoring as soon as the browser has durably recorded a new transfer. */
    fun trackImmediately(downloadId: Long) {
        if (downloadId > 0L) runCatching { enqueueMonitor(downloadId) }
    }

    fun startUserInitiatedTransfer(record: BrowserDownloadRecord, cookie: String?): Boolean {
        store.add(record)
        try {
            store.setRetryCookie(record.downloadId, cookie)
        } catch (_: Exception) {
            store.update(record.downloadId) {
                browserDownloadStartFailure(it, "Could not securely save download session; retry from Downloads")
            }
            runCatching { store.setRetryCookie(record.downloadId, null) }
            return false
        }
        val scheduled = runCatching { scheduleBrowserTransfer(appContext, record) }.getOrDefault(false)
        if (!scheduled) {
            store.update(record.downloadId) { browserDownloadStartFailure(it, "Android could not start the user-initiated transfer") }
            runCatching { store.setRetryCookie(record.downloadId, null) }
        }
        return scheduled
    }

    /** Called by the durable WorkManager worker; safe to call again after process recreation. */
    suspend fun monitor(downloadId: Long) {
        val record = store.get(downloadId) ?: return
        if (record.status in ACTIVE_BROWSER_DOWNLOAD_STATUSES) track(downloadId)
    }

    private fun enqueueMonitor(
        downloadId: Long,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
    ) {
        val request = OneTimeWorkRequestBuilder<BrowserDownloadMonitorWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(BrowserDownloadMonitorWorker.KEY_DOWNLOAD_ID to downloadId))
            .addTag(BrowserDownloadMonitorWorker.TAG)
            .build()
        workManager.enqueueUniqueWork(
            browserDownloadWorkName(downloadId),
            policy,
            request,
        )
    }

    /**
     * Android broadcasts completion independently of GameBox's Activity/process. Replace a
     * delayed retry chain so verification and automatic import start promptly after completion.
     */
    fun resumeAfterSystemCompletion(downloadId: Long) {
        val record = store.get(downloadId) ?: return
        if (!shouldResumeBrowserDownloadAfterCompletion(record.status)) return
        if (downloadManagerStatus(downloadId) != DownloadManager.STATUS_SUCCESSFUL) return
        runCatching { enqueueMonitor(downloadId, ExistingWorkPolicy.REPLACE) }
    }

    private suspend fun scheduleMonitorWithRetry(downloadId: Long) {
        var waitMillis = 1_000L
        repeat(4) { attempt ->
            if (runCatching { enqueueMonitor(downloadId) }.isSuccess) return
            if (attempt < 3) {
                delay(waitMillis)
                waitMillis *= 4
            }
        }
    }

    private suspend fun scheduleManualImportWithRetry(importId: Long) {
        var waitMillis = 1_000L
        repeat(4) { attempt ->
            if (runCatching { enqueueManualRomImportWork(appContext, importId) }.isSuccess) return
            if (attempt < 3) {
                delay(waitMillis)
                waitMillis *= 4
            }
        }
    }

    /** Restarts a failed Android DownloadManager request without losing its tracked game metadata. */
    fun retry(downloadId: Long): Boolean {
        val record = store.get(downloadId) ?: return false
        val canRestartQueuedDownload = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            record.status == BrowserDownloadStatus.QUEUED && record.transferJobId == null
        if (record.status !in setOf(BrowserDownloadStatus.FAILED, BrowserDownloadStatus.PAUSED) &&
            !canRestartQueuedDownload
        ) return false
        val url = record.downloadUrl ?: return false
        if (record.transferJobId != null) {
            val scheduled = runCatching { scheduleBrowserTransfer(appContext, record) }.getOrDefault(false)
            if (scheduled) store.update(downloadId) {
                it.copy(
                    status = BrowserDownloadStatus.QUEUED,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    bytesPerSecond = 0L,
                    error = null,
                )
            }
            return scheduled
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val androidStatus = downloadManagerStatus(downloadId)
            if (androidStatus == DownloadManager.STATUS_RUNNING || androidStatus == DownloadManager.STATUS_SUCCESSFUL) {
                return false
            }
            val migrated = record.copy(
                transferJobId = newBrowserTransferJobId(),
                status = BrowserDownloadStatus.QUEUED,
                downloadedBytes = 0L,
                totalBytes = 0L,
                bytesPerSecond = 0L,
                error = null,
            )
            store.update(downloadId) { migrated }
            val scheduled = runCatching { scheduleBrowserTransfer(appContext, migrated) }.getOrDefault(false)
            if (scheduled) manager.remove(downloadId) else store.update(downloadId) { record }
            return scheduled
        }
        val uri = runCatching { Uri.parse(url) }.getOrNull()
            ?.takeIf { it.scheme == "https" && !it.host.isNullOrBlank() } ?: return false
        return runCatching {
            val oldStatus = downloadManagerStatus(downloadId)
            val stagedDirectory = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (oldStatus in setOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING)) {
                return false
            }
            if (oldStatus in setOf(DownloadManager.STATUS_SUCCESSFUL, null) && stagedDirectory != null &&
                hasRecoverableStagedDownload(java.io.File(stagedDirectory, record.stagedName))
            ) {
                store.update(downloadId) {
                    it.copy(
                        status = BrowserDownloadStatus.QUEUED,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        bytesPerSecond = 0L,
                        error = null,
                    )
                }
                return true
            }
            val stagedName = "${java.util.UUID.randomUUID()}.${record.displayName.substringAfterLast('.').lowercase()}"
            val request = Request(uri)
                .setTitle(record.displayName)
                .setNotificationVisibility(Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, stagedName)
            record.mimeType?.takeIf(String::isNotBlank)?.let(request::setMimeType)
            record.userAgent?.takeIf(String::isNotBlank)?.let { request.addRequestHeader("User-Agent", it) }
            record.referer?.takeIf(String::isNotBlank)?.let { request.addRequestHeader("Referer", it) }
            store.retryCookie(downloadId)?.takeIf(String::isNotBlank)?.let { request.addRequestHeader("Cookie", it) }
            val newId = manager.enqueue(request)
            try {
                store.setRetryCookie(newId, store.retryCookie(downloadId))
                store.replace(downloadId, record.copy(
                    downloadId = newId,
                    stagedName = stagedName,
                    status = BrowserDownloadStatus.QUEUED,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    bytesPerSecond = 0L,
                    error = null,
                ))
            } catch (error: Exception) {
                manager.remove(newId)
                store.setRetryCookie(newId, null)
                throw error
            }
            runCatching { store.setRetryCookie(downloadId, null) }
            manager.remove(downloadId)
        }.isSuccess
    }

    private fun downloadManagerStatus(downloadId: Long): Int? = manager.query(
        DownloadManager.Query().setFilterById(downloadId),
    )?.use { cursor ->
        if (!cursor.moveToFirst()) null
        else cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
    }

    private fun syncDownloadManagerState(downloadId: Long) {
        manager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
            if (!cursor.moveToFirst()) return
            val androidStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (androidStatus == DownloadManager.STATUS_SUCCESSFUL) return
            val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val status = if (androidStatus == DownloadManager.STATUS_FAILED) {
                BrowserDownloadStatus.FAILED
            } else {
                browserDownloadTransferStatus(androidStatus)
            }
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            store.update(downloadId) {
                it.copy(
                    status = status,
                    downloadedBytes = bytes,
                    totalBytes = total,
                    bytesPerSecond = 0L,
                    error = if (status == BrowserDownloadStatus.FAILED) "Android download failed (code $reason)" else null,
                )
            }
        }
    }

    private suspend fun track(downloadId: Long) {
        try {
            val record = store.get(downloadId) ?: return
            var lastSampleAtNanos = 0L
            var lastDownloadedBytes = 0L
            val candidate = BrowserDownloadTracker(appContext).awaitCompletedDownload(
                id = downloadId,
                stagedName = record.stagedName,
                displayName = record.displayName,
                timeoutMillis = 7L * 24 * 60 * 60 * 1_000,
                onProgress = { progress ->
                    val sampledAtNanos = System.nanoTime()
                    val elapsedNanos = sampledAtNanos - lastSampleAtNanos
                    val speed = if (
                        lastSampleAtNanos > 0L && elapsedNanos > 0L &&
                        progress.downloadedBytes >= lastDownloadedBytes &&
                        progress.status != BrowserDownloadStatus.PAUSED
                    ) {
                        (((progress.downloadedBytes - lastDownloadedBytes).toDouble() * 1_000_000_000.0) /
                            elapsedNanos.toDouble()).toLong().coerceAtLeast(0L)
                    } else 0L
                    lastSampleAtNanos = sampledAtNanos
                    lastDownloadedBytes = progress.downloadedBytes
                    store.update(downloadId) { current ->
                        current.copy(
                            status = progress.status,
                            downloadedBytes = progress.downloadedBytes,
                            totalBytes = progress.totalBytes,
                            bytesPerSecond = speed,
                        )
                    }
                },
            )
            importStagedBrowserDownload(downloadId, candidate)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            store.update(downloadId) {
                it.copy(
                    status = BrowserDownloadStatus.FAILED,
                    bytesPerSecond = 0L,
                    error = error.message?.take(180) ?: "Download tracking failed",
                )
            }
        }
    }

    suspend fun importStagedBrowserDownload(downloadId: Long, candidate: BrowserDownloadCandidate) {
        store.update(downloadId) { it.copy(status = BrowserDownloadStatus.IMPORTING, bytesPerSecond = 0L) }
        val current = store.get(downloadId) ?: return
        val game = DiscoveryGame(
            id = GameId(current.gameId),
            title = current.title,
            platformId = current.platformId,
            region = current.region,
            releaseDate = current.releaseDate,
            description = current.description,
            players = current.players,
            rating = current.rating,
            coverUrl = current.coverUrl,
            backgroundUrl = current.backgroundUrl,
            logoUrl = current.logoUrl,
            screenshots = emptyList(),
            favorite = false,
        )
        val result = DiscoveryImportWorkflow(
            context = appContext,
            importer = importer,
            repository = repository,
            game = game,
            platformName = current.platformName,
        ).importSingle(
            candidate.uri,
            candidate.displayName,
            removeSourceAfterSuccess = true,
            onProgress = { progress ->
                store.update(downloadId) {
                    it.copy(
                        importStage = progress.stage.label,
                        importBytesProcessed = progress.bytesProcessed,
                        importTotalBytes = progress.totalBytes,
                        importFileIndex = progress.fileIndex,
                        importFileCount = progress.fileCount,
                    )
                }
            },
        )
        if (!result.succeeded) {
            store.update(downloadId) { it.copy(status = BrowserDownloadStatus.FAILED, error = result.message) }
        } else {
            store.update(downloadId) { it.copy(status = BrowserDownloadStatus.IMPORTED, error = null) }
            store.setRetryCookie(downloadId, null)
            if (current.transferJobId == null) manager.remove(downloadId)
        }
    }

}

internal fun activeBrowserDownloadIds(records: List<BrowserDownloadRecord>): List<Long> =
    records.asSequence()
        .filter { it.status in ACTIVE_BROWSER_DOWNLOAD_STATUSES }
        .filter { it.transferJobId == null }
        .map(BrowserDownloadRecord::downloadId)
        .filter { it > 0L }
        .distinct()
        .toList()

internal fun recoverableManualImportIds(records: List<BrowserDownloadRecord>): List<Long> =
    records.asSequence()
        .filter {
            it.manualImport && (it.status == BrowserDownloadStatus.QUEUED ||
                it.status == BrowserDownloadStatus.IMPORTING)
        }
        .map(BrowserDownloadRecord::downloadId)
        .filter { it < 0L }
        .distinct()
        .toList()

internal fun shouldResumeBrowserDownloadAfterCompletion(status: BrowserDownloadStatus): Boolean =
    status in setOf(
        BrowserDownloadStatus.QUEUED,
        BrowserDownloadStatus.DOWNLOADING,
        BrowserDownloadStatus.PAUSED,
    )

internal fun browserDownloadStartFailure(
    record: BrowserDownloadRecord,
    reason: String,
): BrowserDownloadRecord = record.copy(status = BrowserDownloadStatus.FAILED, error = reason.take(180))

internal fun browserDownloadWorkName(downloadId: Long): String =
    "gamebox-browser-download:$downloadId"
