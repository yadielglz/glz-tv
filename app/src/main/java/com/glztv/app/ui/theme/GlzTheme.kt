package com.glztv.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glztv.app.R

val GoogleSansFlexFamily = FontFamily(
    Font(R.font.google_sans_flex, FontWeight.Light),
    Font(R.font.google_sans_flex, FontWeight.Normal),
    Font(R.font.google_sans_flex, FontWeight.Medium),
    Font(R.font.google_sans_flex, FontWeight.SemiBold),
    Font(R.font.google_sans_flex, FontWeight.Bold),
    Font(R.font.google_sans_flex, FontWeight.ExtraBold),
    Font(R.font.google_sans_flex, FontWeight.Black)
)

private val typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = GoogleSansFlexFamily),
        displayMedium = displayMedium.copy(fontFamily = GoogleSansFlexFamily),
        displaySmall = displaySmall.copy(fontFamily = GoogleSansFlexFamily),
        headlineLarge = headlineLarge.copy(fontFamily = GoogleSansFlexFamily),
        headlineMedium = headlineMedium.copy(fontFamily = GoogleSansFlexFamily),
        headlineSmall = headlineSmall.copy(fontFamily = GoogleSansFlexFamily),
        titleLarge = titleLarge.copy(fontFamily = GoogleSansFlexFamily),
        titleMedium = titleMedium.copy(fontFamily = GoogleSansFlexFamily),
        titleSmall = titleSmall.copy(fontFamily = GoogleSansFlexFamily),
        bodyLarge = bodyLarge.copy(fontFamily = GoogleSansFlexFamily),
        bodyMedium = bodyMedium.copy(fontFamily = GoogleSansFlexFamily),
        bodySmall = bodySmall.copy(fontFamily = GoogleSansFlexFamily),
        labelLarge = labelLarge.copy(fontFamily = GoogleSansFlexFamily),
        labelMedium = labelMedium.copy(fontFamily = GoogleSansFlexFamily),
        labelSmall = labelSmall.copy(fontFamily = GoogleSansFlexFamily)
    )
}

private val defaultDark = darkColorScheme(
    primary = Color(0xFF00F0FF), onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F55), secondary = Color(0xFF7000FF),
    onSecondary = Color(0xFFFFFFFF), background = Color(0xFF090C15),
    surface = Color(0xFF101524), surfaceVariant = Color(0xFF1A2136),
    onSurface = Color(0xFFF0F4FC), onSurfaceVariant = Color(0xFFA0ACCE)
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
private val arctic = darkColorScheme(
    primary = Color(0xFF38E1FF), onPrimary = Color(0xFF003543), primaryContainer = Color(0xFF004E63),
    secondary = Color(0xFF8A99FF), onSecondary = Color(0xFF101B55), background = Color(0xFF060913),
    surface = Color(0xFF0C1322), surfaceVariant = Color(0xFF162035), onSurface = Color(0xFFF0F5FF),
    onSurfaceVariant = Color(0xFFA5B7D8)
)
private val crimson = darkColorScheme(
    primary = Color(0xFFFF1744), onPrimary = Color(0xFF45000C), primaryContainer = Color(0xFF680016),
    secondary = Color(0xFFFF8A00), onSecondary = Color(0xFF421E00), background = Color(0xFF110305),
    surface = Color(0xFF1C070B), surfaceVariant = Color(0xFF2C0F15), onSurface = Color(0xFFFFF0F1),
    onSurfaceVariant = Color(0xFFE2B0B6)
)
private val amethyst = darkColorScheme(
    primary = Color(0xFFB347FF), onPrimary = Color(0xFF2C004F), primaryContainer = Color(0xFF48007E),
    secondary = Color(0xFFFF33A1), onSecondary = Color(0xFF450024), background = Color(0xFF0C0416),
    surface = Color(0xFF160924), surfaceVariant = Color(0xFF24133A), onSurface = Color(0xFFFBF2FF),
    onSurfaceVariant = Color(0xFFD4BEE6)
)
private val synthwave = darkColorScheme(
    primary = Color(0xFF00F5D4), onPrimary = Color(0xFF00372F), primaryContainer = Color(0xFF005549),
    secondary = Color(0xFFF72585), onSecondary = Color(0xFF450020), background = Color(0xFF0C061C),
    surface = Color(0xFF160E2C), surfaceVariant = Color(0xFF251A44), onSurface = Color(0xFFF5EEFF),
    onSurfaceVariant = Color(0xFFC7B6E2)
)
private val solar = darkColorScheme(
    primary = Color(0xFFFFB703), onPrimary = Color(0xFF402B00), primaryContainer = Color(0xFF664500),
    secondary = Color(0xFFFB8500), onSecondary = Color(0xFF421F00), background = Color(0xFF100904),
    surface = Color(0xFF1D1209), surfaceVariant = Color(0xFF2E1F14), onSurface = Color(0xFFFFF7ED),
    onSurfaceVariant = Color(0xFFDFC6B2)
)
private val stealth = darkColorScheme(
    primary = Color(0xFFE2E8F0), onPrimary = Color(0xFF1E293B), primaryContainer = Color(0xFF334155),
    secondary = Color(0xFF38BDF8), onSecondary = Color(0xFF002B3D), background = Color(0xFF040507),
    surface = Color(0xFF0B0D12), surfaceVariant = Color(0xFF161A22), onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun GlzTheme(mode: String, content: @Composable () -> Unit) {
    // GLZ TV is a dark-only experience: the cinematic gradients, glow focus states, and
    // neon accents are designed for a dark canvas. Legacy "light"/"adaptive"/"dark"
    // preferences all resolve to the default dark palette; the named variants below are
    // all dark schemes as well.
    val colors = when (mode) {
        "ocean" -> ocean
        "sunset" -> sunset
        "emerald" -> emerald
        "cyberpunk" -> cyberpunk
        "midnight" -> midnight
        "arctic" -> arctic
        "crimson" -> crimson
        "amethyst" -> amethyst
        "synthwave" -> synthwave
        "solar" -> solar
        "stealth" -> stealth
        else -> defaultDark
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

