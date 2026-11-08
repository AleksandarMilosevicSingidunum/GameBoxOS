package com.gamebox.os.importer

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.HttpURLConnection
import java.util.UUID

internal fun newBrowserTransferJobId(): Int = (UUID.randomUUID().hashCode() and 0x3fffffff).coerceAtLeast(1)

internal fun resolveSecureBrowserDownloadRedirect(current: URI, location: String): URI =
    current.resolve(location).also {
        require(it.scheme == "https" && !it.host.isNullOrBlank()) { "Download redirected to an unsafe URL" }
    }

internal fun browserDownloadMayForwardCredentials(source: URI, destination: URI): Boolean =
    source.host.equals(destination.host, ignoreCase = true)

internal fun browserDownloadStatusAfterTransferStop(status: BrowserDownloadStatus): BrowserDownloadStatus =
    if (status == BrowserDownloadStatus.QUEUED || status == BrowserDownloadStatus.DOWNLOADING ||
        status == BrowserDownloadStatus.PAUSED
    ) BrowserDownloadStatus.QUEUED else status

internal fun validateBrowserDownloadLength(receivedBytes: Long, expectedBytes: Long) {
    if (expectedBytes >= 0L && receivedBytes != expectedBytes) {
        throw IllegalStateException("Download size mismatch: received $receivedBytes of $expectedBytes bytes")
    }
}

internal data class BrowserDownloadRange(val start: Long, val end: Long, val total: Long)

internal fun parseBrowserDownloadRange(contentRange: String?, requestedOffset: Long): BrowserDownloadRange {
    val match = Regex("^bytes (\\d+)-(\\d+)/(\\d+)$").matchEntire(contentRange.orEmpty())
        ?: throw IllegalStateException("Download server returned an invalid resume range")
    val range = BrowserDownloadRange(
        start = match.groupValues[1].toLong(),
        end = match.groupValues[2].toLong(),
        total = match.groupValues[3].toLong(),
    )
    require(range.start == requestedOffset) { "Download resume range did not match" }
    require(range.end >= range.start && range.total > range.end) { "Download server returned an invalid resume range" }
    return range
}

internal fun validateBrowserDownloadRangeBody(range: BrowserDownloadRange, receivedBytes: Long, partial: File) {
    if (receivedBytes != range.end - range.start + 1L) {
        partial.delete()
        throw IllegalStateException("Download response did not match the requested byte range")
    }
}

internal fun finalizeBrowserDownload(partial: File, destination: File, totalBytes: Long): File {
    if (!partial.isFile || partial.length() == 0L) throw IllegalStateException("Download returned an empty file")
    validateBrowserDownloadLength(partial.length(), totalBytes)
    if (destination.exists() && !destination.delete()) throw IllegalStateException("Could not replace staged download")
    if (!partial.renameTo(destination)) throw IllegalStateException("Could not finalize staged download")
    return destination
}

internal fun receiveBrowserDownload(
    connection: HttpURLConnection,
    partial: File,
    append: Boolean,
    offset: Long,
    totalBytes: Long,
    maxBytes: Long,
    shouldStop: () -> Boolean,
    onProgress: (Long, Long) -> Unit,
): Long {
    var receivedBytes = 0L
    try {
        connection.inputStream.use { input ->
            FileOutputStream(partial, append).use { output ->
                var transferred = offset
                var lastReported = 0L
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    if (shouldStop()) throw InterruptedException("Download stopped")
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    transferred += count
                    receivedBytes += count
                    require(transferred <= maxBytes) { "Download exceeds GameBox’s 64 GiB import limit" }
                    output.write(buffer, 0, count)
                    if (transferred - lastReported >= 256L * 1024L) {
                        onProgress(transferred, totalBytes)
                        lastReported = transferred
                    }
                }
                output.flush()
                onProgress(transferred, totalBytes)
            }
        }
    } finally {
        connection.disconnect()
    }
    return receivedBytes
}

internal fun scheduleBrowserTransfer(context: Context, record: BrowserDownloadRecord): Boolean {
    val jobId = record.transferJobId ?: return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
    val job = JobInfo.Builder(
        jobId,
        ComponentName(context, BrowserDownloadTransferJobService::class.java),
    )
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setUserInitiated(true)
        .setPersisted(true)
        .setExtras(PersistableBundle().apply {
            putLong(BrowserDownloadTransferJobService.EXTRA_DOWNLOAD_ID, record.downloadId)
        })
        .build()
    return context.getSystemService(JobScheduler::class.java).schedule(job) == JobScheduler.RESULT_SUCCESS
}
