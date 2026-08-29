package com.gamebox.os.launch

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.download.Sha256Verifier
import com.gamebox.os.download.VerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class EmulatorCapability(
    val id: String,
    val gameId: GameId,
    val packageName: String,
    val contentRelativePath: String,
    val mimeType: String,
    val expectedSha256: String,
    val graphicsProfile: String = "Balanced"
)

class EmulatorCapabilityRegistry(
    private val capabilities: List<EmulatorCapability> = listOf(
        EmulatorCapability(
            id = "retroarch-aarch64-test",
            gameId = GameId("retro-test"),
            packageName = "com.retroarch.aarch64",
            contentRelativePath = "retro/retro-test/content/test.txt",
            mimeType = "application/octet-stream",
            expectedSha256 = "94ee059335e587e501cc4bf90613e0814f00a7b08bc7c648fd865a2af6a22cc2"
        )
    )
) {
    fun optionsFor(game: Game): List<String> = when (game.platform.lowercase()) {
        "retro", "homebrew" -> listOf("com.retroarch.aarch64")
        "psp" -> listOf("org.ppsspp.ppsspp")
        "gamecube", "wii" -> listOf("org.dolphinemu.dolphinemu")
        "ps2" -> listOf("xyz.aethersx2.android")
        else -> emptyList()
    }

    fun forGame(gameId: GameId): EmulatorCapability? = capabilities.firstOrNull { it.gameId == gameId }

    fun forGame(game: Game): EmulatorCapability? {
        capabilities.firstOrNull { it.gameId == game.id }?.let { return it }
        val checksum = game.expectedSha256 ?: return null
        val packageName = game.emulatorPackage?.takeIf { it in optionsFor(game) }
            ?: optionsFor(game).firstOrNull() ?: return null
        return EmulatorCapability(
            id = "platform-" + game.id.value,
            gameId = game.id,
            packageName = packageName,
            contentRelativePath = "remote/" + game.id.value + "/content.bin",
            mimeType = "application/octet-stream",
            expectedSha256 = checksum,
            graphicsProfile = game.graphicsProfile
        )
    }
}

enum class GatewayResult {
    LAUNCHED, EMULATOR_UNAVAILABLE, CONTENT_MISSING, VERIFICATION_FAILED, HANDOFF_REJECTED
}

interface PackageGateway {
    fun launch(capability: EmulatorCapability): GatewayResult
}

class AndroidPackageGateway(
    private val context: Context,
    private val verifier: Sha256Verifier = Sha256Verifier()
) : PackageGateway {
    override fun launch(capability: EmulatorCapability): GatewayResult {
        if (context.packageManager.getLaunchIntentForPackage(capability.packageName) == null) {
            return GatewayResult.EMULATOR_UNAVAILABLE
        }
        val installRoot = context.filesDir.resolve("installed").canonicalFile
        val content = File(installRoot, capability.contentRelativePath).canonicalFile
        val rootPrefix = installRoot.path + File.separator
        if (!content.path.startsWith(rootPrefix) || !content.isFile) {
            return GatewayResult.CONTENT_MISSING
        }
        val verified = content.inputStream().use {
            verifier.verify(it, capability.expectedSha256)
        }
        if (verified != VerificationResult.Verified) return GatewayResult.VERIFICATION_FAILED

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".files",
            content
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, capability.mimeType)
            .setPackage(capability.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (intent.resolveActivity(context.packageManager) == null) {
            return GatewayResult.HANDOFF_REJECTED
        }
        return runCatching {
            context.startActivity(intent)
            GatewayResult.LAUNCHED
        }.getOrDefault(GatewayResult.HANDOFF_REJECTED)
    }
}

data class LaunchUiState(
    val status: Status = Status.IDLE,
    val gameId: GameId? = null,
    val message: String? = null
) {
    enum class Status {
        IDLE, LAUNCHED, RETURNED, EMULATOR_UNAVAILABLE, UNSUPPORTED, NOT_INSTALLED,
        CONTENT_MISSING, VERIFICATION_FAILED, HANDOFF_REJECTED
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
            update(game.id, LaunchUiState.Status.NOT_INSTALLED, "Install and verify before launching")
            return
        }
        val capability = registry.forGame(game)
        if (capability == null) {
            update(game.id, LaunchUiState.Status.UNSUPPORTED, "No approved adapter for this title")
            return
        }
        when (gateway.launch(capability)) {
            GatewayResult.LAUNCHED -> {
                returnTracker.started(game.id)
                waitingForExternalReturn = true
                update(game.id, LaunchUiState.Status.LAUNCHED, "Verified content handed to emulator")
            }
            GatewayResult.EMULATOR_UNAVAILABLE -> update(
                game.id,
                LaunchUiState.Status.EMULATOR_UNAVAILABLE,
                "Install the approved emulator package: ${capability.packageName}"
            )
            GatewayResult.CONTENT_MISSING -> update(
                game.id, LaunchUiState.Status.CONTENT_MISSING, "Verified content is missing; reinstall it"
            )
            GatewayResult.VERIFICATION_FAILED -> update(
                game.id, LaunchUiState.Status.VERIFICATION_FAILED, "Content changed after installation; reinstall it"
            )
            GatewayResult.HANDOFF_REJECTED -> update(
                game.id,
                LaunchUiState.Status.HANDOFF_REJECTED,
                "The emulator rejected this diagnostic payload"
            )
        }
    }

    override fun onHostResumed() {
        if (!waitingForExternalReturn) return
        waitingForExternalReturn = false
        val session = returnTracker.returned() ?: return
        repository.recordPlaySession(session.gameId, session.endedAtMillis, session.minutesPlayed)
        update(session.gameId, LaunchUiState.Status.RETURNED, "Returned safely; play session recorded")
    }

    private fun update(gameId: GameId, status: LaunchUiState.Status, message: String) {
        state.value = LaunchUiState(status, gameId, message)
    }
}
