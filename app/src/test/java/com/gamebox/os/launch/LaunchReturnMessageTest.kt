package com.gamebox.os.launch

import com.gamebox.os.storage.BackupResult
import com.gamebox.os.storage.GameSaveBackupResult
import com.gamebox.os.storage.SaveArtifact
import com.gamebox.os.storage.SaveArtifactBackupResult
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchReturnMessageTest {
    @Test
    fun interruptedHandoffDoesNotClaimPlayOrBackup() {
        assertEquals(
            "Recovered an interrupted handoff; no playtime was added",
            launchReturnMessage(false, 0, Result.failure(IllegalStateException("unused"))),
        )
    }

    @Test
    fun quickConfirmedReturnDoesNotClaimSuccessfulPlay() {
        assertEquals(
            "Emulator returned in under a minute; the game may not have finished starting",
            launchReturnMessage(true, 0, Result.success(null)),
        )
        assertEquals(
            "Emulator returned in under a minute; the game may not have finished starting. " +
                "Automatic save backup failed safely; use Save controls to retry.",
            launchReturnMessage(true, 0, Result.failure(IllegalStateException("backup failed"))),
        )
    }

    @Test
    fun backupFailureAndMissingBackupHaveDistinctFeedback() {
        assertEquals(
            "Returned to GameBox; time away recorded. Automatic save backup failed safely; use Save controls to retry.",
            launchReturnMessage(true, 1, Result.failure(IllegalStateException("backup failed"))),
        )
        assertEquals(
            "Returned to GameBox; time away recorded",
            launchReturnMessage(true, 1, Result.success(null)),
        )
    }

    @Test
    fun partialBackupPreservesCoordinatorMessage() {
        val backup = GameSaveBackupResult("game", emptyList(), "One save needs attention")

        assertEquals(
            "Returned to GameBox; time away recorded. One save needs attention",
            launchReturnMessage(true, 1, Result.success(backup)),
        )
    }

    @Test
    fun emptyAndCompleteBackupsExplainTheirOutcomes() {
        val empty = GameSaveBackupResult("game", emptyList())
        val complete = GameSaveBackupResult(
            "game",
            listOf(
                SaveArtifactBackupResult(
                    SaveArtifact("game", "saves/slot1", 4L, 1L),
                    BackupResult.SUCCESS,
                ),
                SaveArtifactBackupResult(
                    SaveArtifact("game", "saves/slot2", 8L, 2L),
                    BackupResult.SUCCESS,
                ),
            ),
        )

        assertEquals(
            "Returned to GameBox; time away recorded. No managed save was found to back up.",
            launchReturnMessage(true, 1, Result.success(empty)),
        )
        assertEquals(
            "Returned to GameBox; time away recorded. Backed up 2 save artifact(s).",
            launchReturnMessage(true, 1, Result.success(complete)),
        )
    }
}
