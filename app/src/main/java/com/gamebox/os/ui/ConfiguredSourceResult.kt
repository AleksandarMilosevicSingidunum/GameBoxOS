package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.normalizeCatalogTitle
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.canonicalDiscoveryPlatformId
import com.gamebox.os.source.configuredDiscoveryGameId

internal data class ConfiguredSourceLink(
    val sourceId: String,
    val label: String,
    val url: String,
)

internal fun cachedMetadataForConfiguredSourceResult(
    result: DiscoverySourceGame,
    cachedGames: List<DiscoveryGame>,
): DiscoveryGame? {
    val platformId = canonicalDiscoveryPlatformId(result.platform)
    val title = normalizeCatalogTitle(result.title)
    if (platformId.isBlank() || title.isBlank()) return null

    return cachedGames.filter { cached ->
        cached.platformId == platformId &&
            normalizeCatalogTitle(cached.title) == title
    }.singleOrNull()
}

internal fun configuredSourceResultToDiscoveryGame(
    result: DiscoverySourceGame,
    cachedGames: List<DiscoveryGame> = emptyList(),
): DiscoveryGame {
    val cached = cachedMetadataForConfiguredSourceResult(result, cachedGames)
    return DiscoveryGame(
        id = GameId(configuredDiscoveryGameId(result.sourceId, result.externalId)),
        title = result.title,
        platformId = canonicalDiscoveryPlatformId(result.platform),
        region = result.region ?: cached?.region,
        releaseDate = result.year?.toString() ?: cached?.releaseDate,
        description = cached?.description,
        players = cached?.players,
        rating = cached?.rating,
        coverUrl = result.coverUrl ?: cached?.coverUrl,
        backgroundUrl = cached?.backgroundUrl,
        logoUrl = cached?.logoUrl,
        screenshots = cached?.screenshots.orEmpty(),
        favorite = false,
    )
}
