package com.gamebox.os.importer

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException

data class BrowserDownloadCandidate(val uri: Uri, val displayName: String)
data class BrowserDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val percent: Int?,
    val paused: Boolean = false,
)

internal fun hasRecoverableStagedDownload(stagedFile: File): Boolean =
    stagedFile.isFile && stagedFile.length() > 0L

/** Watches only the DownloadManager request that GameBox itself queued. */
class BrowserDownloadTracker(context: Context) {
    private val applicationContext = context.applicationContext
    private val downloadManager = applicationContext.getSystemService(DownloadManager::class.java)

    suspend fun awaitCompletedDownload(
        id: Long,
        stagedName: String,
        displayName: String,
        onProgress: (BrowserDownloadProgress) -> Unit,
        timeoutMillis: Long = 60 * 60_000L,
    ): BrowserDownloadCandidate = withTimeout(timeoutMillis) {
        require(stagedName.matches(Regex("[0-9a-f-]{36}\\.[a-z0-9]+"))) {
            "Invalid download staging name"
        }
        val directory = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IOException("App download storage is unavailable")
        val stagedFile = File(directory, stagedName)
        while (true) {
            val state = withContext(Dispatchers.IO) { query(id) }
            if (state == null) {
                // Android can prune a completed DownloadManager row while the
                // app-owned staged file is still present. Recover that completion
                // instead of marking it failed and making the user start over.
                if (hasRecoverableStagedDownload(stagedFile)) {
                    return@withTimeout BrowserDownloadCandidate(Uri.fromFile(stagedFile), displayName)
                }
                throw IOException("Download disappeared from Android's queue before a staged file was available")
            }
            when (state.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    if (!stagedFile.isFile) throw IOException("Completed download file is missing")
                    return@withTimeout BrowserDownloadCandidate(Uri.fromFile(stagedFile), displayName)
                }
                DownloadManager.STATUS_FAILED -> throw IOException("Android download failed (code ${state.reason})")
            }
            onProgress(
                BrowserDownloadProgress(
                    downloadedBytes = state.downloadedBytes,
                    totalBytes = state.totalBytes,
                    percent = if (state.totalBytes > 0) {
                        (state.downloadedBytes * 100 / state.totalBytes).coerceIn(0, 99).toInt()
                    } else null,
                    paused = state.status == DownloadManager.STATUS_PAUSED,
                ),
            )
            delay(1_000L)
        }
        @Suppress("UNREACHABLE_CODE")
        throw IllegalStateException("Download polling stopped")
    }

    private fun query(id: Long): State? = downloadManager.query(
        DownloadManager.Query().setFilterById(id),
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        State(
            status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
            reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
            totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
            downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
        )
    }

    private data class State(
        val status: Int,
        val reason: Int,
        val totalBytes: Long,
        val downloadedBytes: Long,
    )
}
