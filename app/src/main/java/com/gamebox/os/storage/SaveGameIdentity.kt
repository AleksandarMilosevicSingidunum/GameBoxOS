package com.gamebox.os.storage

import java.io.File
import java.nio.file.Files

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

/** Resolve one game's managed artifact without following linked path components. */
internal fun resolveGameSave(rootDirectory: File, gameId: String, path: String): File {
    requireSavePathForGame(gameId, path)
    val root = rootDirectory.canonicalFile
    var component = root
    path.split('/').forEach {
        component = File(component, it)
        require(!Files.isSymbolicLink(component.toPath())) { "Save path cannot follow symbolic links" }
    }
    val result = component.canonicalFile
    require(result.path.startsWith(File(root, gameId).path + File.separator)) {
        "Save path escaped game storage"
    }
    return result
}
