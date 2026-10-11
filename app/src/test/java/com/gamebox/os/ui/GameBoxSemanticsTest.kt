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
    fun gameCardAnnouncesTrackedPlaytime() {
        val game = Game(
            GameId("galaxy-patrol"),
            "Galaxy Patrol",
            "Retro",
            2018,
            "Arcade",
            1,
            InstallState.INSTALLED,
            minutesPlayed = 125,
        )

        assertEquals(
            "Galaxy Patrol, Retro, installed, 2 hours 5 minutes played",
            GameBoxSemantics.gameCardDescription(game, hero = false),
        )
    }

    @Test
    fun playtimeAnnouncementUsesNaturalSingularAndOmitsZeroUnits() {
        assertEquals("0 minutes played", GameBoxSemantics.playtimeDescription(0))
        assertEquals("1 minute played", GameBoxSemantics.playtimeDescription(1))
        assertEquals("5 minutes played", GameBoxSemantics.playtimeDescription(5))
        assertEquals("1 hour played", GameBoxSemantics.playtimeDescription(60))
        assertEquals("2 hours played", GameBoxSemantics.playtimeDescription(120))
        assertEquals("1 hour 1 minute played", GameBoxSemantics.playtimeDescription(61))
        assertEquals("2 hours 5 minutes played", GameBoxSemantics.playtimeDescription(125))
    }

    @Test
    fun downloadAnnouncesFailureAndBoundedProgress() {
        val job = DownloadJob("job", GameId("game"), "Homebrew", DownloadStatus.FAILED, 200, 50, "Network unavailable")

        assertEquals("Homebrew, failed, Network unavailable", GameBoxSemantics.downloadDescription(job))
        assertEquals("25 percent downloaded", GameBoxSemantics.downloadProgressDescription(job))
    }
}
