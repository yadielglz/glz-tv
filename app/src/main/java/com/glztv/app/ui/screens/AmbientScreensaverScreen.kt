package com.glztv.app.ui.screens

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Nightlight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.model.ForecastDay
import com.glztv.app.model.WeatherInfo
import com.glztv.app.ui.WeatherFormatter
import com.glztv.app.ui.i18n.LocalGlzStrings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AmbientScreensaverScreen(
    weather: WeatherInfo?,
    guestName: String = "Guest",
    radioPlaying: Boolean = false,
    radioStationName: String? = null,
    radioGenre: String? = null,
    radioLogoUrl: String? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var pixelShiftIndex by remember { mutableIntStateOf(0) }

    // Update time every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(3_000L)
        }
    }

    // OLED Pixel-Shift drift every 45 seconds to eliminate burn-in risk
    LaunchedEffect(Unit) {
        while (true) {
            delay(45_000L)
            pixelShiftIndex = (pixelShiftIndex + 1) % 8
        }
    }

    // Auto-focus so any remote D-pad / key immediately captures and wakes up
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { focusRequester.requestFocus() }
    }

    BackHandler(onBack = onDismiss)

    // Calculate pixel shift offsets for burn-in protection
    val shiftOffsets = remember {
        listOf(
            0.dp to 0.dp,
            4.dp to 2.dp,
            (-3).dp to 4.dp,
            2.dp to (-3).dp,
            (-4).dp to (-2).dp,
            3.dp to 3.dp,
            (-2).dp to 4.dp,
            2.dp to (-2).dp
        )
    }
    val currentShift = shiftOffsets[pixelShiftIndex]

    // Slow drifting ambient aurora animation
    val infiniteTransition = rememberInfiniteTransition(label = "AuroraTransition")
    val auroraProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuroraOffset"
    )

    // Organic breathing cycle for the ambient aurora
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuroraBreathScale"
    )
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuroraBreathAlpha"
    )

    // Dynamic audio resonance when radio is streaming
    val musicPulse by if (radioPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_050, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "MusicBassSwell"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    // Breathing pulse for the time colon
    val colonAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ColonPulse"
    )

    // Glowing live dot for radio
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LiveDotPulse"
    )

    val primaryAccent = MaterialTheme.colorScheme.primary
    val secondaryAccent = MaterialTheme.colorScheme.secondary

    val hoursFormat = remember { SimpleDateFormat("h", Locale.getDefault()) }
    val minutesFormat = remember { SimpleDateFormat("mm", Locale.getDefault()) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val dateObj = Date(currentTime)
    val hoursString = hoursFormat.format(dateObj)
    val minutesString = minutesFormat.format(dateObj)
    val amPmString = amPmFormat.format(dateObj).uppercase(Locale.getDefault())
    val dateString = dateFormat.format(dateObj)

    val strings = LocalGlzStrings.current
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> strings.goodMorning
        in 12..17 -> strings.goodAfternoon
        else -> strings.goodEvening
    }

    // Dynamic weather atmosphere colors
    val auroraColors = remember(weather?.weatherCode, hour) {
        val code = weather?.weatherCode ?: 0
        val isNight = hour !in 6..19
        when {
            code in listOf(0, 1) && !isNight -> listOf(
                Color(0xFFFF9100).copy(alpha = 0.20f),
                Color(0xFF00B0FF).copy(alpha = 0.15f)
            )
            code in listOf(0, 1) && isNight -> listOf(
                Color(0xFF3D5AFE).copy(alpha = 0.18f),
                Color(0xFF00E5FF).copy(alpha = 0.13f)
            )
            code in 51..67 || code in 80..82 -> listOf(
                Color(0xFF00B4D8).copy(alpha = 0.20f),
                Color(0xFF023E8A).copy(alpha = 0.16f)
            )
            code in 95..99 -> listOf(
                Color(0xFF7C4DFF).copy(alpha = 0.22f),
                Color(0xFF00E5FF).copy(alpha = 0.14f)
            )
            code in 71..77 || code in listOf(85, 86) -> listOf(
                Color(0xFF80DEEA).copy(alpha = 0.18f),
                Color(0xFF0097A7).copy(alpha = 0.14f)
            )
            code in 2..3 || code in listOf(45, 48) -> listOf(
                Color(0xFF546E7A).copy(alpha = 0.18f),
                Color(0xFF1976D2).copy(alpha = 0.14f)
            )
            else -> listOf(
                primaryAccent.copy(alpha = 0.18f),
                secondaryAccent.copy(alpha = 0.14f)
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060913))
            .drawBehind {
                val totalScale = breathScale * musicPulse
                val effectiveAlpha = breathAlpha * (if (radioPlaying) 1.12f else 1.0f)

                // Primary glowing atmospheric body (upper/center left)
                val cx1 = size.width * (0.24f + 0.28f * auroraProgress)
                val cy1 = size.height * (0.32f + 0.20f * (1f - auroraProgress))
                val r1 = size.width * 0.52f * totalScale
                val c1 = auroraColors[0]

                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to c1.copy(alpha = (c1.alpha * 1.30f * effectiveAlpha).coerceAtMost(0.40f)),
                            0.25f to c1.copy(alpha = (c1.alpha * 0.90f * effectiveAlpha).coerceAtMost(0.28f)),
                            0.50f to c1.copy(alpha = (c1.alpha * 0.50f * effectiveAlpha).coerceAtMost(0.16f)),
                            0.75f to c1.copy(alpha = (c1.alpha * 0.18f * effectiveAlpha).coerceAtMost(0.06f)),
                            0.92f to c1.copy(alpha = (c1.alpha * 0.04f * effectiveAlpha).coerceAtMost(0.015f)),
                            1.0f to Color.Transparent
                        ),
                        center = Offset(cx1, cy1),
                        radius = r1
                    )
                )

                // Secondary glowing atmospheric body (lower/center right)
                val cx2 = size.width * (0.76f - 0.26f * auroraProgress)
                val cy2 = size.height * (0.64f - 0.18f * auroraProgress)
                val r2 = size.width * 0.48f * (2.05f - totalScale)
                val c2 = auroraColors[1]

                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to c2.copy(alpha = (c2.alpha * 1.25f * effectiveAlpha).coerceAtMost(0.35f)),
                            0.25f to c2.copy(alpha = (c2.alpha * 0.85f * effectiveAlpha).coerceAtMost(0.24f)),
                            0.50f to c2.copy(alpha = (c2.alpha * 0.45f * effectiveAlpha).coerceAtMost(0.14f)),
                            0.75f to c2.copy(alpha = (c2.alpha * 0.16f * effectiveAlpha).coerceAtMost(0.05f)),
                            0.92f to c2.copy(alpha = (c2.alpha * 0.03f * effectiveAlpha).coerceAtMost(0.012f)),
                            1.0f to Color.Transparent
                        ),
                        center = Offset(cx2, cy2),
                        radius = r2
                    )
                )

                // Third resonance aura lighting up the music stage when radio is active
                if (radioPlaying) {
                    val cxRadio = size.width * 0.32f
                    val cyRadio = size.height * 0.76f
                    val rRadio = size.width * 0.36f * musicPulse
                    val cRadio = primaryAccent

                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to cRadio.copy(alpha = (0.22f * musicPulse).coerceAtMost(0.35f)),
                                0.32f to cRadio.copy(alpha = (0.12f * musicPulse).coerceAtMost(0.20f)),
                                0.68f to cRadio.copy(alpha = (0.04f * musicPulse).coerceAtMost(0.08f)),
                                1.0f to Color.Transparent
                            ),
                            center = Offset(cxRadio, cyRadio),
                            radius = rRadio
                        )
                    )
                }
            }
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    onDismiss()
                    true
                } else false
            }
    ) {
        // Dynamic breakpoint detection for handhelds vs TV screens
        val isCompactHeight = maxHeight < 500.dp
        val isVeryCompactHeight = maxHeight < 400.dp

        val hPadding = if (isVeryCompactHeight) 24.dp else if (isCompactHeight) 36.dp else 56.dp
        val vPadding = if (isVeryCompactHeight) 10.dp else if (isCompactHeight) 18.dp else 36.dp

        val clockHoursSize = if (isVeryCompactHeight) 78.sp else if (isCompactHeight) 96.sp else 124.sp
        val clockColonSize = if (isVeryCompactHeight) 72.sp else if (isCompactHeight) 88.sp else 114.sp
        val clockDateSize = if (isVeryCompactHeight) 15.sp else if (isCompactHeight) 18.sp else 24.sp

        val weatherTempSize = if (isVeryCompactHeight) 40.sp else if (isCompactHeight) 48.sp else 62.sp
        val weatherGlyphSize = if (isVeryCompactHeight) 32.sp else if (isCompactHeight) 38.sp else 50.sp

        // Main Content Container with OLED Pixel-Shift Offset
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = currentShift.first, y = currentShift.second)
                .padding(horizontal = hPadding, vertical = vPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Greeting, Mode Badge & Wakeup Prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clean Greeting
                Text(
                    "$greeting${if (guestName.isNotBlank() && guestName != "Guest") ", $guestName" else ""}",
                    fontSize = if (isCompactHeight) 14.sp else 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.70f),
                    letterSpacing = 0.5.sp
                )

                // Remote wake-up hint
                Text(
                    strings.screensaverPrompt,
                    fontSize = if (isCompactHeight) 10.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }

            // Central Stage: Emphasized Clock & Radio (Left) + Integrated Weather Hero (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = if (isVeryCompactHeight) 2.dp else if (isCompactHeight) 6.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isCompactHeight) 20.dp else 36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Hero Clock & Live Music / Radio
                Column(
                    modifier = Modifier.weight(1.15f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Massive Emphasized Time Display
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(bottom = if (isCompactHeight) 2.dp else 6.dp)
                    ) {
                        Text(
                            text = hoursString,
                            fontSize = clockHoursSize,
                            fontWeight = FontWeight.Light,
                            color = Color.White,
                            letterSpacing = (-1.5).sp
                        )
                        Text(
                            text = ":",
                            fontSize = clockColonSize,
                            fontWeight = FontWeight.Thin,
                            color = primaryAccent.copy(alpha = colonAlpha),
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .offset(y = if (isVeryCompactHeight) (-4).dp else (-8).dp)
                        )
                        Text(
                            text = minutesString,
                            fontSize = clockHoursSize,
                            fontWeight = FontWeight.Light,
                            color = Color.White,
                            letterSpacing = (-1.5).sp
                        )
                        Spacer(Modifier.width(if (isCompactHeight) 10.dp else 16.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primaryAccent.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.padding(
                                bottom = if (isVeryCompactHeight) 10.dp else if (isCompactHeight) 16.dp else 26.dp
                            )
                        ) {
                            Text(
                                text = amPmString,
                                fontSize = if (isCompactHeight) 12.sp else 15.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryAccent,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(
                                    horizontal = if (isCompactHeight) 8.dp else 11.dp,
                                    vertical = if (isCompactHeight) 3.dp else 5.dp
                                )
                            )
                        }
                    }

                    // Formatted Date
                    Text(
                        text = dateString,
                        fontSize = clockDateSize,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.4.sp
                    )

                    Spacer(Modifier.height(if (isVeryCompactHeight) 6.dp else if (isCompactHeight) 10.dp else 20.dp))

                    // Radio Now Playing Hero Widget
                    if (radioPlaying && !radioStationName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(if (isCompactHeight) 16.dp else 22.dp),
                            color = Color(0xD90D1526),
                            border = BorderStroke(1.2.dp, primaryAccent.copy(alpha = 0.40f)),
                            modifier = Modifier.fillMaxWidth(if (isCompactHeight) 1f else 0.95f)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isCompactHeight) 12.dp else 18.dp,
                                    vertical = if (isVeryCompactHeight) 6.dp else if (isCompactHeight) 8.dp else 12.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (isCompactHeight) 10.dp else 16.dp)
                            ) {
                                // Station Logo / Artwork or fallback icon
                                Surface(
                                    shape = RoundedCornerShape(if (isCompactHeight) 10.dp else 16.dp),
                                    color = primaryAccent.copy(alpha = 0.18f),
                                    modifier = Modifier.size(if (isVeryCompactHeight) 38.dp else if (isCompactHeight) 44.dp else 54.dp)
                                ) {
                                    if (!radioLogoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = radioLogoUrl,
                                            contentDescription = radioStationName,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(if (isCompactHeight) 4.dp else 6.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Radio,
                                                contentDescription = null,
                                                tint = primaryAccent,
                                                modifier = Modifier.size(if (isCompactHeight) 22.dp else 28.dp)
                                            )
                                        }
                                    }
                                }

                                // Station Details
                                val formattedStationName = remember(radioStationName) {
                                    radioStationName?.replace(Regex("(?i)^online\\s*[|·\\-–/:]?\\s*"), "")?.trim()
                                }
                                val formattedRadioSubtitle = remember(radioGenre) {
                                    radioGenre?.let { raw ->
                                        var text = raw.trim()
                                        text = text.replace(Regex("(?i)^online\\s*[|·\\-–/:]?\\s*"), "").trim()
                                        text = text.replace(Regex("(?i)\\s*[|·\\-–/:]?\\s*online$"), "").trim()
                                        if (text.equals("online", ignoreCase = true)) "" else text
                                    }?.takeIf { it.isNotBlank() }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(if (isCompactHeight) 5.dp else 7.dp)
                                                .background(
                                                    Color(0xFF00E676).copy(alpha = liveDotAlpha),
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            strings.nowStreamingRadio,
                                            fontSize = if (isCompactHeight) 8.5.sp else 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = primaryAccent
                                        )
                                    }
                                    Spacer(Modifier.height(1.dp))
                                    Text(
                                        formattedStationName ?: radioStationName,
                                        fontSize = if (isVeryCompactHeight) 14.sp else if (isCompactHeight) 16.sp else 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!formattedRadioSubtitle.isNullOrBlank()) {
                                        Text(
                                            formattedRadioSubtitle,
                                            fontSize = if (isCompactHeight) 10.5.sp else 12.sp,
                                            color = Color.White.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Animated Equalizer Audio Visualizer
                                AnimatedEqualizerBars(
                                    accent = primaryAccent,
                                    isCompact = isCompactHeight
                                )
                            }
                        }
                    } else {
                        // Ambient tranquil pill when no music is playing
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isCompactHeight) 10.dp else 14.dp,
                                    vertical = if (isCompactHeight) 5.dp else 8.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    if (hour in 6..18) Icons.Default.WbSunny else Icons.Default.Nightlight,
                                    contentDescription = null,
                                    tint = primaryAccent.copy(alpha = 0.8f),
                                    modifier = Modifier.size(if (isCompactHeight) 13.dp else 16.dp)
                                )
                                Text(
                                    if (hour in 6..18) "Daylight Ambient" else "Starlight Ambient",
                                    fontSize = if (isCompactHeight) 10.5.sp else 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }

                // Right Column: Integrated Weather Showcase (Hero + Mini Forecast Strip)
                Column(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (weather != null) {
                        val today = weather.forecast.firstOrNull()

                        // Weather Hero Card
                        Surface(
                            shape = RoundedCornerShape(if (isCompactHeight) 18.dp else 26.dp),
                            color = Color(0x9911192A),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = if (isCompactHeight) 14.dp else 22.dp,
                                    vertical = if (isVeryCompactHeight) 8.dp else if (isCompactHeight) 10.dp else 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(if (isVeryCompactHeight) 4.dp else if (isCompactHeight) 6.dp else 10.dp)
                            ) {
                                // Location & Condition Title
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        weather.location.uppercase(Locale.getDefault()),
                                        fontSize = if (isCompactHeight) 9.5.sp else 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = primaryAccent,
                                        letterSpacing = 1.1.sp
                                    )
                                    Text(
                                        weatherDescription(weather.weatherCode),
                                        fontSize = if (isCompactHeight) 12.sp else 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.80f)
                                    )
                                }

                                // Temperature & Weather Symbol
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            "${weather.temperature}°",
                                            fontSize = weatherTempSize,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            letterSpacing = (-1).sp
                                        )
                                        Spacer(Modifier.width(if (isCompactHeight) 8.dp else 14.dp))
                                        Column(modifier = Modifier.padding(bottom = if (isCompactHeight) 4.dp else 8.dp)) {
                                            if (today != null) {
                                                Text(
                                                    "▲ ${today.high}°",
                                                    fontSize = if (isCompactHeight) 10.5.sp else 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.85f)
                                                )
                                                Text(
                                                    "▼ ${today.low}°",
                                                    fontSize = if (isCompactHeight) 10.5.sp else 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.50f)
                                                )
                                            }
                                        }
                                    }

                                    // Dynamic Weather Glyph
                                    Text(
                                        weatherSymbol(weather.weatherCode),
                                        fontSize = weatherGlyphSize
                                    )
                                }

                                // Environmental Telemetry Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    WeatherTelemetryPill(
                                        label = "Feels ${weather.feelsLike}°",
                                        icon = Icons.Default.Thermostat,
                                        accent = primaryAccent,
                                        isCompact = isCompactHeight,
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherTelemetryPill(
                                        label = "${weather.humidity}%",
                                        icon = Icons.Default.WaterDrop,
                                        accent = Color(0xFF00E5FF),
                                        isCompact = isCompactHeight,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    WeatherTelemetryPill(
                                        label = "${weather.windSpeedMph} mph",
                                        icon = Icons.Default.Air,
                                        accent = Color(0xFF81D4FA),
                                        isCompact = isCompactHeight,
                                        modifier = Modifier.weight(1.05f)
                                    )
                                }
                            }
                        }

                        // Mini 4-Day Forecast Strip
                        if (weather.forecast.isNotEmpty()) {
                            Spacer(Modifier.height(if (isVeryCompactHeight) 4.dp else if (isCompactHeight) 6.dp else 10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                weather.forecast.take(4).forEachIndexed { index, day ->
                                    Surface(
                                        shape = RoundedCornerShape(if (isCompactHeight) 12.dp else 16.dp),
                                        color = if (index == 0) primaryAccent.copy(alpha = 0.12f)
                                        else Color.White.copy(alpha = 0.05f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (index == 0) primaryAccent.copy(alpha = 0.30f)
                                            else Color.White.copy(alpha = 0.08f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(
                                                vertical = if (isVeryCompactHeight) 5.dp else if (isCompactHeight) 7.dp else 10.dp,
                                                horizontal = if (isCompactHeight) 3.dp else 6.dp
                                            ),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 2.dp else 4.dp)
                                        ) {
                                            Text(
                                                if (index == 0) "TODAY" else dayLabel(day.date),
                                                fontSize = if (isCompactHeight) 9.sp else 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (index == 0) primaryAccent else Color.White.copy(alpha = 0.70f)
                                            )
                                            Text(
                                                weatherSymbol(day.weatherCode),
                                                fontSize = if (isVeryCompactHeight) 15.sp else if (isCompactHeight) 17.sp else 20.sp
                                            )
                                            Text(
                                                "${day.high}° / ${day.low}°",
                                                fontSize = if (isCompactHeight) 9.5.sp else 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            if (day.precipitationChance > 0) {
                                                Text(
                                                    "☂ ${day.precipitationChance}%",
                                                    fontSize = if (isCompactHeight) 8.sp else 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF00E5FF)
                                                )
                                            } else {
                                                Text(
                                                    "☂ 0%",
                                                    fontSize = if (isCompactHeight) 8.sp else 9.sp,
                                                    color = Color.White.copy(alpha = 0.35f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Weather Unavailable Placeholder Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = primaryAccent.copy(alpha = 0.5f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    "Weather Syncing…",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    "Check weather location in settings",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.4f)
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
private fun WeatherTelemetryPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(if (isCompact) 8.dp else 12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 5.dp else 8.dp,
                vertical = if (isCompact) 3.5.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(if (isCompact) 11.dp else 13.dp)
            )
            Spacer(Modifier.width(if (isCompact) 3.dp else 5.dp))
            Text(
                label,
                fontSize = if (isCompact) 9.5.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AnimatedEqualizerBars(
    accent: Color,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "EqualizerTransition")

    val max1 = if (isCompact) 18f else 26f
    val max2 = if (isCompact) 20f else 30f
    val min1 = if (isCompact) 4f else 6f

    val h1 by transition.animateFloat(
        initialValue = min1,
        targetValue = max1,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqBar1"
    )
    val h2 by transition.animateFloat(
        initialValue = max1,
        targetValue = min1 + 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(530, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqBar2"
    )
    val h3 by transition.animateFloat(
        initialValue = min1 + 3f,
        targetValue = max2,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqBar3"
    )
    val h4 by transition.animateFloat(
        initialValue = max1 - 2f,
        targetValue = min1,
        animationSpec = infiniteRepeatable(
            animation = tween(470, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqBar4"
    )
    val h5 by transition.animateFloat(
        initialValue = min1 + 2f,
        targetValue = max1 + 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(390, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqBar5"
    )

    Row(
        modifier = modifier.height(if (isCompact) 22.dp else 32.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 2.5.dp else 3.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(h1, h2, h3, h4, h5).forEach { heightVal ->
            Box(
                modifier = Modifier
                    .width(if (isCompact) 2.5.dp else 3.5.dp)
                    .height(heightVal.dp)
                    .background(accent, RoundedCornerShape(2.dp))
            )
        }
    }
}

private fun weatherSymbol(code: Int): String = WeatherFormatter.symbol(code)

private fun weatherDescription(code: Int): String = WeatherFormatter.description(code)

private fun dayLabel(value: String): String = WeatherFormatter.dayLabel(value)

