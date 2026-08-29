package com.gamebox.os.ui

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test

class GameFilteringTest {
    private val games = listOf(
        Game(GameId("one"), "Star Racer", "Android", 2026, "Racing", 100, InstallState.INSTALLED, favorite = true),
        Game(GameId("two"), "Cave Quest", "Retro", 2004, "Adventure", 20, InstallState.NOT_INSTALLED),
        Game(GameId("three"), "Retro Racer", "Retro", 1999, "Racing", 30, InstallState.INSTALLED)
    )

    @Test
    fun searchMatchesTitlePlatformAndGenreIgnoringCase() {
        assertEquals(listOf("one", "three"), filterGames(games, "RACER", null, null, false).map { it.id.value })
        assertEquals(listOf("two", "three"), filterGames(games, "retro", null, null, false).map { it.id.value })
        assertEquals(listOf("two"), filterGames(games, "adventure", null, null, false).map { it.id.value })
    }

    @Test
    fun filtersComposeAndFavoritesRemainExplicit() {
        assertEquals(
            listOf("one"),
            filterGames(games, "", "Android", "Racing", favoritesOnly = true).map { it.id.value }
        )
        assertEquals(
            listOf("three"),
            filterGames(games, "", "Retro", "Racing", favoritesOnly = false).map { it.id.value }
        )
    }
}
