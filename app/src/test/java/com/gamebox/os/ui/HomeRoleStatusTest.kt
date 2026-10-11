package com.gamebox.os.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRoleStatusTest {
    @Test
    fun emptyLibrarySetupGetsInitialFocusOnlyWhenHomeRoleIsMissing() {
        assertTrue(shouldFocusEmptyHomeSetup(isDefaultHome = false, gameCount = 0))
        assertFalse(shouldFocusEmptyHomeSetup(isDefaultHome = true, gameCount = 0))
        assertFalse(shouldFocusEmptyHomeSetup(isDefaultHome = false, gameCount = 1))
    }

    @Test
    fun defaultHomeSetupBannerIsLimitedToRelevantSurface() {
        assertTrue(shouldShowDefaultHomeSetupBanner(isDefaultHome = false, allowPromptOnSurface = true))
        assertFalse(shouldShowDefaultHomeSetupBanner(isDefaultHome = true, allowPromptOnSurface = true))
        assertFalse(shouldShowDefaultHomeSetupBanner(isDefaultHome = false, allowPromptOnSurface = false))
    }
}
