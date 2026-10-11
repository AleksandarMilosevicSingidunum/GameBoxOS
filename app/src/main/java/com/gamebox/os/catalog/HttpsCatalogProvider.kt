package com.gamebox.os.catalog

import android.content.Context
import com.gamebox.os.net.basicAuthorizationHeader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Remote HTTPS catalog with bounded responses and a URL-scoped last-known-good cache. */
class HttpsCatalogProvider(
    context: Context,
    private val configuredUrl: suspend () -> String,
    private val credentialStore: CatalogCredentialStore? = null,
    private val credentialKey: suspend () -> String? = { null },
    private val parser: CatalogParser = CatalogParser(),
    private val maxResponseBytes: Int = 1_048_576,
    private val fetchOverride: ((String, CatalogCredentials?) -> String)? = null,
    private val connectionFactory: (String) -> HttpsURLConnection = {
        URL(it).openConnection() as HttpsURLConnection
    },
) : CatalogProvider {
    private val cacheFile = context.filesDir.resolve("catalog/remote-catalog.json")
    private val cacheUrlFile = context.filesDir.resolve("catalog/remote-catalog.url")

    override suspend fun load(): CatalogSnapshot = loadCatalog(allowCachedFallback = true)

    // Refresh and connection testing must reach the remote endpoint, not a stale cache.
    override suspend fun refresh(): CatalogSnapshot = loadCatalog(allowCachedFallback = false)

    private suspend fun loadCatalog(allowCachedFallback: Boolean): CatalogSnapshot = withContext(Dispatchers.IO) {
        val url = validateAuthorizedCatalogUrl(configuredUrl())
        val credentials = credentialStore?.let { store -> credentialKey()?.let(store::credentials) }
        try {
            val text = fetchOverride?.invoke(url, credentials) ?: fetch(url, credentials)
            val snapshot = parser.parse(text)
            cacheFile.parentFile?.mkdirs()
            val temporary = cacheFile.resolveSibling(cacheFile.name + ".tmp")
            temporary.writeText(text)
            if (!temporary.renameTo(cacheFile)) {
                temporary.copyTo(cacheFile, overwrite = true)
                temporary.delete()
            }
            cacheUrlFile.writeText(url)
            snapshot
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            val matchingCache = allowCachedFallback && cacheFile.isFile &&
                runCatching { cacheUrlFile.readText().trim() == url }.getOrDefault(false)
            if (matchingCache) parser.parse(cacheFile.readText()) else throw error
        }
    }

    private suspend fun fetch(url: String, credentials: CatalogCredentials?): String =
        suspendCancellableCoroutine { continuation ->
            val connectionReference = AtomicReference<HttpsURLConnection?>(null)
            val worker = CoroutineScope(Dispatchers.IO).launch {
                try {
                    val connection = connectionFactory(url)
                    connectionReference.set(connection)
                    if (!continuation.isActive) {
                        connection.disconnect()
                        return@launch
                    }
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 15_000
                    connection.instanceFollowRedirects = false
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("User-Agent", "GameBoxOS/0.1")
                    credentials?.let(::basicAuthorizationHeader)?.let { authorization ->
                        connection.setRequestProperty("Authorization", authorization)
                    }
                    val result = try {
                        val status = connection.responseCode
                        require(status in 200..299) {
                            if (status in 300..399) "Catalog redirects are not accepted"
                            else "Catalog request failed with HTTP " + status
                        }
                        val declaredLength = connection.contentLengthLong
                        require(declaredLength < 0 || declaredLength <= maxResponseBytes) {
                            "Catalog response is too large"
                        }
                        val bytes = connection.inputStream.use { input ->
                            readBoundedCatalogResponse(input, declaredLength, maxResponseBytes)
                        }
                        bytes.toString(Charsets.UTF_8)
                    } finally {
                        connection.disconnect()
                    }
                    continuation.resume(result)
                } catch (error: Throwable) {
                    continuation.resumeWithException(error)
                } finally {
                    connectionReference.getAndSet(null)?.disconnect()
                }
            }
            continuation.invokeOnCancellation {
                connectionReference.get()?.disconnect()
                worker.cancel()
            }
        }
}

internal fun validateAuthorizedCatalogUrl(value: String): String {
    val uri = try {
        URI(value.trim())
    } catch (error: Exception) {
        throw IllegalArgumentException("Catalog URL is invalid", error)
    }
    require(uri.scheme.equals("https", ignoreCase = true)) { "Catalog URL must use HTTPS" }
    require(!uri.host.isNullOrBlank()) { "Catalog URL must include a host" }
    require(uri.userInfo == null) { "Credentials must not be embedded in the catalog URL" }
    require(uri.fragment == null) { "Catalog URL must not include a fragment" }
    return uri.toASCIIString()
}

internal fun readBoundedCatalogResponse(
    input: java.io.InputStream,
    declaredLength: Long,
    maxResponseBytes: Int,
): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8_192)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        require(total <= maxResponseBytes) { "Catalog response is too large" }
        output.write(buffer, 0, count)
    }
    if (declaredLength >= 0L && total.toLong() != declaredLength) {
        throw IOException("Catalog response ended with an unexpected length ($total/$declaredLength)")
    }
    return output.toByteArray()
}
