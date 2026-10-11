package com.gamebox.os.importer

import com.gamebox.os.domain.GameId
import java.io.File

/** Canonical paths for one staged import, kept inside the managed imports root. */
internal data class ImportTransactionWorkspace(
    val root: File,
    val targetDirectory: File,
    val stagingDirectory: File,
    val backupDirectory: File,
    val expectedRelativePrefix: String,
) {
    fun prepareDirectories() {
        root.mkdirs()
        check(targetDirectory.parentFile?.isDirectory == true || targetDirectory.parentFile?.mkdirs() == true) {
            "Import console directory is unavailable"
        }
        check(stagingDirectory.mkdirs() || stagingDirectory.isDirectory) {
            "Import staging directory is unavailable"
        }
    }

    companion object {
        fun create(
            filesDirectory: File,
            gameId: GameId,
            platform: String?,
            transactionId: String,
        ): ImportTransactionWorkspace {
            val id = gameId.value
            require(id.matches(Regex("[a-z0-9][a-z0-9-]{0,95}"))) { "Invalid game id" }

            val root = filesDirectory.resolve("imports").canonicalFile
            val rootPrefix = root.path + File.separator
            val platformDirectory = RomImportPolicy.platformDirectory(platform)
            val targetDirectory = root.resolve(platformDirectory).resolve(id).canonicalFile
            require(targetDirectory.path.startsWith(rootPrefix)) { "Import path escaped app storage" }

            val stagingDirectory = root.resolve(".staging-$id-$transactionId").canonicalFile
            val backupDirectory = root.resolve(".backup-$id-$transactionId").canonicalFile
            require(stagingDirectory.path.startsWith(rootPrefix) && backupDirectory.path.startsWith(rootPrefix)) {
                "Import transaction path escaped app storage"
            }
            return ImportTransactionWorkspace(
                root = root,
                targetDirectory = targetDirectory,
                stagingDirectory = stagingDirectory,
                backupDirectory = backupDirectory,
                expectedRelativePrefix = "imports/$platformDirectory/$id/",
            )
        }
    }
}
