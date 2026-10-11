package com.gamebox.os.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.gamebox.os.domain.GameId
import com.gamebox.os.storage.ManagedSaveDiscovery
import com.gamebox.os.storage.SavePresence
import com.gamebox.os.storage.SaveSafetyController
import com.gamebox.os.storage.SaveSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

internal data class GameDetailsSaveContext(
    val controller: SaveSafetyController,
    val description: String,
)

@Composable
internal fun rememberGameDetailsSaveContext(
    gameId: GameId,
    fallbackController: SaveSafetyController,
    discovery: ManagedSaveDiscovery,
    controllerFactory: ((GameId, CoroutineScope) -> SaveSafetyController)?,
    usePerGameController: Boolean,
): GameDetailsSaveContext {
    val parentScope = rememberCoroutineScope()
    val saveScope = remember(gameId, controllerFactory, usePerGameController) {
        if (usePerGameController && controllerFactory != null) {
            CoroutineScope(parentScope.coroutineContext +
                SupervisorJob(parentScope.coroutineContext[Job]))
        } else {
            null
        }
    }
    DisposableEffect(saveScope) {
        onDispose { saveScope?.coroutineContext?.get(Job)?.cancel() }
    }
    val gameController = remember(gameId, controllerFactory, saveScope) {
        saveScope?.let { controllerFactory?.invoke(gameId, it) }
    }
    val discoveredSaves by discovery.state.collectAsState()
    return GameDetailsSaveContext(
        controller = gameController ?: fallbackController,
        description = managedSaveDescription(discoveredSaves[gameId.value]),
    )
}

internal fun managedSaveDescription(summary: SaveSummary?): String = when (summary?.presence) {
    SavePresence.PRESENT ->
        "${summary.artifactCount} managed save files · ${formatDownloadBytes(summary.totalBytes)}"
    SavePresence.NONE -> "No managed save files found"
    SavePresence.ERROR -> "Managed saves could not be inspected"
    null -> "Inspecting managed saves…"
}
