package com.gamebox.os.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class BlueprintRouteTransitionFocusTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun outgoingRouteDoesNotRemainAnAccessibleOrFocusableTarget() {
        val route = mutableStateOf(BlueprintScreenRoute(Destination.HOME, null))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                BlueprintScreenTransition(route.value) { visibleRoute ->
                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = {}) {
                            Text(visibleRoute.destination.title)
                        }
                    }
                }
            }
        }
        compose.waitForIdle()

        compose.runOnIdle {
            route.value = BlueprintScreenRoute(Destination.LIBRARY, null)
        }
        compose.waitForIdle()

        compose.onAllNodesWithText("Home").assertCountEquals(0)
        compose.onNodeWithText("Library").assertHasClickAction()

        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        compose.onNodeWithText("Library").assertHasClickAction()
    }

    @Test
    fun reducedMotionReplacesRoutesImmediatelyWithoutAnExitFrame() {
        val route = mutableStateOf(BlueprintScreenRoute(Destination.HOME, null))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalReducedMotion provides true) {
                    BlueprintScreenTransition(route.value) { visibleRoute ->
                        Text(visibleRoute.destination.title)
                    }
                }
            }
        }
        compose.waitForIdle()

        compose.runOnIdle {
            route.value = BlueprintScreenRoute(Destination.LIBRARY, null)
        }
        compose.waitForIdle()

        compose.onAllNodesWithText("Home").assertCountEquals(0)
        compose.onNodeWithText("Library").assertExists()
    }

    @Test
    fun incomingRouteKeepsItsControllerActionsAfterOutgoingRouteDisposes() {
        val route = mutableStateOf(BlueprintScreenRoute(Destination.HOME, null))
        val registry = ControllerActionRegistry()
        var activeAction = ""
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalControllerActions provides registry) {
                    BlueprintScreenTransition(route.value) { visibleRoute ->
                        val actions = LocalControllerActions.current
                        DisposableEffect(actions, visibleRoute) {
                            val lease = actions?.configure(
                                xLabel = visibleRoute.destination.title,
                                onX = { activeAction = visibleRoute.destination.title },
                                yLabel = "Back",
                                onY = {},
                            )
                            onDispose {
                                if (actions != null && lease != null) actions.clear(lease)
                            }
                        }
                        Text(visibleRoute.destination.title)
                    }
                }
            }
        }
        compose.waitForIdle()

        compose.runOnIdle {
            route.value = BlueprintScreenRoute(Destination.LIBRARY, null)
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()

        compose.runOnIdle {
            assertEquals("Library", registry.xLabel)
            assertTrue(registry.invokeX())
            assertEquals("Library", activeAction)
        }
    }
}
