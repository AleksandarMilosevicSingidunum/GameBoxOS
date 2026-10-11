package com.gamebox.os.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gamebox.os.domain.Game

@Stable
internal class RememberedGameFilters internal constructor(
    query: String,
    platform: String?,
    genre: String?,
    region: String?,
    language: String?,
    favoritesOnly: Boolean,
) {
    var query by mutableStateOf(query)
    var platform by mutableStateOf(platform)
    var genre by mutableStateOf(genre)
    var region by mutableStateOf(region)
    var language by mutableStateOf(language)
    var favoritesOnly by mutableStateOf(favoritesOnly)
}

@Composable
internal fun rememberGameFilters(
    uiState: GameBoxUiState,
    screenKey: String,
): RememberedGameFilters {
    val filters = remember(uiState, screenKey) {
        RememberedGameFilters(
            query = uiState.screenValue("$screenKey.query").orEmpty(),
            platform = uiState.screenValue("$screenKey.platform"),
            genre = uiState.screenValue("$screenKey.genre"),
            region = uiState.screenValue("$screenKey.region"),
            language = uiState.screenValue("$screenKey.language"),
            favoritesOnly = uiState.screenValue("$screenKey.favorites") == "true",
        )
    }
    LaunchedEffect(
        uiState,
        screenKey,
        filters.query,
        filters.platform,
        filters.genre,
        filters.region,
        filters.language,
        filters.favoritesOnly,
    ) {
        uiState.rememberScreenValue("$screenKey.query", filters.query)
        uiState.rememberScreenValue("$screenKey.platform", filters.platform)
        uiState.rememberScreenValue("$screenKey.genre", filters.genre)
        uiState.rememberScreenValue("$screenKey.region", filters.region)
        uiState.rememberScreenValue("$screenKey.language", filters.language)
        uiState.rememberScreenValue("$screenKey.favorites", filters.favoritesOnly.toString())
    }
    return filters
}

@Composable
internal fun rememberFilteredGames(
    games: List<Game>,
    filters: RememberedGameFilters,
): List<Game> = remember(
    games,
    filters.query,
    filters.platform,
    filters.genre,
    filters.favoritesOnly,
    filters.region,
    filters.language,
) {
    filterGames(
        games = games,
        query = filters.query,
        platform = filters.platform,
        genre = filters.genre,
        favoritesOnly = filters.favoritesOnly,
        region = filters.region,
        language = filters.language,
    )
}
