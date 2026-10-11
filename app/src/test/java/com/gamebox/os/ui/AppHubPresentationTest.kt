package com.gamebox.os.ui

import com.gamebox.os.launch.MoonlightConnectivity
import com.gamebox.os.settings.ShortcutLaunchRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppHubPresentationTest {
    @Test
    fun moonlightHeroActionReflectsWhetherTheAppCanLaunch() {
        assertEquals("Launch Moonlight", moonlightHeroActionLabel(installed = true))
        assertEquals("Get Moonlight", moonlightHeroActionLabel(installed = false))
        assertEquals("Moonlight, ready to launch", moonlightHeroContentDescription(installed = true))
        assertEquals(
            "Moonlight not installed, open install page",
            moonlightHeroContentDescription(installed = false),
        )
    }

    @Test
    fun featuredStreamerStatusFollowsRoleAndPackageInsteadOfItsDisplayName() {
        val streamer = AppShortcut(
            title = "Renamed Streaming Client",
            description = "PC streaming",
            packageName = "example.streaming.client",
            role = AppShortcutRole.FEATURED_PC_STREAMING,
        )
        val status = moonlightStatusForHub(
            shortcuts = listOf(streamer),
            installedPackages = setOf(streamer.packageName),
            recentLaunches = listOf(
                ShortcutLaunchRecord(streamer.packageName, 42L),
                ShortcutLaunchRecord("another.app", 30L),
            ),
            networkState = GameBoxNetworkState.LOCAL,
            formatLaunchTime = { "time-$it" },
        )

        assertTrue(status.moonlightInstalled)
        assertEquals(MoonlightConnectivity.LOCAL_NETWORK, status.connectivity)
        assertEquals(listOf("Renamed Streaming Client · time-42"), status.recentSessions)
    }

    @Test
    fun noFeaturedStreamerProducesNoInstalledOrRecentMoonlightStatus() {
        val status = moonlightStatusForHub(
            shortcuts = mediaShortcuts,
            installedPackages = emptySet(),
            recentLaunches = listOf(ShortcutLaunchRecord("com.limelight", 42L)),
            networkState = GameBoxNetworkState.OFFLINE,
        )

        assertFalse(status.moonlightInstalled)
        assertEquals(MoonlightConnectivity.OFFLINE, status.connectivity)
        assertTrue(status.recentSessions.isEmpty())
    }
}
