package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDetailPresentationTest {
    @Test
    fun libraryAndDiscoveryUseTheSameMetadataContract() {
        val library = GameDetailPresentation.from(
            Game(
                id = GameId("shared"),
                title = "Shared Game",
                platform = "PSP",
                year = 2006,
                genre = "Racing",
                sizeMb = 512,
                state = InstallState.INSTALLED,
                favorite = true,
                artworkUrl = "https://example.test/cover.jpg",
                description = "Description",
                players = "2",
                region = "EU",
            )
        )
        val discovery = GameDetailPresentation.from(
            DiscoveryGame(
                id = GameId("shared"),
                title = "Shared Game",
                platformId = "psp",
                region = "EU",
                releaseDate = "2006-04-01",
                description = "Description",
                players = "2",
                rating = 8.5,
                coverUrl = "https://example.test/cover.jpg",
                backgroundUrl = "https://example.test/hero.jpg",
                logoUrl = null,
                screenshots = listOf("shot-a", "shot-a", "shot-b"),
                favorite = true,
            ),
            "PSP",
        )

        assertEquals(library.title, discovery.title)
        assertEquals(library.platform, discovery.platform)
        assertEquals(library.releaseLabel, discovery.releaseLabel)
        assertEquals(library.playersLabel, discovery.playersLabel)
        assertEquals(library.region, discovery.region)
        assertTrue(library.installed)
        assertFalse(discovery.installed)
        assertEquals(listOf("shot-a", "shot-b"), discovery.screenshots)
        assertEquals(GameDetailSource.LIBRARY, library.source)
        assertEquals(GameDetailSource.DISCOVERY, discovery.source)
    }

    @Test
    fun missingDescriptionsHaveSourceSpecificHonestCopy() {
        val library = GameDetailPresentation.from(
            Game(GameId("local"), "Local", "Retro", 0, "", 1, InstallState.NOT_INSTALLED)
        )
        val discovery = GameDetailPresentation.from(
            DiscoveryGame(
                GameId("remote"), "Remote", "ps2", null, null, null, null,
                null, null, null, null, emptyList(), false
            ),
            "PlayStation 2",
        )

        assertEquals("No description is available for this title.", library.description)
        assertTrue(discovery.description.contains("legal copy"))
    }
}
