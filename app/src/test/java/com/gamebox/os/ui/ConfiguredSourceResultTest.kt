package com.gamebox.os.ui

import com.gamebox.os.source.DiscoverySourceGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfiguredSourceResultTest {
    @Test
    fun configuredResultBecomesStableImportableDiscoveryIdentity() {
        val source = DiscoverySourceGame(
            sourceId = "vimm-source",
            externalId = "12345",
            title = "Example Game",
            platform = "PS2",
            region = "USA",
            year = 2005,
            detailsUrl = "https://vimm.net/vault/12345",
        )

        val first = configuredSourceResultToDiscoveryGame(source)
        val second = configuredSourceResultToDiscoveryGame(source)

        assertEquals(first.id, second.id)
        assertTrue(first.id.value.startsWith("src-vimm-source-"))
        assertFalse(first.id.value.contains("12345"))
        assertEquals("Example Game", first.title)
        assertEquals("playstation2", first.platformId)
        assertEquals("USA", first.region)
        assertEquals("2005", first.releaseDate)
        assertEquals(false, first.favorite)
    }

    @Test
    fun differentExternalIdsDoNotShareImportIdentity() {
        val first = configuredSourceResultToDiscoveryGame(
            DiscoverySourceGame("vimm-source", "100", "One", "GameCube")
        )
        val second = configuredSourceResultToDiscoveryGame(
            DiscoverySourceGame("vimm-source", "101", "Two", "GameCube")
        )

        assertTrue(first.id != second.id)
        assertEquals("nintendogamecube", first.platformId)
    }
}
