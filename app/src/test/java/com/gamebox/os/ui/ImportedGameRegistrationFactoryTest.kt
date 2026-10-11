package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.LocalContentFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedGameRegistrationFactoryTest {
    @Test
    fun registrationUsesTheLaunchFileAndRetainsAllDiscFilesAndMetadata() {
        val game = DiscoveryGame(
            id = GameId("test-game"),
            title = "Test Game",
            platformId = "psp",
            region = "Europe",
            releaseDate = "2011-03-15",
            description = "Description",
            players = "1",
            rating = 8.5,
            coverUrl = "https://example.test/cover.jpg",
            backgroundUrl = null,
            logoUrl = null,
            screenshots = emptyList(),
            favorite = true,
        )
        val launchFile = LocalContentFile(
            relativePath = "imports/test-game/disc-1.iso",
            sha256 = "a".repeat(64),
            mimeType = "application/octet-stream",
        )
        val secondDisc = LocalContentFile(
            relativePath = "imports/test-game/disc-2.iso",
            sha256 = "b".repeat(64),
            mimeType = "application/octet-stream",
        )

        val registration = importedGameRegistration(
            game = game,
            platform = "PSP",
            launchFile = launchFile,
            files = listOf(launchFile, secondDisc),
            sizeBytes = 2_048L,
        )

        assertEquals(game.id, registration.id)
        assertEquals("PSP", registration.platform)
        assertEquals(2011, registration.year)
        assertEquals(2_048L, registration.sizeBytes)
        assertEquals(launchFile.relativePath, registration.relativePath)
        assertEquals(launchFile.sha256, registration.sha256)
        assertEquals(listOf(secondDisc), registration.additionalFiles)
        assertTrue(registration.favorite)
        assertEquals(game.coverUrl, registration.artworkUrl)
        assertEquals(game.description, registration.description)
        assertEquals(game.players, registration.players)
        assertEquals(game.region, registration.region)
    }

    @Test(expected = IllegalArgumentException::class)
    fun registrationRejectsLaunchFileOutsideImportedSet() {
        val game = DiscoveryGame(
            id = GameId("test-game"),
            title = "Test Game",
            platformId = "psp",
            releaseDate = null,
            description = null,
            players = null,
            rating = null,
            coverUrl = null,
            backgroundUrl = null,
            logoUrl = null,
            screenshots = emptyList(),
            favorite = false,
        )
        importedGameRegistration(
            game = game,
            platform = "PSP",
            launchFile = LocalContentFile("imports/test-game/launch.iso", "a".repeat(64), "application/octet-stream"),
            files = emptyList(),
            sizeBytes = 1L,
        )
    }
}
