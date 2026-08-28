package com.gamebox.os.domain

@JvmInline
value class GameId(val value: String)

enum class InstallState {
    NOT_INSTALLED, QUEUED, DOWNLOADING, PAUSED, VERIFYING,
    INSTALLING, INSTALLED, UPDATE_AVAILABLE, MISSING_FILES, FAILED
}

data class Game(
    val id: GameId,
    val title: String,
    val platform: String,
    val year: Int,
    val genre: String,
    val sizeMb: Int,
    val state: InstallState,
    val lastPlayed: String? = null,
    val minutesPlayed: Int = 0
)

data class DownloadJob(
    val gameId: GameId,
    val title: String,
    val state: InstallState,
    val progress: Float
)

fun InstallState.primaryAction(): String = when (this) {
    InstallState.NOT_INSTALLED -> "Install"
    InstallState.QUEUED, InstallState.DOWNLOADING -> "View download"
    InstallState.PAUSED -> "Resume"
    InstallState.VERIFYING, InstallState.INSTALLING -> "View progress"
    InstallState.INSTALLED -> "Play"
    InstallState.UPDATE_AVAILABLE -> "Play"
    InstallState.MISSING_FILES -> "Locate or reinstall"
    InstallState.FAILED -> "Retry"
}
