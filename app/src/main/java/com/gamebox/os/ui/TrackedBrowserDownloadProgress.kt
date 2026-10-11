package com.gamebox.os.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.importer.BrowserDownloadStatus

/** One consistent transfer readout for Details and Download Manager. */
@Composable
internal fun TrackedBrowserDownloadProgress(
    record: BrowserDownloadRecord,
    modifier: Modifier = Modifier,
) {
    val knownTotal = record.totalBytes > 0L
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (knownTotal) {
            LinearProgressIndicator(
                progress = {
                    (record.downloadedBytes.toDouble() / record.totalBytes).toFloat().coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        val progressLabel = if (knownTotal) {
            val percent = (record.downloadedBytes.toDouble() * 100.0 / record.totalBytes)
                .toInt().coerceIn(0, 99)
            "${formatDownloadBytes(record.downloadedBytes)} / ${formatDownloadBytes(record.totalBytes)} · $percent%"
        } else {
            "${formatDownloadBytes(record.downloadedBytes)} downloaded"
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                progressLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                if (record.status == BrowserDownloadStatus.PAUSED) "Paused"
                else formatTransferRate(record.bytesPerSecond),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

/** Durable verification/extraction progress after Android's download has completed. */
@Composable
internal fun TrackedBrowserImportProgress(
    record: BrowserDownloadRecord,
    modifier: Modifier = Modifier,
) {
    val knownTotal = record.importTotalBytes?.takeIf { it > 0L }
    val stage = record.importStage ?: "Preparing import"
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (knownTotal != null) {
            LinearProgressIndicator(
                progress = {
                    (record.importBytesProcessed.toDouble() / knownTotal).toFloat().coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        val progressLabel = if (knownTotal != null) {
            "${formatDownloadBytes(record.importBytesProcessed)} / ${formatDownloadBytes(knownTotal)}"
        } else {
            formatDownloadBytes(record.importBytesProcessed)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                buildString {
                    append(stage)
                    if (record.importFileIndex > 0 && record.importFileCount > 0) {
                        append(" · file ${record.importFileIndex} of ${record.importFileCount}")
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            if (knownTotal != null) {
                Text(
                    "${(record.importBytesProcessed.toDouble() * 100.0 / knownTotal).toInt().coerceIn(0, 100)}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        Text(progressLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
