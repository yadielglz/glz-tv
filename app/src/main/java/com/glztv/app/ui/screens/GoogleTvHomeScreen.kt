package com.glztv.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import android.view.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.GuestExperience
import com.glztv.app.RadioStation
import com.glztv.app.model.EntertainmentApp
import com.glztv.app.model.NetworkInfo
import com.glztv.app.model.WeatherInfo
import com.glztv.app.ui.components.GoogleTvChannelLogo
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val homeTimeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun formatHomeTime(millis: Long): String = synchronized(homeTimeFormatter) {
    homeTimeFormatter.format(Date(millis))
}

/**
 * Authentic Full-Screen Google TV / Material 3 Home Experience.
 *
 * Design Architecture:
 * 1. Pinned Top Ambient Status Bar:
 *    - Clickable Welcome Card on left -> brings up "You" modal.
 *    - Clickable Weather Pill -> brings up "Weather" modal.
 *    - Network status, Live Clock, and Dedicated Settings Pill button.
 * 2. Static Hero Spotlight Billboard:
 *    - Fixed at the top half of the screen; does NOT scroll away.
 *    - Displays the highlighted channel's live program synopsis, time countdown, progress bar,
 *      and primary action pills (WATCH LIVE, FAVORITE, LIVE GUIDE).
 *    - Channel stream/showcase artwork on the right has an elegant cinematic gradient on its last 1/3.
 * 3. Scrollable Swimlanes Below:
 *    - The bottom half scrolls smoothly with D-pad while the Billboard and Top Bar stay anchored.
 *    - Horizontal Radio Carousel right on the Home screen.
 *    - Jump Back In / Featured Lineup, On Now 16:9 Live Cards, Favorites, Genre browsing, Apps.
 * 4. Full Real Estate:
 *    - Utilizes 100% of the screen width (no navigation rail).
 */
@Composable
fun GoogleTvHomeScreen(
    channels: List<Channel>,
    guide: EpgGuide,
    resumeChannel: Channel?,
    previewChannel: Channel?,
    favorites: Set<String>,
    recentChannels: List<Channel>,
    radioStations: List<RadioStation> = emptyList(),
    currentRadioStation: RadioStation? = null,
    onPlayRadioStation: (RadioStation) -> Unit = {},
    entertainmentApps: List<EntertainmentApp>,
    guestName: String,
    experience: GuestExperience,
    weather: WeatherInfo?,
    networkInfo: NetworkInfo?,
    onWatchChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenGuide: () -> Unit,
    onOpenRadio: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWelcome: () -> Unit = {},
    onLaunchApp: (EntertainmentApp) -> Unit,
    sportsBarKioskEnabled: Boolean = false,
    onStartSportsBarKiosk: () -> Unit = {},
    previewContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    // GLZ Hub provisioned preview channel has highest priority as requested
    val defaultChannel = remember(channels, resumeChannel, previewChannel) {
        previewChannel ?: resumeChannel ?: channels.firstOrNull()
    }
    var focusedChannel by remember(defaultChannel?.id) { mutableStateOf(defaultChannel) }
    var selectedGenre by remember { mutableStateOf("ALL") }

    val activeChannel = focusedChannel ?: defaultChannel

    // Deduplicated recent channels list with fallback to top lineup so rail is never empty
    val effectiveRecent = remember(recentChannels, resumeChannel, channels) {
        val list = mutableListOf<Channel>()
        resumeChannel?.let { list.add(it) }
        list.addAll(recentChannels.filter { it.id != resumeChannel?.id })
        val distinct = list.distinctBy { it.id }
        if (distinct.isNotEmpty()) distinct.take(10)
        else channels.take(8)
    }

    val liveChannels = remember(channels) {
        channels.take(15)
    }

    val favoriteChannels = remember(channels, favorites) {
        val favs = channels.filter { it.id in favorites }
        if (favs.isNotEmpty()) favs
        else if (channels.size > 8) channels.drop(8).take(8) // Popular channels fallback
        else emptyList()
    }

    val availableGenres = remember(channels) {
        val genres = channels.mapNotNull { it.group.takeIf(String::isNotBlank) }.distinct()
        if (genres.isNotEmpty()) listOf("ALL") + genres.take(8)
        else emptyList()
    }

    val genreFilteredChannels = remember(channels, selectedGenre) {
        if (selectedGenre == "ALL") channels.take(10)
        else channels.filter { it.group.equals(selectedGenre, ignoreCase = true) }.take(10)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GoogleTvTokens.BackgroundDark)
    ) {
        // 1. PINNED / STATIC TOP AMBIENT STATUS ROW (Always visible while on Home)
        GoogleTvHomeTopBar(
            guestName = guestName,
            experience = experience,
            now = now,
            weather = weather,
            networkInfo = networkInfo,
            onOpenWelcome = onOpenWelcome,
            onOpenWeather = onOpenWeather,
            onOpenSettings = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 4.dp)
        )

        // 2. PINNED / STATIC HERO SPOTLIGHT BILLBOARD (FULL WIDTH edge-to-edge as requested)
        GoogleTvSpotlightBillboard(
            channel = activeChannel,
            guide = guide,
            now = now,
            experience = experience,
            isFavorite = activeChannel != null && activeChannel.id in favorites,
            onWatch = { activeChannel?.let(onWatchChannel) ?: onOpenGuide() },
            onToggleFavorite = { activeChannel?.let { onToggleFavorite(it.id) } },
            onOpenGuide = onOpenGuide,
            onOpenRadio = onOpenRadio,
            onOpenWeather = onOpenWeather,
            onOpenSettings = onOpenSettings,
            previewContent = previewContent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )

        // 3. SCROLLABLE SWIMLANES (All horizontal rails scroll beneath the static hero card)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 20.dp)
        ) {
            // RAIL 1: JUMP BACK IN / FEATURED LINEUP
            item(key = "rail-recent") {
                val title = if (recentChannels.isNotEmpty() || resumeChannel != null) "JUMP BACK IN" else "FEATURED LINEUP"
                val icon = if (recentChannels.isNotEmpty() || resumeChannel != null) Icons.Default.History else Icons.Default.Star
                RailHeader(title = title, icon = icon)

                if (channels.isEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(5) {
                            SkeletonCard(width = 200.dp, height = 80.dp)
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(effectiveRecent, key = { index, ch -> "recent-${ch.id.ifBlank { "c" }}-$index" }) { _, ch ->
                            RecentChannelCard(
                                channel = ch,
                                guide = guide,
                                now = now,
                                onFocus = { focusedChannel = ch },
                                onClick = { onWatchChannel(ch) }
                            )
                        }
                    }
                }
            }

            // RAIL 2: ON NOW · LIVE HIGHLIGHTS (16:9 show cards with live progress)
            if (liveChannels.isNotEmpty()) {
                item(key = "rail-live-highlights") {
                    RailHeader(title = "ON NOW · LIVE HIGHLIGHTS", icon = Icons.Default.LiveTv, iconTint = GoogleTvTokens.LiveRed)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(liveChannels, key = { index, ch -> "live-hl-${ch.id.ifBlank { "c" }}-$index" }) { _, ch ->
                            LiveShowCard(
                                channel = ch,
                                guide = guide,
                                now = now,
                                onFocus = { focusedChannel = ch },
                                onClick = { onWatchChannel(ch) }
                            )
                        }
                    }
                }
            }

            // RAIL 3: GLZ RADIO STATIONS (Horizontal Carousel as requested)
            if (radioStations.isNotEmpty()) {
                item(key = "rail-radio-stations") {
                    RailHeader(title = "GLZ RADIO · LIVE STATIONS", icon = Icons.Default.Radio, iconTint = Color(0xFFFF6B2C))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(radioStations, key = { index, st -> "radio-${st.code}-$index" }) { _, st ->
                            RadioStationCard(
                                station = st,
                                nowPlaying = currentRadioStation?.code == st.code,
                                onClick = { onPlayRadioStation(st) }
                            )
                        }
                    }
                }
            }

            // RAIL 4: FAVORITES OR POPULAR CHANNELS
            if (favoriteChannels.isNotEmpty()) {
                item(key = "rail-favorites") {
                    val hasFavs = favorites.isNotEmpty()
                    val title = if (hasFavs) "FAVORITE CHANNELS" else "POPULAR RIGHT NOW"
                    val icon = Icons.Default.Star
                    val iconTint = if (hasFavs) GoogleTvTokens.StarGold else GoogleTvTokens.TextSecondary
                    RailHeader(title = title, icon = icon, iconTint = iconTint)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(favoriteChannels, key = { index, ch -> "fav-${ch.id.ifBlank { "c" }}-$index" }) { _, ch ->
                            FavoriteChannelCard(
                                channel = ch,
                                guide = guide,
                                now = now,
                                isStar = hasFavs,
                                onFocus = { focusedChannel = ch },
                                onClick = { onWatchChannel(ch) }
                            )
                        }
                    }
                }
            }

            // RAIL 5: BROWSE BY GENRE
            if (availableGenres.size > 1) {
                item(key = "rail-genres") {
                    RailHeader(title = "BROWSE BY GENRE", icon = Icons.Default.CalendarMonth)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(availableGenres, key = { index, genre -> "genre-chip-$genre-$index" }) { _, genre ->
                            val isSelected = genre.equals(selectedGenre, ignoreCase = true)
                            var isFocused by remember { mutableStateOf(false) }
                            Surface(
                                onClick = { selectedGenre = genre },
                                shape = CircleShape,
                                color = when {
                                    isFocused -> Color.White
                                    isSelected -> GoogleTvTokens.BlueAccent
                                    else -> Color.White.copy(alpha = 0.08f)
                                },
                                contentColor = when {
                                    isFocused -> Color.Black
                                    isSelected -> Color(0xFF001F2A)
                                    else -> Color.White
                                },
                                modifier = Modifier.googleTvFocusable(
                                    shape = CircleShape,
                                    focusedScale = 1.06f,
                                    onFocusChange = { isFocused = it }
                                )
                            ) {
                                Text(
                                    text = genre.uppercase(),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(genreFilteredChannels, key = { index, ch -> "genre-ch-${ch.id.ifBlank { "c" }}-$index" }) { _, ch ->
                            RecentChannelCard(
                                channel = ch,
                                guide = guide,
                                now = now,
                                onFocus = { focusedChannel = ch },
                                onClick = { onWatchChannel(ch) }
                            )
                        }
                    }
                }
            }

            // RAIL 6: APPS & SERVICES
            item(key = "rail-apps") {
                RailHeader(title = "APPS & SERVICES", icon = Icons.Default.Apps)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    // Streaming & Entertainment Apps
                    itemsIndexed(entertainmentApps, key = { index, app -> "app-${app.packageName}-$index" }) { _, app ->
                        var isFocused by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { onLaunchApp(app) },
                            modifier = Modifier
                                .width(110.dp)
                                .height(72.dp)
                                .googleTvFocusable(
                                    shape = RoundedCornerShape(12.dp),
                                    focusedScale = 1.05f,
                                    onFocusChange = { isFocused = it }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFocused) Color.White else GoogleTvTokens.SurfaceCard,
                            contentColor = if (isFocused) Color.Black else Color.White
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(app.accent)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    app.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Radio Service Shortcut
                    item(key = "srv-radio") {
                        ServiceShortcutCard(
                            label = "GLZ Radio",
                            icon = Icons.Default.Radio,
                            color = Color(0xFFFF6B2C),
                            onClick = onOpenRadio
                        )
                    }

                    // Weather Service Shortcut
                    item(key = "srv-weather") {
                        ServiceShortcutCard(
                            label = "Weather",
                            icon = Icons.Default.WbSunny,
                            color = Color(0xFF29B6F6),
                            onClick = onOpenWeather
                        )
                    }

                    // Sports Bar Kiosk (if enabled)
                    if (sportsBarKioskEnabled) {
                        item(key = "srv-sports-bar") {
                            ServiceShortcutCard(
                                label = "Sports Bar",
                                icon = Icons.Default.Sports,
                                color = Color(0xFF00C853),
                                onClick = onStartSportsBarKiosk
                            )
                        }
                    }

                    // Settings Service Shortcut
                    item(key = "srv-settings") {
                        ServiceShortcutCard(
                            label = "Settings",
                            icon = Icons.Default.Settings,
                            color = Color(0xFF78909C),
                            onClick = onOpenSettings
                        )
                    }
                }
            }

            item(key = "bottom-spacer") {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Top Ambient Header Bar with Clickable Welcome greeting, Stay details, Clock, Weather, and Settings.
 */
@Composable
private fun GoogleTvHomeTopBar(
    guestName: String,
    experience: GuestExperience,
    now: Long,
    weather: WeatherInfo?,
    networkInfo: NetworkInfo?,
    onOpenWelcome: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val timeGreeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Clickable Profile / Welcome Greeting Pill (brings up You Modal)
        var welcomeFocused by remember { mutableStateOf(false) }
        Surface(
            onClick = onOpenWelcome,
            shape = CircleShape,
            color = if (welcomeFocused) Color.White else Color.White.copy(alpha = 0.08f),
            contentColor = if (welcomeFocused) Color.Black else Color.White,
            modifier = Modifier
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP &&
                        (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                         event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                        onOpenWelcome()
                        true
                    } else false
                }
                .googleTvFocusable(
                    shape = CircleShape,
                    focusedScale = 1.04f,
                    onFocusChange = { welcomeFocused = it }
                )
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    Modifier.size(16.dp),
                    tint = if (welcomeFocused) Color.Black else GoogleTvTokens.BlueAccent
                )
                Text(
                    text = buildString {
                        append(timeGreeting)
                        append(", ")
                        append(guestName.ifBlank { "Guest" })
                        if (!experience.roomNumber.isNullOrBlank()) {
                            append(" · Room ")
                            append(experience.roomNumber)
                        }
                        if (experience.propertyName.isNotBlank()) {
                            append(" · ")
                            append(experience.propertyName)
                        }
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (welcomeFocused) Color.Black else Color.White
                )

                experience.checkoutTime?.takeIf(String::isNotBlank)?.let { checkout ->
                    Box(
                        Modifier
                            .padding(start = 4.dp)
                            .clip(CircleShape)
                            .background(if (welcomeFocused) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Checkout $checkout",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (welcomeFocused) Color.Black else GoogleTvTokens.BlueAccent
                        )
                    }
                }
            }
        }

        // Right: Ambient Glance Status (Clickable Weather, Network, Clock, Settings)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Clickable Weather Pill (brings up Weather Modal)
            weather?.let { w ->
                var weatherFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = onOpenWeather,
                    shape = CircleShape,
                    color = if (weatherFocused) Color.White else Color.White.copy(alpha = 0.07f),
                    contentColor = if (weatherFocused) Color.Black else Color.White,
                    modifier = Modifier
                        .onKeyEvent { event ->
                            if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP &&
                                (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                 event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                                onOpenWeather()
                                true
                            } else false
                        }
                        .googleTvFocusable(
                            shape = CircleShape,
                            focusedScale = 1.05f,
                            onFocusChange = { weatherFocused = it }
                        )
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = null,
                            Modifier.size(13.dp),
                            tint = if (weatherFocused) Color.Black else Color(0xFFFFB300)
                        )
                        Text(
                            "${w.temperature}°F · ${w.location}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (weatherFocused) Color.Black else Color.White
                        )
                    }
                }
            }

            networkInfo?.let { net ->
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.06f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            Modifier.size(12.dp),
                            tint = GoogleTvTokens.TextSecondary
                        )
                        Text(
                            net.connection,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoogleTvTokens.TextSecondary
                        )
                    }
                }
            }

            Text(
                formatHomeTime(now),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Settings Pill Button (Matching Google TV Top Right Header)
            var settingsFocused by remember { mutableStateOf(false) }
            Surface(
                onClick = onOpenSettings,
                shape = CircleShape,
                color = if (settingsFocused) Color.White else Color.White.copy(alpha = 0.09f),
                contentColor = if (settingsFocused) Color.Black else Color.White,
                modifier = Modifier
                    .onKeyEvent { event ->
                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP &&
                            (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                             event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                            onOpenSettings()
                            true
                        } else false
                    }
                    .googleTvFocusable(
                        shape = CircleShape,
                        focusedScale = 1.06f,
                        onFocusChange = { settingsFocused = it }
                    )
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        Modifier.size(14.dp),
                        tint = if (settingsFocused) Color.Black else Color.White
                    )
                    Text(
                        "Settings",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (settingsFocused) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

/**
 * Static Hero Spotlight Billboard.
 * Displays highlighted channel, synopsis, progress bar, action buttons,
 * and a right stream/showcase area with gradient blend on the last 1/3 of the card stream.
 */
@Composable
private fun GoogleTvSpotlightBillboard(
    channel: Channel?,
    guide: EpgGuide,
    now: Long,
    experience: GuestExperience,
    isFavorite: Boolean,
    onWatch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenRadio: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenSettings: () -> Unit,
    previewContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeProg = remember(channel?.id, guide, now) {
        channel?.let { guide.nowProgrammeForChannel(it, now) }
    }
    val isLive = activeProg != null && activeProg.startMillis <= now && activeProg.endMillis > now

    val progress = activeProg?.let {
        if (it.startMillis <= now && it.endMillis > now) {
            ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L)).coerceIn(0f, 1f)
        } else 0f
    } ?: 0f

    val remainingMinutes = activeProg?.let {
        if (it.startMillis <= now && it.endMillis > now) {
            ((it.endMillis - now).coerceAtLeast(0L) + 59_999L) / 60_000L
        } else null
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(215.dp),
        shape = RectangleShape,
        color = Color(0xFF0B0D0F)
    ) {
        Box(Modifier.fillMaxSize()) {
            // Billboard Content Layout
            if (channel != null) {
                // --- Spotlight TV Stream Billboard ---
                // 1. Live stream preview container positioned on the right
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.48f)
                        .align(Alignment.CenterEnd)
                        .background(Color(0xFF0B0D0F))
                ) {
                    if (previewContent != null) {
                        Box(Modifier.fillMaxSize()) {
                            previewContent()
                        }
                    } else {
                        CinematicShowcaseCard(
                            channel = channel,
                            guide = guide,
                            activeProgTitle = activeProg?.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Continuous horizontal dissolve gradient:
                    // Left edge (0.00f) is 100% solid Color(0xFF0B0D0F), matching the billboard base color perfectly.
                    // Smoothly eases out to transparent across 50% width, leaving the right portion of the live stream clear & vibrant.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.00f to Color(0xFF0B0D0F),
                                    0.18f to Color(0xFF0B0D0F).copy(alpha = 0.85f),
                                    0.36f to Color(0xFF0B0D0F).copy(alpha = 0.35f),
                                    0.50f to Color.Transparent,
                                    1.00f to Color.Transparent
                                )
                            )
                    )

                    // Subtle vertical edge vignettes for top and bottom framing
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.00f to Color(0xFF0B0D0F).copy(alpha = 0.35f),
                                    0.15f to Color.Transparent,
                                    0.85f to Color.Transparent,
                                    1.00f to Color(0xFF0B0D0F).copy(alpha = 0.35f)
                                )
                            )
                    )
                }

                // 2. Foreground Content: Metadata on the left, badges on the top right
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column (58%): Show Metadata, Time, Progress, & CTA Action Buttons
                    Column(
                        Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .padding(start = 28.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Channel Identity Line
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GoogleTvChannelLogo(channel, 26.dp, guide, shape = RoundedCornerShape(6.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${channel.number.ifBlank { "TV" }} · ${channel.name}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                if (isLive) {
                                    Surface(
                                        shape = CircleShape,
                                        color = GoogleTvTokens.LiveRed.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            "LIVE",
                                            Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = GoogleTvTokens.LiveRed
                                        )
                                    }
                                }
                                activeProg?.let { prog ->
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${formatHomeTime(prog.startMillis)} – ${formatHomeTime(prog.endMillis)}",
                                        color = GoogleTvTokens.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    remainingMinutes?.let {
                                        Text(
                                            " · $it min left",
                                            color = GoogleTvTokens.BlueAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Show Title
                            Text(
                                activeProg?.title ?: "Live Programming",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Show Description
                            Text(
                                activeProg?.description?.takeIf(String::isNotBlank) ?: "Tune in now to enjoy high-definition live programming on ${channel.name}.",
                                color = GoogleTvTokens.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 15.sp
                            )

                            // Progress Bar
                            if (isLive) {
                                Spacer(Modifier.height(1.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = GoogleTvTokens.BlueAccent,
                                    trackColor = Color.White.copy(alpha = 0.12f)
                                )
                            }
                        }

                        // Action Buttons Row (Material 3 Google TV focus physics)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Primary: Watch Live
                            var watchFocused by remember { mutableStateOf(false) }
                            Surface(
                                onClick = onWatch,
                                shape = CircleShape,
                                color = if (watchFocused) Color.White else GoogleTvTokens.BlueAccent,
                                contentColor = if (watchFocused) Color.Black else Color(0xFF001F2A),
                                modifier = Modifier.googleTvFocusable(
                                    shape = CircleShape,
                                    focusedScale = 1.05f,
                                    onFocusChange = { watchFocused = it }
                                )
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(15.dp))
                                    Text("WATCH LIVE", fontSize = 10.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                                }
                            }

                            // Secondary: Toggle Favorite
                            var favFocused by remember { mutableStateOf(false) }
                            Surface(
                                onClick = onToggleFavorite,
                                shape = CircleShape,
                                color = if (favFocused) Color.White else Color.White.copy(alpha = 0.10f),
                                contentColor = if (favFocused) Color.Black else if (isFavorite) GoogleTvTokens.StarGold else Color.White,
                                modifier = Modifier.googleTvFocusable(
                                    shape = CircleShape,
                                    focusedScale = 1.05f,
                                    onFocusChange = { favFocused = it }
                                )
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        Modifier.size(14.dp),
                                        tint = if (favFocused) Color.Black else if (isFavorite) GoogleTvTokens.StarGold else Color.White
                                    )
                                    Text(if (isFavorite) "FAVORITED" else "FAVORITE", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Tertiary: Open Live Guide
                            var guideFocused by remember { mutableStateOf(false) }
                            Surface(
                                onClick = onOpenGuide,
                                shape = CircleShape,
                                color = if (guideFocused) Color.White else Color.White.copy(alpha = 0.10f),
                                contentColor = if (guideFocused) Color.Black else Color.White,
                                modifier = Modifier.googleTvFocusable(
                                    shape = CircleShape,
                                    focusedScale = 1.05f,
                                    onFocusChange = { guideFocused = it }
                                )
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, Modifier.size(14.dp))
                                    Text("LIVE GUIDE", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Right Section (42%): Badges in top-right corner
                    Box(
                        Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        Row(
                            Modifier.align(Alignment.TopEnd),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    "1080p HD",
                                    Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.90f)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = GoogleTvTokens.LiveRed.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Text(
                                        "LIVE",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Cold-boot Hospitality Welcome Billboard (Only when no channel is tuned)
                if (!experience.heroImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = experience.heroImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Scrim gradients for guaranteed text contrast
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0f to Color(0xF50B0D0F),
                                    0.45f to Color(0xD90B0D0F),
                                    0.85f to Color(0x660B0D0F),
                                    1f to Color(0x330B0D0F)
                                )
                            )
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF141720),
                                        Color(0xFF0F1217),
                                        Color(0xFF0A0C0F)
                                    )
                                )
                            )
                    )
                }
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier
                            .weight(0.64f)
                            .fillMaxHeight()
                            .padding(start = 28.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = GoogleTvTokens.BlueAccent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    "GLZ TV SMART HUB · IN-ROOM ENTERTAINMENT",
                                    Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoogleTvTokens.BlueAccent,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Text(
                                experience.propertyName.ifBlank { "Welcome to Your Stay" },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Text(
                                "Connecting live cable channels and interactive program guide. You can also explore streaming apps, local weather, and radio stations.",
                                fontSize = 11.5.sp,
                                color = GoogleTvTokens.TextSecondary,
                                maxLines = 2,
                                lineHeight = 16.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            var guideFocused by remember { mutableStateOf(false) }
                            Surface(
                                onClick = onOpenGuide,
                                shape = CircleShape,
                                color = if (guideFocused) Color.White else GoogleTvTokens.BlueAccent,
                                contentColor = if (guideFocused) Color.Black else Color(0xFF001F2A),
                                modifier = Modifier.googleTvFocusable(shape = CircleShape, focusedScale = 1.05f, onFocusChange = { guideFocused = it })
                            ) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.LiveTv, contentDescription = null, Modifier.size(14.dp))
                                    Text("OPEN LIVE TV", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            var radioFocused by remember { mutableStateOf(false) }
                            Surface(
                                onClick = onOpenRadio,
                                shape = CircleShape,
                                color = if (radioFocused) Color.White else Color.White.copy(alpha = 0.10f),
                                contentColor = if (radioFocused) Color.Black else Color.White,
                                modifier = Modifier.googleTvFocusable(shape = CircleShape, focusedScale = 1.05f, onFocusChange = { radioFocused = it })
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Default.Radio, contentDescription = null, Modifier.size(14.dp))
                                    Text("GLZ RADIO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            var weatherFocused by remember { mutableStateOf(false) }
                            Surface(
                                onClick = onOpenWeather,
                                shape = CircleShape,
                                color = if (weatherFocused) Color.White else Color.White.copy(alpha = 0.10f),
                                contentColor = if (weatherFocused) Color.Black else Color.White,
                                modifier = Modifier.googleTvFocusable(shape = CircleShape, focusedScale = 1.05f, onFocusChange = { weatherFocused = it })
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Default.WbSunny, contentDescription = null, Modifier.size(14.dp))
                                    Text("WEATHER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Right 1/3 Guest Hub Card
                    Surface(
                        modifier = Modifier
                            .weight(0.36f)
                            .fillMaxHeight(),
                        shape = RectangleShape,
                        color = Color.White.copy(alpha = 0.05f)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Column(
                                Modifier.fillMaxSize().padding(14.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Hotel, contentDescription = null, Modifier.size(32.dp), tint = GoogleTvTokens.BlueAccent)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    experience.roomNumber?.let { "Room $it" } ?: "Guest Hub",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text("Fast Wi-Fi Ready", fontSize = 10.5.sp, color = GoogleTvTokens.TextSecondary)
                            }

                            // Gradient blend on last 1/3
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            0f to Color(0xFF0E1117).copy(alpha = 0.50f),
                                            0.20f to Color.Transparent,
                                            0.67f to Color.Transparent,
                                            1f to Color(0xFF0E1117).copy(alpha = 0.85f)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cinematic Google TV Showcase Artwork Card.
 * With smooth gradient blend on the last 1/3 of the card stream.
 */
@Composable
private fun CinematicShowcaseCard(
    channel: Channel,
    guide: EpgGuide,
    activeProgTitle: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RectangleShape,
        color = Color(0xFF131720)
    ) {
        Box(Modifier.fillMaxSize()) {
            // Base visual container
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E2330),
                                Color(0xFF151922),
                                Color(0xFF0F1218)
                            )
                        )
                    )
            )

            // Content
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Channel Logo
                GoogleTvChannelLogo(
                    channel = channel,
                    size = 48.dp,
                    guide = guide,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Bottom Callout
                Text(
                    text = activeProgTitle ?: channel.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Rail Section Header
 */
@Composable
private fun RailHeader(
    title: String,
    icon: ImageVector,
    iconTint: Color = GoogleTvTokens.TextSecondary
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, Modifier.size(13.dp), tint = iconTint)
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
            color = GoogleTvTokens.TextSecondary
        )
    }
}

/**
 * Recent Channel Card (Rail 1)
 */
@Composable
private fun RecentChannelCard(
    channel: Channel,
    guide: EpgGuide,
    now: Long,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val prog = remember(channel.id, guide, now) {
        guide.nowProgrammeForChannel(channel, now)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(190.dp)
            .height(80.dp)
            .googleTvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.04f,
                onFocusChange = {
                    isFocused = it
                    if (it) onFocus()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) Color.White else GoogleTvTokens.SurfaceCard,
        contentColor = if (isFocused) Color.Black else Color.White
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoogleTvChannelLogo(channel, 30.dp, guide, shape = RoundedCornerShape(8.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        channel.number.ifBlank { "CH" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isFocused) Color.Black else GoogleTvTokens.BlueAccent
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        channel.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    prog?.title ?: "Live Broadcast",
                    fontSize = 10.5.sp,
                    color = if (isFocused) Color.Black.copy(alpha = 0.70f) else GoogleTvTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 16:9 Live Show Highlight Card (Rail 2)
 */
@Composable
private fun LiveShowCard(
    channel: Channel,
    guide: EpgGuide,
    now: Long,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val prog = remember(channel.id, guide, now) {
        guide.nowProgrammeForChannel(channel, now)
    }
    val progress = prog?.let {
        if (it.startMillis <= now && it.endMillis > now) {
            ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L)).coerceIn(0f, 1f)
        } else 0f
    } ?: 0f

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(210.dp)
            .height(98.dp)
            .googleTvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.04f,
                onFocusChange = {
                    isFocused = it
                    if (it) onFocus()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) Color.White else GoogleTvTokens.SurfaceCard,
        contentColor = if (isFocused) Color.Black else Color.White
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoogleTvChannelLogo(channel, 22.dp, guide, shape = RoundedCornerShape(6.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    channel.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = CircleShape,
                    color = if (isFocused) GoogleTvTokens.LiveRed else GoogleTvTokens.LiveRed.copy(alpha = 0.22f)
                ) {
                    Text(
                        "LIVE",
                        Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isFocused) Color.White else GoogleTvTokens.LiveRed
                    )
                }
            }

            Column {
                Text(
                    prog?.title ?: "Live Programming",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                prog?.let {
                    Text(
                        "${formatHomeTime(it.startMillis)} – ${formatHomeTime(it.endMillis)}",
                        fontSize = 9.5.sp,
                        color = if (isFocused) Color.Black.copy(alpha = 0.65f) else GoogleTvTokens.TextSecondary
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isFocused) Color.Black else GoogleTvTokens.BlueAccent,
                trackColor = if (isFocused) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.10f)
            )
        }
    }
}

/**
 * Radio Station Card (Rail 3 - Horizontal Carousel)
 */
@Composable
private fun RadioStationCard(
    station: RadioStation,
    nowPlaying: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(185.dp)
            .height(78.dp)
            .googleTvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.04f,
                onFocusChange = { isFocused = it }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) Color.White else GoogleTvTokens.SurfaceCard,
        contentColor = if (isFocused) Color.Black else Color.White
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isFocused) Color.Black.copy(alpha = 0.08f) else Color(0xFFFF6B2C).copy(alpha = 0.15f)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (!station.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = station.logoUrl,
                            contentDescription = station.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Radio,
                            contentDescription = null,
                            Modifier.size(18.dp),
                            tint = if (isFocused) Color.Black else Color(0xFFFF6B2C)
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = station.genre.ifBlank { "Live Radio" },
                    fontSize = 10.sp,
                    color = if (isFocused) Color.Black.copy(alpha = 0.70f) else GoogleTvTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (nowPlaying) {
                Surface(
                    shape = CircleShape,
                    color = if (isFocused) Color.Black else GoogleTvTokens.BlueAccent
                ) {
                    Box(Modifier.size(6.dp))
                }
            }
        }
    }
}

/**
 * Favorite / Popular Channel Card (Rail 4)
 */
@Composable
private fun FavoriteChannelCard(
    channel: Channel,
    guide: EpgGuide,
    now: Long,
    isStar: Boolean = true,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val prog = remember(channel.id, guide, now) {
        guide.nowProgrammeForChannel(channel, now)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(78.dp)
            .googleTvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.04f,
                onFocusChange = {
                    isFocused = it
                    if (it) onFocus()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) Color.White else GoogleTvTokens.SurfaceCard,
        contentColor = if (isFocused) Color.Black else Color.White
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoogleTvChannelLogo(channel, 28.dp, guide, shape = RoundedCornerShape(6.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        channel.name,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isStar) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            Modifier.size(11.dp),
                            tint = if (isFocused) Color.Black else GoogleTvTokens.StarGold
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    prog?.title ?: "Live Programming",
                    fontSize = 10.sp,
                    color = if (isFocused) Color.Black.copy(alpha = 0.70f) else GoogleTvTokens.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Service Shortcut Card (Rail 6)
 */
@Composable
private fun ServiceShortcutCard(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(105.dp)
            .height(72.dp)
            .googleTvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.05f,
                onFocusChange = { isFocused = it }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) Color.White else GoogleTvTokens.SurfaceCard,
        contentColor = if (isFocused) Color.Black else Color.White
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                Modifier.size(18.dp),
                tint = if (isFocused) Color.Black else color
            )
            Spacer(Modifier.height(5.dp))
            Text(
                label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Pulsing skeleton card for cold-boot loading state.
 */
@Composable
private fun SkeletonCard(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = alpha)
    ) {
        Box(Modifier.fillMaxSize())
    }
}
