package com.gamebox.os.catalog

import com.gamebox.os.domain.normalizeCatalogTitle
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * A legal storefront/source search entry. These links are discovery helpers only;
 * GameBox never treats them as downloadable game content or bypasses a provider's
 * licensing and access controls.
 */
data class LegalSourceLink(
    val label: String,
    val url: String,
    val description: String,
)

/**
 * Returns first-party storefronts or well-known source repositories appropriate for
 * a catalog platform. Platforms without a dependable authorized storefront mapping
 * intentionally return no link and remain import-your-own-copy only.
 */
fun legalSourceLinks(title: String, platform: String): List<LegalSourceLink> {
    val trimmedTitle = title.trim()
    if (trimmedTitle.isEmpty()) return emptyList()
    val query = encodeQuery("$trimmedTitle $platform")
    val normalizedPlatform = normalizeCatalogTitle(platform)
    return when {
        normalizedPlatform.contains("homebrew") -> listOf(
            LegalSourceLink(
                label = "itch.io",
                url = "https://itch.io/search?q=$query",
                description = "Search independent and homebrew releases",
            ),
            LegalSourceLink(
                label = "GitHub",
                url = "https://github.com/search?q=$query&type=repositories",
                description = "Search source repositories and release downloads",
            ),
        )
        normalizedPlatform.contains("pc") ||
            normalizedPlatform.contains("windows") ||
            normalizedPlatform.contains("linux") -> listOf(
                LegalSourceLink(
                    label = "Steam",
                    url = "https://store.steampowered.com/search/?term=$query",
                    description = "Search the official Steam storefront",
                ),
                LegalSourceLink(
                    label = "GOG",
                    url = "https://www.gog.com/en/games?query=$query",
                    description = "Search the DRM-free GOG storefront",
                ),
            )
        normalizedPlatform.contains("switch") ||
            normalizedPlatform.contains("3ds") ||
            normalizedPlatform.contains("wii") ||
            normalizedPlatform.contains("gamecube") -> listOf(
            LegalSourceLink(
                label = "Nintendo",
                url = "https://www.nintendo.com/us/search/#q=$query",
                description = "Search Nintendo's official catalog",
            ),
        )
        normalizedPlatform.contains("playstation") ||
            normalizedPlatform.contains("ps2") ||
            normalizedPlatform.contains("psp") -> listOf(
            LegalSourceLink(
                label = "PlayStation Store",
                url = "https://store.playstation.com/en-us/search/$query",
                description = "Search the official PlayStation Store",
            ),
        )
        else -> emptyList()
    }
}

private fun encodeQuery(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

