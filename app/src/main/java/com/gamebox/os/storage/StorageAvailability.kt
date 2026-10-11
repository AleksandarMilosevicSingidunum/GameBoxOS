package com.gamebox.os.storage

import android.content.Context
import android.os.storage.StorageManager
import java.io.File

/** Reports space Android can make available to this app, including reclaimable cached data. */
internal fun availableStorageBytes(
    context: Context,
    directory: File = context.filesDir,
): Long {
    val fallback = { directory.usableSpace.coerceAtLeast(0L) }
    val storageManager = context.getSystemService(StorageManager::class.java) ?: return fallback()
    return resolveAvailableStorageBytes(
        allocatable = { storageManager.getAllocatableBytes(storageManager.getUuidForPath(directory)) },
        fallback = fallback,
    )
}

internal fun resolveAvailableStorageBytes(
    allocatable: () -> Long,
    fallback: () -> Long,
): Long = try {
    allocatable().coerceAtLeast(0L)
} catch (_: Exception) {
    fallback().coerceAtLeast(0L)
}
