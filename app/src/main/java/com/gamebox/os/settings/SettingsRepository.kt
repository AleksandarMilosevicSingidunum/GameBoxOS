package com.gamebox.os.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameBoxDataStore: DataStore<Preferences> by preferencesDataStore(name = "gamebox_settings")

data class GameBoxSettings(
    val safeAreaPercent: Float = 0.04f,
    val showUnavailableGames: Boolean = true,
    val catalogSeededAtEpochMs: Long? = null
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<GameBoxSettings> = context.gameBoxDataStore.data.map { preferences ->
        GameBoxSettings(
            safeAreaPercent = preferences[SAFE_AREA] ?: 0.04f,
            showUnavailableGames = preferences[SHOW_UNAVAILABLE] ?: true,
            catalogSeededAtEpochMs = preferences[CATALOG_SEEDED_AT]
        )
    }

    suspend fun setSafeAreaPercent(value: Float) {
        context.gameBoxDataStore.edit { it[SAFE_AREA] = value.coerceIn(0f, 0.1f) }
    }

    suspend fun setShowUnavailableGames(value: Boolean) {
        context.gameBoxDataStore.edit { it[SHOW_UNAVAILABLE] = value }
    }

    suspend fun markCatalogSeeded(epochMs: Long) {
        context.gameBoxDataStore.edit { it[CATALOG_SEEDED_AT] = epochMs }
    }

    private companion object {
        val SAFE_AREA = floatPreferencesKey("safe_area_percent")
        val SHOW_UNAVAILABLE = booleanPreferencesKey("show_unavailable_games")
        val CATALOG_SEEDED_AT = longPreferencesKey("catalog_seeded_at_epoch_ms")
    }
}
