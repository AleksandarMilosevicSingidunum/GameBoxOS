package com.gamebox.os.ui

import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.InstallState
import java.util.Locale

internal fun formatDownloadBytes(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0L)
    val mebibytes = safeBytes.toDouble() / MEBIBYTE
    return when {
        mebibytes >= 1024.0 -> String.format(Locale.US, "%.1f GB", mebibytes / 1024.0)
        mebibytes >= 1.0 -> String.format(Locale.US, "%.1f MB", mebibytes)
        safeBytes >= 1024L -> String.format(Locale.US, "%.1f KB", safeBytes / 1024.0)
        else -> "$safeBytes B"
    }
}

internal fun formatTransferRate(bytesPerSecond: Long): String {
    val rate = bytesPerSecond.coerceAtLeast(0L)
    return when {
        rate >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB/s", rate / (1024.0 * 1024.0))
        rate >= 1024L -> String.format(Locale.US, "%.0f KB/s", rate / 1024.0)
        else -> "$rate B/s"
    }
}

internal fun formatBytes(bytes: Long): String =
    String.format(Locale.US, "%.1f GB", bytes.toDouble() / GIBIBYTE)

internal fun DownloadStatus.displayName(): String = name.lowercase(Locale.ROOT).replace('_', ' ')

internal fun InstallState.displayName(): String = name.lowercase(Locale.ROOT).replace('_', ' ')

private const val MEBIBYTE = 1024.0 * 1024.0
private const val GIBIBYTE = MEBIBYTE * 1024.0
