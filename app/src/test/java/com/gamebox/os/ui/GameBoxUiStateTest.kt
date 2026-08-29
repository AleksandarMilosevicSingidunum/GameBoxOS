package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameBoxUiStateTest {
    @Test fun encodedStateRestoresDestinationSelectionAndPerTabFocus() {
        val state = GameBoxUiState.create()
        state.rememberFocus("HOME", "galaxy-patrol")
        state.rememberFocus("STORE", "freedoom")
        state.openDestination("STORE")
        state.openGame("freedoom")
        val restored = GameBoxUiState.decode(state.encode())
        assertEquals("STORE", restored.destination)
        assertEquals("freedoom", restored.selectedGameId)
        assertEquals("galaxy-patrol", restored.restoreFocus("HOME", listOf("galaxy-patrol")))
        assertEquals("freedoom", restored.restoreFocus("STORE", listOf("freedoom")))
    }

    @Test fun invalidOrUnavailableStateFallsBackSafely() {
        val restored = GameBoxUiState.decode(listOf("broken"))
        assertEquals("HOME", restored.destination)
        assertNull(restored.selectedGameId)
        val state = GameBoxUiState.create()
        state.rememberFocus("HOME", "removed-game")
        assertNull(state.restoreFocus("HOME", listOf("galaxy-patrol")))
    }
}
