package com.gamebox.os

import com.gamebox.os.domain.GameId
import com.gamebox.os.launch.EmulatorCapability
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import com.gamebox.os.launch.ReturnTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchAdapterTest {
    @Test fun registry_matchesOnlyExplicitPlatformCapability() {
        val registry = EmulatorCapabilityRegistry(
            listOf(EmulatorCapability("approved", "example.emulator", setOf("retro")))
        )

        assertEquals("example.emulator", registry.forPlatform("Retro")?.packageName)
        assertNull(registry.forPlatform("Android"))
    }

    @Test fun returnTracker_recordsOneSessionAndThenClearsIt() {
        var time = 1_000L
        val tracker = ReturnTracker { time }
        tracker.started(GameId("retro-test"))
        time += 125_000L

        val session = tracker.returned()

        assertEquals(GameId("retro-test"), session?.gameId)
        assertEquals(2, session?.minutesPlayed)
        assertNull(tracker.returned())
    }

    @Test fun clockRollback_neverCreatesNegativePlayTime() {
        var time = 2_000L
        val tracker = ReturnTracker { time }
        tracker.started(GameId("retro-test"))
        time = 1_000L

        assertEquals(0, tracker.returned()?.minutesPlayed)
    }
}
