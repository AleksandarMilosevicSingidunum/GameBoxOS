package com.gamebox.os.diagnostics

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.settings.GameBoxSettings
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReportTest {
    @Test
    fun excludesSensitiveCatalogAndGameData() {
        val secretUrl = "https://user:secret@catalog.example.com/private/manifest.json?token=hidden"
        val sourceUrl = "https://downloads.example.com/private/game.bin?signature=secret"
        val checksum = "a".repeat(64)
        val report = buildDiagnosticsReport(
            device = DiagnosticsDevice("Samsung", "Test", 36, "0.1.0", 10, 20),
            settings = GameBoxSettings(catalogUrl = secretUrl),
            games = listOf(
                Game(
                    GameId("demo"), "Demo", "Homebrew", 2026, "Demo", 1,
                    InstallState.INSTALLED, sourceUrl = sourceUrl, expectedSha256 = checksum
                )
            ),
            downloads = emptyList(),
            generatedAt = Instant.EPOCH
        )

        assertTrue(report.contains("catalog.example.com"))
        assertTrue(report.contains("Games installed: 1"))
        assertFalse(report.contains("user:secret"))
        assertFalse(report.contains("/private/"))
        assertFalse(report.contains("token="))
        assertFalse(report.contains(sourceUrl))
        assertFalse(report.contains(checksum))
    }
}
