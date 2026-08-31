package com.gamebox.os

import com.gamebox.os.catalog.CatalogSyncResult
import com.gamebox.os.catalog.TheGamesDbCatalogSync
import com.gamebox.os.catalog.TheGamesDbCatalogTransport
import com.gamebox.os.data.local.CatalogDiscoveryDao
import com.gamebox.os.data.local.CatalogExternalIdEntity
import com.gamebox.os.data.local.CatalogGameEntity
import com.gamebox.os.data.local.CatalogPlatformEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TheGamesDbCatalogSyncTest {
    @Test
    fun resolvesPlatformAndPersistsEveryPage() = runBlocking {
        val dao = RecordingCatalogDao()
        val transport = TheGamesDbCatalogTransport { uri ->
            when {
                uri.path.endsWith("/Platforms") ->
                    """{"data":{"platforms":[{"id":11,"name":"PlayStation 2"}]}}"""
                uri.rawQuery.contains("page=1") ->
                    """{"data":{"pages":{"current":1,"next":2},"games":[{"id":1,"game_title":"First"}]}}"""
                else ->
                    """{"data":{"pages":{"current":2},"games":[{"id":2,"game_title":"Second"}]}}"""
            }
        }
        val sync = TheGamesDbCatalogSync(
            apiKey = { "key" },
            transport = transport,
            dao = dao,
            nowMillis = { 123L },
        )

        val result = sync.syncPlatform("PlayStation 2")

        assertEquals(CatalogSyncResult.Success("playstation2", 2, 2), result)
        assertEquals(listOf("First", "Second"), dao.games.map { it.title })
        assertTrue(dao.ids.all { it.provider == "THE_GAMES_DB" })
    }

    @Test
    fun capsEachPlatformSyncAtTwentyGames() = runBlocking {
        val dao = RecordingCatalogDao()
        val games = (1..25).joinToString(",") { id ->
            """{"id":$id,"game_title":"Game $id"}"""
        }
        val transport = TheGamesDbCatalogTransport { uri ->
            when {
                uri.path.endsWith("/Platforms") ->
                    """{"data":{"platforms":[{"id":11,"name":"PlayStation 2"}]}}"""
                else -> """{"data":{"pages":{"current":1},"games":[$games]}}"""
            }
        }
        val sync = TheGamesDbCatalogSync(
            apiKey = { "key" },
            transport = transport,
            dao = dao,
            maxGamesPerPlatform = 20,
        )

        val result = sync.syncPlatform("PlayStation 2")

        assertEquals(CatalogSyncResult.Success("playstation2", 1, 20), result)
        assertEquals((1..20).map { "Game $it" }, dao.games.map { it.title })
    }

    @Test
    fun missingKeyDoesNotTouchNetworkOrDatabase() = runBlocking {
        var requests = 0
        val dao = RecordingCatalogDao()
        val sync = TheGamesDbCatalogSync(
            apiKey = { null },
            transport = TheGamesDbCatalogTransport { requests += 1; "{}" },
            dao = dao,
        )

        assertEquals(CatalogSyncResult.MissingApiKey, sync.syncPlatform("PlayStation 2"))
        assertEquals(0, requests)
        assertTrue(dao.games.isEmpty())
    }

    private class RecordingCatalogDao : CatalogDiscoveryDao {
        val games = mutableListOf<CatalogGameEntity>()
        val ids = mutableListOf<CatalogExternalIdEntity>()

        override suspend fun upsertPlatforms(platforms: List<CatalogPlatformEntity>) = Unit
        override suspend fun upsertGames(games: List<CatalogGameEntity>) { this.games += games }
        override suspend fun upsertExternalIds(ids: List<CatalogExternalIdEntity>) { this.ids += ids }
        override fun observeGames(
            platformId: String?,
            normalizedQuery: String,
            limit: Int,
            offset: Int,
        ): Flow<List<CatalogGameEntity>> = flowOf(emptyList())
        override fun observePlatforms(): Flow<List<CatalogPlatformEntity>> = flowOf(emptyList())
        override suspend fun countGames(platformId: String): Int = games.count { it.platformId == platformId }
        override suspend fun setFavorite(gameId: String, favorite: Boolean) = Unit
        override suspend fun upsertPage(
            platform: CatalogPlatformEntity,
            games: List<CatalogGameEntity>,
            externalIds: List<CatalogExternalIdEntity>,
        ) {
            upsertGames(games)
            upsertExternalIds(externalIds)
        }
    }
}
