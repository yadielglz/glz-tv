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
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.draw.drawBehind
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
import com.glztv.app.ui.components.QuickSettingsDialog
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.LocalTvViewport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HomeBg = Color(0xFF06080B)
private val HomeElectricBlue = Color(0xFF1990FF)
private val HomeDivider = Color(0xFF2B3038)
private val HomeCardBorder = Color(0xFF252B34)
private val HomePrimaryText = Color(0xFFE2E6EC)
private val HomeSecondaryText = Color(0xFF88909B)
private val HomeTopBarGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFA22262D),
        0.55f to Color(0xFA14171D),
        1.00f to Color(0xFA0E1014)
    )
)
private val HomeCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF161A21), Color(0xFF0F1217))
)
private val HomeHeroFallbackGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF090D14), Color(0xFF102033), Color(0xFF090D14))
)
private val HomeHeroHorizontalScrim = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xF506080B),
        0.44f to Color(0xCC06080B),
        0.74f to Color(0x3806080B),
        1.00f to Color(0x6606080B)
    )
)
private val HomeHeroVerticalScrim = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color(0x5506080B),
        0.45f to Color(0x1406080B),
        0.85f to Color(0xD906080B),
        1.00f to Color(0xFF06080B)
    )
)

/**
 * GLZ TV DirecTV + Google TV Hybrid Home Screen (v1.48.4.00).
 *
 * 1. Slim glossy DirecTV-style top command bar with #1990FF electric-blue active indicator.
 * 2. Full-bleed Live TV hero stage with broadcast channel metadata and 2 clean primary actions.
 * 3. Persistent Mini Radio Player bar when audio is active.
 * 4. Decluttered, auto-hiding content dock:
 *    - Live Sports & Events (when active)
 *    - My Channels (merged Favorites + Recent + Top Lineup)
 *    - Your Apps (Google TV tile richness + long-press management in Family mode)
 *    - Movies & On Demand (cinematic 2:3 posters when available)
 *    - GLZ Radio (square station cards when available)
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
    var showQuickSettings by remember { mutableStateOf(false) }

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

    val timeFmt = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val amPmFmt = remember { SimpleDateFormat("a", Locale.getDefault()) }
    fun formatCompactTime(millis: Long): String {
        val date = Date(millis)
        val suffix = amPmFmt.format(date).lowercase(Locale.getDefault()).firstOrNull()?.toString() ?: ""
        return "${timeFmt.format(date)}$suffix"
    }

    // Hero spotlight channel (GLZ Hub preview override, or resume/first channel)
    val heroChannel = remember(channels, previewChannel, resumeChannel) {
        previewChannel ?: resumeChannel ?: channels.firstOrNull()
    }

    // Active Live Events (auto-hidden when empty)
    val eventChannels = remember(channels) {
        channels.filter { it.isLiveEvent() }
    }

    // Merged "My Channels" row: Favorites + Resume + Recent + top non-event lineup (deduplicated)
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
        delay(120)
        runCatching { watchLiveFocus.requestFocus() }
    }

    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(32.dp)
    val heroHeight = if (viewport.isCompactHeight) 188.dp else 215.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── 1. Slim DirecTV-Style Glossy Top Command Bar ──────────────────────
            HybridTopCommandBar(
                nowTimeText = formatCompactTime(now),
                weather = weather,
                guestLabel = guestName.ifBlank { experience.propertyName.ifBlank { "Guest" } },
                hasUpdate = hasUpdate,
                sportsBarKioskEnabled = sportsBarKioskEnabled,
                compact = viewport.isCompactHeight,
                horizontalPad = horizontalPad,
                onSelectHome = { scope.launch { dockListState.animateScrollToItem(0) } },
                onSelectLiveTv = {
                    (heroChannel ?: channels.firstOrNull())?.let(onWatchChannel) ?: onOpenGuide()
                },
                onSelectGuide = onOpenGuide,
                onSelectRadio = onOpenRadio,
                onSelectSportsBar = onStartSportsBarKiosk,
                onOpenUpdate = onOpenUpdate,
                onOpenWeather = onOpenWeather,
                onOpenWelcome = onOpenWelcome,
                onOpenSettings = { showQuickSettings = true }
            )

            // ── 2. Full-Bleed Live TV Hero Stage ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clipToBounds()
                    .background(HomeHeroFallbackGradient)
            ) {
                if (previewContent != null) {
                    Box(Modifier.fillMaxSize()) {
                        previewContent()
                    }
                }
                // Cinematic left + bottom broadcast scrims
                Box(Modifier.fillMaxSize().background(HomeHeroHorizontalScrim))
                Box(Modifier.fillMaxSize().background(HomeHeroVerticalScrim))

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
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (active != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoogleTvChannelLogo(
                                    channel = active,
                                    size = if (viewport.isCompactHeight) 34.dp else 40.dp,
                                    guide = guide,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                Text(
                                    text = active.number.ifBlank { "LIVE" },
                                    color = HomePrimaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = active.name,
                                    color = HomeSecondaryText,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x33EF4444), RoundedCornerShape(3.dp))
                                        .border(1.dp, Color(0x66EF4444), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(GoogleTvTokens.LiveRed)
                                        )
                                        Text(
                                            text = "LIVE",
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
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
                                fontSize = if (viewport.isCompactHeight) 24.sp else 30.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val heroSubtitle = remember(currentProg, active.group) {
                                buildList {
                                    if (currentProg != null) {
                                        add("${formatCompactTime(currentProg.startMillis)} - ${formatCompactTime(currentProg.endMillis)}")
                                    } else {
                                        add("Live Broadcast")
                                    }
                                    if (active.group.isNotBlank()) add(active.group)
                                }.joinToString(", ")
                            }
                            Text(
                                text = heroSubtitle,
                                color = HomeSecondaryText,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (currentProg != null) {
                                Box(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.16f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progProgress)
                                            .fillMaxHeight()
                                            .background(HomeElectricBlue)
                                    )
                                }
                            }

                            Spacer(Modifier.height(2.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HybridHeroButton(
                                    label = "Watch Live",
                                    primary = true,
                                    modifier = Modifier.focusRequester(watchLiveFocus),
                                    onClick = { onWatchChannel(active) }
                                )
                                HybridHeroButton(
                                    label = "Full Guide",
                                    primary = false,
                                    onClick = onOpenGuide
                                )
                                HybridHeroIconButton(
                                    isFavorite = active.id in favorites,
                                    onClick = { onToggleFavorite(active.id) }
                                )
                            }
                        } else {
                            Text(
                                text = "Welcome to GLZ TV",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = "Open Guide or Radio to start streaming.",
                                color = HomeSecondaryText,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HybridHeroButton(
                                    label = "Open Guide",
                                    primary = true,
                                    modifier = Modifier.focusRequester(watchLiveFocus),
                                    onClick = onOpenGuide
                                )
                                HybridHeroButton(
                                    label = "GLZ Radio",
                                    primary = false,
                                    onClick = onOpenRadio
                                )
                            }
                        }
                    }

                    // Bottom-right dynamic Live Events indicator pill (auto-hides if none)
                    if (eventChannels.isNotEmpty()) {
                        HybridLiveEventsBadge(
                            count = eventChannels.size,
                            onClick = { onWatchChannel(eventChannels.first()) }
                        )
                    }
                }

                // Hairline bottom divider under hero stage
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .background(HomeDivider.copy(alpha = 0.7f))
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

            // ── 4. Decluttered Content Dock Below the Fold ────────────────────────
            LazyColumn(
                state = dockListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Row 1: Live Sports & Events (only shown when live events are active)
                if (eventChannels.isNotEmpty()) {
                    item(key = "row-live-events") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HybridSectionTitle(
                                title = "LIVE SPORTS & EVENTS",
                                trailing = "${eventChannels.size} LIVE NOW",
                                showLiveDot = true,
                                horizontalPad = horizontalPad
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(eventChannels, key = { "ev-${it.id}" }) { eventCh ->
                                    val prog = remember(eventCh.id, guide, now) {
                                        guide.nowProgrammeForChannel(eventCh, now)
                                    }
                                    val pct = prog?.let {
                                        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                                            .coerceIn(0f, 1f)
                                    } ?: 0.5f
                                    HybridLiveEventCard(
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
                    item(key = "row-my-channels") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HybridSectionTitle(
                                title = "MY CHANNELS",
                                trailing = "FAVORITES & RECENT",
                                horizontalPad = horizontalPad
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(myChannels, key = { "mych-${it.id}" }) { ch ->
                                    val prog = remember(ch.id, guide, now) {
                                        guide.nowProgrammeForChannel(ch, now)
                                    }
                                    val pct = prog?.let {
                                        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                                            .coerceIn(0f, 1f)
                                    } ?: 0f
                                    HybridChannelCard(
                                        channel = ch,
                                        programmeTitle = prog?.title ?: ch.eventTitle ?: "Live Broadcast",
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

                // Row 3: Your Apps (Family installed apps or managed entertainment apps)
                if (visibleInstalledApps.isNotEmpty() || entertainmentApps.isNotEmpty()) {
                    item(key = "row-apps") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HybridSectionTitle(
                                title = "YOUR APPS",
                                trailing = if (isFamilyMode && visibleInstalledApps.isNotEmpty()) "HOLD OK FOR OPTIONS" else null,
                                horizontalPad = horizontalPad
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isFamilyMode && visibleInstalledApps.isNotEmpty()) {
                                    items(visibleInstalledApps, key = { "inst-${it.packageName}" }) { app ->
                                        HybridInstalledAppTile(
                                            app = app,
                                            onClick = { LauncherAppsManager.launchApp(context, app.packageName) },
                                            onLongPress = { contextMenuApp = app }
                                        )
                                    }
                                } else {
                                    items(entertainmentApps, key = { "ent-${it.packageName}" }) { app ->
                                        HybridEntertainmentAppTile(
                                            app = app,
                                            onClick = { onLaunchApp(app) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Row 4: Movies & On Demand (cinematic Google TV 2:3 posters, auto-hides if empty)
                if (movies.isNotEmpty()) {
                    item(key = "row-movies") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HybridSectionTitle(
                                title = "MOVIES & ON DEMAND",
                                trailing = "${movies.size} TITLES",
                                horizontalPad = horizontalPad
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(movies, key = { "mov-${it.id}" }) { movie ->
                                    HybridMoviePosterCard(
                                        movie = movie,
                                        onClick = { onWatchMovie(movie) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 5: GLZ Radio (square station cards, auto-hides if empty)
                if (radioStations.isNotEmpty()) {
                    item(key = "row-radio") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HybridSectionTitle(
                                title = "GLZ RADIO",
                                trailing = "${radioStations.size} STATIONS",
                                horizontalPad = horizontalPad
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(radioStations, key = { "rad-${it.code}-${it.name}" }) { station ->
                                    val isCurrent = currentRadioStation?.code == station.code &&
                                        currentRadioStation?.streamUrl == station.streamUrl
                                    HybridRadioStationCard(
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

        // Quick Settings Dialog
        if (showQuickSettings) {
            QuickSettingsDialog(
                networkInfo = networkInfo,
                isFamilyChannel = isFamilyChannel,
                onOpenSpeedTest = onOpenSpeedTest,
                onOpenFullSettings = onOpenSettings,
                onDismiss = { showQuickSettings = false }
            )
        }
    }
}

@Composable
private fun HybridTopCommandBar(
    nowTimeText: String,
    weather: WeatherInfo?,
    guestLabel: String,
    hasUpdate: Boolean,
    sportsBarKioskEnabled: Boolean,
    compact: Boolean,
    horizontalPad: androidx.compose.ui.unit.Dp,
    onSelectHome: () -> Unit,
    onSelectLiveTv: () -> Unit,
    onSelectGuide: () -> Unit,
    onSelectRadio: () -> Unit,
    onSelectSportsBar: () -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenWelcome: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 46.dp else 52.dp)
                .background(HomeTopBarGradient)
                .padding(horizontal = horizontalPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Brand + DirecTV-style navigation tabs
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 18.dp)
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(HomeElectricBlue)
                    )
                    Text(
                        text = "GLZ TV",
                        color = HomePrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                }
                Box(Modifier.width(1.dp).height(24.dp).background(HomeDivider))

                HybridTopBarTab(label = "Home", active = true, onClick = onSelectHome)
                HybridTopBarTab(label = "Live TV", active = false, onClick = onSelectLiveTv)
                HybridTopBarTab(label = "Guide", active = false, onClick = onSelectGuide)
                HybridTopBarTab(label = "Radio", active = false, onClick = onSelectRadio)
                if (sportsBarKioskEnabled) {
                    HybridTopBarTab(label = "Sports Bar", active = false, onClick = onSelectSportsBar)
                }
            }

            // Right: Update (if available) · Weather · Time · Guest · Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasUpdate) {
                    HybridStatusChip(
                        label = "Update",
                        accent = true,
                        onClick = onOpenUpdate
                    ) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = "Update available",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (weather != null) {
                    HybridStatusChip(
                        label = "${weather.temperature}°",
                        onClick = onOpenWeather
                    ) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = "Weather",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = nowTimeText,
                    color = HomePrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                HybridStatusChip(
                    label = guestLabel.take(18),
                    onClick = onOpenWelcome
                )

                HybridStatusChip(
                    label = null,
                    onClick = onOpenSettings
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = HomePrimaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(HomeDivider))
    }
}

@Composable
private fun HybridTopBarTab(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (isFocused || active) Color.Black else Color.Transparent)
            .drawBehind {
                if (isFocused || active) {
                    val barHeight = 3.dp.toPx()
                    drawRect(
                        color = if (isFocused) HomeElectricBlue else HomeElectricBlue.copy(alpha = 0.75f),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(size.width, barHeight)
                    )
                }
            }
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
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = label,
            color = if (isFocused || active) Color.White else HomeSecondaryText,
            fontSize = 13.sp,
            fontWeight = if (isFocused || active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun HybridStatusChip(
    label: String?,
    accent: Boolean = false,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val bg = when {
        isFocused -> Color.Black
        accent -> HomeElectricBlue
        else -> Color(0xFF171B22)
    }
    val borderColor = when {
        isFocused -> HomeElectricBlue
        accent -> HomeElectricBlue
        else -> HomeDivider
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(if (isFocused) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(4.dp))
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
            .padding(horizontal = if (label == null) 8.dp else 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leadingIcon?.invoke()
        if (label != null) {
            Text(
                text = label,
                color = if (isFocused || accent) Color.White else HomePrimaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HybridHeroButton(
    label: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val bg = when {
        isFocused && primary -> Color.White
        isFocused -> Color.Black
        primary -> HomePrimaryText
        else -> Color(0xCC161A21)
    }
    val fg = when {
        primary -> Color.Black
        isFocused -> Color.White
        else -> HomePrimaryText
    }
    val border = when {
        isFocused -> HomeElectricBlue
        primary -> Color.Transparent
        else -> HomeDivider
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(if (isFocused) 2.dp else 1.dp, border, RoundedCornerShape(4.dp))
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
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (primary) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HybridHeroIconButton(
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.Black else Color(0xCC161A21))
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) HomeElectricBlue else HomeDivider, RoundedCornerShape(4.dp))
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
            tint = if (isFavorite) GoogleTvTokens.StarGold else HomePrimaryText,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HybridLiveEventsBadge(
    count: Int,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.Black else Color(0xCC0D1015))
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) HomeElectricBlue else HomeDivider, RoundedCornerShape(4.dp))
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(GoogleTvTokens.LiveRed)
        )
        Text(
            text = "$count Live Event${if (count == 1) "" else "s"} Now",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HybridSectionTitle(
    title: String,
    trailing: String? = null,
    showLiveDot: Boolean = false,
    horizontalPad: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showLiveDot) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GoogleTvTokens.LiveRed)
                )
            }
            Text(
                text = title,
                color = HomeSecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp
            )
        }
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                color = Color(0xFF646C79),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp
            )
        }
    }
}

@Composable
private fun HybridLiveEventCard(
    channel: Channel,
    progress: Float,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val league = channel.eventLeague?.takeIf { it.isNotBlank() } ?: "LIVE"
    val title = channel.eventTitle?.takeIf { it.isNotBlank() } ?: channel.name

    Box(
        modifier = Modifier
            .width(248.dp)
            .height(92.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.Black else Color(0xFF14181F))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) HomeElectricBlue else HomeCardBorder,
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
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(HomeElectricBlue.copy(alpha = 0.20f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = league.uppercase(Locale.getDefault()),
                        color = Color(0xFF60B2FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "● LIVE · ${channel.number.ifBlank { "PPV" }}",
                    color = Color(0xFFF87171),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = title,
                color = if (isFocused) Color.White else HomePrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                        .fillMaxHeight()
                        .background(HomeElectricBlue)
                )
            }
        }
    }
}

@Composable
private fun HybridChannelCard(
    channel: Channel,
    programmeTitle: String,
    progress: Float,
    isFavorite: Boolean,
    guide: EpgGuide,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(208.dp)
            .height(92.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(HomeCardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) HomeElectricBlue else HomeCardBorder,
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
            .padding(11.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoogleTvChannelLogo(
                    channel = channel,
                    size = 28.dp,
                    guide = guide,
                    shape = RoundedCornerShape(4.dp)
                )
                Text(
                    text = "${channel.number.ifBlank { "—" }} · ${channel.name}",
                    color = if (isFocused) Color.White else HomePrimaryText,
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
                        tint = GoogleTvTokens.StarGold,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Text(
                text = programmeTitle,
                color = if (isFocused) HomePrimaryText else HomeSecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(HomeElectricBlue)
                    )
                }
            }
        }
    }
}

@Composable
private fun HybridInstalledAppTile(
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
            .width(126.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color.Black else Color(0xFF14181F))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) HomeElectricBlue else HomeCardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focused ->
                isFocused = focused.isFocused
                if (!focused.isFocused) {
                    longPressJob?.cancel()
                }
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
                        if (!longPressTriggered) {
                            onClick()
                        }
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
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Icon(
                Icons.Default.Apps,
                contentDescription = app.label,
                tint = HomePrimaryText,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            color = if (isFocused) Color.White else HomePrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HybridEntertainmentAppTile(
    app: EntertainmentApp,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(126.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color.Black else Color(0xFF14181F))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) HomeElectricBlue else HomeCardBorder,
                shape = RoundedCornerShape(8.dp)
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
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
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.name,
            color = if (isFocused) Color.White else HomePrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HybridMoviePosterCard(
    movie: Movie,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .width(124.dp)
            .height(184.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF14181F))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) HomeElectricBlue else HomeCardBorder,
                shape = RoundedCornerShape(6.dp)
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
                            1.0f to Color(0xE605070A)
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
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            movie.releaseYear?.let { year ->
                Text(
                    text = year.toString(),
                    color = HomeSecondaryText,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun HybridRadioStationCard(
    station: RadioStation,
    isActive: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(138.dp)
            .height(116.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) Color.Black else Color(0xFF14181F))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = when {
                    isFocused -> HomeElectricBlue
                    isActive -> HomeElectricBlue.copy(alpha = 0.7f)
                    else -> HomeCardBorder
                },
                shape = RoundedCornerShape(6.dp)
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
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(HomeElectricBlue.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Radio,
                        contentDescription = null,
                        tint = HomeElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (isActive) {
                Text(
                    text = "ON AIR",
                    color = HomeElectricBlue,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        }
        Column {
            Text(
                text = station.name,
                color = if (isFocused) Color.White else HomePrimaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = station.genre.ifBlank { "GLZ Radio" },
                color = HomeSecondaryText,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
