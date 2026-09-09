package com.gamebox.os.storage

internal fun requireSaveGameId(gameId: String) {
    require(gameId.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,95}"))) {
        "Invalid save game identity"
    }
}

internal fun requireSavePathForGame(gameId: String, path: String) {
    requireSaveGameId(gameId)
    require(path.startsWith("$gameId/") &&
        path.none { it == '\\' || it == ':' || it == '\u0000' } &&
        path.split('/').none { it.isBlank() || it == "." || it == ".." }) {
        "Save artifact does not belong to the selected game"
    }
}

