package com.gamebox.os.download

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class CompletedTransferRecoveryTest {
    @get:Rule val temporary = TemporaryFolder()
    private val payload = "authorized complete download".toByteArray()
    private val checksum = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }
    private val recovery = CompletedTransferRecovery()

    @Test fun completePartialIsVerifiedAndPromotedLocally() {
        val target = target()
        target.openOutput().use { it.write(payload) }
        assertEquals(payload.size.toLong(), recovery.recover(target, checksum, 1024))
        assertArrayEquals(payload, target.finalFile.readBytes())
        assertFalse(target.stagingFile.exists())
    }

    @Test fun alreadyCommittedContentRecoversCompletionAfterRestart() {
        val target = target()
        target.openOutput().use { it.write(payload) }
        target.commit()
        assertEquals(payload.size.toLong(), recovery.recover(target, checksum, 1024))
        assertEquals(payload.size.toLong(), recovery.recover(target, checksum, 1024))
        assertFalse(target.stagingFile.exists())
    }

    @Test fun incompleteOrAlteredFilesAreNotAcceptedOrDiscarded() {
        val target = target()
        target.openOutput().use { it.write(payload, 0, 5) }
        target.finalFile.writeText("old installed content")
        assertNull(recovery.recover(target, checksum, 1024))
        assertEquals(5L, target.stagedBytes)
        assertEquals("old installed content", target.finalFile.readText())
    }

    @Test fun validPartialReplacesUnverifiedOldFinalContent() {
        val target = target()
        target.openOutput().use { it.write(payload) }
        target.finalFile.writeText("old installed content")
        assertEquals(payload.size.toLong(), recovery.recover(target, checksum, 1024))
        assertArrayEquals(payload, target.finalFile.readBytes())
    }

    @Test fun failedPromotionKeepsContentForSuccessfulLocalRetry() {
        val target = target()
        target.openOutput().use { it.write(payload) }
        assertTrue(target.finalFile.mkdir())
        val blocker = target.finalFile.resolve("blocker")
        blocker.writeText("test-owned directory obstruction")
        try {
            recovery.recover(target, checksum, 1024)
            fail("Nonempty directory must prevent file promotion")
        } catch (expected: java.io.IOException) {
            assertArrayEquals(payload, target.stagingFile.readBytes())
        }
        assertTrue(blocker.delete())
        assertTrue(target.finalFile.delete())
        assertEquals(payload.size.toLong(), recovery.recover(target, checksum, 1024))
        assertArrayEquals(payload, target.finalFile.readBytes())
    }

    @Test fun cancellationAndSizeLimitPreventPromotion() {
        val target = target()
        target.openOutput().use { it.write(payload) }
        assertNull(recovery.recover(target, checksum, 2))
        assertTrue(target.stagingFile.exists())
        try {
            recovery.recover(target, checksum, 1024) { throw kotlinx.coroutines.CancellationException() }
            fail("Cancellation must propagate")
        } catch (expected: kotlinx.coroutines.CancellationException) { }
        assertFalse(target.finalFile.exists())
        assertTrue(target.stagingFile.exists())
    }

    private fun target() = FileStagingTarget(temporary.newFolder(), "game.nes")
}
