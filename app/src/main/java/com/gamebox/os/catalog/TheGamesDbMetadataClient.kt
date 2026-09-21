package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.normalizeCatalogTitle
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
data class MetadataMatchCandidate(
    val externalId: String,
    val title: String,
    val platform: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val description: String? = null,
    val artworkUrl: String? = null,
) {
    init {
        require(externalId.isNotBlank() && externalId.length <= 80)
        require(title.isNotBlank() && title.length <= 240)
        require(year == null || year in 1900..2100)
        require(description == null || description.length <= 4_000)
    }
}

class TheGamesDbMetadataClient(
    private val apiKey: suspend () -> String?,
    private val maxResponseBytes: Int = 2 * 1024 * 1024,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun enrich(game: Game): Game = withContext(Dispatchers.IO) {
        val payload = fetchByName(game.title) ?: return@withContext game
        TheGamesDbMetadataParser.enrich(game, payload, json)
    }

    suspend fun findCandidates(game: Game): List<MetadataMatchCandidate> = withContext(Dispatchers.IO) {
        val payload = fetchByName(game.title) ?: return@withContext emptyList()
        TheGamesDbMetadataParser.candidates(game, payload, json)
    }

    private suspend fun fetchByName(title: String): String? {
        val key = apiKey()?.trim().orEmpty()
        if (key.isBlank()) return null
        val endpoint = "https://api.thegamesdb.net/v1/Games/ByGameName?apikey=" +
            java.net.URLEncoder.encode(key, "UTF-8") +
            "&name=" + java.net.URLEncoder.encode(title, "UTF-8") +
            "&fields=overview,boxart,players,publishers,genres"
        return runCatching { fetch(endpoint) }.getOrNull()
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
    fun candidates(
        game: Game,
        payload: String,
        json: Json = Json { ignoreUnknownKeys = true },
    ): List<MetadataMatchCandidate> = runCatching {
        val root = json.parseToJsonElement(payload).jsonObject
        val artworkBase = root["include"]?.jsonObject
            ?.get("boxart")?.jsonObject
            ?.get("base_url")?.jsonObject
            ?.get("thumb")?.jsonPrimitive?.contentOrNull
        val requestedTitle = normalizeCatalogTitle(game.title)
        root["data"]?.jsonObject?.get("games")?.jsonArray
            ?.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            ?.filter { candidate ->
                val title = candidate["game_title"]?.jsonPrimitive?.contentOrNull
                    ?: candidate["title"]?.jsonPrimitive?.contentOrNull
                normalizeCatalogTitle(title.orEmpty()) == requestedTitle
            }
            ?.take(20)
            ?.mapNotNull { candidate ->
                val externalId = candidate["id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val title = (candidate["game_title"]?.jsonPrimitive?.contentOrNull
                    ?: candidate["title"]?.jsonPrimitive?.contentOrNull)?.trim().orEmpty()
                if (externalId.isEmpty() || title.isEmpty()) return@mapNotNull null
                val release = candidate["release_date"]?.jsonPrimitive?.contentOrNull.orEmpty()
                MetadataMatchCandidate(
                    externalId = externalId,
                    title = title.take(240),
                    platform = candidate["platform"]?.jsonPrimitive?.contentOrNull?.trim()?.take(120),
                    year = release.take(4).toIntOrNull()?.takeIf { it in 1900..2100 },
                    genre = candidate["genre"]?.jsonPrimitive?.contentOrNull?.trim()?.take(120),
                    description = candidate["overview"]?.jsonPrimitive?.contentOrNull
                        ?.trim()?.takeIf(String::isNotEmpty)?.take(4_000),
                    artworkUrl = resolveHttpsArtwork(
                        candidate["boxart"]?.jsonObject?.get("thumb")?.jsonPrimitive?.contentOrNull,
                        artworkBase,
                    ),
                )
            }
            .orEmpty()
    }.getOrDefault(emptyList())

    fun enrich(game: Game, payload: String, json: Json = Json { ignoreUnknownKeys = true }): Game =
        runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            val candidates = root["data"]?.jsonObject
                ?.get("games")?.jsonArray
                ?.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
                .orEmpty()
            val requestedTitle = normalizeCatalogTitle(game.title)
            if (requestedTitle.isEmpty()) return@runCatching game
            val exactMatches = candidates.filter { candidate ->
                val providerTitle = candidate["game_title"]?.jsonPrimitive?.contentOrNull
                    ?: candidate["title"]?.jsonPrimitive?.contentOrNull
                normalizeCatalogTitle(providerTitle.orEmpty()) == requestedTitle
            }
            // Name lookup can return similarly named games and the same title on several
            // platforms. Never attach metadata automatically unless the title identifies
            // exactly one candidate; ambiguous results require an explicit user choice.
            val selected = exactMatches.singleOrNull() ?: return@runCatching game
            val overview = selected["overview"]?.jsonPrimitive?.contentOrNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.take(4_000)
            val artworkPath = selected["boxart"]?.jsonObject
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

    internal fun resolveHttpsArtwork(path: String?, base: String?): String? {
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
