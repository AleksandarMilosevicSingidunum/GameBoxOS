package com.gamebox.os.download

import android.content.Context
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
        val sourceUrl = inputData.getString(KEY_SOURCE_URL) ?: return@withContext failure("missing source")
        val checksum = inputData.getString(KEY_SHA256) ?: return@withContext failure("missing checksum")
        val relativePath = inputData.getString(KEY_RELATIVE_PATH) ?: return@withContext failure("missing path")
        val maxBytes = inputData.getLong(KEY_MAX_BYTES, -1L)
        if (maxBytes <= 0L) return@withContext failure("invalid size limit")

        val requiredSpace = maxBytes + STORAGE_RESERVE_BYTES
        if (applicationContext.filesDir.usableSpace < requiredSpace) {
            return@withContext failure("insufficient storage")
        }

        val result = runCatching {
            TransferEngine().transfer(
                source = HttpsTransferSource(sourceUrl, totalBytes = null, expectedSha256 = checksum),
                staging = FileStagingTarget(
                    applicationContext.filesDir.resolve(AssetDownloadWorker.INSTALL_ROOT),
                    relativePath
                ),
                maxBytes = maxBytes,
                isCancelled = { isStopped },
                onProgress = {
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
            is TransferResult.Success -> Result.success(
                workDataOf(KEY_BYTES_TRANSFERRED to result.bytesTransferred)
            )
            is TransferResult.Cancelled -> failure("cancelled")
            is TransferResult.ChecksumMismatch -> failure("checksum mismatch")
            is TransferResult.SizeLimitExceeded -> failure("size limit exceeded")
            is TransferResult.Failed ->
                if (runAttemptCount < MAX_RETRIES) Result.retry() else failure(result.reason)
        }
    }

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
    }
}

class RemoteDownloadScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun enqueue(game: Game) {
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
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(game: Game) {
        workManager.cancelUniqueWork(RemoteDownloadWorker.TAG + ":" + game.id.value)
    }
}
