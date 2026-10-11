package com.gamebox.os.ui

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gamebox.os.domain.CatalogRefreshState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogActionFocusTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyStateDoesNotStealInitialFocusFromControllerNavigationRail() {
        assert(shouldShowEmptyCatalogAction(false, true, false, false, true, false))
        assert(!shouldShowEmptyCatalogAction(false, true, false, true, false, false))
        assert(!shouldShowEmptyCatalogAction(false, true, false, false, false, false))
        assert(!shouldAutofocusCatalogAction(emptyCatalog = true, hasControllerNavigationRail = true))
        assert(shouldAutofocusCatalogAction(emptyCatalog = true, hasControllerNavigationRail = false))
        assert(!shouldAutofocusCatalogAction(emptyCatalog = false, hasControllerNavigationRail = false))
    }

    @Test
    fun enabledInitialActionClaimsFocusOnceItIsPlaced() {
        val focusRequester = FocusRequester()
        val focusClaimed = mutableStateOf(false)

        compose.setContent {
            MaterialTheme {
                Box {
                    BlueprintButton(
                        onClick = {},
                        modifier = Modifier
                            .claimInitialCatalogActionFocus(
                                enabled = true,
                                focusRequester = focusRequester,
                                focusClaimed = focusClaimed,
                            )
                            .testTag("catalog-sync"),
                    ) { Text("Sync") }
                }
            }
        }

        compose.onNodeWithTag("catalog-sync").assertIsFocused()
        compose.runOnIdle { assert(focusClaimed.value) }
    }

    @Test
    fun disabledInitialActionDoesNotClaimFocus() {
        val focusClaimed = mutableStateOf(false)
        val focusRequester = FocusRequester()

        compose.setContent {
            MaterialTheme {
                BlueprintButton(
                    onClick = {},
                    modifier = Modifier
                        .testTag("catalog-refresh")
                        .claimInitialCatalogActionFocus(
                            enabled = false,
                        focusRequester = focusRequester,
                            focusClaimed = focusClaimed,
                        ),
                ) { Text("Refresh") }
            }
        }

        compose.onNodeWithTag("catalog-refresh").assertIsNotFocused()
        compose.runOnIdle { assert(!focusClaimed.value) }
    }

    @Test
    fun refreshActionAcceptsControllerAAndDisablesWhileRefreshing() {
        val refreshCount = mutableIntStateOf(0)
        compose.setContent {
            val refreshState = remember { mutableStateOf(CatalogRefreshState.IDLE) }
            MaterialTheme {
                CatalogRefreshAction(refreshState.value, onRefresh = {
                    refreshCount.intValue++
                    refreshState.value = CatalogRefreshState.REFRESHING
                })
            }
        }

        compose.onNodeWithText("Refresh").performSemanticsAction(SemanticsActions.RequestFocus)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        compose.onNodeWithText("Refreshing...").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, refreshCount.intValue) }
    }
}
