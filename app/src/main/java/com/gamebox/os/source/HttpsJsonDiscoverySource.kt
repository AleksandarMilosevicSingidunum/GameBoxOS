package com.gamebox.os.source

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
private data class JsonDiscoveryPayload(
    val games: List<JsonDiscoveryGame> = emptyList(),
    val nextPage: Int? = null,
)

@Serializable
private data class JsonDiscoveryGame(
    val id: String,
    val title: String,
    val platform: String,
    val region: String? = null,
    val year: Int? = null,
    val detailsUrl: String? = null,
    val coverUrl: String? = null,
)

/**
 * Bounded HTTPS discovery client for user-configured GameBox JSON catalogs.
 * It is metadata-only: no binary/download URL is accepted or exposed here.
 */
class HttpsJsonDiscoverySource(
    private val config: GameSourceConfig,
    private val maxResponseBytes: Int = 1_048_576,
    private val connectionFactory: (URI) -> HttpURLConnection = {
        URL(it.toString()).openConnection() as HttpURLConnection
    },
) : DiscoverySource {
    init {
        require(config.type == GameSourceProviderType.GAMEBOX_JSON) {
            "JSON discovery source requires GAMEBOX_JSON configuration"
        }
        require(maxResponseBytes in 1..16_777_216)
    }

    override val id: String = config.id
    override val displayName: String = config.name

    override suspend fun search(
        platform: String?,
        query: String,
        page: Int,
    ): DiscoverySourcePage = withContext(Dispatchers.IO) {
        require(page >= 1) { "Page must be positive" }
        val url = if (config.searchUrlTemplate.isNullOrBlank()) {
            config.baseUrl
        } else {
            config.resolveBrowseUrl(query, platform.orEmpty())
        }
        parse(fetch(url)).let { parsed ->
            val filtered = parsed.games.filter { game ->
                (platform.isNullOrBlank() ||
                    normalizeSourcePlatform(game.platform) == normalizeSourcePlatform(platform)) &&
                    (query.isBlank() || game.title.contains(query, ignoreCase = true))
            }
            DiscoverySourcePage(
                games = filtered.map(::toModel),
                nextPage = parsed.nextPage,
            )
        }
    }

    override suspend fun details(externalId: String): DiscoverySourceGame =
        withContext(Dispatchers.IO) {
            require(externalId.isNotBlank()) { "External id is required" }
            val parsed = parse(fetch(config.baseUrl))
            val game = parsed.games.firstOrNull { it.id == externalId }
                ?: throw NoSuchElementException("Game was not found in configured source")
            toModel(game)
        }

    private fun fetch(value: String): String {
        val uri = validateHttps(value)
        val connection = try {
            connectionFactory(uri)
        } catch (error: IOException) {
            throw IOException("Configured source connection failed", error)
        }
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "GameBoxOS/0.1")
        return try {
            val status = connection.responseCode
            require(status in 200..299) {
                if (status in 300..399) "Configured source redirects are not accepted"
                else "Configured source request failed with HTTP $status"
            }
            val declaredLength = connection.contentLengthLong
            require(declaredLength < 0 || declaredLength <= maxResponseBytes) {
                "Configured source response is too large"
            }
            val bytes = connection.inputStream.use { it.readNBytes(maxResponseBytes + 1) }
            require(bytes.size <= maxResponseBytes) { "Configured source response is too large" }
            bytes.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(text: String): JsonDiscoveryPayload {
        val payload = try {
            JSON.decodeFromString<JsonDiscoveryPayload>(text)
        } catch (error: Exception) {
            throw IllegalArgumentException("Configured source JSON is malformed", error)
        }
        require(payload.games.size <= 500) { "Configured source returned too many games" }
        require(payload.games.map { it.id }.distinct().size == payload.games.size) {
            "Configured source game IDs must be unique"
        }
        payload.games.forEach { game ->
            require(game.id.isNotBlank() && game.title.isNotBlank() && game.platform.isNotBlank()) {
                "Configured source games require id, title, and platform"
            }
            game.detailsUrl?.let(::validateHttps)
            game.coverUrl?.let(::validateHttps)
        }
        return payload
    }

    private fun toModel(game: JsonDiscoveryGame): DiscoverySourceGame =
        DiscoverySourceGame(
            sourceId = config.id,
            externalId = game.id,
            title = game.title,
            platform = game.platform,
            region = game.region,
            year = game.year,
            detailsUrl = game.detailsUrl,
            coverUrl = game.coverUrl,
        )

    private fun validateHttps(value: String): URI {
        val uri = try { URI(value.trim()) } catch (error: Exception) {
            throw IllegalArgumentException("Configured source URL is invalid", error)
        }
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) {
            "Configured source URL must be absolute HTTPS"
        }
        require(uri.userInfo == null) { "Configured source URL must not embed credentials" }
        require(uri.fragment == null) { "Configured source URL must not include a fragment" }
        return uri
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}

private fun normalizeSourcePlatform(value: String): String =
    value.lowercase().filter(Char::isLetterOrDigit)
