package com.gamebox.os.importer

import java.io.ByteArrayInputStream
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedArchiveSourceCopyTest {
    @Test
    fun copiesWithinLimitAndReportsFinalProgress() {
        val root = Files.createTempDirectory("gamebox-archive-source-copy-").toFile()
        try {
            val destination = root.resolve("source.zip")
            val progress = mutableListOf<Long>()

            copyBoundedArchiveSource(
                input = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)),
                destination = destination,
                maxBytes = 4,
                onProgress = { progress += it },
            )

            assertArrayEquals(byteArrayOf(1, 2, 3, 4), destination.readBytes())
            assertEquals(4L, progress.last())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun oversizedSourceDeletesAnyPartialStagingFile() {
        val root = Files.createTempDirectory("gamebox-archive-source-limit-").toFile()
        try {
            val destination = root.resolve("source.7z")
            val input = object : ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5, 6)) {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                    super.read(buffer, offset, minOf(length, 2))
            }

            assertThrows(IllegalArgumentException::class.java) {
                copyBoundedArchiveSource(input, destination, maxBytes = 3, onProgress = {})
            }

            assertFalse(destination.exists())
            assertEquals(4L, input.available().let { 6L - it })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun progressCallbackFailureDeletesCompletedTemporaryArchive() {
        val root = Files.createTempDirectory("gamebox-archive-progress-failure-").toFile()
        try {
            val destination = root.resolve("source.zip")

            assertThrows(IllegalStateException::class.java) {
                copyBoundedArchiveSource(
                    input = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)),
                    destination = destination,
                    maxBytes = 4,
                    onProgress = { throw IllegalStateException("cancel progress") },
                )
            }

            assertFalse(destination.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
