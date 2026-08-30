package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Optional metadata enrichment from TheGamesDB. It never supplies game binaries.
 * Callers provide an API key through the credential store and merge only the returned
 * artwork/description fields into their authorized catalog.
 */
class TheGamesDbMetadataClient(
    private val apiKey: suspend () -> String?,
    private val maxResponseBytes: Int = 2 * 1024 * 1024,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun enrich(game: Game): Game = withContext(Dispatchers.IO) {
        val key = apiKey()?.trim().orEmpty()
        if (key.isBlank()) return@withContext game
        val endpoint = "https://api.thegamesdb.net/v1/Games/ByGameName?apikey=" +
            java.net.URLEncoder.encode(key, "UTF-8") +
            "&name=" + java.net.URLEncoder.encode(game.title, "UTF-8") +
            "&fields=overview,boxart"
        val payload = runCatching { fetch(endpoint) }.getOrNull()
            ?: return@withContext game
        TheGamesDbMetadataParser.enrich(game, payload, json)
    }

    private fun fetch(value: String): String {
        val uri = URI(value)
        require(uri.scheme.equals("https", true)) { "TheGamesDB endpoint must use HTTPS" }
        val connection = URL(uri.toASCIIString()).openConnection() as HttpsURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 12_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "GameBoxOS/0.1")
        try {
            require(connection.responseCode in 200..299) { "TheGamesDB request failed" }
            require(connection.contentLengthLong < 0 || connection.contentLengthLong <= maxResponseBytes)
            return connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= maxResponseBytes) { "TheGamesDB response is too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray().toString(Charsets.UTF_8)
            }
        } finally {
            connection.disconnect()
        }
    }
}


internal object TheGamesDbMetadataParser {
    fun enrich(game: Game, payload: String, json: Json = Json { ignoreUnknownKeys = true }): Game =
        runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            val first = root["data"]?.jsonObject
                ?.get("games")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?: return@runCatching game
            val overview = first["overview"]?.jsonPrimitive?.contentOrNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.take(4_000)
            val artworkPath = first["boxart"]?.jsonObject
                ?.get("thumb")?.jsonPrimitive?.contentOrNull
            val artworkBase = root["include"]?.jsonObject
                ?.get("boxart")?.jsonObject
                ?.get("base_url")?.jsonObject
                ?.get("thumb")?.jsonPrimitive?.contentOrNull
            game.copy(
                artworkUrl = resolveHttpsArtwork(artworkPath, artworkBase) ?: game.artworkUrl,
                description = overview ?: game.description,
            )
        }.getOrDefault(game)

    private fun resolveHttpsArtwork(path: String?, base: String?): String? {
        val value = path?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching {
            val resolved = if (value.startsWith("https://", ignoreCase = true)) {
                URI(value)
            } else {
                val baseUri = URI(base?.trim().orEmpty())
                require(baseUri.scheme.equals("https", ignoreCase = true))
                require(!baseUri.host.isNullOrBlank() && baseUri.userInfo == null)
                val normalizedBase = if (baseUri.path.endsWith("/")) baseUri else URI(baseUri.toASCIIString() + "/")
                normalizedBase.resolve(value.removePrefix("/"))
            }
            require(resolved.scheme.equals("https", ignoreCase = true))
            require(!resolved.host.isNullOrBlank() && resolved.userInfo == null)
            require(resolved.fragment == null)
            resolved.toASCIIString()
        }.getOrNull()
    }
}
