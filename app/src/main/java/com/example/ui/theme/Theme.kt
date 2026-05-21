package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SophisticatedColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    secondary = SophisticatedSecondary,
    tertiary = SophisticatedTertiary,
    background = SophisticatedBg,
    surface = SophisticatedSurface,
    surfaceVariant = SophisticatedAltSurface,
    onBackground = SophisticatedOnSurface,
    onSurface = SophisticatedOnSurface,
    onSurfaceVariant = SophisticatedOnSurfaceVariant,
    outline = SophisticatedBorder,
    error = SophisticatedTertiary
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode for Sophisticated Dark experience
  dynamicColor: Boolean = false, // Disable dynamic colors to prevent overriding customized theme
  content: @Composable () -> Unit,
) {
  val colorScheme = SophisticatedColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
