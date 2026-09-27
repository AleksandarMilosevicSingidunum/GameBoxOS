package com.gamebox.os.catalog

import com.gamebox.os.provider.ProviderFailureKind
import com.gamebox.os.provider.ProviderRecoveryPolicy
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import com.gamebox.os.source.fetchCancellableHttpsText

class TheGamesDbTransportException(
    val kind: ProviderFailureKind,
    val httpStatus: Int?,
    val retryAfterMillis: Long?,
    message: String,
    cause: Throwable? = null,
) : java.io.IOException(message, cause)

class HttpsTheGamesDbCatalogTransport(
    private val maxResponseBytes: Int = 4 * 1024 * 1024,
    private val connectionFactory: (URL) -> HttpsURLConnection = {
        it.openConnection() as HttpsURLConnection
    },
) : TheGamesDbCatalogTransport {
    init {
        require(maxResponseBytes in 1..16 * 1024 * 1024)
    }

    override suspend fun get(uri: URI): String {
        require(uri.scheme.equals("https", true))
        require(uri.host == "api.thegamesdb.net")
        require(uri.userInfo == null && uri.fragment == null)
        return fetchCancellableHttpsText(
            URL(uri.toASCIIString()),
            maxResponseBytes,
            "TheGamesDB request",
            configure = {
                it.connectTimeout = 8_000
                it.readTimeout = 15_000
                it.instanceFollowRedirects = false
                it.setRequestProperty("Accept", "application/json")
                it.setRequestProperty("User-Agent", "GameBoxOS/0.1")
            },
            connectionFactory = connectionFactory,
            validateResponse = { connection ->
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
            },
        )
    }
}
