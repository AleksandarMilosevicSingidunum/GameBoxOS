package com.gamebox.os.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** Original GameBox mark used by the blueprint shell. */
@Composable
internal fun GameBoxBrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = min(w, h) * 0.075f
        val shell = Path().apply {
            moveTo(w * 0.50f, h * 0.06f)
            lineTo(w * 0.88f, h * 0.27f)
            lineTo(w * 0.88f, h * 0.72f)
            lineTo(w * 0.50f, h * 0.94f)
            lineTo(w * 0.12f, h * 0.72f)
            lineTo(w * 0.12f, h * 0.27f)
            close()
        }
        drawPath(
            path = shell,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF17D9FF), Color(0xFF6B7BFF), Color(0xFFB443FF)),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            ),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val core = Path().apply {
            moveTo(w * 0.67f, h * 0.31f)
            lineTo(w * 0.47f, h * 0.21f)
            lineTo(w * 0.28f, h * 0.32f)
            lineTo(w * 0.28f, h * 0.67f)
            lineTo(w * 0.49f, h * 0.79f)
            lineTo(w * 0.70f, h * 0.67f)
            lineTo(w * 0.70f, h * 0.51f)
            lineTo(w * 0.51f, h * 0.51f)
        }
        drawPath(
            path = core,
            brush = Brush.linearGradient(
                listOf(Color(0xFF4CC9FF), Color(0xFF9A54FF)),
                start = Offset(w * 0.2f, h * 0.2f),
                end = Offset(w * 0.8f, h * 0.8f),
            ),
            style = Stroke(width = stroke * 0.78f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

internal fun consoleBrandColor(key: String): Color = when (key.lowercase()) {
    "ps2", "psp", "playstation", "sonyplaystation2", "sonyplaystationportable" -> Color(0xFF4C7DFF)
    "gamecube", "nintendogamecube" -> Color(0xFF8B63FF)
    "wii", "nintendowii" -> Color(0xFF25BCEB)
    "dreamcast", "segadreamcast" -> Color(0xFFFF7B39)
    "3ds", "nintendo3ds" -> Color(0xFFFF4C5E)
    "switch", "nintendoswitch" -> Color(0xFFFF4057)
    "homebrew" -> Color(0xFFBD63FF)
    "android" -> Color(0xFF45D483)
    "retro" -> Color(0xFFFFC348)
    else -> Color(0xFF7EA0FF)
}

@Composable
internal fun ConsoleBrandMark(
    key: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val normalized = key.lowercase().replace(" ", "")
    val accent = consoleBrandColor(normalized).copy(alpha = if (selected) 1f else 0.82f)
    Box(modifier, contentAlignment = Alignment.Center) {
        when (normalized) {
            "gamecube", "nintendogamecube" -> Canvas(Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.10f
                val outer = Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.05f)
                    lineTo(size.width * 0.93f, size.height * 0.29f)
                    lineTo(size.width * 0.93f, size.height * 0.73f)
                    lineTo(size.width * 0.50f, size.height * 0.96f)
                    lineTo(size.width * 0.07f, size.height * 0.73f)
                    lineTo(size.width * 0.07f, size.height * 0.29f)
                    close()
                }
                drawPath(outer, accent, style = Stroke(stroke, join = StrokeJoin.Round))
                drawRect(
                    accent,
                    topLeft = Offset(size.width * 0.38f, size.height * 0.38f),
                    size = Size(size.width * 0.34f, size.height * 0.34f),
                    style = Stroke(stroke * 0.75f),
                )
            }
            "switch", "nintendoswitch" -> Canvas(Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.09f
                drawRoundRect(
                    accent,
                    topLeft = Offset(size.width * 0.08f, size.height * 0.06f),
                    size = Size(size.width * 0.37f, size.height * 0.88f),
                    cornerRadius = CornerRadius(size.width * 0.16f),
                    style = Stroke(stroke),
                )
                drawRoundRect(
                    accent,
                    topLeft = Offset(size.width * 0.55f, size.height * 0.06f),
                    size = Size(size.width * 0.37f, size.height * 0.88f),
                    cornerRadius = CornerRadius(size.width * 0.16f),
                    style = Stroke(stroke),
                )
                drawCircle(accent, size.minDimension * 0.085f, Offset(size.width * 0.28f, size.height * 0.36f))
                drawCircle(accent, size.minDimension * 0.085f, Offset(size.width * 0.72f, size.height * 0.64f))
            }
            "dreamcast", "segadreamcast" -> Canvas(Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.085f
                repeat(4) { index ->
                    val inset = size.minDimension * (0.09f + index * 0.09f)
                    drawArc(
                        color = accent,
                        startAngle = -35f + index * 42f,
                        sweepAngle = 235f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            "ps2", "sonyplaystation2" -> BrandWordmark("PS2", accent, 7.sp)
            "psp", "sonyplaystationportable" -> BrandWordmark("PSP", accent, 6.sp)
            "wii", "nintendowii" -> BrandWordmark("Wii", accent, 7.sp)
            "3ds", "nintendo3ds" -> BrandWordmark("3D", accent, 7.sp)
            "homebrew" -> BrandWordmark("{ }", accent, 7.sp)
            "android" -> BrandWordmark("A", accent, 8.sp)
            "retro" -> BrandWordmark("8B", accent, 7.sp)
            else -> BrandWordmark("GB", accent, 7.sp)
        }
    }
}

@Composable
private fun BrandWordmark(label: String, color: Color, size: androidx.compose.ui.unit.TextUnit) {
    Text(
        label,
        color = color,
        fontSize = size,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        maxLines = 1,
    )
}

/** Full-color, code-native fallback marks for the Media and PC launchers. */
@Composable
internal fun AppBrandMark(title: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        when (title) {
            "YouTube" -> Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(Color(0xFFFF1D1D), cornerRadius = CornerRadius(size.minDimension * 0.25f))
                val play = Path().apply {
                    moveTo(size.width * 0.42f, size.height * 0.29f)
                    lineTo(size.width * 0.42f, size.height * 0.71f)
                    lineTo(size.width * 0.72f, size.height * 0.50f)
                    close()
                }
                drawPath(play, Color.White)
            }
            "Netflix" -> Text("N", color = Color(0xFFE50914), fontSize = 27.sp, fontWeight = FontWeight.Black)
            "Kodi" -> Canvas(Modifier.fillMaxSize()) {
                fun diamond(cx: Float, cy: Float, radius: Float, color: Color) {
                    val path = Path().apply {
                        moveTo(cx, cy - radius); lineTo(cx + radius, cy); lineTo(cx, cy + radius); lineTo(cx - radius, cy); close()
                    }
                    drawPath(path, color)
                }
                diamond(size.width * 0.50f, size.height * 0.22f, size.minDimension * 0.20f, Color(0xFF24B7F0))
                diamond(size.width * 0.25f, size.height * 0.51f, size.minDimension * 0.18f, Color(0xFF1675B9))
                diamond(size.width * 0.52f, size.height * 0.52f, size.minDimension * 0.22f, Color(0xFF31C8F5))
                diamond(size.width * 0.76f, size.height * 0.52f, size.minDimension * 0.18f, Color(0xFF0B83C6))
            }
            "Jellyfin" -> Canvas(Modifier.fillMaxSize()) {
                val outer = Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.08f)
                    lineTo(size.width * 0.91f, size.height * 0.85f)
                    lineTo(size.width * 0.09f, size.height * 0.85f)
                    close()
                }
                val inner = Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.34f)
                    lineTo(size.width * 0.70f, size.height * 0.72f)
                    lineTo(size.width * 0.30f, size.height * 0.72f)
                    close()
                }
                drawPath(outer, Brush.verticalGradient(listOf(Color(0xFF9A62FF), Color(0xFF5D37CE))))
                drawPath(inner, Color(0xFF12162A))
            }
            "Plex" -> Text("›", color = Color(0xFFE5A11A), fontSize = 34.sp, fontWeight = FontWeight.Black)
            "Spotify" -> Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFF1ED760))
                repeat(3) { index ->
                    val inset = size.minDimension * (0.20f + index * 0.09f)
                    drawArc(
                        color = Color(0xFF07140C), startAngle = 210f, sweepAngle = 118f, useCenter = false,
                        topLeft = Offset(inset, inset + index * size.minDimension * 0.02f),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        style = Stroke(size.minDimension * 0.07f, cap = StrokeCap.Round),
                    )
                }
            }
            "VLC" -> Canvas(Modifier.fillMaxSize()) {
                val cone = Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.05f)
                    lineTo(size.width * 0.84f, size.height * 0.83f)
                    lineTo(size.width * 0.16f, size.height * 0.83f)
                    close()
                }
                drawPath(cone, Color(0xFFFF8B21))
                drawRect(Color.White, Offset(size.width * 0.29f, size.height * 0.45f), Size(size.width * 0.42f, size.height * 0.10f))
                drawRoundRect(Color(0xFFFFB24E), Offset(size.width * 0.08f, size.height * 0.80f), Size(size.width * 0.84f, size.height * 0.14f), CornerRadius(size.minDimension * 0.08f))
            }
            "Twitch" -> BrandTileText("T", Color.White)
            "Moonlight" -> Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFFF4F6FF), style = Stroke(size.minDimension * 0.08f))
                repeat(8) { index ->
                    rotate(index * 45f) {
                        drawLine(
                            Color(0xFFD9DEFF),
                            Offset(size.width * 0.50f, size.height * 0.12f),
                            Offset(size.width * 0.50f, size.height * 0.43f),
                            strokeWidth = size.minDimension * 0.10f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
            "Winlator" -> Canvas(Modifier.fillMaxSize()) {
                val blue = Color(0xFF21A5F6)
                val gap = size.minDimension * 0.06f
                val cell = (size.minDimension - gap) * 0.42f
                drawRect(blue, Offset(size.width * 0.08f, size.height * 0.08f), Size(cell, cell))
                drawRect(blue, Offset(size.width * 0.52f, size.height * 0.08f), Size(cell, cell))
                drawRect(blue, Offset(size.width * 0.08f, size.height * 0.52f), Size(cell, cell))
                drawRect(blue, Offset(size.width * 0.52f, size.height * 0.52f), Size(cell, cell))
            }
            "Termux" -> BrandTileText(">_", Color(0xFF6EF590))
            "Files" -> Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(Color(0xFFFFC33C), Offset(0f, size.height * 0.22f), Size(size.width, size.height * 0.68f), CornerRadius(size.minDimension * 0.10f))
                drawRoundRect(Color(0xFFFFD768), Offset(size.width * 0.08f, size.height * 0.10f), Size(size.width * 0.42f, size.height * 0.25f), CornerRadius(size.minDimension * 0.08f))
            }
            "Chrome" -> Canvas(Modifier.fillMaxSize()) {
                drawArc(Color(0xFFEA4335), -90f, 120f, true)
                drawArc(Color(0xFFFBBC05), 30f, 120f, true)
                drawArc(Color(0xFF34A853), 150f, 120f, true)
                drawCircle(Color(0xFF4285F4), size.minDimension * 0.25f)
                drawCircle(Color.White, size.minDimension * 0.25f, style = Stroke(size.minDimension * 0.07f))
            }
            "Desktop", "Desktop Mode" -> Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(Color.White, cornerRadius = CornerRadius(size.minDimension * 0.08f), style = Stroke(size.minDimension * 0.07f))
                drawLine(Color.White, Offset(size.width * 0.50f, size.height), Offset(size.width * 0.50f, size.height * 0.73f), size.minDimension * 0.07f)
                drawLine(Color.White, Offset(size.width * 0.28f, size.height * 0.96f), Offset(size.width * 0.72f, size.height * 0.96f), size.minDimension * 0.07f)
            }
            "Steam Library" -> Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White, size.minDimension * 0.47f, style = Stroke(size.minDimension * 0.07f))
                drawCircle(Color.White, size.minDimension * 0.13f, Offset(size.width * 0.67f, size.height * 0.33f), style = Stroke(size.minDimension * 0.07f))
                drawCircle(Color.White, size.minDimension * 0.11f, Offset(size.width * 0.33f, size.height * 0.67f), style = Stroke(size.minDimension * 0.07f))
                drawLine(Color.White, Offset(size.width * 0.42f, size.height * 0.58f), Offset(size.width * 0.58f, size.height * 0.42f), size.minDimension * 0.08f)
            }
            "Xbox" -> Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White, style = Stroke(size.minDimension * 0.07f))
                drawLine(Color.White, Offset(size.width * 0.26f, size.height * 0.22f), Offset(size.width * 0.74f, size.height * 0.78f), size.minDimension * 0.10f, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width * 0.74f, size.height * 0.22f), Offset(size.width * 0.26f, size.height * 0.78f), size.minDimension * 0.10f, StrokeCap.Round)
            }
            "Epic Games" -> BrandTileText("EPIC", Color.White, 7.sp)
            else -> BrandTileText(title.take(2).uppercase(), Color.White, 8.sp)
        }
    }
}

@Composable
private fun BrandTileText(label: String, color: Color, size: androidx.compose.ui.unit.TextUnit = 16.sp) {
    Text(label, color = color, fontSize = size, fontWeight = FontWeight.Black, maxLines = 1)
}

