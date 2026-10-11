package com.gamebox.os.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ByteProgressThrottleTest {
    @Test
    fun coalescesSmallChunksAndPublishesTheFinalByteCount() {
        val updates = mutableListOf<Long>()
        val throttle = ByteProgressThrottle({ updates += it }, intervalBytes = 4)

        (1L..10L).forEach(throttle::onBytesProcessed)
        throttle.finish(10)

        assertEquals(listOf(4L, 8L, 10L), updates)
    }

    @Test
    fun avoidsDuplicateFinalUpdateAtAnExactInterval() {
        val updates = mutableListOf<Long>()
        val throttle = ByteProgressThrottle({ updates += it }, intervalBytes = 4)

        (1L..8L).forEach(throttle::onBytesProcessed)
        throttle.finish(8)

        assertEquals(listOf(4L, 8L), updates)
    }

    @Test
    fun rejectsProgressThatMovesBackwardsBetweenNotifications() {
        val throttle = ByteProgressThrottle({}, intervalBytes = 4)

        throttle.onBytesProcessed(3)

        assertThrows(IllegalArgumentException::class.java) {
            throttle.onBytesProcessed(2)
        }
    }
}
