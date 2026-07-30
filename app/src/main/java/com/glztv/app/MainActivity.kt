@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.glztv.app

import android.Manifest
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.view.KeyEvent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.media3.session.MediaSession
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream

private const val PREFS = "glz_tv"
private const val PLAYLIST_URL = "playlist_url"
private const val EPG_URL = "epg_url"
private const val REQUEST_HEADERS = "request_headers"
private const val FAVORITES = "favorites"
private const val THEME_MODE = "theme_mode"
private const val CAPTIONS_ENABLED = "captions_enabled"
private const val CAPTION_LANGUAGE = "caption_language"
private const val AUTO_UPDATE_CHECK = "auto_update_check"
private const val WIFI_ONLY_UPDATES = "wifi_only_updates"
private const val AUTO_START = "auto_start"
private const val RESUME_LAST_CHANNEL = "resume_last_channel"
private const val START_DESTINATION = "start_destination"
private const val LAST_CHANNEL_ID = "last_channel_id"
private const val WEATHER_LOCATION = "weather_location"
private const val GUEST_NAME = "guest_name"
private const val DEFAULT_PLAYLIST_URL = "http://play.glztech.com/list.m3u"
private const val DEFAULT_EPG_URL = "https://play.glztech.com/epg.xml.gz"
private const val DEFAULT_WEATHER_LOCATION = "San Juan"

private enum class AppSection { Home, Live, Guide, Search }
private enum class PlayerDrawer { None, Channels, Services }

private data class WeatherInfo(
    val temperature: Int,
    val weatherCode: Int,
    val location: String
)

private data class NetworkInfo(val wifiName: String, val isp: String)

private val InterFontFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold),
    Font(R.font.inter_variable, FontWeight.Black)
)

private val InterTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = InterFontFamily),
        displayMedium = displayMedium.copy(fontFamily = InterFontFamily),
        displaySmall = displaySmall.copy(fontFamily = InterFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = InterFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = InterFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = InterFontFamily),
        titleLarge = titleLarge.copy(fontFamily = InterFontFamily),
        titleMedium = titleMedium.copy(fontFamily = InterFontFamily),
        titleSmall = titleSmall.copy(fontFamily = InterFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = InterFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = InterFontFamily),
        bodySmall = bodySmall.copy(fontFamily = InterFontFamily),
        labelLarge = labelLarge.copy(fontFamily = InterFontFamily),
        labelMedium = labelMedium.copy(fontFamily = InterFontFamily),
        labelSmall = labelSmall.copy(fontFamily = InterFontFamily)
    )
}

private data class EntertainmentApp(
    val name: String,
    val packageName: String,
    val accent: Color
)

private val EntertainmentApps = listOf(
    EntertainmentApp("YouTube", "com.google.android.youtube.tv", Color(0xFFFF2020)),
    EntertainmentApp("Netflix", "com.netflix.ninja", Color(0xFFE50914)),
    EntertainmentApp("MLB", "com.bamnetworks.mobile.android.gameday.atbat", Color(0xFF17408B)),
    EntertainmentApp("OleadaTV", "com.android.mgsandroid", Color(0xFF6E55FF)),
    EntertainmentApp("GLZ Radio", "com.glztech.radiostream", Color(0xFFFF6B2C)),
    EntertainmentApp("GeeSports", "com.live.geesports", Color(0xFF00A86B)),
    EntertainmentApp("Paramount+", "com.cbs.ott", Color(0xFF0064FF)),
    EntertainmentApp("Disney+", "com.disney.disneyplus", Color(0xFF1234B8)),
    EntertainmentApp("Peacock", "com.peacocktv.peacockandroid", Color(0xFF5D2B86)),
    EntertainmentApp("Spectrum TV", "com.TWCableTV", Color(0xFF0073CF))
)

class MainActivity : ComponentActivity() {
    private val deepLinkChannelId = mutableStateOf<String?>(null)
    private val networkPermissionRevision = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlzBackgroundSync.schedule(applicationContext)
        deepLinkChannelId.value = intent?.data?.takeIf { it.host == "channel" }?.lastPathSegment
        setContent {
            GlzTvApp(deepLinkChannelId.value, networkPermissionRevision.value)
        }
        requestWifiIdentityPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkChannelId.value = intent.data?.takeIf { it.host == "channel" }?.lastPathSegment
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1989) networkPermissionRevision.value++
    }

    private fun requestWifiIdentityPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        // A TV does not need an SSID badly enough to interrupt first launch with a
        // location permission dialog (and Fire TV may not provide one at all).
        if (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION) return
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        if (permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(permissions, 1989)
        }
    }
}

private val GlzColors = darkColorScheme(
    primary = Color(0xFFFFB690),
    onPrimary = Color(0xFF552006),
    primaryContainer = Color(0xFF7B3416),
    secondary = Color(0xFFC4FF4D),
    onSecondary = Color(0xFF263500),
    background = Color(0xFF07101D),
    surface = Color(0xFF0E1B2C),
    surfaceVariant = Color(0xFF1A2A3E),
    onSurface = Color(0xFFEFF5FB),
    onSurfaceVariant = Color(0xFFB9C8DA)
)

private val GlzLightColors = lightColorScheme(
    primary = Color(0xFF9A3E0A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCA),
    secondary = Color(0xFF4D6700),
    onSecondary = Color.White,
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFF3DED4),
    onSurface = Color(0xFF241A16),
    onSurfaceVariant = Color(0xFF55443C)
)

@Composable
private fun GlzTvApp(deepLinkChannelId: String?, networkPermissionRevision: Int) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var themeMode by remember { mutableStateOf(prefs.getString(THEME_MODE, "adaptive") ?: "adaptive") }
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val colors = if (themeMode == "adaptive" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) GlzColors else GlzLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = InterTypography,
        shapes = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(32.dp)
        )
    ) {
        Surface(Modifier.fillMaxSize()) {
            TvScreen(themeMode, deepLinkChannelId, networkPermissionRevision) {
                themeMode = it
                prefs.edit().putString(THEME_MODE, it).apply()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TvScreen(
    themeMode: String,
    deepLinkChannelId: String?,
    networkPermissionRevision: Int,
    onThemeMode: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val client = remember { OkHttpClient() }
    val scope = rememberCoroutineScope()
    val channels = remember { mutableStateListOf<Channel>() }
    var guide by remember { mutableStateOf(EpgGuide.Empty) }
    var selected by remember { mutableStateOf<Channel?>(null) }
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Add an M3U playlist to start watching.") }
    var loading by remember { mutableStateOf(false) }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var networkInfo by remember { mutableStateOf<NetworkInfo?>(null) }
    var weatherLocation by remember {
        mutableStateOf(prefs.getString(WEATHER_LOCATION, DEFAULT_WEATHER_LOCATION)
            ?: DEFAULT_WEATHER_LOCATION)
    }
    var guestName by remember {
        mutableStateOf(prefs.getString(GUEST_NAME, "Guest") ?: "Guest")
    }
    var guestExperience by remember { mutableStateOf(GuestExperience.from(prefs)) }
    var visibleAppPackages by remember {
        mutableStateOf(GlzHubManager.visibleApps(prefs))
    }
    var appVisibilityManaged by remember {
        mutableStateOf(prefs.getBoolean(GlzHubManager.VISIBLE_APPS_MANAGED, false))
    }
    var hubStatus by remember {
        mutableStateOf(
            GlzHubManager.pairingCode(prefs)?.let { "Pairing code: $it" }
                ?: if (GlzHubManager.isEnrolled(prefs)) "Connected to GLZ Hub"
                else "Not connected"
        )
    }
    var availableUpdate by remember { mutableStateOf<GithubUpdateManager.UpdateInfo?>(null) }
    var updateDownloadStatus by remember { mutableStateOf<String?>(null) }
    var updateDownloading by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSettingsPin by remember { mutableStateOf(false) }
    var captionsEnabled by remember {
        mutableStateOf(prefs.getBoolean(CAPTIONS_ENABLED, false))
    }
    var captionLanguage by remember {
        mutableStateOf(prefs.getString(CAPTION_LANGUAGE, "en") ?: "en")
    }
    var section by remember {
        mutableStateOf(
            runCatching {
                AppSection.valueOf(
                    prefs.getString(START_DESTINATION, AppSection.Home.name) ?: AppSection.Home.name
                )
            }.getOrDefault(AppSection.Home)
        )
    }
    var favorites by remember {
        mutableStateOf(prefs.getStringSet(FAVORITES, emptySet()).orEmpty().toSet())
    }

    suspend fun loadSources(forceRefresh: Boolean = false) {
        val playlistUrl = prefs.getString(PLAYLIST_URL, DEFAULT_PLAYLIST_URL)
            .orEmpty().ifBlank { DEFAULT_PLAYLIST_URL }
        if (playlistUrl.isBlank()) {
            showSettings = true
            return
        }
        loading = true
        status = "Loading your lineup…"
        val headers = parseHeaders(prefs.getString(REQUEST_HEADERS, "").orEmpty())
        val epgUrl = prefs.getString(EPG_URL, DEFAULT_EPG_URL)
            .orEmpty().ifBlank { DEFAULT_EPG_URL }
        val cached = withContext(Dispatchers.IO) {
            if (forceRefresh) null to null else {
                ChannelCache.read(context, playlistUrl) to
                    epgUrl.takeIf(String::isNotBlank)?.let { EpgCache.read(context, it) }
            }
        }
        cached.first?.let { cachedChannels ->
            channels.clear()
            channels.addAll(cachedChannels)
            cached.second?.let { guide = it }
            status = "${cachedChannels.size} channels · restored instantly" +
                cached.second?.let { " · ${it.programmeCount} guide entries" }.orEmpty()
            if (selected == null && prefs.getBoolean(RESUME_LAST_CHANNEL, true)) {
                val lastId = prefs.getString(LAST_CHANNEL_ID, null)
                selected = cachedChannels.firstOrNull { it.id == lastId }
            }
        }
        runCatching {
            withContext(Dispatchers.IO) {
                val parsed = cached.first ?: M3uParser.parse(
                    fetchText(client, playlistUrl, headers),
                    playlistUrl,
                    headers
                ).also { ChannelCache.write(context, playlistUrl, it) }
                val parsedGuide = cached.second ?: if (epgUrl.isNotBlank()) {
                    EpgParser.parse(fetchText(client, epgUrl, headers))
                        .also { EpgCache.write(context, epgUrl, it) }
                } else EpgGuide.Empty
                Triple(parsed, parsedGuide, cached.first != null && cached.second != null)
            }
        }.onSuccess { (parsed, parsedGuide, fromCache) ->
            channels.clear()
            channels.addAll(parsed)
            guide = parsedGuide
            status = "${parsed.size} channels${if (fromCache) " · restored from storage" else ""} · " +
                "${parsedGuide.programmeCount} guide entries"
            if (selected == null && prefs.getBoolean(RESUME_LAST_CHANNEL, true)) {
                val lastId = prefs.getString(LAST_CHANNEL_ID, null)
                selected = parsed.firstOrNull { it.id == lastId }
            }
            withContext(Dispatchers.IO) {
                TvHomePublisher.publish(context.applicationContext, parsed, parsedGuide)
            }
        }.onFailure {
            status = if (channels.isNotEmpty()) {
                "${channels.size} saved channels · refresh unavailable"
            } else "Could not load sources: ${it.message}"
        }
        loading = false
    }

    suspend fun checkForAppUpdate(): String {
        return runCatching {
            withContext(Dispatchers.IO) { GithubUpdateManager.check(client) }
        }.fold(
            onSuccess = { update ->
                availableUpdate = update
                if (update == null) "Version ${BuildConfig.VERSION_NAME} is current"
                else "Version ${update.version} is ready to install"
            },
            onFailure = { "Update check failed: ${it.message}" }
        )
    }

    LaunchedEffect(Unit) {
        val initialSync = runCatching {
            withContext(Dispatchers.IO) { GlzHubManager.sync(prefs, client) }
        }.getOrNull()
        initialSync?.commands?.forEach { command ->
            handleManagedAppCommand(context, prefs, client, command)
        }
        if (initialSync?.changed == true) {
            guestName = prefs.getString(GUEST_NAME, "Guest") ?: "Guest"
            guestExperience = GuestExperience.from(prefs)
            weatherLocation = prefs.getString(WEATHER_LOCATION, DEFAULT_WEATHER_LOCATION)
                ?: DEFAULT_WEATHER_LOCATION
            visibleAppPackages = GlzHubManager.visibleApps(prefs)
            appVisibilityManaged = prefs.getBoolean(GlzHubManager.VISIBLE_APPS_MANAGED, false)
            onThemeMode(prefs.getString(THEME_MODE, themeMode) ?: themeMode)
            hubStatus = "Connected to GLZ Hub"
        }
        loadSources(forceRefresh = initialSync?.changed == true)
        while (true) {
            delay(30_000L)
            runCatching {
                withContext(Dispatchers.IO) {
                    val result = GlzHubManager.sync(prefs, client)
                    GlzHubManager.heartbeat(prefs, client)
                    result
                }
            }.onSuccess { result ->
                result.commands.forEach { command ->
                    handleManagedAppCommand(context, prefs, client, command)
                }
                if (result.changed) {
                    guestName = prefs.getString(GUEST_NAME, "Guest") ?: "Guest"
                    guestExperience = GuestExperience.from(prefs)
                    weatherLocation = prefs.getString(WEATHER_LOCATION, DEFAULT_WEATHER_LOCATION)
                        ?: DEFAULT_WEATHER_LOCATION
                    visibleAppPackages = GlzHubManager.visibleApps(prefs)
                    appVisibilityManaged =
                        prefs.getBoolean(GlzHubManager.VISIBLE_APPS_MANAGED, false)
                    onThemeMode(prefs.getString(THEME_MODE, themeMode) ?: themeMode)
                    loadSources(forceRefresh = true)
                }
                if (GlzHubManager.isEnrolled(prefs)) hubStatus = "Connected to GLZ Hub"
            }.onFailure {
                hubStatus = "GLZ Hub sync unavailable · using saved settings"
            }
        }
    }
    LaunchedEffect(prefs.getBoolean(AUTO_UPDATE_CHECK, true)) {
        if (!prefs.getBoolean(AUTO_UPDATE_CHECK, true)) return@LaunchedEffect
        delay(6_000L)
        while (true) {
            checkForAppUpdate()
            delay(6 * 60 * 60 * 1000L)
        }
    }
    LaunchedEffect(weatherLocation) {
        while (true) {
            weather = withContext(Dispatchers.IO) {
                fetchWeather(client, weatherLocation)
            }
            delay(30 * 60 * 1000L)
        }
    }
    LaunchedEffect(networkPermissionRevision) {
        while (true) {
            networkInfo = withContext(Dispatchers.IO) {
                fetchNetworkInfo(context, client)
            }
            delay(30 * 60 * 1000L)
        }
    }
    LaunchedEffect(deepLinkChannelId, channels.size) {
        if (!deepLinkChannelId.isNullOrBlank() && channels.isNotEmpty()) {
            channels.firstOrNull { it.id == deepLinkChannelId }?.let {
                selected = it
                section = AppSection.Live
            }
        }
    }

    val ordered = remember(channels.toList()) {
        channels.sortedWith(
            compareBy<Channel> { channelNumberValue(it.number) }
                .thenBy { it.number }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }
    val searchResults = remember(ordered, query) {
        ordered.filter {
            query.isBlank() || "${it.name} ${it.group} ${it.number}".lowercase(Locale.ROOT)
                .contains(query.lowercase(Locale.ROOT))
        }
    }
    val tuneChannel: (Channel) -> Unit = {
        selected = it
        prefs.edit().putString(LAST_CHANNEL_ID, it.id).apply()
    }
    val weatherChannel = ordered.firstOrNull {
        it.name.lowercase(Locale.ROOT).contains("weather channel")
    } ?: ordered.firstOrNull {
        it.name.lowercase(Locale.ROOT).contains("weather")
    }
    val immersive = section == AppSection.Live && selected != null
    val managedEntertainmentApps = remember(visibleAppPackages, appVisibilityManaged) {
        if (!appVisibilityManaged) EntertainmentApps
        else EntertainmentApps.filter { it.packageName in visibleAppPackages }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!immersive) {
            SlimHeader(
                loading = loading,
                    contentLoaded = channels.isNotEmpty(),
                    weather = weather,
                    networkInfo = networkInfo,
                    onWeatherClick = weatherChannel?.takeIf { section == AppSection.Home }?.let { channel ->
                        {
                            tuneChannel(channel)
                            section = AppSection.Live
                        }
                    },
                    onRefresh = { scope.launch { loadSources(forceRefresh = true) } },
                    onSettings = { showSettingsPin = true }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(if (immersive) PaddingValues(0.dp) else padding)) {
            if (immersive) {
                ImmersivePlayerScreen(
                    channel = selected!!,
                    channels = ordered,
                    guide = guide,
                    captionsEnabled = captionsEnabled,
                    captionLanguage = captionLanguage,
                    entertainmentApps = managedEntertainmentApps,
                    onTune = tuneChannel,
                    onExit = { section = AppSection.Home }
                )
            } else {
                Box(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)
                        .padding(bottom = 76.dp)
                ) {
                if (section == AppSection.Home) {
                    GuestHubHome(
                        guestName = guestName,
                        experience = guestExperience,
                        entertainmentApps = managedEntertainmentApps,
                        onLive = { section = AppSection.Live },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (section == AppSection.Guide) {
                    GuideSection(
                        channels = ordered,
                        guide = guide,
                        onWatch = {
                            tuneChannel(it)
                            section = AppSection.Live
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (section == AppSection.Search) {
                    SearchSection(
                        channels = searchResults,
                        query = query,
                        onQuery = { query = it },
                        onWatch = {
                            tuneChannel(it)
                            section = AppSection.Live
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ChannelPane(
                        ordered, selected, favorites,
                        onSelect = tuneChannel,
                        onFavorite = { channel ->
                            favorites = toggleFavorite(favorites, channel.id)
                            prefs.edit().putStringSet(FAVORITES, favorites).apply()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                }
                FloatingNavigation(
                    section = section,
                    onSection = { section = it },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
                )
            }
        }
    }

    if (showSettingsPin) {
        SettingsPinDialog(
            onDismiss = { showSettingsPin = false },
            onUnlocked = {
                showSettingsPin = false
                showSettings = true
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            playlist = prefs.getString(PLAYLIST_URL, DEFAULT_PLAYLIST_URL)
                .orEmpty().ifBlank { DEFAULT_PLAYLIST_URL },
            epg = prefs.getString(EPG_URL, DEFAULT_EPG_URL)
                .orEmpty().ifBlank { DEFAULT_EPG_URL },
            headers = prefs.getString(REQUEST_HEADERS, "").orEmpty(),
            weatherLocation = weatherLocation,
            guestName = guestName,
            themeMode = themeMode,
            captionsEnabled = captionsEnabled,
            captionLanguage = captionLanguage,
            autoUpdate = prefs.getBoolean(AUTO_UPDATE_CHECK, true),
            wifiOnly = prefs.getBoolean(WIFI_ONLY_UPDATES, true),
            autoStart = prefs.getBoolean(AUTO_START, false),
            resumeLast = prefs.getBoolean(RESUME_LAST_CHANNEL, true),
            startDestination = prefs.getString(START_DESTINATION, AppSection.Home.name)
                ?: AppSection.Home.name,
            sourceStatus = status,
            hubStatus = hubStatus,
            onCheckForUpdate = { checkForAppUpdate() },
            onBeginHubEnrollment = {
                withContext(Dispatchers.IO) {
                    GlzHubManager.beginEnrollment(prefs, client)
                }.also { code ->
                    hubStatus = "Pairing code: $code"
                }
            },
            onDismiss = { showSettings = false },
            onSave = { playlist, epg, headers, location, name, theme, captions, language, autoUpdate,
                       wifiOnly, autoStart, resumeLast, startDestination ->
                prefs.edit().putString(PLAYLIST_URL, playlist).putString(EPG_URL, epg)
                    .putString(REQUEST_HEADERS, headers)
                    .putString(WEATHER_LOCATION, location)
                    .putString(GUEST_NAME, name)
                    .putBoolean(CAPTIONS_ENABLED, captions)
                    .putString(CAPTION_LANGUAGE, language)
                    .putBoolean(AUTO_UPDATE_CHECK, autoUpdate)
                    .putBoolean(WIFI_ONLY_UPDATES, wifiOnly)
                    .putBoolean(AUTO_START, autoStart)
                    .putBoolean(RESUME_LAST_CHANNEL, resumeLast)
                    .putString(START_DESTINATION, startDestination).apply()
                onThemeMode(theme)
                captionsEnabled = captions
                captionLanguage = language
                weatherLocation = location
                guestName = name
                showSettings = false
                scope.launch { loadSources() }
            }
        )
    }

    availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = {
                if (!updateDownloading) {
                    availableUpdate = null
                    updateDownloadStatus = null
                }
            },
            icon = { Icon(Icons.Default.Refresh, null) },
            title = { Text("GLZ TV ${update.version} is available") },
            text = {
                Column {
                    Text(
                        update.notes.ifBlank {
                            "A new version is ready from the official GLZ TV GitHub release."
                        },
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                    updateDownloadStatus?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !updateDownloading,
                    onClick = {
                        if (!GithubUpdateManager.canInstall(context)) {
                            updateDownloadStatus =
                                "Allow GLZ TV to install unknown apps, then choose Install again."
                            GithubUpdateManager.requestInstallPermission(context)
                        } else {
                            updateDownloading = true
                            updateDownloadStatus = "Downloading update…"
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        GithubUpdateManager.download(context, client, update)
                                    }
                                }.onSuccess { apk ->
                                    updateDownloadStatus = "Opening system installer…"
                                    GithubUpdateManager.launchInstaller(context, apk)
                                }.onFailure {
                                    updateDownloadStatus = "Download failed: ${it.message}"
                                }
                                updateDownloading = false
                            }
                        }
                    }
                ) { Text(if (updateDownloading) "Downloading…" else "Install") }
            },
            dismissButton = {
                Button(
                    enabled = !updateDownloading,
                    onClick = {
                        availableUpdate = null
                        updateDownloadStatus = null
                    }
                ) { Text("Later") }
            }
        )
    }
}

@Composable
private fun SlimHeader(
    loading: Boolean,
    contentLoaded: Boolean,
    weather: WeatherInfo?,
    networkInfo: NetworkInfo?,
    onWeatherClick: (() -> Unit)?,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LiveTv,
                "Glz TV",
                Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Glz TV", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Guest entertainment hub", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            if (contentLoaded) {
                networkInfo?.let {
                    Column(
                        Modifier.padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("Wi-Fi  ${it.wifiName}", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        Text(it.isp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                weather?.let {
                    Column(
                        Modifier
                            .clickable(
                                enabled = onWeatherClick != null,
                                onClick = { onWeatherClick?.invoke() }
                            )
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "${weatherSymbol(it.weatherCode)}  ${it.temperature}°",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            it.location,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Column(
                    Modifier.padding(horizontal = 14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now)),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(now)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            IconButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
            FilledIconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Settings")
            }
        }
    }
}

@Composable
private fun FloatingNavigation(
    section: AppSection,
    onSection: (AppSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 16.dp,
        tonalElevation = 8.dp
    ) {
        Row(
            Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingDestination("Home", section == AppSection.Home, Icons.Default.Home) {
                onSection(AppSection.Home)
            }
            FloatingDestination("Live", section == AppSection.Live, Icons.Default.LiveTv) {
                onSection(AppSection.Live)
            }
            FloatingDestination("Guide", section == AppSection.Guide, Icons.Default.CalendarMonth) {
                onSection(AppSection.Guide)
            }
            FloatingDestination("Search", section == AppSection.Search, Icons.Default.Search) {
                onSection(AppSection.Search)
            }
        }
    }
}

@Composable
private fun FloatingDestination(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
        shape = shape,
        border = BorderStroke(
            if (focused) 4.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.secondary else Color.Transparent
        ),
        color = if (focused) MaterialTheme.colorScheme.primary
        else if (selected) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (focused) MaterialTheme.colorScheme.onPrimary
        else if (selected) MaterialTheme.colorScheme.onSecondary
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, label)
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun GuestHubHome(
    guestName: String,
    experience: GuestExperience,
    entertainmentApps: List<EntertainmentApp>,
    onLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = .20f),
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.background
                )
            )
        ).padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        val guestHeight = (maxHeight * .30f).coerceIn(105.dp, 145.dp)
        val appHeight = (maxHeight * .22f).coerceIn(78.dp, 105.dp)
        val serviceHeight = (maxHeight * .19f).coerceIn(72.dp, 92.dp)
        val appWidth = appHeight * 1.9f
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
            Modifier.fillMaxWidth().height(guestHeight),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                ).padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                experience.heroImageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .52f)))
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        experience.logoUrl?.let { logoUrl ->
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = experience.propertyName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.height(34.dp).width(120.dp)
                            )
                        }
                        Text("WELCOME", color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black)
                        Text("Welcome, ${guestName.ifBlank { "Guest" }}",
                            fontSize = 32.sp, fontWeight = FontWeight.Black)
                        Text(experience.welcomeMessage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 18.sp)
                        val stayLine = listOfNotNull(
                            experience.propertyName.takeIf(String::isNotBlank),
                            experience.roomNumber?.let { "Room $it" },
                            experience.checkoutTime?.let { "Checkout $it" }
                        ).joinToString("  •  ")
                        if (stayLine.isNotBlank()) Text(stayLine, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
            HubSectionTitle("ENTERTAINMENT", "Live TV, apps and services")
            LazyRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 8.dp)
            ) {
                item { LiveTvHubCard(onLive, appWidth, appHeight) }
                items(entertainmentApps, key = EntertainmentApp::packageName) { app ->
                    EntertainmentAppCard(app, appWidth, appHeight)
                }
            }
            val serviceItems = buildList {
                experience.noticeTitle?.let {
                    add(GuestService(it, experience.noticeBody, null))
                }
                experience.wifiName?.let {
                    add(GuestService("Wi-Fi · $it", experience.wifiInstructions, null))
                }
                experience.frontDesk?.let {
                    add(GuestService("Front Desk", it, null))
                }
                addAll(experience.services)
            }
            if (serviceItems.isNotEmpty()) {
                HubSectionTitle("YOUR STAY", experience.noticeTitle ?: "Hotel information and services")
                LazyRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp)
                ) {
                    items(serviceItems, key = { "${it.title}-${it.actionUrl}" }) { service ->
                        GuestServiceCard(service, appWidth, serviceHeight)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestServiceCard(service: GuestService, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = {
            service.actionUrl?.let { url ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        },
        modifier = Modifier.width(width).height(height).onFocusChanged { focused = it.isFocused },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (focused) 4.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = .08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(service.title, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            service.subtitle?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PremiumHero(
    channel: Channel,
    programme: Programme?,
    onWatch: () -> Unit,
    onGuide: () -> Unit,
    onFocused: () -> Unit
) {
    val now = System.currentTimeMillis()
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
                ChannelLogo(channel, 112.dp)
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
private fun HubSectionTitle(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LiveHubCard(
    channel: Channel,
    programmes: List<Programme>,
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
            ChannelLogo(channel, 52.dp)
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

@Composable
private fun LiveTvHubCard(
    onClick: () -> Unit,
    width: androidx.compose.ui.unit.Dp = 190.dp,
    height: androidx.compose.ui.unit.Dp = 100.dp
) {
    PremiumFocusCard(
        modifier = Modifier.width(width).height(height),
        onClick = onClick,
        accent = MaterialTheme.colorScheme.primary
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LiveTv, "Live TV", Modifier.size(34.dp), tint = Color.White)
                Spacer(Modifier.height(2.dp))
                Text("Live TV", color = Color.White, fontSize = 18.sp,
                    fontWeight = FontWeight.Black)
                Text("Channels & guide", color = Color.White.copy(alpha = .72f),
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EntertainmentAppCard(
    app: EntertainmentApp,
    width: androidx.compose.ui.unit.Dp = 190.dp,
    height: androidx.compose.ui.unit.Dp = 100.dp
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val launchIntent = remember(app.packageName) {
        findAppLaunchIntent(context, app.packageName)
    }
    val installedIcon = remember(app.packageName, launchIntent) {
        if (launchIntent != null) runCatching {
            packageManager.getApplicationIcon(app.packageName)
        }.getOrNull() else null
    }
    val appNameSize = when {
        app.name.length <= 9 -> 16.sp
        app.name.length <= 12 -> 14.sp
        else -> 12.sp
    }
    PremiumFocusCard(
        modifier = Modifier.width(width).height(height),
        onClick = { launchEntertainmentApp(context, app.packageName, launchIntent) },
        accent = app.accent
    ) {
        Row(
            Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdaptiveAppIcon(
                icon = installedIcon,
                appName = app.name,
                accent = app.accent,
                size = 52.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.name,
                    fontSize = appNameSize,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
                Text(
                    if (launchIntent != null) "Open" else "Install",
                    color = if (launchIntent != null) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun AdaptiveAppIcon(
    icon: Drawable?,
    appName: String,
    accent: Color,
    size: androidx.compose.ui.unit.Dp
) {
    val isAdaptive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        icon is AdaptiveIconDrawable
    val shape = RoundedCornerShape(18.dp)
    Surface(
        Modifier.size(size),
        shape = shape,
        color = if (isAdaptive) Color.Transparent else Color.White
    ) {
        when {
            icon != null -> AsyncImage(
                model = icon,
                contentDescription = appName,
                modifier = Modifier.fillMaxSize()
                    .clip(shape)
                    .then(if (isAdaptive) Modifier else Modifier.padding(8.dp)),
                contentScale = if (isAdaptive) ContentScale.Crop else ContentScale.Fit
            )
            else -> Box(
                Modifier.fillMaxSize().background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(appName.take(2).uppercase(), color = Color.White,
                    fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PremiumFocusCard(
    modifier: Modifier,
    onClick: () -> Unit,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
        shape = shape,
        border = BorderStroke(
            if (focused) 5.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.secondary
            else Color.White.copy(alpha = .08f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (focused) MaterialTheme.colorScheme.surfaceContainerHighest
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize(), content = content)
        }
    }
}

private fun launchEntertainmentApp(context: Context, packageName: String, launchIntent: Intent?) {
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

private suspend fun handleManagedAppCommand(
    context: Context,
    prefs: SharedPreferences,
    client: OkHttpClient,
    command: GlzHubManager.AppCommand
) {
    val result = runCatching {
        val uri = if (command.sourceType == "repository") {
            Uri.parse(requireNotNull(command.sourceUrl) { "Repository URL is missing" })
        } else {
            Uri.parse("market://details?id=${command.packageName}")
        }
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    withContext(Dispatchers.IO) {
        runCatching {
            GlzHubManager.completeCommand(
                prefs, client, command.id, result.isSuccess,
                if (result.isSuccess) "Installer opened on TV" else (result.exceptionOrNull()?.message ?: "Could not open installer")
            )
        }
    }
}

private fun findAppLaunchIntent(context: Context, packageName: String): Intent? {
    val packageManager = context.packageManager
    return packageManager.getLeanbackLaunchIntentForPackage(packageName)
        ?: packageManager.getLaunchIntentForPackage(packageName)
        ?: Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            .setPackage(packageName)
            .takeIf { it.resolveActivity(packageManager) != null }
}

@Composable
private fun SearchSection(
    channels: List<Channel>,
    query: String,
    onQuery: (String) -> Unit,
    onWatch: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search channels, groups, or numbers") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardActions = KeyboardActions.Default
            )
            Text(
                if (query.isBlank()) "All channels" else "${channels.size} results",
                Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(channels, key = { "search-${it.streamUrl}" }) { channel ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onWatch(channel) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChannelLogo(channel, 50.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(channel.name, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                Text(
                                    "${channel.number} · ${channel.group}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun GuideSection(
    channels: List<Channel>,
    guide: EpgGuide,
    onWatch: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    Card(
        modifier,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            if (guide.programmeCount == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            null,
                            Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Guide data is unavailable", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Refresh the sources or check the XMLTV address.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                EpgGrid(channels, guide, now, onWatch, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun EpgGrid(
    channels: List<Channel>,
    guide: EpgGuide,
    now: Long,
    onWatch: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val halfHour = 30 * 60 * 1000L
    val start = now - (now % halfHour)
    val slots = 8
    val slotWidth = 150.dp
    val channelWidth = 210.dp
    val timelineWidth = slotWidth * slots
    val totalWidth = channelWidth + timelineWidth
    val horizontal = rememberScrollState()

    Column(modifier.horizontalScroll(horizontal)) {
        Column(Modifier.width(totalWidth).fillMaxHeight()) {
            Row(
                Modifier.height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            ) {
                Box(
                    Modifier.width(channelWidth).fillMaxHeight().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("CHANNEL", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary)
                }
                repeat(slots) { slot ->
                    Box(
                        Modifier.width(slotWidth).fillMaxHeight()
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            DateFormat.getTimeInstance(DateFormat.SHORT)
                                .format(Date(start + slot * halfHour)),
                            Modifier.padding(start = 12.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(channels, key = { "grid-${it.streamUrl}" }) { channel ->
                    EpgGridRow(
                        channel = channel,
                        programmes = guide.forChannel(channel),
                        windowStart = start,
                        windowEnd = start + slots * halfHour,
                        channelWidth = channelWidth,
                        timelineWidth = timelineWidth,
                        slotWidth = slotWidth,
                        onWatch = { onWatch(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpgGridRow(
    channel: Channel,
    programmes: List<Programme>,
    windowStart: Long,
    windowEnd: Long,
    channelWidth: androidx.compose.ui.unit.Dp,
    timelineWidth: androidx.compose.ui.unit.Dp,
    slotWidth: androidx.compose.ui.unit.Dp,
    onWatch: () -> Unit
) {
    val halfHour = 30 * 60 * 1000f
    val visible = programmes.filter { it.endMillis > windowStart && it.startMillis < windowEnd }
    Row(
        Modifier.height(82.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier.width(channelWidth).fillMaxHeight().clickable(onClick = onWatch)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(channel, 46.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    channel.number.ifBlank { "TV" },
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Black
                )
                Text(channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
        Box(
            Modifier.width(timelineWidth).fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
        ) {
            repeat(8) { slot ->
                Box(
                    Modifier.offset(x = slotWidth * slot).width(slotWidth).fillMaxHeight()
                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = .18f))
                )
            }
            visible.forEach { programme ->
                val clippedStart = maxOf(programme.startMillis, windowStart)
                val clippedEnd = minOf(programme.endMillis, windowEnd)
                val x = slotWidth * ((clippedStart - windowStart) / halfHour)
                val width = slotWidth * ((clippedEnd - clippedStart) / halfHour)
                Surface(
                    modifier = Modifier.offset(x = x).width(maxOf(width, 52.dp))
                        .fillMaxHeight().padding(3.dp).clickable(onClick = onWatch),
                    shape = RoundedCornerShape(13.dp),
                    color = if (programme.startMillis <= System.currentTimeMillis() &&
                        programme.endMillis > System.currentTimeMillis()
                    ) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(programme.title, fontWeight = FontWeight.Bold, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            DateFormat.getTimeInstance(DateFormat.SHORT)
                                .format(Date(programme.startMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelLogo(channel: Channel, size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = Color.White
    ) {
        AsyncImage(
            model = channel.logoUrl.takeIf { it.isNotBlank() },
            contentDescription = "${channel.name} logo",
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_launcher),
            error = painterResource(R.drawable.ic_launcher),
            fallback = painterResource(R.drawable.ic_launcher)
        )
    }
}

@Composable
private fun ChannelPane(
    channels: List<Channel>,
    selected: Channel?,
    favorites: Set<String>,
    onSelect: (Channel) -> Unit,
    onFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier, shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Text("${channels.size} channels", modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channels, key = { it.streamUrl }) { channel ->
                    ChannelCard(channel, channel == selected, channel.id in favorites,
                        { onSelect(channel) }, { onFavorite(channel) })
                }
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel, selected: Boolean, favorite: Boolean,
    onClick: () -> Unit, onFavorite: () -> Unit
) {
    var focused by remember(channel.id) { mutableStateOf(false) }
    val container = if (focused) MaterialTheme.colorScheme.surfaceContainerHighest
    else if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    Card(
        Modifier.fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            if (focused) 5.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.secondary else Color.Transparent
        ),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(channel, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(channel.name, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(
                    "${channel.number.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()}${channel.group}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorite", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ImmersivePlayerScreen(
    channel: Channel,
    channels: List<Channel>,
    guide: EpgGuide,
    captionsEnabled: Boolean,
    captionLanguage: String,
    entertainmentApps: List<EntertainmentApp>,
    onTune: (Channel) -> Unit,
    onExit: () -> Unit
) {
    var drawer by remember { mutableStateOf(PlayerDrawer.None) }
    var showNavigationTip by remember { mutableStateOf(true) }
    var showOsd by remember { mutableStateOf(false) }
    val playerFocus = remember { FocusRequester() }
    val selectedChannelFocus = remember { FocusRequester() }
    val firstServiceFocus = remember { FocusRequester() }
    val selectedIndex = channels.indexOfFirst { it.id == channel.id }.coerceAtLeast(0)
    val channelListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - 2).coerceAtLeast(0)
    )
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val channelProgrammes = guide.forChannel(channel)
    val currentProgramme = channelProgrammes
        .firstOrNull { it.startMillis <= now && it.endMillis > now }
    val nextProgramme = channelProgrammes
        .firstOrNull { it.startMillis >= (currentProgramme?.endMillis ?: now) }

    LaunchedEffect(Unit) {
        delay(5_000)
        showNavigationTip = false
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(showOsd, channel.id) {
        if (showOsd) {
            delay(6_000)
            showOsd = false
        }
    }

    fun stepChannel(direction: Int) {
        val currentIndex = channels.indexOfFirst { it.id == channel.id }
        if (currentIndex >= 0 && channels.isNotEmpty()) {
            onTune(channels[(currentIndex + direction + channels.size) % channels.size])
        }
    }

    BackHandler {
        if (drawer != PlayerDrawer.None) drawer = PlayerDrawer.None else onExit()
    }
    LaunchedEffect(channel.id, drawer) {
        when (drawer) {
            PlayerDrawer.None -> playerFocus.requestFocus()
            PlayerDrawer.Channels -> {
                channelListState.scrollToItem((selectedIndex - 2).coerceAtLeast(0))
                delay(60)
                selectedChannelFocus.requestFocus()
            }
            PlayerDrawer.Services -> {
                delay(60)
                firstServiceFocus.requestFocus()
            }
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        showNavigationTip = false
                        drawer = PlayerDrawer.Channels
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        showNavigationTip = false
                        drawer = PlayerDrawer.Services
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (drawer == PlayerDrawer.None) {
                            showNavigationTip = false
                            showOsd = !showOsd
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_CHANNEL_UP -> {
                        stepChannel(-1)
                        true
                    }
                    KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        stepChannel(1)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (drawer == PlayerDrawer.None) {
                            stepChannel(-1)
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (drawer == PlayerDrawer.None) {
                            stepChannel(1)
                            true
                        } else false
                    }
                    else -> false
                }
            }
    ) {
        VideoPlayer(channel, captionsEnabled, captionLanguage, Modifier.fillMaxSize())

        if (drawer == PlayerDrawer.None && showOsd) {
            PlayerOsd(
                channel = channel,
                currentProgramme = currentProgramme,
                nextProgramme = nextProgramme,
                now = now,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (drawer == PlayerDrawer.None && showNavigationTip) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(24.dp),
                color = Color.Black.copy(alpha = .58f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "◀ Channels                         Services ▶",
                    Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    color = Color.White.copy(alpha = .86f),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (drawer == PlayerDrawer.Channels) {
            val drawerWidth = if (maxWidth < 520.dp) maxWidth * .88f else 420.dp
            Surface(
                Modifier.width(drawerWidth).fillMaxHeight(),
                color = Color(0xF20B1114),
                tonalElevation = 18.dp,
                shadowElevation = 24.dp
            ) {
                Column(Modifier.fillMaxSize().padding(top = 22.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelLogo(channel, 66.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(channel.number.ifBlank { "LIVE" },
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Black)
                            Text(channel.name, color = Color.White, fontSize = 24.sp,
                                fontWeight = FontWeight.Black, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Surface(
                        Modifier.fillMaxWidth().padding(22.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .18f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("ON NOW", color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black)
                            Text(currentProgramme?.title ?: "Live programming",
                                color = Color.White, fontSize = 19.sp,
                                fontWeight = FontWeight.Bold, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            currentProgramme?.let {
                                Text(
                                    "${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.startMillis))} – " +
                                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.endMillis)),
                                    color = Color.White.copy(alpha = .68f)
                                )
                            }
                        }
                    }
                    Text("CHANNELS", Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
                        color = Color.White.copy(alpha = .56f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black)
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        state = channelListState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(channels, key = { "drawer-${it.streamUrl}" }) { item ->
                            val isSelected = item.id == channel.id
                            var isFocused by remember(item.id) { mutableStateOf(false) }
                            val itemProgramme = guide.forChannel(item)
                                .firstOrNull { it.startMillis <= now && it.endMillis > now }
                            Surface(
                                Modifier.fillMaxWidth()
                                    .then(if (isSelected) Modifier.focusRequester(selectedChannelFocus)
                                    else Modifier)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .clickable {
                                        onTune(item)
                                        drawer = PlayerDrawer.None
                                    }
                                    .focusable(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    if (isFocused) 5.dp else 1.dp,
                                    if (isFocused) MaterialTheme.colorScheme.secondary
                                    else Color.Transparent
                                ),
                                color = if (isFocused)
                                    MaterialTheme.colorScheme.primary
                                else if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = .35f)
                                else Color.Transparent
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    ChannelLogo(item, 42.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(item.number.ifBlank { "—" }, Modifier.width(45.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary
                                        else Color.White.copy(alpha = .62f),
                                        fontWeight = FontWeight.Black)
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, color = Color.White,
                                            fontWeight = FontWeight.Bold, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Text(itemProgramme?.title ?: "Guide unavailable",
                                            color = Color.White.copy(alpha = .58f),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (drawer == PlayerDrawer.Services) {
            val context = LocalContext.current
            val drawerWidth = if (maxWidth < 520.dp) maxWidth * .88f else 390.dp
            Surface(
                Modifier.width(drawerWidth).fillMaxHeight().align(Alignment.CenterEnd),
                color = Color(0xF20B1114),
                tonalElevation = 18.dp,
                shadowElevation = 24.dp
            ) {
                Column(Modifier.fillMaxSize().padding(22.dp)) {
                    Text("ENTERTAINMENT", color = Color.White, fontSize = 26.sp,
                        fontWeight = FontWeight.Black)
                    Text("Choose another service",
                        color = Color.White.copy(alpha = .6f))
                    Spacer(Modifier.height(20.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(entertainmentApps, key = { "service-${it.packageName}" }) { app ->
                            val launchIntent = remember(app.packageName) {
                                findAppLaunchIntent(context, app.packageName)
                            }
                            val icon = remember(app.packageName, launchIntent) {
                                if (launchIntent == null) null else runCatching {
                                    context.packageManager.getApplicationIcon(app.packageName)
                                }.getOrNull()
                            }
                            var isFocused by remember(app.packageName) { mutableStateOf(false) }
                            val isFirst = app == entertainmentApps.firstOrNull()
                            Surface(
                                Modifier.fillMaxWidth()
                                    .then(if (isFirst) Modifier.focusRequester(firstServiceFocus)
                                    else Modifier)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .clickable {
                                        launchEntertainmentApp(context, app.packageName, launchIntent)
                                    }
                                    .focusable(),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(
                                    if (isFocused) 5.dp else 1.dp,
                                    if (isFocused) MaterialTheme.colorScheme.secondary
                                    else Color.Transparent
                                ),
                                color = if (isFocused) app.accent
                                else Color.White.copy(alpha = .08f),
                                shadowElevation = 0.dp
                            ) {
                                Row(Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    AdaptiveAppIcon(
                                        icon = icon,
                                        appName = app.name,
                                        accent = app.accent,
                                        size = 54.dp
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text(app.name, color = Color.White, fontSize = 18.sp,
                                            fontWeight = FontWeight.Black)
                                        Text(if (launchIntent == null) "Install" else "Open",
                                            color = Color.White.copy(alpha = .7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerOsd(
    channel: Channel,
    currentProgramme: Programme?,
    nextProgramme: Programme?,
    now: Long,
    modifier: Modifier = Modifier
) {
    val remainingMinutes = currentProgramme?.let {
        ((it.endMillis - now).coerceAtLeast(0L) + 59_999L) / 60_000L
    }
    val progress = currentProgramme?.let {
        ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L))
            .coerceIn(0f, 1f)
    } ?: 0f

    Surface(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0B0F12),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .18f)),
        shadowElevation = 24.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(channel, 82.dp)
            Spacer(Modifier.width(22.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(channel.number.ifBlank { "LIVE" }, color = MaterialTheme.colorScheme.secondary,
                        fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(12.dp))
                    Text(channel.name, color = Color.White, fontSize = 25.sp,
                        fontWeight = FontWeight.Black, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(currentProgramme?.title ?: "Live programming",
                        Modifier.weight(1f), color = Color.White, fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    remainingMinutes?.let {
                        Text("$it min remaining", color = Color.White.copy(alpha = .74f),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                currentProgramme?.let {
                    Text(
                        "${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.startMillis))} – " +
                            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.endMillis)),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (it.description.isNotBlank()) {
                        Text(
                            it.description,
                            Modifier.padding(top = 5.dp),
                            color = Color.White.copy(alpha = .78f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(4.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = Color.White.copy(alpha = .18f)
                )
                nextProgramme?.let {
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "NEXT  ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.startMillis))}",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            it.title,
                            color = Color.White.copy(alpha = .75f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerAndGuide(
    channel: Channel?,
    guide: EpgGuide,
    captionsEnabled: Boolean,
    captionLanguage: String,
    modifier: Modifier = Modifier
) {
    if (channel == null) {
        Card(modifier, shape = RoundedCornerShape(32.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LiveTv, null, Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text("Choose a channel", fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("Live playback and the programme guide will appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }
    val programmes = guide.forChannel(channel)
    val now = System.currentTimeMillis()
    val upcoming = programmes.filter { it.endMillis > now }.take(6)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedContent(channel, transitionSpec = {
            (fadeIn(spring()) + scaleIn(initialScale = .96f)).togetherWith(fadeOut())
        }, label = "channel") { animatedChannel ->
            VideoPlayer(
                animatedChannel,
                captionsEnabled,
                captionLanguage,
                Modifier.fillMaxWidth().aspectRatio(16 / 9f)
            )
        }
        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Text(channel.name, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(channel.group, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("ON NOW & NEXT", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary)
                if (upcoming.isEmpty()) {
                    Text("No EPG information is available for this channel.",
                        Modifier.padding(top = 14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(upcoming) { programme -> ProgrammeCard(programme, programme.startMillis <= now) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgrammeCard(programme: Programme, isNow: Boolean) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isNow) 22.dp else 14.dp),
        color = if (isNow) MaterialTheme.colorScheme.secondary.copy(alpha = .14f)
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Text(
                if (isNow) "NOW" else DateFormat.getTimeInstance(DateFormat.SHORT)
                    .format(Date(programme.startMillis)),
                Modifier.width(62.dp), color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black
            )
            Column {
                Text(programme.title, fontWeight = FontWeight.Bold)
                if (programme.description.isNotBlank()) {
                    Text(programme.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun VideoPlayer(
    channel: Channel,
    captionsEnabled: Boolean,
    captionLanguage: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val httpFactory = remember {
        DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
    }
    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
    }
    val mediaSession = remember {
        MediaSession.Builder(context, player).build()
    }
    var retryAttempt by remember(channel.id) { mutableStateOf(0) }
    var playbackMessage by remember(channel.id) { mutableStateOf<String?>(null) }
    DisposableEffect(player, channel.id) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (retryAttempt < 3) {
                    retryAttempt += 1
                    playbackMessage = "Reconnecting… attempt $retryAttempt of 3"
                } else {
                    playbackMessage = "This channel is temporarily unavailable"
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) playbackMessage = null
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(channel.streamUrl, captionsEnabled, captionLanguage, retryAttempt) {
        if (retryAttempt > 0) delay((1L shl (retryAttempt - 1)) * 1_000L)
        httpFactory
            .setDefaultRequestProperties(channel.headers)
            .setUserAgent(channel.headers["User-Agent"] ?: "GLZ-TV/2.0")
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsEnabled)
            .setPreferredTextLanguage(captionLanguage.ifBlank { null })
            .build()
        player.stop()
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(channel.streamUrl)
                .setMediaId(channel.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(channel.name)
                        .setArtist(channel.group)
                        .setArtworkUri(channel.logoUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
                        .build()
                )
                .build(),
            true
        )
        player.prepare()
        player.playWhenReady = true
    }
    DisposableEffect(player, mediaSession) {
        onDispose {
            mediaSession.release()
            player.release()
        }
    }
    Box(modifier.background(Color.Black, RoundedCornerShape(24.dp))) {
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player; keepScreenOn = true } },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )
        playbackMessage?.let { message ->
            Surface(
                color = Color.Black.copy(alpha = .82f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (retryAttempt in 1..3) LinearProgressIndicator(Modifier.width(180.dp))
                    Text(message, Modifier.padding(top = 10.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsPinDialog(
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Settings, null) },
        title = { Text("Protected settings", fontWeight = FontWeight.Black) },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter(Char::isDigit).take(4)
                    invalid = false
                },
                label = { Text("Settings PIN") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = invalid,
                supportingText = {
                    Text(if (invalid) "Incorrect PIN" else "Enter the 4-digit administrator PIN")
                }
            )
        },
        confirmButton = {
            Button(onClick = {
                if (pin == "1989") onUnlocked() else invalid = true
            }) { Text("Unlock") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun SettingsDialog(
    playlist: String, epg: String, headers: String,
    weatherLocation: String,
    guestName: String,
    themeMode: String,
    captionsEnabled: Boolean,
    captionLanguage: String,
    autoUpdate: Boolean,
    wifiOnly: Boolean,
    autoStart: Boolean,
    resumeLast: Boolean,
    startDestination: String,
    sourceStatus: String,
    hubStatus: String,
    onCheckForUpdate: suspend () -> String,
    onBeginHubEnrollment: suspend () -> String,
    onDismiss: () -> Unit,
    onSave: (
        String, String, String, String, String, String, Boolean, String, Boolean, Boolean,
        Boolean, Boolean, String
    ) -> Unit
) {
    var playlistValue by remember { mutableStateOf(playlist) }
    var epgValue by remember { mutableStateOf(epg) }
    var headerValue by remember { mutableStateOf(headers) }
    var weatherLocationValue by remember { mutableStateOf(weatherLocation) }
    var guestNameValue by remember { mutableStateOf(guestName) }
    var themeValue by remember { mutableStateOf(themeMode) }
    var captionsValue by remember { mutableStateOf(captionsEnabled) }
    var languageValue by remember { mutableStateOf(captionLanguage) }
    var autoUpdateValue by remember { mutableStateOf(autoUpdate) }
    var wifiOnlyValue by remember { mutableStateOf(wifiOnly) }
    var autoStartValue by remember { mutableStateOf(autoStart) }
    var resumeLastValue by remember { mutableStateOf(resumeLast) }
    var startDestinationValue by remember { mutableStateOf(startDestination) }
    var updateStatus by remember { mutableStateOf("Version ${BuildConfig.VERSION_NAME}") }
    var hubMessage by remember(hubStatus) { mutableStateOf(hubStatus) }
    var hubLoading by remember { mutableStateOf(false) }
    val settingsScope = rememberCoroutineScope()
    BackHandler(onBack = onDismiss)
    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 42.dp, vertical = 24.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, null, Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("Glz TV guest hub configuration",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TvSettingsButton("Back", onDismiss)
            }
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsLabel("SOURCE STATUS")
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        sourceStatus,
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SettingsLabel("GLZ HUB")
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(hubMessage, fontWeight = FontWeight.Bold)
                        Text(
                            "Manage this television at glzhub.glztech.com/pair",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        TvSettingsButton(
                            label = if (hubLoading) "Connecting…" else "Generate pairing code",
                            enabled = !hubLoading,
                            onClick = {
                                hubLoading = true
                                settingsScope.launch {
                                    runCatching { onBeginHubEnrollment() }
                                        .onSuccess { hubMessage = "Pairing code: $it · expires in 15 minutes" }
                                        .onFailure { hubMessage = "Could not reach GLZ Hub: ${it.message}" }
                                    hubLoading = false
                                }
                            }
                        )
                    }
                }
                SettingsLabel("APPEARANCE")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("adaptive" to "Adaptive", "dark" to "Dark", "light" to "Light")
                        .forEach { (value, label) ->
                            var focused by remember(value) { mutableStateOf(false) }
                            Surface(
                                Modifier
                                    .onFocusChanged { focused = it.isFocused }
                                    .clickable { themeValue = value }
                                    .focusable(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    if (focused) 4.dp else 1.dp,
                                    if (focused) MaterialTheme.colorScheme.secondary
                                    else Color.Transparent
                                ),
                                color = if (themeValue == value) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (themeValue == value) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(label, Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                }
                ProtectedSourceField(guestNameValue, { guestNameValue = it },
                    "Welcome guest name", "Shown in the Home welcome message")
                ProtectedSourceField(weatherLocationValue, { weatherLocationValue = it },
                    "Weather location", "City or municipality used by Open-Meteo")
                SettingsLabel("CLOSED CAPTIONS")
                SettingsToggle("Enable captions", captionsValue) { captionsValue = it }
                ProtectedSourceField(languageValue, { languageValue = it },
                    "Preferred language code", "Examples: en, es, fr",
                    enabled = captionsValue)
                SettingsLabel("APP UPDATES")
                SettingsToggle("Check automatically", autoUpdateValue) { autoUpdateValue = it }
                SettingsToggle("Download on Wi-Fi only", wifiOnlyValue) { wifiOnlyValue = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(updateStatus, Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TvSettingsButton("Check now", onClick = {
                        updateStatus = "Checking GitHub…"
                        settingsScope.launch {
                            updateStatus = onCheckForUpdate()
                        }
                    })
                }
                SettingsLabel("STARTUP")
                SettingsToggle("Open after device restart", autoStartValue) {
                    autoStartValue = it
                }
                SettingsToggle("Resume last channel", resumeLastValue) {
                    resumeLastValue = it
                }
                Text("Start destination", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(AppSection.Home, AppSection.Live, AppSection.Guide).forEach { destination ->
                        var focused by remember(destination) { mutableStateOf(false) }
                        Surface(
                            Modifier
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { startDestinationValue = destination.name }
                                .focusable(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                if (focused) 4.dp else 1.dp,
                                if (focused) MaterialTheme.colorScheme.secondary
                                else Color.Transparent
                            ),
                            color = if (startDestinationValue == destination.name)
                                MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (startDestinationValue == destination.name)
                                MaterialTheme.colorScheme.onSecondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                destination.name,
                                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                SettingsLabel("TV SOURCES")
                ProtectedSourceField(
                    value = playlistValue,
                    onValueChange = { playlistValue = it },
                    label = "M3U playlist URL"
                )
                ProtectedSourceField(
                    value = epgValue,
                    onValueChange = { epgValue = it },
                    label = "XMLTV EPG URL"
                )
                ProtectedSourceField(headerValue, { headerValue = it },
                    "Request headers", "One Name: value header per line",
                    singleLine = false, minLines = 3)
                Spacer(Modifier.height(20.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TvSettingsButton("Cancel", onDismiss)
                Spacer(Modifier.width(12.dp))
                TvSettingsButton(
                    label = "Save & load",
                    onClick = {
                        onSave(
                            playlistValue.trim(), epgValue.trim(), headerValue.trim(),
                            weatherLocationValue.trim().ifBlank { DEFAULT_WEATHER_LOCATION },
                            guestNameValue.trim().ifBlank { "Guest" },
                            themeValue, captionsValue, languageValue.trim(),
                            autoUpdateValue, wifiOnlyValue, autoStartValue, resumeLastValue,
                            startDestinationValue
                        )
                    },
                    enabled = playlistValue.startsWith("http")
                )
            }
        }
    }
}

@Composable
private fun TvSettingsButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled),
        shape = RoundedCornerShape(16.dp),
        color = if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
        border = BorderStroke(
            if (focused) 4.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.secondary else Color.Transparent
        )
    ) {
        Text(label, Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ProtectedSourceField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    help: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    var editing by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(editing) {
        if (editing) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                if (focused) 4.dp else 0.dp,
                if (focused) MaterialTheme.colorScheme.secondary else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .then(if (!editing) Modifier.clickable { editing = true } else Modifier)
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) editing = false
            }
            .onPreviewKeyEvent { event ->
                val isSelect = event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode in listOf(
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER
                    )
                if (isSelect && !editing) {
                    editing = true
                    true
                } else false
            },
        label = { Text(label) },
        readOnly = !editing,
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            focusedLabelColor = MaterialTheme.colorScheme.secondary,
            cursorColor = MaterialTheme.colorScheme.secondary
        ),
        supportingText = {
            Text(
                when {
                    editing -> "Editing enabled"
                    focused -> "Press OK to edit${help.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}"
                    help.isNotBlank() -> help
                    else -> "Press OK to edit"
                }
            )
        }
    )
}

@Composable
private fun SettingsLabel(value: String) {
    Text(
        value,
        Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        Modifier.fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clickable { onChecked(!checked) }
            .focusable(),
        shape = RoundedCornerShape(14.dp),
        color = if (focused) MaterialTheme.colorScheme.surfaceContainerHighest
        else Color.Transparent,
        border = BorderStroke(
            if (focused) 4.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.secondary else Color.Transparent
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

private fun toggleFavorite(values: Set<String>, id: String) =
    if (id in values) values - id else values + id

private fun channelNumberValue(value: String): Double =
    value.trim().toDoubleOrNull()
        ?: Regex("\\d+(?:\\.\\d+)?").find(value)?.value?.toDoubleOrNull()
        ?: Double.MAX_VALUE

private fun parseHeaders(source: String): Map<String, String> = buildMap {
    source.lineSequence().forEach { line ->
        val separator = line.indexOf(':')
        if (separator > 0) put(line.take(separator).trim(), line.drop(separator + 1).trim())
    }
}

private fun fetchNetworkInfo(context: Context, client: OkHttpClient): NetworkInfo {
    val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
    val wifiName = when {
        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> runCatching {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.connectionInfo.ssid
                .takeUnless { it.isNullOrBlank() || it == WifiManager.UNKNOWN_SSID }
                ?.removeSurrounding("\"")
                ?: "Wi-Fi connected"
        }.getOrDefault("Wi-Fi connected")
        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
        else -> "Network"
    }
    val isp = runCatching {
        JSONObject(fetchText(client, "https://ipwho.is/", emptyMap()))
            .optJSONObject("connection")
            ?.optString("isp")
            ?.takeIf(String::isNotBlank)
    }.getOrNull() ?: "Internet connected"
    return NetworkInfo(wifiName, isp)
}

private fun fetchWeather(client: OkHttpClient, location: String): WeatherInfo? = runCatching {
    val geocoding = JSONObject(
        fetchText(
            client,
            "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=${Uri.encode(location)}&count=1&language=en&format=json",
            emptyMap()
        )
    )
    val place = geocoding.getJSONArray("results").getJSONObject(0)
    val latitude = place.getDouble("latitude")
    val longitude = place.getDouble("longitude")
    val displayName = place.optString("name", location)
    val forecast = JSONObject(
        fetchText(
            client,
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,weather_code&temperature_unit=fahrenheit",
            emptyMap()
        )
    ).getJSONObject("current")
    WeatherInfo(
        temperature = forecast.getDouble("temperature_2m").toInt(),
        weatherCode = forecast.getInt("weather_code"),
        location = displayName
    )
}.getOrNull()

private fun weatherSymbol(code: Int): String = when (code) {
    0 -> "☀"
    1, 2 -> "⛅"
    3 -> "☁"
    45, 48 -> "≋"
    in 51..67, in 80..82 -> "☂"
    in 71..77, 85, 86 -> "❄"
    in 95..99 -> "ϟ"
    else -> "°"
}

private fun fetchText(client: OkHttpClient, url: String, headers: Map<String, String>): String {
    val request = Request.Builder().url(url).apply {
        headers.forEach { (name, value) -> header(name, value) }
    }.build()
    client.newCall(request).execute().use {
        check(it.isSuccessful) { "HTTP ${it.code}" }
        val bytes = it.body?.bytes() ?: error("Empty response")
        if (bytes.isEmpty()) error("Empty response")
        val isGzip = bytes.size >= 2 &&
            bytes[0].toInt() and 0xff == 0x1f &&
            bytes[1].toInt() and 0xff == 0x8b
        return if (isGzip) {
            GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }
        } else {
            bytes.toString(Charsets.UTF_8)
        }
    }
}
