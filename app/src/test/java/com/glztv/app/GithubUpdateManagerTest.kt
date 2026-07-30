package com.glztv.app

import org.junit.Assert.assertEquals
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
}
