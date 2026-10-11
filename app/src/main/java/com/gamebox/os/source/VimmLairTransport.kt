package com.gamebox.os.source

import java.net.URI
import java.net.URL
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection

internal class VimmLairPageNotFoundException : java.io.IOException(
    "Vimm's Lair listing is no longer available",
)

fun interface VimmLairTransport {
    suspend fun get(uri: URI): String
}

internal class VimmLairPageCache(
    private val maxEntries: Int = 24,
    private val ttlMillis: Long = 5 * 60_000L,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private data class Entry(val body: String, val expiresAtMillis: Long)
    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)

    init {
        require(maxEntries in 1..128) { "Vimm cache size must be between 1 and 128" }
        require(ttlMillis in 1_000L..60 * 60_000L) {
            "Vimm cache TTL must be between 1 second and 1 hour"
        }
    }

    @Synchronized
    fun get(uri: URI): String? {
        val key = uri.toASCIIString()
        val entry = entries[key] ?: return null
        if (entry.expiresAtMillis <= nowMillis()) {
            entries.remove(key)
            return null
        }
        return entry.body
    }

    @Synchronized
    fun put(uri: URI, body: String) {
        val key = uri.toASCIIString()
        entries[key] = Entry(body = body, expiresAtMillis = nowMillis() + ttlMillis)
        while (entries.size > maxEntries) {
            val eldest = entries.entries.iterator()
            if (!eldest.hasNext()) break
            eldest.next()
            eldest.remove()
        }
    }
}

private val sharedVimmLairPageCache = VimmLairPageCache()

internal class CachedVimmLairTransport(
    private val delegate: VimmLairTransport,
    private val cache: VimmLairPageCache = sharedVimmLairPageCache,
) : VimmLairTransport {
    override suspend fun get(uri: URI): String {
        cache.get(uri)?.let { return it }
        return delegate.get(uri).also { cache.put(uri, it) }
    }
}

internal fun defaultVimmLairTransport(): VimmLairTransport =
    CachedVimmLairTransport(HttpsVimmLairTransport())

class HttpsVimmLairTransport(
    private val maxResponseBytes: Int = 2 * 1024 * 1024,
    private val connectionFactory: (URL) -> HttpsURLConnection = {
        it.openConnection() as HttpsURLConnection
    },
) : VimmLairTransport {
    init {
        require(maxResponseBytes in 1..8 * 1024 * 1024)
    }

    override suspend fun get(uri: URI): String {
        val validated = validateVimmLairUrl(uri.toASCIIString())
        return fetchCancellableHttpsText(
            URI(validated).toURL(),
            maxResponseBytes,
            "Vimm's Lair request",
            configure = {
                it.connectTimeout = 10_000
                it.readTimeout = 15_000
                it.instanceFollowRedirects = false
                it.setRequestProperty("Accept", "text/html,application/xhtml+xml")
                it.setRequestProperty("User-Agent", "GameBoxOS/0.1")
            },
            connectionFactory = connectionFactory,
            validateResponse = { connection ->
                val status = connection.responseCode
                when {
                    status == HttpURLConnection.HTTP_NOT_FOUND -> throw VimmLairPageNotFoundException()
                    status in 200..299 -> Unit
                    status in 300..399 -> throw IllegalArgumentException("Vimm's Lair request redirects are not accepted")
                    else -> throw IllegalArgumentException("Vimm's Lair request failed with HTTP $status")
                }
            },
        )
    }
}
