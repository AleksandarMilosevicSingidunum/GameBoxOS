package com.gamebox.os.display

import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalDisplayLauncherTest {
    @Test
    fun externalDisplayUsesASeparateStandardHost() {
        assertEquals("com.gamebox.os.ExternalDisplayActivity", externalDisplayActivityClass.name)
    }

    @Test
    fun firstPresentationTargetPrefersFirstDisplayAndHandlesNone() {
        val first = ExternalDisplayTarget(23, "SMART TV", width = 1920, height = 1080)
        val second = ExternalDisplayTarget(24, "Capture")
        assertEquals(
            first,
            firstPresentationDisplayTarget(listOf(first, second)),
        )
        assertEquals(null, firstPresentationDisplayTarget(emptyList()))
    }

    @Test
    fun firstPresentationTargetPrefersPhysicalDisplayOverVirtualCapture() {
        val virtual = ExternalDisplayTarget(24, "Android Studio", isVirtual = true)
        val physical = ExternalDisplayTarget(23, "SMART TV", width = 1920, height = 1080)
        assertEquals(physical, firstPresentationDisplayTarget(listOf(virtual, physical)))
        assertEquals(virtual, firstPresentationDisplayTarget(listOf(virtual)))
    }

    @Test
    fun knownCaptureAndScreenSharingNamesAreVirtual() {
        assertEquals(true, isLikelyVirtualPresentationDisplay("studio.screen.sharing:8"))
        assertEquals(true, isLikelyVirtualPresentationDisplay("Android Studio Capture"))
        assertEquals(false, isLikelyVirtualPresentationDisplay("SMART TV"))
    }

    @Test
    fun externalTargetCanCarryFullDisplayLaunchBounds() {
        val target = ExternalDisplayTarget(23, "SMART TV", width = 1920, height = 1080)
        assertEquals(1920, target.width)
        assertEquals(1080, target.height)
    }

    @Test
    fun externalDisplaySuccessMessageDoesNotClaimConfirmationAlreadyCompleted() {
        assertEquals(
            "GameBox TV mode requested on SMART TV; confirm the display switch if prompted",
            externalDisplayLaunchRequestedMessage(ExternalDisplayTarget(23, "SMART TV")),
        )
    }

    @Test
    fun onlyTheRemovedExternalDisplayFinishesItsActivity() {
        assertEquals(true, shouldFinishExternalDisplayActivityForRemovedDisplay(23, 23))
        assertEquals(false, shouldFinishExternalDisplayActivityForRemovedDisplay(23, 24))
        assertEquals(false, shouldFinishExternalDisplayActivityForRemovedDisplay(0, 0))
        assertEquals(false, shouldFinishExternalDisplayActivityForRemovedDisplay(-1, -1))
    }

    @Test
    fun onlyTheChangedExternalDisplayReappliesItsWindowPolicy() {
        assertEquals(true, shouldReapplyExternalDisplayWindowPolicyForChangedDisplay(23, 23))
        assertEquals(false, shouldReapplyExternalDisplayWindowPolicyForChangedDisplay(23, 24))
        assertEquals(false, shouldReapplyExternalDisplayWindowPolicyForChangedDisplay(0, 0))
    }

    @Test
    fun externalDisplayPredicateExcludesDefaultAndUnknownDisplayIds() {
        assertEquals(false, isExternalDisplayId(-1))
        assertEquals(false, isExternalDisplayId(0))
        assertEquals(true, isExternalDisplayId(8))
    }

    @Test
    fun fullscreenRetryRequiresFocusOnAnExternalDisplay() {
        assertEquals(false, shouldRequestExternalFullscreenOnFocus(0, hasFocus = true))
        assertEquals(false, shouldRequestExternalFullscreenOnFocus(8, hasFocus = false))
        assertEquals(true, shouldRequestExternalFullscreenOnFocus(8, hasFocus = true))
    }

    @Test
    fun primaryFullscreenRequestRequiresFocusedMultiWindowLauncher() {
        assertEquals(false, shouldRequestPrimaryFullscreenOnFocus(0, hasFocus = false, isInMultiWindowMode = true))
        assertEquals(false, shouldRequestPrimaryFullscreenOnFocus(0, hasFocus = true, isInMultiWindowMode = false))
        assertEquals(true, shouldRequestPrimaryFullscreenOnFocus(0, hasFocus = true, isInMultiWindowMode = true))
        assertEquals(false, shouldRequestPrimaryFullscreenOnFocus(8, hasFocus = true, isInMultiWindowMode = true))
    }
}
