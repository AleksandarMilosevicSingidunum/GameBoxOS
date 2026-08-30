package com.gamebox.os

import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.InstallState
import com.gamebox.os.launch.EmulatorCapability
import com.gamebox.os.launch.EmulatorCapabilityRegistry
import com.gamebox.os.launch.ReturnTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchAdapterTest {
    private val capability = EmulatorCapability(
        "approved",
        GameId("retro-test"),
        "example.emulator",
        "retro/retro-test/content/test.txt",
        "application/octet-stream",
        "94ee059335e587e501cc4bf90613e0814f00a7b08bc7c648fd865a2af6a22cc2"
    )

    @Test fun registry_matchesOnlyExplicitGameCapability() {
        val registry = EmulatorCapabilityRegistry(listOf(capability))

        assertEquals("example.emulator", registry.forGame(GameId("retro-test"))?.packageName)
        assertNull(registry.forGame(GameId("different-game")))
    }

    @Test fun capabilityContainsOnlyRelativeScopedContentPath() {
        assertEquals("retro/retro-test/content/test.txt", capability.contentRelativePath)
        assertEquals("application/octet-stream", capability.mimeType)
    }

    @Test fun galaxyPatrolDeclaresRequiredNesCore() {
        val game = Game(GameId("galaxy-patrol"), "Galaxy Patrol", "Retro", 2018, "Arcade", 1, InstallState.INSTALLED)
        val message = EmulatorCapabilityRegistry().readinessMessage(game)
        assertEquals(true, message?.contains("FCEUmm"))
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
