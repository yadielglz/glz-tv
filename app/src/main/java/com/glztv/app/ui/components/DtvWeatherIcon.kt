package com.glztv.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Calendar

/**
 * Returns true if the given timestamp (default: now) falls in local nighttime hours
 * (before 6:00 AM or 7:00 PM and later).
 */
fun isLocalNight(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return hour < 6 || hour >= 19
}

/**
 * Parses an hourly label like "9 PM", "11 PM", "2 AM", "21:00" to determine whether
 * that forecast hour is at night.
 */
fun isNightHourLabel(label: String): Boolean {
    val trimmed = label.trim().uppercase()
    val amPmMatch = Regex("^(\\d{1,2})(?::\\d{2})?\\s*(AM|PM)$").find(trimmed)
    if (amPmMatch != null) {
        val rawH = amPmMatch.groupValues[1].toIntOrNull() ?: return false
        val isPm = amPmMatch.groupValues[2] == "PM"
        val hour24 = when {
            isPm && rawH < 12 -> rawH + 12
            !isPm && rawH == 12 -> 0
            else -> rawH
        }
        return hour24 < 6 || hour24 >= 19
    }
    val h24Match = Regex("^(\\d{1,2}):\\d{2}$").find(trimmed)
    if (h24Match != null) {
        val hour24 = h24Match.groupValues[1].toIntOrNull() ?: return false
        return hour24 < 6 || hour24 >= 19
    }
    return false
}

/**
 * Resolves a crisp Material vector icon and semantic broadcast tint for a WMO weather code
 * with Day/Night awareness (zero Unicode emojis, so never renders as a blank tofu square on TV).
 */
fun resolveDtvWeatherVisual(weatherCode: Int, isNight: Boolean): Pair<ImageVector, Color> = when (weatherCode) {
    0 -> if (isNight) Icons.Default.NightsStay to Color(0xFF93C5FD) else Icons.Default.WbSunny to Color(0xFFF5B724)
    1, 2 -> if (isNight) Icons.Default.NightsStay to Color(0xFFCBD5E1) else Icons.Default.WbCloudy to Color(0xFFFCD34D)
    3 -> Icons.Default.Cloud to Color(0xFF94A3B8)
    45, 48 -> Icons.Default.Cloud to Color(0xFF94A3B8)
    51, 53, 55, 56, 57 -> Icons.Default.Grain to Color(0xFF7DD3FC)
    61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Default.WaterDrop to Color(0xFF38BDF8)
    71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit to Color(0xFFE0F2FE)
    95, 96, 99 -> Icons.Default.Thunderstorm to Color(0xFFFBBF24)
    else -> if (isNight) Icons.Default.NightsStay to Color(0xFF93C5FD) else Icons.Default.WbCloudy to Color(0xFFCBD5E1)
}

@Composable
fun DtvWeatherIcon(
    weatherCode: Int,
    size: Dp = 24.dp,
    isNight: Boolean = isLocalNight(),
    tintOverride: Color? = null,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val (vector, defaultTint) = resolveDtvWeatherVisual(weatherCode, isNight)
    Icon(
        imageVector = vector,
        contentDescription = contentDescription,
        tint = tintOverride ?: defaultTint,
        modifier = modifier.size(size)
    )
}
