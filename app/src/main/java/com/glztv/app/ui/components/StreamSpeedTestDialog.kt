package com.glztv.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.player.SpeedTestResult
import com.glztv.app.player.StreamSpeedTester
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@Composable
fun StreamSpeedTestDialog(
    client: OkHttpClient,
    targetUrl: String? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var testing by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(0.0) }
    var result by remember { mutableStateOf<SpeedTestResult?>(null) }
    val scope = rememberCoroutineScope()
    val testAgainFocus = remember { FocusRequester() }

    fun startTest() {
        testing = true
        result = null
        currentSpeed = 0.0
        scope.launch {
            val res = StreamSpeedTester.runTest(
                client = client,
                targetStreamUrl = targetUrl,
                onProgress = { currentSpeed = it }
            )
            result = res
            currentSpeed = res.speedMbps
            testing = false
            runCatching { testAgainFocus.requestFocus() }
        }
    }

    LaunchedEffect(Unit) {
        startTest()
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF20F1626),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "STREAM & NETWORK TEST",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                            Text(
                                if (targetUrl != null) "Testing active stream connection" else "Testing server gateway throughput",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Speed Display Hero
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format("%.1f", currentSpeed),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            color = if (testing) MaterialTheme.colorScheme.primary else Color.White
                        )
                        Text(
                            "Mbps Download Throughput",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        if (testing) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        } else if (result != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    result!!.speedMbps >= 25.0 -> Color(0xFF00FF9D).copy(alpha = 0.20f)
                                    result!!.speedMbps >= 12.0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                    result!!.speedMbps >= 5.0 -> Color(0xFFFFD700).copy(alpha = 0.20f)
                                    else -> Color(0xFFFF4040).copy(alpha = 0.20f)
                                }
                            ) {
                                Text(
                                    result!!.rating,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        result!!.speedMbps >= 25.0 -> Color(0xFF00FF9D)
                                        result!!.speedMbps >= 12.0 -> MaterialTheme.colorScheme.primary
                                        result!!.speedMbps >= 5.0 -> Color(0xFFFFD700)
                                        else -> Color(0xFFFF6060)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Stats Row: Latency & Jitter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SpeedStatTile(
                        label = "LATENCY (PING)",
                        value = if (result != null) "${result!!.pingMs} ms" else if (testing) "Measuring…" else "--",
                        modifier = Modifier.weight(1f)
                    )
                    SpeedStatTile(
                        label = "JITTER",
                        value = if (result != null) "${result!!.jitterMs} ms" else if (testing) "Measuring…" else "--",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpeedActionButton(
                        label = if (testing) "Testing…" else "Test Again",
                        isPrimary = true,
                        enabled = !testing,
                        loading = testing,
                        focusRequester = testAgainFocus,
                        onClick = { startTest() },
                        modifier = Modifier.weight(1f)
                    )

                    SpeedActionButton(
                        label = "Close",
                        isPrimary = false,
                        enabled = true,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SpeedActionButton(
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
    val shape = RoundedCornerShape(16.dp)

    val backgroundColor = when {
        focused -> Color.White
        !enabled -> if (isPrimary) accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)
        isPrimary -> accent
        else -> Color(0xFF1E283C)
    }

    val contentColor = when {
        focused -> Color.Black
        !enabled -> if (isPrimary) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.35f)
        isPrimary -> Color(0xFF001E24)
        else -> Color.White
    }

    val borderStroke = when {
        focused -> BorderStroke(2.dp, Color.White)
        !enabled -> BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        isPrimary -> BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f))
        else -> BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f))
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .tvFocusableWithPhysics(
                shape = shape,
                focusedScale = 1.05f,
                glowColor = if (isPrimary) accent else Color.White,
                onFocusChange = { focused = it }
            ),
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                label,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
    }
}
