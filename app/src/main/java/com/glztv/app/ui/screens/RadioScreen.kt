package com.glztv.app.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.sin
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.RadioStation
import com.glztv.app.player.RadioPlaybackState
import com.glztv.app.player.RadioPlaybackStatus
import com.glztv.app.player.RadioPlayerController
import com.glztv.app.ui.i18n.LocalGlzStrings
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.DtvSegmentedStripBar
import com.glztv.app.ui.theme.LocalTvViewport
import java.util.Locale

private const val GENRE_ALL = "ALL"
private const val GENRE_FAVORITES = "★ FAVORITES"

/**
 * DirecTV HD Full-Screen Radio Experience (v1.48.4.10).
 *
 * - Shared DirecTV HD Top Command Bar (`Radio` active, no legacy left rail).
 * - DirecTV Segmented Genre Filter Strip + Refresh.
 * - Glossy Dark Now Playing Broadcast Deck with Transport Controls & Sleep Timer.
 * - Dense Hairline-Bordered Broadcast Station Grid with Pure-Black + #1990FF Focus Bar.
 */
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
    topBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalGlzStrings.current
    val viewport = LocalTvViewport.current
    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(28.dp)

    var selectedGenre by remember { mutableStateOf(GENRE_ALL) }
    val genres = remember(stations, favoriteStationKeys) {
        val raw = stations.mapNotNull { it.genre.trim().takeIf { g -> g.isNotBlank() } }
            .distinctBy { it.lowercase(Locale.ROOT) }
        val hasFavs = stations.any { RadioPlayerController.stationKey(it) in favoriteStationKeys }
        buildList {
            add(GENRE_ALL)
            if (hasFavs) add(GENRE_FAVORITES)
            addAll(raw)
        }
    }

    val filteredStations = remember(stations, selectedGenre, favoriteStationKeys) {
        val base = when (selectedGenre) {
            GENRE_ALL -> stations
            GENRE_FAVORITES -> stations.filter { RadioPlayerController.stationKey(it) in favoriteStationKeys }
            else -> stations.filter { it.genre.equals(selectedGenre, ignoreCase = true) }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DtvChrome.ScreenBg)
            .onPreviewKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                if (native.action == KeyEvent.ACTION_DOWN) {
                    when (native.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { onTogglePlayPause(); true }
                        KeyEvent.KEYCODE_MEDIA_STOP -> { onStop(); true }
                        KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_CHANNEL_UP -> { onNextStation(); true }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_CHANNEL_DOWN -> { onPreviousStation(); true }
                        else -> false
                    }
                } else false
            }
    ) {
        // 1. Shared DirecTV HD Top Command Bar
        topBar?.invoke()

        // 2. DirecTV Segmented Genre Filter Strip + Refresh
        DtvSegmentedStripBar(
            items = genres,
            selectedItem = selectedGenre,
            onSelect = { selectedGenre = it },
            trailing = {
                DtvRadioTransportButton(
                    icon = Icons.Default.Refresh,
                    label = strings.refresh,
                    active = false,
                    compact = true,
                    onClick = onRefreshStations
                )
            }
        )

        // 3. Glossy Dark Now Playing Broadcast Deck
        val deckHeight = if (viewport.isCompactHeight) 118.dp else 132.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(deckHeight)
                .background(DtvChrome.BannerGradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPad, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Station Artwork + Broadcast Metadata
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (viewport.isCompactHeight) 74.dp else 88.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF12161E))
                            .border(
                                width = 1.dp,
                                color = if (playbackState.isPlaying) DtvChrome.ElectricBlue else DtvChrome.Divider,
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!activeStation?.logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = activeStation?.logoUrl,
                                contentDescription = activeStation?.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Radio,
                                contentDescription = null,
                                tint = DtvChrome.ElectricBlue,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val statusLabel = when (playbackState.status) {
                            RadioPlaybackStatus.Playing -> "ON AIR"
                            RadioPlaybackStatus.Buffering, RadioPlaybackStatus.Reconnecting -> "CONNECTING..."
                            RadioPlaybackStatus.Paused -> "PAUSED"
                            RadioPlaybackStatus.Error -> "STREAM ERROR"
                            RadioPlaybackStatus.Idle -> if (loading) "LOADING CATALOG..." else "${filteredStations.size} STATIONS READY"
                        }
                        val sleepSecs = playbackState.sleepTimerRemainingSec ?: 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                color = if (playbackState.status == RadioPlaybackStatus.Error) DtvChrome.LiveRed else DtvChrome.ElectricBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            if (activeStation != null && activeStation.bitrateKbps > 0) {
                                Text(
                                    text = "·  ${activeStation.bitrateKbps} KBPS",
                                    color = DtvChrome.MutedText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (sleepSecs > 0) {
                                val mins = (sleepSecs + 59) / 60
                                Text(
                                    text = "·  SLEEP ${mins}m",
                                    color = DtvChrome.Gold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = activeStation?.name ?: "Select a Radio Station",
                            color = Color.White,
                            fontSize = if (viewport.isCompactHeight) 20.sp else 23.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val icyLine = listOfNotNull(
                            playbackState.icyArtist?.takeIf { it.isNotBlank() },
                            playbackState.icyTitle?.takeIf { it.isNotBlank() }
                        ).joinToString(" — ").takeIf { it.isNotBlank() }
                        val sub = icyLine
                            ?: activeStation?.genre?.takeIf { it.isNotBlank() }
                            ?: if (fromCache) strings.radioSavedCache else strings.radioSubtitle
                        Text(
                            text = sub,
                            color = DtvChrome.SecondaryText,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: DirecTV Transport Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DtvRadioTransportButton(
                        icon = Icons.Default.SkipPrevious,
                        label = null,
                        onClick = onPreviousStation
                    )
                    DtvRadioTransportButton(
                        icon = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        label = if (playbackState.isPlaying) "Pause" else "Play",
                        active = playbackState.isPlaying,
                        onClick = {
                            if (playbackState.status == RadioPlaybackStatus.Error) onRetry()
                            else if (activeStation == null && filteredStations.isNotEmpty()) onPlayStation(filteredStations.first())
                            else onTogglePlayPause()
                        }
                    )
                    DtvRadioTransportButton(
                        icon = Icons.Default.SkipNext,
                        label = null,
                        onClick = onNextStation
                    )
                    DtvRadioTransportButton(
                        icon = Icons.Default.Stop,
                        label = null,
                        onClick = onStop
                    )
                    if (activeStation != null) {
                        val isFav = RadioPlayerController.stationKey(activeStation) in favoriteStationKeys
                        DtvRadioTransportButton(
                            icon = if (isFav) Icons.Default.Star else Icons.Outlined.StarOutline,
                            label = null,
                            active = isFav,
                            activeColor = DtvChrome.Gold,
                            onClick = { onToggleFavorite(activeStation) }
                        )
                    }
                    val sleepRem = playbackState.sleepTimerRemainingSec ?: 0
                    DtvRadioTransportButton(
                        icon = Icons.Default.Timer,
                        label = if (sleepRem > 0) "${(sleepRem + 59) / 60}m" else "Sleep",
                        active = sleepRem > 0,
                        onClick = onCycleSleepTimer
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.BottomCenter)
                    .background(DtvChrome.Divider)
            )
        }

        // 4. Broadcast Station Grid
        if (loading && filteredStations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DtvChrome.ElectricBlue, strokeWidth = 2.5.dp)
            }
        } else if (filteredStations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No radio stations available.",
                    color = DtvChrome.SecondaryText,
                    fontSize = 14.sp
                )
            }
        } else {
            val columns = if (viewport.widthDp >= 1000.dp) 3 else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    filteredStations,
                    key = { idx, s -> "dtv-rad-$idx-${s.code}-${s.streamUrl}" }
                ) { index, station ->
                    val isCurrent = activeStation?.streamUrl == station.streamUrl
                    val isFav = RadioPlayerController.stationKey(station) in favoriteStationKeys
                    val focusReq = when {
                        isCurrent -> activeStationFocus
                        index == 0 -> firstStationFocus
                        else -> null
                    }
                    DtvRadioStationTile(
                        station = station,
                        isActive = isCurrent && playbackState.isPlaying,
                        isSelected = isCurrent,
                        isFavorite = isFav,
                        modifier = if (focusReq != null) Modifier.focusRequester(focusReq) else Modifier,
                        onClick = { onPlayStation(station) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DtvRadioStationTile(
    station: RadioStation,
    isActive: Boolean,
    isSelected: Boolean,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(
                when {
                    isFocused -> Color.Black
                    isSelected -> DtvChrome.ElectricBlue.copy(alpha = 0.14f)
                    else -> Color(0xFF12161D)
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = when {
                    isFocused -> DtvChrome.ElectricBlue
                    isSelected -> DtvChrome.ElectricBlue.copy(alpha = 0.6f)
                    else -> DtvChrome.CardBorder
                },
                shape = RoundedCornerShape(3.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isFocused || isSelected) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(DtvChrome.ElectricBlue)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1A1F28)),
                contentAlignment = Alignment.Center
            ) {
                if (!station.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = station.logoUrl,
                        contentDescription = station.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Radio,
                        contentDescription = null,
                        tint = DtvChrome.ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = station.name,
                    color = if (isFocused || isSelected) Color.White else DtvChrome.PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = if (isFocused || isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildList {
                        if (station.genre.isNotBlank()) add(station.genre)
                        if (station.bitrateKbps > 0) add("${station.bitrateKbps}k")
                    }.joinToString(" · "),
                    color = DtvChrome.SecondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isActive) {
                Text(
                    text = "ON AIR",
                    color = DtvChrome.ElectricBlueSoft,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (isFavorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = DtvChrome.Gold,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DtvRadioTransportButton(
    icon: ImageVector,
    label: String?,
    active: Boolean = false,
    activeColor: Color = DtvChrome.ElectricBlue,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(
                when {
                    isFocused -> Color.Black
                    active -> activeColor.copy(alpha = 0.22f)
                    else -> Color(0xFF171B22)
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = when {
                    isFocused -> DtvChrome.ElectricBlue
                    active -> activeColor
                    else -> DtvChrome.Divider
                },
                shape = RoundedCornerShape(3.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = if (label == null) 10.dp else if (compact) 10.dp else 13.dp,
                vertical = if (compact) 5.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                isFocused -> Color.White
                active -> activeColor
                else -> DtvChrome.PrimaryText
            },
            modifier = Modifier.size(if (compact) 14.dp else 17.dp)
        )
        if (label != null) {
            Text(
                text = label,
                color = if (isFocused) Color.White else DtvChrome.PrimaryText,
                fontSize = if (compact) 11.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

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
