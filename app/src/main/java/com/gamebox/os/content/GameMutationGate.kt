package com.gamebox.os.content

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide per-game mutation serialization for production file/database workflows.
 *
 * Keep the critical section around a complete user-visible mutation (for example,
 * replace imported files + publish the matching library record), not just one file move.
 */
object GameMutationGate {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun <T> withGameLock(gameId: String, block: suspend () -> T): T {
        require(gameId.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,95}"))) {
            "Invalid game ID"
        }
        val mutex = locks.computeIfAbsent(gameId) { Mutex() }
        return mutex.withLock { block() }
    }
}
