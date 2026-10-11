package com.gamebox.os.ui

import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.data.ImportedGameRegistration
import com.gamebox.os.domain.LocalContentFile

/** Builds the same Library record for single-file imports and multi-disc sets. */
internal fun importedGameRegistration(
    game: DiscoveryGame,
    platform: String,
    launchFile: LocalContentFile,
    files: List<LocalContentFile>,
    sizeBytes: Long,
): ImportedGameRegistration {
    require(files.any { it.relativePath == launchFile.relativePath }) {
        "Launch file must be part of the imported content set"
    }
    val releaseYear = game.releaseDate
        ?.let { Regex("""(?:19|20)\d{2}""").find(it)?.value?.toIntOrNull() }
        ?: 0
    return ImportedGameRegistration(
        id = game.id,
        title = game.title,
        platform = platform,
        year = releaseYear,
        sizeBytes = sizeBytes,
        relativePath = launchFile.relativePath,
        sha256 = launchFile.sha256,
        mimeType = launchFile.mimeType,
        favorite = game.favorite,
        artworkUrl = game.coverUrl,
        description = game.description,
        players = game.players,
        region = game.region,
        additionalFiles = files.filterNot { it.relativePath == launchFile.relativePath },
    )
}
