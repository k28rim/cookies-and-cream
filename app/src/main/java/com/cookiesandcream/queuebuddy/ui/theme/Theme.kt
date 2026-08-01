package com.cookiesandcream.queuebuddy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F7A5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8FF0D3),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF3D6B59),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7E9D6),
    onSecondaryContainer = Color(0xFF06251C),
    tertiary = Color(0xFF50606E),
    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF1B1C1B),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF1B1C1B),
    surfaceVariant = Color(0xFFE3E3E1),
    onSurfaceVariant = Color(0xFF454746),
    surfaceTint = Color(0xFFFCFCFC),
    surfaceBright = Color(0xFFFCFCFC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7F5),
    surfaceContainer = Color(0xFFF1F1EF),
    surfaceContainerHigh = Color(0xFFEBEBE9),
    surfaceContainerHighest = Color(0xFFE5E5E3),
    surfaceDim = Color(0xFFDCDCDA),
    outline = Color(0xFF757874),
    outlineVariant = Color(0xFFC5C7C3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FDDB8),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513D),
    onPrimaryContainer = Color(0xFF8FF0D3),
    secondary = Color(0xFFA8C7B9),
    onSecondary = Color(0xFF14352A),
    secondaryContainer = Color(0xFF20503F),
    onSecondaryContainer = Color(0xFFB7E9D6),
    tertiary = Color(0xFFB7C9DA),
    background = Color(0xFF1A1C1B),
    onBackground = Color(0xFFE2E3DF),
    surface = Color(0xFF1A1C1B),
    onSurface = Color(0xFFE2E3DF),
    surfaceVariant = Color(0xFF444745),
    onSurfaceVariant = Color(0xFFC4C7C3),
    surfaceTint = Color(0xFF1A1C1B),
    surfaceBright = Color(0xFF40403E),
    surfaceContainerLowest = Color(0xFF0E100F),
    surfaceContainerLow = Color(0xFF1A1C1B),
    surfaceContainer = Color(0xFF1E2120),
    surfaceContainerHigh = Color(0xFF292B2A),
    surfaceContainerHighest = Color(0xFF333635),
    surfaceDim = Color(0xFF121413),
    outline = Color(0xFF8E918D)
)

@Composable
fun QueueBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),

    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
