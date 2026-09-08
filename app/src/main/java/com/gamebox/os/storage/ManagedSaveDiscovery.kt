package com.gamebox.os.storage

import com.gamebox.os.data.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Read-only discovery of GameBox-owned saves, not access to emulator-private data. */
class ManagedSaveDiscovery(
    private val games: GameRepository,
    adapter: SaveAdapter,
    scope: CoroutineScope,
) {
    private val inspection = SaveInspectionService(adapter)
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val results = MutableStateFlow<Map<String, SaveSummary>>(emptyMap())
    val state: StateFlow<Map<String, SaveSummary>> = results.asStateFlow()

    init {
        scope.launch { games.observeGames().collect { refresh() } }
        scope.launch {
            for (request in requests) {
                val snapshot = games.observeGames().value
                results.value = snapshot.associate { it.id.value to inspection.inspect(it.id.value) }
            }
        }
    }

    fun refresh() { requests.trySend(Unit) }
}
