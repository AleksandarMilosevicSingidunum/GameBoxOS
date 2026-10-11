package com.gamebox.os.ui

import kotlinx.coroutines.CancellationException

/** Runs one settings mutation with consistent busy cleanup and user-facing error reporting. */
internal suspend fun runSettingsUiAction(
    failureMessage: String,
    onBusyChanged: (Boolean) -> Unit,
    onFailure: (String) -> Unit,
    action: suspend () -> Unit,
) {
    onBusyChanged(true)
    try {
        action()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        onFailure(error.message ?: failureMessage)
    } finally {
        onBusyChanged(false)
    }
}
