package com.glztv.app

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * Runtime form-factor guard ensuring Android TV / Google TV / Fire TV hardware
 * always uses the TV interface and regular ATV update channel, even if a Mobile
 * APK were accidentally sideloaded or pushed onto a TV box.
 */
object DeviceFormFactor {
    fun isTelevision(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        if (uiMode == Configuration.UI_MODE_TYPE_TELEVISION) return true
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            pm.hasSystemFeature("amazon.hardware.fire_tv")
    }

    fun isMobileExperience(context: Context): Boolean =
        BuildConfig.IS_MOBILE && !isTelevision(context)
}
