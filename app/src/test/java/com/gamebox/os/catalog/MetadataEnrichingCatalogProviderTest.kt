package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataEnrichingCatalogProviderTest {
    private val game = Game(
        id = GameId("test"),
        title = "Test Game",
        platform = "Retro",
        year = 2026,
        genre = "Test",
        sizeMb = 1,
        state = InstallState.INSTALLED
    )

    @Test
    fun enrichmentFailurePreservesAuthorizedCatalogEntry() = runBlocking {
        val base = object : CatalogProvider {
            override suspend fun load() = CatalogSnapshot("v1", "Authorized", listOf(game))
        }
        val provider = MetadataEnrichingCatalogProvider(base) {
            error("metadata service unavailable")
        }

        assertEquals(listOf(game), provider.load().games)
    }

    @Test
    fun decoratorPropagatesFallbackReason() = runBlocking {
        val base = object : CatalogProvider, CatalogFallbackStatus {
            override suspend fun load() = CatalogSnapshot("v1", "Fallback", listOf(game))
            override fun consumeFallbackReason() = CatalogFallbackReason.REMOTE_FAILURE
        }
        val provider = MetadataEnrichingCatalogProvider(base) { it }

        provider.load()
        assertEquals(CatalogFallbackReason.REMOTE_FAILURE, provider.consumeFallbackReason())
    }
}
