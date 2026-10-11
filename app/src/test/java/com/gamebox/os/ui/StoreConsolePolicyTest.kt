package com.gamebox.os.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreConsolePolicyTest {
    @Test
    fun homebrewConsoleIncludesLocalRetroAndSourcePortPlatforms() {
        assertTrue(isHomebrewStorePlatform("Homebrew"))
        assertTrue(isHomebrewStorePlatform("Retro"))
        assertTrue(isHomebrewStorePlatform("Android"))
        assertTrue(isHomebrewStorePlatform("Source Port"))
        assertFalse(isHomebrewStorePlatform("Nintendo GameCube"))
    }
}
