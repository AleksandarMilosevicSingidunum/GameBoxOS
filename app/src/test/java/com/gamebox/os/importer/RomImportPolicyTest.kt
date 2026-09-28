package com.gamebox.os.importer

import org.junit.Assert.assertEquals
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
        assertEquals(true, "7z" in RomImportPolicy.supportedExtensions("PSP"))
        assertEquals("application/x-7z-compressed", RomImportPolicy.mimeType("game.7z"))
    }
}
