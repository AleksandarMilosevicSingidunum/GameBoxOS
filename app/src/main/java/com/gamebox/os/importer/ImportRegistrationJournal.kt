package com.gamebox.os.importer

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PendingImportFile(
    val relativePath: String,
    val sha256: String,
)

@Serializable
data class PendingImportTransaction(
    val schemaVersion: Int = 1,
    val gameId: String,
    val transactionId: String,
    val createdAtMillis: Long,
    val hadExistingTarget: Boolean,
    val files: List<PendingImportFile>,
)

data class ImportRegistrationRecoveryReport(
    val confirmed: Int = 0,
    val rolledBack: Int = 0,
    val failures: Int = 0,
)

/**
 * Durable boundary between an atomic import-file commit and Room library registration.
 *
 * A marker is written before the target directory is swapped. The old target directory
 * is retained as the transaction backup until the matching Room registration is known to
 * be durable. On a later process start, registered bytes are confirmed; otherwise the
 * filesystem is rolled back to the pre-import state.
 */
class ImportRegistrationJournal(
    filesDirectory: File,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val filesRoot = filesDirectory.canonicalFile
    private val importsRoot = File(filesRoot, "imports")
    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    fun prepare(
        gameId: GameId,
        transactionId: String,
        hadExistingTarget: Boolean,
        files: List<PendingImportFile>,
    ): PendingImportTransaction {
        requireSafeGameId(gameId.value)
        requireUuid(transactionId)
        require(files.isNotEmpty()) { "Pending import must contain at least one file" }
        require(files.size <= 64) { "Pending import contains too many files" }
        require(files.map { it.relativePath.lowercase() }.distinct().size == files.size) {
            "Pending import paths must be unique"
        }
        files.forEach { file ->
            require(file.relativePath.startsWith(gameId.value + "/")) {
                "Pending import path is outside the selected game"
            }
            require(
                file.relativePath.none { it == '\\' || it == '\u0000' } &&
                    file.relativePath.split('/').none { it.isBlank() || it == "." || it == ".." }
            ) { "Pending import path is unsafe" }
            require(file.sha256.matches(Regex("^[a-f0-9]{64}$"))) {
                "Pending import SHA-256 is invalid"
            }
        }

        check(importsRoot.mkdirs() || importsRoot.isDirectory) {
            "Import root is unavailable"
        }
        val pending = PendingImportTransaction(
            gameId = gameId.value,
            transactionId = transactionId,
            createdAtMillis = nowMillis().coerceAtLeast(0L),
            hadExistingTarget = hadExistingTarget,
            files = files,
        )
        val destination = markerFile(gameId.value, transactionId)
        val temporary = File(importsRoot, destination.name + ".partial")
        temporary.writeText(json.encodeToString(pending), Charsets.UTF_8)
        runCatching {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }.getOrElse {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
        return pending
    }

    fun pending(): List<PendingImportTransaction> {
        if (!importsRoot.isDirectory) return emptyList()
        return importsRoot.listFiles().orEmpty()
            .filter { MARKER_NAME.matches(it.name) && it.isFile && !Files.isSymbolicLink(it.toPath()) }
            .sortedBy { it.name }
            .map { marker ->
                val parsed = try {
                    json.decodeFromString<PendingImportTransaction>(marker.readText(Charsets.UTF_8))
                } catch (error: Exception) {
                    throw IllegalStateException("Pending import journal is malformed", error)
                }
                validateMarkerIdentity(marker, parsed)
                parsed
            }
    }

    fun pendingFor(gameId: GameId): List<PendingImportTransaction> =
        pending().filter { it.gameId == gameId.value }

    fun matchesRegisteredGame(pending: PendingImportTransaction, game: Game?): Boolean {
        if (game == null || game.id.value != pending.gameId) return false
        val registered = game.localContentFiles
            .map { PendingImportFile(it.relativePath, it.sha256) }
            .sortedBy { it.relativePath.lowercase() }
        val expected = pending.files.sortedBy { it.relativePath.lowercase() }
        return registered == expected
    }

    fun targetMatchesPending(pending: PendingImportTransaction): Boolean {
        validatePending(pending)
        val target = targetDirectory(pending.gameId)
        if (!target.isDirectory || Files.isSymbolicLink(target.toPath())) return false
        return pending.files.all { expected ->
            val file = File(importsRoot, expected.relativePath)
            if (!file.isFile || Files.isSymbolicLink(file.toPath())) return@all false
            if (!file.canonicalPath.startsWith(target.canonicalPath + File.separator)) return@all false
            sha256(file) == expected.sha256
        }
    }

    fun confirm(pending: PendingImportTransaction) {
        validatePending(pending)
        val backup = backupDirectory(pending)
        val staging = stagingDirectory(pending)
        if (backup.exists()) deleteOwnedDirectory(backup)
        if (staging.exists()) deleteOwnedDirectory(staging)
        Files.deleteIfExists(markerFile(pending.gameId, pending.transactionId).toPath())
    }

    fun rollback(pending: PendingImportTransaction) {
        validatePending(pending)
        val target = targetDirectory(pending.gameId)
        val backup = backupDirectory(pending)
        val staging = stagingDirectory(pending)

        if (pending.hadExistingTarget) {
            when {
                backup.exists() -> {
                    if (target.exists()) deleteOwnedDirectory(target)
                    moveDirectory(backup, target)
                }
                staging.exists() && target.isDirectory -> {
                    // Marker publication happened, but the directory swap had not started.
                    // The pre-import target is still authoritative.
                }
                else -> {
                    throw IllegalStateException(
                        "Previous import content cannot be restored safely; keep the recovery journal for retry"
                    )
                }
            }
        } else if (target.exists()) {
            deleteOwnedDirectory(target)
        }

        if (staging.exists()) deleteOwnedDirectory(staging)
        if (backup.exists()) deleteOwnedDirectory(backup)
        Files.deleteIfExists(markerFile(pending.gameId, pending.transactionId).toPath())
    }

    suspend fun reconcile(
        createdBeforeMillis: Long = Long.MAX_VALUE,
        gameLookup: suspend (GameId) -> Game?,
    ): ImportRegistrationRecoveryReport {
        require(createdBeforeMillis >= 0L) { "Import recovery cutoff is invalid" }
        var confirmed = 0
        var rolledBack = 0
        var failures = 0
        val records = try {
            pending()
        } catch (_: Exception) {
            return ImportRegistrationRecoveryReport(failures = 1)
        }

        records.filter { it.createdAtMillis < createdBeforeMillis }
            .groupBy { it.gameId }.toSortedMap().forEach { (gameId, transactions) ->
            if (transactions.size != 1) {
                failures += transactions.size
                return@forEach
            }
            val pending = transactions.single()
            runCatching {
                val game = gameLookup(GameId(gameId))
                if (matchesRegisteredGame(pending, game) && targetMatchesPending(pending)) {
                    confirm(pending)
                    confirmed += 1
                } else {
                    rollback(pending)
                    rolledBack += 1
                }
            }.onFailure {
                failures += 1
            }
        }
        return ImportRegistrationRecoveryReport(confirmed, rolledBack, failures)
    }

    private fun validateMarkerIdentity(marker: File, pending: PendingImportTransaction) {
        validatePending(pending)
        require(marker.canonicalFile == marker.absoluteFile) {
            "Pending import marker redirects outside app storage"
        }
        require(marker.canonicalPath.startsWith(importsRoot.canonicalPath + File.separator)) {
            "Pending import marker escaped imports root"
        }
        require(marker.name == markerFile(pending.gameId, pending.transactionId).name) {
            "Pending import marker identity does not match its payload"
        }
    }

    private fun validatePending(pending: PendingImportTransaction) {
        require(pending.schemaVersion == 1) { "Unsupported pending import journal schema" }
        requireSafeGameId(pending.gameId)
        requireUuid(pending.transactionId)
        require(pending.createdAtMillis >= 0L) { "Pending import timestamp is invalid" }
        require(pending.files.isNotEmpty() && pending.files.size <= 64) {
            "Pending import file set is invalid"
        }
        require(pending.files.map { it.relativePath.lowercase() }.distinct().size == pending.files.size) {
            "Pending import file paths must be unique"
        }
        pending.files.forEach { file ->
            require(file.relativePath.startsWith(pending.gameId + "/")) {
                "Pending import file belongs to another game"
            }
            require(
                file.relativePath.none { it == '\\' || it == '\u0000' } &&
                    file.relativePath.split('/').none { it.isBlank() || it == "." || it == ".." }
            ) { "Pending import file path is unsafe" }
            require(file.sha256.matches(Regex("^[a-f0-9]{64}$"))) {
                "Pending import checksum is invalid"
            }
        }
    }

    private fun markerFile(gameId: String, transactionId: String): File =
        File(importsRoot, ".pending-registration-" + gameId + "-" + transactionId + ".json")

    private fun targetDirectory(gameId: String): File = File(importsRoot, gameId)

    private fun backupDirectory(pending: PendingImportTransaction): File =
        File(importsRoot, ".backup-" + pending.gameId + "-" + pending.transactionId)

    private fun stagingDirectory(pending: PendingImportTransaction): File =
        File(importsRoot, ".staging-" + pending.gameId + "-" + pending.transactionId)

    private fun deleteOwnedDirectory(file: File) {
        require(!Files.isSymbolicLink(file.toPath())) {
            "Import transaction directory cannot be a symbolic link"
        }
        require(file.canonicalPath.startsWith(importsRoot.canonicalPath + File.separator)) {
            "Import transaction directory escaped imports root"
        }
        require(!file.exists() || file.isDirectory) {
            "Import transaction path must be a directory"
        }
        if (file.exists()) {
            require(file.deleteRecursively()) { "Could not clean import transaction directory" }
        }
    }

    private fun moveDirectory(source: File, target: File) {
        require(!Files.isSymbolicLink(source.toPath()) && source.isDirectory) {
            "Import rollback source is invalid"
        }
        require(!target.exists()) { "Import rollback target already exists" }
        runCatching {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(source.toPath(), target.toPath())
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun requireSafeGameId(value: String) {
        require(value.matches(Regex("[a-z0-9][a-z0-9-]{0,95}"))) { "Invalid pending import game ID" }
    }

    private fun requireUuid(value: String) {
        require(UUID_PATTERN.matches(value)) { "Invalid pending import transaction ID" }
    }

    companion object {
        private val UUID_PATTERN = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        )
        internal val MARKER_NAME = Regex(
            """\.pending-registration-[a-z0-9][a-z0-9-]{0,95}-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\.json"""
        )
    }
}
