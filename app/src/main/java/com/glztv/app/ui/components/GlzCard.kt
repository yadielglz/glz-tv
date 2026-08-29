package com.glztv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Shared card language for GLZ TV, taken from [ExpressiveNavigationRail]:
 * very round corners, translucent surfaceVariant fills, a white hairline border,
 * and a focus state where the fill flips to a solid accent.
 *
 * Use [GlzPanel] for static containers and [GlzFocusCard] for anything the
 * D-pad can land on.
 */
object GlzCardDefaults {
    /** Chips, list rows, dense tiles. */
    val RadiusSmall = 18.dp
    /** Standard cards — matches the rail's focusable destinations. */
    val RadiusMedium = 24.dp
    /** Hero cards and full-height panels — matches the rail shell. */
    val RadiusLarge = 32.dp

    /** 1dp hairline used on every card edge (dark-only app). */
    val hairline = Color.White.copy(alpha = 0.12f)

    @Composable
    fun panelFill(): Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    @Composable
    fun cardFill(): Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    /** Readable ink for text/icons laid directly on [accent]. */
    fun onAccent(accent: Color): Color =
        if (accent.luminance() > 0.55f) Color(0xFF0A0A0A) else Color.White
}

/** Static container card. Mirrors the rail's outer [Surface]. */
@Composable
fun GlzPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlzCardDefaults.RadiusLarge),
    fill: Color = GlzCardDefaults.panelFill(),
    border: BorderStroke? = BorderStroke(1.dp, GlzCardDefaults.hairline),
    tonalElevation: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = fill,
        border = border,
        tonalElevation = tonalElevation,
        content = content,
    )
}

/**
 * Focusable card. Identical focus/selected model to the rail's `RailDestination`:
 *
 * - rest: translucent surfaceVariant, hairline border, `onSurface` content
 * - selected: `accent @ 18%` wash, `accent @ 50%` border, `accent` content
 * - focused: solid `accent` fill, contrast content, spring scale + accent glow
 *
 * [content] receives the current focused flag so callers can adjust secondary
 * text emphasis if they need to.
 */
@Composable
fun GlzFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(GlzCardDefaults.RadiusMedium),
    focusedScale: Float = 1.06f,
    restFill: Color = GlzCardDefaults.cardFill(),
    contentModifier: Modifier = Modifier,
    onFocusChange: ((Boolean) -> Unit)? = null,
    content: @Composable ColumnScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    val container = when {
        focused -> accent
        selected -> accent.copy(alpha = 0.18f)
        else -> restFill
    }
    val contentColor = when {
        focused -> GlzCardDefaults.onAccent(accent)
        selected -> accent
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border = when {
        focused -> null // the glow ring from tvFocusableWithPhysics is the edge
        selected -> BorderStroke(1.dp, accent.copy(alpha = 0.5f))
        else -> BorderStroke(1.dp, GlzCardDefaults.hairline)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.tvFocusableWithPhysics(
            shape = shape,
            focusedScale = focusedScale,
            glowColor = accent,
            onFocusChange = {
                focused = it
                onFocusChange?.invoke(it)
            },
        ),
        shape = shape,
        color = container,
        contentColor = contentColor,
        border = border,
        tonalElevation = if (focused) 12.dp else 2.dp,
    ) {
        Column(modifier = contentModifier, content = { content(focused) })
    }
}
