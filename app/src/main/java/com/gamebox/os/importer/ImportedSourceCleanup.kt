package com.gamebox.os.importer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

/** Deletes only the selected original after its imported copy is verified and registered. */
internal fun deleteImportedSource(context: Context, uri: Uri): Boolean = runCatching {
    when {
        uri.scheme == "file" -> uri.path?.let { java.io.File(it).delete() } == true
        DocumentsContract.isDocumentUri(context, uri) -> {
            val access = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            // ACTION_OPEN_DOCUMENT grants can be temporary. Promote the selected
            // write grant while the import runs, then release it after deletion.
            runCatching { context.contentResolver.takePersistableUriPermission(uri, access) }
            try {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } finally {
                runCatching { context.contentResolver.releasePersistableUriPermission(uri, access) }
            }
        }
        else -> context.contentResolver.delete(uri, null, null) > 0
    }
}.getOrDefault(false)
