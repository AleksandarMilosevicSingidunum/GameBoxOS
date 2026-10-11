package com.gamebox.os.ui

import com.gamebox.os.source.vimmLairDefaultPresetPlatforms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreConsoleTest {
    @Test
    fun storeExposesEveryMetadataConsoleAndLocalHomebrew() {
        assertEquals(
            setOf(
                "dreamcast", "gba", "gbc", "gamecube", "n64", "nes", "ps1", "ps2", "ps3",
                "psp", "snes", "wii", "wiiware", "xbox", "xbox360", "ds", "3ds", "switch",
                "homebrew",
            ),
            storeConsoles.map { it.key }.toSet(),
        )
        assertTrue(storeConsoles.single { it.key == "homebrew" }.aliases.isEmpty())
    }

    @Test
    fun aliasesAreNormalizedAndDoNotCrossMatch() {
        val psp = storeConsoles.single { it.key == "psp" }
        assertTrue(storeConsoleMatches(psp, "Sony PlayStation Portable"))
        assertTrue(storeConsoleMatches(psp, "PSP"))
        assertFalse(storeConsoleMatches(psp, "Nintendo GameCube"))
        assertFalse(storeConsoleMatches(storeConsoles.single { it.key == "homebrew" }, "Homebrew"))
    }

    @Test
    fun everyRequestedVimmPresetPlatformHasItsOwnStoreFilter() {
        val keys = storeConsoles.map { it.key }.toSet()
        assertTrue(
            keys.containsAll(
                setOf(
                    "dreamcast", "gba", "gbc", "gamecube", "n64", "nes", "ps1", "ps2", "ps3",
                    "psp", "snes", "wii", "wiiware", "xbox", "xbox360", "ds", "3ds", "switch",
                ),
            ),
        )
        assertTrue(
            vimmLairDefaultPresetPlatforms.all { platform ->
                storeConsoles.any { it.label == platform }
            },
        )
    }

}
