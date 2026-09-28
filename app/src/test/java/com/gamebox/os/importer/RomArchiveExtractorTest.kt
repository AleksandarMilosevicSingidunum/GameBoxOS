package com.gamebox.os.importer

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RomArchiveExtractorTest {
    @Test
    fun extractsSupportedFilesAndRejectsTraversalEntries() {
        val root = Files.createTempDirectory("gamebox-archive-").toFile()
        try {
            val archive = root.resolve("game.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("../outside.nes")); zip.write(byteArrayOf(1)); zip.closeEntry()
                zip.putNextEntry(ZipEntry("legal/Galaxy.nes")); zip.write(byteArrayOf(2, 3)); zip.closeEntry()
            }
            val result = RomArchiveExtractor.extractIfArchive(
                source = archive,
                staging = root.resolve("staging"),
                platform = "NES",
                maxFiles = 64,
                maxBytes = 1024,
            )
            assertEquals(listOf("Galaxy.nes"), result!!.map { it.displayName })
            assertTrue(result.single().file.readBytes().contentEquals(byteArrayOf(2, 3)))
            assertTrue(!root.resolve("outside.nes").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun extractsPspIsoFromSevenZipArchive() {
        val root = Files.createTempDirectory("gamebox-7z-").toFile()
        try {
            val payload = root.resolve("GodOfWar.iso").apply { writeBytes(byteArrayOf(7, 8, 9)) }
            val archive = root.resolve("GodOfWar.7z")
            SevenZOutputFile(archive).use { sevenZip ->
                sevenZip.putArchiveEntry(sevenZip.createArchiveEntry(payload, "nested/${payload.name}"))
                sevenZip.write(payload.readBytes())
                sevenZip.closeArchiveEntry()
            }

            val result = RomArchiveExtractor.extractIfArchive(
                source = archive,
                staging = root.resolve("staging"),
                platform = "PSP",
                maxFiles = 64,
                maxBytes = 1024,
            )

            assertEquals(listOf("GodOfWar.iso"), result!!.map { it.displayName })
            assertTrue(result.single().file.readBytes().contentEquals(byteArrayOf(7, 8, 9)))
        } finally {
            root.deleteRecursively()
        }
    }
}
