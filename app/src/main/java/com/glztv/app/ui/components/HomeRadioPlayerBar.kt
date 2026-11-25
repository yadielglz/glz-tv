package com.glztv.app.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.RadioStation
import com.glztv.app.player.RadioPlaybackState
import com.glztv.app.ui.screens.AudioSpectrumVisualizer
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.LocalTvViewport

/**
 * DirecTV HD Bottom Audio Bar docked cleanly along the bottom of Home when Radio is active.
 */
@Composable
fun HomeRadioPlayerBar(
    station: RadioStation?,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    playbackState: RadioPlaybackState? = null,
    focusRequester: FocusRequester? = null,
    onFocusUp: (() -> Unit)? = null,
    onFocusDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (station == null) return

    val viewport = LocalTvViewport.current
    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(24.dp)
    val isBuffering = playbackState?.isBuffering == true
    val icyLine = listOfNotNull(
        playbackState?.icyArtist?.takeIf { it.isNotBlank() },
        playbackState?.icyTitle?.takeIf { it.isNotBlank() }
    ).joinToString(" — ").takeIf { it.isNotBlank() }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.Divider))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(DtvChrome.TopBarGradient)
                .padding(horizontal = horizontalPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF141820))
                        .border(1.dp, DtvChrome.ElectricBlue.copy(alpha = 0.6f), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!station.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = station.logoUrl,
                            contentDescription = station.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Radio,
                            contentDescription = null,
                            tint = DtvChrome.ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(DtvChrome.ElectricBlue.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
                        .border(1.dp, DtvChrome.ElectricBlue.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPlaying) "RADIO ON AIR" else if (isBuffering) "CONNECTING" else "PAUSED",
                        color = DtvChrome.ElectricBlueSoft,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }

                Text(
                    text = station.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "·  ${icyLine ?: station.genre.ifBlank { "GLZ Radio" }}",
                    color = DtvChrome.SecondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AudioSpectrumVisualizer(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    barCount = 5,
                    barColor = DtvChrome.ElectricBlue,
                    secondaryColor = DtvChrome.ElectricBlueSoft,
                    modifier = Modifier
                        .width(28.dp)
                        .height(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                DtvRadioBarControl(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    label = if (isPlaying) "Pause" else "Play",
                    modifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
                    onFocusUp = onFocusUp,
                    onFocusDown = onFocusDown,
                    onClick = onTogglePlayPause
                )
                DtvRadioBarControl(
                    icon = Icons.Default.Stop,
                    label = "Stop",
                    onFocusUp = onFocusUp,
                    onFocusDown = onFocusDown,
                    onClick = onStop
                )
            }
        }
    }
}

@Composable
private fun DtvRadioBarControl(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onFocusUp: (() -> Unit)? = null,
    onFocusDown: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (focused) Color.Black else Color(0xFF171B22))
            .border(if (focused) 2.dp else 1.dp, if (focused) DtvChrome.ElectricBlue else DtvChrome.Divider, RoundedCornerShape(3.dp))
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> { onFocusUp?.invoke(); onFocusUp != null }
                        KeyEvent.KEYCODE_DPAD_DOWN -> { onFocusDown?.invoke(); onFocusDown != null }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (focused) Color.White else DtvChrome.PrimaryText,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = if (focused) Color.White else DtvChrome.PrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
