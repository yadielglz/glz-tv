package com.glztv.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Clean Material 3 focus physics for Android TV.
 * Uses crisp high-contrast border and subtle scaling without blurry glow/shadow layers.
 */
fun Modifier.tvFocusableWithPhysics(
    shape: Shape = RoundedCornerShape(20.dp),
    focusedScale: Float = 1.04f,
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

    val activeBorderColor = glowColor ?: MaterialTheme.colorScheme.primary

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
}

