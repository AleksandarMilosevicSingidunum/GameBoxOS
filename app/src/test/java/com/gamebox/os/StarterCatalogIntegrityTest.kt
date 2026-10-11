package com.gamebox.os

import com.gamebox.os.catalog.CatalogParser
import com.gamebox.os.domain.InstallState
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class StarterCatalogIntegrityTest {
    @Test fun starterCatalogNeverClaimsGamesAreInstalledOrQueued() {
        val input = File("src/main/assets/catalog/authorized-fixture.json").readText()
        assertFalse("Starter metadata must not contain fabricated local states", input.contains("initialState"))
        val games = CatalogParser().parse(input).games
        assertTrue(games.isNotEmpty())
        assertTrue(games.all { it.state == InstallState.NOT_INSTALLED })
        assertTrue(games.any { it.id.value == "galaxy-patrol" })
    }

    @Test fun starterDoesNotUseBoxArtFromDifferentCommercialGames() {
        val input = File("src/main/assets/catalog/authorized-fixture.json").readText()
        assertFalse(input.contains("/apps/70300/")) // VVVVVV is not Cave Story.
        assertFalse(input.contains("/apps/504230/")) // Celeste is not Celeste Classic.
    }

    @Test fun starterRetroAndHomebrewTitlesHaveTitleCorrectArtwork() {
        val input = File("src/main/assets/catalog/authorized-fixture.json").readText()
        val games = CatalogParser().parse(input).games.associateBy { it.id.value }

        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/8/8e/CelesteClassic.png",
            games.getValue("celeste").artworkUrl,
        )
        assertEquals(
            "https://thumbnails.libretro.com/Nintendo%20-%20Nintendo%203DS/Named_Boxarts/Cave%20Story%20%28USA%29.png",
            games.getValue("cave-story").artworkUrl,
        )
    }
}
