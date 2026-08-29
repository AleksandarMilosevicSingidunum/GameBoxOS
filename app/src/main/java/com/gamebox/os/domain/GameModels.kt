package com.gamebox.os.domain

object GraphicsProfiles {
    const val COMPATIBILITY = "Compatibility"
    const val BALANCED = "Balanced"
    const val PERFORMANCE = "Performance"
    val ALL = setOf(COMPATIBILITY, BALANCED, PERFORMANCE)
}

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
    val minutesPlayed: Int = 0,
    val favorite: Boolean = false,
    val sourceUrl: String? = null,
    val expectedSha256: String? = null,
    val emulatorPackage: String? = null,
    val graphicsProfile: String = "Balanced",
    val savePresent: Boolean = false,
    val saveSizeBytes: Long = 0L
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, VERIFYING, INSTALLING,
    COMPLETED, FAILED, CANCELLED
}

data class DownloadJob(
    val id: String,
    val gameId: GameId,
    val title: String,
    val status: DownloadStatus,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val errorReason: String? = null
) {
    val progress: Float
        get() = if (totalBytes <= 0L) 0f
        else (downloadedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
}

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

enum class CatalogRefreshState {
    IDLE, REFRESHING, SUCCESS, ERROR
}
