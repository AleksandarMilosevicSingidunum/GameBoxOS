package com.gamebox.os

import com.gamebox.os.launch.LaunchPreparation
import com.gamebox.os.download.Sha256Verifier
import java.io.InputStream
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class LaunchPreparationTest {
    @Test fun failedSessionPersistencePreventsExternalHandoff() {
        val preparation = LaunchPreparation()
        preparation.beforeDispatch { throw java.io.IOException("database unavailable") }
        assertThrows(java.io.IOException::class.java) {
            preparation.dispatch { fail("Do not launch without a durable session") }
        }
    }

    @Test fun cancelledPreparationCannotDispatch() {
        val preparation = LaunchPreparation()
        assertTrue(preparation.cancel())
        assertThrows(CancellationException::class.java) { preparation.dispatch { fail("Must not open emulator") } }
    }

    @Test fun committedHandoffCannotBeCancelledOrRepeated() {
        val preparation = LaunchPreparation()
        assertEquals("opened", preparation.dispatch {
            assertFalse(preparation.cancel())
            "opened"
        })
        assertThrows(IllegalStateException::class.java) { preparation.dispatch { fail("Duplicate handoff") } }
    }

    @Test fun hashingStopsBetweenReadChunksWithoutReadingWholeGame() {
        val preparation = LaunchPreparation()
        var reads = 0
        val input = object : InputStream() {
            override fun read(): Int = error("Chunked read expected")
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                reads++
                preparation.cancel()
                bytes[offset] = 1
                return 1
            }
        }
        assertThrows(CancellationException::class.java) {
            Sha256Verifier().verify(input, "a".repeat(64), preparation::checkActive)
        }
        assertEquals(1, reads)
    }
}
