package com.glztv.app.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Viewport classification for Android TV and mobile/tablet screens.
 *
 * Standard Android TV 720p (tvdpi/hdpi), 1080p (xhdpi 2.0x), and 4K (xxxhdpi 4.0x)
 * all map to a logical ~960x540dp canvas ([TvStandard]).
 * Some 720p/1080p boxes using mdpi/hdpi map to ~1280x720dp ([TvExpanded]).
 */
enum class TvViewportClass {
    Compact,
    TvStandard,
    TvExpanded
}

@Immutable
data class TvViewportSpec(
    val viewportClass: TvViewportClass,
    val widthDp: Dp,
    val heightDp: Dp,
    val safeHorizontal: Dp,
    val safeVertical: Dp,
    val sectionGap: Dp,
    val homeHeroHeight: Dp,
    val guideHeroHeight: Dp,
    val guideChannelColWidth: Dp,
    val guideSlotWidth: Dp,
    val drawerMaxWidth: Dp,
    val cardScale: Float,
    val minReadableSp: TextUnit,
    val isCompactHeight: Boolean,
    val isTvLayout: Boolean,
) {
    fun scaledDp(base: Dp): Dp = base * cardScale
}

val DefaultTvViewport = TvViewportSpec(
    viewportClass = TvViewportClass.TvStandard,
    widthDp = 960.dp,
    heightDp = 540.dp,
    safeHorizontal = 36.dp,
    safeVertical = 20.dp,
    sectionGap = 14.dp,
    homeHeroHeight = 186.dp,
    guideHeroHeight = 132.dp,
    guideChannelColWidth = 176.dp,
    guideSlotWidth = 144.dp,
    drawerMaxWidth = 420.dp,
    cardScale = 1.0f,
    minReadableSp = 12.sp,
    isCompactHeight = false,
    isTvLayout = true,
)

val LocalTvViewport = staticCompositionLocalOf { DefaultTvViewport }

@Composable
fun ProvideTvViewport(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val spec = remember(maxWidth, maxHeight) {
            computeTvViewportSpec(maxWidth, maxHeight)
        }
        CompositionLocalProvider(LocalTvViewport provides spec) {
            content()
        }
    }
}

fun computeTvViewportSpec(width: Dp, height: Dp): TvViewportSpec {
    return when {
        width < 600.dp -> TvViewportSpec(
            viewportClass = TvViewportClass.Compact,
            widthDp = width,
            heightDp = height,
            safeHorizontal = 16.dp,
            safeVertical = 12.dp,
            sectionGap = 10.dp,
            homeHeroHeight = (height * 0.30f).coerceIn(150.dp, 200.dp),
            guideHeroHeight = 118.dp,
            guideChannelColWidth = 132.dp,
            guideSlotWidth = 124.dp,
            drawerMaxWidth = (width * 0.92f).coerceAtMost(380.dp),
            cardScale = 0.88f,
            minReadableSp = 11.sp,
            isCompactHeight = height < 500.dp,
            isTvLayout = false,
        )

        width >= 1180.dp && height >= 660.dp -> TvViewportSpec(
            viewportClass = TvViewportClass.TvExpanded,
            widthDp = width,
            heightDp = height,
            safeHorizontal = 48.dp,
            safeVertical = 26.dp,
            sectionGap = 18.dp,
            homeHeroHeight = (height * 0.31f).coerceIn(205.dp, 248.dp),
            guideHeroHeight = 152.dp,
            guideChannelColWidth = 204.dp,
            guideSlotWidth = 168.dp,
            drawerMaxWidth = 460.dp,
            cardScale = 1.12f,
            minReadableSp = 12.sp,
            isCompactHeight = false,
            isTvLayout = true,
        )

        else -> TvViewportSpec(
            viewportClass = TvViewportClass.TvStandard,
            widthDp = width,
            heightDp = height,
            safeHorizontal = 36.dp,
            safeVertical = 20.dp,
            sectionGap = 14.dp,
            homeHeroHeight = (height * 0.34f).coerceIn(170.dp, 196.dp),
            guideHeroHeight = (height * 0.24f).coerceIn(124.dp, 144.dp),
            guideChannelColWidth = (width * 0.185f).coerceIn(168.dp, 192.dp),
            guideSlotWidth = (width * 0.15f).coerceIn(136.dp, 156.dp),
            drawerMaxWidth = (width * 0.44f).coerceIn(380.dp, 430.dp),
            cardScale = (width.value / 960f).coerceIn(0.92f, 1.06f),
            minReadableSp = 12.sp,
            isCompactHeight = height < 500.dp,
            isTvLayout = true,
        )
    }
}
