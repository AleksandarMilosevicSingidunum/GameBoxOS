package com.gamebox.os.ui

import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailControllerActionPlanTest {
    @Test
    fun installedGameOffersPlayAndFavorite() {
        assertEquals(
            DetailControllerActionPlan(
                "Play",
                DetailControllerPrimaryAction.PRIMARY,
                "Favorite",
            ),
            plan(InstallState.INSTALLED),
        )
    }

    @Test
    fun activeWorkOpensDownloads() {
        listOf(
            InstallState.QUEUED,
            InstallState.DOWNLOADING,
            InstallState.VERIFYING,
            InstallState.INSTALLING,
        ).forEach { state ->
            assertEquals(
                DetailControllerPrimaryAction.OPEN_DOWNLOADS,
                plan(state).xAction,
            )
        }
    }

    @Test
    fun authorizedFixtureWorkerOpensDownloadsInsteadOfStartingDuplicateWork() {
        val result = detailControllerActionPlan(
            state = InstallState.NOT_INSTALLED,
            favorite = false,
            authorizedFixture = true,
            workerActive = true,
            hasRemoteSource = false,
        )

        assertEquals("View download", result.xLabel)
        assertEquals(DetailControllerPrimaryAction.OPEN_DOWNLOADS, result.xAction)
    }

    @Test
    fun missingSourceOpensGameSettingsInsteadOfPretendingToInstall() {
        val result = plan(
            state = InstallState.MISSING_FILES,
            hasRemoteSource = false,
        )

        assertEquals("Game settings", result.xLabel)
        assertEquals(DetailControllerPrimaryAction.OPEN_GAME_SETTINGS, result.xAction)
    }

    @Test
    fun pausedRemoteGameOffersResume() {
        val result = plan(InstallState.PAUSED)

        assertEquals("Resume", result.xLabel)
        assertEquals(DetailControllerPrimaryAction.PRIMARY, result.xAction)
    }

    @Test
    fun favoriteStateChangesTheYLabel() {
        assertEquals(
            "Unfavorite",
            detailControllerActionPlan(
                state = InstallState.INSTALLED,
                favorite = true,
                authorizedFixture = false,
                workerActive = false,
                hasRemoteSource = true,
            ).yLabel,
        )
    }

    private fun plan(
        state: InstallState,
        hasRemoteSource: Boolean = true,
    ) = detailControllerActionPlan(
        state = state,
        favorite = false,
        authorizedFixture = false,
        workerActive = false,
        hasRemoteSource = hasRemoteSource,
    )
}
