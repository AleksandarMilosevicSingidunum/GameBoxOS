package com.gamebox.os.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gamebox.os.domain.GameId

/** Keeps Store's console and selected discovery detail as one coherent navigation state. */
@Stable
internal class StoreSelectionState internal constructor(
    consoleKey: String?,
    discoveryId: GameId?,
) {
    var consoleKey by mutableStateOf(consoleKey)
        private set
    var discoveryId by mutableStateOf(discoveryId)
        private set

    fun selectConsole(key: String?) {
        if (key != consoleKey) {
            consoleKey = key
            discoveryId = null
        }
    }

    fun selectDiscovery(id: GameId) {
        discoveryId = id
    }

    fun clearDiscovery() {
        discoveryId = null
    }
}

@Composable
internal fun rememberStoreSelectionState(uiState: GameBoxUiState): StoreSelectionState {
    val selection = remember(uiState) {
        StoreSelectionState(
            consoleKey = uiState.screenValue(STORE_CONSOLE_STATE_KEY),
            discoveryId = uiState.screenValue(STORE_DISCOVERY_STATE_KEY)?.let(::GameId),
        )
    }
    LaunchedEffect(uiState, selection.consoleKey, selection.discoveryId) {
        uiState.rememberScreenValue(STORE_CONSOLE_STATE_KEY, selection.consoleKey)
        uiState.rememberScreenValue(STORE_DISCOVERY_STATE_KEY, selection.discoveryId?.value)
    }
    return selection
}

private const val STORE_CONSOLE_STATE_KEY = "store.console"
private const val STORE_DISCOVERY_STATE_KEY = "store.discovery"
