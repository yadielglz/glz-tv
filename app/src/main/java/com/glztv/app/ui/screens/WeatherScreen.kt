package com.glztv.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.glztv.app.ui.components.tvFocusableWithPhysics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.model.ForecastDay
import com.glztv.app.model.WeatherInfo
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
            .background(Color.Transparent)
            .padding(14.dp)
    ) {
        val narrow = maxWidth < 700.dp
        val shortTv = maxHeight < 500.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Weather",
                        fontSize = if (shortTv) 28.sp else 36.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                blurRadius = 6f
                            )
                        )
                    )
                    Text(
                        weather?.location ?: location,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.95f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = onRefresh,
                    enabled = !loading,
                    modifier = Modifier.tvFocusableWithPhysics(
                        shape = RoundedCornerShape(20.dp),
                        focusedScale = 1.08f
                    )
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Text(if (loading) "Updating…" else "Refresh", Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                }
            }
            if (loading && weather == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(.45f), color = MaterialTheme.colorScheme.primary)
                }
            } else if (weather == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Weather unavailable", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            error ?: "Check the configured weather location.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (narrow) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CurrentWeatherCard(weather, Modifier.weight(.8f).fillMaxHeight())
                        ForecastRow(weather.forecast, Modifier.weight(1.2f).fillMaxHeight(), stacked = true)
                    }
                } else {
                    CurrentWeatherCard(
                        weather,
                        Modifier.fillMaxWidth().weight(if (shortTv) .72f else .9f)
                    )
                    ForecastRow(
                        weather.forecast,
                        Modifier.fillMaxWidth().weight(if (shortTv) 1.28f else 1.1f),
                        stacked = false,
                        compact = shortTv
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentWeatherCard(weather: WeatherInfo, modifier: Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        ),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                )
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(weatherSymbol(weather.weatherCode), fontSize = 76.sp)
                Column(Modifier.padding(start = 24.dp)) {
                    Text(
                        "${weather.temperature}°",
                        fontSize = 66.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        weatherDescription(weather.weatherCode),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "CURRENT CONDITIONS",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastRow(
    days: List<ForecastDay>,
    modifier: Modifier,
    stacked: Boolean,
    compact: Boolean = false
) {
    if (days.isEmpty()) return
    if (stacked) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            days.forEachIndexed { index, day ->
                ForecastCard(day, index, Modifier.weight(1f), compact = true)
            }
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            days.forEachIndexed { index, day ->
                ForecastCard(day, index, Modifier.weight(1f).fillMaxHeight(), compact)
            }
        }
    }
}

@Composable
private fun ForecastCard(
    day: ForecastDay,
    index: Int,
    modifier: Modifier,
    compact: Boolean
) {
    Surface(
        modifier,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp
    ) {
        Column(
            Modifier.fillMaxSize().padding(if (compact) 10.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (index == 0) "TODAY" else dayLabel(day.date),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(weatherSymbol(day.weatherCode), fontSize = if (compact) 34.sp else 44.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${day.high}°  /  ${day.low}°",
                fontSize = if (compact) 19.sp else 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                weatherDescription(day.weatherCode),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "☂ ${day.precipitationChance}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private fun dayLabel(value: String): String = runCatching {
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val output = SimpleDateFormat("EEEE", Locale.getDefault())
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
    0 -> "Clear sky"
    1 -> "Mostly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    in 51..57 -> "Drizzle"
    in 61..67, in 80..82 -> "Rain"
    in 71..77, 85, 86 -> "Snow"
    in 95..99 -> "Thunderstorms"
    else -> "Mixed conditions"
}
