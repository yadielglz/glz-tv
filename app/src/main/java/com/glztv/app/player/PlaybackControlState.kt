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

    internal var selectAudio: (String) -> Unit = {}
    internal var selectSubtitle: (String?) -> Unit = {}

    fun chooseAudio(id: String) = selectAudio(id)
    fun chooseSubtitle(id: String?) = selectSubtitle(id)
}
