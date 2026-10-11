package com.gamebox.os.importer

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportDirectoryPublisherTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun promotionKeepsPreviousContentInBackupForRegistrationRollback() {
        val root = temporaryFolder.newFolder()
        val target = directoryWithRom(root, "installed", "old game")
        val staging = directoryWithRom(root, "staging", "new game")
        val backup = File(root, "backup")

        ImportDirectoryPublisher.replaceAtomically(target, staging, backup)

        assertEquals("new game", File(target, "game.rom").readText())
        assertEquals("old game", File(backup, "game.rom").readText())
        assertFalse(staging.exists())
    }

    @Test
    fun failedPromotionRestoresPreviousTargetAndDoesNotLeaveBackupStranded() {
        val root = temporaryFolder.newFolder()
        val target = directoryWithRom(root, "installed", "old game")
        val staging = File(root, "missing-staging")
        val backup = File(root, "backup")

        val failure = runCatching {
            ImportDirectoryPublisher.replaceAtomically(target, staging, backup)
        }.exceptionOrNull()

        assertTrue("missing staging must fail promotion", failure != null)
        assertEquals("old game", File(target, "game.rom").readText())
        assertFalse("restored content must not remain in backup", backup.exists())
    }

    private fun directoryWithRom(root: File, name: String, content: String): File =
        File(root, name).apply {
            check(mkdirs())
            File(this, "game.rom").writeText(content)
        }
}
