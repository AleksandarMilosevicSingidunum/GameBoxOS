package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        val payload = fetch(endpoint)
        val result = runCatching {
            val data = json.parseToJsonElement(payload).jsonObject["data"]?.jsonObject
            val first = data?.get("games")?.jsonArray?.firstOrNull()?.jsonObject ?: return@runCatching null
            val overview = first["overview"]?.jsonPrimitive?.contentOrNull
            val boxart = first["boxart"]?.jsonObject?.get("thumb")?.jsonPrimitive?.contentOrNull
            game.copy(
                artworkUrl = boxart?.takeIf { it.startsWith("https://") } ?: game.artworkUrl,
                description = overview ?: game.description
            )
        }.getOrNull()
        result ?: game
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
                val bytes = input.readBytes()
                require(bytes.size <= maxResponseBytes) { "TheGamesDB response is too large" }
                bytes.toString(Charsets.UTF_8)
            }
        } finally {
            connection.disconnect()
        }
    }
}
