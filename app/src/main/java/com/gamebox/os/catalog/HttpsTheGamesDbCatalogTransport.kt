package com.gamebox.os.catalog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.gamebox.os.provider.ProviderFailureKind
import com.gamebox.os.provider.ProviderRecoveryPolicy
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class TheGamesDbTransportException(
    val kind: ProviderFailureKind,
    val httpStatus: Int?,
    val retryAfterMillis: Long?,
    message: String,
    cause: Throwable? = null,
) : java.io.IOException(message, cause)

class HttpsTheGamesDbCatalogTransport(
    private val maxResponseBytes: Int = 4 * 1024 * 1024,
) : TheGamesDbCatalogTransport {
    init {
        require(maxResponseBytes in 1..16 * 1024 * 1024)
    }

    override suspend fun get(uri: URI): String = withContext(Dispatchers.IO) {
        require(uri.scheme.equals("https", true))
        require(uri.host == "api.thegamesdb.net")
        require(uri.userInfo == null && uri.fragment == null)
        val connection = URL(uri.toASCIIString()).openConnection() as HttpsURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "GameBoxOS/0.1")
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val decision = ProviderRecoveryPolicy.classify(responseCode)
                val retryAfterMillis = connection.getHeaderField("Retry-After")
                    ?.trim()
                    ?.toLongOrNull()
                    ?.coerceIn(1L, 86_400L)
                    ?.let { System.currentTimeMillis() + it * 1_000L }
                throw TheGamesDbTransportException(
                    kind = decision.kind,
                    httpStatus = responseCode,
                    retryAfterMillis = retryAfterMillis,
                    message = decision.userMessage,
                )
            }
            require(connection.contentLengthLong < 0 || connection.contentLengthLong <= maxResponseBytes) {
                "TheGamesDB response is too large"
            }
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= maxResponseBytes) { "TheGamesDB response is too large" }
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }
        } finally {
            connection.disconnect()
        }
    }
}
