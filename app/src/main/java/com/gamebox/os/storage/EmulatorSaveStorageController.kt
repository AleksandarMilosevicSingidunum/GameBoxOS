package com.gamebox.os.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.gamebox.os.settings.SettingsRepository

/** Owns the separate SAF permission used for emulator-private save synchronization. */
class EmulatorSaveStorageController(
    context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val applicationContext = context.applicationContext

    suspend fun adoptTree(uri: Uri): ExternalStorageStatus {
        require(uri.scheme == "content") { "Emulator save root must use Android document storage" }
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(uri, flags)
        val status = inspect(uri.toString())
        require(status.state == ExternalStorageState.AVAILABLE_READ_WRITE) {
            "Emulator save root must be writable: ${status.message}"
        }
        settingsRepository.setEmulatorSaveRootUri(uri.toString())
        return status
    }

    suspend fun forgetTree(uriString: String) {
        if (uriString.isNotBlank()) {
            runCatching {
                applicationContext.contentResolver.releasePersistableUriPermission(
                    uriString.toUri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        settingsRepository.setEmulatorSaveRootUri("")
    }

    fun inspect(uriString: String): ExternalStorageStatus {
        if (uriString.isBlank()) {
            return ExternalStorageStatus(
                ExternalStorageState.NOT_CONFIGURED,
                message = "No emulator save root selected",
            )
        }
        val uri = runCatching { uriString.toUri() }.getOrNull()
            ?: return ExternalStorageStatus(ExternalStorageState.INVALID, message = "Stored save root is invalid")
        if (uri.scheme != "content") {
            return ExternalStorageStatus(ExternalStorageState.INVALID, message = "Stored save root is not a document tree")
        }
        val permission = applicationContext.contentResolver.persistedUriPermissions
            .firstOrNull { it.uri == uri && it.isReadPermission }
            ?: return ExternalStorageStatus(
                ExternalStorageState.PERMISSION_MISSING,
                message = "Emulator save root permission must be granted again",
            )
        val document = runCatching { DocumentFile.fromTreeUri(applicationContext, uri) }.getOrNull()
        if (document == null || !document.exists() || !document.isDirectory || !document.canRead()) {
            return ExternalStorageStatus(
                ExternalStorageState.DISCONNECTED,
                message = "Emulator save root is unavailable or disconnected",
            )
        }
        return if (permission.isWritePermission && document.canWrite()) {
            ExternalStorageStatus(
                ExternalStorageState.AVAILABLE_READ_WRITE,
                document.name,
                "Emulator save root is available",
            )
        } else {
            ExternalStorageStatus(
                ExternalStorageState.AVAILABLE_READ_ONLY,
                document.name,
                "Emulator save root is read-only",
            )
        }
    }
}
