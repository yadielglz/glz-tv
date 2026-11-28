package com.glztv.app.ui.screens

import android.content.Context
import android.view.KeyEvent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.BuildConfig
import com.glztv.app.launcher.LauncherAppItem
import com.glztv.app.launcher.LauncherAppsManager
import com.glztv.app.launcher.toSafeImageBitmap
import com.glztv.app.model.EntertainmentApp
import com.glztv.app.ui.components.AppContextMenuDialog
import com.glztv.app.ui.theme.BarlowCondensedFontFamily
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.DtvSegmentedStripBar
import com.glztv.app.ui.theme.LocalTvViewport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private const val APPS_FILTER_ALL = "ALL APPS"
private const val APPS_FILTER_FAVS = "★ FAVORITES"

/**
 * Dedicated DirecTV HD Apps Screen (v1.48.4.15).
 * Houses installed Android TV apps (in Family/Debug mode with long-press options)
 * and GlzHub-managed entertainment apps under the shared DirecTV Top Command Bar.
 */
@Composable
fun AppsScreen(
    entertainmentApps: List<EntertainmentApp>,
    isFamilyChannel: Boolean,
    onLaunchApp: (EntertainmentApp) -> Unit,
    topBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewport = LocalTvViewport.current
    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(28.dp)
    val prefs = remember { context.getSharedPreferences("glz_tv", Context.MODE_PRIVATE) }

    val isFamilyMode = isFamilyChannel || BuildConfig.DEBUG
    var installedApps by remember { mutableStateOf<List<LauncherAppItem>>(emptyList()) }
    var contextMenuApp by remember { mutableStateOf<LauncherAppItem?>(null) }
    var filter by remember { mutableStateOf(APPS_FILTER_ALL) }

    fun refreshInstalledApps() {
        if (isFamilyMode) {
            scope.launch {
                installedApps = LauncherAppsManager.loadInstalledApps(context, prefs)
            }
        }
    }

    if (isFamilyMode) {
        DisposableEffect(isFamilyMode) {
            refreshInstalledApps()
            val receiver = LauncherAppsManager.registerAppListener(context) {
                refreshInstalledApps()
            }
            onDispose { runCatching { context.unregisterReceiver(receiver) } }
        }
    }

    val visibleInstalled = remember(installedApps, filter) {
        val base = installedApps.filterNot { it.isHidden }
        if (filter == APPS_FILTER_FAVS) base.filter { it.isFavorite } else base
    }
    val hasFavInstalled = remember(installedApps) { installedApps.any { it.isFavorite && !it.isHidden } }
    val showInstalled = isFamilyMode && installedApps.isNotEmpty()
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(showInstalled, visibleInstalled.size, entertainmentApps.size) {
        delay(100)
        runCatching { firstFocus.requestFocus() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DtvChrome.ScreenBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            topBar?.invoke()

            if (showInstalled && hasFavInstalled) {
                DtvSegmentedStripBar(
                    items = listOf(APPS_FILTER_ALL, APPS_FILTER_FAVS),
                    selectedItem = filter,
                    onSelect = { filter = it },
                    trailing = {
                        Text(
                            text = "HOLD OK FOR APP OPTIONS",
                            color = DtvChrome.MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        )
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(DtvChrome.StripGradient)
                        .padding(horizontal = horizontalPad),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (showInstalled) "INSTALLED APPS (${visibleInstalled.size})" else "ENTERTAINMENT APPS (${entertainmentApps.size})",
                        color = DtvChrome.PrimaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    if (showInstalled) {
                        Text(
                            text = "HOLD OK FOR APP OPTIONS",
                            color = DtvChrome.MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(DtvChrome.Divider))
            }

            val cols = if (viewport.widthDp >= 1000.dp) 6 else 5
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showInstalled) {
                    itemsIndexed(visibleInstalled, key = { _, app -> "apps-inst-${app.packageName}" }) { index, app ->
                        DtvAppsInstalledCard(
                            app = app,
                            modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                            onClick = { LauncherAppsManager.launchApp(context, app.packageName) },
                            onLongPress = { contextMenuApp = app }
                        )
                    }
                } else {
                    itemsIndexed(entertainmentApps, key = { _, app -> "apps-ent-${app.packageName}" }) { index, app ->
                        DtvAppsManagedCard(
                            app = app,
                            modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                            onClick = { onLaunchApp(app) }
                        )
                    }
                }
            }
        }

        contextMenuApp?.let { app ->
            AppContextMenuDialog(
                app = app,
                onOpenApp = { LauncherAppsManager.launchApp(context, app.packageName) },
                onToggleFavorite = {
                    LauncherAppsManager.setFavorite(prefs, app.packageName, !app.isFavorite)
                    refreshInstalledApps()
                },
                onToggleHide = {
                    LauncherAppsManager.setHidden(prefs, app.packageName, !app.isHidden)
                    refreshInstalledApps()
                },
                onOpenDetails = { LauncherAppsManager.openAppDetails(context, app.packageName) },
                onUninstall = { LauncherAppsManager.uninstallApp(context, app.packageName) },
                onDismiss = { contextMenuApp = null }
            )
        }
    }
}

@Composable
private fun DtvAppsInstalledCard(
    app: LauncherAppItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val hasBanner = app.banner != null
    val artworkBitmap = remember(app.packageName, app.banner, app.icon) {
        if (app.banner != null) app.banner.toSafeImageBitmap(320, 180)
        else app.icon?.toSafeImageBitmap(192, 192)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF141A24))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .onFocusChanged {
                isFocused = it.isFocused
                if (!it.isFocused) longPressJob?.cancel()
            }
            .onKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                val isSelect = code == KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == KeyEvent.KEYCODE_ENTER ||
                    code == KeyEvent.KEYCODE_NUMPAD_ENTER
                if (isSelect) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                            longPressTriggered = false
                            longPressJob?.cancel()
                            longPressJob = scope.launch {
                                delay(550L)
                                longPressTriggered = true
                                onLongPress()
                            }
                        }
                        return@onKeyEvent true
                    } else if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        longPressJob?.cancel()
                        if (!longPressTriggered) onClick()
                        longPressTriggered = false
                        return@onKeyEvent true
                    }
                }
                false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        if (artworkBitmap != null) {
            // Full-bleed edge-to-edge fill of the entire button
            Image(
                bitmap = artworkBitmap,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DtvChrome.CardGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = app.label,
                    tint = DtvChrome.PrimaryText,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        // Subtle bottom scrim + crisp app label
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xCC06090F), Color(0xEE06090F))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (isFocused || !hasBanner) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (app.isFavorite) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = DtvChrome.Gold,
                modifier = Modifier
                    .padding(6.dp)
                    .size(14.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun DtvAppsManagedCard(
    app: EntertainmentApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // Tier 1: Real installed Android TV app banner/icon from PackageManager
    val installedBanner = remember(app.packageName) {
        val pm = context.packageManager
        runCatching { pm.getApplicationBanner(app.packageName) }.getOrNull()
            ?.toSafeImageBitmap(320, 180)
    }
    val installedIcon = remember(app.packageName, installedBanner) {
        if (installedBanner != null) null
        else runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
            ?.toSafeImageBitmap(192, 192)
    }

    // Tier 2: Official high-res brand logo URL when app is not yet installed
    val brandLogoUrl = remember(app.packageName, app.name) {
        managedAppLogoUrl(app.packageName, app.name)
    }
    var remoteLogoFailed by remember(brandLogoUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        app.accent.copy(alpha = 0.92f),
                        app.accent.copy(alpha = 0.52f),
                        Color(0xFF0E131D)
                    )
                )
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(6.dp)
            )
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
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        when {
            installedBanner != null -> {
                Image(
                    bitmap = installedBanner,
                    contentDescription = app.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            installedIcon != null -> {
                Image(
                    bitmap = installedIcon,
                    contentDescription = app.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            brandLogoUrl != null && !remoteLogoFailed -> {
                AsyncImage(
                    model = brandLogoUrl,
                    contentDescription = app.name,
                    contentScale = ContentScale.Crop,
                    onError = { remoteLogoFailed = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Text(
                    text = app.name.uppercase(Locale.getDefault()),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontFamily = BarlowCondensedFontFamily,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 12.dp)
                )
            }
        }
        // Bottom scrim with crisp app title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xCC06090F), Color(0xEE06090F))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = app.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun managedAppLogoUrl(packageName: String, name: String): String? {
    val domain = when {
        packageName.contains("youtube", ignoreCase = true) || name.equals("YouTube", ignoreCase = true) -> "youtube.com"
        packageName.contains("netflix", ignoreCase = true) || name.equals("Netflix", ignoreCase = true) -> "netflix.com"
        packageName.contains("bamnetworks", ignoreCase = true) || name.equals("MLB", ignoreCase = true) -> "mlb.com"
        packageName.contains("cbs.ott", ignoreCase = true) || name.contains("Paramount", ignoreCase = true) -> "paramountplus.com"
        packageName.contains("disney", ignoreCase = true) || name.contains("Disney", ignoreCase = true) -> "disneyplus.com"
        packageName.contains("peacock", ignoreCase = true) || name.contains("Peacock", ignoreCase = true) -> "peacocktv.com"
        packageName.contains("TWCableTV", ignoreCase = true) || name.contains("Spectrum", ignoreCase = true) -> "spectrum.net"
        packageName.contains("amazon", ignoreCase = true) || name.contains("Prime", ignoreCase = true) -> "primevideo.com"
        packageName.contains("hulu", ignoreCase = true) || name.equals("Hulu", ignoreCase = true) -> "hulu.com"
        packageName.contains("hbo", ignoreCase = true) || packageName.contains("wbd", ignoreCase = true) || name.equals("Max", ignoreCase = true) -> "max.com"
        packageName.contains("spotify", ignoreCase = true) || name.equals("Spotify", ignoreCase = true) -> "spotify.com"
        packageName.contains("plex", ignoreCase = true) || name.equals("Plex", ignoreCase = true) -> "plex.tv"
        packageName.contains("apple", ignoreCase = true) || name.contains("Apple TV", ignoreCase = true) -> "tv.apple.com"
        packageName.contains("espn", ignoreCase = true) || name.contains("ESPN", ignoreCase = true) -> "espn.com"
        packageName.contains("tubi", ignoreCase = true) || name.contains("Tubi", ignoreCase = true) -> "tubitv.com"
        packageName.contains("pluto", ignoreCase = true) || name.contains("Pluto", ignoreCase = true) -> "pluto.tv"
        packageName.contains("twitch", ignoreCase = true) || name.equals("Twitch", ignoreCase = true) -> "twitch.tv"
        else -> null
    }
    return domain?.let { "https://www.google.com/s2/favicons?domain=$it&sz=128" }
}

