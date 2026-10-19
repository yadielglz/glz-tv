package com.glztv.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glztv.app.Movie
import com.glztv.app.SubtitleTrack
import com.glztv.app.data.OpenSubtitlesRepository
import com.glztv.app.data.OpenSubtitlesSearchResult
import com.glztv.app.ui.theme.GoogleTvTokens
import com.glztv.app.ui.theme.googleTvFocusable
import kotlinx.coroutines.launch

@Composable
fun MovieSubtitleDialog(
    movie: Movie,
    captionsEnabled: Boolean,
    captionLanguage: String,
    activeSubtitles: List<SubtitleTrack>,
    openSubtitlesRepository: OpenSubtitlesRepository,
    onToggleCaptions: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onApplySubtitleTrack: (SubtitleTrack) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialFocus = remember { FocusRequester() }

    var selectedSearchLang by remember { mutableStateOf(captionLanguage.ifBlank { "es" }) }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<OpenSubtitlesSearchResult>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var downloadingFileId by remember { mutableStateOf<Int?>(null) }

    fun doSearch(lang: String) {
        scope.launch {
            isSearching = true
            statusMessage = "Searching OpenSubtitles ($lang)…"
            val results = openSubtitlesRepository.search(movie.title, movie.releaseYear, lang)
            searchResults = results
            isSearching = false
            statusMessage = if (results.isEmpty()) "No subtitles found for ${lang.uppercase()}" else null
        }
    }

    LaunchedEffect(Unit) {
        initialFocus.requestFocus()
        // Auto-query on first open
        doSearch(selectedSearchLang)
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(480.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            color = Color(0xFF101520),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SUBTITLES & CAPTIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GoogleTvTokens.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable(onClick = onDismiss)
                            .googleTvFocusable(shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Section 1: Quick Enable / Disable
                Text(
                    text = "STATUS",
                    color = GoogleTvTokens.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubtitleDialogPill(
                        label = "Subtitles Off",
                        active = !captionsEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(initialFocus),
                        onClick = { onToggleCaptions(false) }
                    )
                    SubtitleDialogPill(
                        label = "Subtitles On",
                        active = captionsEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onToggleCaptions(true) }
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Section 2: Search by Language
                Text(
                    text = "SEARCH OPENSUBTITLES",
                    color = GoogleTvTokens.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubtitleDialogPill(
                        label = "Español (ES)",
                        active = selectedSearchLang.equals("es", ignoreCase = true),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedSearchLang = "es"
                            onSelectLanguage("es")
                            doSearch("es")
                        }
                    )
                    SubtitleDialogPill(
                        label = "English (EN)",
                        active = selectedSearchLang.equals("en", ignoreCase = true),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedSearchLang = "en"
                            onSelectLanguage("en")
                            doSearch("en")
                        }
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { doSearch(selectedSearchLang) }
                            .googleTvFocusable(shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GoogleTvTokens.BlueAccent, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (statusMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = statusMessage!!,
                        color = GoogleTvTokens.BlueAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Section 3: Results & Active Subtitles
                Text(
                    text = "AVAILABLE SUBTITLES (${searchResults.size})",
                    color = GoogleTvTokens.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (searchResults.isEmpty() && !isSearching) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No subtitles found.\nTry switching between Spanish and English.",
                                    color = GoogleTvTokens.TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    items(searchResults, key = { it.fileId }) { result ->
                        val isDownloading = downloadingFileId == result.fileId
                        val isAttached = activeSubtitles.any { it.id.contains("${result.fileId}") }

                        SubtitleResultCard(
                            result = result,
                            isAttached = isAttached && captionsEnabled,
                            isDownloading = isDownloading,
                            onClick = {
                                scope.launch {
                                    downloadingFileId = result.fileId
                                    statusMessage = "Downloading ${result.releaseName}…"
                                    val downloaded = openSubtitlesRepository.downloadSubtitle(context, movie.id, result)
                                    downloadingFileId = null
                                    if (downloaded != null) {
                                        val track = SubtitleTrack(
                                            id = "os_${result.fileId}",
                                            language = result.language.lowercase(),
                                            label = result.label,
                                            uri = android.net.Uri.fromFile(downloaded).toString()
                                        )
                                        onApplySubtitleTrack(track)
                                        statusMessage = "✓ Subtitle applied!"
                                    } else {
                                        statusMessage = "Failed to download subtitle"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleDialogPill(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) GoogleTvTokens.BlueAccent else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .googleTvFocusable(shape = RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SubtitleResultCard(
    result: OpenSubtitlesSearchResult,
    isAttached: Boolean,
    isDownloading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isAttached) GoogleTvTokens.BlueAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isAttached) 1.5.dp else 1.dp,
                color = if (isAttached) GoogleTvTokens.BlueAccent else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .googleTvFocusable(shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isAttached) GoogleTvTokens.BlueAccent else Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else if (isAttached) {
                    Icon(Icons.Default.Check, contentDescription = "Applied", tint = Color.Black, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = result.language.uppercase(),
                        color = GoogleTvTokens.BlueAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (result.hearingImpaired) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "[CC]",
                            color = GoogleTvTokens.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (result.downloadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "· ⬇ ${result.downloadCount}",
                            color = GoogleTvTokens.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = result.releaseName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
