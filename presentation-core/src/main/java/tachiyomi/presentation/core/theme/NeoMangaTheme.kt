package tachiyomi.presentation.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = NeoColors.NeoViolet,
    onPrimary = NeoColors.OnSurfaceLight,
    primaryContainer = NeoColors.NeoVioletLight,
    secondary = NeoColors.CyberTeal,
    tertiary = NeoColors.AccentPink,
    background = NeoColors.BackgroundLight,
    surface = NeoColors.SurfaceLight,
    surfaceVariant = NeoColors.SurfaceVariantLight,
    onSurface = NeoColors.OnSurfaceLight,
    onBackground = NeoColors.OnSurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = NeoColors.NeoViolet,
    onPrimary = NeoColors.OnSurfaceDark,
    primaryContainer = NeoColors.NeoVioletDark,
    secondary = NeoColors.CyberTeal,
    tertiary = NeoColors.AccentPink,
    background = NeoColors.BackgroundDark,
    surface = NeoColors.SurfaceDark,
    surfaceVariant = NeoColors.SurfaceVariantDark,
    onSurface = NeoColors.OnSurfaceDark,
    onBackground = NeoColors.OnSurfaceDark
)

private val OledColorScheme = darkColorScheme(
    primary = NeoColors.NeoViolet,
    onPrimary = NeoColors.OnSurfaceOled,
    primaryContainer = NeoColors.NeoVioletDark,
    secondary = NeoColors.CyberTeal,
    tertiary = NeoColors.AccentPink,
    background = NeoColors.BackgroundOled,
    surface = NeoColors.SurfaceOled,
    surfaceVariant = NeoColors.SurfaceVariantOled,
    onSurface = NeoColors.OnSurfaceOled,
    onBackground = NeoColors.OnSurfaceOled
)

@Composable
fun NeoMangaTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    isOled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isDark && isOled -> OledColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NeoTypography.Typography,
        shapes = NeoShapes.Shapes,
        content = content
    )
}
