package com.gamebox.os.source

import com.gamebox.os.domain.normalizeCatalogTitle
import java.net.URI

internal fun parseVimmLairListing(
    html: String,
    sourceId: String,
    platform: String,
    baseUrl: String,
    query: String,
    limit: Int = 200,
): List<DiscoverySourceGame> {
    require(limit in 1..500) { "Vimm result limit must be between 1 and 500" }
    val normalizedQuery = normalizeCatalogTitle(query)
    if (normalizedQuery.isEmpty()) return emptyList()

    val anchor = Regex(
        """<a\b[^>]*href\s*=\s*["'](/vault/(\d+))["'][^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    return anchor.findAll(html)
        .mapNotNull { match ->
            val externalId = match.groupValues[2]
            val title = decodeVimmHtmlText(
                match.groupValues[3].replace(Regex("<[^>]+>"), " ")
            )
                .replace(Regex("\\s+"), " ")
                .trim()
                .takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            if (!normalizeCatalogTitle(title).contains(normalizedQuery) &&
                !vimmLairApproximateTitleMatch(title, query)
            ) {
                return@mapNotNull null
            }
            DiscoverySourceGame(
                sourceId = sourceId,
                externalId = externalId,
                title = title,
                platform = platform,
                detailsUrl = vimmLairDetailsUrl(baseUrl, externalId),
            )
        }
        .distinctBy { it.externalId }
        .sortedWith(
            compareBy<DiscoverySourceGame> { vimmLairMatchRank(it.title, query) }
                .thenBy { it.title.lowercase() }
        )
        .take(limit)
        .toList()
}

internal fun parseVimmLairDetails(
    html: String,
    sourceId: String,
    externalId: String,
    detailsUrl: String,
): DiscoverySourceGame {
    require(externalId.matches(Regex("\\d{1,12}"))) { "Vimm game id is invalid" }
    val safeDetails = validateVimmLairUrl(detailsUrl)

    val title = vimmMetaContent(html, "og:title")
        ?.let(::cleanVimmPageTitle)
        ?.takeIf(::isMeaningfulVimmTitle)
        ?: vimmHtmlTitle(html)?.let(::cleanVimmPageTitle)
            ?.takeIf(::isMeaningfulVimmTitle)
        ?: "Vimm vault " + externalId

    val platform = vimmLabeledValue(html, setOf("System", "Platform", "Console"))
        ?.take(80)
        .orEmpty()
    val region = vimmLabeledValue(html, setOf("Region"))?.take(80)
    val year = sequenceOf(
        vimmLabeledValue(html, setOf("Year")),
        vimmLabeledValue(html, setOf("Release Date", "Released")),
    ).filterNotNull()
        .mapNotNull { Regex("(19|20)\\d{2}").find(it)?.value?.toIntOrNull() }
        .firstOrNull()
    val coverUrl = vimmMetaContent(html, "og:image")
        ?.let { runCatching { validateVimmLairAssetUrl(it) }.getOrNull() }

    return DiscoverySourceGame(
        sourceId = sourceId,
        externalId = externalId,
        title = title,
        platform = platform,
        region = region,
        year = year,
        detailsUrl = safeDetails,
        coverUrl = coverUrl,
    )
}

internal fun mergeVimmLairDetails(
    searchResult: DiscoverySourceGame,
    parsed: DiscoverySourceGame,
): DiscoverySourceGame {
    require(searchResult.sourceId.equals(parsed.sourceId, ignoreCase = true)) {
        "Vimm detail source does not match search result"
    }
    require(searchResult.externalId == parsed.externalId) {
        "Vimm detail id does not match search result"
    }
    return searchResult.copy(
        title = parsed.title.takeUnless { it.startsWith("Vimm vault ") } ?: searchResult.title,
        platform = parsed.platform.takeIf(String::isNotBlank) ?: searchResult.platform,
        region = parsed.region ?: searchResult.region,
        year = parsed.year ?: searchResult.year,
        detailsUrl = parsed.detailsUrl ?: searchResult.detailsUrl,
        coverUrl = parsed.coverUrl ?: searchResult.coverUrl,
    )
}

private fun vimmMetaContent(html: String, property: String): String? {
    val metaTags = Regex(
        """<meta\b[^>]*>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    val attribute = Regex(
        """([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)')""",
        RegexOption.IGNORE_CASE,
    )
    return metaTags.findAll(html).mapNotNull { tag ->
        val attributes = attribute.findAll(tag.value).associate { match ->
            val name = match.groupValues[1].lowercase()
            val value = match.groupValues[2].ifEmpty { match.groupValues[3] }
            name to decodeVimmHtmlText(value).trim()
        }
        if (attributes["property"]?.equals(property, ignoreCase = true) == true) {
            attributes["content"]?.takeIf(String::isNotEmpty)
        } else null
    }.firstOrNull()
}

private fun vimmHtmlTitle(html: String): String? =
    Regex("""<title\b[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        ?.let(::decodeVimmHtmlText)
        ?.replace(Regex("\\s+"), " ")
        ?.trim()

private fun cleanVimmPageTitle(value: String): String =
    value
        .replace(Regex("""\s*[-|:]\s*Vimm['’]s Lair.*$""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun isMeaningfulVimmTitle(value: String): Boolean {
    val normalized = normalizeCatalogTitle(value)
    return normalized.isNotBlank() && normalized !in setOf("vimmslair", "vimm")
}

private fun vimmLabeledValue(html: String, labels: Set<String>): String? {
    val row = Regex(
        """<tr\b[^>]*>\s*<(?:th|td)\b[^>]*>\s*([^<]{1,40})\s*</(?:th|td)>\s*<td\b[^>]*>(.*?)</td>\s*</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    val normalizedLabels = labels.map { normalizeCatalogTitle(it) }.toSet()
    return row.findAll(html).mapNotNull { match ->
        val label = decodeVimmHtmlText(match.groupValues[1]).trim()
        if (normalizeCatalogTitle(label) !in normalizedLabels) return@mapNotNull null
        decodeVimmHtmlText(match.groupValues[2].replace(Regex("<[^>]+>"), " "))
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf(String::isNotEmpty)
    }.firstOrNull()
}

internal fun validateVimmLairAssetUrl(value: String): String {
    val safe = validateGameSourceUrl(value, "Vimm artwork URL")
    val uri = URI(safe)
    val host = uri.host.lowercase()
    require(host == "vimm.net" || host == "www.vimm.net") {
        "Vimm artwork must use vimm.net"
    }
    return uri.toASCIIString()
}

private fun decodeVimmHtmlText(value: String): String {
    var result = value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&apos;", "'", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&nbsp;", " ", ignoreCase = true)

    result = Regex("&#(\\d{1,7});").replace(result) { match ->
        match.groupValues[1].toIntOrNull()
            ?.takeIf { it in 0..0x10FFFF }
            ?.let { codePoint -> String(Character.toChars(codePoint)) }
            ?: match.value
    }
    result = Regex("&#x([0-9a-fA-F]{1,6});").replace(result) { match ->
        match.groupValues[1].toIntOrNull(16)
            ?.takeIf { it in 0..0x10FFFF }
            ?.let { codePoint -> String(Character.toChars(codePoint)) }
            ?: match.value
    }
    return result
}
