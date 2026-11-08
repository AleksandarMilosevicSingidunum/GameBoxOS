package com.gamebox.os.importer

import android.app.job.JobParameters
import android.app.job.JobService
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.annotation.RequiresApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import com.gamebox.os.GameBoxApplication
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

class BrowserDownloadTransferJobService : JobService() {
    private data class RunningTransfer(
        @Volatile var thread: Thread? = null,
        @Volatile var connection: HttpURLConnection? = null,
        @Volatile var stopped: Boolean = false,
    )

    private val executor = Executors.newCachedThreadPool()
    private val running = ConcurrentHashMap<Int, RunningTransfer>()

    override fun onStartJob(params: JobParameters): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        val app = application as? GameBoxApplication ?: return false
        val downloadId = params.extras.getLong(EXTRA_DOWNLOAD_ID, -1L)
        val record = app.browserDownloadStore.get(downloadId)?.takeIf {
            it.transferJobId == params.jobId && it.status in ACTIVE_BROWSER_DOWNLOAD_STATUSES
        }
            ?: return false
        ensureNotificationChannel()
        updateNotification(params, record, 0L, -1L)
        val task = RunningTransfer()
        running[params.jobId] = task
        executor.execute {
            task.thread = Thread.currentThread()
            try {
                val staged = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                    File(it, record.stagedName)
                }?.takeIf(::hasRecoverableStagedDownload)
                val file = if (staged != null) staged
                else download(app, record, task) { bytes, total ->
                    app.browserDownloadStore.update(downloadId) {
                        it.copy(
                            status = BrowserDownloadStatus.DOWNLOADING,
                            downloadedBytes = bytes,
                            totalBytes = total,
                        )
                    }
                    updateNotification(params, record, bytes, total)
                }
                if (task.stopped) return@execute
                app.browserDownloadStore.update(downloadId) {
                    it.copy(status = BrowserDownloadStatus.IMPORTING, bytesPerSecond = 0L)
                }
                runBlocking {
                    app.browserDownloadCoordinator.importStagedBrowserDownload(
                        downloadId,
                        BrowserDownloadCandidate(Uri.fromFile(file), record.displayName),
                    )
                }
                if (!task.stopped) jobFinished(params, false)
            } catch (error: Exception) {
                if (!task.stopped) {
                    app.browserDownloadStore.update(downloadId) {
                        it.copy(
                            status = BrowserDownloadStatus.FAILED,
                            bytesPerSecond = 0L,
                            error = error.message?.take(180) ?: "Download failed",
                        )
                    }
                    jobFinished(params, false)
                }
            } finally {
                running.remove(params.jobId, task)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val task = running.remove(params.jobId) ?: return true
        task.stopped = true
        task.connection?.disconnect()
        task.thread?.interrupt()
        (application as? GameBoxApplication)?.browserDownloadStore?.update(
            params.extras.getLong(EXTRA_DOWNLOAD_ID, -1L),
        ) { it.copy(status = browserDownloadStatusAfterTransferStop(it.status), bytesPerSecond = 0L) }
        return true
    }

    private fun download(
        app: GameBoxApplication,
        record: BrowserDownloadRecord,
        task: RunningTransfer,
        onProgress: (Long, Long) -> Unit,
    ): File {
        require(record.stagedName.matches(Regex("[0-9a-f-]{36}\\.[a-z0-9]+"))) { "Invalid staging name" }
        val directory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IllegalStateException("GameBox download storage is unavailable")
        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not create download folder")
        val destination = File(directory, record.stagedName)
        val partial = File(directory, record.stagedName + ".part")
        val source = URI.create(requireNotNull(record.downloadUrl) { "Download URL is missing" })
        require(source.scheme == "https" && !source.host.isNullOrBlank()) { "Only HTTPS downloads are supported" }
        val cookie = app.browserDownloadStore.retryCookie(record.downloadId)
        var offset = partial.length().coerceAtMost(MAX_DOWNLOAD_BYTES)
        var current = source
        var redirects = 0
        while (true) {
            if (task.stopped || Thread.currentThread().isInterrupted) throw InterruptedException("Download stopped")
            require(current.scheme == "https" && !current.host.isNullOrBlank()) { "Download URL is unsafe" }
            val connection = (URL(current.toString()).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("Accept", "*/*")
                record.userAgent?.takeIf(String::isNotBlank)?.let { setRequestProperty("User-Agent", it) }
                if (browserDownloadMayForwardCredentials(source, current)) {
                    cookie?.takeIf(String::isNotBlank)?.let { setRequestProperty("Cookie", it) }
                    record.referer?.takeIf(String::isNotBlank)?.let { setRequestProperty("Referer", it) }
                }
                if (offset > 0L) setRequestProperty("Range", "bytes=$offset-")
            }
            task.connection = connection
            val code = connection.responseCode
            if (code in 300..399) {
                if (++redirects > MAX_REDIRECTS) throw IllegalStateException("Too many download redirects")
                val location = connection.getHeaderField("Location")
                    ?: throw IllegalStateException("Download redirect had no destination")
                current = resolveSecureBrowserDownloadRedirect(current, location)
                connection.disconnect()
                continue
            }
            if (code == 416 && offset > 0L) {
                connection.disconnect()
                partial.delete()
                offset = 0L
                current = source
                redirects = 0
                continue
            }
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                throw IllegalStateException("Download server returned HTTP $code")
            }
            if (connection.contentType.orEmpty().substringBefore(';').trim().equals("text/html", true)) {
                throw IllegalStateException("The source returned a web page instead of the game file")
            }
            if (offset > 0L && code == HttpURLConnection.HTTP_OK) offset = 0L
            val range = if (code == HttpURLConnection.HTTP_PARTIAL) {
                parseBrowserDownloadRange(connection.getHeaderField("Content-Range"), offset)
            } else null
            val total = range?.total ?: connection.contentLengthLong
            require(total <= MAX_DOWNLOAD_BYTES) { "Download exceeds GameBox’s 64 GiB import limit" }
            val receivedBytes = receiveBrowserDownload(
                connection = connection,
                partial = partial,
                append = code == HttpURLConnection.HTTP_PARTIAL && offset > 0L,
                offset = offset,
                totalBytes = total,
                maxBytes = MAX_DOWNLOAD_BYTES,
                shouldStop = { task.stopped || Thread.currentThread().isInterrupted },
                onProgress = onProgress,
            )
            range?.let { validateBrowserDownloadRangeBody(it, receivedBytes, partial) }
            return finalizeBrowserDownload(partial, destination, total)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun updateNotification(params: JobParameters, record: BrowserDownloadRecord, bytes: Long, total: Long) {
        val determinate = total > 0L
        val percent = if (determinate) (bytes * 100L / total).toInt().coerceIn(0, 99) else 0
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading ${record.title}")
            .setContentText(if (bytes > 0L) "${bytes / (1024 * 1024)} MB downloaded" else "Connecting to source…")
            .setProgress(100, percent, !determinate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        setNotification(params, params.jobId, notification, JOB_END_NOTIFICATION_POLICY_REMOVE)
    }

    private fun ensureNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "GameBox downloads", NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        const val EXTRA_DOWNLOAD_ID = "gamebox_browser_download_id"
        private const val CHANNEL_ID = "gamebox_user_downloads"
        private const val MAX_REDIRECTS = 8
        private const val MAX_DOWNLOAD_BYTES = 64L * 1024L * 1024L * 1024L
    }
}
