package com.glztv.app.data

import com.glztv.app.Channel
import com.glztv.app.EpgGuide
import java.util.Locale

/**
 * Inspects live sports events, filters out empty/placeholder/expired channels,
 * resolves direct watchable channels for live scoreboard games (by team matchup
 * or verified broadcast network + EPG inspection), and enriches generic EPG titles
 * (such as "Live: College Football") with the real active matchup & live score.
 */
object SportsChannelMatcher {

    private val PLACEHOLDER_TITLE_REGEX = Regex(
        """^\(?((?:MLB|NBA|NFL|NHL|MLS|NCAA|NCAAF|NCAAB|ESPN\+?|UFC|PPV|EVENT|LIVE\s*EVENT|SPORTS\s*PPV)\s*#?\d{0,4})\)?(\s*\|.*)?$""",
        RegexOption.IGNORE_CASE
    )
    private val EMPTY_EVENT_KEYWORDS = Regex(
        """\b(NO\s+EVENT|OFFLINE|NO\s+SIGNAL|COMING\s+SOON|TBA|TBD|STANDBY|TEST\s+FEED|209\d-\d{2}-\d{2}|24/7)\b""",
        RegexOption.IGNORE_CASE
    )
    private val GENERIC_SPORTS_EPG_REGEX = Regex(
        """^(?:live:\s*)?(?:college\s+football|college\s+basketball|men'?s\s+college\s+basketball|nba\s+basketball|nfl\s+football|mlb\s+baseball|nhl\s+hockey|sports\s+ppv|live\s+sports|live\s+broadcast|live\s+event|ncaa\s+football|ncaa\s+basketball|friday\s+night\s+football|saturday\s+night\s+football|sunday\s+night\s+football|monday\s+night\s+football|thursday\s+night\s+football|big\s+noon\s+saturday)\b""",
        RegexOption.IGNORE_CASE
    )
    private val NON_SPORTS_EPG_PENALTY = Regex(
        """\b(paid\s+programming|infomercial|local\s+news|eyewitness\s+news|action\s+news|weather|today\s+show|good\s+morning|jeopardy|wheel\s+of\s+fortune)\b""",
        RegexOption.IGNORE_CASE
    )

    private data class NetworkMatcher(
        val canonical: String,
        val broadcastPatterns: List<String>,
        val channelTokens: List<String>,
        val excludeChannelTokens: List<String> = emptyList()
    )

    private val NETWORK_MATCHERS = listOf(
        NetworkMatcher(
            canonical = "ESPN2",
            broadcastPatterns = listOf("ESPN2", "ESPN 2"),
            channelTokens = listOf("ESPN2", "ESPN 2")
        ),
        NetworkMatcher(
            canonical = "ESPNU",
            broadcastPatterns = listOf("ESPNU", "ESPN U"),
            channelTokens = listOf("ESPNU", "ESPN U")
        ),
        NetworkMatcher(
            canonical = "ESPNEWS",
            broadcastPatterns = listOf("ESPNEWS", "ESPN NEWS"),
            channelTokens = listOf("ESPNEWS", "ESPN NEWS")
        ),
        NetworkMatcher(
            canonical = "ESPN DEPORTES",
            broadcastPatterns = listOf("ESPN DEPORTES", "ESPNDEP"),
            channelTokens = listOf("ESPN DEPORTES", "ESPNDEP")
        ),
        NetworkMatcher(
            canonical = "ESPN",
            broadcastPatterns = listOf("ESPN"),
            channelTokens = listOf("ESPN"),
            excludeChannelTokens = listOf("ESPN2", "ESPN 2", "ESPNU", "ESPNEWS", "ESPN+", "ESPN PLUS", "DEPORTES")
        ),
        NetworkMatcher(
            canonical = "FS1",
            broadcastPatterns = listOf("FS1", "FOX SPORTS 1"),
            channelTokens = listOf("FS1", "FOX SPORTS 1")
        ),
        NetworkMatcher(
            canonical = "FS2",
            broadcastPatterns = listOf("FS2", "FOX SPORTS 2"),
            channelTokens = listOf("FS2", "FOX SPORTS 2")
        ),
        NetworkMatcher(
            canonical = "FOX DEPORTES",
            broadcastPatterns = listOf("FOX DEPORTES"),
            channelTokens = listOf("FOX DEPORTES")
        ),
        NetworkMatcher(
            canonical = "FOX",
            broadcastPatterns = listOf("FOX"),
            channelTokens = listOf("FOX", "WOFL", "WNYW", "KTTV", "WFLD", "WTTG", "WJBK", "WAGA", "KRIV", "KDFW", "WFXT", "WTVT", "WSVN"),
            excludeChannelTokens = listOf("FOX NEWS", "FOX BUSINESS", "FOX WEATHER", "FS1", "FS2", "FOX SPORTS", "DEPORTES", "SOCCER")
        ),
        NetworkMatcher(
            canonical = "ABC",
            broadcastPatterns = listOf("ABC"),
            channelTokens = listOf("ABC", "WFTV", "WABC", "KABC", "WLS", "WPVI", "WFAA", "KTRK", "WJLA", "WCVB", "WPLG", "WXYZ", "WSB"),
            excludeChannelTokens = listOf("ABC NEWS", "SPARK", "FAMILY")
        ),
        NetworkMatcher(
            canonical = "CBSSN",
            broadcastPatterns = listOf("CBSSN", "CBS SPORTS NETWORK", "CBS SPORTS"),
            channelTokens = listOf("CBSSN", "CBS SPORTS")
        ),
        NetworkMatcher(
            canonical = "CBS",
            broadcastPatterns = listOf("CBS"),
            channelTokens = listOf("CBS", "WKMG", "WCBS", "KCBS", "WBBM", "KYW", "KTVT", "KHOU", "WUSA", "WBZ", "WFOR", "WWJ", "WANF"),
            excludeChannelTokens = listOf("CBS NEWS", "CBSSN", "CBS SPORTS", "CNBC")
        ),
        NetworkMatcher(
            canonical = "NBC",
            broadcastPatterns = listOf("NBC"),
            channelTokens = listOf("NBC", "WESH", "WNBC", "KNBC", "WMAQ", "WCAU", "KXAS", "KPRC", "WRC", "WBTS", "WTVJ", "WDIV", "WXIA"),
            excludeChannelTokens = listOf("NBC NEWS", "NBCSN", "CNBC", "MSNBC", "UNIVERSO", "SPORTS")
        ),
        NetworkMatcher(
            canonical = "TNT",
            broadcastPatterns = listOf("TNT"),
            channelTokens = listOf("TNT"),
            excludeChannelTokens = listOf("TNT SPORTS")
        ),
        NetworkMatcher(
            canonical = "TBS",
            broadcastPatterns = listOf("TBS"),
            channelTokens = listOf("TBS")
        ),
        NetworkMatcher(
            canonical = "TRUTV",
            broadcastPatterns = listOf("TRUTV", "TRU TV"),
            channelTokens = listOf("TRUTV", "TRU TV")
        ),
        NetworkMatcher(
            canonical = "NBA TV",
            broadcastPatterns = listOf("NBA TV", "NBATV"),
            channelTokens = listOf("NBA TV", "NBATV")
        ),
        NetworkMatcher(
            canonical = "NFL NETWORK",
            broadcastPatterns = listOf("NFL NETWORK", "NFLN", "NFL NET"),
            channelTokens = listOf("NFL NETWORK", "NFLN", "NFL NET")
        ),
        NetworkMatcher(
            canonical = "MLB NETWORK",
            broadcastPatterns = listOf("MLB NETWORK", "MLBN", "MLB NET"),
            channelTokens = listOf("MLB NETWORK", "MLBN", "MLB NET")
        ),
        NetworkMatcher(
            canonical = "BTN",
            broadcastPatterns = listOf("BTN", "BIG TEN NETWORK", "BIG TEN"),
            channelTokens = listOf("BTN", "BIG TEN")
        ),
        NetworkMatcher(
            canonical = "SEC NETWORK",
            broadcastPatterns = listOf("SEC NETWORK", "SECN"),
            channelTokens = listOf("SEC NETWORK", "SECN")
        ),
        NetworkMatcher(
            canonical = "ACC NETWORK",
            broadcastPatterns = listOf("ACC NETWORK", "ACCN"),
            channelTokens = listOf("ACC NETWORK", "ACCN")
        ),
        NetworkMatcher(
            canonical = "CW",
            broadcastPatterns = listOf("THE CW", "CW"),
            channelTokens = listOf("THE CW", "CW", "WKCF", "WPIX", "KTLA")
        ),
        NetworkMatcher(
            canonical = "TELEMUNDO",
            broadcastPatterns = listOf("TELEMUNDO", "TELEM"),
            channelTokens = listOf("TELEMUNDO", "WKAQ", "WNJU", "KVEA", "WTMO")
        ),
        NetworkMatcher(
            canonical = "UNIVISION",
            broadcastPatterns = listOf("UNIVISION", "TUDN", "UNIMAS"),
            channelTokens = listOf("UNIVISION", "TUDN", "UNIMAS", "WLII", "WVEN")
        )
    )

    private fun wordBoundaryContains(haystackUpper: String, tokenUpper: String): Boolean {
        if (tokenUpper.isBlank()) return false
        return if (tokenUpper.length <= 4 && !tokenUpper.contains(" ")) {
            Regex("""(?:^|[^A-Z0-9])${Regex.escape(tokenUpper)}(?:$|[^A-Z0-9])""").containsMatchIn(haystackUpper)
        } else {
            haystackUpper.contains(tokenUpper)
        }
    }

    private fun matchesNetwork(channel: Channel, matcher: NetworkMatcher): Boolean {
        val identity = "${channel.callSign} ${channel.name} ${channel.id}".uppercase(Locale.US)
        if (matcher.excludeChannelTokens.any { wordBoundaryContains(identity, it) }) return false
        return matcher.channelTokens.any { wordBoundaryContains(identity, it) }
    }

    private fun resolveNetworkMatcher(broadcastName: String): NetworkMatcher? {
        val norm = broadcastName.trim().uppercase(Locale.US)
        if (norm.isEmpty()) return null
        return NETWORK_MATCHERS.firstOrNull { matcher ->
            matcher.broadcastPatterns.any { pat ->
                norm == pat || wordBoundaryContains(norm, pat)
            }
        }
    }

    /**
     * Returns true when a dedicated Live Event / PPV channel is empty, a numbered placeholder
     * (e.g. "NBA 13", "SPORTS PPV"), or has already expired.
     */
    fun isEmptyOrStaleLiveEvent(
        channel: Channel,
        guide: EpgGuide,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!channel.isLiveEvent()) return false
        if (channel.streamUrl.isBlank()) return true
        // Expired event window (> 30 min past end)
        if (channel.eventEndMillis > 0L && nowMillis > channel.eventEndMillis + 30 * 60_000L) {
            return true
        }
        val progTitle = guide.nowProgrammeForChannel(channel, nowMillis)?.title.orEmpty().trim()
        val combined = listOf(channel.eventTitle, progTitle, channel.name)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (combined.isEmpty()) return true

        val bestText = channel.eventTitle.ifBlank { progTitle.ifBlank { channel.name } }.trim()
        if (EMPTY_EVENT_KEYWORDS.containsMatchIn(bestText)) return true
        if (PLACEHOLDER_TITLE_REGEX.matches(bestText) && !bestText.contains(" vs", ignoreCase = true) && !bestText.contains(" @ ")) {
            // Check if the other fields actually carry a real matchup
            val anyReal = combined.any { text ->
                !PLACEHOLDER_TITLE_REGEX.matches(text) && !EMPTY_EVENT_KEYWORDS.containsMatchIn(text)
            }
            if (!anyReal) return true
        }
        return false
    }

    /**
     * Filters Live Event channels, removing empty placeholders and stale/expired entries,
     * and keeping only verified real live/upcoming matchups.
     */
    fun filterValidLiveEvents(
        channels: List<Channel>,
        guide: EpgGuide,
        games: List<SportsGame> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): List<Channel> {
        val activeGames = games.filter { !it.isFinal }
        return channels.filter { ch ->
            if (!ch.isLiveEvent()) return@filter false
            if (isEmptyOrStaleLiveEvent(ch, guide, nowMillis)) return@filter false
            // If ESPN returned live games for this event's league and this channel has a matchup
            // that matches a FINAL game, drop it so completed games don't linger as "Live".
            val textUpper = "${ch.eventTitle} ${ch.name} ${guide.nowProgrammeForChannel(ch, nowMillis)?.title.orEmpty()}"
                .uppercase(Locale.US)
            val matchedFinal = games.firstOrNull { g ->
                g.isFinal && matchesGameTeams(textUpper, g)
            }
            if (matchedFinal != null && activeGames.none { matchesGameTeams(textUpper, it) }) {
                return@filter false
            }
            true
        }
    }

    fun matchesGameTeams(textUpper: String, game: SportsGame): Boolean {
        val awayHit = game.awayTeam.tokens.any { wordBoundaryContains(textUpper, it) }
        val homeHit = game.homeTeam.tokens.any { wordBoundaryContains(textUpper, it) }
        return awayHit && homeHit
    }

    /**
     * Finds the best direct watchable channel in the user's lineup for a given ESPN scoreboard game.
     *
     * Priority 1: Dedicated valid live-event channel matching both teams.
     * Priority 2: Linear broadcast channel matching ESPN's broadcast network (e.g. FOX, ABC, CBS,
     *             NBC, ESPN, ESPN2, FS1, TNT, NBA TV, NFL Network), scored by EPG sports confirmation.
     */
    fun findDirectChannel(
        game: SportsGame,
        channels: List<Channel>,
        guide: EpgGuide,
        nowMillis: Long = System.currentTimeMillis()
    ): Channel? {
        if (channels.isEmpty()) return null

        // Priority 1: Exact team matchup in event channels or EPG titles
        val exactMatch = channels.firstOrNull { ch ->
            if (ch.streamUrl.isBlank()) return@firstOrNull false
            if (ch.isLiveEvent() && isEmptyOrStaleLiveEvent(ch, guide, nowMillis)) return@firstOrNull false
            val prog = guide.nowProgrammeForChannel(ch, nowMillis)
            val textUpper = "${ch.eventTitle} ${ch.name} ${prog?.title.orEmpty()} ${prog?.description.orEmpty()}"
                .uppercase(Locale.US)
            matchesGameTeams(textUpper, game)
        }
        if (exactMatch != null) return exactMatch

        // Priority 2: Match against ESPN broadcast networks + inspect EPG to pick the right channel
        if (game.broadcasts.isEmpty()) return null
        val sportKeywords = when (game.subLeague) {
            "NCAAF" -> listOf("COLLEGE FOOTBALL", "NCAAF", "FOOTBALL", "BIG NOON")
            "NCAAB" -> listOf("COLLEGE BASKETBALL", "NCAAB", "BASKETBALL")
            "NFL" -> listOf("NFL", "FOOTBALL")
            "NBA" -> listOf("NBA", "BASKETBALL")
            "MLB" -> listOf("MLB", "BASEBALL")
            else -> listOf("SPORTS", "LIVE")
        }

        var bestChannel: Channel? = null
        var bestScore = Int.MIN_VALUE

        for ((bIdx, broadcast) in game.broadcasts.withIndex()) {
            val matcher = resolveNetworkMatcher(broadcast) ?: continue
            val networkPriorityBonus = (10 - bIdx).coerceAtLeast(1) * 10
            for (ch in channels) {
                if (ch.streamUrl.isBlank() || ch.isLiveEvent()) continue
                if (!matchesNetwork(ch, matcher)) continue

                var score = networkPriorityBonus
                val prog = guide.nowProgrammeForChannel(ch, nowMillis)
                val progText = "${prog?.title.orEmpty()} ${prog?.description.orEmpty()}".uppercase(Locale.US)

                if (prog != null && progText.isNotBlank()) {
                    if (matchesGameTeams(progText, game)) score += 120
                    if (sportKeywords.any { progText.contains(it) }) score += 50
                    if (game.isLive && NON_SPORTS_EPG_PENALTY.containsMatchIn(progText)) score -= 60
                }
                // Slight preference for lower channel numbers / HD call signs
                if (ch.name.contains("HD", ignoreCase = true) || ch.callSign.isNotBlank()) score += 5

                if (score > bestScore) {
                    bestScore = score
                    bestChannel = ch
                }
            }
        }
        return bestChannel
    }

    /**
     * Inspects a channel's current show and enriches generic sports EPG labels
     * (like "Live: College Football", "NBA Basketball", "SPORTS PPV") with the actual
     * live matchup & score from ESPN when matched.
     */
    fun resolveChannelShowTitle(
        channel: Channel,
        guide: EpgGuide,
        games: List<SportsGame>,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val prog = guide.nowProgrammeForChannel(channel, nowMillis)
        val rawTitle = prog?.title?.takeIf { it.isNotBlank() }
            ?: channel.eventTitle.takeIf { it.isNotBlank() }
            ?: ""

        if (games.isNotEmpty()) {
            val activeOrSoon = games.filter { g ->
                g.isLive || (g.isUpcoming && g.startTimeMillis in (nowMillis - 15 * 60_000L)..(nowMillis + 45 * 60_000L))
            }

            // 1. Direct team match in event title / channel name / EPG title or description
            val inspectText = "${channel.eventTitle} ${channel.name} $rawTitle ${prog?.description.orEmpty()}"
                .uppercase(Locale.US)
            val directTeamGame = activeOrSoon.firstOrNull { matchesGameTeams(inspectText, it) }
            if (directTeamGame != null) {
                return directTeamGame.liveHeadline
            }

            // 2. If EPG shows a generic sports label (e.g. "Live: College Football" on FOX/ABC/ESPN/ESPN2),
            //    inspect live games currently broadcasting on this exact network!
            val isGenericSportsTitle = rawTitle.isBlank() ||
                GENERIC_SPORTS_EPG_REGEX.containsMatchIn(rawTitle) ||
                PLACEHOLDER_TITLE_REGEX.matches(rawTitle)

            if (isGenericSportsTitle) {
                val liveNetworkGame = activeOrSoon.firstOrNull { game ->
                    if (!leagueConsistentWithLabel(game, rawTitle)) return@firstOrNull false
                    game.broadcasts.any { b ->
                        val matcher = resolveNetworkMatcher(b) ?: return@any false
                        matchesNetwork(channel, matcher)
                    }
                }
                if (liveNetworkGame != null) {
                    return liveNetworkGame.liveHeadline
                }
            }
        }

        return rawTitle.ifBlank { channel.group.ifBlank { "Live Broadcast" } }
    }

    private fun leagueConsistentWithLabel(game: SportsGame, rawTitle: String): Boolean {
        val upper = rawTitle.uppercase(Locale.US)
        if (upper.isBlank() || upper.contains("SPORTS PPV") || upper.contains("LIVE BROADCAST") || upper.contains("LIVE EVENT")) {
            return true
        }
        return when {
            upper.contains("COLLEGE FOOTBALL") || upper.contains("NCAAF") || upper.contains("BIG NOON") -> game.subLeague == "NCAAF"
            upper.contains("COLLEGE BASKETBALL") || upper.contains("NCAAB") -> game.subLeague == "NCAAB"
            upper.contains("NBA") -> game.league == "NBA"
            upper.contains("NFL") -> game.league == "NFL"
            upper.contains("MLB") || upper.contains("BASEBALL") -> game.league == "MLB"
            upper.contains("FOOTBALL") -> game.league == "NFL" || game.subLeague == "NCAAF"
            upper.contains("BASKETBALL") -> game.league == "NBA" || game.subLeague == "NCAAB"
            else -> true
        }
    }
}
