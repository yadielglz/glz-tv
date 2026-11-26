package com.glztv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.Programme
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.formatDtvCompactTime

private val SurfBarGradient = Brush.verticalGradient(
    listOf(Color(0xF0151A22), Color(0xF50B0E14))
)

/**
 * Compact DirecTV Bottom Channel Surfer Bar (v1.48.5.30).
 *
 * Appears cleanly at the bottom of Live TV when pressing UP/DOWN (or CH+/CH-)
 * so users can step one channel at a time without interrupting playback.
 * Auto-tunes after dwelling on a channel for 2 seconds (or instantly on OK),
 * designed compactly so it never overshadows the full-width top OSD.
 */
@Composable
fun DtvChannelSurfBar(
    surfChannel: Channel,
    currentProgramme: Programme?,
    guide: EpgGuide?,
    nowMillis: Long,
    isCurrentChannel: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = currentProgramme?.let {
        ((nowMillis - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
            .coerceIn(0f, 1f)
    } ?: 0f
    val showTitle = currentProgramme?.title?.takeIf { it.isNotBlank() }
        ?: surfChannel.eventTitle?.takeIf { it.isNotBlank() }
        ?: "Live Broadcast"
    val timeRange = currentProgramme?.let {
        "${formatDtvCompactTime(it.startMillis)} - ${formatDtvCompactTime(it.endMillis)}"
    } ?: surfChannel.group.ifBlank { "Live" }

    val isCompact = !com.glztv.app.ui.theme.LocalTvViewport.current.isTvLayout || com.glztv.app.BuildConfig.IS_MOBILE
    Column(
        modifier = modifier
            .then(
                if (isCompact) Modifier.fillMaxWidth().widthIn(max = 680.dp)
                else Modifier.widthIn(min = 460.dp, max = 680.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .background(SurfBarGradient)
            .border(1.dp, DtvChrome.Divider, RoundedCornerShape(4.dp))
    ) {
        // Crisp 2dp electric-blue top accent bar
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(DtvChrome.ElectricBlue)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Subtle Up/Down surf indicator arrows
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = DtvChrome.ElectricBlueSoft,
                    modifier = Modifier.size(16.dp)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = DtvChrome.ElectricBlueSoft,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Uniform Channel Logo
            GoogleTvChannelLogo(
                channel = surfChannel,
                size = 42.dp,
                guide = guide,
                shape = RoundedCornerShape(4.dp)
            )

            // Channel Number + Name + Show Title + Progress
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = surfChannel.number.ifBlank { "LIVE" },
                        color = DtvChrome.ElectricBlueSoft,
                        fontSize = 15.sp,
                        fontFamily = BarlowCondensedFontFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = surfChannel.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Text(
                    text = showTitle,
                    color = DtvChrome.PrimaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1000,
                            velocity = 34.dp
                        )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = timeRange,
                        color = DtvChrome.SecondaryText,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    if (currentProgramme != null) {
                        Box(
                            modifier = Modifier
                                .width(110.dp)
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
                }
            }

            // Right-side compact status hint
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isCurrentChannel) Color(0xFF1E242E) else DtvChrome.ElectricBlue.copy(alpha = 0.22f),
                            RoundedCornerShape(2.dp)
                        )
                        .border(
                            1.dp,
                            if (isCurrentChannel) DtvChrome.Divider else DtvChrome.ElectricBlue.copy(alpha = 0.7f),
                            RoundedCornerShape(2.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isCurrentChannel) "CURRENT" else "TUNING…",
                        color = if (isCurrentChannel) DtvChrome.SecondaryText else DtvChrome.ElectricBlueSoft,
                        fontSize = 10.sp,
                        fontFamily = BarlowCondensedFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }
                Text(
                    text = if (isCurrentChannel) "▲▼ Surf" else "OK now · Back cancel",
                    color = DtvChrome.MutedText,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Compact DirecTV Direct Channel Number Entry HUD (0-9 remote keys).
 */
@Composable
fun DtvChannelNumberHud(
    digits: String,
    matchedChannel: Channel?,
    guide: EpgGuide?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfBarGradient)
            .border(1.5.dp, DtvChrome.ElectricBlue, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (matchedChannel != null) {
            GoogleTvChannelLogo(
                channel = matchedChannel,
                size = 34.dp,
                guide = guide,
                shape = RoundedCornerShape(4.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "CH $digits",
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = BarlowCondensedFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = matchedChannel?.name ?: "Searching lineup…",
                color = if (matchedChannel != null) DtvChrome.ElectricBlueSoft else DtvChrome.MutedText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
