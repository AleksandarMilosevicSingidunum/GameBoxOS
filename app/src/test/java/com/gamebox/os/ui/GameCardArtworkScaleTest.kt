package com.gamebox.os.ui

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test

class GameCardArtworkScaleTest {
    @Test
    fun regularLibraryAndCollectionCardsShowTheWholeBoxCover() {
        assertEquals(ContentScale.Fit, gameCardArtworkScale(hero = false, poster = true))
    }

    @Test
    fun wideHomeHeroStillUsesImmersiveCroppedArtwork() {
        assertEquals(ContentScale.Crop, gameCardArtworkScale(hero = true, poster = false))
    }

    @Test
    fun longTitlesGetAnExtraLineOnWideTvCardsWithoutChangingSmallCards() {
        assertEquals(3, gameCardTitleMaxLines(116.dp, poster = true))
        assertEquals(3, gameCardTitleMaxLines(198.dp, poster = false))
        assertEquals(2, gameCardTitleMaxLines(108.dp, poster = true))
        assertEquals(2, gameCardTitleMaxLines(300.dp, poster = false))
    }

    @Test
    fun landscapeLibraryCardsUseACoverSizedRailAndAmbientBackground() {
        assertEquals(ContentScale.Crop, gameCardArtworkScale(hero = false, poster = false))
        assertEquals(65.72.dp, gameCardCoverRailWidth(106.dp))
        assertEquals(54.dp, gameCardCoverRailWidth(40.dp))
        assertEquals(72.dp, gameCardCoverRailWidth(160.dp))
    }

    @Test
    fun posterSubtitleShowsConsoleWithoutRepeatingAvailabilityBadge() {
        assertEquals("DS", gameCardPosterSubtitle(game(InstallState.INSTALLED)))
        assertEquals("DS", gameCardPosterSubtitle(game(InstallState.UPDATE_AVAILABLE)))
        assertEquals("DS", gameCardPosterSubtitle(game(InstallState.NOT_INSTALLED)))
        assertEquals("DS", gameCardPosterSubtitle(game(InstallState.MISSING_FILES)))
    }

    @Test
    fun cardConsoleLabelsUseKnownShortNamesAndPreserveUnknownPlatforms() {
        assertEquals("GameCube", gameCardConsoleLabel("Nintendo GameCube"))
        assertEquals("GBA", gameCardConsoleLabel("Nintendo Game Boy Advance"))
        assertEquals("GBC", gameCardConsoleLabel("Nintendo Game Boy Color"))
        assertEquals("PS1", gameCardConsoleLabel("Sony Playstation"))
        assertEquals("Custom system", gameCardConsoleLabel("Custom system"))
    }

    @Test
    fun posterSubtitleAddsCompactTrackedPlaytimeWhenAvailable() {
        assertEquals(
            "DS · 5m",
            gameCardPosterSubtitle(game(InstallState.INSTALLED).copy(minutesPlayed = 5)),
        )
        assertEquals(
            "DS · 1h 5m",
            gameCardPosterSubtitle(game(InstallState.INSTALLED).copy(minutesPlayed = 65)),
        )
        assertEquals(
            "DS · 12h",
            gameCardPosterSubtitle(game(InstallState.INSTALLED).copy(minutesPlayed = 720)),
        )
        assertEquals("0m", formatCompactPlaytime(-1))
    }

    @Test
    fun landscapeCardSubtitleShowsPlatformInstallStateAndPlaytime() {
        assertEquals(
            "DS · installed · 2h 5m",
            gameCardSubtitle(game(InstallState.INSTALLED).copy(minutesPlayed = 125)),
        )
    }

    @Test
    fun tvLibraryPosterTilesUseFourAcrossAvailableWidthWithReasonableBounds() {
        assertEquals(190.dp, libraryPosterTileWidth(840.dp))
        assertEquals(152.dp, libraryPosterTileWidth(640.dp))
        assertEquals(108.dp, libraryPosterTileWidth(360.dp))
        assertEquals(190.dp, libraryPosterTileHeight(190.dp))
    }

    private fun game(state: InstallState) = Game(
        id = GameId("poster-subtitle-test"),
        title = "Test game",
        platform = "Nintendo DS",
        year = 2000,
        genre = "Adventure",
        sizeMb = 1,
        state = state,
    )
}
