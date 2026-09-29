package com.glztv.app

/** Optional GlzHub styling. Defaults also work with external playlists. */
data class ChannelLogoStyle(
    val background: String = "",
    val foreground: String = "",
    val mode: String = "auto",
    val removeBackground: Boolean = true
)
