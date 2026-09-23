package com.glztv.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.glztv.app.data.EpgRepository
import com.glztv.app.data.PlaylistRepository
import com.glztv.app.data.PreferencesRepository
import com.glztv.app.data.WeatherRepository
import com.glztv.app.data.createPermissiveOkHttpClient
import com.glztv.app.model.DefaultEntertainmentApps
import com.glztv.app.model.NetworkInfo
import com.glztv.app.model.WeatherInfo
import com.glztv.app.player.PlaybackPerformance
import com.glztv.app.player.RecentChannelManager
import com.glztv.app.player.SpeedTestResult
import com.glztv.app.ui.GlzTvApp
import com.glztv.app.ui.components.SlimHeader
import com.glztv.app.ui.components.StreamSpeedTestDialog
import com.glztv.app.ui.i18n.LocalGlzStrings
import com.glztv.app.ui.navigation.AppSection
import com.glztv.app.ui.navigation.ExpressiveNavigationRail
import com.glztv.app.ui.screens.AmbientScreensaverScreen
import com.glztv.app.ui.screens.RadioScreen
import com.glztv.app.ui.screens.WeatherScreen
import com.glztv.app.ui.screens.guide.GuideSection
import com.glztv.app.ui.screens.home.GuestHubHome
import com.glztv.app.ui.screens.home.handleManagedHubCommand
import com.glztv.app.ui.screens.player.ImmersivePlayerScreen
import com.glztv.app.ui.screens.player.MultiViewScreen
import com.glztv.app.ui.screens.player.SportsBarKioskScreen
import com.glztv.app.ui.screens.settings.SettingsDialog
import com.glztv.app.ui.screens.settings.UpdateNotificationBanner
import com.glztv.app.ui.screens.you.GuestYouSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayInputStream
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
private const val AUTO_UPDATE_CHECK = "auto_update"
private const val LEGACY_AUTO_UPDATE_CHECK = "auto_update_check"
private const val WIFI_ONLY_UPDATES = "wifi_only"
private const val LEGACY_WIFI_ONLY_UPDATES = "wifi_only_updates"
private const val UPDATE_CHANNEL = "update_channel"
private const val AUTO_START = "auto_start"
private const val RESUME_LAST_CHANNEL = "resume_last_channel"
private const val START_DESTINATION = "start_destination"
private const val LAST_CHANNEL_ID = "last_channel_id"
private const val WEATHER_LOCATION = "weather_location"
private const val GUEST_NAME = "guest_name"
private const val OSD_TIMEOUT_SECONDS = "osd_timeout_seconds"
private const val SCREENSAVER_TIMEOUT_MINUTES = "screensaver_timeout_minutes"
private const val DEFAULT_PLAYLIST_URL = "http://play.glztech.com/list.m3u"
private const val DEFAULT_EPG_URL = "https://play.glztech.com/epg.xml.gz"
private const val DEFAULT_WEATHER_LOCATION = "San Juan"

private const val PANEL_ANIM_IN = 240
private const val PANEL_ANIM_OUT = 170

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
    onThemeMode: (String) -> Unit,
    onLanguageChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val safeHorizontalPadding = if (LocalConfiguration.current.screenWidthDp >= 600) 40.dp else 12.dp
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var appLanguage by remember {
        mutableStateOf(prefs.getString(GlzHubManager.APP_LANGUAGE, "en") ?: "en")
    }
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
    var sportsBarKioskEnabled by remember {
        mutableStateOf(prefs.getBoolean(GlzHubManager.SPORTS_BAR_KIOSK_ENABLED, false))
    }
    var radioPlaying by remember { mutableStateOf(false) }
    var currentRadioStation by remember { mutableStateOf<RadioStation?>(null) }
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
    var showScreensaver by remember { mutableStateOf(false) }
    var showSpeedTestDialog by remember { mutableStateOf(false) }
    var speedTestTargetUrl by remember { mutableStateOf<String?>(null) }
    var screensaverTimeoutMinutes by remember {
        mutableStateOf(prefs.getInt(SCREENSAVER_TIMEOUT_MINUTES, 5))
    }
    var lastUserInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var lastSpeedTestResult by remember { mutableStateOf<SpeedTestResult?>(null) }

    val keepScreenAwake = radioPlaying || section == AppSection.SportsBarKiosk ||
        (section == AppSection.Home && keepAwakeAtHome)
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
                val radioStations = runCatching { RadioCatalogManager.load(prefs, client).stations }.getOrDefault(emptyList())
                TvHomePublisher.publish(
                    context = context.applicationContext,
                    channels = parsed,
                    guide = parsedGuide,
                    favorites = favorites,
                    radioStations = radioStations
                )
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
            val syncedLang = prefs.getString(GlzHubManager.APP_LANGUAGE, appLanguage) ?: appLanguage
            if (syncedLang != appLanguage) {
                appLanguage = syncedLang
                onLanguageChanged(syncedLang)
            }
            captionsEnabled = prefs.getBoolean(CAPTIONS_ENABLED, captionsEnabled)
            captionLanguage = prefs.getString(CAPTION_LANGUAGE, captionLanguage) ?: captionLanguage
            keepAwakeAtHome = prefs.getBoolean(KEEP_AWAKE_HOME, keepAwakeAtHome)
            homePreviewChannelId = prefs.getString(HOME_PREVIEW_CHANNEL_ID, homePreviewChannelId)
            sportsBarKioskEnabled = prefs.getBoolean(GlzHubManager.SPORTS_BAR_KIOSK_ENABLED, false)
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
        val channel = GithubUpdateManager.UpdateChannel.from(prefs.getString(UPDATE_CHANNEL, null))
        return runCatching {
            withContext(Dispatchers.IO) { GithubUpdateManager.check(client, channel) }
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
            val initialSyncedLang = prefs.getString(GlzHubManager.APP_LANGUAGE, appLanguage) ?: appLanguage
            if (initialSyncedLang != appLanguage) {
                appLanguage = initialSyncedLang
                onLanguageChanged(initialSyncedLang)
            }
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
        var nextConfigSyncAt = System.currentTimeMillis() + 5 * 60_000L
        var nextHeartbeatAt = System.currentTimeMillis() + 90_000L
        var nextCommandCheckAt = System.currentTimeMillis() + 30_000L
        while (true) {
            delay(15_000L)
            val pendingEnrollment = GlzHubManager.pairingCode(prefs) != null
            val now = System.currentTimeMillis()
            if (!pendingEnrollment && now >= nextHeartbeatAt) {
                runCatching { withContext(Dispatchers.IO) { GlzHubManager.heartbeat(prefs, client) } }
                nextHeartbeatAt = now + 90_000L
            }
            if (!pendingEnrollment && now >= nextCommandCheckAt) {
                runCatching { withContext(Dispatchers.IO) { GlzHubManager.commands(prefs, client) } }
                    .onSuccess { commands ->
                        for (command in commands) {
                            handleManagedHubCommand(context, prefs, client, command) {
                                loadSources(forceRefresh = true)
                            }
                        }
                    }
                nextCommandCheckAt = now + 30_000L
            }
            if (!pendingEnrollment && now < nextConfigSyncAt) continue
            runCatching {
                withContext(Dispatchers.IO) { GlzHubManager.sync(prefs, client) }
            }.onSuccess { result ->
                nextConfigSyncAt = now + 5 * 60_000L
                nextHeartbeatAt = minOf(nextHeartbeatAt, now + 90_000L)
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
                    val loopSyncedLang = prefs.getString(GlzHubManager.APP_LANGUAGE, appLanguage) ?: appLanguage
                    if (loopSyncedLang != appLanguage) {
                        appLanguage = loopSyncedLang
                        onLanguageChanged(loopSyncedLang)
                    }
                    captionsEnabled = prefs.getBoolean(CAPTIONS_ENABLED, captionsEnabled)
                    captionLanguage = prefs.getString(CAPTION_LANGUAGE, captionLanguage) ?: captionLanguage
                    keepAwakeAtHome = prefs.getBoolean(KEEP_AWAKE_HOME, keepAwakeAtHome)
                    homePreviewChannelId = prefs.getString(HOME_PREVIEW_CHANNEL_ID, homePreviewChannelId)
                    sportsBarKioskEnabled = prefs.getBoolean(GlzHubManager.SPORTS_BAR_KIOSK_ENABLED, false)
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
                nextConfigSyncAt = now + if (pendingEnrollment) 15_000L else 5 * 60_000L
                hubStatus = "GLZ Hub sync unavailable · using saved settings"
            }
        }
    }

    val autoUpdateEnabled = (prefs.all[AUTO_UPDATE_CHECK] as? Boolean)
        ?: prefs.getBoolean(LEGACY_AUTO_UPDATE_CHECK, true)
    LaunchedEffect(autoUpdateEnabled) {
        if (!autoUpdateEnabled) return@LaunchedEffect
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
            val nowTime = System.currentTimeMillis()
            val currentProg = guide.forChannel(it).firstOrNull { prog -> prog.startMillis <= nowTime && prog.endMillis > nowTime }
            scope.launch(Dispatchers.IO) {
                TvHomePublisher.recordWatchNext(context.applicationContext, it, currentProg)
            }
        }
        selected = it
        playerActive = true
        GlzHubManager.reportActivity(prefs, "channel", it.name)
        prefs.edit().putString(LAST_CHANNEL_ID, it.id).apply()
    }

    val immersive = (section == AppSection.Live && selected != null && playerActive) ||
        section == AppSection.SportsBarKiosk

    LaunchedEffect(sportsBarKioskEnabled) {
        if (!sportsBarKioskEnabled && section == AppSection.SportsBarKiosk) {
            section = AppSection.Home
        }
    }

    LaunchedEffect(screensaverTimeoutMinutes, showScreensaver, immersive, radioPlaying, showSettings, showSpeedTestDialog) {
        if (screensaverTimeoutMinutes <= 0) return@LaunchedEffect
        while (true) {
            delay(5_000L)
            val idleTime = System.currentTimeMillis() - lastUserInteractionTime
            val timeoutMillis = screensaverTimeoutMinutes * 60 * 1000L
            if (idleTime >= timeoutMillis && !showScreensaver && !showSettings && !showSpeedTestDialog) {
                if (!immersive || radioPlaying) {
                    showScreensaver = true
                }
            }
        }
    }

    val resumeChannel = remember(ordered, recentRevision) {
        prefs.getString(LAST_CHANNEL_ID, null)?.let { id -> ordered.firstOrNull { it.id == id } }
    }

    val managedEntertainmentApps = remember(visibleAppPackages, appVisibilityManaged) {
        if (!appVisibilityManaged) DefaultEntertainmentApps
        else DefaultEntertainmentApps.filter { it.packageName in visibleAppPackages }
    }

    BackHandler(enabled = !showSettings && !immersive) {
        if (section != AppSection.Home) {
            section = AppSection.Home
        } else {
            (context as? Activity)?.finishAndRemoveTask()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    lastUserInteractionTime = System.currentTimeMillis()
                }
                false
            }
    ) {
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
                        onScreensaver = { showScreensaver = true },
                        minimal = section == AppSection.Home
                    )
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(if (immersive) PaddingValues(0.dp) else padding)) {
                if (immersive) {
                    if (section == AppSection.SportsBarKiosk) {
                        SportsBarKioskScreen(
                            channels = ordered,
                            prefs = prefs,
                            client = client,
                            captionLanguage = captionLanguage,
                            onExit = { section = AppSection.Home }
                        )
                    } else if (multiViewSecondary != null) {
                        MultiViewScreen(
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
                        )
                    } else {
                        ImmersivePlayerScreen(
                            channel = selected!!,
                            channels = ordered,
                            guide = guide,
                            captionsEnabled = captionsEnabled,
                            captionLanguage = captionLanguage,
                            speedTestResult = lastSpeedTestResult,
                            onRunSpeedTest = { url ->
                                speedTestTargetUrl = url
                                showSpeedTestDialog = true
                            },
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
                    }
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
                            AnimatedContent(
                                targetState = section,
                                transitionSpec = {
                                    (fadeIn(tween(PANEL_ANIM_IN)) +
                                        slideInHorizontally(tween(PANEL_ANIM_IN)) { it / 14 })
                                        .togetherWith(
                                            fadeOut(tween(PANEL_ANIM_OUT)) +
                                                slideOutHorizontally(tween(PANEL_ANIM_OUT)) { -it / 14 }
                                        )
                                },
                                label = "section"
                            ) { visibleSection ->
                                when (visibleSection) {
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
                                        sportsBarKioskEnabled = sportsBarKioskEnabled,
                                        onStartSportsBarKiosk = { section = AppSection.SportsBarKiosk },
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
                                        onPlayingChanged = { isPlaying, station ->
                                            radioPlaying = isPlaying
                                            currentRadioStation = station
                                            if (isPlaying && station != null) {
                                                scope.launch(Dispatchers.IO) {
                                                    TvHomePublisher.recordWatchNextStation(context.applicationContext, station)
                                                }
                                            }
                                        },
                                        onScreensaverTriggered = {
                                            showScreensaver = true
                                        },
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
                                    AppSection.SportsBarKiosk -> Unit
                                }
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
                autoUpdate = (prefs.all[AUTO_UPDATE_CHECK] as? Boolean)
                    ?: prefs.getBoolean(LEGACY_AUTO_UPDATE_CHECK, true),
                wifiOnly = (prefs.all[WIFI_ONLY_UPDATES] as? Boolean)
                    ?: prefs.getBoolean(LEGACY_WIFI_ONLY_UPDATES, false),
                autoStart = prefs.getBoolean(AUTO_START, false),
                resumeLast = prefs.getBoolean(RESUME_LAST_CHANNEL, true),
                startDestination = prefs.getString(START_DESTINATION, AppSection.Home.name)
                    ?: AppSection.Home.name,
                updateChannel = prefs.getString(UPDATE_CHANNEL, null)
                    ?: GithubUpdateManager.UpdateChannel.PRODUCTION.id,
                sourceStatus = status,
                hubStatus = hubStatus,
                screensaverTimeoutMinutes = screensaverTimeoutMinutes,
                appLanguage = appLanguage,
                onOpenSpeedTest = {
                    speedTestTargetUrl = null
                    showSpeedTestDialog = true
                },
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
                           captions, language, osdTimeout, screensaverTimeout, autoUpdate, wifiOnly, autoStart, resumeLast,
                           startDestination, updateChannel, chosenLang ->
                    prefs.edit().putString(PLAYLIST_URL, playlist).putString(EPG_URL, epg)
                        .putString(REQUEST_HEADERS, headers)
                        .putString(WEATHER_LOCATION, location)
                        .putString(GUEST_NAME, name)
                        .putString("custom_connection_label", connectionLabel)
                        .putString("custom_isp_name", ispName)
                        .putBoolean(CAPTIONS_ENABLED, captions)
                        .putString(CAPTION_LANGUAGE, language)
                        .putInt(OSD_TIMEOUT_SECONDS, osdTimeout)
                        .putInt(SCREENSAVER_TIMEOUT_MINUTES, screensaverTimeout)
                        .putBoolean(AUTO_UPDATE_CHECK, autoUpdate)
                        .remove(LEGACY_AUTO_UPDATE_CHECK)
                        .putBoolean(WIFI_ONLY_UPDATES, wifiOnly)
                        .remove(LEGACY_WIFI_ONLY_UPDATES)
                        .putBoolean(AUTO_START, autoStart)
                        .putBoolean(RESUME_LAST_CHANNEL, resumeLast)
                        .putString(START_DESTINATION, startDestination)
                        .putString(UPDATE_CHANNEL, updateChannel)
                        .putString(GlzHubManager.APP_LANGUAGE, chosenLang)
                        .apply()
                    onThemeMode(theme)
                    if (chosenLang != appLanguage) {
                        appLanguage = chosenLang
                        onLanguageChanged(chosenLang)
                    }
                    captionsEnabled = captions
                    captionLanguage = language
                    osdTimeoutSeconds = osdTimeout
                    screensaverTimeoutMinutes = screensaverTimeout
                    weatherLocation = location
                    guestName = name
                    networkOverrideRevision++
                    showSettings = false
                    scope.launch { loadSources() }
                }
            )
        }

        if (showSpeedTestDialog) {
            StreamSpeedTestDialog(
                client = client,
                targetUrl = speedTestTargetUrl,
                onDismiss = { showSpeedTestDialog = false }
            )
        }

        AnimatedVisibility(
            visible = showScreensaver,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(300))
        ) {
            AmbientScreensaverScreen(
                weather = weather,
                guestName = guestName,
                radioPlaying = radioPlaying,
                radioStationName = currentRadioStation?.name,
                radioGenre = currentRadioStation?.genre,
                radioLogoUrl = currentRadioStation?.logoUrl,
                onDismiss = {
                    lastUserInteractionTime = System.currentTimeMillis()
                    showScreensaver = false
                }
            )
        }

        BackHandler(enabled = availableUpdate != null && !updateDownloading) {
            availableUpdate = null
            updateDownloadStatus = null
        }

        AnimatedVisibility(
            visible = availableUpdate != null,
            enter = slideInVertically(initialOffsetY = { it * 2 }, animationSpec = tween(350)) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(targetOffsetY = { it * 2 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = safeHorizontalPadding, vertical = 24.dp)
        ) {
            availableUpdate?.let { update ->
                UpdateNotificationBanner(
                    update = update,
                    downloading = updateDownloading,
                    downloadStatus = updateDownloadStatus,
                    onUpdateNow = {
                        if (!GithubUpdateManager.canInstall(context)) {
                            updateDownloadStatus =
                                "Allow GLZ TV to install unknown apps, then choose Update Now again."
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
                    },
                    onNotNow = {
                        availableUpdate = null
                        updateDownloadStatus = null
                    }
                )
            }
        }
    }
}

private fun channelNumberValue(value: String): Double =
    value.trim().toDoubleOrNull()
        ?: Regex("\\d+(?:\\.\\d+)?").find(value)?.value?.toDoubleOrNull()
        ?: Double.MAX_VALUE

private fun fetchNetworkInfo(
    context: Context,
    prefs: android.content.SharedPreferences,
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
