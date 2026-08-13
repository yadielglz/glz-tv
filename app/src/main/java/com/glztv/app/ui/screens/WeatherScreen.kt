package com.glztv.app.ui.screens

import androidx.compose.foundation.background
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
        modifier.background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = .6f),
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.background
                )
            )
        ).padding(18.dp)
    ) {
        val compact = maxWidth < 700.dp || maxHeight < 450.dp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Weather", fontSize = if (compact) 28.sp else 38.sp,
                        fontWeight = FontWeight.Black)
                    Text(
                        weather?.location ?: location,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
                Button(onClick = onRefresh, enabled = !loading) {
                    Icon(Icons.Default.Refresh, null)
                    Text(if (loading) "Updating…" else "Refresh", Modifier.padding(start = 8.dp))
                }
            }
            if (loading && weather == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(.45f))
                }
            } else if (weather == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Weather unavailable", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(error ?: "Check the configured weather location.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                if (compact) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CurrentWeatherCard(weather, Modifier.weight(.8f).fillMaxHeight())
                        ForecastRow(weather.forecast, Modifier.weight(1.2f).fillMaxHeight(), stacked = true)
                    }
                } else {
                    CurrentWeatherCard(weather, Modifier.fillMaxWidth().weight(.9f))
                    ForecastRow(weather.forecast, Modifier.fillMaxWidth().weight(1.1f), stacked = false)
                }
            }
        }
    }
}

@Composable
private fun CurrentWeatherCard(weather: WeatherInfo, modifier: Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(weatherSymbol(weather.weatherCode), fontSize = 72.sp)
            Column(Modifier.padding(start = 24.dp)) {
                Text("${weather.temperature}°", fontSize = 64.sp, fontWeight = FontWeight.Black)
                Text(weatherDescription(weather.weatherCode), fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("CURRENT CONDITIONS", color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ForecastRow(days: List<ForecastDay>, modifier: Modifier, stacked: Boolean) {
    if (days.isEmpty()) return
    if (stacked) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEachIndexed { index, day -> ForecastCard(day, index, Modifier.weight(1f)) }
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            days.forEachIndexed { index, day ->
                ForecastCard(day, index, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ForecastCard(day: ForecastDay, index: Int, modifier: Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (index == 0) "TODAY" else dayLabel(day.date),
                fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(weatherSymbol(day.weatherCode), fontSize = 42.sp)
            Text("${day.high}°  /  ${day.low}°", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(weatherDescription(day.weatherCode), maxLines = 1,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("☂ ${day.precipitationChance}%", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
