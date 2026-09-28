package com.gamebox.os.importer

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.gamebox.os.domain.normalizeCatalogTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

data class BrowserDownloadCandidate(
    val uri: Uri,
    val displayName: String,
)

/**
 * Observes downloads created by the Android/browser download stack. It does not
 * create a network request; the caller still owns the legal-download decision
 * and passes the completed local URI to AuthorizedRomImporter.
 */
class BrowserDownloadTracker(context: Context) {
    private val applicationContext = context.applicationContext
    private val downloadManager = applicationContext.getSystemService(DownloadManager::class.java)

    fun findCompletedDownload(
        title: String,
        platform: String?,
        startedAtMillis: Long,
    ): BrowserDownloadCandidate? {
        val query = DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
        return runCatching {
            downloadManager.query(query)?.use { cursor ->
                val idColumn = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                val timestampColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)
                val titleColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                val uriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                var candidate: BrowserDownloadCandidate? = null
                while (candidate == null && cursor.moveToNext()) {
                    val timestamp = if (timestampColumn >= 0) cursor.getLong(timestampColumn) else 0L
                    if (timestamp < startedAtMillis) continue
                    val id = if (idColumn >= 0) cursor.getLong(idColumn) else continue
                    val localUri = if (uriColumn >= 0) cursor.getString(uriColumn)?.let(Uri::parse) ?: continue else continue
                    val displayName = displayName(localUri, id)
                    val managerTitle = if (titleColumn >= 0) cursor.getString(titleColumn).orEmpty() else ""
                    if (!matchesTitle(title, displayName, managerTitle)) continue
                    if (runCatching { RomImportPolicy.safeFileName(displayName, platform) }.isFailure) continue
                    candidate = BrowserDownloadCandidate(localUri, displayName)
                }
                candidate
            }
        }.getOrNull()
    }

    suspend fun awaitCompletedDownload(
        title: String,
        platform: String?,
        startedAtMillis: Long,
        timeoutMillis: Long = 30 * 60_000L,
    ): BrowserDownloadCandidate? {
        var candidate: BrowserDownloadCandidate? = null
        withTimeoutOrNull(timeoutMillis) {
            while (candidate == null) {
                candidate = findCompletedDownload(title, platform, startedAtMillis)
                if (candidate == null) delay(POLL_INTERVAL_MILLIS)
            }
        }
        return candidate
    }

    private fun displayName(uri: Uri, downloadId: Long): String = runCatching {
        applicationContext.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: "download-$downloadId"

    private fun matchesTitle(title: String, displayName: String, managerTitle: String): Boolean {
        val normalizedTitle = normalizeCatalogTitle(title)
        if (normalizedTitle.isBlank()) return false
        val haystack = normalizeCatalogTitle(displayName + " " + managerTitle)
        if (haystack.contains(normalizedTitle)) return true
        val tokens = normalizedTitle.split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 4 }
        return tokens.isNotEmpty() && tokens.count(haystack::contains) >= (tokens.size + 1) / 2
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 1_000L
    }
}
