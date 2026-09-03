package com.gamebox.os.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

private const val MAX_ARTWORK_BYTES = 2L * 1024L * 1024L
private val artworkCache = object : LruCache<String, android.graphics.Bitmap>(8) {}

internal fun isSafeArtworkUrl(value: String?): Boolean {
    val uri = runCatching { URI(value?.trim().orEmpty()) }.getOrNull() ?: return false
    return uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null &&
        uri.fragment == null
}

@Composable
internal fun RemoteArtwork(url: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf(url?.let { artworkCache.get(it) }) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            if (!isSafeArtworkUrl(url)) return@withContext null
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 6_000
                    instanceFollowRedirects = false
                    setRequestProperty("Accept", "image/*")
                }
                try {
                    if (connection.responseCode !in 200..299 || connection.contentLengthLong > MAX_ARTWORK_BYTES) return@runCatching null
                    connection.inputStream.use { stream ->
                        val bytes = stream.readNBytes(MAX_ARTWORK_BYTES.toInt() + 1)
                        if (bytes.size > MAX_ARTWORK_BYTES) null
                        else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }
        bitmap?.let { loaded -> url?.let { artworkCache.put(it, loaded) } }
    }
    if (bitmap == null) {
        androidx.compose.foundation.layout.Box(modifier.background(Color.Transparent))
    } else {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // Blueprint artwork is a first-class surface, not a faint watermark.
            // Screen-level gradients provide the contrast needed for readable text.
            modifier = modifier.alpha(0.86f)
        )
    }
}

