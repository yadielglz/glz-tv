package com.glztv.app

import com.glztv.app.GithubUpdateManager.UpdateChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

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
        // Everything before the floor is not offered to Production
        assertFalse(GithubUpdateManager.matchesChannel("v26.828.01", UpdateChannel.PRODUCTION))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-rc2", UpdateChannel.PRODUCTION))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-weekly", UpdateChannel.PRODUCTION))
    }

    @Test
    fun monthlyChannelAcceptsMonthlyAndStableTags() {
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05-monthly", UpdateChannel.MONTHLY))
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05", UpdateChannel.MONTHLY))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-weekly", UpdateChannel.MONTHLY))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-testing", UpdateChannel.MONTHLY))
    }

    @Test
    fun weeklyChannelAcceptsWeeklyAndLegacyFamilyBuildsPlusStable() {
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05-weekly", UpdateChannel.WEEKLY))
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05-family1", UpdateChannel.WEEKLY))
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05", UpdateChannel.WEEKLY))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-rc1", UpdateChannel.WEEKLY))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-testing", UpdateChannel.WEEKLY))
    }

    @Test
    fun biweeklyChannelAcceptsBiweeklyAndLegacyRcBuildsPlusStable() {
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05-biweekly", UpdateChannel.BIWEEKLY))
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05-rc3", UpdateChannel.BIWEEKLY))
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05", UpdateChannel.BIWEEKLY))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-weekly", UpdateChannel.BIWEEKLY))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-testing", UpdateChannel.BIWEEKLY))
    }

    @Test
    fun testingChannelAcceptsTestingAndLegacyBetaBuildsPlusStable() {
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05-testing", UpdateChannel.TESTING))
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05-beta2", UpdateChannel.TESTING))
        assertTrue(GithubUpdateManager.matchesChannel("v26.902.05", UpdateChannel.TESTING))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-rc1", UpdateChannel.TESTING))
        assertFalse(GithubUpdateManager.matchesChannel("v26.902.05-weekly", UpdateChannel.TESTING))
    }

    @Test
    fun channelIdRoundTripsAndSupportsLegacyAliases() {
        // Direct IDs
        assertEquals(UpdateChannel.PRODUCTION, UpdateChannel.from("production"))
        assertEquals(UpdateChannel.WEEKLY, UpdateChannel.from("weekly"))
        assertEquals(UpdateChannel.BIWEEKLY, UpdateChannel.from("biweekly"))
        assertEquals(UpdateChannel.MONTHLY, UpdateChannel.from("monthly"))
        assertEquals(UpdateChannel.TESTING, UpdateChannel.from("testing"))

        // Legacy aliases
        assertEquals(UpdateChannel.TESTING, UpdateChannel.from("beta"))
        assertEquals(UpdateChannel.WEEKLY, UpdateChannel.from("family"))
        assertEquals(UpdateChannel.BIWEEKLY, UpdateChannel.from("rc"))

        // Defaults
        assertEquals(UpdateChannel.PRODUCTION, UpdateChannel.from(null))
        assertEquals(UpdateChannel.PRODUCTION, UpdateChannel.from("nonsense"))
    }

    @Test
    fun monthlyMilestoneDeadlineCalculatesCorrectDateAndForceOverdue() {
        val tz = TimeZone.getTimeZone("GMT-5")
        // October 16, 2026 release (2026.1016.15)
        val releaseCal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.OCTOBER, 16, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val releaseMillis = releaseCal.timeInMillis

        val deadline = GithubUpdateManager.getMonthlyMilestoneDeadline(releaseMillis)
        val deadlineCal = Calendar.getInstance(tz).apply { timeInMillis = deadline }

        // Must be Nov 13, 2026 at 23:59:00 GMT-5
        assertEquals(2026, deadlineCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, deadlineCal.get(Calendar.MONTH))
        assertEquals(13, deadlineCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, deadlineCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, deadlineCal.get(Calendar.MINUTE))

        // Before milestone deadline (e.g. Nov 10, 2026)
        val checkNov10 = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 10, 12, 0, 0)
        }.timeInMillis
        val statusNov10 = GithubUpdateManager.checkMilestoneStatus("2026.1016.15", checkNov10)
        assertFalse("Should not be past milestone on Nov 10", statusNov10.isPastMilestone)
        assertFalse("Should not force update on Nov 10", statusNov10.isForceRequired)

        // After milestone deadline, but within 7 days grace (e.g. Nov 15, 2026)
        val checkNov15 = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
        }.timeInMillis
        val statusNov15 = GithubUpdateManager.checkMilestoneStatus("2026.1016.15", checkNov15)
        assertTrue("Should be past milestone on Nov 15", statusNov15.isPastMilestone)
        assertFalse("Should not force update yet on Nov 15", statusNov15.isForceRequired)

        // 7+ days past milestone deadline (e.g. Nov 21, 2026) -> Force update required!
        val checkNov21 = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 21, 12, 0, 0)
        }.timeInMillis
        val statusNov21 = GithubUpdateManager.checkMilestoneStatus("2026.1016.15", checkNov21)
        assertTrue("Should be past milestone on Nov 21", statusNov21.isPastMilestone)
        assertTrue("Should force update on Nov 21 (7+ days past mark)", statusNov21.isForceRequired)
    }
}
