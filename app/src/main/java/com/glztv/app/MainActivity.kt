@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.glztv.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.KeyEvent
import android.view.WindowManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.SurfaceView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.session.MediaSession
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import com.glztv.app.data.EpgRepository
import com.glztv.app.data.PlaylistRepository
import com.glztv.app.data.PreferencesRepository
import com.glztv.app.data.WeatherRepository
import com.glztv.app.data.createPermissiveOkHttpClient
import com.glztv.app.player.RecentChannelManager
import com.glztv.app.player.PlaybackControlState
import com.glztv.app.player.PlaybackDiagnostics
import com.glztv.app.player.PlaybackErrorCategorizer
import com.glztv.app.player.PlaybackErrorCategory
import com.glztv.app.player.TrackOption
import com.glztv.app.player.TrackPreferenceManager
import com.glztv.app.player.PlaybackPerformance
import com.glztv.app.player.PlaybackDiagnosticsPanel
import com.glztv.app.ui.theme.GlzTheme
import com.glztv.app.ui.theme.AmbientBackground
import com.glztv.app.ui.components.tvFocusableWithPhysics
import com.glztv.app.ui.components.GlzFocusCard
import com.glztv.app.ui.components.GlzPanel
import com.glztv.app.ui.components.GlzCardDefaults
import com.glztv.app.ui.navigation.AppSection
import com.glztv.app.ui.navigation.ExpressiveNavigationRail
import com.glztv.app.ui.components.SlimHeader
import com.glztv.app.model.NetworkInfo
import com.glztv.app.model.WeatherInfo
import com.glztv.app.ui.GlzTvApp
import com.glztv.app.ui.screens.RadioScreen
import com.glztv.app.ui.screens.WeatherScreen
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
private const val CAPTION_LANGUAGE = "captions_language"
private const val LEGACY_CAPTION_LANGUAGE = "caption_language"
private const val KEEP_AWAKE_HOME = "keep_awake_home"
private const val HOME_PREVIEW_CHANNEL_ID = "home_preview_channel_id"
private const val AUTO_UPDATE_CHECK = "auto_update_check"
private const val WIFI_ONLY_UPDATES = "wifi_only_updates"
private const val AUTO_START = "auto_start"
private const val RESUME_LAST_CHANNEL = "resume_last_channel"
private const val START_DESTINATION = "start_destination"
private const val LAST_CHANNEL_ID = "last_channel_id"
private const val WEATHER_LOCATION = "weather_location"
private const val GUEST_NAME = "guest_name"
private const val OSD_TIMEOUT_SECONDS = "osd_timeout_seconds"
private const val DEFAULT_PLAYLIST_URL = "http://play.glztech.com/list.m3u"
private const val DEFAULT_EPG_URL = "https://play.glztech.com/epg.xml.gz"
private const val DEFAULT_WEATHER_LOCATION = "San Juan"

private enum class PlayerDrawer { None, Channels, Services, Recent }

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

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (GlzHubManager.restoreActivityAfterApp(prefs)) {
            lifecycleScope.launch(Dispatchers.IO) {
                GlzHubManager.heartbeat(prefs, createPermissiveOkHttpClient())
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TvScreen(
    themeMode: String,
    deepLinkChannelId: String?,
    networkPermissionRevision: Int,
    onThemeMode: (String) -> Unit
) {
    val context = LocalContext.current
    val safeHorizontalPadding = if (LocalConfiguration.current.screenWidthDp >= 600) 40.dp else 12.dp
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val client = remember { createPermissiveOkHttpClient() }
    val sourcePreferences = remember { PreferencesRepository(context) }
    val playlistRepository = remember { PlaylistRepository(context, sourcePreferences, client) }
    val epgRepository = remember { EpgRepository(context, sourcePreferences, client) }
    val weatherRepository = remember { WeatherRepository(client) }
    val scope = rememberCoroutineScope()
    val channels = remember { mutableStateListOf<Channel>() }
    var guide by remember { mutableStateOf(EpgGuide.Empty) }
    var selected by remember { mutableStateOf<Channel?>(null) }
    var multiViewSecondary by remember { mutableStateOf<Channel?>(null) }
    var playerActive by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Add an M3U playlist to start watching.") }
    var loading by remember { mutableStateOf(false) }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var weatherLoading by remember { mutableStateOf(false) }
    var weatherError by remember { mutableStateOf<String?>(null) }
    var networkInfo by remember { mutableStateOf<NetworkInfo?>(null) }
    var networkOverrideRevision by remember { mutableStateOf(0) }
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
    var captionsEnabled by remember {
        mutableStateOf(prefs.getBoolean(CAPTIONS_ENABLED, false))
    }
    var captionLanguage by remember {
        mutableStateOf(
            prefs.getString(CAPTION_LANGUAGE, null)
                ?: prefs.getString(LEGACY_CAPTION_LANGUAGE, "en")
                ?: "en"
        )
    }
    var keepAwakeAtHome by remember { mutableStateOf(prefs.getBoolean(KEEP_AWAKE_HOME, false)) }
    var homePreviewChannelId by remember {
        mutableStateOf(prefs.getString(HOME_PREVIEW_CHANNEL_ID, null))
    }
    var radioPlaying by remember { mutableStateOf(false) }
    var osdTimeoutSeconds by remember {
        mutableStateOf(prefs.getInt(OSD_TIMEOUT_SECONDS, 8))
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
    val recentChannelManager = remember { RecentChannelManager(prefs) }
    var recentRevision by remember { mutableStateOf(0) }

    val keepScreenAwake = radioPlaying || (section == AppSection.Home && keepAwakeAtHome)
    DisposableEffect(keepScreenAwake) {
        val window = (context as? Activity)?.window
        if (keepScreenAwake) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (keepScreenAwake) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    suspend fun reportHubSync(percent: Int, message: String, syncStatus: String = "syncing") {
        withContext(Dispatchers.IO) {
            GlzHubManager.heartbeat(
                prefs, client, GlzHubManager.SyncProgress(percent, message, syncStatus)
            )
        }
    }

    suspend fun loadSources(
        forceRefresh: Boolean = false,
        onProgress: suspend (Int, String) -> Unit = { _, _ -> }
    ) {
        val playlistUrl = sourcePreferences.playlistUrl
        if (playlistUrl.isBlank()) {
            showSettings = true
            return
        }
        loading = true
        status = "Loading your lineup…"
        onProgress(45, "Restoring saved channel data")
        val cached = withContext(Dispatchers.IO) {
            playlistRepository.cached() to epgRepository.cached()
        }
        cached.first?.let { cachedChannels ->
            channels.clear()
            channels.addAll(cachedChannels)
            cached.second?.let { guide = it }
            status = if (BuildConfig.DEBUG) {
                "${cachedChannels.size} channels · restored instantly" +
                    cached.second?.let { " · ${it.programmeCount} guide entries" }.orEmpty()
            } else "Your channels are ready"
            if (selected == null && prefs.getBoolean(RESUME_LAST_CHANNEL, true)) {
                val lastId = prefs.getString(LAST_CHANNEL_ID, null)
                selected = cachedChannels.firstOrNull { it.id == lastId }
            }
        }
        val refreshResult = runCatching {
            onProgress(55, "Downloading TV lineup & programme guide")
            coroutineScope {
                val parsedDeferred = async(Dispatchers.IO) { playlistRepository.load(forceRefresh) }
                val guideDeferred = async(Dispatchers.IO) { epgRepository.load(forceRefresh) }
                val parsed = parsedDeferred.await()
                val parsedGuide = guideDeferred.await()
                val matchedChannels = parsed.count { parsedGuide.forChannel(it).isNotEmpty() }
                check(parsedGuide.programmeCount == 0 || matchedChannels > 0) {
                    "EPG downloaded ${parsedGuide.programmeCount} programmes but matched 0 of ${parsed.size} channels"
                }
                Triple(parsed, parsedGuide, Pair(cached.first != null && cached.second != null, matchedChannels))
            }
        }
        refreshResult.onSuccess { (parsed, parsedGuide, refreshInfo) ->
            val (fromCache, matchedChannels) = refreshInfo
            channels.clear()
            channels.addAll(parsed)
            guide = parsedGuide
            status = if (BuildConfig.DEBUG) {
                "${parsed.size} channels${if (fromCache) " · restored from storage" else ""} · " +
                    "${parsedGuide.programmeCount} guide entries · $matchedChannels EPG matches"
            } else "${parsed.size} channels ready"
            if (selected == null && prefs.getBoolean(RESUME_LAST_CHANNEL, true)) {
                val lastId = prefs.getString(LAST_CHANNEL_ID, null)
                selected = parsed.firstOrNull { it.id == lastId }
            }
            onProgress(90, "Publishing Android TV home channels")
            withContext(Dispatchers.IO) {
                TvHomePublisher.publish(context.applicationContext, parsed, parsedGuide)
            }
        }.onFailure { error ->
            if (forceRefresh) guide = EpgGuide.Empty
            status = if (channels.isNotEmpty()) {
                if (BuildConfig.DEBUG) "${channels.size} saved channels · refresh failed: ${error.message}"
                else "Showing your saved channels — couldn't refresh right now"
            } else {
                if (BuildConfig.DEBUG) "Could not load sources: ${error.message}"
                else "Couldn't load channels — check your connection or Settings"
            }
        }
        loading = false
        if (forceRefresh) refreshResult.exceptionOrNull()?.let { throw it }
    }

    suspend fun syncEverythingNow(onProgress: (Int, String) -> Unit): String {
        suspend fun report(percent: Int, message: String, syncStatus: String = "syncing") {
            status = message
            onProgress(percent, message)
            reportHubSync(percent, message, syncStatus)
        }
        return try {
        report(5, "Contacting GLZ Hub")
        val syncResult = withContext(Dispatchers.IO) { GlzHubManager.sync(prefs, client) }
        report(20, "Configuration received")
        val radioCount = withContext(Dispatchers.IO) {
            RadioCatalogManager.load(prefs, client).stations.size
        }
        report(35, "Radio stations updated")
        for (command in syncResult.commands) {
            handleManagedHubCommand(context, prefs, client, command) {
                loadSources(forceRefresh = true)
            }
        }
        guestName = prefs.getString(GUEST_NAME, "Guest") ?: "Guest"
        guestExperience = GuestExperience.from(prefs)
        weatherLocation = prefs.getString(WEATHER_LOCATION, DEFAULT_WEATHER_LOCATION)
            ?: DEFAULT_WEATHER_LOCATION
        visibleAppPackages = GlzHubManager.visibleApps(prefs)
        appVisibilityManaged = prefs.getBoolean(GlzHubManager.VISIBLE_APPS_MANAGED, false)
        networkOverrideRevision++
        onThemeMode(prefs.getString(THEME_MODE, themeMode) ?: themeMode)
        captionsEnabled = prefs.getBoolean(CAPTIONS_ENABLED, captionsEnabled)
        captionLanguage = prefs.getString(CAPTION_LANGUAGE, captionLanguage) ?: captionLanguage
        keepAwakeAtHome = prefs.getBoolean(KEEP_AWAKE_HOME, keepAwakeAtHome)
        homePreviewChannelId = prefs.getString(HOME_PREVIEW_CHANNEL_ID, homePreviewChannelId)
        osdTimeoutSeconds = prefs.getInt(OSD_TIMEOUT_SECONDS, osdTimeoutSeconds)
        loadSources(forceRefresh = true) { percent, message -> report(percent, message) }
        val matchedChannels = channels.count { guide.forChannel(it).isNotEmpty() }
        val result = if (BuildConfig.DEBUG) {
            "Synced now · ${channels.size} TV channels · $radioCount radio stations · " +
                "${guide.programmeCount} guide entries · $matchedChannels EPG matches"
        } else {
            "Synced · ${channels.size} channels · $radioCount radio stations"
        }
        report(100, result, "complete")
        result
        } catch (error: Throwable) {
            runCatching {
                report(
                    100,
                    if (BuildConfig.DEBUG) "Sync failed · ${error.message}" else "Sync didn't finish — try again",
                    "failed"
                )
            }
            throw error
        }
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
        initialSync?.commands?.let { commands ->
            for (command in commands) {
                handleManagedHubCommand(context, prefs, client, command) {
                    loadSources(forceRefresh = true)
                }
            }
        }
        if (initialSync?.changed == true) {
            guestName = prefs.getString(GUEST_NAME, "Guest") ?: "Guest"
            guestExperience = GuestExperience.from(prefs)
            weatherLocation = prefs.getString(WEATHER_LOCATION, DEFAULT_WEATHER_LOCATION)
                ?: DEFAULT_WEATHER_LOCATION
            visibleAppPackages = GlzHubManager.visibleApps(prefs)
            appVisibilityManaged = prefs.getBoolean(GlzHubManager.VISIBLE_APPS_MANAGED, false)
            networkOverrideRevision++
            onThemeMode(prefs.getString(THEME_MODE, themeMode) ?: themeMode)
            hubStatus = GlzHubManager.pairingCode(prefs)?.let { "Pairing code: $it" }
                ?: if (GlzHubManager.isEnrolled(prefs)) "Connected to GLZ Hub"
                else "Not connected"
        }
        if (initialSync?.changed == true || initialSync?.forceRefreshTriggered == true) {
            reportHubSync(35, "Configuration received")
            loadSources(forceRefresh = true) { percent, message ->
                reportHubSync(percent, message)
            }
            reportHubSync(100, "Sync complete", "complete")
        } else {
            loadSources()
        }
        while (true) {
            delay(if (GlzHubManager.pairingCode(prefs) != null) 3_000L else 5_000L)
            runCatching {
                withContext(Dispatchers.IO) {
                    val result = GlzHubManager.sync(prefs, client)
                    GlzHubManager.heartbeat(prefs, client)
                    result
                }
            }.onSuccess { result ->
                for (command in result.commands) {
                    handleManagedHubCommand(context, prefs, client, command) {
                        loadSources(forceRefresh = true)
                    }
                }
                if (result.changed || result.forceRefreshTriggered) {
                    guestName = prefs.getString(GUEST_NAME, "Guest") ?: "Guest"
                    guestExperience = GuestExperience.from(prefs)
                    weatherLocation = prefs.getString(WEATHER_LOCATION, DEFAULT_WEATHER_LOCATION)
                        ?: DEFAULT_WEATHER_LOCATION
                    visibleAppPackages = GlzHubManager.visibleApps(prefs)
                    appVisibilityManaged =
                        prefs.getBoolean(GlzHubManager.VISIBLE_APPS_MANAGED, false)
                    networkOverrideRevision++
                    onThemeMode(prefs.getString(THEME_MODE, themeMode) ?: themeMode)
                    captionsEnabled = prefs.getBoolean(CAPTIONS_ENABLED, captionsEnabled)
                    captionLanguage = prefs.getString(CAPTION_LANGUAGE, captionLanguage) ?: captionLanguage
                    keepAwakeAtHome = prefs.getBoolean(KEEP_AWAKE_HOME, keepAwakeAtHome)
                    homePreviewChannelId = prefs.getString(HOME_PREVIEW_CHANNEL_ID, homePreviewChannelId)
                    reportHubSync(35, "Configuration received")
                    loadSources(forceRefresh = true) { percent, message ->
                        reportHubSync(percent, message)
                    }
                    reportHubSync(100, "Sync complete", "complete")
                }
                hubStatus = GlzHubManager.pairingCode(prefs)?.let { "Pairing code: $it" }
                    ?: if (GlzHubManager.isEnrolled(prefs)) "Connected to GLZ Hub"
                    else "Not connected"
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
            weatherLoading = true
            runCatching { withContext(Dispatchers.IO) { weatherRepository.load(weatherLocation) } }
                .onSuccess {
                    weather = it
                    weatherError = null
                }
                .onFailure { weatherError = it.message ?: "Weather service unavailable" }
            weatherLoading = false
            delay(30 * 60 * 1000L)
        }
    }
    LaunchedEffect(networkPermissionRevision, networkOverrideRevision) {
        while (true) {
            networkInfo = withContext(Dispatchers.IO) {
                fetchNetworkInfo(context, prefs, client)
            }
            delay(30 * 60 * 1000L)
        }
    }
    LaunchedEffect(deepLinkChannelId, channels.size) {
        if (!deepLinkChannelId.isNullOrBlank() && channels.isNotEmpty()) {
            channels.firstOrNull { it.id == deepLinkChannelId }?.let {
                selected = it
                playerActive = true
                GlzHubManager.reportActivity(prefs, "channel", it.name)
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
    val tuneChannel: (Channel) -> Unit = {
        if (selected?.id != it.id) {
            PlaybackPerformance.channelSelected(it.id)
            recentChannelManager.record(it.id)
            recentRevision++
        }
        selected = it
        playerActive = true
        GlzHubManager.reportActivity(prefs, "channel", it.name)
        prefs.edit().putString(LAST_CHANNEL_ID, it.id).apply()
    }
    val immersive = section == AppSection.Live && selected != null && playerActive
    val resumeChannel = remember(ordered, recentRevision) {
        prefs.getString(LAST_CHANNEL_ID, null)?.let { id -> ordered.firstOrNull { it.id == id } }
    }
    val managedEntertainmentApps = remember(visibleAppPackages, appVisibilityManaged) {
        if (!appVisibilityManaged) EntertainmentApps
        else EntertainmentApps.filter { it.packageName in visibleAppPackages }
    }

    BackHandler(enabled = !showSettings && !immersive) {
        if (section != AppSection.Home) {
            section = AppSection.Home
        } else {
            (context as? Activity)?.finishAndRemoveTask()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (!immersive) {
            SlimHeader(
                loading = loading,
                    contentLoaded = channels.isNotEmpty(),
                    weather = weather,
                    networkInfo = networkInfo,
                    onWeatherClick = { section = AppSection.Weather },
                    onRefresh = { scope.launch { loadSources(forceRefresh = true) } },
                    onSettings = { showSettings = true },
                    minimal = section == AppSection.Home
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(if (immersive) PaddingValues(0.dp) else padding)) {
            if (immersive) {
                if (multiViewSecondary != null) MultiViewScreen(
                    primary = selected!!,
                    secondary = multiViewSecondary!!,
                    guide = guide,
                    captionsEnabled = captionsEnabled,
                    captionLanguage = captionLanguage,
                    onExpand = {
                        multiViewSecondary = null
                        tuneChannel(it)
                    },
                    onExit = { multiViewSecondary = null }
                ) else ImmersivePlayerScreen(
                    channel = selected!!,
                    channels = ordered,
                    guide = guide,
                    captionsEnabled = captionsEnabled,
                    captionLanguage = captionLanguage,
                    onCaptionsChanged = { enabled, language ->
                        captionsEnabled = enabled
                        captionLanguage = language
                        prefs.edit()
                            .putBoolean(CAPTIONS_ENABLED, enabled)
                            .putString(CAPTION_LANGUAGE, language)
                            .apply()
                    },
                    osdTimeoutSeconds = osdTimeoutSeconds,
                    entertainmentApps = managedEntertainmentApps,
                    recentChannels = remember(recentRevision, channels.toList()) {
                        recentChannelManager.recentIds().mapNotNull { id ->
                            channels.firstOrNull { it.id == id }
                        }
                    },
                    onPreviousChannel = {
                        recentChannelManager.previousId(selected!!.id)
                            ?.let { id -> channels.firstOrNull { it.id == id } }
                            ?.let(tuneChannel)
                    },
                    onAddToMultiView = { multiViewSecondary = it },
                    onTune = tuneChannel,
                    onExit = {
                        playerActive = false
                        GlzHubManager.reportActivity(prefs, "idle")
                        section = AppSection.Home
                    }
                )
            } else {
                Row(
                    Modifier.fillMaxSize()
                        .padding(horizontal = safeHorizontalPadding, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ExpressiveNavigationRail(
                        section = section,
                        onSection = {
                            if (it == AppSection.Live) {
                                playerActive = false
                                GlzHubManager.reportActivity(prefs, "idle")
                            }
                            section = it
                        }
                    )
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        when (section) {
                            AppSection.Home -> GuestHubHome(
                                guestName = guestName,
                                experience = guestExperience,
                                entertainmentApps = managedEntertainmentApps,
                                previewChannel = ordered.firstOrNull { it.id == homePreviewChannelId },
                                resumeChannel = resumeChannel,
                                weather = weather,
                                networkInfo = networkInfo,
                                captionLanguage = captionLanguage,
                                channels = ordered,
                                guide = guide,
                                onWatchChannel = { channel ->
                                    section = AppSection.Live
                                    tuneChannel(channel)
                                },
                                onOpenGuide = {
                                    playerActive = false
                                    GlzHubManager.reportActivity(prefs, "idle")
                                    section = AppSection.Live
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            AppSection.Live -> GuideSection(
                                channels = ordered,
                                guide = guide,
                                previewChannel = ordered.firstOrNull { it.id == homePreviewChannelId },
                                captionLanguage = captionLanguage,
                                favorites = favorites,
                                onWatch = tuneChannel,
                                modifier = Modifier.fillMaxSize()
                            )
                            AppSection.Radio -> RadioScreen(
                                prefs = prefs,
                                client = client,
                                onPlayingChanged = { radioPlaying = it },
                                modifier = Modifier.fillMaxSize()
                            )
                            AppSection.Weather -> WeatherScreen(
                                weather = weather,
                                location = weatherLocation,
                                loading = weatherLoading,
                                error = weatherError,
                                onRefresh = {
                                    scope.launch {
                                        weatherLoading = true
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                weatherRepository.load(weatherLocation)
                                            }
                                        }.onSuccess {
                                            weather = it
                                            weatherError = null
                                        }.onFailure {
                                            weatherError = it.message ?: "Weather service unavailable"
                                        }
                                        weatherLoading = false
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            AppSection.You -> GuestYouSection(
                                guestName = guestName,
                                experience = guestExperience,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
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
            customConnectionLabel = prefs.getString("custom_connection_label", "").orEmpty(),
            customIspName = prefs.getString("custom_isp_name", "").orEmpty(),
            themeMode = themeMode,
            captionsEnabled = captionsEnabled,
            captionLanguage = captionLanguage,
            osdTimeoutSeconds = osdTimeoutSeconds,
            autoUpdate = prefs.getBoolean(AUTO_UPDATE_CHECK, true),
            wifiOnly = prefs.getBoolean(WIFI_ONLY_UPDATES, true),
            autoStart = prefs.getBoolean(AUTO_START, false),
            resumeLast = prefs.getBoolean(RESUME_LAST_CHANNEL, true),
            startDestination = prefs.getString(START_DESTINATION, AppSection.Home.name)
                ?: AppSection.Home.name,
            sourceStatus = status,
            hubStatus = hubStatus,
            onSyncNow = { progress -> syncEverythingNow(progress) },
            onCheckForUpdate = { checkForAppUpdate() },
            onBeginHubEnrollment = {
                withContext(Dispatchers.IO) {
                    GlzHubManager.beginEnrollment(prefs, client)
                }.also { code ->
                    hubStatus = "Pairing code: $code"
                }
            },
            onDismiss = { showSettings = false },
            onSave = { playlist, epg, headers, location, name, connectionLabel, ispName, theme,
                       captions, language, osdTimeout, autoUpdate, wifiOnly, autoStart, resumeLast,
                       startDestination ->
                prefs.edit().putString(PLAYLIST_URL, playlist).putString(EPG_URL, epg)
                    .putString(REQUEST_HEADERS, headers)
                    .putString(WEATHER_LOCATION, location)
                    .putString(GUEST_NAME, name)
                    .putString("custom_connection_label", connectionLabel)
                    .putString("custom_isp_name", ispName)
                    .putBoolean(CAPTIONS_ENABLED, captions)
                    .putString(CAPTION_LANGUAGE, language)
                    .putInt(OSD_TIMEOUT_SECONDS, osdTimeout)
                    .putBoolean(AUTO_UPDATE_CHECK, autoUpdate)
                    .putBoolean(WIFI_ONLY_UPDATES, wifiOnly)
                    .putBoolean(AUTO_START, autoStart)
                    .putBoolean(RESUME_LAST_CHANNEL, resumeLast)
                    .putString(START_DESTINATION, startDestination).apply()
                onThemeMode(theme)
                captionsEnabled = captions
                captionLanguage = language
                osdTimeoutSeconds = osdTimeout
                weatherLocation = location
                guestName = name
                networkOverrideRevision++
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
private fun GuestHubHome(
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
    onOpenGuide: () -> Unit,
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

    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val timeGreeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Box(modifier.fillMaxSize()) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            val compactHeight = maxHeight < 440.dp
            // Hero occupies the upper ~2/3 of the available space.
            val guestHeight = if (compactHeight) (maxHeight * 0.62f).coerceAtLeast(150.dp)
            else (maxHeight * 0.667f).coerceIn(240.dp, 460.dp)

            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (compactHeight) 10.dp else 16.dp)
            ) {
                // 1. TOP HERO CARD
                Card(
                    Modifier
                        .fillMaxWidth()
                        .height(guestHeight),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
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

                        // Live channel preview covers the base when a Home preview channel
                        // is set.
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

                        // Scrims — legibility for the identity + context text over an image
                        // or video. Lighter over a still image so the backdrop still reads.
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
                            // Guest identity — narrowed when there's a backdrop (video or
                            // image) so it clears the right side; full width otherwise.
                            val hasBackdrop = previewChannel != null ||
                                !experience.heroImageUrl.isNullOrBlank()
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
                                    guestName.ifBlank { "Guest" },
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
                                if (!compactHeight) {
                                    experience.roomNumber?.takeIf(String::isNotBlank)?.let {
                                        Text(
                                            "Room $it",
                                            color = Color.White.copy(alpha = 0.72f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    val stayInfo = listOfNotNull(
                                        experience.checkoutTime?.let { "Checkout $it" },
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

                            // Live context — one metric per line; channel tag anchors
                            // the right edge of the network line.
                            val timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now))
                            val meridiemSep = timeText.lastIndexOf(' ')
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    if (meridiemSep > 0) timeText.substring(0, meridiemSep) else timeText,
                                    color = Color.White,
                                    fontSize = if (compactHeight) 20.sp else 24.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp,
                                    maxLines = 1
                                )
                                if (meridiemSep > 0) {
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        timeText.substring(meridiemSep + 1),
                                        color = Color.White.copy(alpha = 0.70f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(if (compactHeight) 4.dp else 8.dp))
                            weather?.let {
                                HeroInfoLine(
                                    "WEATHER",
                                    it.location,
                                    "${weatherGlyph(it.weatherCode)}  ${it.temperature}°F, ${weatherConditionText(it.weatherCode)}"
                                )
                                Spacer(Modifier.height(if (compactHeight) 3.dp else 6.dp))
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HeroInfoLine(
                                    "NETWORK",
                                    networkInfo?.connection ?: "Offline",
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
                                                "${channel.number.ifBlank { "LIVE" }} · ${channel.name}",
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

                // 2. CONTENT SHORTCUTS ROW (section navigation lives in the rail)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (resumeChannel != null) {
                        HomeNavActionButton(
                            label = "CONTINUE  ·  ${resumeChannel.name.uppercase(Locale.getDefault())}",
                            icon = Icons.Default.PlayArrow,
                            isPrimary = true,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { onWatchChannel(resumeChannel) },
                            modifier = Modifier.weight(2f)
                        )
                        HomeNavActionButton(
                            label = "LIVE TV",
                            icon = Icons.Default.LiveTv,
                            isPrimary = false,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { showQuickWatchDrawer = true },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        HomeNavActionButton(
                            label = "LIVE TV",
                            icon = Icons.Default.LiveTv,
                            isPrimary = true,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = { showQuickWatchDrawer = true },
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                    HomeNavActionButton(
                        label = "GUIDE",
                        icon = Icons.Default.CalendarMonth,
                        isPrimary = false,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = onOpenGuide,
                        modifier = Modifier.weight(1f)
                    )
                    HomeNavActionButton(
                        label = "APPS",
                        icon = Icons.Default.Apps,
                        isPrimary = false,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { showAppsDrawer = true },
                        modifier = Modifier.weight(1f)
                    )
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
                    enter = fadeIn(tween(200)) + slideInHorizontally(tween(220), initialOffsetX = { it }),
                    exit = fadeOut(tween(150)) + slideOutHorizontally(tween(180), targetOffsetX = { it }),
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
                    enter = fadeIn(tween(200)) + slideInHorizontally(tween(220), initialOffsetX = { it }),
                    exit = fadeOut(tween(150)) + slideOutHorizontally(tween(180), targetOffsetX = { it }),
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
private fun HomeAppsDrawer(
    apps: List<EntertainmentApp>,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val client = remember { createPermissiveOkHttpClient() }
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
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
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
                        Text("APPS", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                        Text(
                            "${apps.size} apps & services",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TvSettingsButton(label = "Close", onClick = onClose)
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

private fun weatherGlyph(code: Int): String = when (code) {
    0 -> "☀"
    1, 2 -> "⛅"
    3 -> "☁"
    45, 48 -> "≋"
    in 51..67, in 80..82 -> "☂"
    in 71..77, 85, 86 -> "❄"
    in 95..99 -> "ϟ"
    else -> "•"
}

private fun weatherConditionText(code: Int): String = when (code) {
    0 -> "Sunny"
    1 -> "Mostly Clear"
    2 -> "Partly Cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    in 51..57 -> "Drizzle"
    in 61..67, in 80..82 -> "Rain"
    in 71..77, 85, 86 -> "Snow"
    in 95..99 -> "Thunderstorms"
    else -> "—"
}

@Composable
private fun HeroInfoLine(
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
private fun HomeNavActionButton(
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
private fun QuickWatchChannelDrawer(
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
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            // Header
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
                        Text("LIVE TV", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                        Text("${channels.size} channels available", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TvSettingsButton(
                    label = "Close",
                    onClick = onClose
                )
            }

            Spacer(Modifier.height(14.dp))

            // Channel List
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
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
                        border = BorderStroke(
                            if (focused) 2.dp else 1.dp,
                            if (focused) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.10f)
                        )
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Channel Number Badge
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
                                // Channel Logo
                                ChannelLogo(channel, 38.dp, guide)
                                Spacer(Modifier.width(12.dp))
                                // Channel Name & Programme Info
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
private fun GuestYouSection(
    guestName: String,
    experience: GuestExperience,
    modifier: Modifier = Modifier
) {
    val services = buildList {
        experience.noticeTitle?.let {
            add(GuestService(it, experience.noticeBody, null))
        }
        experience.frontDesk?.let {
            add(GuestService("Front Desk", it, null))
        }
        addAll(experience.services)
    }
    val listState = rememberLazyListState()

    BoxWithConstraints(
        modifier
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        val compact = maxWidth < 700.dp

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header Row
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.60f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "You",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your stay at a glance, ${guestName.ifBlank { "Guest" }}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Stay Summary & Wi-Fi Access Cards
            item {
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        StaySummaryCard(guestName, experience, Modifier.fillMaxWidth())
                        WifiInformationCard(experience, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StaySummaryCard(guestName, experience, Modifier.weight(1f))
                        WifiInformationCard(experience, Modifier.weight(1f))
                    }
                }
            }

            // Services & Visit Information
            if (services.isNotEmpty()) {
                item {
                    HubSectionTitle("VISIT INFORMATION", "Helpful details & services for your stay")
                }
                items(services, key = { "${it.title}-${it.actionUrl}" }) { service ->
                    GuestServiceCard(service)
                }
            }
        }
    }
}

@Composable
private fun StaySummaryCard(
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
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) primaryColor else Color.White.copy(alpha = 0.14f)
        ),
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
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.50f))
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
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
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
private fun WifiInformationCard(
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
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) secondaryColor else Color.White.copy(alpha = 0.14f)
        ),
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
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp)),
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
                            border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.45f))
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
private fun WifiQrCode(ssid: String, password: String?, size: androidx.compose.ui.unit.Dp) {
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
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.80f)),
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
private fun GuestServiceCard(service: GuestService) {
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
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
        ),
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
private fun PremiumHero(
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
private fun HubSectionTitle(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
        Spacer(Modifier.width(12.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun LiveHubCard(
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

private fun Color.toLuminousAccent(): Color {
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
private fun LiveTvHubCard(
    onClick: () -> Unit,
    width: androidx.compose.ui.unit.Dp = 190.dp,
    height: androidx.compose.ui.unit.Dp = 100.dp
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
private fun EntertainmentAppCard(
    app: EntertainmentApp,
    width: androidx.compose.ui.unit.Dp = 190.dp,
    height: androidx.compose.ui.unit.Dp = 100.dp
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
    val client = remember { createPermissiveOkHttpClient() }
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
private fun AdaptiveAppIcon(
    icon: Drawable?,
    appName: String,
    accent: Color,
    size: androidx.compose.ui.unit.Dp
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        Modifier.size(size),
        shape = shape,
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
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
private fun PremiumFocusCard(
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

private suspend fun handleManagedHubCommand(
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
private fun GuideSection(
    channels: List<Channel>,
    guide: EpgGuide,
    previewChannel: Channel?,
    captionLanguage: String,
    favorites: Set<String> = emptySet(),
    onWatch: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var timeOffsetMillis by remember { mutableStateOf(0L) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var focusedChannel by remember { mutableStateOf(previewChannel ?: channels.firstOrNull()) }
    var focusedProgramme by remember { mutableStateOf<Programme?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    // Categories derived from channels
    val categories = remember(channels) {
        val groups = channels.mapNotNull { it.group.takeIf { g -> g.isNotBlank() } }.distinct()
        listOf("ALL", "FAVORITES") + groups
    }

    val filteredChannels = remember(channels, selectedCategory, favorites) {
        when (selectedCategory) {
            "ALL" -> channels
            "FAVORITES" -> channels.filter { it.id in favorites }
            else -> channels.filter { it.group.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val activeChannel = focusedChannel ?: previewChannel ?: filteredChannels.firstOrNull()

    GlzPanel(
        modifier = modifier,
        shape = RoundedCornerShape(GlzCardDefaults.RadiusMedium),
        fill = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
            if (guide.programmeCount == 0 && channels.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Guide data is unavailable", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Refresh the sources or check the XMLTV address in Settings.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. COMPACT PROGRAMME DETAIL & PREVIEW HEADER (Takes minimal vertical space)
                    if (activeChannel != null) {
                        EpgPreviewHeader(
                            focusedChannel = activeChannel,
                            focusedProgramme = focusedProgramme,
                            previewChannel = previewChannel,
                            guide = guide,
                            now = now,
                            captionLanguage = captionLanguage,
                            onWatch = { onWatch(activeChannel) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 2. COMPACT CATEGORY FILTERS & TIME JUMP SHORTCUTS BAR
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .focusGroup(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Category Chips (Horizontal Scrollable)
                        LazyRow(
                            Modifier.weight(1f).padding(end = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(categories) { cat ->
                                GlzFocusCard(
                                    onClick = { selectedCategory = cat },
                                    selected = selectedCategory == cat,
                                    shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
                                    focusedScale = 1.04f
                                ) { _ ->
                                    Text(
                                        cat.uppercase(),
                                        Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Time Jump Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(
                                "NOW" to 0L,
                                "+2H" to 2 * 3600 * 1000L,
                                "TONIGHT" to calculateTonightOffset(now),
                                "TOMORROW" to 24 * 3600 * 1000L
                            ).forEach { (label, offset) ->
                                GlzFocusCard(
                                    onClick = { timeOffsetMillis = offset },
                                    selected = timeOffsetMillis == offset,
                                    accent = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
                                    focusedScale = 1.04f
                                ) { _ ->
                                    Text(
                                        label,
                                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // 3. HIGH DENSITY EPG TIMELINE GRID (Thinner rows for maximum channel visibility)
                    EpgGrid(
                        channels = filteredChannels,
                        guide = guide,
                        now = now,
                        timeOffsetMillis = timeOffsetMillis,
                        onFocusChannel = { ch ->
                            focusedChannel = ch
                            val progs = guide.forChannel(ch)
                            focusedProgramme = progs.firstOrNull { it.startMillis <= now && it.endMillis > now }
                        },
                        onFocusProgramme = { ch, prog ->
                            focusedChannel = ch
                            focusedProgramme = prog
                        },
                        onWatch = onWatch,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun calculateTonightOffset(now: Long): Long {
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = now
        set(java.util.Calendar.HOUR_OF_DAY, 20)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val tonightMillis = cal.timeInMillis
    return if (tonightMillis > now) tonightMillis - now else 0L
}

@Composable
private fun EpgPreviewHeader(
    focusedChannel: Channel,
    focusedProgramme: Programme?,
    previewChannel: Channel?,
    guide: EpgGuide,
    now: Long,
    captionLanguage: String,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val programmes = remember(focusedChannel, guide) { guide.forChannel(focusedChannel) }
    val current = remember(programmes, now) {
        programmes.firstOrNull { it.startMillis <= now && it.endMillis > now }
    }
    val activeProg = focusedProgramme ?: current
    val next = remember(programmes, activeProg, now) {
        val targetEnd = activeProg?.endMillis ?: now
        programmes.firstOrNull { it.startMillis >= targetEnd }
    }
    val progress = activeProg?.let {
        if (it.startMillis <= now && it.endMillis > now) {
            ((now - it.startMillis).toFloat() / (it.endMillis - it.startMillis).coerceAtLeast(1L)).coerceIn(0f, 1f)
        } else 0f
    } ?: 0f

    val isLive = activeProg != null && activeProg.startMillis <= now && activeProg.endMillis > now

    Surface(
        modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChannelLogo(focusedChannel, 34.dp, guide)

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${focusedChannel.number.ifBlank { "TV" }} · ${focusedChannel.name}",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isLive) Color(0xFF00E5FF).copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, if (isLive) Color(0xFF00E5FF).copy(alpha = 0.50f) else Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            if (isLive) "LIVE" else "UPCOMING",
                            Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLive) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.80f)
                        )
                    }
                    activeProg?.let { prog ->
                        val startStr = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prog.startMillis))
                        val endStr = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prog.endMillis))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$startStr – $endStr",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        activeProg?.title ?: "Live Broadcast",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    next?.let { n ->
                        Text(
                            "Next: ${n.title}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }

                if (isLive) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp).clip(RoundedCornerShape(1.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
                }
            }

            // Fixed preview PIP video (stays on tuned channel without changing on focus)
            previewChannel?.let { channel ->
                Surface(
                    Modifier
                        .width(110.dp)
                        .height(62.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                ) {
                    VideoPlayer(
                        channel = channel,
                        captionsEnabled = false,
                        captionLanguage = captionLanguage,
                        modifier = Modifier.fillMaxSize(),
                        muted = true,
                        keepScreenOn = false,
                        cropVideo = true
                    )
                }
            }
        }
    }
}

@Composable
private fun EpgGrid(
    channels: List<Channel>,
    guide: EpgGuide,
    now: Long,
    timeOffsetMillis: Long,
    onFocusChannel: (Channel) -> Unit,
    onFocusProgramme: (Channel, Programme) -> Unit,
    onWatch: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val halfHour = 30 * 60 * 1000L
    val baseTime = now + timeOffsetMillis
    val start = baseTime - (baseTime % halfHour)
    val slots = 8
    val slotWidth = 140.dp
    val channelWidth = 180.dp
    val timelineWidth = slotWidth * slots
    val totalWidth = channelWidth + timelineWidth
    val horizontal = rememberScrollState()

    Column(modifier.horizontalScroll(horizontal)) {
        Box(Modifier.width(totalWidth).fillMaxHeight()) {
            Column(Modifier.fillMaxSize()) {
                // Header Timeline Row (compact: 28.dp height)
                Row(
                    Modifier
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f), RoundedCornerShape(10.dp))
                ) {
                    Box(
                        Modifier
                            .width(channelWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "CHANNELS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    repeat(slots) { slot ->
                        Box(
                            Modifier
                                .width(slotWidth)
                                .fillMaxHeight()
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(start + slot * halfHour)),
                                Modifier.padding(start = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Channel Rows (thinner, more channels visible)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(channels, key = { "grid-${it.streamUrl}" }) { channel ->
                        EpgGridRow(
                            channel = channel,
                            programmes = guide.forChannel(channel),
                            guide = guide,
                            windowStart = start,
                            windowEnd = start + slots * halfHour,
                            channelWidth = channelWidth,
                            timelineWidth = timelineWidth,
                            slotWidth = slotWidth,
                            onFocusChannel = { onFocusChannel(channel) },
                            onFocusProgramme = { prog -> onFocusProgramme(channel, prog) },
                            onWatch = { onWatch(channel) }
                        )
                    }
                }
            }

            // Real-Time "NOW" Indicator Cursor
            if (now in start..(start + slots * halfHour)) {
                val elapsedRatio = (now - start).toFloat() / (slots * halfHour).toFloat()
                val clockX = channelWidth + (timelineWidth * elapsedRatio)
                Box(
                    Modifier
                        .offset(x = clockX - 1.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF00E5FF).copy(alpha = 0.70f),
                                    Color(0xFF00E5FF).copy(alpha = 0.10f)
                                )
                            )
                        )
                )
                Surface(
                    modifier = Modifier.offset(x = clockX - 16.dp, y = 4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF00E5FF),
                    contentColor = Color.Black
                ) {
                    Row(
                        Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(Modifier.size(3.dp).background(Color.Red, CircleShape))
                        Text("NOW", fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgGridRow(
    channel: Channel,
    programmes: List<Programme>,
    guide: EpgGuide? = null,
    windowStart: Long,
    windowEnd: Long,
    channelWidth: androidx.compose.ui.unit.Dp,
    timelineWidth: androidx.compose.ui.unit.Dp,
    slotWidth: androidx.compose.ui.unit.Dp,
    onFocusChannel: () -> Unit,
    onFocusProgramme: (Programme) -> Unit,
    onWatch: () -> Unit
) {
    val halfHour = 30 * 60 * 1000f
    val visible = programmes.filter { it.endMillis > windowStart && it.startMillis < windowEnd }
    val now = remember { System.currentTimeMillis() }
    var channelFocused by remember { mutableStateOf(false) }

    Row(
        Modifier
            .height(46.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
    ) {
        // Channel Column Badge
        Surface(
            onClick = onWatch,
            modifier = Modifier
                .width(channelWidth)
                .fillMaxHeight()
                .tvFocusableWithPhysics(
                    shape = RoundedCornerShape(10.dp),
                    focusedScale = 1.02f,
                    glowColor = MaterialTheme.colorScheme.primary,
                    onFocusChange = {
                        channelFocused = it
                        if (it) onFocusChannel()
                    }
                ),
            shape = RoundedCornerShape(10.dp),
            color = if (channelFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (channelFocused) Color.Black else Color.White
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogo(channel, 28.dp, guide)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        channel.number.ifBlank { "TV" },
                        color = if (channelFocused) Color.Black else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        channel.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Timeline Program Blocks
        Box(
            Modifier
                .width(timelineWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
        ) {
            repeat(8) { slot ->
                Box(
                    Modifier
                        .offset(x = slotWidth * slot)
                        .width(slotWidth)
                        .fillMaxHeight()
                        .border(0.5.dp, Color.White.copy(alpha = 0.06f))
                )
            }
            if (visible.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "No program information",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                visible.forEach { programme ->
                    val clippedStart = maxOf(programme.startMillis, windowStart)
                    val clippedEnd = minOf(programme.endMillis, windowEnd)
                    val x = slotWidth * ((clippedStart - windowStart) / halfHour)
                    val width = slotWidth * ((clippedEnd - clippedStart) / halfHour)
                    val isCurrent = programme.startMillis <= now && programme.endMillis > now
                    var progFocused by remember { mutableStateOf(false) }

                    Surface(
                        onClick = onWatch,
                        modifier = Modifier
                            .offset(x = x)
                            .width(maxOf(width, 48.dp))
                            .fillMaxHeight()
                            .padding(2.dp)
                            .tvFocusableWithPhysics(
                                shape = RoundedCornerShape(8.dp),
                                focusedScale = 1.03f,
                                glowColor = Color(0xFF00E5FF),
                                onFocusChange = {
                                    progFocused = it
                                    if (it) onFocusProgramme(programme)
                                }
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            progFocused -> Color(0xFF00E5FF)
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
                        },
                        contentColor = when {
                            progFocused -> Color.Black
                            isCurrent -> Color.White
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        border = BorderStroke(
                            if (progFocused) 1.5.dp else 0.5.dp,
                            when {
                                progFocused -> Color(0xFF00E5FF)
                                isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else -> Color.White.copy(alpha = 0.08f)
                            }
                        )
                    ) {
                        Column(
                            Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                programme.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(programme.startMillis)),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (progFocused) Color.Black.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelLogo(
    channel: Channel,
    size: androidx.compose.ui.unit.Dp,
    guide: EpgGuide? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logoUrl = channel.logoUrl.takeIf { it.isNotBlank() } ?: guide?.logoForChannel(channel)
    val model = remember(logoUrl, channel.headers) {
        if (logoUrl.isNullOrBlank()) null
        else if (channel.headers.isEmpty()) logoUrl
        else {
            val headersBuilder = NetworkHeaders.Builder()
            channel.headers.forEach { (k, v) -> headersBuilder.set(k, v) }
            ImageRequest.Builder(context)
                .data(logoUrl)
                .httpHeaders(headersBuilder.build())
            .build()
        }
    }
    var logoLoaded by remember(model) { mutableStateOf(false) }
    val initials = remember(channel.name) {
        channel.name.split(Regex("\\s+"))
            .mapNotNull { word -> word.firstOrNull(Char::isLetterOrDigit) }
            .take(2)
            .joinToString("")
            .uppercase()
            .ifBlank { "TV" }
    }
    Surface(
        modifier = modifier.then(Modifier.size(size)),
        shape = CircleShape,
        color = Color(0xFFF7F7F4),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = .12f)),
        shadowElevation = 3.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!logoLoaded) {
                Text(
                    text = initials,
                    color = Color(0xFF243447),
                    fontSize = (size.value * .27f).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = "${channel.name} logo",
                    modifier = Modifier.fillMaxSize().padding(size * .14f),
                    contentScale = ContentScale.Fit,
                    onSuccess = { logoLoaded = true },
                    onError = { logoLoaded = false }
                )
            }
        }
    }
}

@Composable
private fun ChannelPane(
    channels: List<Channel>,
    selected: Channel?,
    favorites: Set<String>,
    guide: EpgGuide? = null,
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
                        guide, { onSelect(channel) }, { onFavorite(channel) })
                }
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel, selected: Boolean, favorite: Boolean,
    guide: EpgGuide? = null,
    onClick: () -> Unit, onFavorite: () -> Unit
) {
    var focused by remember(channel.id) { mutableStateOf(false) }
    val container = if (focused) MaterialTheme.colorScheme.surfaceContainerHighest
    else if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    Card(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .tvFocusableWithPhysics(
                shape = RoundedCornerShape(20.dp),
                focusedScale = 1.03f,
                glowColor = MaterialTheme.colorScheme.secondary,
                onFocusChange = { focused = it }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(if (focused) 8.dp else 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(channel, 48.dp, guide)
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
private fun DrawerSectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
        color = Color.White.copy(alpha = .55f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun TvOptionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(14.dp),
        color = when {
            isFocused -> Color(0xFFC4FF4D)
            selected -> Color(0xFF23405F)
            else -> Color.White.copy(alpha = 0.12f)
        },
        contentColor = when {
            isFocused -> Color.Black
            selected -> Color(0xFFC4FF4D)
            else -> Color.White
        },
        border = BorderStroke(
            width = if (isFocused) 3.5.dp else if (selected) 2.dp else 1.dp,
            color = when {
                isFocused -> Color(0xFFC4FF4D)
                selected -> Color(0xFFC4FF4D).copy(alpha = 0.7f)
                else -> Color.White.copy(alpha = 0.18f)
            }
        ),
        shadowElevation = if (isFocused) 8.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            fontWeight = if (isFocused || selected) FontWeight.Black else FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ImmersivePlayerScreen(
    channel: Channel,
    channels: List<Channel>,
    guide: EpgGuide,
    captionsEnabled: Boolean,
    captionLanguage: String,
    onCaptionsChanged: (Boolean, String) -> Unit,
    osdTimeoutSeconds: Int = 8,
    entertainmentApps: List<EntertainmentApp>,
    recentChannels: List<Channel>,
    onPreviousChannel: () -> Unit,
    onAddToMultiView: (Channel) -> Unit,
    onTune: (Channel) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
    val client = remember { createPermissiveOkHttpClient() }
    val trackPreferences = remember { TrackPreferenceManager(prefs) }
    val playbackControls = remember { PlaybackControlState() }
    val scope = rememberCoroutineScope()
    var drawer by remember { mutableStateOf(PlayerDrawer.None) }
    var showOsd by remember { mutableStateOf(false) }
    var selectingMultiViewChannel by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
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
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(showOsd, channel.id, osdTimeoutSeconds) {
        if (showOsd) {
            delay(osdTimeoutSeconds * 1_000L)
            showOsd = false
        }
    }
    LaunchedEffect(channel.id) {
        // The ready callback below turns this on at the exact point playback starts.
        showOsd = false
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
            PlayerDrawer.Recent -> Unit
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
                        if (drawer == PlayerDrawer.None) {
                            drawer = PlayerDrawer.Channels
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (drawer == PlayerDrawer.None) {
                            drawer = PlayerDrawer.Services
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (drawer == PlayerDrawer.None) {
                            showOsd = !showOsd
                            true
                        } else false
                    }
                    // Up = next channel in list order, Down = previous. CHANNEL_UP/DOWN match.
                    KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                        if (drawer == PlayerDrawer.None) {
                            stepChannel(1)
                            showOsd = true
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (drawer == PlayerDrawer.None) {
                            stepChannel(-1)
                            showOsd = true
                            true
                        } else false
                    }
                    // Some full remotes still have MENU — send it to the same panel as ▶.
                    KeyEvent.KEYCODE_MENU -> {
                        if (drawer == PlayerDrawer.None) {
                            showOsd = false
                            drawer = PlayerDrawer.Services
                            true
                        } else false
                    }
                    else -> false
                }
            }
    ) {
        VideoPlayer(
            channel,
            captionsEnabled,
            captionLanguage,
            Modifier.fillMaxSize(),
            controlState = playbackControls,
            preferredAudioLanguage = trackPreferences.audioLanguage,
            onAudioLanguageChanged = { trackPreferences.audioLanguage = it },
            onSubtitleChanged = { enabled, language ->
                trackPreferences.subtitlesEnabled = enabled
                trackPreferences.subtitleLanguage = language
                onCaptionsChanged(enabled, language ?: captionLanguage)
            },
            onPlaybackError = { category ->
                val report = PlaybackErrorCategorizer.sanitizedReport(
                    category, channel.name, System.currentTimeMillis()
                )
                scope.launch(Dispatchers.IO) {
                    runCatching { GlzHubManager.heartbeat(prefs, client, lastError = report) }
                }
            },
            onPlaybackReady = { readyChannelId ->
                if (readyChannelId == channel.id) showOsd = true
            }
        )

        AnimatedVisibility(
            visible = drawer == PlayerDrawer.None && showOsd,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(650))
        ) {
            PlayerOsd(
                channel = channel,
                currentProgramme = currentProgramme,
                nextProgramme = nextProgramme,
                guide = guide,
                now = now
            )
        }

        AnimatedVisibility(
            visible = drawer == PlayerDrawer.None && showOsd,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(650))
        ) {
            Surface(
                color = Color.Black.copy(alpha = .58f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "▲ Next   ▼ Previous      ◀ Guide      ▶ Apps · Audio · CC      OK Info",
                    Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
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
                contentColor = Color.White,
                tonalElevation = 18.dp,
                shadowElevation = 24.dp
            ) {
                Column(Modifier.fillMaxSize().padding(top = 22.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelLogo(channel, 66.dp, guide)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(channel.number.ifBlank { "LIVE" },
                                color = Color(0xFFC4FF4D),
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
                    Text(if (selectingMultiViewChannel) "SELECT SECOND CHANNEL" else "CHANNELS",
                        Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
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
                                        if (selectingMultiViewChannel) {
                                            onAddToMultiView(item)
                                            selectingMultiViewChannel = false
                                        } else {
                                            onTune(item)
                                        }
                                        drawer = PlayerDrawer.None
                                    }
                                    .focusable(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    if (isFocused) 5.dp else 1.dp,
                                    if (isFocused) Color(0xFFC4FF4D)
                                    else Color.Transparent
                                ),
                                color = if (isFocused)
                                    Color(0xFF23405F)
                                else if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = .35f)
                                else Color.Transparent
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    ChannelLogo(item, 42.dp, guide)
                                    Spacer(Modifier.width(12.dp))
                                    Text(item.number.ifBlank { "—" }, Modifier.width(45.dp),
                                        color = if (isSelected) Color(0xFFC4FF4D)
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
            val drawerWidth = if (maxWidth < 520.dp) maxWidth * .92f else 440.dp
            Surface(
                Modifier.width(drawerWidth).fillMaxHeight().align(Alignment.CenterEnd),
                color = Color(0xF20B1114),
                contentColor = Color.White,
                tonalElevation = 18.dp,
                shadowElevation = 24.dp
            ) {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Column(Modifier.padding(bottom = 6.dp)) {
                            Text("ENTERTAINMENT", color = Color.White, fontSize = 24.sp,
                                fontWeight = FontWeight.Black)
                            Text("Playback options and other apps",
                                color = Color.White.copy(alpha = .6f))
                        }
                    }

                    // --- PLAYBACK CONTROLS (folded in from the old MENU drawer) ---
                    item { DrawerSectionLabel("PLAYBACK") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvOptionButton(
                                text = "Previous channel",
                                onClick = { onPreviousChannel(); drawer = PlayerDrawer.None },
                                modifier = Modifier.fillMaxWidth().focusRequester(firstServiceFocus)
                            )
                            TvOptionButton(
                                text = "Recent channels",
                                onClick = { drawer = PlayerDrawer.Recent },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TvOptionButton(
                                text = "Add to MultiView",
                                onClick = { selectingMultiViewChannel = true; drawer = PlayerDrawer.Channels },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TvOptionButton(
                                text = if (showDiagnostics) "Hide stream info" else "Stream info",
                                selected = showDiagnostics,
                                onClick = { showDiagnostics = !showDiagnostics },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (playbackControls.audioTracks.isNotEmpty()) {
                        item { DrawerSectionLabel("AUDIO") }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                playbackControls.audioTracks.forEach { track ->
                                    TvOptionButton(
                                        text = if (track.selected) "✓ ${track.label}" else track.label,
                                        selected = track.selected,
                                        onClick = { playbackControls.chooseAudio(track.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    item { DrawerSectionLabel("SUBTITLES / CC") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvOptionButton(
                                text = if (!captionsEnabled) "✓ Off" else "Off",
                                selected = !captionsEnabled,
                                onClick = { playbackControls.chooseSubtitle(null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            playbackControls.subtitleTracks.forEach { track ->
                                val isSelected = track.selected && captionsEnabled
                                TvOptionButton(
                                    text = if (isSelected) "✓ ${track.label}" else track.label,
                                    selected = isSelected,
                                    onClick = { playbackControls.chooseSubtitle(track.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // --- OTHER APPS ---
                    item { DrawerSectionLabel("APPS") }
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
                        Surface(
                            Modifier.fillMaxWidth()
                                .onFocusChanged { isFocused = it.isFocused }
                                .clickable {
                                    GlzHubManager.reportLaunchedApp(
                                        prefs, app.name, app.packageName
                                    )
                                    scope.launch(Dispatchers.IO) {
                                        GlzHubManager.heartbeat(prefs, client)
                                    }
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

        if (showDiagnostics && drawer == PlayerDrawer.Services) {
            PlaybackDiagnosticsPanel(
                diagnostics = playbackControls.diagnostics,
                modifier = Modifier.align(Alignment.TopStart).padding(30.dp)
            )
        }

        if (drawer == PlayerDrawer.Recent) {
            Surface(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 30.dp),
                color = Color(0xF20B1114), contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                tonalElevation = 18.dp
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("RECENT CHANNELS", color = Color(0xFFC4FF4D),
                        fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentChannels, key = { "recent-${it.id}" }) { recent ->
                            val programme = guide.forChannel(recent)
                                .firstOrNull { it.startMillis <= now && it.endMillis > now }
                            var isFocused by remember(recent.id) { mutableStateOf(false) }
                            Surface(
                                onClick = {
                                    onTune(recent)
                                    drawer = PlayerDrawer.None
                                },
                                modifier = Modifier
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isFocused) Color(0xFFC4FF4D) else Color.White.copy(alpha = 0.12f),
                                contentColor = if (isFocused) Color.Black else Color.White,
                                border = BorderStroke(
                                    width = if (isFocused) 3.5.dp else 1.dp,
                                    color = if (isFocused) Color(0xFFC4FF4D) else Color.White.copy(alpha = 0.18f)
                                ),
                                shadowElevation = if (isFocused) 8.dp else 0.dp
                            ) {
                                Column(Modifier.width(180.dp).padding(14.dp)) {
                                    Text(recent.name, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFocused) Color.Black else Color.White)
                                    Text(programme?.title ?: "Live programming", maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFocused) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.6f))
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
private fun MultiViewScreen(
    primary: Channel,
    secondary: Channel,
    guide: EpgGuide,
    captionsEnabled: Boolean,
    captionLanguage: String,
    onExpand: (Channel) -> Unit,
    onExit: () -> Unit
) {
    var activePane by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    BackHandler(onBack = onExit)
    LaunchedEffect(Unit) {
        delay(60)
        focusRequester.requestFocus()
    }

    Row(
        Modifier.fillMaxSize().background(Color.Black).focusRequester(focusRequester).focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { activePane = 0; true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { activePane = 1; true }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        onExpand(if (activePane == 0) primary else secondary)
                        true
                    }
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                        onExit()
                        true
                    }
                    else -> false
                }
            }
    ) {
        listOf(primary, secondary).forEachIndexed { index, paneChannel ->
            val isFocused = activePane == index
            val programme = guide.forChannel(paneChannel).firstOrNull {
                it.startMillis <= System.currentTimeMillis() && it.endMillis > System.currentTimeMillis()
            }
            Box(
                Modifier.weight(1f).fillMaxHeight().padding(6.dp)
                    .clickable {
                        if (activePane == index) {
                            onExpand(paneChannel)
                        } else {
                            activePane = index
                        }
                    }
                    .border(
                        if (isFocused) 4.5.dp else 1.dp,
                        if (isFocused) Color(0xFFC4FF4D)
                        else Color.White.copy(alpha = .18f),
                        RoundedCornerShape(18.dp)
                    ).clip(RoundedCornerShape(18.dp))
            ) {
                VideoPlayer(
                    channel = paneChannel,
                    captionsEnabled = captionsEnabled,
                    captionLanguage = captionLanguage,
                    modifier = Modifier.fillMaxSize(),
                    muted = !isFocused,
                    createMediaSession = isFocused
                )
                Surface(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                    color = Color.Black.copy(alpha = .78f)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ChannelLogo(paneChannel, 42.dp, guide)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(paneChannel.name, color = Color.White,
                                fontWeight = FontWeight.Black, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text(programme?.title ?: "Live programming",
                                color = Color.White.copy(alpha = .7f), maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFocused) Color(0xFFC4FF4D) else Color.White.copy(alpha = .15f)
                        ) {
                            Text(
                                if (isFocused) "AUDIO ACTIVE" else "MUTED",
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (isFocused) Color.Black else Color.White.copy(alpha = .6f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
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
    guide: EpgGuide? = null,
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        color = Color(0xF5080C10),
        shadowElevation = 24.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel Logo & Full Channel Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(0.42f)
                ) {
                    ChannelLogo(channel, 58.dp, guide)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Text(
                                "CH ${channel.number.ifBlank { "LIVE" }}",
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            channel.name,
                            color = Color.White,
                            fontSize = if (channel.name.length > 22) 17.sp else 20.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(24.dp))

                // Programme Title & Progress
                Column(Modifier.weight(0.58f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            currentProgramme?.title ?: "Live Broadcast",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        remainingMinutes?.let {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "$it min left",
                                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    currentProgramme?.let { prog ->
                        Text(
                            "${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prog.startMillis))} – ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prog.endMillis))}",
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = Color.White.copy(alpha = 0.18f)
                    )
                }
            }

            // Up Next Footer Snippet
            nextProgramme?.let { next ->
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            "UP NEXT",
                            Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(next.startMillis)),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        next.title,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
            fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
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
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    createMediaSession: Boolean = true,
    keepScreenOn: Boolean = true,
    cropVideo: Boolean = false,
    controlState: PlaybackControlState? = null,
    preferredAudioLanguage: String? = null,
    onAudioLanguageChanged: (String?) -> Unit = {},
    onSubtitleChanged: (Boolean, String?) -> Unit = { _, _ -> },
    onPlaybackError: (PlaybackErrorCategory) -> Unit = {},
    onPlaybackReady: (String) -> Unit = {}
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
    val mediaSession = remember(createMediaSession, channel.id) {
        if (createMediaSession) {
            runCatching {
                MediaSession.Builder(context, player)
                    .setId("glz_tv_${channel.id}_${System.identityHashCode(player)}")
                    .build()
            }.getOrNull()
        } else null
    }
    var retryAttempt by remember(channel.id) { mutableStateOf(0) }
    var playbackMessage by remember(channel.id) { mutableStateOf<String?>("Connecting…") }
    var lastPlaybackError by remember(channel.id) { mutableStateOf<PlaybackErrorCategory?>(null) }
    DisposableEffect(player, channel.id) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val category = PlaybackErrorCategorizer.categorize(error)
                lastPlaybackError = category
                onPlaybackError(category)
                if (retryAttempt < 3) {
                    retryAttempt += 1
                    playbackMessage = if (retryAttempt >= 2) "Trying compatibility mode…"
                    else "Retrying stream…"
                } else {
                    playbackMessage = "Stream unavailable"
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playbackMessage = null
                } else if (playbackState == Player.STATE_BUFFERING && retryAttempt == 0) {
                    playbackMessage = "Connecting…"
                }
            }

            override fun onRenderedFirstFrame() {
                PlaybackPerformance.firstFrameRendered(channel.id)
                onPlaybackReady(channel.id)
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audioSelections = mutableMapOf<String, Pair<Tracks.Group, Int>>()
                val subtitleSelections = mutableMapOf<String, Pair<Tracks.Group, Int>>()
                val audio = mutableListOf<TrackOption>()
                val subtitles = mutableListOf<TrackOption>()
                tracks.groups.forEachIndexed { groupIndex, group ->
                    for (trackIndex in 0 until group.length) {
                        if (!group.isTrackSupported(trackIndex)) continue
                        val format = group.getTrackFormat(trackIndex)
                        val id = "$groupIndex:$trackIndex"
                        val option = TrackOption(
                            id = id,
                            label = trackLabel(format.label, format.language, format.channelCount),
                            language = format.language,
                            selected = group.isTrackSelected(trackIndex)
                        )
                        when (group.type) {
                            C.TRACK_TYPE_AUDIO -> {
                                audio += option
                                audioSelections[id] = group to trackIndex
                            }
                            C.TRACK_TYPE_TEXT -> {
                                subtitles += option
                                subtitleSelections[id] = group to trackIndex
                            }
                        }
                    }
                }
                controlState?.audioTracks = audio
                controlState?.subtitleTracks = subtitles
                controlState?.selectAudio = { id ->
                    audioSelections[id]?.let { (group, index) ->
                        val language = group.getTrackFormat(index).language
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(index)))
                            .build()
                        onAudioLanguageChanged(language)
                    }
                }
                controlState?.selectSubtitle = { id ->
                    if (id == null) {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
                        onSubtitleChanged(false, null)
                    } else subtitleSelections[id]?.let { (group, index) ->
                        val language = group.getTrackFormat(index).language
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(index)))
                            .build()
                        onSubtitleChanged(true, language)
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(channel.streamUrl, captionsEnabled, captionLanguage, preferredAudioLanguage, retryAttempt) {
        if (retryAttempt > 0) delay((1L shl (retryAttempt - 1)) * 1_000L)
        httpFactory
            .setDefaultRequestProperties(channel.headers)
            .setUserAgent(channel.headers["User-Agent"] ?: "GLZ-TV/2.0")
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsEnabled)
            .setPreferredTextLanguage(captionLanguage.ifBlank { null })
            .setPreferredAudioLanguage(preferredAudioLanguage)
            .setSelectUndeterminedTextLanguage(captionsEnabled)
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
    LaunchedEffect(player, channel.id, controlState) {
        if (controlState == null) return@LaunchedEffect
        while (true) {
            val video = player.videoFormat
            val audio = player.audioFormat
            controlState.diagnostics = PlaybackDiagnostics(
                channelName = channel.name,
                protocol = PlaybackErrorCategorizer.protocol(channel.streamUrl),
                resolution = if (video != null && video.width > 0 && video.height > 0)
                    "${video.width} × ${video.height}" else null,
                videoCodec = video?.codecs ?: video?.sampleMimeType,
                audioCodec = audio?.codecs ?: audio?.sampleMimeType,
                bitrate = listOfNotNull(video?.bitrate, audio?.bitrate)
                    .filter { it > 0 }.takeIf { it.isNotEmpty() }?.sum(),
                bufferDurationMs = player.totalBufferedDuration.coerceAtLeast(0),
                droppedFrames = player.videoDecoderCounters?.droppedBufferCount?.toLong() ?: 0,
                networkTransport = currentNetworkTransport(context),
                playbackState = playbackStateLabel(player.playbackState),
                lastError = lastPlaybackError
            )
            delay(1_000)
        }
    }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    DisposableEffect(mediaSession) { onDispose { mediaSession?.release() } }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(modifier.background(Color.Black, RoundedCornerShape(24.dp))) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    this.keepScreenOn = keepScreenOn
                    resizeMode = if (cropVideo) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else AspectRatioFrameLayout.RESIZE_MODE_FIT
                    useController = false
                    controllerAutoShow = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    (videoSurfaceView as? SurfaceView)?.setZOrderMediaOverlay(true)
                }
            },
            update = {
                it.player = player
                it.keepScreenOn = keepScreenOn
                it.resizeMode = if (cropVideo) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else AspectRatioFrameLayout.RESIZE_MODE_FIT
                (it.videoSurfaceView as? SurfaceView)?.setZOrderMediaOverlay(true)
            },
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

private fun trackLabel(label: String?, language: String?, channelCount: Int): String {
    val languageLabel = language?.let { code ->
        runCatching { Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault()) }
            .getOrNull()?.takeIf(String::isNotBlank)
    }
    val base = label?.takeIf(String::isNotBlank) ?: languageLabel ?: "Original"
    val layout = when {
        channelCount >= 6 -> "5.1"
        channelCount == 2 -> "Stereo"
        channelCount == 1 -> "Mono"
        else -> null
    }
    return listOfNotNull(base, layout).distinct().joinToString(" · ")
}

private fun playbackStateLabel(state: Int) = when (state) {
    Player.STATE_IDLE -> "Idle"
    Player.STATE_BUFFERING -> "Buffering"
    Player.STATE_READY -> "Playing"
    Player.STATE_ENDED -> "Ended"
    else -> "Unavailable"
}

private fun currentNetworkTransport(context: Context): String? {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return null
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return null
    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        else -> "Other"
    }
}

private enum class SettingsTab(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    Sources("Sources & Lineup", "M3U Playlists, EPG data & Hub Sync", Icons.Default.Refresh, Color(0xFF00E5FF)),
    Appearance("Appearance & Themes", "Color palettes, contrast & UI styling", Icons.Default.Settings, Color(0xFFD500F9)),
    Playback("Player & Captions", "OSD timeout, subtitle language & tracks", Icons.Default.LiveTv, Color(0xFF00E676)),
    Startup("Startup & Updates", "Default destination, auto-start & updates", Icons.Default.Home, Color(0xFFFF9100)),
    Hub("GLZ Hub Pairing", "Remote management & pairing code", Icons.Default.Wifi, Color(0xFF1DE9B6)),
    Guest("Guest & Network", "Guest greeting, weather city & ISP labels", Icons.Default.Person, Color(0xFFFF4081))
}

@Composable
private fun TvCategoryTab(
    tab: SettingsTab,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val activeColor = tab.accentColor

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusableWithPhysics(
                shape = RoundedCornerShape(18.dp),
                focusedScale = 1.04f,
                glowColor = activeColor,
                onFocusChange = {
                    focused = it
                    if (it) onSelect()
                }
            )
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        color = when {
            focused -> activeColor
            selected -> activeColor.copy(alpha = 0.22f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
        },
        contentColor = when {
            focused -> Color.Black
            selected -> activeColor
            else -> MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> activeColor
                selected -> activeColor.copy(alpha = 0.60f)
                else -> Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (focused) Color.Black.copy(alpha = 0.2f) else activeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (focused) Color.Black else activeColor
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tab.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    tab.description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (focused) Color.Black.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    playlist: String,
    epg: String,
    headers: String,
    weatherLocation: String,
    guestName: String,
    customConnectionLabel: String,
    customIspName: String,
    themeMode: String,
    captionsEnabled: Boolean,
    captionLanguage: String,
    osdTimeoutSeconds: Int = 8,
    autoUpdate: Boolean,
    wifiOnly: Boolean,
    autoStart: Boolean,
    resumeLast: Boolean,
    startDestination: String,
    sourceStatus: String,
    hubStatus: String,
    onSyncNow: suspend ((Int, String) -> Unit) -> String,
    onCheckForUpdate: suspend () -> String,
    onBeginHubEnrollment: suspend () -> String,
    onDismiss: () -> Unit,
    onSave: (
        String, String, String, String, String, String, String, String, Boolean, String, Int,
        Boolean, Boolean, Boolean, Boolean, String
    ) -> Unit
) {
    var activeTab by remember { mutableStateOf(SettingsTab.Sources) }
    var playlistValue by remember { mutableStateOf(playlist) }
    var epgValue by remember { mutableStateOf(epg) }
    var headerValue by remember { mutableStateOf(headers) }
    var weatherLocationValue by remember { mutableStateOf(weatherLocation) }
    var guestNameValue by remember { mutableStateOf(guestName) }
    var connectionLabelValue by remember { mutableStateOf(customConnectionLabel) }
    var ispNameValue by remember { mutableStateOf(customIspName) }
    // Legacy light/adaptive selections collapse into the dark palette (GLZ TV is dark-only).
    var themeValue by remember {
        mutableStateOf(if (themeMode == "light" || themeMode == "adaptive") "dark" else themeMode)
    }
    var captionsValue by remember { mutableStateOf(captionsEnabled) }
    var languageValue by remember { mutableStateOf(captionLanguage) }
    var osdTimeoutValue by remember { mutableStateOf(osdTimeoutSeconds) }
    var autoUpdateValue by remember { mutableStateOf(autoUpdate) }
    var wifiOnlyValue by remember { mutableStateOf(wifiOnly) }
    var autoStartValue by remember { mutableStateOf(autoStart) }
    var resumeLastValue by remember { mutableStateOf(resumeLast) }
    var startDestinationValue by remember { mutableStateOf(startDestination) }
    var updateStatus by remember { mutableStateOf("Version ${BuildConfig.VERSION_NAME}") }
    var hubMessage by remember(hubStatus) { mutableStateOf(hubStatus) }
    var hubLoading by remember { mutableStateOf(false) }
    var syncLoading by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf(0) }
    var syncMessage by remember(sourceStatus) { mutableStateOf(sourceStatus) }
    val settingsScope = rememberCoroutineScope()
    val initialFocus = remember { FocusRequester() }
    val rightPaneFocusRequester = remember { FocusRequester() }
    val tabFocusRequesters = remember { SettingsTab.values().associateWith { FocusRequester() } }

    val rightPanelScrollState = rememberScrollState()
    LaunchedEffect(activeTab) {
        rightPanelScrollState.scrollTo(0)
    }

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { initialFocus.requestFocus() }
    }

    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF10192A), Color(0xFF070B14), Color.Black)
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.40f),
                border = BorderStroke(0.dp, Color.Transparent)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "SETTINGS DASHBOARD",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            "Configure playlist sources, player behavior & TV settings",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = activeTab.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, activeTab.accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            activeTab.title.uppercase(Locale.ROOT),
                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = activeTab.accentColor
                        )
                    }
                }
            }

            // Main Dashboard Body
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 36.dp, vertical = 16.dp)
            ) {
                // Left Navigation Pane
                Column(
                    Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .padding(end = 20.dp)
                        .focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingsTab.values().forEachIndexed { index, tab ->
                        TvCategoryTab(
                            tab = tab,
                            selected = activeTab == tab,
                            onSelect = { activeTab = tab },
                            modifier = (if (index == 0) Modifier.focusRequester(initialFocus) else Modifier)
                                .focusRequester(tabFocusRequesters[tab]!!)
                                .focusProperties { right = rightPaneFocusRequester }
                        )
                    }
                }

                // Vertical Divider
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.10f))
                )

                // Right Content Panel
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 28.dp)
                        .verticalScroll(rightPanelScrollState)
                        .focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        when (activeTab) {
                            SettingsTab.Sources -> {
                                SettingsLabel("SOURCE STATUS & REFRESH")
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                ) {
                                    Column(Modifier.padding(20.dp)) {
                                        Text(
                                            syncMessage,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp
                                        )
                                        if (syncLoading) {
                                            Spacer(Modifier.height(12.dp))
                                            LinearProgressIndicator(
                                                progress = { syncProgress / 100f },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                "$syncProgress%",
                                                color = SettingsTab.Sources.accentColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.height(14.dp))
                                        TvSettingsButton(
                                            label = if (syncLoading) "Syncing content…" else "↻ Refresh & Sync Now",
                                            enabled = !syncLoading,
                                            onClick = {
                                                syncLoading = true
                                                syncProgress = 0
                                                syncMessage = "Contacting GLZ Hub and refreshing all managed data…"
                                                settingsScope.launch {
                                                    syncMessage = runCatching {
                                                        onSyncNow { percent, message ->
                                                            syncProgress = percent
                                                            syncMessage = message
                                                        }
                                                    }.getOrElse { "Sync failed · ${it.message}" }
                                                    syncLoading = false
                                                }
                                            },
                                            modifier = Modifier
                                                .focusRequester(rightPaneFocusRequester)
                                                .focusProperties { left = tabFocusRequesters[SettingsTab.Sources]!! }
                                        )
                                    }
                                }
                                SettingsLabel("PLAYLIST & EPG SOURCES")
                                ProtectedSourceField(
                                    value = playlistValue,
                                    onValueChange = { playlistValue = it },
                                    label = "M3U Playlist URL",
                                    modifier = Modifier.focusProperties { left = tabFocusRequesters[SettingsTab.Sources]!! }
                                )
                                ProtectedSourceField(
                                    value = epgValue,
                                    onValueChange = { epgValue = it },
                                    label = "XMLTV EPG URL",
                                    modifier = Modifier.focusProperties { left = tabFocusRequesters[SettingsTab.Sources]!! }
                                )
                                ProtectedSourceField(
                                    headerValue, { headerValue = it },
                                    "Request Headers", "One Name: value header per line",
                                    singleLine = false, minLines = 3,
                                    modifier = Modifier.focusProperties { left = tabFocusRequesters[SettingsTab.Sources]!! }
                                )
                            }

                            SettingsTab.Appearance -> {
                                SettingsLabel("COLOR THEMES")
                                Text(
                                    "Choose your preferred TV color palette",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                val themes = listOf(
                                    "dark" to "GLZ Dark",
                                    "ocean" to "Ocean Breeze",
                                    "sunset" to "Sunset Glow",
                                    "emerald" to "Emerald Forest",
                                    "cyberpunk" to "Neon Cyberpunk",
                                    "midnight" to "Midnight Gold"
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    themes.forEachIndexed { index, (value, label) ->
                                        val isSelected = themeValue == value
                                        var focused by remember(value) { mutableStateOf(false) }
                                        Surface(
                                            modifier = (if (index == 0) Modifier.focusRequester(rightPaneFocusRequester) else Modifier)
                                                .fillMaxWidth()
                                                .tvFocusableWithPhysics(
                                                    shape = RoundedCornerShape(16.dp),
                                                    focusedScale = 1.03f,
                                                    glowColor = SettingsTab.Appearance.accentColor,
                                                    onFocusChange = { focused = it }
                                                )
                                                .clickable { themeValue = value }
                                                .focusProperties { left = tabFocusRequesters[SettingsTab.Appearance]!! },
                                            shape = RoundedCornerShape(16.dp),
                                            color = when {
                                                focused -> SettingsTab.Appearance.accentColor
                                                isSelected -> SettingsTab.Appearance.accentColor.copy(alpha = 0.20f)
                                                else -> Color.White.copy(alpha = 0.05f)
                                            },
                                            contentColor = when {
                                                focused -> Color.Black
                                                isSelected -> SettingsTab.Appearance.accentColor
                                                else -> Color.White
                                            },
                                            border = BorderStroke(
                                                if (focused) 2.dp else 1.dp,
                                                when {
                                                    focused -> SettingsTab.Appearance.accentColor
                                                    isSelected -> SettingsTab.Appearance.accentColor.copy(alpha = 0.50f)
                                                    else -> Color.White.copy(alpha = 0.10f)
                                                }
                                            )
                                        ) {
                                            Row(
                                                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                                if (isSelected) {
                                                    Text("SELECTED ✓", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsTab.Playback -> {
                                SettingsLabel("PLAYER BANNERS (OSD)")
                                Text(
                                    "Banner display duration during channel change",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    listOf(
                                        5 to "5 Seconds",
                                        7 to "7 Seconds",
                                        8 to "8 Seconds (Default)",
                                        10 to "10 Seconds"
                                    ).forEachIndexed { index, (timeout, label) ->
                                        val isSelected = osdTimeoutValue == timeout
                                        var focused by remember(timeout) { mutableStateOf(false) }
                                        Surface(
                                            modifier = (if (index == 0) Modifier.focusRequester(rightPaneFocusRequester) else Modifier)
                                                .weight(1f)
                                                .tvFocusableWithPhysics(
                                                    shape = RoundedCornerShape(16.dp),
                                                    focusedScale = 1.04f,
                                                    glowColor = SettingsTab.Playback.accentColor,
                                                    onFocusChange = { focused = it }
                                                )
                                                .clickable { osdTimeoutValue = timeout }
                                                .focusProperties { left = tabFocusRequesters[SettingsTab.Playback]!! },
                                            shape = RoundedCornerShape(16.dp),
                                            color = when {
                                                focused -> SettingsTab.Playback.accentColor
                                                isSelected -> SettingsTab.Playback.accentColor.copy(alpha = 0.20f)
                                                else -> Color.White.copy(alpha = 0.05f)
                                            },
                                            contentColor = when {
                                                focused -> Color.Black
                                                isSelected -> SettingsTab.Playback.accentColor
                                                else -> Color.White
                                            },
                                            border = BorderStroke(
                                                if (focused) 2.dp else 1.dp,
                                                when {
                                                    focused -> SettingsTab.Playback.accentColor
                                                    isSelected -> SettingsTab.Playback.accentColor.copy(alpha = 0.50f)
                                                    else -> Color.White.copy(alpha = 0.10f)
                                                }
                                            )
                                        ) {
                                            Text(
                                                label,
                                                Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }

                                SettingsLabel("CLOSED CAPTIONS")
                                SettingsToggle("Enable Closed Captions", captionsValue) { captionsValue = it }
                                ProtectedSourceField(
                                    languageValue, { languageValue = it },
                                    "Preferred Language Code", "Examples: en, es, fr",
                                    enabled = captionsValue,
                                    modifier = Modifier.focusProperties { left = tabFocusRequesters[SettingsTab.Playback]!! }
                                )
                            }

                            SettingsTab.Startup -> {
                                SettingsLabel("START DESTINATION")
                                Text(
                                    "Screen shown when GLZ TV launches",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(
                                        AppSection.Home to "Home Screen",
                                        AppSection.Live to "Live TV Direct",
                                        AppSection.Radio to "Radio",
                                        AppSection.Weather to "Weather",
                                        AppSection.You to "You & Apps"
                                    ).forEachIndexed { index, (destination, label) ->
                                        val isSelected = startDestinationValue == destination.name
                                        var focused by remember(destination) { mutableStateOf(false) }
                                        Surface(
                                            modifier = (if (index == 0) Modifier.focusRequester(rightPaneFocusRequester) else Modifier)
                                                .weight(1f)
                                                .tvFocusableWithPhysics(
                                                    shape = RoundedCornerShape(16.dp),
                                                    focusedScale = 1.04f,
                                                    glowColor = SettingsTab.Startup.accentColor,
                                                    onFocusChange = { focused = it }
                                                )
                                                .clickable { startDestinationValue = destination.name }
                                                .focusProperties { left = tabFocusRequesters[SettingsTab.Startup]!! },
                                            shape = RoundedCornerShape(16.dp),
                                            color = when {
                                                focused -> SettingsTab.Startup.accentColor
                                                isSelected -> SettingsTab.Startup.accentColor.copy(alpha = 0.20f)
                                                else -> Color.White.copy(alpha = 0.05f)
                                            },
                                            contentColor = when {
                                                focused -> Color.Black
                                                isSelected -> SettingsTab.Startup.accentColor
                                                else -> Color.White
                                            },
                                            border = BorderStroke(
                                                if (focused) 2.dp else 1.dp,
                                                when {
                                                    focused -> SettingsTab.Startup.accentColor
                                                    isSelected -> SettingsTab.Startup.accentColor.copy(alpha = 0.50f)
                                                    else -> Color.White.copy(alpha = 0.10f)
                                                }
                                            )
                                        ) {
                                            Text(
                                                label,
                                                Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                SettingsLabel("STARTUP & REBOOT")
                                SettingsToggle("Open automatically after device restart", autoStartValue) { autoStartValue = it }
                                SettingsToggle("Resume last playing channel", resumeLastValue) { resumeLastValue = it }

                                SettingsLabel("APPLICATION UPDATES")
                                SettingsToggle("Check automatically for updates", autoUpdateValue) { autoUpdateValue = it }
                                SettingsToggle("Download updates on Wi-Fi only", wifiOnlyValue) { wifiOnlyValue = it }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        updateStatus,
                                        Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp
                                    )
                                    TvSettingsButton("Check Now", onClick = {
                                        updateStatus = "Checking GitHub…"
                                        settingsScope.launch {
                                            updateStatus = onCheckForUpdate()
                                        }
                                    })
                                }
                            }

                            SettingsTab.Hub -> {
                                SettingsLabel("GLZ HUB PAIRING STATUS")
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                ) {
                                    Column(Modifier.padding(20.dp)) {
                                        Text(hubMessage, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Manage this television at glzhub.glztech.com/pair",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        TvSettingsButton(
                                            label = if (hubLoading) "Connecting…" else "🔑 Generate Pairing Code",
                                            enabled = !hubLoading,
                                            onClick = {
                                                hubLoading = true
                                                settingsScope.launch {
                                                    runCatching { onBeginHubEnrollment() }
                                                        .onSuccess { hubMessage = "Pairing code: $it · expires in 1 hour" }
                                                        .onFailure { hubMessage = "Could not reach GLZ Hub: ${it.message}" }
                                                    hubLoading = false
                                                }
                                            },
                                            modifier = Modifier
                                                .focusRequester(rightPaneFocusRequester)
                                                .focusProperties { left = tabFocusRequesters[SettingsTab.Hub]!! }
                                        )
                                    }
                                }
                            }

                            SettingsTab.Guest -> {
                                SettingsLabel("GUEST & LOCATION")
                                ProtectedSourceField(
                                    guestNameValue, { guestNameValue = it },
                                    "Welcome Guest Name", "Shown in the Home welcome card",
                                    modifier = Modifier
                                        .focusRequester(rightPaneFocusRequester)
                                        .focusProperties { left = tabFocusRequesters[SettingsTab.Guest]!! }
                                )
                                ProtectedSourceField(
                                    weatherLocationValue, { weatherLocationValue = it },
                                    "Weather Location", "City or municipality used by Open-Meteo",
                                    modifier = Modifier.focusProperties { left = tabFocusRequesters[SettingsTab.Guest]!! }
                                )
                                SettingsLabel("NETWORK DISPLAY")
                                Text(
                                    "Optional labels shown in the top status bar. Leave blank for automatic detection.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                ProtectedSourceField(
                                    connectionLabelValue, { connectionLabelValue = it },
                                    "Connection Label", "Example: Resort Ethernet or Guest Wi-Fi",
                                    modifier = Modifier.focusProperties { left = tabFocusRequesters[SettingsTab.Guest]!! }
                                )
                                ProtectedSourceField(
                                    ispNameValue, { ispNameValue = it },
                                    "ISP or Network Name", "Example: GLZ Fiber or Charter Spectrum",
                                    modifier = Modifier.focusProperties { left = tabFocusRequesters[SettingsTab.Guest]!! }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Sticky Bottom Action Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Navigate with D-Pad · Press Back or Cancel to exit",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TvSettingsButton(
                            label = "Cancel",
                            onClick = onDismiss
                        )
                        TvSettingsButton(
                            label = "Save & Apply",
                            onClick = {
                                onSave(
                                    playlistValue.trim(), epgValue.trim(), headerValue.trim(),
                                    weatherLocationValue.trim().ifBlank { DEFAULT_WEATHER_LOCATION },
                                    guestNameValue.trim().ifBlank { "Guest" },
                                    connectionLabelValue.trim(), ispNameValue.trim(),
                                    themeValue, captionsValue, languageValue.trim(), osdTimeoutValue,
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
    }
}

@Composable
private fun TvSettingsButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val isPrimary = label.contains("Save") || label.contains("Sync") || label.contains("Generate")

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .tvFocusableWithPhysics(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.05f,
                glowColor = if (isPrimary) MaterialTheme.colorScheme.primary else Color.White,
                onFocusChange = { focused = it }
            ),
        shape = RoundedCornerShape(16.dp),
        color = when {
            focused && isPrimary -> MaterialTheme.colorScheme.primary
            focused -> Color.White
            isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            else -> Color.White.copy(alpha = 0.08f)
        },
        contentColor = when {
            focused -> Color.Black
            isPrimary -> MaterialTheme.colorScheme.primary
            else -> Color.White
        },
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused && isPrimary -> MaterialTheme.colorScheme.primary
                focused -> Color.White
                isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                else -> Color.White.copy(alpha = 0.15f)
            }
        )
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
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
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(editing) {
        if (editing) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusableWithPhysics(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.02f,
                glowColor = MaterialTheme.colorScheme.secondary,
                onFocusChange = {
                    focused = it
                    if (!it) editing = false
                }
            )
            .clickable(enabled = enabled) { editing = true },
        shape = RoundedCornerShape(16.dp),
        color = when {
            focused -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            else -> Color.White.copy(alpha = 0.05f)
        },
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> MaterialTheme.colorScheme.secondary
                else -> Color.White.copy(alpha = 0.10f)
            }
        )
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusProperties { canFocus = editing },
            label = { Text(label, fontWeight = FontWeight.Bold) },
            readOnly = !editing,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                cursorColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = Color.Transparent
            ),
            supportingText = {
                Text(
                    when {
                        editing -> "Keyboard active · Press Back when done editing"
                        focused -> "Press OK to edit${help.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}"
                        help.isNotBlank() -> help
                        else -> "Press OK to edit"
                    },
                    color = if (focused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@Composable
private fun SettingsLabel(value: String) {
    Text(
        value,
        Modifier.padding(top = 10.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        Modifier
            .fillMaxWidth()
            .tvFocusableWithPhysics(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.02f,
                glowColor = MaterialTheme.colorScheme.primary,
                onFocusChange = { focused = it }
            )
            .clickable { onChecked(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = when {
            focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            checked -> Color.White.copy(alpha = 0.08f)
            else -> Color.White.copy(alpha = 0.04f)
        },
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> MaterialTheme.colorScheme.primary
                checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                else -> Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
            Switch(
                checked = checked,
                onCheckedChange = null
            )
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

private fun fetchNetworkInfo(
    context: Context,
    prefs: SharedPreferences,
    client: OkHttpClient
): NetworkInfo {
    val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
    val detectedConnection = when {
        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi: Connected"
        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true ->
            "Ethernet: Connected"
        else -> "Network: Connected"
    }
    val connection = prefs.getString("custom_connection_label", null)
        ?.trim()?.takeIf(String::isNotBlank) ?: detectedConnection
    val configuredIsp = prefs.getString("custom_isp_name", null)
        ?.trim()?.takeIf(String::isNotBlank)
    val rawIsp = configuredIsp ?: runCatching {
        JSONObject(fetchText(client, "https://ipwho.is/", emptyMap()))
            .optJSONObject("connection")
            ?.optString("isp")
            ?.takeIf(String::isNotBlank)
    }.getOrNull() ?: "Internet connected"
    val isp = when {
        rawIsp.contains("charter", ignoreCase = true) ||
            rawIsp.contains("spectrum", ignoreCase = true) -> "Charter Spectrum"
        rawIsp.contains("t-mobile", ignoreCase = true) ||
            rawIsp.contains("tmobile", ignoreCase = true) -> "T-Mobile 5G Home"
        else -> rawIsp
    }
    return NetworkInfo(connection, isp)
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
