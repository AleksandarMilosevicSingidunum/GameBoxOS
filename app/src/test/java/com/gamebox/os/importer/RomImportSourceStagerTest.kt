package com.gamebox.os.importer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.EmptyCoroutineContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RomImportSourceStagerTest {
    @Test
    fun revokedDocumentAccessReturnsUnavailableWithoutStagingFiles() {
        val staging = Files.createTempDirectory("gamebox-revoked-import-").toFile()
        try {
            val progress = mutableListOf<RomImportStage>()
            val stager = RomImportSourceStager(
                maxBytes = 1024,
                progress = RomImportProgressReporter(EmptyCoroutineContext) { progress += it.stage },
            )

            val result = stager.stage(
                openInputStream = { throw SecurityException("document permission revoked") },
                safeName = "game.nes",
                sourceSize = 12,
                sourceIndex = 1,
                sourceCount = 1,
                stagedFileCount = 0,
                bytesAlreadyStaged = 0,
                platform = "NES",
                staging = staging,
                expectedPrefix = "imports/nes/game/",
            )

            assertNull(result)
            assertEquals(listOf(RomImportStage.COPYING_AND_VERIFYING), progress)
            assertTrue(staging.listFiles().isNullOrEmpty())
        } finally {
            staging.deleteRecursively()
        }
    }

    @Test
    fun duplicatePayloadNamesAcrossArchivesAreRejectedWithoutOverwritingFirstFile() {
        val staging = Files.createTempDirectory("gamebox-archive-name-collision-").toFile()
        try {
            val stager = RomImportSourceStager(
                maxBytes = 1024,
                progress = RomImportProgressReporter(EmptyCoroutineContext) {},
            )
            val firstBytes = byteArrayOf(1, 2, 3)
            val firstArchive = zipRom("same.nes", firstBytes)
            val secondArchive = zipRom("same.nes", byteArrayOf(9, 8, 7))
            fun stage(name: String, bytes: ByteArray, index: Int, stagedBytes: Long = 0L) = stager.stage(
                openInputStream = { ByteArrayInputStream(bytes) },
                safeName = name,
                sourceSize = bytes.size.toLong(),
                sourceIndex = index,
                sourceCount = 2,
                stagedFileCount = if (index == 1) 0 else 1,
                bytesAlreadyStaged = stagedBytes,
                platform = "NES",
                staging = staging,
                expectedPrefix = "imports/nes/fixture/",
            )

            val first = requireNotNull(stage("first.zip", firstArchive, 1))
            val payload = staging.resolve("same.nes")
            assertTrue(payload.readBytes().contentEquals(firstBytes))

            val error = assertThrows(IllegalArgumentException::class.java) {
                stage("second.zip", secondArchive, 2, first.single().hashes.sizeBytes)
            }

            assertTrue(error.message.orEmpty().contains("duplicate game file name"))
            assertTrue(
                "a later archive must not replace the already staged payload",
                payload.readBytes().contentEquals(firstBytes),
            )
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun zipRom(name: String, bytes: ByteArray): ByteArray = ByteArrayOutputStream().use { archive ->
        ZipOutputStream(archive).use { zip ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
        archive.toByteArray()
    }
}
