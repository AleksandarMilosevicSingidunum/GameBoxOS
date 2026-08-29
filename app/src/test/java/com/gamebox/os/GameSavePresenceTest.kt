package com.gamebox.os

import com.gamebox.os.data.enrichGamesWithSaveRecords
import com.gamebox.os.data.local.SaveRecordEntity
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSavePresenceTest {
    private val games = listOf(
        Game(GameId("alpha"), "Alpha", "NES", 1990, "Action", 4, InstallState.INSTALLED),
        Game(GameId("beta"), "Beta", "SNES", 1992, "RPG", 8, InstallState.NOT_INSTALLED)
    )

    @Test
    fun matchingSaveRecordMarksGameAndExposesSize() {
        val result = enrichGamesWithSaveRecords(
            games,
            listOf(SaveRecordEntity("beta", "saves/beta.sav", 1234L, 4096L))
        )

        assertFalse(result[0].savePresent)
        assertEquals(0L, result[0].saveSizeBytes)
        assertTrue(result[1].savePresent)
        assertEquals(4096L, result[1].saveSizeBytes)
    }

    @Test
    fun staleAndNegativeRecordsCannotCorruptVisibleSaveState() {
        val result = enrichGamesWithSaveRecords(
            games,
            listOf(
                SaveRecordEntity("missing", "saves/missing.sav", 1L, 999L),
                SaveRecordEntity("alpha", "saves/alpha.sav", 2L, -4L)
            )
        )

        assertEquals(2, result.size)
        assertTrue(result[0].savePresent)
        assertEquals(0L, result[0].saveSizeBytes)
        assertFalse(result[1].savePresent)
    }
}
