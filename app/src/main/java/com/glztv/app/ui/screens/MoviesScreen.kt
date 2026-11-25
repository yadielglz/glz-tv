package com.glztv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.glztv.app.Movie
import com.glztv.app.ui.theme.DtvActionButton
import com.glztv.app.ui.theme.DtvChrome
import com.glztv.app.ui.theme.DtvSectionHeader
import com.glztv.app.ui.theme.DtvSegmentedStripBar
import com.glztv.app.ui.theme.LocalTvViewport
import kotlinx.coroutines.delay

/**
 * DirecTV On Demand Movies Screen (v1.48.4.10).
 *
 * Features:
 * - Shared DirecTV HD Top Command Bar (`Movies` tab active).
 * - DirecTV Segmented Genre Filter Strip.
 * - Split-top On Demand Showcase (left: metadata, synopsis & Play Movie; right: framed 16:9 backdrop/poster).
 * - Crisp 2:3 Poster Shelf below launching the existing ImmersiveMoviePlayer + OpenSubtitles CC.
 */
@Composable
fun MoviesScreen(
    movies: List<Movie>,
    onWatchMovie: (Movie) -> Unit,
    topBar: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val viewport = LocalTvViewport.current
    val horizontalPad = viewport.safeHorizontal.coerceAtLeast(28.dp)

    var selectedGenre by remember { mutableStateOf("ALL") }
    val genres = remember(movies) {
        val unique = movies.flatMap { it.genres }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        buildList {
            add("ALL")
            addAll(unique)
        }
    }
    val filteredMovies = remember(movies, selectedGenre) {
        if (selectedGenre == "ALL") movies
        else movies.filter { m -> m.genres.any { it.equals(selectedGenre, ignoreCase = true) } }
    }

    var focusedMovie by remember(filteredMovies) { mutableStateOf(filteredMovies.firstOrNull()) }
    val displayMovie = focusedMovie?.takeIf { it in filteredMovies } ?: filteredMovies.firstOrNull()
    val firstPosterFocus = remember { FocusRequester() }

    LaunchedEffect(filteredMovies.isNotEmpty()) {
        if (filteredMovies.isNotEmpty()) {
            delay(100)
            runCatching { firstPosterFocus.requestFocus() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DtvChrome.ScreenBg)
    ) {
        topBar?.invoke()

        if (genres.size > 1) {
            DtvSegmentedStripBar(
                items = genres,
                selectedItem = selectedGenre,
                onSelect = { selectedGenre = it }
            )
        }

        if (displayMovie == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "No Movies Available",
                        color = DtvChrome.PrimaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Add movies in GlzHub to browse On Demand titles here.",
                        color = DtvChrome.SecondaryText,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // Split-Top DirecTV On Demand Showcase
            val showcaseHeight = if (viewport.isCompactHeight) 174.dp else 206.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(showcaseHeight)
                    .background(DtvChrome.BannerGradient)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPad, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ON DEMAND",
                                color = DtvChrome.ElectricBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            )
                            Text(
                                text = displayMovie.title,
                                color = Color.White,
                                fontSize = if (viewport.isCompactHeight) 22.sp else 26.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val metaLine = buildList {
                                displayMovie.releaseYear?.let { add(it.toString()) }
                                displayMovie.runtimeMinutes?.takeIf { it > 0 }?.let { add("${it}m") }
                                if (displayMovie.genres.isNotEmpty()) add(displayMovie.genres.take(3).joinToString(" / "))
                                if (displayMovie.subtitles.isNotEmpty()) add("CC")
                            }.joinToString("  ·  ")
                            if (metaLine.isNotBlank()) {
                                Text(
                                    text = metaLine,
                                    color = DtvChrome.SecondaryText,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (displayMovie.description.isNotBlank()) {
                                Text(
                                    text = displayMovie.description,
                                    color = DtvChrome.PrimaryText.copy(alpha = 0.82f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DtvActionButton(
                                label = "Play Movie",
                                primary = true,
                                showPlayIcon = true,
                                onClick = { onWatchMovie(displayMovie) }
                            )
                        }
                    }

                    // Right: Framed 16:9 Backdrop / Poster Artwork
                    val artUrl = displayMovie.backdropUrl.ifBlank { displayMovie.posterUrl }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(3.dp))
                            .clipToBounds()
                            .background(Color.Black)
                            .border(1.dp, DtvChrome.Divider, RoundedCornerShape(3.dp))
                    ) {
                        if (artUrl.isNotBlank()) {
                            AsyncImage(
                                model = artUrl,
                                contentDescription = displayMovie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .background(DtvChrome.Divider)
                )
            }

            // Poster Shelf Below
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DtvSectionHeader(
                    title = if (selectedGenre == "ALL") "ALL MOVIES" else selectedGenre.uppercase(),
                    trailing = "${filteredMovies.size} TITLES",
                    horizontalPad = horizontalPad
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = horizontalPad),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(filteredMovies, key = { _, m -> "od-${m.id}" }) { index, movie ->
                        DtvMoviePosterCard(
                            movie = movie,
                            modifier = if (index == 0) Modifier.focusRequester(firstPosterFocus) else Modifier,
                            onFocused = { focusedMovie = movie },
                            onClick = { onWatchMovie(movie) }
                        )
                    }
                }
            }
        }
    }
}
