package com.glztv.app.ui.screens.settings

import android.content.Context
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.BuildConfig
import com.glztv.app.GithubUpdateManager
import com.glztv.app.GlzHubManager
import com.glztv.app.ui.components.GlzCardDefaults
import com.glztv.app.ui.components.tvFocusableWithPhysics
import com.glztv.app.ui.i18n.GlzStrings
import com.glztv.app.ui.i18n.LocalGlzStrings
import com.glztv.app.ui.navigation.AppSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

private const val DEFAULT_WEATHER_LOCATION = "San Juan"

private val THEME_VALUES = listOf(
    "dark" to "GLZ Dark",
    "ocean" to "Ocean Breeze",
    "sunset" to "Sunset Glow",
    "emerald" to "Emerald Forest",
    "cyberpunk" to "Neon Cyberpunk",
    "midnight" to "Midnight Gold",
    "arctic" to "Arctic Frost",
    "crimson" to "Crimson Eclipse",
    "amethyst" to "Amethyst Neon",
    "synthwave" to "Tokyo Synthwave",
    "solar" to "Solar Flare",
    "stealth" to "Carbon Stealth"
)

private val THEME_LABELS_ES = mapOf(
    "dark" to "GLZ Oscuro",
    "ocean" to "Brisa Marina",
    "sunset" to "Resplandor del Atardecer",
    "emerald" to "Bosque Esmeralda",
    "cyberpunk" to "Cyberpunk Neón",
    "midnight" to "Oro de Medianoche",
    "arctic" to "Escarcha Ártica",
    "crimson" to "Eclipse Carmesí",
    "amethyst" to "Amatista Neón",
    "synthwave" to "Tokyo Synthwave",
    "solar" to "Llamarada Solar",
    "stealth" to "Carbono Sigilo"
)

private val OSD_VALUES = listOf(5, 7, 8, 10)
private val START_DESTINATIONS = listOf(
    AppSection.Home to "Home Screen",
    AppSection.Live to "Live TV",
    AppSection.Radio to "Radio",
    AppSection.Weather to "Weather",
    AppSection.You to "You & Apps"
)

private val UPDATE_CHANNEL_IDS = GithubUpdateManager.UpdateChannel.values().map { it.id }

private fun onOff(value: Boolean, language: String = "en") =
    if (language.startsWith("es", ignoreCase = true)) {
        if (value) "Activado" else "Desactivado"
    } else {
        if (value) "On" else "Off"
    }

private fun themeLabel(value: String, language: String = "en") =
    if (language.startsWith("es", ignoreCase = true)) {
        THEME_LABELS_ES[value] ?: THEME_VALUES.firstOrNull { it.first == value }?.second ?: "GLZ Oscuro"
    } else {
        THEME_VALUES.firstOrNull { it.first == value }?.second ?: "GLZ Dark"
    }

private fun startDestinationLabel(name: String, language: String = "en"): String {
    val isEs = language.startsWith("es", ignoreCase = true)
    return when (name) {
        AppSection.Home.name -> if (isEs) "Pantalla de Inicio" else "Home Screen"
        AppSection.Live.name -> if (isEs) "TV en Vivo" else "Live TV"
        AppSection.Radio.name -> "Radio"
        AppSection.Weather.name -> if (isEs) "Clima" else "Weather"
        AppSection.You.name -> if (isEs) "Tú y Apps" else "You & Apps"
        else -> if (isEs) "Pantalla de Inicio" else "Home Screen"
    }
}

private fun updateChannelLabel(id: String) = GithubUpdateManager.UpdateChannel.from(id).label

private fun <T> cycleList(values: List<T>, current: T, direction: Int): T {
    if (values.isEmpty()) return current
    val index = values.indexOf(current).let { if (it < 0) 0 else it }
    val next = ((index + direction) % values.size + values.size) % values.size
    return values[next]
}

@Composable
fun UpdateNotificationBanner(
    update: GithubUpdateManager.UpdateInfo,
    downloading: Boolean,
    downloadStatus: String?,
    onUpdateNow: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(update) {
        delay(150L)
        runCatching { initialFocus.requestFocus() }
    }

    val accent = MaterialTheme.colorScheme.primary
    val strings = LocalGlzStrings.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF20F1626),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.60f)),
        tonalElevation = 16.dp,
        shadowElevation = 24.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "GLZ TV v${update.version}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accent.copy(alpha = 0.20f)
                    ) {
                        Text(
                            strings.updateAvailableTitle.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            color = accent
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = downloadStatus ?: update.notes.ifBlank {
                        "A new version is ready from the official GLZ TV GitHub release."
                    },
                    color = if (downloadStatus != null) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (downloadStatus != null) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UpdateActionButton(
                    label = if (downloading) strings.buffering else strings.updateNow,
                    isPrimary = true,
                    enabled = !downloading,
                    loading = downloading,
                    focusRequester = initialFocus,
                    onClick = onUpdateNow
                )

                UpdateActionButton(
                    label = strings.notNow,
                    isPrimary = false,
                    enabled = !downloading,
                    loading = false,
                    onClick = onNotNow
                )
            }
        }
    }
}

@Composable
fun UpdateActionButton(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)

    val backgroundColor = when {
        focused -> Color.White
        !enabled -> if (isPrimary) accent.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.05f)
        isPrimary -> accent
        else -> Color(0xFF1E283C)
    }

    val contentColor = when {
        focused -> Color.Black
        !enabled -> if (isPrimary) GlzCardDefaults.onAccent(accent).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.35f)
        isPrimary -> GlzCardDefaults.onAccent(accent)
        else -> Color.White
    }

    val borderStroke = when {
        focused -> BorderStroke(2.dp, Color.White)
        !enabled -> BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        isPrimary -> BorderStroke(1.5.dp, Color.White.copy(alpha = 0.50f))
        else -> BorderStroke(1.5.dp, Color.White.copy(alpha = 0.50f))
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocusableWithPhysics(
                shape = shape,
                focusedScale = 1.08f,
                glowColor = if (isPrimary) accent else Color.White,
                onFocusChange = { focused = it }
            ),
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            }
            Text(
                label,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SettingsChoiceRow(
    name: String,
    valueText: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    first: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (first && focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocusableWithPhysics(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.02f,
                glowColor = accent,
                onFocusChange = { focused = it }
            )
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { onPrev(); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { onNext(); true }
                    else -> false
                }
            }
            .clickable { onNext() },
        shape = RoundedCornerShape(16.dp),
        color = if (focused) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Text(
                "‹",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = if (focused) accent else Color.White.copy(alpha = 0.45f)
            )
            Text(
                valueText,
                Modifier.padding(horizontal = 14.dp),
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (focused) accent else Color.White
            )
            Text(
                "›",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = if (focused) accent else Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
fun SettingsActionRow(
    name: String,
    valueText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    first: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (first && focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocusableWithPhysics(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.02f,
                glowColor = accent,
                onFocusChange = { focused = it }
            )
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (focused) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                if (valueText.isNotBlank()) {
                    Text(
                        valueText,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "OK ▸",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = if (focused) accent else Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
fun SettingsDialog(
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
    updateChannel: String,
    sourceStatus: String,
    hubStatus: String,
    screensaverTimeoutMinutes: Int = 5,
    appLanguage: String = "en",
    onOpenChannelEditor: () -> Unit = {},
    onOpenSpeedTest: () -> Unit,
    onSyncNow: suspend ((Int, String) -> Unit) -> String,
    onCheckForUpdate: suspend () -> String,
    onBeginHubEnrollment: suspend () -> String,
    onDismiss: () -> Unit,
    onSave: (
        String, String, String, String, String, String, String, String, Boolean, String, Int, Int,
        Boolean, Boolean, Boolean, Boolean, String, String, String
    ) -> Unit
) {
    var showAdvanced by remember { mutableStateOf(false) }
    var playlistValue by remember { mutableStateOf(playlist) }
    var epgValue by remember { mutableStateOf(epg) }
    var headerValue by remember { mutableStateOf(headers) }
    var weatherLocationValue by remember { mutableStateOf(weatherLocation) }
    var guestNameValue by remember { mutableStateOf(guestName) }
    var connectionLabelValue by remember { mutableStateOf(customConnectionLabel) }
    var ispNameValue by remember { mutableStateOf(customIspName) }
    var themeValue by remember {
        mutableStateOf(if (themeMode == "light" || themeMode == "adaptive") "dark" else themeMode)
    }
    var appLanguageValue by remember { mutableStateOf(appLanguage) }
    val strings = GlzStrings.get(appLanguageValue)
    var captionsValue by remember { mutableStateOf(captionsEnabled) }
    var languageValue by remember { mutableStateOf(captionLanguage) }
    var osdTimeoutValue by remember { mutableStateOf(osdTimeoutSeconds) }
    var screensaverTimeoutValue by remember { mutableStateOf(screensaverTimeoutMinutes) }
    var autoUpdateValue by remember { mutableStateOf(autoUpdate) }
    var wifiOnlyValue by remember { mutableStateOf(wifiOnly) }
    var autoStartValue by remember { mutableStateOf(autoStart) }
    var resumeLastValue by remember { mutableStateOf(resumeLast) }
    var startDestinationValue by remember { mutableStateOf(startDestination) }
    var updateChannelValue by remember { mutableStateOf(updateChannel) }
    var updateStatus by remember { mutableStateOf("Version ${BuildConfig.VERSION_NAME}") }
    var hubMessage by remember(hubStatus) { mutableStateOf(hubStatus) }
    var hubLoading by remember { mutableStateOf(false) }
    var syncLoading by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf(0) }
    var syncMessage by remember(sourceStatus) { mutableStateOf(sourceStatus) }
    val settingsScope = rememberCoroutineScope()
    val initialFocus = remember { FocusRequester() }
    val bodyScrollState = rememberScrollState()

    LaunchedEffect(showAdvanced) {
        bodyScrollState.scrollTo(0)
        delay(80)
        runCatching { initialFocus.requestFocus() }
    }

    BackHandler(onBack = onDismiss)
    BackHandler(enabled = showAdvanced) { showAdvanced = false }
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.40f),
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
                            if (showAdvanced) "ADVANCED · TEXT & URLS" else "SETTINGS",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            if (showAdvanced) "Playlist, EPG, headers, names & network labels"
                            else "Scroll · press Left / Right to change a value",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    ) {
                        Text(
                            "v${BuildConfig.VERSION_NAME}",
                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(bodyScrollState)
                    .padding(horizontal = 36.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    Modifier.widthIn(max = 760.dp).fillMaxWidth().focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showAdvanced) {
                        SettingsActionRow(
                            name = "‹  Back to Settings",
                            valueText = "",
                            onClick = { showAdvanced = false },
                            first = true,
                            focusRequester = initialFocus
                        )
                        SettingsLabel("PLAYLIST & EPG")
                        ProtectedSourceField(playlistValue, { playlistValue = it }, "M3U Playlist URL")
                        ProtectedSourceField(epgValue, { epgValue = it }, "XMLTV EPG URL")
                        ProtectedSourceField(
                            headerValue, { headerValue = it },
                            "Request Headers", "One Name: value header per line",
                            singleLine = false, minLines = 3
                        )
                        SettingsLabel("CAPTIONS")
                        ProtectedSourceField(
                            languageValue, { languageValue = it },
                            "Preferred Caption Language", "Examples: en, es, fr",
                            enabled = captionsValue
                        )
                        SettingsLabel("GUEST & NETWORK LABELS")
                        ProtectedSourceField(guestNameValue, { guestNameValue = it }, "Welcome Guest Name", "Shown on the Home welcome card")
                        ProtectedSourceField(weatherLocationValue, { weatherLocationValue = it }, "Weather Location", "City or municipality used by Open-Meteo")
                        ProtectedSourceField(connectionLabelValue, { connectionLabelValue = it }, "Connection Label", "Example: Resort Ethernet or Guest Wi-Fi")
                        ProtectedSourceField(ispNameValue, { ispNameValue = it }, "ISP or Network Name", "Example: GLZ Fiber or Charter Spectrum")
                    } else {
                        SettingsLabel(strings.sectionAppearance.uppercase())
                        SettingsChoiceRow(
                            name = strings.theme,
                            valueText = themeLabel(themeValue, appLanguageValue),
                            onPrev = { themeValue = cycleList(THEME_VALUES.map { it.first }, themeValue, -1) },
                            onNext = { themeValue = cycleList(THEME_VALUES.map { it.first }, themeValue, 1) },
                            first = true,
                            focusRequester = initialFocus
                        )
                        SettingsChoiceRow(
                            name = strings.appLanguage,
                            valueText = if (appLanguageValue == "es") "Español" else "English",
                            onPrev = { appLanguageValue = if (appLanguageValue == "es") "en" else "es" },
                            onNext = { appLanguageValue = if (appLanguageValue == "es") "en" else "es" }
                        )

                        SettingsLabel(strings.sectionPlayer.uppercase())
                        SettingsChoiceRow(
                            name = strings.osdTimeout,
                            valueText = "$osdTimeoutValue ${strings.seconds}" + if (osdTimeoutValue == 8) " (default)" else "",
                            onPrev = { osdTimeoutValue = cycleList(OSD_VALUES, osdTimeoutValue, -1) },
                            onNext = { osdTimeoutValue = cycleList(OSD_VALUES, osdTimeoutValue, 1) }
                        )
                        SettingsChoiceRow(
                            name = strings.screensaverTimeout,
                            valueText = when (screensaverTimeoutValue) {
                                0 -> strings.off
                                2 -> "2 ${strings.minutes}"
                                5 -> "5 ${strings.minutes} (default)"
                                10 -> "10 ${strings.minutes}"
                                15 -> "15 ${strings.minutes}"
                                else -> "$screensaverTimeoutValue ${strings.minutes}"
                            },
                            onPrev = { screensaverTimeoutValue = cycleList(listOf(0, 2, 5, 10, 15), screensaverTimeoutValue, -1) },
                            onNext = { screensaverTimeoutValue = cycleList(listOf(0, 2, 5, 10, 15), screensaverTimeoutValue, 1) }
                        )
                        SettingsChoiceRow(
                            name = strings.closedCaptions,
                            valueText = onOff(captionsValue, appLanguageValue),
                            onPrev = { captionsValue = !captionsValue },
                            onNext = { captionsValue = !captionsValue }
                        )

                        SettingsLabel(strings.sectionStartup.uppercase())
                        SettingsChoiceRow(
                            name = strings.startDestination,
                            valueText = startDestinationLabel(startDestinationValue, appLanguageValue),
                            onPrev = { startDestinationValue = cycleList(START_DESTINATIONS.map { it.first.name }, startDestinationValue, -1) },
                            onNext = { startDestinationValue = cycleList(START_DESTINATIONS.map { it.first.name }, startDestinationValue, 1) }
                        )
                        SettingsChoiceRow(
                            name = strings.autoStart,
                            valueText = onOff(autoStartValue, appLanguageValue),
                            onPrev = { autoStartValue = !autoStartValue },
                            onNext = { autoStartValue = !autoStartValue }
                        )
                        SettingsChoiceRow(
                            name = strings.resumeLast,
                            valueText = onOff(resumeLastValue, appLanguageValue),
                            onPrev = { resumeLastValue = !resumeLastValue },
                            onNext = { resumeLastValue = !resumeLastValue }
                        )

                        SettingsLabel(strings.sectionUpdates.uppercase())
                        SettingsChoiceRow(
                            name = strings.updateChannel,
                            valueText = updateChannelLabel(updateChannelValue),
                            onPrev = { updateChannelValue = cycleList(UPDATE_CHANNEL_IDS, updateChannelValue, -1) },
                            onNext = { updateChannelValue = cycleList(UPDATE_CHANNEL_IDS, updateChannelValue, 1) }
                        )
                        SettingsChoiceRow(
                            name = strings.autoUpdateCheck,
                            valueText = onOff(autoUpdateValue, appLanguageValue),
                            onPrev = { autoUpdateValue = !autoUpdateValue },
                            onNext = { autoUpdateValue = !autoUpdateValue }
                        )
                        SettingsChoiceRow(
                            name = strings.wifiOnly,
                            valueText = onOff(wifiOnlyValue, appLanguageValue),
                            onPrev = { wifiOnlyValue = !wifiOnlyValue },
                            onNext = { wifiOnlyValue = !wifiOnlyValue }
                        )
                        SettingsActionRow(
                            name = strings.checkUpdates,
                            valueText = updateStatus,
                            onClick = {
                                updateStatus = strings.checkingUpdate
                                settingsScope.launch { updateStatus = onCheckForUpdate() }
                            }
                        )

                        SettingsLabel(strings.sectionSources.uppercase())
                        SettingsActionRow(
                            name = if (syncLoading) "${strings.buffering}  $syncProgress%" else strings.syncNow,
                            valueText = syncMessage,
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
                            }
                        )
                        SettingsActionRow(
                            name = if (hubLoading) "Connecting to GLZ Hub…" else strings.hubStatus,
                            valueText = hubMessage,
                            enabled = !hubLoading,
                            onClick = {
                                hubLoading = true
                                settingsScope.launch {
                                    runCatching { onBeginHubEnrollment() }
                                        .onSuccess { hubMessage = "Pairing code: $it · expires in 1 hour" }
                                        .onFailure { hubMessage = "Could not reach GLZ Hub: ${it.message}" }
                                    hubLoading = false
                                }
                            }
                        )
                        SettingsActionRow(
                            name = "Channel Manager & Playlist Editor",
                            valueText = "Reorder channels, custom numbers, names & channel visibility",
                            onClick = onOpenChannelEditor
                        )
                        SettingsActionRow(
                            name = strings.advancedOptions,
                            valueText = "Playlist, EPG, headers, guest & network labels",
                            onClick = { showAdvanced = true }
                        )

                        SettingsLabel("NETWORK & DIAGNOSTICS")
                        SettingsActionRow(
                            name = "Stream & Network Speed Test",
                            valueText = "Benchmark latency, jitter & download throughput",
                            onClick = onOpenSpeedTest
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.65f),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (appLanguageValue == "es") "Arriba / Abajo para mover · Izq / Der para cambiar · Atrás para salir"
                        else "Up / Down to move · Left / Right to change · Back to exit",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TvSettingsButton(
                            label = strings.close,
                            onClick = onDismiss
                        )
                        TvSettingsButton(
                            label = strings.saveAndApply,
                            onClick = {
                                onSave(
                                    playlistValue.trim(), epgValue.trim(), headerValue.trim(),
                                    weatherLocationValue.trim().ifBlank { DEFAULT_WEATHER_LOCATION },
                                    guestNameValue.trim().ifBlank { "Guest" },
                                    connectionLabelValue.trim(), ispNameValue.trim(),
                                    themeValue, captionsValue, languageValue.trim(), osdTimeoutValue,
                                    screensaverTimeoutValue,
                                    autoUpdateValue, wifiOnlyValue, autoStartValue, resumeLastValue,
                                    startDestinationValue, updateChannelValue,
                                    appLanguageValue
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
fun TvSettingsButton(
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
