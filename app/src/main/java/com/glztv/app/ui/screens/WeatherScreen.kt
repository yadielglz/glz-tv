package com.glztv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.model.ForecastDay
import com.glztv.app.model.HourlyForecast
import com.glztv.app.model.WeatherInfo
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvActionButton
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.DtvSectionHeader
import com.glztv.app.ui.theme.LocalTvViewport
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Non-Scrollable DirecTV Broadcast Weather Channel Screen (v1.48.4.25).
 *
 * - Shared DirecTV HD Top Command Bar (`Weather` tab active).
 * - Glossy Dark Broadcast Hero Banner with borderless inline telemetry (Humidity, Wind, UV, Pressure).
 * - Non-scrollable Hourly Forecast strip + 3-Day Outlook table fitting cleanly on one TV screen.
 */
@Composable
fun WeatherScreen(
    weather: WeatherInfo?,
    location: String,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    topBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val viewport = LocalTvViewport.current
    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(28.dp)

    val inputFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DtvChrome.ScreenBg)
    ) {
        // 1. Shared DirecTV HD Top Command Bar
        topBar?.invoke()

        if (loading && weather == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DtvChrome.ElectricBlue, strokeWidth = 2.5.dp)
            }
        } else if (weather == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = error ?: "Weather data unavailable for ${location.ifBlank { "current location" }}",
                        color = DtvChrome.SecondaryText,
                        fontSize = 14.sp
                    )
                    DtvActionButton(label = "Retry", primary = true, onClick = onRefresh)
                }
            }
        } else {
            // 2. Glossy Dark Broadcast Hero Banner (Borderless Inline Weather Info)
            val today = weather.forecast.firstOrNull()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DtvChrome.BannerGradient)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad, vertical = if (viewport.isCompactHeight) 14.dp else 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Large Broadcast Temp + Condition + Location
                    Row(
                        modifier = Modifier.weight(1.05f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        com.glztv.app.ui.components.DtvWeatherIcon(
                            weatherCode = weather.weatherCode,
                            size = if (viewport.isCompactHeight) 48.dp else 56.dp,
                            contentDescription = weatherLabel(weather.weatherCode)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "LOCAL WEATHER",
                                color = DtvChrome.ElectricBlue,
                                fontSize = 11.sp,
                                fontFamily = BarlowCondensedFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${weather.temperature}°",
                                    color = Color.White,
                                    fontSize = if (viewport.isCompactHeight) 44.sp else 52.sp,
                                    fontWeight = FontWeight.Light
                                )
                                Column(Modifier.padding(bottom = 6.dp)) {
                                    Text(
                                        text = weatherLabel(weather.weatherCode),
                                        color = DtvChrome.PrimaryText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = buildList {
                                            add(weather.location.ifBlank { location })
                                            add("Feels ${weather.feelsLike}°")
                                            if (today != null) add("H ${today.high}° / L ${today.low}°")
                                        }.joinToString("  ·  "),
                                        color = DtvChrome.SecondaryText,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Right: Borderless Inline Broadcast Telemetry + Refresh
                    Row(
                        modifier = Modifier.weight(0.95f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DtvWeatherTelemetryItem("HUMIDITY", "${weather.humidity}%", Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(30.dp).background(DtvChrome.Divider.copy(alpha = 0.6f)))
                        DtvWeatherTelemetryItem("WIND", "${weather.windSpeedMph} mph ${weather.windDirection}", Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(30.dp).background(DtvChrome.Divider.copy(alpha = 0.6f)))
                        DtvWeatherTelemetryItem("UV INDEX", String.format(Locale.US, "%.1f", weather.uvIndex), Modifier.weight(0.8f))
                        Box(Modifier.width(1.dp).height(30.dp).background(DtvChrome.Divider.copy(alpha = 0.6f)))
                        DtvWeatherTelemetryItem("PRESSURE", "${weather.pressureHpa} hPa", Modifier.weight(1f))
                        DtvActionButton(label = if (loading) "..." else "Refresh", onClick = onRefresh)
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(DtvChrome.Divider)
                )
            }

            // 3. Non-Scrollable Hourly Strip + 3-Day Outlook
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (weather.hourly.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DtvSectionHeader(
                            title = "HOURLY FORECAST",
                            horizontalPad = horizontalPad
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPad),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            weather.hourly.take(8).forEach { hour ->
                                DtvHourlyCell(hour = hour, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (weather.forecast.isNotEmpty()) {
                    val threeDay = weather.forecast.take(3)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DtvSectionHeader(
                            title = "3-DAY OUTLOOK",
                            horizontalPad = horizontalPad
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPad)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF101318))
                                .border(1.dp, DtvChrome.Divider, RoundedCornerShape(3.dp))
                        ) {
                            threeDay.forEachIndexed { idx, day ->
                                val dayLabel = remember(day.date, idx) {
                                    if (idx == 0) "Today"
                                    else runCatching { dayFmt.format(requireNotNull(inputFmt.parse(day.date))) }
                                        .getOrDefault(day.date)
                                }
                                DtvForecastTableRow(dayLabel = dayLabel, day = day)
                                if (idx < threeDay.lastIndex) {
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.RowDivider))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DtvWeatherTelemetryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = DtvChrome.MutedText,
            fontSize = 10.sp,
            fontFamily = BarlowCondensedFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            maxLines = 1
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DtvHourlyCell(
    hour: HourlyForecast,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(DtvChrome.CardGradient)
            .border(1.dp, DtvChrome.CardBorder, RoundedCornerShape(3.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = hour.timeLabel,
            color = DtvChrome.SecondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        com.glztv.app.ui.components.DtvWeatherIcon(
            weatherCode = hour.weatherCode,
            size = 22.dp,
            isNight = com.glztv.app.ui.components.isNightHourLabel(hour.timeLabel)
        )
        Text(
            text = "${hour.temperature}°",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (hour.precipitationChance > 0) {
            Text(
                text = "${hour.precipitationChance}%",
                color = DtvChrome.ElectricBlueSoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DtvForecastTableRow(
    dayLabel: String,
    day: ForecastDay
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel,
            color = DtvChrome.PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(130.dp)
        )
        Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.CenterStart) {
            com.glztv.app.ui.components.DtvWeatherIcon(
                weatherCode = day.weatherCode,
                size = 20.dp,
                isNight = false
            )
        }
        Text(
            text = weatherLabel(day.weatherCode),
            color = DtvChrome.SecondaryText,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (day.precipitationChance > 0) "${day.precipitationChance}% precip" else "Dry",
            color = if (day.precipitationChance > 0) DtvChrome.ElectricBlueSoft else DtvChrome.MutedText,
            fontSize = 12.sp,
            modifier = Modifier.width(96.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "H ${day.high}°",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "L ${day.low}°",
                color = DtvChrome.SecondaryText,
                fontSize = 13.sp
            )
        }
    }
}

private fun weatherGlyph(code: Int): String = when (code) {
    0 -> "☀️"
    1, 2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> "🌧️"
    71, 73, 75, 85, 86 -> "❄️"
    95, 96, 99 -> "⛈️"
    else -> "🌤️"
}

private fun weatherLabel(code: Int): String = when (code) {
    0 -> "Clear"
    1 -> "Mainly Clear"
    2 -> "Partly Cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rain"
    71, 73, 75 -> "Snow"
    80, 81, 82 -> "Rain Showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Fair"
}
