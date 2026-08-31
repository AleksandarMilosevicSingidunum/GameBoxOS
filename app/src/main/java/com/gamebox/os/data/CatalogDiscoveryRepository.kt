package com.gamebox.os.data

import com.gamebox.os.catalog.CatalogSyncResult
import com.gamebox.os.catalog.TheGamesDbCatalogSync
import com.gamebox.os.data.local.CatalogDiscoveryDao
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.normalizeCatalogTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DiscoveryGame(
    val id: GameId,
    val title: String,
    val platformId: String,
    val releaseDate: String?,
    val description: String?,
    val players: String?,
    val rating: Double?,
    val coverUrl: String?,
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

    fun observePlatforms(): Flow<List<DiscoveryPlatform>>
    suspend fun syncPlatform(platformName: String): CatalogSyncResult
    suspend fun setFavorite(gameId: GameId, favorite: Boolean)
}

class RoomCatalogDiscoveryRepository(
    private val dao: CatalogDiscoveryDao,
    private val sync: TheGamesDbCatalogSync,
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
            rows.map { row ->
                DiscoveryGame(
                    id = GameId(row.id),
                    title = row.title,
                    platformId = row.platformId,
                    releaseDate = row.releaseDate,
                    description = row.description,
                    players = row.players,
                    rating = row.rating,
                    coverUrl = row.coverUrl,
                    favorite = row.favorite,
                )
            }
        }
    }

    override fun observePlatforms(): Flow<List<DiscoveryPlatform>> =
        dao.observePlatforms().map { rows -> rows.map { DiscoveryPlatform(it.id, it.name) } }

    override suspend fun syncPlatform(platformName: String): CatalogSyncResult =
        sync.syncPlatform(platformName)

    override suspend fun setFavorite(gameId: GameId, favorite: Boolean) =
        dao.setFavorite(gameId.value, favorite)
}
