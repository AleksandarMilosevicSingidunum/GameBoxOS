package com.gamebox.os.ui

import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.importer.BrowserDownloadStatus

internal data class HomeDownloadActivity(
    val title: String,
    val status: String,
    val progress: Float?,
    val count: Int,
)

internal fun homeDownloadActivity(
    jobs: List<DownloadJob>,
    records: List<BrowserDownloadRecord>,
): HomeDownloadActivity? {
    val pendingJobs = jobs.filter { it.status != DownloadStatus.COMPLETED }
    val pendingRecords = records.filter { it.status !in setOf(BrowserDownloadStatus.IMPORTED, BrowserDownloadStatus.CANCELLED) }
    val count = pendingJobs.size + pendingRecords.size
    if (count == 0) return null

    val attentionJob = pendingJobs.firstOrNull { it.status in setOf(DownloadStatus.FAILED, DownloadStatus.PAUSED, DownloadStatus.CANCELLED) }
    val attentionRecord = pendingRecords.firstOrNull { it.status in setOf(BrowserDownloadStatus.FAILED, BrowserDownloadStatus.PAUSED) }
    val job = attentionJob ?: if (attentionRecord == null) pendingJobs.firstOrNull() else null
    if (job != null) {
        val status = when (job.status) {
            DownloadStatus.QUEUED -> "Queued"
            DownloadStatus.DOWNLOADING -> "Downloading"
            DownloadStatus.PAUSED -> "Paused"
            DownloadStatus.VERIFYING -> "Checking"
            DownloadStatus.INSTALLING -> "Installing"
            DownloadStatus.FAILED -> "Needs attention"
            DownloadStatus.CANCELLED -> "Cancelled"
            DownloadStatus.COMPLETED -> "Completed"
        }
        val progress = if (job.status == DownloadStatus.DOWNLOADING && job.totalBytes > 0) job.progress else null
        return HomeDownloadActivity(job.title, status, progress, count)
    }

    val record = attentionRecord ?: pendingRecords.first()
    val status = when (record.status) {
        BrowserDownloadStatus.QUEUED -> "Queued"
        BrowserDownloadStatus.DOWNLOADING -> "Downloading"
        BrowserDownloadStatus.PAUSED -> "Paused by Android"
        BrowserDownloadStatus.IMPORTING -> "Importing"
        BrowserDownloadStatus.FAILED -> "Needs attention"
        BrowserDownloadStatus.IMPORTED -> "Added to Library"
        BrowserDownloadStatus.CANCELLED -> "Cancelled"
    }
    val progress = when {
        record.status == BrowserDownloadStatus.DOWNLOADING && record.totalBytes > 0 ->
            (record.downloadedBytes.toDouble() / record.totalBytes).toFloat().coerceIn(0f, 1f)
        record.status == BrowserDownloadStatus.IMPORTING && (record.importTotalBytes ?: 0L) > 0 ->
            (record.importBytesProcessed.toDouble() / record.importTotalBytes!!).toFloat().coerceIn(0f, 1f)
        else -> null
    }
    return HomeDownloadActivity(record.title.ifBlank { record.displayName }, status, progress, count)
}
