package com.gamebox.os.download

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DownloadConcurrencyGateTest {
    @Test
    fun defaultGateAllowsAtMostTwoTransfers() = runBlocking {
        val gate = DownloadConcurrencyGate()
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val completed = AtomicInteger()

        (1..8).map {
            async {
                gate.run {
                    val now = active.incrementAndGet()
                    peak.updateAndGet { previous -> maxOf(previous, now) }
                    try {
                        delay(20)
                        completed.incrementAndGet()
                    } finally {
                        active.decrementAndGet()
                    }
                }
            }
        }.awaitAll()

        assertEquals(DEFAULT_MAX_CONCURRENT_DOWNLOADS, peak.get())
        assertEquals(8, completed.get())
        assertEquals(0, active.get())
    }

    @Test
    fun gateRejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            DownloadConcurrencyGate(0)
        }
    }
}
