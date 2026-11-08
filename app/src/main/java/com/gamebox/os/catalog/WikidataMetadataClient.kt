package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.normalizeCatalogTitle
import com.gamebox.os.source.fetchCancellableHttpsText
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Keyless metadata fallback backed by Wikidata's public entity API.
 *
 * This is deliberately metadata-only: it never resolves a ROM/download URL. Exact title
 * matching is required before provider-owned fields are changed, and missing Wikidata fields
 * leave the existing catalog values untouched.
 */
class WikidataMetadataClient(
    private val maxResponseBytes: Int = 1_500_000,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val connectionFactory: (URL) -> HttpsURLConnection = {
        it.openConnection() as HttpsURLConnection
    },
) : GameMetadataCandidateProvider {
    private val cache = LinkedHashMap<String, List<WikidataMetadata>>(CACHE_CAPACITY, 0.75f, true)

    suspend fun enrich(game: Game): Game = withContext(Dispatchers.IO) {
        if (game.title.isBlank()) return@withContext game
        val key = normalizeCatalogTitle(game.title)
        val matches = cachedLookup(key) { lookup(game.title) }.orEmpty()
        val hasPlatformEvidence = matches.any { it.hasPlatformClaims }
        val platformMatches = if (hasPlatformEvidence) {
            matches.filter { metadata -> metadata.platforms.any { sameGamePlatform(game.platform, it) } }
        } else matches
        val selected = platformMatches.singleOrNull() ?: platformMatches
            .filter { it.year == game.year && game.year != 0 }
            .singleOrNull()
        selected?.mergeInto(game) ?: game
    }

    override suspend fun findCandidates(game: Game): List<MetadataMatchCandidate> = withContext(Dispatchers.IO) {
        if (game.title.isBlank()) return@withContext emptyList()
        val key = normalizeCatalogTitle(game.title)
        cachedLookup(key) { lookup(game.title) }.orEmpty().map { metadata ->
            MetadataMatchCandidate(
                externalId = metadata.externalId,
                title = metadata.title,
                platform = metadata.platforms.takeIf { it.isNotEmpty() }?.joinToString(", "),
                year = metadata.year,
                description = metadata.description,
                artworkUrl = metadata.artworkUrl,
                provider = "WIKIDATA",
            )
        }
    }

    private suspend fun cachedLookup(key: String, load: suspend () -> List<WikidataMetadata>): List<WikidataMetadata>? {
        synchronized(cache) { cache[key] }?.let { return it }
        val metadata = runCatching { load() }
            .onFailure { if (it is CancellationException) throw it }
            .getOrNull()
        if (!metadata.isNullOrEmpty()) synchronized(cache) {
            cache[key] = metadata
            while (cache.size > CACHE_CAPACITY) cache.remove(cache.keys.first())
        }
        return metadata
    }

    private suspend fun lookup(title: String): List<WikidataMetadata> {
        val search = request(
            "https://www.wikidata.org/w/api.php?action=wbsearchentities" +
                "&search=" + encode(title) +
                "&language=en&uselang=en&format=json&limit=8",
        )
        val candidates = search.jsonObject["search"]?.jsonArray.orEmpty()
            .mapNotNull { it.jsonObject }
            .filter { normalizeCatalogTitle(it["label"]?.jsonPrimitive?.contentOrNull.orEmpty()) ==
                normalizeCatalogTitle(title) }
            .take(4)
        if (candidates.isEmpty()) return emptyList()

        val ids = candidates.mapNotNull { it["id"]?.jsonPrimitive?.contentOrNull }
        val entities = request(
            "https://www.wikidata.org/w/api.php?action=wbgetentities" +
                "&ids=" + encode(ids.joinToString("|")) +
                "&props=" + encode("claims|descriptions") + "&languages=en&format=json",
        ).jsonObject["entities"]?.jsonObject.orEmpty()
        val metadata = candidates.mapNotNull { candidate ->
            val id = candidate["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            parseEntity(id, entities[id]?.jsonObject, title)
        }.distinctBy { it.externalId }
        val platformIds = metadata.flatMap { it.platformIds }.distinct().take(MAX_PLATFORM_IDS)
        val platformLabels = if (platformIds.isEmpty()) emptyMap() else runCatching {
            val platformEntities = request(
                "https://www.wikidata.org/w/api.php?action=wbgetentities" +
                    "&ids=" + encode(platformIds.joinToString("|")) +
                    "&props=labels&languages=en&format=json",
            ).jsonObject["entities"]?.jsonObject.orEmpty()
            platformEntities.mapNotNull { (id, value) ->
                value.jsonObject["labels"]?.jsonObject?.get("en")?.jsonObject
                    ?.get("value")?.jsonPrimitive?.contentOrNull?.trim()
                    ?.takeIf { it.isNotEmpty() && it.length <= 120 }?.let { id to it }
            }.toMap()
        }.onFailure { if (it is CancellationException) throw it }.getOrDefault(emptyMap())
        return metadata.map { item ->
            item.copy(platforms = item.platformIds.mapNotNull(platformLabels::get))
        }
    }

    private fun parseEntity(id: String, entity: JsonObject?, requestedTitle: String): WikidataMetadata? {
        if (entity == null) return null
        val description = entity["descriptions"]?.jsonObject
            ?.get("en")?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
            ?.trim()?.takeIf { it.isNotEmpty() }?.take(4_000)
        val claims = entity["claims"]?.jsonObject ?: return null
        val isVideoGame = claims["P31"]?.jsonArray.orEmpty().any { claim ->
            claim.jsonObject["mainsnak"]?.jsonObject?.get("datavalue")?.jsonObject
                ?.get("value")?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull == VIDEO_GAME_QID
        }
        if (!isVideoGame) return null
        val year = claims["P577"]?.jsonArray.orEmpty()
            .firstNotNullOfOrNull { claim ->
                val value = claim.jsonObject["mainsnak"]?.jsonObject
                    ?.get("datavalue")?.jsonObject?.get("value")
                val time = value?.jsonObject?.get("time")?.jsonPrimitive?.contentOrNull
                    ?: value?.jsonPrimitive?.contentOrNull
                time?.removePrefix("+")?.take(4)?.toIntOrNull()
                    ?: Regex("\\\"time\\\"\\s*:\\s*\\\"\\+?(\\d{4})").find(claim.toString())
                        ?.groupValues?.getOrNull(1)?.toIntOrNull()
            }?.takeIf { it in 1900..2100 }
        val image = claims["P18"]?.jsonArray.orEmpty()
            .firstNotNullOfOrNull { claim ->
                claim.jsonObject["mainsnak"]?.jsonObject
                    ?.get("datavalue")?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
            }?.let(::commonsFileUrl)
        if (description == null && year == null && image == null) return null
        return WikidataMetadata(
            externalId = id,
            title = requestedTitle,
            year = year,
            description = description,
            artworkUrl = image,
            platformIds = claims[PLATFORM_PROPERTY]?.jsonArray.orEmpty().mapNotNull { claim ->
                claim.jsonObject["mainsnak"]?.jsonObject?.get("datavalue")?.jsonObject
                    ?.get("value")?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
            }.distinct().take(MAX_PLATFORM_IDS),
        )
    }

    private suspend fun request(value: String): JsonObject {
        val uri = URI(value)
        require(uri.scheme.equals("https", true) && uri.host == "www.wikidata.org")
        val text = fetchCancellableHttpsText(
            URL(uri.toASCIIString()), maxResponseBytes, "Wikidata metadata request",
            configure = {
                it.connectTimeout = 8_000
                it.readTimeout = 12_000
                it.instanceFollowRedirects = false
                it.setRequestProperty("Accept", "application/json")
                it.setRequestProperty("User-Agent", "GameBoxOS/0.1 metadata; contact=local")
            },
            connectionFactory = connectionFactory,
        )
        return json.parseToJsonElement(text).jsonObject
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun commonsFileUrl(value: String): String? = runCatching {
        val file = value.trim().replace(' ', '_')
        require(file.isNotEmpty() && file.length <= 240)
        "https://commons.wikimedia.org/wiki/Special:FilePath/" + encode(file)
    }.getOrNull()

    private companion object {
        const val CACHE_CAPACITY = 128
        const val VIDEO_GAME_QID = "Q7889"
        const val PLATFORM_PROPERTY = "P400"
        const val MAX_PLATFORM_IDS = 32
    }
}

private fun sameGamePlatform(gamePlatform: String, metadataPlatform: String): Boolean {
    fun identity(value: String): String {
        val normalized = normalizeCatalogTitle(value)
        return PLATFORM_ALIASES.firstOrNull { normalized in it }?.first() ?: normalized
    }
    return identity(gamePlatform) == identity(metadataPlatform)
}

private val PLATFORM_ALIASES = listOf(
    setOf("nes", "nintendoentertainmentsystem"),
    setOf("snes", "supernintendo", "supernintendoentertainmentsystem"),
    setOf("gb", "gameboy"),
    setOf("gbc", "gameboycolor"),
    setOf("gba", "gameboyadvance"),
    setOf("n64", "nintendo64"),
    setOf("ds", "nds", "nintendods"),
    setOf("3ds", "nintendo3ds"),
    setOf("gamecube", "nintendogamecube"),
    setOf("wii", "wiiware", "nintendowii"),
    setOf("ps1", "psx", "playstation", "sonyplaystation"),
    setOf("ps2", "playstation2", "sonyplaystation2"),
    setOf("ps3", "playstation3", "sonyplaystation3"),
    setOf("psp", "playstationportable", "sonyplaystationportable"),
    setOf("dreamcast", "segadreamcast"),
    setOf("xbox", "microsoftxbox"),
    setOf("xbox360", "microsoftxbox360"),
)

private data class WikidataMetadata(
    val externalId: String,
    val title: String,
    val year: Int?,
    val description: String?,
    val artworkUrl: String?,
    val platformIds: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
) {
    val hasPlatformClaims: Boolean get() = platformIds.isNotEmpty()

    fun mergeInto(game: Game): Game = game.copy(
        year = if (game.year == 0) year ?: game.year else game.year,
        artworkUrl = game.artworkUrl ?: artworkUrl,
        description = game.description ?: description,
        providerYear = if (game.providerYear == 0) year ?: game.providerYear else game.providerYear,
        providerArtworkUrl = game.providerArtworkUrl ?: artworkUrl,
        providerDescription = game.providerDescription ?: description,
        metadataProvider = game.metadataProvider ?: "WIKIDATA",
        metadataExternalId = game.metadataExternalId ?: externalId,
        metadataMatchedAtMillis = game.metadataMatchedAtMillis ?: System.currentTimeMillis(),
    )
}
