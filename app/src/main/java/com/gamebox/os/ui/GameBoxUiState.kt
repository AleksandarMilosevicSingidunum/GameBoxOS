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
    private val focusIndexByDestination: MutableMap<String, Int>,
    private val screenValues: MutableMap<String, String>,
) {
    var destination: String by mutableStateOf(destination)
        private set
    var selectedGameId: String? by mutableStateOf(selectedGameId)
        private set

    fun openDestination(value: String) {
        require(value.isNotBlank()) { "Destination is required" }
        if (value != Destination.SETTINGS.name) rememberScreenValue(SETTINGS_SECTION_REQUEST, null)
        destination = value
        selectedGameId = null
    }

    fun openSettings(section: SettingsSection) {
        rememberScreenValue(SETTINGS_SECTION_REQUEST, section.name)
        openDestination(Destination.SETTINGS.name)
    }

    fun requestedSettingsSection(): SettingsSection? =
        screenValue(SETTINGS_SECTION_REQUEST)?.let { name ->
            SettingsSection.entries.firstOrNull { it.name == name }
        }

    fun consumeSettingsSectionRequest(section: SettingsSection) {
        if (requestedSettingsSection() == section) rememberScreenValue(SETTINGS_SECTION_REQUEST, null)
    }

    fun openGame(gameId: String) {
        require(gameId.isNotBlank()) { "Game ID is required" }
        selectedGameId = gameId
    }

    fun clearSelection() { selectedGameId = null }

    fun rememberFocus(destination: String, gameId: String, orderedGameIds: List<String> = emptyList()) {
        require(destination.isNotBlank() && gameId.isNotBlank())
        focusedByDestination[destination] = gameId
        val index = orderedGameIds.indexOf(gameId)
        if (index >= 0) focusIndexByDestination[destination] = index
    }

    /**
     * Restores the exact card when it still exists. If filtering, uninstalling, or a
     * catalog refresh removed it, focus moves to the card occupying its prior position,
     * clamped to the previous neighbor at the end of a row.
     */
    fun restoreFocus(destination: String, availableGameIds: Collection<String>): String? {
        val ordered = availableGameIds.toList()
        if (ordered.isEmpty()) return null
        focusedByDestination[destination]?.takeIf { it in ordered }?.let { return it }
        val priorIndex = focusIndexByDestination[destination] ?: return null
        val replacementIndex = priorIndex.coerceIn(0, ordered.lastIndex)
        return ordered[replacementIndex].also { replacement ->
            focusedByDestination[destination] = replacement
            focusIndexByDestination[destination] = replacementIndex
        }
    }

    fun screenValue(key: String): String? = screenValues[key]

    fun requestStoreVimmSearch() = rememberScreenValue(STORE_VIMM_SEARCH_REQUEST, "true")

    fun hasStoreVimmSearchRequest(): Boolean =
        screenValue(STORE_VIMM_SEARCH_REQUEST) == "true"

    fun consumeStoreVimmSearchRequest() = rememberScreenValue(STORE_VIMM_SEARCH_REQUEST, null)

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
        focusIndexByDestination.toSortedMap().forEach { (destination, index) ->
            add("focusIndex:" + destination)
            add(index.toString())
        }
        screenValues.toSortedMap().forEach { (key, value) ->
            add("state:" + key)
            add(value)
        }
    }

    companion object {
        private const val SETTINGS_SECTION_REQUEST = "navigation.settingsSectionRequest"

        fun create(): GameBoxUiState =
            GameBoxUiState("HOME", null, mutableMapOf(), mutableMapOf(), mutableMapOf())

        fun decode(values: List<String>): GameBoxUiState {
            if (values.size < 2 || (values.size - 2) % 2 != 0) return create()
            val destination = values[0].takeIf { it.isNotBlank() } ?: "HOME"
            val selected = values[1].takeIf { it.isNotBlank() }
            val focused = mutableMapOf<String, String>()
            val focusIndices = mutableMapOf<String, Int>()
            val screenValues = mutableMapOf<String, String>()
            values.drop(2).chunked(2).forEach { pair ->
                val key = pair[0]
                val value = pair[1]
                if (key.isBlank() || value.isBlank()) return@forEach
                when {
                    key.startsWith("focusIndex:") -> value.toIntOrNull()?.takeIf { it >= 0 }?.let {
                        focusIndices[key.removePrefix("focusIndex:")] = it
                    }
                    key.startsWith("focus:") -> focused[key.removePrefix("focus:")] = value
                    key.startsWith("state:") -> screenValues[key.removePrefix("state:")] = value
                    else -> focused[key] = value // Backward-compatible with the original saver.
                }
            }
            return GameBoxUiState(destination, selected, focused, focusIndices, screenValues)
        }
    }
}

private const val STORE_VIMM_SEARCH_REQUEST = "store.vimmSearchRequested"

private val GameBoxUiStateSaver = listSaver<GameBoxUiState, String>(
    save = { state -> state.encode() },
    restore = { values -> GameBoxUiState.decode(values) },
)

@Composable
internal fun rememberGameBoxUiState(): GameBoxUiState =
    rememberSaveable(saver = GameBoxUiStateSaver) { GameBoxUiState.create() }
