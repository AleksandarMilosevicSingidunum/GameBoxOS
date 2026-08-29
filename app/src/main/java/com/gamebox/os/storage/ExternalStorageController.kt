package com.gamebox.os.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.gamebox.os.settings.SettingsRepository

enum class ExternalStorageState {
    NOT_CONFIGURED,
    AVAILABLE_READ_WRITE,
    AVAILABLE_READ_ONLY,
    PERMISSION_MISSING,
    DISCONNECTED,
    INVALID
}

data class ExternalStorageStatus(
    val state: ExternalStorageState,
    val displayName: String? = null,
    val message: String
)

class ExternalStorageController(
    context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val applicationContext = context.applicationContext

    suspend fun adoptTree(uri: Uri): ExternalStorageStatus {
        require(uri.scheme == "content") { "External library must use Android document storage" }
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(uri, flags)
        val status = inspect(uri.toString())
        require(status.state == ExternalStorageState.AVAILABLE_READ_WRITE ||
            status.state == ExternalStorageState.AVAILABLE_READ_ONLY) {
            status.message
        }
        settingsRepository.setExternalLibraryUri(uri.toString())
        return status
    }

    suspend fun forgetTree(uriString: String) {
        if (uriString.isNotBlank()) {
            runCatching {
                applicationContext.contentResolver.releasePersistableUriPermission(
                    Uri.parse(uriString),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        settingsRepository.setExternalLibraryUri("")
    }

    fun inspect(uriString: String): ExternalStorageStatus {
        if (uriString.isBlank()) {
            return ExternalStorageStatus(
                ExternalStorageState.NOT_CONFIGURED,
                message = "No external library selected"
            )
        }
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
            ?: return ExternalStorageStatus(ExternalStorageState.INVALID, message = "Stored location is invalid")
        if (uri.scheme != "content") {
            return ExternalStorageStatus(
                ExternalStorageState.INVALID,
                message = "Stored location is not an Android document tree"
            )
        }
        val permission = applicationContext.contentResolver.persistedUriPermissions
            .firstOrNull { it.uri == uri && it.isReadPermission }
            ?: return ExternalStorageStatus(
                ExternalStorageState.PERMISSION_MISSING,
                message = "External library permission must be granted again"
            )
        val document = runCatching { DocumentFile.fromTreeUri(applicationContext, uri) }
            .getOrNull()
        if (document == null || !document.exists() || !document.isDirectory || !document.canRead()) {
            return ExternalStorageStatus(
                ExternalStorageState.DISCONNECTED,
                message = "External library is unavailable or disconnected"
            )
        }
        return if (permission.isWritePermission && document.canWrite()) {
            ExternalStorageStatus(
                ExternalStorageState.AVAILABLE_READ_WRITE,
                document.name,
                "External library is available"
            )
        } else {
            ExternalStorageStatus(
                ExternalStorageState.AVAILABLE_READ_ONLY,
                document.name,
                "External library is read-only"
            )
        }
    }
}
