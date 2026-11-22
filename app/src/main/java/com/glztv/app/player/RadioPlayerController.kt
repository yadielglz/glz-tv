@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.glztv.app.player

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.glztv.app.BuildConfig
import com.glztv.app.GlzHubManager
import com.glztv.app.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RadioPlaybackStatus {
    Idle,
    Buffering,
    Playing,
    Paused,
    Reconnecting,
    Error
}

@Immutable
data class RadioPlaybackState(
    val station: RadioStation? = null,
    val status: RadioPlaybackStatus = RadioPlaybackStatus.Idle,
    val icyTitle: String? = null,
    val icyArtist: String? = null,
    val retryAttempt: Int = 0,
    val errorMessage: String? = null,
    val sleepTimerRemainingSec: Int? = null,
) {
    val isPlaying: Boolean get() = status == RadioPlaybackStatus.Playing
    val isBuffering: Boolean get() = status == RadioPlaybackStatus.Buffering || status == RadioPlaybackStatus.Reconnecting
    val isActive: Boolean get() = station != null && status != RadioPlaybackStatus.Idle
}

/**
 * Single shared Radio playback controller used by Home, HomeRadioPlayerBar,
 * RadioScreen, and AmbientScreensaverScreen so audio persists seamlessly
 * across sections without dual ExoPlayer instances or header leakage.
 */
class RadioPlayerController(
    context: Context,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val PREF_FAVORITE_RADIO = "favorite_radio_stations"
        val SLEEP_TIMER_OPTIONS_MIN = listOf(0, 15, 30, 60)

        fun stationKey(station: RadioStation): String =
            station.code.ifBlank { station.streamUrl }
    }

    var state by mutableStateOf(RadioPlaybackState())
        private set

    var favoriteStationKeys by mutableStateOf(
        prefs.getStringSet(PREF_FAVORITE_RADIO, emptySet()).orEmpty().toSet()
    )
        private set

    private var retryJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("GLZ-TV-Radio/${BuildConfig.VERSION_NAME}")
        .setAllowCrossProtocolRedirects(true)

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()
        .apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val current = state.station ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    val nextStatus = if (state.retryAttempt > 0) {
                        RadioPlaybackStatus.Reconnecting
                    } else {
                        RadioPlaybackStatus.Buffering
                    }
                    state = state.copy(status = nextStatus, errorMessage = null)
                }
                Player.STATE_READY -> {
                    val nextStatus = if (player.isPlaying) {
                        RadioPlaybackStatus.Playing
                    } else {
                        RadioPlaybackStatus.Paused
                    }
                    state = state.copy(status = nextStatus, retryAttempt = 0, errorMessage = null)
                }
                Player.STATE_ENDED -> {
                    // Live streams occasionally signal ENDED on drop — attempt reconnect.
                    scheduleReconnect(current, "Stream ended unexpectedly")
                }
                Player.STATE_IDLE -> {
                    if (state.status != RadioPlaybackStatus.Error && state.status != RadioPlaybackStatus.Reconnecting) {
                        state = state.copy(status = RadioPlaybackStatus.Idle)
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val current = state.station ?: return
            if (isPlaying) {
                retryJob?.cancel()
                state = state.copy(
                    status = RadioPlaybackStatus.Playing,
                    retryAttempt = 0,
                    errorMessage = null
                )
            } else if (player.playbackState == Player.STATE_READY) {
                state = state.copy(status = RadioPlaybackStatus.Paused)
            } else if (player.playbackState == Player.STATE_BUFFERING) {
                state = state.copy(
                    status = if (state.retryAttempt > 0) RadioPlaybackStatus.Reconnecting else RadioPlaybackStatus.Buffering
                )
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val current = state.station ?: return
            val rawTitle = mediaMetadata.title?.toString()?.trim()?.takeIf { it.isNotBlank() }
            val rawArtist = mediaMetadata.artist?.toString()?.trim()?.takeIf { it.isNotBlank() }
            // Ignore metadata that merely echoes the static station name/genre we set on prepare
            val cleanedTitle = rawTitle?.takeUnless { it.equals(current.name, ignoreCase = true) }
            val cleanedArtist = rawArtist?.takeUnless { it.equals(current.genre, ignoreCase = true) }
            if (cleanedTitle != null || cleanedArtist != null) {
                state = state.copy(icyTitle = cleanedTitle, icyArtist = cleanedArtist)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val current = state.station ?: return
            scheduleReconnect(current, error.localizedMessage ?: error.errorCodeName)
        }
    }

    init {
        player.addListener(listener)
    }

    fun playStation(station: RadioStation) {
        retryJob?.cancel()
        val sameStation = state.station?.streamUrl == station.streamUrl && state.station?.code == station.code
        if (sameStation && state.status == RadioPlaybackStatus.Playing) {
            player.pause()
            state = state.copy(status = RadioPlaybackStatus.Paused)
            return
        }
        if (sameStation && state.status == RadioPlaybackStatus.Paused && player.playbackState == Player.STATE_READY) {
            player.play()
            state = state.copy(status = RadioPlaybackStatus.Buffering)
            return
        }
        startStream(station, retryAttempt = 0)
        GlzHubManager.reportActivity(prefs, "radio", station.name)
    }

    fun togglePlayPause() {
        val current = state.station ?: return
        when (state.status) {
            RadioPlaybackStatus.Playing -> {
                player.pause()
                state = state.copy(status = RadioPlaybackStatus.Paused)
            }
            RadioPlaybackStatus.Paused -> {
                if (player.playbackState == Player.STATE_READY) {
                    player.play()
                    state = state.copy(status = RadioPlaybackStatus.Buffering)
                } else {
                    startStream(current, retryAttempt = 0)
                }
            }
            RadioPlaybackStatus.Error, RadioPlaybackStatus.Idle -> {
                startStream(current, retryAttempt = 0)
                GlzHubManager.reportActivity(prefs, "radio", current.name)
            }
            RadioPlaybackStatus.Buffering, RadioPlaybackStatus.Reconnecting -> {
                // Already connecting; allow pausing/canceling
                retryJob?.cancel()
                player.pause()
                state = state.copy(status = RadioPlaybackStatus.Paused)
            }
        }
    }

    fun retry() {
        val current = state.station ?: return
        retryJob?.cancel()
        startStream(current, retryAttempt = 0)
        GlzHubManager.reportActivity(prefs, "radio", current.name)
    }

    fun playNext(stations: List<RadioStation>) {
        if (stations.isEmpty()) return
        val currentIdx = stations.indexOfFirst {
            it.code == state.station?.code && it.streamUrl == state.station?.streamUrl
        }
        val next = stations[(currentIdx + 1).mod(stations.size)]
        playStation(next)
    }

    fun playPrevious(stations: List<RadioStation>) {
        if (stations.isEmpty()) return
        val currentIdx = stations.indexOfFirst {
            it.code == state.station?.code && it.streamUrl == state.station?.streamUrl
        }
        val prev = if (currentIdx <= 0) stations.last() else stations[currentIdx - 1]
        playStation(prev)
    }

    fun isFavorite(station: RadioStation): Boolean =
        stationKey(station) in favoriteStationKeys

    fun toggleFavorite(station: RadioStation) {
        val key = stationKey(station)
        val updated = if (key in favoriteStationKeys) {
            favoriteStationKeys - key
        } else {
            favoriteStationKeys + key
        }
        favoriteStationKeys = updated
        prefs.edit().putStringSet(PREF_FAVORITE_RADIO, updated).apply()
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            state = state.copy(sleepTimerRemainingSec = null)
            return
        }
        val totalSeconds = minutes * 60
        state = state.copy(sleepTimerRemainingSec = totalSeconds)
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1_000L)
                remaining -= 1
                if (state.station == null) {
                    state = state.copy(sleepTimerRemainingSec = null)
                    return@launch
                }
                state = state.copy(sleepTimerRemainingSec = remaining)
            }
            stop()
        }
    }

    fun cycleSleepTimer() {
        val currentSec = state.sleepTimerRemainingSec ?: 0
        val currentMin = (currentSec + 59) / 60
        val nextMin = SLEEP_TIMER_OPTIONS_MIN.firstOrNull { it > currentMin } ?: 0
        setSleepTimer(nextMin)
    }

    fun stop() {
        retryJob?.cancel()
        sleepTimerJob?.cancel()
        player.stop()
        player.clearMediaItems()
        val hadStation = state.station != null
        state = RadioPlaybackState()
        if (hadStation) {
            GlzHubManager.reportActivity(prefs, "idle")
        }
    }

    fun release() {
        retryJob?.cancel()
        sleepTimerJob?.cancel()
        player.removeListener(listener)
        player.stop()
        player.release()
    }

    private fun startStream(station: RadioStation, retryAttempt: Int) {
        // setDefaultRequestProperties replaces (clears + sets) the map in Media3 so headers never leak
        dataSourceFactory.setDefaultRequestProperties(station.requestHeaders)
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.genre)
            .apply { station.logoUrl?.let { setArtworkUri(Uri.parse(it)) } }
            .build()
        state = RadioPlaybackState(
            station = station,
            status = if (retryAttempt > 0) RadioPlaybackStatus.Reconnecting else RadioPlaybackStatus.Buffering,
            icyTitle = if (retryAttempt > 0) state.icyTitle else null,
            icyArtist = if (retryAttempt > 0) state.icyArtist else null,
            retryAttempt = retryAttempt,
            errorMessage = null,
            sleepTimerRemainingSec = state.sleepTimerRemainingSec
        )
        player.stop()
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(station.streamUrl)
                .setMediaMetadata(metadata)
                .build()
        )
        player.prepare()
        player.play()
    }

    private fun scheduleReconnect(station: RadioStation, reason: String) {
        retryJob?.cancel()
        val nextAttempt = state.retryAttempt + 1
        if (nextAttempt > MAX_RETRY_ATTEMPTS) {
            state = state.copy(
                status = RadioPlaybackStatus.Error,
                retryAttempt = MAX_RETRY_ATTEMPTS,
                errorMessage = reason
            )
            return
        }
        val backoffSec = minOf(15L, 1L shl minOf(nextAttempt - 1, 4))
        state = state.copy(
            status = RadioPlaybackStatus.Reconnecting,
            retryAttempt = nextAttempt,
            errorMessage = reason
        )
        retryJob = scope.launch {
            delay(backoffSec * 1_000L)
            if (state.station?.streamUrl == station.streamUrl) {
                startStream(station, retryAttempt = nextAttempt)
            }
        }
    }
}
