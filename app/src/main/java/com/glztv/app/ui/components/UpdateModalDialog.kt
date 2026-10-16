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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.BuildConfig
import com.glztv.app.GithubUpdateManager
import com.glztv.app.ui.i18n.LocalGlzStrings
import kotlinx.coroutines.delay

/**
 * Centered TV Modal Dialog triggered when an update is available or required.
 * When isForceUpdate is true (e.g. 7+ days past monthly milestone mark), dismissing
 * is disabled to ensure the device stays compliant within the rolling window.
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
    val initialFocus = remember { FocusRequester() }
    val strings = LocalGlzStrings.current
    val accent = if (isForceUpdate) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        delay(150L)
        runCatching { initialFocus.requestFocus() }
    }

    BackHandler(enabled = !downloading) {
        if (!isForceUpdate) {
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(enabled = !isForceUpdate && !downloading, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF20F1626),
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.50f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accent.copy(alpha = 0.18f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                if (isForceUpdate) Icons.Default.ErrorOutline else Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isForceUpdate) "Milestone Update Required" else "Update Available",
                            color = if (isForceUpdate) Color(0xFFFCA5A5) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} → v${update.version}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accent.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = channel.label.uppercase(),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp,
                                    color = accent
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Force Update Warning Banner
                if (isForceUpdate) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.50f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Text(
                            text = "This device is past the monthly milestone window (7+ days overdue). A rolling milestone update is required to continue.",
                            color = Color(0xFFFEE2E2),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Release Notes Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                ) {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Release Notes:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = update.notes.ifBlank {
                                "A new update is ready from the official GLZ TV GitHub release."
                            },
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Download Progress / Status
                if (downloadStatus != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (downloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = accent,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = downloadStatus,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isForceUpdate) {
                        var notNowFocused by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !downloading,
                            shape = CircleShape,
                            border = BorderStroke(
                                1.dp,
                                if (notNowFocused) Color.White else Color.White.copy(alpha = 0.25f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (notNowFocused) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .tvFocusableWithPhysics(
                                    shape = CircleShape,
                                    focusedScale = 1.05f,
                                    onFocusChange = { notNowFocused = it }
                                )
                        ) {
                            Text(strings.notNow, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.width(12.dp))
                    }

                    var updateFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onUpdateNow,
                        enabled = !downloading,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isForceUpdate) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .focusRequester(initialFocus)
                            .tvFocusableWithPhysics(
                                shape = CircleShape,
                                focusedScale = 1.05f,
                                onFocusChange = { updateFocused = it }
                            )
                    ) {
                        Text(
                            text = if (downloading) strings.buffering else strings.updateNow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
