package com.gamebox.os.download

import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import java.util.Locale
import kotlin.math.roundToLong

data class DownloadTelemetry(
    val bytesPerSecond: Long,
    val etaSeconds: Long?
)

data class DownloadCapacityWarning(
    val requiredBytes: Long,
    val availableBytes: Long
) {
    val shortfallBytes: Long
        get() = (requiredBytes - availableBytes).coerceAtLeast(0L)
}

class DownloadTelemetryTracker(
    private val smoothingFactor: Double = 0.35
) {
    init {
        require(smoothingFactor in 0.0..1.0) { "Smoothing factor must be between 0 and 1" }
    }

    private data class Sample(
        val bytes: Long,
        val atMillis: Long,
        val smoothedBytesPerSecond: Double? = null
    )

    private val samples = mutableMapOf<String, Sample>()

    fun sample(job: DownloadJob, nowMillis: Long): DownloadTelemetry? {
        if (job.status != DownloadStatus.DOWNLOADING) {
            samples.remove(job.id)
            return null
        }

        val currentBytes = job.downloadedBytes.coerceAtLeast(0L)
        val previous = samples[job.id]
        if (previous == null || currentBytes < previous.bytes || nowMillis <= previous.atMillis) {
            samples[job.id] = Sample(currentBytes, nowMillis)
            return null
        }
        if (currentBytes == previous.bytes) {
            return previous.smoothedBytesPerSecond?.toTelemetry(job)
        }

        val elapsedMillis = nowMillis - previous.atMillis
        val instantaneous = (currentBytes - previous.bytes).toDouble() * 1000.0 / elapsedMillis
        val smoothed = previous.smoothedBytesPerSecond?.let {
            it * (1.0 - smoothingFactor) + instantaneous * smoothingFactor
        } ?: instantaneous
        samples[job.id] = Sample(currentBytes, nowMillis, smoothed)
        return smoothed.toTelemetry(job)
    }

    private fun Double.toTelemetry(job: DownloadJob): DownloadTelemetry? {
        if (!isFinite() || this <= 0.0) return null
        val speed = roundToLong().coerceAtLeast(1L)
        val remaining = (job.totalBytes - job.downloadedBytes).coerceAtLeast(0L)
        val eta = if (job.totalBytes > 0L) {
            (remaining.toDouble() / this).roundToLong().coerceAtLeast(0L)
        } else {
            null
        }
        return DownloadTelemetry(speed, eta)
    }
}

fun assessDownloadCapacity(
    job: DownloadJob,
    availableBytes: Long,
    reserveBytes: Long = RemoteDownloadWorker.STORAGE_RESERVE_BYTES
): DownloadCapacityWarning? {
    if (job.status !in setOf(
            DownloadStatus.QUEUED,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.PAUSED
        )
    ) return null
    val remaining = (job.totalBytes - job.downloadedBytes).coerceAtLeast(0L)
    val required = remaining + reserveBytes.coerceAtLeast(0L)
    val available = availableBytes.coerceAtLeast(0L)
    return if (available < required) DownloadCapacityWarning(required, available) else null
}

internal fun formatDownloadTelemetry(telemetry: DownloadTelemetry): String {
    val speed = readableBytes(telemetry.bytesPerSecond) + "/s"
    val eta = telemetry.etaSeconds?.let { " - " + formatEta(it) } ?: ""
    return speed + eta
}

internal fun formatCapacityWarning(warning: DownloadCapacityWarning): String =
    "Low storage: " + readableBytes(warning.shortfallBytes) + " more required"

private fun formatEta(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    return when {
        safe < 60L -> safe.toString() + " sec remaining"
        safe < 3600L -> (safe / 60L).toString() + " min remaining"
        else -> String.format(Locale.US, "%.1f hr remaining", safe / 3600.0)
    }
}

private fun readableBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L).toDouble()
    val mib = safe / (1024.0 * 1024.0)
    return when {
        mib >= 1024.0 -> String.format(Locale.US, "%.1f GB", mib / 1024.0)
        mib >= 1.0 -> String.format(Locale.US, "%.1f MB", mib)
        else -> String.format(Locale.US, "%.1f KB", safe / 1024.0)
    }
}
