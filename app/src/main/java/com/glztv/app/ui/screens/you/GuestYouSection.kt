package com.glztv.app.ui.screens.you

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.GuestExperience
import com.glztv.app.GuestService
import com.glztv.app.ui.screens.home.GuestServiceCard
import com.glztv.app.ui.screens.home.HubSectionTitle
import com.glztv.app.ui.screens.home.StaySummaryCard
import com.glztv.app.ui.screens.home.WifiInformationCard

@Composable
fun GuestYouSection(
    guestName: String,
    experience: GuestExperience,
    modifier: Modifier = Modifier
) {
    val services = buildList {
        experience.noticeTitle?.let {
            add(GuestService(it, experience.noticeBody, null))
        }
        experience.frontDesk?.let {
            add(GuestService("Front Desk", it, null))
        }
        addAll(experience.services)
    }
    val listState = rememberLazyListState()

    BoxWithConstraints(
        modifier
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        val compact = maxWidth < 700.dp

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header Row
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "You",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your stay at a glance, ${guestName.ifBlank { "Guest" }}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Stay Summary & Wi-Fi Access Cards
            item {
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        StaySummaryCard(guestName, experience, Modifier.fillMaxWidth())
                        WifiInformationCard(experience, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StaySummaryCard(guestName, experience, Modifier.weight(1f))
                        WifiInformationCard(experience, Modifier.weight(1f))
                    }
                }
            }

            // Services & Visit Information
            if (services.isNotEmpty()) {
                item {
                    HubSectionTitle("VISIT INFORMATION", "Helpful details & services for your stay")
                }
                items(services, key = { "${it.title}-${it.actionUrl}" }) { service ->
                    GuestServiceCard(service)
                }
            }
        }
    }
}
