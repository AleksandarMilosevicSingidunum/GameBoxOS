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
}
