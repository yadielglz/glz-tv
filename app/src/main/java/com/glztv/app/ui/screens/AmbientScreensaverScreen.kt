package com.glztv.app.ui.screens

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.model.WeatherInfo
import com.glztv.app.ui.i18n.LocalGlzStrings
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.LocalTvViewport
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Modern Material 3 / Google TV Ambient Screensaver.
 * Engineered for minimal RAM and CPU footprint:
 * - Zero 60fps full-screen recompositions (clock ticks every 10s, SimpleDateFormats remembered)
 * - Smooth OLED burn-in drift via draw-phase graphicsLayer translation every 45s
 * - Crisp Material 3 cards for Weather, 4-Day Outlook, and Live Radio Now Playing
 */
@Composable
fun AmbientScreensaverScreen(
    weather: WeatherInfo?,
    guestName: String,
    radioPlaying: Boolean = false,
    radioStationName: String? = null,
    radioGenre: String? = null,
    radioLogoUrl: String? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalGlzStrings.current
    val viewport = LocalTvViewport.current
    val focusRequester = remember { FocusRequester() }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var driftStep by remember { mutableIntStateOf(0) }

    // Low-overhead clock tick (every 10 seconds instead of 60fps)
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    // Subtle OLED burn-in protection shift every 45 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(45_000L)
            driftStep = (driftStep + 1) % 8
        }
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    BackHandler(onBack = onDismiss)

    val driftOffsets = remember {
        listOf(
            0f to 0f, 8f to 4f, 12f to -4f, 4f to -8f,
            -8f to -4f, -12f to 4f, -4f to 8f, 4f to 4f
        )
    }
    val targetOffset = driftOffsets[driftStep]
    val driftX by animateFloatAsState(
        targetValue = targetOffset.first,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "oled-drift-x"
    )
    val driftY by animateFloatAsState(
        targetValue = targetOffset.second,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "oled-drift-y"
    )

    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val formattedTime = remember(nowMillis / 10_000L) { timeFormat.format(Date(nowMillis)) }
    val formattedAmPm = remember(nowMillis / 10_000L) { amPmFormat.format(Date(nowMillis)).uppercase(Locale.US) }
    val formattedDate = remember(nowMillis / 60_000L) { dateFormat.format(Date(nowMillis)) }
    val hourOfDay = remember(nowMillis / 60_000L) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    }

    val greeting = when (hourOfDay) {
        in 5..11 -> strings.goodMorning
        in 12..17 -> strings.goodAfternoon
        else -> strings.goodEvening
    }

    val accent = MaterialTheme.colorScheme.primary
    val ambientPalette = remember(weather?.weatherCode, hourOfDay, accent) {
        ambientGradientColors(weather?.weatherCode, hourOfDay, accent)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090F))
            .drawBehind {
                val r1 = size.maxDimension * 0.65f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(ambientPalette.first.copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.22f),
                        radius = r1
                    )
                )
                val r2 = size.maxDimension * 0.58f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(ambientPalette.second.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.84f, size.height * 0.78f),
                        radius = r2
                    )
                )
            }
            .focusRequester(focusRequester)
            .focusable()
            .clickable(onClick = onDismiss)
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = viewport.safeHorizontal + 12.dp, vertical = viewport.safeVertical + 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = driftX
                    translationY = driftY
                },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Minimal Material 3 Ambient Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.glztv.app.R.drawable.glztv_logo_emblem),
                        contentDescription = "GlzTV",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (guestName.isNotBlank() && !guestName.equals("Guest", ignoreCase = true)) {
                            "$greeting, $guestName"
                        } else {
                            greeting
                        },
                        color = GoogleTvTokens.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Text(
                        text = strings.screensaverPrompt,
                        color = GoogleTvTokens.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // 2. Main Ambient Canvas: Left Clock & Radio | Right Material 3 Weather & Outlook
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = if (viewport.isCompactHeight) 10.dp else 20.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Hero Clock + Date + Live Radio Card
                Column(
                    modifier = Modifier
                        .weight(1.08f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formattedTime,
                            color = Color.White,
                            fontSize = if (viewport.isCompactHeight) 74.sp else 94.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-2).sp,
                            lineHeight = if (viewport.isCompactHeight) 76.sp else 96.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accent.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                            modifier = Modifier.padding(bottom = 14.dp)
                        ) {
                            Text(
                                text = formattedAmPm,
                                color = accent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = formattedDate,
                        color = GoogleTvTokens.TextSecondary,
                        fontSize = if (viewport.isCompactHeight) 18.sp else 22.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(if (viewport.isCompactHeight) 18.dp else 28.dp))

                    if (radioPlaying && !radioStationName.isNullOrBlank()) {
                        AmbientRadioNowPlayingCard(
                            stationName = radioStationName,
                            genre = radioGenre,
                            logoUrl = radioLogoUrl,
                            accent = accent,
                            liveLabel = strings.nowStreamingRadio
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF16181D).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.screensaverTitle,
                                    color = GoogleTvTokens.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // Right Column: Material 3 Weather Summary & 4-Day Forecast
                Column(
                    modifier = Modifier
                        .weight(0.92f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (weather != null) {
                        AmbientWeatherCard(
                            weather = weather,
                            accent = accent,
                            compact = viewport.isCompactHeight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientRadioNowPlayingCard(
    stationName: String,
    genre: String?,
    logoUrl: String?,
    accent: Color,
    liveLabel: String
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF16181D).copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(0.92f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1F2430),
                modifier = Modifier.size(52.dp)
            ) {
                if (!logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = stationName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Text(
                        text = liveLabel,
                        color = Color(0xFF00E676),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
                Text(
                    text = stationName.substringAfter("|").trim(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!genre.isNullOrBlank()) {
                    Text(
                        text = genre,
                        color = GoogleTvTokens.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AudioSpectrumVisualizer(
                isPlaying = true,
                barCount = 5,
                barColor = accent,
                secondaryColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .width(34.dp)
                    .height(22.dp)
            )
        }
    }
}

@Composable
private fun AmbientWeatherCard(
    weather: WeatherInfo,
    accent: Color,
    compact: Boolean
) {
    val today = weather.forecast.firstOrNull()
    val inputFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF16181D).copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 18.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
        ) {
            // Header: Location + Current Temp + Weather Glyph
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weather.location.uppercase(Locale.getDefault()),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${weather.temperature}°",
                        color = Color.White,
                        fontSize = if (compact) 46.sp else 54.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = if (compact) 48.sp else 56.sp
                    )
                    Text(
                        text = ambientWeatherDescription(weather.weatherCode),
                        color = GoogleTvTokens.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (today != null) {
                        Text(
                            text = "H ${today.high}°  ·  L ${today.low}°",
                            color = GoogleTvTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = ambientWeatherSymbol(weather.weatherCode),
                    fontSize = if (compact) 48.sp else 56.sp
                )
            }

            // Telemetry Chips Row (Feels Like, Humidity, Wind)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AmbientMetricChip(Icons.Default.Thermostat, "Feels ${weather.feelsLike}°", Modifier.weight(1f))
                AmbientMetricChip(Icons.Default.WaterDrop, "${weather.humidity}%", Modifier.weight(1f))
                AmbientMetricChip(Icons.Default.Air, "${weather.windSpeedMph} mph", Modifier.weight(1f))
            }

            // 4-Day Forecast Strip
            val days = weather.forecast.take(4)
            if (days.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    days.forEachIndexed { idx, day ->
                        val label = remember(day.date, idx) {
                            if (idx == 0) "TODAY"
                            else runCatching { dayFmt.format(requireNotNull(inputFmt.parse(day.date))).uppercase(Locale.getDefault()) }
                                .getOrDefault(day.date.takeLast(2))
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E222B)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (idx == 0) accent else GoogleTvTokens.TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = ambientWeatherSymbol(day.weatherCode), fontSize = 20.sp)
                                Text(
                                    text = "${day.high}°/${day.low}°",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientMetricChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E222B)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoogleTvTokens.BlueAccent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = GoogleTvTokens.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun ambientGradientColors(weatherCode: Int?, hour: Int, fallback: Color): Pair<Color, Color> = when {
    weatherCode != null && weatherCode in 95..99 -> Color(0xFF7C4DFF) to Color(0xFF311B92)
    weatherCode != null && (weatherCode in 51..67 || weatherCode in 80..82) -> Color(0xFF29B6F6) to Color(0xFF0277BD)
    hour in 5..10 -> Color(0xFFFFB74D) to Color(0xFF42A5F5)
    hour in 11..16 -> fallback to Color(0xFF26C6DA)
    hour in 17..20 -> Color(0xFFFF7043) to Color(0xFF7E57C2)
    else -> Color(0xFF3949AB) to Color(0xFF00ACC1)
}

private fun ambientWeatherSymbol(code: Int): String = when (code) {
    0 -> "☀"
    1, 2 -> "⛅"
    3 -> "☁"
    45, 48 -> "≋"
    in 51..67, in 80..82 -> "☂"
    in 71..77, 85, 86 -> "❄"
    in 95..99 -> "ϟ"
    else -> "☀"
}

private fun ambientWeatherDescription(code: Int): String = when (code) {
    0 -> "Clear Sky"
    1 -> "Mostly Clear"
    2 -> "Partly Cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    in 51..57 -> "Light Drizzle"
    in 61..67, in 80..82 -> "Rain Showers"
    in 71..77, 85, 86 -> "Snowfall"
    in 95..99 -> "Thunderstorms"
    else -> "Fair"
}
