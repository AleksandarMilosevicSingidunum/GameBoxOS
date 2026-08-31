package com.gamebox.os.importer

import com.gamebox.os.domain.GameId

object RomImportPolicy {
    private val supportedExtensions = setOf(
        "nes", "sfc", "smc", "gb", "gbc", "gba", "n64", "z64", "v64",
        "cue", "bin", "iso", "chd", "cso", "gcz", "rvz", "zip", "7z", "rom"
    )

    fun safeFileName(value: String): String {
        val name = value.trim().substringAfterLast('/').substringAfterLast('\\')
        require(name.length in 1..180) { "Invalid import filename" }
        require(name.none { it.code < 32 }) { "Invalid import filename" }
        require(name != "." && name != "..") { "Invalid import filename" }
        val extension = name.substringAfterLast('.', "").lowercase()
        require(extension in supportedExtensions) { "Unsupported game file type" }
        return name
    }

    fun relativePath(gameId: GameId, fileName: String): String {
        val id = gameId.value
        require(id.matches(Regex("[a-z0-9][a-z0-9-]{0,95}"))) { "Invalid game id" }
        return "imports/$id/" + safeFileName(fileName)
    }
}
