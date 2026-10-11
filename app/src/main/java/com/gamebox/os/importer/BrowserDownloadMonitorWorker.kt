package com.gamebox.os.importer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gamebox.os.GameBoxApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Keeps tracking and importing independent of the browser screen and app process lifetime.
 * Android's DownloadManager owns the transfer; this durable worker reconnects to it after
 * process recreation and completes GameBox's verification/import step.
 */
class BrowserDownloadMonitorWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        if (downloadId <= 0L) return Result.failure()
        val app = applicationContext.applicationContext as? GameBoxApplication ?: return Result.failure()
        return try {
            // WorkManager's regular execution window is shorter than a large game transfer.
            // Periodically yield and retry so the durable work chain can resume monitoring.
            withTimeout(MONITOR_WINDOW_MILLIS) {
                app.browserDownloadCoordinator.monitor(downloadId)
            }
            Result.success()
        } catch (_: TimeoutCancellationException) {
            Result.retry()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val TAG = "gamebox-browser-download-monitor"
        const val KEY_DOWNLOAD_ID = "browser_download_id"
        internal const val MONITOR_WINDOW_MILLIS = 8L * 60L * 1_000L
    }
}
