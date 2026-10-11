package com.gamebox.os.ui

import com.gamebox.os.domain.normalizeCatalogTitle

private val homebrewStorePlatformAliases = setOf("homebrew", "retro", "android", "sourceport")

internal fun isHomebrewStorePlatform(platform: String): Boolean =
    normalizeCatalogTitle(platform) in homebrewStorePlatformAliases
