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
        state.rememberScreenValue("store.query", "racing")
        state.rememberScreenValue("store.console", "ps2")
        state.rememberScreenValue("store.favorites", "true")
        val restored = GameBoxUiState.decode(state.encode())
        assertEquals("STORE", restored.destination)
        assertEquals("freedoom", restored.selectedGameId)
        assertEquals("galaxy-patrol", restored.restoreFocus("HOME", listOf("galaxy-patrol")))
        assertEquals("freedoom", restored.restoreFocus("STORE", listOf("freedoom")))
        assertEquals("racing", restored.screenValue("store.query"))
        assertEquals("ps2", restored.screenValue("store.console"))
        assertEquals("true", restored.screenValue("store.favorites"))
    }

    @Test fun clearedScreenValuesAreNotRestoredAndLegacyFocusStillDecodes() {
        val state = GameBoxUiState.create()
        state.rememberScreenValue("library.query", "platformer")
        state.rememberScreenValue("library.query", null)
        assertNull(GameBoxUiState.decode(state.encode()).screenValue("library.query"))

        val legacy = GameBoxUiState.decode(listOf("LIBRARY", "", "LIBRARY", "galaxy-patrol"))
        assertEquals("galaxy-patrol", legacy.restoreFocus("LIBRARY", listOf("galaxy-patrol")))
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
