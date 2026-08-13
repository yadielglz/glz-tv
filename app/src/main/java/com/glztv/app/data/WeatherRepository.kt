package com.glztv.app.data

import android.net.Uri
import com.glztv.app.model.ForecastDay
import com.glztv.app.model.WeatherInfo
import okhttp3.OkHttpClient
import org.json.JSONObject

class WeatherRepository(client: OkHttpClient) {
    private val sourceClient = SourceClient(client)

    fun load(location: String): WeatherInfo {
        val geocoding = JSONObject(
            sourceClient.fetchText(
                "https://geocoding-api.open-meteo.com/v1/search" +
                    "?name=${Uri.encode(location)}&count=1&language=en&format=json",
                emptyMap()
            )
        )
        val place = geocoding.getJSONArray("results").getJSONObject(0)
        val latitude = place.getDouble("latitude")
        val longitude = place.getDouble("longitude")
        val displayName = place.optString("name", location)
        val forecast = JSONObject(
            sourceClient.fetchText(
                "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,weather_code" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&temperature_unit=fahrenheit&forecast_days=3&timezone=auto",
                emptyMap()
            )
        )
        val current = forecast.getJSONObject("current")
        val daily = forecast.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val highs = daily.getJSONArray("temperature_2m_max")
        val lows = daily.getJSONArray("temperature_2m_min")
        val precipitation = daily.getJSONArray("precipitation_probability_max")
        val days = buildList {
            for (index in 0 until minOf(3, dates.length())) {
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
        return WeatherInfo(
            temperature = current.getDouble("temperature_2m").toInt(),
            weatherCode = current.getInt("weather_code"),
            location = displayName,
            forecast = days
        )
    }
}
