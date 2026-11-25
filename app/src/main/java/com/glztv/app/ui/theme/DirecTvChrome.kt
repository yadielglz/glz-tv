package com.glztv.app.ui.theme

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.model.WeatherInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared DirecTV HD Broadcast Design System tokens & chrome used across
 * Home, EPG Guide, Movies On Demand, Radio, Weather, and Settings.
 */
object DtvChrome {
    val ScreenBg = Color(0xFF06080B)
    val ElectricBlue = Color(0xFF1990FF)
    val ElectricBlueSoft = Color(0xFF4DA8FF)
    val LiveRed = Color(0xFFEF4444)
    val Gold = Color(0xFFF5B724)
    val Divider = Color(0xFF2B3038)
    val RowDivider = Color(0xFF1E232B)
    val CardBorder = Color(0xFF282E38)
    val PrimaryText = Color(0xFFE4E8EE)
    val SecondaryText = Color(0xFF8A929E)
    val MutedText = Color(0xFF636B77)

    val TopBarGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF23272F),
            0.50f to Color(0xFF15181E),
            1.00f to Color(0xFF0D0F13)
        )
    )
    val BannerGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF1C2027),
            0.48f to Color(0xFF101318),
            1.00f to Color(0xFF080A0D)
        )
    )
    val StripGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF20242B),
            0.50f to Color(0xFF16191F),
            1.00f to Color(0xFF101318)
        )
    )
    val CardGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF191D25),
            0.55f to Color(0xFF12151B),
            1.00f to Color(0xFF0D1014)
        )
    )
    val PanelBgGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF1A1E24),
            0.35f to Color(0xFF0E1116),
            1.00f to Color(0xFF06080B)
        )
    )
}

enum class DtvNavTab(val label: String) {
    Home("Home"),
    LiveTv("Live TV"),
    Guide("Guide"),
    Movies("Movies"),
    Radio("Radio"),
    Apps("Apps"),
    Weather("Weather"),
    SportsBar("Sports Bar")
}

private val dtvHourMinFmt = SimpleDateFormat("h:mm", Locale.getDefault())
private val dtvAmPmFmt = SimpleDateFormat("a", Locale.getDefault())
private val dtvDateFmt = SimpleDateFormat("EEE M/d", Locale.getDefault())

fun formatDtvCompactTime(millis: Long): String {
    val date = Date(millis)
    val hm = synchronized(dtvHourMinFmt) { dtvHourMinFmt.format(date) }
    val suffix = synchronized(dtvAmPmFmt) { dtvAmPmFmt.format(date) }
        .lowercase(Locale.getDefault())
        .firstOrNull()
        ?.toString()
        .orEmpty()
    return "$hm$suffix"
}

fun formatDtvCompactDate(millis: Long): String = synchronized(dtvDateFmt) {
    dtvDateFmt.format(Date(millis))
}

/**
 * Shared DirecTV HD Top Command Bar pinned identically across Home, Guide, Movies, Radio, Apps, and Weather.
 */
@Composable
fun DtvTopCommandBar(
    activeTab: DtvNavTab,
    nowMillis: Long,
    weather: WeatherInfo?,
    guestLabel: String,
    hasUpdate: Boolean = false,
    showMoviesTab: Boolean = true,
    sportsBarKioskEnabled: Boolean = false,
    onSelectTab: (DtvNavTab) -> Unit,
    onOpenUpdate: () -> Unit = {},
    onOpenWelcome: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewport = LocalTvViewport.current
    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(24.dp)
    val barHeight = if (viewport.isCompactHeight) 42.dp else 46.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(DtvChrome.TopBarGradient)
                .padding(horizontal = horizontalPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: GlzTV wordmark + hairline-separated navigation tabs
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.padding(end = 14.dp)
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(DtvChrome.ElectricBlue)
                    )
                    Text(
                        text = "GlzTV",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        maxLines = 1
                    )
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(DtvChrome.Divider))

                val navTabs = buildList {
                    add(DtvNavTab.Home)
                    add(DtvNavTab.LiveTv)
                    add(DtvNavTab.Guide)
                    if (showMoviesTab) add(DtvNavTab.Movies)
                    add(DtvNavTab.Radio)
                    add(DtvNavTab.Apps)
                    add(DtvNavTab.Weather)
                    if (sportsBarKioskEnabled) add(DtvNavTab.SportsBar)
                }
                navTabs.forEach { tab ->
                    DtvTopBarTab(
                        label = tab.label,
                        active = tab == activeTab,
                        compact = viewport.isCompactHeight,
                        onClick = { onSelectTab(tab) }
                    )
                    Box(Modifier.width(1.dp).fillMaxHeight().background(DtvChrome.Divider.copy(alpha = 0.7f)))
                }
            }

            // Right: Borderless inline broadcast telemetry (Update · Weather · Clock · Name · Settings)
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (hasUpdate) {
                    DtvTopStatusChip(
                        label = "Update",
                        accent = true,
                        onClick = onOpenUpdate
                    ) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = "Update available",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                if (weather != null) {
                    DtvTopStatusChip(
                        label = "${weather.temperature}°",
                        onClick = { onSelectTab(DtvNavTab.Weather) }
                    ) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = "Weather",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = formatDtvCompactTime(nowMillis),
                    color = DtvChrome.PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                if (guestLabel.isNotBlank()) {
                    DtvTopStatusChip(
                        label = guestLabel.take(14),
                        onClick = onOpenWelcome
                    )
                }

                DtvTopStatusChip(
                    label = null,
                    onClick = onOpenSettings
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = DtvChrome.PrimaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.Divider))
    }
}

@Composable
private fun DtvTopBarTab(
    label: String,
    active: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (isFocused || active) Color.Black else Color.Transparent)
            .drawBehind {
                if (isFocused || active) {
                    val barH = 3.5.dp.toPx()
                    drawRect(
                        color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.ElectricBlue.copy(alpha = 0.72f),
                        topLeft = Offset(0f, size.height - barH),
                        size = Size(size.width, barH)
                    )
                }
            }
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (compact) 10.dp else 12.dp)
    ) {
        Text(
            text = label,
            color = if (isFocused || active) Color.White else DtvChrome.SecondaryText,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = if (isFocused || active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun DtvTopStatusChip(
    label: String?,
    accent: Boolean = false,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val bg = when {
        isFocused -> Color.Black
        accent -> DtvChrome.ElectricBlue
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .background(bg)
            .drawBehind {
                if (isFocused) {
                    val barH = 3.dp.toPx()
                    drawRect(
                        color = DtvChrome.ElectricBlue,
                        topLeft = Offset(0f, size.height - barH),
                        size = Size(size.width, barH)
                    )
                }
            }
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (label == null) 9.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        leadingIcon?.invoke()
        if (label != null) {
            Text(
                text = label,
                color = if (isFocused || accent) Color.White else DtvChrome.PrimaryText,
                fontSize = 13.sp,
                fontWeight = if (isFocused || accent) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * DirecTV HD Glossy Segmented Filter Strip (matches the OSD options strip)
 * used for EPG categories, Movie genres, and Radio genres.
 */
@Composable
fun DtvSegmentedStripBar(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val viewport = LocalTvViewport.current
    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(28.dp)
    val stripHeight = if (viewport.isCompactHeight) 36.dp else 40.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(stripHeight)
                .background(DtvChrome.StripGradient)
                .padding(horizontal = horizontalPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(0.dp)
            ) {
                items(items, key = { "dtv-strip-$it" }) { item ->
                    DtvSegmentedStripTab(
                        label = item,
                        selected = item.equals(selectedItem, ignoreCase = true),
                        onClick = { onSelect(item) }
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(DtvChrome.Divider.copy(alpha = 0.7f))
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.Divider))
    }
}

@Composable
private fun DtvSegmentedStripTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (isFocused || selected) Color.Black else Color.Transparent)
            .drawBehind {
                if (isFocused || selected) {
                    val barH = 3.dp.toPx()
                    drawRect(
                        color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.ElectricBlue.copy(alpha = 0.75f),
                        topLeft = Offset(0f, size.height - barH),
                        size = Size(size.width, barH)
                    )
                }
            }
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = label,
            color = if (isFocused || selected) Color.White else DtvChrome.SecondaryText,
            fontSize = 12.sp,
            fontWeight = if (isFocused || selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun DtvSectionHeader(
    title: String,
    trailing: String? = null,
    showLiveDot: Boolean = false,
    horizontalPad: Dp = 28.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showLiveDot) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(DtvChrome.LiveRed)
                )
            } else {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(12.dp)
                        .background(DtvChrome.ElectricBlue)
                )
            }
            Text(
                text = title,
                color = DtvChrome.PrimaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                maxLines = 1
            )
        }
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                color = DtvChrome.MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DtvActionButton(
    label: String,
    primary: Boolean = false,
    showPlayIcon: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val bg = when {
        isFocused && primary -> Color.White
        isFocused -> Color.Black
        primary -> DtvChrome.PrimaryText
        else -> Color(0xFF171B22)
    }
    val fg = when {
        primary -> Color.Black
        isFocused -> Color.White
        else -> DtvChrome.PrimaryText
    }
    val border = when {
        isFocused -> DtvChrome.ElectricBlue
        primary -> Color.Transparent
        else -> DtvChrome.Divider
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .border(if (isFocused) 2.dp else 1.dp, border, RoundedCornerShape(3.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showPlayIcon) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun DtvProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(3.dp)
            .background(Color.White.copy(alpha = 0.14f))
    ) {
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(DtvChrome.ElectricBlue)
            )
        }
    }
}
