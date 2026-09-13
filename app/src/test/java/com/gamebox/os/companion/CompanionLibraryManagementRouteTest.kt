package com.gamebox.os.companion

import com.gamebox.os.data.GameRepository
import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.*
import org.junit.Test

class CompanionLibraryManagementRouteTest {
    private val secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private class FakeGames : GameRepository {
        private val item = Game(GameId("game-1"), "Game", "NES", 2026, "Homebrew", 1, InstallState.INSTALLED)
        private val items = MutableStateFlow(listOf(item))
        var favorite: Boolean? = null
        override fun observeGames(): StateFlow<List<Game>> = items
        override fun game(id: GameId): Game? = item.takeIf { it.id == id }
        override fun setFavorite(id: GameId, favorite: Boolean) { this.favorite = favorite }
        override fun setEmulatorSettings(id: GameId, packageName: String?, graphicsProfile: String) = Unit
        override fun setInstallState(id: GameId, state: InstallState) = Unit
        override fun recordPlaySession(id: GameId, endedAtMillis: Long, minutesPlayed: Int) = Unit
        override fun observeCatalogRefreshState(): StateFlow<CatalogRefreshState> =
            MutableStateFlow(CatalogRefreshState.IDLE)
        override fun refreshCatalog() = Unit
        override suspend fun registerImportedGame(imported: ImportedGameRegistration) = Unit
    }

    @Test fun authenticatedFavoriteUpdateIsIdempotentAndGameScoped() {
        val path = "/v1/library/game-1/favorite/on"
        val games = FakeGames()
        val response = CompanionLibraryManagementRoute.handle(
            CompanionHttpRequest(
                "PUT", path,
                CompanionProtocol.createAuthorization(secret, "PUT", path, 1_700_000_000),
            ),
            secret, games, 1_700_000_030,
        )
        assertEquals(200, response.status)
        assertEquals(true, games.favorite)
        assertTrue(response.body.contains("\"favorite\":true"))
    }

    @Test fun rejectsTamperingUnknownGamesAndUnsupportedActions() {
        val games = FakeGames()
        val path = "/v1/library/game-1/favorite/off"
        val authorization = CompanionProtocol.createAuthorization(secret, "PUT", path, 1_700_000_000)
        assertEquals(401, CompanionLibraryManagementRoute.handle(
            CompanionHttpRequest("PUT", path + "x", authorization),
            secret, games, 1_700_000_030,
        ).status)
        val missing = "/v1/library/missing/favorite/off"
        assertEquals(404, CompanionLibraryManagementRoute.handle(
            CompanionHttpRequest(
                "PUT", missing,
                CompanionProtocol.createAuthorization(secret, "PUT", missing, 1_700_000_000),
            ),
            secret, games, 1_700_000_030,
        ).status)
        assertEquals(404, CompanionLibraryManagementRoute.handle(
            CompanionHttpRequest("PUT", "/v1/library/../favorite/on", null),
            secret, games, 1_700_000_030,
        ).status)
    }
}
