package com.gamebox.os.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.gamebox.os.catalog.CatalogCredentials
import com.gamebox.os.catalog.CatalogProviderConfig
import com.gamebox.os.catalog.CatalogTransport
import com.gamebox.os.catalog.ProviderHealth
import com.gamebox.os.catalog.ProviderHealthStatus
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.decodeGameSourceConfigs
import com.gamebox.os.source.encodeGameSourceConfigs
import java.security.SecureRandom

private val Context.gameBoxDataStore: DataStore<Preferences> by preferencesDataStore(name = "gamebox_settings")

data class GameBoxSettings(
    val safeAreaPercent: Float = 0.04f,
    val reducedMotion: Boolean = false,
    val focusDebugOverlayEnabled: Boolean = false,
    val developerLayoutMode: DeveloperLayoutMode = DeveloperLayoutMode.AUTO,
    val catalogFailureSimulation: CatalogFailureSimulation = CatalogFailureSimulation.LIVE,
    val activeProfileName: String = "Local player",
    val showUnavailableGames: Boolean = true,
    val showUnavailableShortcuts: Boolean = true,
    val downloadsUnmeteredOnly: Boolean = true,
    val catalogSeededAtEpochMs: Long? = null,
    val catalogRefreshedAtEpochMs: Long? = null,
    val theGamesDbHealth: ProviderHealth = ProviderHealth(),
    val catalogUrl: String = "",
    val catalogTransport: String = "HTTPS",
    val catalogBucket: String = "",
    val catalogPrefix: String = "",
    val catalogRegion: String = "us-east-1",
    val gameSources: List<GameSourceConfig> = emptyList(),
    val externalLibraryUri: String = "",
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
    private val secretStore = AndroidKeystoreSecretStore(context.applicationContext)
    val settings: Flow<GameBoxSettings> = context.gameBoxDataStore.data.map { preferences ->
        GameBoxSettings(
            safeAreaPercent = preferences[SAFE_AREA] ?: 0.04f,
            reducedMotion = preferences[REDUCED_MOTION] ?: false,
            focusDebugOverlayEnabled = preferences[FOCUS_DEBUG_OVERLAY] ?: false,
            developerLayoutMode = DeveloperLayoutMode.fromStored(preferences[DEVELOPER_LAYOUT_MODE]),
            catalogFailureSimulation = CatalogFailureSimulation.fromStored(preferences[CATALOG_FAILURE_SIMULATION]),
            activeProfileName = preferences[ACTIVE_PROFILE] ?: "Local player",
            showUnavailableGames = preferences[SHOW_UNAVAILABLE] ?: true,
            showUnavailableShortcuts = preferences[SHOW_UNAVAILABLE_SHORTCUTS] ?: true,
            downloadsUnmeteredOnly = preferences[DOWNLOADS_UNMETERED_ONLY] ?: true,
            catalogSeededAtEpochMs = preferences[CATALOG_SEEDED_AT],
            catalogRefreshedAtEpochMs = preferences[CATALOG_REFRESHED_AT],
            theGamesDbHealth = ProviderHealth(
                status = runCatching {
                    ProviderHealthStatus.valueOf(
                        preferences[THEGAMESDB_HEALTH_STATUS] ?: ProviderHealthStatus.NOT_CONFIGURED.name
                    )
                }.getOrDefault(ProviderHealthStatus.NOT_CONFIGURED),
                lastAttemptAtMillis = preferences[THEGAMESDB_LAST_ATTEMPT],
                lastSuccessAtMillis = preferences[THEGAMESDB_LAST_SUCCESS],
                latencyMillis = preferences[THEGAMESDB_LATENCY],
                retryAfterMillis = preferences[THEGAMESDB_RETRY_AFTER],
                message = preferences[THEGAMESDB_HEALTH_MESSAGE],
            ),
            catalogUrl = preferences[CATALOG_URL] ?: "",
            catalogTransport = preferences[CATALOG_TRANSPORT] ?: "HTTPS",
            catalogBucket = preferences[CATALOG_BUCKET] ?: "",
            catalogPrefix = preferences[CATALOG_PREFIX] ?: "",
            catalogRegion = preferences[CATALOG_REGION] ?: "us-east-1",
            gameSources = decodeGameSourceConfigs(preferences[GAME_SOURCES]),
            externalLibraryUri = preferences[EXTERNAL_LIBRARY_URI] ?: "",
            cloudSaveProvider = preferences[CLOUD_SAVE_PROVIDER] ?: "WEBDAV",
            cloudSaveEndpoint = preferences[CLOUD_SAVE_ENDPOINT] ?: "",
            cloudSaveRegion = preferences[CLOUD_SAVE_REGION] ?: "us-east-1",
            companionEnabled = preferences[COMPANION_ENABLED] ?: false,
            companionPort = (preferences[COMPANION_PORT] ?: 49_500).coerceIn(10_240, 65_535),
            moonlightHost = preferences[MOONLIGHT_HOST] ?: "",
            moonlightPort = (preferences[MOONLIGHT_PORT] ?: 47_984).coerceIn(1, 65_535),
            platformEmulatorDefaults = decodePlatformDefaults(preferences[PLATFORM_EMULATOR_DEFAULTS]),
            recentShortcutLaunches = decodeShortcutLaunchHistory(preferences[RECENT_SHORTCUT_LAUNCHES]),
            hiddenShortcutPackages = decodeHiddenShortcutPackages(preferences[HIDDEN_SHORTCUT_PACKAGES]),
        )
    }

    suspend fun theGamesDbApiKey(): String? = withContext(Dispatchers.IO) {
        secretStore.get(THEGAMESDB_API_KEY)
    }

    suspend fun hasTheGamesDbApiKey(): Boolean = withContext(Dispatchers.IO) {
        secretStore.contains(THEGAMESDB_API_KEY)
    }

    suspend fun setTheGamesDbApiKey(value: String?) = withContext(Dispatchers.IO) {
        secretStore.put(THEGAMESDB_API_KEY, value)
    }

    suspend fun cloudSaveCredentials(provider: String): CatalogCredentials? = withContext(Dispatchers.IO) {
        when (provider.uppercase()) {
            "WEBDAV" -> CatalogCredentials(
                username = secretStore.get(CLOUD_SAVE_USERNAME),
                password = secretStore.get(CLOUD_SAVE_PASSWORD),
            ).takeIf(CatalogCredentials::hasBasicAuth)
            "S3" -> CatalogCredentials(
                accessKey = secretStore.get(CLOUD_SAVE_ACCESS_KEY),
                secretKey = secretStore.get(CLOUD_SAVE_SECRET_KEY),
            ).takeIf(CatalogCredentials::hasS3Auth)
            else -> null
        }
    }

    suspend fun hasCloudSaveCredentials(provider: String): Boolean =
        cloudSaveCredentials(provider) != null

    suspend fun setCloudSaveCredentials(provider: String, identity: String?, secret: String?) =
        withContext(Dispatchers.IO) {
            when (provider.uppercase()) {
                "WEBDAV" -> {
                    secretStore.put(CLOUD_SAVE_USERNAME, identity)
                    secretStore.put(CLOUD_SAVE_PASSWORD, secret)
                    secretStore.put(CLOUD_SAVE_ACCESS_KEY, null)
                    secretStore.put(CLOUD_SAVE_SECRET_KEY, null)
                }
                "S3" -> {
                    secretStore.put(CLOUD_SAVE_ACCESS_KEY, identity)
                    secretStore.put(CLOUD_SAVE_SECRET_KEY, secret)
                    secretStore.put(CLOUD_SAVE_USERNAME, null)
                    secretStore.put(CLOUD_SAVE_PASSWORD, null)
                }
                else -> require(false) { "Unsupported cloud save provider" }
            }
        }

    suspend fun clearCloudSaveCredentials() = withContext(Dispatchers.IO) {
        secretStore.put(CLOUD_SAVE_USERNAME, null)
        secretStore.put(CLOUD_SAVE_PASSWORD, null)
        secretStore.put(CLOUD_SAVE_ACCESS_KEY, null)
        secretStore.put(CLOUD_SAVE_SECRET_KEY, null)
    }

    suspend fun setCloudSaveConfiguration(provider: String, endpoint: String, region: String) {
        require(provider.uppercase() in setOf("WEBDAV", "S3")) { "Unsupported cloud save provider" }
        context.gameBoxDataStore.edit { preferences ->
            preferences[CLOUD_SAVE_PROVIDER] = provider.uppercase()
            preferences[CLOUD_SAVE_ENDPOINT] = endpoint.trim()
            preferences[CLOUD_SAVE_REGION] = region.trim().ifEmpty { "us-east-1" }
        }
    }

    suspend fun companionPairingSecret(): String? = withContext(Dispatchers.IO) {
        secretStore.get(COMPANION_PAIRING_SECRET)
    }

    suspend fun rotateCompanionPairingSecret(): String = withContext(Dispatchers.IO) {
        val secret = ByteArray(32).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        secretStore.put(COMPANION_PAIRING_SECRET, secret)
        secret
    }

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
        val credentialKey = when (current.catalogTransport.uppercase()) {
            "WEBDAV" -> CATALOG_WEBDAV_CREDENTIALS.takeIf { catalogCredentials(it) != null }
            "S3" -> CATALOG_S3_CREDENTIALS
            else -> CATALOG_HTTPS_CREDENTIALS.takeIf { catalogCredentials(it) != null }
        }
        return CatalogProviderConfig(transport, credentialKey)
    }

    fun catalogCredentials(key: String): CatalogCredentials? = when (key) {
        CATALOG_WEBDAV_CREDENTIALS, CATALOG_HTTPS_CREDENTIALS -> CatalogCredentials(
            username = secretStore.get(CATALOG_USERNAME),
            password = secretStore.get(CATALOG_PASSWORD),
        ).takeIf(CatalogCredentials::hasBasicAuth)
        CATALOG_S3_CREDENTIALS -> CatalogCredentials(
            accessKey = secretStore.get(CATALOG_ACCESS_KEY),
            secretKey = secretStore.get(CATALOG_SECRET_KEY),
        ).takeIf(CatalogCredentials::hasS3Auth)
        else -> null
    }

    suspend fun hasCatalogCredentials(transport: String): Boolean = withContext(Dispatchers.IO) {
        catalogCredentials(catalogCredentialKey(transport)) != null
    }

    suspend fun setCatalogCredentials(transport: String, identity: String?, secret: String?) =
        withContext(Dispatchers.IO) {
            when (transport.uppercase()) {
                "S3" -> {
                    secretStore.put(CATALOG_ACCESS_KEY, identity)
                    secretStore.put(CATALOG_SECRET_KEY, secret)
                    secretStore.put(CATALOG_USERNAME, null)
                    secretStore.put(CATALOG_PASSWORD, null)
                }
                "HTTPS", "WEBDAV" -> {
                    secretStore.put(CATALOG_USERNAME, identity)
                    secretStore.put(CATALOG_PASSWORD, secret)
                    secretStore.put(CATALOG_ACCESS_KEY, null)
                    secretStore.put(CATALOG_SECRET_KEY, null)
                }
                else -> require(false) { "Unsupported catalog transport" }
            }
        }

    suspend fun clearCatalogCredentials() = withContext(Dispatchers.IO) {
        secretStore.put(CATALOG_USERNAME, null)
        secretStore.put(CATALOG_PASSWORD, null)
        secretStore.put(CATALOG_ACCESS_KEY, null)
        secretStore.put(CATALOG_SECRET_KEY, null)
    }

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
            if (sources.isEmpty()) preferences.remove(GAME_SOURCES)
            else preferences[GAME_SOURCES] = encoded
        }
    }

    suspend fun upsertGameSource(source: GameSourceConfig) {
        val current = settings.first().gameSources
            .filterNot { it.id.equals(source.id, ignoreCase = true) }
        setGameSources(current + source)
    }

    suspend fun removeGameSource(id: String) {
        setGameSources(
            settings.first().gameSources.filterNot { it.id.equals(id, ignoreCase = true) }
        )
    }

    private fun catalogCredentialKey(transport: String): String = when (transport.uppercase()) {
        "S3" -> CATALOG_S3_CREDENTIALS
        "WEBDAV" -> CATALOG_WEBDAV_CREDENTIALS
        else -> CATALOG_HTTPS_CREDENTIALS
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

    suspend fun setTheGamesDbHealth(health: ProviderHealth) {
        context.gameBoxDataStore.edit { preferences ->
            preferences[THEGAMESDB_HEALTH_STATUS] = health.status.name
            health.lastAttemptAtMillis?.let { preferences[THEGAMESDB_LAST_ATTEMPT] = it }
                ?: preferences.remove(THEGAMESDB_LAST_ATTEMPT)
            // A failed refresh must not erase the last known successful contact.
            health.lastSuccessAtMillis?.let { preferences[THEGAMESDB_LAST_SUCCESS] = it }
            health.latencyMillis?.let { preferences[THEGAMESDB_LATENCY] = it }
                ?: preferences.remove(THEGAMESDB_LATENCY)
            health.retryAfterMillis?.let { preferences[THEGAMESDB_RETRY_AFTER] = it }
                ?: preferences.remove(THEGAMESDB_RETRY_AFTER)
            health.message?.take(200)?.let { preferences[THEGAMESDB_HEALTH_MESSAGE] = it }
                ?: preferences.remove(THEGAMESDB_HEALTH_MESSAGE)
        }
    }

    private companion object {
        val SAFE_AREA = floatPreferencesKey("safe_area_percent")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val FOCUS_DEBUG_OVERLAY = booleanPreferencesKey("focus_debug_overlay")
        val DEVELOPER_LAYOUT_MODE = stringPreferencesKey("developer_layout_mode")
        val CATALOG_FAILURE_SIMULATION = stringPreferencesKey("catalog_failure_simulation")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val SHOW_UNAVAILABLE = booleanPreferencesKey("show_unavailable_games")
        val SHOW_UNAVAILABLE_SHORTCUTS = booleanPreferencesKey("show_unavailable_shortcuts")
        val DOWNLOADS_UNMETERED_ONLY = booleanPreferencesKey("downloads_unmetered_only")
        val CATALOG_SEEDED_AT = longPreferencesKey("catalog_seeded_at_epoch_ms")
        val CATALOG_REFRESHED_AT = longPreferencesKey("catalog_refreshed_at_epoch_ms")
        val THEGAMESDB_HEALTH_STATUS = stringPreferencesKey("thegamesdb_health_status")
        val THEGAMESDB_LAST_ATTEMPT = longPreferencesKey("thegamesdb_last_attempt")
        val THEGAMESDB_LAST_SUCCESS = longPreferencesKey("thegamesdb_last_success")
        val THEGAMESDB_LATENCY = longPreferencesKey("thegamesdb_latency")
        val THEGAMESDB_RETRY_AFTER = longPreferencesKey("thegamesdb_retry_after")
        val THEGAMESDB_HEALTH_MESSAGE = stringPreferencesKey("thegamesdb_health_message")
        val CATALOG_URL = stringPreferencesKey("catalog_url")
        val CATALOG_TRANSPORT = stringPreferencesKey("catalog_transport")
        val CATALOG_BUCKET = stringPreferencesKey("catalog_bucket")
        val CATALOG_PREFIX = stringPreferencesKey("catalog_prefix")
        val CATALOG_REGION = stringPreferencesKey("catalog_region")
        val GAME_SOURCES = stringPreferencesKey("game_sources")
        val EXTERNAL_LIBRARY_URI = stringPreferencesKey("external_library_uri")
        val CLOUD_SAVE_PROVIDER = stringPreferencesKey("cloud_save_provider")
        val CLOUD_SAVE_ENDPOINT = stringPreferencesKey("cloud_save_endpoint")
        val CLOUD_SAVE_REGION = stringPreferencesKey("cloud_save_region")
        val COMPANION_ENABLED = booleanPreferencesKey("companion_enabled")
        val COMPANION_PORT = androidx.datastore.preferences.core.intPreferencesKey("companion_port")
        val MOONLIGHT_HOST = stringPreferencesKey("moonlight_host")
        val MOONLIGHT_PORT = androidx.datastore.preferences.core.intPreferencesKey("moonlight_port")
        val PLATFORM_EMULATOR_DEFAULTS = stringPreferencesKey("platform_emulator_defaults")
        val RECENT_SHORTCUT_LAUNCHES = stringPreferencesKey("recent_shortcut_launches")
        val HIDDEN_SHORTCUT_PACKAGES = stringPreferencesKey("hidden_shortcut_packages")
        const val THEGAMESDB_API_KEY = "thegamesdb_api_key"
        const val CATALOG_USERNAME = "catalog_username"
        const val CATALOG_PASSWORD = "catalog_password"
        const val CATALOG_ACCESS_KEY = "catalog_access_key"
        const val CATALOG_SECRET_KEY = "catalog_secret_key"
        const val CATALOG_HTTPS_CREDENTIALS = "catalog-https"
        const val CATALOG_WEBDAV_CREDENTIALS = "catalog-webdav"
        const val CATALOG_S3_CREDENTIALS = "catalog-s3"
        const val CLOUD_SAVE_USERNAME = "cloud_save_username"
        const val CLOUD_SAVE_PASSWORD = "cloud_save_password"
        const val CLOUD_SAVE_ACCESS_KEY = "cloud_save_access_key"
        const val CLOUD_SAVE_SECRET_KEY = "cloud_save_secret_key"
        const val COMPANION_PAIRING_SECRET = "companion_pairing_secret"
    }
}


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
