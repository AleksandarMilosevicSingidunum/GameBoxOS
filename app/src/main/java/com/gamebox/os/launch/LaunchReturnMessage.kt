package com.gamebox.os.launch

import com.gamebox.os.storage.GameSaveBackupResult

/** Converts session recovery and automatic-backup outcomes into concise UI feedback. */
internal fun launchReturnMessage(
    launchConfirmed: Boolean,
    backupAttempt: Result<GameSaveBackupResult?>,
): String {
    val backup = backupAttempt.getOrNull()
    return when {
        !launchConfirmed -> "Recovered an interrupted handoff; no playtime was added"
        backupAttempt.isFailure ->
            "Returned to GameBox; time away recorded. Automatic save backup failed safely; use Save controls to retry."
        backup == null -> "Returned to GameBox; time away recorded"
        backup.failedCount > 0 || backup.message != null ->
            "Returned to GameBox; time away recorded. " +
                (backup.message ?: "Automatic save backup was incomplete; the previous complete snapshot was retained.")
        backup.artifacts.isEmpty() ->
            "Returned to GameBox; time away recorded. No managed save was found to back up."
        else -> "Returned to GameBox; time away recorded. Backed up ${backup.successfulCount} save artifact(s)."
    }
}
