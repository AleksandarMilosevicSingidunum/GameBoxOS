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

val GameBoxBlue = Color(0xFF5184FF)
val GameBoxElectricBlue = Color(0xFF326BFF)
val GameBoxPurple = Color(0xFF8C38F5)
val GameBoxCyan = Color(0xFF39BFF7)
val GameBoxBackground = Color(0xFF03070D)
val GameBoxSurface = Color(0xFF0B111B)
val GameBoxSurfaceRaised = Color(0xFF131C2A)
val GameBoxOutline = Color(0xFF2B374B)

private val colors = darkColorScheme(
    primary = GameBoxBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF173170),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = GameBoxPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF252348),
    onSecondaryContainer = Color(0xFFEBDDFF),
    tertiary = GameBoxCyan,
    onTertiary = Color(0xFF001A26),
    tertiaryContainer = Color(0xFF123447),
    onTertiaryContainer = Color(0xFFD1F0FF),
    background = GameBoxBackground,
    surface = GameBoxSurface,
    surfaceVariant = GameBoxSurfaceRaised,
    // Material's purple container defaults otherwise bleed into menus and controls.
    surfaceTint = Color.Transparent,
    surfaceDim = GameBoxBackground,
    surfaceBright = Color(0xFF243044),
    surfaceContainerLowest = Color(0xFF050A12),
    surfaceContainerLow = GameBoxSurface,
    surfaceContainer = Color(0xFF101824),
    surfaceContainerHigh = GameBoxSurfaceRaised,
    surfaceContainerHighest = Color(0xFF1C2738),
    onSurfaceVariant = Color(0xFFACB9CE),
    outline = GameBoxOutline,
    outlineVariant = Color(0xFF17243B),
    onBackground = Color(0xFFF4F7FF),
    onSurface = Color(0xFFF4F7FF),
    error = Color(0xFFFF6B81),
    onError = Color(0xFF330410),
    errorContainer = Color(0xFF471B29),
    onErrorContainer = Color(0xFFFFDCE3),
    inverseSurface = Color(0xFFE2E9F5),
    inverseOnSurface = Color(0xFF111925),
    inversePrimary = Color(0xFF214DB4),
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
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
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
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
    ),
)

private val shapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
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
