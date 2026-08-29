package com.gamebox.os.ui

import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test

class GameBoxSemanticsTest {
    @Test
    fun gameCardAnnouncesIdentityStateAndFavoriteStatus() {
        val game = Game(GameId("galaxy-patrol"), "Galaxy Patrol", "Retro", 2018, "Arcade", 1,
            InstallState.INSTALLED, favorite = true)

        assertEquals(
            "Galaxy Patrol, Retro, installed, favorite, continue playing",
            GameBoxSemantics.gameCardDescription(game, hero = true),
        )
    }

    @Test
    fun downloadAnnouncesFailureAndBoundedProgress() {
        val job = DownloadJob("job", GameId("game"), "Homebrew", DownloadStatus.FAILED, 200, 50, "Network unavailable")

        assertEquals("Homebrew, failed, Network unavailable", GameBoxSemantics.downloadDescription(job))
        assertEquals("25 percent downloaded", GameBoxSemantics.downloadProgressDescription(job))
    }
}
