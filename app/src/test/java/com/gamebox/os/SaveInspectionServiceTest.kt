package com.gamebox.os

import com.gamebox.os.storage.SaveAdapter
import com.gamebox.os.storage.SaveArtifact
import com.gamebox.os.storage.SaveInspectionService
import com.gamebox.os.storage.SavePresence
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveInspectionServiceTest {
    @Test
    fun summarizesDiscoveredArtifacts() {
        val service = SaveInspectionService(SaveAdapter {
            listOf(
                SaveArtifact(it, "a.sav", 10, 100),
                SaveArtifact(it, "b.sav", 20, 200),
            )
        })

        val summary = service.inspect("game-a")

        assertEquals(SavePresence.PRESENT, summary.presence)
        assertEquals(2, summary.artifactCount)
        assertEquals(30, summary.totalBytes)
        assertEquals(200, summary.latestModifiedAtMillis)
    }

    @Test
    fun convertsAdapterFailureToErrorSummary() {
        val service = SaveInspectionService(SaveAdapter { error("unavailable") })

        val summary = service.inspect("game-a")

        assertEquals(SavePresence.ERROR, summary.presence)
        assertEquals("unavailable", summary.message)
    }
}
