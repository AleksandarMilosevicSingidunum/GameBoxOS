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
        val first = ExternalDisplayTarget(23, "SMART TV")
        val second = ExternalDisplayTarget(24, "Capture")
        assertEquals(
            first,
            firstPresentationDisplayTarget(listOf(first, second)),
        )
        assertEquals(null, firstPresentationDisplayTarget(emptyList()))
    }

    @Test
    fun externalDisplaySuccessMessageDoesNotClaimConfirmationAlreadyCompleted() {
        assertEquals(
            "Display switch requested for SMART TV; confirm it if the system prompts you",
            externalDisplayLaunchRequestedMessage(ExternalDisplayTarget(23, "SMART TV")),
        )
    }
}
