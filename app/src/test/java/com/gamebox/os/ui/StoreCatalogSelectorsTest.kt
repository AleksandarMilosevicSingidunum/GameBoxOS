package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreCatalogSelectorsTest {
    @Test
    fun initialStoreRailFocusDoesNotOverrideAnExplicitSearchRequest() {
        assertEquals(
            true,
            shouldAutofocusCatalogRail(Destination.STORE.name, compact = false, hostWindowHasFocus = true, searchFocusRequested = false),
        )
        assertEquals(
            false,
            shouldAutofocusCatalogRail(Destination.STORE.name, compact = false, hostWindowHasFocus = true, searchFocusRequested = true),
        )
        assertEquals(
            false,
            shouldAutofocusCatalogRail(Destination.STORE.name, compact = true, hostWindowHasFocus = true, searchFocusRequested = false),
        )
        assertEquals(
            false,
            shouldAutofocusCatalogRail(Destination.LIBRARY.name, compact = false, hostWindowHasFocus = true, searchFocusRequested = false),
        )
    }

    @Test
    fun authorizedSelectorAppliesFavoriteInstallAndMetadataFiltersBeforeSorting() {
        val games = listOf(
            game("Zulu", state = InstallState.INSTALLED, favorite = true, region = "NA", language = "en"),
            game("Alpha", state = InstallState.INSTALLED, favorite = true, region = "NA", language = "en"),
            game("Beta", state = InstallState.UPDATE_AVAILABLE, favorite = true, region = "EU", language = "en"),
            game("Gamma", state = InstallState.INSTALLED, favorite = false, region = "NA", language = "en"),
        )

        val visible = selectVisibleAuthorizedStoreGames(
            games = games,
            favoritesOnly = true,
            installedOnly = true,
            genreFilter = "Action",
            regionFilter = "NA",
            languageFilter = "en",
        )

        assertEquals(listOf("Alpha", "Zulu"), visible.map { it.title })
    }

    @Test
    fun discoverySelectorFiltersFavoritesAndSortsByRatingOrTitle() {
        val games = listOf(
            discovery("Zulu", rating = 8.0, favorite = true),
            discovery("Alpha", rating = 9.5, favorite = false),
            discovery("Beta", rating = null, favorite = true),
        )

        assertEquals(
            listOf("Zulu", "Beta"),
            selectVisibleDiscoveryStoreGames(
                games, favoritesOnly = true, sortByRating = true,
                genreFilter = null, regionFilter = null, languageFilter = null,
            ).map { it.title },
        )
        assertEquals(
            listOf("Alpha", "Beta", "Zulu"),
            selectVisibleDiscoveryStoreGames(
                games, favoritesOnly = false, sortByRating = false,
                genreFilter = null, regionFilter = null, languageFilter = null,
            ).map { it.title },
        )
    }

    private fun game(
        title: String,
        state: InstallState = InstallState.NOT_INSTALLED,
        favorite: Boolean = false,
        region: String? = null,
        language: String? = null,
    ) = Game(
        id = GameId(title.lowercase()),
        title = title,
        platform = "PSP",
        year = 2020,
        genre = "Action",
        sizeMb = 1,
        state = state,
        favorite = favorite,
        region = region,
        language = language,
    )

    private fun discovery(
        title: String,
        rating: Double?,
        favorite: Boolean,
    ) = DiscoveryGame(
        id = GameId(title.lowercase()),
        title = title,
        platformId = "psp",
        region = "NA",
        releaseDate = null,
        description = null,
        players = null,
        rating = rating,
        coverUrl = null,
        backgroundUrl = null,
        logoUrl = null,
        screenshots = emptyList(),
        favorite = favorite,
    )
}
