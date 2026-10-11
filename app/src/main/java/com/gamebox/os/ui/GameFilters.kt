package com.gamebox.os.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.gamebox.os.domain.Game

internal data class GameFilterOptions(
    val platforms: List<String>,
    val genres: List<String>,
    val regions: List<String>,
    val languages: List<String>,
)

@Composable
internal fun GameFilterBar(
    games: List<Game>,
    query: String,
    onQuery: (String) -> Unit,
    platform: String?,
    onPlatform: (String?) -> Unit,
    genre: String?,
    onGenre: (String?) -> Unit,
    favoritesOnly: Boolean,
    onFavoritesOnly: (Boolean) -> Unit,
    showMetadataFilters: Boolean = false,
    extraRegions: List<String> = emptyList(),
    region: String? = null,
    onRegion: (String?) -> Unit = {},
    language: String? = null,
    onLanguage: (String?) -> Unit = {},
    searchFocusRequester: FocusRequester? = null,
) {
    val options = remember(games, extraRegions) { gameFilterOptions(games, extraRegions) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            label = { Text("Search games") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().then(
                searchFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
            ),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BlueprintFilterChip(selected = platform == null, onClick = { onPlatform(null) }, label = { Text("All platforms") })
            options.platforms.forEach { value ->
                BlueprintFilterChip(selected = platform == value, onClick = { onPlatform(value) }, label = { Text(value) })
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BlueprintFilterChip(
                selected = favoritesOnly,
                onClick = { onFavoritesOnly(!favoritesOnly) },
                label = { Text("Favorites") },
            )
            BlueprintFilterChip(selected = genre == null, onClick = { onGenre(null) }, label = { Text("All genres") })
            options.genres.forEach { value ->
                BlueprintFilterChip(selected = genre == value, onClick = { onGenre(value) }, label = { Text(value) })
            }
        }
        if (showMetadataFilters && (options.regions.isNotEmpty() || options.languages.isNotEmpty() || region != null || language != null)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (options.regions.isNotEmpty() || region != null) {
                    BlueprintFilterChip(selected = region == null, onClick = { onRegion(null) }, label = { Text("All regions") })
                    options.regions.forEach { value ->
                        BlueprintFilterChip(selected = region == value, onClick = { onRegion(value) }, label = { Text(value) })
                    }
                }
                if (options.languages.isNotEmpty() || language != null) {
                    BlueprintFilterChip(selected = language == null, onClick = { onLanguage(null) }, label = { Text("All languages") })
                    options.languages.forEach { value ->
                        BlueprintFilterChip(selected = language == value, onClick = { onLanguage(value) }, label = { Text(value) })
                    }
                }
            }
        }
    }
}

internal fun gameFilterOptions(
    games: List<Game>,
    extraRegions: List<String> = emptyList(),
): GameFilterOptions {
    val platforms = linkedSetOf<String>()
    val genres = linkedSetOf<String>()
    val regions = mutableListOf<String>()
    val languages = linkedSetOf<String>()
    games.forEach { game ->
        platforms += game.platform
        genres += game.genre
        game.region?.let(regions::add)
        game.language?.takeIf(String::isNotBlank)?.let(languages::add)
    }
    regions += extraRegions
    return GameFilterOptions(
        platforms = platforms.sorted(),
        genres = genres.sorted(),
        regions = metadataFilterOptions(regions),
        languages = languages.sorted(),
    )
}

internal fun configuredSourceDetailFailureMessage(sourceName: String): String =
    "$sourceName result found, but extra details could not be loaded. You can still use Download to open the source, or import a copy you own."

internal fun configuredSourceNotFoundMessage(sourceName: String): String =
    "$sourceName details could not be loaded in GameBox. You can still open the source page, or import a copy you own."

internal fun filterGames(
    games: List<Game>,
    query: String,
    platform: String?,
    genre: String?,
    favoritesOnly: Boolean,
    region: String? = null,
    language: String? = null,
): List<Game> {
    val normalized = query.trim()
    return games.filter { game ->
        (normalized.isEmpty() ||
            game.title.contains(normalized, ignoreCase = true) ||
            game.platform.contains(normalized, ignoreCase = true) ||
            game.genre.contains(normalized, ignoreCase = true)) &&
            (platform == null || game.platform == platform) &&
            matchesStoreMetadataFilters(
                game.genre, game.region, game.language,
                genre, region, language,
            ) &&
            (!favoritesOnly || game.favorite)
    }
}
