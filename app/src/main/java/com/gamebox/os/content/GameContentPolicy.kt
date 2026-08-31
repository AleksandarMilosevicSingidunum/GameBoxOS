package com.gamebox.os.content

import java.net.URI

data class GameContentDescriptor(
    val relativePath: String,
    val mimeType: String,
    val fileExtension: String,
)

object GameContentPolicy {
    private data class PlatformPolicy(
        val defaultExtension: String,
        val extensions: Map<String, String>,
    )

    private val policies = mapOf(
        "ps1" to PlatformPolicy(
            defaultExtension = "chd",
            extensions = mapOf(
                "chd" to "application/x-chd",
                "cue" to "application/x-cue",
                "pbp" to "application/x-playstation-pbp",
            ),
        ),
        "n64" to PlatformPolicy(
            defaultExtension = "z64",
            extensions = mapOf(
                "n64" to "application/x-n64-rom",
                "z64" to "application/x-n64-rom",
                "v64" to "application/x-n64-rom",
            ),
        ),
        "dreamcast" to PlatformPolicy(
            defaultExtension = "chd",
            extensions = mapOf(
                "chd" to "application/x-chd",
                "gdi" to "application/x-dreamcast-gdi",
                "cdi" to "application/x-discjuggler-cdi",
                "cue" to "application/x-cue",
                "bin" to "application/octet-stream",
            ),
        ),
        "psp" to PlatformPolicy(
            defaultExtension = "iso",
            extensions = mapOf(
                "iso" to "application/x-iso9660-image",
                "cso" to "application/x-compressed-iso",
                "pbp" to "application/x-psp-pbp",
            ),
        ),
        "ps2" to PlatformPolicy(
            defaultExtension = "iso",
            extensions = mapOf(
                "iso" to "application/x-iso9660-image",
                "chd" to "application/x-chd",
                "cso" to "application/x-compressed-iso",
                "bin" to "application/octet-stream",
                "cue" to "application/x-cue",
                "mdf" to "application/octet-stream",
                "mds" to "application/octet-stream",
                "nrg" to "application/octet-stream",
            ),
        ),
        "gamecube" to PlatformPolicy(
            defaultExtension = "rvz",
            extensions = mapOf(
                "rvz" to "application/x-dolphin-rvz",
                "gcm" to "application/x-gamecube-rom",
                "iso" to "application/x-iso9660-image",
                "gcz" to "application/x-dolphin-gcz",
                "wia" to "application/x-dolphin-wia",
                "ciso" to "application/x-compressed-iso",
            ),
        ),
        "wii" to PlatformPolicy(
            defaultExtension = "rvz",
            extensions = mapOf(
                "rvz" to "application/x-dolphin-rvz",
                "wbfs" to "application/x-wbfs",
                "iso" to "application/x-iso9660-image",
                "gcz" to "application/x-dolphin-gcz",
                "wia" to "application/x-dolphin-wia",
                "wad" to "application/x-wii-wad",
                "ciso" to "application/x-compressed-iso",
            ),
        ),
        "3ds" to PlatformPolicy(
            defaultExtension = "3ds",
            extensions = mapOf(
                "3ds" to "application/x-nintendo-3ds-rom",
                "cci" to "application/x-nintendo-3ds-rom",
                "cxi" to "application/x-nintendo-3ds-cxi",
                "cia" to "application/x-nintendo-3ds-cia",
                "3dsx" to "application/x-nintendo-3ds-homebrew",
            ),
        ),
        "switch" to PlatformPolicy(
            defaultExtension = "xci",
            extensions = mapOf(
                "xci" to "application/x-nintendo-switch-xci",
                "xcz" to "application/x-nintendo-switch-xcz",
                "nsp" to "application/x-nintendo-switch-nsp",
                "nsz" to "application/x-nintendo-switch-nsz",
                "nca" to "application/x-nintendo-switch-nca",
                "nro" to "application/x-nintendo-switch-homebrew",
                "nso" to "application/x-nintendo-switch-object",
            ),
        ),
        "retro" to PlatformPolicy(
            defaultExtension = "zip",
            extensions = mapOf(
                "zip" to "application/zip",
                "nes" to "application/x-nes-rom",
                "sfc" to "application/x-snes-rom",
                "smc" to "application/x-snes-rom",
                "gb" to "application/x-gameboy-rom",
                "gbc" to "application/x-gameboy-color-rom",
                "gba" to "application/x-gba-rom",
                "n64" to "application/x-n64-rom",
                "z64" to "application/x-n64-rom",
                "v64" to "application/x-n64-rom",
                "md" to "application/x-genesis-rom",
                "gen" to "application/x-genesis-rom",
            ),
        ),
        "homebrew" to PlatformPolicy(
            defaultExtension = "zip",
            extensions = mapOf(
                "zip" to "application/zip",
                "apk" to "application/vnd.android.package-archive",
            ),
        ),
    )

    private val aliases = mapOf(
        "playstation" to "ps1",
        "sonyplaystation" to "ps1",
        "playstation2" to "ps2",
        "sonyplaystation2" to "ps2",
        "playstationportable" to "psp",
        "sonyplaystationportable" to "psp",
        "nintendogamecube" to "gamecube",
        "nintendo64" to "n64",
        "nintendowii" to "wii",
        "nintendo3ds" to "3ds",
        "nintendoswitch" to "switch",
        "segadreamcast" to "dreamcast",
    )

    fun describe(gameId: String, platform: String, sourceUrl: String?): GameContentDescriptor {
        require(gameId.matches(Regex("^[A-Za-z0-9._-]+$"))) { "Game ID is unsafe for storage" }
        val normalizedPlatform = platform.lowercase().filter(Char::isLetterOrDigit)
        val policy = policies[aliases[normalizedPlatform] ?: normalizedPlatform]
        val sourceExtension = sourceUrl
            ?.let(::safePathExtension)
            ?.takeIf { extension -> policy?.extensions?.containsKey(extension) == true }
        val extension = sourceExtension ?: policy?.defaultExtension ?: "bin"
        val mimeType = policy?.extensions?.get(extension) ?: "application/octet-stream"
        return GameContentDescriptor(
            relativePath = "remote/$gameId/content.$extension",
            mimeType = mimeType,
            fileExtension = extension,
        )
    }

    private fun safePathExtension(sourceUrl: String): String? = runCatching {
        val path = URI(sourceUrl).path.orEmpty()
        path.substringAfterLast('/', missingDelimiterValue = "")
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .takeIf { it.matches(Regex("^[a-z0-9]{1,8}$")) }
    }.getOrNull()
}

