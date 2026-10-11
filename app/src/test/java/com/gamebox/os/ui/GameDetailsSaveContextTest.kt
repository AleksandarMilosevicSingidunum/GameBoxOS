package com.gamebox.os.ui

import com.gamebox.os.storage.SavePresence
import com.gamebox.os.storage.SaveSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class GameDetailsSaveContextTest {
    @Test
    fun describesSaveInspectionStatesForTheDetailsPanel() {
        assertEquals("Inspecting managed saves…", managedSaveDescription(null))
        assertEquals("No managed save files found", managedSaveDescription(summary(SavePresence.NONE)))
        assertEquals("Managed saves could not be inspected", managedSaveDescription(summary(SavePresence.ERROR)))
        assertEquals(
            "3 managed save files · 1.0 MB",
            managedSaveDescription(summary(SavePresence.PRESENT, artifactCount = 3, totalBytes = 1_048_576L)),
        )
    }

    private fun summary(
        presence: SavePresence,
        artifactCount: Int = 0,
        totalBytes: Long = 0,
    ) = SaveSummary(
        gameId = "test-game",
        presence = presence,
        artifactCount = artifactCount,
        totalBytes = totalBytes,
    )
}
