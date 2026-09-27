package com.gamebox.os.source

import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Reads a bounded HTTPS text response and disconnects the active socket on cancellation. */
internal suspend fun fetchCancellableHttpsText(
    url: URL,
    maxResponseBytes: Int,
    requestName: String,
    configure: (HttpsURLConnection) -> Unit,
    connectionFactory: (URL) -> HttpsURLConnection = {
        it.openConnection() as HttpsURLConnection
    },
): String = suspendCancellableCoroutine { continuation ->
    val connectionReference = AtomicReference<HttpsURLConnection?>(null)
    val worker = CoroutineScope(Dispatchers.IO).launch {
        try {
            val connection = connectionFactory(url)
            connectionReference.set(connection)
            if (!continuation.isActive) {
                connection.disconnect()
                return@launch
            }
            configure(connection)
            val result = try {
                val status = connection.responseCode
                require(status in 200..299) {
                    if (status in 300..399) "$requestName redirects are not accepted"
                    else "$requestName failed with HTTP $status"
                }
                require(connection.contentLengthLong < 0 || connection.contentLengthLong <= maxResponseBytes) {
                    "$requestName response is too large"
                }
                val output = java.io.ByteArrayOutputStream()
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= maxResponseBytes) { "$requestName response is too large" }
                        output.write(buffer, 0, count)
                    }
                }
                output.toString(Charsets.UTF_8.name())
            } finally {
                connection.disconnect()
            }
            if (continuation.isActive) continuation.resume(result)
        } catch (error: Throwable) {
            if (continuation.isActive) continuation.resumeWithException(error)
        } finally {
            connectionReference.getAndSet(null)?.disconnect()
        }
    }
    continuation.invokeOnCancellation {
        connectionReference.get()?.disconnect()
        worker.cancel()
    }
}
