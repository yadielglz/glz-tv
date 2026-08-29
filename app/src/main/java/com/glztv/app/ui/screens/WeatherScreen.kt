package com.glztv.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.model.ForecastDay
import com.glztv.app.model.HourlyForecast
import com.glztv.app.model.WeatherInfo
import com.glztv.app.ui.components.tvFocusableWithPhysics
import com.glztv.app.ui.components.GlzPanel
import com.glztv.app.ui.components.GlzCardDefaults
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WeatherScreen(
    weather: WeatherInfo?,
    location: String,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val compactHeight = maxHeight < 560.dp

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (compactHeight) 10.dp else 14.dp)
        ) {
            // 1. Top Header Row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Weather",
                        fontSize = if (compactHeight) 24.sp else 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        weather?.location ?: location,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onRefresh,
                    enabled = !loading,
                    modifier = Modifier.tvFocusableWithPhysics(
                        shape = RoundedCornerShape(20.dp),
                        focusedScale = 1.08f,
                        glowColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (loading) "Updating…" else "Refresh", fontWeight = FontWeight.Bold)
                }
            }

            if (loading && weather == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(
                        Modifier.width(240.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (weather == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Weather unavailable",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            error ?: "Check the configured weather location in Settings.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 2. Weather Content Layout (Scrollable or 2-column)
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    // Top Hero & Metrics Row
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Current Temperature Hero Banner
                            WeatherHeroCard(
                                weather = weather,
                                modifier = Modifier
                                    .weight(1.25f)
                                    .height(if (compactHeight) 190.dp else 225.dp)
                            )

                            // Quick Meteorological Metrics Grid
                            WeatherMetricsPanel(
                                weather = weather,
                                modifier = Modifier
                                    .weight(1.75f)
                                    .height(if (compactHeight) 190.dp else 225.dp)
                            )
                        }
                    }

                    // Hourly Forecast Ribbon (if available)
                    if (weather.hourly.isNotEmpty()) {
                        item {
                            WeatherSectionHeader("HOURLY FORECAST", "Next 18 hours forecast progression")
                            Spacer(Modifier.height(6.dp))
                            HourlyForecastRow(weather.hourly)
                        }
                    }

                    // 5-Day Daily Forecast Section
                    if (weather.forecast.isNotEmpty()) {
                        item {
                            WeatherSectionHeader("5-DAY EXTENDED FORECAST", "Daily outlook & precipitation probability")
                            Spacer(Modifier.height(6.dp))
                            DailyForecastGrid(weather.forecast)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherSectionHeader(title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "•",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun WeatherHeroCard(weather: WeatherInfo, modifier: Modifier = Modifier) {
    val gradientColors = remember(weather.weatherCode) {
        getAtmosphericGradient(weather.weatherCode)
    }
    val todayForecast = weather.forecast.firstOrNull()

    GlzPanel(
        modifier = modifier,
        shape = RoundedCornerShape(GlzCardDefaults.RadiusLarge)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "${weather.temperature}°",
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 60.sp
                        )
                        Text(
                            weatherDescription(weather.weatherCode),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                    Text(
                        weatherSymbol(weather.weatherCode),
                        fontSize = 56.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Text(
                            "Feels like ${weather.feelsLike}°",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (todayForecast != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                        ) {
                            Text(
                                "H: ${todayForecast.high}°  L: ${todayForecast.low}°",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricsPanel(weather: WeatherInfo, modifier: Modifier = Modifier) {
    GlzPanel(
        modifier = modifier,
        shape = RoundedCornerShape(GlzCardDefaults.RadiusLarge)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricItem(
                    title = "HUMIDITY",
                    value = "${weather.humidity}%",
                    subtitle = if (weather.humidity > 65) "High moisture" else "Comfortable",
                    icon = Icons.Default.WaterDrop,
                    accentColor = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    title = "WIND",
                    value = "${weather.windSpeedMph} mph",
                    subtitle = "Direction ${weather.windDirection}",
                    icon = Icons.Default.Air,
                    accentColor = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    title = "UV INDEX",
                    value = String.format(Locale.US, "%.1f", weather.uvIndex),
                    subtitle = uvRiskLabel(weather.uvIndex),
                    icon = Icons.Default.WbSunny,
                    accentColor = Color(0xFFFFB300),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricItem(
                    title = "PRESSURE",
                    value = "${weather.pressureHpa} hPa",
                    subtitle = if (weather.pressureHpa > 1013) "High pressure" else "Low pressure",
                    icon = Icons.Default.Compress,
                    accentColor = Color(0xFFB388FF),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    title = "FEELS LIKE",
                    value = "${weather.feelsLike}°",
                    subtitle = if (weather.feelsLike > weather.temperature) "Warmer than actual" else "Normal",
                    icon = Icons.Default.Thermostat,
                    accentColor = Color(0xFFFF5252),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    title = "PRECIPITATION",
                    value = "${weather.forecast.firstOrNull()?.precipitationChance ?: 0}%",
                    subtitle = "Today's probability",
                    icon = Icons.Default.WaterDrop,
                    accentColor = Color(0xFF40C4FF),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    GlzPanel(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = accentColor
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HourlyForecastRow(hourly: List<HourlyForecast>) {
    LazyRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
    ) {
        items(hourly) { item ->
            GlzPanel(
                shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
                modifier = Modifier
                    .width(88.dp)
                    .height(130.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        item.timeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.timeLabel == "NOW") MaterialTheme.colorScheme.primary else Color.White
                    )
                    Text(
                        weatherSymbol(item.weatherCode),
                        fontSize = 28.sp
                    )
                    Text(
                        "${item.temperature}°",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    if (item.precipitationChance > 0) {
                        Text(
                            "☂ ${item.precipitationChance}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF40C4FF)
                        )
                    } else {
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyForecastGrid(forecast: List<ForecastDay>) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        forecast.forEachIndexed { index, day ->
            GlzPanel(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                shape = RoundedCornerShape(GlzCardDefaults.RadiusMedium)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (index == 0) "TODAY" else dayLabel(day.date),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (index == 0) MaterialTheme.colorScheme.primary else Color.White
                    )
                    Text(
                        weatherSymbol(day.weatherCode),
                        fontSize = 34.sp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${day.high}° / ${day.low}°",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            weatherDescription(day.weatherCode),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (day.precipitationChance > 0) {
                        Text(
                            "☂ ${day.precipitationChance}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    } else {
                        Text(
                            "☂ 0%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private fun getAtmosphericGradient(weatherCode: Int): List<Color> = when (weatherCode) {
    0, 1 -> listOf(
        Color(0xFFFF8F00).copy(alpha = 0.65f),
        Color(0xFFE65100).copy(alpha = 0.40f),
        Color(0xFF101524).copy(alpha = 0.85f)
    )
    2, 3 -> listOf(
        Color(0xFF37474F).copy(alpha = 0.70f),
        Color(0xFF1E2A38).copy(alpha = 0.60f),
        Color(0xFF101524).copy(alpha = 0.90f)
    )
    in 51..67, in 80..82 -> listOf(
        Color(0xFF0277BD).copy(alpha = 0.65f),
        Color(0xFF00363A).copy(alpha = 0.50f),
        Color(0xFF090C15).copy(alpha = 0.90f)
    )
    in 71..77, 85, 86 -> listOf(
        Color(0xFF0097A7).copy(alpha = 0.60f),
        Color(0xFF004D57).copy(alpha = 0.50f),
        Color(0xFF090C15).copy(alpha = 0.90f)
    )
    in 95..99 -> listOf(
        Color(0xFF512DA8).copy(alpha = 0.70f),
        Color(0xFF311B92).copy(alpha = 0.55f),
        Color(0xFF0A0216).copy(alpha = 0.92f)
    )
    else -> listOf(
        Color(0xFF004F55).copy(alpha = 0.65f),
        Color(0xFF1A2136).copy(alpha = 0.65f),
        Color(0xFF101524).copy(alpha = 0.90f)
    )
}

private fun uvRiskLabel(uv: Double): String = when {
    uv < 3.0 -> "Low risk"
    uv < 6.0 -> "Moderate"
    uv < 8.0 -> "High"
    uv < 11.0 -> "Very high"
    else -> "Extreme"
}

private fun dayLabel(value: String): String = runCatching {
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val output = SimpleDateFormat("EEE", Locale.getDefault())
    output.format(requireNotNull(input.parse(value))).uppercase(Locale.getDefault())
}.getOrDefault(value)

private fun weatherSymbol(code: Int): String = when (code) {
    0 -> "☀"
    1, 2 -> "⛅"
    3 -> "☁"
    45, 48 -> "≋"
    in 51..67, in 80..82 -> "☂"
    in 71..77, 85, 86 -> "❄"
    in 95..99 -> "ϟ"
    else -> "°"
}

private fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear Sky"
    1 -> "Mostly Clear"
    2 -> "Partly Cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    in 51..57 -> "Light Drizzle"
    in 61..67, in 80..82 -> "Rain Showers"
    in 71..77, 85, 86 -> "Snowfall"
    in 95..99 -> "Thunderstorms"
    else -> "Mixed Conditions"
}
