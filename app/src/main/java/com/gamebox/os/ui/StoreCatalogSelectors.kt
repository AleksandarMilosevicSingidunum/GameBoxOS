package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.InstallState
import java.util.Locale

internal fun shouldAutofocusCatalogRail(
    destination: String,
    compact: Boolean,
    hostWindowHasFocus: Boolean,
    searchFocusRequested: Boolean,
): Boolean = destination == Destination.STORE.name &&
    !compact && hostWindowHasFocus && !searchFocusRequested

internal fun matchesStoreMetadataFilters(
    genre: String?,
    region: String?,
    language: String?,
    genreFilter: String?,
    regionFilter: String?,
    languageFilter: String?,
): Boolean =
    (genreFilter == null || genre.equals(genreFilter, ignoreCase = true)) &&
        (regionFilter == null || region.equals(regionFilter, ignoreCase = true)) &&
        (languageFilter == null || language.equals(languageFilter, ignoreCase = true))

/** Pure Store-list selectors, kept outside Compose so filtering semantics stay directly testable. */
internal fun selectVisibleAuthorizedStoreGames(
    games: List<Game>,
    favoritesOnly: Boolean,
    installedOnly: Boolean,
    genreFilter: String?,
    regionFilter: String?,
    languageFilter: String?,
): List<Game> = games.filter { game ->
    (!favoritesOnly || game.favorite) &&
        (!installedOnly || game.state == InstallState.INSTALLED || game.state == InstallState.UPDATE_AVAILABLE) &&
        matchesStoreMetadataFilters(
            game.genre, game.region, game.language,
            genreFilter, regionFilter, languageFilter,
        )
}.sortedBy { it.title.lowercase(Locale.ROOT) }

/** Discovery records have rating and region data, but no provider genre or language fields. */
internal fun selectVisibleDiscoveryStoreGames(
    games: List<DiscoveryGame>,
    favoritesOnly: Boolean,
    sortByRating: Boolean,
    genreFilter: String?,
    regionFilter: String?,
    languageFilter: String?,
): List<DiscoveryGame> = games.filter { game ->
    (!favoritesOnly || game.favorite) &&
        matchesStoreMetadataFilters(
            genre = null,
            region = game.region,
            language = null,
            genreFilter,
            regionFilter,
            languageFilter,
        )
}.let { visible ->
    if (sortByRating) visible.sortedByDescending { it.rating ?: -1.0 }
    else visible.sortedBy { it.title.lowercase(Locale.ROOT) }
}
