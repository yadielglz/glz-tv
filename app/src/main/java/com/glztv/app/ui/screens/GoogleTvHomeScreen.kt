package com.glztv.app.ui.screens

import android.content.Context
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.BuildConfig
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.GuestExperience
import com.glztv.app.Movie
import com.glztv.app.RadioStation
import com.glztv.app.launcher.LauncherAppItem
import com.glztv.app.launcher.LauncherAppsManager
import com.glztv.app.launcher.toSafeImageBitmap
import com.glztv.app.model.EntertainmentApp
import com.glztv.app.model.NetworkInfo
import com.glztv.app.model.WeatherInfo
import com.glztv.app.player.RadioPlaybackState
import com.glztv.app.ui.components.AppContextMenuDialog
import com.glztv.app.ui.components.GoogleTvChannelLogo
import com.glztv.app.ui.components.HomeRadioPlayerBar
import com.glztv.app.ui.theme.DtvActionButton
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.DtvNavTab
import com.glztv.app.ui.theme.DtvProgressBar
import com.glztv.app.ui.theme.DtvSectionHeader
import com.glztv.app.ui.theme.DtvTopCommandBar
import com.glztv.app.ui.theme.LocalTvViewport
import com.glztv.app.ui.theme.formatDtvCompactTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Classic DirecTV HD Home Screen (v1.48.4.10).
 *
 * 1. Shared DirecTV HD Top Command Bar (`Home` active).
 * 2. Classic DirecTV HD Split-Top Broadcast Panel:
 *    - Left: Live channel logo, number, callsign, LIVE badge, show title, time,
 *      synopsis, electric-blue progress bar, and crisp Watch Live / Full Guide / Favorite controls.
 *    - Right: Sharp framed 16:9 TextureView live TV preview box with 1dp metallic border.
 * 3. Persistent Mini Radio Player bar when audio is active.
 * 4. Hairline-bordered DirecTV content rows: Live Sports & Events, My Channels,
 *    Your Apps, Movies & On Demand, and GLZ Radio.
 */
@Composable
fun GoogleTvHomeScreen(
    channels: List<Channel>,
    guide: EpgGuide,
    resumeChannel: Channel?,
    previewChannel: Channel?,
    favorites: Set<String>,
    recentChannels: List<Channel>,
    radioStations: List<RadioStation> = emptyList(),
    movies: List<Movie> = emptyList(),
    currentRadioStation: RadioStation? = null,
    onPlayRadioStation: (RadioStation) -> Unit = {},
    onWatchMovie: (Movie) -> Unit = {},
    entertainmentApps: List<EntertainmentApp>,
    guestName: String,
    experience: GuestExperience,
    weather: WeatherInfo?,
    networkInfo: NetworkInfo?,
    onWatchChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenGuide: () -> Unit,
    onOpenMovies: () -> Unit = {},
    onOpenRadio: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWelcome: () -> Unit = {},
    onLaunchApp: (EntertainmentApp) -> Unit,
    sportsBarKioskEnabled: Boolean = false,
    onStartSportsBarKiosk: () -> Unit = {},
    previewContent: (@Composable () -> Unit)? = null,
    isFamilyChannel: Boolean = false,
    hasUpdate: Boolean = false,
    onOpenUpdate: () -> Unit = {},
    radioPlaying: Boolean = false,
    radioPlaybackState: RadioPlaybackState? = null,
    onToggleRadioPlayPause: () -> Unit = {},
    onStopRadio: () -> Unit = {},
    onOpenSpeedTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewport = LocalTvViewport.current
    val prefs = remember { context.getSharedPreferences("glz_tv", Context.MODE_PRIVATE) }
    val dockListState = rememberLazyListState()

    var installedApps by remember { mutableStateOf<List<LauncherAppItem>>(emptyList()) }
    var contextMenuApp by remember { mutableStateOf<LauncherAppItem?>(null) }

    val isFamilyMode = isFamilyChannel || BuildConfig.DEBUG

    fun refreshInstalledApps() {
        if (isFamilyMode) {
            scope.launch {
                installedApps = LauncherAppsManager.loadInstalledApps(context, prefs)
            }
        }
    }

    if (isFamilyMode) {
        DisposableEffect(isFamilyMode) {
            refreshInstalledApps()
            val receiver = LauncherAppsManager.registerAppListener(context) {
                refreshInstalledApps()
            }
            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    val heroChannel = remember(channels, previewChannel, resumeChannel) {
        previewChannel ?: resumeChannel ?: channels.firstOrNull()
    }
    val eventChannels = remember(channels) {
        channels.filter { it.isLiveEvent() }
    }
    val myChannels = remember(channels, favorites, recentChannels, resumeChannel) {
        buildList {
            resumeChannel?.let { add(it) }
            channels.filter { it.id in favorites }.forEach { add(it) }
            recentChannels.forEach { add(it) }
            channels.filterNot { it.isLiveEvent() }.take(14).forEach { add(it) }
        }.distinctBy { it.id }.take(16)
    }
    val visibleInstalledApps = remember(installedApps) {
        installedApps.filterNot { it.isHidden }
    }

    val watchLiveFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { watchLiveFocus.requestFocus() }
    }

    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(28.dp)
    val splitHeight = if (viewport.isCompactHeight) 172.dp else 198.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DtvChrome.ScreenBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── 1. Shared DirecTV HD Top Command Bar ──────────────────────────────
            DtvTopCommandBar(
                activeTab = DtvNavTab.Home,
                nowMillis = now,
                weather = weather,
                guestLabel = guestName.ifBlank { experience.propertyName.ifBlank { "Guest" } },
                hasUpdate = hasUpdate,
                showMoviesTab = movies.isNotEmpty(),
                sportsBarKioskEnabled = sportsBarKioskEnabled,
                onSelectTab = { tab ->
                    when (tab) {
                        DtvNavTab.Home -> scope.launch { dockListState.animateScrollToItem(0) }
                        DtvNavTab.LiveTv -> (heroChannel ?: channels.firstOrNull())?.let(onWatchChannel) ?: onOpenGuide()
                        DtvNavTab.Guide -> onOpenGuide()
                        DtvNavTab.Movies -> onOpenMovies()
                        DtvNavTab.Radio -> onOpenRadio()
                        DtvNavTab.Weather -> onOpenWeather()
                        DtvNavTab.SportsBar -> onStartSportsBarKiosk()
                    }
                },
                onOpenUpdate = onOpenUpdate,
                onOpenWelcome = onOpenWelcome,
                onOpenSettings = onOpenSettings
            )

            // ── 2. Classic DirecTV HD Split-Top Broadcast Panel ───────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(splitHeight)
                    .background(DtvChrome.BannerGradient)
            ) {
                val active = heroChannel
                val currentProg = remember(active?.id, guide, now) {
                    active?.let { guide.nowProgrammeForChannel(it, now) }
                }
                val progProgress = currentProg?.let {
                    ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                        .coerceIn(0f, 1f)
                } ?: 0f

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPad, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Broadcast Channel & Program Details
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (active != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GoogleTvChannelLogo(
                                        channel = active,
                                        size = if (viewport.isCompactHeight) 32.dp else 36.dp,
                                        guide = guide,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    Text(
                                        text = active.number.ifBlank { "LIVE" },
                                        color = DtvChrome.PrimaryText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = active.name,
                                        color = DtvChrome.SecondaryText,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0x2EEF4444), RoundedCornerShape(2.dp))
                                            .border(1.dp, Color(0x66EF4444), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "ON AIR",
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
                                        )
                                    }
                                    if (eventChannels.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .background(DtvChrome.ElectricBlue.copy(alpha = 0.16f), RoundedCornerShape(2.dp))
                                                .border(1.dp, DtvChrome.ElectricBlue.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "${eventChannels.size} LIVE EVENT${if (eventChannels.size == 1) "" else "S"}",
                                                color = DtvChrome.ElectricBlueSoft,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }

                                val heroTitle = currentProg?.title?.takeIf { it.isNotBlank() }
                                    ?: active.eventTitle?.takeIf { it.isNotBlank() }
                                    ?: active.name
                                Text(
                                    text = heroTitle,
                                    color = Color.White,
                                    fontSize = if (viewport.isCompactHeight) 21.sp else 25.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val timeRange = currentProg?.let {
                                    "${formatDtvCompactTime(it.startMillis)} - ${formatDtvCompactTime(it.endMillis)}"
                                } ?: "Live Broadcast"
                                val sub = buildList {
                                    add(timeRange)
                                    if (active.group.isNotBlank()) add(active.group)
                                }.joinToString(", ")
                                Text(
                                    text = sub,
                                    color = DtvChrome.SecondaryText,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                currentProg?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                    Text(
                                        text = desc,
                                        color = DtvChrome.PrimaryText.copy(alpha = 0.78f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (currentProg != null) {
                                    DtvProgressBar(
                                        progress = progProgress,
                                        modifier = Modifier.width(260.dp)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DtvActionButton(
                                    label = "Watch Live",
                                    primary = true,
                                    showPlayIcon = true,
                                    modifier = Modifier.focusRequester(watchLiveFocus),
                                    onClick = { onWatchChannel(active) }
                                )
                                DtvActionButton(
                                    label = "Full Guide",
                                    primary = false,
                                    onClick = onOpenGuide
                                )
                                DtvHomeFavIconButton(
                                    isFavorite = active.id in favorites,
                                    onClick = { onToggleFavorite(active.id) }
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Welcome to GLZ TV",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = "Open Guide, Radio, or Settings to get started.",
                                    color = DtvChrome.SecondaryText,
                                    fontSize = 13.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DtvActionButton(
                                    label = "Open Guide",
                                    primary = true,
                                    modifier = Modifier.focusRequester(watchLiveFocus),
                                    onClick = onOpenGuide
                                )
                                DtvActionButton(
                                    label = "GLZ Radio",
                                    primary = false,
                                    onClick = onOpenRadio
                                )
                            }
                        }
                    }

                    // Right: Sharp Framed 16:9 TextureView Live Preview Window
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(3.dp))
                            .clipToBounds()
                            .background(Color.Black)
                            .border(1.dp, DtvChrome.Divider, RoundedCornerShape(3.dp))
                    ) {
                        if (previewContent != null) {
                            previewContent()
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(DtvChrome.CardGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = active?.name ?: "GLZ TV",
                                    color = DtvChrome.SecondaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        // Crisp subtle corner channel tag inside the framed preview
                        if (active != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(Color(0xCC06080B), RoundedCornerShape(2.dp))
                                    .border(1.dp, DtvChrome.Divider, RoundedCornerShape(2.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${active.number.ifBlank { "LIVE" }} ${active.name}".take(20),
                                    color = DtvChrome.PrimaryText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Hairline bottom divider under split-top panel
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .background(DtvChrome.Divider)
                )
            }

            // ── 3. Persistent Mini Radio Player Bar (when Radio is active) ────────
            AnimatedVisibility(
                visible = currentRadioStation != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                HomeRadioPlayerBar(
                    station = currentRadioStation,
                    isPlaying = radioPlaying,
                    playbackState = radioPlaybackState,
                    onTogglePlayPause = onToggleRadioPlayPause,
                    onStop = onStopRadio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad, vertical = 6.dp)
                )
            }

            // ── 4. DirecTV HD Content Rows Below ──────────────────────────────────
            LazyColumn(
                state = dockListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 14.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Row 1: Live Sports & Events (auto-hides when empty)
                if (eventChannels.isNotEmpty()) {
                    item(key = "dtv-home-events") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DtvSectionHeader(
                                title = "LIVE SPORTS & EVENTS",
                                trailing = "${eventChannels.size} LIVE NOW",
                                showLiveDot = true,
                                horizontalPad = horizontalPad
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(eventChannels, key = { "ev-${it.id}" }) { eventCh ->
                                    val prog = remember(eventCh.id, guide, now) {
                                        guide.nowProgrammeForChannel(eventCh, now)
                                    }
                                    val pct = prog?.let {
                                        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                                            .coerceIn(0f, 1f)
                                    } ?: 0.5f
                                    DtvHomeLiveEventCard(
                                        channel = eventCh,
                                        progress = pct,
                                        onClick = { onWatchChannel(eventCh) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 2: My Channels (Merged Favorites + Recent + Top Lineup)
                if (myChannels.isNotEmpty()) {
                    item(key = "dtv-home-channels") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DtvSectionHeader(
                                title = "MY CHANNELS",
                                trailing = "FAVORITES & RECENT",
                                horizontalPad = horizontalPad
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(myChannels, key = { "mych-${it.id}" }) { ch ->
                                    val prog = remember(ch.id, guide, now) {
                                        guide.nowProgrammeForChannel(ch, now)
                                    }
                                    val pct = prog?.let {
                                        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                                            .coerceIn(0f, 1f)
                                    } ?: 0f
                                    val timeLabel = prog?.let {
                                        "${formatDtvCompactTime(it.startMillis)} - ${formatDtvCompactTime(it.endMillis)}"
                                    } ?: "Live"
                                    DtvHomeChannelCard(
                                        channel = ch,
                                        programmeTitle = prog?.title ?: ch.eventTitle ?: "Live Broadcast",
                                        timeRange = timeLabel,
                                        progress = pct,
                                        isFavorite = ch.id in favorites,
                                        guide = guide,
                                        onClick = { onWatchChannel(ch) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 3: Your Apps
                if (visibleInstalledApps.isNotEmpty() || entertainmentApps.isNotEmpty()) {
                    item(key = "dtv-home-apps") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DtvSectionHeader(
                                title = "APPS",
                                trailing = if (isFamilyMode && visibleInstalledApps.isNotEmpty()) "HOLD OK FOR OPTIONS" else null,
                                horizontalPad = horizontalPad
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isFamilyMode && visibleInstalledApps.isNotEmpty()) {
                                    items(visibleInstalledApps, key = { "inst-${it.packageName}" }) { app ->
                                        DtvHomeInstalledAppTile(
                                            app = app,
                                            onClick = { LauncherAppsManager.launchApp(context, app.packageName) },
                                            onLongPress = { contextMenuApp = app }
                                        )
                                    }
                                } else {
                                    items(entertainmentApps, key = { "ent-${it.packageName}" }) { app ->
                                        DtvHomeEntertainmentAppTile(
                                            app = app,
                                            onClick = { onLaunchApp(app) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Row 4: Movies & On Demand
                if (movies.isNotEmpty()) {
                    item(key = "dtv-home-movies") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DtvSectionHeader(
                                title = "MOVIES & ON DEMAND",
                                trailing = "${movies.size} TITLES",
                                horizontalPad = horizontalPad
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(movies, key = { "mov-${it.id}" }) { movie ->
                                    DtvMoviePosterCard(
                                        movie = movie,
                                        onClick = { onWatchMovie(movie) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 5: GLZ Radio
                if (radioStations.isNotEmpty()) {
                    item(key = "dtv-home-radio") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DtvSectionHeader(
                                title = "GLZ RADIO",
                                trailing = "${radioStations.size} STATIONS",
                                horizontalPad = horizontalPad
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(radioStations, key = { "rad-${it.code}-${it.name}" }) { station ->
                                    val isCurrent = currentRadioStation != null &&
                                        currentRadioStation.code == station.code &&
                                        currentRadioStation.streamUrl == station.streamUrl
                                    DtvHomeRadioCard(
                                        station = station,
                                        isActive = isCurrent && radioPlaying,
                                        onClick = { onPlayRadioStation(station) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // App Context Menu Dialog (Family Mode long-press)
        contextMenuApp?.let { app ->
            AppContextMenuDialog(
                app = app,
                onOpenApp = { LauncherAppsManager.launchApp(context, app.packageName) },
                onToggleFavorite = {
                    LauncherAppsManager.setFavorite(prefs, app.packageName, !app.isFavorite)
                    refreshInstalledApps()
                },
                onToggleHide = {
                    LauncherAppsManager.setHidden(prefs, app.packageName, !app.isHidden)
                    refreshInstalledApps()
                },
                onOpenDetails = { LauncherAppsManager.openAppDetails(context, app.packageName) },
                onUninstall = { LauncherAppsManager.uninstallApp(context, app.packageName) },
                onDismiss = { contextMenuApp = null }
            )
        }
    }
}

@Composable
private fun DtvHomeFavIconButton(
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color.Black else Color(0xFF171B22))
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) DtvChrome.ElectricBlue else DtvChrome.Divider, RoundedCornerShape(3.dp))
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
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
            tint = if (isFavorite) DtvChrome.Gold else DtvChrome.PrimaryText,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun DtvHomeLiveEventCard(
    channel: Channel,
    progress: Float,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val league = channel.eventLeague?.takeIf { it.isNotBlank() } ?: "LIVE"
    val title = channel.eventTitle?.takeIf { it.isNotBlank() } ?: channel.name

    Row(
        modifier = Modifier
            .width(256.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
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
    ) {
        // DirecTV signature left vertical accent bar
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (isFocused) DtvChrome.ElectricBlue else DtvChrome.LiveRed)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = league.uppercase(Locale.getDefault()),
                    color = DtvChrome.ElectricBlueSoft,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = "● LIVE · ${channel.number.ifBlank { "PPV" }}",
                    color = Color(0xFFF87171),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = title,
                color = if (isFocused) Color.White else DtvChrome.PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            DtvProgressBar(progress = progress.coerceIn(0.05f, 1f), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DtvHomeChannelCard(
    channel: Channel,
    programmeTitle: String,
    timeRange: String,
    progress: Float,
    isFavorite: Boolean,
    guide: EpgGuide,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(216.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
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
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GoogleTvChannelLogo(
                channel = channel,
                size = 26.dp,
                guide = guide,
                shape = RoundedCornerShape(3.dp)
            )
            Text(
                text = channel.number.ifBlank { "—" },
                color = if (isFocused) DtvChrome.ElectricBlueSoft else DtvChrome.SecondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = channel.name,
                color = if (isFocused) Color.White else DtvChrome.PrimaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isFavorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = DtvChrome.Gold,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = programmeTitle,
                color = if (isFocused) Color.White else DtvChrome.PrimaryText.copy(alpha = 0.85f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = timeRange,
                color = DtvChrome.MutedText,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
        DtvProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DtvHomeInstalledAppTile(
    app: LauncherAppItem,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val iconBitmap = remember(app.packageName, app.banner, app.icon) {
        (app.banner ?: app.icon)?.toSafeImageBitmap(128, 128)
    }

    Column(
        modifier = Modifier
            .width(120.dp)
            .height(82.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(4.dp)
            )
            .onFocusChanged { focused ->
                isFocused = focused.isFocused
                if (!focused.isFocused) longPressJob?.cancel()
            }
            .onKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                val isSelect = code == KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == KeyEvent.KEYCODE_ENTER ||
                    code == KeyEvent.KEYCODE_NUMPAD_ENTER
                if (isSelect) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                            longPressTriggered = false
                            longPressJob?.cancel()
                            longPressJob = scope.launch {
                                delay(550L)
                                longPressTriggered = true
                                onLongPress()
                            }
                        }
                        return@onKeyEvent true
                    } else if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        longPressJob?.cancel()
                        if (!longPressTriggered) onClick()
                        longPressTriggered = false
                        return@onKeyEvent true
                    }
                }
                false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Icon(
                Icons.Default.Apps,
                contentDescription = app.label,
                tint = DtvChrome.PrimaryText,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = app.label,
            color = if (isFocused) Color.White else DtvChrome.PrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DtvHomeEntertainmentAppTile(
    app: EntertainmentApp,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(120.dp)
            .height(82.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(4.dp)
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
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(app.accent.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.name.take(2).uppercase(Locale.getDefault()),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = app.name,
            color = if (isFocused) Color.White else DtvChrome.PrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun DtvMoviePosterCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .width(122.dp)
            .height(178.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF13171E))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(4.dp)
            )
            .onFocusChanged {
                val focused = it.isFocused
                if (focused && !isFocused) onFocused?.invoke()
                isFocused = focused
            }
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
    ) {
        if (movie.posterUrl.isNotBlank()) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1.0f to Color(0xEB05070A)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            movie.releaseYear?.let { year ->
                Text(
                    text = year.toString(),
                    color = DtvChrome.SecondaryText,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun DtvHomeRadioCard(
    station: RadioStation,
    isActive: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(140.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = when {
                    isFocused -> DtvChrome.ElectricBlue
                    isActive -> DtvChrome.ElectricBlue.copy(alpha = 0.7f)
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
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!station.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = station.logoUrl,
                    contentDescription = station.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DtvChrome.ElectricBlue.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Radio,
                        contentDescription = null,
                        tint = DtvChrome.ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (isActive) {
                Text(
                    text = "ON AIR",
                    color = DtvChrome.ElectricBlueSoft,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        }
        Column {
            Text(
                text = station.name,
                color = if (isFocused) Color.White else DtvChrome.PrimaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = station.genre.ifBlank { "GLZ Radio" },
                color = DtvChrome.SecondaryText,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
