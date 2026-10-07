package com.dzaky.anitrack.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dzaky.anitrack.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFFAE4034),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD3),
    onPrimaryContainer = Color(0xFF542017),
    secondary = Color(0xFF645D54),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECE4D9),
    onSecondaryContainer = Color(0xFF28221C),
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF222122),
    surface = Color(0xFFFAF8F5),
    onSurface = Color(0xFF222122),
    surfaceVariant = Color(0xFFECE7E1),
    onSurfaceVariant = Color(0xFF69635F),
    surfaceContainer = Color(0xFFF2EEE9),
    surfaceContainerLow = Color(0xFFF6F3EF),
    surfaceContainerHigh = Color(0xFFECE7E1),
    outline = Color(0xFF817872),
    outlineVariant = Color(0xFFD7D0C8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF38C7C),
    onPrimary = Color(0xFF511E15),
    primaryContainer = Color(0xFF793228),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFD0C3B6),
    onSecondary = Color(0xFF373029),
    secondaryContainer = Color(0xFF484038),
    onSecondaryContainer = Color(0xFFF0E3D6),
    background = Color(0xFF141415),
    onBackground = Color(0xFFEDE7E2),
    surface = Color(0xFF141415),
    onSurface = Color(0xFFEDE7E2),
    surfaceVariant = Color(0xFF2B2827),
    onSurfaceVariant = Color(0xFFBDB4AE),
    surfaceContainer = Color(0xFF211F1F),
    surfaceContainerLow = Color(0xFF1B1A1B),
    surfaceContainerHigh = Color(0xFF2B2827),
    outline = Color(0xFF938880),
    outlineVariant = Color(0xFF403A36),
)

private val AniTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun AniTrackTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AniTypography, content = content)
}
