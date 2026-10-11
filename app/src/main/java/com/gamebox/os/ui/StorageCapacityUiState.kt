package com.gamebox.os.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.gamebox.os.storage.availableStorageBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

internal data class StorageCapacitySnapshot(
    val availableBytes: Long,
    val totalBytes: Long,
) {
    val usedFraction: Float
        get() = if (totalBytes > 0L) {
            (1f - availableBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

/** Samples allocatable storage off the UI thread and refreshes while the owner stays visible. */
@Composable
internal fun rememberStorageCapacitySnapshot(context: Context): StorageCapacitySnapshot? {
    val snapshot = produceState<StorageCapacitySnapshot?>(initialValue = null, context) {
        suspend fun readSnapshot(): StorageCapacitySnapshot = withContext(Dispatchers.IO) {
            StorageCapacitySnapshot(
                availableBytes = availableStorageBytes(context),
                totalBytes = context.filesDir.totalSpace.coerceAtLeast(0L),
            )
        }

        value = readSnapshot()
        while (isActive) {
            delay(STORAGE_CAPACITY_REFRESH_MILLIS)
            value = readSnapshot()
        }
    }.value
    return snapshot
}

private const val STORAGE_CAPACITY_REFRESH_MILLIS = 30_000L
