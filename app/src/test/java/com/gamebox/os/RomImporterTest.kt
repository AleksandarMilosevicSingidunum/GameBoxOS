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
    fun rejectsTraversalAndUnsupportedTypes() {
        assertThrows(IllegalArgumentException::class.java) {
            RomImportPolicy.relativePath(GameId("../escape"), "game.iso")
        }
        assertThrows(IllegalArgumentException::class.java) {
            RomImportPolicy.relativePath(GameId("safe-game"), "installer.exe")
        }
    }
}
