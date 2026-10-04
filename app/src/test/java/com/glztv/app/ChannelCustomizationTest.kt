package com.glztv.app

import com.glztv.app.data.ChannelCustomizationManager
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ChannelCustomizationTest {
    @Test
    fun parsesChannelNumbersCorrectly() {
        assertEquals(1.0, ChannelCustomizationManager.channelNumberValue("1"), 0.0001)
        assertEquals(2.1, ChannelCustomizationManager.channelNumberValue("2.1"), 0.0001)
        assertEquals(105.0, ChannelCustomizationManager.channelNumberValue("105"), 0.0001)
        assertEquals(4.2, ChannelCustomizationManager.channelNumberValue("Ch 4.2 HD"), 0.0001)
        assertEquals(30.00001, ChannelCustomizationManager.channelNumberValue("30-01"), 0.000001)
        assertEquals(30.00011, ChannelCustomizationManager.channelNumberValue("30-11"), 0.000001)
        assertEquals(Double.MAX_VALUE, ChannelCustomizationManager.channelNumberValue("No Number"), 0.0001)
    }

    @Test
    fun sortsChannelsNumericallyWithDecimals() {
        val list = listOf(
            Channel("ch-10", "Channel 10", "General", "10", "", ""),
            Channel("ch-2", "Channel 2", "General", "2", "", ""),
            Channel("ch-1-1", "Channel 1.1", "General", "1.1", "", ""),
            Channel("ch-1", "Channel 1", "General", "1", "", ""),
            Channel("ch-2-2", "Channel 2.2", "General", "2.2", "", "")
        )

        val sorted = list.sortedWith(
            compareBy<Channel> { ChannelCustomizationManager.channelNumberValue(it.number) }
                .thenBy { it.number }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )

        val sortedNumbers = sorted.map { it.number }
        assertEquals(listOf("1", "1.1", "2", "2.2", "10"), sortedNumbers)
    }

    @Test
    fun sortsPpvAndSubChannelsInCorrectNumericalOrder() {
        val list = listOf(
            Channel("ch-30-11", "30-11 NBA Event", "Events", "30-11", "", ""),
            Channel("ch-30-01", "30-01 MLB Event", "Events", "30-01", "", ""),
            Channel("ch-30-02", "30-02 MLB Event", "Events", "30-02", "", ""),
            Channel("ch-29-01", "29-01 PPV Event", "Events", "29-01", "", ""),
            Channel("ch-31-01", "31-01 MLB Event", "Events", "31-01", "", "")
        )

        val sorted = list.sortedWith(
            compareBy<Channel> { ChannelCustomizationManager.channelNumberValue(it.number) }
                .thenBy { it.number }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )

        val sortedNumbers = sorted.map { it.number }
        assertEquals(listOf("29-01", "30-01", "30-02", "30-11", "31-01"), sortedNumbers)
    }
}
