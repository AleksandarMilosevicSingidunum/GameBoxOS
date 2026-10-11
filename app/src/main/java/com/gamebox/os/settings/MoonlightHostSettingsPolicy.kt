package com.gamebox.os.settings

/** Validates user-entered Moonlight host values before persistence or network use. */
internal fun normalizeMoonlightHost(value: String): String {
    val trimmed = value.trim()
    val bracketed = trimmed.startsWith('[') || trimmed.endsWith(']')
    require(!bracketed || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
        "IPv6 address brackets are incomplete"
    }
    val normalized = if (bracketed) trimmed.substring(1, trimmed.lastIndex) else trimmed
    require(normalized.isNotEmpty() && normalized.length <= 253 &&
        normalized.none(Char::isWhitespace) &&
        normalized.none { it == '/' || it == '\\' || it == '?' || it == '#' || it == '@' }) {
        "Enter a host name or IP address without a scheme or path"
    }
    return normalized
}

internal fun requireMoonlightPort(value: Int): Int {
    require(value in 1..65_535) { "Moonlight port must be between 1 and 65535" }
    return value
}
