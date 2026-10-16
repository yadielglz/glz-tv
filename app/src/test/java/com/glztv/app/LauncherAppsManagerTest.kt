package com.glztv.app

import com.glztv.app.launcher.LauncherAppItem
import com.glztv.app.launcher.LauncherAppsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.Color

class LauncherAppsManagerTest {

    @Test
    fun appSortingPrioritizesFavoritesThenAlphabetical() {
        val apps = listOf(
            LauncherAppItem(
                packageName = "com.netflix.ninja",
                activityName = "MainActivity",
                label = "Netflix",
                banner = null,
                icon = null,
                accent = Color.Red,
                isFavorite = false,
                isHidden = false,
                isSystemApp = false
            ),
            LauncherAppItem(
                packageName = "com.google.android.youtube.tv",
                activityName = "MainActivity",
                label = "YouTube",
                banner = null,
                icon = null,
                accent = Color.Red,
                isFavorite = true,
                isHidden = false,
                isSystemApp = false
            ),
            LauncherAppItem(
                packageName = "com.disney.disneyplus",
                activityName = "MainActivity",
                label = "Disney+",
                banner = null,
                icon = null,
                accent = Color.Blue,
                isFavorite = false,
                isHidden = false,
                isSystemApp = false
            ),
            LauncherAppItem(
                packageName = "com.amazon.amazonvideo.livingroom",
                activityName = "MainActivity",
                label = "Prime Video",
                banner = null,
                icon = null,
                accent = Color.Cyan,
                isFavorite = true,
                isHidden = false,
                isSystemApp = false
            )
        )

        val sorted = apps.sortedWith(
            compareByDescending<LauncherAppItem> { it.isFavorite }
                .thenBy { it.label.lowercase() }
        )

        assertEquals("Prime Video", sorted[0].label)
        assertEquals("YouTube", sorted[1].label)
        assertEquals("Disney+", sorted[2].label)
        assertEquals("Netflix", sorted[3].label)
    }

    @Test
    fun familyChannelValidation() {
        val family = GithubUpdateManager.UpdateChannel.from("family")
        assertEquals(GithubUpdateManager.UpdateChannel.WEEKLY, family)
        assertTrue(family == GithubUpdateManager.UpdateChannel.FAMILY)

        val weekly = GithubUpdateManager.UpdateChannel.from("weekly")
        assertEquals(GithubUpdateManager.UpdateChannel.WEEKLY, weekly)

        val prod = GithubUpdateManager.UpdateChannel.from("production")
        assertFalse(prod == GithubUpdateManager.UpdateChannel.FAMILY)

        val defaultChannel = GithubUpdateManager.UpdateChannel.from(null)
        assertFalse(defaultChannel == GithubUpdateManager.UpdateChannel.FAMILY)
    }
}
