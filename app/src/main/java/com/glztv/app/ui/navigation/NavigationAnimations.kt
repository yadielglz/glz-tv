package com.glztv.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val PANEL_ANIM_IN = 240
private const val PANEL_ANIM_OUT = 170

/** Enter transition for an edge panel: slide in from its own screen edge + fade. */
fun panelEnter(fromLeft: Boolean = false) =
    fadeIn(tween(PANEL_ANIM_IN)) +
        slideInHorizontally(tween(PANEL_ANIM_IN)) { width -> if (fromLeft) -width else width }

/** Exit transition mirroring [panelEnter]. */
fun panelExit(fromLeft: Boolean = false) =
    fadeOut(tween(PANEL_ANIM_OUT)) +
        slideOutHorizontally(tween(PANEL_ANIM_OUT)) { width -> if (fromLeft) -width else width }
