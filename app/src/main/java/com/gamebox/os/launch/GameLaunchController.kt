package com.gamebox.os.launch

import android.content.Context
import android.content.Intent
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EmulatorCapability(
    val id: String,
    val packageName: String,
    val supportedPlatforms: Set<String>
)

class EmulatorCapabilityRegistry(
    private val capabilities: List<EmulatorCapability> = listOf(
        EmulatorCapability(
            id = "retroarch-aarch64",
            packageName = "com.retroarch.aarch64",
            supportedPlatforms = setOf("retro")
        )
    )
) {
    fun forPlatform(platform: String): EmulatorCapability? =
        capabilities.firstOrNull { platform.trim().lowercase() in it.supportedPlatforms }
}

interface PackageGateway {
    fun launch(packageName: String): Boolean
}

class AndroidPackageGateway(private val context: Context) : PackageGateway {
    override fun launch(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.setPackage(packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}

data class LaunchUiState(
    val status: Status = Status.IDLE,
    val gameId: GameId? = null,
    val message: String? = null
) {
    enum class Status { IDLE, LAUNCHED, RETURNED, EMULATOR_UNAVAILABLE, UNSUPPORTED, NOT_INSTALLED }
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
    fun launch(game: Game)
    fun onHostResumed()
}

class DefaultGameLaunchController(
    private val registry: EmulatorCapabilityRegistry,
    private val gateway: PackageGateway,
    private val repository: GameRepository,
    private val returnTracker: ReturnTracker = ReturnTracker()
) : GameLaunchController {
    private val state = MutableStateFlow(LaunchUiState())
    private var waitingForExternalReturn = false

    override fun observeState(): StateFlow<LaunchUiState> = state.asStateFlow()

    override fun launch(game: Game) {
        if (game.state !in setOf(InstallState.INSTALLED, InstallState.UPDATE_AVAILABLE)) {
            state.value = LaunchUiState(
                LaunchUiState.Status.NOT_INSTALLED,
                game.id,
                "Install and verify the game before launching"
            )
            return
        }
        val capability = registry.forPlatform(game.platform)
        if (capability == null) {
            state.value = LaunchUiState(
                LaunchUiState.Status.UNSUPPORTED,
                game.id,
                "No approved launch adapter for ${game.platform}"
            )
            return
        }
        if (!gateway.launch(capability.packageName)) {
            state.value = LaunchUiState(
                LaunchUiState.Status.EMULATOR_UNAVAILABLE,
                game.id,
                "Install the approved emulator package: ${capability.packageName}"
            )
            return
        }
        returnTracker.started(game.id)
        waitingForExternalReturn = true
        state.value = LaunchUiState(LaunchUiState.Status.LAUNCHED, game.id)
    }

    override fun onHostResumed() {
        if (!waitingForExternalReturn) return
        waitingForExternalReturn = false
        val session = returnTracker.returned() ?: return
        repository.recordPlaySession(
            session.gameId,
            session.endedAtMillis,
            session.minutesPlayed
        )
        state.value = LaunchUiState(LaunchUiState.Status.RETURNED, session.gameId)
    }
}
