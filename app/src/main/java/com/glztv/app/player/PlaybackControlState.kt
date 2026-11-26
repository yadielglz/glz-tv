package com.glztv.app.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class TrackOption(
    val id: String,
    val label: String,
    val language: String?,
    val selected: Boolean
)

class PlaybackControlState {
    var audioTracks by mutableStateOf<List<TrackOption>>(emptyList())
        internal set
    var subtitleTracks by mutableStateOf<List<TrackOption>>(emptyList())
        internal set
    var diagnostics by mutableStateOf<PlaybackDiagnostics?>(null)
        internal set
    var isPlaying by mutableStateOf(true)
        internal set
    var isMuted by mutableStateOf(false)
        internal set
    var currentPositionMs by mutableStateOf(0L)
        internal set
    var durationMs by mutableStateOf(0L)
        internal set

    internal var selectAudio: (String) -> Unit = {}
    internal var selectSubtitle: (String?) -> Unit = {}
    internal var togglePlayPauseAction: () -> Unit = {}
    internal var toggleMuteAction: () -> Unit = {}
    internal var seekToAction: (Long) -> Unit = {}
    internal var seekByAction: (Long) -> Unit = {}

    fun chooseAudio(id: String) = selectAudio(id)
    fun chooseSubtitle(id: String?) = selectSubtitle(id)
    fun togglePlayPause() = togglePlayPauseAction()
    fun toggleMute() = toggleMuteAction()
    fun seekTo(positionMs: Long) = seekToAction(positionMs)
    fun seekBy(deltaMs: Long) = seekByAction(deltaMs)
}
