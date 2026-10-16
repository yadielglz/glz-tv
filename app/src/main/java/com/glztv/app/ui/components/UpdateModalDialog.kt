package com.glztv.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.BuildConfig
import com.glztv.app.GithubUpdateManager
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable
import kotlinx.coroutines.delay

/**
 * Authentic Google TV style System Update prompt.
 * Features clean elevated dark surface, prominent version badging,
 * and standard Google TV [Cancel] / [Install] focus action pills.
 */
@Composable
fun UpdateModalDialog(
    update: GithubUpdateManager.UpdateInfo,
    channel: GithubUpdateManager.UpdateChannel,
    isForceUpdate: Boolean = false,
    downloading: Boolean = false,
    downloadStatus: String? = null,
    onUpdateNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cancelFocus = remember { FocusRequester() }
    val installFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120L)
        runCatching { installFocus.requestFocus() }
    }

    BackHandler(enabled = !downloading) {
        if (!isForceUpdate) {
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = !isForceUpdate && !downloading, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF16181D),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 26.dp)
            ) {
                // Header: Icon + Title + Version Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = GoogleTvTokens.BlueAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isForceUpdate) "Update Required" else "System Update",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "v${update.version.removePrefix("v")}",
                                color = GoogleTvTokens.TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoogleTvTokens.BlueAccent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = channel.label.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = GoogleTvTokens.BlueAccent
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Explanatory prompt
                Text(
                    text = "A new version of GLZ TV is ready to install with stability and performance improvements.",
                    color = GoogleTvTokens.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Release Notes Card (if present)
                if (update.notes.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 130.dp)
                    ) {
                        val scroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scroll)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = update.notes.trim(),
                                fontSize = 12.5.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Milestone / Required update warning banner
                if (isForceUpdate) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2B1616),
                        border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "A milestone update is required to keep this device compliant with service requirements.",
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Download Progress / Status
                if (downloading || downloadStatus != null) {
                    Spacer(Modifier.height(16.dp))
                    if (downloading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = GoogleTvTokens.BlueAccent,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (downloadStatus != null) {
                        Text(
                            text = downloadStatus,
                            color = if (downloadStatus.contains("failed", ignoreCase = true)) {
                                Color(0xFFF87171)
                            } else {
                                GoogleTvTokens.BlueAccent
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Actions: [Cancel] [Install]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isForceUpdate) {
                        var cancelFocused by remember { mutableStateOf(false) }
                        Surface(
                            onClick = onDismiss,
                            enabled = !downloading,
                            shape = GoogleTvTokens.ShapePill,
                            color = if (cancelFocused) Color.White else Color.White.copy(alpha = 0.08f),
                            contentColor = if (cancelFocused) Color.Black else Color.White,
                            modifier = Modifier
                                .focusRequester(cancelFocus)
                                .focusProperties { right = installFocus }
                                .googleTvFocusable(
                                    shape = GoogleTvTokens.ShapePill,
                                    focusedScale = 1.04f,
                                    onFocusChange = { cancelFocused = it }
                                )
                        ) {
                            Text(
                                text = "Cancel",
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(12.dp))
                    }

                    var installFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = onUpdateNow,
                        enabled = !downloading,
                        shape = GoogleTvTokens.ShapePill,
                        color = when {
                            installFocused -> Color.White
                            isForceUpdate -> Color(0xFFDC2626)
                            else -> GoogleTvTokens.BlueAccent.copy(alpha = 0.22f)
                        },
                        contentColor = when {
                            installFocused -> Color.Black
                            isForceUpdate -> Color.White
                            else -> GoogleTvTokens.BlueAccent
                        },
                        modifier = Modifier
                            .focusRequester(installFocus)
                            .focusProperties { left = cancelFocus }
                            .googleTvFocusable(
                                shape = GoogleTvTokens.ShapePill,
                                focusedScale = 1.04f,
                                onFocusChange = { installFocused = it }
                            )
                    ) {
                        Text(
                            text = if (downloading) "Downloading…" else "Install",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
