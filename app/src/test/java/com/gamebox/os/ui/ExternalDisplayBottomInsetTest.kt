package com.gamebox.os.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalDisplayBottomInsetTest {
    @Test
    fun convertsSystemPixelsUsingTheActiveDisplayDensity() {
        assertEquals(43f, externalDisplayBottomInsetDp(true, 43, 1f), 0.01f)
        assertEquals(21.5f, externalDisplayBottomInsetDp(true, 43, 2f), 0.01f)
    }

    @Test
    fun ignoresPhoneInsetsAndInvalidMeasurements() {
        assertEquals(0f, externalDisplayBottomInsetDp(false, 43, 1f), 0f)
        assertEquals(0f, externalDisplayBottomInsetDp(true, 0, 1f), 0f)
        assertEquals(0f, externalDisplayBottomInsetDp(true, -1, 1f), 0f)
        assertEquals(0f, externalDisplayBottomInsetDp(true, 43, 0f), 0f)
        assertEquals(0f, externalDisplayBottomInsetDp(true, 43, Float.NaN), 0f)
    }
}
