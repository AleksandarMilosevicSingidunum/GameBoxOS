package com.gamebox.os

import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.data.mergeImportedGame
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
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

    private fun imported() = ImportedGameRegistration(
        id = GameId("game"),
        title = "Game",
        platform = "PlayStation 2",
        year = 2004,
        sizeBytes = 1024L * 1024L + 1L,
        relativePath = "game/Game.iso",
        sha256 = "c".repeat(64),
        mimeType = "application/x-iso9660-image",
    )
}
