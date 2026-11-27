package com.glztv.app.ui.mobile

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.BuildConfig
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.Movie
import com.glztv.app.R
import com.glztv.app.RadioStation
import com.glztv.app.model.WeatherInfo
import com.glztv.app.player.PlaybackControlState
import com.glztv.app.ui.components.DtvWeatherIcon
import com.glztv.app.ui.components.GoogleTvChannelLogo
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.formatDtvCompactTime
import kotlinx.coroutines.delay

enum class MobileTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Guide("Guide", Icons.Default.LiveTv),
    Movies("Movies", Icons.Default.Movie),
    Radio("Radio", Icons.Default.Radio),
    More("More", Icons.Default.Menu)
}

/**
 * Portrait-first Mobile Shell for GLZ TV Mobile (v1.48.5.30-mobile).
 * Keeps all essentials: Home (Live Events, Favorites, Recents), Live TV Guide + Search,
 * Movies, Radio (+ mini-player), Weather, Sleep Timer, Sync Refresh, and Settings.
 */
@Composable
fun GlzMobileRoot(
    channels: List<Channel>,
    guide: EpgGuide,
    movies: List<Movie>,
    radioStations: List<RadioStation>,
    activeRadioStation: RadioStation?,
    radioPlaying: Boolean,
    favorites: Set<String>,
    recentChannels: List<Channel>,
    weather: WeatherInfo?,
    loading: Boolean,
    hasUpdate: Boolean,
    captionsEnabled: Boolean,
    sleepTimerRemainingMinutes: Int?,
    activeChannel: Channel?,
    playerActive: Boolean,
    selectedMovie: Movie?,
    onWatchChannel: (Channel) -> Unit,
    onPreviousChannel: () -> Unit,
    onExitLivePlayer: () -> Unit,
    onSelectMovie: (Movie) -> Unit,
    onExitMoviePlayer: () -> Unit,
    onPlayRadio: (RadioStation) -> Unit,
    onToggleRadioPlayPause: () -> Unit,
    onStopRadio: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleCaptions: () -> Unit,
    onCycleSleepTimer: () -> Unit,
    onRefreshSources: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUpdate: () -> Unit,
    videoPlayerSlot: @Composable (Channel, PlaybackControlState, Modifier) -> Unit,
    moviePlayerSlot: @Composable (Movie, PlaybackControlState, Modifier) -> Unit
) {
    // Active Touch Players take over the full screen
    if (selectedMovie != null) {
        MobileMoviePlayerScreen(
            movie = selectedMovie,
            captionsEnabled = captionsEnabled,
            sleepTimerRemainingMinutes = sleepTimerRemainingMinutes,
            onToggleCaptions = onToggleCaptions,
            onCycleSleepTimer = onCycleSleepTimer,
            onExit = onExitMoviePlayer,
            moviePlayerSlot = moviePlayerSlot
        )
        return
    }

    if (playerActive && activeChannel != null) {
        MobileLivePlayerScreen(
            channel = activeChannel,
            channels = channels,
            guide = guide,
            favorites = favorites,
            captionsEnabled = captionsEnabled,
            sleepTimerRemainingMinutes = sleepTimerRemainingMinutes,
            onToggleFavorite = onToggleFavorite,
            onToggleCaptions = onToggleCaptions,
            onCycleSleepTimer = onCycleSleepTimer,
            onPreviousChannel = onPreviousChannel,
            onTune = onWatchChannel,
            onExit = onExitLivePlayer,
            videoPlayerSlot = videoPlayerSlot
        )
        return
    }

    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MobileTab.Home) }
    var guideCategoryFilter by remember { mutableStateOf("All") }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    BackHandler {
        if (currentTab != MobileTab.Home) {
            currentTab = MobileTab.Home
        } else {
            (context as? Activity)?.finishAndRemoveTask()
        }
    }

    val visibleTabs = remember(movies.isNotEmpty()) {
        if (movies.isNotEmpty()) MobileTab.entries
        else MobileTab.entries.filter { it != MobileTab.Movies }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D12))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Mobile Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1118))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.glztv_logo_horizontal),
                contentDescription = "GLZ TV",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(24.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(DtvChrome.ElectricBlue.copy(alpha = 0.2f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "MOBILE",
                    color = DtvChrome.ElectricBlueSoft,
                    fontSize = 9.sp,
                    fontFamily = BarlowCondensedFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.weight(1f))
            if (weather != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DtvWeatherIcon(weatherCode = weather.weatherCode, size = 18.dp)
                    Text(
                        text = "${weather.temperature}°",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (hasUpdate) {
                IconButton(onClick = onOpenUpdate, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = DtvChrome.ElectricBlueSoft,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = DtvChrome.SecondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(color = DtvChrome.Divider)

        // Main Portrait Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (currentTab) {
                MobileTab.Home -> MobileHomeTab(
                    channels = channels,
                    guide = guide,
                    favorites = favorites,
                    recentChannels = recentChannels,
                    now = now,
                    loading = loading,
                    onWatchChannel = {
                        onStopRadio()
                        onWatchChannel(it)
                    },
                    onSelectCategory = { cat ->
                        guideCategoryFilter = cat
                        currentTab = MobileTab.Guide
                    }
                )
                MobileTab.Guide -> MobileGuideTab(
                    channels = channels,
                    guide = guide,
                    favorites = favorites,
                    now = now,
                    initialCategory = guideCategoryFilter,
                    onCategoryChanged = { guideCategoryFilter = it },
                    onToggleFavorite = onToggleFavorite,
                    onWatchChannel = {
                        onStopRadio()
                        onWatchChannel(it)
                    }
                )
                MobileTab.Movies -> MobileMoviesTab(
                    movies = movies,
                    onSelectMovie = {
                        onStopRadio()
                        onSelectMovie(it)
                    }
                )
                MobileTab.Radio -> MobileRadioTab(
                    stations = radioStations,
                    activeStation = activeRadioStation,
                    radioPlaying = radioPlaying,
                    onPlayStation = onPlayRadio,
                    onTogglePlayPause = onToggleRadioPlayPause
                )
                MobileTab.More -> MobileMoreTab(
                    weather = weather,
                    channelCount = channels.size,
                    movieCount = movies.size,
                    radioCount = radioStations.size,
                    loading = loading,
                    hasUpdate = hasUpdate,
                    sleepTimerRemainingMinutes = sleepTimerRemainingMinutes,
                    onCycleSleepTimer = onCycleSleepTimer,
                    onRefreshSources = onRefreshSources,
                    onOpenSettings = onOpenSettings,
                    onOpenUpdate = onOpenUpdate
                )
            }
        }

        // Persistent Radio Mini-Player Bar
        if (activeRadioStation != null && radioPlaying) {
            HorizontalDivider(color = DtvChrome.Divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131B26))
                    .clickable { currentTab = MobileTab.Radio }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Radio,
                    contentDescription = null,
                    tint = DtvChrome.ElectricBlueSoft,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeRadioStation.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeRadioStation.genre.ifBlank { "Live Radio" },
                        color = DtvChrome.SecondaryText,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onToggleRadioPlayPause, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (radioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onStopRadio, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop Radio",
                        tint = DtvChrome.SecondaryText
                    )
                }
            }
        }

        // Portrait Bottom Navigation Bar
        HorizontalDivider(color = DtvChrome.Divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1118))
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleTabs.forEach { tab ->
                val selected = tab == currentTab
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { currentTab = tab }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) DtvChrome.ElectricBlueSoft else DtvChrome.MutedText,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = tab.label,
                        color = if (selected) DtvChrome.ElectricBlueSoft else DtvChrome.MutedText,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileHomeTab(
    channels: List<Channel>,
    guide: EpgGuide,
    favorites: Set<String>,
    recentChannels: List<Channel>,
    now: Long,
    loading: Boolean,
    onWatchChannel: (Channel) -> Unit,
    onSelectCategory: (String) -> Unit
) {
    val liveEvents = remember(channels) {
        channels.filter { ch ->
            ch.isLiveEvent || ch.group.contains("Live Event", ignoreCase = true) ||
                ch.group.contains("PPV", ignoreCase = true)
        }
    }
    val favoriteChannels = remember(channels, favorites) {
        channels.filter { it.id in favorites }
    }
    val heroChannel = remember(liveEvents, recentChannels, channels) {
        recentChannels.firstOrNull() ?: liveEvents.firstOrNull() ?: channels.firstOrNull()
    }
    val categories = remember(channels) {
        channels.map { it.group.ifBlank { "Live" } }.distinct().take(12)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (heroChannel != null) {
            item {
                val heroProg = remember(heroChannel.id, guide, now) {
                    guide.nowProgrammeForChannel(heroChannel, now)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF15263B), Color(0xFF0E141E))
                            )
                        )
                        .border(1.dp, DtvChrome.ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onWatchChannel(heroChannel) }
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GoogleTvChannelLogo(
                                channel = heroChannel,
                                size = 46.dp,
                                guide = guide,
                                shape = RoundedCornerShape(6.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (recentChannels.isNotEmpty()) "CONTINUE WATCHING" else "FEATURED LIVE",
                                    color = DtvChrome.ElectricBlueSoft,
                                    fontSize = 10.sp,
                                    fontFamily = BarlowCondensedFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${heroChannel.number.ifBlank { "" }} ${heroChannel.name}".trim(),
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = heroProg?.title?.takeIf { it.isNotBlank() }
                                ?: heroChannel.eventTitle?.takeIf { it.isNotBlank() }
                                ?: "Tap to watch live",
                            color = DtvChrome.PrimaryText,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DtvChrome.ElectricBlue)
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Watch Live",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (loading) {
            item {
                Text(
                    text = "Syncing channels & guide…",
                    color = DtvChrome.SecondaryText,
                    fontSize = 14.sp
                )
            }
        }

        if (liveEvents.isNotEmpty()) {
            item {
                MobileChannelRail(
                    title = "Live Events",
                    channels = liveEvents.take(15),
                    guide = guide,
                    now = now,
                    onWatchChannel = onWatchChannel
                )
            }
        }

        if (favoriteChannels.isNotEmpty()) {
            item {
                MobileChannelRail(
                    title = "Favorites",
                    channels = favoriteChannels,
                    guide = guide,
                    now = now,
                    onWatchChannel = onWatchChannel
                )
            }
        }

        if (recentChannels.size > 1) {
            item {
                MobileChannelRail(
                    title = "Recently Watched",
                    channels = recentChannels,
                    guide = guide,
                    now = now,
                    onWatchChannel = onWatchChannel
                )
            }
        }

        if (categories.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "BROWSE CATEGORIES",
                        color = DtvChrome.SecondaryText,
                        fontSize = 12.sp,
                        fontFamily = BarlowCondensedFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF151B25))
                                    .border(1.dp, DtvChrome.Divider, RoundedCornerShape(8.dp))
                                    .clickable { onSelectCategory(cat) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileChannelRail(
    title: String,
    channels: List<Channel>,
    guide: EpgGuide,
    now: Long,
    onWatchChannel: (Channel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            color = DtvChrome.SecondaryText,
            fontSize = 12.sp,
            fontFamily = BarlowCondensedFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(channels, key = { index, ch -> "m-rail-$index-${ch.id}-${ch.streamUrl}" }) { _, ch ->
                val prog = remember(ch, guide, now) { guide.nowProgrammeForChannel(ch, now) }
                Column(
                    modifier = Modifier
                        .width(156.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF131923))
                        .border(1.dp, DtvChrome.Divider, RoundedCornerShape(10.dp))
                        .clickable { onWatchChannel(ch) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoogleTvChannelLogo(
                            channel = ch,
                            size = 34.dp,
                            guide = guide,
                            shape = RoundedCornerShape(4.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            if (ch.number.isNotBlank()) {
                                Text(
                                    text = ch.number,
                                    color = DtvChrome.ElectricBlueSoft,
                                    fontSize = 12.sp,
                                    fontFamily = BarlowCondensedFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = ch.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = prog?.title?.takeIf { it.isNotBlank() }
                            ?: ch.eventTitle?.takeIf { it.isNotBlank() }
                            ?: ch.group.ifBlank { "Live" },
                        color = DtvChrome.SecondaryText,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileGuideTab(
    channels: List<Channel>,
    guide: EpgGuide,
    favorites: Set<String>,
    now: Long,
    initialCategory: String,
    onCategoryChanged: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onWatchChannel: (Channel) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val categories = remember(channels) {
        listOf("All", "Favorites") + channels.map { it.group.ifBlank { "Live" } }.distinct()
    }
    val filtered = remember(channels, initialCategory, query, favorites, guide, now) {
        val q = query.trim().lowercase()
        channels.filter { ch ->
            val catOk = when (initialCategory) {
                "All" -> true
                "Favorites" -> ch.id in favorites
                else -> ch.group.ifBlank { "Live" } == initialCategory
            }
            if (!catOk) return@filter false
            if (q.isEmpty()) return@filter true
            val progTitle = guide.nowProgrammeForChannel(ch, now)?.title.orEmpty().lowercase()
            ch.name.lowercase().contains(q) ||
                ch.callSign.lowercase().contains(q) ||
                ch.number.lowercase().contains(q) ||
                ch.eventTitle.orEmpty().lowercase().contains(q) ||
                progTitle.contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Touch search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141A24))
                .border(1.dp, DtvChrome.Divider, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = DtvChrome.MutedText, modifier = Modifier.size(18.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search channels, numbers, or shows…", color = DtvChrome.MutedText, fontSize = 13.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(DtvChrome.ElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = DtvChrome.MutedText,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { query = "" }
                )
            }
        }

        // Category pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val active = cat == initialCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) DtvChrome.ElectricBlue else Color(0xFF151B25))
                        .border(
                            1.dp,
                            if (active) DtvChrome.ElectricBlue else DtvChrome.Divider,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onCategoryChanged(cat) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (active) Color.White else DtvChrome.SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filtered, key = { index, ch -> "m-guide-$index-${ch.id}-${ch.streamUrl}" }) { _, ch ->
                val prog = remember(ch, guide, now) { guide.nowProgrammeForChannel(ch, now) }
                val progress = prog?.let {
                    ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                        .coerceIn(0f, 1f)
                } ?: 0f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF121720))
                        .border(1.dp, DtvChrome.Divider, RoundedCornerShape(10.dp))
                        .clickable { onWatchChannel(ch) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoogleTvChannelLogo(
                        channel = ch,
                        size = 44.dp,
                        guide = guide,
                        shape = RoundedCornerShape(5.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (ch.number.isNotBlank()) {
                                Text(
                                    text = ch.number,
                                    color = DtvChrome.ElectricBlueSoft,
                                    fontSize = 14.sp,
                                    fontFamily = BarlowCondensedFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = ch.callSign.ifBlank { ch.name },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = prog?.title?.takeIf { it.isNotBlank() }
                                ?: ch.eventTitle?.takeIf { it.isNotBlank() }
                                ?: ch.group.ifBlank { "Live Broadcast" },
                            color = DtvChrome.PrimaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (prog != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${formatDtvCompactTime(prog.startMillis)} - ${formatDtvCompactTime(prog.endMillis)}",
                                    color = DtvChrome.MutedText,
                                    fontSize = 10.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.14f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxHeight()
                                            .background(DtvChrome.ElectricBlue)
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = { onToggleFavorite(ch.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        val fav = ch.id in favorites
                        Icon(
                            imageVector = if (fav) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (fav) Color(0xFFFACC15) else DtvChrome.MutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileMoviesTab(
    movies: List<Movie>,
    onSelectMovie: (Movie) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(movies, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) movies
        else movies.filter { it.title.lowercase().contains(q) || it.genres.any { g -> g.lowercase().contains(q) } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141A24))
                .border(1.dp, DtvChrome.Divider, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = DtvChrome.MutedText, modifier = Modifier.size(18.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search movies…", color = DtvChrome.MutedText, fontSize = 13.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(DtvChrome.ElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(filtered, key = { index, movie -> "m-movie-$index-${movie.id}" }) { _, movie ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectMovie(movie) },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF151C26))
                            .border(1.dp, DtvChrome.Divider, RoundedCornerShape(8.dp))
                    ) {
                        if (movie.posterUrl.isNotBlank()) {
                            AsyncImage(
                                model = movie.posterUrl,
                                contentDescription = movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Movie,
                                contentDescription = null,
                                tint = DtvChrome.MutedText,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                    Text(
                        text = movie.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileRadioTab(
    stations: List<RadioStation>,
    activeStation: RadioStation?,
    radioPlaying: Boolean,
    onPlayStation: (RadioStation) -> Unit,
    onTogglePlayPause: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(stations, key = { index, station -> "m-radio-$index-${station.code}-${station.streamUrl}" }) { _, station ->
            val isActive = activeStation?.code == station.code
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) Color(0xFF15263B) else Color(0xFF121720))
                    .border(
                        1.dp,
                        if (isActive) DtvChrome.ElectricBlue else DtvChrome.Divider,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        if (isActive) onTogglePlayPause() else onPlayStation(station)
                    }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B2330)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!station.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = station.logoUrl,
                            contentDescription = station.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = DtvChrome.ElectricBlueSoft)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = station.genre.ifBlank { "Live Radio" },
                        color = DtvChrome.SecondaryText,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isActive && radioPlaying) DtvChrome.ElectricBlue else Color(0xFF1E2633)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive && radioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileMoreTab(
    weather: WeatherInfo?,
    channelCount: Int,
    movieCount: Int,
    radioCount: Int,
    loading: Boolean,
    hasUpdate: Boolean,
    sleepTimerRemainingMinutes: Int?,
    onCycleSleepTimer: () -> Unit,
    onRefreshSources: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUpdate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (weather != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF121822))
                        .border(1.dp, DtvChrome.Divider, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DtvWeatherIcon(weatherCode = weather.weatherCode, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = weather.location.ifBlank { "Local Weather" },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Feels like ${weather.feelsLike}° · Humidity ${weather.humidity}% · Wind ${weather.windSpeedMph} mph",
                            color = DtvChrome.SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "${weather.temperature}°",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontFamily = BarlowCondensedFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            MobileActionRow(
                icon = Icons.Default.Bedtime,
                title = "Sleep Timer",
                subtitle = if (sleepTimerRemainingMinutes != null) "Active · ${sleepTimerRemainingMinutes} min remaining (tap to cycle)"
                else "Off · Tap to set 15m, 30m, 60m, or 90m",
                onClick = onCycleSleepTimer
            )
        }
        item {
            MobileActionRow(
                icon = Icons.Default.Refresh,
                title = if (loading) "Syncing Channels & Guide…" else "Refresh Channels & Guide",
                subtitle = "$channelCount channels · $movieCount movies · $radioCount radio stations",
                onClick = onRefreshSources
            )
        }
        item {
            MobileActionRow(
                icon = Icons.Default.Settings,
                title = "Settings & GlzHub Provisioning",
                subtitle = "Sources, account, captions, diagnostics",
                onClick = onOpenSettings
            )
        }
        if (hasUpdate) {
            item {
                MobileActionRow(
                    icon = Icons.Default.SystemUpdate,
                    title = "Mobile Update Available",
                    subtitle = "Tap to view and install the latest GLZ TV Mobile release",
                    accent = true,
                    onClick = onOpenUpdate
                )
            }
        }
        item {
            Text(
                text = "GLZ TV Mobile · v${BuildConfig.VERSION_NAME}",
                color = DtvChrome.MutedText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun MobileActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (accent) DtvChrome.ElectricBlue.copy(alpha = 0.16f) else Color(0xFF121720))
            .border(
                1.dp,
                if (accent) DtvChrome.ElectricBlue else DtvChrome.Divider,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (accent) DtvChrome.ElectricBlueSoft else DtvChrome.SecondaryText,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = DtvChrome.SecondaryText, fontSize = 12.sp)
        }
    }
}
