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

private val DarkColorScheme =
  darkColorScheme(
      primary = TerminalNeonGreen,
      secondary = TerminalAmber,
      tertiary = TerminalDarkGreen,
      background = TerminalBackground,
      surface = TerminalDarkGray,
      onPrimary = TerminalBackground,
      onSecondary = TerminalBackground,
      onTertiary = TerminalNeonGreen,
      onBackground = TerminalNeonGreen,
      onSurface = TerminalNeonGreen,
      error = TerminalRed
  )

private val LightColorScheme = DarkColorScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark Theme
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Force custom theme
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
