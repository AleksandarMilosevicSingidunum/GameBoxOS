package com.gamebox.os.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDiscoveryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlatforms(platforms: List<CatalogPlatformEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGames(games: List<CatalogGameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExternalIds(ids: List<CatalogExternalIdEntity>)

    @Query(
        """
        SELECT * FROM catalog_games
        WHERE (:platformId IS NULL OR platformId = :platformId)
          AND (:normalizedQuery = '' OR normalizedTitle LIKE '%' || :normalizedQuery || '%')
        ORDER BY favorite DESC, title COLLATE NOCASE ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observeGames(
        platformId: String?,
        normalizedQuery: String,
        limit: Int,
        offset: Int,
    ): Flow<List<CatalogGameEntity>>

    @Query("SELECT * FROM catalog_platforms ORDER BY name COLLATE NOCASE ASC")
    fun observePlatforms(): Flow<List<CatalogPlatformEntity>>

    @Query("SELECT COUNT(*) FROM catalog_games WHERE platformId = :platformId")
    suspend fun countGames(platformId: String): Int

    @Query("UPDATE catalog_games SET favorite = :favorite WHERE id = :gameId")
    suspend fun setFavorite(gameId: String, favorite: Boolean)

    @Transaction
    suspend fun upsertPage(
        platform: CatalogPlatformEntity,
        games: List<CatalogGameEntity>,
        externalIds: List<CatalogExternalIdEntity>,
    ) {
        upsertPlatforms(listOf(platform))
        upsertGames(games)
        upsertExternalIds(externalIds)
    }
}
