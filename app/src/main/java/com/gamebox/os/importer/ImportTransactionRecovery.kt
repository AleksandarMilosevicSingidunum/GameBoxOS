package com.gamebox.os.importer

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class ImportRecoveryReport(
    val rolledBackTransactions: Int = 0,
    val cleanedStagingEntries: Int = 0,
    val cleanedPartialFiles: Int = 0,
    val failures: Int = 0,
)

/**
 * Repairs only importer-owned temporary filesystem state left by an interrupted process.
 *
 * A surviving .backup-<game>-<uuid> means the directory swap never completed its normal
 * cleanup, so the pre-import directory is restored. A staging directory without a backup
 * is discarded. Single-file *.partial files are also discarded.
 *
 * This does not infer or publish Room registration; it only restores the file layout to a
 * safe pre-transaction state when the importer itself was interrupted.
 */
class ImportTransactionRecovery(filesDirectory: File) {
    private val filesRoot = filesDirectory.canonicalFile
    private val importsRoot = File(filesRoot, "imports")

    fun recover(): ImportRecoveryReport {
        if (!importsRoot.exists()) return ImportRecoveryReport()
        if (!importsRoot.isDirectory) return ImportRecoveryReport(failures = 1)

        var rolledBack = 0
        var stagingCleaned = 0
        var partialCleaned = 0
        var failures = 0

        val entries = importsRoot.listFiles().orEmpty().toList()
        val transactions = mutableMapOf<TransactionKey, TransactionFiles>()

        entries.forEach { entry ->
            parseTransaction(entry.name)?.let { (kind, key) ->
                val current = transactions.getOrPut(key) { TransactionFiles() }
                when (kind) {
                    TransactionKind.STAGING -> current.staging = entry
                    TransactionKind.BACKUP -> current.backup = entry
                }
            }
        }

        transactions.toSortedMap(compareBy<TransactionKey>({ it.gameId }, { it.transactionId }))
            .forEach { (key, tx) ->
                val result = runCatching {
                    requireSafeGameId(key.gameId)
                    val target = File(importsRoot, key.gameId)
                    val backup = tx.backup
                    val staging = tx.staging

                    if (backup != null && backup.exists()) {
                        requireOwnedTransactionEntry(backup)
                        if (target.exists()) {
                            deleteOwnedEntry(target)
                        }
                        moveReplacing(backup, target)
                        rolledBack += 1
                    }

                    if (staging != null && staging.exists()) {
                        requireOwnedTransactionEntry(staging)
                        deleteOwnedEntry(staging)
                        stagingCleaned += 1
                    }
                }
                if (result.isFailure) failures += 1
            }

        importsRoot.walkTopDown()
            .onEnter { dir ->
                dir == importsRoot || (
                    runCatching { dir.canonicalPath.startsWith(importsRoot.canonicalPath + File.separator) }
                        .getOrDefault(false) &&
                    !Files.isSymbolicLink(dir.toPath())
                )
            }
            .filter { file ->
                file.isFile &&
                    !Files.isSymbolicLink(file.toPath()) &&
                    file.name.endsWith(".partial")
            }
            .toList()
            .forEach { partial ->
                val removed = runCatching {
                    require(partial.canonicalPath.startsWith(importsRoot.canonicalPath + File.separator)) {
                        "Partial import path escaped imports root"
                    }
                    Files.deleteIfExists(partial.toPath())
                }.getOrElse {
                    failures += 1
                    false
                }
                if (removed) partialCleaned += 1
            }

        return ImportRecoveryReport(
            rolledBackTransactions = rolledBack,
            cleanedStagingEntries = stagingCleaned,
            cleanedPartialFiles = partialCleaned,
            failures = failures,
        )
    }

    private fun parseTransaction(name: String): Pair<TransactionKind, TransactionKey>? {
        val match = TRANSACTION_NAME.matchEntire(name) ?: return null
        val kind = when (match.groupValues[1]) {
            "staging" -> TransactionKind.STAGING
            "backup" -> TransactionKind.BACKUP
            else -> return null
        }
        val gameId = match.groupValues[2]
        val transactionId = match.groupValues[3]
        return kind to TransactionKey(gameId, transactionId)
    }

    private fun requireOwnedTransactionEntry(file: File) {
        require(!Files.isSymbolicLink(file.toPath())) { "Import transaction entry is a symbolic link" }
        require(file.canonicalPath.startsWith(importsRoot.canonicalPath + File.separator)) {
            "Import transaction path escaped imports root"
        }
        require(file.isDirectory) { "Import transaction entry must be a directory" }
    }

    private fun deleteOwnedEntry(file: File) {
        if (Files.isSymbolicLink(file.toPath())) {
            Files.deleteIfExists(file.toPath())
            return
        }
        require(file.canonicalPath.startsWith(importsRoot.canonicalPath + File.separator)) {
            "Import cleanup path escaped imports root"
        }
        if (file.exists()) {
            require(file.deleteRecursively()) { "Could not remove interrupted import content" }
        }
    }

    private fun moveReplacing(source: File, target: File) {
        requireOwnedTransactionEntry(source)
        require(!target.exists()) { "Import recovery target still exists" }
        runCatching {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.getOrElse {
            Files.move(source.toPath(), target.toPath())
        }
    }

    private fun requireSafeGameId(value: String) {
        require(value.matches(Regex("[a-z0-9][a-z0-9-]{0,95}"))) { "Invalid interrupted import game ID" }
    }

    private data class TransactionKey(val gameId: String, val transactionId: String)
    private data class TransactionFiles(var staging: File? = null, var backup: File? = null)
    private enum class TransactionKind { STAGING, BACKUP }

    private companion object {
        // Game IDs may contain dashes; UUID is captured from the fixed 36-char suffix.
        val TRANSACTION_NAME = Regex(
            """.(staging|backup)-([a-z0-9][a-z0-9-]{0,95})-([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"""
        )
    }
}
