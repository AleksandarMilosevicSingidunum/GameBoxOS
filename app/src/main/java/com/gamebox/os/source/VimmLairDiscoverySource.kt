package com.gamebox.os.source

import java.net.URI

class VimmLairDiscoverySource(
    private val config: GameSourceConfig,
    private val transport: VimmLairTransport = defaultVimmLairTransport(),
) : DiscoverySource {
    override val id: String = config.id
    override val displayName: String = config.name
    override val capabilities = DiscoverySourceCapabilities(
        search = true,
        paging = false,
        details = true,
        externalOpen = true,
    )

    init {
        require(config.type == GameSourceProviderType.VIMM_LAIR) {
            "Vimm discovery source requires VIMM_LAIR provider type"
        }
        validateVimmLairUrl(config.baseUrl)
    }

    override suspend fun search(
        platform: String?,
        query: String,
        page: Int,
    ): DiscoverySourcePage {
        if (!config.enabled || page != 1) return DiscoverySourcePage(emptyList())
        val titleQuery = query.trim()
        require(titleQuery.isNotEmpty()) {
            "Enter a game title before searching Vimm's Lair"
        }
        val platformName = platform?.trim().orEmpty()
        require(platformName.isNotEmpty()) {
            "Choose a console before searching Vimm's Lair"
        }
        require(config.supportsPlatform(platformName)) {
            config.name + " is not enabled for " + platformName
        }
        val slug = vimmLairPlatformSlug(platformName)
            ?: throw IllegalArgumentException(
                "Vimm's Lair mapping is not configured for " + platformName
            )
        val bucket = vimmLairLetterBucket(titleQuery)
            ?: throw IllegalArgumentException(
                "Search title must start with a letter or number"
            )
        val browseUrl = vimmLairPlatformUrl(config.baseUrl, slug, bucket)
        val html = transport.get(URI(browseUrl))
        val games = parseVimmLairListing(
            html = html,
            sourceId = config.id,
            platform = platformName,
            baseUrl = config.baseUrl,
            query = titleQuery,
        )
        return DiscoverySourcePage(games = games, nextPage = null)
    }

    override suspend fun details(externalId: String): DiscoverySourceGame {
        val detailsUrl = vimmLairDetailsUrl(config.baseUrl, externalId)
        val html = transport.get(URI(detailsUrl))
        return parseVimmLairDetails(
            html = html,
            sourceId = config.id,
            externalId = externalId,
            detailsUrl = detailsUrl,
        )
    }

    suspend fun probe(selectedPlatform: String? = null): VimmLairProbeResult {
        val platform = vimmLairSearchPlatforms(config, selectedPlatform, maxPlatforms = 1)
            .firstOrNull()
            ?: throw IllegalArgumentException("No supported Vimm console is configured")
        val slug = requireNotNull(vimmLairPlatformSlug(platform)) {
            "Vimm platform mapping is unavailable"
        }
        val url = vimmLairPlatformUrl(config.baseUrl, slug, "A")
        val html = transport.get(URI(url))
        val vaultLinks = Regex(
            """<a\b[^>]*href\s*=\s*["']/vault/\d+["'][^>]*>""",
            RegexOption.IGNORE_CASE,
        ).findAll(html).count()
        require(vaultLinks > 0) {
            "Vimm responded, but the expected vault listing structure was not found"
        }
        return VimmLairProbeResult(platform = platform, url = url, vaultLinks = vaultLinks)
    }

    suspend fun hydrate(result: DiscoverySourceGame): DiscoverySourceGame {
        require(result.sourceId.equals(config.id, ignoreCase = true)) {
            "Vimm result belongs to another configured source"
        }
        val detailsUrl = vimmLairDetailsUrl(config.baseUrl, result.externalId)
        val html = transport.get(URI(detailsUrl))
        return mergeVimmLairDetails(
            searchResult = result,
            parsed = parseVimmLairDetails(
                html = html,
                sourceId = config.id,
                externalId = result.externalId,
                detailsUrl = detailsUrl,
            ),
        )
    }
}
