package com.gamebox.os.ui

import com.gamebox.os.domain.GameId

class GameFocusMemory {
    private val focusedByDestination = mutableMapOf<String, GameId>()

    fun remember(destination: String, gameId: GameId) {
        require(destination.isNotBlank()) { "Destination is required" }
        focusedByDestination[destination] = gameId
    }

    fun restore(destination: String, availableGames: Collection<GameId>): GameId? {
        val remembered = focusedByDestination[destination] ?: return null
        return remembered.takeIf { it in availableGames }
    }

    fun restoreOrFirst(destination: String, availableGames: List<GameId>): GameId? =
        restore(destination, availableGames) ?: availableGames.firstOrNull()
}
