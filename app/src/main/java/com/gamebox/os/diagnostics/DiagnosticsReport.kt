package com.gamebox.os.diagnostics

import com.gamebox.os.domain.DownloadJob
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.InstallState
import com.gamebox.os.settings.GameBoxSettings
import java.net.URI
import java.time.Instant
import java.util.Locale

data class DiagnosticsDevice(
    val manufacturer: String,
    val model: String,
    val sdk: Int,
    val appVersion: String,
    val usableBytes: Long,
    val totalBytes: Long
)

fun buildDiagnosticsReport(
    device: DiagnosticsDevice,
    settings: GameBoxSettings,
    games: List<Game>,
    downloads: List<DownloadJob>,
    generatedAt: Instant = Instant.now()
): String {
    val providerHost = settings.catalogUrl.takeIf { it.isNotBlank() }?.let { value ->
        runCatching { URI(value).host }.getOrNull()?.takeIf { it.isNotBlank() }
    } ?: "bundled-offline"
    val installed = games.count {
        it.state == InstallState.INSTALLED || it.state == InstallState.UPDATE_AVAILABLE
    }
    val favoriteCount = games.count { it.favorite }
    val downloadStates = downloads.groupingBy { it.status.name }.eachCount().toSortedMap()

    return buildString {
        appendLine("GameBox OS diagnostics")
        appendLine("Generated: ${generatedAt}")
        appendLine("App version: ${device.appVersion}")
        appendLine("Device: ${device.manufacturer} ${device.model}")
        appendLine("Android SDK: ${device.sdk}")
        appendLine("Storage usable: ${formatDiagnosticBytes(device.usableBytes)}")
        appendLine("Storage total: ${formatDiagnosticBytes(device.totalBytes)}")
        appendLine("Catalog provider host: ${providerHost}")
        appendLine("Catalog seeded: ${settings.catalogSeededAtEpochMs ?: "not recorded"}")
        appendLine("Catalog refreshed: ${settings.catalogRefreshedAtEpochMs ?: "not recorded"}")
        appendLine("Games total: ${games.size}")
        appendLine("Games installed: ${installed}")
        appendLine("Favorites: ${favoriteCount}")
        appendLine("Download jobs: ${downloads.size}")
        appendLine(
            "Download states: " +
                if (downloadStates.isEmpty()) "none"
                else downloadStates.entries.joinToString { (state, count) -> "${state}=${count}" }
        )
        appendLine()
        appendLine("Privacy note: catalog paths, query strings, credentials, game source URLs,")
        appendLine("checksums, file paths, and save contents are intentionally excluded.")
    }
}

private fun formatDiagnosticBytes(bytes: Long): String {
    val gib = bytes.coerceAtLeast(0L).toDouble() / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f GB", gib)
}
