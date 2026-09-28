package com.gamebox.os.ui

import com.gamebox.os.settings.DeveloperLayoutMode
import org.junit.Assert.assertEquals
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

    @Test
    fun tvViewportFits1080pDesignSurfaceAt160Dpi() {
        assertEquals(0.9375f, blueprintViewportScale(1200f, 675f), 0.0001f)
        assertEquals(1.875f, blueprintViewportScale(2400f, 1350f), 0.0001f)
    }

    @Test
    fun physicalTvDimensionsMapToBlueprintScale() {
        assertEquals(1.5f, blueprintViewportScale(1920f, 1080f), 0.0001f)
        assertEquals(2f, blueprintViewportScale(3840f, 2160f), 0.0001f)
    }

    @Test
    fun tvDesktopCaptionWindowStaysWideAndFitsAvailableHeight() {
        assertFalse(shouldUseCompactLayout(DeveloperLayoutMode.AUTO, 1920f, 1037f))
        assertEquals(1037f / 720f, blueprintViewportScale(1920f, 1037f), 0.0001f)
    }

    @Test
    fun highDensityLandscapeWideSurfaceFitsItsShortViewport() {
        assertFalse(shouldUseCompactLayout(DeveloperLayoutMode.AUTO, 914f, 411f))
        assertEquals(411f / 720f, blueprintViewportScale(914f, 411f), 0.0001f)
    }

    @Test
    fun phoneViewportDoesNotShrinkForWideDesignMath() {
        assertEquals(1f, blueprintViewportScale(412f, 915f), 0.0001f)
        assertEquals(1f, blueprintViewportScale(Float.NaN, 675f), 0.0001f)
    }
}
