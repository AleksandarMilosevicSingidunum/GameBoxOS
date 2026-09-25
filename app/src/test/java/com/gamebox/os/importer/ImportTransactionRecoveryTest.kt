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
