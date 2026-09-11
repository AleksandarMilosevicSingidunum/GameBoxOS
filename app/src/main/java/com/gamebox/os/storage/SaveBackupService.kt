package com.gamebox.os.storage

import com.gamebox.os.download.Sha256Verifier
import com.gamebox.os.download.VerificationResult
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

enum class BackupResult {
    SUCCESS, SOURCE_MISSING, BACKUP_MISSING, CHECKSUM_MISMATCH, SIZE_LIMIT_EXCEEDED,
    CROSS_GAME_PATH, SNAPSHOT_ABORTED
}

class SaveBackupService(
    savesDirectory: File,
    backupsDirectory: File,
    private val verifier: Sha256Verifier = Sha256Verifier(),
    private val maxBackupBytes: Long = 16L * 1024L * 1024L
) {
    init {
        require(maxBackupBytes > 0L) { "Backup size limit must be positive" }
    }
    private val savesRoot = savesDirectory.canonicalFile
    private val backupsRoot = backupsDirectory.canonicalFile

    fun hasBackup(relativePath: String): Boolean {
        val backup = resolveContained(backupsRoot, relativePath)
        if (AtomicSaveSnapshot.hasHeader(backup)) {
            return runCatching { AtomicSaveSnapshot.read(backup, maxBackupBytes) }.isSuccess
        }
        return legacyBackupIsValid(backup)
    }

    fun createBackup(relativePath: String): BackupResult {
        val source = resolveContained(savesRoot, relativePath)
        when (val inspection = inspectSource(relativePath)) {
            BackupResult.SUCCESS -> Unit
            BackupResult.CHECKSUM_MISMATCH -> require(false) { "Save is empty; existing backup retained" }
            else -> return inspection
        }
        val backup = resolveContained(backupsRoot, relativePath)
        validateBackupDestination(backup)
        check(backup.parentFile?.mkdirs() != false || backup.parentFile?.isDirectory == true)
        source.inputStream().use { AtomicSaveSnapshot.write(backup, it, maxBackupBytes) }
        return BackupResult.SUCCESS
    }

    internal fun inspectSource(relativePath: String): BackupResult {
        val source = resolveContained(savesRoot, relativePath)
        if (!source.isFile) return BackupResult.SOURCE_MISSING
        if (source.length() == 0L) return BackupResult.CHECKSUM_MISMATCH
        if (source.length() > maxBackupBytes) return BackupResult.SIZE_LIMIT_EXCEEDED
        return BackupResult.SUCCESS
    }

    fun restore(relativePath: String): BackupResult {
        val backup = resolveContained(backupsRoot, relativePath)
        if (!AtomicSaveSnapshot.hasHeader(backup) && backup.isFile && backup.length() > maxBackupBytes) {
            return BackupResult.SIZE_LIMIT_EXCEEDED
        }
        val payload = readBackup(backup) ?: return if (backup.exists()) {
            BackupResult.CHECKSUM_MISMATCH
        } else BackupResult.BACKUP_MISSING

        val destination = resolveContained(savesRoot, relativePath)
        val staged = resolveContained(savesRoot, relativePath + ".restore.part")
        check(destination.parentFile?.mkdirs() != false || destination.parentFile?.isDirectory == true)
        try {
            val copied = payload.inputStream().use { input ->
                staged.outputStream().use { output -> copyBounded(input, output, maxBackupBytes) }
            }
            if (!copied) return BackupResult.SIZE_LIMIT_EXCEEDED
            promote(staged, destination)
            return BackupResult.SUCCESS
        } finally {
            staged.delete()
        }
    }

    fun exportBackup(relativePath: String, output: OutputStream): BackupResult {
        val backup = resolveContained(backupsRoot, relativePath)
        if (!AtomicSaveSnapshot.hasHeader(backup) && backup.isFile && backup.length() > maxBackupBytes) {
            return BackupResult.SIZE_LIMIT_EXCEEDED
        }
        val payload = readBackup(backup) ?: return if (backup.exists()) {
            BackupResult.CHECKSUM_MISMATCH
        } else BackupResult.BACKUP_MISSING
        payload.inputStream().use { input -> input.copyTo(output) }
        output.flush()
        return BackupResult.SUCCESS
    }

    fun importBackup(
        relativePath: String,
        input: InputStream,
        maxBytes: Long = 16L * 1024L * 1024L
    ): BackupResult {
        require(maxBytes > 0L) { "Import size limit must be positive" }
        val backup = resolveContained(backupsRoot, relativePath)
        val staged = resolveContained(backupsRoot, relativePath + ".import.part")
        validateBackupDestination(backup)
        check(backup.parentFile?.mkdirs() != false || backup.parentFile?.isDirectory == true)
        var transferred = 0L
        var exceeded = false
        try {
            staged.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (!exceeded) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    transferred += count
                    if (transferred > maxBytes) {
                        exceeded = true
                    } else {
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (exceeded) return BackupResult.SIZE_LIMIT_EXCEEDED
            require(transferred > 0L) { "Selected backup is empty; existing backup retained" }
            staged.inputStream().use { AtomicSaveSnapshot.write(backup, it, maxBytes) }
            return BackupResult.SUCCESS
        } finally {
            // Provider failures must not leave a partial document in managed storage.
            staged.delete()
        }
    }

    private fun copyBounded(input: InputStream, output: OutputStream, limit: Long): Boolean {
        var transferred = 0L
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) return true
            if (count == 0) continue
            transferred += count
            if (transferred > limit) return false
            output.write(buffer, 0, count)
        }
    }

    private fun resolveContained(root: File, relativePath: String): File {
        require(relativePath.isNotBlank()) { "Save path cannot be blank" }
        require(!File(relativePath).isAbsolute) { "Save path must be relative" }
        require(relativePath.none { it == '\\' || it == ':' || it == '\u0000' } &&
            relativePath.split('/').none { it.isBlank() || it == "." || it == ".." }) {
            "Save path contains unsafe components"
        }
        var component = root
        relativePath.split('/').forEach {
            component = File(component, it)
            require(!Files.isSymbolicLink(component.toPath())) { "Save path cannot follow symbolic links" }
        }
        val target = File(root, relativePath).canonicalFile
        require(target.path.startsWith(root.path + File.separator)) {
            "Save path escapes managed storage"
        }
        return target
    }

    private fun validateBackupDestination(backup: File) {
        val checksum = checksumFile(backup)
        require(!backup.exists() || backup.isFile) { "Backup destination is not a file" }
        require(!checksum.exists() || checksum.isFile) { "Backup checksum destination is not a file" }
    }

    private fun checksumFile(backup: File) =
        resolveContained(backupsRoot, backup.relativeTo(backupsRoot).invariantSeparatorsPath + ".sha256")

    private fun legacyBackupIsValid(backup: File): Boolean {
        val checksum = checksumFile(backup)
        if (!backup.isFile || !checksum.isFile || backup.length() !in 1..maxBackupBytes) return false
        val expected = checksum.readText().trim()
        return Regex("[0-9a-fA-F]{64}").matches(expected) &&
            backup.inputStream().use { verifier.verify(it, expected) } == VerificationResult.Verified
    }

    private fun readBackup(backup: File): ByteArray? = if (AtomicSaveSnapshot.hasHeader(backup)) {
        runCatching { AtomicSaveSnapshot.read(backup, maxBackupBytes) }.getOrNull()
    } else if (legacyBackupIsValid(backup)) {
        backup.readBytes()
    } else null

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun promote(staged: File, destination: File) {
        try {
            Files.move(
                staged.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(staged.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
