package com.gamebox.os.importer

import java.io.File
import java.io.InputStream

/** Copies an archive into private staging without allowing oversized or partial sources to persist. */
internal fun copyBoundedArchiveSource(
    input: InputStream,
    destination: File,
    maxBytes: Long,
    onProgress: (copiedBytes: Long) -> Unit,
) {
    require(maxBytes > 0L) { "Archive copy limit must be positive" }
    var copiedBytes = 0L
    var complete = false
    val progressThrottle = ByteProgressThrottle(onProgress)
    try {
        destination.outputStream().buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                require(copiedBytes <= maxBytes - count) {
                    "Archive source exceeds the configured import limit"
                }
                output.write(buffer, 0, count)
                copiedBytes += count
                progressThrottle.onBytesProcessed(copiedBytes)
            }
        }
        progressThrottle.finish(copiedBytes)
        complete = true
    } finally {
        if (!complete) destination.delete()
    }
}
