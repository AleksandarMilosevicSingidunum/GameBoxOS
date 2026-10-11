package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LibretroThumbnails(val boxartUrl: String?, val screenshotUrl: String?)

/** Keyless, exact-filename artwork lookup; never guesses a game download URL. */
class LibretroThumbnailClient(
    private val exists: suspend (String) -> Boolean = ::headExists,
) : GameMetadataCandidateProvider {
    private val cache = LinkedHashMap<String, LibretroThumbnails>(128, 0.75f, true)
    private val boxartCache = LinkedHashMap<String, String>(128, 0.75f, true)

    /** Fast box-art-only lookup for result cards; avoids probing screenshots as well. */
    suspend fun lookupBoxart(platform: String, title: String, region: String? = null): String? {
        val system = systemDirectory(platform) ?: return null
        val key = "$system|$title|$region"
        synchronized(boxartCache) { boxartCache[key]?.let { return it } }
        synchronized(cache) { cache[key]?.boxartUrl?.let { return it } }
        val artwork = firstExistingThumbnail(system, "Named_Boxarts", thumbnailFileNames(title, region))
        if (artwork != null) {
            synchronized(boxartCache) {
                if (boxartCache.size >= 128) boxartCache.remove(boxartCache.keys.first())
                boxartCache[key] = artwork
            }
        }
        return artwork
    }

    suspend fun lookup(platform: String, title: String, region: String? = null): LibretroThumbnails {
        val system = systemDirectory(platform) ?: return LibretroThumbnails(null, null)
        val key = "$system|$title|$region"
        synchronized(cache) { cache[key]?.let { return it } }
        val names = thumbnailFileNames(title, region)
        // Archive lookups may need several exact-name variants (region and
        // revision suffixes). Probe small batches concurrently so one 4-second
        // timeout per candidate does not turn a Library import into a minute-long
        // wait. Keep batches and their result ordering deterministic: the first
        // preferred filename that exists still wins.
        val boxart = firstExistingThumbnail(system, "Named_Boxarts", names)
        val screenshot = firstExistingThumbnail(system, "Named_Snaps", names)
        val result = LibretroThumbnails(boxart, screenshot)
        // Cache only positive matches. A miss can be caused by a temporary network
        // failure (including a cold start before connectivity is ready); caching it
        // forever means neither catalog refresh nor a later import can recover art.
        if (result.boxartUrl != null || result.screenshotUrl != null) {
            synchronized(cache) {
                if (cache.size >= 128) cache.remove(cache.keys.first())
                cache[key] = result
            }
        }
        return result
    }

    private suspend fun firstExistingThumbnail(
        system: String,
        kind: String,
        names: List<String>,
    ): String? {
        for (batch in names.chunked(4)) {
            val matches = coroutineScope {
                batch.map { name ->
                    val url = thumbnailUrl(system, kind, name)
                    async { url.takeIf { exists(it) } }
                }.awaitAll()
            }
            matches.firstOrNull { it != null }?.let { return it }
        }
        return null
    }

    suspend fun enrich(game: Game): Game {
        // Display-title overrides are presentation-only. Archive filenames are based
        // on the provider's canonical release title, so use that stable value for
        // exact matching or a harmless rename makes existing box art disappear.
        val art = lookup(game.platform, game.providerTitle, game.region).boxartUrl ?: return game
        return game.copy(artworkUrl = art, providerArtworkUrl = art)
    }

    override suspend fun findCandidates(game: Game): List<MetadataMatchCandidate> {
        val art = lookup(game.platform, game.title, game.region).boxartUrl ?: return emptyList()
        return listOf(MetadataMatchCandidate(
            externalId = "${game.platform}:${game.title}".take(80),
            title = game.title,
            platform = game.platform,
            artworkUrl = art,
            provider = "LIBRETRO_THUMBNAILS",
        ))
    }
}

internal fun systemDirectory(platform: String): String? =
    when (platform.lowercase().filter(Char::isLetterOrDigit)) {
        "nes", "nintendoentertainmentsystem" -> "Nintendo - Nintendo Entertainment System"
        "snes", "supernintendo", "supernintendoentertainmentsystem" -> "Nintendo - Super Nintendo Entertainment System"
        "gb", "gameboy", "nintendogameboy" -> "Nintendo - Game Boy"
        "gbc", "gameboycolor", "nintendogameboycolor" -> "Nintendo - Game Boy Color"
        "gba", "gameboyadvance", "nintendogameboyadvance" -> "Nintendo - Game Boy Advance"
        "n64", "nintendo64" -> "Nintendo - Nintendo 64"
        "ds", "nds", "nintendods" -> "Nintendo - Nintendo DS"
        "3ds", "nintendo3ds" -> "Nintendo - Nintendo 3DS"
        "gamecube", "nintendogamecube" -> "Nintendo - GameCube"
        "wii", "nintendowii" -> "Nintendo - Wii"
        // Libretro publishes Wii artwork, but no separate WiiWare directory.
        "wiiware" -> "Nintendo - Wii"
        // There is no Switch directory in the official Libretro thumbnail archive.
        "ps1", "psx", "playstation", "sonyplaystation" -> "Sony - PlayStation"
        "ps2", "playstation2", "sonyplaystation2" -> "Sony - PlayStation 2"
        "ps3", "playstation3", "sonyplaystation3" -> "Sony - PlayStation 3"
        "psp", "playstationportable", "sonyplaystationportable" -> "Sony - PlayStation Portable"
        "dreamcast", "segadreamcast" -> "Sega - Dreamcast"
        "xbox", "microsoftxbox" -> "Microsoft - Xbox"
        "xbox360", "microsoftxbox360" -> "Microsoft - Xbox 360"
        else -> null
    }

internal fun thumbnailFileNames(title: String, region: String?): List<String> {
    // Imported filenames may retain both a region and a language/revision suffix,
    // e.g. "Title (Europe) (En,Fr)". Keep the full cleaned name first, then
    // progressively remove trailing qualifiers so archive naming still matches.
    val stems = generateSequence(title.trim()) { previous ->
        previous.replace(Regex("\\s*\\([^)]*\\)\\s*$"), "").trim()
            .takeIf { it != previous }
    }.map { stem ->
        stem.replace(Regex("\\s*:\\s*"), " - ")
            .replace(Regex("[&*/:<>?\\\\|\"]"), "_")
            .replace(Regex("\\s+"), " ").trim()
    }.filter(String::isNotBlank).distinct().toList()
    if (stems.isEmpty()) return emptyList()
    val regions = when (region?.lowercase()) {
        "usa", "us", "north america" -> listOf("USA", "Europe", "Japan")
        "europe", "eu" -> listOf("Europe", "USA", "Japan")
        "japan", "jp" -> listOf("Japan", "USA", "Europe")
        else -> listOf("USA", "Europe", "Japan")
    }
    return ((regions + "USA, Europe").flatMap { candidateRegion ->
        stems.map { "$it ($candidateRegion)" }
    } + stems)
        .distinct()
}

internal fun thumbnailUrl(system: String, kind: String, name: String): String =
    "https://thumbnails.libretro.com/${encodePath(system)}/$kind/${encodePath(name)}.png"

private fun encodePath(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

private suspend fun headExists(url: String): Boolean = withContext(Dispatchers.IO) {
    val endpoint = URL(url)
    if (endpoint.protocol != "https" || endpoint.host != "thumbnails.libretro.com") {
        return@withContext false
    }
    val connection = endpoint.openConnection() as HttpsURLConnection
    try {
        connection.requestMethod = "HEAD"
        connection.connectTimeout = 4_000
        connection.readTimeout = 4_000
        connection.instanceFollowRedirects = false
        connection.responseCode == HttpsURLConnection.HTTP_OK &&
            connection.contentType?.startsWith("image/") == true
    } catch (_: Exception) {
        false
    } finally {
        connection.disconnect()
    }
}
