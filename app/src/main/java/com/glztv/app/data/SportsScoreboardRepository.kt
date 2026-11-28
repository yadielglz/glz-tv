package com.glztv.app.data

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Immutable
data class SportsTeam(
    val abbreviation: String,
    val shortName: String,
    val displayName: String,
    val score: String,
    val logoUrl: String,
    val rank: Int? = null,
    val winner: Boolean = false,
    val tokens: List<String> = emptyList()
) {
    val rankedShortName: String
        get() = if (rank != null && rank in 1..25) "#$rank $shortName" else shortName

    val rankedAbbr: String
        get() = if (rank != null && rank in 1..25) "#$rank $abbreviation" else abbreviation
}

@Immutable
data class SportsGame(
    val id: String,
    val league: String,
    val subLeague: String,
    val state: String, // "in", "pre", "post"
    val statusDetail: String,
    val startTimeMillis: Long,
    val awayTeam: SportsTeam,
    val homeTeam: SportsTeam,
    val broadcasts: List<String>
) {
    val isLive: Boolean get() = state == "in"
    val isFinal: Boolean get() = state == "post"
    val isUpcoming: Boolean get() = state == "pre"

    val primaryBroadcast: String
        get() = broadcasts.firstOrNull { it.isNotBlank() }.orEmpty()

    val matchupTitle: String
        get() = "${awayTeam.rankedShortName} vs ${homeTeam.rankedShortName}"

    val liveHeadline: String
        get() = when {
            isLive -> {
                val scorePart = "${awayTeam.rankedShortName} ${awayTeam.score} – ${homeTeam.rankedShortName} ${homeTeam.score}"
                if (statusDetail.isNotBlank()) "$scorePart · $statusDetail" else scorePart
            }
            isFinal -> "${awayTeam.shortName} ${awayTeam.score} – ${homeTeam.shortName} ${homeTeam.score} · Final"
            else -> {
                val base = "${awayTeam.rankedShortName} @ ${homeTeam.rankedShortName}"
                if (statusDetail.isNotBlank()) "$base · $statusDetail" else base
            }
        }
}

class SportsScoreboardRepository(private val client: OkHttpClient) {

    private data class FeedSpec(
        val league: String,
        val subLeague: String,
        val url: String
    )

    companion object {
        private val FEEDS = listOf(
            FeedSpec("NFL", "NFL", "https://site.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard"),
            FeedSpec("NBA", "NBA", "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard"),
            FeedSpec("MLB", "MLB", "https://site.api.espn.com/apis/site/v2/sports/baseball/mlb/scoreboard"),
            FeedSpec("NCAA", "NCAAF", "https://site.api.espn.com/apis/site/v2/sports/football/college-football/scoreboard"),
            FeedSpec("NCAA", "NCAAB", "https://site.api.espn.com/apis/site/v2/sports/basketball/mens-college-basketball/scoreboard")
        )

        private val MAJOR_BROADCAST_NETWORKS = setOf(
            "ABC", "CBS", "FOX", "NBC", "ESPN", "ESPN2", "FS1", "TNT", "TBS", "CW",
            "NFL NETWORK", "NBA TV", "MLB NETWORK", "BTN", "SEC NETWORK", "ACC NETWORK"
        )

        fun parseScoreboardJson(
            rawJson: String,
            league: String,
            subLeague: String
        ): List<SportsGame> {
            val root = runCatching { JSONObject(rawJson) }.getOrNull() ?: return emptyList()
            val events = root.optJSONArray("events") ?: return emptyList()
            val games = mutableListOf<SportsGame>()
            for (i in 0 until events.length()) {
                val ev = events.optJSONObject(i) ?: continue
                val id = ev.optString("id").ifBlank { "$subLeague-$i" }
                val startMs = parseIsoDateMillis(ev.optString("date"))
                val statusObj = ev.optJSONObject("status")
                val statusType = statusObj?.optJSONObject("type")
                val rawState = statusType?.optString("state", "pre").orEmpty().lowercase(Locale.US)
                val state = when (rawState) {
                    "in" -> "in"
                    "post" -> "post"
                    else -> "pre"
                }
                val detail = statusType?.optString("shortDetail").orEmpty()
                    .ifBlank { statusType?.optString("detail").orEmpty() }
                    .ifBlank { statusType?.optString("description").orEmpty() }

                val competitions = ev.optJSONArray("competitions")
                val comp = competitions?.optJSONObject(0) ?: continue
                val competitors = comp.optJSONArray("competitors") ?: continue
                if (competitors.length() < 2) continue

                var homeJson: JSONObject? = null
                var awayJson: JSONObject? = null
                for (cIdx in 0 until competitors.length()) {
                    val c = competitors.optJSONObject(cIdx) ?: continue
                    when (c.optString("homeAway").lowercase(Locale.US)) {
                        "home" -> homeJson = c
                        "away" -> awayJson = c
                    }
                }
                val home = parseTeam(homeJson ?: competitors.optJSONObject(0) ?: continue)
                val away = parseTeam(awayJson ?: competitors.optJSONObject(1) ?: continue)
                if (home.shortName.isBlank() || away.shortName.isBlank()) continue

                val broadcasts = LinkedHashSet<String>()
                comp.optJSONArray("broadcasts")?.let { arr ->
                    for (bIdx in 0 until arr.length()) {
                        val b = arr.optJSONObject(bIdx) ?: continue
                        val names = b.optJSONArray("names") ?: continue
                        for (nIdx in 0 until names.length()) {
                            val name = names.optString(nIdx).trim()
                            if (name.isNotEmpty()) broadcasts += name
                        }
                    }
                }
                comp.optJSONArray("geoBroadcasts")?.let { arr ->
                    for (bIdx in 0 until arr.length()) {
                        val b = arr.optJSONObject(bIdx) ?: continue
                        val media = b.optJSONObject("media")
                        val shortName = media?.optString("shortName").orEmpty().trim()
                        if (shortName.isNotEmpty()) broadcasts += shortName
                    }
                }

                games += SportsGame(
                    id = "$subLeague:$id",
                    league = league,
                    subLeague = subLeague,
                    state = state,
                    statusDetail = detail,
                    startTimeMillis = startMs,
                    awayTeam = away,
                    homeTeam = home,
                    broadcasts = broadcasts.toList()
                )
            }
            return games
        }

        private fun parseTeam(comp: JSONObject): SportsTeam {
            val team = comp.optJSONObject("team") ?: JSONObject()
            val abbr = team.optString("abbreviation").trim()
            val shortName = team.optString("shortDisplayName").trim()
                .ifBlank { team.optString("name").trim() }
                .ifBlank { abbr }
            val displayName = team.optString("displayName").trim().ifBlank { shortName }
            val location = team.optString("location").trim()
            val mascot = team.optString("name").trim()
            val score = comp.optString("score").trim().ifBlank { "0" }
            val logo = team.optString("logo").trim()
            val rawRank = comp.optJSONObject("curatedRank")?.optInt("current", 99) ?: 99
            val rank = if (rawRank in 1..25) rawRank else null
            val winner = comp.optBoolean("winner", false)
            val tokens = LinkedHashSet<String>().apply {
                listOf(displayName, shortName, mascot, location, abbr).forEach { raw ->
                    val cleaned = raw.trim().uppercase(Locale.US)
                    if (cleaned.length >= 2) add(cleaned)
                }
            }.toList()
            return SportsTeam(
                abbreviation = abbr.ifBlank { shortName.take(4).uppercase(Locale.US) },
                shortName = shortName,
                displayName = displayName,
                score = score,
                logoUrl = logo,
                rank = rank,
                winner = winner,
                tokens = tokens
            )
        }

        private fun parseIsoDateMillis(raw: String): Long {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return 0L
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm'Z'"
            )
            val normalized = trimmed.replace(Regex("\\.\\d+"), "")
            for (pattern in patterns) {
                runCatching {
                    val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    sdf.parse(normalized)?.time
                }.getOrNull()?.let { return it }
            }
            return 0L
        }

        fun sortGames(games: List<SportsGame>): List<SportsGame> =
            games.sortedWith(
                compareBy<SportsGame>(
                    {
                        when (it.state) {
                            "in" -> 0
                            "pre" -> 1
                            else -> 2
                        }
                    },
                    {
                        // Prioritize national TV broadcasts and ranked matchups within the same state
                        val hasNational = it.broadcasts.any { b -> b.uppercase(Locale.US) in MAJOR_BROADCAST_NETWORKS }
                        val isRanked = (it.awayTeam.rank != null) || (it.homeTeam.rank != null)
                        val proLeague = it.league in setOf("NFL", "NBA", "MLB")
                        when {
                            hasNational && (proLeague || isRanked) -> 0
                            hasNational -> 1
                            proLeague || isRanked -> 2
                            else -> 3
                        }
                    },
                    { if (it.startTimeMillis > 0L) it.startTimeMillis else Long.MAX_VALUE }
                )
            )
    }

    suspend fun loadAllScores(): List<SportsGame> = coroutineScope {
        val fetched = FEEDS.map { feed ->
            async(Dispatchers.IO) {
                runCatching {
                    val req = Request.Builder()
                        .url(feed.url)
                        .header("User-Agent", "GLZ-TV/2.0")
                        .get()
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@use emptyList()
                        val body = resp.body?.string().orEmpty()
                        if (body.isBlank()) return@use emptyList()
                        parseScoreboardJson(body, feed.league, feed.subLeague)
                    }
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()

        sortGames(fetched)
    }
}
