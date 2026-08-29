package com.gamebox.os.domain

data class LibrarySummary(
    val totalGames: Int,
    val installedGames: Int,
    val favorites: Int,
    val totalMinutesPlayed: Int,
    val resumeGame: Game?
) {
    val totalHoursPlayed: Int
        get() = totalMinutesPlayed / 60
    val remainingMinutes: Int
        get() = totalMinutesPlayed % 60
}

fun summarizeLibrary(games: List<Game>): LibrarySummary {
    val playable = games.filter {
        it.state == InstallState.INSTALLED || it.state == InstallState.UPDATE_AVAILABLE
    }
    return LibrarySummary(
        totalGames = games.size,
        installedGames = playable.size,
        favorites = games.count { it.favorite },
        totalMinutesPlayed = games.sumOf { it.minutesPlayed.coerceAtLeast(0) },
        resumeGame = games.filter { it.lastPlayed != null }.maxByOrNull { it.lastPlayed.orEmpty() }
    )
}
