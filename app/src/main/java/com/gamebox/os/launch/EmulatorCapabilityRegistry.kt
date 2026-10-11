package com.gamebox.os.launch

import android.annotation.SuppressLint
import com.gamebox.os.content.GameContentPolicy
import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.LocalContentFile

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

internal fun emulatorPackageCandidates(packageName: String): List<String> =
    if (packageName == "com.retroarch.aarch64") {
        listOf("com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32")
    } else listOf(packageName)

/** Stable path required by RetroArch's exported Android launcher activity. */
@SuppressLint("SdCardPath") // RetroArch requires its own app-private path, not GameBox's filesDir.
internal fun retroArchCorePath(packageName: String, coreFileName: String): String {
    require(packageName.startsWith("com.retroarch")) { "RetroArch package is required" }
    require(coreFileName.endsWith("_libretro_android.so")) { "Android libretro core is required" }
    return "/data/data/$packageName/cores/$coreFileName"
}

/** Resolves supported emulator packages, cores, and verified content locations per game. */
class EmulatorCapabilityRegistry(
    private val capabilities: List<EmulatorCapability> = listOf(
        EmulatorCapability(
            id = "nes-emu-galaxy-patrol",
            gameId = GameId("galaxy-patrol"),
            packageName = "com.explusalpha.NesEmu",
            contentRelativePath = "retro/galaxy-patrol/content/galaxy-patrol.nes",
            mimeType = "application/x-nes-rom",
            expectedSha256 = "97c1757ffd6a5bc1a591809b2b0f8988741f61f6abd82889c148ecae8a2f471f",
        ),
    ),
) {
    fun readinessMessage(game: Game): String? = forGame(game)?.let { capability ->
        capability.requiredCore?.let { core ->
            "Install RetroArch (standard, ARM64, or RA32) and the $core core before launching this game."
        }
    }

    fun displayName(packageName: String): String = when (packageName) {
        "com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32" -> "RetroArch"
        "org.ppsspp.ppsspp", "org.ppsspp.ppssppgold", "org.ppsspp.ppsspplegacy" -> "PPSSPP"
        "org.dolphinemu.dolphinemu" -> "Dolphin"
        "xyz.aethersx2.android" -> "AetherSX2"
        "com.github.stenzek.duckstation" -> "DuckStation"
        "com.explusalpha.NesEmu" -> "NES.emu"
        "org.mupen64plusae.v3.fzurita" -> "M64Plus FZ"
        "com.flycast.emulator" -> "Flycast"
        "org.azahar_emu.azahar" -> "Azahar"
        else -> packageName.substringAfterLast(".")
    }

    /** Makes otherwise identical RetroArch choices unambiguous in game settings. */
    fun optionDisplayName(packageName: String): String = when (packageName) {
        "com.retroarch.aarch64" -> "RetroArch ARM64"
        "com.retroarch" -> "RetroArch standard"
        "com.retroarch.ra32" -> "RetroArch RA32"
        else -> displayName(packageName)
    }

    fun optionsFor(game: Game): List<String> = (when (game.platform.lowercase().filter(Char::isLetterOrDigit)) {
        "psp", "playstationportable", "sonyplaystationportable" ->
            listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold", "org.ppsspp.ppsspplegacy")
        "ps1", "psx", "playstation", "sonyplaystation" -> listOf("com.github.stenzek.duckstation")
        "n64", "nintendo64" -> listOf("org.mupen64plusae.v3.fzurita")
        "dreamcast", "segadreamcast" -> listOf("com.flycast.emulator")
        "gamecube", "nintendogamecube", "wii", "nintendowii", "wiiware" ->
            listOf("org.dolphinemu.dolphinemu")
        "3ds", "nintendo3ds" -> listOf("org.azahar_emu.azahar")
        "ps2", "playstation2", "sonyplaystation2" -> listOf("xyz.aethersx2.android")
        "nes", "famicom", "nintendoentertainmentsystem" -> listOf("com.explusalpha.NesEmu")
        "snes", "superfamicom", "supernintendo", "supernintendoentertainmentsystem" ->
            listOf("com.explusalpha.Snes9xPlus")
        "gb", "gameboy", "nintendogameboy", "gbc", "gameboycolor", "nintendogameboycolor" ->
            listOf("com.explusalpha.GbcEmu")
        "gba", "gameboyadvance", "nintendogameboyadvance" -> listOf("com.explusalpha.GbaEmu")
        "ds", "nds", "nintendods", "dsi" -> listOf("me.magnum.melonds")
        "mastersystem", "segamastersystem", "gamegear", "segagamegear",
        "genesis", "megadrive", "segagenesis", "segamegadrive" -> listOf("com.explusalpha.MdEmu")
        "atari2600" -> listOf("com.explusalpha.A2600Emu")
        else -> emptyList()
    } + listOfNotNull(forGame(game.id)?.packageName)).distinct()

    fun hasApprovedAdapter(game: Game): Boolean = forGame(game.id) != null || optionsFor(game).isNotEmpty()

    fun forGame(gameId: GameId): EmulatorCapability? = capabilities.firstOrNull { it.gameId == gameId }

    fun forGame(game: Game, platformDefaultPackage: String? = null): EmulatorCapability? {
        val explicit = capabilities.firstOrNull { it.gameId == game.id }
        val importedPath = game.localContentRelativePath
        val importedChecksum = game.localContentSha256
        val importedMimeType = game.localContentMimeType
        if (importedPath != null && importedChecksum != null && importedMimeType != null) {
            val packageName = game.emulatorPackage?.takeIf { it in optionsFor(game) }
                ?: platformDefaultPackage?.takeIf { it in optionsFor(game) }
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
                requiredCore = explicit?.requiredCore.takeIf { packageName.startsWith("com.retroarch") },
                retroArchCoreFileName = explicit?.retroArchCoreFileName.takeIf { packageName.startsWith("com.retroarch") },
                contentRoot = EmulatorContentRoot.IMPORTS,
                companionFiles = game.localContentFiles.filterNot { file -> file.relativePath == importedPath },
            )
        }
        explicit?.let { return it }
        val checksum = game.expectedSha256 ?: return null
        val packageName = game.emulatorPackage?.takeIf { it in optionsFor(game) }
            ?: platformDefaultPackage?.takeIf { it in optionsFor(game) }
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
            graphicsProfile = game.graphicsProfile,
            requiredCore = explicit?.requiredCore.takeIf { packageName.startsWith("com.retroarch") },
            retroArchCoreFileName = explicit?.retroArchCoreFileName.takeIf { packageName.startsWith("com.retroarch") },
        )
    }
}
