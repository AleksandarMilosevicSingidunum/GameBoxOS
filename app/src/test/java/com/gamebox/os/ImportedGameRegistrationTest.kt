package com.gamebox.os

import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.data.mergeImportedGame
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameMetadataOverrides
import com.gamebox.os.data.local.toDomain
import com.gamebox.os.data.local.toEntity
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.LocalContentFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportedGameRegistrationTest {
    @Test fun promotesAnImportToInstalledLibraryContent() {
        val registration = imported()

        val game = mergeImportedGame(null, registration)

        assertEquals(InstallState.INSTALLED, game.state)
        assertEquals("game/Game.iso", game.localContentRelativePath)
        assertEquals("c".repeat(64), game.localContentSha256)
        assertEquals(2, game.sizeMb)
        assertEquals(2, game.localContentFiles.size)
    }

    @Test fun replacingAFilePreservesUserStateAndSettings() {
        val existing = Game(
            GameId("game"), "Old", "PS2", 2004, "Racing", 1, InstallState.INSTALLED,
            lastPlayed = "2026-08-31T12:00:00Z",
            minutesPlayed = 45,
            favorite = true,
            emulatorPackage = "xyz.aethersx2.android",
            graphicsProfile = "Performance",
        )

        val game = mergeImportedGame(existing, imported())

        assertEquals(true, game.favorite)
        assertEquals(45, game.minutesPlayed)
        assertEquals("xyz.aethersx2.android", game.emulatorPackage)
        assertEquals("Performance", game.graphicsProfile)
    }

    @Test fun rejectsTraversalAndInvalidChecksums() {
        assertThrows(IllegalArgumentException::class.java) {
            imported().copy(relativePath = "game/../outside.iso")
        }
        assertThrows(IllegalArgumentException::class.java) {
            imported().copy(sha256 = "not-a-checksum")
        }
    }

    @Test fun replacementRetainsCorrectionsProviderIdentityAndResetValuesThroughPersistence() {
        val overrides = GameMetadataOverrides(
            title = "My title", year = 2005, genre = "My genre",
            artworkUrl = "https://example.test/custom.jpg", description = "My notes",
        )
        val existing = Game(
            GameId("game"), "My title", "PS2", 2005, "My genre", 1, InstallState.MISSING_FILES,
            lastPlayed = "2026-09-20T10:00:00Z", minutesPlayed = 75, favorite = true,
            artworkUrl = overrides.artworkUrl, description = overrides.description,
            region = "EU", language = "French", players = "2",
            metadataOverrides = overrides,
            providerTitle = "Confirmed title", providerYear = 2004, providerGenre = "Racing",
            providerArtworkUrl = "https://example.test/provider.jpg",
            providerDescription = "Provider description",
            metadataProvider = "THE_GAMES_DB", metadataExternalId = "123",
            metadataMatchedAtMillis = 1000L,
        )
        val replacement = imported().copy(title = "Stale discovery", region = "US")
        val merged = mergeImportedGame(existing, replacement)
        val roundTrip = merged.toEntity().toDomain()
        assertEquals(overrides, roundTrip.metadataOverrides)
        assertEquals(existing.title, roundTrip.title)
        assertEquals(existing.platform, roundTrip.platform)
        assertEquals("EU", roundTrip.region)
        assertEquals("French", roundTrip.language)
        assertEquals("123", roundTrip.metadataExternalId)
        assertEquals("THE_GAMES_DB", roundTrip.metadataProvider)
        assertEquals(1000L, roundTrip.metadataMatchedAtMillis)
        assertEquals(75, roundTrip.minutesPlayed)
        assertEquals(InstallState.INSTALLED, roundTrip.state)
        assertEquals(replacement.relativePath, roundTrip.localContentRelativePath)
        val reset = merged.toEntity().copy(
            userTitle = null, userYear = null, userGenre = null,
            userArtworkUrl = null, userDescription = null,
        ).toDomain()
        assertEquals("Confirmed title", reset.title)
        assertEquals(2004, reset.year)
        assertEquals("Racing", reset.genre)
        assertEquals("https://example.test/provider.jpg", reset.artworkUrl)
        assertEquals("Provider description", reset.description)
    }

    @Test fun replacementRejectsDifferentGameIdentity() {
        val other = Game(GameId("other"), "Other", "PS2", 2004, "Racing", 1, InstallState.INSTALLED)
        assertThrows(IllegalArgumentException::class.java) { mergeImportedGame(other, imported()) }
    }

    private fun imported() = ImportedGameRegistration(
        id = GameId("game"),
        title = "Game",
        platform = "PlayStation 2",
        year = 2004,
        sizeBytes = 1024L * 1024L + 1L,
        relativePath = "game/Game.iso",
        sha256 = "c".repeat(64),
        mimeType = "application/x-iso9660-image",
        additionalFiles = listOf(
            LocalContentFile("game/Game.bin", "d".repeat(64), "application/octet-stream")
        ),
    )
}
