package com.gamebox.os.ui

import androidx.compose.runtime.Composable
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.data.GameRepository
import com.gamebox.os.importer.AuthorizedRomImporter
import com.gamebox.os.source.DiscoverySourceGame
import com.gamebox.os.source.GameSourceConfig
import com.gamebox.os.source.GameSourceProviderType

/** Renders a configured-source result without pulling its route into Store orchestration. */
@Composable
internal fun ConfiguredDiscoveryDetailsRoute(
    result: DiscoverySourceGame,
    allDiscoveryGames: List<DiscoveryGame>,
    gameSources: List<GameSourceConfig>,
    onBack: () -> Unit,
    sourceNotice: String?,
    importer: AuthorizedRomImporter,
    repository: GameRepository,
    onOpenDownloads: () -> Unit = {},
) {
    val source = gameSources.firstOrNull { it.id.equals(result.sourceId, ignoreCase = true) }
    val directSource = result.detailsUrl?.let { url ->
        ConfiguredSourceLink(
            sourceId = result.sourceId,
            label = source?.name ?: "Configured source",
            url = url,
            tracksBrowserDownload = source?.type == GameSourceProviderType.VIMM_LAIR,
        )
    }
    DiscoveryDetailsScreen(
        game = configuredSourceResultToDiscoveryGame(result, allDiscoveryGames),
        platformName = result.platform,
        onBack = onBack,
        onFavorite = {},
        favoriteEnabled = false,
        directSource = directSource,
        sourceNotice = sourceNotice,
        importer = importer,
        repository = repository,
        gameSources = gameSources,
        onOpenDownloads = onOpenDownloads,
    )
}

/** Renders an in-catalog discovery result with its favorite and back transitions. */
@Composable
internal fun DiscoveryCatalogDetailsRoute(
    game: DiscoveryGame,
    platformName: String,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    importer: AuthorizedRomImporter,
    repository: GameRepository,
    gameSources: List<GameSourceConfig>,
    onOpenDownloads: () -> Unit = {},
) {
    DiscoveryDetailsScreen(
        game = game,
        platformName = platformName,
        onBack = onBack,
        onFavorite = onFavorite,
        importer = importer,
        repository = repository,
        gameSources = gameSources,
        onOpenDownloads = onOpenDownloads,
    )
}
