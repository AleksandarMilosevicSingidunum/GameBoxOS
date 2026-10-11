package com.gamebox.os.importer

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.gamebox.os.settings.AndroidKeystoreSecretStore

@Serializable
data class BrowserDownloadRecord(
    val downloadId: Long,
    val stagedName: String,
    val displayName: String,
    val gameId: String,
    val title: String,
    val platformName: String,
    val platformId: String,
    val region: String? = null,
    val releaseDate: String? = null,
    val description: String? = null,
    val players: String? = null,
    val rating: Double? = null,
    val coverUrl: String? = null,
    val backgroundUrl: String? = null,
    val logoUrl: String? = null,
    val downloadUrl: String? = null,
    val userAgent: String? = null,
    val mimeType: String? = null,
    val referer: String? = null,
    val status: BrowserDownloadStatus = BrowserDownloadStatus.QUEUED,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val bytesPerSecond: Long = 0L,
    val importStage: String? = null,
    val importBytesProcessed: Long = 0L,
    val importTotalBytes: Long? = null,
    val importFileIndex: Int = 0,
    val importFileCount: Int = 0,
    val error: String? = null,
)

@Serializable
enum class BrowserDownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    IMPORTING,
    IMPORTED,
    FAILED,
}

internal val ACTIVE_BROWSER_DOWNLOAD_STATUSES = setOf(
    BrowserDownloadStatus.QUEUED,
    BrowserDownloadStatus.DOWNLOADING,
    BrowserDownloadStatus.PAUSED,
    BrowserDownloadStatus.IMPORTING,
)
private const val MAX_BROWSER_DOWNLOAD_RECORDS = 100

internal fun retainRecentBrowserDownloads(records: List<BrowserDownloadRecord>): List<BrowserDownloadRecord> {
    val active = records.filter { it.status in ACTIVE_BROWSER_DOWNLOAD_STATUSES }
    val terminal = records.asSequence()
        .filterNot { it.status in ACTIVE_BROWSER_DOWNLOAD_STATUSES }
        .sortedByDescending(BrowserDownloadRecord::downloadId)
        .take((MAX_BROWSER_DOWNLOAD_RECORDS - active.size).coerceAtLeast(0))
        .toList()
    return (active + terminal).sortedByDescending(BrowserDownloadRecord::downloadId)
}

/** Durable, app-owned metadata for DownloadManager transfers initiated from a source browser. */
class BrowserDownloadStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val secrets = AndroidKeystoreSecretStore(appContext)
    private val lock = Any()
    private val json = Json { ignoreUnknownKeys = true }
    private val mutableRecords = MutableStateFlow(readRecords())
    val records: StateFlow<List<BrowserDownloadRecord>> = mutableRecords.asStateFlow()

    fun add(record: BrowserDownloadRecord) = mutate { current ->
        (current.filterNot { it.downloadId == record.downloadId } + record)
            .sortedByDescending { it.downloadId }
    }

    fun replace(oldDownloadId: Long, replacement: BrowserDownloadRecord) = mutate { current ->
        (current.filterNot { it.downloadId == oldDownloadId || it.downloadId == replacement.downloadId } + replacement)
            .sortedByDescending { it.downloadId }
    }

    fun setRetryCookie(downloadId: Long, cookie: String?) =
        secrets.put(retryCookieKey(downloadId), cookie)

    fun retryCookie(downloadId: Long): String? = secrets.get(retryCookieKey(downloadId))

    private fun retryCookieKey(downloadId: Long) = "browser_download_cookie_$downloadId"

    fun update(downloadId: Long, transform: (BrowserDownloadRecord) -> BrowserDownloadRecord) =
        mutate { current -> current.map { if (it.downloadId == downloadId) transform(it) else it } }

    fun get(downloadId: Long): BrowserDownloadRecord? =
        records.value.firstOrNull { it.downloadId == downloadId }

    private fun mutate(transform: (List<BrowserDownloadRecord>) -> List<BrowserDownloadRecord>) {
        synchronized(lock) {
            val updated = retainRecentBrowserDownloads(transform(mutableRecords.value))
            check(preferences.edit().putString(KEY_RECORDS, json.encodeToString(updated)).commit()) {
                "Could not persist browser download state"
            }
            mutableRecords.value = updated
        }
    }

    private fun readRecords(): List<BrowserDownloadRecord> = runCatching {
        json.decodeFromString<List<BrowserDownloadRecord>>(
            preferences.getString(KEY_RECORDS, "[]") ?: "[]",
        ).let(::retainRecentBrowserDownloads)
    }.getOrDefault(emptyList())

    private companion object {
        const val PREFERENCES_NAME = "tracked_browser_downloads"
        const val KEY_RECORDS = "records"
    }
}
