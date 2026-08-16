package com.glztv.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.BuildConfig
import com.glztv.app.ui.components.tvFocusableWithPhysics

@Composable
fun ExpressiveNavigationRail(
    section: AppSection,
    onSection: (AppSection) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(112.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 8.dp
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compactHeight = maxHeight < 600.dp
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 8.dp,
                        vertical = if (compactHeight) 12.dp else 20.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(if (compactHeight) 8.dp else 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RailDestination("Home", section == AppSection.Home, Icons.Default.Home, compactHeight) {
                    onSection(AppSection.Home)
                }
                RailDestination("Live TV", section == AppSection.Live, Icons.Default.LiveTv, compactHeight) {
                    onSection(AppSection.Live)
                }
                RailDestination("Radio", section == AppSection.Radio, Icons.Default.Radio, compactHeight) {
                    onSection(AppSection.Radio)
                }
                RailDestination("Weather", section == AppSection.Weather, Icons.Default.WbSunny, compactHeight) {
                    onSection(AppSection.Weather)
                }
                RailDestination("You", section == AppSection.You, Icons.Default.Person, compactHeight) {
                    onSection(AppSection.You)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RailDestination(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    compactHeight: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val activeColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .tvFocusableWithPhysics(
                shape = RoundedCornerShape(24.dp),
                focusedScale = 1.08f,
                glowColor = activeColor,
                onFocusChange = { focused = it }
            ),
        shape = RoundedCornerShape(24.dp),
        color = when {
            focused -> activeColor
            selected -> activeColor.copy(alpha = 0.20f)
            else -> Color.Transparent
        },
        border = if (selected && !focused) BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)) else null,
        contentColor = when {
            focused -> MaterialTheme.colorScheme.onPrimary
            selected -> activeColor
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 6.dp,
                    vertical = if (compactHeight) 10.dp else 14.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                label,
                Modifier.size(if (compactHeight) 22.dp else 26.dp)
            )
            Text(
                label,
                fontWeight = if (selected || focused) FontWeight.ExtraBold else FontWeight.Medium,
                fontSize = if (compactHeight) 11.sp else 12.sp,
                maxLines = 1
            )
        }
    }
}
