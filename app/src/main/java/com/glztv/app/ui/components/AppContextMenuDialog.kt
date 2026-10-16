package com.glztv.app.ui.components

import androidx.activity.compose.BackHandler
import android.view.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.glztv.app.launcher.LauncherAppItem
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable

@Composable
fun AppContextMenuDialog(
    app: LauncherAppItem,
    onOpenApp: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHide: () -> Unit,
    onOpenDetails: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val initialFocus = remember { FocusRequester() }
    val openTime = remember { System.currentTimeMillis() }
    var allowActionExecution by remember { mutableStateOf(false) }
    var receivedKeyDownInDialog by remember { mutableStateOf(false) }

    fun isSafeClick(): Boolean = allowActionExecution && (System.currentTimeMillis() - openTime) > 400L

    LaunchedEffect(Unit) {
        delay(350L)
        allowActionExecution = true
        try {
            initialFocus.requestFocus()
        } catch (_: Exception) {}
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(460.dp)
                .onKeyEvent { event ->
                    val native = event.nativeKeyEvent
                    if (native.action == KeyEvent.ACTION_DOWN) {
                        if (allowActionExecution && native.repeatCount == 0) {
                            receivedKeyDownInDialog = true
                        }
                        false
                    } else if (native.action == KeyEvent.ACTION_UP) {
                        if (!receivedKeyDownInDialog) {
                            // Consume trailing ACTION_UP from the long-press that opened this dialog
                            true
                        } else false
                    } else false
                }
                .clickable(enabled = false) {}, // prevent click-through
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141923),
            tonalElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with App Icon, Name, and Package
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bitmap = remember(app.packageName) {
                        runCatching { (app.icon ?: app.banner)?.toBitmap()?.asImageBitmap() }.getOrNull()
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(app.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.label.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = app.packageName,
                            color = GoogleTvTokens.TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (app.isSystemApp) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "SYSTEM APP",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Actions List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ContextMenuActionItem(
                        icon = Icons.Default.PlayArrow,
                        title = "Open App",
                        subtitle = "Launch application",
                        focusRequester = initialFocus,
                        isSafeClick = { isSafeClick() && receivedKeyDownInDialog },
                        onClick = {
                            onDismiss()
                            onOpenApp()
                        }
                    )

                    ContextMenuActionItem(
                        icon = if (app.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        iconTint = if (app.isFavorite) GoogleTvTokens.StarGold else Color.White,
                        title = if (app.isFavorite) "Remove from Favorites" else "Pin to Favorites",
                        subtitle = if (app.isFavorite) "Unpin from home favorite apps" else "Keep at the front of home screen",
                        isSafeClick = { isSafeClick() },
                        onClick = {
                            onToggleFavorite()
                            onDismiss()
                        }
                    )

                    ContextMenuActionItem(
                        icon = Icons.Default.Info,
                        title = "App Info & Permissions",
                        subtitle = "Storage, permissions, notifications & cache",
                        isSafeClick = { isSafeClick() },
                        onClick = {
                            onDismiss()
                            onOpenDetails()
                        }
                    )

                    ContextMenuActionItem(
                        icon = if (app.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        title = if (app.isHidden) "Show on Home" else "Hide from Home",
                        subtitle = if (app.isHidden) "Unhide this app on the home screen" else "Hide this app from the launcher view",
                        isSafeClick = { isSafeClick() },
                        onClick = {
                            onToggleHide()
                            onDismiss()
                        }
                    )

                    if (!app.isSystemApp) {
                        ContextMenuActionItem(
                            icon = Icons.Default.Delete,
                            iconTint = Color(0xFFFF5252),
                            title = "Uninstall App",
                            subtitle = "Remove this application from the device",
                            isSafeClick = { isSafeClick() },
                            onClick = {
                                onDismiss()
                                onUninstall()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Close Button
                var isCloseFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .googleTvFocusable(
                            shape = RoundedCornerShape(10.dp),
                            focusedScale = 1.02f,
                            onFocusChange = { isCloseFocused = it }
                        ),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCloseFocused) Color.White else Color.White.copy(alpha = 0.08f),
                    contentColor = if (isCloseFocused) Color.Black else Color.White
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuActionItem(
    icon: ImageVector,
    iconTint: Color = Color.White,
    title: String,
    subtitle: String,
    focusRequester: FocusRequester? = null,
    isSafeClick: () -> Boolean = { true },
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = {
            if (isSafeClick()) {
                onClick()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .googleTvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.02f,
                onFocusChange = { isFocused = it }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) Color.White else Color(0xFF1E2533),
        contentColor = if (isFocused) Color.Black else Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isFocused) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isFocused) (if (iconTint != Color.White) iconTint else Color.Black) else iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isFocused) Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = if (isFocused) Color.Black.copy(alpha = 0.7f) else GoogleTvTokens.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
