package com.gamebox.os.tvmode

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import java.io.InputStream
import java.io.FilterInputStream
import java.io.BufferedInputStream
import java.net.ConnectException
import java.net.URI
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import java.net.URL
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection

internal data class TvLaunchableApp(val packageName: String, val label: String)
internal data class TvWebDestination(val title: String, val url: String)
internal data class TvStream(val url: String, val requestHeaders: Map<String, String> = emptyMap())
internal data class TvChannel(
    val name: String,
    val streamUrl: String,
    val group: String? = null,
    val guideId: String? = null,
    val alternateStreamUrls: List<String> = emptyList(),
    val logoUrl: String? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
    val alternateStreams: List<TvStream> = emptyList(),
) {
    val streams: List<TvStream> get() = (listOf(TvStream(streamUrl, requestHeaders)) + alternateStreams + alternateStreamUrls.map(::TvStream)).distinct()
    val streamUrls: List<String> get() = streams.map(TvStream::url)
}
internal data class TvProgramme(val channelId: String, val start: java.time.Instant, val end: java.time.Instant, val title: String)

private val unverifiedChannelQualitySuffix = Regex(
    "\\s*(?:\\(\\s*(?:\\d{3,4}p|\\d{3,4}\\s*[x×]\\s*\\d{3,4}|hd|sd|fhd|uhd|[48]k)\\s*\\)|(?:\\d{3,4}p|\\d{3,4}\\s*[x×]\\s*\\d{3,4}|hd|sd|fhd|uhd|[48]k))\\s*$",
    RegexOption.IGNORE_CASE,
)

internal fun displayTvChannelName(name: String): String {
    val original = name.trim()
    return original.replace(unverifiedChannelQualitySuffix, "")
        .trimEnd(' ', '-', '–', '—', '|', '·')
        .trim()
        .ifBlank { original }
}

internal fun tvChannelKey(channel: TvChannel): String = channel.guideId?.substringBefore('@')?.lowercase()
    ?.takeIf(String::isNotBlank) ?: channel.name.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")

internal fun nextTvStreamIndex(current: Int, streamCount: Int): Int? =
    (current + 1).takeIf { it in 0 until streamCount }

internal fun retryTvStreamIndex(streamCount: Int): Int? = 0.takeIf { streamCount > 0 }

internal fun tvPlaybackErrorMessage(errorCodeName: String, cause: Throwable): String {
    val causes = generateSequence(cause as Throwable?) { it.cause }.toList()
    return when {
        causes.any { it is UnknownServiceException && it.message.orEmpty().contains("CLEARTEXT", true) } ->
            "This channel uses unencrypted HTTP, which Android blocks for security. Choose an HTTPS stream."
        causes.any { it is UnknownHostException } ->
            "The stream server could not be found. Check the network or choose another channel."
        causes.any { it is ConnectException || it is SocketTimeoutException } ->
            "Could not connect to this stream. It may be offline or unavailable on this network."
        else -> "Stream failed ($errorCodeName). Try another channel or check that it is online."
    }
}

internal fun currentAndNextProgrammes(channel: TvChannel, programmes: List<TvProgramme>, now: java.time.Instant): Pair<TvProgramme?, TvProgramme?> {
    val ids = setOfNotNull(channel.guideId?.lowercase(), channel.guideId?.substringBefore('@')?.lowercase())
    val matching = programmes.filter { it.channelId.lowercase() in ids }.sortedBy(TvProgramme::start)
    return matching.firstOrNull { now >= it.start && now < it.end } to matching.firstOrNull { it.start > now }
}

internal fun hasCurrentTvProgramme(label: String?): Boolean = label?.startsWith("Now ·") == true

internal fun sortSerbianChannelsByPopularity(channels: List<TvChannel>): List<TvChannel> =
    channels.withIndex()
        .sortedWith(compareBy({ serbianChannelPopularityRank(it.value.name) }, { it.index }))
        .map { it.value }

internal fun orderTvChannelsForPlaylist(playlistUri: String?, channels: List<TvChannel>): List<TvChannel> =
    if (playlistUri?.let(::iptvOrgPlaylistCountryCode) == "rs") sortSerbianChannelsByPopularity(channels) else channels

// shortcut: published ratings cover national broadcasters only; unranked channels retain playlist order.
private fun serbianChannelPopularityRank(name: String): Int {
    val key = name.lowercase().filter(Char::isLetterOrDigit)
    return when {
        isMeasuredChannel(key, "rts1", "ртс1") -> 0
        isMeasuredChannel(key, "pink", "пинк") -> 1
        isMeasuredChannel(key, "prva", "прва") -> 2
        isMeasuredChannel(key, "happy", "хепи") -> 3
        isMeasuredChannel(key, "b92", "б92") -> 4
        isMeasuredChannel(key, "rts2", "ртс2") -> 5
        else -> Int.MAX_VALUE
    }
}

private fun isMeasuredChannel(key: String, vararg aliases: String): Boolean = aliases.any { alias ->
    key == alias || key.matches(Regex("$alias(?:\\d{3,4}p|sd|hd|fhd|uhd|4k)"))
}

internal fun discoverTvLaunchableApps(packageManager: PackageManager, ownPackage: String): List<TvLaunchableApp> {
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val matches: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= 33) {
        packageManager.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(query, 0)
    }
    return matches.mapNotNull { info ->
        val activity = info.activityInfo ?: return@mapNotNull null
        val packageName = activity.packageName
        if (packageName == ownPackage) return@mapNotNull null
        TvLaunchableApp(packageName, info.loadLabel(packageManager).toString())
    }.distinctBy(TvLaunchableApp::packageName).sortedBy { it.label.lowercase() }
}

internal fun orderTvApps(apps: List<TvLaunchableApp>, favorites: List<String>): List<TvLaunchableApp> {
    val byPackage = apps.associateBy(TvLaunchableApp::packageName)
    return favorites.mapNotNull(byPackage::get) + apps.filterNot { it.packageName in favorites }
}

internal fun inAppTvWebDestination(app: TvLaunchableApp): TvWebDestination? {
    val key = "${app.packageName} ${app.label}".lowercase()
    return when {
        "netflix" in key -> TvWebDestination(app.label, "https://www.netflix.com/")
        "prime video" in key || "primevideo" in key || "amazon video" in key -> TvWebDestination(app.label, "https://www.primevideo.com/")
        "youtube" in key -> TvWebDestination(app.label, "https://www.youtube.com/tv")
        Regex("\\beon\\b").containsMatchIn(key) -> TvWebDestination(app.label, "https://sr.eon.tv/")
        "rts planeta" in key -> TvWebDestination(app.label, "https://rtsplaneta.rs/sr_lat/live/tv")
        else -> null
    }
}

internal fun officialRtsPlanetaLiveUrl(channel: TvChannel): String? {
    val id = channel.guideId.orEmpty().substringBefore('@').lowercase()
    val name = channel.name.lowercase().replace(Regex("[^a-z0-9]"), "")
    val path = when {
        id == "rts1.rs" || name.startsWith("rts1") -> "5/rts-1"
        id == "rts2.rs" || name.startsWith("rts2") -> "4/rts-2"
        id == "rts3.rs" || name.startsWith("rts3") -> "2/rts-3"
        id == "rtssvet.rs" || name.startsWith("rtssvet") -> "16889/rts-svet"
        else -> return null
    }
    return "https://rtsplaneta.rs/live/tv/$path"
}

internal fun moveTvFavorite(favorites: List<String>, packageName: String, offset: Int): List<String> {
    val index = favorites.indexOf(packageName)
    val destination = index + offset
    if (index < 0 || destination !in favorites.indices) return favorites
    return favorites.toMutableList().apply {
        add(destination, removeAt(index))
    }
}

internal fun playlistGrantToRelease(previousUri: String?, nextUri: String?): String? =
    previousUri?.takeIf { it != nextUri && it.startsWith("content://") }

internal fun playlistUriAfterLoad(activeUri: String?, pendingUri: String?, loadedUri: String, hasChannels: Boolean): String? =
    if (hasChannels && pendingUri != null && pendingUri == loadedUri) pendingUri else activeUri

internal fun iptvOrgCountryPlaylistUrl(countryCode: String): String {
    val code = countryCode.trim().lowercase()
    require(code.matches(Regex("[a-z]{2}"))) { "Enter a two-letter country code" }
    return "https://iptv-org.github.io/iptv/countries/$code.m3u"
}

internal fun fetchIptvOrgPlaylist(url: String): String {
    val uri = URI(url)
    require(uri.scheme == "https" && uri.host.equals("iptv-org.github.io", ignoreCase = true) &&
        uri.path.matches(Regex("/iptv/countries/[a-z]{2}\\.m3u"))
    ) { "Unsupported IPTV playlist source" }
    val connection = URL(url).openConnection() as HttpsURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 15_000
    connection.instanceFollowRedirects = false
    return try {
        require(connection.responseCode == 200) { "iptv-org returned HTTP ${connection.responseCode}" }
        require(connection.contentLengthLong in -1L..MAX_PLAYLIST_BYTES) { "Playlist exceeds the 8 MiB limit" }
        connection.inputStream.buffered().use(::readPlaylist)
    } finally {
        connection.disconnect()
    }
}

internal fun playlistGuideUrl(text: String): String? {
    val header = text.lineSequence().firstOrNull { it.trimStart().startsWith("#EXTM3U", true) } ?: return null
    val match = Regex("(?:url-tvg|x-tvg-url|tvg-url)=\"([^\"]{1,2048})\"", RegexOption.IGNORE_CASE).find(header) ?: return null
    val uri = runCatching { URI(match.groupValues[1]) }.getOrNull() ?: return null
    return uri.toASCIIString().takeIf { uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null }
}

internal fun iptvOrgPlaylistCountryCode(url: String): String? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    if (!uri.host.equals("iptv-org.github.io", true)) return null
    return Regex("/iptv/countries/([a-z]{2})\\.m3u", RegexOption.IGNORE_CASE)
        .matchEntire(uri.path)?.groupValues?.get(1)?.lowercase()
}

internal fun activeTvPlaylistLabel(url: String?): String = url?.let {
    iptvOrgPlaylistCountryCode(it)?.uppercase()?.let { code -> "iptv-org · $code" } ?: "Custom playlist"
} ?: "iptv-org · RS (default)"

internal fun freeEpgCountryGuideUrl(countryCode: String): String {
    val code = countryCode.trim().lowercase()
    require(code.matches(Regex("[a-z]{2}"))) { "Enter a two-letter country code" }
    return "https://free-epg.de/api/epg/$code.xml.gz"
}

internal fun fetchCurrentXmltvProgrammes(url: String, channels: Map<String, String>, now: java.time.Instant): List<TvProgramme> {
    val uri = URI(url)
    require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) { "Guide must use HTTPS" }
    require(channels.isNotEmpty()) { "Playlist has no channel guide IDs" }
    val connection = URL(url).openConnection() as HttpsURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 30_000
    return try {
        require(connection.responseCode == 200) { "Guide returned HTTP ${connection.responseCode}" }
        require(connection.contentLengthLong in -1L..MAX_COMPRESSED_GUIDE_BYTES) { "Guide download exceeds the 16 MiB limit" }
        connection.inputStream.buffered().use { raw ->
            parseCurrentXmltvProgrammes(BoundedGuideInputStream(xmltvContentStream(raw)), channels, now)
        }
    } finally { connection.disconnect() }
}

internal fun readPlaylist(input: InputStream): String {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0L
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        require(total <= MAX_PLAYLIST_BYTES) { "Playlist exceeds the 8 MiB limit" }
        output.write(buffer, 0, count)
    }
    val bytes = output.toByteArray()
    val (offset, charset) = when {
        bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> 3 to Charsets.UTF_8
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> 2 to Charsets.UTF_16LE
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> 2 to Charsets.UTF_16BE
        else -> 0 to Charsets.UTF_8
    }
    return String(bytes, offset, bytes.size - offset, charset)
}

private const val MAX_PLAYLIST_BYTES = 8 * 1024 * 1024L
private const val MAX_COMPRESSED_GUIDE_BYTES = 16 * 1024 * 1024L
private const val MAX_GUIDE_BYTES = 96 * 1024 * 1024L
private const val MAX_CURRENT_GUIDE_PROGRAMMES = 25_000
private const val MAX_PROGRAMMES_PER_GUIDE_CHANNEL = 96

internal fun xmltvContentStream(input: InputStream): InputStream {
    val buffered = if (input is BufferedInputStream) input else BufferedInputStream(input)
    buffered.mark(2)
    val first = buffered.read()
    val second = buffered.read()
    buffered.reset()
    return if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
}

internal fun parseAuthorizedM3u(text: String): List<TvChannel> {
    val channels = mutableListOf<TvChannel>()
    var pendingName: String? = null
    var pendingGroup: String? = null
    var pendingGuideId: String? = null
    var pendingLogoUrl: String? = null
    var pendingHeaders = emptyMap<String, String>()
    text.lineSequence().take(20_000).forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("#EXTINF", ignoreCase = true) -> {
                pendingHeaders = emptyMap()
                pendingName = extinfDisplayName(line)
                pendingGroup = Regex("(?:^|\\s)group-title=\"([^\"]{1,80})\"", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.get(1)?.takeIf(String::isNotBlank)
                pendingGuideId = Regex("(?:^|\\s)tvg-id=\"([^\"]{1,160})\"", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.get(1)?.takeIf(String::isNotBlank)
                pendingLogoUrl = Regex("(?:^|\\s)tvg-logo=\"([^\"]{1,2048})\"", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.get(1)?.let(::safeChannelLogoUrl)
            }
            line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                parseM3uRequestHeader(line.substringAfter(':'))?.let { (name, value) -> pendingHeaders = pendingHeaders + (name to value) }
            }
            line.isNotEmpty() && !line.startsWith('#') && pendingName != null -> {
                val rawUrl = line.substringBefore('|').trim()
                val uri = runCatching { URI(rawUrl) }.getOrNull()
                val pipeHeaders = line.substringAfter('|', "").split('&').mapNotNull(::parseM3uRequestHeader)
                    .toMap()
                val headers = pendingHeaders + pipeHeaders
                if (uri != null && uri.scheme?.lowercase() in setOf("https", "http", "rtsp") &&
                    !uri.host.isNullOrBlank() && uri.userInfo == null
                ) {
                    channels += TvChannel(
                        pendingName,
                        uri.toASCIIString(),
                        pendingGroup,
                        pendingGuideId,
                        logoUrl = pendingLogoUrl,
                        requestHeaders = headers,
                    )
                    if (channels.size == 500) return channels
                }
                pendingName = null
                pendingGroup = null
                pendingGuideId = null
                pendingLogoUrl = null
                pendingHeaders = emptyMap()
            }
        }
    }
    return channels.groupBy { channel ->
        channel.guideId?.substringBefore('@')?.lowercase() ?: channel.name.lowercase()
            .replace(Regex("\\s*[\\[(](?:\\d{3,4}p|\\d{3,4}x\\d{3,4}|4k|8k|uhd|fhd|hd|sd)[\\])]\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(?:4k|8k|uhd|fhd|hd|sd)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ").trim()
    }.values.map { matches ->
        val primary = matches.first()
        primary.copy(alternateStreams = matches.drop(1).flatMap(TvChannel::streams).distinct())
    }
}

private fun parseM3uRequestHeader(option: String): Pair<String, String>? {
    val nameAndValue = option.trim().split('=', limit = 2).takeIf { it.size == 2 } ?: return null
    val name = when (nameAndValue[0].trim().lowercase()) {
        "http-user-agent", "user-agent" -> "User-Agent"
        "http-referrer", "http-referer", "referer" -> "Referer"
        "http-origin", "origin" -> "Origin"
        else -> return null
    }
    val value = runCatching {
        java.net.URLDecoder.decode(nameAndValue[1].trim(), Charsets.UTF_8.name())
    }.getOrNull()?.removeSurrounding("\"")?.takeIf {
        it.isNotBlank() && it.length <= 512 && it.none(Char::isISOControl)
    } ?: return null
    return name to value
}

private fun safeChannelLogoUrl(value: String): String? = runCatching { URI(value) }.getOrNull()?.let { uri ->
    uri.toASCIIString().takeIf { text -> text.length <= 2048 && uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.userInfo == null }
}

private fun extinfDisplayName(line: String): String? {
    var quoted = false
    line.forEachIndexed { index, char ->
        if (char == '"' && (index == 0 || line[index - 1] != '\\')) quoted = !quoted
        else if (char == ',' && !quoted) return line.substring(index + 1).trim().takeIf(String::isNotBlank)
    }
    return null
}

internal fun parseXmltvProgrammes(xml: String): List<TvProgramme> =
    parseXmltv(xml.byteInputStream(), emptySet(), null)

internal fun parseCurrentXmltvProgrammes(
    input: InputStream,
    channels: Map<String, String>,
    now: java.time.Instant,
): List<TvProgramme> = parseXmltv(
    input,
    channels.keys.flatMap { id -> listOf(id.lowercase(), id.substringBefore('@').lowercase()) }.toSet(),
    now,
    channels,
)

private fun parseXmltv(
    input: InputStream,
    channelIds: Set<String>,
    now: java.time.Instant?,
    channelNames: Map<String, String> = emptyMap(),
): List<TvProgramme> {
    val reader = javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser().xmlReader.apply {
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    }
    val programmes = mutableListOf<TvProgramme>()
    val programmeCounts = mutableMapOf<String, Int>()
    val cutoff = now?.plusSeconds(24 * 60 * 60)
    var id: String? = null
    var start: java.time.Instant? = null
    var end: java.time.Instant? = null
    var inTitle = false
    var title = StringBuilder()
    var xmltvChannelId: String? = null
    var inDisplayName = false
    var displayName = StringBuilder()
    val matchedIds = channelIds.toMutableSet()
    val targetIdByAlias = mutableMapOf<String, String>()
    val programmeIdByChannel = mutableMapOf<String, String>()
    channelNames.forEach { (id, name) ->
        listOf(id.lowercase(), id.substringBefore('@').lowercase()).forEach { programmeIdByChannel[it] = id }
        listOf(normalizedGuideName(id), normalizedGuideName(name)).filter(String::isNotEmpty).forEach { alias ->
            targetIdByAlias.putIfAbsent(alias, id)
        }
    }
    val handler = object : org.xml.sax.helpers.DefaultHandler(), org.xml.sax.ext.LexicalHandler {
        override fun startDTD(name: String?, publicId: String?, systemId: String?) {
            throw org.xml.sax.SAXException("DOCTYPE is not allowed")
        }
        override fun endDTD() = Unit
        override fun startEntity(name: String?) = Unit
        override fun endEntity(name: String?) = Unit
        override fun startCDATA() = Unit
        override fun endCDATA() = Unit
        override fun comment(ch: CharArray, start: Int, length: Int) = Unit
        override fun startElement(uri: String?, localName: String?, qName: String, attributes: org.xml.sax.Attributes) {
            if (qName == "channel") {
                xmltvChannelId = attributes.getValue("id")
                displayName = StringBuilder()
            } else if (qName == "display-name" && xmltvChannelId != null) {
                inDisplayName = true
            } else if (qName == "programme") {
                id = attributes.getValue("channel")
                start = parseXmltvTime(attributes.getValue("start"))
                end = parseXmltvTime(attributes.getValue("stop"))
                title = StringBuilder()
            } else if (qName == "title" && id != null && (channelIds.isEmpty() || id!!.lowercase() in matchedIds)) inTitle = true
        }
        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (inTitle) title.append(ch, start, length)
            if (inDisplayName) displayName.append(ch, start, length)
        }
        override fun endElement(uri: String?, localName: String?, qName: String) {
            if (qName == "title") inTitle = false
            if (qName == "display-name") inDisplayName = false
            if (qName == "channel") {
                val sourceId = xmltvChannelId
                val aliases = listOf(sourceId.orEmpty(), displayName.toString()).map(::normalizedGuideName)
                val targetId = aliases.firstNotNullOfOrNull(targetIdByAlias::get)
                if (sourceId != null && targetId != null) {
                    matchedIds += sourceId.lowercase()
                    programmeIdByChannel[sourceId.lowercase()] = targetId
                }
                xmltvChannelId = null
            } else if (qName == "programme") {
                val from = start; val until = end; val label = title.toString().trim()
                val channelId = id
                val programmeCount = channelId?.lowercase()?.let(programmeCounts::get) ?: 0
                val matches = channelId != null && (channelIds.isEmpty() || channelId.lowercase() in matchedIds)
                if (matches && from != null && until != null && label.isNotEmpty() &&
                    (now == null || (until > now && from < cutoff &&
                        programmeCount < MAX_PROGRAMMES_PER_GUIDE_CHANNEL &&
                        programmes.size < MAX_CURRENT_GUIDE_PROGRAMMES))
                ) {
                    programmes += TvProgramme(programmeIdByChannel[channelId.lowercase()] ?: channelId, from, until, label)
                    if (now != null) programmeCounts[channelId.lowercase()] = programmeCount + 1
                }
                id = null
            }
        }
    }
    reader.contentHandler = handler
    reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler)
    reader.parse(org.xml.sax.InputSource(input))
    return programmes
}

internal fun normalizedGuideName(value: String): String = value
    .substringBefore('@')
    .replace(Regex("\\.[a-z]{2}$", RegexOption.IGNORE_CASE), "")
    .replace(Regex("\\([^)]*\\)|\\[[^]]*]"), " ")
    .replace(Regex("([a-z])([A-Z])"), "$1 $2")
    .replace(Regex("([A-Za-z])([0-9])"), "$1 $2")
    .replace(Regex("([0-9])([A-Za-z])"), "$1 $2")
    .lowercase()
    .split(Regex("[^a-z0-9]+"))
    .filter { it.isNotBlank() && it !in setOf("tv", "sport", "serbia", "rs", "hd", "sd", "uhd", "fhd", "4k", "8k") }
    .sorted()
    .joinToString(" ")

private class BoundedGuideInputStream(input: InputStream) : FilterInputStream(input) {
    private var bytesRead = 0L
    override fun read(): Int = super.read().also { if (it >= 0) record(1) }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = super.read(buffer, offset, length).also { if (it > 0) record(it) }
    private fun record(count: Int) {
        bytesRead += count
        require(bytesRead <= MAX_GUIDE_BYTES) { "Guide exceeds the 96 MiB unpacked limit" }
    }
}

private fun parseXmltvTime(value: String?): java.time.Instant? = runCatching {
    val text = value?.trim()?.takeIf { it.length >= 14 } ?: return null
    if (text.length > 14 && text.substring(14).isNotBlank()) {
        java.time.OffsetDateTime.parse(text, java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z")).toInstant()
    } else {
        java.time.LocalDateTime.parse(text.take(14), java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            .atZone(java.time.ZoneId.systemDefault()).toInstant()
    }
}.getOrNull()
