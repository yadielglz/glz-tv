package com.glztv.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@Composable
fun ExpressiveNavigationRail(
    section: AppSection,
    onSection: (AppSection) -> Unit
) {
    Surface(
        modifier = Modifier.width(116.dp).fillMaxHeight(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .92f),
        tonalElevation = 6.dp
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RailDestination("Home", section == AppSection.Home, Icons.Default.Home) {
                onSection(AppSection.Home)
            }
            RailDestination("Live TV", section == AppSection.Live, Icons.Default.LiveTv) {
                onSection(AppSection.Live)
            }
            RailDestination("Radio", section == AppSection.Radio, Icons.Default.Radio) {
                onSection(AppSection.Radio)
            }
            RailDestination("You", section == AppSection.You, Icons.Default.Person) {
                onSection(AppSection.You)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "v${BuildConfig.VERSION_NAME}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun RailDestination(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            if (focused) 3.dp else 0.dp,
            if (focused) MaterialTheme.colorScheme.secondary else Color.Transparent
        ),
        color = when {
            focused -> MaterialTheme.colorScheme.primary
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> Color.Transparent
        },
        contentColor = when {
            focused -> MaterialTheme.colorScheme.onPrimary
            selected -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, label, Modifier.size(25.dp))
            Text(label, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
        }
    }
}
