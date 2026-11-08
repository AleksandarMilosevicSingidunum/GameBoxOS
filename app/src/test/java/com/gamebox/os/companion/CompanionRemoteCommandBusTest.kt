package com.gamebox.os.companion

import android.view.KeyEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionRemoteCommandBusTest {
    @Test
    fun commandsReachOnlyTheResumedExternalDisplayWhenPhoneAndTvAreOpen() = runBlocking {
        val phone = Any()
        val tv = Any()
        val phoneCommands = MutableSharedFlow<CompanionRemoteCommand>(extraBufferCapacity = 1)
        val tvCommands = MutableSharedFlow<CompanionRemoteCommand>(extraBufferCapacity = 1)
        val phoneReceived = CompletableDeferred<CompanionRemoteCommand>()
        val tvReceived = CompletableDeferred<CompanionRemoteCommand>()
        val tvTextReceived = CompletableDeferred<CompanionRemoteCommand.Text>()
        val phoneCollector = launch(start = CoroutineStart.UNDISPATCHED) {
            phoneCommands.collect(phoneReceived::complete)
        }
        val tvCollector = launch(start = CoroutineStart.UNDISPATCHED) {
            tvCommands.collect { command ->
                if (command is CompanionRemoteCommand.Text) tvTextReceived.complete(command)
                else tvReceived.complete(command)
            }
        }
        try {
            CompanionRemoteCommandBus.register(phone, externalDisplay = false, commands = phoneCommands)
            CompanionRemoteCommandBus.register(tv, externalDisplay = true, commands = tvCommands)
            CompanionRemoteCommandBus.setHostState(phone, resumed = true, textEntryAvailable = false)
            CompanionRemoteCommandBus.setHostState(tv, resumed = true, textEntryAvailable = true)

            val command = CompanionRemoteCommand.Key(KeyEvent.KEYCODE_DPAD_RIGHT)
            assertTrue(CompanionRemoteCommandBus.publish(command))
            assertEquals(command, tvReceived.await())
            val text = CompanionRemoteCommand.Text("RS")
            assertTrue(CompanionRemoteCommandBus.publish(text))
            assertEquals(text, tvTextReceived.await())
            assertFalse(phoneReceived.isCompleted)
        } finally {
            CompanionRemoteCommandBus.unregister(phone)
            CompanionRemoteCommandBus.unregister(tv)
            phoneCollector.cancelAndJoin()
            tvCollector.cancelAndJoin()
        }
    }

    @Test
    fun pausedExternalDisplayFallsBackToResumedPhoneAndTextRequiresItsSearch() = runBlocking {
        val phone = Any()
        val tv = Any()
        val phoneCommands = MutableSharedFlow<CompanionRemoteCommand>(extraBufferCapacity = 1)
        val tvCommands = MutableSharedFlow<CompanionRemoteCommand>(extraBufferCapacity = 1)
        val phoneReceived = CompletableDeferred<CompanionRemoteCommand>()
        val phoneCollector = launch(start = CoroutineStart.UNDISPATCHED) {
            phoneCommands.collect(phoneReceived::complete)
        }
        try {
            CompanionRemoteCommandBus.register(phone, externalDisplay = false, commands = phoneCommands)
            CompanionRemoteCommandBus.register(tv, externalDisplay = true, commands = tvCommands)
            CompanionRemoteCommandBus.setHostState(phone, resumed = true, textEntryAvailable = true)
            CompanionRemoteCommandBus.setHostState(tv, resumed = false, textEntryAvailable = false)

            assertTrue(CompanionRemoteCommandBus.isTextEntryAvailable())
            val command = CompanionRemoteCommand.Key(KeyEvent.KEYCODE_DPAD_LEFT)
            assertTrue(CompanionRemoteCommandBus.publish(command))
            assertEquals(command, phoneReceived.await())
            CompanionRemoteCommandBus.setHostState(tv, resumed = true, textEntryAvailable = false)
            assertFalse(CompanionRemoteCommandBus.isTextEntryAvailable())
            assertFalse(CompanionRemoteCommandBus.publish(CompanionRemoteCommand.Text("phone")))
        } finally {
            CompanionRemoteCommandBus.unregister(phone)
            CompanionRemoteCommandBus.unregister(tv)
            phoneCollector.cancelAndJoin()
        }
    }

    @Test
    fun resumedExternalBrowserTakesPriorityOverItsExternalGameBoxHost() = runBlocking {
        val tv = Any()
        val browser = Any()
        val tvCommands = MutableSharedFlow<CompanionRemoteCommand>(extraBufferCapacity = 1)
        val browserCommands = MutableSharedFlow<CompanionRemoteCommand>(extraBufferCapacity = 1)
        val tvReceived = CompletableDeferred<CompanionRemoteCommand>()
        val browserReceived = CompletableDeferred<CompanionRemoteCommand>()
        val tvCollector = launch(start = CoroutineStart.UNDISPATCHED) { tvCommands.collect(tvReceived::complete) }
        val browserCollector = launch(start = CoroutineStart.UNDISPATCHED) { browserCommands.collect(browserReceived::complete) }
        try {
            CompanionRemoteCommandBus.register(tv, externalDisplay = true, commands = tvCommands)
            CompanionRemoteCommandBus.register(browser, externalDisplay = true, commands = browserCommands, priority = 1)
            CompanionRemoteCommandBus.setHostState(tv, resumed = true, textEntryAvailable = false)
            CompanionRemoteCommandBus.setHostState(browser, resumed = true, textEntryAvailable = false)

            val command = CompanionRemoteCommand.Key(KeyEvent.KEYCODE_DPAD_RIGHT)
            assertTrue(CompanionRemoteCommandBus.publish(command))
            assertEquals(command, browserReceived.await())
            assertFalse(tvReceived.isCompleted)
        } finally {
            CompanionRemoteCommandBus.unregister(tv)
            CompanionRemoteCommandBus.unregister(browser)
            tvCollector.cancelAndJoin()
            browserCollector.cancelAndJoin()
        }
    }
}
