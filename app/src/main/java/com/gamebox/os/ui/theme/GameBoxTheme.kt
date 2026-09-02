package com.gamebox.os.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val GameBoxBlue = Color(0xFF5B8CFF)
val GameBoxElectricBlue = Color(0xFF2F80FF)
val GameBoxPurple = Color(0xFF8B5CF6)
val GameBoxCyan = Color(0xFF22D3EE)
val GameBoxBackground = Color(0xFF05070F)
val GameBoxSurface = Color(0xFF101827)
val GameBoxSurfaceRaised = Color(0xFF18233A)
val GameBoxOutline = Color(0xFF34415C)

private val colors = darkColorScheme(
    primary = GameBoxBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A285A),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = GameBoxPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A174E),
    onSecondaryContainer = Color(0xFFEBDDFF),
    tertiary = GameBoxCyan,
    background = GameBoxBackground,
    surface = GameBoxSurface,
    surfaceVariant = GameBoxSurfaceRaised,
    onSurfaceVariant = Color(0xFFB4C0D8),
    outline = GameBoxOutline,
    outlineVariant = Color(0xFF17243B),
    onBackground = Color(0xFFF4F7FF),
    onSurface = Color(0xFFF4F7FF),
    error = Color(0xFFFF6B81),
    scrim = Color(0xFF01030A),
)

private val typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
)

private val shapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
)

@Composable
fun GameBoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes) {
        // Android's platform default content color can be black; explicitly seed every
        // screen with the dark-surface foreground so plain Text remains readable.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground) {
            content()
        }
    }
}
