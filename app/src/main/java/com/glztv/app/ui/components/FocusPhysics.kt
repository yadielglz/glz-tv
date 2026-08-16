package com.glztv.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom Compose modifier providing smooth D-Pad focus spring physics, scale elevation,
 * dynamic drop shadow, and a glowing border outline for Android TV and touchscreen inputs.
 */
fun Modifier.tvFocusableWithPhysics(
    shape: Shape = RoundedCornerShape(20.dp),
    focusedScale: Float = 1.05f,
    focusedBorderWidth: Dp = 3.dp,
    glowColor: Color? = null,
    onFocusChange: ((Boolean) -> Unit)? = null
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "FocusScale"
    )

    val activeGlowColor = glowColor ?: MaterialTheme.colorScheme.primary

    this
        .onFocusChanged { state ->
            isFocused = state.isFocused
            onFocusChange?.invoke(state.isFocused)
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (isFocused) {
                Modifier
                    .shadow(
                        elevation = 12.dp,
                        shape = shape,
                        ambientColor = activeGlowColor,
                        spotColor = activeGlowColor
                    )
                    .border(
                        border = BorderStroke(focusedBorderWidth, activeGlowColor),
                        shape = shape
                    )
            } else {
                Modifier
            }
        )
        .focusable()
}
