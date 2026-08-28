package com.gamebox.os

import com.gamebox.os.download.StagingTarget
import com.gamebox.os.download.TransferEngine
import com.gamebox.os.download.TransferResult
import com.gamebox.os.download.TransferSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class TransferEngineTest {
    private val checksum = "94ee059335e587e501cc4bf90613e0814f00a7b08bc7c648fd865a2af6a22cc2"

    @Test fun verifiedTransfer_commitsOnlyAfterChecksumPasses() {
        val staging = MemoryStaging()
        val progress = mutableListOf<Long>()
        val result = TransferEngine(bufferSize = 2).transfer(
            source("TEST".toByteArray(), checksum),
            staging,
            maxBytes = 100,
            onProgress = { progress += it.bytesTransferred }
        )

        assertEquals(TransferResult.Success(4), result)
        assertTrue(staging.committed)
        assertFalse(staging.discarded)
        assertEquals(listOf(2L, 4L), progress)
    }

    @Test fun mismatch_discardsAndNeverCommits() {
        val staging = MemoryStaging()
        val result = TransferEngine().transfer(
            source("OTHER".toByteArray(), checksum),
            staging,
            maxBytes = 100
        )

        assertTrue(result is TransferResult.ChecksumMismatch)
        assertTrue(staging.discarded)
        assertFalse(staging.committed)
    }

    @Test fun cancellation_discardsPartialStagingData() {
        val staging = MemoryStaging()
        var checks = 0
        val result = TransferEngine(bufferSize = 2).transfer(
            source("TEST".toByteArray(), checksum),
            staging,
            maxBytes = 100,
            isCancelled = { checks++ > 0 }
        )

        assertEquals(TransferResult.Cancelled(2), result)
        assertTrue(staging.discarded)
        assertFalse(staging.committed)
    }

    @Test fun undeclaredOversizeStream_isStoppedAndDiscarded() {
        val staging = MemoryStaging()
        val result = TransferEngine(bufferSize = 2).transfer(
            source("TEST".toByteArray(), checksum, declaredSize = null),
            staging,
            maxBytes = 3
        )

        assertEquals(TransferResult.SizeLimitExceeded(3), result)
        assertTrue(staging.discarded)
        assertFalse(staging.committed)
    }

    @Test fun declaredOversize_isRejectedBeforeOpeningSource() {
        val staging = MemoryStaging()
        var opened = false
        val source = object : TransferSource {
            override val totalBytes = 101L
            override val expectedSha256 = checksum
            override fun openInput(): InputStream {
                opened = true
                return ByteArrayInputStream(byteArrayOf())
            }
        }

        val result = TransferEngine().transfer(source, staging, maxBytes = 100)

        assertEquals(TransferResult.SizeLimitExceeded(100), result)
        assertFalse(opened)
        assertTrue(staging.discarded)
    }

    private fun source(
        bytes: ByteArray,
        expected: String,
        declaredSize: Long? = bytes.size.toLong()
    ) = object : TransferSource {
        override val totalBytes = declaredSize
        override val expectedSha256 = expected
        override fun openInput(): InputStream = ByteArrayInputStream(bytes)
    }

    private class MemoryStaging : StagingTarget {
        private val buffer = ByteArrayOutputStream()
        var committed = false
        var discarded = false

        override fun openOutput(): OutputStream = buffer
        override fun openInput(): InputStream = ByteArrayInputStream(buffer.toByteArray())
        override fun commit() {
            check(!discarded)
            committed = true
        }
        override fun discard() {
            discarded = true
            buffer.reset()
        }
    }
}
