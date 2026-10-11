package com.gamebox.os.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RomImportPolicyTest {
    @Test
    fun genericRetroCatalogResolvesUniqueGameBoyColorExtension() {
        assertEquals(
            "Game Boy Color",
            RomImportPolicy.resolvedPlatform("Retro", "brickster.gbc"),
        )
    }

    @Test
    fun concretePlatformRemainsAuthoritative() {
        assertEquals(
            "Nintendo Entertainment System",
            RomImportPolicy.resolvedPlatform("NES", "game.nes"),
        )
    }

    @Test
    fun ambiguousArchiveKeepsGenericCatalogPlatform() {
        assertEquals("Retro", RomImportPolicy.resolvedPlatform("Retro", "game.zip"))
    }

    @Test
    fun pspAuthorizedCopyAcceptsSevenZipArchive() {
        assertTrue("7z" in RomImportPolicy.supportedExtensions("PSP"))
        assertEquals("application/x-7z-compressed", RomImportPolicy.mimeType("game.7z"))
    }

    @Test
    fun everyVimmPresetConsoleAcceptsSevenZipButNotAsARegisteredGameFile() {
        listOf(
            "Dreamcast", "Game Boy Advance", "Game Boy Color", "GameCube", "N64", "NES",
            "PS1", "PS2", "PS3", "PSP", "SNES", "Wii", "WiiWare", "Xbox",
            "Xbox 360", "DS", "3DS", "Switch",
        ).forEach { platform ->
            assertTrue("$platform should accept a 7z archive", "7z" in RomImportPolicy.supportedExtensions(platform))
            assertFalse("$platform must extract, not register, a 7z container", "7z" in RomImportPolicy.supportedGameExtensions(platform))
        }
    }
}
