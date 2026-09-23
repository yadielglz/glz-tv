package com.glztv.app.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.Programme
import com.glztv.app.ui.components.ChannelLogo
import com.glztv.app.ui.components.GlzCardDefaults
import com.glztv.app.ui.components.GlzFocusCard
import com.glztv.app.ui.components.GlzPanel
import com.glztv.app.ui.components.tvFocusableWithPhysics
import com.glztv.app.ui.screens.player.VideoPlayer
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun GuideSection(
    channels: List<Channel>,
    guide: EpgGuide,
    previewChannel: Channel?,
    captionLanguage: String,
    favorites: Set<String> = emptySet(),
    onWatch: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var timeOffsetMillis by remember { mutableStateOf(0L) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var focusedChannel by remember { mutableStateOf(previewChannel ?: channels.firstOrNull()) }
    var focusedProgramme by remember { mutableStateOf<Programme?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    val categories = remember(channels) {
        val groups = channels.mapNotNull { it.group.takeIf { g -> g.isNotBlank() } }.distinct()
        listOf("ALL", "FAVORITES") + groups
    }

    val filteredChannels = remember(channels, selectedCategory, favorites) {
        when (selectedCategory) {
            "ALL" -> channels
            "FAVORITES" -> channels.filter { it.id in favorites }
            else -> channels.filter { it.group.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val activeChannel = focusedChannel ?: previewChannel ?: filteredChannels.firstOrNull()

    GlzPanel(
        modifier = modifier,
        shape = RoundedCornerShape(GlzCardDefaults.RadiusMedium),
        fill = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
            if (guide.programmeCount == 0 && channels.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Guide data is unavailable", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Refresh the sources or check the XMLTV address in Settings.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activeChannel != null) {
                        EpgPreviewHeader(
                            focusedChannel = activeChannel,
                            focusedProgramme = focusedProgramme,
                            previewChannel = previewChannel,
                            guide = guide,
                            now = now,
                            captionLanguage = captionLanguage,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .focusGroup(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LazyRow(
                            Modifier.weight(1f).padding(end = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(categories) { cat ->
                                GlzFocusCard(
                                    onClick = { selectedCategory = cat },
                                    selected = selectedCategory == cat,
                                    shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
                                    focusedScale = 1.04f
                                ) { _ ->
                                    Text(
                                        cat.uppercase(),
                                        Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(
                                "NOW" to 0L,
                                "+2H" to 2 * 3600 * 1000L,
                                "TONIGHT" to calculateTonightOffset(now),
                                "TOMORROW" to 24 * 3600 * 1000L
                            ).forEach { (label, offset) ->
                                GlzFocusCard(
                                    onClick = { timeOffsetMillis = offset },
                                    selected = timeOffsetMillis == offset,
                                    accent = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
                                    focusedScale = 1.04f
                                ) { _ ->
                                    Text(
                                        label,
                                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    EpgGrid(
                        channels = filteredChannels,
                        guide = guide,
                        now = now,
                        timeOffsetMillis = timeOffsetMillis,
                        onFocusChannel = { ch ->
                            focusedChannel = ch
                            val progs = guide.forChannel(ch)
                            focusedProgramme = progs.firstOrNull { it.startMillis <= now && it.endMillis > now }
                        },
                        onFocusProgramme = { ch, prog ->
                            focusedChannel = ch
                            focusedProgramme = prog
                        },
                        onWatch = onWatch,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    }
}

fun calculateTonightOffset(now: Long): Long {
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = now
        set(java.util.Calendar.HOUR_OF_DAY, 20)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val tonightMillis = cal.timeInMillis
    return if (tonightMillis > now) tonightMillis - now else 0L
}

@Composable
fun EpgPreviewHeader(
    focusedChannel: Channel,
    focusedProgramme: Programme?,
    previewChannel: Channel?,
    guide: EpgGuide,
    now: Long,
    captionLanguage: String,
    modifier: Modifier = Modifier
) {
    val programmes = remember(focusedChannel, guide) { guide.forChannel(focusedChannel) }
    val current = remember(programmes, now) {
        programmes.firstOrNull { it.startMillis <= now && it.endMillis > now }
    }
    val activeProg = focusedProgramme ?: current
    val next = remember(programmes, activeProg, now) {
        val targetEnd = activeProg?.endMillis ?: now
        programmes.firstOrNull { it.startMillis >= targetEnd }
    }
    val progress = activeProg?.let {
        if (it.startMillis <= now && it.endMillis > now) {
            ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L)).coerceIn(0f, 1f)
        } else 0f
    } ?: 0f

    val isLive = activeProg != null && activeProg.startMillis <= now && activeProg.endMillis > now

    Surface(
        modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChannelLogo(focusedChannel, 34.dp, guide)

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${focusedChannel.number.ifBlank { "TV" }} · ${focusedChannel.name}",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isLive) Color(0xFF00E5FF).copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f),
                    ) {
                        Text(
                            if (isLive) "LIVE" else "UPCOMING",
                            Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLive) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.80f)
                        )
                    }
                    activeProg?.let { prog ->
                        val startStr = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prog.startMillis))
                        val endStr = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prog.endMillis))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$startStr – $endStr",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        activeProg?.title ?: "Live Broadcast",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    next?.let { n ->
                        Text(
                            "Next: ${n.title}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }

                if (isLive) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp).clip(RoundedCornerShape(1.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
                }
            }

            // Fixed preview PIP video
            previewChannel?.let { channel ->
                Surface(
                    Modifier
                        .width(110.dp)
                        .height(62.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black,
                ) {
                    VideoPlayer(
                        channel = channel,
                        captionsEnabled = false,
                        captionLanguage = captionLanguage,
                        modifier = Modifier.fillMaxSize(),
                        muted = true,
                        keepScreenOn = false,
                        cropVideo = true
                    )
                }
            }
        }
    }
}

@Composable
fun EpgGrid(
    channels: List<Channel>,
    guide: EpgGuide,
    now: Long,
    timeOffsetMillis: Long,
    onFocusChannel: (Channel) -> Unit,
    onFocusProgramme: (Channel, Programme) -> Unit,
    onWatch: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val halfHour = 30 * 60 * 1000L
    val baseTime = now + timeOffsetMillis
    val start = baseTime - (baseTime % halfHour)
    val slots = 8
    val slotWidth = 140.dp
    val channelWidth = 180.dp
    val timelineWidth = slotWidth * slots
    val totalWidth = channelWidth + timelineWidth
    val horizontal = rememberScrollState()

    Column(modifier.horizontalScroll(horizontal)) {
        Box(Modifier.width(totalWidth).fillMaxHeight()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f), RoundedCornerShape(10.dp))
                ) {
                    Box(
                        Modifier
                            .width(channelWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "CHANNELS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    repeat(slots) { slot ->
                        Box(
                            Modifier
                                .width(slotWidth)
                                .fillMaxHeight()
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(start + slot * halfHour)),
                                Modifier.padding(start = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize().focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(channels, key = { index, ch -> "grid-$index-${ch.streamUrl}" }) { _, channel ->
                        EpgGridRow(
                            channel = channel,
                            programmes = guide.forChannel(channel),
                            guide = guide,
                            windowStart = start,
                            windowEnd = start + slots * halfHour,
                            channelWidth = channelWidth,
                            timelineWidth = timelineWidth,
                            slotWidth = slotWidth,
                            onFocusChannel = { onFocusChannel(channel) },
                            onFocusProgramme = { prog -> onFocusProgramme(channel, prog) },
                            onWatch = { onWatch(channel) }
                        )
                    }
                }
            }

            if (now in start..(start + slots * halfHour)) {
                val elapsedRatio = (now - start).toFloat() / (slots * halfHour).toFloat()
                val clockX = channelWidth + (timelineWidth * elapsedRatio)
                Box(
                    Modifier
                        .offset(x = clockX - 1.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF00E5FF).copy(alpha = 0.70f),
                                    Color(0xFF00E5FF).copy(alpha = 0.10f)
                                )
                            )
                        )
                )
                Surface(
                    modifier = Modifier.offset(x = clockX - 16.dp, y = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF00E5FF),
                    contentColor = Color.Black
                ) {
                    Row(
                        Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(Modifier.size(3.dp).background(Color.Red, CircleShape))
                        Text("NOW", fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun EpgGridRow(
    channel: Channel,
    programmes: List<Programme>,
    guide: EpgGuide? = null,
    windowStart: Long,
    windowEnd: Long,
    channelWidth: Dp,
    timelineWidth: Dp,
    slotWidth: Dp,
    onFocusChannel: () -> Unit,
    onFocusProgramme: (Programme) -> Unit,
    onWatch: () -> Unit
) {
    val halfHour = 30 * 60 * 1000f
    val visible = programmes.filter { it.endMillis > windowStart && it.startMillis < windowEnd }
    val now = remember { System.currentTimeMillis() }
    var channelFocused by remember { mutableStateOf(false) }

    Row(
        Modifier
            .height(46.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
    ) {
        Surface(
            onClick = onWatch,
            modifier = Modifier
                .width(channelWidth)
                .fillMaxHeight()
                .tvFocusableWithPhysics(
                    shape = RoundedCornerShape(10.dp),
                    focusedScale = 1.02f,
                    glowColor = MaterialTheme.colorScheme.primary,
                    onFocusChange = {
                        channelFocused = it
                        if (it) onFocusChannel()
                    }
                ),
            shape = RoundedCornerShape(10.dp),
            color = if (channelFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (channelFocused) Color.Black else Color.White
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogo(channel, 28.dp, guide)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        channel.number.ifBlank { "TV" },
                        color = if (channelFocused) Color.Black else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        channel.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Box(
            Modifier
                .width(timelineWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
        ) {
            repeat(8) { slot ->
                Box(
                    Modifier
                        .offset(x = slotWidth * slot)
                        .width(slotWidth)
                        .fillMaxHeight()
                        .border(0.5.dp, Color.White.copy(alpha = 0.06f))
                )
            }
            if (visible.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "No program information",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                visible.forEach { programme ->
                    val clippedStart = maxOf(programme.startMillis, windowStart)
                    val clippedEnd = minOf(programme.endMillis, windowEnd)
                    val x = slotWidth * ((clippedStart - windowStart) / halfHour)
                    val width = slotWidth * ((clippedEnd - clippedStart) / halfHour)
                    val isCurrent = programme.startMillis <= now && programme.endMillis > now
                    var progFocused by remember { mutableStateOf(false) }

                    Surface(
                        onClick = onWatch,
                        modifier = Modifier
                            .offset(x = x)
                            .width(maxOf(width, 48.dp))
                            .fillMaxHeight()
                            .padding(2.dp)
                            .tvFocusableWithPhysics(
                                shape = RoundedCornerShape(8.dp),
                                focusedScale = 1.03f,
                                glowColor = Color(0xFF00E5FF),
                                onFocusChange = {
                                    progFocused = it
                                    if (it) onFocusProgramme(programme)
                                }
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            progFocused -> Color(0xFF00E5FF)
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
                        },
                        contentColor = when {
                            progFocused -> Color.Black
                            isCurrent -> Color.White
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    ) {
                        Column(
                            Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                programme.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(programme.startMillis)),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (progFocused) Color.Black.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelPane(
    channels: List<Channel>,
    selected: Channel?,
    favorites: Set<String>,
    guide: EpgGuide? = null,
    onSelect: (Channel) -> Unit,
    onFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Text(
                "${channels.size} channels",
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(channels, key = { index, ch -> "$index-${ch.streamUrl}" }) { _, channel ->
                    ChannelCard(
                        channel = channel,
                        selected = channel == selected,
                        favorite = channel.id in favorites,
                        guide = guide,
                        onClick = { onSelect(channel) },
                        onFavorite = { onFavorite(channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    selected: Boolean,
    favorite: Boolean,
    guide: EpgGuide? = null,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    var focused by remember(channel.id) { mutableStateOf(false) }
    val container = if (focused) MaterialTheme.colorScheme.surfaceContainerHighest
    else if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .tvFocusableWithPhysics(
                shape = RoundedCornerShape(20.dp),
                focusedScale = 1.03f,
                glowColor = MaterialTheme.colorScheme.secondary,
                onFocusChange = { focused = it }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(if (focused) 8.dp else 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(channel, 48.dp, guide)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    channel.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${channel.number.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()}${channel.group}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorite",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DrawerSectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
        color = Color.White.copy(alpha = .55f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}

@Composable
fun TvOptionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        color = when {
            isFocused -> Color(0xFFC4FF4D)
            selected -> Color(0xFF23405F)
            else -> Color.White.copy(alpha = 0.12f)
        },
        contentColor = when {
            isFocused -> Color.Black
            selected -> Color(0xFFC4FF4D)
            else -> Color.White
        },
        shadowElevation = if (isFocused) 8.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            fontWeight = if (isFocused || selected) FontWeight.Black else FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun PlayerAndGuide(
    channel: Channel?,
    guide: EpgGuide,
    captionsEnabled: Boolean,
    captionLanguage: String,
    modifier: Modifier = Modifier
) {
    if (channel == null) {
        Card(modifier, shape = RoundedCornerShape(32.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LiveTv,
                        null,
                        Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Choose a channel", fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(
                        "Live playback and the programme guide will appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }
    val programmes = guide.forChannel(channel)
    val now = System.currentTimeMillis()
    val upcoming = programmes.filter { it.endMillis > now }.take(6)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VideoPlayer(
            channel = channel,
            captionsEnabled = captionsEnabled,
            captionLanguage = captionLanguage,
            modifier = Modifier.fillMaxWidth().aspectRatio(16 / 9f)
        )
        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Text(channel.name, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(channel.group, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text(
                    "ON NOW & NEXT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (upcoming.isEmpty()) {
                    Text(
                        "No EPG information is available for this channel.",
                        Modifier.padding(top = 14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(upcoming) { programme ->
                            ProgrammeCard(programme, programme.startMillis <= now)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgrammeCard(programme: Programme, isNow: Boolean) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isNow) 22.dp else 14.dp),
        color = if (isNow) MaterialTheme.colorScheme.secondary.copy(alpha = .14f)
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Text(
                if (isNow) "NOW" else DateFormat.getTimeInstance(DateFormat.SHORT)
                    .format(Date(programme.startMillis)),
                Modifier.width(62.dp),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black
            )
            Column {
                Text(programme.title, fontWeight = FontWeight.Bold)
                if (programme.description.isNotBlank()) {
                    Text(
                        programme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
