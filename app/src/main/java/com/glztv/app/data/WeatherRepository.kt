package com.glztv.app.data

import android.net.Uri
import com.glztv.app.model.ForecastDay
import com.glztv.app.model.WeatherInfo
import okhttp3.OkHttpClient
import org.json.JSONObject

class WeatherRepository(client: OkHttpClient) {
    private val sourceClient = SourceClient(client)

    private fun fetchTextWithFallback(url: String): String {
        return try {
            sourceClient.fetchText(url, emptyMap())
        } catch (e: Exception) {
            if (url.startsWith("https://")) {
                val httpUrl = url.replaceFirst("https://", "http://")
                sourceClient.fetchText(httpUrl, emptyMap())
            } else {
                throw e
            }
        }
    }

    fun load(location: String): WeatherInfo {
        val trimmedLocation = location.trim().ifBlank { "San Juan" }
        val geocodingJson = fetchTextWithFallback(
            "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=${Uri.encode(trimmedLocation)}&count=1&language=en&format=json"
        )
        val geocoding = JSONObject(geocodingJson)
        val results = geocoding.optJSONArray("results")
        if (results == null || results.length() == 0) {
            throw IllegalArgumentException("Location \"$trimmedLocation\" not found")
        }
        val place = results.getJSONObject(0)
        val latitude = place.getDouble("latitude")
        val longitude = place.getDouble("longitude")
        val displayName = place.optString("name", trimmedLocation)
        val forecastJson = fetchTextWithFallback(
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,surface_pressure" +
                "&hourly=temperature_2m,weather_code,precipitation_probability" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max" +
                "&wind_speed_unit=mph&temperature_unit=fahrenheit&forecast_days=5&timezone=auto"
        )
        val forecast = JSONObject(forecastJson)
        val current = forecast.getJSONObject("current")
        val daily = forecast.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val highs = daily.getJSONArray("temperature_2m_max")
        val lows = daily.getJSONArray("temperature_2m_min")
        val precipitation = daily.getJSONArray("precipitation_probability_max")
        val uvMax = daily.optJSONArray("uv_index_max")
        
        val days = buildList {
            for (index in 0 until minOf(5, dates.length())) {
                add(
                    ForecastDay(
                        date = dates.getString(index),
                        weatherCode = codes.getInt(index),
                        high = highs.getDouble(index).toInt(),
                        low = lows.getDouble(index).toInt(),
                        precipitationChance = precipitation.optInt(index, 0)
                    )
                )
            }
        }

        val hourlyList = buildList {
            if (forecast.has("hourly")) {
                val hourly = forecast.getJSONObject("hourly")
                val times = hourly.getJSONArray("time")
                val hCodes = hourly.getJSONArray("weather_code")
                val hTemps = hourly.getJSONArray("temperature_2m")
                val hPrecip = hourly.optJSONArray("precipitation_probability")
                val currentTimeStr = current.optString("time", "")

                var startIndex = 0
                if (currentTimeStr.isNotEmpty()) {
                    for (i in 0 until times.length()) {
                        if (times.getString(i) >= currentTimeStr) {
                            startIndex = i
                            break
                        }
                    }
                }

                for (i in startIndex until minOf(startIndex + 18, times.length())) {
                    val rawTime = times.getString(i)
                    val label = formatHourlyTime(rawTime, isFirst = (i == startIndex))
                    add(
                        com.glztv.app.model.HourlyForecast(
                            timeLabel = label,
                            weatherCode = hCodes.getInt(i),
                            temperature = hTemps.getDouble(i).toInt(),
                            precipitationChance = hPrecip?.optInt(i, 0) ?: 0
                        )
                    )
                }
            }
        }

        val windDegrees = current.optInt("wind_direction_10m", 0)
        val windDir = windDegreeToDirection(windDegrees)
        val currentUv = if (uvMax != null && uvMax.length() > 0) uvMax.optDouble(0, 0.0) else 0.0

        return WeatherInfo(
            temperature = current.getDouble("temperature_2m").toInt(),
            weatherCode = current.getInt("weather_code"),
            location = displayName,
            forecast = days,
            feelsLike = current.optDouble("apparent_temperature", current.getDouble("temperature_2m")).toInt(),
            humidity = current.optInt("relative_humidity_2m", 0),
            windSpeedMph = current.optDouble("wind_speed_10m", 0.0).toInt(),
            windDirection = windDir,
            uvIndex = currentUv,
            pressureHpa = current.optDouble("surface_pressure", 1013.25).toInt(),
            hourly = hourlyList
        )
    }

    private fun formatHourlyTime(rawTime: String, isFirst: Boolean): String {
        if (isFirst) return "NOW"
        return runCatching {
            val parts = rawTime.split("T")
            if (parts.size > 1) {
                val hourStr = parts[1].split(":")[0]
                val hour = hourStr.toInt()
                when {
                    hour == 0 -> "12 AM"
                    hour == 12 -> "12 PM"
                    hour > 12 -> "${hour - 12} PM"
                    else -> "$hour AM"
                }
            } else rawTime
        }.getOrDefault(rawTime)
    }

    private fun windDegreeToDirection(degrees: Int): String {
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val normalized = ((degrees % 360) + 360) % 360
        val index = ((normalized + 11.25) / 22.5).toInt() % 16
        return directions[index.coerceIn(0, 15)]
    }
}
