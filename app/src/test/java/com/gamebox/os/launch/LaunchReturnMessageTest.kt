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
            launchReturnMessage(false, Result.failure(IllegalStateException("unused"))),
        )
    }

    @Test
    fun backupFailureAndMissingBackupHaveDistinctFeedback() {
        assertEquals(
            "Returned to GameBox; time away recorded. Automatic save backup failed safely; use Save controls to retry.",
            launchReturnMessage(true, Result.failure(IllegalStateException("backup failed"))),
        )
        assertEquals(
            "Returned to GameBox; time away recorded",
            launchReturnMessage(true, Result.success(null)),
        )
    }

    @Test
    fun partialBackupPreservesCoordinatorMessage() {
        val backup = GameSaveBackupResult("game", emptyList(), "One save needs attention")

        assertEquals(
            "Returned to GameBox; time away recorded. One save needs attention",
            launchReturnMessage(true, Result.success(backup)),
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
            launchReturnMessage(true, Result.success(empty)),
        )
        assertEquals(
            "Returned to GameBox; time away recorded. Backed up 2 save artifact(s).",
            launchReturnMessage(true, Result.success(complete)),
        )
    }
}
