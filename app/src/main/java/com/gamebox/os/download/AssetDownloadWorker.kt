package com.gamebox.os.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetDownloadWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val assetPath = inputData.getString(KEY_ASSET_PATH) ?: return@withContext failure("missing asset")
        val relativePath = inputData.getString(KEY_RELATIVE_PATH) ?: return@withContext failure("missing path")
        val checksum = inputData.getString(KEY_SHA256) ?: return@withContext failure("missing checksum")
        val totalBytes = inputData.getLong(KEY_TOTAL_BYTES, -1L).takeIf { it >= 0L }
        val maxBytes = inputData.getLong(KEY_MAX_BYTES, -1L)
        if (maxBytes <= 0L) return@withContext failure("invalid size limit")

        val result = runCatching {
            TransferEngine().transfer(
                source = AssetTransferSource(
                    applicationContext.assets,
                    assetPath,
                    totalBytes,
                    checksum
                ),
                staging = FileStagingTarget(
                    applicationContext.filesDir.resolve(INSTALL_ROOT),
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
        }.getOrElse { return@withContext failure(it::class.simpleName ?: "configuration error") }

        when (result) {
            is TransferResult.Success -> Result.success(
                workDataOf(KEY_BYTES_TRANSFERRED to result.bytesTransferred)
            )
            is TransferResult.Cancelled -> Result.failure(
                workDataOf(KEY_ERROR to "cancelled")
            )
            is TransferResult.ChecksumMismatch -> failure("checksum mismatch")
            is TransferResult.SizeLimitExceeded -> failure("size limit exceeded")
            is TransferResult.Failed -> failure(result.reason)
        }
    }

    private fun failure(reason: String): Result = Result.failure(workDataOf(KEY_ERROR to reason))

    companion object {
        const val INSTALL_ROOT = "installed"
        const val KEY_ASSET_PATH = "asset_path"
        const val KEY_RELATIVE_PATH = "relative_path"
        const val KEY_SHA256 = "sha256"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_MAX_BYTES = "max_bytes"
        const val KEY_BYTES_TRANSFERRED = "bytes_transferred"
        const val KEY_ERROR = "error"
    }
}

object AuthorizedTestDownload {
    const val UNIQUE_WORK_NAME = "authorized-test-download"
    const val ASSET_PATH = "downloads/test.txt"
    const val RELATIVE_PATH = "retro/retro-test/content/test.txt"
    const val SHA256 = "94ee059335e587e501cc4bf90613e0814f00a7b08bc7c648fd865a2af6a22cc2"
    const val SIZE_BYTES = 4L
    const val MAX_BYTES = 1024L
}

class DownloadWorkScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun enqueueAuthorizedTest() {
        val request = OneTimeWorkRequestBuilder<AssetDownloadWorker>()
            .setInputData(
                workDataOf(
                    AssetDownloadWorker.KEY_ASSET_PATH to AuthorizedTestDownload.ASSET_PATH,
                    AssetDownloadWorker.KEY_RELATIVE_PATH to AuthorizedTestDownload.RELATIVE_PATH,
                    AssetDownloadWorker.KEY_SHA256 to AuthorizedTestDownload.SHA256,
                    AssetDownloadWorker.KEY_TOTAL_BYTES to AuthorizedTestDownload.SIZE_BYTES,
                    AssetDownloadWorker.KEY_MAX_BYTES to AuthorizedTestDownload.MAX_BYTES
                )
            )
            .build()
        workManager.enqueueUniqueWork(
            AuthorizedTestDownload.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
