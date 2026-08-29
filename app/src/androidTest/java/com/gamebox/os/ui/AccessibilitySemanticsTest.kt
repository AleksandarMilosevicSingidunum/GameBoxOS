package com.gamebox.os.ui

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.DownloadStatus
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilitySemanticsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun gameCardExposesUsefulSpokenDescriptionAndClickAction() {
        val game = Game(GameId("galaxy-patrol"), "Galaxy Patrol", "Retro", 2018, "Arcade", 1,
            InstallState.INSTALLED, favorite = true)
        composeRule.setContent {
            GameCard(game, Modifier.size(320.dp, 180.dp), hero = true, onClick = {})
        }

        composeRule
            .onNodeWithContentDescription("Galaxy Patrol, Retro, installed, favorite, continue playing")
            .assertHasClickAction()
    }

    @Test
    fun downloadProgressExposesRangeAndSpokenPercentage() {
        val job = DownloadJob("job", GameId("game"), "Galaxy Patrol", DownloadStatus.DOWNLOADING, 100, 50)
        composeRule.setContent { DownloadProgressIndicator(job) }

        composeRule.onNodeWithContentDescription("Download progress for Galaxy Patrol")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "50 percent downloaded"))
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(0.5f, 0f..1f),
            ))
    }
}
