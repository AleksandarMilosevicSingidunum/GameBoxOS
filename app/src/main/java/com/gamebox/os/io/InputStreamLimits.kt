package com.gamebox.os.io

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Reads at most [maxBytes], allowing callers to request limit + 1 to detect overflow. */
internal fun InputStream.readAtMost(maxBytes: Int): ByteArray {
    require(maxBytes >= 0) { "Read limit must not be negative" }
    val output = ByteArrayOutputStream(minOf(maxBytes, BUFFER_SIZE))
    val buffer = ByteArray(minOf(maxBytes.coerceAtLeast(1), BUFFER_SIZE))
    var remaining = maxBytes
    while (remaining > 0) {
        val count = read(buffer, 0, minOf(buffer.size, remaining))
        when {
            count < 0 -> break
            count == 0 -> {
                val value = read()
                if (value < 0) break
                output.write(value)
                remaining--
            }
            else -> {
                output.write(buffer, 0, count)
                remaining -= count
            }
        }
    }
    return output.toByteArray()
}

private const val BUFFER_SIZE = 8 * 1024
