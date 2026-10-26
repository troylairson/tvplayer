package com.tvplayer.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

private val TvColors = darkColorScheme(
    primary = Cream,
    onPrimary = Ink,
    background = Ink,
    onBackground = Cream,
    surface = InkRaised,
    onSurface = Cream,
    surfaceVariant = InkBorder,
    onSurfaceVariant = CreamMuted,
)

// Mobile M3 components (settings text fields) read their own theme — keep them dark too
private val MobileColors = androidx.compose.material3.darkColorScheme(
    primary = Cream,
    onPrimary = Ink,
    background = Ink,
    onBackground = Cream,
    surface = InkRaised,
    onSurface = Cream,
    onSurfaceVariant = CreamMuted,
    outline = CreamMuted,
)

@OptIn(ExperimentalTvMaterial3Api::class)
private val LauncherTypography = Typography(
    displayLarge  = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Medium, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Medium),
    headlineMedium= TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium),
    titleLarge    = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    titleMedium   = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge     = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelLarge    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(colorScheme = MobileColors) {
        MaterialTheme(colorScheme = TvColors, typography = LauncherTypography, content = content)
    }
}
