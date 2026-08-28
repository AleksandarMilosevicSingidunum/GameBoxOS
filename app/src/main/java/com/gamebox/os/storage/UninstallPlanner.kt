package com.gamebox.os.storage

import com.gamebox.os.domain.GameId

enum class ArtifactKind {
    GAME_CONTENT,
    SAVE_DATA,
    SAVE_STATE,
    METADATA,
    ARTWORK,
    PLAY_HISTORY
}

data class StoredArtifact(
    val gameId: GameId,
    val uri: String,
    val kind: ArtifactKind,
    val sizeBytes: Long
) {
    init {
        require(uri.isNotBlank()) { "Artifact URI is required" }
        require(sizeBytes >= 0L) { "Artifact size cannot be negative" }
    }
}

data class UninstallPlan(
    val gameId: GameId,
    val deleteArtifacts: List<StoredArtifact>,
    val retainArtifacts: List<StoredArtifact>
) {
    val bytesFreed: Long = deleteArtifacts.sumOf { it.sizeBytes }
}

class UninstallPlanner {
    fun plan(
        gameId: GameId,
        artifacts: List<StoredArtifact>,
        deleteProgress: Boolean = false
    ): UninstallPlan {
        require(artifacts.all { it.gameId == gameId }) {
            "Uninstall input cannot contain artifacts from another game"
        }

        val deletableKinds = if (deleteProgress) {
            setOf(ArtifactKind.GAME_CONTENT, ArtifactKind.SAVE_DATA, ArtifactKind.SAVE_STATE)
        } else {
            setOf(ArtifactKind.GAME_CONTENT)
        }
        val (delete, retain) = artifacts.partition { it.kind in deletableKinds }
        return UninstallPlan(gameId, delete, retain)
    }
}
