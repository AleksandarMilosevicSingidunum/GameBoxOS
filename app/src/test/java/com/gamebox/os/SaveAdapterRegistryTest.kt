package com.gamebox.os

import com.gamebox.os.storage.SaveAdapter
import com.gamebox.os.storage.SaveAdapterRegistry
import com.gamebox.os.storage.SaveArtifact
import com.gamebox.os.storage.SavePresence
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveAdapterRegistryTest {
    @Test
    fun resolvesPlatformCaseInsensitively() {
        val adapter = SaveAdapter { gameId -> listOf(SaveArtifact(gameId, "save.dat", 1, 2)) }
        val summary = SaveAdapterRegistry(mapOf("PPSSPP" to adapter)).inspect("ppsspp", "game-a")
        assertEquals(SavePresence.PRESENT, summary.presence)
        assertEquals(1, summary.artifactCount)
    }

    @Test
    fun reportsUnsupportedPlatformExplicitly() {
        val summary = SaveAdapterRegistry(emptyMap()).inspect("unknown", "game-a")
        assertEquals(SavePresence.ERROR, summary.presence)
        assertEquals("Save discovery is not configured for platform: unknown", summary.message)
    }
}
