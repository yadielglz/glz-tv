package com.glztv.app.ui.screens.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.GlzHubManager
import com.glztv.app.GuestExperience
import com.glztv.app.GuestService
import com.glztv.app.ManagedInstallActivity
import com.glztv.app.Programme
import com.glztv.app.model.EntertainmentApp
import com.glztv.app.model.NetworkInfo
import com.glztv.app.model.WeatherInfo
import com.glztv.app.ui.WeatherFormatter
import com.glztv.app.ui.components.ChannelLogo
import com.glztv.app.ui.components.GlzCardDefaults
import com.glztv.app.ui.components.GlzFocusCard
import com.glztv.app.ui.components.tvFocusableWithPhysics
import com.glztv.app.ui.i18n.LocalGlzStrings
import com.glztv.app.ui.navigation.panelEnter
import com.glztv.app.ui.navigation.panelExit
import com.glztv.app.ui.screens.player.VideoPlayer
import com.glztv.app.ui.screens.settings.TvSettingsButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val PREFS = "glz_tv_prefs"

@Composable
fun GuestHubHome(
    guestName: String,
    experience: GuestExperience,
    entertainmentApps: List<EntertainmentApp>,
    previewChannel: Channel?,
    resumeChannel: Channel?,
    weather: WeatherInfo?,
    networkInfo: NetworkInfo?,
    captionLanguage: String,
    channels: List<Channel>,
    guide: EpgGuide,
    onWatchChannel: (Channel) -> Unit,
    sportsBarKioskEnabled: Boolean = false,
    onStartSportsBarKiosk: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showQuickWatchDrawer by remember { mutableStateOf(false) }
    var showAppsDrawer by remember { mutableStateOf(false) }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - (now % 60_000L))
        }
    }

    val strings = LocalGlzStrings.current
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val timeGreeting = when (hour) {
        in 5..11 -> strings.goodMorning
        in 12..17 -> strings.goodAfternoon
        else -> strings.goodEvening
    }

    Box(modifier.fillMaxSize()) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            val compactHeight = maxHeight < 440.dp

            Column(Modifier.fillMaxSize()) {
                // 1. HERO — fills everything above the pinned action row.
                Card(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        val scrimColor = MaterialTheme.colorScheme.surface
                        val hasPreview = previewChannel != null

                        // Base layer: the hero background image whenever one is configured,
                        // otherwise the ambient gradient. Drawn first so it shows while a
                        // preview video is buffering or if playback fails.
                        if (!experience.heroImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = experience.heroImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Live channel preview covers the base when a Home preview channel is set.
                        previewChannel?.let { channel ->
                            VideoPlayer(
                                channel = channel,
                                captionsEnabled = false,
                                captionLanguage = captionLanguage,
                                modifier = Modifier.fillMaxSize(),
                                muted = true,
                                createMediaSession = false,
                                keepScreenOn = false,
                                cropVideo = true
                            )
                        }

                        // Scrims — legibility for the identity + context text over an image or video.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        0f to scrimColor.copy(alpha = if (hasPreview) 0.96f else 0.82f),
                                        0.34f to scrimColor.copy(alpha = if (hasPreview) 0.80f else 0.45f),
                                        0.68f to scrimColor.copy(alpha = 0f)
                                    )
                                )
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.10f),
                                            Color.Black.copy(alpha = 0.48f)
                                        )
                                    )
                                )
                        )

                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp, vertical = if (compactHeight) 14.dp else 22.dp)
                        ) {
                            // Guest identity
                            val hasBackdrop = previewChannel != null || !experience.heroImageUrl.isNullOrBlank()
                            Column(Modifier.fillMaxWidth(if (hasBackdrop) 0.60f else 1f)) {
                                Text(
                                    timeGreeting,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = if (compactHeight) 14.sp else 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1
                                )
                                Text(
                                    guestName.ifBlank { strings.guest },
                                    color = Color.White,
                                    fontSize = if (compactHeight) 32.sp else 48.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    letterSpacing = (-1).sp,
                                    lineHeight = if (compactHeight) 34.sp else 50.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (experience.propertyName.isNotBlank()) {
                                    Text(
                                        experience.propertyName,
                                        color = Color.White.copy(alpha = 0.90f),
                                        fontSize = if (compactHeight) 13.sp else 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                }
                                experience.roomNumber?.takeIf(String::isNotBlank)?.let {
                                    Text(
                                        "${strings.room} $it",
                                        color = Color.White.copy(alpha = 0.78f),
                                        fontSize = if (compactHeight) 13.sp else 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                                if (!compactHeight) {
                                    val stayInfo = listOfNotNull(
                                        experience.checkoutTime?.let { "${strings.checkout} $it" },
                                        if (!experience.arrivalDate.isNullOrBlank() &&
                                            !experience.departureDate.isNullOrBlank()
                                        ) "${experience.arrivalDate} – ${experience.departureDate}" else null
                                    ).joinToString("   ·   ")
                                    if (stayInfo.isNotBlank()) {
                                        Text(
                                            stayInfo,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 2.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            // Live context
                            val timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now))
                            val meridiemSep = timeText.lastIndexOf(' ')
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    strings.now,
                                    Modifier
                                        .width(74.dp)
                                        .padding(bottom = 3.dp),
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    maxLines = 1
                                )
                                Text(
                                    if (meridiemSep > 0) timeText.substring(0, meridiemSep) else timeText,
                                    color = Color.White,
                                    fontSize = if (compactHeight) 22.sp else 28.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp,
                                    maxLines = 1
                                )
                                if (meridiemSep > 0) {
                                    Text(
                                        timeText.substring(meridiemSep + 1),
                                        color = Color.White.copy(alpha = 0.70f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(if (compactHeight) 6.dp else 10.dp))
                            weather?.let {
                                HeroInfoLine(
                                    strings.weather,
                                    it.location,
                                    "${WeatherFormatter.symbol(it.weatherCode)}  ${it.temperature}°F, ${WeatherFormatter.description(it.weatherCode)}"
                                )
                                Spacer(Modifier.height(if (compactHeight) 3.dp else 6.dp))
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HeroInfoLine(
                                    strings.network,
                                    networkInfo?.connection ?: strings.offline,
                                    networkInfo?.isp.orEmpty(),
                                    modifier = Modifier.weight(1f, fill = false),
                                    stacked = true
                                )
                                Spacer(Modifier.weight(1f))
                                previewChannel?.let { channel ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.55f)
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(6.dp)
                                                    .background(Color(0xFFFF3B30), CircleShape)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "${channel.number.ifBlank { strings.live }} · ${channel.name}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(if (compactHeight) 10.dp else 16.dp))

                // 2. ACTION ROW — pinned to the bottom
                Row(
                    Modifier
                        .fillMaxWidth()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (resumeChannel != null) {
                        HomeNavActionButton(
                            label = "${strings.continueWatching}  ·  ${resumeChannel.name.uppercase(Locale.getDefault())}",
                            icon = Icons.Default.PlayArrow,
                            isPrimary = true,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { onWatchChannel(resumeChannel) },
                            modifier = Modifier.weight(2f)
                        )
                        HomeNavActionButton(
                            label = strings.liveTv,
                            icon = Icons.Default.LiveTv,
                            isPrimary = false,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { showQuickWatchDrawer = true },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        HomeNavActionButton(
                            label = strings.liveTv,
                            icon = Icons.Default.LiveTv,
                            isPrimary = true,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { showQuickWatchDrawer = true },
                            modifier = Modifier.weight(1.6f)
                        )
                    }
                    HomeNavActionButton(
                        label = strings.apps,
                        icon = Icons.Default.Apps,
                        isPrimary = false,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { showAppsDrawer = true },
                        modifier = Modifier.weight(1f)
                    )
                    if (sportsBarKioskEnabled) {
                        HomeNavActionButton(
                            label = "Sports Bar",
                            icon = Icons.Default.Radio,
                            isPrimary = false,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = onStartSportsBarKiosk,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. LIVE TV — QUICK CHANNEL SELECTION FLYOUT
        if (showQuickWatchDrawer) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(onClick = { showQuickWatchDrawer = false })
            ) {
                AnimatedVisibility(
                    visible = showQuickWatchDrawer,
                    enter = panelEnter(fromLeft = false),
                    exit = panelExit(fromLeft = false),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    QuickWatchChannelDrawer(
                        channels = channels,
                        guide = guide,
                        onSelectChannel = { channel ->
                            showQuickWatchDrawer = false
                            onWatchChannel(channel)
                        },
                        onClose = { showQuickWatchDrawer = false }
                    )
                }
            }
        }

        // 4. APPS FLYOUT
        if (showAppsDrawer) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(onClick = { showAppsDrawer = false })
            ) {
                AnimatedVisibility(
                    visible = showAppsDrawer,
                    enter = panelEnter(fromLeft = false),
                    exit = panelExit(fromLeft = false),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    HomeAppsDrawer(
                        apps = entertainmentApps,
                        onClose = { showAppsDrawer = false }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeAppsDrawer(
    apps: List<EntertainmentApp>,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val client = remember { OkHttpClient.Builder().build() }
    val scope = rememberCoroutineScope()
    val firstItemFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120L)
        runCatching { firstItemFocus.requestFocus() }
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(440.dp)
            .clickable(enabled = false, onClick = {}),
        color = Color(0xFF0A101C).copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        val drawerStrings = LocalGlzStrings.current
                        Text(drawerStrings.apps, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                        Text(
                            "${apps.size} apps & services",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val drawerStrings = LocalGlzStrings.current
                TvSettingsButton(label = drawerStrings.close, onClick = onClose)
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    val launchIntent = remember(app.packageName) {
                        findAppLaunchIntent(context, app.packageName)
                    }
                    val icon = remember(app.packageName, launchIntent) {
                        if (launchIntent == null) null else runCatching {
                            context.packageManager.getApplicationIcon(app.packageName)
                        }.getOrNull()
                    }
                    GlzFocusCard(
                        onClick = {
                            GlzHubManager.reportLaunchedApp(prefs, app.name, app.packageName)
                            scope.launch(Dispatchers.IO) {
                                runCatching { GlzHubManager.heartbeat(prefs, client) }
                            }
                            launchEntertainmentApp(context, app.packageName, launchIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier),
                        accent = app.accent,
                        shape = RoundedCornerShape(GlzCardDefaults.RadiusMedium),
                        focusedScale = 1.03f
                    ) { _ ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AdaptiveAppIcon(
                                icon = icon,
                                appName = app.name,
                                accent = app.accent,
                                size = 46.dp
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    app.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (launchIntent == null) "Not installed · opens store" else "Installed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroInfoLine(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
    stacked: Boolean = false
) {
    Row(
        modifier,
        verticalAlignment = if (stacked) Alignment.Top else Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            Modifier.width(74.dp),
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            maxLines = 1
        )
        if (stacked) {
            Column {
                Text(
                    value,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (detail.isNotBlank()) {
                    Text(
                        detail,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Text(
                value,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (detail.isNotBlank()) {
                Text(
                    detail,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HomeNavActionButton(
    label: String,
    icon: ImageVector,
    isPrimary: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlzFocusCard(
        onClick = onClick,
        modifier = modifier,
        selected = isPrimary,
        accent = accentColor,
        shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
        focusedScale = 1.06f
    ) { _ ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickWatchChannelDrawer(
    channels: List<Channel>,
    guide: EpgGuide,
    onSelectChannel: (Channel) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val now = remember { System.currentTimeMillis() }
    val firstItemFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120L)
        runCatching { firstItemFocus.requestFocus() }
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(440.dp)
            .clickable(enabled = false, onClick = {}),
        color = Color(0xFF0A101C).copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        val drawerStrings = LocalGlzStrings.current
                        Text(drawerStrings.liveTv, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                        Text("${channels.size} channels available", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val drawerStrings = LocalGlzStrings.current
                TvSettingsButton(
                    label = drawerStrings.close,
                    onClick = onClose
                )
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(channels, key = { index, channel -> "$index:${channel.id}" }) { index, channel ->
                    val currentProg = remember(channel, guide) {
                        guide.forChannel(channel).firstOrNull { it.startMillis <= now && it.endMillis > now }
                    }
                    val progress = currentProg?.let {
                        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L)).coerceIn(0f, 1f)
                    } ?: 0f
                    var focused by remember { mutableStateOf(false) }

                    Surface(
                        onClick = { onSelectChannel(channel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier)
                            .tvFocusableWithPhysics(
                                shape = RoundedCornerShape(16.dp),
                                focusedScale = 1.03f,
                                glowColor = Color(0xFF00E5FF),
                                onFocusChange = { focused = it }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            focused -> Color(0xFF00E5FF)
                            else -> Color.White.copy(alpha = 0.05f)
                        },
                        contentColor = when {
                            focused -> Color.Black
                            else -> Color.White
                        },
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (focused) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.10f),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Text(
                                        channel.number?.let { "#$it" } ?: "#",
                                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (focused) Color.Black else Color(0xFF00E5FF)
                                    )
                                }
                                ChannelLogo(channel, 38.dp, guide)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        channel.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    currentProg?.let { prog ->
                                        Text(
                                            prog.title,
                                            fontSize = 12.sp,
                                            color = if (focused) Color.Black.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (currentProg != null) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (focused) Color.Black else Color(0xFF00E5FF),
                                    trackColor = if (focused) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.12f)
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
fun StaySummaryCard(
    guestName: String,
    experience: GuestExperience,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(26.dp)
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .heightIn(min = 210.dp)
            .tvFocusableWithPhysics(
                shape = shape,
                focusedScale = 1.03f,
                glowColor = primaryColor,
                onFocusChange = { focused = it }
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    )
                )
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "YOUR STAY",
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    experience.roomNumber?.let { room ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = primaryColor.copy(alpha = 0.20f),
                        ) {
                            Text(
                                "ROOM $room",
                                color = primaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        experience.propertyName.ifBlank { "Guest Suite" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        guestName.ifBlank { "Guest" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val stayPills = buildList {
                    experience.arrivalDate?.let { add("Arrival: $it") }
                    experience.departureDate?.let { add("Departure: $it") }
                    experience.checkoutTime?.let { add("Checkout: $it") }
                }
                if (stayPills.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        stayPills.take(2).forEach { info ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.08f),
                            ) {
                                Text(
                                    info,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
fun WifiInformationCard(
    experience: GuestExperience,
    modifier: Modifier = Modifier
) {
    val wifiName = experience.wifiName.orEmpty()
    val wifiPass = experience.wifiInstructions.orEmpty()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(26.dp)
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = modifier
            .heightIn(min = 210.dp)
            .tvFocusableWithPhysics(
                shape = shape,
                focusedScale = 1.03f,
                glowColor = secondaryColor,
                onFocusChange = { focused = it }
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            secondaryColor.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    )
                )
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (wifiName.isNotBlank()) {
                    WifiQrCode(wifiName, wifiPass, 130.dp)
                } else {
                    Box(
                        Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.40f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint = secondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "WI-FI ACCESS",
                            color = secondaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        wifiName.ifBlank { "Wi-Fi Details Unavailable" },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (wifiPass.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = secondaryColor.copy(alpha = 0.20f),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Pass: ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryColor
                                )
                                Text(
                                    wifiPass,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        "Scan QR with phone camera to connect",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun WifiQrCode(ssid: String, password: String?, size: Dp) {
    val bitmap = remember(ssid, password) {
        val escape: (String) -> String = {
            it.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace(":", "\\:")
        }
        val payload = "WIFI:T:WPA;S:${escape(ssid)};P:${escape(password.orEmpty())};;"
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 384, 384)
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            val x = index % matrix.width
            val y = index / matrix.width
            if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
        }
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.size(size)
    ) {
        Image(
            bitmap.asImageBitmap(),
            contentDescription = "Wi-Fi QR code for $ssid",
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        )
    }
}

@Composable
fun GuestServiceCard(service: GuestService) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    val serviceIcon = when {
        service.title.contains("Front Desk", ignoreCase = true) -> Icons.Default.Person
        service.title.contains("Notice", ignoreCase = true) || service.title.contains("Thank", ignoreCase = true) -> Icons.Default.Home
        else -> Icons.Default.LiveTv
    }

    Card(
        onClick = {
            service.actionUrl?.let { url ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .tvFocusableWithPhysics(
                shape = shape,
                focusedScale = 1.04f,
                glowColor = MaterialTheme.colorScheme.primary,
                onFocusChange = { focused = it }
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (focused) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(if (focused) 12.dp else 2.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            serviceIcon,
                            contentDescription = null,
                            tint = if (focused) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        service.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    service.subtitle?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            it,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (service.actionUrl != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun PremiumHero(
    channel: Channel,
    programme: Programme?,
    guide: EpgGuide? = null,
    onWatch: () -> Unit,
    onGuide: () -> Unit,
    onFocused: () -> Unit
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }
    val progress = programme?.let {
        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
            .coerceIn(0f, 1f)
    } ?: 0f
    Card(
        Modifier.fillMaxWidth().height(245.dp),
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
        ) {
            Row(
                Modifier.fillMaxSize().padding(30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogo(channel, 112.dp, guide)
                Spacer(Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${channel.number} · LIVE",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Black
                    )
                    Text(channel.name, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    Text(
                        programme?.title ?: "Live programming",
                        fontSize = 21.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    programme?.description?.takeIf(String::isNotBlank)?.let {
                        Text(
                            it,
                            Modifier.padding(top = 6.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (programme != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(.72f).padding(top = 14.dp).height(4.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)
                        )
                    }
                    Row(
                        Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onWatch,
                            modifier = Modifier.onFocusChanged {
                                if (it.isFocused) onFocused()
                            }
                        ) { Text("Watch now") }
                        Button(
                            onClick = onGuide,
                            modifier = Modifier.onFocusChanged {
                                if (it.isFocused) onFocused()
                            }
                        ) { Text("View guide") }
                    }
                }
            }
        }
    }
}

@Composable
fun HubSectionTitle(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
        Spacer(Modifier.width(12.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
fun LiveHubCard(
    channel: Channel,
    programmes: List<Programme>,
    guide: EpgGuide? = null,
    now: Long,
    onClick: () -> Unit
) {
    val programme = programmes.firstOrNull { it.startMillis <= now && it.endMillis > now }
    val progress = programme?.let {
        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
            .coerceIn(0f, 1f)
    } ?: 0f
    PremiumFocusCard(
        modifier = Modifier.width(252.dp).aspectRatio(16f / 9f),
        onClick = onClick,
        accent = MaterialTheme.colorScheme.primary
    ) {
        Row(Modifier.padding(start = 15.dp, top = 14.dp, end = 15.dp),
            verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(channel, 52.dp, guide)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(channel.number, color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Black)
                Text(channel.name, fontWeight = FontWeight.Black, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            programme?.title ?: "Live programming",
            Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)
        )
    }
}

fun Color.toLuminousAccent(): Color {
    val r = red
    val g = green
    val b = blue
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
    return if (luminance < 0.38f) {
        val factor = 0.50f / (luminance + 0.05f)
        Color(
            (r * factor).coerceAtMost(1f),
            (g * factor).coerceAtMost(1f),
            (b * factor).coerceAtMost(1f),
            alpha
        )
    } else {
        this
    }
}

@Composable
fun LiveTvHubCard(
    onClick: () -> Unit,
    width: Dp = 190.dp,
    height: Dp = 100.dp
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    PremiumFocusCard(
        modifier = Modifier.width(width).height(height),
        onClick = onClick,
        accent = primaryColor
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            primaryColor,
                            secondaryColor
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.LiveTv,
                    contentDescription = "Live TV",
                    modifier = Modifier.size(34.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Live TV",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EntertainmentAppCard(
    app: EntertainmentApp,
    width: Dp = 190.dp,
    height: Dp = 100.dp
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
    val client = remember { OkHttpClient.Builder().build() }
    val scope = rememberCoroutineScope()
    val packageManager = context.packageManager
    val launchIntent = remember(app.packageName) {
        findAppLaunchIntent(context, app.packageName)
    }
    val installedIcon = remember(app.packageName, launchIntent) {
        if (launchIntent != null) runCatching {
            packageManager.getApplicationIcon(app.packageName)
        }.getOrNull() else null
    }

    val isLightApp = app.name.equals("YouTube", ignoreCase = true) ||
        app.name.equals("YouTube Music", ignoreCase = true)
    val cardBackground = if (isLightApp) Color(0xFFF6F8FA) else Color(0xFF161C2C)

    PremiumFocusCard(
        modifier = Modifier.width(width).height(height),
        onClick = {
            GlzHubManager.reportLaunchedApp(prefs, app.name, app.packageName)
            scope.launch(Dispatchers.IO) {
                GlzHubManager.heartbeat(prefs, client)
            }
            launchEntertainmentApp(context, app.packageName, launchIntent)
        },
        accent = app.accent
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            cardBackground,
                            cardBackground.copy(alpha = 0.95f),
                            app.accent.copy(alpha = 0.25f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AdaptiveAppIcon(
                    icon = installedIcon,
                    appName = app.name,
                    accent = app.accent,
                    size = 44.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.name,
                        color = Color.White,
                        fontSize = if (app.name.length > 10) 14.sp else 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (installedIcon != null) "INSTALLED" else "APP",
                        color = app.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AdaptiveAppIcon(
    icon: Drawable?,
    appName: String,
    accent: Color,
    size: Dp
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        Modifier.size(size),
        shape = shape,
        color = Color.White.copy(alpha = 0.08f),
    ) {
        when {
            icon != null -> AsyncImage(
                model = icon,
                contentDescription = appName,
                modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit
            )
            else -> Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.7f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    appName.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PremiumFocusCard(
    modifier: Modifier,
    onClick: () -> Unit,
    accent: Color,
    onFocusChange: ((Boolean) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlzFocusCard(
        onClick = onClick,
        modifier = modifier,
        accent = accent,
        shape = RoundedCornerShape(GlzCardDefaults.RadiusMedium),
        focusedScale = 1.07f,
        contentModifier = Modifier.fillMaxSize(),
        onFocusChange = onFocusChange
    ) { _ ->
        content()
    }
}

fun launchEntertainmentApp(context: Context, packageName: String, launchIntent: Intent?) {
    try {
        if (launchIntent != null) {
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

suspend fun handleManagedHubCommand(
    context: Context,
    prefs: SharedPreferences,
    client: OkHttpClient,
    command: GlzHubManager.HubCommand,
    onForceRefresh: suspend () -> Unit
) {
    when (command) {
        is GlzHubManager.ForceRefreshCommand -> {
            onForceRefresh()
            withContext(Dispatchers.IO) {
                runCatching {
                    GlzHubManager.completeCommand(
                        prefs, client, command.id, true, "EPG and M3U force refresh completed on TV"
                    )
                }
            }
        }
        is GlzHubManager.AppCommand -> {
            val result = runCatching {
                if (command.sourceType == "repository") {
                    val url = requireNotNull(command.sourceUrl) { "Repository URL is missing" }
                    context.startActivity(
                        ManagedInstallActivity.intent(
                            context = context,
                            sourceUrl = url,
                            packageName = command.packageName,
                            commandId = command.id
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } else {
                    val uri = Uri.parse("market://details?id=${command.packageName}")
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            withContext(Dispatchers.IO) {
                runCatching {
                    GlzHubManager.completeCommand(
                        prefs, client, command.id, result.isSuccess,
                        if (result.isSuccess) "In-app installer opened on TV" else (result.exceptionOrNull()?.message ?: "Could not open installer")
                    )
                }
            }
        }
    }
}

fun findAppLaunchIntent(context: Context, packageName: String): Intent? {
    val packageManager = context.packageManager
    return packageManager.getLeanbackLaunchIntentForPackage(packageName)
        ?: packageManager.getLaunchIntentForPackage(packageName)
        ?: Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setPackage(packageName)
            .takeIf { it.resolveActivity(packageManager) != null }
}
