package com.gamebox.os.launch

import com.gamebox.os.data.FakeGameRepository
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class LaunchSessionControllerTest {
    private class Journal : LaunchSessionJournal {
        val events = mutableListOf<String>()
        var pending: String? = null
        var confirmed = false
        var failRecovery = false
        var failBegin = false
        var failConfirm = false
        override suspend fun begin(gameId: String): String {
            if (failBegin) throw java.io.IOException("write failed")
            check(pending == null)
            events += "begin"
            pending = gameId
            return "ticket"
        }
        override suspend fun confirm(ticket: String) {
            if (failConfirm) throw java.io.IOException("confirm failed")
            events += "confirm"
            confirmed = true
        }
        override suspend fun abandon(ticket: String) { events += "abandon"; pending = null }
        override suspend fun finish(): RecoveredLaunchSession? {
            if (failRecovery) throw java.io.IOException("read failed")
            val id = pending ?: return null
            events += "finish"
            pending = null
            return RecoveredLaunchSession(id, confirmed, if (confirmed) 2 else 0)
        }
    }

    private val capability = EmulatorCapability("test", GameId("session-game"), "example.emulator",
        "game.rom", "application/octet-stream", "a".repeat(64))

    @Test fun aNewControllerRecoversPersistedDispatchWithoutLaunchingAgain(): Unit = runBlocking {
        val repository = FakeGameRepository()
        val game = repository.observeGames().value.first().copy(state = InstallState.INSTALLED)
        val journal = Journal()
        var dispatches = 0
        val gateway = object : PackageGateway {
            override fun launch(capability: EmulatorCapability, preparation: LaunchPreparation) = preparation.dispatch {
                assertEquals(game.id.value, journal.pending)
                journal.events += "dispatch"
                dispatches++
                GatewayResult.LAUNCHED
            }
        }
        val registry = EmulatorCapabilityRegistry(listOf(capability.copy(gameId = game.id)))
        val first = DefaultGameLaunchController(registry, gateway, repository, scope = this, sessionJournal = journal)
        first.launch(game)
        withTimeout(5_000) { first.observeState().first { it.status == LaunchUiState.Status.LAUNCHED } }
        assertEquals(listOf("begin", "dispatch", "confirm"), journal.events)
        val recreated = DefaultGameLaunchController(registry, gateway, repository, scope = this, sessionJournal = journal)
        recreated.onHostResumed()
        withTimeout(5_000) { recreated.observeState().first { it.status == LaunchUiState.Status.RETURNED } }
        assertNull(journal.pending)
        assertEquals(1, dispatches)
        assertEquals(listOf("begin", "dispatch", "confirm", "finish"), journal.events)
    }

    @Test fun failedColdRecoveryBlocksPlayUntilExplicitRetrySucceeds(): Unit = runBlocking {
        val repository = FakeGameRepository()
        val game = repository.observeGames().value.first().copy(state = InstallState.INSTALLED)
        val journal = Journal().apply { pending = game.id.value; confirmed = true; failRecovery = true }
        val gateway = object : PackageGateway {
            override fun launch(capability: EmulatorCapability, preparation: LaunchPreparation): GatewayResult =
                error("Must not dispatch while history recovery is blocked")
        }
        val controller = DefaultGameLaunchController(EmulatorCapabilityRegistry(), gateway, repository,
            scope = this, sessionJournal = journal)
        controller.onHostResumed()
        withTimeout(5_000) { controller.observeState().first { it.status == LaunchUiState.Status.SESSION_ERROR } }
        controller.launch(game)
        assertEquals(LaunchUiState.Status.SESSION_ERROR, controller.observeState().value.status)
        assertEquals(game.id.value, journal.pending)
        journal.failRecovery = false
        controller.onHostResumed()
        withTimeout(5_000) { controller.observeState().first { it.status == LaunchUiState.Status.RETURNED } }
        assertNull(journal.pending)
    }

    @Test fun persistenceFailuresBeforeAndAfterDispatchRemainRecoverable(): Unit = runBlocking {
        for (failureBeforeDispatch in listOf(true, false)) {
            val repository = FakeGameRepository()
            val game = repository.observeGames().value.first().copy(state = InstallState.INSTALLED)
            val journal = Journal().apply { failBegin = failureBeforeDispatch; failConfirm = !failureBeforeDispatch }
            var dispatches = 0
            val gateway = object : PackageGateway {
                override fun launch(capability: EmulatorCapability, preparation: LaunchPreparation) = preparation.dispatch {
                    dispatches++
                    GatewayResult.LAUNCHED
                }
            }
            val controller = DefaultGameLaunchController(
                EmulatorCapabilityRegistry(listOf(capability.copy(gameId = game.id))), gateway, repository,
                scope = this, sessionJournal = journal)
            controller.launch(game)
            withTimeout(5_000) { controller.observeState().first { it.status == LaunchUiState.Status.SESSION_ERROR } }
            assertEquals(if (failureBeforeDispatch) 0 else 1, dispatches)
            controller.onHostResumed()
            withTimeout(5_000) { controller.observeState().first { it.status != LaunchUiState.Status.SESSION_ERROR } }
            assertNull(journal.pending)
            if (!failureBeforeDispatch) assertTrue(controller.observeState().value.message!!.contains("no playtime"))
        }
    }
}
