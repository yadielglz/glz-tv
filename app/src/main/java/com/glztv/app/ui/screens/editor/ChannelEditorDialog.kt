package com.glztv.app.ui.screens.editor

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.Channel
import com.glztv.app.data.ChannelCustomizationManager
import com.glztv.app.ui.components.ChannelLogo
import com.glztv.app.ui.components.GlzCardDefaults
import com.glztv.app.ui.components.tvFocusableWithPhysics
import com.glztv.app.ui.screens.settings.TvSettingsButton
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ChannelEditorDialog(
    rawChannels: List<Channel>,
    customizationManager: ChannelCustomizationManager,
    onDismiss: () -> Unit,
    onChannelsUpdated: (List<Channel>) -> Unit
) {
    var revision by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var focusedChannelId by remember { mutableStateOf<String?>(null) }
    var showRenumberConfirm by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val primaryAccent = MaterialTheme.colorScheme.primary

    // Current customized channels list
    val currentDisplayChannels = remember(rawChannels, revision) {
        customizationManager.apply(rawChannels, includeHidden = true)
    }

    val categories = remember(rawChannels) {
        listOf("ALL") + rawChannels.map { it.group.trim() }.filter { it.isNotBlank() }.distinct()
    }

    val filteredChannels = remember(currentDisplayChannels, selectedCategory, searchQuery) {
        currentDisplayChannels.filter { channel ->
            val matchesCategory = selectedCategory == "ALL" || channel.group.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                channel.name.contains(searchQuery, ignoreCase = true) ||
                channel.number.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val selectedChannel = remember(filteredChannels, focusedChannelId) {
        filteredChannels.firstOrNull { it.id == focusedChannelId }
            ?: filteredChannels.firstOrNull()
    }

    LaunchedEffect(focusedChannelId) {
        if (focusedChannelId == null && filteredChannels.isNotEmpty()) {
            focusedChannelId = filteredChannels.first().id
        }
    }

    BackHandler(onBack = {
        onChannelsUpdated(customizationManager.apply(rawChannels, includeHidden = false))
        onDismiss()
    })

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0F1829), Color(0xFF070B14), Color.Black)
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.50f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = primaryAccent.copy(alpha = 0.20f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "CHANNEL MANAGER & PLAYLIST EDITOR",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = primaryAccent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        "${filteredChannels.size} CHANNELS",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = primaryAccent
                                    )
                                }
                            }
                            Text(
                                "Edit channel numbers, names, reorder & hide channels in real-time",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvSettingsButton(
                            label = "1..N Renumber",
                            onClick = { showRenumberConfirm = true }
                        )
                        TvSettingsButton(
                            label = "Reset All",
                            onClick = { showResetAllConfirm = true }
                        )
                        TvSettingsButton(
                            label = "Done",
                            onClick = {
                                onChannelsUpdated(customizationManager.apply(rawChannels, includeHidden = false))
                                onDismiss()
                            }
                        )
                    }
                }
            }

            // Category Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    var isFocused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { selectedCategory = category },
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isFocused -> primaryAccent
                            isSelected -> primaryAccent.copy(alpha = 0.25f)
                            else -> Color.White.copy(alpha = 0.05f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isFocused) Color.White else if (isSelected) primaryAccent else Color.White.copy(alpha = 0.10f)
                        ),
                        modifier = Modifier.tvFocusableWithPhysics(
                            shape = RoundedCornerShape(12.dp),
                            focusedScale = 1.05f,
                            glowColor = primaryAccent,
                            onFocusChange = { isFocused = it }
                        )
                    ) {
                        Text(
                            text = category.uppercase(Locale.getDefault()),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected || isFocused) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isFocused) Color.Black else if (isSelected) primaryAccent else Color.White.copy(alpha = 0.80f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Main Editor Split Pane
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Channel List (60% width)
                Surface(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x66111A2E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredChannels, key = { _, ch -> ch.id }) { index, channel ->
                            val override = customizationManager.getOverride(channel.id)
                            val isSelected = channel.id == selectedChannel?.id
                            var isFocused by remember { mutableStateOf(false) }

                            Surface(
                                onClick = { focusedChannelId = channel.id },
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    isFocused -> primaryAccent.copy(alpha = 0.20f)
                                    isSelected -> Color(0xFF1B273E)
                                    else -> Color.White.copy(alpha = 0.03f)
                                },
                                border = BorderStroke(
                                    if (isFocused) 1.5.dp else 1.dp,
                                    if (isFocused) primaryAccent else if (isSelected) primaryAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusableWithPhysics(
                                        shape = RoundedCornerShape(14.dp),
                                        focusedScale = 1.02f,
                                        glowColor = primaryAccent,
                                        onFocusChange = {
                                            isFocused = it
                                            if (it) focusedChannelId = channel.id
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Channel Number Badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (override.customNumber != null) primaryAccent else Color.White.copy(alpha = 0.12f),
                                        modifier = Modifier.width(56.dp)
                                    ) {
                                        Text(
                                            text = channel.number.ifBlank { (index + 1).toString() },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (override.customNumber != null) Color.Black else Color.White,
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    // Logo
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.3f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        ChannelLogo(
                                            logoUrl = channel.logoUrl,
                                            channelName = channel.name,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    // Name & Category
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = channel.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (override.isHidden) Color.White.copy(alpha = 0.40f) else Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (override.customName != null || override.customNumber != null) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = primaryAccent.copy(alpha = 0.20f)
                                                ) {
                                                    Text(
                                                        "EDITED",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = primaryAccent,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (channel.group.isNotBlank()) {
                                            Text(
                                                text = channel.group,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // Hidden indicator
                                    if (override.isHidden) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEF5350).copy(alpha = 0.20f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFFEF5350),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    "HIDDEN",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEF5350)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Column: Channel Inspector & Quick Action Controls
                Surface(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x66111A2E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    if (selectedChannel != null) {
                        val currentChannel = selectedChannel
                        val override = customizationManager.getOverride(currentChannel.id)
                        var editNumberText by remember(currentChannel.id, revision) {
                            mutableStateOf(currentChannel.number)
                        }
                        var editNameText by remember(currentChannel.id, revision) {
                            mutableStateOf(currentChannel.name)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Inspector Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.40f),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    ChannelLogo(
                                        logoUrl = currentChannel.logoUrl,
                                        channelName = currentChannel.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentChannel.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ID: ${currentChannel.id} · Group: ${currentChannel.group.ifBlank { "General" }}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Channel Number Stepper & Direct Input
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "CHANNEL NUMBER",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = primaryAccent,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            "Auto-reorders instantly",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.40f)
                                        )
                                    }

                                    // Stepper Bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StepperButton(label = "-10", onClick = {
                                            val currentVal = ChannelCustomizationManager.channelNumberValue(editNumberText)
                                            val newVal = if (currentVal != Double.MAX_VALUE) (currentVal - 10).coerceAtLeast(1.0) else 1.0
                                            val formatted = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else "%.1f".format(newVal)
                                            editNumberText = formatted
                                            customizationManager.setChannelNumber(currentChannel.id, formatted)
                                            revision++
                                        })
                                        StepperButton(label = "-1", onClick = {
                                            val currentVal = ChannelCustomizationManager.channelNumberValue(editNumberText)
                                            val newVal = if (currentVal != Double.MAX_VALUE) (currentVal - 1).coerceAtLeast(1.0) else 1.0
                                            val formatted = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else "%.1f".format(newVal)
                                            editNumberText = formatted
                                            customizationManager.setChannelNumber(currentChannel.id, formatted)
                                            revision++
                                        })

                                        // Editable Text Box
                                        OutlinedTextField(
                                            value = editNumberText,
                                            onValueChange = { input ->
                                                editNumberText = input
                                                customizationManager.setChannelNumber(currentChannel.id, input)
                                                revision++
                                            },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = Color(0xFF10192A),
                                                unfocusedContainerColor = Color(0xFF0C121E),
                                                focusedBorderColor = primaryAccent,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.20f)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )

                                        StepperButton(label = "+1", onClick = {
                                            val currentVal = ChannelCustomizationManager.channelNumberValue(editNumberText)
                                            val newVal = if (currentVal != Double.MAX_VALUE) (currentVal + 1) else 1.0
                                            val formatted = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else "%.1f".format(newVal)
                                            editNumberText = formatted
                                            customizationManager.setChannelNumber(currentChannel.id, formatted)
                                            revision++
                                        })
                                        StepperButton(label = "+10", onClick = {
                                            val currentVal = ChannelCustomizationManager.channelNumberValue(editNumberText)
                                            val newVal = if (currentVal != Double.MAX_VALUE) (currentVal + 10) else 10.0
                                            val formatted = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else "%.1f".format(newVal)
                                            editNumberText = formatted
                                            customizationManager.setChannelNumber(currentChannel.id, formatted)
                                            revision++
                                        })
                                    }
                                }
                            }

                            // Channel Name Editor
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "CUSTOM CHANNEL NAME",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = primaryAccent,
                                        letterSpacing = 0.8.sp
                                    )
                                    OutlinedTextField(
                                        value = editNameText,
                                        onValueChange = { input ->
                                            editNameText = input
                                            customizationManager.setChannelName(currentChannel.id, input)
                                            revision++
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF10192A),
                                            unfocusedContainerColor = Color(0xFF0C121E),
                                            focusedBorderColor = primaryAccent,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.20f)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Action Buttons (Hide/Show, Move Up/Down, Reset)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Toggle Visibility Button
                                ActionRowButton(
                                    label = if (override.isHidden) "Show Channel (Currently Hidden)" else "Hide Channel from Guide & Live TV",
                                    icon = if (override.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    isWarning = !override.isHidden,
                                    onClick = {
                                        customizationManager.toggleHidden(currentChannel.id)
                                        revision++
                                    }
                                )

                                // Move Swap Up / Down Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val currentIndex = filteredChannels.indexOfFirst { it.id == currentChannel.id }
                                    ActionRowButton(
                                        label = "Move Up",
                                        icon = Icons.Default.ArrowUpward,
                                        enabled = currentIndex > 0,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (currentIndex > 0) {
                                                val prevChannel = filteredChannels[currentIndex - 1]
                                                val prevNumber = prevChannel.number
                                                val curNumber = currentChannel.number
                                                customizationManager.setChannelNumber(currentChannel.id, prevNumber)
                                                customizationManager.setChannelNumber(prevChannel.id, curNumber)
                                                revision++
                                            }
                                        }
                                    )
                                    ActionRowButton(
                                        label = "Move Down",
                                        icon = Icons.Default.ArrowDownward,
                                        enabled = currentIndex in 0 until filteredChannels.size - 1,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (currentIndex in 0 until filteredChannels.size - 1) {
                                                val nextChannel = filteredChannels[currentIndex + 1]
                                                val nextNumber = nextChannel.number
                                                val curNumber = currentChannel.number
                                                customizationManager.setChannelNumber(currentChannel.id, nextNumber)
                                                customizationManager.setChannelNumber(nextChannel.id, curNumber)
                                                revision++
                                            }
                                        }
                                    )
                                }

                                // Reset Channel Button
                                if (override.customNumber != null || override.customName != null || override.isHidden) {
                                    ActionRowButton(
                                        label = "Reset This Channel to Playlist Default",
                                        icon = Icons.Default.RestartAlt,
                                        onClick = {
                                            customizationManager.resetChannel(currentChannel.id)
                                            revision++
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Select a channel on the left to edit",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Renumber Confirmation Dialog
        if (showRenumberConfirm) {
            ConfirmationOverlay(
                title = "Renumber All Channels Sequentially?",
                description = "This will assign channel numbers starting from 1, 2, 3... based on the current order. Custom overrides will be updated.",
                confirmLabel = "Renumber All",
                onConfirm = {
                    customizationManager.renumberSequentially(filteredChannels, startFrom = 1)
                    revision++
                    showRenumberConfirm = false
                },
                onDismiss = { showRenumberConfirm = false }
            )
        }

        // Reset All Confirmation Dialog
        if (showResetAllConfirm) {
            ConfirmationOverlay(
                title = "Reset All Customizations?",
                description = "This will remove all custom channel numbers, names, and unhide any hidden channels, reverting back to the original M3U playlist.",
                confirmLabel = "Reset Everything",
                isDestructive = true,
                onConfirm = {
                    customizationManager.resetAll()
                    revision++
                    showResetAllConfirm = false
                },
                onDismiss = { showResetAllConfirm = false }
            )
        }
    }
}

@Composable
private fun StepperButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isFocused) accent else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (isFocused) Color.White else Color.White.copy(alpha = 0.15f)),
        modifier = modifier.tvFocusableWithPhysics(
            shape = RoundedCornerShape(8.dp),
            focusedScale = 1.08f,
            glowColor = accent,
            onFocusChange = { isFocused = it }
        )
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun ActionRowButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    val warningColor = Color(0xFFEF5350)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = when {
            !enabled -> Color.White.copy(alpha = 0.02f)
            isFocused && isWarning -> warningColor
            isFocused -> accent
            else -> Color.White.copy(alpha = 0.05f)
        },
        border = BorderStroke(
            1.dp,
            when {
                !enabled -> Color.White.copy(alpha = 0.04f)
                isFocused -> Color.White
                isWarning -> warningColor.copy(alpha = 0.3f)
                else -> Color.White.copy(alpha = 0.10f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .tvFocusableWithPhysics(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.03f,
                glowColor = if (isWarning) warningColor else accent,
                onFocusChange = { isFocused = it }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = when {
                    !enabled -> Color.White.copy(alpha = 0.2f)
                    isFocused -> Color.Black
                    isWarning -> warningColor
                    else -> accent
                },
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    !enabled -> Color.White.copy(alpha = 0.2f)
                    isFocused -> Color.Black
                    else -> Color.White
                }
            )
        }
    }
}

@Composable
private fun ConfirmationOverlay(
    title: String,
    description: String,
    confirmLabel: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xF2121A2C),
            border = BorderStroke(1.5.dp, if (isDestructive) Color(0xFFEF5350) else MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .width(480.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvSettingsButton(
                        label = "Cancel",
                        onClick = onDismiss
                    )
                    Spacer(Modifier.width(12.dp))
                    TvSettingsButton(
                        label = confirmLabel,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}
