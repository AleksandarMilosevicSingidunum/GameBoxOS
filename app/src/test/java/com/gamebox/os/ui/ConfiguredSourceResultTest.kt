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
    fun consoleResultRowsMergeVimmPlatformAliasesAndFollowPresetOrder() {
        val rows = configuredDiscoveryResultConsoleRows(
            listOf(
                DiscoverySourceGame("vimm-source", "ps2-1", "PS2 First", "PlayStation 2"),
                DiscoverySourceGame("vimm-source", "gba-1", "GBA First", "GBA"),
                DiscoverySourceGame("vimm-source", "ps2-2", "PS2 Second", "PS2"),
                DiscoverySourceGame("vimm-source", "gba-2", "GBA Second", "Game Boy Advance"),
            ),
        )

        assertEquals(listOf("Game Boy Advance", "PS2"), rows.map { it.label })
        assertEquals(listOf("GBA First", "GBA Second"), rows[0].games.map { it.title })
        assertEquals(listOf("PS2 First", "PS2 Second"), rows[1].games.map { it.title })
    }

    @Test
    fun editionLabelShowsOnlyRegionAndYearProvidedBySource() {
        assertEquals(
            "Europe · 2004",
            configuredSourceResultEditionLabel(
                DiscoverySourceGame(
                    sourceId = "vimm-source",
                    externalId = "12345",
                    title = "Example Game",
                    platform = "PSP",
                    region = " Europe ",
                    year = 2004,
                ),
            ),
        )
        assertEquals(
            "Japan",
            configuredSourceResultEditionLabel(
                DiscoverySourceGame("vimm-source", "12346", "Example Game", "PSP", region = "Japan"),
            ),
        )
        assertEquals(
            null,
            configuredSourceResultEditionLabel(
                DiscoverySourceGame("vimm-source", "12347", "Example Game", "PSP"),
            ),
        )
    }

    @Test
    fun sourcePlatformBadgeUsesCompactConsoleLabels() {
        assertEquals("GBA", configuredSourceResultPlatformLabel("Nintendo Game Boy Advance"))
        assertEquals("GBC", configuredSourceResultPlatformLabel("Game Boy Color"))
        assertEquals("PS2", configuredSourceResultPlatformLabel("Sony PlayStation 2"))
        assertEquals("Unknown Console", configuredSourceResultPlatformLabel(" Unknown Console "))
    }

    @Test
    fun staleSourceDetailsProduceActionableMetadataOnlyNotice() {
        assertEquals(
            "Vimm's Lair result found, but extra details could not be loaded. You can still use Download to open the source, or import a copy you own.",
            configuredSourceDetailFailureMessage("Vimm's Lair"),
        )
    }

    @Test
    fun sourceDetailLookupFailureDoesNotClaimTheListingWasRemoved() {
        val notice = configuredSourceNotFoundMessage("Vimm's Lair")

        assertTrue(notice.contains("details could not be loaded in GameBox"))
        assertTrue(notice.contains("open the source page"))
        assertFalse(notice.contains("no longer has this listing"))
    }

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
