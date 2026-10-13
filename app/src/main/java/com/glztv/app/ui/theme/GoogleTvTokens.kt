package com.glztv.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Authentic Google TV Design System tokens: colors, shapes, and focus physics
 * modeled 1:1 on the Google TV Live Channels interface.
 */
object GoogleTvTokens {
    // Backgrounds
    val BackgroundDark = Color(0xFF0B0D0F)
    val BackgroundElevated = Color(0xFF13151B)
    val SurfaceCard = Color(0xFF1A1D24)
    val SurfaceCardTranslucent = Color(0xE614161D)
    val SurfaceTimelineSlot = Color(0x33282C37)
    val SurfaceTimelineSlotActive = Color(0x663B4151)

    // Google Accents
    val BlueAccent = Color(0xFF8AB4F8)
    val BlueAccentDark = Color(0xFF1A73E8)
    val LiveRed = Color(0xFFEA4335)
    val StarGold = Color(0xFFFDD663)
    val TextPrimary = Color(0xFFF1F3F4)
    val TextSecondary = Color(0xFF9AA0A6)
    val TextTertiary = Color(0xFF5F6368)

    // Shapes
    val ShapePill = CircleShape
    val ShapeCardSmall = RoundedCornerShape(8.dp)
    val ShapeCard = RoundedCornerShape(12.dp)
    val ShapeCardLarge = RoundedCornerShape(18.dp)
    val ShapeBanner = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    // Scrims & Gradients
    val BottomOsdScrim = Brush.verticalGradient(
        listOf(
            Color.Transparent,
            Color(0xCC08090C),
            Color(0xF50B0D0F),
            Color(0xFF0B0D0F)
        )
    )

    val TopHeroScrim = Brush.horizontalGradient(
        listOf(
            Color(0xFF0B0D0F),
            Color(0xF20B0D0F),
            Color(0xAA0B0D0F),
            Color.Transparent
        )
    )

    val GuideBackdropScrim = Brush.verticalGradient(
        listOf(
            Color(0xB30B0D0F),
            Color(0xEE0B0D0F),
            Color(0xFF0B0D0F)
        )
    )
}

/**
 * Authentic Google TV / Material 3 focus behavior:
 * - Clean solid fill inversion (handled by container)
 * - Subtle scale (1.02x - 1.05x)
 * - No border outlines, no shadows, no glow — exactly like official Google TV software
 */
fun Modifier.googleTvFocusable(
    shape: Shape = GoogleTvTokens.ShapeCard,
    focusedScale: Float = 1.04f,
    focusBorderColor: Color = Color.Transparent,
    onFocusChange: ((Boolean) -> Unit)? = null
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    this
        .onFocusChanged { state ->
            if (isFocused != state.isFocused) {
                isFocused = state.isFocused
                onFocusChange?.invoke(state.isFocused)
            }
        }
        .then(
            if (isFocused && focusedScale > 1.001f) {
                Modifier.graphicsLayer {
                    scaleX = focusedScale
                    scaleY = focusedScale
                    this.shape = shape
                    this.clip = true
                }
            } else {
                Modifier
            }
        )
}
