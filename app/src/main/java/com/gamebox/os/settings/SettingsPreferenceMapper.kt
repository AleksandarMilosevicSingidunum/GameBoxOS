package com.gamebox.os.settings

import androidx.datastore.preferences.core.Preferences
import com.gamebox.os.source.decodeGameSourceConfigs
import com.gamebox.os.source.defaultVimmLairGameSource

internal fun Preferences.toGameBoxSettings(): GameBoxSettings {
    return GameBoxSettings(
        safeAreaPercent = this[SAFE_AREA] ?: 0f,
        reducedMotion = this[REDUCED_MOTION] ?: false,
        uiSoundEffectsEnabled = this[UI_SOUND_EFFECTS] ?: true,
        focusDebugOverlayEnabled = this[FOCUS_DEBUG_OVERLAY] ?: false,
        developerLayoutMode = DeveloperLayoutMode.fromStored(this[DEVELOPER_LAYOUT_MODE]),
        catalogFailureSimulation = CatalogFailureSimulation.fromStored(this[CATALOG_FAILURE_SIMULATION]),
        activeProfileName = this[ACTIVE_PROFILE] ?: "Local player",
        showUnavailableGames = this[SHOW_UNAVAILABLE] ?: true,
        showUnavailableShortcuts = this[SHOW_UNAVAILABLE_SHORTCUTS] ?: true,
        downloadsUnmeteredOnly = this[DOWNLOADS_UNMETERED_ONLY] ?: true,
        catalogSeededAtEpochMs = this[CATALOG_SEEDED_AT],
        catalogRefreshedAtEpochMs = this[CATALOG_REFRESHED_AT],
        catalogUrl = this[CATALOG_URL] ?: "",
        catalogTransport = this[CATALOG_TRANSPORT] ?: "HTTPS",
        catalogBucket = this[CATALOG_BUCKET] ?: "",
        catalogPrefix = this[CATALOG_PREFIX] ?: "",
        catalogRegion = this[CATALOG_REGION] ?: "us-east-1",
        gameSources = this[GAME_SOURCES]?.let(::decodeGameSourceConfigs)
            ?: listOf(defaultVimmLairGameSource()),
        externalLibraryUri = this[EXTERNAL_LIBRARY_URI] ?: "",
        emulatorSaveRootUri = this[EMULATOR_SAVE_ROOT_URI] ?: "",
        cloudSaveProvider = this[CLOUD_SAVE_PROVIDER] ?: "WEBDAV",
        cloudSaveEndpoint = this[CLOUD_SAVE_ENDPOINT] ?: "",
        cloudSaveRegion = this[CLOUD_SAVE_REGION] ?: "us-east-1",
        companionEnabled = this[COMPANION_ENABLED] ?: false,
        companionPort = (this[COMPANION_PORT] ?: 49_500).coerceIn(10_240, 65_535),
        moonlightHost = this[MOONLIGHT_HOST] ?: "",
        moonlightPort = (this[MOONLIGHT_PORT] ?: 47_984).coerceIn(1, 65_535),
        platformEmulatorDefaults = decodePlatformDefaults(this[PLATFORM_EMULATOR_DEFAULTS]),
        recentShortcutLaunches = decodeShortcutLaunchHistory(this[RECENT_SHORTCUT_LAUNCHES]),
        hiddenShortcutPackages = decodeHiddenShortcutPackages(this[HIDDEN_SHORTCUT_PACKAGES]),
    )
}
