package com.gamebox.os

import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.GameSource
import com.gamebox.os.domain.GameSourceType
import com.gamebox.os.domain.StoreAvailability
import com.gamebox.os.domain.normalizeCatalogTitle
import com.gamebox.os.domain.storeAvailability
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogModelsTest {
    private val gameId = GameId("gamebox-ps2-example")

    @Test
    fun metadataOnlyGamesNeverPretendToBeInstallable() {
        assertEquals(StoreAvailability.DISCOVER_ONLY, storeAvailability(false, emptyList()))
    }

    @Test
    fun authorizedDownloadsRequireChecksum() {
        val unverified = GameSource(
            gameId = gameId,
            providerId = "catalog",
            type = GameSourceType.AUTHORIZED_HTTP,
            location = "https://example.invalid/game.iso",
            available = true,
        )
        val verified = unverified.copy(expectedSha256 = "a".repeat(64))

        assertEquals(StoreAvailability.DISCOVER_ONLY, storeAvailability(false, listOf(unverified)))
        assertEquals(StoreAvailability.AUTHORIZED_DOWNLOAD, storeAvailability(false, listOf(verified)))
    }

    @Test
    fun externalPagesRemainExternalSources() {
        val source = GameSource(
            gameId = gameId,
            providerId = "external-catalog",
            type = GameSourceType.EXTERNAL_PAGE,
            location = "https://example.invalid/game",
            available = true,
        )

        assertEquals(StoreAvailability.EXTERNAL_SOURCE, storeAvailability(false, listOf(source)))
    }

    @Test
    fun titleNormalizationIsProviderIndependent() {
        assertEquals("godofwarii", normalizeCatalogTitle("God of War II"))
        assertEquals("godofwarii", normalizeCatalogTitle("God-of-War_II"))
    }
}
