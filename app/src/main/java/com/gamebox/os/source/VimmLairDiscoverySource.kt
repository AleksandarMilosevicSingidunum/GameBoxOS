package com.gamebox.os.source

import com.gamebox.os.domain.normalizeCatalogTitle
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface VimmLairTransport {
    suspend fun get(uri: URI): String
}

class HttpsVimmLairTransport(
    private val maxResponseBytes: Int = 2 * 1024 * 1024,
) : VimmLairTransport {
    init {
        require(maxResponseBytes in 1..8 * 1024 * 1024)
    }

    override suspend fun get(uri: URI): String = withContext(Dispatchers.IO) {
        val validated = validateVimmLairUrl(uri.toASCIIString())
        val connection = URI(validated).toURL().openConnection() as HttpsURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
        connection.setRequestProperty("User-Agent", "GameBoxOS/0.1")
        try {
            val status = connection.responseCode
            require(status in 200..299) {
                if (status in 300..399) "Vimm's Lair redirects are not accepted"
                else "Vimm's Lair request failed with HTTP " + status
            }
            require(connection.contentLengthLong < 0 || connection.contentLengthLong <= maxResponseBytes) {
                "Vimm's Lair response is too large"
            }
            val output = java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= maxResponseBytes) {
                        "Vimm's Lair response is too large"
                    }
                    output.write(buffer, 0, count)
                }
            }
            output.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }
}

class VimmLairDiscoverySource(
    private val config: GameSourceConfig,
    private val transport: VimmLairTransport = HttpsVimmLairTransport(),
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
            if (!normalizeCatalogTitle(title).contains(normalizedQuery)) {
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
        ?.takeIf(String::isNotBlank)
        ?: vimmHtmlTitle(html)?.let(::cleanVimmPageTitle)
        ?.takeIf(String::isNotBlank)
        ?: "Vimm vault " + externalId

    val platform = vimmLabeledValue(html, setOf("System", "Platform", "Console"))
        ?.take(80)
        .orEmpty()

    val region = vimmLabeledValue(html, setOf("Region"))
        ?.take(80)

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
    val escaped = Regex.escape(property)
    val patterns = listOf(
        Regex(
            """<meta\\b[^>]*property\\s*=\\s*["']$escaped["'][^>]*content\\s*=\\s*["']([^"']+)["'][^>]*>""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """<meta\\b[^>]*content\\s*=\\s*["']([^"']+)["'][^>]*property\\s*=\\s*["']$escaped["'][^>]*>""",
            RegexOption.IGNORE_CASE,
        ),
    )
    return patterns.asSequence()
        .mapNotNull { it.find(html)?.groupValues?.getOrNull(1) }
        .map(::decodeVimmHtmlText)
        .map(String::trim)
        .firstOrNull(String::isNotEmpty)
}

private fun vimmHtmlTitle(html: String): String? =
    Regex("""<title\\b[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        ?.let(::decodeVimmHtmlText)
        ?.replace(Regex("\\s+"), " ")
        ?.trim()

private fun cleanVimmPageTitle(value: String): String =
    value
        .replace(Regex("""\\s*[-|:]\\s*Vimm['’]s Lair.*$""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun vimmLabeledValue(html: String, labels: Set<String>): String? {
    val row = Regex(
        """<tr\\b[^>]*>\\s*<(?:th|td)\\b[^>]*>\\s*([^<]{1,40})\\s*</(?:th|td)>\\s*<td\\b[^>]*>(.*?)</td>\\s*</tr>""",
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

internal fun vimmLairBrowseUrl(
    baseUrl: String,
    platform: String,
    query: String,
): String {
    val slug = vimmLairPlatformSlug(platform)
        ?: return validateVimmLairUrl(baseUrl)
    val bucket = vimmLairLetterBucket(query)
    return if (bucket == null) {
        vimmLairPlatformUrl(baseUrl, slug, null)
    } else {
        vimmLairPlatformUrl(baseUrl, slug, bucket)
    }
}

internal fun vimmLairPlatformSlug(platform: String): String? {
    return when (normalizeCatalogTitle(platform)) {
        "ps2", "playstation2", "sonyplaystation2" -> "PS2"
        "gamecube", "nintendogamecube" -> "GameCube"
        "wii", "nintendowii" -> "Wii"
        "psp", "playstationportable", "sonyplaystationportable" -> "PSP"
        "dreamcast", "segadreamcast" -> "Dreamcast"
        "xbox", "microsoftxbox" -> "Xbox"
        "ps1", "playstation", "sonyplaystation" -> "PS1"
        "n64", "nintendo64" -> "N64"
        "ds", "nintendods" -> "DS"
        "gba", "gameboyadvance", "nintendogameboyadvance" -> "GBA"
        "gbc", "gameboycolor", "nintendogameboycolor" -> "GBC"
        "gb", "gameboy", "nintendogameboy" -> "GB"
        "nes", "nintendoentertainmentsystem" -> "NES"
        "snes", "supernintendo", "supernintendoentertainmentsystem" -> "SNES"
        "genesis", "megadrive", "segagenesis" -> "Genesis"
        "saturn", "segasaturn" -> "Saturn"
        else -> null
    }
}

internal fun vimmLairLetterBucket(query: String): String? {
    val first = query.trim().firstOrNull() ?: return null
    return when {
        first.isLetter() -> first.uppercaseChar().toString()
        first.isDigit() -> "0-9"
        else -> null
    }
}

internal fun vimmLairPlatformUrl(
    baseUrl: String,
    platformSlug: String,
    bucket: String?,
): String {
    val base = URI(validateVimmLairUrl(baseUrl))
    val vaultRoot = if (base.path.trimEnd('/').endsWith("/vault")) {
        base.path.trimEnd('/')
    } else {
        "/vault"
    }
    val path = buildString {
        append(vaultRoot)
        append("/")
        append(platformSlug)
        bucket?.let {
            append("/")
            append(it)
        }
    }
    return validateVimmLairUrl(
        URI(base.scheme, null, base.host, base.port, path, null, null).toASCIIString()
    )
}

internal fun vimmLairDetailsUrl(baseUrl: String, externalId: String): String {
    require(externalId.matches(Regex("\\d{1,12}"))) {
        "Vimm game id is invalid"
    }
    val base = URI(validateVimmLairUrl(baseUrl))
    return validateVimmLairUrl(
        URI(base.scheme, null, base.host, base.port, "/vault/" + externalId, null, null)
            .toASCIIString()
    )
}

internal fun validateVimmLairUrl(value: String): String {
    val safe = validateGameSourceUrl(value, "Vimm's Lair URL")
    val uri = URI(safe)
    val host = uri.host.lowercase()
    require(host == "vimm.net" || host == "www.vimm.net") {
        "Vimm's Lair source must use vimm.net"
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
