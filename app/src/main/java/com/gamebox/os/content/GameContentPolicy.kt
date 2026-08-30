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
            ),
        ),
        "gamecube" to PlatformPolicy(
            defaultExtension = "rvz",
            extensions = mapOf(
                "rvz" to "application/x-dolphin-rvz",
                "gcm" to "application/x-gamecube-rom",
                "iso" to "application/x-iso9660-image",
            ),
        ),
        "wii" to PlatformPolicy(
            defaultExtension = "rvz",
            extensions = mapOf(
                "rvz" to "application/x-dolphin-rvz",
                "wbfs" to "application/x-wbfs",
                "iso" to "application/x-iso9660-image",
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

    fun describe(gameId: String, platform: String, sourceUrl: String?): GameContentDescriptor {
        require(gameId.matches(Regex("^[A-Za-z0-9._-]+$"))) { "Game ID is unsafe for storage" }
        val policy = policies[platform.trim().lowercase()]
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
