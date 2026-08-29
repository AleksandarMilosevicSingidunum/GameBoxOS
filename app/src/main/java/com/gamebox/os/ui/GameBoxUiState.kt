package com.gamebox.os.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
internal class GameBoxUiState private constructor(
    destination: String,
    selectedGameId: String?,
    private val focusedByDestination: MutableMap<String, String>,
) {
    var destination: String by mutableStateOf(destination)
        private set
    var selectedGameId: String? by mutableStateOf(selectedGameId)
        private set

    fun openDestination(value: String) {
        require(value.isNotBlank()) { "Destination is required" }
        destination = value
        selectedGameId = null
    }

    fun openGame(gameId: String) {
        require(gameId.isNotBlank()) { "Game ID is required" }
        selectedGameId = gameId
    }

    fun clearSelection() { selectedGameId = null }

    fun rememberFocus(destination: String, gameId: String) {
        require(destination.isNotBlank() && gameId.isNotBlank())
        focusedByDestination[destination] = gameId
    }

    fun restoreFocus(destination: String, availableGameIds: Collection<String>): String? =
        focusedByDestination[destination]?.takeIf { it in availableGameIds }

    fun encode(): List<String> = buildList {
        add(destination)
        add(selectedGameId.orEmpty())
        focusedByDestination.toSortedMap().forEach { (destination, gameId) ->
            add(destination)
            add(gameId)
        }
    }

    companion object {
        fun create(): GameBoxUiState = GameBoxUiState("HOME", null, mutableMapOf())

        fun decode(values: List<String>): GameBoxUiState {
            if (values.size < 2 || (values.size - 2) % 2 != 0) return create()
            val destination = values[0].takeIf { it.isNotBlank() } ?: "HOME"
            val selected = values[1].takeIf { it.isNotBlank() }
            val focused = mutableMapOf<String, String>()
            values.drop(2).chunked(2).forEach { pair ->
                if (pair[0].isNotBlank() && pair[1].isNotBlank()) focused[pair[0]] = pair[1]
            }
            return GameBoxUiState(destination, selected, focused)
        }
    }
}

private val GameBoxUiStateSaver = listSaver<GameBoxUiState, String>(
    save = { state -> state.encode() },
    restore = { values -> GameBoxUiState.decode(values) },
)

@Composable
internal fun rememberGameBoxUiState(): GameBoxUiState =
    rememberSaveable(saver = GameBoxUiStateSaver) { GameBoxUiState.create() }
