package com.gamebox.os.data.local

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.UUID

internal data class DatabaseMigrationBackup(
    val directory: File,
    val sourceVersion: Int,
    val targetVersion: Int,
)

/**
 * Creates a durable snapshot of an existing SQLite database before Room can
 * migrate it. The database and WAL are copied into a staging directory, synced,
 * and then atomically exposed by renaming the directory.
 */
internal class DatabaseMigrationBackupManager(
    private val backupRoot: File,
    private val retainedBackups: Int = 2,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    init { require(retainedBackups > 0) { "retainedBackups must be positive" } }

    fun createIfNeeded(databaseFile: File, targetVersion: Int): DatabaseMigrationBackup? {
        require(targetVersion > 0) { "targetVersion must be positive" }
        if (!databaseFile.isFile) return null
        val sourceVersion = readUserVersion(databaseFile)
        if (sourceVersion <= 0 || sourceVersion >= targetVersion) return null

        check(backupRoot.exists() || backupRoot.mkdirs()) {
            "Unable to create database backup directory"
        }
        val finalDirectory = backupRoot.resolve(
            "pre-migration-v${sourceVersion}-to-v${targetVersion}-${nowMillis()}"
        )
        val stagingDirectory = backupRoot.resolve(".staging-${UUID.randomUUID()}")
        check(stagingDirectory.mkdir()) { "Unable to stage database backup" }

        try {
            copyAndSync(databaseFile, stagingDirectory.resolve(databaseFile.name))
            val wal = File(databaseFile.path + "-wal")
            if (wal.isFile) copyAndSync(wal, stagingDirectory.resolve(wal.name))
            stagingDirectory.resolve("manifest.txt").writeText(
                "sourceVersion=${sourceVersion}\ntargetVersion=${targetVersion}\n"
            )
            sync(stagingDirectory.resolve("manifest.txt"))
            check(stagingDirectory.renameTo(finalDirectory)) {
                "Unable to commit database backup"
            }
        } catch (failure: Throwable) {
            stagingDirectory.deleteRecursively()
            throw failure
        }

        prune(finalDirectory)
        return DatabaseMigrationBackup(finalDirectory, sourceVersion, targetVersion)
    }

    private fun prune(keep: File) {
        backupRoot.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith("pre-migration-") && it != keep }
            .sortedByDescending(File::lastModified)
            .drop((retainedBackups - 1).coerceAtLeast(0))
            .forEach(File::deleteRecursively)
    }

    private fun copyAndSync(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
    }

    private fun sync(file: File) {
        FileOutputStream(file, true).use { it.fd.sync() }
    }

    companion object {
        private const val SQLITE_USER_VERSION_OFFSET = 60L
        private const val SQLITE_MIN_HEADER_BYTES = 64L

        internal fun readUserVersion(databaseFile: File): Int {
            if (!databaseFile.isFile || databaseFile.length() < SQLITE_MIN_HEADER_BYTES) return 0
            return RandomAccessFile(databaseFile, "r").use { file ->
                file.seek(SQLITE_USER_VERSION_OFFSET)
                file.readInt()
            }
        }
    }
}
