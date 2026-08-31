package com.gamebox.os.catalog

import com.gamebox.os.domain.CatalogGame
import com.gamebox.os.domain.CatalogMedia
import com.gamebox.os.domain.CatalogPlatform
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.MetadataProviderId
import com.gamebox.os.domain.normalizeCatalogTitle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI

data class TheGamesDbCatalogPage(
    val games: List<CatalogGame>,
    val currentPage: Int,
    val nextPage: Int?,
)

internal object TheGamesDbCatalogParser {
    fun parsePlatforms(payload: String, json: Json = Json { ignoreUnknownKeys = true }): List<CatalogPlatform> =
        runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            val platformElement = root["data"]?.jsonObject?.get("platforms")
            val platformItems = when (platformElement) {
                is JsonArray -> platformElement
                // /Platforms returns an object keyed by numeric platform id, while
                // some API variants return the same records as an array.
                is JsonObject -> JsonArray(platformElement.values.toList())
                else -> JsonArray(emptyList())
            }
            platformItems.mapNotNull { element ->
                val item = element.jsonObject
                val id = item.text("id") ?: return@mapNotNull null
                val name = item.text("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                CatalogPlatform(
                    // The API names this entry "Sony Playstation 2"; keep the
                    // provider-neutral id stable so callers can request "PlayStation 2".
                    id = normalizeCatalogTitle(name).removePrefix("sony"),
                    name = name,
                    externalIds = mapOf(MetadataProviderId.THE_GAMES_DB to id),
                )
            }
        }.getOrDefault(emptyList())

    fun parsePlatformPage(
        payload: String,
        platform: CatalogPlatform,
        json: Json = Json { ignoreUnknownKeys = true },
    ): TheGamesDbCatalogPage = runCatching {
        val root = json.parseToJsonElement(payload).jsonObject
        val data = root["data"]?.jsonObject ?: JsonObject(emptyMap())
        val pages = data["pages"]?.jsonObject
        val current = pages?.text("current")?.toIntOrNull() ?: 1
        val next = pages?.text("next")?.toIntOrNull()?.takeIf { it > current }
        val boxart = root["include"]?.jsonObject?.get("boxart")?.jsonObject
        val baseUrls = boxart?.get("base_url")?.jsonObject
        val artworkByGame = boxart?.get("data")?.jsonObject
        val games = data["games"]?.jsonArray.orEmpty().mapNotNull { element ->
            val item = element.jsonObject
            val providerId = item.text("id") ?: return@mapNotNull null
            val title = item.text("game_title")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val releaseDate = item.text("release_date")
            val year = releaseDate?.take(4)?.toIntOrNull()
            val images = artworkByGame?.get(providerId)?.let { imageElement ->
                (imageElement as? JsonArray).orEmpty()
            } ?: emptyList()
            val coverImage = images.firstOrNull { image ->
                image.jsonObject.text("type").equals("boxart", ignoreCase = true) &&
                    (image.jsonObject.text("side")?.equals("front", ignoreCase = true) != false)
            } ?: images.firstOrNull { image ->
                image.jsonObject.text("type").equals("boxart", ignoreCase = true)
            }
            val backgroundImage = images.firstOrNull { image ->
                image.jsonObject.text("type").equals("fanart", ignoreCase = true) ||
                    image.jsonObject.text("type").equals("banner", ignoreCase = true)
            }
            val logoImage = images.firstOrNull { image ->
                image.jsonObject.text("type").equals("clearlogo", ignoreCase = true)
            }
            val screenshots = images.filter { image ->
                image.jsonObject.text("type").equals("screenshot", ignoreCase = true)
            }.mapNotNull { image ->
                resolveArtwork(baseUrls, image.jsonObject.text("filename"), "medium")
            }.distinct()
            CatalogGame(
                id = GameId("tgdb-" + platform.id + "-" + providerId),
                title = title,
                platform = platform,
                releaseDate = releaseDate ?: year?.toString(),
                description = item.text("overview")?.take(4_000),
                developer = item.text("developer"),
                publisher = item.text("publisher"),
                players = item.text("players"),
                rating = item.text("rating")?.toDoubleOrNull(),
                media = CatalogMedia(
                    cover = resolveArtwork(baseUrls, coverImage?.jsonObject?.text("filename"), "thumb"),
                    background = resolveArtwork(baseUrls, backgroundImage?.jsonObject?.text("filename"), "large"),
                    logo = resolveArtwork(baseUrls, logoImage?.jsonObject?.text("filename"), "original"),
                    screenshots = screenshots,
                ),
                externalIds = mapOf(MetadataProviderId.THE_GAMES_DB to providerId),
            )
        }
        TheGamesDbCatalogPage(games, current, next)
    }.getOrDefault(TheGamesDbCatalogPage(emptyList(), 1, null))

    private fun JsonObject.text(name: String): String? =
        get(name)?.jsonPrimitive?.contentOrNull

    private fun resolveArtwork(baseUrls: JsonObject?, path: String?, size: String): String? = runCatching {
        val raw = path?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val base = baseUrls?.get(size)?.jsonPrimitive?.contentOrNull
            ?: baseUrls?.get("thumb")?.jsonPrimitive?.contentOrNull
        val resolved = if (raw.startsWith("https://", true)) URI(raw) else {
            val root = URI(base?.trim().orEmpty())
            require(root.scheme.equals("https", true) && !root.host.isNullOrBlank() && root.userInfo == null)
            val normalized = if (root.path.endsWith("/")) root else URI(root.toASCIIString() + "/")
            normalized.resolve(raw.removePrefix("/"))
        }
        require(resolved.scheme.equals("https", true) && !resolved.host.isNullOrBlank())
        require(resolved.userInfo == null && resolved.fragment == null)
        resolved.toASCIIString()
    }.getOrNull()
}
