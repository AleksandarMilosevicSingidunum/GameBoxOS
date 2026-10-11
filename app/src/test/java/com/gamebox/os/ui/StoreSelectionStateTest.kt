package com.gamebox.os.ui

import com.gamebox.os.domain.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreSelectionStateTest {
    @Test
    fun changingConsoleClearsTheSelectedDiscoveryDetail() {
        val state = StoreSelectionState("psp", GameId("discovery-game"))

        state.selectConsole("ps2")

        assertEquals("ps2", state.consoleKey)
        assertNull(state.discoveryId)
    }

    @Test
    fun reselectingCurrentConsolePreservesDiscoveryDetail() {
        val discoveryId = GameId("discovery-game")
        val state = StoreSelectionState("psp", discoveryId)

        state.selectConsole("psp")

        assertEquals("psp", state.consoleKey)
        assertEquals(discoveryId, state.discoveryId)
    }

    @Test
    fun clearingOrSelectingDiscoveryUpdatesTheSharedSelection() {
        val state = StoreSelectionState(null, null)
        val discoveryId = GameId("discovery-game")

        state.selectDiscovery(discoveryId)
        assertEquals(discoveryId, state.discoveryId)
        state.clearDiscovery()
        assertNull(state.discoveryId)
    }
}
