package com.glztv.app.model

import androidx.compose.ui.graphics.Color

data class EntertainmentApp(
    val name: String,
    val packageName: String,
    val accent: Color
)

val DefaultEntertainmentApps = listOf(
    EntertainmentApp("YouTube", "com.google.android.youtube.tv", Color(0xFFFF2020)),
    EntertainmentApp("Netflix", "com.netflix.ninja", Color(0xFFE50914)),
    EntertainmentApp("MLB", "com.bamnetworks.mobile.android.gameday.atbat", Color(0xFF17408B)),
    EntertainmentApp("Disney+", "com.disney.disneyplus", Color(0xFF113CCF)),
    EntertainmentApp("Prime Video", "com.amazon.amazonvideo.livingroom", Color(0xFF00A8E1)),
    EntertainmentApp("HBO Max", "com.wbd.stream", Color(0xFF9900FF)),
    EntertainmentApp("Apple TV", "com.apple.atve.androidtv.appletv", Color(0xFFFFFFFF)),
    EntertainmentApp("Hulu", "com.hulu.livingroomplus", Color(0xFF1CE783)),
    EntertainmentApp("Paramount+", "com.cbs.ott", Color(0xFF0064FF)),
    EntertainmentApp("Peacock", "com.peacocktv.peacockandroid", Color(0xFF00B2FE)),
    EntertainmentApp("ESPN", "com.espn.score_center", Color(0xFFCC0000)),
    EntertainmentApp("NBA", "com.nbaimd.gametime.nba2011", Color(0xFF17408B)),
    EntertainmentApp("Spotify", "com.spotify.tv.android", Color(0xFF1ED760)),
    EntertainmentApp("YouTube Music", "com.google.android.youtube.tvmusic", Color(0xFFFF0000)),
    EntertainmentApp("Twitch", "tv.twitch.android.app", Color(0xFF9146FF)),
    EntertainmentApp("Plex", "com.plexapp.android", Color(0xFFE5A00D)),
    EntertainmentApp("VLC", "org.videolan.vlc", Color(0xFFFF8800)),
    EntertainmentApp("Kodi", "org.xbmc.kodi", Color(0xFF17B2E7)),
    EntertainmentApp("SmartTube", "com.teamsmart.videomanager.tv", Color(0xFFFF3333)),
    EntertainmentApp("TiviMate", "ar.tvplayer.tv", Color(0xFF00C853)),
    EntertainmentApp("OTT Navigator", "studio.scillarium.ottnavigator", Color(0xFF29B6F6)),
    EntertainmentApp("Telemundo", "com.nbcuni.nbc.telemundo", Color(0xFFE50914)),
    EntertainmentApp("Univision NOW", "com.univision.prendatv", Color(0xFFFF3366)),
    EntertainmentApp("ViX", "com.univision.vix", Color(0xFFFF5200)),
    EntertainmentApp("Pluto TV", "tv.pluto.android", Color(0xFFFFDF00)),
    EntertainmentApp("Tubi", "com.tubitv", Color(0xFFFF4800)),
    EntertainmentApp("Freevee", "com.amazon.imdb.tv.livingroom", Color(0xFF00E5FF)),
    EntertainmentApp("Plex Live", "com.plexapp.android", Color(0xFFE5A00D)),
    EntertainmentApp("Sling TV", "com.sling", Color(0xFFFF6600)),
    EntertainmentApp("Fubo TV", "tv.fubo.mobile", Color(0xFFFF4500)),
    EntertainmentApp("DirecTV Stream", "com.att.tv", Color(0xFF00A3E0)),
    EntertainmentApp("Philo", "com.philo.philo.google", Color(0xFF00BCD4)),
    EntertainmentApp("Crunchyroll", "com.crunchyroll.crunchyroid", Color(0xFFFF6B00)),
    EntertainmentApp("Haystack News", "com.haystack.android", Color(0xFF00E676)),
    EntertainmentApp("NewsON", "com.newson.newsontv", Color(0xFF1E88E5)),
    EntertainmentApp("Red Bull TV", "com.nousguide.android.rbtv", Color(0xFF00205B)),
    EntertainmentApp("NASA TV", "gov.nasa", Color(0xFF0B3D91)),
    EntertainmentApp("TED TV", "com.ted.android.tv", Color(0xFFE62B1E)),
    EntertainmentApp("Weather Channel", "com.weather.weatherchannel.tv", Color(0xFF00598E)),
    EntertainmentApp("AccuWeather TV", "com.accuweather.android", Color(0xFFFF6600)),
    EntertainmentApp("Google Play Store", "com.android.vending", Color(0xFF00E676)),
    EntertainmentApp("Settings", "com.android.tv.settings", Color(0xFF78909C)),
    EntertainmentApp("GLZ TV App Updater", "com.glztv.app", Color(0xFF00E5FF))
)
