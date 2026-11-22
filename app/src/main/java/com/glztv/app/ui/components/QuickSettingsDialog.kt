package com.glztv.app.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.BuildConfig
import com.glztv.app.launcher.LauncherAppsManager
import com.glztv.app.model.NetworkInfo
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.input.pointer.pointerInput
import com.glztv.app.ui.i18n.LocalGlzStrings
import com.glztv.app.ui.theme.LocalTvViewport

@Composable
fun QuickSettingsDialog(
    networkInfo: NetworkInfo?,
    isFamilyChannel: Boolean,
    onOpenSpeedTest: () -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalGlzStrings.current
    val viewport = LocalTvViewport.current
    val accent = MaterialTheme.colorScheme.primary
    val initialFocus = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    var launcherAliasEnabled by remember {
        mutableStateOf(LauncherAppsManager.isHomeLauncherAliasEnabled(context))
    }

    LaunchedEffect(Unit) {
        runCatching { initialFocus.requestFocus() }
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            .pointerInput(onDismiss) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = 320.dp, max = viewport.drawerMaxWidth)
                .fillMaxWidth(if (viewport.isTvLayout) 0.44f else 0.92f)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                },
            shape = RoundedCornerShape(
                topStart = GlzCardDefaults.RadiusMedium,
                bottomStart = GlzCardDefaults.RadiusMedium,
                topEnd = 0.dp,
                bottomEnd = 0.dp
            ),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            tonalElevation = 16.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                // Unified Header matching Weather, Radio & Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accent.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.quickSettingsTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.quickSettingsSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    var isCloseHeaderFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .tvFocusableWithPhysics(
                                shape = CircleShape,
                                focusedScale = 1.1f,
                                glowColor = accent,
                                onFocusChange = { isCloseHeaderFocused = it }
                            ),
                        shape = CircleShape,
                        color = if (isCloseHeaderFocused) accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        contentColor = if (isCloseHeaderFocused) GlzCardDefaults.onAccent(accent) else MaterialTheme.colorScheme.onSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.close,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Scrollable settings body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // SECTION 1: NETWORK & DIAGNOSTICS
                QuickSettingsHeader(title = "NETWORK & CONNECTIVITY")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickSettingsActionCard(
                        icon = Icons.Default.Wifi,
                        title = "Android Wi-Fi & Network Settings",
                        subtitle = if (networkInfo != null) "${networkInfo.connection} · ${networkInfo.isp}" else "Configure wireless & ethernet",
                        focusRequester = initialFocus,
                        onClick = {
                            LauncherAppsManager.openWifiSettings(context)
                        }
                    )

                    QuickSettingsActionCard(
                        icon = Icons.Default.Speed,
                        title = "Network & Stream Speed Test",
                        subtitle = "Benchmark throughput, jitter & streaming latency",
                        onClick = {
                            onDismiss()
                            onOpenSpeedTest()
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // SECTION 2: ANDROID TV SYSTEM SETTINGS
                QuickSettingsHeader(title = "ANDROID TV SYSTEM")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickSettingsActionCard(
                        icon = Icons.Default.Settings,
                        title = "Android TV Settings",
                        subtitle = "Device preferences, accounts, apps & storage",
                        onClick = {
                            LauncherAppsManager.openAndroidSettings(context)
                        }
                    )

                    QuickSettingsActionCard(
                        icon = Icons.Default.Bluetooth,
                        title = "Remotes & Accessories",
                        subtitle = "Pair Bluetooth controllers, remotes & audio",
                        onClick = {
                            LauncherAppsManager.openBluetoothSettings(context)
                        }
                    )

                    QuickSettingsActionCard(
                        icon = Icons.Default.DisplaySettings,
                        title = "Display & Audio Settings",
                        subtitle = "Resolution, HDR, refresh rate & surround sound",
                        onClick = {
                            LauncherAppsManager.openDisplaySettings(context)
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // SECTION 3: DEVELOPER & DEBUG
                QuickSettingsHeader(title = "DEVICE & DEBUG")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickSettingsActionCard(
                        icon = Icons.Default.BugReport,
                        title = "Developer Options",
                        subtitle = "ADB debugging, GPU profiling & animation scales",
                        onClick = {
                            LauncherAppsManager.openDeveloperSettings(context)
                        }
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.05f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = GoogleTvTokens.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · GLZ-TV v${BuildConfig.VERSION_NAME}",
                                    color = GoogleTvTokens.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // SECTION 4: FAMILY CHANNEL HOME LAUNCHER (FAMILY CHANNEL ONLY)
                if (isFamilyChannel) {
                    Spacer(Modifier.height(16.dp))
                    QuickSettingsHeader(title = "HOME REPLACEMENT (FAMILY LABS)")

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        QuickSettingsActionCard(
                            icon = Icons.Default.Home,
                            iconTint = GoogleTvTokens.BlueAccent,
                            title = if (launcherAliasEnabled) "Home Launcher Alias: Active" else "Home Launcher Alias: Inactive",
                            subtitle = if (launcherAliasEnabled) "GLZ-TV can be selected as default launcher (Click to toggle)" else "Enable Home Launcher role on this device",
                            onClick = {
                                val next = !launcherAliasEnabled
                                LauncherAppsManager.setHomeLauncherAliasEnabled(context, next)
                                launcherAliasEnabled = next
                            }
                        )

                        QuickSettingsActionCard(
                            icon = Icons.Default.Tune,
                            title = "Set as Default Home App",
                            subtitle = "Open Android home app picker or settings",
                            onClick = {
                                LauncherAppsManager.openHomeSettings(context)
                            }
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = GoogleTvTokens.BlueAccent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Tip for Google TV: If Google TV hides launcher selection, disable stock via ADB: adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    GlzSectionActionButton(
                        label = strings.quickSettingsAllSettings,
                        icon = Icons.Default.Settings,
                        primary = true,
                        onClick = {
                            onDismiss()
                            onOpenFullSettings()
                        },
                        modifier = Modifier.weight(1.25f)
                    )

                    GlzSectionActionButton(
                        label = strings.close,
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun QuickSettingsActionCard(
    icon: ImageVector,
    iconTint: Color = Color.Unspecified,
    title: String,
    subtitle: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val resolvedTint = if (iconTint == Color.Unspecified) accent else iconTint

    GlzFocusCard(
        onClick = onClick,
        shape = RoundedCornerShape(GlzCardDefaults.RadiusSmall),
        focusedScale = 1.02f,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        contentModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) { isFocused ->
        val ink = if (isFocused) GlzCardDefaults.onAccent(accent) else MaterialTheme.colorScheme.onSurface
        val subInk = if (isFocused) ink.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isFocused) ink.copy(alpha = 0.14f) else accent.copy(alpha = 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isFocused) ink else resolvedTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = subInk,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
