package com.gamebox.os.ui

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test

class GameFilterOptionsTest {
    @Test
    fun aggregatesSortedOptionsAndKeepsMetadataRegionSemantics() {
        val games = listOf(
            game("First", "PSP", "Action", region = "NA", language = "en"),
            game("Second", "PS2", "Adventure", region = "EU", language = "fr"),
            game("Third", "PSP", "Action", region = "na", language = "en"),
        )

        assertEquals(
            GameFilterOptions(
                platforms = listOf("PS2", "PSP"),
                genres = listOf("Action", "Adventure"),
                regions = listOf("APAC", "EU", "NA"),
                languages = listOf("en", "fr"),
            ),
            gameFilterOptions(games, extraRegions = listOf("APAC", "EU", " ")),
        )
    }

    private fun game(
        title: String,
        platform: String,
        genre: String,
        region: String,
        language: String,
    ) = Game(
        id = GameId(title.lowercase()),
        title = title,
        platform = platform,
        year = 2026,
        genre = genre,
        sizeMb = 1,
        state = InstallState.NOT_INSTALLED,
        region = region,
        language = language,
    )
}
