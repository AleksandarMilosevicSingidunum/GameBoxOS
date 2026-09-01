package com.gamebox.os

import com.gamebox.os.domain.GameId
import com.gamebox.os.importer.RomHasher
import com.gamebox.os.importer.RomImportPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class RomImporterTest {
    @Test
    fun computesCanonicalHashesInOnePass() {
        val hashes = RomHasher.hash(ByteArrayInputStream("hello".toByteArray()))

        assertEquals("3610a686", hashes.crc32)
        assertEquals("5d41402abc4b2a76b9719d911017c592", hashes.md5)
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", hashes.sha1)
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hashes.sha256)
        assertEquals(5L, hashes.sizeBytes)
    }

    @Test
    fun rejectsInputThatExceedsBound() {
        assertThrows(IllegalArgumentException::class.java) {
            RomHasher.hash(ByteArrayInputStream(ByteArray(5)), maxBytes = 4)
        }
    }

    @Test
    fun buildsGameScopedImportPaths() {
        assertEquals(
            "imports/god-of-war-ii/Game.iso",
            RomImportPolicy.relativePath(GameId("god-of-war-ii"), "Game.iso"),
        )
    }

    @Test
    fun acceptsSwitchXciAndOtherPlatformSpecificLegalCopyFormats() {
        val cases = listOf(
            Triple("nintendoswitch", "Super Mario Odyssey.xci", "super-mario-odyssey"),
            Triple("nintendo3ds", "Portable Game.cia", "portable-game"),
            Triple("nintendogamecube", "Adventure.rvz", "gamecube-adventure"),
            Triple("nintendowii", "Sports.wbfs", "wii-sports"),
            Triple("sonyplaystation2", "Racing.chd", "ps2-racing"),
            Triple("sonyplaystationportable", "Portable.cso", "psp-portable"),
            Triple("segadreamcast", "Arcade.gdi", "dreamcast-arcade"),
        )

        cases.forEach { (platform, fileName, gameId) ->
            assertEquals(
                "imports/$gameId/$fileName",
                RomImportPolicy.relativePath(GameId(gameId), fileName, platform),
            )
        }
    }

    @Test
    fun exposesFriendlyPlatformLabelsAndFormats() {
        assertEquals("Nintendo Switch", RomImportPolicy.profileLabel("nintendoswitch"))
        assertEquals(
            setOf("xci", "xcz", "nsp", "nsz", "nca", "nro", "nso"),
            RomImportPolicy.supportedExtensions("Nintendo Switch"),
        )
    }

    @Test
    fun convertsStoredPathsAndReportsLaunchMimeTypes() {
        assertEquals(
            "super-mario-odyssey/Super Mario Odyssey.xci",
            RomImportPolicy.importRootRelativePath(
                GameId("super-mario-odyssey"),
                "imports/super-mario-odyssey/Super Mario Odyssey.xci",
            ),
        )
        assertEquals("application/x-nintendo-switch-xci", RomImportPolicy.mimeType("Mario.xci"))
        assertEquals("application/x-dolphin-rvz", RomImportPolicy.mimeType("Metroid.rvz"))
    }

    @Test
    fun rejectsAValidConsoleFormatWhenItDoesNotMatchTheSelectedPlatform() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            RomImportPolicy.relativePath(GameId("portable-game"), "Portable.xci", "PSP")
        }

        assertEquals(
            ".xci is not supported for PSP. Supported formats: .CSO, .ISO, .PBP",
            error.message,
        )
    }

    @Test
    fun rejectsTraversalAndUnsupportedTypes() {
        assertThrows(IllegalArgumentException::class.java) {
            RomImportPolicy.relativePath(GameId("../escape"), "game.iso")
        }
        assertThrows(IllegalArgumentException::class.java) {
            RomImportPolicy.relativePath(GameId("safe-game"), "installer.exe")
        }
    }
}
