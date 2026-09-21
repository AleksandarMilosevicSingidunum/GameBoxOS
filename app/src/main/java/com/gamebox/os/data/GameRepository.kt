package com.gamebox.os.data

import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.GameMetadataOverrides
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.flow.StateFlow

interface GameRepository {
    fun observeGames(): StateFlow<List<Game>>
    fun game(id: GameId): Game?
    fun setFavorite(id: GameId, favorite: Boolean)
    fun setEmulatorSettings(id: GameId, packageName: String?, graphicsProfile: String)
    suspend fun setMetadataOverrides(id: GameId, overrides: GameMetadataOverrides)
    fun setInstallState(id: GameId, state: InstallState)
    suspend fun setInstallStateAndAwait(id: GameId, state: InstallState) { setInstallState(id, state) }
    fun recordPlaySession(id: GameId, endedAtMillis: Long, minutesPlayed: Int)
    fun observeCatalogRefreshState(): StateFlow<CatalogRefreshState>
    fun refreshCatalog()
    suspend fun registerImportedGame(imported: ImportedGameRegistration)
    suspend fun forgetMissingImportedContent(id: GameId): Boolean = false
    suspend fun registerManagedSave(id: GameId, relativePath: String, updatedAtMillis: Long, sizeBytes: Long) {
        error("Managed save registration is unavailable")
    }
}
