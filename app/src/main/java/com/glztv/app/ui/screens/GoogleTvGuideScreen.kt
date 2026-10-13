package com.glztv.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.Programme
import com.glztv.app.ui.components.GoogleTvChannelLogo
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sharedTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun formatGuideTime(millis: Long): String {
    return synchronized(sharedTimeFormat) {
        sharedTimeFormat.format(Date(millis))
    }
}

/**
 * Authentic Google TV 1:1 Live Guide Experience.
 * Features:
 * - Top Category Pills Bar
 * - Top Hero Synopsis with live video PiP
 * - Sticky Channel Header Column (stays fixed on left)
 * - Proportional Synchronized Horizontal Timeline
 * - Real-time "NOW" indicator needle
 */
@Composable
fun GoogleTvGuideScreen(
    channels: List<Channel>,
    guide: EpgGuide,
    activeTunedChannel: Channel?,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onWatchChannel: (Channel) -> Unit,
    previewContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var focusedChannel by remember { mutableStateOf(activeTunedChannel ?: channels.firstOrNull()) }
    var focusedProgramme by remember { mutableStateOf<Programme?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    // Categories derived from channels
    val categories = remember(channels) {
        val groups = channels.mapNotNull { it.group.takeIf(String::isNotBlank) }.distinct()
        listOf("ALL", "FAVORITES", "RECENT") + groups
    }

    val filteredChannels = remember(channels, selectedCategory, favorites) {
        when (selectedCategory) {
            "ALL" -> channels
            "FAVORITES" -> channels.filter { it.id in favorites }
            "RECENT" -> channels.take(15)
            else -> channels.filter { it.group.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val displayChannel = focusedChannel ?: activeTunedChannel ?: filteredChannels.firstOrNull()

    // 30 minute timeline calculation
    val halfHour = 30 * 60 * 1000L
    val slots = 12 // 6 hours continuous scroll window
    val timelineStart = now - (now % halfHour)
    val timelineEnd = timelineStart + slots * halfHour
    val slotWidth = 150.dp
    val channelColumnWidth = 190.dp
    val totalTimelineWidth = slotWidth * slots
    val timelineScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GoogleTvTokens.BackgroundDark)
    ) {
        // 1. TOP CATEGORY PILLS BAR
        GoogleTvCategoryPillsBar(
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )

        // 2. TOP HERO SYNOPSIS PANEL WITH LIVE PREVIEW
        displayChannel?.let { ch ->
            GoogleTvHeroSynopsisPanel(
                channel = ch,
                focusedProgramme = focusedProgramme,
                guide = guide,
                now = now,
                isFavorite = ch.id in favorites,
                onToggleFavorite = { onToggleFavorite(ch.id) },
                onWatch = { onWatchChannel(ch) },
                previewContent = previewContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // 3. EPG GRID (STICKY CHANNEL COLUMN + SYNCHRONIZED TIMELINE)
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            val listState = rememberLazyListState()

            Column(Modifier.fillMaxSize()) {
                // Header Timeline Row (Sticky Time Slots Header)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    // "CHANNELS" Sticky Column Header
                    Box(
                        Modifier
                            .width(channelColumnWidth)
                            .fillMaxHeight()
                            .background(GoogleTvTokens.SurfaceCard, RoundedCornerShape(topStart = 8.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "CHANNELS",
                            Modifier.padding(start = 14.dp),
                            color = GoogleTvTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Horizontal Scrolling Time Ticks Header
                    Row(
                        Modifier
                            .weight(1f)
                            .horizontalScroll(timelineScrollState)
                    ) {
                        repeat(slots) { slotIndex ->
                            val slotTime = timelineStart + slotIndex * halfHour
                            Box(
                                Modifier
                                    .width(slotWidth)
                                    .fillMaxHeight()
                                    .background(GoogleTvTokens.SurfaceTimelineSlot)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    formatGuideTime(slotTime),
                                    Modifier.padding(start = 10.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoogleTvTokens.TextPrimary
                                )
                            }
                        }
                    }
                }

                // Channel Rows
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    itemsIndexed(filteredChannels, key = { index, ch -> "gtv-${ch.id}-$index" }) { _, channel ->
                        GoogleTvGridRow(
                            channel = channel,
                            guide = guide,
                            windowStart = timelineStart,
                            windowEnd = timelineEnd,
                            now = now,
                            slotWidth = slotWidth,
                            slots = slots,
                            halfHour = halfHour,
                            channelColumnWidth = channelColumnWidth,
                            totalTimelineWidth = totalTimelineWidth,
                            timelineScrollState = timelineScrollState,
                            isFavorite = channel.id in favorites,
                            onToggleFavorite = { onToggleFavorite(channel.id) },
                            onFocusChannel = {
                                focusedChannel = channel
                                val progs = guide.forChannel(channel)
                                focusedProgramme = progs.firstOrNull { it.startMillis <= now && it.endMillis > now }
                            },
                            onFocusProgramme = { prog ->
                                focusedChannel = channel
                                focusedProgramme = prog
                            },
                            onWatch = { onWatchChannel(channel) }
                        )
                    }
                }
            }

            // Real-time "NOW" Needle Cursor (uses layout offset so scrolling doesn't trigger recomposition)
            if (now in timelineStart..timelineEnd) {
                TimelineNeedle(
                    now = now,
                    timelineStart = timelineStart,
                    slots = slots,
                    halfHour = halfHour,
                    channelColumnWidth = channelColumnWidth,
                    totalTimelineWidth = totalTimelineWidth,
                    timelineScrollState = timelineScrollState,
                    maxWidth = maxWidth
                )
            }
        }
    }
}

/**
 * Top Category Pills Bar with Google TV focus physics.
 */
@Composable
private fun GoogleTvCategoryPillsBar(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.equals(selectedCategory, ignoreCase = true)
            var isFocused by remember { mutableStateOf(false) }

            Surface(
                onClick = { onSelectCategory(category) },
                shape = CircleShape,
                color = when {
                    isFocused -> Color.White
                    isSelected -> GoogleTvTokens.BlueAccent
                    else -> Color.White.copy(alpha = 0.08f)
                },
                contentColor = when {
                    isFocused -> Color.Black
                    isSelected -> Color(0xFF001F2A)
                    else -> Color.White
                },
                modifier = Modifier.googleTvFocusable(
                    shape = CircleShape,
                    focusedScale = 1.06f,
                    onFocusChange = { isFocused = it }
                )
            ) {
                Text(
                    text = category.uppercase(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Top Hero Synopsis Panel.
 * Left: Program Metadata, Title, Description, Progress, and Watch Action.
 * Right: Live stream PiP player window with rounded borders.
 */
@Composable
private fun GoogleTvHeroSynopsisPanel(
    channel: Channel,
    focusedProgramme: Programme?,
    guide: EpgGuide,
    now: Long,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onWatch: () -> Unit,
    previewContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val programmes = remember(channel, guide) { guide.forChannel(channel) }
    val current = remember(programmes, now) {
        programmes.firstOrNull { it.startMillis <= now && it.endMillis > now }
    }
    val activeProg = focusedProgramme ?: current
    val isLive = activeProg != null && activeProg.startMillis <= now && activeProg.endMillis > now

    val progress = activeProg?.let {
        if (it.startMillis <= now && it.endMillis > now) {
            ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L)).coerceIn(0f, 1f)
        } else 0f
    } ?: 0f

    val remainingMinutes = activeProg?.let {
        if (it.startMillis <= now && it.endMillis > now) {
            ((it.endMillis - now).coerceAtLeast(0L) + 59_999L) / 60_000L
        } else null
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = GoogleTvTokens.SurfaceCardTranslucent
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Metadata Column
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Channel Identity Line
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoogleTvChannelLogo(channel, 26.dp, guide, shape = RoundedCornerShape(6.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${channel.number.ifBlank { "CH" }} · ${channel.name}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    if (isLive) {
                        Surface(
                            shape = CircleShape,
                            color = GoogleTvTokens.LiveRed.copy(alpha = 0.20f)
                        ) {
                            Text(
                                "LIVE",
                                Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = GoogleTvTokens.LiveRed
                            )
                        }
                    }
                    activeProg?.let { prog ->
                        val startStr = formatGuideTime(prog.startMillis)
                        val endStr = formatGuideTime(prog.endMillis)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$startStr – $endStr",
                            color = GoogleTvTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        remainingMinutes?.let {
                            Text(
                                " · $it min left",
                                color = GoogleTvTokens.BlueAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Show Title
                Text(
                    activeProg?.title ?: "Live Programming",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Progress Bar
                if (isLive) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = GoogleTvTokens.BlueAccent,
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
                }

                // Description
                Text(
                    activeProg?.description?.takeIf(String::isNotBlank)
                        ?: "Watch live broadcast from ${channel.name}.",
                    color = GoogleTvTokens.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Quick Action Buttons
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onWatch,
                        shape = CircleShape,
                        color = GoogleTvTokens.BlueAccent,
                        contentColor = Color(0xFF001F2A),
                        modifier = Modifier.googleTvFocusable(shape = CircleShape, focusedScale = 1.06f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(14.dp))
                            Text("WATCH", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Surface(
                        onClick = onToggleFavorite,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.10f),
                        contentColor = if (isFavorite) GoogleTvTokens.StarGold else Color.White,
                        modifier = Modifier.googleTvFocusable(shape = CircleShape, focusedScale = 1.06f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                Modifier.size(13.dp),
                                tint = if (isFavorite) GoogleTvTokens.StarGold else Color.White
                            )
                            Text(if (isFavorite) "FAVORITED" else "FAVORITE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // Right PiP Video Preview
            Surface(
                modifier = Modifier
                    .width(180.dp)
                    .height(101.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black
            ) {
                previewContent()
            }
        }
    }
}

/**
 * Google TV EPG Row.
 * Left: Sticky Channel Badge (Never scrolls horizontally).
 * Right: Horizontal timeline with proportional programme duration blocks.
 */
@Composable
private fun GoogleTvGridRow(
    channel: Channel,
    guide: EpgGuide,
    windowStart: Long,
    windowEnd: Long,
    now: Long,
    slotWidth: Dp,
    slots: Int,
    halfHour: Long,
    channelColumnWidth: Dp,
    totalTimelineWidth: Dp,
    timelineScrollState: androidx.compose.foundation.ScrollState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onFocusChannel: () -> Unit,
    onFocusProgramme: (Programme) -> Unit,
    onWatch: () -> Unit
) {
    val programmes = remember(channel, guide) { guide.forChannel(channel) }
    val visible = remember(programmes, windowStart, windowEnd) {
        programmes.filter { it.endMillis > windowStart && it.startMillis < windowEnd }
    }
    var channelFocused by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        // 1. STICKY CHANNEL COLUMN BADGE
        Surface(
            onClick = onWatch,
            modifier = Modifier
                .width(channelColumnWidth)
                .fillMaxHeight()
                .googleTvFocusable(
                    shape = RoundedCornerShape(8.dp),
                    focusedScale = 1.02f,
                    onFocusChange = {
                        channelFocused = it
                        if (it) onFocusChannel()
                    }
                ),
            shape = RoundedCornerShape(8.dp),
            color = if (channelFocused) Color.White else GoogleTvTokens.SurfaceCard,
            contentColor = if (channelFocused) Color.Black else Color.White
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoogleTvChannelLogo(channel, 28.dp, guide, shape = RoundedCornerShape(6.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            channel.number.ifBlank { "TV" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (channelFocused) Color.Black else GoogleTvTokens.BlueAccent
                        )
                        if (isFavorite) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = GoogleTvTokens.StarGold,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                    Text(
                        channel.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2. SYNCHRONIZED TIMELINE PROGRAMME BLOCKS
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(timelineScrollState)
        ) {
            Box(
                Modifier
                    .width(totalTimelineWidth)
                    .fillMaxHeight()
            ) {
                if (visible.isEmpty()) {
                    Surface(
                        onClick = onWatch,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp)
                            .googleTvFocusable(shape = RoundedCornerShape(8.dp), focusedScale = 1.01f),
                        shape = RoundedCornerShape(8.dp),
                        color = GoogleTvTokens.SurfaceTimelineSlot
                    ) {
                        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                            Text(
                                "Live Broadcast",
                                fontSize = 11.sp,
                                color = GoogleTvTokens.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    visible.forEach { programme ->
                        val clippedStart = maxOf(programme.startMillis, windowStart)
                        val clippedEnd = minOf(programme.endMillis, windowEnd)
                        val durationHalfHours = ((clippedEnd - clippedStart).toFloat() / halfHour.toFloat()).coerceAtLeast(0.3f)
                        val startOffset = (((clippedStart - windowStart).toFloat() / halfHour.toFloat()) * slotWidth.value).dp
                        val blockWidth = (slotWidth * durationHalfHours).coerceAtLeast(40.dp)
                        val isCurrent = programme.startMillis <= now && programme.endMillis > now
                        var progFocused by remember { mutableStateOf(false) }

                        Surface(
                            onClick = onWatch,
                            modifier = Modifier
                                .offset(x = startOffset)
                                .width(blockWidth)
                                .fillMaxHeight()
                                .padding(horizontal = 1.dp)
                                .googleTvFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    focusedScale = 1.0f,
                                    onFocusChange = {
                                        progFocused = it
                                        if (it) onFocusProgramme(programme)
                                    }
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                progFocused -> Color.White
                                isCurrent -> GoogleTvTokens.SurfaceTimelineSlotActive
                                else -> GoogleTvTokens.SurfaceTimelineSlot
                            },
                            contentColor = if (progFocused) Color.Black else Color.White
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    programme.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent || progFocused) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val startFormatted = formatGuideTime(programme.startMillis)
                                Text(
                                    startFormatted,
                                    fontSize = 9.sp,
                                    color = if (progFocused) Color.Black.copy(alpha = 0.7f) else GoogleTvTokens.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineNeedle(
    now: Long,
    timelineStart: Long,
    slots: Int,
    halfHour: Long,
    channelColumnWidth: Dp,
    totalTimelineWidth: Dp,
    timelineScrollState: androidx.compose.foundation.ScrollState,
    maxWidth: Dp
) {
    val density = LocalDensity.current
    val channelColPx = with(density) { channelColumnWidth.toPx() }
    val totalTimelinePx = with(density) { totalTimelineWidth.toPx() }
    val maxScreenPx = with(density) { maxWidth.toPx() }

    Box(
        Modifier
            .width(2.dp)
            .fillMaxHeight()
            .offset {
                val elapsedRatio = (now - timelineStart).toFloat() / (slots * halfHour).toFloat()
                val needleTimelineX = totalTimelinePx * elapsedRatio
                val scrollOffsetPx = timelineScrollState.value
                val screenXPx = channelColPx + needleTimelineX - scrollOffsetPx

                if (screenXPx in channelColPx..maxScreenPx) {
                    IntOffset(screenXPx.toInt() - 1, 0)
                } else {
                    IntOffset(-10000, 0)
                }
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        GoogleTvTokens.BlueAccent,
                        GoogleTvTokens.BlueAccent.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            )
    )
}

