package com.gamebox.os.launch

data class RecoveredLaunchSession(val gameId: String, val launchConfirmed: Boolean, val minutesAway: Int)

/** Implementations must finish history and remove the pending record atomically. */
interface LaunchSessionJournal {
    suspend fun begin(gameId: String): String
    suspend fun confirm(ticket: String)
    suspend fun abandon(ticket: String)
    suspend fun finish(): RecoveredLaunchSession?
}

internal fun sessionMinutesAway(startedAt: Long, endedAt: Long): Int {
    if (startedAt < 0 || endedAt <= startedAt) return 0
    return ((endedAt - startedAt) / 60_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
