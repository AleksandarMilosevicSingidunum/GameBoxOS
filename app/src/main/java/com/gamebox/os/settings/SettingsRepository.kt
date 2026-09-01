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

private val Context.gameBoxDataStore: DataStore<Preferences> by preferencesDataStore(name = "gamebox_settings")

data class GameBoxSettings(
    val safeAreaPercent: Float = 0.04f,
    val showUnavailableGames: Boolean = true,
    val showUnavailableShortcuts: Boolean = true,
    val catalogSeededAtEpochMs: Long? = null,
    val catalogRefreshedAtEpochMs: Long? = null,
    val catalogUrl: String = "",
    val externalLibraryUri: String = "",
    val cloudSaveProvider: String = "WEBDAV",
    val cloudSaveEndpoint: String = "",
    val cloudSaveRegion: String = "us-east-1",
)

class SettingsRepository(private val context: Context) {
    private val secretStore = AndroidKeystoreSecretStore(context.applicationContext)
    val settings: Flow<GameBoxSettings> = context.gameBoxDataStore.data.map { preferences ->
        GameBoxSettings(
            safeAreaPercent = preferences[SAFE_AREA] ?: 0.04f,
            showUnavailableGames = preferences[SHOW_UNAVAILABLE] ?: true,
            showUnavailableShortcuts = preferences[SHOW_UNAVAILABLE_SHORTCUTS] ?: true,
            catalogSeededAtEpochMs = preferences[CATALOG_SEEDED_AT],
            catalogRefreshedAtEpochMs = preferences[CATALOG_REFRESHED_AT],
            catalogUrl = preferences[CATALOG_URL] ?: "",
            externalLibraryUri = preferences[EXTERNAL_LIBRARY_URI] ?: "",
            cloudSaveProvider = preferences[CLOUD_SAVE_PROVIDER] ?: "WEBDAV",
            cloudSaveEndpoint = preferences[CLOUD_SAVE_ENDPOINT] ?: "",
            cloudSaveRegion = preferences[CLOUD_SAVE_REGION] ?: "us-east-1",
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

    suspend fun setExternalLibraryUri(value: String) {
        context.gameBoxDataStore.edit { preferences ->
            if (value.isBlank()) preferences.remove(EXTERNAL_LIBRARY_URI)
            else preferences[EXTERNAL_LIBRARY_URI] = value
        }
    }

    suspend fun catalogUrl(): String = settings.first().catalogUrl

    suspend fun setCatalogUrl(value: String) {
        context.gameBoxDataStore.edit { it[CATALOG_URL] = value.trim() }
    }

    suspend fun setSafeAreaPercent(value: Float) {
        context.gameBoxDataStore.edit { it[SAFE_AREA] = value.coerceIn(0f, 0.1f) }
    }

    suspend fun setShowUnavailableGames(value: Boolean) {
        context.gameBoxDataStore.edit { it[SHOW_UNAVAILABLE] = value }
    }

    suspend fun setShowUnavailableShortcuts(value: Boolean) {
        context.gameBoxDataStore.edit { it[SHOW_UNAVAILABLE_SHORTCUTS] = value }
    }

    suspend fun markCatalogRefreshed(epochMs: Long) {
        context.gameBoxDataStore.edit { it[CATALOG_REFRESHED_AT] = epochMs }
    }

    suspend fun markCatalogSeeded(epochMs: Long) {
        context.gameBoxDataStore.edit { it[CATALOG_SEEDED_AT] = epochMs }
    }

    private companion object {
        val SAFE_AREA = floatPreferencesKey("safe_area_percent")
        val SHOW_UNAVAILABLE = booleanPreferencesKey("show_unavailable_games")
        val SHOW_UNAVAILABLE_SHORTCUTS = booleanPreferencesKey("show_unavailable_shortcuts")
        val CATALOG_SEEDED_AT = longPreferencesKey("catalog_seeded_at_epoch_ms")
        val CATALOG_REFRESHED_AT = longPreferencesKey("catalog_refreshed_at_epoch_ms")
        val CATALOG_URL = stringPreferencesKey("catalog_url")
        val EXTERNAL_LIBRARY_URI = stringPreferencesKey("external_library_uri")
        val CLOUD_SAVE_PROVIDER = stringPreferencesKey("cloud_save_provider")
        val CLOUD_SAVE_ENDPOINT = stringPreferencesKey("cloud_save_endpoint")
        val CLOUD_SAVE_REGION = stringPreferencesKey("cloud_save_region")
        const val THEGAMESDB_API_KEY = "thegamesdb_api_key"
        const val CLOUD_SAVE_USERNAME = "cloud_save_username"
        const val CLOUD_SAVE_PASSWORD = "cloud_save_password"
        const val CLOUD_SAVE_ACCESS_KEY = "cloud_save_access_key"
        const val CLOUD_SAVE_SECRET_KEY = "cloud_save_secret_key"
    }
}
