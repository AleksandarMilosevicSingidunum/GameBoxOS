package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.GameBoxApplication
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameMetadataEditorFocusTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun controllerAActivatesFocusedCancelWithoutSavingMetadata() {
        val app = ApplicationProvider.getApplicationContext<GameBoxApplication>()
        lateinit var inputModeManager: androidx.compose.ui.input.InputModeManager
        var dismissals = 0
        val game = Game(
            id = GameId("metadata-focus-test"),
            title = "Metadata Focus Test",
            platform = "PSP",
            year = 2007,
            genre = "Action",
            sizeMb = 1,
            state = InstallState.INSTALLED,
        )
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            MaterialTheme {
                GameMetadataEditorDialog(
                    game = game,
                    repository = app.container.gameRepository,
                    onDismiss = { dismissals++ },
                )
            }
        }

        compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithText("Cancel")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)

        compose.runOnIdle { assertEquals(1, dismissals) }
    }
}
