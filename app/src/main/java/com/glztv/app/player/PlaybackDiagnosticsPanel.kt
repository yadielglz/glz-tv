package com.glztv.app.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.ui.components.tvFocusableWithPhysics

@Composable
fun PlaybackDiagnosticsPanel(
    diagnostics: PlaybackDiagnostics?,
    speedTestResult: SpeedTestResult? = null,
    onRunSpeedTest: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(340.dp),
        color = Color(0xF20B1114),
        contentColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 20.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "STREAM INFORMATION",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black
            )
            if (diagnostics == null) {
                Text("Waiting for stream data…", color = Color.White.copy(alpha = .65f))
            } else {
                listOf(
                    "Channel" to diagnostics.channelName,
                    "Protocol" to diagnostics.protocol,
                    "Resolution" to diagnostics.resolution,
                    "Video codec" to diagnostics.videoCodec,
                    "Audio codec" to diagnostics.audioCodec,
                    "Bitrate" to diagnostics.bitrate?.let { "${it / 1_000} kbps" },
                    "Buffer" to "${diagnostics.bufferDurationMs} ms",
                    "Dropped frames" to diagnostics.droppedFrames.toString(),
                    "Network" to diagnostics.networkTransport,
                    "State" to diagnostics.playbackState,
                    "Last error" to diagnostics.lastError?.name
                ).forEach { (name, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, color = Color.White.copy(alpha = .6f))
                        Text(
                            value ?: "Unavailable",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (speedTestResult != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Throughput", color = Color.White.copy(alpha = .6f))
                        Text(
                            "${speedTestResult.speedMbps} Mbps (${speedTestResult.pingMs}ms)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (onRunSpeedTest != null) {
                    Spacer(Modifier.height(8.dp))
                    var isFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = onRunSpeedTest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusableWithPhysics(
                                shape = RoundedCornerShape(12.dp),
                                focusedScale = 1.04f,
                                glowColor = MaterialTheme.colorScheme.secondary,
                                onFocusChange = { isFocused = it }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFocused) Color.White else MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                        contentColor = if (isFocused) Color.Black else Color.White
                    ) {
                        Text(
                            "⚡ Run Speed Test",
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

