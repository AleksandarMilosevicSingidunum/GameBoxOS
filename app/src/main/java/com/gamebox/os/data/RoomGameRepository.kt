package com.gamebox.os.data

import com.gamebox.os.data.local.GameDao
import com.gamebox.os.data.local.toDomain
import com.gamebox.os.data.local.toEntity
import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomGameRepository(
    private val dao: GameDao,
    private val scope: CoroutineScope
) : GameRepository {
    private val initialGames = FakeGameRepository.fixtures

    private val games: StateFlow<List<Game>> = dao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            if (dao.count() == 0) dao.upsertAll(initialGames.map { it.toEntity() })
        }
    }

    override fun observeGames(): StateFlow<List<Game>> = games
    override fun game(id: GameId): Game? = games.value.firstOrNull { it.id == id }

    override fun advanceInstall(id: GameId) {
        val game = game(id) ?: return
        val next = when (game.state) {
            InstallState.NOT_INSTALLED, InstallState.FAILED, InstallState.MISSING_FILES -> InstallState.QUEUED
            InstallState.QUEUED -> InstallState.DOWNLOADING
            InstallState.DOWNLOADING, InstallState.PAUSED -> InstallState.VERIFYING
            InstallState.VERIFYING -> InstallState.INSTALLING
            InstallState.INSTALLING -> InstallState.INSTALLED
            else -> game.state
        }
        scope.launch { dao.updateInstallState(id.value, next.name) }
    }

    override fun pauseOrResume(id: GameId) {
        val game = game(id) ?: return
        val next = when (game.state) {
            InstallState.DOWNLOADING -> InstallState.PAUSED
            InstallState.PAUSED -> InstallState.DOWNLOADING
            else -> game.state
        }
        scope.launch { dao.updateInstallState(id.value, next.name) }
    }

    override fun downloads(): List<DownloadJob> = games.value
        .filter { it.state in activeStates }
        .map { game ->
            DownloadJob(game.id, game.title, game.state, when (game.state) {
                InstallState.QUEUED -> 0f
                InstallState.DOWNLOADING, InstallState.PAUSED -> 0.42f
                InstallState.VERIFYING -> 0.9f
                InstallState.INSTALLING -> 0.96f
                else -> 1f
            })
        }

    private companion object {
        val activeStates = setOf(
            InstallState.QUEUED, InstallState.DOWNLOADING, InstallState.PAUSED,
            InstallState.VERIFYING, InstallState.INSTALLING
        )
    }
}
