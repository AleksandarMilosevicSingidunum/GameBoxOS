package com.gamebox.os.display

import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalDisplayLauncherTest {
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
}
