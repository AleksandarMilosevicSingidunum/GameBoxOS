package com.gamebox.os.ui

import com.gamebox.os.launch.MoonlightConnectivity
import com.gamebox.os.launch.MoonlightStatus
import com.gamebox.os.settings.ShortcutLaunchRecord

internal fun moonlightHeroActionLabel(installed: Boolean): String =
    if (installed) "Launch Moonlight" else "Get Moonlight"

internal fun moonlightHeroContentDescription(installed: Boolean): String =
    if (installed) "Moonlight, ready to launch" else "Moonlight not installed, open install page"

internal fun moonlightStatusForHub(
    shortcuts: List<AppShortcut>,
    installedPackages: Set<String>,
    recentLaunches: List<ShortcutLaunchRecord>,
    networkState: GameBoxNetworkState,
    formatLaunchTime: (Long) -> String = ::formatShortcutLaunchTime,
): MoonlightStatus {
    val featuredStreamer = shortcuts.firstOrNull {
        it.role == AppShortcutRole.FEATURED_PC_STREAMING
    }
    val recentSessions = if (featuredStreamer == null) {
        emptyList()
    } else {
        recentLaunches.asSequence()
            .filter { it.packageName == featuredStreamer.packageName }
            .map { featuredStreamer.title + " · " + formatLaunchTime(it.launchedAtEpochMs) }
            .toList()
    }
    return MoonlightStatus(
        connectivity = when (networkState) {
            GameBoxNetworkState.OFFLINE -> MoonlightConnectivity.OFFLINE
            GameBoxNetworkState.LOCAL -> MoonlightConnectivity.LOCAL_NETWORK
            GameBoxNetworkState.INTERNET -> MoonlightConnectivity.INTERNET
        },
        moonlightInstalled = featuredStreamer?.packageName?.let(installedPackages::contains) == true,
        recentSessions = recentSessions,
    )
}
