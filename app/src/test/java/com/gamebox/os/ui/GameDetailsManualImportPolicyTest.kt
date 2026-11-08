package com.gamebox.os.ui

import com.gamebox.os.domain.InstallState
import com.gamebox.os.importer.BrowserDownloadRecord
import com.gamebox.os.importer.BrowserDownloadStatus
import com.gamebox.os.importer.shouldRemoveImportedArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDetailsManualImportPolicyTest {
    @Test
    fun onlySuccessfulArchiveImportsRequestSourceRemoval() {
        assertTrue(shouldRemoveImportedArchive("copy.zip"))
        assertTrue(shouldRemoveImportedArchive("copy.7Z"))
        assertFalse(shouldRemoveImportedArchive("copy.nes"))
        assertFalse(shouldRemoveImportedArchive("gothicvania.sfc"))
        assertFalse(shouldRemoveImportedArchive("disc.cue"))
        assertFalse(shouldRemoveImportedArchive("track.bin"))
        assertFalse(shouldRemoveImportedArchive("copy"))
    }

    @Test
    fun completedImportMessageRequiresGameToRemainInstalled() {
        val imported = BrowserDownloadRecord(
            downloadId = -1L,
            stagedName = "manual.gba",
            displayName = "manual.gba",
            gameId = "homebrew",
            title = "Homebrew",
            platformName = "GBA",
            platformId = "gba",
            manualImport = true,
            status = BrowserDownloadStatus.IMPORTED,
        )

        assertEquals(
            "Import complete. The game is ready in your Library.",
            manualImportOutcome(imported, InstallState.INSTALLED),
        )
        assertNull(manualImportOutcome(imported, InstallState.NOT_INSTALLED))
        assertNull(manualImportOutcome(imported, InstallState.MISSING_FILES))
    }
}
