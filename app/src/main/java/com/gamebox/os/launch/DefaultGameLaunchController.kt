package com.gamebox.os.launch

import android.app.Activity
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

data class LaunchUiState(
    val status: Status = Status.IDLE,
    val gameId: GameId? = null,
    val message: String? = null,
) {
    enum class Status {
        IDLE, PREPARING, LAUNCHED, RETURNED, SESSION_ERROR, EMULATOR_UNAVAILABLE, UNSUPPORTED, NOT_INSTALLED,
        CONTENT_MISSING, VERIFICATION_FAILED, HANDOFF_REJECTED,
    }
}

class ReturnTracker(private val nowMillis: () -> Long = System::currentTimeMillis) {
    private var pending: Pair<GameId, Long>? = null

    fun started(gameId: GameId) {
        pending = gameId to nowMillis()
    }

    fun returned(): PlaySession? {
        val (gameId, startedAt) = pending ?: return null
        pending = null
        val endedAt = nowMillis().coerceAtLeast(startedAt)
        return PlaySession(gameId, startedAt, endedAt)
    }
}

data class PlaySession(val gameId: GameId, val startedAtMillis: Long, val endedAtMillis: Long) {
    val minutesPlayed: Int
        get() = ((endedAtMillis - startedAtMillis) / 60_000L).toInt().coerceAtLeast(0)
}

interface GameLaunchController {
    fun observeState(): StateFlow<LaunchUiState>
    fun hasApprovedAdapter(game: Game): Boolean = true
    fun launch(game: Game)
    fun launchOnDisplay(game: Game, displayId: Int?) = launch(game)
    fun launchOnDisplay(game: Game, displayId: Int?, sourceActivity: Activity) =
        launchOnDisplay(game, displayId)
    fun onHostResumed()
    fun onHostPaused() {}
    fun onHostWindowFocusChanged(hasFocus: Boolean) {}
    fun cancelPreparation() {}
}

/** Coordinates verified game preparation, emulator handoff, and return/session recovery. */
class DefaultGameLaunchController(
    private val registry: EmulatorCapabilityRegistry,
    private val gateway: PackageGateway,
    private val repository: GameRepository,
    private val returnTracker: ReturnTracker = ReturnTracker(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val sessionJournal: LaunchSessionJournal? = null,
    private val platformEmulatorDefault: suspend (String) -> String? = { null },
    private val backupAfterSession: suspend (GameId) -> com.gamebox.os.storage.GameSaveBackupResult? = { null },
    private val restoreBeforeSession: suspend (GameId) -> com.gamebox.os.storage.GameSaveRestoreResult? = { null },
) : GameLaunchController {
    private val state = MutableStateFlow(LaunchUiState())
    private val preparing = AtomicBoolean(false)
    private val recovering = AtomicBoolean(false)
    private val deferredHostReturn = DeferredHostReturn()
    private var waitingForExternalReturn = false
    @Volatile private var activePreparation: LaunchPreparation? = null

    override fun observeState(): StateFlow<LaunchUiState> = state.asStateFlow()

    override fun hasApprovedAdapter(game: Game): Boolean = registry.hasApprovedAdapter(game)

    override fun launch(game: Game) = launchOnDisplay(game, displayId = null)

    override fun launchOnDisplay(game: Game, displayId: Int?) =
        launchPreparedOnDisplay(game, displayId, sourceActivity = null)

    override fun launchOnDisplay(game: Game, displayId: Int?, sourceActivity: Activity) =
        launchPreparedOnDisplay(game, displayId, sourceActivity)

    private fun launchPreparedOnDisplay(game: Game, displayId: Int?, sourceActivity: Activity?) {
        if (state.value.status == LaunchUiState.Status.SESSION_ERROR || waitingForExternalReturn ||
            recovering.get() || !preparing.compareAndSet(false, true)
        ) return
        val preparation = LaunchPreparation()
        activePreparation = preparation
        update(game.id, LaunchUiState.Status.PREPARING, "Preparing game; checking installed content")
        scope.launch {
            try {
                launchPrepared(game, preparation, displayId, sourceActivity)
            } catch (cancelled: CancellationException) {
                update(game.id, LaunchUiState.Status.IDLE, "Launch cancelled")
                throw cancelled
            } catch (_: Exception) {
                if (sessionJournal != null && preparation.isCommitted()) {
                    update(
                        game.id,
                        LaunchUiState.Status.SESSION_ERROR,
                        "Session tracking was interrupted. Recover session history before launching another game.",
                    )
                } else {
                    update(
                        game.id,
                        LaunchUiState.Status.HANDOFF_REJECTED,
                        "Could not prepare or open this game. Check storage access and try again.",
                    )
                }
            } finally {
                activePreparation = null
                preparing.set(false)
                if (deferredHostReturn.take()) onHostResumed()
            }
        }
    }

    override fun cancelPreparation() {
        activePreparation?.cancel()
    }

    private suspend fun launchPrepared(
        game: Game,
        preparation: LaunchPreparation,
        displayId: Int?,
        sourceActivity: Activity?,
    ) {
        preparation.checkActive()
        if (game.state !in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE)) {
            update(game.id, LaunchUiState.Status.NOT_INSTALLED, "Install and verify before launching")
            return
        }
        val platformDefault = platformEmulatorDefault(game.platform)
        val capability = registry.forGame(game, platformDefault)
        if (capability == null) {
            update(game.id, LaunchUiState.Status.UNSUPPORTED, "No approved adapter for this title")
            return
        }
        val restored = restoreBeforeSession(game.id)
        if (restored != null && restored.failedCount > 0) {
            update(
                game.id,
                LaunchUiState.Status.HANDOFF_REJECTED,
                restored.message ?: "A configured emulator save could not be restored; launch was stopped",
            )
            return
        }
        var ticket: String? = null
        preparation.beforeDispatch {
            ticket = runBlocking { sessionJournal?.begin(game.id.value) }
            deferredHostReturn.arm()
        }
        val automaticSelection = game.emulatorPackage == null && platformDefault == null
        var selectedCapability: EmulatorCapability = capability
        val result = withContext(Dispatchers.IO) {
            fun launch(selected: EmulatorCapability) = if (sourceActivity == null) {
                gateway.launchOnDisplay(selected, preparation, displayId)
            } else {
                gateway.launchOnDisplayFromActivity(selected, preparation, displayId, sourceActivity)
            }

            var attempt = launch(selectedCapability)
            if (attempt == GatewayResult.EMULATOR_UNAVAILABLE && automaticSelection) {
                for (packageName in registry.optionsFor(game).filterNot { it == capability.packageName }) {
                    val alternative = registry.forGame(game, packageName) ?: continue
                    selectedCapability = alternative
                    attempt = launch(alternative)
                    if (attempt != GatewayResult.EMULATOR_UNAVAILABLE) break
                }
            }
            attempt
        }
        if (result == GatewayResult.LAUNCHED) {
            ticket?.let { sessionJournal?.confirm(it) }
        } else {
            ticket?.let { sessionJournal?.abandon(it) }
            deferredHostReturn.clear()
        }
        when (result) {
            GatewayResult.LAUNCHED -> {
                returnTracker.started(game.id)
                waitingForExternalReturn = true
                update(game.id, LaunchUiState.Status.LAUNCHED, "Verified content handed to emulator")
            }
            GatewayResult.EMULATOR_UNAVAILABLE -> update(
                game.id,
                LaunchUiState.Status.EMULATOR_UNAVAILABLE,
                "${registry.displayName(selectedCapability.packageName)} is unavailable. Install it or choose another emulator in Game Settings.",
            )
            GatewayResult.CONTENT_MISSING -> {
                repository.setInstallState(game.id, InstallState.MISSING_FILES)
                update(game.id, LaunchUiState.Status.CONTENT_MISSING, "Verified content is missing; reinstall it")
            }
            GatewayResult.VERIFICATION_FAILED -> {
                repository.setInstallState(game.id, InstallState.FAILED)
                update(game.id, LaunchUiState.Status.VERIFICATION_FAILED, "Content changed after installation; reinstall it")
            }
            GatewayResult.HANDOFF_REJECTED -> update(
                game.id,
                LaunchUiState.Status.HANDOFF_REJECTED,
                buildHandoffRejectedMessage(selectedCapability),
            )
        }
    }

    override fun onHostPaused() {
        deferredHostReturn.onPaused()
    }

    override fun onHostWindowFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) {
            if (preparing.get() || waitingForExternalReturn) deferredHostReturn.onPaused()
            return
        }
        if (preparing.get()) {
            deferredHostReturn.onResumed()
        } else {
            // Desktop/freeform hosts can remain RESUMED while the emulator owns focus.
            // Regained focus is therefore also a signal that the player returned.
            onHostResumed()
        }
    }

    override fun onHostResumed() {
        if (preparing.get()) {
            deferredHostReturn.onResumed()
            return
        }
        deferredHostReturn.clear()
        gateway.cleanupLaunchedContent()
        if (sessionJournal != null) {
            if (preparing.get() || !recovering.compareAndSet(false, true)) return
            scope.launch {
                try {
                    val session = sessionJournal.finish()
                    waitingForExternalReturn = false
                    returnTracker.returned()
                    if (session == null && state.value.status == LaunchUiState.Status.SESSION_ERROR) {
                        state.value = LaunchUiState()
                    }
                    session?.let {
                        val returnedGameId = GameId(it.gameId)
                        val backupAttempt = if (it.launchConfirmed) {
                            runCatching { backupAfterSession(returnedGameId) }
                        } else {
                            Result.success(null)
                        }
                        val message = launchReturnMessage(it.launchConfirmed, it.minutesAway, backupAttempt)
                        update(returnedGameId, LaunchUiState.Status.RETURNED, message)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    state.value = state.value.copy(
                        status = LaunchUiState.Status.SESSION_ERROR,
                        message = "Could not recover session history. Your pending session is retained; retry before launching another game.",
                    )
                } finally {
                    recovering.set(false)
                }
            }
            return
        }
        if (!waitingForExternalReturn) return
        waitingForExternalReturn = false
        val session = returnTracker.returned() ?: return
        repository.recordPlaySession(session.gameId, session.endedAtMillis, session.minutesPlayed)
        update(session.gameId, LaunchUiState.Status.RETURNED, "Returned safely; play session recorded")
    }

    private fun buildHandoffRejectedMessage(capability: EmulatorCapability): String {
        val coreHint = capability.requiredCore?.let { " Verify that the $it core is installed and selected." } ?: ""
        return "The approved " + registry.displayName(capability.packageName) +
            " handoff was rejected. Open the emulator and load the installed game manually." + coreHint
    }

    private fun update(gameId: GameId, status: LaunchUiState.Status, message: String) {
        state.value = LaunchUiState(status, gameId, message)
    }
}
