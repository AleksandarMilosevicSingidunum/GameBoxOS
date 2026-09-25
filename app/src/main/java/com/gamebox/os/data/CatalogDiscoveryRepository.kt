package com.gamebox.os.data

import com.gamebox.os.catalog.CatalogSyncResult
import com.gamebox.os.catalog.TheGamesDbCatalogSync
import com.gamebox.os.data.local.CatalogDiscoveryDao
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.normalizeCatalogTitle
import com.gamebox.os.source.ConfiguredDiscoverySyncResult
import com.gamebox.os.source.GameBoxJsonDiscoverySync
import com.gamebox.os.source.GameSourceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DiscoveryGame(
    val id: GameId,
    val title: String,
    val platformId: String,
    val region: String? = null,
    val releaseDate: String?,
    val description: String?,
    val players: String?,
    val rating: Double?,
    val coverUrl: String?,
    val backgroundUrl: String?,
    val logoUrl: String?,
    val screenshots: List<String>,
    val favorite: Boolean,
)

data class DiscoveryPlatform(val id: String, val name: String, val gameCount: Int? = null)

interface CatalogDiscoveryRepository {
    fun observeGames(
        platformId: String? = null,
        query: String = "",
        limit: Int = 100,
        offset: Int = 0,
    ): Flow<List<DiscoveryGame>>

    /** Optional exact cached detail lookup; Room provides the production implementation. */
    fun observeGame(gameId: GameId): Flow<DiscoveryGame?> = kotlinx.coroutines.flow.flowOf(null)

    fun observePlatforms(): Flow<List<DiscoveryPlatform>>
    suspend fun syncPlatform(platformName: String): CatalogSyncResult

    suspend fun syncConfiguredSource(source: GameSourceConfig): ConfiguredDiscoverySyncResult =
        ConfiguredDiscoverySyncResult.Unsupported

    suspend fun setFavorite(gameId: GameId, favorite: Boolean)
}

class RoomCatalogDiscoveryRepository(
    private val dao: CatalogDiscoveryDao,
    private val sync: TheGamesDbCatalogSync,
    private val configuredSync: GameBoxJsonDiscoverySync = GameBoxJsonDiscoverySync(dao),
) : CatalogDiscoveryRepository {
    override fun observeGames(
        platformId: String?,
        query: String,
        limit: Int,
        offset: Int,
    ): Flow<List<DiscoveryGame>> {
        require(limit in 1..250) { "Discovery page size must be between 1 and 250" }
        require(offset >= 0) { "Discovery offset must not be negative" }
        return dao.observeGames(platformId, normalizeCatalogTitle(query), limit, offset).map { rows ->
            rows.map { it.toDiscoveryGame() }
        }
    }

    override fun observeGame(gameId: GameId): Flow<DiscoveryGame?> =
        dao.observeGame(gameId.value).map { it?.toDiscoveryGame() }

    override fun observePlatforms(): Flow<List<DiscoveryPlatform>> =
        dao.observePlatforms().map { rows -> rows.map { DiscoveryPlatform(it.id, it.name) } }

    override suspend fun syncPlatform(platformName: String): CatalogSyncResult =
        sync.syncPlatform(platformName)

    override suspend fun syncConfiguredSource(source: GameSourceConfig): ConfiguredDiscoverySyncResult =
        configuredSync.sync(source)

    override suspend fun setFavorite(gameId: GameId, favorite: Boolean) =
        dao.setFavorite(gameId.value, favorite)
}

private fun com.gamebox.os.data.local.CatalogGameEntity.toDiscoveryGame(): DiscoveryGame {
    val row = this
    return DiscoveryGame(
                    id = GameId(row.id),
                    title = row.title,
                    platformId = row.platformId,
                    region = row.region,
                    releaseDate = row.releaseDate,
                    description = row.description,
                    players = row.players,
                    rating = row.rating,
                    coverUrl = row.coverUrl,
                    backgroundUrl = row.backgroundUrl,
                    logoUrl = row.logoUrl,
                    screenshots = row.screenshotsJson.orEmpty()
                        .split('\n')
                        .map(String::trim)
                        .filter(String::isNotEmpty),
                    favorite = row.favorite,
                )
}
