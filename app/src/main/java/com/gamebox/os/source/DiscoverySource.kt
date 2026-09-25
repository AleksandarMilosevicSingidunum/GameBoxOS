package com.gamebox.os.source

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class GameSourceProviderType {
    GAMEBOX_JSON,
    EXTERNAL_WEB,
    WEBDAV,
    S3,
}

@Serializable
data class GameSourceConfig(
    val id: String,
    val name: String,
    val type: GameSourceProviderType,
    val baseUrl: String,
    val enabled: Boolean = true,
    val platforms: Set<String> = emptySet(),
    val credentialKey: String? = null,
    val searchUrlTemplate: String? = null,
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9._-]{1,63}"))) {
            "Source id must contain 2-64 lowercase letters, digits, dots, underscores, or dashes"
        }
        require(name.isNotBlank()) { "Source name must not be blank" }
        require(credentialKey == null || credentialKey.isNotBlank()) {
            "Credential key must not be blank"
        }
        validateGameSourceUrl(baseUrl, "Source URL")
        searchUrlTemplate?.trim()?.takeIf { it.isNotEmpty() }?.let { template ->
            validateGameSourceUrl(
                template
                    .replace("{query}", "game")
                    .replace("{title}", "game")
                    .replace("{platform}", "platform"),
                "Search URL template",
            )
        }
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

private val gameSourceJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun encodeGameSourceConfigs(sources: List<GameSourceConfig>): String {
    require(sources.size <= 32) { "At most 32 game sources are supported" }
    require(sources.map { it.id.lowercase() }.distinct().size == sources.size) {
        "Game source IDs must be unique"
    }
    return gameSourceJson.encodeToString(sources.sortedBy { it.id })
}

internal fun decodeGameSourceConfigs(value: String?): List<GameSourceConfig> {
    if (value.isNullOrBlank()) return emptyList()
    return runCatching {
        gameSourceJson.decodeFromString<List<GameSourceConfig>>(value)
            .take(32)
            .distinctBy { it.id.lowercase() }
    }.getOrDefault(emptyList())
}

fun nextGameSourceId(name: String, existingIds: Set<String>): String {
    val base = name.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(48)
        .ifBlank { "source" }
        .let { if (it.length == 1) it + "-source" else it }
    val normalizedExisting = existingIds.mapTo(mutableSetOf()) { it.lowercase() }
    if (base !in normalizedExisting) return base
    var suffix = 2
    while (true) {
        val candidate = base.take(56) + "-" + suffix
        if (candidate !in normalizedExisting) return candidate
        suffix += 1
    }
}

fun GameSourceConfig.supportsPlatform(platform: String?): Boolean {
    if (!enabled) return false
    if (platform.isNullOrBlank() || platforms.isEmpty()) return true
    val normalized = platform.lowercase().filter(Char::isLetterOrDigit)
    return platforms.any { configured ->
        configured.lowercase().filter(Char::isLetterOrDigit) == normalized
    }
}

fun GameSourceConfig.resolveBrowseUrl(title: String, platform: String): String {
    val template = searchUrlTemplate?.trim().orEmpty()
    if (template.isEmpty()) return baseUrl.trim()

    fun encoded(value: String): String =
        URLEncoder.encode(value.trim(), StandardCharsets.UTF_8.name()).replace("+", "%20")

    val titleValue = encoded(title)
    val platformValue = encoded(platform)
    val queryValue = encoded(listOf(title.trim(), platform.trim()).filter(String::isNotBlank).joinToString(" "))
    val resolved = template
        .replace("{query}", queryValue)
        .replace("{title}", titleValue)
        .replace("{platform}", platformValue)
    validateGameSourceUrl(resolved, "Resolved source URL")
    return resolved
}

private fun validateGameSourceUrl(value: String, label: String) {
    val uri = try {
        URI(value.trim())
    } catch (error: Exception) {
        throw IllegalArgumentException("$label is invalid", error)
    }
    require(uri.scheme.equals("https", ignoreCase = true)) {
        "$label must use HTTPS"
    }
    require(!uri.host.isNullOrBlank()) { "$label must include a host" }
    require(uri.userInfo == null) { "Credentials must not be embedded in the source URL" }
    require(uri.fragment == null) { "$label must not include a fragment" }
}
