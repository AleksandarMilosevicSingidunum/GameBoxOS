package com.gamebox.os.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.gamebox.os.settings.SettingsRepository
import com.gamebox.os.storage.EmulatorSaveStorageController
import com.gamebox.os.storage.ExternalStorageController
import com.gamebox.os.storage.ExternalStorageState
import com.gamebox.os.storage.ExternalStorageStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class SettingsStorageManagementState(
    val externalController: ExternalStorageController,
    val emulatorSaveController: EmulatorSaveStorageController,
    val externalStatus: ExternalStorageStatus?,
    val emulatorSaveStatus: ExternalStorageStatus?,
    val chooseExternalLibrary: () -> Unit,
    val chooseEmulatorSaveRoot: () -> Unit,
)

/** Owns SAF controllers, permission inspection, and folder-picker actions for Settings. */
@Composable
internal fun rememberSettingsStorageManagement(
    context: Context,
    settingsRepository: SettingsRepository,
    externalLibraryUri: String,
    emulatorSaveRootUri: String,
    onMessage: (String) -> Unit,
): SettingsStorageManagementState {
    val externalController = remember(context, settingsRepository) {
        ExternalStorageController(context, settingsRepository)
    }
    val emulatorSaveController = remember(context, settingsRepository) {
        EmulatorSaveStorageController(context, settingsRepository)
    }
    val externalStatus = rememberStorageStatus(
        externalLibraryUri,
        externalController,
    ) { externalController.inspect(it) }
    val emulatorSaveStatus = rememberStorageStatus(
        emulatorSaveRootUri,
        emulatorSaveController,
    ) { emulatorSaveController.inspect(it) }
    val chooseExternalLibrary = rememberStorageTreePicker(
        context = context,
        failureMessage = "External library could not be selected",
        onAdoptTree = externalController::adoptTree,
        onMessage = onMessage,
    )
    val chooseEmulatorSaveRoot = rememberStorageTreePicker(
        context = context,
        failureMessage = "Emulator save root could not be selected",
        onAdoptTree = emulatorSaveController::adoptTree,
        onMessage = onMessage,
    )
    return SettingsStorageManagementState(
        externalController = externalController,
        emulatorSaveController = emulatorSaveController,
        externalStatus = externalStatus,
        emulatorSaveStatus = emulatorSaveStatus,
        chooseExternalLibrary = chooseExternalLibrary,
        chooseEmulatorSaveRoot = chooseEmulatorSaveRoot,
    )
}

/** Performs SAF status queries off-main; [inspectorKey] must be stable for an inspector instance. */
@Composable
internal fun rememberStorageStatus(
    uriString: String,
    inspectorKey: Any,
    inspect: (String) -> ExternalStorageStatus,
): ExternalStorageStatus? {
    val status by produceState<ExternalStorageStatus?>(null, uriString, inspectorKey) {
        value = null
        value = withContext(Dispatchers.IO) {
            runCatching { inspect(uriString) }.getOrElse { failure ->
                ExternalStorageStatus(
                    state = ExternalStorageState.INVALID,
                    message = failure.message ?: "Storage location could not be inspected",
                )
            }
        }
    }
    return status
}
