package com.gamebox.os.importer

/** Coalesces byte callbacks while always allowing callers to publish final progress. */
internal class ByteProgressThrottle(
    private val onProgress: (Long) -> Unit,
    private val intervalBytes: Long = DEFAULT_INTERVAL_BYTES,
) {
    private var lastReportedBytes = 0L
    private var lastObservedBytes = 0L

    init {
        require(intervalBytes > 0L)
    }

    fun onBytesProcessed(bytesProcessed: Long) {
        observe(bytesProcessed)
        if (bytesProcessed - lastReportedBytes >= intervalBytes) {
            onProgress(bytesProcessed)
            lastReportedBytes = bytesProcessed
        }
    }

    fun finish(bytesProcessed: Long) {
        observe(bytesProcessed)
        if (bytesProcessed != lastReportedBytes) {
            onProgress(bytesProcessed)
            lastReportedBytes = bytesProcessed
        }
    }

    private fun observe(bytesProcessed: Long) {
        require(bytesProcessed >= lastObservedBytes) { "Progress bytes cannot move backwards" }
        lastObservedBytes = bytesProcessed
    }

    private companion object {
        const val DEFAULT_INTERVAL_BYTES = 1024L * 1024L
    }
}
