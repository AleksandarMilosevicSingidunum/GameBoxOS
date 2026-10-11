package com.gamebox.os.launch

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.edit
import java.io.File
import java.io.InputStream
import java.util.UUID

internal data class RetroArchContentSource(
    val relativePath: String,
    val mimeType: String,
    val openSource: () -> InputStream,
)

/** Publishes verified content only for RetroArch builds that require a filesystem path. */
internal class RetroArchContentPublisher(
    context: Context,
    preferencesName: String = PREFERENCES,
) {
    private val applicationContext = context.applicationContext
    private val preference = applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun cleanup() {
        val persistedUris = preference.getStringSet(PUBLISHED_URI_KEY, emptySet())
            .orEmpty()
            .mapNotNull { raw -> runCatching { android.net.Uri.parse(raw) }.getOrNull() }
        persistedUris.distinct().forEach { uri ->
            runCatching { applicationContext.contentResolver.delete(uri, null, null) }
        }
        preference.edit { remove(PUBLISHED_URI_KEY) }
    }

    fun publish(
        source: File,
        fileName: String,
        mimeType: String,
        relativePath: String = DEFAULT_RELATIVE_PATH,
    ): String? = publish(fileName, mimeType, relativePath) { source.inputStream() }

    fun publish(
        fileName: String,
        mimeType: String,
        relativePath: String = DEFAULT_RELATIVE_PATH,
        openSource: () -> InputStream,
    ): String? = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@runCatching null
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val published = applicationContext.contentResolver.insert(collection, values)
            ?: return@runCatching null
        try {
            applicationContext.contentResolver.openOutputStream(published, "w")?.use { output ->
                openSource().use { input -> input.copyTo(output) }
            } ?: throw java.io.IOException("Cannot write exported game")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            check(applicationContext.contentResolver.update(published, values, null, null) == 1) {
                "Cannot publish exported game"
            }
            // MediaStore may rename duplicate names; launch with the actual path.
            @Suppress("DEPRECATION")
            val exportedPath = applicationContext.contentResolver.query(
                published,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            check(!exportedPath.isNullOrBlank() && File(exportedPath).isAbsolute) {
                "Exported game has no filesystem path"
            }
            track(listOf(published))
            exportedPath
        } catch (error: Exception) {
            applicationContext.contentResolver.delete(published, null, null)
            throw error
        }
    }.getOrNull()

    /** Publishes an entire descriptor-backed disc set so CUE/GDI references resolve beside it. */
    fun publishSet(
        files: List<RetroArchContentSource>,
        primaryRelativePath: String,
        relativeDirectory: String = "Download/GameBox/session-${UUID.randomUUID()}",
    ): String? = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@runCatching null
        require(files.isNotEmpty()) { "No RetroArch content was supplied" }
        val normalized = files.map { source ->
            val segments = safeRelativeSegments(source.relativePath)
            require(source.mimeType.isNotBlank()) { "RetroArch content MIME type is missing" }
            source to segments
        }
        val primarySegments = safeRelativeSegments(primaryRelativePath)
        val primaryName = primarySegments.last()
        val sourceParent = primarySegments.dropLast(1)
        require(normalized.all { (_, segments) -> segments.dropLast(1) == sourceParent }) {
            "Disc descriptor and tracks must share one imported directory"
        }
        require(normalized.map { it.second.last().lowercase() }.distinct().size == normalized.size) {
            "Disc set contains duplicate filenames"
        }
        val primary = normalized.singleOrNull { (_, segments) -> segments == primarySegments }
            ?: throw IllegalArgumentException("Primary RetroArch content is not in the verified disc set")

        val folder = safeRelativeSegments(relativeDirectory).joinToString("/")
        require(folder.startsWith("Download/GameBox/")) { "RetroArch exports must stay in GameBox's Downloads folder" }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val pending = mutableListOf<Pair<android.net.Uri, RetroArchContentSource>>()
        try {
            normalized.forEach { (source, segments) ->
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, segments.last())
                    put(MediaStore.Downloads.MIME_TYPE, source.mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "$folder/")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = applicationContext.contentResolver.insert(collection, values)
                    ?: throw java.io.IOException("Cannot create RetroArch disc-set export")
                pending += uri to source
                applicationContext.contentResolver.openOutputStream(uri, "w")?.use { output ->
                    source.openSource().use { input -> input.copyTo(output) }
                } ?: throw java.io.IOException("Cannot write RetroArch disc-set file")
            }
            pending.forEach { (uri, _) ->
                val publishValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                check(applicationContext.contentResolver.update(uri, publishValues, null, null) == 1) {
                    "Cannot publish RetroArch disc-set file"
                }
            }
            val primaryUri = pending.single { (_, source) -> source.relativePath == primary.first.relativePath }.first
            @Suppress("DEPRECATION")
            val exportedPath = applicationContext.contentResolver.query(
                primaryUri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            check(!exportedPath.isNullOrBlank() && File(exportedPath).isAbsolute) {
                "Exported disc descriptor has no filesystem path"
            }
            track(pending.map { it.first })
            exportedPath
        } catch (error: Exception) {
            pending.forEach { (uri, _) ->
                runCatching { applicationContext.contentResolver.delete(uri, null, null) }
            }
            throw error
        }
    }.getOrNull()

    private fun track(uris: List<android.net.Uri>) {
        synchronized(preference) {
            val trackedUris = preference.getStringSet(PUBLISHED_URI_KEY, emptySet())
                .orEmpty()
                .toMutableSet()
                .apply { addAll(uris.map(android.net.Uri::toString)) }
            check(preference.edit().putStringSet(PUBLISHED_URI_KEY, trackedUris).commit()) {
                "Could not persist RetroArch export cleanup ownership"
            }
        }
    }

    private fun safeRelativeSegments(relativePath: String): List<String> {
        require(relativePath.isNotBlank() && '\\' !in relativePath && ':' !in relativePath && '\u0000' !in relativePath) {
            "Unsafe RetroArch content path"
        }
        val segments = relativePath.split('/')
        require(segments.none { it.isBlank() || it == "." || it == ".." }) {
            "Unsafe RetroArch content path"
        }
        return segments
    }

    private companion object {
        const val PREFERENCES = "gamebox_launch_exports"
        const val PUBLISHED_URI_KEY = "uris"
        const val DEFAULT_RELATIVE_PATH = "Download/GameBox"
    }
}
