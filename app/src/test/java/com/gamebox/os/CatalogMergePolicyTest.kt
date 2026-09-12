package com.gamebox.os

import com.gamebox.os.data.mergeCatalogPreservingLocalState
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.LocalContentFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMergePolicyTest {
    @Test fun refresh_updatesMetadataButPreservesLocalProgress() {
        val local = game("same", "Old title", InstallState.INSTALLED, minutes = 90).copy(
            localContentRelativePath = "same/game.iso",
            localContentSha256 = "a".repeat(64),
            localContentMimeType = "application/x-iso9660-image",
            localContentFiles = listOf(
                LocalContentFile("same/game.iso", "a".repeat(64), "application/x-iso9660-image")
            ),
        )
        val remote = game("same", "New title", InstallState.NOT_INSTALLED, minutes = 0)

        val merged = mergeCatalogPreservingLocalState(listOf(local), listOf(remote)).single()

        assertEquals("New title", merged.title)
        assertEquals(InstallState.INSTALLED, merged.state)
        assertEquals(90, merged.minutesPlayed)
        assertEquals("same/game.iso", merged.localContentRelativePath)
        assertEquals("a".repeat(64), merged.localContentSha256)
        assertEquals(1, merged.localContentFiles.size)
    }

    @Test fun refreshKeepsMissingLocalGamesButInvalidatesRemovedRemoteSource() {
        val localOnly = game("local", "Local", InstallState.INSTALLED).copy(
            favorite = true,
            minutesPlayed = 27,
            sourceUrl = "https://catalog.example/games/local.zip",
            expectedSha256 = "b".repeat(64),
            localContentRelativePath = "local/game.zip",
            localContentSha256 = "c".repeat(64),
            localContentMimeType = "application/zip",
        )
        val remoteOnly = game("remote", "Remote", InstallState.NOT_INSTALLED)

        val merged = mergeCatalogPreservingLocalState(listOf(localOnly), listOf(remoteOnly))
        val retained = merged.single { it.id == GameId("local") }

        assertEquals(2, merged.size)
        assertEquals(InstallState.INSTALLED, retained.state)
        assertTrue(retained.favorite)
        assertEquals(27, retained.minutesPlayed)
        assertEquals("local/game.zip", retained.localContentRelativePath)
        assertEquals("c".repeat(64), retained.localContentSha256)
        assertEquals(null, retained.sourceUrl)
        assertEquals(null, retained.expectedSha256)
        assertTrue(merged.any { it.id == GameId("remote") })
    }

    @Test fun newlyDiscoveredGamesCannotClaimInstallationOrQueueWork() {
        InstallState.entries.forEach { state ->
            val merged = mergeCatalogPreservingLocalState(emptyList(), listOf(game("new", "New", state))).single()
            assertEquals(InstallState.NOT_INSTALLED, merged.state)
        }
    }

    private fun game(id: String, title: String, state: InstallState, minutes: Int = 0) = Game(
        id = GameId(id),
        title = title,
        platform = "Homebrew",
        year = 2026,
        genre = "Test",
        sizeMb = 1,
        state = state,
        minutesPlayed = minutes
    )
}
