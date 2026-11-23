package com.glztv.app.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.R
import com.glztv.app.RadioStation
import com.glztv.app.player.RadioPlaybackState
import com.glztv.app.player.RadioPlaybackStatus
import com.glztv.app.player.RadioPlayerController
import com.glztv.app.ui.components.GlzCardDefaults
import com.glztv.app.ui.components.GlzFocusCard
import com.glztv.app.ui.components.GlzPanel
import com.glztv.app.ui.components.GlzSectionActionButton
import com.glztv.app.ui.components.GlzSectionHeader
import com.glztv.app.ui.components.GlzSectionPill
import com.glztv.app.ui.components.GlzSectionShell
import com.glztv.app.ui.i18n.LocalGlzStrings
import com.glztv.app.ui.theme.LocalTvViewport
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

private const val GENRE_FAVORITES = "__FAVORITES__"

@Composable
fun RadioScreen(
    stations: List<RadioStation>,
    playbackState: RadioPlaybackState,
    loading: Boolean,
    fromCache: Boolean,
    favoriteStationKeys: Set<String> = emptySet(),
    onPlayStation: (RadioStation) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onNextStation: () -> Unit,
    onPreviousStation: () -> Unit,
    onToggleFavorite: (RadioStation) -> Unit = {},
    onCycleSleepTimer: () -> Unit = {},
    onRefreshStations: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalGlzStrings.current
    val viewport = LocalTvViewport.current
    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    var selectedGenre by remember { mutableStateOf<String?>(null) }
    val genres = remember(stations) {
        stations.mapNotNull { it.genre.trim().takeIf { g -> g.isNotBlank() } }
            .distinctBy { it.lowercase(Locale.ROOT) }
    }
    val hasFavorites = remember(stations, favoriteStationKeys) {
        stations.any { RadioPlayerController.stationKey(it) in favoriteStationKeys }
    }
    val filteredStations = remember(stations, selectedGenre, favoriteStationKeys) {
        val g = selectedGenre
        val base = when {
            g == GENRE_FAVORITES -> stations.filter { RadioPlayerController.stationKey(it) in favoriteStationKeys }
            g.isNullOrBlank() -> stations
            else -> stations.filter { it.genre.equals(g, ignoreCase = true) }
        }
        base.sortedByDescending { RadioPlayerController.stationKey(it) in favoriteStationKeys }
    }

    val activeStation = playbackState.station
    val firstStationFocus = remember { FocusRequester() }
    val activeStationFocus = remember { FocusRequester() }

    LaunchedEffect(stations.isNotEmpty(), activeStation?.code) {
        if (stations.isNotEmpty()) {
            val target = if (activeStation != null && filteredStations.any { it.streamUrl == activeStation.streamUrl }) {
                activeStationFocus
            } else {
                firstStationFocus
            }
            runCatching { target.requestFocus() }
        }
    }

    val subtitleText = when {
        loading && stations.isEmpty() -> strings.radioLoading
        fromCache -> strings.radioSavedCache
        else -> strings.radioSubtitle
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                if (native.action == KeyEvent.ACTION_DOWN) {
                    when (native.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            onTogglePlayPause(); true
                        }
                        KeyEvent.KEYCODE_MEDIA_STOP -> {
                            onStop(); true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT,
                        KeyEvent.KEYCODE_CHANNEL_UP -> {
                            onNextStation(); true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                        KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            onPreviousStation(); true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        GlzSectionShell {
            GlzSectionHeader(
                title = strings.radioTitle,
                subtitle = subtitleText,
                icon = Icons.Default.Radio,
                accent = accent,
                trailing = {
                    GlzSectionPill(
                        text = "${filteredStations.size} ${strings.radioStationsCount}",
                        accent = accent,
                        dotColor = if (playbackState.isPlaying) Color(0xFF00E676) else null
                    )
                    GlzSectionActionButton(
                        label = strings.refresh,
                        icon = Icons.Default.Refresh,
                        onClick = onRefreshStations,
                        accent = accent
                    )
                }
            )

            // Genre & Favorites filter row
            if (genres.size > 1 || hasFavorites) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item(key = "genre_all") {
                        GlzFocusCard(
                            onClick = { selectedGenre = null },
                            selected = selectedGenre == null,
                            shape = RoundedCornerShape(999.dp),
                            focusedScale = 1.04f,
                            contentModifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = strings.radioAllGenres,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (hasFavorites) {
                        item(key = "genre_favorites") {
                            GlzFocusCard(
                                onClick = { selectedGenre = GENRE_FAVORITES },
                                selected = selectedGenre == GENRE_FAVORITES,
                                shape = RoundedCornerShape(999.dp),
                                focusedScale = 1.04f,
                                contentModifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "★ ${strings.favorites}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    items(genres, key = { "genre_$it" }) { genre ->
                        GlzFocusCard(
                            onClick = { selectedGenre = genre },
                            selected = selectedGenre.equals(genre, ignoreCase = true),
                            shape = RoundedCornerShape(999.dp),
                            focusedScale = 1.04f,
                            contentModifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = genre,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (viewport.isCompactHeight) 12.dp else 16.dp)
            ) {
                // Left Pane: Station List
                GlzPanel(
                    modifier = Modifier
                        .weight(1.12f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(GlzCardDefaults.RadiusMedium),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    when {
                        loading && filteredStations.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.width(220.dp),
                                        color = accent
                                    )
                                    Text(
                                        text = strings.radioLoading,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        filteredStations.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = strings.radioUnavailable,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(
                                    items = filteredStations,
                                    key = { idx, st -> st.code.ifBlank { "${st.streamUrl}#$idx" } }
                                ) { index, station ->
                                    val isSelected = activeStation?.streamUrl == station.streamUrl &&
                                        activeStation.code == station.code
                                    val isFav = RadioPlayerController.stationKey(station) in favoriteStationKeys
                                    val rowModifier = when {
                                        isSelected -> Modifier.focusRequester(activeStationFocus)
                                        index == 0 -> Modifier.focusRequester(firstStationFocus)
                                        else -> Modifier
                                    }
                                    RadioStationRow(
                                        station = station,
                                        selected = isSelected,
                                        isFavorite = isFav,
                                        isPlaying = isSelected && playbackState.isPlaying,
                                        isBuffering = isSelected && playbackState.isBuffering,
                                        onClick = { onPlayStation(station) },
                                        modifier = rowModifier
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Pane: Now-Playing Hero with Ambient Aura & Smooth Canvas Visualizer
                GlzPanel(
                    modifier = Modifier
                        .weight(0.88f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(GlzCardDefaults.RadiusMedium),
                    border = BorderStroke(
                        1.dp,
                        if (playbackState.isPlaying) accent.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                ) {
                    RadioNowPlayingHero(
                        playbackState = playbackState,
                        hasStations = filteredStations.isNotEmpty(),
                        isActiveFavorite = activeStation != null && RadioPlayerController.stationKey(activeStation) in favoriteStationKeys,
                        accent = accent,
                        secondary = secondary,
                        onTogglePlayPause = {
                            if (playbackState.station != null) onTogglePlayPause()
                            else filteredStations.firstOrNull()?.let(onPlayStation)
                        },
                        onStop = onStop,
                        onRetry = onRetry,
                        onPrevious = onPreviousStation,
                        onNext = onNextStation,
                        onToggleFavorite = { activeStation?.let(onToggleFavorite) },
                        onCycleSleepTimer = onCycleSleepTimer
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioNowPlayingHero(
    playbackState: RadioPlaybackState,
    hasStations: Boolean,
    isActiveFavorite: Boolean,
    accent: Color,
    secondary: Color,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleSleepTimer: () -> Unit,
) {
    val strings = LocalGlzStrings.current
    val viewport = LocalTvViewport.current
    val station = playbackState.station
    val isPlaying = playbackState.isPlaying
    val isBuffering = playbackState.isBuffering
    val isError = playbackState.status == RadioPlaybackStatus.Error

    // Breathing aura only animates while live/buffering to avoid idle ticks
    val auraPulse = if (isPlaying || isBuffering) {
        val transition = rememberInfiniteTransition(label = "radio-hero-aura")
        val value by transition.animateFloat(
            initialValue = 0.78f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (isBuffering) 900 else 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aura-pulse"
        )
        value
    } else {
        0.55f
    }

    val statusText = when (playbackState.status) {
        RadioPlaybackStatus.Playing -> strings.radioLive
        RadioPlaybackStatus.Paused -> strings.radioPaused
        RadioPlaybackStatus.Buffering -> strings.radioConnecting
        RadioPlaybackStatus.Reconnecting -> "${strings.radioReconnecting} (#${playbackState.retryAttempt})"
        RadioPlaybackStatus.Error -> strings.radioOfflineError
        RadioPlaybackStatus.Idle -> strings.radioStopped
    }

    val statusColor = when (playbackState.status) {
        RadioPlaybackStatus.Playing -> Color(0xFF00E676)
        RadioPlaybackStatus.Buffering, RadioPlaybackStatus.Reconnecting -> Color(0xFFFFB74D)
        RadioPlaybackStatus.Error -> Color(0xFFFF5252)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val sleepLabel = playbackState.sleepTimerRemainingSec?.let { sec ->
        "%d:%02d".format(sec / 60, sec % 60)
    } ?: strings.off

    val artworkSize = if (viewport.isCompactHeight) 104.dp else 128.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = if (isPlaying) 0.20f * auraPulse else 0.10f),
                        secondary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                )
            )
            .padding(if (viewport.isCompactHeight) 14.dp else 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Status + Bitrate + Favorite/Sleep Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlzSectionPill(
                    text = statusText,
                    accent = statusColor,
                    dotColor = statusColor
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (station != null && station.bitrateKbps > 0) {
                        GlzSectionPill(
                            text = "${station.bitrateKbps}K",
                            accent = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    GlzSectionActionButton(
                        label = sleepLabel,
                        icon = Icons.Default.Timer,
                        primary = playbackState.sleepTimerRemainingSec != null,
                        onClick = onCycleSleepTimer,
                        accent = accent
                    )
                }
            }

            // Center Artwork + Station Metadata with Smooth Crossfade
            AnimatedContent(
                targetState = station,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                label = "radio-station-hero",
                modifier = Modifier.fillMaxWidth()
            ) { targetStation ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (viewport.isCompactHeight) 6.dp else 9.dp)
                ) {
                    // Artwork with breathing radial aura + buffering ring
                    Box(
                        modifier = Modifier.size(artworkSize + 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying || isBuffering) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val r = (size.minDimension / 2f) * auraPulse
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            accent.copy(alpha = 0.35f),
                                            secondary.copy(alpha = 0.12f),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = r.coerceAtLeast(1f)
                                    ),
                                    radius = r
                                )
                            }
                        }

                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(artworkSize + 12.dp),
                                color = accent,
                                trackColor = accent.copy(alpha = 0.15f),
                                strokeWidth = 3.dp
                            )
                        }

                        Surface(
                            modifier = Modifier.size(artworkSize),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            border = BorderStroke(
                                width = if (isPlaying) 2.dp else 1.dp,
                                color = if (isPlaying) accent.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        ) {
                            if (targetStation != null) {
                                AsyncImage(
                                    model = targetStation.logoUrl ?: R.drawable.ic_launcher,
                                    contentDescription = targetStation.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Radio,
                                        contentDescription = null,
                                        tint = accent.copy(alpha = 0.7f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (targetStation != null) {
                        val parts = targetStation.name.split("|", limit = 2)
                        val tag = if (parts.size == 2) parts[0].trim() else null
                        val cleanTitle = if (parts.size == 2) parts[1].trim() else targetStation.name

                        if (!tag.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = accent.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = tag.uppercase(Locale.US),
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = cleanTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = if (viewport.isCompactHeight) 18.sp else 21.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        // ICY Live Track Info or Genre Subtitle
                        val trackLine = listOfNotNull(playbackState.icyArtist, playbackState.icyTitle)
                            .joinToString(" · ")
                            .ifBlank { targetStation.genre }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (playbackState.icyTitle != null || playbackState.icyArtist != null) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = trackLine,
                                color = if (playbackState.icyTitle != null) accent
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = strings.radioChooseStation,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Always-mounted Draw-Phase Canvas Visualizer (smooth decay, zero layout jump)
            AudioSpectrumVisualizer(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                barCount = 14,
                barColor = accent,
                secondaryColor = secondary,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(if (viewport.isCompactHeight) 26.dp else 34.dp)
            )

            // Transport Controls (Prev · Play/Pause or Retry · Next · Fav · Stop)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlzSectionActionButton(
                    label = strings.radioPrevious,
                    icon = Icons.Default.SkipPrevious,
                    onClick = onPrevious,
                    enabled = hasStations,
                    accent = accent
                )
                if (isError) {
                    GlzSectionActionButton(
                        label = strings.radioRetry,
                        icon = Icons.Default.Refresh,
                        onClick = onRetry,
                        primary = true,
                        accent = accent
                    )
                } else {
                    GlzSectionActionButton(
                        label = if (isPlaying) strings.radioPause else strings.radioPlay,
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = onTogglePlayPause,
                        primary = true,
                        enabled = hasStations,
                        accent = accent
                    )
                }
                GlzSectionActionButton(
                    label = strings.radioNext,
                    icon = Icons.Default.SkipNext,
                    onClick = onNext,
                    enabled = hasStations,
                    accent = accent
                )
                if (station != null) {
                    GlzSectionActionButton(
                        label = if (isActiveFavorite) "★" else "☆",
                        icon = if (isActiveFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        primary = isActiveFavorite,
                        onClick = onToggleFavorite,
                        accent = Color(0xFFFFD54F)
                    )
                    GlzSectionActionButton(
                        label = strings.radioStop,
                        icon = Icons.Default.Stop,
                        onClick = onStop,
                        accent = Color(0xFFFF6E6E)
                    )
                }
            }
        }
    }
}

/**
 * Draw-phase Canvas audio spectrum visualizer.
 * Runs zero frame ticks when idle/paused and smoothly decays to resting baseline.
 */
@Composable
fun AudioSpectrumVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isBuffering: Boolean = false,
    barCount: Int = 12,
    barColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val active = isPlaying || isBuffering
    val phase = if (active) {
        val transition = rememberInfiniteTransition(label = "AudioSpectrumPhase")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (isBuffering) 1600 else 1100,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        p
    } else {
        0f
    }

    val energy by animateFloatAsState(
        targetValue = when {
            isPlaying -> 1f
            isBuffering -> 0.42f
            else -> 0.14f
        },
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "spectrum-energy"
    )

    Canvas(modifier = modifier) {
        val count = barCount.coerceAtLeast(3)
        val gap = (size.width / (count * 3.2f)).coerceIn(2.dp.toPx(), 5.dp.toPx())
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2.dp.toPx())
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until count) {
            val freq = 1.0f + (i % 3) * 0.45f
            val offset = i * 0.65f
            val wave = if (active) {
                0.5f + 0.5f * sin(phase * freq + offset)
            } else {
                0.2f + 0.15f * sin(i * 0.9f)
            }
            val heightFrac = (0.14f + wave * 0.86f * energy).coerceIn(0.12f, 1.0f)
            val barH = size.height * heightFrac
            val x = i * (barWidth + gap)
            val y = size.height - barH
            val blend = i.toFloat() / (count - 1).coerceAtLeast(1)
            val color = Color(
                red = barColor.red + (secondaryColor.red - barColor.red) * blend * 0.45f,
                green = barColor.green + (secondaryColor.green - barColor.green) * blend * 0.45f,
                blue = barColor.blue + (secondaryColor.blue - barColor.blue) * blend * 0.45f,
                alpha = if (isPlaying) 0.92f else 0.50f
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = radius
            )
        }
    }
}

@Composable
private fun RadioStationRow(
    station: RadioStation,
    selected: Boolean,
    isFavorite: Boolean = false,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val parts = station.name.split("|", limit = 2)
    val tag = if (parts.size == 2) parts[0].trim() else null
    val title = if (parts.size == 2) parts[1].trim() else station.name

    GlzFocusCard(
        onClick = onClick,
        selected = selected,
        shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
        focusedScale = 1.03f,
        modifier = modifier.fillMaxWidth(),
        contentModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) { focused ->
        val ink = when {
            focused -> Color(0xFF0B0D0F)
            selected -> accent
            else -> Color(0xFFF1F3F4)
        }
        val secondaryInk = when {
            focused -> Color(0xFF0B0D0F).copy(alpha = 0.72f)
            else -> Color(0xFF9AA0A6)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = station.logoUrl ?: R.drawable.ic_launcher,
                contentDescription = station.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!tag.isNullOrBlank()) {
                    Text(
                        text = tag.uppercase(Locale.US),
                        color = if (focused) ink.copy(alpha = 0.85f) else accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    color = ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(station.genre)
                        if (station.bitrateKbps > 0) append(" · ${station.bitrateKbps}k")
                    },
                    color = secondaryInk,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (focused) ink else Color(0xFFFFD54F),
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (selected) {
                    if (isPlaying || isBuffering) {
                        AudioSpectrumVisualizer(
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            barCount = 4,
                            barColor = ink,
                            secondaryColor = ink,
                            modifier = Modifier
                                .width(26.dp)
                                .height(18.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
