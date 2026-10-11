package com.gamebox.os.ui

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteArtworkFallbackTest {
    @Test
    fun starterFallbackIdentitiesSurviveEditedAndLegacyTitles() {
        val expected = mapOf(
            "cave-story" to "Cave Story",
            "celeste" to "Celeste Classic",
            "freedoom" to "Freedoom",
            "galaxy-patrol" to "Galaxy Patrol",
            "luanti" to "Luanti",
            "openarena" to "OpenArena",
            "openmw" to "OpenMW",
            "supertuxkart" to "SuperTuxKart",
        )

        expected.forEach { (id, title) ->
            val game = Game(
                id = GameId(id), title = "Edited display title", providerTitle = "Legacy provider title",
                platform = "Homebrew", year = 2026, genre = "Test", sizeMb = 1,
                state = InstallState.NOT_INSTALLED,
            )
            assertEquals("Fallback identity for $id", title, gameArtworkFallbackTitle(game))
        }
    }

    @Test
    fun fallbackMonogramIsReadableAndStableForCommonTitleShapes() {
        assertEquals("CS", artworkPlaceholderMonogram("Cave Story"))
        assertEquals("OP", artworkPlaceholderMonogram("OpenMW"))
        assertEquals("NES", artworkPlaceholderMonogram("NES"))
        assertEquals("PS2", artworkPlaceholderMonogram("PS2"))
        assertEquals("GB", artworkPlaceholderMonogram("Game Box"))
        assertEquals("GB", artworkPlaceholderMonogram("---"))
    }

    @Test
    fun libraryFallbackRetainsBundledCoverAfterDisplayTitleIsEdited() {
        val game = Game(
            id = GameId("cave-story"),
            title = "My Cave Story",
            providerTitle = "Cave Story",
            platform = "Retro",
            year = 2004,
            genre = "Adventure",
            sizeMb = 18,
            state = InstallState.INSTALLED,
        )

        assertEquals("Cave Story", gameArtworkFallbackTitle(game))
    }

    @Test
    fun preloadedLibraryCoverUsesStableGameIdWhenStoredTitlesAreStale() {
        val preloaded = Game(
            id = GameId("supertuxkart"),
            title = "Legacy display title",
            providerTitle = "Legacy provider title",
            platform = "Android",
            year = 2025,
            genre = "Racing",
            sizeMb = 820,
            state = InstallState.NOT_INSTALLED,
        )

        assertEquals("SuperTuxKart", gameArtworkFallbackTitle(preloaded))
    }

    @Test
    fun galaxyPatrolUsesBundledCoverWhenCatalogHasNoArtworkUrl() {
        val game = Game(
            id = GameId("galaxy-patrol"),
            title = "Legacy Galaxy Patrol title",
            providerTitle = "Older provider title",
            platform = "Retro",
            year = 2018,
            genre = "Arcade",
            sizeMb = 1,
            state = InstallState.INSTALLED,
        )

        assertEquals(
            R.drawable.galaxy_patrol_cover,
            bundledArtworkResource(gameArtworkFallbackTitle(game)),
        )
        assertNull(bundledArtworkResource("Unmatched title"))
    }
}
