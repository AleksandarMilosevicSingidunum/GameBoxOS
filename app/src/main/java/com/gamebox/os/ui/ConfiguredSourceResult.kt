package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.GameId
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.canonicalDiscoveryPlatformId
import com.gamebox.os.source.configuredDiscoveryGameId

internal data class ConfiguredSourceLink(
    val sourceId: String,
    val label: String,
    val url: String,
)

internal fun configuredSourceResultToDiscoveryGame(
    result: DiscoverySourceGame,
): DiscoveryGame = DiscoveryGame(
    id = GameId(configuredDiscoveryGameId(result.sourceId, result.externalId)),
    title = result.title,
    platformId = canonicalDiscoveryPlatformId(result.platform),
    region = result.region,
    releaseDate = result.year?.toString(),
    description = null,
    players = null,
    rating = null,
    coverUrl = result.coverUrl,
    backgroundUrl = null,
    logoUrl = null,
    screenshots = emptyList(),
    favorite = false,
)
