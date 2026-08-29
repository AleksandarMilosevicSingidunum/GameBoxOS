package com.gamebox.os.launch

import com.gamebox.os.domain.GameId

data class EmulatorLaunchRequest(
    val gameId: GameId,
    val packageName: String,
    val contentUri: String,
    val graphicsProfile: String
)

interface EmulatorAdapter {
    val packageName: String
    fun buildLaunchRequest(request: EmulatorLaunchRequest): EmulatorLaunchRequest
}