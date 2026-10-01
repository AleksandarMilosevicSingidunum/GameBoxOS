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
) {
    private val cache = LinkedHashMap<String, WikidataMetadata?>(128, 0.75f, true)

    suspend fun enrich(game: Game): Game = withContext(Dispatchers.IO) {
        if (game.title.isBlank()) return@withContext game
        val key = normalizeCatalogTitle(game.title)
        val metadata = synchronized(cache) {
            if (cache.containsKey(key)) cache[key]
            else null
        } ?: runCatching { lookup(game.title) }
            .onFailure {
                if (it is CancellationException) throw it
            }
            .getOrNull()
        synchronized(cache) {
            if (!cache.containsKey(key)) cache[key] = metadata
        }
        metadata?.mergeInto(game) ?: game
    }

    private suspend fun lookup(title: String): WikidataMetadata? {
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
        if (candidates.isEmpty()) return null

        val ids = candidates.mapNotNull { it["id"]?.jsonPrimitive?.contentOrNull }
        val entities = request(
            "https://www.wikidata.org/w/api.php?action=wbgetentities" +
                "&ids=" + encode(ids.joinToString("|")) +
                "&props=" + encode("claims|descriptions") + "&languages=en&format=json",
        ).jsonObject["entities"]?.jsonObject.orEmpty()
        val selected = candidates.firstNotNullOfOrNull { candidate ->
            val id = candidate["id"]?.jsonPrimitive?.contentOrNull ?: return@firstNotNullOfOrNull null
            parseEntity(id, entities[id]?.jsonObject, title)
        } ?: return null
        return selected
    }

    private fun parseEntity(id: String, entity: JsonObject?, requestedTitle: String): WikidataMetadata? {
        if (entity == null) return null
        val description = entity["descriptions"]?.jsonObject
            ?.get("en")?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
            ?.trim()?.takeIf { it.isNotEmpty() }?.take(4_000)
        val claims = entity["claims"]?.jsonObject ?: return null
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
}

private data class WikidataMetadata(
    val externalId: String,
    val title: String,
    val year: Int?,
    val description: String?,
    val artworkUrl: String?,
) {
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
