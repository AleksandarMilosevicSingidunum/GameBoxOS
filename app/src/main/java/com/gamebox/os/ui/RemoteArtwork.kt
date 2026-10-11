package com.gamebox.os.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gamebox.os.R
import com.gamebox.os.domain.Game
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

private const val MAX_ARTWORK_BYTES = 4 * 1024 * 1024
internal fun normalizeArtworkUrl(value: String?): String? {
    val candidate = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val uri = runCatching { URI(candidate).normalize() }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true) ||
        uri.host.isNullOrBlank() ||
        uri.userInfo != null ||
        uri.fragment != null
    ) return null
    return uri.toASCIIString()
}

internal fun isSafeArtworkUrl(value: String?): Boolean = normalizeArtworkUrl(value) != null

private val artworkCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}

internal fun bundledArtworkResource(fallbackKey: String?): Int? = when (fallbackKey) {
    "Galaxy Patrol" -> R.drawable.galaxy_patrol_cover
    else -> null
}

/** Decoration only: visible title and console semantics are supplied by the parent card. */
@Composable
internal fun RemoteArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    fallbackKey: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    showFallbackMonogram: Boolean = true,
) {
    val cacheDir = LocalContext.current.cacheDir
    val safeUrl = normalizeArtworkUrl(url)
    val bundledArtwork = remember(fallbackKey) { bundledArtworkResource(fallbackKey) }
    var bitmap by remember(safeUrl) { mutableStateOf(safeUrl?.let(artworkCache::get)) }
    val artworkAlpha by animateFloatAsState(
        targetValue = if (bitmap == null) 0f else 1f,
        animationSpec = if (LocalReducedMotion.current) snap() else tween(220),
        label = "remote-artwork-fade",
    )
    LaunchedEffect(safeUrl) {
        // Real provider art fades over a title-aware fallback. Never present
        // decorative starter illustrations as if they were authentic box art.
        if (bitmap != null || safeUrl == null) return@LaunchedEffect
        for (waitMillis in artworkRetryDelaysMillis()) {
            if (waitMillis > 0L) delay(waitMillis)
            val loaded = withContext(Dispatchers.IO) {
                try { loadArtwork(safeUrl, cacheDir) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            }
            if (loaded != null) {
                bitmap = loaded
                break
            }
        }
    }
    Box(modifier) {
        if (bundledArtwork != null) {
            Image(
                painterResource(bundledArtwork),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ArtworkPlaceholder(
                fallbackKey ?: url.orEmpty(),
                Modifier.fillMaxSize(),
                showMonogram = showFallbackMonogram,
            )
        }
        bitmap?.let { loaded ->
            Image(
                loaded.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = artworkAlpha },
            )
        }
    }
}

internal fun artworkRetryDelaysMillis(maxAttempts: Int = 3): List<Long> =
    List(maxAttempts.coerceAtLeast(0)) { attempt ->
        when (attempt) {
            0 -> 0L
            1 -> 1_500L
            else -> 4_000L
        }
    }

private val starterArtworkIdentityByGameId = mapOf(
    "cave-story" to "Cave Story",
    "celeste" to "Celeste Classic",
    "freedoom" to "Freedoom",
    "galaxy-patrol" to "Galaxy Patrol",
    "luanti" to "Luanti",
    "openarena" to "OpenArena",
    "openmw" to "OpenMW",
    "supertuxkart" to "SuperTuxKart",
)

/** Keep starter-library art stable even when a stored/display title is stale or edited. */
internal fun gameArtworkFallbackTitle(game: Game): String =
    starterArtworkIdentityByGameId[game.id.value]
        ?: game.providerTitle.takeIf(String::isNotBlank)
        ?: game.title

internal fun artworkPlaceholderMonogram(title: String): String {
    val compactLabel = title.trim()
    if (compactLabel.length in 2..4 && compactLabel.all { it.isUpperCase() || it.isDigit() }) {
        return compactLabel
    }
    val words = Regex("[\\p{L}\\p{N}]+").findAll(title)
        .map { it.value }
        .take(2)
        .toList()
    return when (words.size) {
        0 -> "GB"
        1 -> words.single().take(2).uppercase()
        else -> words.joinToString("") { it.take(1).uppercase() }
    }
}

/** Resolve a redirect without allowing artwork URLs to downgrade from HTTPS. */
internal fun nextArtworkRedirect(currentUrl: String, location: String?): String? {
    if (location.isNullOrBlank()) return null
    val current = runCatching { URI(currentUrl) }.getOrNull() ?: return null
    val next = runCatching { current.resolve(location).normalize().toString() }.getOrNull()
        ?: return null
    return normalizeArtworkUrl(next)
}

private fun loadArtwork(url: String, cacheDir: File): Bitmap? {
    artworkCache.get(url)?.let { return it }
    val directory = File(cacheDir, "box-art").apply { mkdirs() }
    val name = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
    val cached = File(directory, name)
    var bytes = cached.takeIf { it.isFile && it.length() in 1..MAX_ARTWORK_BYTES.toLong() }?.readBytes()
    if (bytes == null) bytes = downloadArtwork(url)
    if (bytes == null) return null
    val data = bytes
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) { cached.delete(); return null }
    val options = BitmapFactory.Options().apply {
        inSampleSize = 1
        while (bounds.outWidth / inSampleSize > 1600 || bounds.outHeight / inSampleSize > 1600) inSampleSize *= 2
    }
    val result = BitmapFactory.decodeByteArray(data, 0, data.size, options) ?: run {
        cached.delete()
        return null
    }
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

private fun downloadArtwork(initialUrl: String): ByteArray? {
    var currentUrl = normalizeArtworkUrl(initialUrl) ?: return null
    repeat(MAX_ARTWORK_REDIRECTS + 1) { redirectCount ->
        val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 8_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "image/*")
        }
        try {
            when (connection.responseCode) {
                in 200..299 -> {
                    if (connection.contentLengthLong > MAX_ARTWORK_BYTES) return null
                    val contentType = connection.contentType
                    if (contentType != null && !contentType.startsWith("image/", ignoreCase = true)) return null
                    return connection.inputStream.use { stream ->
                        readBoundedArtworkResponse(stream, connection.contentLengthLong, MAX_ARTWORK_BYTES)
                    }
                }
                HTTP_MOVED_PERM, HTTP_MOVED_TEMP, HTTP_SEE_OTHER, HTTP_TEMP_REDIRECT, HTTP_PERM_REDIRECT -> {
                    if (redirectCount == MAX_ARTWORK_REDIRECTS) return null
                    currentUrl = nextArtworkRedirect(currentUrl, connection.getHeaderField("Location")) ?: return null
                }
                else -> return null
            }
        } finally {
            connection.disconnect()
        }
    }
    return null
}

private const val MAX_ARTWORK_REDIRECTS = 4
private const val HTTP_MOVED_PERM = 301
private const val HTTP_MOVED_TEMP = 302
private const val HTTP_SEE_OTHER = 303
private const val HTTP_TEMP_REDIRECT = 307
private const val HTTP_PERM_REDIRECT = 308

internal fun readBoundedArtworkResponse(
    input: java.io.InputStream,
    declaredLength: Long,
    maxBytes: Int,
): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        require(total <= maxBytes) { "Artwork response is too large" }
        output.write(buffer, 0, count)
    }
    if (declaredLength >= 0L && total.toLong() != declaredLength) {
        throw java.io.IOException(
            "Artwork response ended with an unexpected length ($total/$declaredLength)",
        )
    }
    return output.toByteArray()
}

@Composable
private fun ArtworkPlaceholder(key: String, modifier: Modifier, showMonogram: Boolean) {
    val palette = listOf(Color(0xFF22549B), Color(0xFF573E91), Color(0xFF176C75), Color(0xFF913E6F), Color(0xFF345F66))
    val accent = palette[(key.hashCode() and Int.MAX_VALUE) % palette.size]
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent, Color(0xFF0B1526))))) {
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
        if (showMonogram) {
            Text(
                text = artworkPlaceholderMonogram(key),
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = .9f),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
            )
        }
    }
}
