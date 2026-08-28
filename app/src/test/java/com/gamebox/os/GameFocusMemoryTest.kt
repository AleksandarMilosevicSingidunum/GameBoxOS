package com.gamebox.os

import com.gamebox.os.domain.GameId
import com.gamebox.os.ui.GameFocusMemory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameFocusMemoryTest {
    @Test fun focusIsRememberedIndependentlyPerTab() {
        val memory = GameFocusMemory()
        memory.remember("HOME", GameId("celeste"))
        memory.remember("STORE", GameId("retro-test"))

        assertEquals(GameId("celeste"), memory.restore("HOME", listOf(GameId("celeste"))))
        assertEquals(GameId("retro-test"), memory.restore("STORE", listOf(GameId("retro-test"))))
    }

    @Test fun missingCatalogEntryIsNeverRestored() {
        val memory = GameFocusMemory()
        memory.remember("LIBRARY", GameId("removed"))

        assertNull(memory.restore("LIBRARY", listOf(GameId("installed"))))
    }

    @Test fun unknownTabHasNoSyntheticFocusWhenStrictlyRestoring() {
        assertNull(GameFocusMemory().restore("HOME", listOf(GameId("celeste"))))
    }

    @Test fun firstVisitCanFocusFirstAvailableGame() {
        val games = listOf(GameId("first"), GameId("second"))

        assertEquals(GameId("first"), GameFocusMemory().restoreOrFirst("STORE", games))
        assertNull(GameFocusMemory().restoreOrFirst("STORE", emptyList()))
    }

    @Test fun removedRememberedGameFallsBackToFirstAvailable() {
        val memory = GameFocusMemory()
        memory.remember("LIBRARY", GameId("removed"))

        assertEquals(
            GameId("installed"),
            memory.restoreOrFirst("LIBRARY", listOf(GameId("installed")))
        )
    }
}
