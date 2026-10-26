package com.iptvplayer.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Cyan400,
    onPrimary = Navy900,
    primaryContainer = Navy800,
    onPrimaryContainer = Cyan200,
    secondary = Gold400,
    onSecondary = Navy900,
    background = Navy950,
    onBackground = White,
    surface = Navy800,
    onSurface = White,
    surfaceVariant = Navy700,
    onSurfaceVariant = Gray300,
    error = Red400,
    onError = White,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun IptvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = IptvTypography,
        content = content,
    )
}
