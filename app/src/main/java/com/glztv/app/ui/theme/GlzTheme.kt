package com.glztv.app.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glztv.app.R

private val typography = Typography().run {
    val family = FontFamily(
        Font(R.font.roboto_flex, FontWeight.Normal),
        Font(R.font.roboto_flex, FontWeight.Medium),
        Font(R.font.roboto_flex, FontWeight.SemiBold),
        Font(R.font.roboto_flex, FontWeight.Bold),
        Font(R.font.roboto_flex, FontWeight.Black)
    )
    copy(
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family),
        headlineLarge = headlineLarge.copy(fontFamily = family),
        headlineMedium = headlineMedium.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(fontFamily = family),
        titleMedium = titleMedium.copy(fontFamily = family),
        titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family)
    )
}

private val defaultDark = darkColorScheme(
    primary = Color(0xFF00F0FF), onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F55), secondary = Color(0xFF7000FF),
    onSecondary = Color(0xFFFFFFFF), background = Color(0xFF090C15),
    surface = Color(0xFF101524), surfaceVariant = Color(0xFF1A2136),
    onSurface = Color(0xFFF0F4FC), onSurfaceVariant = Color(0xFFA0ACCE)
)
private val defaultLight = lightColorScheme(
    primary = Color(0xFF006970), onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1FA), secondary = Color(0xFF6200EE),
    onSecondary = Color.White, background = Color(0xFFF4F7FC),
    surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE1E7F0),
    onSurface = Color(0xFF101524), onSurfaceVariant = Color(0xFF424B60)
)
private val ocean = darkColorScheme(
    primary = Color(0xFF00E5FF), onPrimary = Color(0xFF00363D), primaryContainer = Color(0xFF004D57),
    secondary = Color(0xFF00FFB2), onSecondary = Color(0xFF003827), background = Color(0xFF040D14),
    surface = Color(0xFF0A1926), surfaceVariant = Color(0xFF12283A), onSurface = Color(0xFFE8FAFF),
    onSurfaceVariant = Color(0xFF9EC4D4)
)
private val sunset = darkColorScheme(
    primary = Color(0xFFFF6B35), onPrimary = Color(0xFF4D1700), primaryContainer = Color(0xFF7A2A00),
    secondary = Color(0xFFFF2E93), onSecondary = Color(0xFF500028), background = Color(0xFF120714),
    surface = Color(0xFF200F24), surfaceVariant = Color(0xFF331A3A), onSurface = Color(0xFFFFF0F7),
    onSurfaceVariant = Color(0xFFE3B3D2)
)
private val emerald = darkColorScheme(
    primary = Color(0xFF00FF9D), onPrimary = Color(0xFF003820), primaryContainer = Color(0xFF005230),
    secondary = Color(0xFFCCFF00), onSecondary = Color(0xFF384500), background = Color(0xFF03140E),
    surface = Color(0xFF08241A), surfaceVariant = Color(0xFF10382A), onSurface = Color(0xFFE6FFF5),
    onSurfaceVariant = Color(0xFF9EDBC5)
)
private val cyberpunk = darkColorScheme(
    primary = Color(0xFFFF007F), onPrimary = Color(0xFF4A0022), primaryContainer = Color(0xFF7A003D),
    secondary = Color(0xFF00F0FF), onSecondary = Color(0xFF00363D), background = Color(0xFF0A0216),
    surface = Color(0xFF16062B), surfaceVariant = Color(0xFF260A44), onSurface = Color(0xFFFDE8FF),
    onSurfaceVariant = Color(0xFFD4B3E6)
)
private val midnight = darkColorScheme(
    primary = Color(0xFFFFD700), onPrimary = Color(0xFF423700), primaryContainer = Color(0xFF6B5800),
    secondary = Color(0xFFFF9100), onSecondary = Color(0xFF472400), background = Color(0xFF050508),
    surface = Color(0xFF0E0E14), surfaceVariant = Color(0xFF181824), onSurface = Color(0xFFF5F5FA),
    onSurfaceVariant = Color(0xFFCCCCCC)
)

@Composable
fun GlzTheme(mode: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (mode) {
        "dark", "ocean", "sunset", "emerald", "cyberpunk", "midnight" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = when {
        mode == "ocean" -> ocean
        mode == "sunset" -> sunset
        mode == "emerald" -> emerald
        mode == "cyberpunk" -> cyberpunk
        mode == "midnight" -> midnight
        mode == "adaptive" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> defaultDark
        else -> defaultLight
    }
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}

@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(0.85f, 0.15f),
                    radius = 1800f
                )
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7000FF).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(0.1f, 0.9f),
                    radius = 1400f
                )
            )
    ) {
        content()
    }
}

