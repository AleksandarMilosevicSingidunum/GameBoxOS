package com.gamebox.os.ui

internal data class HubControllerLabels(val xLabel: String, val yLabel: String)

internal enum class AppHubKind { MEDIA, PC }

internal enum class AppShortcutRole { STANDARD, FEATURED_PC_STREAMING, FILE_MANAGER }

internal fun hubControllerLabels(kind: AppHubKind, showUnavailable: Boolean): HubControllerLabels =
    HubControllerLabels(
        xLabel = if (showUnavailable) "Installed only" else "Show setup",
        yLabel = when (kind) {
            AppHubKind.PC -> "Desktop mode"
            AppHubKind.MEDIA -> "Audio"
        },
    )

internal data class AppShortcut(
    val title: String,
    val description: String,
    val packageName: String,
    val role: AppShortcutRole = AppShortcutRole.STANDARD,
)

internal val mediaShortcuts = listOf(
    AppShortcut("YouTube", "Video", "com.google.android.youtube"),
    AppShortcut("Netflix", "Streaming", "com.netflix.mediaclient"),
    AppShortcut("Kodi", "Media center", "org.xbmc.kodi"),
    AppShortcut("Jellyfin", "Personal media", "org.jellyfin.mobile"),
    AppShortcut("Plex", "Personal media", "com.plexapp.android"),
    AppShortcut("Spotify", "Music", "com.spotify.music"),
    AppShortcut("VLC", "Local media", "org.videolan.vlc"),
    AppShortcut("Twitch", "Live streams", "tv.twitch.android.app"),
)

internal val pcShortcuts = listOf(
    AppShortcut(
        "Moonlight",
        "PC game streaming",
        "com.limelight",
        AppShortcutRole.FEATURED_PC_STREAMING,
    ),
    AppShortcut("Winlator", "Windows applications", "com.winlator"),
    AppShortcut("Termux", "Linux terminal", "com.termux"),
    AppShortcut(
        "Files",
        "Android document manager",
        "com.google.android.documentsui",
        AppShortcutRole.FILE_MANAGER,
    ),
    AppShortcut("Chrome", "Web browser", "com.android.chrome"),
)
