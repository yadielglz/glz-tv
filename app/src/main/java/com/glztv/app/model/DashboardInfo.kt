package com.glztv.app.model

data class WeatherInfo(
    val temperature: Int,
    val weatherCode: Int,
    val location: String,
    val forecast: List<ForecastDay> = emptyList(),
    val feelsLike: Int = temperature,
    val humidity: Int = 0,
    val windSpeedMph: Int = 0,
    val windDirection: String = "N",
    val uvIndex: Double = 0.0,
    val pressureHpa: Int = 1013,
    val hourly: List<HourlyForecast> = emptyList()
)

data class ForecastDay(
    val date: String,
    val weatherCode: Int,
    val high: Int,
    val low: Int,
    val precipitationChance: Int
)

data class HourlyForecast(
    val timeLabel: String,
    val weatherCode: Int,
    val temperature: Int,
    val precipitationChance: Int
)

data class NetworkInfo(val connection: String, val isp: String)

