package com.glztv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable

/**
 * Google TV 1:1 In-Playback Mini Guide Overlay.
 * Allows effortless channel surfing and checking "What's On"
 * while live stream playback continues uninterrupted.
 */
@Composable
fun GoogleTvMiniGuide(
    currentChannel: Channel,
    channels: List<Channel>,
    guide: EpgGuide,
    now: Long,
    favorites: Set<String>,
    onTune: (Channel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndex = channels.indexOfFirst { it.id == currentChannel.id }.coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0)
    )
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GoogleTvTokens.BottomOsdScrim)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "QUICK GUIDE · CHANNELS",
                    color = GoogleTvTokens.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "Press OK to watch",
                    color = GoogleTvTokens.TextTertiary,
                    fontSize = 11.sp
                )
            }

            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                itemsIndexed(channels, key = { index, ch -> "mini-${ch.id.ifBlank { "c" }}-$index" }) { index, channel ->
                    val isPlaying = channel.id == currentChannel.id
                    val isFav = channel.id in favorites
                    val activeProg = guide.nowProgrammeForChannel(channel, now)
                    val progress = activeProg?.let {
                        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L)).coerceIn(0f, 1f)
                    } ?: 0f

                    MiniGuideChannelCard(
                        channel = channel,
                        isPlaying = isPlaying,
                        isFavorite = isFav,
                        currentProgrammeTitle = activeProg?.title ?: "Live Broadcast",
                        progress = progress,
                        guide = guide,
                        onClick = { onTune(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniGuideChannelCard(
    channel: Channel,
    isPlaying: Boolean,
    isFavorite: Boolean,
    currentProgrammeTitle: String,
    progress: Float,
    guide: EpgGuide,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(104.dp)
            .googleTvFocusable(
                shape = RoundedCornerShape(14.dp),
                focusedScale = 1.05f,
                onFocusChange = { isFocused = it }
            ),
        shape = RoundedCornerShape(14.dp),
        color = when {
            isFocused -> GoogleTvTokens.SurfaceCard
            isPlaying -> GoogleTvTokens.BlueAccent.copy(alpha = 0.20f)
            else -> GoogleTvTokens.SurfaceCardTranslucent
        },
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoogleTvChannelLogo(
                    channel = channel,
                    size = 32.dp,
                    guide = guide,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            channel.number.ifBlank { "CH" },
                            color = if (isPlaying) GoogleTvTokens.BlueAccent else GoogleTvTokens.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (isFavorite) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = GoogleTvTokens.StarGold,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        channel.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(Modifier.fillMaxWidth()) {
                Text(
                    currentProgrammeTitle,
                    color = if (isFocused) Color.White else GoogleTvTokens.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = if (isPlaying) GoogleTvTokens.BlueAccent else Color.White.copy(alpha = 0.45f),
                    trackColor = Color.White.copy(alpha = 0.10f)
                )
            }
        }
    }
}
