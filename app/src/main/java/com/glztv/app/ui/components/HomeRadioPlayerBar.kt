package com.glztv.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.RadioStation
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable

private val RadioOrange = Color(0xFFFF6B2C)

@Composable
fun HomeRadioPlayerBar(
    station: RadioStation?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    focusRequester: FocusRequester? = null,
    onFocusUp: (() -> Unit)? = null,
    onFocusDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (station == null) return

    var isBarFocused by remember { mutableStateOf(false) }
    var isStopFocused by remember { mutableStateOf(false) }
    val stopRequester = remember { FocusRequester() }
    val barRequester = focusRequester ?: remember { FocusRequester() }

    val infiniteTransition = rememberInfiniteTransition(label = "radio-pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot-alpha"
    )

    Surface(
        onClick = onTogglePlayPause,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(barRequester) else Modifier)
            .googleTvFocusable(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.02f,
                onFocusChange = { isBarFocused = it }
            )
            .onKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (native.action == KeyEvent.ACTION_DOWN) {
                    when (native.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            onTogglePlayPause()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_STOP -> {
                            onStop()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            try {
                                stopRequester.requestFocus()
                            } catch (_: Exception) {}
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (onFocusUp != null) {
                                onFocusUp()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (onFocusDown != null) {
                                onFocusDown()
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF111722),
        border = when {
            isBarFocused -> BorderStroke(2.dp, Color.White)
            isPlaying -> BorderStroke(1.dp, RadioOrange.copy(alpha = 0.5f))
            else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        },
        tonalElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            RadioOrange.copy(alpha = 0.16f),
                            Color(0xFF141C2B),
                            Color(0xFF0F1420)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Station Logo / Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!station.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = station.logoUrl,
                            contentDescription = station.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = RadioOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                // Station Info + Live Badge
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Live indicator badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isPlaying) RadioOrange.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPlaying) RadioOrange.copy(alpha = dotAlpha)
                                            else Color.Gray
                                        )
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (isPlaying) "LIVE RADIO" else "PAUSED",
                                    color = if (isPlaying) RadioOrange else Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = station.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = if (!station.genre.isNullOrBlank()) station.genre else "GLZ Radio Broadcast Network",
                        color = GoogleTvTokens.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(14.dp))

                // Animated Equalizer Bars
                RadioEqualizerBars(
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(end = 16.dp)
                )

                // Play / Pause Button Indicator
                Surface(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = if (isBarFocused) Color.White else RadioOrange,
                    contentColor = if (isBarFocused) Color.Black else Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Stop / Close Button
                Surface(
                    onClick = onStop,
                    modifier = Modifier
                        .size(38.dp)
                        .focusRequester(stopRequester)
                        .googleTvFocusable(
                            shape = CircleShape,
                            focusedScale = 1.15f,
                            onFocusChange = { isStopFocused = it }
                        )
                        .onKeyEvent { event ->
                            val native = event.nativeKeyEvent
                            if (native.action == KeyEvent.ACTION_DOWN) {
                                when (native.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        try {
                                            barRequester.requestFocus()
                                        } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        if (onFocusUp != null) {
                                            onFocusUp()
                                            true
                                        } else false
                                    }
                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        if (onFocusDown != null) {
                                            onFocusDown()
                                            true
                                        } else false
                                    }
                                    KeyEvent.KEYCODE_DPAD_CENTER,
                                    KeyEvent.KEYCODE_ENTER,
                                    KeyEvent.KEYCODE_MEDIA_STOP -> {
                                        onStop()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    shape = CircleShape,
                    color = if (isStopFocused) Color.White else Color.White.copy(alpha = 0.12f),
                    contentColor = if (isStopFocused) Color.Black else Color.White,
                    border = if (isStopFocused) BorderStroke(2.dp, Color.White) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Radio",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq-transition")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h4"
    )

    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barColor = RadioOrange
        val barInactive = RadioOrange.copy(alpha = 0.3f)

        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) (20.dp * h1).coerceAtLeast(4.dp) else 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPlaying) barColor else barInactive)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) (20.dp * h2).coerceAtLeast(4.dp) else 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPlaying) barColor else barInactive)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) (20.dp * h3).coerceAtLeast(4.dp) else 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPlaying) barColor else barInactive)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) (20.dp * h4).coerceAtLeast(4.dp) else 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPlaying) barColor else barInactive)
        )
    }
}
