package com.gamebox.os.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalLibraryCutoverPolicyTest {
    @Test fun externalLayoutKeepsGameOwnershipBoundary() {
        assertEquals(
            listOf("game-one", "remote", "game-one", "content", "game.chd"),
            externalLibrarySegments("game-one", "remote/game-one/content/game.chd"),
        )
        listOf("../escape", "remote//game", "/absolute", "remote/game:bad").forEach { path ->
            assertThrows(IllegalArgumentException::class.java) {
                externalLibrarySegments("game-one", path)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            externalLibrarySegments("../other-game", "remote/game/content.chd")
        }
    }

    @Test fun cutoverRequiresMatchingSizeAndSha256() {
        val checksum = ByteArray(32) { it.toByte() }
        assertTrue(migrationDestinationVerified(4, 4, checksum, checksum.copyOf()))
        assertFalse(migrationDestinationVerified(4, 3, checksum, checksum.copyOf()))
        assertFalse(
            migrationDestinationVerified(
                4,
                4,
                checksum,
                checksum.copyOf().also { it[0] = 99 },
            )
        )
    }
}
