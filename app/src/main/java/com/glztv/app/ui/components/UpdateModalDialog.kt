package com.glztv.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NewReleases
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.BuildConfig
import com.glztv.app.GithubUpdateManager
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable
import kotlinx.coroutines.delay

/**
 * Material 3 / Google TV System Update Modal.
 * Displays clear version transition (Current -> New), release ring badge,
 * sanitized user-facing highlights (no raw GitHub changelog/PR noise),
 * download progress, and high-contrast Material 3 action pills.
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

    val highlights = remember(update.notes) {
        GithubUpdateManager.sanitizeReleaseNotes(update.notes).ifEmpty {
            listOf(
                "Improved app stability, UI polish, and playback smoothness",
                "Performance optimizations and bug fixes for Android TV"
            )
        }
    }

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
            .background(Color.Black.copy(alpha = 0.78f))
            .pointerInput(isForceUpdate, downloading) {
                detectTapGestures(onTap = {
                    if (!isForceUpdate && !downloading) onDismiss()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) },
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF16181D),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 26.dp)
            ) {
                // Header: Material 3 Icon Container + Title + Channel Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GoogleTvTokens.BlueAccent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, GoogleTvTokens.BlueAccent.copy(alpha = 0.35f)),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isForceUpdate) Icons.Default.NewReleases else Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = GoogleTvTokens.BlueAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isForceUpdate) "Required System Update" else "GLZ TV Update Ready",
                            color = GoogleTvTokens.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = GoogleTvTokens.BlueAccent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = channel.label.uppercase(),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp,
                                    color = GoogleTvTokens.BlueAccent
                                )
                            }
                            update.publishedAt?.take(10)?.let { date ->
                                Text(
                                    text = "Released $date",
                                    color = GoogleTvTokens.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Clear Version Transition Banner (Current -> New)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GoogleTvTokens.SurfaceCard,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "INSTALLED",
                                color = GoogleTvTokens.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                color = GoogleTvTokens.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = GoogleTvTokens.BlueAccent,
                            modifier = Modifier.size(20.dp)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "NEW VERSION",
                                color = GoogleTvTokens.BlueAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            )
                            Text(
                                text = "v${update.version.removePrefix("v")}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Clean User-Facing "What's Included" List
                Text(
                    text = "WHAT'S INCLUDED",
                    color = GoogleTvTokens.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                ) {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        highlights.forEach { bullet ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GoogleTvTokens.BlueAccent,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )
                                Text(
                                    text = bullet,
                                    fontSize = 13.sp,
                                    color = GoogleTvTokens.TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Required Milestone Notice
                if (isForceUpdate) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2B1616),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "This milestone update is required to keep your device connected to GLZ TV services.",
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
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp)),
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
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Material 3 Google TV Pill Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isForceUpdate) {
                        var cancelFocused by remember { mutableStateOf(false) }
                        Surface(
                            onClick = onDismiss,
                            enabled = !downloading,
                            shape = CircleShape,
                            color = if (cancelFocused) Color.White else Color.White.copy(alpha = 0.10f),
                            contentColor = if (cancelFocused) Color.Black else GoogleTvTokens.TextPrimary,
                            modifier = Modifier
                                .focusRequester(cancelFocus)
                                .focusProperties { right = installFocus }
                                .googleTvFocusable(
                                    shape = CircleShape,
                                    focusedScale = 1.05f,
                                    onFocusChange = { cancelFocused = it }
                                )
                        ) {
                            Text(
                                text = "Later",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp)
                            )
                        }
                    }

                    var installFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = onUpdateNow,
                        enabled = !downloading,
                        shape = CircleShape,
                        color = when {
                            downloading -> GoogleTvTokens.BlueAccent.copy(alpha = 0.35f)
                            installFocused -> Color.White
                            else -> GoogleTvTokens.BlueAccent
                        },
                        contentColor = Color(0xFF001F2A),
                        modifier = Modifier
                            .focusRequester(installFocus)
                            .focusProperties {
                                if (!isForceUpdate) left = cancelFocus
                            }
                            .googleTvFocusable(
                                shape = CircleShape,
                                focusedScale = 1.06f,
                                onFocusChange = { installFocused = it }
                            )
                    ) {
                        Text(
                            text = if (downloading) "Downloading…" else "Update Now",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 26.dp, vertical = 11.dp)
                        )
                    }
                }
            }
        }
    }
}
