package com.gamebox.os.importer

import java.io.File
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserDownloadTransferSchedulerTest {
    private class TestConnection(private val body: ByteArray) : HttpURLConnection(URL("http://localhost/")) {
        var disconnected = false
            private set

        override fun disconnect() {
            disconnected = true
        }

        override fun usingProxy() = false

        override fun connect() = Unit

        override fun getInputStream() = ByteArrayInputStream(body)
    }

    @Test
    fun redirectsStayOnHttpsAndCredentialsStayOnTheOriginalHost() {
        val source = URI("https://dl.vimm.net/start")
        assertEquals(URI("https://dl.vimm.net/file.zip"), resolveSecureBrowserDownloadRedirect(source, "/file.zip"))
        assertTrue(browserDownloadMayForwardCredentials(source, URI("https://dl.vimm.net/file.zip")))
        assertFalse(browserDownloadMayForwardCredentials(source, URI("https://cdn.example.net/file.zip")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun redirectCannotDowngradeToHttp() {
        resolveSecureBrowserDownloadRedirect(URI("https://dl.vimm.net/start"), "http://example.net/file.zip")
    }

    @Test
    fun resumedRangeMustStartAtRequestedOffset() {
        assertEquals(
            BrowserDownloadRange(start = 5L, end = 9L, total = 10L),
            parseBrowserDownloadRange("bytes 5-9/10", requestedOffset = 5L),
        )
        assertThrows(IllegalArgumentException::class.java) {
            parseBrowserDownloadRange("bytes 4-9/10", requestedOffset = 5L)
        }
    }

    @Test
    fun wrongResumedBodyLengthDeletesPartialFile() {
        val partial = File.createTempFile("gamebox-range-", ".part")
        try {
            partial.writeBytes(byteArrayOf(1, 2, 3))
            assertThrows(IllegalStateException::class.java) {
                validateBrowserDownloadRangeBody(
                    BrowserDownloadRange(start = 3L, end = 6L, total = 10L),
                    receivedBytes = 3L,
                    partial = partial,
                )
            }
            assertFalse(partial.exists())
        } finally {
            partial.delete()
        }
    }

    @Test
    fun stoppingTransferQueuesItButDoesNotRollBackImportOrTerminalState() {
        assertEquals(BrowserDownloadStatus.QUEUED, browserDownloadStatusAfterTransferStop(BrowserDownloadStatus.DOWNLOADING))
        assertEquals(BrowserDownloadStatus.QUEUED, browserDownloadStatusAfterTransferStop(BrowserDownloadStatus.PAUSED))
        listOf(BrowserDownloadStatus.IMPORTING, BrowserDownloadStatus.IMPORTED, BrowserDownloadStatus.FAILED,
            BrowserDownloadStatus.CANCELLED).forEach { status ->
            assertEquals(status, browserDownloadStatusAfterTransferStop(status))
        }
    }

    @Test
    fun truncatedDownloadIsNotPromotedAndKeepsExistingDestination() {
        val directory = Files.createTempDirectory("gamebox-transfer-").toFile()
        val partial = File(directory, "game.part").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val destination = File(directory, "game.zip").apply { writeText("existing") }
        try {
            assertThrows(IllegalStateException::class.java) {
                finalizeBrowserDownload(partial, destination, 4L)
            }
            assertTrue(partial.isFile)
            assertEquals("existing", destination.readText())
        } finally {
            partial.delete()
            destination.delete()
            directory.delete()
        }
    }

    @Test
    fun completeKnownAndUnknownLengthDownloadsCanBePromoted() {
        val directory = Files.createTempDirectory("gamebox-transfer-").toFile()
        val partial = File(directory, "game.part").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val destination = File(directory, "game.zip")
        try {
            assertEquals(destination, finalizeBrowserDownload(partial, destination, 3L))
            assertTrue(destination.isFile)

            val unknownLengthPartial = File(directory, "unknown.part").apply { writeBytes(byteArrayOf(4)) }
            assertEquals(
                File(directory, "unknown.zip"),
                finalizeBrowserDownload(unknownLengthPartial, File(directory, "unknown.zip"), -1L),
            )
        } finally {
            directory.listFiles()?.forEach(File::delete)
            directory.delete()
        }
    }

    @Test
    fun shortResponseBodyIsNotPromoted() {
        val directory = Files.createTempDirectory("gamebox-response-transfer-").toFile()
        val partial = File(directory, "game.part")
        val destination = File(directory, "game.zip")
        val connection = TestConnection(byteArrayOf(1, 2, 3))
        try {
            receiveBrowserDownload(connection, partial, false, 0L, 4L, 1024L, { false }) { _, _ -> }
            assertTrue(connection.disconnected)
            assertThrows(Exception::class.java) {
                finalizeBrowserDownload(partial, destination, 4L)
            }
            assertTrue(partial.isFile)
            assertEquals(3L, partial.length())
            assertFalse(destination.exists())
        } finally {
            partial.delete()
            destination.delete()
            directory.delete()
        }
    }

    @Test
    fun completeResponseBodyIsPromoted() {
        val bytes = byteArrayOf(1, 2, 3)
        val directory = Files.createTempDirectory("gamebox-response-transfer-").toFile()
        val partial = File(directory, "game.part")
        val destination = File(directory, "game.zip")
        val connection = TestConnection(bytes)
        try {
            receiveBrowserDownload(connection, partial, false, 0L, bytes.size.toLong(), 1024L, { false }) { _, _ -> }
            assertTrue(connection.disconnected)
            assertEquals(destination, finalizeBrowserDownload(partial, destination, bytes.size.toLong()))
            assertArrayEquals(bytes, destination.readBytes())
        } finally {
            partial.delete()
            destination.delete()
            directory.delete()
        }
    }
}
