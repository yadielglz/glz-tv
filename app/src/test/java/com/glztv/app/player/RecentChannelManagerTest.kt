package com.glztv.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentChannelManagerTest {
    @Test fun newestChannelComesFirst() {
        assertEquals(listOf("c", "b", "a"), RecentChannelManager.update(listOf("b", "a"), "c"))
    }

    @Test fun consecutiveDuplicateDoesNotChangeHistory() {
        assertEquals(listOf("b", "a"), RecentChannelManager.update(listOf("b", "a"), "b"))
    }

    @Test fun duplicateIsMovedToFront() {
        assertEquals(listOf("a", "c", "b"), RecentChannelManager.update(listOf("c", "b", "a"), "a"))
    }

    @Test fun historyIsLimitedToTen() {
        val result = (1..12).fold(emptyList<String>()) { history, id ->
            RecentChannelManager.update(history, id.toString())
        }
        assertEquals(10, result.size)
        assertEquals("12", result.first())
        assertEquals("3", result.last())
    }

    @Test fun previousSkipsCurrentChannel() {
        assertEquals("b", RecentChannelManager.previous(listOf("a", "b", "c"), "a"))
        assertNull(RecentChannelManager.previous(listOf("a"), "a"))
    }
}
