package com.gamebox.os.catalog

import android.content.Context
import com.gamebox.os.domain.Game
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CatalogProvider {
    suspend fun load(): CatalogSnapshot
}

enum class CatalogFallbackReason {
    NONE, OFFLINE, REMOTE_FAILURE
}

/**
 * Optional status contract for provider decorators. The reason is consumed after each load
 * so a previous degraded refresh cannot leak into a later successful one.
 */
interface CatalogFallbackStatus {
    fun consumeFallbackReason(): CatalogFallbackReason
}

class AssetCatalogProvider(
    private val context: Context,
    private val parser: CatalogParser = CatalogParser(),
    private val assetPath: String = "catalog/authorized-fixture.json"
) : CatalogProvider {
    override suspend fun load(): CatalogSnapshot = withContext(Dispatchers.IO) {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        parser.parse(text)
    }
}

internal fun validateAuthorizedCatalogUrl(value: String): String {
    val uri = try { URI(value.trim()) } catch (error: Exception) {
        throw IllegalArgumentException("Catalog URL is invalid", error)
    }
    require(uri.scheme.equals("https", ignoreCase = true)) { "Catalog URL must use HTTPS" }
    require(!uri.host.isNullOrBlank()) { "Catalog URL must include a host" }
    require(uri.userInfo == null) { "Credentials must not be embedded in the catalog URL" }
    require(uri.fragment == null) { "Catalog URL must not include a fragment" }
    return uri.toASCIIString()
}

class ConfiguredCatalogProvider(
    private val fallback: CatalogProvider,
    private val remote: CatalogProvider,
    private val configuredUrl: suspend () -> String,
    private val networkAvailable: suspend () -> Boolean = { true }
) : CatalogProvider, CatalogFallbackStatus {
    private val fallbackReason = java.util.concurrent.atomic.AtomicReference(CatalogFallbackReason.NONE)

    override suspend fun load(): CatalogSnapshot {
        val url = configuredUrl()
        if (url.isBlank()) {
            fallbackReason.set(CatalogFallbackReason.NONE)
            return fallback.load()
        }
        if (!networkAvailable()) {
            fallbackReason.set(CatalogFallbackReason.OFFLINE)
            return fallback.load()
        }
        return try {
            val snapshot = remote.load()
            fallbackReason.set(CatalogFallbackReason.NONE)
            snapshot
        } catch (_: Exception) {
            fallbackReason.set(CatalogFallbackReason.REMOTE_FAILURE)
            fallback.load()
        }
    }

    override fun consumeFallbackReason(): CatalogFallbackReason =
        fallbackReason.getAndSet(CatalogFallbackReason.NONE)
}

class HttpsCatalogProvider(
    context: Context,
    private val configuredUrl: suspend () -> String,
    private val credentialStore: CatalogCredentialStore? = null,
    private val credentialKey: suspend () -> String? = { null },
    private val parser: CatalogParser = CatalogParser(),
    private val maxResponseBytes: Int = 1_048_576
) : CatalogProvider {
    private val cacheFile = context.filesDir.resolve("catalog/remote-catalog.json")
    private val cacheUrlFile = context.filesDir.resolve("catalog/remote-catalog.url")

    override suspend fun load(): CatalogSnapshot = withContext(Dispatchers.IO) {
        val url = validateAuthorizedCatalogUrl(configuredUrl())
        val credentials = credentialStore?.let { store -> credentialKey()?.let(store::credentials) }
        try {
            val text = fetch(url, credentials)
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
            if (cacheFile.isFile && cacheUrlFile.readText().trim() == url) parser.parse(cacheFile.readText())
            else throw error
        }
    }

    private fun fetch(url: String, credentials: CatalogCredentials?): String {
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "GameBoxOS/0.1")
        if (credentials?.username != null && credentials.password != null) {
            val token = java.util.Base64.getEncoder().encodeToString(
                (credentials.username + ":" + credentials.password).toByteArray()
            )
            connection.setRequestProperty("Authorization", "Basic " + token)
        }
        try {
            val status = connection.responseCode
            require(status in 200..299) {
                if (status in 300..399) "Catalog redirects are not accepted"
                else "Catalog request failed with HTTP " + status
            }
            val declaredLength = connection.contentLengthLong
            require(declaredLength < 0 || declaredLength <= maxResponseBytes) { "Catalog response is too large" }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= maxResponseBytes) { "Catalog response is too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return bytes.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }
}


/** Enriches an authorized catalog with metadata from an external provider. */
class MetadataEnrichingCatalogProvider(
    private val base: CatalogProvider,
    private val enrich: suspend (Game) -> Game
) : CatalogProvider, CatalogFallbackStatus {
    override suspend fun load(): CatalogSnapshot {
        val snapshot = base.load()
        val enriched = snapshot.games.map { game ->
            runCatching { enrich(game) }.getOrDefault(game)
        }
        return snapshot.copy(games = enriched)
    }

    override fun consumeFallbackReason(): CatalogFallbackReason =
        (base as? CatalogFallbackStatus)?.consumeFallbackReason()
            ?: CatalogFallbackReason.NONE
}
