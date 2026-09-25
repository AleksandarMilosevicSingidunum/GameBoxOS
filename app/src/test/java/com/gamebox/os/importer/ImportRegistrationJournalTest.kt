package com.gamebox.os.importer

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.LocalContentFile
import java.io.File
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
        val sha = "c".repeat(64)
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
}
