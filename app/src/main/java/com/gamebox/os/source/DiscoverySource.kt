package com.gamebox.os.source

import java.net.URI

enum class GameSourceProviderType {
    GAMEBOX_JSON,
    EXTERNAL_WEB,
    WEBDAV,
    S3,
}

data class GameSourceConfig(
    val id: String,
    val name: String,
    val type: GameSourceProviderType,
    val baseUrl: String,
    val enabled: Boolean = true,
    val platforms: Set<String> = emptySet(),
    val credentialKey: String? = null,
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9._-]{1,63}"))) {
            "Source id must contain 2-64 lowercase letters, digits, dots, underscores, or dashes"
        }
        require(name.isNotBlank()) { "Source name must not be blank" }
        require(credentialKey == null || credentialKey.isNotBlank()) {
            "Credential key must not be blank"
        }

        val uri = try {
            URI(baseUrl.trim())
        } catch (error: Exception) {
            throw IllegalArgumentException("Source URL is invalid", error)
        }
        require(uri.scheme.equals("https", ignoreCase = true)) {
            "Game sources require HTTPS"
        }
        require(!uri.host.isNullOrBlank()) { "Source URL must include a host" }
        require(uri.userInfo == null) { "Credentials must not be embedded in the source URL" }
        require(uri.fragment == null) { "Source URL must not include a fragment" }
    }
}

data class DiscoverySourceCapabilities(
    val search: Boolean = true,
    val paging: Boolean = true,
    val details: Boolean = true,
    val externalOpen: Boolean = true,
)

data class DiscoverySourceGame(
    val sourceId: String,
    val externalId: String,
    val title: String,
    val platform: String,
    val region: String? = null,
    val year: Int? = null,
    val detailsUrl: String? = null,
    val coverUrl: String? = null,
)

data class DiscoverySourcePage(
    val games: List<DiscoverySourceGame>,
    val nextPage: Int? = null,
)

interface DiscoverySource {
    val id: String
    val displayName: String
    val capabilities: DiscoverySourceCapabilities
        get() = DiscoverySourceCapabilities()

    suspend fun search(
        platform: String? = null,
        query: String = "",
        page: Int = 1,
    ): DiscoverySourcePage

    suspend fun details(externalId: String): DiscoverySourceGame
}

class DiscoverySourceRegistry(
    sources: Collection<DiscoverySource>,
) {
    private val byId = sources.associateBy { it.id.lowercase() }

    init {
        require(byId.size == sources.size) { "Discovery source IDs must be unique" }
    }

    fun source(id: String): DiscoverySource? = byId[id.lowercase()]

    fun all(): List<DiscoverySource> = byId.values.sortedBy { it.displayName.lowercase() }
}
