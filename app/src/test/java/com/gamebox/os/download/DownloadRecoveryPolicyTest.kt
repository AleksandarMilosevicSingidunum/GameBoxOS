package com.gamebox.os.download

import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DownloadRecoveryPolicyTest {
    @get:Rule val temporary = TemporaryFolder()
    private val payload = "homebrew".toByteArray()
    private val checksum = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }

    @Test fun permanentHttpErrorsDoNotRetryButTransientErrorsDo() {
        for (status in listOf(401, 403, 404, 302, 400)) {
            val failure = transferFailure(DownloadRequestException(status), 3)
            assertFalse("HTTP " + status, shouldRetryTransfer(failure, 0))
        }
        for (status in listOf(408, 429, 500, 503)) {
            val failure = transferFailure(DownloadRequestException(status), 3)
            assertTrue("HTTP " + status, shouldRetryTransfer(failure, 0))
            assertTrue(shouldRetryTransfer(failure, 2))
            assertFalse(shouldRetryTransfer(failure, 3))
        }
        assertTrue(transferFailure(java.net.SocketTimeoutException(), 3).retryable)
        assertFalse(transferFailure(IllegalArgumentException("bad credentials"), 3).retryable)
    }

    @Test fun earlyEofPreservesPartialAndNextAttemptResumesThenVerifies() {
        val target = FileStagingTarget(temporary.newFolder(), "game.nes")
        val first = source(Connection(200, payload.copyOfRange(0, 3), payload.size.toLong()))
        val failure = transfer(first, target) as ResumableTransferResult.Failed
        assertTrue(failure.retryable)
        assertEquals(3L, target.stagedBytes)
        assertFalse(target.finalFile.exists())
        val resumed = Connection(206, payload.copyOfRange(3, payload.size),
            (payload.size - 3).toLong(), "bytes 3-7/8")
        assertEquals(ResumableTransferResult.Success(8), transfer(source(resumed), target))
        assertEquals("bytes=3-", resumed.getRequestProperty("Range"))
        assertArrayEquals(payload, target.finalFile.readBytes())
    }

    @Test fun rejectedRangeRestartsOnceInsteadOfRetryingSameOffsetForever() {
        val target = FileStagingTarget(temporary.newFolder(), "game.nes")
        target.openOutput().use { it.write(payload, 0, 3) }
        val rejected = Connection(416, byteArrayOf(), 0)
        val full = Connection(200, payload, 8)
        val responses = ArrayDeque(listOf(rejected, full))
        val source = HttpsTransferSource("https://example.test/game.nes", null, checksum,
            connectionFactory = { responses.removeFirst() })
        assertEquals(ResumableTransferResult.Success(8), transfer(source, target))
        assertTrue(rejected.disconnected)
        assertNull(full.getRequestProperty("Range"))
    }

    @Test fun malformedRangeIsClosedAndDoesNotReachFileAppend() {
        listOf("bytes 3-2/8", "bytes 3-8/8", "bytes 4-7/8", "bytes 3-7/99999999999999999999999").forEach { range ->
            val response = Connection(206, payload, 5, range)
            try {
                source(response).openInputAt(3)
                fail(range)
            } catch (expected: RangeNotSupportedException) {
                assertTrue(response.disconnected)
            }
        }
    }

    @Test fun cancellationIsNotConvertedToRetryFailure() {
        try {
            transferFailure(kotlinx.coroutines.CancellationException("cancelled"), 3)
            fail("Cancellation must propagate")
        } catch (expected: kotlinx.coroutines.CancellationException) { }
    }

    private fun source(connection: Connection) = HttpsTransferSource(
        "https://example.test/game.nes", null, checksum, connectionFactory = { connection })
    private fun transfer(source: HttpsTransferSource, target: FileStagingTarget) =
        ResumableTransferEngine().transfer(source, target, 1024, { false }, {})

    private class Connection(
        private val status: Int, private val body: ByteArray,
        private val length: Long, private val range: String? = null,
    ) : HttpURLConnection(URL("https://example.test/game.nes")) {
        var disconnected = false
        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getContentLengthLong() = length
        override fun getHeaderField(name: String): String? = if (name == "Content-Range") range else null
        override fun getInputStream() = ByteArrayInputStream(body)
    }
}
