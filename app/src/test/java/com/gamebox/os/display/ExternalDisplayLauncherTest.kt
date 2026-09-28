package com.gamebox.os.display

import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalDisplayLauncherTest {
    @Test
    fun externalDisplayUsesASeparateStandardHost() {
        assertEquals("com.gamebox.os.TvActivity", externalDisplayActivityClass.name)
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
            "Display switch requested for SMART TV; confirm it if the system prompts you",
            externalDisplayLaunchRequestedMessage(ExternalDisplayTarget(23, "SMART TV")),
        )
    }

    @Test
    fun onlyTheRemovedExternalDisplayFinishesTvActivity() {
        assertEquals(true, shouldFinishTvActivityForRemovedDisplay(23, 23))
        assertEquals(false, shouldFinishTvActivityForRemovedDisplay(23, 24))
        assertEquals(false, shouldFinishTvActivityForRemovedDisplay(0, 0))
    }
}
