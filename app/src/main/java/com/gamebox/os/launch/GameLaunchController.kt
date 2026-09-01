package com.gamebox.os.launch

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.os.Environment
import androidx.core.content.FileProvider
import com.gamebox.os.content.GameContentPolicy
import com.gamebox.os.data.GameRepository
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import com.gamebox.os.domain.LocalContentFile
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
    val graphicsProfile: String = "Balanced",
    val requiredCore: String? = null,
    val retroArchCoreFileName: String? = null,
    val contentRoot: EmulatorContentRoot = EmulatorContentRoot.INSTALLED,
    val companionFiles: List<LocalContentFile> = emptyList(),
)

enum class EmulatorContentRoot(val directoryName: String) {
    INSTALLED("installed"),
    IMPORTS("imports"),
}

/** Stable path required by RetroArch's exported Android launcher activity. */
internal fun retroArchCorePath(packageName: String, coreFileName: String): String {
    require(packageName.startsWith("com.retroarch")) { "RetroArch package is required" }
    require(coreFileName.endsWith("_libretro_android.so")) { "Android libretro core is required" }
    return "/data/data/$packageName/cores/$coreFileName"
}

class EmulatorCapabilityRegistry(
    private val capabilities: List<EmulatorCapability> = listOf(
        EmulatorCapability(
            id = "retroarch-aarch64-galaxy-patrol",
            gameId = GameId("galaxy-patrol"),
            packageName = "com.retroarch.aarch64",
            contentRelativePath = "retro/galaxy-patrol/content/galaxy-patrol.nes",
            mimeType = "application/x-nes-rom",
            expectedSha256 = "97c1757ffd6a5bc1a591809b2b0f8988741f61f6abd82889c148ecae8a2f471f",
            requiredCore = "Nintendo - NES / Famicom (FCEUmm)",
            retroArchCoreFileName = "fceumm_libretro_android.so",
        )
    )
) {
    fun readinessMessage(game: Game): String? = forGame(game)?.let { capability ->
        capability.requiredCore?.let { core ->
            "Install RetroArch (standard, ARM64, or RA32) and the $core core before launching this game."
        }
    }

    fun displayName(packageName: String): String = when (packageName) {
        "com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32" -> "RetroArch"
        "org.ppsspp.ppsspp" -> "PPSSPP"
        "org.dolphinemu.dolphinemu" -> "Dolphin"
        "xyz.aethersx2.android" -> "AetherSX2"
        "com.github.stenzek.duckstation" -> "DuckStation"
        "org.mupen64plusae.v3.fzurita" -> "M64Plus FZ"
        "com.flycast.emulator" -> "Flycast"
        else -> packageName.substringAfterLast(".")
    }

    fun optionsFor(game: Game): List<String> = when (game.platform.lowercase().filter(Char::isLetterOrDigit)) {
        in retroArchPlatformAliases -> retroArchPackages
        "psp", "playstationportable", "sonyplaystationportable" -> listOf("org.ppsspp.ppsspp")
        "ps1", "psx", "playstation", "sonyplaystation" -> listOf("com.github.stenzek.duckstation", "com.retroarch.aarch64")
        "n64", "nintendo64" -> listOf("org.mupen64plusae.v3.fzurita", "com.retroarch.aarch64")
        "dreamcast", "segadreamcast" -> listOf("com.flycast.emulator", "com.retroarch.aarch64")
        "gamecube", "nintendogamecube", "wii", "nintendowii" -> listOf("org.dolphinemu.dolphinemu")
        "ps2", "playstation2", "sonyplaystation2" -> listOf("xyz.aethersx2.android")
        else -> emptyList()
    }

    private val retroArchPackages = listOf("com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32")
    private val retroArchPlatformAliases = setOf(
        "retro", "homebrew", "nes", "famicom", "nintendoentertainmentsystem",
        "snes", "superfamicom", "supernintendo", "supernintendoentertainmentsystem",
        "gb", "gameboy", "nintendogameboy", "gbc", "gameboycolor", "nintendogameboycolor",
        "gba", "gameboyadvance", "nintendogameboyadvance", "nds", "nintendods",
        "mastersystem", "segamastersystem", "gamegear", "segagamegear",
        "genesis", "megadrive", "segagenesis", "segamegadrive", "segacd", "megacd",
        "saturn", "segasaturn", "pcengine", "turbografx16", "neogeo", "snkneogeo",
        "neogeopocket", "neogeopocketcolor", "atari", "atari2600", "atari5200",
        "atari7800", "atarijaguar", "atarilynx", "wonderswan", "wonderswancolor", "arcade", "mame",
    )

    fun forGame(gameId: GameId): EmulatorCapability? = capabilities.firstOrNull { it.gameId == gameId }

    fun forGame(game: Game): EmulatorCapability? {
        val explicit = capabilities.firstOrNull { it.gameId == game.id }
        val importedPath = game.localContentRelativePath
        val importedChecksum = game.localContentSha256
        val importedMimeType = game.localContentMimeType
        if (importedPath != null && importedChecksum != null && importedMimeType != null) {
            val packageName = game.emulatorPackage?.takeIf { it in optionsFor(game) }
                ?: explicit?.packageName
                ?: optionsFor(game).firstOrNull()
                ?: return null
            return EmulatorCapability(
                id = "imported-" + game.id.value,
                gameId = game.id,
                packageName = packageName,
                contentRelativePath = importedPath,
                mimeType = importedMimeType,
                expectedSha256 = importedChecksum,
                graphicsProfile = game.graphicsProfile,
                requiredCore = explicit?.requiredCore,
                retroArchCoreFileName = explicit?.retroArchCoreFileName,
                contentRoot = EmulatorContentRoot.IMPORTS,
                companionFiles = game.localContentFiles.filterNot { file ->
                    file.relativePath == importedPath
                },
            )
        }
        explicit?.let { return it }
        val checksum = game.expectedSha256 ?: return null
        val packageName = game.emulatorPackage?.takeIf { it in optionsFor(game) }
            ?: optionsFor(game).firstOrNull() ?: return null
        val content = GameContentPolicy.describe(
            gameId = game.id.value,
            platform = game.platform,
            sourceUrl = game.sourceUrl,
        )
        return EmulatorCapability(
            id = "platform-" + game.id.value,
            gameId = game.id,
            packageName = packageName,
            contentRelativePath = content.relativePath,
            mimeType = content.mimeType,
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
        val resolvedPackage = resolvePackageName(capability.packageName)
            ?: return GatewayResult.EMULATOR_UNAVAILABLE
        val launcherIntent = context.packageManager.getLaunchIntentForPackage(resolvedPackage)
            ?: return GatewayResult.EMULATOR_UNAVAILABLE
        val installRoot = context.filesDir.resolve(capability.contentRoot.directoryName).canonicalFile
        val rootPrefix = installRoot.path + File.separator
        val approvedFiles = listOf(
            LocalContentFile(
                capability.contentRelativePath,
                capability.expectedSha256,
                capability.mimeType,
            )
        ) + capability.companionFiles
        val contentUris = mutableListOf<android.net.Uri>()
        approvedFiles.forEach { approved ->
            val content = File(installRoot, approved.relativePath).canonicalFile
            if (!content.path.startsWith(rootPrefix) || !content.isFile) {
                return GatewayResult.CONTENT_MISSING
            }
            val verified = content.inputStream().use {
                verifier.verify(it, approved.sha256)
            }
            if (verified != VerificationResult.Verified) return GatewayResult.VERIFICATION_FAILED
            contentUris += FileProvider.getUriForFile(
                context,
                context.packageName + ".files",
                content,
            )
        }
        val uri = contentUris.first()
        val plan = EmulatorIntentPolicy.plan(
            packageName = resolvedPackage,
            contentUri = uri.toString(),
            graphicsProfile = capability.graphicsProfile,
            retroArchCorePath = capability.retroArchCoreFileName?.let { coreFileName ->
                // RetroArch's externally launched activity resolves an absolute core path
                // from its app-private data directory. Using the /data/user/0 alias is
                // not reliable across current Android/RetroArch combinations and can
                // leave RetroActivityFuture on a black surface when the core cannot open.
                retroArchCorePath(resolvedPackage, coreFileName)
            },
        )
        // RetroArch's native external launcher does not reliably consume a FileProvider
        // content URI. In particular, it may leave RetroActivityFuture on a permanent
        // black screen even though the core is installed. The bundled Galaxy Patrol
        // fixture is explicitly redistributable, so publish that verified copy to the
        // public Downloads collection and hand RetroArch the normal absolute path it
        // expects. Imported/private content deliberately remains URI-based.
        val retroArchRom = if (
            resolvedPackage.startsWith("com.retroarch") &&
            capability.gameId == GameId("galaxy-patrol")
        ) {
            publishGalaxyPatrolForRetroArch(content) ?: return GatewayResult.HANDOFF_REJECTED
        } else {
            uri.toString()
        }
        val intent = (when (plan.style) {
            EmulatorIntentStyle.ACTION_VIEW -> Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, capability.mimeType)
                .setPackage(resolvedPackage)
            EmulatorIntentStyle.LAUNCHER_EXTRAS -> {
                val baseIntent = plan.activityClassName?.let { activityClassName ->
                    Intent().setClassName(resolvedPackage, activityClassName)
                } ?: launcherIntent
                Intent(baseIntent).apply {
                    plan.stringExtras.forEach { (key, value) -> putExtra(key, value) }
                    if (resolvedPackage.startsWith("com.retroarch")) {
                        putExtra(EmulatorIntentPolicy.RETROARCH_ROM, retroArchRom)
                    }
                    plan.stringArrayExtras.forEach { (key, values) -> putExtra(key, values.toTypedArray()) }
                }
            }
        })
            .apply {
                if (resolvedPackage.startsWith("com.retroarch")) {
                    val target = context.packageManager.getApplicationInfo(resolvedPackage, 0)
                    EmulatorIntentPolicy.retroArchRuntimeExtras(
                        dataDir = target.dataDir,
                        apkPath = target.sourceDir,
                        externalDir = Environment.getExternalStorageDirectory().path +
                            "/Android/data/$resolvedPackage/files",
                        storageRoot = Environment.getExternalStorageDirectory().path,
                    ).forEach { (key, value) -> putExtra(key, value) }
                }
            }
            .putExtra("gamebox.graphics_profile", capability.graphicsProfile)
            .putExtra("gamebox.graphics_profile_applied", plan.graphicsProfileApplied)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        intent.clipData = contentClipData(uri, contentUris.drop(1))
        if (intent.resolveActivity(context.packageManager) == null) {
            return GatewayResult.HANDOFF_REJECTED
        }
        return runCatching {
            context.startActivity(intent)
            GatewayResult.LAUNCHED
        }.getOrElse {
            // Some RetroArch Android builds expose a launcher activity but reject
            // launcher extras. Retry once with the standard scoped ACTION_VIEW contract.
            if (resolvedPackage.startsWith("com.retroarch")) {
                val fallback = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, capability.mimeType)
                    setPackage(resolvedPackage)
                    clipData = contentClipData(uri, contentUris.drop(1))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching {
                    context.startActivity(fallback)
                    GatewayResult.LAUNCHED
                }.getOrDefault(GatewayResult.HANDOFF_REJECTED)
            } else {
                GatewayResult.HANDOFF_REJECTED
            }
        }
    }

    private fun contentClipData(primary: android.net.Uri, companions: List<android.net.Uri>): ClipData =
        ClipData.newRawUri("GameBox content", primary).apply {
            companions.forEach { companionUri -> addItem(ClipData.Item(companionUri)) }
        }

    /**
     * Publishes only the built-in MIT fixture. MediaStore needs no broad storage
     * permission on Android 10+, and produces the conventional path RetroArch needs.
     */
    private fun publishGalaxyPatrolForRetroArch(source: File): String? = runCatching {
        val relativePath = "Download/GameBox"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "galaxy-patrol.nes")
            put(MediaStore.Downloads.MIME_TYPE, "application/x-nes-rom")
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val published = context.contentResolver.insert(collection, values) ?: return@runCatching null
        try {
            context.contentResolver.openOutputStream(published, "w")?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: return@runCatching null
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(published, values, null, null)
            "/storage/emulated/0/$relativePath/galaxy-patrol.nes"
        } catch (error: Exception) {
            context.contentResolver.delete(published, null, null)
            throw error
        }
    }.getOrNull()

    private fun resolvePackageName(requested: String): String? {
        val candidates = if (requested == "com.retroarch.aarch64") {
            listOf("com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32")
        } else listOf(requested)
        return candidates.firstOrNull {
            context.packageManager.getLaunchIntentForPackage(it) != null
        }
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
                buildHandoffRejectedMessage(capability)
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

    private fun buildHandoffRejectedMessage(capability: EmulatorCapability): String {
        val coreHint = capability.requiredCore?.let { " Verify that the $it core is installed and selected." } ?: ""
        return "The approved " + registry.displayName(capability.packageName) +
            " handoff was rejected. Open the emulator and load the installed game manually." + coreHint
    }

    private fun update(gameId: GameId, status: LaunchUiState.Status, message: String) {
        state.value = LaunchUiState(status, gameId, message)
    }
}

