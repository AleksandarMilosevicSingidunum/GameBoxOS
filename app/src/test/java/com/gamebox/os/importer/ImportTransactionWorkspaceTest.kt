package com.gamebox.os.importer

import com.gamebox.os.domain.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ImportTransactionWorkspaceTest {
    @Test
    fun workspaceUsesOneConsoleScopedTargetAndKeepsTemporaryPathsInsideRoot() {
        val appFiles = Files.createTempDirectory("gamebox-import-workspace").toFile()
        try {
            val workspace = ImportTransactionWorkspace.create(
                filesDirectory = appFiles,
                gameId = GameId("god-of-war-test"),
                platform = "PSP",
                transactionId = "tx-123",
            )

            assertEquals("imports/psp/god-of-war-test/", workspace.expectedRelativePrefix)
            assertEquals(
                workspace.root.resolve("psp/god-of-war-test").canonicalFile,
                workspace.targetDirectory,
            )
            listOf(workspace.targetDirectory, workspace.stagingDirectory, workspace.backupDirectory).forEach {
                assertTrue(it.canonicalPath.startsWith(workspace.root.canonicalPath + File.separator))
            }
            workspace.prepareDirectories()
            assertTrue(workspace.stagingDirectory.isDirectory)
            assertTrue(workspace.targetDirectory.parentFile!!.isDirectory)
        } finally {
            appFiles.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun workspaceRejectsInvalidGameIdBeforeResolvingPaths() {
        val appFiles = Files.createTempDirectory("gamebox-import-invalid").toFile()
        try {
            ImportTransactionWorkspace.create(appFiles, GameId("../escape"), "PSP", "tx")
        } finally {
            appFiles.deleteRecursively()
        }
    }
}
