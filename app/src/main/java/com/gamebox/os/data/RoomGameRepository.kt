package com.gamebox.os.data

import com.gamebox.os.catalog.CatalogProvider
import com.gamebox.os.catalog.CatalogFallbackReason
import com.gamebox.os.catalog.CatalogFallbackStatus
import com.gamebox.os.data.local.GameDao
import com.gamebox.os.data.local.SaveRecordDao
import com.gamebox.os.data.local.SaveRecordEntity
import com.gamebox.os.data.local.toDomain
import com.gamebox.os.data.local.toEntity
import com.gamebox.os.domain.CatalogRefreshState
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.GraphicsProfiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomGameRepository(
    private val dao: GameDao,
    private val saveRecordDao: SaveRecordDao,
    private val catalogProvider: CatalogProvider,
    private val scope: CoroutineScope,
    private val onCatalogSeeded: suspend (Long) -> Unit,
    private val onCatalogRefreshed: suspend (Long) -> Unit
) : GameRepository {
    private val installStateWrites = Mutex()
    private val games: StateFlow<List<Game>> = combine(
        dao.observeAll(),
        saveRecordDao.observeAll()
    ) { entities, saveRecords ->
        enrichGamesWithSaveRecords(
            entities.map { it.toDomain() },
            saveRecords
        )
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val refreshState = MutableStateFlow(CatalogRefreshState.IDLE)

    init {
        scope.launch {
            // Repair only known legacy demo records. SQL guards protect imports and
            // real download sources, and no files, saves or user metadata are removed.
            dao.clearLegacyCatalogInstallClaims()
            if (dao.count() == 0) {
                val snapshot = catalogProvider.load()
                consumeFallbackReason()
                dao.upsertAll(snapshot.games.map { it.copy(state = InstallState.NOT_INSTALLED).toEntity() })
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
                val fallbackReason = consumeFallbackReason()
                val existing = dao.getAllOnce().map { it.toDomain() }
                dao.upsertAll(mergeCatalogPreservingLocalState(existing, incoming).map { it.toEntity() })
                onCatalogRefreshed(System.currentTimeMillis())
                refreshState.value = when (fallbackReason) {
                    CatalogFallbackReason.OFFLINE -> CatalogRefreshState.OFFLINE_FALLBACK
                    CatalogFallbackReason.REMOTE_FAILURE -> CatalogRefreshState.REMOTE_FALLBACK
                    CatalogFallbackReason.NONE -> CatalogRefreshState.SUCCESS
                }
            } catch (_: Exception) {
                refreshState.value = CatalogRefreshState.ERROR
            }
        }
    }

    override suspend fun registerImportedGame(imported: ImportedGameRegistration) {
        val existing = dao.getById(imported.id.value)?.toDomain()
        dao.upsert(mergeImportedGame(existing, imported).toEntity())
    }

    private fun consumeFallbackReason(): CatalogFallbackReason =
        (catalogProvider as? CatalogFallbackStatus)?.consumeFallbackReason()
            ?: CatalogFallbackReason.NONE

    override fun setEmulatorSettings(id: GameId, packageName: String?, graphicsProfile: String) {
        scope.launch { dao.updateEmulatorSettings(id.value, packageName, graphicsProfile.takeIf { it in GraphicsProfiles.ALL } ?: GraphicsProfiles.BALANCED) }
    }

    override fun setFavorite(id: GameId, favorite: Boolean) {
        scope.launch { dao.updateFavorite(id.value, favorite) }
    }

    override fun setInstallState(id: GameId, state: InstallState) {
        // Enter the queue before returning so a delayed QUEUED write cannot
        // overwrite the successful result of a small, fast installation.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            installStateWrites.withLock { dao.updateInstallState(id.value, state.name) }
        }
    }

    override fun recordPlaySession(id: GameId, endedAtMillis: Long, minutesPlayed: Int) {
        val lastPlayed = java.time.Instant.ofEpochMilli(endedAtMillis).toString()
        scope.launch {
            dao.recordPlaySession(id.value, lastPlayed, minutesPlayed.coerceAtLeast(0))
        }
    }

    override suspend fun setInstallStateAndAwait(id: GameId, state: InstallState) {
        installStateWrites.withLock { dao.updateInstallState(id.value, state.name) }
    }
}

fun enrichGamesWithSaveRecords(
    games: List<Game>,
    saveRecords: List<SaveRecordEntity>
): List<Game> {
    val savesByGameId = saveRecords.associateBy { it.gameId }
    return games.map { game ->
        val save = savesByGameId[game.id.value]
        game.copy(
            savePresent = save != null,
            saveSizeBytes = save?.sizeBytes?.coerceAtLeast(0L) ?: 0L
        )
    }
}

fun mergeCatalogPreservingLocalState(existing: List<Game>, incoming: List<Game>): List<Game> {
    val existingById = existing.associateBy { it.id }
    val incomingIds = incoming.mapTo(mutableSetOf()) { it.id }
    val mergedIncoming = incoming.map { remote ->
        val local = existingById[remote.id]
        if (local == null) remote.copy(state = InstallState.NOT_INSTALLED) else remote.copy(
            state = local.state,
            lastPlayed = local.lastPlayed,
            minutesPlayed = local.minutesPlayed,
            favorite = local.favorite,
            emulatorPackage = local.emulatorPackage,
            graphicsProfile = local.graphicsProfile,
            localContentRelativePath = local.localContentRelativePath,
            localContentSha256 = local.localContentSha256,
            localContentMimeType = local.localContentMimeType,
            localContentFiles = local.localContentFiles,
        )
    }
    val retainedLocal = existing
        .filter { it.id !in incomingIds }
        .map { local ->
            // The provider no longer advertises this title. Keep user-owned state,
            // metadata and verified local files, but never retain a vanished remote
            // acquisition endpoint. Retain the checksum because installed content
            // still needs it for offline integrity verification and launch.
            local.copy(sourceUrl = null)
        }
    return mergedIncoming + retainedLocal
}
