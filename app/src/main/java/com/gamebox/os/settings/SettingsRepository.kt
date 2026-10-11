package com.gamebox.os.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.CatalogProviderConfig
import com.gamebox.os.catalog.CatalogTransport
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.decodeGameSourceConfigs
import com.gamebox.os.source.defaultVimmLairGameSource
import com.gamebox.os.source.encodeGameSourceConfigs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.gameBoxDataStore: DataStore<Preferences> by preferencesDataStore(name = "gamebox_settings")

data class GameBoxSettings(
    val safeAreaPercent: Float = 0f,
    val reducedMotion: Boolean = false,
    val uiSoundEffectsEnabled: Boolean = true,
    val focusDebugOverlayEnabled: Boolean = false,
    val developerLayoutMode: DeveloperLayoutMode = DeveloperLayoutMode.AUTO,
    val catalogFailureSimulation: CatalogFailureSimulation = CatalogFailureSimulation.LIVE,
    val activeProfileName: String = "Local player",
    val showUnavailableGames: Boolean = true,
    val showUnavailableShortcuts: Boolean = true,
    val downloadsUnmeteredOnly: Boolean = true,
    val catalogSeededAtEpochMs: Long? = null,
    val catalogRefreshedAtEpochMs: Long? = null,
    val catalogUrl: String = "",
    val catalogTransport: String = "HTTPS",
    val catalogBucket: String = "",
    val catalogPrefix: String = "",
    val catalogRegion: String = "us-east-1",
    val gameSources: List<GameSourceConfig> = emptyList(),
    val externalLibraryUri: String = "",
    val emulatorSaveRootUri: String = "",
    val cloudSaveProvider: String = "WEBDAV",
    val cloudSaveEndpoint: String = "",
    val cloudSaveRegion: String = "us-east-1",
    val companionEnabled: Boolean = false,
    val companionPort: Int = 49_500,
    val moonlightHost: String = "",
    val moonlightPort: Int = 47_984,
    val platformEmulatorDefaults: Map<String, String> = emptyMap(),
    val recentShortcutLaunches: List<ShortcutLaunchRecord> = emptyList(),
    val hiddenShortcutPackages: Set<String> = emptySet(),
)

class SettingsRepository(private val context: Context) {
    private val credentialStore = SettingsCredentialStore(context)
    val settings: Flow<GameBoxSettings> = context.gameBoxDataStore.data.map(Preferences::toGameBoxSettings)

    suspend fun cloudSaveCredentials(provider: String): CatalogCredentials? =
        credentialStore.cloudSaveCredentials(provider)

    suspend fun hasCloudSaveCredentials(provider: String): Boolean =
        credentialStore.cloudSaveCredentials(provider) != null

    suspend fun setCloudSaveCredentials(provider: String, identity: String?, secret: String?) =
        credentialStore.setCloudSaveCredentials(provider, identity, secret)

    suspend fun clearCloudSaveCredentials() = credentialStore.clearCloudSaveCredentials()

    suspend fun setCloudSaveConfiguration(provider: String, endpoint: String, region: String) {
        require(provider.uppercase() in setOf("WEBDAV", "S3")) { "Unsupported cloud save provider" }
        context.gameBoxDataStore.edit { preferences ->
            preferences[CLOUD_SAVE_PROVIDER] = provider.uppercase()
            preferences[CLOUD_SAVE_ENDPOINT] = endpoint.trim()
            preferences[CLOUD_SAVE_REGION] = region.trim().ifEmpty { "us-east-1" }
        }
    }

    suspend fun companionPairingSecret(): String? = credentialStore.companionPairingSecret()

    suspend fun rotateCompanionPairingSecret(): String = credentialStore.rotateCompanionPairingSecret()

    suspend fun clearCompanionPairingSecret() = credentialStore.clearCompanionPairingSecret()

    suspend fun setCompanionConfiguration(enabled: Boolean, port: Int = 49_500) {
        require(port in 10_240..65_535) { "Companion port must be between 10240 and 65535" }
        context.gameBoxDataStore.edit { preferences ->
            preferences[COMPANION_ENABLED] = enabled
            preferences[COMPANION_PORT] = port
        }
    }

    suspend fun setMoonlightHost(host: String, port: Int) {
        val normalized = normalizeMoonlightHost(host)
        requireMoonlightPort(port)
        context.gameBoxDataStore.edit { preferences ->
            preferences[MOONLIGHT_HOST] = normalized
            preferences[MOONLIGHT_PORT] = port
        }
    }

    suspend fun clearMoonlightHost() {
        context.gameBoxDataStore.edit { preferences ->
            preferences.remove(MOONLIGHT_HOST)
            preferences.remove(MOONLIGHT_PORT)
        }
    }

    suspend fun platformEmulatorDefault(platform: String): String? =
        settings.first().platformEmulatorDefaults[normalizePlatformKey(platform)]

    suspend fun setPlatformEmulatorDefault(platform: String, packageName: String?) {
        val key = normalizePlatformKey(platform)
        require(key.isNotEmpty()) { "Platform is required" }
        context.gameBoxDataStore.edit { preferences ->
            val defaults = decodePlatformDefaults(preferences[PLATFORM_EMULATOR_DEFAULTS]).toMutableMap()
            if (packageName.isNullOrBlank()) defaults.remove(key)
            else defaults[key] = packageName.trim()
            if (defaults.isEmpty()) preferences.remove(PLATFORM_EMULATOR_DEFAULTS)
            else preferences[PLATFORM_EMULATOR_DEFAULTS] = encodePlatformDefaults(defaults)
        }
    }

    suspend fun setShortcutVisible(packageName: String, visible: Boolean) {
        context.gameBoxDataStore.edit { preferences ->
            val updated = updateShortcutVisibility(
                decodeHiddenShortcutPackages(preferences[HIDDEN_SHORTCUT_PACKAGES]),
                packageName,
                visible,
            )
            if (updated.isEmpty()) preferences.remove(HIDDEN_SHORTCUT_PACKAGES)
            else preferences[HIDDEN_SHORTCUT_PACKAGES] = encodeHiddenShortcutPackages(updated)
        }
    }

    suspend fun showAllShortcuts() {
        context.gameBoxDataStore.edit { it.remove(HIDDEN_SHORTCUT_PACKAGES) }
    }

    suspend fun recordShortcutLaunch(packageName: String, launchedAtEpochMs: Long = System.currentTimeMillis()) {
        context.gameBoxDataStore.edit { preferences ->
            val updated = addShortcutLaunch(
                decodeShortcutLaunchHistory(preferences[RECENT_SHORTCUT_LAUNCHES]),
                packageName,
                launchedAtEpochMs,
            )
            preferences[RECENT_SHORTCUT_LAUNCHES] = encodeShortcutLaunchHistory(updated)
        }
    }

    suspend fun clearShortcutLaunchHistory() {
        context.gameBoxDataStore.edit { it.remove(RECENT_SHORTCUT_LAUNCHES) }
    }

    suspend fun setExternalLibraryUri(value: String) {
        context.gameBoxDataStore.edit { preferences ->
            if (value.isBlank()) preferences.remove(EXTERNAL_LIBRARY_URI)
            else preferences[EXTERNAL_LIBRARY_URI] = value
        }
    }

    suspend fun setEmulatorSaveRootUri(value: String) {
        context.gameBoxDataStore.edit { preferences ->
            if (value.isBlank()) preferences.remove(EMULATOR_SAVE_ROOT_URI)
            else preferences[EMULATOR_SAVE_ROOT_URI] = value
        }
    }

    suspend fun catalogUrl(): String = settings.first().catalogUrl

    suspend fun catalogTransport(): String = settings.first().catalogTransport.uppercase()

    suspend fun catalogConfigured(): Boolean = settings.first().catalogUrl.isNotBlank()

    suspend fun catalogProviderConfig(): CatalogProviderConfig {
        val current = settings.first()
        val transport = when (current.catalogTransport.uppercase()) {
            "WEBDAV" -> CatalogTransport.WebDav(current.catalogUrl)
            "S3" -> CatalogTransport.S3(
                endpoint = current.catalogUrl,
                bucket = current.catalogBucket,
                prefix = current.catalogPrefix,
                region = current.catalogRegion,
            )
            else -> CatalogTransport.Https(current.catalogUrl)
        }
        val configuredCredentialKey = credentialStore.catalogCredentialKey(current.catalogTransport)
        val credentialKey = configuredCredentialKey.takeIf {
            current.catalogTransport.equals("S3", ignoreCase = true) ||
                credentialStore.catalogCredentials(it) != null
        }
        return CatalogProviderConfig(transport, credentialKey)
    }

    fun catalogCredentials(key: String): CatalogCredentials? = credentialStore.catalogCredentials(key)

    suspend fun hasCatalogCredentials(transport: String): Boolean =
        credentialStore.hasCatalogCredentials(transport)

    suspend fun setCatalogCredentials(transport: String, identity: String?, secret: String?) =
        credentialStore.setCatalogCredentials(transport, identity, secret)

    suspend fun clearCatalogCredentials() = credentialStore.clearCatalogCredentials()

    suspend fun setCatalogConfiguration(
        transport: String,
        endpoint: String,
        bucket: String = "",
        prefix: String = "",
        region: String = "us-east-1",
    ) {
        val normalizedTransport = transport.uppercase()
        require(normalizedTransport in setOf("HTTPS", "WEBDAV", "S3")) { "Unsupported catalog transport" }
        context.gameBoxDataStore.edit { preferences ->
            preferences[CATALOG_TRANSPORT] = normalizedTransport
            preferences[CATALOG_URL] = endpoint.trim()
            preferences[CATALOG_BUCKET] = bucket.trim()
            preferences[CATALOG_PREFIX] = prefix.trim().trim('/')
            preferences[CATALOG_REGION] = region.trim().ifEmpty { "us-east-1" }
        }
    }

    suspend fun setCatalogUrl(value: String) {
        setCatalogConfiguration("HTTPS", value)
    }

    suspend fun setGameSources(sources: List<GameSourceConfig>) {
        val encoded = encodeGameSourceConfigs(sources)
        context.gameBoxDataStore.edit { preferences ->
            preferences[GAME_SOURCES] = encoded
        }
    }

    suspend fun upsertGameSource(source: GameSourceConfig) {
        updateGameSources { current ->
            current.filterNot { it.id.equals(source.id, ignoreCase = true) } + source
        }
    }

    suspend fun removeGameSource(id: String) {
        updateGameSources { current -> current.filterNot { it.id.equals(id, ignoreCase = true) } }
    }

    private suspend fun updateGameSources(
        transform: (List<GameSourceConfig>) -> List<GameSourceConfig>,
    ) {
        context.gameBoxDataStore.edit { preferences ->
            val current = preferences[GAME_SOURCES]?.let(::decodeGameSourceConfigs)
                ?: listOf(defaultVimmLairGameSource())
            val updated = transform(current)
            preferences[GAME_SOURCES] = encodeGameSourceConfigs(updated)
        }
    }

    suspend fun setSafeAreaPercent(value: Float) {
        context.gameBoxDataStore.edit { it[SAFE_AREA] = value.coerceIn(0f, 0.1f) }
    }

    suspend fun setCompanionManagedPreferences(
        reducedMotion: Boolean,
        showUnavailableGames: Boolean,
        showUnavailableShortcuts: Boolean,
        downloadsUnmeteredOnly: Boolean,
    ) {
        context.gameBoxDataStore.edit { preferences ->
            preferences[REDUCED_MOTION] = reducedMotion
            preferences[SHOW_UNAVAILABLE] = showUnavailableGames
            preferences[SHOW_UNAVAILABLE_SHORTCUTS] = showUnavailableShortcuts
            preferences[DOWNLOADS_UNMETERED_ONLY] = downloadsUnmeteredOnly
        }
    }

    suspend fun setReducedMotion(value: Boolean) {
        context.gameBoxDataStore.edit { it[REDUCED_MOTION] = value }
    }

    suspend fun setUiSoundEffectsEnabled(value: Boolean) {
        context.gameBoxDataStore.edit { it[UI_SOUND_EFFECTS] = value }
    }

    suspend fun setFocusDebugOverlayEnabled(value: Boolean) {
        context.gameBoxDataStore.edit { it[FOCUS_DEBUG_OVERLAY] = value }
    }

    suspend fun setDeveloperLayoutMode(value: DeveloperLayoutMode) {
        context.gameBoxDataStore.edit { it[DEVELOPER_LAYOUT_MODE] = value.name }
    }

    suspend fun setCatalogFailureSimulation(value: CatalogFailureSimulation) {
        context.gameBoxDataStore.edit { it[CATALOG_FAILURE_SIMULATION] = value.name }
    }

    suspend fun setActiveProfileName(value: String) {
        require(value in setOf("Local player", "Guest")) { "Unsupported local profile" }
        context.gameBoxDataStore.edit { it[ACTIVE_PROFILE] = value }
    }

    suspend fun setShowUnavailableGames(value: Boolean) {
        context.gameBoxDataStore.edit { it[SHOW_UNAVAILABLE] = value }
    }

    suspend fun setShowUnavailableShortcuts(value: Boolean) {
        context.gameBoxDataStore.edit { it[SHOW_UNAVAILABLE_SHORTCUTS] = value }
    }

    suspend fun setDownloadsUnmeteredOnly(value: Boolean) {
        context.gameBoxDataStore.edit { it[DOWNLOADS_UNMETERED_ONLY] = value }
    }

    suspend fun markCatalogRefreshed(epochMs: Long) {
        context.gameBoxDataStore.edit { it[CATALOG_REFRESHED_AT] = epochMs }
    }

    suspend fun markCatalogSeeded(epochMs: Long) {
        context.gameBoxDataStore.edit { it[CATALOG_SEEDED_AT] = epochMs }
    }

}
