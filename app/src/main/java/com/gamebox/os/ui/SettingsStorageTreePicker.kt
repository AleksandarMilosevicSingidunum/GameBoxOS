package com.gamebox.os.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.gamebox.os.display.externalDisplayActivityResultOptions
import com.gamebox.os.storage.ExternalStorageStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberStorageTreePicker(
    context: Context,
    failureMessage: String,
    onAdoptTree: suspend (Uri) -> ExternalStorageStatus,
    onMessage: (String) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) { onAdoptTree(uri) }
                }
                onMessage(storageTreePickerMessage(result, failureMessage))
            }
        }
    }
    val pickerOptions = remember(context) { externalDisplayActivityResultOptions(context) }
    return remember(launcher, pickerOptions) {
        { launcher.launch(null, pickerOptions) }
    }
}

internal fun storageTreePickerMessage(
    result: Result<ExternalStorageStatus>,
    failureMessage: String,
): String = result.fold(
    onSuccess = ExternalStorageStatus::message,
    onFailure = { it.message ?: failureMessage },
)
