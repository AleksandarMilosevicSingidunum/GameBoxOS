package com.gamebox.os.storage

enum class SavePresence { NONE, PRESENT, ERROR }

data class SaveSummary(
    val gameId: String,
    val presence: SavePresence,
    val artifactCount: Int = 0,
    val totalBytes: Long = 0,
    val latestModifiedAtMillis: Long? = null,
    val message: String? = null,
)

class SaveInspectionService(private val adapter: SaveAdapter) {
    fun inspect(gameId: String): SaveSummary =
        runCatching { adapter.discover(gameId) }.fold(
            onSuccess = { artifacts ->
                if (artifacts.isEmpty()) {
                    SaveSummary(gameId, SavePresence.NONE)
                } else {
                    SaveSummary(
                        gameId = gameId,
                        presence = SavePresence.PRESENT,
                        artifactCount = artifacts.size,
                        totalBytes = artifacts.sumOf { it.sizeBytes },
                        latestModifiedAtMillis = artifacts.maxOf { it.modifiedAtMillis },
                    )
                }
            },
            onFailure = { error ->
                SaveSummary(
                    gameId = gameId,
                    presence = SavePresence.ERROR,
                    message = error.message ?: "Save discovery failed",
                )
            },
        )
}
