@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.glztv.app.ui.screens

import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.glztv.app.ui.components.tvFocusableWithPhysics
import com.glztv.app.ui.components.GlzPanel
import com.glztv.app.ui.components.GlzFocusCard
import com.glztv.app.ui.components.GlzCardDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil3.compose.AsyncImage
import com.glztv.app.BuildConfig
import com.glztv.app.GlzHubManager
import com.glztv.app.R
import com.glztv.app.RadioStation
import com.glztv.app.data.PreferencesRepository
import com.glztv.app.data.RadioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.Locale

@Composable
fun RadioScreen(
    prefs: SharedPreferences,
    client: OkHttpClient,
    onPlayingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember {
        RadioRepository(PreferencesRepository(context), client)
    }
    var stations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var selected by remember { mutableStateOf<RadioStation?>(null) }
    var loading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("Loading stations from GLZ Hub…") }
    var playing by remember { mutableStateOf(false) }
    var screensaverVisible by remember { mutableStateOf(false) }
    var screensaverRevision by remember { mutableStateOf(0) }
    var radioRetryAttempt by remember(selected?.code) { mutableStateOf(0) }
    val dataSourceFactory = remember {
        DefaultHttpDataSource.Factory().setUserAgent("GLZ-TV-Radio/${BuildConfig.VERSION_NAME}")
    }
    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)
                setWakeMode(C.WAKE_MODE_LOCAL)
            }
    }

    fun stopRadio() {
        radioRetryAttempt = 0
        player.stop()
        player.clearMediaItems()
        playing = false
        status = "Stopped"
        GlzHubManager.reportActivity(prefs, "idle")
    }

    fun playStation(station: RadioStation) {
        selected = station
        radioRetryAttempt = 0
        dataSourceFactory.setDefaultRequestProperties(station.requestHeaders)
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.genre)
            .apply { station.logoUrl?.let { setArtworkUri(Uri.parse(it)) } }
            .build()
        player.setMediaItem(
            MediaItem.Builder().setUri(station.streamUrl).setMediaMetadata(metadata).build()
        )
        player.prepare()
        player.play()
        status = "Connecting…"
        GlzHubManager.reportActivity(prefs, "radio", station.name)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                onPlayingChanged(isPlaying)
                if (isPlaying) {
                    radioRetryAttempt = 0
                    status = "Live"
                } else if (player.playbackState == Player.STATE_READY && selected != null) {
                    radioRetryAttempt = 0
                    status = "Paused"
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playing = false
                radioRetryAttempt += 1
                val nextDelaySec = minOf(15, 1 shl minOf(radioRetryAttempt - 1, 4))
                status = "Station offline · retrying in ${nextDelaySec}s (#$radioRetryAttempt)"
            }
        }
        player.addListener(listener)
        onDispose {
            onPlayingChanged(false)
            player.removeListener(listener)
            player.stop()
            player.release()
            GlzHubManager.reportActivity(prefs, "idle")
        }
    }

    LaunchedEffect(selected?.code, radioRetryAttempt) {
        val currentStation = selected ?: return@LaunchedEffect
        if (radioRetryAttempt > 0) {
            val backoffSec = minOf(15L, 1L shl minOf(radioRetryAttempt - 1, 4))
            delay(backoffSec * 1_000L)
            status = "Reconnecting to station (#$radioRetryAttempt)…"
            dataSourceFactory.setDefaultRequestProperties(currentStation.requestHeaders)
            val metadata = MediaMetadata.Builder()
                .setTitle(currentStation.name)
                .setArtist(currentStation.genre)
                .apply { currentStation.logoUrl?.let { setArtworkUri(Uri.parse(it)) } }
                .build()
            player.setMediaItem(
                MediaItem.Builder().setUri(currentStation.streamUrl).setMediaMetadata(metadata).build()
            )
            player.prepare()
            player.play()
        }
    }

    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { repository.load() } }
            .onSuccess { result ->
                stations = result.stations
                loading = false
                status = if (result.fromCache) {
                    "Saved station list · Hub temporarily unavailable"
                } else "Choose a station"
            }
            .onFailure {
                loading = false
                status = "Radio stations unavailable"
            }
    }

    LaunchedEffect(playing, selected?.code, screensaverRevision) {
        screensaverVisible = false
        if (playing && selected != null) {
            delay(15_000L)
            screensaverVisible = true
        }
    }

    Box(
        modifier.onPreviewKeyEvent { event ->
            if (screensaverVisible && event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                screensaverVisible = false
                screensaverRevision++
                true
            } else false
        }
    ) {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "GLZ Radio",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Live audio streams managed by GLZ Hub",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            ) {
                Text(
                    "${stations.size} STATIONS",
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GlzPanel(
                modifier = Modifier.weight(1.15f).fillMaxHeight(),
                shape = RoundedCornerShape(GlzCardDefaults.RadiusLarge)
            ) {
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LinearProgressIndicator(Modifier.width(220.dp), color = MaterialTheme.colorScheme.primary)
                    }
                } else if (stations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(status, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(stations, key = { it.code }) { station ->
                            RadioStationRow(
                                station = station,
                                selected = selected?.code == station.code,
                                isPlaying = playing && selected?.code == station.code
                            ) {
                                playStation(station)
                            }
                        }
                    }
                }
            }
            GlzPanel(
                modifier = Modifier.weight(.85f).fillMaxHeight(),
                shape = RoundedCornerShape(GlzCardDefaults.RadiusLarge)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                )
                            )
                        )
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val parts = (selected?.name ?: "").split("|").map(String::trim).filter(String::isNotBlank)
                        val displayTitle = if (parts.size > 1) parts.last() else (selected?.name ?: "Choose a station")
                        val displayTag = if (parts.size > 1) parts.first() else ""

                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            shadowElevation = 16.dp
                        ) {
                            AsyncImage(
                                model = selected?.logoUrl ?: R.drawable.ic_launcher,
                                contentDescription = selected?.name,
                                modifier = Modifier
                                    .size(150.dp)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }

                        if (displayTag.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text(
                                    displayTag,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            displayTitle,
                            Modifier.padding(top = if (displayTag.isNotBlank()) 8.dp else 16.dp),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            selected?.genre ?: "Browse the live station list",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (playing && selected != null) {
                            AudioSpectrumVisualizer(
                                isPlaying = true,
                                modifier = Modifier
                                    .padding(top = 14.dp)
                                    .width(68.dp)
                                    .height(24.dp),
                                barColor = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (playing) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 14.dp)
                        ) {
                            Text(
                                status.uppercase(Locale.ROOT),
                                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            Modifier.padding(top = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Button(
                                enabled = selected != null,
                                onClick = {
                                    if (playing) player.pause()
                                    else if (player.mediaItemCount > 0) player.play()
                                    else selected?.let(::playStation)
                                },
                                modifier = Modifier.tvFocusableWithPhysics(
                                    shape = RoundedCornerShape(18.dp),
                                    focusedScale = 1.08f,
                                    glowColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null
                                )
                                Text(if (playing) "Pause" else "Play", Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                enabled = selected != null || player.mediaItemCount > 0,
                                onClick = ::stopRadio,
                                modifier = Modifier.tvFocusableWithPhysics(
                                    shape = RoundedCornerShape(18.dp),
                                    focusedScale = 1.08f,
                                    glowColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Text("Stop", Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
        if (screensaverVisible && playing) {
            selected?.let { station ->
                RadioNowPlayingScreensaver(
                    station = station,
                    onDismiss = {
                        screensaverVisible = false
                        screensaverRevision++
                    }
                )
            }
        }
    }
}

@Composable
fun AudioSpectrumVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    barColor: Color = MaterialTheme.colorScheme.secondary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio-spectrum")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { index ->
            val duration = remember(index) { 320 + (index * 65) % 400 }
            val heightPercent by if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = 0.18f,
                    targetValue = 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = duration, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar-$index"
                )
            } else {
                remember { mutableStateOf(0.18f) }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(heightPercent)
                    .background(barColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun RadioNowPlayingScreensaver(
    station: RadioStation,
    onDismiss: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "radio-logo-fade")
    val logoAlpha by transition.animateFloat(
        initialValue = .32f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radio-logo-alpha"
    )
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF18344A), Color(0xFF07101D), Color.Black)
                )
            )
            .clickable(onClick = onDismiss)
    ) {
        AsyncImage(
            model = station.logoUrl ?: R.drawable.ic_launcher,
            contentDescription = station.name,
            modifier = Modifier.align(Alignment.Center).size(280.dp).alpha(logoAlpha),
            contentScale = ContentScale.Fit
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(64.dp),
            color = Color.Black.copy(alpha = .78f),
            contentColor = Color.White
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioSpectrumVisualizer(
                    isPlaying = true,
                    modifier = Modifier.width(36.dp).height(24.dp)
                )
                Text(
                    "GLZ RADIO",
                    Modifier.padding(start = 12.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
                Text(
                    "  ·  ${station.name}",
                    Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("PLAYING", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RadioStationRow(
    station: RadioStation,
    selected: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    val rawName = station.name.trim()
    val parts = rawName.split("|").map(String::trim).filter(String::isNotBlank)
    val title = if (parts.size > 1) parts.last() else rawName
    val tag = if (parts.size > 1) parts.first() else ""
    val activeColor = MaterialTheme.colorScheme.primary

    GlzFocusCard(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        accent = activeColor,
        shape = RoundedCornerShape(GlzCardDefaults.RadiusMedium),
        focusedScale = 1.04f
    ) { focused ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.size(56.dp)
            ) {
                AsyncImage(
                    model = station.logoUrl ?: R.drawable.ic_launcher,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Column(Modifier.weight(1f)) {
                if (tag.isNotBlank()) {
                    Text(
                        tag.uppercase(Locale.ROOT),
                        color = if (focused) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        else if (selected) activeColor
                        else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    title,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    station.genre.ifBlank { "Radio Stream" },
                    color = if (focused) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isPlaying) {
                        AudioSpectrumVisualizer(
                            isPlaying = true,
                            modifier = Modifier
                                .width(22.dp)
                                .height(16.dp),
                            barColor = if (focused) MaterialTheme.colorScheme.onPrimary else activeColor
                        )
                    }
                    Icon(
                        Icons.Default.Radio,
                        "Playing station",
                        tint = if (focused) MaterialTheme.colorScheme.onPrimary else activeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
