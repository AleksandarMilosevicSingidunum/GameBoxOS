package com.gamebox.os.storage

import com.gamebox.os.domain.GameId

class InstallPathPolicy {
    fun relativeContentPath(platform: String, gameId: GameId, fileName: String): String {
        val platformSlug = platform.toSlug()
        require(platformSlug.isNotBlank()) { "Platform must contain letters or digits" }
        require(SAFE_ID.matches(gameId.value)) { "Game ID contains unsafe path characters" }
        require(SAFE_FILE.matches(fileName)) { "Filename contains unsafe path characters" }
        require(fileName != "." && fileName != "..") { "Filename cannot be a traversal segment" }
        return platformSlug + "/" + gameId.value + "/content/" + fileName
    }

    private fun String.toSlug(): String = trim()
        .lowercase()
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')

    private companion object {
        val SAFE_ID = Regex("[A-Za-z0-9._-]+")
        val SAFE_FILE = Regex("[A-Za-z0-9._ -]+")
    }
}
