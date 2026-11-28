package com.glztv.app.ui.mobile

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.Movie
import com.glztv.app.data.SportsChannelMatcher
import com.glztv.app.data.SportsGame
import com.glztv.app.player.PlaybackControlState
import com.glztv.app.ui.components.DtvChannelSurfBar
import com.glztv.app.ui.components.GoogleTvChannelLogo
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.formatDtvCompactTime
import kotlinx.coroutines.delay

/**
 * Mobile-only touch Live TV player.
 *
 * - Portrait mode: 16:9 live player on top with full touch controls + Now/Next EPG & quick
 *   scrollable channel switcher below.
 * - Fullscreen mode: locks landscape + hides system bars; tap video for touch controls,
 *   swipe up/down to channel-surf with 2s dwell auto-tune (or tap bar to tune immediately).
 */
@Composable
fun MobileLivePlayerScreen(
    channel: Channel,
    channels: List<Channel>,
    guide: EpgGuide,
    sportsGames: List<SportsGame> = emptyList(),
    favorites: Set<String>,
    captionsEnabled: Boolean,
    sleepTimerRemainingMinutes: Int?,
    onToggleFavorite: (String) -> Unit,
    onToggleCaptions: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onPreviousChannel: () -> Unit,
    onTune: (Channel) -> Unit,
    onExit: () -> Unit,
    videoPlayerSlot: @Composable (Channel, PlaybackControlState, Modifier) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val controlState = remember { PlaybackControlState() }
    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var controlsRevision by remember { mutableStateOf(0) }
    var showTrackSheet by remember { mutableStateOf(false) }
    var surfChannel by remember { mutableStateOf<Channel?>(null) }
    var surfRevision by remember { mutableStateOf(0) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    val currentProgramme = remember(channel, guide, now) {
        guide.nowProgrammeForChannel(channel, now)
    }
    val nextProgramme = remember(channel, guide, currentProgramme, now) {
        guide.nextProgrammeForChannel(channel, currentProgramme?.endMillis ?: now)
    }

    // Orientation & immersive system bars management (always restores portrait on dispose)
    DisposableEffect(activity, isFullscreen) {
        if (activity != null) {
            runCatching {
                activity.requestedOrientation = if (isFullscreen) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            runCatching {
                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                if (isFullscreen) {
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        onDispose {
            if (activity != null) {
                runCatching {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                runCatching {
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                        .show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(showControls, controlsRevision, showTrackSheet) {
        if (showControls && !showTrackSheet) {
            delay(4_000L)
            showControls = false
        }
    }
    LaunchedEffect(channel.id, channel.streamUrl) {
        surfChannel = null
        showControls = true
        controlsRevision++
    }
    LaunchedEffect(surfChannel?.id, surfChannel?.streamUrl, surfRevision) {
        val target = surfChannel ?: return@LaunchedEffect
        delay(2_000L)
        surfChannel = null
        if (target != channel) onTune(target)
    }

    fun stepChannel(direction: Int) {
        if (channels.isEmpty()) return
        val idx = channels.indexOfChannel(channel).coerceAtLeast(0)
        onTune(channels[(idx + direction + channels.size) % channels.size])
    }

    fun stepSurf(direction: Int) {
        if (channels.isEmpty()) return
        val base = surfChannel ?: channel
        val idx = channels.indexOfChannel(base).coerceAtLeast(0)
        now = System.currentTimeMillis()
        surfChannel = channels[(idx + direction + channels.size) % channels.size]
        surfRevision++
    }

    BackHandler {
        when {
            showTrackSheet -> showTrackSheet = false
            surfChannel != null -> surfChannel = null
            isFullscreen -> isFullscreen = false
            else -> onExit()
        }
    }

    val isFav = channel.id in favorites
    val showTitle = remember(channel, guide, sportsGames, now) {
        SportsChannelMatcher.resolveChannelShowTitle(channel, guide, sportsGames, now)
    }
    val progress = currentProgramme?.let {
        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
            .coerceIn(0f, 1f)
    } ?: 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D12))
            .then(if (!isFullscreen) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)
    ) {
        // Video container (16:9 in portrait, full screen in landscape)
        val videoModifier = if (isFullscreen) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        }

        var swipeAccum by remember { mutableStateOf(0f) }
        Box(
            modifier = videoModifier
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (surfChannel != null) {
                                surfChannel = null
                            } else {
                                showControls = !showControls
                                if (showControls) controlsRevision++
                            }
                        }
                    )
                }
                .pointerInput(channels.size, channel.id) {
                    detectVerticalDragGestures(
                        onDragStart = { swipeAccum = 0f },
                        onVerticalDrag = { _, dragAmount -> swipeAccum += dragAmount },
                        onDragEnd = {
                            if (swipeAccum < -70f) {
                                showControls = false
                                stepSurf(1)
                            } else if (swipeAccum > 70f) {
                                showControls = false
                                stepSurf(-1)
                            }
                            swipeAccum = 0f
                        }
                    )
                }
        ) {
            videoPlayerSlot(channel, controlState, Modifier.fillMaxSize())

            // Touch Controls Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls && surfChannel == null,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(220))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xCC07090D),
                                    Color(0x4407090D),
                                    Color(0xD807090D)
                                )
                            )
                        )
                ) {
                    // Top touch bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = { if (isFullscreen) isFullscreen = false else onExit() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        GoogleTvChannelLogo(
                            channel = channel,
                            size = 30.dp,
                            guide = guide,
                            shape = RoundedCornerShape(4.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (channel.number.isNotBlank()) {
                                    Text(
                                        text = channel.number,
                                        color = DtvChrome.ElectricBlueSoft,
                                        fontSize = 13.sp,
                                        fontFamily = BarlowCondensedFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = channel.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = showTitle,
                                color = DtvChrome.SecondaryText,
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1200)
                            )
                        }
                        // Favorite toggle
                        IconButton(onClick = {
                            controlsRevision++
                            onToggleFavorite(channel.id)
                        }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) Color(0xFFFACC15) else Color.White
                            )
                        }
                        // Sleep timer pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (sleepTimerRemainingMinutes != null) DtvChrome.ElectricBlue.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .clickable {
                                    controlsRevision++
                                    onCycleSleepTimer()
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerRemainingMinutes != null) DtvChrome.ElectricBlueSoft else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            if (sleepTimerRemainingMinutes != null) {
                                Text(
                                    text = "${sleepTimerRemainingMinutes}m",
                                    color = DtvChrome.ElectricBlueSoft,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Center touch transport controls (Prev Channel / Play-Pause / Next Channel)
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(26.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    controlsRevision++
                                    stepChannel(-1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous Channel",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(DtvChrome.ElectricBlue)
                                .clickable {
                                    controlsRevision++
                                    controlState.togglePlayPause()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (controlState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (controlState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    controlsRevision++
                                    stepChannel(1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next Channel",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Bottom touch controls bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (currentProgramme != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .background(DtvChrome.ElectricBlue)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFDC2626))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = BarlowCondensedFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (currentProgramme != null) {
                                Text(
                                    text = "${formatDtvCompactTime(currentProgramme.startMillis)} - ${formatDtvCompactTime(currentProgramme.endMillis)}",
                                    color = DtvChrome.SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            // Last channel quick flip
                            IconButton(
                                onClick = {
                                    controlsRevision++
                                    onPreviousChannel()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "Last Channel",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Mute toggle
                            IconButton(
                                onClick = {
                                    controlsRevision++
                                    controlState.toggleMute()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (controlState.isMuted) Icons.AutoMirrored.Filled.VolumeOff
                                    else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = if (controlState.isMuted) Color(0xFFF87171) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Captions toggle
                            IconButton(
                                onClick = {
                                    controlsRevision++
                                    onToggleCaptions()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ClosedCaption,
                                    contentDescription = "Captions",
                                    tint = if (captionsEnabled) DtvChrome.ElectricBlueSoft else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Audio / Track picker
                            if (controlState.audioTracks.isNotEmpty() || controlState.subtitleTracks.isNotEmpty()) {
                                IconButton(
                                    onClick = { showTrackSheet = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Subtitles,
                                        contentDescription = "Audio & Subtitles",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            // Fullscreen / Rotate toggle
                            IconButton(
                                onClick = {
                                    controlsRevision++
                                    isFullscreen = !isFullscreen
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Swipe-to-surf bottom bar overlay (tap to tune immediately or wait 2s)
            androidx.compose.animation.AnimatedVisibility(
                visible = surfChannel != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                enter = fadeIn(tween(140)) + slideInVertically(tween(140)) { it / 2 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 2 }
            ) {
                val preview = surfChannel
                if (preview != null) {
                    val surfProg = remember(preview, guide, now) {
                        guide.nowProgrammeForChannel(preview, now)
                    }
                    Box(
                        modifier = Modifier.clickable {
                            surfChannel = null
                            if (preview != channel) onTune(preview)
                        }
                    ) {
                        DtvChannelSurfBar(
                            surfChannel = preview,
                            currentProgramme = surfProg,
                            guide = guide,
                            nowMillis = now,
                            isCurrentChannel = preview == channel
                        )
                    }
                }
            }
        }

        // Portrait Companion Panel (Now & Next EPG + Quick Channel List)
        if (!isFullscreen) {
            var selectedGroup by remember { mutableStateOf("All") }
            val validLiveEvents = remember(channels, guide, sportsGames, now) {
                SportsChannelMatcher.filterValidLiveEvents(channels, guide, sportsGames, now)
            }
            val validLiveEventKeys = remember(validLiveEvents) {
                validLiveEvents.map { "${it.id}|${it.streamUrl}" }.toSet()
            }
            val groups = remember(channels, validLiveEvents) {
                val raw = channels.map { it.group.ifBlank { "Live" } }.distinct().filterNot { grp ->
                    val u = grp.uppercase()
                    (u.contains("EVENT") || u.contains("PPV")) && validLiveEvents.isEmpty()
                }
                buildList {
                    add("All")
                    add("Favorites")
                    if (validLiveEvents.isNotEmpty() && raw.none { it.equals("Live Events", ignoreCase = true) }) {
                        add("Live Events")
                    }
                    addAll(raw)
                }
            }
            val filteredChannels = remember(channels, validLiveEventKeys, selectedGroup, favorites, channel) {
                channels.filter { ch ->
                    if (ch != channel && ch.isLiveEvent() && "${ch.id}|${ch.streamUrl}" !in validLiveEventKeys) {
                        return@filter false
                    }
                    when (selectedGroup) {
                        "All" -> true
                        "Favorites" -> ch.id in favorites
                        "Live Events" -> ch.isLiveEvent()
                        else -> ch.group.ifBlank { "Live" } == selectedGroup
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Polished Now Playing & Up Next summary card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF131B28), Color(0xFF0E131D))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x2EEF4444))
                                .border(1.dp, Color(0x66EF4444), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "● ON NOW",
                                color = Color(0xFFFCA5A5),
                                fontSize = 10.sp,
                                fontFamily = BarlowCondensedFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            )
                        }
                        if (currentProgramme != null) {
                            Text(
                                text = "${formatDtvCompactTime(currentProgramme.startMillis)} – ${formatDtvCompactTime(currentProgramme.endMillis)}",
                                color = DtvChrome.SecondaryText,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "Swipe ▲▼ to surf",
                            color = DtvChrome.MutedText,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = showTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentProgramme != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(DtvChrome.ElectricBlue)
                            )
                        }
                    }
                    if (nextProgramme != null) {
                        Text(
                            text = "Up Next (${formatDtvCompactTime(nextProgramme.startMillis)}): ${nextProgramme.title}",
                            color = DtvChrome.SecondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFF1E2838))

                // Category filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { grp ->
                        val active = grp == selectedGroup
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) DtvChrome.ElectricBlue else Color(0xFF141B27))
                                .border(
                                    1.dp,
                                    if (active) DtvChrome.ElectricBlue else Color(0xFF232D3F),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedGroup = grp }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = grp,
                                color = if (active) Color.White else DtvChrome.SecondaryText,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Quick channel list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    itemsIndexed(filteredChannels, key = { index, ch -> "m-player-$index-${ch.id}-${ch.streamUrl}" }) { _, ch ->
                        val chProg = remember(ch, guide, now) { guide.nowProgrammeForChannel(ch, now) }
                        val chShowTitle = remember(ch, guide, sportsGames, now) {
                            SportsChannelMatcher.resolveChannelShowTitle(ch, guide, sportsGames, now)
                        }
                        val chProgress = chProg?.let {
                            ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                                .coerceIn(0f, 1f)
                        } ?: 0f
                        val isCurrent = ch == channel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) Color(0xFF15273E) else Color(0xFF121822))
                                .border(
                                    1.dp,
                                    if (isCurrent) DtvChrome.ElectricBlue else Color(0xFF1F2939),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onTune(ch) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GoogleTvChannelLogo(
                                channel = ch,
                                size = 40.dp,
                                guide = guide,
                                shape = RoundedCornerShape(6.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (ch.number.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isCurrent) DtvChrome.ElectricBlue
                                                    else DtvChrome.ElectricBlue.copy(alpha = 0.16f)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = ch.number,
                                                color = if (isCurrent) Color.White else DtvChrome.ElectricBlueSoft,
                                                fontSize = 11.sp,
                                                fontFamily = BarlowCondensedFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = ch.callSign.ifBlank { ch.name },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(DtvChrome.ElectricBlue.copy(alpha = 0.25f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "PLAYING",
                                                color = DtvChrome.ElectricBlueSoft,
                                                fontSize = 9.sp,
                                                fontFamily = BarlowCondensedFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.6.sp
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = chShowTitle,
                                    color = if (isCurrent) DtvChrome.PrimaryText else DtvChrome.SecondaryText,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (chProg != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.5.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.White.copy(alpha = 0.12f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(chProgress)
                                                .fillMaxHeight()
                                                .background(DtvChrome.ElectricBlue)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { onToggleFavorite(ch.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                val fav = ch.id in favorites
                                Icon(
                                    imageVector = if (fav) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (fav) Color(0xFFFACC15) else DtvChrome.MutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTrackSheet) {
        MobileTrackPickerOverlay(
            controlState = controlState,
            onDismiss = { showTrackSheet = false }
        )
    }
}

/**
 * Mobile-only touch VOD Movie Player (v1.48.5.30-mobile).
 * Supports tap controls, -10s/+10s skip, interactive seek slider, audio/subtitle picker,
 * sleep timer, and portrait/fullscreen-landscape toggle.
 */
@Composable
fun MobileMoviePlayerScreen(
    movie: Movie,
    captionsEnabled: Boolean,
    sleepTimerRemainingMinutes: Int?,
    onToggleCaptions: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onExit: () -> Unit,
    moviePlayerSlot: @Composable (Movie, PlaybackControlState, Modifier) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val controlState = remember { PlaybackControlState() }
    var isFullscreen by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var controlsRevision by remember { mutableStateOf(0) }
    var showTrackSheet by remember { mutableStateOf(false) }

    DisposableEffect(activity, isFullscreen) {
        if (activity != null) {
            runCatching {
                activity.requestedOrientation = if (isFullscreen) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            runCatching {
                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                if (isFullscreen) {
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        onDispose {
            if (activity != null) {
                runCatching {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                runCatching {
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                        .show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    LaunchedEffect(showControls, controlsRevision, showTrackSheet) {
        if (showControls && !showTrackSheet) {
            delay(4_000L)
            showControls = false
        }
    }

    BackHandler {
        when {
            showTrackSheet -> showTrackSheet = false
            else -> onExit()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(if (!isFullscreen) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)
    ) {
        val videoMod = if (isFullscreen) Modifier.fillMaxSize()
        else Modifier.fillMaxWidth().aspectRatio(16f / 9f)

        Box(
            modifier = videoMod
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                            if (showControls) controlsRevision++
                        }
                    )
                }
        ) {
            moviePlayerSlot(movie, controlState, Modifier.fillMaxSize())

            androidx.compose.animation.AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(220))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xCC07090D),
                                    Color(0x4407090D),
                                    Color(0xDC07090D)
                                )
                            )
                        )
                ) {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onExit) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1200)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (sleepTimerRemainingMinutes != null) DtvChrome.ElectricBlue.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .clickable {
                                    controlsRevision++
                                    onCycleSleepTimer()
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerRemainingMinutes != null) DtvChrome.ElectricBlueSoft else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            if (sleepTimerRemainingMinutes != null) {
                                Text(
                                    text = "${sleepTimerRemainingMinutes}m",
                                    color = DtvChrome.ElectricBlueSoft,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Center -10s / Play-Pause / +10s
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    controlsRevision++
                                    controlState.seekBy(-10_000L)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(DtvChrome.ElectricBlue)
                                .clickable {
                                    controlsRevision++
                                    controlState.togglePlayPause()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (controlState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    controlsRevision++
                                    controlState.seekBy(10_000L)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    // Bottom scrub slider + actions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        val duration = controlState.durationMs.coerceAtLeast(1L)
                        val position = controlState.currentPositionMs.coerceIn(0L, duration)
                        Slider(
                            value = position.toFloat(),
                            onValueChange = {
                                controlsRevision++
                                controlState.seekTo(it.toLong())
                            },
                            valueRange = 0f..duration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = DtvChrome.ElectricBlue,
                                activeTrackColor = DtvChrome.ElectricBlue,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatElapsedMs(position)} / ${formatElapsedMs(controlState.durationMs)}",
                                color = DtvChrome.PrimaryText,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    controlsRevision++
                                    controlState.toggleMute()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (controlState.isMuted) Icons.AutoMirrored.Filled.VolumeOff
                                    else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = if (controlState.isMuted) Color(0xFFF87171) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    controlsRevision++
                                    onToggleCaptions()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ClosedCaption,
                                    contentDescription = "Captions",
                                    tint = if (captionsEnabled) DtvChrome.ElectricBlueSoft else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (controlState.audioTracks.isNotEmpty() || controlState.subtitleTracks.isNotEmpty()) {
                                IconButton(
                                    onClick = { showTrackSheet = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Subtitles,
                                        contentDescription = "Tracks",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    controlsRevision++
                                    isFullscreen = !isFullscreen
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0A0D12))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                val meta = listOfNotNull(
                    movie.releaseYear?.toString(),
                    movie.runtimeMinutes?.let { "${it} min" },
                    movie.genres.take(3).joinToString(" · ").takeIf { it.isNotBlank() }
                ).joinToString("  •  ")
                if (meta.isNotBlank()) {
                    Text(text = meta, color = DtvChrome.ElectricBlueSoft, fontSize = 12.sp)
                }
                if (movie.description.isNotBlank()) {
                    Text(
                        text = movie.description,
                        color = DtvChrome.SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }

    if (showTrackSheet) {
        MobileTrackPickerOverlay(
            controlState = controlState,
            onDismiss = { showTrackSheet = false }
        )
    }
}

@Composable
private fun MobileTrackPickerOverlay(
    controlState: PlaybackControlState,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color(0xFF121721))
                .border(1.dp, DtvChrome.Divider, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio & Subtitles",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            if (controlState.audioTracks.isNotEmpty()) {
                Text("AUDIO", color = DtvChrome.ElectricBlueSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                controlState.audioTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (track.selected) DtvChrome.ElectricBlue.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                controlState.chooseAudio(track.id)
                                onDismiss()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = track.label,
                            color = if (track.selected) DtvChrome.ElectricBlueSoft else Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            if (controlState.subtitleTracks.isNotEmpty()) {
                Text("SUBTITLES", color = DtvChrome.ElectricBlueSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            controlState.chooseSubtitle(null)
                            onDismiss()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Off", color = Color.White, fontSize = 14.sp)
                }
                controlState.subtitleTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (track.selected) DtvChrome.ElectricBlue.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                controlState.chooseSubtitle(track.id)
                                onDismiss()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = track.label,
                            color = if (track.selected) DtvChrome.ElectricBlueSoft else Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun List<Channel>.indexOfChannel(target: Channel): Int {
    val exact = indexOfFirst {
        it === target || (it.id == target.id && it.streamUrl == target.streamUrl && it.number == target.number && it.name == target.name)
    }
    if (exact >= 0) return exact
    return indexOfFirst { it.id == target.id && it.streamUrl == target.streamUrl }
        .takeIf { it >= 0 }
        ?: indexOfFirst { it.id == target.id }
}

private fun formatElapsedMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
