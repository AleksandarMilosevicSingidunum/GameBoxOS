package com.gamebox.os

import com.gamebox.os.storage.FileContentUninstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileContentUninstallerTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun uninstall_removesOnlyExactContentAndRetainsSave() {
        val installRoot = temporaryFolder.newFolder("installed")
        val content = installRoot.resolve("retro/test/content/test.txt")
        content.parentFile.mkdirs()
        content.writeText("TEST")
        val save = temporaryFolder.newFolder("saves").resolve("retro-test/save.dat")
        save.parentFile.mkdirs()
        save.writeText("SAVE")

        val removed = FileContentUninstaller(installRoot)
            .uninstall("retro/test/content/test.txt")

        assertTrue(removed)
        assertFalse(content.exists())
        assertEquals("SAVE", save.readText())
    }

    @Test fun traversalAndDirectoryDeletion_areRejected() {
        val installRoot = temporaryFolder.newFolder("installed")
        assertThrows(IllegalArgumentException::class.java) {
            FileContentUninstaller(installRoot).uninstall("../saves/save.dat")
        }
        assertThrows(IllegalArgumentException::class.java) {
            FileContentUninstaller(installRoot).uninstall(".")
        }
    }

    @Test fun missingContent_isIdempotent() {
        assertFalse(
            FileContentUninstaller(temporaryFolder.newFolder("installed"))
                .uninstall("retro/test/content/missing.bin")
        )
    }
}
