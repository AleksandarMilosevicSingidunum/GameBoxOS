package com.gamebox.os

import com.gamebox.os.download.Sha256Verifier
import com.gamebox.os.download.VerificationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class Sha256VerifierTest {
    private val verifier = Sha256Verifier()
    private val testChecksum = "94ee059335e587e501cc4bf90613e0814f00a7b08bc7c648fd865a2af6a22cc2"

    @Test fun matchingChecksum_isVerified() {
        val result = verifier.verify(ByteArrayInputStream("TEST".toByteArray()), testChecksum)
        assertEquals(VerificationResult.Verified, result)
    }

    @Test fun mismatch_returnsActualDigest() {
        val result = verifier.verify(ByteArrayInputStream("OTHER".toByteArray()), testChecksum)
        assertTrue(result is VerificationResult.ChecksumMismatch)
        assertEquals(64, (result as VerificationResult.ChecksumMismatch).actualHex.length)
    }

    @Test fun malformedExpectedChecksum_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            verifier.verify(ByteArrayInputStream(byteArrayOf()), "not-a-checksum")
        }
    }
}
