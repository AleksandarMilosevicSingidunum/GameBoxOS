package com.gamebox.os.importer

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class RomArchiveExtractorTest {
    @Test
    fun throttlesArchiveProgressToMegabyteBoundariesAndReportsFinalBytes() {
        val root = Files.createTempDirectory("gamebox-archive-progress-").toFile()
        try {
            val payload = ByteArray(2 * 1024 * 1024 + 17)
            val archive = root.resolve("large-game.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("large-game.nes"))
                zip.write(payload)
                zip.closeEntry()
            }
            val progress = mutableListOf<Pair<Long, Long?>>()

            RomArchiveExtractor.extractIfArchive(
                source = archive,
                staging = root.resolve("staging"),
                platform = "NES",
                maxFiles = 64,
                maxBytes = payload.size.toLong(),
                onProgress = { processed, total -> progress += processed to total },
            )

            assertTrue("progress updates should be coalesced", progress.size <= 3)
            assertEquals(payload.size.toLong() to payload.size.toLong(), progress.last())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun extractsSupportedFilesAndRejectsTraversalEntries() {
        val root = Files.createTempDirectory("gamebox-archive-").toFile()
        try {
            val progress = mutableListOf<Pair<Long, Long?>>()
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
                onProgress = { processed, total -> progress += processed to total },
            )
            assertEquals(listOf("Galaxy.nes"), result!!.map { it.displayName })
            assertTrue(result.single().file.readBytes().contentEquals(byteArrayOf(2, 3)))
            assertEquals(2L to 2L, progress.last())
            assertTrue(!root.resolve("outside.nes").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun extractsPspIsoFromSevenZipArchive() {
        val root = Files.createTempDirectory("gamebox-7z-").toFile()
        try {
            val progress = mutableListOf<Pair<Long, Long?>>()
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
                onProgress = { processed, total -> progress += processed to total },
            )

            assertEquals(listOf("GodOfWar.iso"), result!!.map { it.displayName })
            assertTrue(result.single().file.readBytes().contentEquals(byteArrayOf(7, 8, 9)))
            assertEquals(3L to 3L, progress.last())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun extractsPs2IsoFromSevenZipArchive() {
        val root = Files.createTempDirectory("gamebox-ps2-7z-").toFile()
        try {
            val payload = root.resolve("test-disc.iso").apply { writeBytes(byteArrayOf(4, 5, 6, 7)) }
            val archive = root.resolve("test-disc.7z")
            SevenZOutputFile(archive).use { sevenZip ->
                sevenZip.putArchiveEntry(sevenZip.createArchiveEntry(payload, "nested/${payload.name}"))
                sevenZip.write(payload.readBytes())
                sevenZip.closeArchiveEntry()
            }

            val result = RomArchiveExtractor.extractIfArchive(
                source = archive,
                staging = root.resolve("staging"),
                platform = "PS2",
                maxFiles = 64,
                maxBytes = 1024,
            )

            assertEquals(listOf("test-disc.iso"), result!!.map { it.displayName })
            assertTrue(result.single().file.readBytes().contentEquals(byteArrayOf(4, 5, 6, 7)))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun removesAllExpandedFilesWhenArchiveExceedsByteLimit() {
        val root = Files.createTempDirectory("gamebox-archive-limit-").toFile()
        try {
            val archive = root.resolve("oversized.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("first.nes")); zip.write(byteArrayOf(1, 2)); zip.closeEntry()
                zip.putNextEntry(ZipEntry("second.nes")); zip.write(byteArrayOf(3, 4, 5)); zip.closeEntry()
            }
            val staging = root.resolve("staging")

            assertThrows(IllegalArgumentException::class.java) {
                RomArchiveExtractor.extractIfArchive(
                    source = archive,
                    staging = staging,
                    platform = "NES",
                    maxFiles = 64,
                    maxBytes = 3,
                )
            }

            assertFalse("failed extraction must not leave earlier or partial files", staging.resolve("archive-content").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun removesExtractionDirectoryWhenArchiveIsCorrupt() {
        val root = Files.createTempDirectory("gamebox-archive-corrupt-").toFile()
        try {
            val archive = root.resolve("broken.zip").apply { writeBytes("not a ZIP archive".toByteArray()) }
            val staging = root.resolve("staging")

            assertThrows(Exception::class.java) {
                RomArchiveExtractor.extractIfArchive(
                    source = archive,
                    staging = staging,
                    platform = "NES",
                    maxFiles = 64,
                    maxBytes = 1024,
                )
            }

            assertFalse("corrupt archives must not leave extraction staging", staging.resolve("archive-content").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun removesEarlierAndPartialFilesWhenLaterZipEntryIsCorrupt() {
        val root = Files.createTempDirectory("gamebox-archive-crc-").toFile()
        try {
            val archive = root.resolve("late-corruption.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("first.nes"))
                zip.write(byteArrayOf(1, 2))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("second.nes"))
                zip.write(ByteArray(256) { it.toByte() })
                zip.closeEntry()
            }

            val bytes = archive.readBytes()
            val secondName = "second.nes".toByteArray()
            val nameOffset = bytes.indices.first { offset ->
                offset + secondName.size <= bytes.size &&
                    secondName.indices.all { bytes[offset + it] == secondName[it] }
            }
            val localHeaderOffset = nameOffset - 30
            val extraLength = (bytes[localHeaderOffset + 28].toInt() and 0xff) or
                ((bytes[localHeaderOffset + 29].toInt() and 0xff) shl 8)
            val payloadOffset = nameOffset + secondName.size + extraLength
            bytes[payloadOffset] = 0xff.toByte()
            archive.writeBytes(bytes)

            val staging = root.resolve("staging")
            assertThrows(Exception::class.java) {
                RomArchiveExtractor.extractIfArchive(
                    source = archive,
                    staging = staging,
                    platform = "NES",
                    maxFiles = 64,
                    maxBytes = 1024,
                )
            }

            assertFalse("corrupt later entry must remove earlier extracted files", staging.resolve("archive-content").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun doesNotTreatNestedArchiveAsAnInstalledGameAndCleansStaging() {
        val root = Files.createTempDirectory("gamebox-nested-archive-").toFile()
        try {
            val archive = root.resolve("outer.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("inner.zip"))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }
            val staging = root.resolve("staging")

            assertThrows(IllegalArgumentException::class.java) {
                RomArchiveExtractor.extractIfArchive(
                    source = archive,
                    staging = staging,
                    platform = "PS2",
                    maxFiles = 64,
                    maxBytes = 1024,
                )
            }

            assertFalse("nested archive must not be promoted as a ROM", staging.resolve("archive-content").exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
