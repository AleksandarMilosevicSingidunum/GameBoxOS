package com.gamebox.os.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingsUiActionTest {
    @Test
    fun reportsFailureAndAlwaysResetsBusyState() = runBlocking {
        val busyEvents = mutableListOf<Boolean>()
        val failures = mutableListOf<String>()

        runSettingsUiAction(
            failureMessage = "fallback",
            onBusyChanged = busyEvents::add,
            onFailure = failures::add,
        ) {
            error("storage unavailable")
        }

        assertEquals(listOf(true, false), busyEvents)
        assertEquals(listOf("storage unavailable"), failures)
    }

    @Test
    fun usesFallbackWhenFailureHasNoMessage() = runBlocking {
        val failures = mutableListOf<String>()

        runSettingsUiAction(
            failureMessage = "fallback",
            onBusyChanged = {},
            onFailure = failures::add,
        ) {
            throw IllegalStateException()
        }

        assertEquals(listOf("fallback"), failures)
    }

    @Test
    fun propagatesCancellationAfterResettingBusyState() {
        val busyEvents = mutableListOf<Boolean>()
        val failures = mutableListOf<String>()
        val cancellation = CancellationException("screen closed")

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking {
                runSettingsUiAction(
                    failureMessage = "fallback",
                    onBusyChanged = busyEvents::add,
                    onFailure = failures::add,
                ) {
                    throw cancellation
                }
            }
        }

        assertSame(cancellation, thrown)
        assertEquals(listOf(true, false), busyEvents)
        assertEquals(emptyList<String>(), failures)
        assertFalse(busyEvents.last())
    }
}
