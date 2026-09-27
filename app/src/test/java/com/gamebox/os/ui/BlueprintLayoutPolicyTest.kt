package com.gamebox.os.ui

import com.gamebox.os.settings.DeveloperLayoutMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueprintLayoutPolicyTest {
    @Test
    fun autoKeepsSixteenByNineSurfacesWideAtHighDensity() {
        assertFalse(shouldUseCompactLayout(DeveloperLayoutMode.AUTO, 872f, 393f))
        assertFalse(shouldUseCompactLayout(DeveloperLayoutMode.AUTO, 1280f, 720f))
    }

    @Test
    fun autoKeepsPortraitAndNarrowWindowsCompact() {
        assertTrue(shouldUseCompactLayout(DeveloperLayoutMode.AUTO, 393f, 873f))
        assertTrue(shouldUseCompactLayout(DeveloperLayoutMode.AUTO, 640f, 360f))
        assertTrue(shouldUseCompactLayout(DeveloperLayoutMode.AUTO, 800f, 800f))
    }

    @Test
    fun explicitModesAlwaysWin() {
        assertFalse(shouldUseCompactLayout(DeveloperLayoutMode.WIDE, 320f, 640f))
        assertTrue(shouldUseCompactLayout(DeveloperLayoutMode.COMPACT, 1920f, 1080f))
    }
}
