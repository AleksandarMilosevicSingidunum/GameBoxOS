package com.gamebox.os.importer

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Promotes staged game content while retaining the previous directory for registration rollback. */
internal object ImportDirectoryPublisher {
    fun replaceAtomically(target: File, staging: File, backup: File) {
        var previousMoved = false
        try {
            if (target.exists()) {
                moveReplacing(target, backup)
                previousMoved = true
            }
            moveReplacing(staging, target)
        } catch (error: Exception) {
            if (previousMoved && backup.exists()) {
                if (target.exists()) target.deleteRecursively()
                runCatching { moveReplacing(backup, target) }
            }
            throw error
        }
    }

    private fun moveReplacing(source: File, target: File) {
        runCatching {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }.getOrElse {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
