package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.normalizeCatalogTitle
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.canonicalDiscoveryPlatformId
import com.gamebox.os.source.configuredDiscoveryGameId
import com.gamebox.os.source.vimmLairDefaultPresetPlatforms
import com.gamebox.os.source.vimmLairPlatformSlug
import com.gamebox.os.source.vimmLairSupportedPlatforms

internal data class ConfiguredDiscoveryConsoleRow(
    val key: String,
    val label: String,
    val games: List<DiscoverySourceGame>,
)

/** Groups platform aliases together and orders console shelves in the Vimm preset's order. */
internal fun configuredDiscoveryResultConsoleRows(
    results: List<DiscoverySourceGame>,
): List<ConfiguredDiscoveryConsoleRow> {
    val preferredPlatforms = (vimmLairDefaultPresetPlatforms + vimmLairSupportedPlatforms)
        .distinctBy { vimmLairPlatformSlug(it) ?: canonicalDiscoveryPlatformId(it) }
    val platformOrder = preferredPlatforms.mapIndexed { index, platform ->
        val key = vimmLairPlatformSlug(platform)?.lowercase() ?: canonicalDiscoveryPlatformId(platform)
        key to (index to platform)
    }.toMap()
    val groups = LinkedHashMap<String, MutableList<DiscoverySourceGame>>()
    results.forEach { result ->
        val key = vimmLairPlatformSlug(result.platform)?.lowercase()
            ?: canonicalDiscoveryPlatformId(result.platform)
        groups.getOrPut(key) { mutableListOf() } += result
    }
    return groups.map { (key, games) ->
        ConfiguredDiscoveryConsoleRow(
            key = key,
            label = platformOrder[key]?.second ?: games.first().platform,
            games = games.toList(),
        )
    }.sortedWith(
        compareBy<ConfiguredDiscoveryConsoleRow> { platformOrder[it.key]?.first ?: Int.MAX_VALUE }
            .thenBy { it.label.lowercase() },
    )
}

internal fun configuredSourceResultEditionLabel(result: DiscoverySourceGame): String? =
    listOfNotNull(
        result.region?.trim()?.takeIf(String::isNotEmpty),
        result.year?.toString(),
    ).joinToString(" · ").takeIf(String::isNotEmpty)

internal fun configuredSourceResultPlatformLabel(platform: String): String =
    storeConsoles.firstOrNull { normalizeCatalogTitle(platform) in it.aliases }?.cardLabel
        ?: platform.trim().ifEmpty { "Game" }

internal data class ConfiguredSourceLink(
    val sourceId: String,
    val label: String,
    val url: String,
    val tracksBrowserDownload: Boolean = false,
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
        backgroundUrl = result.screenshotUrl ?: cached?.backgroundUrl,
        logoUrl = cached?.logoUrl,
        screenshots = (listOfNotNull(result.screenshotUrl) + cached?.screenshots.orEmpty()).distinct(),
        favorite = false,
    )
}
