package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.InstallState

enum class GameDetailSource { LIBRARY, DISCOVERY }

data class GameDetailPresentation(
    val id: String,
    val title: String,
    val platform: String,
    val releaseLabel: String?,
    val genre: String?,
    val playersLabel: String?,
    val region: String?,
    val ratingLabel: String?,
    val description: String,
    val coverUrl: String?,
    val heroUrl: String?,
    val screenshots: List<String>,
    val favorite: Boolean,
    val installed: Boolean,
    val source: GameDetailSource,
) {
    companion object {
        fun from(game: Game): GameDetailPresentation = GameDetailPresentation(
            id = game.id.value,
            title = game.title,
            platform = game.platform,
            releaseLabel = game.year.takeIf { it > 0 }?.toString(),
            genre = game.genre.takeIf(String::isNotBlank),
            playersLabel = game.players?.takeIf(String::isNotBlank)?.let(::formatPlayers),
            region = game.region?.takeIf(String::isNotBlank),
            ratingLabel = null,
            description = game.description?.takeIf(String::isNotBlank)
                ?: "No description is available for this title.",
            coverUrl = game.artworkUrl,
            heroUrl = game.artworkUrl,
            screenshots = emptyList(),
            favorite = game.favorite,
            installed = game.state in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE),
            source = GameDetailSource.LIBRARY,
        )

        fun from(game: DiscoveryGame, platformName: String): GameDetailPresentation =
            GameDetailPresentation(
                id = game.id.value,
                title = game.title,
                platform = platformName,
                releaseLabel = game.releaseDate?.takeIf(String::isNotBlank)?.take(4),
                genre = null,
                playersLabel = game.players?.takeIf(String::isNotBlank)?.let(::formatPlayers),
                region = game.region?.takeIf(String::isNotBlank),
                ratingLabel = game.rating?.let { rating ->
                    if (rating % 1.0 == 0.0) rating.toInt().toString() else "%.1f".format(rating)
                },
                description = game.description?.takeIf(String::isNotBlank)
                    ?: "Discover this title and import your own legal copy to play.",
                coverUrl = game.coverUrl,
                heroUrl = game.backgroundUrl ?: game.screenshots.firstOrNull() ?: game.coverUrl,
                screenshots = game.screenshots.distinct(),
                favorite = game.favorite,
                installed = false,
                source = GameDetailSource.DISCOVERY,
            )

        private fun formatPlayers(value: String): String =
            if (value.contains("player", ignoreCase = true)) value else "$value players"
    }
}
