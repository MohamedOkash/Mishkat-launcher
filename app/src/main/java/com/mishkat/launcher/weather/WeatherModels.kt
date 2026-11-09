package com.mishkat.launcher.weather

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class DayForecast(
    val dateIso: String,
    val weatherCode: Int,
    val tempMaxC: Int,
    val tempMinC: Int,
)

@Immutable
@Serializable
data class WeatherSnapshot(
    val cityId: String,
    val fetchedAtEpochMs: Long,
    val currentTempC: Int,
    val currentWeatherCode: Int,
    val days: List<DayForecast> = emptyList(),
)

@Serializable
internal data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null,
    val daily: OpenMeteoDaily? = null,
)

@Serializable
internal data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature2m: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0,
)

@Serializable
internal data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val tempMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val tempMin: List<Double> = emptyList(),
)
