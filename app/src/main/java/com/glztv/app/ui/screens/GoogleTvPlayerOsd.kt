package com.glztv.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.Programme
import com.glztv.app.ui.components.GoogleTvChannelLogo
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable
import java.text.DateFormat
import java.util.Date

/**
 * Authentic Google TV 1:1 Bottom Channel Banner (OSD).
 * Anchored to the bottom 25% of the screen with a gradient shadow scrim,
 * high-contrast metadata, live progress bar, and "Up Next" preview.
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
    firstButtonFocusRequester: FocusRequester? = null,
    onToggleFavorite: () -> Unit = {},
    onToggleCaptions: () -> Unit = {},
    onOpenAudioSelector: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val remainingMinutes = currentProgramme?.let {
        ((it.endMillis - now).coerceAtLeast(0L) + 59_999L) / 60_000L
    }
    val progress = currentProgramme?.let {
        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
            .coerceIn(0f, 1f)
    } ?: 0f

    val startEndFormatted = remember(currentProgramme) {
        currentProgramme?.let {
            val start = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.startMillis))
            val end = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.endMillis))
            "$start – $end"
        } ?: "Live broadcast"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GoogleTvTokens.BottomOsdScrim)
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Top Row: Channel Identity + Program Details + Up Next Card
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Channel Badge & Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(0.32f)
                ) {
                    GoogleTvChannelLogo(
                        channel = channel,
                        size = 56.dp,
                        guide = guide,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    channel.number.ifBlank { "LIVE" },
                                    Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            if (isFavorite) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Favorite",
                                    tint = GoogleTvTokens.StarGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            channel.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(20.dp))

                // 2. Program Details & Elapsed Progress
                Column(Modifier.weight(0.48f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LIVE Badge with red pulsing bullet
                        Surface(
                            shape = CircleShape,
                            color = GoogleTvTokens.LiveRed.copy(alpha = 0.20f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .background(GoogleTvTokens.LiveRed, CircleShape)
                                )
                                Text(
                                    "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoogleTvTokens.LiveRed
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Text(
                            startEndFormatted,
                            color = GoogleTvTokens.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        remainingMinutes?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "· $it min left",
                                color = GoogleTvTokens.BlueAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        currentProgramme?.title ?: "Live Programming",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = GoogleTvTokens.BlueAccent,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )

                    Spacer(Modifier.height(6.dp))

                    // Media Badges (HD, CC, Stereo)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OsdSpecBadge("HD")
                        if (captionsEnabled) {
                            OsdSpecBadge("CC ${captionLanguage.uppercase()}", active = true)
                        } else {
                            OsdSpecBadge("CC")
                        }
                        OsdSpecBadge("STEREO")
                    }
                }

                Spacer(Modifier.width(20.dp))

                // 3. "UP NEXT" Card
                nextProgramme?.let { next ->
                    Surface(
                        modifier = Modifier.weight(0.20f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.07f)
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                "UP NEXT",
                                color = GoogleTvTokens.TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                next.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val nextTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(next.startMillis))
                            Text(
                                nextTime,
                                color = GoogleTvTokens.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Quick Actions Bar (D-Pad Action Buttons)
            val favFocus = firstButtonFocusRequester ?: remember { FocusRequester() }
            val ccFocus = remember { FocusRequester() }
            val audioFocus = remember { FocusRequester() }
            val guideFocus = remember { FocusRequester() }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OsdActionButton(
                    label = if (isFavorite) "Favorited" else "Favorite",
                    icon = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                    active = isFavorite,
                    modifier = Modifier
                        .focusRequester(favFocus)
                        .focusProperties {
                            right = ccFocus
                        }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                                    ccFocus.requestFocus()
                                    return@onKeyEvent true
                                }
                            }
                            false
                        },
                    onClick = onToggleFavorite
                )
                OsdActionButton(
                    label = if (captionsEnabled) "CC: ${captionLanguage.uppercase()}" else "CC: Off",
                    icon = Icons.Default.ClosedCaption,
                    active = captionsEnabled,
                    modifier = Modifier
                        .focusRequester(ccFocus)
                        .focusProperties {
                            left = favFocus
                            right = audioFocus
                        }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        favFocus.requestFocus()
                                        return@onKeyEvent true
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        audioFocus.requestFocus()
                                        return@onKeyEvent true
                                    }
                                }
                            }
                            false
                        },
                    onClick = onToggleCaptions
                )
                OsdActionButton(
                    label = "Audio",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    active = false,
                    modifier = Modifier
                        .focusRequester(audioFocus)
                        .focusProperties {
                            left = ccFocus
                            right = guideFocus
                        }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        ccFocus.requestFocus()
                                        return@onKeyEvent true
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        guideFocus.requestFocus()
                                        return@onKeyEvent true
                                    }
                                }
                            }
                            false
                        },
                    onClick = onOpenAudioSelector
                )
                OsdActionButton(
                    label = "Full Guide",
                    icon = Icons.AutoMirrored.Filled.List,
                    active = false,
                    modifier = Modifier
                        .focusRequester(guideFocus)
                        .focusProperties {
                            left = audioFocus
                        }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                                    audioFocus.requestFocus()
                                    return@onKeyEvent true
                                }
                            }
                            false
                        },
                    onClick = onOpenGuide
                )
            }
        }
    }
}

@Composable
private fun OsdSpecBadge(text: String, active: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (active) GoogleTvTokens.BlueAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) GoogleTvTokens.BlueAccent else GoogleTvTokens.TextSecondary
        )
    }
}

@Composable
private fun OsdActionButton(
    label: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = when {
            isFocused -> Color.White
            active -> GoogleTvTokens.BlueAccent.copy(alpha = 0.22f)
            else -> Color.White.copy(alpha = 0.10f)
        },
        contentColor = when {
            isFocused -> Color.Black
            active -> GoogleTvTokens.BlueAccent
            else -> Color.White
        },
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier.googleTvFocusable(
            shape = CircleShape,
            focusedScale = 1.04f,
            onFocusChange = { isFocused = it }
        )
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = when {
                    isFocused -> Color.Black
                    active -> GoogleTvTokens.BlueAccent
                    else -> Color.White
                }
            )
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
