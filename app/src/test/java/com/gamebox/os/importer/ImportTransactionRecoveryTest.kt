package com.gamebox.os.importer

import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportTransactionRecoveryTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun restoresBackupWhenProcessStoppedDuringDirectorySwap() {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val gameId = "disc-game"
        val tx = UUID.randomUUID().toString()
        val target = File(imports, gameId).apply {
            mkdirs()
            resolve("game.cue").writeText("new bytes")
        }
        val backup = File(imports, ".backup-$gameId-$tx").apply {
            mkdirs()
            resolve("game.cue").writeText("old bytes")
        }
        val staging = File(imports, ".staging-$gameId-$tx").apply {
            mkdirs()
            resolve("game.cue").writeText("staged bytes")
        }

        val report = ImportTransactionRecovery(files).recover()

        assertEquals(1, report.rolledBackTransactions)
        assertEquals(1, report.cleanedStagingEntries)
        assertEquals(0, report.failures)
        assertEquals("old bytes", target.resolve("game.cue").readText())
        assertFalse(backup.exists())
        assertFalse(staging.exists())
    }

    @Test
    fun restoresBackupWhenTargetWasMovedAwayBeforeReplacement() {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val gameId = "disc-game"
        val tx = UUID.randomUUID().toString()
        val target = File(imports, gameId)
        val backup = File(imports, ".backup-$gameId-$tx").apply {
            mkdirs()
            resolve("game.iso").writeText("old bytes")
        }

        val report = ImportTransactionRecovery(files).recover()

        assertEquals(1, report.rolledBackTransactions)
        assertEquals("old bytes", target.resolve("game.iso").readText())
        assertFalse(backup.exists())
    }

    @Test
    fun removesAbandonedStagingWithoutTouchingExistingTarget() {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val gameId = "disc-game"
        val tx = UUID.randomUUID().toString()
        val target = File(imports, gameId).apply {
            mkdirs()
            resolve("game.iso").writeText("current bytes")
        }
        val staging = File(imports, ".staging-$gameId-$tx").apply {
            mkdirs()
            resolve("game.iso").writeText("staged bytes")
        }

        val report = ImportTransactionRecovery(files).recover()

        assertEquals(0, report.rolledBackTransactions)
        assertEquals(1, report.cleanedStagingEntries)
        assertEquals("current bytes", target.resolve("game.iso").readText())
        assertFalse(staging.exists())
    }

    @Test
    fun removesSingleFilePartialsButLeavesUnrelatedFiles() {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val game = File(imports, "portable").apply { mkdirs() }
        val partial = game.resolve("game.iso.partial").apply { writeText("partial") }
        val real = game.resolve("notes.partial.txt").apply { writeText("keep") }
        val outside = File(files, "outside.partial").apply { writeText("keep") }

        val report = ImportTransactionRecovery(files).recover()

        assertEquals(1, report.cleanedPartialFiles)
        assertFalse(partial.exists())
        assertTrue(real.exists())
        assertTrue(outside.exists())
    }

    @Test
    fun journaledTransactionIsLeftForRoomReconciliation() {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val gameId = com.gamebox.os.domain.GameId("journal-game")
        val tx = UUID.randomUUID().toString()
        ImportRegistrationJournal(files).prepare(
            gameId,
            tx,
            hadExistingTarget = true,
            files = listOf(PendingImportFile("journal-game/game.iso", "f".repeat(64))),
        )
        val target = File(imports, gameId.value).apply {
            mkdirs()
            resolve("game.iso").writeText("new")
        }
        val backup = File(imports, ".backup-" + gameId.value + "-" + tx).apply {
            mkdirs()
            resolve("game.iso").writeText("old")
        }
        val staging = File(imports, ".staging-" + gameId.value + "-" + tx).apply {
            mkdirs()
            resolve("game.iso").writeText("staged")
        }

        val report = ImportTransactionRecovery(files).recover()

        assertEquals(0, report.rolledBackTransactions)
        assertEquals(0, report.cleanedStagingEntries)
        assertEquals("new", target.resolve("game.iso").readText())
        assertEquals("old", backup.resolve("game.iso").readText())
        assertTrue(staging.exists())
    }

    @Test
    fun markerShapedDirectoryDoesNotProtectInterruptedTransaction() {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val gameId = "fake-marker"
        val tx = UUID.randomUUID().toString()
        val target = File(imports, gameId).apply {
            mkdirs()
            resolve("game.iso").writeText("new")
        }
        val backup = File(imports, ".backup-" + gameId + "-" + tx).apply {
            mkdirs()
            resolve("game.iso").writeText("old")
        }
        File(imports, ".pending-registration-" + gameId + "-" + tx + ".json").mkdirs()

        val report = ImportTransactionRecovery(files).recover()

        assertEquals(1, report.rolledBackTransactions)
        assertEquals("old", target.resolve("game.iso").readText())
        assertFalse(backup.exists())
    }

    @Test
    fun transactionNameRequiresLiteralLeadingDot() {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val gameId = "literal-dot"
        val tx = UUID.randomUUID().toString()
        val unrelated = File(imports, "xbackup-" + gameId + "-" + tx).apply {
            mkdirs()
            resolve("keep.iso").writeText("keep")
        }

        val report = ImportTransactionRecovery(files).recover()

        assertEquals(0, report.rolledBackTransactions)
        assertTrue(unrelated.resolve("keep.iso").isFile)
    }

    @Test
    fun malformedTransactionNamesAreIgnored() {
        val files = temporary.newFolder("files")
        val imports = File(files, "imports").apply { mkdirs() }
        val unrelated = File(imports, ".backup-not-a-valid-transaction").apply {
            mkdirs()
            resolve("keep.txt").writeText("keep")
        }

        val report = ImportTransactionRecovery(files).recover()

        assertEquals(0, report.rolledBackTransactions)
        assertEquals(0, report.failures)
        assertTrue(unrelated.resolve("keep.txt").isFile)
    }
}
