package com.gamebox.os.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.gamebox.os.domain.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class RemoteDownloadWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val gameId = inputData.getString(KEY_GAME_ID) ?: return@withContext failure("missing game")
        setForeground(createForegroundInfo(gameId, 0L, -1L))
        val sourceUrl = inputData.getString(KEY_SOURCE_URL) ?: return@withContext failure("missing source")
        val checksum = inputData.getString(KEY_SHA256) ?: return@withContext failure("missing checksum")
        val relativePath = inputData.getString(KEY_RELATIVE_PATH) ?: return@withContext failure("missing path")
        val maxBytes = inputData.getLong(KEY_MAX_BYTES, -1L)
        if (maxBytes <= 0L) return@withContext failure("invalid size limit")

        val staging = FileStagingTarget(
            applicationContext.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT),
            relativePath
        )
        val remainingCapacity = (maxBytes - staging.stagedBytes).coerceAtLeast(0L)
        val requiredSpace = remainingCapacity + STORAGE_RESERVE_BYTES
        if (applicationContext.filesDir.usableSpace < requiredSpace) {
            return@withContext failure("insufficient storage")
        }

        val result = runCatching {
            ResumableTransferEngine().transfer(
                source = HttpsTransferSource(sourceUrl, totalBytes = null, expectedSha256 = checksum),
                staging = staging,
                maxBytes = maxBytes,
                isPausedOrCancelled = { isStopped },
                onProgress = {
                    setForegroundAsync(
                        createForegroundInfo(gameId, it.bytesTransferred, it.totalBytes ?: -1L)
                    )
                    setProgressAsync(
                        workDataOf(
                            KEY_BYTES_TRANSFERRED to it.bytesTransferred,
                            KEY_TOTAL_BYTES to (it.totalBytes ?: -1L)
                        )
                    )
                }
            )
        }.getOrElse { return@withContext failure(it.message ?: "download configuration failed") }

        when (result) {
            is ResumableTransferResult.Success -> {
                showCompletionNotification(gameId, result.bytesTransferred)
                Result.success(workDataOf(KEY_BYTES_TRANSFERRED to result.bytesTransferred))
            }
            is ResumableTransferResult.Paused ->
                failure("paused")
            is ResumableTransferResult.ChecksumMismatch -> failure("checksum mismatch")
            is ResumableTransferResult.SizeLimitExceeded -> failure("size limit exceeded")
            is ResumableTransferResult.Failed ->
                if (runAttemptCount < MAX_RETRIES) Result.retry() else failure(result.reason)
        }
    }

    private fun createForegroundInfo(gameId: String, transferred: Long, total: Long): ForegroundInfo {
        ensureNotificationChannel()
        val determinate = total > 0L
        val progress = if (determinate) ((transferred * 100L) / total).toInt().coerceIn(0, 100) else 0
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading " + gameId)
            .setContentText(formatTransferProgress(transferred, total))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, !determinate)
            .build()
        return ForegroundInfo(notificationId(gameId), notification)
    }

    private fun showCompletionNotification(gameId: String, transferred: Long) {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(gameId + " installed")
            .setContentText(formatTransferProgress(transferred, transferred))
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(notificationId(gameId), notification)
    }

    private fun ensureNotificationChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Game downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progress and completion for authorized GameBox downloads"
            }
        )
    }

    private fun notificationId(gameId: String): Int =
        NOTIFICATION_ID_BASE + (gameId.hashCode() and 0x0FFFFFFF)

    private fun failure(reason: String): Result = Result.failure(workDataOf(KEY_ERROR to reason))

    companion object {
        const val TAG = "gamebox-remote-download"
        const val KEY_GAME_ID = "game_id"
        const val KEY_SOURCE_URL = "source_url"
        const val KEY_RELATIVE_PATH = "relative_path"
        const val KEY_SHA256 = "sha256"
        const val KEY_MAX_BYTES = "max_bytes"
        const val KEY_BYTES_TRANSFERRED = "bytes_transferred"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ERROR = "error"
        const val STORAGE_RESERVE_BYTES = 128L * 1024L * 1024L
        private const val MAX_RETRIES = 3
        private const val CHANNEL_ID = "gamebox_downloads"
        private const val NOTIFICATION_ID_BASE = 10_000
    }
}

class RemoteDownloadScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun enqueue(game: Game, replace: Boolean = false) {
        val source = requireNotNull(game.sourceUrl) { "Game has no authorized source" }
        val checksum = requireNotNull(game.expectedSha256) { "Game has no checksum" }
        require(game.id.value.matches(Regex("^[A-Za-z0-9._-]+$"))) { "Game ID is unsafe for storage" }
        val expectedBytes = game.sizeMb.toLong().coerceAtLeast(1L) * 1024L * 1024L
        val maxBytes = expectedBytes + 16L * 1024L * 1024L
        val relativePath = "remote/" + game.id.value + "/content.bin"
        val request = OneTimeWorkRequestBuilder<RemoteDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    RemoteDownloadWorker.KEY_GAME_ID to game.id.value,
                    RemoteDownloadWorker.KEY_SOURCE_URL to source,
                    RemoteDownloadWorker.KEY_RELATIVE_PATH to relativePath,
                    RemoteDownloadWorker.KEY_SHA256 to checksum,
                    RemoteDownloadWorker.KEY_MAX_BYTES to maxBytes
                )
            )
            .addTag(RemoteDownloadWorker.TAG)
            .addTag(RemoteDownloadWorker.TAG + ":" + game.id.value)
            .build()
        workManager.enqueueUniqueWork(
            RemoteDownloadWorker.TAG + ":" + game.id.value,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(game: Game) {
        workManager.cancelUniqueWork(RemoteDownloadWorker.TAG + ":" + game.id.value)
    }
}


internal fun formatTransferProgress(transferred: Long, total: Long): String {
    fun readable(bytes: Long): String {
        val mib = bytes.coerceAtLeast(0L).toDouble() / (1024.0 * 1024.0)
        return if (mib >= 1024.0) {
            String.format(java.util.Locale.US, "%.1f GB", mib / 1024.0)
        } else {
            String.format(java.util.Locale.US, "%.1f MB", mib)
        }
    }
    return if (total > 0L) readable(transferred) + " of " + readable(total)
    else readable(transferred)
}
