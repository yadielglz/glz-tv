package com.glztv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.GuestExperience
import com.glztv.app.model.EntertainmentApp
import com.glztv.app.model.NetworkInfo
import com.glztv.app.model.WeatherInfo
import com.glztv.app.ui.components.GoogleTvChannelLogo
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val homeTimeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun formatHomeTime(millis: Long): String = synchronized(homeTimeFormatter) {
    homeTimeFormatter.format(Date(millis))
}

/**
 * Authentic Google TV / Material 3 Home Experience.
 *
 * Features:
 * 1. Top Ambient Status Bar: Welcome chip, Live Clock, Weather pill, Network status.
 * 2. Hero Spotlight Billboard: Dynamic preview, show synopsis, live countdown, and primary action buttons.
 * 3. Horizontal Content Rails:
 *    - Jump Back In / Recent Channels (fast resume)
 *    - On Now · Live Highlights (16:9 show cards with live progress bars)
 *    - Favorite Channels (quick starred lineup)
 *    - Browse by Genre (category filter pills and matching channels)
 *    - Apps & Services (installed streaming apps, Radio, Weather, Settings)
 */
@Composable
fun GoogleTvHomeScreen(
    channels: List<Channel>,
    guide: EpgGuide,
    resumeChannel: Channel?,
    previewChannel: Channel?,
    favorites: Set<String>,
    recentChannels: List<Channel>,
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
    onLaunchApp: (EntertainmentApp) -> Unit,
    sportsBarKioskEnabled: Boolean = false,
    onStartSportsBarKiosk: () -> Unit = {},
    previewContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    val defaultChannel = resumeChannel ?: previewChannel ?: channels.firstOrNull()
    var focusedChannel by remember(defaultChannel?.id) { mutableStateOf(defaultChannel) }
    var selectedGenre by remember { mutableStateOf("ALL") }

    val activeChannel = focusedChannel ?: defaultChannel

    val effectiveRecent = remember(recentChannels, resumeChannel) {
        val list = mutableListOf<Channel>()
        resumeChannel?.let { list.add(it) }
        list.addAll(recentChannels.filter { it.id != resumeChannel?.id })
        list.distinctBy { it.id }.take(10)
    }
    val liveChannels = remember(channels) {
        channels.take(15)
    }
    val favoriteChannels = remember(channels, favorites) {
        channels.filter { it.id in favorites }
    }
    val availableGenres = remember(channels) {
        val genres = channels.mapNotNull { it.group.takeIf(String::isNotBlank) }.distinct()
        listOf("ALL") + genres.take(8)
    }
    val genreFilteredChannels = remember(channels, selectedGenre) {
        if (selectedGenre == "ALL") channels.take(10)
        else channels.filter { it.group.equals(selectedGenre, ignoreCase = true) }.take(10)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GoogleTvTokens.BackgroundDark),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. TOP AMBIENT STATUS ROW
        item(key = "home-top-bar") {
            GoogleTvHomeTopBar(
                guestName = guestName,
                experience = experience,
                now = now,
                weather = weather,
                networkInfo = networkInfo,
                onOpenWeather = onOpenWeather
            )
        }

        // 2. HERO SPOTLIGHT BILLBOARD
        activeChannel?.let { ch ->
            item(key = "home-hero-billboard") {
                GoogleTvSpotlightBillboard(
                    channel = ch,
                    guide = guide,
                    now = now,
                    isFavorite = ch.id in favorites,
                    onWatch = { onWatchChannel(ch) },
                    onToggleFavorite = { onToggleFavorite(ch.id) },
                    onOpenGuide = onOpenGuide,
                    previewContent = previewContent
                )
            }
        }

        // 3. RAIL 1: JUMP BACK IN / RECENT CHANNELS
        if (effectiveRecent.isNotEmpty()) {
            item(key = "rail-recent") {
                RailHeader(title = "JUMP BACK IN", icon = Icons.Default.History)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(effectiveRecent, key = { "recent-${it.id}" }) { ch ->
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

        // 4. RAIL 2: ON NOW · LIVE HIGHLIGHTS
        if (liveChannels.isNotEmpty()) {
            item(key = "rail-live-highlights") {
                RailHeader(title = "ON NOW · LIVE HIGHLIGHTS", icon = Icons.Default.LiveTv)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(liveChannels, key = { "live-highlight-${it.id}" }) { ch ->
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

        // 5. RAIL 3: FAVORITES (IF POPULATED)
        if (favoriteChannels.isNotEmpty()) {
            item(key = "rail-favorites") {
                RailHeader(title = "FAVORITE CHANNELS", icon = Icons.Default.Star, iconTint = GoogleTvTokens.StarGold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(favoriteChannels, key = { "fav-${it.id}" }) { ch ->
                        FavoriteChannelCard(
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

        // 6. RAIL 4: BROWSE BY GENRE
        if (availableGenres.size > 1) {
            item(key = "rail-genres") {
                RailHeader(title = "BROWSE BY GENRE", icon = Icons.Default.CalendarMonth)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(availableGenres) { genre ->
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
                    items(genreFilteredChannels, key = { "genre-ch-${it.id}" }) { ch ->
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

        // 7. RAIL 5: APPS & SERVICES
        item(key = "rail-apps") {
            RailHeader(title = "APPS & SERVICES", icon = Icons.Default.Apps)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // Streaming & Entertainment Apps
                items(entertainmentApps, key = { "app-${it.packageName}" }) { app ->
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

                // Radio Service
                item {
                    ServiceShortcutCard(
                        label = "GLZ Radio",
                        icon = Icons.Default.Radio,
                        color = Color(0xFFFF6B2C),
                        onClick = onOpenRadio
                    )
                }

                // Weather Service
                item {
                    ServiceShortcutCard(
                        label = "Weather",
                        icon = Icons.Default.WbSunny,
                        color = Color(0xFF29B6F6),
                        onClick = onOpenWeather
                    )
                }

                // Sports Bar Kiosk (if enabled)
                if (sportsBarKioskEnabled) {
                    item {
                        ServiceShortcutCard(
                            label = "Sports Bar",
                            icon = Icons.Default.Sports,
                            color = Color(0xFF00C853),
                            onClick = onStartSportsBarKiosk
                        )
                    }
                }

                // Settings Service
                item {
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
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Top Ambient Header Bar with Welcome greeting, Clock, Weather, and Network status.
 */
@Composable
private fun GoogleTvHomeTopBar(
    guestName: String,
    experience: GuestExperience,
    now: Long,
    weather: WeatherInfo?,
    networkInfo: NetworkInfo?,
    onOpenWeather: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Profile Greeting Pill
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.08f)
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    Modifier.size(16.dp),
                    tint = GoogleTvTokens.BlueAccent
                )
                Text(
                    text = buildString {
                        append("Welcome, ")
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Right: Ambient Glance Status (Clock, Weather, Network)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            weather?.let { w ->
                Surface(
                    onClick = onOpenWeather,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.googleTvFocusable(shape = CircleShape, focusedScale = 1.05f)
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
                            tint = Color(0xFFFFB300)
                        )
                        Text(
                            "${w.temperature}°F · ${w.location}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Hero Spotlight Billboard.
 * Displays the currently highlighted channel, live synopsis, countdown, and action buttons.
 */
@Composable
private fun GoogleTvSpotlightBillboard(
    channel: Channel,
    guide: EpgGuide,
    now: Long,
    isFavorite: Boolean,
    onWatch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenGuide: () -> Unit,
    previewContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProg = remember(channel.id, guide, now) {
        guide.nowProgrammeForChannel(channel, now)
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GoogleTvTokens.SurfaceCardTranslucent
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Show Metadata, Time, Progress, & CTA Action Buttons
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Channel Identity Line
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoogleTvChannelLogo(channel, 30.dp, guide, shape = RoundedCornerShape(8.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${channel.number.ifBlank { "TV" }} · ${channel.name}",
                        color = Color.White,
                        fontSize = 13.sp,
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
                                Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                fontSize = 9.sp,
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
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Show Description
                Text(
                    activeProg?.description?.takeIf(String::isNotBlank) ?: "Tune in now to enjoy live streaming on ${channel.name}.",
                    color = GoogleTvTokens.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                // Progress Bar
                if (isLive) {
                    Spacer(Modifier.height(2.dp))
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

                Spacer(Modifier.height(6.dp))

                // Action Buttons Row (Material 3 Google TV focus physics)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(16.dp))
                            Text("WATCH LIVE", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
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
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                Modifier.size(15.dp),
                                tint = if (favFocused) Color.Black else if (isFavorite) GoogleTvTokens.StarGold else Color.White
                            )
                            Text(if (isFavorite) "FAVORITED" else "FAVORITE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, Modifier.size(15.dp))
                            Text("LIVE GUIDE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.width(18.dp))

            // Right Column: Live Stream PiP Window
            Surface(
                modifier = Modifier
                    .width(210.dp)
                    .height(118.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black
            ) {
                previewContent()
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
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, Modifier.size(14.dp), tint = iconTint)
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
            .width(200.dp)
            .height(86.dp)
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
            GoogleTvChannelLogo(channel, 32.dp, guide, shape = RoundedCornerShape(8.dp))
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
                    fontSize = 11.sp,
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
            .width(220.dp)
            .height(104.dp)
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
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoogleTvChannelLogo(channel, 24.dp, guide, shape = RoundedCornerShape(6.dp))
                Spacer(Modifier.width(8.dp))
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                prog?.let {
                    Text(
                        "${formatHomeTime(it.startMillis)} – ${formatHomeTime(it.endMillis)}",
                        fontSize = 10.sp,
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
 * Favorite Channel Card (Rail 3)
 */
@Composable
private fun FavoriteChannelCard(
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
            .width(180.dp)
            .height(82.dp)
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
            GoogleTvChannelLogo(channel, 30.dp, guide, shape = RoundedCornerShape(6.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        channel.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        Modifier.size(12.dp),
                        tint = GoogleTvTokens.StarGold
                    )
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
 * Service Shortcut Card (Rail 5)
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
            Icon(
                icon,
                contentDescription = null,
                Modifier.size(18.dp),
                tint = if (isFocused) Color.Black else color
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
