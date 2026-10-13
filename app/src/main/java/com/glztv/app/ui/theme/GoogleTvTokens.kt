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
 * Google TV 1:1 focus behavior:
 * - Subtle spring scale (1.04x - 1.06x)
 * - Solid crisp focus border outline (white or blue)
 * - Soft diffuse back-glow
 */
fun Modifier.googleTvFocusable(
    shape: Shape = GoogleTvTokens.ShapeCard,
    focusedScale: Float = 1.05f,
    focusBorderColor: Color = Color.White,
    glowColor: Color = Color(0x558AB4F8),
    onFocusChange: ((Boolean) -> Unit)? = null
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "GtvFocusScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "GtvFocusGlow"
    )

    this
        .onFocusChanged { state ->
            isFocused = state.isFocused
            onFocusChange?.invoke(state.isFocused)
        }
        .drawBehind {
            if (glowAlpha > 0.01f) {
                val spread = 10.dp.toPx() * glowAlpha
                drawRoundRect(
                    color = glowColor.copy(alpha = glowColor.alpha * glowAlpha),
                    topLeft = Offset(-spread, -spread),
                    size = Size(size.width + spread * 2f, size.height + spread * 2f),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.shape = shape
            this.clip = true
        }
        .then(
            if (isFocused) {
                Modifier.border(2.dp, focusBorderColor, shape)
            } else {
                Modifier
            }
        )
}
