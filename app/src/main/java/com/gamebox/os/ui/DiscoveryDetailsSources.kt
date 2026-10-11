package com.gamebox.os.ui

import com.gamebox.os.domain.normalizeCatalogTitle
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType
import com.gamebox.os.source.resolveBrowseUrl

internal fun configuredSourceGuidance(tracksBrowserDownload: Boolean): String =
    if (tracksBrowserDownload) {
        "A supported transfer continues in the background, appears in Downloads, and is verified and imported automatically. Downloads from other browser apps must be imported manually."
    } else {
        "These are discovery links only. Installing content still requires an imported copy or a verified trusted catalog source."
    }

/** Resolves the enabled, platform-compatible browser links shown for a discovery title. */
internal fun configuredDetailsSourceLinks(
    title: String,
    platformName: String,
    sources: List<GameSourceConfig>,
    directSource: ConfiguredSourceLink?,
): List<ConfiguredSourceLink> = buildList {
    directSource?.let(::add)
    addAll(
        sources.filter { source ->
            source.enabled &&
                source.id != directSource?.sourceId &&
                source.type in detailBrowseSourceTypes &&
                (source.platforms.isEmpty() || source.platforms.any {
                    normalizeCatalogTitle(it) == normalizeCatalogTitle(platformName)
                })
        }.mapNotNull { source ->
            runCatching {
                ConfiguredSourceLink(
                    sourceId = source.id,
                    label = source.name,
                    url = source.resolveBrowseUrl(title, platformName),
                    tracksBrowserDownload = source.type == GameSourceProviderType.VIMM_LAIR,
                )
            }.getOrNull()
        },
    )
}.distinctBy(ConfiguredSourceLink::url)

private val detailBrowseSourceTypes = setOf(
    GameSourceProviderType.EXTERNAL_WEB,
    GameSourceProviderType.VIMM_LAIR,
)
