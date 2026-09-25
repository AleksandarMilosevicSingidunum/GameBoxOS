package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.GameId
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
    fun exactCachedTitleAndPlatformEnrichVimmPresentation() {
        val source = DiscoverySourceGame(
            sourceId = "vimm-source",
            externalId = "12345",
            title = "God Hand",
            platform = "PS2",
        )
        val cached = DiscoveryGame(
            id = GameId("thegamesdb-god-hand"),
            title = "God Hand",
            platformId = "playstation2",
            region = "USA",
            releaseDate = "2006-10-10",
            description = "Cached description",
            players = "1",
            rating = 8.8,
            coverUrl = "https://cdn.example.test/god-hand.jpg",
            backgroundUrl = "https://cdn.example.test/god-hand-bg.jpg",
            logoUrl = "https://cdn.example.test/god-hand-logo.png",
            screenshots = listOf("https://cdn.example.test/god-hand-shot.jpg"),
            favorite = true,
        )

        val enriched = configuredSourceResultToDiscoveryGame(source, listOf(cached))

        assertEquals("Cached description", enriched.description)
        assertEquals("https://cdn.example.test/god-hand.jpg", enriched.coverUrl)
        assertEquals("https://cdn.example.test/god-hand-bg.jpg", enriched.backgroundUrl)
        assertEquals(8.8, enriched.rating)
        assertEquals("2006-10-10", enriched.releaseDate)
        assertEquals(false, enriched.favorite)
        assertTrue(enriched.id.value.startsWith("src-vimm-source-"))
    }

    @Test
    fun sourceSpecificRegionAndYearOverrideCachedMetadata() {
        val source = DiscoverySourceGame(
            sourceId = "vimm-source",
            externalId = "200",
            title = "Example",
            platform = "GameCube",
            region = "Europe",
            year = 2004,
        )
        val cached = DiscoveryGame(
            id = GameId("cached"),
            title = "Example",
            platformId = "nintendogamecube",
            region = "USA",
            releaseDate = "2003",
            description = "Metadata",
            players = null,
            rating = null,
            coverUrl = null,
            backgroundUrl = null,
            logoUrl = null,
            screenshots = emptyList(),
            favorite = false,
        )

        val enriched = configuredSourceResultToDiscoveryGame(source, listOf(cached))

        assertEquals("Europe", enriched.region)
        assertEquals("2004", enriched.releaseDate)
        assertEquals("Metadata", enriched.description)
    }

    @Test
    fun ambiguousOrWrongPlatformCachedMatchesAreIgnored() {
        val source = DiscoverySourceGame(
            sourceId = "vimm-source",
            externalId = "300",
            title = "Same Name",
            platform = "PS2",
        )
        val sameOne = DiscoveryGame(
            GameId("one"), "Same Name", "playstation2", null, "2001",
            "One", null, null, "https://cdn.example.test/one.jpg", null, null,
            emptyList(), false,
        )
        val sameTwo = sameOne.copy(id = GameId("two"), description = "Two")
        val wrongPlatform = sameOne.copy(
            id = GameId("gc"),
            platformId = "nintendogamecube",
            description = "Wrong platform",
        )

        val ambiguous = configuredSourceResultToDiscoveryGame(
            source,
            listOf(sameOne, sameTwo, wrongPlatform),
        )
        val wrongOnly = configuredSourceResultToDiscoveryGame(source, listOf(wrongPlatform))

        assertEquals(null, ambiguous.description)
        assertEquals(null, ambiguous.coverUrl)
        assertEquals(null, wrongOnly.description)
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
