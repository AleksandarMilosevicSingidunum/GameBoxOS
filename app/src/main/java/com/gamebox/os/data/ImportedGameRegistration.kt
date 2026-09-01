package com.gamebox.os.data

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlin.math.ceil

data class ImportedGameRegistration(
    val id: GameId,
    val title: String,
    val platform: String,
    val year: Int,
    val sizeBytes: Long,
    val relativePath: String,
    val sha256: String,
    val mimeType: String,
    val favorite: Boolean = false,
    val artworkUrl: String? = null,
    val description: String? = null,
    val players: String? = null,
    val region: String? = null,
) {
    init {
        require(title.isNotBlank()) { "Imported title is required" }
        require(platform.isNotBlank()) { "Imported platform is required" }
        require(sizeBytes >= 0) { "Imported size must not be negative" }
        require(sha256.matches(Regex("^[a-f0-9]{64}$"))) { "Imported SHA-256 is invalid" }
        require(mimeType.isNotBlank()) { "Imported MIME type is required" }
        require(relativePath.startsWith(id.value + "/")) { "Imported path is outside the selected game" }
        require(relativePath.none { it == '\\' } && relativePath.split('/').none { it == ".." || it.isEmpty() }) {
            "Imported path is unsafe"
        }
    }
}

fun mergeImportedGame(existing: Game?, imported: ImportedGameRegistration): Game = Game(
    id = imported.id,
    title = imported.title,
    platform = imported.platform,
    year = imported.year,
    genre = existing?.genre?.takeUnless { it.isBlank() } ?: "Imported",
    sizeMb = if (imported.sizeBytes == 0L) 0 else ceil(imported.sizeBytes / (1024.0 * 1024.0)).toInt().coerceAtLeast(1),
    state = InstallState.INSTALLED,
    lastPlayed = existing?.lastPlayed,
    minutesPlayed = existing?.minutesPlayed ?: 0,
    favorite = existing?.favorite ?: imported.favorite,
    sourceUrl = existing?.sourceUrl,
    expectedSha256 = existing?.expectedSha256,
    emulatorPackage = existing?.emulatorPackage,
    graphicsProfile = existing?.graphicsProfile ?: "Balanced",
    artworkUrl = imported.artworkUrl ?: existing?.artworkUrl,
    description = imported.description ?: existing?.description,
    players = imported.players ?: existing?.players,
    language = existing?.language,
    region = imported.region ?: existing?.region,
    localContentRelativePath = imported.relativePath,
    localContentSha256 = imported.sha256,
    localContentMimeType = imported.mimeType,
)
