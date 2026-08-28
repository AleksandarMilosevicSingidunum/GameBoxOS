package com.gamebox.os

import com.gamebox.os.download.FileStagingTarget
import com.gamebox.os.download.TransferEngine
import com.gamebox.os.download.TransferResult
import com.gamebox.os.download.TransferSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.InputStream

class FileStagingTargetTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val checksum = "94ee059335e587e501cc4bf90613e0814f00a7b08bc7c648fd865a2af6a22cc2"

    @Test fun verifiedTransfer_atomicallyPromotesStagedFile() {
        val target = FileStagingTarget(temporaryFolder.root, "retro/test/content/test.txt")

        val result = TransferEngine().transfer(source("TEST", checksum), target, maxBytes = 100)

        assertEquals(TransferResult.Success(4), result)
        assertEquals("TEST", target.finalFile.readText())
        assertFalse(target.stagingFile.exists())
    }

    @Test fun mismatch_leavesNeitherFinalNorStagedFile() {
        val target = FileStagingTarget(temporaryFolder.root, "retro/test/content/test.txt")

        val result = TransferEngine().transfer(source("OTHER", checksum), target, maxBytes = 100)

        assertTrue(result is TransferResult.ChecksumMismatch)
        assertFalse(target.finalFile.exists())
        assertFalse(target.stagingFile.exists())
    }

    @Test fun traversalPath_isRejectedBeforeWriting() {
        val result = runCatching {
            FileStagingTarget(temporaryFolder.root, "../outside.txt")
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertFalse(temporaryFolder.root.parentFile.resolve("outside.txt").exists())
    }

    @Test fun absolutePath_isRejectedBeforeWriting() {
        val result = runCatching {
            FileStagingTarget(temporaryFolder.root, temporaryFolder.newFile().absolutePath)
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    private fun source(content: String, expected: String) = object : TransferSource {
        private val bytes = content.toByteArray()
        override val totalBytes = bytes.size.toLong()
        override val expectedSha256 = expected
        override fun openInput(): InputStream = ByteArrayInputStream(bytes)
    }
}
