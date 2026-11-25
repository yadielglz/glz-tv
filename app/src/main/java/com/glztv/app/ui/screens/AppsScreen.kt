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
import com.glztv.app.BuildConfig
import com.glztv.app.launcher.LauncherAppItem
import com.glztv.app.launcher.LauncherAppsManager
import com.glztv.app.launcher.toSafeImageBitmap
import com.glztv.app.model.EntertainmentApp
import com.glztv.app.ui.components.AppContextMenuDialog
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
    val iconBitmap = remember(app.packageName, app.banner, app.icon) {
        (app.banner ?: app.icon)?.toSafeImageBitmap(128, 128)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(98.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(4.dp)
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
            .padding(10.dp)
    ) {
        if (app.isFavorite) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = DtvChrome.Gold,
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = app.label,
                    tint = DtvChrome.PrimaryText,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = app.label,
                color = if (isFocused) Color.White else DtvChrome.PrimaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(98.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.Black else Color.Transparent)
            .then(if (!isFocused) Modifier.background(DtvChrome.CardGradient) else Modifier)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) DtvChrome.ElectricBlue else DtvChrome.CardBorder,
                shape = RoundedCornerShape(4.dp)
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
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(app.accent.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.name.take(2).uppercase(Locale.getDefault()),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.name,
            color = if (isFocused) Color.White else DtvChrome.PrimaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
