package com.gamebox.os.ui

import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType
import com.gamebox.os.source.supportsPlatform
import com.gamebox.os.source.vimmLairPlatformSlug

private val storeDiscoveryProviderTypes = setOf(
    GameSourceProviderType.EXTERNAL_WEB,
    GameSourceProviderType.GAMEBOX_JSON,
    GameSourceProviderType.VIMM_LAIR,
)

/**
 * Keeps Store source choices aligned with the selected console and providers
 * that can actually browse or search from the app.
 */
internal fun configuredSourcesForStore(
    sources: List<GameSourceConfig>,
    selectedConsole: StoreConsole?,
): List<GameSourceConfig> = sources.filter { source ->
    source.type in storeDiscoveryProviderTypes && source.supportsStoreConsole(selectedConsole) &&
        (source.type != GameSourceProviderType.VIMM_LAIR || selectedConsole == null ||
            vimmLairPlatformSlug(selectedConsole.label) != null)
}

private fun GameSourceConfig.supportsStoreConsole(console: StoreConsole?): Boolean =
    supportsPlatform(console?.label) ||
        (enabled && console != null && platforms.any { configured -> storeConsoleMatches(console, configured) })
