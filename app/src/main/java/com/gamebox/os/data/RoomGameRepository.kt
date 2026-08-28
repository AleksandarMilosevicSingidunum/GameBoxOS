package com.gamebox.os.data

import com.gamebox.os.catalog.CatalogProvider
import com.gamebox.os.data.local.GameDao
import com.gamebox.os.data.local.toDomain
import com.gamebox.os.data.local.toEntity
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomGameRepository(
    private val dao: GameDao,
    private val catalogProvider: CatalogProvider,
    private val scope: CoroutineScope,
    private val onCatalogSeeded: suspend (Long) -> Unit,
    private val onCatalogRefreshed: suspend (Long) -> Unit
) : GameRepository {
    private val games: StateFlow<List<Game>> = dao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val refreshState = MutableStateFlow(CatalogRefreshState.IDLE)

    init {
        scope.launch {
            if (dao.count() == 0) {
                val snapshot = catalogProvider.load()
                dao.upsertAll(snapshot.games.map { it.toEntity() })
                onCatalogSeeded(System.currentTimeMillis())
            }
        }
    }

    override fun observeGames(): StateFlow<List<Game>> = games
    override fun game(id: GameId): Game? = games.value.firstOrNull { it.id == id }
    override fun observeCatalogRefreshState(): StateFlow<CatalogRefreshState> = refreshState.asStateFlow()

    override fun refreshCatalog() {
        if (refreshState.value == CatalogRefreshState.REFRESHING) return
        scope.launch {
            refreshState.value = CatalogRefreshState.REFRESHING
            try {
                val incoming = catalogProvider.load().games
                val existing = dao.getAllOnce().map { it.toDomain() }
                dao.upsertAll(mergeCatalogPreservingLocalState(existing, incoming).map { it.toEntity() })
                onCatalogRefreshed(System.currentTimeMillis())
                refreshState.value = CatalogRefreshState.SUCCESS
            } catch (_: Exception) {
                refreshState.value = CatalogRefreshState.ERROR
            }
        }
    }

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

    override fun cancelInstall(id: GameId) {
        scope.launch { dao.updateInstallState(id.value, InstallState.NOT_INSTALLED.name) }
    }
}

fun mergeCatalogPreservingLocalState(existing: List<Game>, incoming: List<Game>): List<Game> {
    val existingById = existing.associateBy { it.id }
    val incomingIds = incoming.mapTo(mutableSetOf()) { it.id }
    val mergedIncoming = incoming.map { remote ->
        val local = existingById[remote.id]
        if (local == null) remote else remote.copy(
            state = local.state,
            lastPlayed = local.lastPlayed,
            minutesPlayed = local.minutesPlayed
        )
    }
    return mergedIncoming + existing.filter { it.id !in incomingIds }
}
