package com.glztv.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.model.NetworkInfo
import com.glztv.app.model.WeatherInfo
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun SlimHeader(
    loading: Boolean,
    contentLoaded: Boolean,
    weather: WeatherInfo?,
    networkInfo: NetworkInfo?,
    onWeatherClick: (() -> Unit)?,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    val compactHeader = LocalConfiguration.current.screenWidthDp < 700
    val headerHorizontalPadding = if (compactHeader) 12.dp else 40.dp
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = headerHorizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LiveTv,
                "Glz TV",
                Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(10.dp))
            Text("Glz TV", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Black)
            if (contentLoaded) {
                if (!compactHeader) networkInfo?.let {
                    Column(
                        Modifier.width(190.dp).padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            it.connection, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium, maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            it.isp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!compactHeader) weather?.let {
                    Column(
                        Modifier
                            .clickable(
                                enabled = onWeatherClick != null,
                                onClick = { onWeatherClick?.invoke() }
                            )
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "${weatherSymbol(it.weatherCode)}  ${it.temperature}°",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            it.location,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Column(
                    Modifier.padding(horizontal = 14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now)),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (!compactHeader) {
                        Text(
                            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(now)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            IconButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
            FilledIconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Settings")
            }
        }
    }
}

private fun weatherSymbol(code: Int): String = when (code) {
    0 -> "☀"
    1, 2 -> "⛅"
    3 -> "☁"
    45, 48 -> "≋"
    in 51..67, in 80..82 -> "☂"
    in 71..77, 85, 86 -> "❄"
    in 95..99 -> "ϟ"
    else -> "°"
}
