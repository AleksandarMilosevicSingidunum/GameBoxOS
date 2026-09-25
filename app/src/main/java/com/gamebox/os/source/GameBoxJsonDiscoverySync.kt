package com.gamebox.os.source

import com.gamebox.os.data.local.CatalogDiscoveryDao
import com.gamebox.os.data.local.CatalogExternalIdEntity
import com.gamebox.os.data.local.CatalogGameEntity
import com.gamebox.os.data.local.CatalogPlatformEntity
import com.gamebox.os.domain.normalizeCatalogTitle
import java.net.URI
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
internal data class GameBoxDiscoveryManifest(
    val schemaVersion: Int,
    val games: List<GameBoxDiscoveryItem>,
)

@Serializable
internal data class GameBoxDiscoveryItem(
    val id: String,
    val title: String,
    val platform: String,
    val year: Int? = null,
    val region: String? = null,
    val description: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val players: String? = null,
    val rating: Double? = null,
    val coverUrl: String? = null,
    val backgroundUrl: String? = null,
    val logoUrl: String? = null,
    val screenshots: List<String> = emptyList(),
    val detailsUrl: String? = null,
)

internal data class ParsedGameBoxDiscoveryItem(
    val externalId: String,
    val title: String,
    val platform: String,
    val platformId: String,
    val year: Int?,
    val region: String?,
    val description: String?,
    val developer: String?,
    val publisher: String?,
    val players: String?,
    val rating: Double?,
    val coverUrl: String?,
    val backgroundUrl: String?,
    val logoUrl: String?,
    val screenshots: List<String>,
    val detailsUrl: String?,
)

internal class GameBoxJsonDiscoveryParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parse(payload: String): List<ParsedGameBoxDiscoveryItem> {
        val manifest = try {
            json.decodeFromString<GameBoxDiscoveryManifest>(payload)
        } catch (error: Exception) {
            throw IllegalArgumentException("GameBox JSON discovery manifest is malformed", error)
        }
        require(manifest.schemaVersion == 1) {
            "Unsupported GameBox JSON discovery schema: ${manifest.schemaVersion}"
        }
        require(manifest.games.size <= 5_000) {
            "GameBox JSON discovery manifest contains too many games"
        }
        require(manifest.games.map { it.id }.distinct().size == manifest.games.size) {
            "GameBox JSON discovery game IDs must be unique"
        }

        return manifest.games.map { item ->
            val externalId = item.id.trim()
            val title = item.title.trim()
            val platform = item.platform.trim()
            require(externalId.isNotEmpty() && externalId.length <= 256) {
                "Discovery game ID must contain 1-256 characters"
            }
            require(title.isNotEmpty() && title.length <= 240) {
                "Discovery game title must contain 1-240 characters"
            }
            require(platform.isNotEmpty() && platform.length <= 80) {
                "Discovery game platform must contain 1-80 characters"
            }
            require(item.year == null || item.year in 1900..2100) {
                "Discovery game year is invalid"
            }
            require(item.rating == null || (item.rating.isFinite() && item.rating in 0.0..10.0)) {
                "Discovery game rating must be between 0 and 10"
            }
            val platformId = normalizeCatalogTitle(platform)
            require(platformId.isNotEmpty()) { "Discovery game platform is invalid" }

            ParsedGameBoxDiscoveryItem(
                externalId = externalId,
                title = title,
                platform = platform,
                platformId = platformId,
                year = item.year,
                region = item.region?.trim()?.takeIf(String::isNotEmpty),
                description = item.description?.trim()?.takeIf(String::isNotEmpty),
                developer = item.developer?.trim()?.takeIf(String::isNotEmpty),
                publisher = item.publisher?.trim()?.takeIf(String::isNotEmpty),
                players = item.players?.trim()?.takeIf(String::isNotEmpty),
                rating = item.rating,
                coverUrl = item.coverUrl.safeOptionalUrl("Cover artwork URL"),
                backgroundUrl = item.backgroundUrl.safeOptionalUrl("Background artwork URL"),
                logoUrl = item.logoUrl.safeOptionalUrl("Logo artwork URL"),
                screenshots = item.screenshots
                    .mapNotNull { it.safeOptionalUrl("Screenshot URL") }
                    .distinct()
                    .take(12),
                detailsUrl = item.detailsUrl.safeOptionalUrl("Details URL"),
            )
        }
    }

    private fun String?.safeOptionalUrl(label: String): String? =
        this?.trim()?.takeIf(String::isNotEmpty)?.let { validateGameSourceUrl(it, label) }
}

internal fun gameBoxJsonDiscoveryGameId(sourceId: String, externalId: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(externalId.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(24)
    return "src-$sourceId-$digest"
}

fun interface GameBoxJsonDiscoveryTransport {
    suspend fun get(uri: URI): String
}

class HttpsGameBoxJsonDiscoveryTransport(
    private val maxResponseBytes: Int = 2 * 1024 * 1024,
) : GameBoxJsonDiscoveryTransport {
    init {
        require(maxResponseBytes in 1..8 * 1024 * 1024)
    }

    override suspend fun get(uri: URI): String = withContext(Dispatchers.IO) {
        val validated = URI(validateGameSourceUrl(uri.toASCIIString(), "GameBox JSON source URL"))
        val connection = validated.toURL().openConnection() as HttpsURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "GameBoxOS/0.1")
        try {
            val status = connection.responseCode
            require(status in 200..299) {
                if (status in 300..399) "GameBox JSON source redirects are not accepted"
                else "GameBox JSON source failed with HTTP $status"
            }
            require(connection.contentLengthLong < 0 || connection.contentLengthLong <= maxResponseBytes) {
                "GameBox JSON source response is too large"
            }
            val output = java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= maxResponseBytes) {
                        "GameBox JSON source response is too large"
                    }
                    output.write(buffer, 0, count)
                }
            }
            output.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }
}

sealed interface ConfiguredDiscoverySyncResult {
    data class Success(
        val sourceId: String,
        val games: Int,
        val platforms: Int,
    ) : ConfiguredDiscoverySyncResult

    data class Failed(val reason: String) : ConfiguredDiscoverySyncResult
    data object Disabled : ConfiguredDiscoverySyncResult
    data object Unsupported : ConfiguredDiscoverySyncResult
}

class GameBoxJsonDiscoverySync(
    private val dao: CatalogDiscoveryDao,
    private val transport: GameBoxJsonDiscoveryTransport = HttpsGameBoxJsonDiscoveryTransport(),
    private val parser: GameBoxJsonDiscoveryParser = GameBoxJsonDiscoveryParser(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun sync(source: GameSourceConfig): ConfiguredDiscoverySyncResult {
        if (!source.enabled) return ConfiguredDiscoverySyncResult.Disabled
        if (source.type != GameSourceProviderType.GAMEBOX_JSON) {
            return ConfiguredDiscoverySyncResult.Unsupported
        }

        return try {
            val payload = transport.get(URI(source.baseUrl))
            val games = parser.parse(payload)
            val updatedAt = nowMillis()
            val gameIds = games.map { gameBoxJsonDiscoveryGameId(source.id, it.externalId) }
            val favoriteIds = if (gameIds.isEmpty()) emptySet() else
                dao.favoriteGameIds(gameIds).toSet()
            val providerId = "GAMEBOX_JSON:$source.id"

            games.groupBy { it.platformId }.forEach { (platformId, platformGames) ->
                val existingPlatform = dao.platform(platformId)
                val displayName = platformGames.first().platform
                dao.upsertPage(
                    platform = CatalogPlatformEntity(
                        id = platformId,
                        name = existingPlatform?.name?.takeIf(String::isNotBlank) ?: displayName,
                        theGamesDbId = existingPlatform?.theGamesDbId,
                        updatedAtMillis = updatedAt,
                    ),
                    games = platformGames.map { game ->
                        val gameId = gameBoxJsonDiscoveryGameId(source.id, game.externalId)
                        CatalogGameEntity(
                            id = gameId,
                            title = game.title,
                            normalizedTitle = normalizeCatalogTitle(game.title),
                            platformId = platformId,
                            region = game.region,
                            releaseDate = game.year?.toString(),
                            description = game.description,
                            developer = game.developer,
                            publisher = game.publisher,
                            players = game.players,
                            rating = game.rating,
                            coverUrl = game.coverUrl,
                            backgroundUrl = game.backgroundUrl,
                            logoUrl = game.logoUrl,
                            screenshotsJson = game.screenshots.joinToString("\n").ifBlank { null },
                            favorite = gameId in favoriteIds,
                            updatedAtMillis = updatedAt,
                        )
                    },
                    externalIds = platformGames.map { game ->
                        CatalogExternalIdEntity(
                            gameId = gameBoxJsonDiscoveryGameId(source.id, game.externalId),
                            provider = providerId,
                            externalId = game.externalId,
                        )
                    },
                )
            }

            ConfiguredDiscoverySyncResult.Success(
                sourceId = source.id,
                games = games.size,
                platforms = games.map { it.platformId }.distinct().size,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ConfiguredDiscoverySyncResult.Failed(
                error.message?.take(200) ?: "GameBox JSON source sync failed"
            )
        }
    }
}
