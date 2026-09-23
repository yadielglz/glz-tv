package com.glztv.app.ui

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Locale

object WeatherFormatter {
    fun symbol(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67, in 80..82 -> "🌧️"
        in 71..77, 85, 86 -> "❄️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }

    fun description(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1 -> "Mainly Clear"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Rain Showers"
        in 85..86 -> "Snow Showers"
        in 95..99 -> "Thunderstorm"
        else -> "Fair"
    }

    fun atmosphericGradient(weatherCode: Int): List<Color> = when (weatherCode) {
        0, 1 -> listOf(Color(0xFF0F3057), Color(0xFF00587A), Color(0xFF008891))
        2, 3 -> listOf(Color(0xFF1E2630), Color(0xFF2B3A4A), Color(0xFF3B4D61))
        45, 48 -> listOf(Color(0xFF2C3E50), Color(0xFF3F4C6B), Color(0xFF606C88))
        in 51..67, in 80..82 -> listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77))
        in 71..77, 85, 86 -> listOf(Color(0xFF1A2A3A), Color(0xFF2C3E50), Color(0xFF4CA1AF))
        in 95..99 -> listOf(Color(0xFF1A0B2E), Color(0xFF2C1654), Color(0xFF4B1E78))
        else -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    }

    fun dayLabel(value: String): String = runCatching {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("EEE", Locale.getDefault())
        output.format(requireNotNull(input.parse(value))).uppercase(Locale.getDefault())
    }.getOrDefault(value)
}
