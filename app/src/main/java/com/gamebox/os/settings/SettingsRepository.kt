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

private val Context.gameBoxDataStore: DataStore<Preferences> by preferencesDataStore(name = "gamebox_settings")

data class GameBoxSettings(
    val safeAreaPercent: Float = 0.04f,
    val showUnavailableGames: Boolean = true,
    val showUnavailableShortcuts: Boolean = true,
    val catalogSeededAtEpochMs: Long? = null,
    val catalogRefreshedAtEpochMs: Long? = null,
    val catalogUrl: String = "",
    val externalLibraryUri: String = ""
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
            externalLibraryUri = preferences[EXTERNAL_LIBRARY_URI] ?: ""
        )
    }

    suspend fun theGamesDbApiKey(): String? = secretStore.get(THEGAMESDB_API_KEY)

    suspend fun hasTheGamesDbApiKey(): Boolean = secretStore.contains(THEGAMESDB_API_KEY)

    suspend fun setTheGamesDbApiKey(value: String?) {
        secretStore.put(THEGAMESDB_API_KEY, value)
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
        const val THEGAMESDB_API_KEY = "thegamesdb_api_key"
    }
}
