package com.gamebox.os.launch

import com.gamebox.os.storage.GameSaveBackupResult

/** Converts session recovery and automatic-backup outcomes into concise UI feedback. */
internal fun launchReturnMessage(
    launchConfirmed: Boolean,
    minutesAway: Int,
    backupAttempt: Result<GameSaveBackupResult?>,
): String {
    val backup = backupAttempt.getOrNull()
    val returned = if (minutesAway == 0) {
        "Emulator returned in under a minute; the game may not have finished starting"
    } else {
        "Returned to GameBox; time away recorded"
    }
    return when {
        !launchConfirmed -> "Recovered an interrupted handoff; no playtime was added"
        backupAttempt.isFailure ->
            "$returned. Automatic save backup failed safely; use Save controls to retry."
        backup == null -> returned
        backup.failedCount > 0 || backup.message != null ->
            "$returned. " +
                (backup.message ?: "Automatic save backup was incomplete; the previous complete snapshot was retained.")
        backup.artifacts.isEmpty() ->
            "$returned. No managed save was found to back up."
        else -> "$returned. Backed up ${backup.successfulCount} save artifact(s)."
    }
}
