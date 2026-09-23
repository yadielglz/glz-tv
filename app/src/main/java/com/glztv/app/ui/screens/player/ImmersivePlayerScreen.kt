package com.glztv.app.ui.screens.player

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.view.KeyEvent
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.glztv.app.BuildConfig
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.GlzHubManager
import com.glztv.app.Programme
import com.glztv.app.RadioCatalogManager
import com.glztv.app.RadioStation
import com.glztv.app.data.PreferencesRepository
import com.glztv.app.model.EntertainmentApp
import com.glztv.app.player.PlaybackControlState
import com.glztv.app.player.PlaybackDiagnostics
import com.glztv.app.player.PlaybackDiagnosticsPanel
import com.glztv.app.player.PlaybackErrorCategorizer
import com.glztv.app.player.PlaybackErrorCategory
import com.glztv.app.player.PlaybackPerformance
import com.glztv.app.player.SpeedTestResult
import com.glztv.app.player.TrackOption
import com.glztv.app.player.TrackPreferenceManager
import com.glztv.app.ui.components.ChannelLogo
import com.glztv.app.ui.components.tvFocusableWithPhysics
import com.glztv.app.ui.navigation.panelEnter
import com.glztv.app.ui.navigation.panelExit
import com.glztv.app.ui.screens.guide.DrawerSectionLabel
import com.glztv.app.ui.screens.guide.TvOptionButton
import com.glztv.app.ui.screens.home.AdaptiveAppIcon
import com.glztv.app.ui.screens.home.findAppLaunchIntent
import com.glztv.app.ui.screens.home.launchEntertainmentApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val PREFS = "glz_tv_prefs"
private const val PANEL_ANIM_IN = 240
private const val SPORTS_BAR_KIOSK_CHANNEL_ID = "sports_bar_kiosk_channel_id"
private const val SPORTS_BAR_KIOSK_RADIO_CODE = "sports_bar_kiosk_radio_code"

/** Min position advance (ms) over a 1 s tick that counts as "still playing". */
private const val PLAYBACK_PROGRESS_EPSILON_MS = 250L
/** Continuous stall (ms) before rolling over to the next source. */
private const val PLAYBACK_STALL_LIMIT_MS = 12_000L
/** Delay (ms) before re-probing when the last source is also dead. */
private const val DEAD_SOURCE_REPROBE_MS = 30_000L

enum class PlayerDrawer { None, Channels, Services, Recent }

@Composable
fun ImmersivePlayerScreen(
    channel: Channel,
    channels: List<Channel>,
    guide: EpgGuide,
    captionsEnabled: Boolean,
    captionLanguage: String,
    onCaptionsChanged: (Boolean, String) -> Unit,
    osdTimeoutSeconds: Int = 8,
    entertainmentApps: List<EntertainmentApp>,
    recentChannels: List<Channel>,
    speedTestResult: SpeedTestResult? = null,
    onRunSpeedTest: ((String) -> Unit)? = null,
    onPreviousChannel: () -> Unit,
    onAddToMultiView: (Channel) -> Unit,
    onTune: (Channel) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
    val client = remember { OkHttpClient.Builder().build() }
    val trackPreferences = remember { TrackPreferenceManager(prefs) }
    val playbackControls = remember { PlaybackControlState() }
    val scope = rememberCoroutineScope()
    var drawer by remember { mutableStateOf(PlayerDrawer.None) }
    var showOsd by remember { mutableStateOf(false) }
    var selectingMultiViewChannel by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    val playerFocus = remember { FocusRequester() }
    val selectedChannelFocus = remember { FocusRequester() }
    val firstServiceFocus = remember { FocusRequester() }
    val selectedIndex = channels.indexOfFirst { it.id == channel.id }.coerceAtLeast(0)
    val channelListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - 2).coerceAtLeast(0)
    )
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val channelProgrammes = guide.forChannel(channel)
    val currentProgramme = channelProgrammes
        .firstOrNull { it.startMillis <= now && it.endMillis > now }
    val nextProgramme = channelProgrammes
        .firstOrNull { it.startMillis >= (currentProgramme?.endMillis ?: now) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(showOsd, channel.id, osdTimeoutSeconds) {
        if (showOsd) {
            delay(osdTimeoutSeconds * 1_000L)
            showOsd = false
        }
    }
    LaunchedEffect(channel.id) {
        showOsd = false
    }

    fun stepChannel(direction: Int) {
        val currentIndex = channels.indexOfFirst { it.id == channel.id }
        if (currentIndex >= 0 && channels.isNotEmpty()) {
            onTune(channels[(currentIndex + direction + channels.size) % channels.size])
        }
    }

    BackHandler {
        if (drawer != PlayerDrawer.None) drawer = PlayerDrawer.None else onExit()
    }
    LaunchedEffect(channel.id, drawer) {
        when (drawer) {
            PlayerDrawer.None -> playerFocus.requestFocus()
            PlayerDrawer.Channels -> {
                runCatching { channelListState.scrollToItem((selectedIndex - 2).coerceAtLeast(0)) }
                delay(PANEL_ANIM_IN.toLong())
                runCatching { selectedChannelFocus.requestFocus() }
            }
            PlayerDrawer.Services -> {
                delay(PANEL_ANIM_IN.toLong())
                runCatching { firstServiceFocus.requestFocus() }
            }
            PlayerDrawer.Recent -> Unit
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (drawer == PlayerDrawer.None) {
                            drawer = PlayerDrawer.Channels
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (drawer == PlayerDrawer.None) {
                            drawer = PlayerDrawer.Services
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (drawer == PlayerDrawer.None) {
                            showOsd = !showOsd
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                        if (drawer == PlayerDrawer.None) {
                            stepChannel(1)
                            showOsd = true
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (drawer == PlayerDrawer.None) {
                            stepChannel(-1)
                            showOsd = true
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_MENU -> {
                        if (drawer == PlayerDrawer.None) {
                            showOsd = false
                            drawer = PlayerDrawer.Services
                            true
                        } else false
                    }
                    else -> false
                }
            }
    ) {
        VideoPlayer(
            channel,
            captionsEnabled,
            captionLanguage,
            Modifier.fillMaxSize(),
            controlState = playbackControls,
            preferredAudioLanguage = trackPreferences.audioLanguage,
            onAudioLanguageChanged = { trackPreferences.audioLanguage = it },
            onSubtitleChanged = { enabled, language ->
                trackPreferences.subtitlesEnabled = enabled
                trackPreferences.subtitleLanguage = language
                onCaptionsChanged(enabled, language ?: captionLanguage)
            },
            onPlaybackError = { category ->
                val report = PlaybackErrorCategorizer.sanitizedReport(
                    category, channel.name, System.currentTimeMillis()
                )
                scope.launch(Dispatchers.IO) {
                    runCatching { GlzHubManager.heartbeat(prefs, client, lastError = report) }
                }
            },
            onPlaybackReady = { readyChannelId ->
                if (readyChannelId == channel.id) showOsd = true
            }
        )

        AnimatedVisibility(
            visible = drawer == PlayerDrawer.None && showOsd,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(650))
        ) {
            PlayerOsd(
                channel = channel,
                currentProgramme = currentProgramme,
                nextProgramme = nextProgramme,
                guide = guide,
                now = now
            )
        }

        AnimatedVisibility(
            visible = drawer == PlayerDrawer.None && showOsd,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(650))
        ) {
            Surface(
                color = Color.Black.copy(alpha = .58f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "▲ Next   ▼ Previous      ◀ Guide      ▶ Apps · Audio · CC      OK Info",
                    Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
                    color = Color.White.copy(alpha = .86f),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        AnimatedVisibility(
            visible = drawer == PlayerDrawer.Channels,
            enter = panelEnter(fromLeft = true),
            exit = panelExit(fromLeft = true),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            val drawerWidth = if (maxWidth < 520.dp) maxWidth * .88f else 420.dp
            Surface(
                Modifier.width(drawerWidth).fillMaxHeight(),
                color = Color(0xF20B1114),
                contentColor = Color.White,
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                tonalElevation = 18.dp,
                shadowElevation = 24.dp
            ) {
                Column(Modifier.fillMaxSize().padding(top = 22.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelLogo(channel, 66.dp, guide)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(channel.number.ifBlank { "LIVE" },
                                color = Color(0xFFC4FF4D),
                                fontWeight = FontWeight.Black)
                            Text(channel.name, color = Color.White, fontSize = 24.sp,
                                fontWeight = FontWeight.Black, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Surface(
                        Modifier.fillMaxWidth().padding(22.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .18f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("ON NOW", color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black)
                            Text(currentProgramme?.title ?: "Live programming",
                                color = Color.White, fontSize = 19.sp,
                                fontWeight = FontWeight.Bold, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            currentProgramme?.let {
                                Text(
                                    "${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.startMillis))} – " +
                                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.endMillis)),
                                    color = Color.White.copy(alpha = .68f)
                                )
                            }
                        }
                    }
                    Text(if (selectingMultiViewChannel) "SELECT SECOND CHANNEL" else "CHANNELS",
                        Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
                        color = Color.White.copy(alpha = .56f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black)
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        state = channelListState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(channels, key = { index, ch -> "drawer-$index-${ch.streamUrl}" }) { _, item ->
                            val isSelected = item.id == channel.id
                            var isFocused by remember(item.id) { mutableStateOf(false) }
                            val itemProgramme = guide.forChannel(item)
                                .firstOrNull { it.startMillis <= now && it.endMillis > now }
                            Surface(
                                Modifier.fillMaxWidth()
                                    .then(if (isSelected) Modifier.focusRequester(selectedChannelFocus)
                                    else Modifier)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .clickable {
                                        if (selectingMultiViewChannel) {
                                            onAddToMultiView(item)
                                            selectingMultiViewChannel = false
                                        } else {
                                            onTune(item)
                                        }
                                        drawer = PlayerDrawer.None
                                    }
                                    .focusable(),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isFocused)
                                    Color(0xFF23405F)
                                else if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = .35f)
                                else Color.Transparent
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    ChannelLogo(item, 42.dp, guide)
                                    Spacer(Modifier.width(12.dp))
                                    Text(item.number.ifBlank { "—" }, Modifier.width(45.dp),
                                        color = if (isSelected) Color(0xFFC4FF4D)
                                        else Color.White.copy(alpha = .62f),
                                        fontWeight = FontWeight.Black)
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, color = Color.White,
                                            fontWeight = FontWeight.Bold, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Text(itemProgramme?.title ?: "Guide unavailable",
                                            color = Color.White.copy(alpha = .58f),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = drawer == PlayerDrawer.Services,
            enter = panelEnter(fromLeft = false),
            exit = panelExit(fromLeft = false),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            val drawerWidth = if (maxWidth < 520.dp) maxWidth * .92f else 440.dp
            Surface(
                Modifier.width(drawerWidth).fillMaxHeight(),
                color = Color(0xF20B1114),
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                tonalElevation = 18.dp,
                shadowElevation = 24.dp
            ) {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Column(Modifier.padding(bottom = 6.dp)) {
                            Text("ENTERTAINMENT", color = Color.White, fontSize = 24.sp,
                                fontWeight = FontWeight.Black)
                            Text("Playback options and other apps",
                                color = Color.White.copy(alpha = .6f))
                        }
                    }

                    item { DrawerSectionLabel("PLAYBACK") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvOptionButton(
                                text = "Previous channel",
                                onClick = { onPreviousChannel(); drawer = PlayerDrawer.None },
                                modifier = Modifier.fillMaxWidth().focusRequester(firstServiceFocus)
                            )
                            TvOptionButton(
                                text = "Recent channels",
                                onClick = { drawer = PlayerDrawer.Recent },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TvOptionButton(
                                text = "Add to MultiView",
                                onClick = { selectingMultiViewChannel = true; drawer = PlayerDrawer.Channels },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TvOptionButton(
                                text = if (showDiagnostics) "Hide stream info" else "Stream info",
                                selected = showDiagnostics,
                                onClick = { showDiagnostics = !showDiagnostics },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (playbackControls.audioTracks.isNotEmpty()) {
                        item { DrawerSectionLabel("AUDIO") }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                playbackControls.audioTracks.forEach { track ->
                                    TvOptionButton(
                                        text = if (track.selected) "✓ ${track.label}" else track.label,
                                        selected = track.selected,
                                        onClick = { playbackControls.chooseAudio(track.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    item { DrawerSectionLabel("SUBTITLES / CC") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvOptionButton(
                                text = if (!captionsEnabled) "✓ Off" else "Off",
                                selected = !captionsEnabled,
                                onClick = { playbackControls.chooseSubtitle(null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            playbackControls.subtitleTracks.forEach { track ->
                                val isSelected = track.selected && captionsEnabled
                                TvOptionButton(
                                    text = if (isSelected) "✓ ${track.label}" else track.label,
                                    selected = isSelected,
                                    onClick = { playbackControls.chooseSubtitle(track.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    item { DrawerSectionLabel("APPS") }
                    items(entertainmentApps, key = { "service-${it.packageName}" }) { app ->
                        val launchIntent = remember(app.packageName) {
                            findAppLaunchIntent(context, app.packageName)
                        }
                        val icon = remember(app.packageName, launchIntent) {
                            if (launchIntent == null) null else runCatching {
                                context.packageManager.getApplicationIcon(app.packageName)
                            }.getOrNull()
                        }
                        var isFocused by remember(app.packageName) { mutableStateOf(false) }
                        Surface(
                            Modifier.fillMaxWidth()
                                .onFocusChanged { isFocused = it.isFocused }
                                .clickable {
                                    GlzHubManager.reportLaunchedApp(
                                        prefs, app.name, app.packageName
                                    )
                                    scope.launch(Dispatchers.IO) {
                                        GlzHubManager.heartbeat(prefs, client)
                                    }
                                    launchEntertainmentApp(context, app.packageName, launchIntent)
                                }
                                .focusable(),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isFocused) app.accent
                            else Color.White.copy(alpha = .08f),
                            shadowElevation = 0.dp
                        ) {
                            Row(Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                AdaptiveAppIcon(
                                    icon = icon,
                                    appName = app.name,
                                    accent = app.accent,
                                    size = 54.dp
                                )
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(app.name, color = Color.White, fontSize = 18.sp,
                                        fontWeight = FontWeight.Black)
                                    Text(if (launchIntent == null) "Install" else "Open",
                                        color = Color.White.copy(alpha = .7f))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDiagnostics && drawer == PlayerDrawer.Services) {
            PlaybackDiagnosticsPanel(
                diagnostics = playbackControls.diagnostics,
                speedTestResult = speedTestResult,
                onRunSpeedTest = onRunSpeedTest?.let { action -> { action(channel.streamUrl) } },
                modifier = Modifier.align(Alignment.TopStart).padding(30.dp)
            )
        }

        if (drawer == PlayerDrawer.Recent) {
            Surface(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 30.dp),
                color = Color(0xF20B1114), contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 18.dp
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("RECENT CHANNELS", color = Color(0xFFC4FF4D),
                        fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentChannels, key = { "recent-${it.id}" }) { recent ->
                            val programme = guide.forChannel(recent)
                                .firstOrNull { it.startMillis <= now && it.endMillis > now }
                            var isFocused by remember(recent.id) { mutableStateOf(false) }
                            Surface(
                                onClick = {
                                    onTune(recent)
                                    drawer = PlayerDrawer.None
                                },
                                modifier = Modifier
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isFocused) Color(0xFFC4FF4D) else Color.White.copy(alpha = 0.12f),
                                contentColor = if (isFocused) Color.Black else Color.White,
                                shadowElevation = if (isFocused) 8.dp else 0.dp
                            ) {
                                Column(Modifier.width(180.dp).padding(14.dp)) {
                                    Text(recent.name, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFocused) Color.Black else Color.White)
                                    Text(programme?.title ?: "Live programming", maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFocused) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.6f))
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
fun MultiViewScreen(
    primary: Channel,
    secondary: Channel,
    guide: EpgGuide,
    captionsEnabled: Boolean,
    captionLanguage: String,
    onExpand: (Channel) -> Unit,
    onExit: () -> Unit
) {
    var activePane by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    BackHandler(onBack = onExit)
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { focusRequester.requestFocus() }
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(10.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { activePane = 0; true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { activePane = 1; true }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        onExpand(if (activePane == 0) primary else secondary)
                        true
                    }
                    else -> false
                }
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(primary to 0, secondary to 1).forEach { (ch, paneIndex) ->
            val isActive = activePane == paneIndex
            val borderModifier = if (isActive) {
                Modifier.border(3.dp, Color(0xFFC4FF4D), RoundedCornerShape(20.dp))
            } else {
                Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .then(borderModifier)
                    .clickable {
                        activePane = paneIndex
                        onExpand(ch)
                    }
            ) {
                VideoPlayer(
                    channel = ch,
                    captionsEnabled = captionsEnabled && isActive,
                    captionLanguage = captionLanguage,
                    modifier = Modifier.fillMaxSize(),
                    muted = !isActive,
                    createMediaSession = isActive,
                    cropVideo = false,
                    zOrderMediaOverlay = paneIndex == 1
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelLogo(ch, 26.dp, guide)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            ch.name,
                            color = if (isActive) Color(0xFFC4FF4D) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isActive) {
                            Spacer(Modifier.width(6.dp))
                            Text("● Audio", color = Color(0xFFC4FF4D), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerOsd(
    channel: Channel,
    currentProgramme: Programme?,
    nextProgramme: Programme?,
    guide: EpgGuide? = null,
    now: Long,
    modifier: Modifier = Modifier
) {
    val remainingMinutes = currentProgramme?.let {
        ((it.endMillis - now).coerceAtLeast(0L) + 59_999L) / 60_000L
    }
    val progress = currentProgramme?.let {
        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
            .coerceIn(0f, 1f)
    } ?: 0f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        color = Color(0xF5080C10),
        shadowElevation = 24.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(0.42f)
                ) {
                    ChannelLogo(channel, 58.dp, guide)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Text(
                                "CH ${channel.number.ifBlank { "LIVE" }}",
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            channel.name,
                            color = Color.White,
                            fontSize = if (channel.name.length > 22) 17.sp else 20.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(24.dp))

                Column(Modifier.weight(0.58f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            currentProgramme?.title ?: "Live Broadcast",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        remainingMinutes?.let {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "$it min left",
                                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    currentProgramme?.let { prog ->
                        Text(
                            "${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prog.startMillis))} – ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prog.endMillis))}",
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = Color.White.copy(alpha = 0.18f)
                    )
                }
            }

            nextProgramme?.let { next ->
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            "UP NEXT",
                            Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(next.startMillis)),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        next.title,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SportsBarKioskScreen(
    channels: List<Channel>,
    prefs: SharedPreferences,
    client: OkHttpClient,
    captionLanguage: String,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var selectedChannelId by remember {
        mutableStateOf(prefs.getString(SPORTS_BAR_KIOSK_CHANNEL_ID, null))
    }
    var stations by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var selectedStationCode by remember {
        mutableStateOf(prefs.getString(SPORTS_BAR_KIOSK_RADIO_CODE, null))
    }
    var chooser by remember { mutableStateOf<String?>(null) }
    var radioStatus by remember { mutableStateOf("Loading GLZ Radio…") }
    val channel = channels.firstOrNull { it.id == selectedChannelId } ?: channels.firstOrNull()
    val station = stations.firstOrNull { it.code == selectedStationCode } ?: stations.firstOrNull()
    val radioFactory = remember {
        DefaultHttpDataSource.Factory().setUserAgent("GLZ-TV-SportsBar/${BuildConfig.VERSION_NAME}")
    }
    val radioPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(radioFactory))
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA).build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)
                setWakeMode(C.WAKE_MODE_LOCAL)
            }
    }
    val chooserFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = chooser == null) { onExit() }

    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { RadioCatalogManager.load(prefs, client) } }
            .onSuccess {
                stations = it.stations
                radioStatus = if (it.fromCache) "Saved GLZ Radio stations" else "GLZ Radio live"
            }
            .onFailure { radioStatus = "GLZ Radio is temporarily unavailable" }
    }
    LaunchedEffect(channels, selectedChannelId) {
        channel?.let {
            if (it.id != selectedChannelId) selectedChannelId = it.id
            prefs.edit().putString(SPORTS_BAR_KIOSK_CHANNEL_ID, it.id).apply()
            GlzHubManager.reportActivity(prefs, "sports_bar", it.name)
        }
    }
    LaunchedEffect(station?.code, station?.streamUrl) {
        val activeStation = station ?: return@LaunchedEffect
        if (activeStation.code != selectedStationCode) selectedStationCode = activeStation.code
        prefs.edit().putString(SPORTS_BAR_KIOSK_RADIO_CODE, activeStation.code).apply()
        radioPlayer.stop()
        radioFactory.setDefaultRequestProperties(activeStation.requestHeaders)
        radioPlayer.setMediaItem(
            MediaItem.Builder().setUri(activeStation.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(activeStation.name)
                        .setArtist(activeStation.genre).build()
                ).build()
        )
        radioPlayer.prepare()
        radioPlayer.play()
    }
    LaunchedEffect(chooser) {
        if (chooser != null) {
            chooserFocusRequester.requestFocus()
        }
    }
    DisposableEffect(radioPlayer) {
        onDispose {
            radioPlayer.release()
            GlzHubManager.reportActivity(prefs, "idle")
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_MENU -> { chooser = "channel"; true }
                    else -> false
                }
            }
    ) {
        channel?.let {
            VideoPlayer(
                channel = it,
                captionsEnabled = false,
                captionLanguage = captionLanguage,
                modifier = Modifier.fillMaxSize(),
                muted = true,
                createMediaSession = false,
                cropVideo = true
            )
        } ?: Surface(Modifier.align(Alignment.Center), color = Color.Black.copy(alpha = .72f)) {
            Text("No TV channels are available", Modifier.padding(24.dp), color = Color.White)
        }

        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(22.dp),
            color = Color.Black.copy(alpha = .68f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
                Text("SPORTS BAR", color = Color(0xFFC4FF4D), fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp)
                Text("TV is muted · GLZ Radio is playing", color = Color.White.copy(alpha = .88f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(24.dp).focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvOptionButton(
                "TV: ${channel?.name ?: "Choose channel"}",
                onClick = { if (chooser == null) chooser = "channel" }
            )
            TvOptionButton(
                "RADIO: ${station?.name ?: radioStatus}",
                onClick = { if (chooser == null) chooser = "radio" }
            )
            TvOptionButton("Exit", onClick = { if (chooser == null) onExit() })
        }

        chooser?.let { type ->
            BackHandler { chooser = null }
            Surface(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(.82f).fillMaxHeight(.78f),
                color = Color(0xF20B1114), contentColor = Color.White,
                shape = RoundedCornerShape(24.dp), tonalElevation = 18.dp
            ) {
                Column(Modifier.fillMaxSize().padding(22.dp)) {
                    Text(
                        if (type == "channel") "Choose background channel" else "Choose GLZ Radio station",
                        fontSize = 24.sp, fontWeight = FontWeight.Black
                    )
                    Text("Changes stay on this TV", color = Color.White.copy(alpha = .65f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (type == "channel") {
                            itemsIndexed(
                                channels,
                                key = { index, item -> "${item.id.ifBlank { "chan" }}_$index" }
                            ) { index, item ->
                                TvOptionButton(
                                    "${item.number.takeIf { it.isNotBlank() }?.plus(" · ").orEmpty()}${item.name}",
                                    onClick = { selectedChannelId = item.id; chooser = null },
                                    selected = item.id == channel?.id,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (index == 0) Modifier.focusRequester(chooserFocusRequester) else Modifier)
                                )
                            }
                        } else {
                            itemsIndexed(
                                stations,
                                key = { index, item -> "${item.code.ifBlank { "stat" }}_$index" }
                            ) { index, item ->
                                TvOptionButton(
                                    item.name,
                                    onClick = { selectedStationCode = item.code; chooser = null },
                                    selected = item.code == station?.code,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (index == 0) Modifier.focusRequester(chooserFocusRequester) else Modifier)
                                )
                            }
                        }
                    }
                    TvOptionButton("Close", onClick = { chooser = null }, modifier = Modifier.align(Alignment.End))
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    channel: Channel,
    captionsEnabled: Boolean,
    captionLanguage: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    createMediaSession: Boolean = true,
    keepScreenOn: Boolean = true,
    cropVideo: Boolean = false,
    zOrderMediaOverlay: Boolean = false,
    controlState: PlaybackControlState? = null,
    preferredAudioLanguage: String? = null,
    onAudioLanguageChanged: (String?) -> Unit = {},
    onSubtitleChanged: (Boolean, String?) -> Unit = { _, _ -> },
    onPlaybackError: (PlaybackErrorCategory) -> Unit = {},
    onPlaybackReady: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val httpFactory = remember {
        DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
    }
    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
    }
    val mediaSession = remember(createMediaSession, channel.id) {
        if (createMediaSession) {
            runCatching {
                MediaSession.Builder(context, player)
                    .setId("glz_tv_${channel.id}_${System.identityHashCode(player)}")
                    .build()
            }.getOrNull()
        } else null
    }
    var retryAttempt by remember(channel.id) { mutableStateOf(0) }
    var sourceIndex by remember(channel.id) { mutableStateOf(0) }
    var playbackMessage by remember(channel.id) { mutableStateOf<String?>("Connecting…") }
    var lastPlaybackError by remember(channel.id) { mutableStateOf<PlaybackErrorCategory?>(null) }
    val streamUrls = remember(channel.id, channel.streamUrl) {
        val overrides = PreferencesRepository.parseChannelFallbacks(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(PreferencesRepository.CHANNEL_FALLBACKS, "").orEmpty()
        )
        val backups = overrides[channel.number.trim()] ?: overrides[channel.id] ?: emptyList()
        (listOf(channel.streamUrl) + backups).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }
    val activeUrl = streamUrls.getOrElse(sourceIndex) { channel.streamUrl }
    val switchToNextSource = switch@{
        if (sourceIndex >= streamUrls.lastIndex) return@switch false
        sourceIndex += 1
        retryAttempt = 0
        playbackMessage = "Switching to backup source…"
        true
    }
    DisposableEffect(player, channel.id, streamUrls) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val category = PlaybackErrorCategorizer.categorize(error)
                lastPlaybackError = category
                onPlaybackError(category)
                if (retryAttempt < 3) {
                    retryAttempt += 1
                    playbackMessage = if (retryAttempt >= 2) "Trying compatibility mode…"
                    else "Retrying stream…"
                } else if (!switchToNextSource()) {
                    playbackMessage = "Stream unavailable"
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playbackMessage = null
                } else if (playbackState == Player.STATE_BUFFERING && retryAttempt == 0) {
                    playbackMessage = "Connecting…"
                } else if (playbackState == Player.STATE_ENDED && !switchToNextSource()) {
                    playbackMessage = "Stream unavailable"
                }
            }

            override fun onRenderedFirstFrame() {
                PlaybackPerformance.firstFrameRendered(channel.id)
                onPlaybackReady(channel.id)
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audioSelections = mutableMapOf<String, Pair<Tracks.Group, Int>>()
                val subtitleSelections = mutableMapOf<String, Pair<Tracks.Group, Int>>()
                val audio = mutableListOf<TrackOption>()
                val subtitles = mutableListOf<TrackOption>()
                tracks.groups.forEachIndexed { groupIndex, group ->
                    for (trackIndex in 0 until group.length) {
                        if (!group.isTrackSupported(trackIndex)) continue
                        val format = group.getTrackFormat(trackIndex)
                        val id = "$groupIndex:$trackIndex"
                        val option = TrackOption(
                            id = id,
                            label = trackLabel(format.label, format.language, format.channelCount),
                            language = format.language,
                            selected = group.isTrackSelected(trackIndex)
                        )
                        when (group.type) {
                            C.TRACK_TYPE_AUDIO -> {
                                audio += option
                                audioSelections[id] = group to trackIndex
                            }
                            C.TRACK_TYPE_TEXT -> {
                                subtitles += option
                                subtitleSelections[id] = group to trackIndex
                            }
                        }
                    }
                }
                controlState?.audioTracks = audio
                controlState?.subtitleTracks = subtitles
                controlState?.selectAudio = { id ->
                    audioSelections[id]?.let { (group, index) ->
                        val language = group.getTrackFormat(index).language
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(index)))
                            .build()
                        onAudioLanguageChanged(language)
                    }
                }
                controlState?.selectSubtitle = { id ->
                    if (id == null) {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
                        onSubtitleChanged(false, null)
                    } else subtitleSelections[id]?.let { (group, index) ->
                        val language = group.getTrackFormat(index).language
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(index)))
                            .build()
                        onSubtitleChanged(true, language)
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(activeUrl, captionsEnabled, captionLanguage, preferredAudioLanguage, retryAttempt) {
        if (retryAttempt > 0) delay((1L shl (retryAttempt - 1)) * 1_000L)
        httpFactory
            .setDefaultRequestProperties(channel.headers)
            .setUserAgent(channel.headers["User-Agent"] ?: "GLZ-TV/2.0")
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsEnabled)
            .setPreferredTextLanguage(captionLanguage.ifBlank { null })
            .setPreferredAudioLanguage(preferredAudioLanguage)
            .setSelectUndeterminedTextLanguage(captionsEnabled)
            .build()
        player.stop()
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(activeUrl)
                .setMediaId(channel.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(channel.name)
                        .setArtist(channel.group)
                        .setArtworkUri(channel.logoUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
                        .build()
                )
                .build(),
            true
        )
        player.prepare()
        player.playWhenReady = true
    }
    LaunchedEffect(player, channel.id, controlState, sourceIndex) {
        if (controlState == null) return@LaunchedEffect
        while (true) {
            val video = player.videoFormat
            val audio = player.audioFormat
            controlState.diagnostics = PlaybackDiagnostics(
                channelName = if (sourceIndex > 0) "${channel.name} (backup ${sourceIndex})" else channel.name,
                protocol = PlaybackErrorCategorizer.protocol(activeUrl),
                resolution = if (video != null && video.width > 0 && video.height > 0)
                    "${video.width} × ${video.height}" else null,
                videoCodec = video?.codecs ?: video?.sampleMimeType,
                audioCodec = audio?.codecs ?: audio?.sampleMimeType,
                bitrate = listOfNotNull(video?.bitrate, audio?.bitrate)
                    .filter { it > 0 }.takeIf { it.isNotEmpty() }?.sum(),
                bufferDurationMs = player.totalBufferedDuration.coerceAtLeast(0),
                droppedFrames = player.videoDecoderCounters?.droppedBufferCount?.toLong() ?: 0,
                networkTransport = currentNetworkTransport(context),
                playbackState = playbackStateLabel(player.playbackState),
                lastError = lastPlaybackError
            )
            delay(1_000)
        }
    }
    LaunchedEffect(player, channel.id, streamUrls, sourceIndex) {
        var lastPosition = -1L
        var stalledMs = 0L
        var reprobeArmed = false
        while (true) {
            delay(1_000)
            val state = player.playbackState
            val position = player.currentPosition
            val advancing = lastPosition >= 0 && position - lastPosition >= PLAYBACK_PROGRESS_EPSILON_MS
            lastPosition = position
            val watching = player.playWhenReady &&
                (state == Player.STATE_READY || state == Player.STATE_BUFFERING)
            if (watching && !advancing) {
                stalledMs += 1_000
            } else {
                stalledMs = 0
                reprobeArmed = false
            }
            if (stalledMs >= PLAYBACK_STALL_LIMIT_MS) {
                if (switchToNextSource()) {
                    stalledMs = 0
                } else if (!reprobeArmed) {
                    reprobeArmed = true
                    playbackMessage = "Stream unavailable"
                    launch {
                        delay(DEAD_SOURCE_REPROBE_MS)
                        player.prepare()
                        player.playWhenReady = true
                    }
                }
            }
        }
    }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    DisposableEffect(mediaSession) { onDispose { mediaSession?.release() } }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(modifier.background(Color.Black, RoundedCornerShape(24.dp))) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    this.keepScreenOn = keepScreenOn
                    resizeMode = if (cropVideo) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else AspectRatioFrameLayout.RESIZE_MODE_FIT
                    useController = false
                    controllerAutoShow = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    if (zOrderMediaOverlay) {
                        (videoSurfaceView as? SurfaceView)?.setZOrderMediaOverlay(true)
                    }
                }
            },
            update = {
                it.player = player
                it.keepScreenOn = keepScreenOn
                it.resizeMode = if (cropVideo) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else AspectRatioFrameLayout.RESIZE_MODE_FIT
                if (zOrderMediaOverlay) {
                    (it.videoSurfaceView as? SurfaceView)?.setZOrderMediaOverlay(true)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        playbackMessage?.let { message ->
            Surface(
                color = Color.Black.copy(alpha = .82f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (retryAttempt in 1..3) LinearProgressIndicator(Modifier.width(180.dp))
                    Text(message, Modifier.padding(top = 10.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun trackLabel(label: String?, language: String?, channelCount: Int): String {
    val languageLabel = language?.let { code ->
        runCatching { Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault()) }
            .getOrNull()?.takeIf(String::isNotBlank)
    }
    val base = label?.takeIf(String::isNotBlank) ?: languageLabel ?: "Original"
    val layout = when {
        channelCount >= 6 -> "5.1"
        channelCount == 2 -> "Stereo"
        channelCount == 1 -> "Mono"
        else -> null
    }
    return listOfNotNull(base, layout).distinct().joinToString(" · ")
}

fun playbackStateLabel(state: Int) = when (state) {
    Player.STATE_IDLE -> "Idle"
    Player.STATE_BUFFERING -> "Buffering"
    Player.STATE_READY -> "Playing"
    Player.STATE_ENDED -> "Ended"
    else -> "Unavailable"
}

fun currentNetworkTransport(context: Context): String? {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return null
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return null
    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        else -> "Other"
    }
}
