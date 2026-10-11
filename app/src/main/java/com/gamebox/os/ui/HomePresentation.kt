package com.gamebox.os.ui

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState

/** Data selected for the Home dashboard, kept independent of Compose rendering. */
internal data class HomePresentationState(
    val installedGames: List<Game>,
    val recentlyPlayed: List<Game>,
    val hero: Game,
    val focusTarget: GameId,
)

internal data class HomeSecondarySection(
    val title: String,
    val games: List<Game>,
)

/** Keeps the Continue Playing hero from appearing a second time in the row below it. */
internal fun homeSecondarySection(
    games: List<Game>,
    recentlyPlayed: List<Game>,
    hero: Game,
): HomeSecondarySection? {
    val otherRecentlyPlayed = recentlyPlayed.filterNot { it.id == hero.id }
    if (otherRecentlyPlayed.isNotEmpty()) {
        return HomeSecondarySection("Recently played", otherRecentlyPlayed)
    }

    val otherGames = games.filterNot { it.id == hero.id }
    return otherGames.takeIf { it.isNotEmpty() }?.let {
        HomeSecondarySection("Explore games", it)
    }
}

internal fun homePresentationState(
    games: List<Game>,
    restoreGameId: GameId?,
    compact: Boolean,
): HomePresentationState? {
    if (games.isEmpty()) return null

    val installed = games.filter {
        it.state == InstallState.INSTALLED || it.state == InstallState.UPDATE_AVAILABLE
    }
    val recentlyPlayed = installed
        .filter { it.lastPlayed != null }
        .sortedByDescending { it.lastPlayed }
    val hero = recentlyPlayed.firstOrNull()
        ?: installed.firstOrNull()
        ?: games.firstOrNull { it.id.value == "galaxy-patrol" }
        ?: games.first()
    val visibleGameIds = buildSet {
        add(hero.id)
        if (compact) addAll(installed.map(Game::id))
        else addAll(installed.take(8).map(Game::id))
        addAll(recentlyPlayed.map(Game::id))
        if (recentlyPlayed.isEmpty()) addAll(games.map(Game::id))
    }
    val focusTarget = restoreGameId?.takeIf(visibleGameIds::contains) ?: hero.id

    return HomePresentationState(
        installedGames = installed,
        recentlyPlayed = recentlyPlayed,
        hero = hero,
        focusTarget = focusTarget,
    )
}
