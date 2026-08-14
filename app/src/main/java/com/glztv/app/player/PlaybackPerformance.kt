package com.glztv.app.player

import android.os.Build
import android.os.Trace

object PlaybackPerformance {
    private const val CHANNEL_SWITCH_TRACE = "channelSwitch"

    fun channelSelected(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.beginAsyncSection(CHANNEL_SWITCH_TRACE, channelId.hashCode())
        }
    }

    fun firstFrameRendered(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.endAsyncSection(CHANNEL_SWITCH_TRACE, channelId.hashCode())
        }
    }
}
