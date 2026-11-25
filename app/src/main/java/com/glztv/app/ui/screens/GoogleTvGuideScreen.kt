package com.glztv.app.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.Programme
import com.glztv.app.ui.components.GoogleTvChannelLogo
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.DtvProgressBar
import com.glztv.app.ui.theme.DtvSegmentedStripBar
import com.glztv.app.ui.theme.LocalTvViewport
import com.glztv.app.ui.theme.formatDtvCompactTime
import kotlinx.coroutines.delay

/**
 * Authentic DirecTV HD Live Program Guide (v1.48.4.10).
 *
 * Features:
 * - Shared DirecTV HD Top Command Bar (`Guide` tab active).
 * - DirecTV Segmented Category Strip (ALL | FAVORITES | LIVE EVENTS | RECENT | Groups).
 * - Split-Top Program Info Banner + Framed 16:9 TextureView Live Preview.
 * - Crisp Rectangular 1dp-Bordered EPG Grid with Sticky Channel Column,
 *   Synchronized Time-Window Stepper, and Electric-Blue Vertical "NOW" Line.
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
    topBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var categoriesExpanded by remember { mutableStateOf(false) }
    var focusedChannel by remember { mutableStateOf(activeTunedChannel ?: channels.firstOrNull()) }
    var focusedProgramme by remember { mutableStateOf<Programme?>(null) }

    val topChannelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(140L)
        runCatching { topChannelFocusRequester.requestFocus() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    val hasLiveEvents = remember(channels) { channels.any { it.isLiveEvent() } }
    val categories = remember(channels, hasLiveEvents) {
        val groups = channels.mapNotNull { it.group.takeIf(String::isNotBlank) }.distinct()
        buildList {
            add("ALL")
            add("FAVORITES")
            if (hasLiveEvents) add("LIVE EVENTS")
            add("RECENT")
            addAll(groups)
        }
    }

    val filteredChannels = remember(channels, selectedCategory, favorites) {
        when (selectedCategory) {
            "ALL" -> channels
            "FAVORITES" -> channels.filter { it.id in favorites }
            "LIVE EVENTS" -> channels.filter { it.isLiveEvent() }
            "RECENT" -> channels.take(15)
            else -> channels.filter { it.group.equals(selectedCategory, ignoreCase = true) }
        }
    }

    LaunchedEffect(filteredChannels) {
        if (focusedChannel != null && focusedChannel !in filteredChannels) {
            focusedChannel = filteredChannels.firstOrNull()
            focusedProgramme = null
        }
    }

    val displayChannel = focusedChannel ?: activeTunedChannel ?: filteredChannels.firstOrNull()
    val viewport = LocalTvViewport.current
    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(24.dp)

    val halfHour = 30 * 60 * 1000L
    val visibleSlots = if (viewport.widthDp >= 1180.dp) 5 else 4
    val maxOffsetSlots = 12
    var timelineOffsetSlots by remember { mutableStateOf(0) }
    val baseTimelineStart = now - (now % halfHour)
    val timelineStart = baseTimelineStart + timelineOffsetSlots * halfHour
    val timelineEnd = timelineStart + visibleSlots * halfHour
    val channelColumnWidth = viewport.guideChannelColWidth.coerceAtLeast(188.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DtvChrome.ScreenBg)
    ) {
        // 1. Shared DirecTV HD Top Command Bar
        topBar?.invoke()

        // 2. Classic DirecTV Split-Top Info Banner + Framed 16:9 Live Preview
        val bannerHeight = if (viewport.isCompactHeight) 136.dp else 154.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .background(DtvChrome.BannerGradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPad, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Selected Channel + Show Synopsis
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (displayChannel != null) {
                        val activeProg = focusedProgramme
                            ?: remember(displayChannel.id, guide, now) {
                                guide.nowProgrammeForChannel(displayChannel, now)
                            }
                        val isFav = displayChannel.id in favorites
                        val progProgress = activeProg?.let {
                            ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                                .coerceIn(0f, 1f)
                        } ?: 0f

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoogleTvChannelLogo(
                                    channel = displayChannel,
                                    size = 30.dp,
                                    guide = guide,
                                    shape = RoundedCornerShape(3.dp)
                                )
                                Text(
                                    text = displayChannel.number.ifBlank { "LIVE" },
                                    color = DtvChrome.ElectricBlueSoft,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = displayChannel.name,
                                    color = DtvChrome.PrimaryText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                DtvGuideFavPill(
                                    isFavorite = isFav,
                                    onClick = { onToggleFavorite(displayChannel.id) }
                                )
                            }

                            Text(
                                text = activeProg?.title ?: displayChannel.eventTitle ?: displayChannel.name,
                                color = Color.White,
                                fontSize = if (viewport.isCompactHeight) 18.sp else 21.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val timeRange = activeProg?.let {
                                "${formatDtvCompactTime(it.startMillis)} - ${formatDtvCompactTime(it.endMillis)}"
                            } ?: "Live Broadcast"
                            Text(
                                text = buildList {
                                    add(timeRange)
                                    if (displayChannel.group.isNotBlank()) add(displayChannel.group)
                                }.joinToString(", "),
                                color = DtvChrome.SecondaryText,
                                fontSize = 12.sp,
                                maxLines = 1
                            )

                            activeProg?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                Text(
                                    text = desc,
                                    color = DtvChrome.PrimaryText.copy(alpha = 0.80f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (activeProg != null && now in activeProg.startMillis..activeProg.endMillis) {
                            DtvProgressBar(progress = progProgress, modifier = Modifier.width(240.dp))
                        }
                    }
                }

                // Right: Framed 16:9 Live Preview Window
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(3.dp))
                        .clipToBounds()
                        .background(Color.Black)
                        .border(1.dp, DtvChrome.Divider, RoundedCornerShape(3.dp))
                ) {
                    previewContent()
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

        // 3. Collapsible DirecTV Category Filter Bar (under Hero Banner)
        DtvGuideCategoryToggleBar(
            selectedCategory = selectedCategory,
            channelCount = filteredChannels.size,
            expanded = categoriesExpanded,
            horizontalPad = horizontalPad,
            onToggle = { categoriesExpanded = !categoriesExpanded }
        )
        AnimatedVisibility(
            visible = categoriesExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            DtvSegmentedStripBar(
                items = categories,
                selectedItem = selectedCategory,
                onSelect = { cat ->
                    selectedCategory = cat
                    categoriesExpanded = false
                }
            )
        }

        // 4. DirecTV HD Rectangular EPG Grid
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = horizontalPad)
        ) {
            val listState = rememberLazyListState()
            val availableTimelineWidth = (maxWidth - channelColumnWidth).coerceAtLeast(240.dp)
            val slotWidth = availableTimelineWidth / visibleSlots

            Column(Modifier.fillMaxSize()) {
                // Timeline Header Row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(DtvChrome.StripGradient),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Channel Column Header with Time Stepper
                    Row(
                        Modifier
                            .width(channelColumnWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (timelineOffsetSlots == 0) "ON NOW" else "+${timelineOffsetSlots * 30}m",
                            color = DtvChrome.ElectricBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DtvGuideTimeButton(
                                label = "◀",
                                enabled = timelineOffsetSlots > 0,
                                onClick = { timelineOffsetSlots = (timelineOffsetSlots - 2).coerceAtLeast(0) }
                            )
                            if (timelineOffsetSlots > 0) {
                                DtvGuideTimeButton(
                                    label = "NOW",
                                    enabled = true,
                                    onClick = { timelineOffsetSlots = 0 }
                                )
                            }
                            DtvGuideTimeButton(
                                label = "▶",
                                enabled = timelineOffsetSlots < maxOffsetSlots,
                                onClick = { timelineOffsetSlots = (timelineOffsetSlots + 2).coerceAtMost(maxOffsetSlots) }
                            )
                        }
                    }

                    Box(Modifier.width(1.dp).fillMaxHeight().background(DtvChrome.Divider))

                    // Time Slot Columns
                    Row(
                        Modifier
                            .width(availableTimelineWidth)
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (slot in 0 until visibleSlots) {
                            val slotTime = timelineStart + slot * halfHour
                            Box(
                                modifier = Modifier
                                    .width(slotWidth)
                                    .fillMaxHeight()
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = formatDtvCompactTime(slotTime),
                                    color = if (slot == 0 && timelineOffsetSlots == 0) Color.White else DtvChrome.SecondaryText,
                                    fontSize = 12.sp,
                                    fontWeight = if (slot == 0 && timelineOffsetSlots == 0) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                            if (slot < visibleSlots - 1) {
                                Box(Modifier.width(1.dp).fillMaxHeight().background(DtvChrome.Divider.copy(alpha = 0.7f)))
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.Divider))

                // Grid Body + Vertical NOW Needle
                Box(Modifier.fillMaxSize()) {
                    if (filteredChannels.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No channels in this category.",
                                color = DtvChrome.SecondaryText,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                filteredChannels,
                                key = { index, ch -> "dtv-epg-$index-${ch.id}" }
                            ) { index, ch ->
                                val rowProgs = remember(ch.id, guide, timelineStart, timelineEnd) {
                                    guide.forChannel(ch).filter { prog ->
                                        prog.endMillis > timelineStart && prog.startMillis < timelineEnd
                                    }
                                }
                                DtvGuideChannelRow(
                                    channel = ch,
                                    programmes = rowProgs,
                                    guide = guide,
                                    isTuned = ch.id == activeTunedChannel?.id,
                                    isFavorite = ch.id in favorites,
                                    now = now,
                                    timelineStart = timelineStart,
                                    timelineEnd = timelineEnd,
                                    channelColumnWidth = channelColumnWidth,
                                    timelineWidth = availableTimelineWidth,
                                    firstCellFocusRequester = if (index == 0) topChannelFocusRequester else null,
                                    onFocusCell = { prog ->
                                        focusedChannel = ch
                                        focusedProgramme = prog
                                    },
                                    onWatch = { onWatchChannel(ch) }
                                )
                            }
                        }
                    }

                    // Electric-Blue Vertical NOW Needle
                    if (now in timelineStart..timelineEnd) {
                        val fraction = ((now - timelineStart).toFloat() / (timelineEnd - timelineStart).coerceAtLeast(1L))
                            .coerceIn(0f, 1f)
                        val density = LocalDensity.current
                        val needleX = with(density) {
                            (channelColumnWidth + availableTimelineWidth * fraction).roundToPx()
                        }
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(needleX, 0) }
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(DtvChrome.ElectricBlue.copy(alpha = 0.85f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DtvGuideChannelRow(
    channel: Channel,
    programmes: List<Programme>,
    guide: EpgGuide,
    isTuned: Boolean,
    isFavorite: Boolean,
    now: Long,
    timelineStart: Long,
    timelineEnd: Long,
    channelColumnWidth: Dp,
    timelineWidth: Dp,
    firstCellFocusRequester: FocusRequester?,
    onFocusCell: (Programme?) -> Unit,
    onWatch: () -> Unit
) {
    val rowHeight = 54.dp
    val totalDuration = (timelineEnd - timelineStart).coerceAtLeast(1L).toFloat()

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed Left Channel Identity Cell
            Row(
                modifier = Modifier
                    .width(channelColumnWidth)
                    .fillMaxHeight()
                    .background(if (isTuned) DtvChrome.ElectricBlue.copy(alpha = 0.14f) else Color(0xFF101318))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = channel.number.ifBlank { "—" },
                    color = if (isTuned) DtvChrome.ElectricBlueSoft else DtvChrome.SecondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.width(36.dp)
                )
                GoogleTvChannelLogo(
                    channel = channel,
                    size = 26.dp,
                    guide = guide,
                    shape = RoundedCornerShape(3.dp)
                )
                Text(
                    text = channel.name,
                    color = if (isTuned) Color.White else DtvChrome.PrimaryText,
                    fontSize = 12.sp,
                    fontWeight = if (isTuned) FontWeight.SemiBold else FontWeight.Medium,
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

            Box(Modifier.width(1.dp).fillMaxHeight().background(DtvChrome.Divider))

            // Programme Cells Track
            Row(
                modifier = Modifier
                    .width(timelineWidth)
                    .fillMaxHeight()
            ) {
                if (programmes.isEmpty()) {
                    DtvGuideProgrammeCell(
                        title = channel.eventTitle ?: channel.name,
                        subtitle = "Live Broadcast",
                        isLiveNow = true,
                        width = timelineWidth,
                        focusRequester = firstCellFocusRequester,
                        onFocus = { onFocusCell(null) },
                        onClick = onWatch
                    )
                } else {
                    var cursor = timelineStart
                    var firstAttached = false
                    for (prog in programmes) {
                        val clampedStart = prog.startMillis.coerceIn(timelineStart, timelineEnd)
                        val clampedEnd = prog.endMillis.coerceIn(timelineStart, timelineEnd)
                        if (clampedEnd <= clampedStart) continue

                        if (clampedStart > cursor) {
                            val gapFrac = (clampedStart - cursor) / totalDuration
                            val gapWidth = (timelineWidth * gapFrac).coerceAtLeast(12.dp)
                            DtvGuideProgrammeCell(
                                title = channel.name,
                                subtitle = "",
                                isLiveNow = false,
                                width = gapWidth,
                                focusRequester = if (!firstAttached) { firstAttached = true; firstCellFocusRequester } else null,
                                onFocus = { onFocusCell(null) },
                                onClick = onWatch
                            )
                        }

                        val frac = (clampedEnd - clampedStart) / totalDuration
                        val cellWidth = (timelineWidth * frac).coerceAtLeast(24.dp)
                        val isLiveNow = now in prog.startMillis..prog.endMillis
                        val sub = "${formatDtvCompactTime(prog.startMillis)} - ${formatDtvCompactTime(prog.endMillis)}"
                        DtvGuideProgrammeCell(
                            title = prog.title,
                            subtitle = sub,
                            isLiveNow = isLiveNow,
                            width = cellWidth,
                            focusRequester = if (!firstAttached) { firstAttached = true; firstCellFocusRequester } else null,
                            onFocus = { onFocusCell(prog) },
                            onClick = onWatch
                        )
                        cursor = clampedEnd
                    }
                    if (cursor < timelineEnd) {
                        val remFrac = (timelineEnd - cursor) / totalDuration
                        val remWidth = (timelineWidth * remFrac).coerceAtLeast(12.dp)
                        DtvGuideProgrammeCell(
                            title = "Programming TBA",
                            subtitle = "",
                            isLiveNow = false,
                            width = remWidth,
                            focusRequester = null,
                            onFocus = { onFocusCell(null) },
                            onClick = onWatch
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.RowDivider))
    }
}

@Composable
private fun DtvGuideProgrammeCell(
    title: String,
    subtitle: String,
    isLiveNow: Boolean,
    width: Dp,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val bg = when {
        isFocused -> Color.Black
        isLiveNow -> Color(0xFF171C24)
        else -> Color(0xFF0E1116)
    }

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else 0.5.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.RowDivider
            )
            .onFocusChanged {
                val focused = it.isFocused
                if (focused && !isFocused) onFocus()
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = if (isFocused) Color.White else DtvChrome.PrimaryText,
                fontSize = 13.sp,
                fontWeight = if (isFocused || isLiveNow) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = if (isFocused) DtvChrome.ElectricBlueSoft else DtvChrome.MutedText,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DtvGuideTimeButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (isFocused) Color.Black else Color(0xFF1A1E26))
            .border(1.dp, if (isFocused) DtvChrome.ElectricBlue else DtvChrome.Divider, RoundedCornerShape(2.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (enabled && keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
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
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> DtvChrome.MutedText.copy(alpha = 0.4f)
                isFocused -> Color.White
                else -> DtvChrome.PrimaryText
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DtvGuideFavPill(
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color.Black else Color(0xFF171B22))
            .border(1.dp, if (isFocused) DtvChrome.ElectricBlue else DtvChrome.Divider, RoundedCornerShape(3.dp))
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
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
            contentDescription = "Favorite",
            tint = if (isFavorite) DtvChrome.Gold else DtvChrome.SecondaryText,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = if (isFavorite) "Favorited" else "Favorite",
            color = if (isFocused) Color.White else DtvChrome.SecondaryText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DtvGuideCategoryToggleBar(
    selectedCategory: String,
    channelCount: Int,
    expanded: Boolean,
    horizontalPad: Dp,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(if (isFocused) Color.Black else Color(0xFF10141C))
                .onFocusChanged { isFocused = it.isFocused }
                .onKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                onToggle()
                                return@onKeyEvent true
                            }
                        }
                    }
                    false
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                )
                .padding(horizontal = horizontalPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(11.dp)
                        .background(if (isFocused || expanded) DtvChrome.ElectricBlue else DtvChrome.ElectricBlueSoft)
                )
                Text(
                    text = "CATEGORIES",
                    color = DtvChrome.MutedText,
                    fontSize = 11.sp,
                    fontFamily = BarlowCondensedFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = selectedCategory,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = BarlowCondensedFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "($channelCount ch)",
                    color = DtvChrome.SecondaryText,
                    fontSize = 11.sp
                )
            }
            Text(
                text = if (expanded) "Hide Categories ▲" else "Filter Categories ▾",
                color = if (isFocused || expanded) DtvChrome.ElectricBlue else DtvChrome.SecondaryText,
                fontSize = 11.sp,
                fontFamily = BarlowCondensedFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (isFocused) 2.dp else 1.dp)
                .background(if (isFocused) DtvChrome.ElectricBlue else DtvChrome.Divider)
        )
    }
}

