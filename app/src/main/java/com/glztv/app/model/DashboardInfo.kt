package com.glztv.app.model

data class WeatherInfo(
    val temperature: Int,
    val weatherCode: Int,
    val location: String
)

data class NetworkInfo(val connection: String, val isp: String)
