package com.glztv.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * D-Pad focus physics for Android TV. No drawn outline — focus reads as a
 * smooth (non-bouncy) scale bump plus a soft feathered colour glow behind the
 * element, so the fill/selected state of the underlying Surface is the "selector".
 *
 * The glow is painted as a few stacked translucent round-rects rather than an
 * elevation shadow, which on some renderers showed a hard square while animating.
 */
fun Modifier.tvFocusableWithPhysics(
    shape: Shape = RoundedCornerShape(20.dp),
    focusedScale: Float = 1.05f,
    glowColor: Color? = null,
    onFocusChange: ((Boolean) -> Unit)? = null
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "FocusScale"
    )

    val glow by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = if (isFocused) 150 else 130),
        label = "FocusGlow"
    )

    val activeGlowColor = glowColor ?: MaterialTheme.colorScheme.primary

    this
        .onFocusChanged { state ->
            isFocused = state.isFocused
            onFocusChange?.invoke(state.isFocused)
        }
        .drawBehind {
            if (glow > 0.01f) {
                val layers = 4
                val maxSpread = 14.dp.toPx()
                val baseCorner = 20.dp.toPx()
                for (i in layers downTo 1) {
                    val spread = maxSpread * (i.toFloat() / layers) * glow
                    val alpha = 0.12f * glow * (1f - (i - 1f) / layers)
                    drawRoundRect(
                        color = activeGlowColor.copy(alpha = alpha),
                        topLeft = Offset(-spread, -spread),
                        size = Size(size.width + spread * 2f, size.height + spread * 2f),
                        cornerRadius = CornerRadius(baseCorner + spread, baseCorner + spread)
                    )
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.shape = shape
            this.clip = true
        }
}
