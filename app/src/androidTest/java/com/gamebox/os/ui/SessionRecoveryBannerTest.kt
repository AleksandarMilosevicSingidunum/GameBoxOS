package com.gamebox.os.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamebox.os.domain.Game
import com.gamebox.os.launch.GameLaunchController
import com.gamebox.os.launch.LaunchUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionRecoveryBannerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun coldStartErrorOffersRecoveryWithoutSelectedGame() {
        val state = MutableStateFlow(LaunchUiState(status = LaunchUiState.Status.SESSION_ERROR))
        var retries = 0
        val controller = object : GameLaunchController {
            override fun observeState() = state
            override fun launch(game: Game) = Unit
            override fun onHostResumed() { retries++; state.value = LaunchUiState() }
        }
        compose.setContent { MaterialTheme { SessionRecoveryBanner(controller) } }
        compose.onNodeWithText("Retry session recovery").performClick()
        compose.onNodeWithText("Retry session recovery").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
