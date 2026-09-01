package com.gamebox.os

import com.gamebox.os.data.mergeCatalogPreservingLocalState
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMergePolicyTest {
    @Test fun refresh_updatesMetadataButPreservesLocalProgress() {
        val local = game("same", "Old title", InstallState.INSTALLED, minutes = 90).copy(
            localContentRelativePath = "same/game.iso",
            localContentSha256 = "a".repeat(64),
            localContentMimeType = "application/x-iso9660-image",
        )
        val remote = game("same", "New title", InstallState.NOT_INSTALLED, minutes = 0)

        val merged = mergeCatalogPreservingLocalState(listOf(local), listOf(remote)).single()

        assertEquals("New title", merged.title)
        assertEquals(InstallState.INSTALLED, merged.state)
        assertEquals(90, merged.minutesPlayed)
        assertEquals("same/game.iso", merged.localContentRelativePath)
        assertEquals("a".repeat(64), merged.localContentSha256)
    }

    @Test fun refreshKeepsMissingLocalGamesAndAddsNewRemoteGames() {
        val localOnly = game("local", "Local", InstallState.INSTALLED)
        val remoteOnly = game("remote", "Remote", InstallState.NOT_INSTALLED)

        val merged = mergeCatalogPreservingLocalState(listOf(localOnly), listOf(remoteOnly))

        assertEquals(2, merged.size)
        assertTrue(merged.any { it.id == GameId("local") && it.state == InstallState.INSTALLED })
        assertTrue(merged.any { it.id == GameId("remote") })
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
