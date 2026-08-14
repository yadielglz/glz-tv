package com.glztv.app.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackDiagnosticsPanel(
    diagnostics: PlaybackDiagnostics?,
    modifier: Modifier = Modifier
) {
    Surface(modifier.width(330.dp), color = Color(0xF20B1114), contentColor = Color.White,
        shape = RoundedCornerShape(20.dp), tonalElevation = 20.dp) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("STREAM INFORMATION", color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black)
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
                        Text(value ?: "Unavailable", maxLines = 1,
                            overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
