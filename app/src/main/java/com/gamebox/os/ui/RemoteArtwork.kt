package com.gamebox.os.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val MAX_ARTWORK_BYTES = 4 * 1024 * 1024
private val artworkCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}

/** Decoration only: visible title and console semantics are supplied by the parent card. */
@Composable
internal fun RemoteArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    fallbackKey: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val cacheDir = LocalContext.current.cacheDir
    var bitmap by remember(url) { mutableStateOf(url?.let(artworkCache::get)) }
    LaunchedEffect(url) {
        if (bitmap != null || url.isNullOrBlank() || !url.startsWith("https://")) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            try { loadArtwork(url, cacheDir) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        }
    }
    Box(modifier) {
        if (bitmap == null) ArtworkPlaceholder(fallbackKey ?: url.orEmpty(), Modifier.fillMaxSize())
        else Image(bitmap!!.asImageBitmap(), contentDescription = null, contentScale = contentScale, modifier = Modifier.fillMaxSize())
    }
}

private fun loadArtwork(url: String, cacheDir: File): Bitmap? {
    artworkCache.get(url)?.let { return it }
    val directory = File(cacheDir, "box-art").apply { mkdirs() }
    val name = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
    val cached = File(directory, name)
    var bytes = cached.takeIf { it.isFile && it.length() in 1..MAX_ARTWORK_BYTES.toLong() }?.readBytes()
    if (bytes == null) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 8_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "image/*")
        }
        try {
            if (connection.responseCode !in 200..299 || connection.contentLengthLong > MAX_ARTWORK_BYTES) return null
            bytes = connection.inputStream.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                while (output.size() <= MAX_ARTWORK_BYTES) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            if (bytes.size > MAX_ARTWORK_BYTES) return null
        } finally { connection.disconnect() }
    }
    val data = bytes ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) { cached.delete(); return null }
    val options = BitmapFactory.Options().apply {
        inSampleSize = 1
        while (bounds.outWidth / inSampleSize > 1600 || bounds.outHeight / inSampleSize > 1600) inSampleSize *= 2
    }
    val result = BitmapFactory.decodeByteArray(data, 0, data.size, options) ?: return null
    artworkCache.put(url, result)
    if (!cached.exists()) {
        // Cache failure must never prevent already decoded artwork from appearing.
        runCatching {
            val temporary = File.createTempFile("art-", ".tmp", directory)
            try { temporary.writeBytes(data); temporary.renameTo(cached) } finally { temporary.delete() }
            var total = directory.listFiles()?.sumOf { it.length() } ?: 0L
            directory.listFiles()?.sortedBy { it.lastModified() }?.forEach { file ->
                if (total > 48L * 1024 * 1024 && file != cached) {
                    val length = file.length()
                    if (file.delete()) total -= length
                }
            }
        }
    }
    return result
}

@Composable
private fun ArtworkPlaceholder(key: String, modifier: Modifier) {
    val palette = listOf(Color(0xFF22549B), Color(0xFF573E91), Color(0xFF176C75), Color(0xFF913E6F), Color(0xFF345F66))
    val accent = palette[(key.hashCode() and Int.MAX_VALUE) % palette.size]
    Canvas(modifier.background(Brush.linearGradient(listOf(accent, Color(0xFF0B1526))))) {
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .9f), Color.Transparent), center = Offset(size.width * .72f, size.height * .28f), radius = size.maxDimension * .7f),
            radius = size.maxDimension, center = center)
        val ridge = Path().apply {
            moveTo(0f, size.height * .72f)
            lineTo(size.width * .35f, size.height * .33f)
            lineTo(size.width * .61f, size.height * .57f)
            lineTo(size.width, size.height * .18f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(ridge, Color.White.copy(alpha = .09f))
        drawLine(Color.White.copy(alpha = .20f), Offset(size.width * .15f, size.height * .18f), Offset(size.width * .76f, size.height * .79f), strokeWidth = 1.5f)
        drawCircle(Color.White.copy(alpha = .13f), radius = size.minDimension * .14f, center = Offset(size.width * .74f, size.height * .27f))
    }
}
