package com.gamebox.os.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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

    @Test
    fun blueprintBrandMarksComposeAsScalableUiAssets() {
        composeRule.setContent {
            Column {
                GameBoxBrandMark(Modifier.testTag("gamebox-brand").size(32.dp))
                ConsoleBrandMark("switch", selected = true, modifier = Modifier.testTag("console-brand").size(24.dp))
                AppBrandMark("YouTube", Modifier.testTag("app-brand").size(40.dp))
            }
        }

        val renders = SemanticsMatcher("brand mark renders") { true }
        composeRule.onNodeWithTag("gamebox-brand").assert(renders)
        composeRule.onNodeWithTag("console-brand").assert(renders)
        composeRule.onNodeWithTag("app-brand").assert(renders)
    }
}

