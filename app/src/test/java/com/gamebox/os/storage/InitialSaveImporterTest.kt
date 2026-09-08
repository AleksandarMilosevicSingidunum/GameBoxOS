package com.gamebox.os.storage

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InitialSaveImporterTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun importsRealBytesAndRefusesOverwrite() {
        val importer = InitialSaveImporter(temporary.root)
        assertEquals(4L, importer.importNew("game/save.dat", "save".byteInputStream()))
        assertThrows(IllegalStateException::class.java) {
            importer.importNew("game/save.dat", "replacement".byteInputStream())
        }
        assertEquals("save", temporary.root.resolve("game/save.dat").readText())
    }

    @Test fun rejectsEmptyOversizedAndEscapingFilesWithoutPartialSave() {
        val importer = InitialSaveImporter(temporary.root, 4)
        for (payload in listOf("", "12345")) {
            assertThrows(IllegalArgumentException::class.java) {
                importer.importNew("game/save.dat", payload.byteInputStream())
            }
            assertFalse(temporary.root.resolve("game/save.dat").exists())
            assertTrue(temporary.root.resolve("game").listFiles()!!.isEmpty())
        }
        assertThrows(IllegalArgumentException::class.java) {
            importer.importNew("../outside.dat", "save".byteInputStream())
        }
    }

    @Test fun cleansStagingWhenReadingFails() {
        val importer = InitialSaveImporter(temporary.root)
        val broken = object : java.io.InputStream() {
            override fun read(): Int = throw java.io.IOException("read failed")
        }
        assertThrows(java.io.IOException::class.java) { importer.importNew("game/save.dat", broken) }
        assertTrue(temporary.root.resolve("game").listFiles()!!.isEmpty())
    }
}
