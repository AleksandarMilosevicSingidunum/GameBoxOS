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
    private val screenValues: MutableMap<String, String>,
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

    fun screenValue(key: String): String? = screenValues[key]

    fun rememberScreenValue(key: String, value: String?) {
        require(key.isNotBlank())
        if (value.isNullOrEmpty()) screenValues.remove(key) else screenValues[key] = value
    }

    fun encode(): List<String> = buildList {
        add(destination)
        add(selectedGameId.orEmpty())
        focusedByDestination.toSortedMap().forEach { (destination, gameId) ->
            add("focus:" + destination)
            add(gameId)
        }
        screenValues.toSortedMap().forEach { (key, value) ->
            add("state:" + key)
            add(value)
        }
    }

    companion object {
        fun create(): GameBoxUiState = GameBoxUiState("HOME", null, mutableMapOf(), mutableMapOf())

        fun decode(values: List<String>): GameBoxUiState {
            if (values.size < 2 || (values.size - 2) % 2 != 0) return create()
            val destination = values[0].takeIf { it.isNotBlank() } ?: "HOME"
            val selected = values[1].takeIf { it.isNotBlank() }
            val focused = mutableMapOf<String, String>()
            val screenValues = mutableMapOf<String, String>()
            values.drop(2).chunked(2).forEach { pair ->
                val key = pair[0]
                val value = pair[1]
                if (key.isBlank() || value.isBlank()) return@forEach
                when {
                    key.startsWith("focus:") -> focused[key.removePrefix("focus:")] = value
                    key.startsWith("state:") -> screenValues[key.removePrefix("state:")] = value
                    else -> focused[key] = value // Backward-compatible with the original saver.
                }
            }
            return GameBoxUiState(destination, selected, focused, screenValues)
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
