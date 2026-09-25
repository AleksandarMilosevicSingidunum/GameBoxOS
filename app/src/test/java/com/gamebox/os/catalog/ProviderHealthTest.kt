package com.gamebox.os.catalog

import com.gamebox.os.data.local.CatalogDiscoveryDao
import com.gamebox.os.data.local.CatalogExternalIdEntity
import com.gamebox.os.data.local.CatalogGameEntity
import com.gamebox.os.data.local.CatalogPlatformEntity
import com.gamebox.os.provider.ProviderFailureKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderHealthTest {
    @Test
    fun successfulSyncPublishesLatencyAndLastSuccess() = runBlocking {
        val health = mutableListOf<ProviderHealth>()
        val times = ArrayDeque(listOf(1_000L, 1_125L, 1_125L))
        val sync = TheGamesDbCatalogSync(
            apiKey = { "key" },
            transport = TheGamesDbCatalogTransport { uri ->
                if (uri.path.endsWith("/Platforms")) {
                    """{"data":{"platforms":[{"id":11,"name":"PlayStation 2"}]}}"""
                } else {
                    """{"data":{"pages":{"current":1},"games":[]}}"""
                }
            },
            dao = EmptyCatalogDao(),
            nowMillis = { times.removeFirst() },
            onHealthChanged = { health += it },
        )

        assertTrue(sync.syncPlatform("PlayStation 2") is CatalogSyncResult.Success)
        assertEquals(ProviderHealthStatus.HEALTHY, health.single().status)
        assertEquals(1_125L, health.single().lastSuccessAtMillis)
        assertEquals(125L, health.single().latencyMillis)
    }

    @Test
    fun authenticationFailureIsActionableAndDoesNotInventSuccess() = runBlocking {
        val health = mutableListOf<ProviderHealth>()
        val sync = TheGamesDbCatalogSync(
            apiKey = { "expired" },
            transport = TheGamesDbCatalogTransport {
                throw TheGamesDbTransportException(
                    ProviderFailureKind.AUTHENTICATION,
                    401,
                    null,
                    "Provider credentials are invalid or expired",
                )
            },
            dao = EmptyCatalogDao(),
            nowMillis = { 2_000L },
            onHealthChanged = { health += it },
        )

        val result = sync.syncPlatform("PlayStation 2")
        assertTrue(result is CatalogSyncResult.Failed)
        assertEquals(ProviderFailureKind.AUTHENTICATION, (result as CatalogSyncResult.Failed).kind)
        assertEquals(ProviderHealthStatus.AUTHENTICATION_REQUIRED, health.single().status)
        assertNull(health.single().lastSuccessAtMillis)
    }

    @Test
    fun rateLimitRetainsProviderRetryDeadline() = runBlocking {
        val retryAt = 80_000L
        val health = mutableListOf<ProviderHealth>()
        val sync = TheGamesDbCatalogSync(
            apiKey = { "key" },
            transport = TheGamesDbCatalogTransport {
                throw TheGamesDbTransportException(
                    ProviderFailureKind.RATE_LIMITED,
                    429,
                    retryAt,
                    "Provider requested a retry later",
                )
            },
            dao = EmptyCatalogDao(),
            nowMillis = { 20_000L },
            onHealthChanged = { health += it },
        )

        val result = sync.syncPlatform("PlayStation 2") as CatalogSyncResult.Failed
        assertEquals(retryAt, result.retryAfterMillis)
        assertEquals(ProviderHealthStatus.RATE_LIMITED, health.single().status)
        assertEquals("Rate limited • 0 ms • retry in 1m",
            providerHealthSummary(health.single(), nowMillis = 20_000L))
    }

    @Test
    fun missingCredentialPublishesNotConfiguredWithoutNetwork() = runBlocking {
        var requested = false
        val health = mutableListOf<ProviderHealth>()
        val sync = TheGamesDbCatalogSync(
            apiKey = { null },
            transport = TheGamesDbCatalogTransport { requested = true; "{}" },
            dao = EmptyCatalogDao(),
            nowMillis = { 3_000L },
            onHealthChanged = { health += it },
        )

        assertEquals(CatalogSyncResult.MissingApiKey, sync.syncPlatform("PSP"))
        assertTrue(!requested)
        assertEquals(ProviderHealthStatus.NOT_CONFIGURED, health.single().status)
        assertEquals(3_000L, health.single().lastAttemptAtMillis)
    }

    private class EmptyCatalogDao : CatalogDiscoveryDao {
        override suspend fun upsertPlatforms(platforms: List<CatalogPlatformEntity>) = Unit
        override suspend fun upsertGames(games: List<CatalogGameEntity>) = Unit
        override suspend fun upsertExternalIds(ids: List<CatalogExternalIdEntity>) = Unit
        override fun observeGames(platformId: String?, normalizedQuery: String, limit: Int, offset: Int):
            Flow<List<CatalogGameEntity>> = flowOf(emptyList())
        override fun observeGame(gameId: String): kotlinx.coroutines.flow.Flow<CatalogGameEntity?> =
            kotlinx.coroutines.flow.flowOf(null)
        override fun observePlatforms(): Flow<List<CatalogPlatformEntity>> = flowOf(emptyList())
        override suspend fun countGames(platformId: String): Int = 0
        override suspend fun setFavorite(gameId: String, favorite: Boolean) = Unit
        override suspend fun favoriteGameIds(gameIds: List<String>): List<String> = emptyList()
    }
}
