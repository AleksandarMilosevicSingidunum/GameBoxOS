package com.gamebox.os.catalog

import com.gamebox.os.data.local.CatalogDiscoveryDao
import com.gamebox.os.data.local.CatalogExternalIdEntity
import com.gamebox.os.data.local.CatalogGameEntity
import com.gamebox.os.data.local.CatalogPlatformEntity
import kotlinx.coroutines.runBlocking
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TheGamesDbCatalogSyncTest {
    @Test
    fun sonyPlatformAliasesResolveToTheGamesDbPlatformId() = runBlocking {
        val requestedUris = mutableListOf<URI>()
        val dao = RecordingCatalogDiscoveryDao()
        val transport = TheGamesDbCatalogTransport { uri ->
            requestedUris += uri
            if (uri.path.endsWith("/Platforms")) {
                """
                {"data":{"platforms":[
                  {"id":"21","name":"Sony Playstation 2"},
                  {"id":"18","name":"Sony Playstation Portable"}
                ]}}
                """.trimIndent()
            } else {
                """{"data":{"games":[],"pages":{"current":1}}}"""
            }
        }
        val sync = TheGamesDbCatalogSync(
            apiKey = { "test-key" },
            transport = transport,
            dao = dao,
        )

        assertTrue(sync.syncPlatform("Sony Playstation 2") is CatalogSyncResult.Success)
        assertTrue(sync.syncPlatform("Sony Playstation Portable") is CatalogSyncResult.Success)
        assertEquals(listOf("21", "18"), requestedUris
            .filter { it.path.endsWith("/Games/ByPlatformID") }
            .map { it.getQueryParameter("id") })
    }

    private class RecordingCatalogDiscoveryDao : CatalogDiscoveryDao {
        override suspend fun upsertPlatforms(platforms: List<CatalogPlatformEntity>) = Unit
        override suspend fun upsertGames(games: List<CatalogGameEntity>) = Unit
        override suspend fun upsertExternalIds(ids: List<CatalogExternalIdEntity>) = Unit
        override fun observeGames(platformId: String?, normalizedQuery: String, limit: Int, offset: Int) =
            kotlinx.coroutines.flow.emptyFlow<List<CatalogGameEntity>>()
        override fun observePlatforms() = kotlinx.coroutines.flow.emptyFlow<List<CatalogPlatformEntity>>()
        override suspend fun countGames(platformId: String) = 0
        override suspend fun setFavorite(gameId: String, favorite: Boolean) = Unit
    }

    private fun URI.getQueryParameter(name: String): String? = rawQuery
        ?.split('&')
        ?.mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator < 0 || part.substring(0, separator) != name) null
            else java.net.URLDecoder.decode(part.substring(separator + 1), Charsets.UTF_8.name())
        }
        ?.firstOrNull()
}

