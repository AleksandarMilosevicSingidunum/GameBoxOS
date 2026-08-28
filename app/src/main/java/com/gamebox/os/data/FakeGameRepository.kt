package com.gamebox.os.data

import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface GameRepository {
    fun observeGames(): StateFlow<List<Game>>
    fun game(id: GameId): Game?
    fun advanceInstall(id: GameId)
    fun pauseOrResume(id: GameId)
    fun cancelInstall(id: GameId)
}

class FakeGameRepository : GameRepository {
    private val games = MutableStateFlow(fixtures)

    override fun observeGames(): StateFlow<List<Game>> = games.asStateFlow()
    override fun game(id: GameId): Game? = games.value.firstOrNull { it.id == id }

    override fun advanceInstall(id: GameId) {
        update(id) { game ->
            game.copy(state = when (game.state) {
                InstallState.NOT_INSTALLED, InstallState.FAILED, InstallState.MISSING_FILES -> InstallState.QUEUED
                InstallState.QUEUED -> InstallState.DOWNLOADING
                InstallState.DOWNLOADING, InstallState.PAUSED -> InstallState.VERIFYING
                InstallState.VERIFYING -> InstallState.INSTALLING
                InstallState.INSTALLING -> InstallState.INSTALLED
                else -> game.state
            })
        }
    }

    override fun pauseOrResume(id: GameId) {
        update(id) { game ->
            when (game.state) {
                InstallState.DOWNLOADING -> game.copy(state = InstallState.PAUSED)
                InstallState.PAUSED -> game.copy(state = InstallState.DOWNLOADING)
                else -> game
            }
        }
    }

    override fun cancelInstall(id: GameId) {
        update(id) { it.copy(state = InstallState.NOT_INSTALLED) }
    }

    private fun update(id: GameId, transform: (Game) -> Game) {
        games.value = games.value.map { if (it.id == id) transform(it) else it }
    }

    companion object {
        val fixtures = listOf(
            Game(GameId("celeste"), "Celeste Classic", "Homebrew", 2015, "Platformer", 32, InstallState.INSTALLED, "Today", 180),
            Game(GameId("cave-story"), "Cave Story", "Retro", 2004, "Adventure", 18, InstallState.INSTALLED, "Yesterday", 95),
            Game(GameId("openarena"), "OpenArena", "Android", 2012, "Arena FPS", 480, InstallState.INSTALLED),
            Game(GameId("supertuxkart"), "SuperTuxKart", "Android", 2025, "Racing", 820, InstallState.UPDATE_AVAILABLE),
            Game(GameId("freedoom"), "Freedoom", "Homebrew", 2024, "FPS", 45, InstallState.NOT_INSTALLED),
            Game(GameId("luanti"), "Luanti", "Android", 2026, "Sandbox", 210, InstallState.QUEUED),
            Game(GameId("openmw"), "OpenMW", "Source Port", 2026, "RPG", 155, InstallState.PAUSED),
            Game(GameId("retro-test"), "Retro Test Suite", "Retro", 2026, "Test", 8, InstallState.VERIFYING)
        )
    }
}
