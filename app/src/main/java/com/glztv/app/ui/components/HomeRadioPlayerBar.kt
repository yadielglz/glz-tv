package com.glztv.app.ui.components

import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.RadioStation
import com.glztv.app.player.RadioPlaybackState
import com.glztv.app.player.RadioPlaybackStatus
import com.glztv.app.ui.i18n.LocalGlzStrings
import com.glztv.app.ui.screens.AudioSpectrumVisualizer
import com.glztv.app.ui.theme.googleTvFocusable

@Composable
fun HomeRadioPlayerBar(
    station: RadioStation?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    playbackState: RadioPlaybackState? = null,
    focusRequester: FocusRequester? = null,
    onFocusUp: (() -> Unit)? = null,
    onFocusDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (station == null) return

    val strings = LocalGlzStrings.current
    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val effectiveStatus = playbackState?.status ?: if (isPlaying) RadioPlaybackStatus.Playing else RadioPlaybackStatus.Paused
    val isLive = effectiveStatus == RadioPlaybackStatus.Playing
    val isBuffering = effectiveStatus == RadioPlaybackStatus.Buffering || effectiveStatus == RadioPlaybackStatus.Reconnecting
    val isError = effectiveStatus == RadioPlaybackStatus.Error

    var isBarFocused by remember { mutableStateOf(false) }
    var isStopFocused by remember { mutableStateOf(false) }
    val stopRequester = remember { FocusRequester() }
    val barRequester = focusRequester ?: remember { FocusRequester() }

    // Gate infinite transition strictly on live/buffering so paused state runs zero animation ticks
    val dotAlpha = if (isLive || isBuffering) {
        val transition = rememberInfiniteTransition(label = "radio-mini-pulse")
        val a by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot-alpha"
        )
        a
    } else {
        0.55f
    }

    val statusColor = when (effectiveStatus) {
        RadioPlaybackStatus.Playing -> Color(0xFF00E676)
        RadioPlaybackStatus.Buffering, RadioPlaybackStatus.Reconnecting -> Color(0xFFFFB74D)
        RadioPlaybackStatus.Error -> Color(0xFFFF5252)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusLabel = when (effectiveStatus) {
        RadioPlaybackStatus.Playing -> strings.radioLive
        RadioPlaybackStatus.Buffering -> strings.radioConnecting
        RadioPlaybackStatus.Reconnecting -> strings.radioReconnecting
        RadioPlaybackStatus.Error -> strings.radioOfflineError
        else -> strings.radioPaused
    }

    Surface(
        onClick = onTogglePlayPause,
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(barRequester) else Modifier)
            .googleTvFocusable(
                shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
                focusedScale = 1.015f,
                onFocusChange = { isBarFocused = it }
            )
            .onKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (native.action == KeyEvent.ACTION_DOWN) {
                    when (native.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            onTogglePlayPause(); true
                        }
                        KeyEvent.KEYCODE_MEDIA_STOP -> {
                            onStop(); true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            runCatching { stopRequester.requestFocus() }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (onFocusUp != null) { onFocusUp(); true } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (onFocusDown != null) { onFocusDown(); true } else false
                        }
                        else -> false
                    }
                } else false
            },
        shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = when {
            isBarFocused -> BorderStroke(2.dp, Color.White)
            isLive -> BorderStroke(1.dp, accent.copy(alpha = 0.55f))
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
        },
        tonalElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = if (isLive) 0.16f else 0.08f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Station Artwork + Info with Crossfade on station switch
                AnimatedContent(
                    targetState = station,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                    label = "mini-bar-station",
                    modifier = Modifier.weight(1f)
                ) { st ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!st.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = st.logoUrl,
                                    contentDescription = st.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(42.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = statusColor.copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(statusColor.copy(alpha = dotAlpha))
                                        )
                                        Text(
                                            text = statusLabel,
                                            color = statusColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
                                        )
                                    }
                                }
                                Text(
                                    text = st.name.substringAfter("|").trim(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val subtitle = listOfNotNull(playbackState?.icyArtist, playbackState?.icyTitle)
                                .joinToString(" · ")
                                .ifBlank { st.genre }
                            Text(
                                text = subtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Active Sleep Timer Countdown Pill
                playbackState?.sleepTimerRemainingSec?.let { remSec ->
                    val mins = remSec / 60
                    val secs = remSec % 60
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "⏱ %d:%02d".format(mins, secs),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }

                // Draw-phase Equalizer or Buffering Spinner
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    AudioSpectrumVisualizer(
                        isPlaying = isLive,
                        barCount = 5,
                        barColor = accent,
                        secondaryColor = secondary,
                        modifier = Modifier
                            .width(30.dp)
                            .height(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Non-focusable Play/Pause visual indicator (whole bar handles toggle)
                Surface(
                    shape = CircleShape,
                    color = if (isBarFocused) accent else accent.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                isError -> Icons.Default.Refresh
                                isLive -> Icons.Default.Pause
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = if (isLive) strings.radioPause else strings.radioPlay,
                            tint = if (isBarFocused) GlzCardDefaults.onAccent(accent) else accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Dedicated Stop button reachable via DPAD_RIGHT
                Surface(
                    onClick = onStop,
                    modifier = Modifier
                        .size(36.dp)
                        .focusRequester(stopRequester)
                        .googleTvFocusable(
                            shape = CircleShape,
                            focusedScale = 1.1f,
                            onFocusChange = { isStopFocused = it }
                        )
                        .onKeyEvent { event ->
                            val native = event.nativeKeyEvent
                            if (native.action == KeyEvent.ACTION_DOWN) {
                                when (native.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        runCatching { barRequester.requestFocus() }
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        if (onFocusUp != null) { onFocusUp(); true } else false
                                    }
                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        if (onFocusDown != null) { onFocusDown(); true } else false
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    shape = CircleShape,
                    color = if (isStopFocused) Color(0xFFFF5252) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = if (isStopFocused) BorderStroke(2.dp, Color.White) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = strings.radioStop,
                            tint = if (isStopFocused) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
