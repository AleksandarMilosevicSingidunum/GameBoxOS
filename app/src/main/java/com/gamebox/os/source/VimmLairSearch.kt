package com.gamebox.os.source

import com.gamebox.os.domain.normalizeCatalogTitle
import kotlinx.coroutines.CancellationException
import java.net.URI

data class VimmLairProbeResult(
    val platform: String,
    val url: String,
    val vaultLinks: Int,
)

internal fun vimmLairBrowseUrl(
    baseUrl: String,
    platform: String,
    query: String,
): String {
    val slug = vimmLairPlatformSlug(platform)
        ?: return validateVimmLairUrl(baseUrl)
    val bucket = vimmLairLetterBucket(query)
    return vimmLairPlatformUrl(baseUrl, slug, bucket)
}

data class VimmLairSearchSummary(
    val games: List<DiscoverySourceGame>,
    val attemptedPlatforms: List<String>,
    val failedPlatforms: List<String>,
    val failureReasons: Map<String, String> = emptyMap(),
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
    val failureReasons = linkedMapOf<String, String>()
    platforms.forEach { platform ->
        try {
            games += source.search(platform = platform, query = query).games
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            failed += platform
            failureReasons[platform] = vimmLairFailureReason(error)
        }
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
        failureReasons = failureReasons,
    )
}

private fun vimmLairFailureReason(error: Exception): String {
    val message = error.message.orEmpty()
    val httpStatus = Regex("HTTP (\\d{3})", RegexOption.IGNORE_CASE)
        .find(message)?.groupValues?.getOrNull(1)
    return when {
        httpStatus != null -> "HTTP $httpStatus"
        error is VimmLairPageNotFoundException -> "listing unavailable"
        error is java.net.SocketTimeoutException -> "timed out"
        error is java.io.IOException -> "network error"
        message.contains("redirect", ignoreCase = true) -> "redirect rejected"
        else -> "invalid response"
    }
}

/** Platforms with an explicit Vimm vault slug mapping, available to configured searches. */
internal val vimmLairSupportedPlatforms = listOf(
    "Atari 2600", "Atari 5200", "Atari 7800", "CD-i", "Dreamcast",
    "Game Boy", "Game Boy Advance", "Game Boy Color", "Game Gear", "GameCube",
    "Genesis", "Jaguar", "Jaguar CD", "Lynx", "Master System", "N64",
    "NES", "PS1", "PS2", "PS3", "PSP", "Saturn", "Sega 32X", "Sega CD",
    "SNES", "TurboGrafx-16", "TurboGrafx-CD", "Virtual Boy", "Wii", "WiiWare",
    "Xbox", "Xbox 360", "DS",
)

/** Requested discovery preset. 3DS and Switch remain visible but have no verified Vimm vault mapping. */
internal val vimmLairDefaultPresetPlatforms = listOf(
    "Dreamcast", "Game Boy Advance", "Game Boy Color", "GameCube", "N64", "NES",
    "PS1", "PS2", "PS3", "PSP", "SNES", "Wii", "WiiWare", "Xbox",
    "Xbox 360", "DS", "3DS", "Switch",
)

internal fun defaultVimmLairGameSource(): GameSourceConfig = GameSourceConfig(
    id = "vimms-lair",
    name = "Vimm's Lair",
    type = GameSourceProviderType.VIMM_LAIR,
    baseUrl = "https://vimm.net/vault",
    platforms = vimmLairDefaultPresetPlatforms.toSet(),
)

internal fun vimmLairSearchPlatforms(
    config: GameSourceConfig,
    selectedPlatform: String?,
    maxPlatforms: Int = vimmLairSupportedPlatforms.size,
): List<String> {
    require(maxPlatforms in 1..64) { "Vimm search platform limit must be between 1 and 64" }
    selectedPlatform?.trim()?.takeIf(String::isNotEmpty)?.let { selected ->
        return if (config.supportsPlatform(selected) && vimmLairPlatformSlug(selected) != null) {
            listOf(selected)
        } else {
            emptyList()
        }
    }

    val candidates = if (config.platforms.isNotEmpty()) {
        config.platforms.toList()
    } else vimmLairSupportedPlatforms
    return candidates.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .filter { vimmLairPlatformSlug(it) != null }
        .distinctBy { normalizeCatalogTitle(it) }
        .sortedBy { normalizeCatalogTitle(it) }
        .take(maxPlatforms)
        .toList()
}

internal fun vimmLairPlatformSlug(platform: String): String? =
    when (normalizeCatalogTitle(platform)) {
        "ps2", "playstation2", "sonyplaystation2" -> "PS2"
        "ps3", "playstation3", "sonyplaystation3" -> "PS3"
        "xbox360", "microsoftxbox360" -> "Xbox360"
        "atari2600" -> "Atari2600"
        "atari5200" -> "Atari5200"
        "atari7800" -> "Atari7800"
        "cdi", "philipscd-i", "philipscdi" -> "CDi"
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
        "gamegear", "segagamegear" -> "GameGear"
        "mastersystem", "segamastersystem" -> "MasterSystem"
        "virtualboy", "nintendovirtualboy" -> "VirtualBoy"
        "lynx", "atarilynx" -> "Lynx"
        "jaguar", "atarijaguar" -> "Jaguar"
        "jaguarcd", "atarijaguarcd" -> "JaguarCD"
        "segacd", "megacd" -> "SegaCD"
        "32x", "sega32x" -> "32X"
        "turbografx16", "necpcengine" -> "TG16"
        "turbografxcd", "necpcenginecd" -> "TGCD"
        "wiiware", "nintendowiiware" -> "WiiWare"
        else -> null
    }

internal fun vimmLairMatchRank(title: String, query: String): Int {
    val normalizedTitle = normalizeCatalogTitle(title)
    val normalizedQuery = normalizeCatalogTitle(query)
    if (normalizedQuery.isBlank()) return 3
    return when {
        normalizedTitle == normalizedQuery -> 0
        normalizedTitle.startsWith(normalizedQuery) -> 1
        vimmSearchWords(title).any { titleWord -> normalizeCatalogTitle(titleWord) == normalizedQuery } -> 2
        else -> 3
    }
}

/** Google-like typo tolerance over titles from the selected Vimm letter index. */
internal fun vimmLairApproximateTitleMatch(title: String, query: String): Boolean {
    val queryWords = vimmSearchWords(query)
    val titleWords = vimmSearchWords(title)
    if (queryWords.isEmpty() || titleWords.isEmpty()) return false
    return queryWords.all { queryWord ->
        titleWords.any { titleWord ->
            titleWord.startsWith(queryWord) ||
                (queryWord.length >= 3 && titleWord.length >= 3 &&
                    vimmEditDistanceAtMost(queryWord, titleWord, if (queryWord.length >= 8) 2 else 1))
        }
    }
}

/** Suggests corrected query words only when a close spelling is present in returned titles. */
internal fun vimmLairCorrectedQuery(query: String, games: List<DiscoverySourceGame>): String? {
    val words = Regex("[\\p{L}\\p{N}]+").findAll(query).toList()
    val titleWords = games.flatMap { game ->
        Regex("[\\p{L}\\p{N}]+").findAll(game.title).map { it.value }.toList()
    }.distinctBy { it.lowercase() }
    val replacements = words.mapNotNull { match ->
        val typed = match.value
        if (typed.length < 3 || titleWords.any { it.equals(typed, ignoreCase = true) }) return@mapNotNull null
        val correction = titleWords.firstOrNull { candidate ->
            !candidate.startsWith(typed, ignoreCase = true) &&
                vimmEditDistanceAtMost(typed.lowercase(), candidate.lowercase(), 1)
        } ?: return@mapNotNull null
        match.range to correction
    }
    if (replacements.isEmpty()) return null
    val corrected = StringBuilder(query)
    replacements.asReversed().forEach { (range, replacement) ->
        corrected.replace(range.first, range.last + 1, replacement)
    }
    return corrected.toString().takeIf { !it.equals(query, ignoreCase = true) }
}

private fun vimmSearchWords(value: String) = value.lowercase()
    .split(Regex("[^\\p{L}\\p{N}]+"))
    .filter(String::isNotBlank)

private fun vimmEditDistanceAtMost(left: String, right: String, maximum: Int): Boolean {
    if (kotlin.math.abs(left.length - right.length) > maximum) return false
    if (left.length == right.length) {
        val mismatches = left.indices.filter { left[it] != right[it] }
        if (mismatches.size == 2 && mismatches[1] == mismatches[0] + 1 &&
            left[mismatches[0]] == right[mismatches[1]] &&
            left[mismatches[1]] == right[mismatches[0]]
        ) return true
    }
    var previous = IntArray(right.length + 1) { it }
    for (i in left.indices) {
        val current = IntArray(right.length + 1)
        current[0] = i + 1
        var rowMinimum = current[0]
        for (j in right.indices) {
            current[j + 1] = minOf(
                current[j] + 1,
                previous[j + 1] + 1,
                previous[j] + if (left[i] == right[j]) 0 else 1,
            )
            rowMinimum = minOf(rowMinimum, current[j + 1])
        }
        if (rowMinimum > maximum) return false
        previous = current
    }
    return previous.last() <= maximum
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
