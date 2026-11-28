package com.glztv.app.data

import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import com.glztv.app.Programme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SportsScoreboardAndMatcherTest {

    @Test
    fun parsesEspnScoreboardJsonWithRanksLogosAndBroadcasts() {
        val sampleJson = """
            {
              "events": [
                {
                  "id": "401858314",
                  "date": "2026-11-28T17:00Z",
                  "status": {
                    "type": {
                      "state": "in",
                      "shortDetail": "3:16 - 2nd"
                    }
                  },
                  "competitions": [
                    {
                      "broadcasts": [{ "names": ["ABC"] }],
                      "competitors": [
                        {
                          "homeAway": "home",
                          "score": "10",
                          "team": {
                            "abbreviation": "SYR",
                            "shortDisplayName": "Syracuse",
                            "displayName": "Syracuse Orange",
                            "location": "Syracuse",
                            "name": "Orange",
                            "logo": "https://a.espncdn.com/i/teamlogos/ncaa/500/183.png"
                          },
                          "curatedRank": { "current": 99 }
                        },
                        {
                          "homeAway": "away",
                          "score": "21",
                          "team": {
                            "abbreviation": "ND",
                            "shortDisplayName": "Notre Dame",
                            "displayName": "Notre Dame Fighting Irish",
                            "location": "Notre Dame",
                            "name": "Fighting Irish",
                            "logo": "https://a.espncdn.com/i/teamlogos/ncaa/500/87.png"
                          },
                          "curatedRank": { "current": 4 }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val games = SportsScoreboardRepository.parseScoreboardJson(sampleJson, "NCAA", "NCAAF")
        assertEquals(1, games.size)
        val g = games.first()
        assertTrue(g.isLive)
        assertEquals("3:16 - 2nd", g.statusDetail)
        assertEquals(listOf("ABC"), g.broadcasts)
        assertEquals(4, g.awayTeam.rank)
        assertNull(g.homeTeam.rank)
        assertEquals("#4 Notre Dame 21 – Syracuse 10 · 3:16 - 2nd", g.liveHeadline)
    }

    @Test
    fun matchesBroadcastNetworkWithoutFalsePositivesAndEnrichesGenericEpg() {
        val now = 1_800_000_000_000L
        val foxNews = Channel("foxnews", "FOX News", "News", "25", "", "https://s/fn.m3u8", emptyMap())
        val foxOrlando = Channel("wofl", "FOX 35 Orlando", "Orlando", "35", "", "https://s/fox.m3u8", emptyMap())
        val abcOrlando = Channel("wftv", "ABC Orlando", "Orlando", "9", "", "https://s/abc.m3u8", emptyMap())
        val espn = Channel("espn", "ESPN", "Sports", "12", "", "https://s/espn.m3u8", emptyMap())
        val espn2 = Channel("espn2", "ESPN 2", "Sports", "13", "", "https://s/espn2.m3u8", emptyMap())

        val guide = EpgGuide(
            programmes = mapOf(
                "wofl" to listOf(Programme("wofl", now - 600_000L, now + 3_600_000L, "Live: College Football", "")),
                "wftv" to listOf(Programme("wftv", now - 600_000L, now + 3_600_000L, "Live: College Football", "")),
                "foxnews" to listOf(Programme("foxnews", now - 600_000L, now + 3_600_000L, "Fox & Friends", ""))
            ),
            channelNames = emptyMap()
        )

        val michOsu = SportsGame(
            id = "NCAAF:1",
            league = "NCAA",
            subLeague = "NCAAF",
            state = "in",
            statusDetail = "2nd · 8:31",
            startTimeMillis = now - 3_600_000L,
            awayTeam = SportsTeam("MICH", "Michigan", "Michigan Wolverines", "0", "", null, false, listOf("MICHIGAN", "WOLVERINES", "MICH")),
            homeTeam = SportsTeam("OSU", "Ohio State", "Ohio State Buckeyes", "14", "", 2, false, listOf("OHIO STATE", "BUCKEYES", "OSU")),
            broadcasts = listOf("FOX")
        )
        val espnGame = SportsGame(
            id = "NCAAF:2",
            league = "NCAA",
            subLeague = "NCAAF",
            state = "in",
            statusDetail = "Q1 · 5:00",
            startTimeMillis = now - 1_200_000L,
            awayTeam = SportsTeam("ALA", "Alabama", "Alabama Crimson Tide", "7", "", 5, false, listOf("ALABAMA", "ALA")),
            homeTeam = SportsTeam("AUB", "Auburn", "Auburn Tigers", "3", "", null, false, listOf("AUBURN", "AUB")),
            broadcasts = listOf("ESPN")
        )

        val channels = listOf(foxNews, foxOrlando, abcOrlando, espn, espn2)
        val matchedFox = SportsChannelMatcher.findDirectChannel(michOsu, channels, guide, now)
        assertNotNull(matchedFox)
        assertEquals("wofl", matchedFox!!.id)

        val matchedEspn = SportsChannelMatcher.findDirectChannel(espnGame, channels, guide, now)
        assertNotNull(matchedEspn)
        assertEquals("espn", matchedEspn!!.id)

        val enrichedFoxTitle = SportsChannelMatcher.resolveChannelShowTitle(foxOrlando, guide, listOf(michOsu, espnGame), now)
        assertEquals("Michigan 0 – #2 Ohio State 14 · 2nd · 8:31", enrichedFoxTitle)
    }

    @Test
    fun filtersEmptyPlaceholderAndExpiredLiveEventChannels() {
        val now = 1_800_000_000_000L
        val emptyPlaceholder = Channel("ev1", "NBA 13", "Live Events", "30-01", "", "https://s/1.m3u8", emptyMap(), "", "NBA", 0L, 0L)
        val expiredEvent = Channel("ev2", "Lakers vs Celtics", "Live Events", "30-02", "", "https://s/2.m3u8", emptyMap(), "Lakers vs Celtics", "NBA", now - 14_400_000L, now - 3_600_000L)
        val realLiveEvent = Channel("ev3", "UFC 310", "Live Events", "30-03", "", "https://s/3.m3u8", emptyMap(), "UFC 310: Main Event", "UFC", now - 1_800_000L, now + 7_200_000L)

        val valid = SportsChannelMatcher.filterValidLiveEvents(
            listOf(emptyPlaceholder, expiredEvent, realLiveEvent),
            EpgGuide.Empty,
            emptyList(),
            now
        )
        assertEquals(1, valid.size)
        assertEquals("ev3", valid.first().id)
        assertTrue(SportsChannelMatcher.isEmptyOrStaleLiveEvent(emptyPlaceholder, EpgGuide.Empty, now))
        assertTrue(SportsChannelMatcher.isEmptyOrStaleLiveEvent(expiredEvent, EpgGuide.Empty, now))
        assertFalse(SportsChannelMatcher.isEmptyOrStaleLiveEvent(realLiveEvent, EpgGuide.Empty, now))
    }
}
