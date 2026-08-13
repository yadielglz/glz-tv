package com.glztv.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    primary = Color(0xFFFFB690), onPrimary = Color(0xFF552006),
    primaryContainer = Color(0xFF7B3416), secondary = Color(0xFFC4FF4D),
    onSecondary = Color(0xFF263500), background = Color(0xFF07101D),
    surface = Color(0xFF0E1B2C), surfaceVariant = Color(0xFF1A2A3E),
    onSurface = Color(0xFFEFF5FB), onSurfaceVariant = Color(0xFFB9C8DA)
)
private val defaultLight = lightColorScheme(
    primary = Color(0xFF9A3E0A), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCA), secondary = Color(0xFF4D6700),
    onSecondary = Color.White, background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFFBFF), surfaceVariant = Color(0xFFF3DED4),
    onSurface = Color(0xFF241A16), onSurfaceVariant = Color(0xFF55443C)
)
private val ocean = darkColorScheme(
    primary = Color(0xFF65D8FF), onPrimary = Color(0xFF003545), primaryContainer = Color(0xFF004D63),
    secondary = Color(0xFF72F1C8), onSecondary = Color(0xFF00382B), background = Color(0xFF03151D),
    surface = Color(0xFF09232D), surfaceVariant = Color(0xFF123642), onSurface = Color(0xFFE8F8FC),
    onSurfaceVariant = Color(0xFFB7D3DC)
)
private val sunset = darkColorScheme(
    primary = Color(0xFFFFB06B), onPrimary = Color(0xFF4D2500), primaryContainer = Color(0xFF713B12),
    secondary = Color(0xFFFF7BA9), onSecondary = Color(0xFF56102C), background = Color(0xFF1A0D19),
    surface = Color(0xFF2A1727), surfaceVariant = Color(0xFF43243A), onSurface = Color(0xFFFFF0F5),
    onSurfaceVariant = Color(0xFFE3C2D1)
)
private val emerald = darkColorScheme(
    primary = Color(0xFF50E3C2), onPrimary = Color(0xFF00382B), primaryContainer = Color(0xFF005240),
    secondary = Color(0xFFA8FF78), onSecondary = Color(0xFF1E3800), background = Color(0xFF041A14),
    surface = Color(0xFF0A2920), surfaceVariant = Color(0xFF12382C), onSurface = Color(0xFFE6FAF5),
    onSurfaceVariant = Color(0xFFA3D6C9)
)
private val cyberpunk = darkColorScheme(
    primary = Color(0xFFFF007F), onPrimary = Color(0xFF4A0022), primaryContainer = Color(0xFF7A003D),
    secondary = Color(0xFF00F0FF), onSecondary = Color(0xFF00363D), background = Color(0xFF0D021A),
    surface = Color(0xFF190632), surfaceVariant = Color(0xFF280C4B), onSurface = Color(0xFFFDE8FF),
    onSurfaceVariant = Color(0xFFD4B3E6)
)
private val midnight = darkColorScheme(
    primary = Color(0xFFFFD700), onPrimary = Color(0xFF423700), primaryContainer = Color(0xFF6B5800),
    secondary = Color(0xFFFF9100), onSecondary = Color(0xFF472400), background = Color.Black,
    surface = Color(0xFF0D0D0D), surfaceVariant = Color(0xFF181818), onSurface = Color(0xFFF5F5F5),
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
