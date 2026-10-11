package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BlueprintScreenTransitionTest {
    @Test
    fun primaryTabsMoveHorizontallyInNavigationOrder() {
        assertEquals(
            BlueprintTransitionSpec(BlueprintTransitionAxis.HORIZONTAL, 1),
            blueprintTransitionSpec(
                BlueprintScreenRoute(Destination.HOME, null),
                BlueprintScreenRoute(Destination.LIBRARY, null),
            ),
        )
        assertEquals(
            BlueprintTransitionSpec(BlueprintTransitionAxis.HORIZONTAL, -1),
            blueprintTransitionSpec(
                BlueprintScreenRoute(Destination.SETTINGS, null),
                BlueprintScreenRoute(Destination.PC, null),
            ),
        )
        assertEquals(
            BlueprintTransitionSpec(BlueprintTransitionAxis.HORIZONTAL, 1),
            blueprintTransitionSpec(
                BlueprintScreenRoute(Destination.SETTINGS, null),
                BlueprintScreenRoute(Destination.HOME, null),
            ),
        )
        assertEquals(
            BlueprintTransitionSpec(BlueprintTransitionAxis.HORIZONTAL, -1),
            blueprintTransitionSpec(
                BlueprintScreenRoute(Destination.HOME, null),
                BlueprintScreenRoute(Destination.SETTINGS, null),
            ),
        )
    }

    @Test
    fun detailsUseVerticalMotionAndDownloadsUseTabMotion() {
        assertEquals(
            BlueprintTransitionSpec(BlueprintTransitionAxis.VERTICAL, 1),
            blueprintTransitionSpec(
                BlueprintScreenRoute(Destination.STORE, null),
                BlueprintScreenRoute(Destination.STORE, "game-1"),
            ),
        )
        assertEquals(
            BlueprintTransitionSpec(BlueprintTransitionAxis.VERTICAL, -1),
            blueprintTransitionSpec(
                BlueprintScreenRoute(Destination.STORE, "game-1"),
                BlueprintScreenRoute(Destination.STORE, null),
            ),
        )
        assertEquals(
            BlueprintTransitionSpec(BlueprintTransitionAxis.HORIZONTAL, 1),
            blueprintTransitionSpec(
                BlueprintScreenRoute(Destination.HOME, null),
                BlueprintScreenRoute(Destination.DOWNLOADS, null),
            ),
        )
    }

    @Test
    fun firstAndUnchangedRoutesDoNotTranslate() {
        val route = BlueprintScreenRoute(Destination.HOME, null)
        assertEquals(
            BlueprintTransitionSpec(BlueprintTransitionAxis.NONE),
            blueprintTransitionSpec(null, route),
        )
        assertEquals(
            BlueprintTransitionSpec(BlueprintTransitionAxis.NONE),
            blueprintTransitionSpec(route, route),
        )
    }
}
