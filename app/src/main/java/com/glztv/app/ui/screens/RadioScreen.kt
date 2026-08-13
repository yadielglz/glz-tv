@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.glztv.app.ui.screens

import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
        player.stop()
        player.clearMediaItems()
        playing = false
        status = "Stopped"
        GlzHubManager.reportActivity(prefs, "idle")
    }

    fun playStation(station: RadioStation) {
        selected = station
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
                if (isPlaying) status = "Live"
                else if (player.playbackState == Player.STATE_READY && selected != null) {
                    status = "Paused"
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playing = false
                status = "Station unavailable · choose another station"
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
    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("GLZ Radio", fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text(
                    "Live stations managed by GLZ Hub",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    "${stations.size} STATIONS",
                    Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Surface(
                modifier = Modifier.weight(1.15f).fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .9f)
            ) {
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LinearProgressIndicator(Modifier.width(220.dp))
                    }
                } else if (stations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(status, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stations, key = { it.code }) { station ->
                            RadioStationRow(station, selected?.code == station.code) {
                                playStation(station)
                            }
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.weight(.85f).fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    Modifier.fillMaxSize().padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AsyncImage(
                        model = selected?.logoUrl ?: R.drawable.ic_launcher,
                        contentDescription = selected?.name,
                        modifier = Modifier.size(150.dp).clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = .08f)),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        selected?.name ?: "Choose a station",
                        Modifier.padding(top = 24.dp),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        selected?.genre ?: "Browse the live station list",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .7f)
                    )
                    Text(
                        status.uppercase(Locale.ROOT),
                        Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    Row(
                        Modifier.padding(top = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Button(
                            enabled = selected != null,
                            onClick = {
                                if (playing) player.pause()
                                else if (player.mediaItemCount > 0) player.play()
                                else selected?.let(::playStation)
                            }
                        ) {
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null
                            )
                            Text(if (playing) "Pause" else "Play", Modifier.padding(start = 8.dp))
                        }
                        Button(
                            enabled = selected != null || player.mediaItemCount > 0,
                            onClick = ::stopRadio
                        ) {
                            Icon(Icons.Default.Stop, null)
                            Text("Stop", Modifier.padding(start = 8.dp))
                        }
                    }
                    Text(
                        "Stop ends playback completely.",
                        Modifier.padding(top = 18.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .6f),
                        fontSize = 12.sp
                    )
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
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(58.dp),
            color = Color.Black.copy(alpha = .78f),
            contentColor = Color.White
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(8.dp).background(MaterialTheme.colorScheme.secondary, CircleShape)
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
    onPlay: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val parts = station.name.split("|").map(String::trim)
    val title = parts.lastOrNull().orEmpty().ifBlank { station.name }
    val frequency = parts.takeIf { it.size > 1 }?.firstOrNull().orEmpty()
    Surface(
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onPlay).focusable(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            if (focused) 4.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.secondary else Color.Transparent
        ),
        color = when {
            focused -> MaterialTheme.colorScheme.primary
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (focused) {
            MaterialTheme.colorScheme.onPrimary
        } else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsyncImage(
                model = station.logoUrl ?: R.drawable.ic_launcher,
                contentDescription = null,
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = .08f)),
                contentScale = ContentScale.Fit
            )
            Column(Modifier.weight(1f)) {
                Text(
                    title, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOf(frequency, station.genre).filter(String::isNotBlank).joinToString(" · "),
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Radio,
                    "Playing station",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
