package com.gamebox.os.source

import com.gamebox.os.domain.normalizeCatalogTitle
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface VimmLairTransport {
    suspend fun get(uri: URI): String
}

internal class VimmLairPageCache(
    private val maxEntries: Int = 24,
    private val ttlMillis: Long = 5 * 60_000L,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private data class Entry(val body: String, val expiresAtMillis: Long)
    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)

    init {
        require(maxEntries in 1..128) { "Vimm cache size must be between 1 and 128" }
        require(ttlMillis in 1_000L..60 * 60_000L) {
            "Vimm cache TTL must be between 1 second and 1 hour"
        }
    }

    @Synchronized
    fun get(uri: URI): String? {
        val key = uri.toASCIIString()
        val entry = entries[key] ?: return null
        if (entry.expiresAtMillis <= nowMillis()) {
            entries.remove(key)
            return null
        }
        return entry.body
    }

    @Synchronized
    fun put(uri: URI, body: String) {
        val key = uri.toASCIIString()
        entries[key] = Entry(
            body = body,
            expiresAtMillis = nowMillis() + ttlMillis,
        )
        while (entries.size > maxEntries) {
            val eldest = entries.entries.iterator()
            if (!eldest.hasNext()) break
            eldest.next()
            eldest.remove()
        }
    }
}

private val sharedVimmLairPageCache = VimmLairPageCache()

internal class CachedVimmLairTransport(
    private val delegate: VimmLairTransport,
    private val cache: VimmLairPageCache = sharedVimmLairPageCache,
) : VimmLairTransport {
    override suspend fun get(uri: URI): String {
        cache.get(uri)?.let { return it }
        return delegate.get(uri).also { cache.put(uri, it) }
    }
}

private fun defaultVimmLairTransport(): VimmLairTransport =
    CachedVimmLairTransport(HttpsVimmLairTransport())

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
        return VimmLairProbeResult(
            platform = platform,
            url = url,
            vaultLinks = vaultLinks,
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

data class VimmLairProbeResult(
    val platform: String,
    val url: String,
    val vaultLinks: Int,
)

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

data class VimmLairSearchSummary(
    val games: List<DiscoverySourceGame>,
    val attemptedPlatforms: List<String>,
    val failedPlatforms: List<String>,
)

suspend fun searchVimmLairAcrossPlatforms(
    config: GameSourceConfig,
    query: String,
    selectedPlatform: String? = null,
    transport: VimmLairTransport = defaultVimmLairTransport(),
): VimmLairSearchSummary {
    val platforms = vimmLairSearchPlatforms(config, selectedPlatform)
    require(platforms.isNotEmpty()) {
        if (selectedPlatform.isNullOrBlank()) "No supported Vimm console is configured"
        else "Vimm is not available for " + selectedPlatform
    }
    val source = VimmLairDiscoverySource(config, transport)
    val games = mutableListOf<DiscoverySourceGame>()
    val failed = mutableListOf<String>()
    platforms.forEach { platform ->
        runCatching {
            source.search(platform = platform, query = query).games
        }.onSuccess(games::addAll)
            .onFailure { failed += platform }
    }
    return VimmLairSearchSummary(
        games = games
            .distinctBy { it.sourceId.lowercase() + ":" + it.externalId }
            .sortedWith(
                compareBy<DiscoverySourceGame> { vimmLairMatchRank(it.title, query) }
                    .thenBy { it.platform.lowercase() }
                    .thenBy { it.title.lowercase() }
            ),
        attemptedPlatforms = platforms,
        failedPlatforms = failed,
    )
}

internal fun vimmLairSearchPlatforms(
    config: GameSourceConfig,
    selectedPlatform: String?,
    maxPlatforms: Int = 8,
): List<String> {
    require(maxPlatforms in 1..16) { "Vimm search platform limit must be between 1 and 16" }
    selectedPlatform?.trim()?.takeIf(String::isNotEmpty)?.let { selected ->
        return if (config.supportsPlatform(selected) && vimmLairPlatformSlug(selected) != null) {
            listOf(selected)
        } else {
            emptyList()
        }
    }

    val candidates = if (config.platforms.isNotEmpty()) {
        config.platforms.toList()
    } else {
        listOf("PS2", "GameCube", "Wii", "PSP", "Dreamcast")
    }
    return candidates.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .filter { vimmLairPlatformSlug(it) != null }
        .distinctBy { normalizeCatalogTitle(it) }
        .sortedBy { normalizeCatalogTitle(it) }
        .take(maxPlatforms)
        .toList()
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

internal fun vimmLairMatchRank(title: String, query: String): Int {
    val normalizedTitle = normalizeCatalogTitle(title)
    val normalizedQuery = normalizeCatalogTitle(query)
    if (normalizedQuery.isBlank()) return 3
    return when {
        normalizedTitle == normalizedQuery -> 0
        normalizedTitle.startsWith(normalizedQuery) -> 1
        normalizedTitle.split(Regex("[^a-z0-9]+"))
            .any { it.startsWith(normalizedQuery) } -> 2
        else -> 3
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
