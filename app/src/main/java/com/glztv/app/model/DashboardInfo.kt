package com.glztv.app.model

data class WeatherInfo(
    val temperature: Int,
    val weatherCode: Int,
    val location: String,
    val forecast: List<ForecastDay> = emptyList()
)

data class ForecastDay(
    val date: String,
    val weatherCode: Int,
    val high: Int,
    val low: Int,
    val precipitationChance: Int
)

data class NetworkInfo(val connection: String, val isp: String)
