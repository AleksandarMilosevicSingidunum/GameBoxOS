package com.gamebox.os.settings

/** Stable preference codec for the per-platform emulator selection map. */
internal fun normalizePlatformKey(platform: String): String =
    platform.lowercase().filter(Char::isLetterOrDigit)

internal fun encodePlatformDefaults(defaults: Map<String, String>): String =
    defaults.entries.sortedBy { it.key }.joinToString("\n") { (platform, packageName) ->
        platform + "\t" + packageName
    }

internal fun decodePlatformDefaults(value: String?): Map<String, String> =
    value.orEmpty().lineSequence().mapNotNull { line ->
        val separator = line.indexOf('\t')
        if (separator <= 0 || separator == line.lastIndex) null
        else line.substring(0, separator) to line.substring(separator + 1)
    }.filter { (platform, packageName) ->
        platform.all(Char::isLetterOrDigit) &&
            packageName.all { it.isLetterOrDigit() || it == '.' || it == '_' }
    }.toMap()
