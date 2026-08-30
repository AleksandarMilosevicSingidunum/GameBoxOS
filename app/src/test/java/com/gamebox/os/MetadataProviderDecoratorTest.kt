package com.gamebox.os

import com.gamebox.os.catalog.CatalogProvider
import com.gamebox.os.catalog.CatalogSnapshot
import com.gamebox.os.catalog.MetadataEnrichingCatalogProvider
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataProviderDecoratorTest {
    @Test
    fun enrichmentPreservesAuthorizedFieldsAndAddsMetadata() = runBlocking {
        val original = Game(GameId("g"), "Game", "Retro", 2024, "Arcade", 1, InstallState.NOT_INSTALLED,
            sourceUrl = "https://downloads.example.test/game.nes",
            expectedSha256 = "a".repeat(64))
        val base = object : CatalogProvider {
            override suspend fun load() = CatalogSnapshot("provider", "Provider", listOf(original))
        }
        val provider = MetadataEnrichingCatalogProvider(base) {
            it.copy(artworkUrl = "https://cdn.example.test/game.jpg", description = "About")
        }
        val result = provider.load()
        assertEquals("provider", result.providerId)
        assertEquals(original.sourceUrl, result.games.single().sourceUrl)
        assertEquals(original.expectedSha256, result.games.single().expectedSha256)
        assertEquals("About", result.games.single().description)
    }
}
