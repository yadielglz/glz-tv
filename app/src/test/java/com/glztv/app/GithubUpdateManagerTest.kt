package com.glztv.app

import com.glztv.app.GithubUpdateManager.UpdateChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubUpdateManagerTest {
    @Test
    fun comparesSemanticVersionsNumerically() {
        assertTrue(GithubUpdateManager.compareVersions("1.10", "1.6.1") > 0)
        assertTrue(GithubUpdateManager.compareVersions("2.0", "1.99.9") > 0)
        assertTrue(GithubUpdateManager.compareVersions("1.6.2", "1.6.1-firetv") > 0)
        assertEquals(0, GithubUpdateManager.compareVersions("1.6.1", "1.6.1-firetv"))
    }

    @Test
    fun productionChannelTakesStableTagsFromTheFloorOnward() {
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05", UpdateChannel.PRODUCTION))
        assertTrue(GithubUpdateManager.matchesChannel("v27.101.01", UpdateChannel.PRODUCTION))
        // Everything before the floor is Family, not Production.
        assertFalse(GithubUpdateManager.matchesChannel("v26.828.01", UpdateChannel.PRODUCTION))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-rc2", UpdateChannel.PRODUCTION))
    }

    @Test
    fun familyChannelKeepsThePreFloorHistoryPlusFamilyBuilds() {
        assertTrue(GithubUpdateManager.matchesChannel("v26.828.01", UpdateChannel.FAMILY))
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05", UpdateChannel.FAMILY))
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05-family1", UpdateChannel.FAMILY))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-rc1", UpdateChannel.FAMILY))
    }

    @Test
    fun preReleaseChannelsAcceptTheirMarkerPlusStable() {
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05-rc3", UpdateChannel.RELEASE_CANDIDATE))
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05", UpdateChannel.RELEASE_CANDIDATE))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-beta1", UpdateChannel.RELEASE_CANDIDATE))

        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05-beta2", UpdateChannel.BETA))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-rc1", UpdateChannel.BETA))
    }

    @Test
    fun channelIdRoundTripsAndFallsBackToProduction() {
        assertEquals(UpdateChannel.BETA, UpdateChannel.from("beta"))
        assertEquals(UpdateChannel.RELEASE_CANDIDATE, UpdateChannel.from("rc"))
        assertEquals(UpdateChannel.PRODUCTION, UpdateChannel.from(null))
        assertEquals(UpdateChannel.PRODUCTION, UpdateChannel.from("nonsense"))
    }
}
