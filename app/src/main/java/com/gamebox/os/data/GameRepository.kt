package com.gamebox.os.data

import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.flow.StateFlow

interface GameRepository {
    fun observeGames(): StateFlow<List<Game>>
    fun game(id: GameId): Game?
    fun setFavorite(id: GameId, favorite: Boolean)
    fun setEmulatorSettings(id: GameId, packageName: String?, graphicsProfile: String)
    fun setInstallState(id: GameId, state: InstallState)
    fun recordPlaySession(id: GameId, endedAtMillis: Long, minutesPlayed: Int)
    fun observeCatalogRefreshState(): StateFlow<CatalogRefreshState>
    fun refreshCatalog()
    suspend fun registerImportedGame(imported: ImportedGameRegistration)
}
