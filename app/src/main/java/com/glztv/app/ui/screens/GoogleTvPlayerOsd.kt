package com.glztv.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.Programme
import com.glztv.app.player.PlaybackDiagnostics
import com.glztv.app.ui.components.GoogleTvChannelLogo
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.LocalTvViewport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DtvBannerGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xF51E2228),
        0.44f to Color(0xF7111419),
        0.52f to Color(0xFA090B0E),
        1.00f to Color(0xFC040507)
    )
)

private val DtvMenuStripGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFA22262D),
        0.45f to Color(0xFA171A20),
        1.00f to Color(0xFA111419)
    )
)

private val DtvElectricBlue = Color(0xFF1990FF)
private val DtvDivider = Color(0xFF2E343E)
private val DtvPrimaryText = Color(0xFFE0E4EA)
private val DtvSecondaryText = Color(0xFF88909B)
private val DtvTabUnfocusedText = Color(0xFF8B939E)

private data class DtvOsdTab(
    val id: String,
    val label: String,
    val indicatorColor: Color? = null,
    val onClick: () -> Unit
)

/**
 * DirecTV-inspired Top Channel Banner OSD with an attached full-width options menu strip
 * directly below the channel info bar, plus an expandable Program Info drawer.
 */
@Composable
fun GoogleTvPlayerOsd(
    channel: Channel,
    currentProgramme: Programme?,
    nextProgramme: Programme?,
    guide: EpgGuide? = null,
    now: Long,
    isFavorite: Boolean = false,
    captionsEnabled: Boolean = false,
    captionLanguage: String = "en",
    diagnostics: PlaybackDiagnostics? = null,
    firstButtonFocusRequester: FocusRequester? = null,
    onToggleFavorite: () -> Unit = {},
    onToggleCaptions: () -> Unit = {},
    onOpenAudioSelector: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onOpenChannels: () -> Unit = {},
    onOpenMultiView: () -> Unit = {},
    onInteraction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewport = LocalTvViewport.current
    var showInfoPanel by remember(channel.id) { mutableStateOf(false) }

    val timeFmt = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val amPmFmt = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEE M/d", Locale.getDefault()) }

    fun formatCompactTime(millis: Long): String {
        val d = Date(millis)
        val suffix = amPmFmt.format(d).lowercase(Locale.getDefault()).firstOrNull()?.toString() ?: ""
        return "${timeFmt.format(d)}$suffix"
    }

    val nowTimeText = formatCompactTime(now)
    val nowDateText = dateFmt.format(Date(now))

    val progress = currentProgramme?.let {
        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
            .coerceIn(0f, 1f)
    } ?: 0f

    val programmeTimeRange = remember(currentProgramme) {
        currentProgramme?.let {
            "${formatCompactTime(it.startMillis)} - ${formatCompactTime(it.endMillis)}"
        } ?: "Live"
    }

    // Technical badges formatted compactly in the DirecTV subtitle line (e.g. "11:00a - 12:00p, HD, CC")
    val videoBadge = diagnostics?.videoQualityBadge()?.let {
        when (it) {
            "4K UHD" -> "4K"
            "1080p", "720p" -> "HD"
            "SD" -> "SD"
            else -> null
        }
    }
    val audioBadge = diagnostics?.audioChannelBadge()?.takeIf { it == "5.1" || it == "7.1" }

    val subtitleText = remember(programmeTimeRange, videoBadge, audioBadge, captionsEnabled, channel.group) {
        buildList {
            add(programmeTimeRange)
            if (videoBadge != null) add(videoBadge)
            if (audioBadge != null) add(audioBadge)
            if (captionsEnabled) add("CC")
            if (size == 1 && channel.group.isNotBlank()) add(channel.group)
        }.joinToString(", ")
    }

    val displayTitle = currentProgramme?.title?.takeIf { it.isNotBlank() }
        ?: channel.eventTitle?.takeIf { it.isNotBlank() }
        ?: channel.name

    val tabs = listOf(
        DtvOsdTab(
            id = "guide",
            label = "Guide",
            onClick = {
                onInteraction()
                onOpenGuide()
            }
        ),
        DtvOsdTab(
            id = "info",
            label = "Info",
            indicatorColor = if (showInfoPanel) DtvElectricBlue else null,
            onClick = {
                onInteraction()
                showInfoPanel = !showInfoPanel
            }
        ),
        DtvOsdTab(
            id = "favorites",
            label = "Favorites",
            indicatorColor = if (isFavorite) GoogleTvTokens.StarGold else null,
            onClick = {
                onInteraction()
                onToggleFavorite()
            }
        ),
        DtvOsdTab(
            id = "cc",
            label = "CC",
            indicatorColor = if (captionsEnabled) DtvElectricBlue else null,
            onClick = {
                onInteraction()
                onToggleCaptions()
            }
        ),
        DtvOsdTab(
            id = "av",
            label = "Audio/Video",
            onClick = {
                onInteraction()
                onOpenAudioSelector()
            }
        ),
        DtvOsdTab(
            id = "channels",
            label = "Channels",
            onClick = {
                onInteraction()
                onOpenChannels()
            }
        ),
        DtvOsdTab(
            id = "multiview",
            label = "Multi-View",
            onClick = {
                onInteraction()
                onOpenMultiView()
            }
        )
    )

    val focusRequesters = remember(tabs.size, firstButtonFocusRequester) {
        List(tabs.size) { index ->
            // Focus "Favorites" (index 3) or "Info" (index 1) on open; attach parent requester to index 1 ("Info")
            if (index == 1 && firstButtonFocusRequester != null) firstButtonFocusRequester
            else FocusRequester()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── 1. Top Glossy Dark Channel Banner ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DtvBannerGradient)
        ) {
            // Subtle glossy top edge highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.09f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = viewport.safeHorizontal.coerceAtLeast(36.dp),
                        vertical = if (viewport.isCompactHeight) 14.dp else 18.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Channel Logo + Number & Callsign/Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.widthIn(min = 170.dp, max = 270.dp)
                ) {
                    GoogleTvChannelLogo(
                        channel = channel,
                        size = if (viewport.isCompactHeight) 42.dp else 48.dp,
                        guide = guide,
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = channel.number.ifBlank { "LIVE" },
                            color = DtvPrimaryText,
                            fontSize = if (viewport.isCompactHeight) 19.sp else 22.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Text(
                            text = channel.name,
                            color = DtvSecondaryText,
                            fontSize = if (viewport.isCompactHeight) 14.sp else 16.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(24.dp))

                // Center: Program Title + Time Window & Metadata
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = displayTitle,
                        color = DtvPrimaryText,
                        fontSize = if (viewport.isCompactHeight) 21.sp else 25.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleText,
                        color = DtvSecondaryText,
                        fontSize = if (viewport.isCompactHeight) 14.sp else 16.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentProgramme != null) {
                        Spacer(Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(DtvElectricBlue)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(24.dp))

                // Right: Crisp Current Time & Date (clean, no third-party logo)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = nowTimeText,
                        color = DtvPrimaryText,
                        fontSize = if (viewport.isCompactHeight) 20.sp else 23.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                    Text(
                        text = nowDateText,
                        color = DtvSecondaryText,
                        fontSize = if (viewport.isCompactHeight) 13.sp else 15.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }

            // Crisp hairline divider between banner and menu strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.BottomCenter)
                    .background(DtvDivider)
            )
        }

        // ── 2. Attached DirecTV-Style Horizontal Options Menu Strip ───────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (viewport.isCompactHeight) 42.dp else 48.dp)
                .background(DtvMenuStripGradient),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val requester = focusRequesters[index]
                val prevRequester = if (index > 0) focusRequesters[index - 1] else null
                val nextRequester = if (index < tabs.lastIndex) focusRequesters[index + 1] else null

                DtvStripTabItem(
                    tab = tab,
                    compact = viewport.isCompactHeight,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(requester)
                        .focusProperties {
                            left = prevRequester ?: requester
                            right = nextRequester ?: requester
                            down = requester
                        }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        onInteraction()
                                        if (prevRequester != null) {
                                            runCatching { prevRequester.requestFocus() }
                                        }
                                        return@onKeyEvent true
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        onInteraction()
                                        if (nextRequester != null) {
                                            runCatching { nextRequester.requestFocus() }
                                        }
                                        return@onKeyEvent true
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                    android.view.KeyEvent.KEYCODE_ENTER,
                                    android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                                    android.view.KeyEvent.KEYCODE_SPACE -> {
                                        tab.onClick()
                                        return@onKeyEvent true
                                    }
                                }
                            }
                            false
                        },
                    onFocus = onInteraction
                )

                if (index < tabs.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(DtvDivider.copy(alpha = 0.75f))
                    )
                }
            }
        }

        // Bottom hairline shadow under menu strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black.copy(alpha = 0.85f))
        )

        // ── 3. Expandable DirecTV-Style Program Info Panel (when "Info" is toggled)
        AnimatedVisibility(
            visible = showInfoPanel,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xF0090B0F))
                    .padding(
                        horizontal = viewport.safeHorizontal.coerceAtLeast(36.dp),
                        vertical = 14.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1.4f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val description = currentProgramme?.description?.takeIf { it.isNotBlank() }
                            ?: "Live broadcast on ${channel.name}."
                        Text(
                            text = description,
                            color = DtvPrimaryText.copy(alpha = 0.90f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            diagnostics?.videoQualityBadge()
                                ?.takeIf { it != "LIVE" }
                                ?.let { DtvSpecPill(it) }
                            diagnostics?.fpsBadge()
                                ?.let { DtvSpecPill(it) }
                            diagnostics?.audioChannelBadge()
                                ?.let { DtvSpecPill(it) }
                            if (captionsEnabled) {
                                DtvSpecPill("CC ${captionLanguage.uppercase()}", accent = true)
                            }
                        }
                    }

                    if (nextProgramme != null) {
                        Column(
                            modifier = Modifier.weight(0.6f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "UP NEXT",
                                color = DtvElectricBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = nextProgramme.title,
                                color = DtvPrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatCompactTime(nextProgramme.startMillis)} - ${formatCompactTime(nextProgramme.endMillis)}",
                                color = DtvSecondaryText,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DtvStripTabItem(
    tab: DtvOsdTab,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .background(if (isFocused) Color.Black else Color.Transparent)
            .onFocusChanged {
                val nowFocused = it.isFocused
                if (nowFocused && !isFocused) onFocus()
                isFocused = nowFocused
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = tab.onClick
            )
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (tab.indicatorColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(tab.indicatorColor)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = tab.label,
                color = if (isFocused) Color.White else DtvTabUnfocusedText,
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Signature DirecTV electric blue bottom bar on the active/focused tab
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(DtvElectricBlue)
            )
        }
    }
}

@Composable
private fun DtvSpecPill(text: String, accent: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (accent) DtvElectricBlue.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            color = if (accent) DtvElectricBlue else DtvPrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
