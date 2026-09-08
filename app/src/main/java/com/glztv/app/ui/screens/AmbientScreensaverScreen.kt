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
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Radio
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.model.WeatherInfo
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var pixelShiftIndex by remember { mutableIntStateOf(0) }

    // Update time every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(5_000L)
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

    // Calculate pixel shift offsets
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
            animation = tween(durationMillis = 24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuroraOffset"
    )

    val primaryAccent = MaterialTheme.colorScheme.primary
    val secondaryAccent = MaterialTheme.colorScheme.secondary

    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val dateObj = Date(currentTime)
    val timeString = timeFormat.format(dateObj)
    val amPmString = amPmFormat.format(dateObj).uppercase(Locale.getDefault())
    val dateString = dateFormat.format(dateObj)

    val strings = com.glztv.app.ui.i18n.LocalGlzStrings.current
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> strings.goodMorning
        in 12..17 -> strings.goodAfternoon
        else -> strings.goodEvening
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060911))
            .drawBehind {
                val cx1 = size.width * (0.25f + 0.35f * auroraProgress)
                val cy1 = size.height * (0.35f + 0.25f * (1f - auroraProgress))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(cx1, cy1),
                        radius = size.width * 0.55f
                    )
                )

                val cx2 = size.width * (0.80f - 0.30f * auroraProgress)
                val cy2 = size.height * (0.65f - 0.20f * auroraProgress)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryAccent.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(cx2, cy2),
                        radius = size.width * 0.50f
                    )
                )
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
        // Main Content Container with OLED Pixel-Shift Offset
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = currentShift.first, y = currentShift.second)
                .padding(horizontal = 64.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Greeting & Weather
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Greeting & Guest Name
                Column {
                    Text(
                        "$greeting${if (guestName.isNotBlank() && guestName != "Guest") ", $guestName" else ""}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.65f),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        strings.screensaverTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryAccent,
                        letterSpacing = 1.2.sp
                    )
                }

                // Weather Capsule
                if (weather != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (weather.weatherCode in listOf(0, 1)) Icons.Default.WbSunny else Icons.Default.Cloud,
                                contentDescription = null,
                                tint = primaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "${weather.temperature}°",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        weather.location,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.70f)
                                    )
                                }
                                if (weather.forecast.isNotEmpty()) {
                                    val today = weather.forecast.first()
                                    Text(
                                        "H: ${today.high}° · L: ${today.low}°",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.50f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Center Display: Clock & Date or Radio Playing Card
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeString,
                        fontSize = 92.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = Color.White,
                        letterSpacing = (-2).sp
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = amPmString,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )
                }

                Text(
                    text = dateString,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.80f),
                    letterSpacing = 0.5.sp
                )

                // Radio Now Playing Hero Card (if radio is playing)
                if (radioPlaying && !radioStationName.isNullOrBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xCC111827),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryAccent.copy(alpha = 0.40f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = primaryAccent.copy(alpha = 0.20f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Radio,
                                        contentDescription = null,
                                        tint = primaryAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    strings.nowStreamingRadio,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = primaryAccent
                                )
                                Text(
                                    radioStationName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                if (!radioGenre.isNullOrBlank()) {
                                    Text(
                                        radioGenre,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            // Equalizer Wave Bars
                            AnimatedEqualizerBars(accent = primaryAccent)
                        }
                    }
                }
            }

            // Bottom Hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.screensaverPrompt,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.35f)
                )

                Text(
                    "GLZ TV",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = primaryAccent.copy(alpha = 0.40f)
                )
            }
        }
    }
}

@Composable
private fun AnimatedEqualizerBars(
    accent: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "EqualizerTransition")

    val h1 by transition.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqBar1"
    )
    val h2 by transition.animateFloat(
        initialValue = 26f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqBar2"
    )
    val h3 by transition.animateFloat(
        initialValue = 12f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqBar3"
    )
    val h4 by transition.animateFloat(
        initialValue = 22f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqBar4"
    )

    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(h1, h2, h3, h4).forEach { heightVal ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(heightVal.dp)
                    .background(accent, RoundedCornerShape(2.dp))
            )
        }
    }
}
