package com.glztv.app.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.GuestExperience
import com.glztv.app.Movie
import com.glztv.app.R
import com.glztv.app.RadioStation
import com.glztv.app.model.EntertainmentApp
import com.glztv.app.model.NetworkInfo
import com.glztv.app.model.WeatherInfo
import com.glztv.app.player.RadioPlaybackState
import com.glztv.app.ui.components.GoogleTvChannelLogo
import com.glztv.app.ui.components.HomeRadioPlayerBar
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvActionButton
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.DtvNavTab
import com.glztv.app.ui.theme.DtvProgressBar
import com.glztv.app.ui.theme.DtvTopCommandBar
import com.glztv.app.ui.theme.LocalTvViewport
import com.glztv.app.ui.theme.formatDtvCompactTime
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

private val HeroPreviewLeftFade = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFF0E1116),
        0.22f to Color(0xD90E1116),
        0.52f to Color(0x330E1116),
        1.00f to Color.Transparent
    )
)

/**
 * Non-Scrollable DirecTV HD Home Dashboard (v1.48.4.15).
 *
 * - Top Bar: "GlzTV" wordmark, navigation tabs (Home | Live TV | Guide | Movies | Radio | Apps | Weather),
 *   and borderless right-side Weather, Clock, Name, Settings.
 * - Hero Bar: Live TV preview bleeds across the right third (no redundant channel badge);
 *   when Radio is playing / no preview, Weather takes the right third. Left side shows channel/program,
 *   progress bar, and Watch Live + Apps + Favorite (zero clipping).
 * - Strong DirecTV Divider separating Hero Bar from the non-scrollable 2-Column Dashboard:
 *   - Left Column (44%): Live Sports & Events (up to 3 compact DirecTV rows + "More in Guide ▸")
 *     plus Weather Widget whenever there are fewer than 3 live events.
 *   - Right Column (56%): Compact 2x3 DirecTV My Channels grid (6 channels + "All N in Guide ▸").
 * - Bottom Edge: Flush DirecTV Radio Player Bar when Radio is active.
 */
@Composable
fun GoogleTvHomeScreen(
    channels: List<Channel>,
    guide: EpgGuide,
    sportsGames: List<com.glztv.app.data.SportsGame> = emptyList(),
    resumeChannel: Channel?,
    previewChannel: Channel?,
    favorites: Set<String>,
    recentChannels: List<Channel>,
    radioStations: List<RadioStation> = emptyList(),
    movies: List<Movie> = emptyList(),
    currentRadioStation: RadioStation? = null,
    onPlayRadioStation: (RadioStation) -> Unit = {},
    onWatchMovie: (Movie) -> Unit = {},
    entertainmentApps: List<EntertainmentApp>,
    guestName: String,
    experience: GuestExperience,
    weather: WeatherInfo?,
    networkInfo: NetworkInfo?,
    onWatchChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenGuide: () -> Unit,
    onOpenMovies: () -> Unit = {},
    onOpenRadio: () -> Unit,
    onOpenApps: () -> Unit = {},
    onOpenWeather: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWelcome: () -> Unit = {},
    onLaunchApp: (EntertainmentApp) -> Unit,
    sportsBarKioskEnabled: Boolean = false,
    onStartSportsBarKiosk: () -> Unit = {},
    previewContent: (@Composable () -> Unit)? = null,
    propertyVideoBackdrop: (@Composable () -> Unit)? = null,
    isFamilyChannel: Boolean = false,
    hasUpdate: Boolean = false,
    onOpenUpdate: () -> Unit = {},
    radioPlaying: Boolean = false,
    radioPlaybackState: RadioPlaybackState? = null,
    onToggleRadioPlayPause: () -> Unit = {},
    onStopRadio: () -> Unit = {},
    onOpenSpeedTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewport = LocalTvViewport.current
    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(24.dp)

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    val quickWatchChannel = remember(channels, previewChannel, resumeChannel) {
        previewChannel ?: resumeChannel ?: channels.firstOrNull()
    }
    val eventChannels = remember(channels, guide, sportsGames, now) {
        com.glztv.app.data.SportsChannelMatcher.filterValidLiveEvents(channels, guide, sportsGames, now)
    }
    var selectedLeague by remember { mutableStateOf("ALL") }
    val availableLeagues = remember(sportsGames) {
        val leagues = listOf("NBA", "NFL", "MLB", "NCAA").filter { lg -> sportsGames.any { it.league == lg } }
        listOf("ALL") + leagues
    }
    val filteredGames = remember(sportsGames, selectedLeague) {
        val base = if (selectedLeague == "ALL") sportsGames else sportsGames.filter { it.league == selectedLeague }
        base.filter { !it.isFinal }.ifEmpty { base }
    }
    val myChannels = remember(channels, favorites, recentChannels, resumeChannel) {
        buildList {
            resumeChannel?.let { add(it) }
            channels.filter { it.id in favorites }.forEach { add(it) }
            recentChannels.forEach { add(it) }
            channels.filterNot { it.isLiveEvent() }.take(12).forEach { add(it) }
        }.distinctBy { it.id }.take(6)
    }

    val watchLiveFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { watchLiveFocus.requestFocus() }
    }

    val isRadioBannerMode = radioPlaying || currentRadioStation != null
    val showPreviewVideo = !isRadioBannerMode && previewChannel != null && previewContent != null
    val heroHeight = if (viewport.isCompactHeight) 178.dp else 196.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DtvChrome.ScreenBg)
    ) {
        // ── 1. Shared DirecTV HD Top Bar ("GlzTV") ────────────────────────────────
        DtvTopCommandBar(
            activeTab = DtvNavTab.Home,
            nowMillis = now,
            weather = weather,
            guestLabel = guestName.ifBlank { experience.propertyName.ifBlank { "Guest" } },
            hasUpdate = hasUpdate,
            showMoviesTab = movies.isNotEmpty(),
            sportsBarKioskEnabled = sportsBarKioskEnabled,
            onSelectTab = { tab ->
                when (tab) {
                    DtvNavTab.Home -> runCatching { watchLiveFocus.requestFocus() }
                    DtvNavTab.LiveTv -> quickWatchChannel?.let(onWatchChannel) ?: onOpenGuide()
                    DtvNavTab.Guide -> onOpenGuide()
                    DtvNavTab.Movies -> onOpenMovies()
                    DtvNavTab.Radio -> onOpenRadio()
                    DtvNavTab.Apps -> onOpenApps()
                    DtvNavTab.Weather -> onOpenWeather()
                    DtvNavTab.SportsBar -> onStartSportsBarKiosk()
                }
            },
            onOpenUpdate = onOpenUpdate,
            onOpenWelcome = onOpenWelcome,
            onOpenSettings = onOpenSettings
        )

        // ── 2. DirecTV HD Hero Bar (GlzHub Preview Video OR Property Image / GlzTV Wallpaper + Bled Weather)
        val propertyBgUrl = experience.heroImageUrl?.takeIf { it.isNotBlank() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .clipToBounds()
                .background(DtvChrome.BannerGradient)
        ) {
            // Whenever Radio is active OR no GlzHub preview channel is set, show Property Video/Image (or GlzTV orbital wallpaper fallback)
            if (!showPreviewVideo) {
                val isVideoBackdrop = propertyVideoBackdrop != null
                if (isVideoBackdrop) {
                    Image(
                        painter = painterResource(id = R.drawable.glztv_splash_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    propertyVideoBackdrop?.invoke()
                } else if (propertyBgUrl != null) {
                    AsyncImage(
                        model = propertyBgUrl,
                        contentDescription = experience.propertyName.ifBlank { "Property" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.glztv_splash_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color(0xD90A0D12),
                                    0.45f to Color(0x8C0A0D12),
                                    0.72f to Color(0xCC0C1017),
                                    1.00f to Color(0xF20B0F16)
                                )
                            )
                        )
                )
            }

            // Right Third: GlzHub Live TV preview bleeding smoothly into Hero OR borderless Weather bled into Hero
            val activePreview = previewContent
            if (showPreviewVideo && activePreview != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .clipToBounds()
                ) {
                    activePreview()
                    Box(Modifier.fillMaxSize().background(HeroPreviewLeftFade))
                }
            } else if (weather != null) {
                HeroRightWeatherPanel(
                    weather = weather,
                    onClick = onOpenWeather,
                    modifier = Modifier
                        .fillMaxWidth(0.38f)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .padding(end = horizontalPad, top = 14.dp, bottom = 14.dp)
                )
            }

            // Left Two-Thirds: Radio Playing OR GlzHub Preview Channel OR Property Welcome
            val active = previewChannel
            val currentProg = remember(active?.id, guide, now) {
                active?.let { guide.nowProgrammeForChannel(it, now) }
            }
            val progProgress = currentProg?.let {
                ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                    .coerceIn(0f, 1f)
            } ?: 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .fillMaxHeight()
                    .padding(start = horizontalPad, top = 16.dp, bottom = 14.dp, end = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (isRadioBannerMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(DtvChrome.ElectricBlue)
                            )
                            Text(
                                text = "RADIO ON AIR",
                                color = DtvChrome.ElectricBlueSoft,
                                fontSize = 11.sp,
                                fontFamily = BarlowCondensedFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            )
                        }
                        Text(
                            text = "Radio Playing",
                            color = Color.White,
                            fontSize = if (viewport.isCompactHeight) 26.sp else 30.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        currentRadioStation?.let { st ->
                            val track = listOfNotNull(
                                radioPlaybackState?.icyArtist?.takeIf { it.isNotBlank() },
                                radioPlaybackState?.icyTitle?.takeIf { it.isNotBlank() }
                            ).joinToString(" — ").takeIf { it.isNotBlank() }
                            Text(
                                text = if (track != null) "${st.name}  ·  $track" else st.name,
                                color = DtvChrome.SecondaryText,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DtvActionButton(
                            label = "Open Radio",
                            primary = true,
                            modifier = Modifier.focusRequester(watchLiveFocus),
                            onClick = onOpenRadio
                        )
                        if (quickWatchChannel != null) {
                            DtvActionButton(
                                label = "Watch Live",
                                primary = false,
                                showPlayIcon = true,
                                onClick = { onWatchChannel(quickWatchChannel) }
                            )
                        }
                        DtvActionButton(
                            label = "Apps",
                            primary = false,
                            onClick = onOpenApps
                        )
                    }
                } else if (showPreviewVideo && active != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GoogleTvChannelLogo(
                                channel = active,
                                size = 30.dp,
                                guide = guide,
                                shape = RoundedCornerShape(3.dp)
                            )
                            Text(
                                text = active.number.ifBlank { "LIVE" },
                                color = DtvChrome.ElectricBlueSoft,
                                fontSize = 16.sp,
                                fontFamily = BarlowCondensedFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = active.name,
                                color = DtvChrome.SecondaryText,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0x2EEF4444), RoundedCornerShape(2.dp))
                                    .border(1.dp, Color(0x66EF4444), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "ON AIR",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.sp,
                                    fontFamily = BarlowCondensedFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.7.sp
                                )
                            }
                        }

                        val heroTitle = remember(active, guide, sportsGames, now) {
                            com.glztv.app.data.SportsChannelMatcher.resolveChannelShowTitle(
                                active, guide, sportsGames, now
                            ).ifBlank { active.name }
                        }
                        Text(
                            text = heroTitle,
                            color = Color.White,
                            fontSize = if (viewport.isCompactHeight) 22.sp else 25.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val timeRange = currentProg?.let {
                            "${formatDtvCompactTime(it.startMillis)} - ${formatDtvCompactTime(it.endMillis)}"
                        } ?: "Live Broadcast"
                        Text(
                            text = buildList {
                                add(timeRange)
                                if (active.group.isNotBlank()) add(active.group)
                            }.joinToString(", "),
                            color = DtvChrome.SecondaryText,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (currentProg != null) {
                            Spacer(Modifier.height(2.dp))
                            DtvProgressBar(
                                progress = progProgress,
                                modifier = Modifier.width(240.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DtvActionButton(
                            label = "Watch Live",
                            primary = true,
                            showPlayIcon = true,
                            modifier = Modifier.focusRequester(watchLiveFocus),
                            onClick = { onWatchChannel(active) }
                        )
                        DtvActionButton(
                            label = "Apps",
                            primary = false,
                            onClick = onOpenApps
                        )
                        DtvHomeFavToggle(
                            isFavorite = active.id in favorites,
                            onClick = { onToggleFavorite(active.id) }
                        )
                    }
                } else {
                    // No GlzHub preview channel set: Property Welcome over Property Image / GlzTV Wallpaper
                    val propertyEyebrow = experience.propertyName.ifBlank { "GlzTV" }.uppercase()
                    val welcomeHeadline = if (guestName.isNotBlank() && !guestName.equals("Guest", ignoreCase = true)) {
                        "Welcome, $guestName"
                    } else if (experience.propertyName.isNotBlank()) {
                        "Welcome to ${experience.propertyName}"
                    } else {
                        "Welcome to GlzTV"
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = propertyEyebrow,
                            color = DtvChrome.ElectricBlueSoft,
                            fontSize = 11.sp,
                            fontFamily = BarlowCondensedFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            maxLines = 1
                        )
                        Text(
                            text = welcomeHeadline,
                            color = Color.White,
                            fontSize = if (viewport.isCompactHeight) 24.sp else 28.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = experience.welcomeMessage.ifBlank { "Relax, explore, and enjoy live TV, Guide, Movies, Radio, and Apps." },
                            color = DtvChrome.SecondaryText,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DtvActionButton(
                            label = "Watch Live",
                            primary = true,
                            showPlayIcon = true,
                            modifier = Modifier.focusRequester(watchLiveFocus),
                            onClick = { quickWatchChannel?.let(onWatchChannel) ?: onOpenGuide() }
                        )
                        DtvActionButton(
                            label = "Apps",
                            primary = false,
                            onClick = onOpenApps
                        )
                    }
                }
            }
        }

        // ── Strong DirecTV Divider between Hero Bar and 2-Column Dashboard ────────
        Box(Modifier.fillMaxWidth().height(2.dp).background(DtvChrome.Divider))
        Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.ElectricBlue.copy(alpha = 0.35f)))

        // ── 3. Non-Scrollable 2-Column DirecTV Dashboard ──────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = horizontalPad, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // LEFT COLUMN (44%): Live Scoreboard (NBA · NFL · MLB · NCAA) with Direct Channel Tuning
            // Falls back to verified Live Events / Weather Widget when no scores are loaded.
            Column(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                val visibleGames = filteredGames.take(4)
                val visibleEvents = eventChannels.take(3)
                if (visibleGames.isNotEmpty()) {
                    val liveCount = filteredGames.count { it.isLive }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (liveCount > 0) DtvChrome.LiveRed else DtvChrome.ElectricBlue)
                            )
                            Text(
                                text = if (selectedLeague == "ALL") "LIVE SCOREBOARD" else "$selectedLeague SCOREBOARD",
                                color = DtvChrome.PrimaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.9.sp
                            )
                            if (liveCount > 0) {
                                Text(
                                    text = "$liveCount LIVE",
                                    color = DtvChrome.LiveRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (availableLeagues.size > 1) {
                            DtvMoreLinkChip(
                                label = "$selectedLeague ▾",
                                onClick = {
                                    val idx = availableLeagues.indexOf(selectedLeague).coerceAtLeast(0)
                                    selectedLeague = availableLeagues[(idx + 1) % availableLeagues.size]
                                }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleGames.forEach { game ->
                            val directChannel = remember(game.id, channels, guide, now) {
                                com.glztv.app.data.SportsChannelMatcher.findDirectChannel(game, channels, guide, now)
                            }
                            DtvScoreboardGameRow(
                                game = game,
                                directChannel = directChannel,
                                onClick = { directChannel?.let(onWatchChannel) ?: onOpenGuide() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                        if (visibleGames.size <= 2 && weather != null) {
                            DtvHomeWeatherWidget(
                                weather = weather,
                                compact = true,
                                onClick = onOpenWeather,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                } else {
                    if (visibleEvents.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(DtvChrome.LiveRed))
                                Text(
                                    text = "LIVE SPORTS & EVENTS",
                                    color = DtvChrome.PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.9.sp
                                )
                            }
                            if (eventChannels.size > 3) {
                                DtvMoreLinkChip(
                                    label = "+${eventChannels.size - 3} more ▸",
                                    onClick = onOpenGuide
                                )
                            } else {
                                Text(
                                    text = "${eventChannels.size} LIVE",
                                    color = DtvChrome.MutedText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            visibleEvents.forEach { eventCh ->
                                val prog = remember(eventCh.id, guide, now) {
                                    guide.nowProgrammeForChannel(eventCh, now)
                                }
                                val pct = prog?.let {
                                    ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                                        .coerceIn(0f, 1f)
                                } ?: 0.5f
                                DtvCompactEventRow(
                                    channel = eventCh,
                                    progress = pct,
                                    onClick = { onWatchChannel(eventCh) }
                                )
                            }
                        }
                    }

                    // Weather Widget in Left Column whenever there are 0, 1, or 2 live events
                    if (visibleEvents.size < 3 && weather != null) {
                        DtvHomeWeatherWidget(
                            weather = weather,
                            compact = visibleEvents.isNotEmpty(),
                            onClick = onOpenWeather,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }

            // Crisp Vertical DirecTV Divider between Left and Right columns
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(DtvChrome.Divider)
            )

            // RIGHT COLUMN (56%): Compact 2x3 DirecTV My Channels Grid + "All in Guide ▸"
            Column(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(Modifier.width(3.dp).height(11.dp).background(DtvChrome.ElectricBlue))
                        Text(
                            text = "MY CHANNELS",
                            color = DtvChrome.PrimaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.9.sp
                        )
                    }
                    DtvMoreLinkChip(
                        label = "All ${channels.size} in Guide ▸",
                        onClick = onOpenGuide
                    )
                }

                // 3 rows × 2 columns = 6 crisp compact DirecTV channel tiles (zero scrolling)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val rows = myChannels.chunked(2)
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { ch ->
                                val prog = remember(ch.id, guide, now) {
                                    guide.nowProgrammeForChannel(ch, now)
                                }
                                val enrichedTitle = remember(ch, guide, sportsGames, now) {
                                    com.glztv.app.data.SportsChannelMatcher.resolveChannelShowTitle(
                                        ch, guide, sportsGames, now
                                    )
                                }
                                val pct = prog?.let {
                                    ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
                                        .coerceIn(0f, 1f)
                                } ?: 0f
                                val timeLabel = prog?.let {
                                    "${formatDtvCompactTime(it.startMillis)} - ${formatDtvCompactTime(it.endMillis)}"
                                } ?: "Live"
                                DtvCompactChannelCard(
                                    channel = ch,
                                    programmeTitle = enrichedTitle,
                                    timeRange = timeLabel,
                                    progress = pct,
                                    isFavorite = ch.id in favorites,
                                    guide = guide,
                                    onClick = { onWatchChannel(ch) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // ── 4. Flush DirecTV Radio Player Bar (docked at bottom when Radio active)
        AnimatedVisibility(
            visible = currentRadioStation != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            HomeRadioPlayerBar(
                station = currentRadioStation,
                isPlaying = radioPlaying,
                playbackState = radioPlaybackState,
                onTogglePlayPause = onToggleRadioPlayPause,
                onStop = onStopRadio
            )
        }
    }
}

@Composable
private fun HeroRightWeatherPanel(
    weather: WeatherInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val today = weather.forecast.firstOrNull()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (focused) Color(0xCC000000) else Color.Transparent)
            .then(
                if (focused) Modifier.border(2.dp, DtvChrome.ElectricBlue, RoundedCornerShape(3.dp))
                else Modifier
            )
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "LOCAL WEATHER",
                color = DtvChrome.ElectricBlueSoft,
                fontSize = 11.sp,
                fontFamily = BarlowCondensedFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.glztv.app.ui.components.DtvWeatherIcon(
                    weatherCode = weather.weatherCode,
                    size = 40.dp,
                    contentDescription = "Weather"
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${weather.temperature}°",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = weather.location.ifBlank { "Current Location" },
                        color = DtvChrome.PrimaryText.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = buildList {
                    add("Feels ${weather.feelsLike}°")
                    if (today != null) add("H ${today.high}° / L ${today.low}°")
                    add("${weather.humidity}% hum")
                }.joinToString(" · "),
                color = DtvChrome.SecondaryText,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DtvScoreboardGameRow(
    game: com.glztv.app.data.SportsGame,
    directChannel: Channel?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val accentColor = when {
        isFocused -> DtvChrome.ElectricBlue
        game.isLive -> DtvChrome.LiveRed
        game.isUpcoming -> DtvChrome.ElectricBlueSoft
        else -> DtvChrome.MutedText
    }
    val awayScoreInt = game.awayTeam.score.toIntOrNull()
    val homeScoreInt = game.homeTeam.score.toIntOrNull()
    val awayLeading = awayScoreInt != null && homeScoreInt != null && awayScoreInt > homeScoreInt
    val homeLeading = awayScoreInt != null && homeScoreInt != null && homeScoreInt > awayScoreInt

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(3.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accentColor)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: League + Live Status/Clock on left, Direct Watch Channel or Network on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = game.subLeague,
                        color = DtvChrome.ElectricBlueSoft,
                        fontSize = 9.sp,
                        fontFamily = BarlowCondensedFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = if (game.isLive) "● ${game.statusDetail.ifBlank { "LIVE" }}"
                        else game.statusDetail.ifBlank { if (game.isFinal) "FINAL" else "UPCOMING" },
                        color = if (game.isLive) Color(0xFFF87171) else DtvChrome.MutedText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (directChannel != null) {
                    val chLabel = buildString {
                        append("▶ ")
                        if (directChannel.number.isNotBlank()) {
                            append("CH ").append(directChannel.number)
                            val shortNet = directChannel.callSign.ifBlank { directChannel.name }
                            if (shortNet.isNotBlank()) append(" · ").append(shortNet.take(10))
                        } else {
                            append(directChannel.callSign.ifBlank { directChannel.name }.take(12))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isFocused) DtvChrome.ElectricBlue else DtvChrome.ElectricBlue.copy(alpha = 0.22f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = chLabel,
                            color = if (isFocused) Color.White else DtvChrome.ElectricBlueSoft,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                } else if (game.primaryBroadcast.isNotBlank()) {
                    Text(
                        text = game.primaryBroadcast,
                        color = DtvChrome.MutedText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            // Two compact matchup rows (Away vs Home)
            DtvScoreboardTeamLine(
                team = game.awayTeam,
                showScore = !game.isUpcoming,
                isLeader = awayLeading || game.awayTeam.winner,
                isFocused = isFocused
            )
            DtvScoreboardTeamLine(
                team = game.homeTeam,
                showScore = !game.isUpcoming,
                isLeader = homeLeading || game.homeTeam.winner,
                isFocused = isFocused
            )
        }
    }
}

@Composable
private fun DtvScoreboardTeamLine(
    team: com.glztv.app.data.SportsTeam,
    showScore: Boolean,
    isLeader: Boolean,
    isFocused: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (team.logoUrl.isNotBlank()) {
            AsyncImage(
                model = team.logoUrl,
                contentDescription = team.shortName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = team.rankedShortName,
            color = when {
                isFocused -> Color.White
                isLeader -> Color.White
                else -> DtvChrome.SecondaryText
            },
            fontSize = 11.sp,
            fontWeight = if (isLeader || isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (showScore) team.score.ifBlank { "0" } else "—",
            color = when {
                !showScore -> DtvChrome.MutedText
                isLeader || isFocused -> Color.White
                else -> DtvChrome.SecondaryText
            },
            fontSize = 12.sp,
            fontFamily = BarlowCondensedFontFamily,
            fontWeight = if (isLeader) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun DtvCompactEventRow(
    channel: Channel,
    progress: Float,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val league = channel.eventLeague?.takeIf { it.isNotBlank() } ?: "LIVE"
    val title = channel.eventTitle?.takeIf { it.isNotBlank() } ?: channel.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(3.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (isFocused) DtvChrome.ElectricBlue else DtvChrome.LiveRed)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = league.uppercase(Locale.getDefault()),
                    color = DtvChrome.ElectricBlueSoft,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = "● LIVE · ${channel.number.ifBlank { "PPV" }}",
                    color = Color(0xFFF87171),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = title,
                color = if (isFocused) Color.White else DtvChrome.PrimaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            DtvProgressBar(progress = progress.coerceIn(0.06f, 1f), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DtvHomeWeatherWidget(
    weather: WeatherInfo,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val today = weather.forecast.firstOrNull()
    val inputFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (focused) Color.Black else Color.Transparent)
            .then(if (!focused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(3.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOCAL WEATHER",
                    color = DtvChrome.ElectricBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Full Forecast ▸",
                    color = if (focused) DtvChrome.ElectricBlueSoft else DtvChrome.MutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.glztv.app.ui.components.DtvWeatherIcon(
                        weatherCode = weather.weatherCode,
                        size = if (compact) 28.dp else 38.dp,
                        contentDescription = "Weather"
                    )
                    Column {
                        Text(
                            text = "${weather.temperature}°",
                            color = Color.White,
                            fontSize = if (compact) 24.sp else 34.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = weather.location.ifBlank { "Current Location" },
                            color = DtvChrome.SecondaryText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (today != null) {
                        Text(
                            text = "H ${today.high}° / L ${today.low}°",
                            color = DtvChrome.PrimaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${weather.humidity}% hum · ${weather.windSpeedMph} mph",
                        color = DtvChrome.SecondaryText,
                        fontSize = 10.sp
                    )
                }
            }

            if (!compact && weather.forecast.size >= 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weather.forecast.take(4).forEachIndexed { idx, day ->
                        val label = remember(day.date, idx) {
                            if (idx == 0) "Today"
                            else runCatching { dayFmt.format(requireNotNull(inputFmt.parse(day.date))) }
                                .getOrDefault(day.date.takeLast(2))
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF101319), RoundedCornerShape(2.dp))
                                .border(1.dp, DtvChrome.RowDivider, RoundedCornerShape(2.dp))
                                .padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(label, color = DtvChrome.MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            com.glztv.app.ui.components.DtvWeatherIcon(
                                weatherCode = day.weatherCode,
                                size = 16.dp,
                                isNight = false
                            )
                            Text("${day.high}°/${day.low}°", color = DtvChrome.PrimaryText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DtvCompactChannelCard(
    channel: Channel,
    programmeTitle: String,
    timeRange: String,
    progress: Float,
    isFavorite: Boolean,
    guide: EpgGuide,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(3.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        if (isFocused) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(DtvChrome.ElectricBlue))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                GoogleTvChannelLogo(
                    channel = channel,
                    size = 22.dp,
                    guide = guide,
                    shape = RoundedCornerShape(2.dp)
                )
                Text(
                    text = channel.number.ifBlank { "—" },
                    color = if (isFocused) DtvChrome.ElectricBlueSoft else DtvChrome.SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = channel.name,
                    color = if (isFocused) Color.White else DtvChrome.PrimaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isFavorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = DtvChrome.Gold,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = programmeTitle,
                    color = if (isFocused) Color.White else DtvChrome.SecondaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = timeRange,
                    color = DtvChrome.MutedText,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
            DtvProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DtvMoreLinkChip(
    label: String,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = label,
        color = if (focused) Color.White else DtvChrome.ElectricBlueSoft,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (focused) DtvChrome.ElectricBlue else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun DtvHomeFavToggle(
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color.Black else Color(0xFF171B22))
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) DtvChrome.ElectricBlue else DtvChrome.Divider, RoundedCornerShape(3.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
            tint = if (isFavorite) DtvChrome.Gold else DtvChrome.PrimaryText,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
internal fun DtvMoviePosterCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .width(122.dp)
            .height(178.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF13171E))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(4.dp)
            )
            .onFocusChanged {
                val focused = it.isFocused
                if (focused && !isFocused) onFocused?.invoke()
                isFocused = focused
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        if (movie.posterUrl.isNotBlank()) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1.0f to Color(0xEB05070A)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            movie.releaseYear?.let { year ->
                Text(
                    text = year.toString(),
                    color = DtvChrome.SecondaryText,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun homeWeatherIcon(code: Int): String = when (code) {
    0 -> "☀️"
    1, 2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> "🌧️"
    71, 73, 75, 85, 86 -> "❄️"
    95, 96, 99 -> "⛈️"
    else -> "🌤️"
}
