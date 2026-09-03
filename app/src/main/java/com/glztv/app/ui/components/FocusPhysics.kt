package com.glztv.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.dp

/**
 * D-Pad focus physics for Android TV. No drawn outline — focus reads as a
 * spring scale bump plus a soft coloured glow (a tinted elevation halo) so the
 * selection/fill state of the underlying Surface is the "selector".
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
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "FocusScale"
    )

    val glowElevation by animateDpAsState(
        targetValue = if (isFocused) 22.dp else 0.dp,
        animationSpec = tween(180),
        label = "FocusGlow"
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
            this.shape = shape
            this.clip = true
        }
        .shadow(
            elevation = glowElevation,
            shape = shape,
            clip = false,
            ambientColor = activeGlowColor,
            spotColor = activeGlowColor
        )
}
