package com.gamebox.os

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.summarizeLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibrarySummaryTest {
    @Test fun summaryCountsInstalledFavoritesAndPlaytime() {
        val summary = summarizeLibrary(listOf(
            game("one", InstallState.INSTALLED, 125, true, "2026-08-01T10:00:00Z"),
            game("two", InstallState.NOT_INSTALLED, 35, false, null),
            game("three", InstallState.UPDATE_AVAILABLE, 60, true, "2026-08-02T10:00:00Z")
        ))
        assertEquals(3, summary.totalGames)
        assertEquals(2, summary.installedGames)
        assertEquals(2, summary.favorites)
        assertEquals(220, summary.totalMinutesPlayed)
        assertEquals(3, summary.totalHoursPlayed)
        assertEquals(40, summary.remainingMinutes)
        assertEquals("three", summary.resumeGame?.id?.value)
    }

    @Test fun emptySummaryHasNoResumeGame() {
        assertNull(summarizeLibrary(emptyList()).resumeGame)
    }

    private fun game(id: String, state: InstallState, minutes: Int, favorite: Boolean, lastPlayed: String?) =
        Game(GameId(id), id, "Retro", 2020, "Action", 1, state, lastPlayed, minutes, favorite)
}
