package com.gamebox.os.content

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameMutationGateTest {
    @Test
    fun sameGameMutationsAreSerialized() = runBlocking {
        val inside = AtomicInteger(0)
        val maxInside = AtomicInteger(0)
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()

        val first = async {
            GameMutationGate.withGameLock("game-one") {
                val current = inside.incrementAndGet()
                maxInside.updateAndGet { maxOf(it, current) }
                firstEntered.complete(Unit)
                releaseFirst.await()
                inside.decrementAndGet()
            }
        }

        firstEntered.await()

        val second = async {
            GameMutationGate.withGameLock("game-one") {
                val current = inside.incrementAndGet()
                maxInside.updateAndGet { maxOf(it, current) }
                inside.decrementAndGet()
            }
        }

        delay(50)
        assertEquals(1, inside.get())
        releaseFirst.complete(Unit)
        first.await()
        second.await()
        assertEquals(1, maxInside.get())
    }

    @Test
    fun differentGamesCanMutateIndependently() = runBlocking {
        val bothEntered = CompletableDeferred<Unit>()
        val inside = AtomicInteger(0)

        suspend fun enter(id: String) {
            GameMutationGate.withGameLock(id) {
                if (inside.incrementAndGet() == 2) bothEntered.complete(Unit)
                bothEntered.await()
                inside.decrementAndGet()
            }
        }

        val first = async { enter("game-a") }
        val second = async { enter("game-b") }

        withTimeout(2_000) { bothEntered.await() }
        assertTrue(inside.get() >= 1)
        withTimeout(2_000) {
            first.await()
            second.await()
        }
    }
}
