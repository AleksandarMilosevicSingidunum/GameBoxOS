package com.gamebox.os.ui

import android.graphics.BitmapFactory
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
import java.net.URL

@Composable
internal fun RemoteArtwork(url: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = withContext(Dispatchers.IO) {
            if (url.isNullOrBlank() || !url.startsWith("https://")) return@withContext null
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 6_000
                    instanceFollowRedirects = false
                    setRequestProperty("Accept", "image/*")
                }
                connection.use {
                    if (it.responseCode !in 200..299 || it.contentLengthLong > 2L * 1024L * 1024L) return@runCatching null
                    it.inputStream.use { stream -> BitmapFactory.decodeStream(stream) }
                }
            }.getOrNull()
        }
    }
    if (bitmap == null) {
        androidx.compose.foundation.layout.Box(modifier.background(Color.Transparent))
    } else {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.alpha(0.34f)
        )
    }
}
