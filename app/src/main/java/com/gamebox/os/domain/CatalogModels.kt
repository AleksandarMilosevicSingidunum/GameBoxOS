package com.gamebox.os.domain

/**
 * Canonical catalog identity is independent from metadata providers and acquisition sources.
 * A discovery-only record may be browsed and favorited without exposing an Install action.
 */
data class CatalogGame(
    val id: GameId,
    val title: String,
    val normalizedTitle: String = normalizeCatalogTitle(title),
    val platform: CatalogPlatform,
    val region: String? = null,
    val releaseDate: String? = null,
    val description: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val genres: Set<String> = emptySet(),
    val players: String? = null,
    val rating: Double? = null,
    val media: CatalogMedia = CatalogMedia(),
    val externalIds: Map<MetadataProviderId, String> = emptyMap(),
)

data class CatalogPlatform(
    val id: String,
    val name: String,
    val externalIds: Map<MetadataProviderId, String> = emptyMap(),
)

data class CatalogMedia(
    val cover: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val screenshots: List<String> = emptyList(),
)

enum class MetadataProviderId { THE_GAMES_DB, SCREEN_SCRAPER, REDUMP, NO_INTRO }

data class RomIdentity(
    val gameId: GameId,
    val region: String? = null,
    val revision: String? = null,
    val crc32: String? = null,
    val md5: String? = null,
    val sha1: String? = null,
    val sizeBytes: Long? = null,
) {
    init {
        require(listOf(crc32, md5, sha1).any { !it.isNullOrBlank() }) {
            "ROM identity requires at least one hash"
        }
        require(sizeBytes == null || sizeBytes >= 0) { "ROM size must not be negative" }
    }
}

enum class GameSourceType { LOCAL_FILE, NAS, AUTHORIZED_HTTP, AUTHORIZED_TORRENT, EXTERNAL_PAGE }

data class GameSource(
    val gameId: GameId,
    val providerId: String,
    val type: GameSourceType,
    val location: String,
    val available: Boolean,
    val license: String? = null,
    val expectedSha256: String? = null,
)

enum class StoreAvailability {
    INSTALLED,
    AUTHORIZED_DOWNLOAD,
    IMPORT_AVAILABLE,
    EXTERNAL_SOURCE,
    DISCOVER_ONLY,
}

fun storeAvailability(installed: Boolean, sources: List<GameSource>): StoreAvailability = when {
    installed -> StoreAvailability.INSTALLED
    sources.any { it.available && it.type in setOf(
        GameSourceType.AUTHORIZED_HTTP,
        GameSourceType.AUTHORIZED_TORRENT,
    ) && !it.expectedSha256.isNullOrBlank() } -> StoreAvailability.AUTHORIZED_DOWNLOAD
    sources.any { it.available && it.type in setOf(GameSourceType.LOCAL_FILE, GameSourceType.NAS) } ->
        StoreAvailability.IMPORT_AVAILABLE
    sources.any { it.available && it.type == GameSourceType.EXTERNAL_PAGE } ->
        StoreAvailability.EXTERNAL_SOURCE
    else -> StoreAvailability.DISCOVER_ONLY
}

fun normalizeCatalogTitle(value: String): String =
    value.lowercase().filter { it.isLetterOrDigit() }
