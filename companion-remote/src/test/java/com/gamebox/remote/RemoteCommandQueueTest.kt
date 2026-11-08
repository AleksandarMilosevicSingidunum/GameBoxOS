package com.gamebox.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteCommandQueueTest {
    @Test
    fun requestsRunInArrivalOrderEvenWhenAnEarlierRequestIsSlow() = runBlocking {
        val queue = RemoteCommandQueue()
        val order = mutableListOf<String>()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()

        val first = launch {
            queue.execute {
                order += "first-start"
                firstStarted.complete(Unit)
                releaseFirst.await()
                order += "first-finish"
            }
        }
        firstStarted.await()
        val second = launch { queue.execute { order += "second" } }
        yield()
        assertEquals(listOf("first-start"), order)

        releaseFirst.complete(Unit)
        joinAll(first, second)
        assertEquals(listOf("first-start", "first-finish", "second"), order)
    }
}
