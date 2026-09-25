package com.gamebox.os.importer

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.LocalContentFile
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportRegistrationJournalTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun unregisteredNewImportIsRemovedOnRecovery() = runBlocking {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val id = GameId("new-game")
        val tx = UUID.randomUUID().toString()
        val sha = "a".repeat(64)
        val journal = ImportRegistrationJournal(files)
        journal.prepare(
            gameId = id,
            transactionId = tx,
            hadExistingTarget = false,
            files = listOf(PendingImportFile("new-game/game.iso", sha)),
        )
        val target = File(imports, id.value).apply {
            mkdirs()
            resolve("game.iso").writeText("new")
        }

        val report = journal.reconcile { null }

        assertEquals(1, report.rolledBack)
        assertEquals(0, report.failures)
        assertFalse(target.exists())
        assertTrue(journal.pending().isEmpty())
    }

    @Test
    fun unregisteredReplacementRestoresPreviousDirectory() = runBlocking {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val id = GameId("replace-game")
        val tx = UUID.randomUUID().toString()
        val journal = ImportRegistrationJournal(files)
        journal.prepare(
            gameId = id,
            transactionId = tx,
            hadExistingTarget = true,
            files = listOf(PendingImportFile("replace-game/game.iso", "b".repeat(64))),
        )
        val backup = File(imports, ".backup-" + id.value + "-" + tx).apply {
            mkdirs()
            resolve("game.iso").writeText("old")
        }
        val target = File(imports, id.value).apply {
            mkdirs()
            resolve("game.iso").writeText("new")
        }

        val report = journal.reconcile { null }

        assertEquals(1, report.rolledBack)
        assertEquals("old", target.resolve("game.iso").readText())
        assertFalse(backup.exists())
        assertTrue(journal.pending().isEmpty())
    }

    @Test
    fun durableRoomRegistrationConfirmsNewFilesInsteadOfRollingBack() = runBlocking {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val id = GameId("registered-game")
        val tx = UUID.randomUUID().toString()
        val sha = sha256("new")
        val journal = ImportRegistrationJournal(files)
        val pending = journal.prepare(
            gameId = id,
            transactionId = tx,
            hadExistingTarget = true,
            files = listOf(PendingImportFile("registered-game/game.iso", sha)),
        )
        val backup = File(imports, ".backup-" + id.value + "-" + tx).apply {
            mkdirs()
            resolve("game.iso").writeText("old")
        }
        val target = File(imports, id.value).apply {
            mkdirs()
            resolve("game.iso").writeText("new")
        }
        val registered = Game(
            id = id,
            title = "Registered",
            platform = "PSP",
            year = 2008,
            genre = "Imported",
            sizeMb = 1,
            state = InstallState.INSTALLED,
            localContentRelativePath = "registered-game/game.iso",
            localContentSha256 = sha,
            localContentMimeType = "application/octet-stream",
            localContentFiles = listOf(
                LocalContentFile("registered-game/game.iso", sha, "application/octet-stream")
            ),
        )
        assertTrue(journal.matchesRegisteredGame(pending, registered))

        val report = journal.reconcile { registered }

        assertEquals(1, report.confirmed)
        assertEquals("new", target.resolve("game.iso").readText())
        assertFalse(backup.exists())
        assertTrue(journal.pending().isEmpty())
    }

    @Test
    fun registeredMetadataWithCorruptCommittedBytesFailsClosed() = runBlocking {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val id = GameId("corrupt-game")
        val tx = UUID.randomUUID().toString()
        val expectedSha = sha256("expected")
        val journal = ImportRegistrationJournal(files)
        journal.prepare(
            id,
            tx,
            true,
            listOf(PendingImportFile("corrupt-game/game.iso", expectedSha)),
        )
        val backup = File(imports, ".backup-" + id.value + "-" + tx).apply {
            mkdirs()
            resolve("game.iso").writeText("old")
        }
        val target = File(imports, id.value).apply {
            mkdirs()
            resolve("game.iso").writeText("corrupt")
        }
        val registered = Game(
            id, "Corrupt", "PSP", 2008, "Imported", 1, InstallState.INSTALLED,
            localContentRelativePath = "corrupt-game/game.iso",
            localContentSha256 = expectedSha,
            localContentMimeType = "application/octet-stream",
            localContentFiles = listOf(
                LocalContentFile("corrupt-game/game.iso", expectedSha, "application/octet-stream")
            ),
        )

        val report = journal.reconcile { registered }

        assertEquals(1, report.failures)
        assertEquals(0, report.rolledBack)
        assertEquals("corrupt", target.resolve("game.iso").readText())
        assertTrue(backup.exists())
        assertEquals(1, journal.pending().size)
    }

    @Test
    fun markerBeforeSwapCanBeDiscardedWithoutBackup() = runBlocking {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val id = GameId("pre-swap")
        val tx = UUID.randomUUID().toString()
        val journal = ImportRegistrationJournal(files)
        journal.prepare(
            id,
            tx,
            true,
            listOf(PendingImportFile("pre-swap/game.iso", sha256("new"))),
        )
        val target = File(imports, id.value).apply {
            mkdirs()
            resolve("game.iso").writeText("old")
        }
        val staging = File(imports, ".staging-" + id.value + "-" + tx).apply {
            mkdirs()
            resolve("game.iso").writeText("new")
        }

        val report = journal.reconcile { null }

        assertEquals(1, report.rolledBack)
        assertEquals("old", target.resolve("game.iso").readText())
        assertFalse(staging.exists())
        assertTrue(journal.pending().isEmpty())
    }

    @Test
    fun missingBackupAfterSwapFailsClosed() = runBlocking {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val id = GameId("ambiguous-game")
        val tx = UUID.randomUUID().toString()
        val journal = ImportRegistrationJournal(files)
        journal.prepare(
            id,
            tx,
            true,
            listOf(PendingImportFile("ambiguous-game/game.iso", sha256("new"))),
        )
        File(imports, id.value).apply {
            mkdirs()
            resolve("game.iso").writeText("new")
        }

        val report = journal.reconcile { null }

        assertEquals(1, report.failures)
        assertEquals(1, journal.pending().size)
    }

    @Test
    fun startupCutoffIgnoresTransactionsCreatedInCurrentProcess() = runBlocking {
        val files = temporary.newFolder("files")
        var now = 1_000L
        val journal = ImportRegistrationJournal(files) { now }
        val oldId = GameId("old-game")
        journal.prepare(
            oldId,
            UUID.randomUUID().toString(),
            false,
            listOf(PendingImportFile("old-game/game.iso", sha256("old"))),
        )
        now = 2_000L
        val currentId = GameId("current-game")
        journal.prepare(
            currentId,
            UUID.randomUUID().toString(),
            false,
            listOf(PendingImportFile("current-game/game.iso", sha256("current"))),
        )

        val report = journal.reconcile(gameLookup = { null }, createdBeforeMillis = 1_500L)

        assertEquals(1, report.rolledBack)
        assertEquals(listOf("current-game"), journal.pending().map { it.gameId })
    }

    @Test
    fun multiplePendingTransactionsForSameGameFailClosed() = runBlocking {
        val files = temporary.newFolder("files")
        val journal = ImportRegistrationJournal(files)
        val id = GameId("same-game")
        journal.prepare(
            id,
            UUID.randomUUID().toString(),
            false,
            listOf(PendingImportFile("same-game/a.iso", "d".repeat(64))),
        )
        journal.prepare(
            id,
            UUID.randomUUID().toString(),
            false,
            listOf(PendingImportFile("same-game/b.iso", "e".repeat(64))),
        )

        val report = journal.reconcile { null }

        assertEquals(2, report.failures)
        assertEquals(2, journal.pending().size)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
