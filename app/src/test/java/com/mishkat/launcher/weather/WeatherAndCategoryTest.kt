package com.mishkat.launcher.weather

import com.mishkat.launcher.radio.StationEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class WeatherAndCategoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parseOpenMeteo7DayForecastJson() {
        val sampleJson = """
            {
              "current": { "temperature_2m": 26.4, "weather_code": 1 },
              "daily": {
                "time": ["2026-11-08","2026-11-09","2026-11-10","2026-11-11","2026-11-12","2026-11-13","2026-11-14"],
                "weather_code": [0, 1, 2, 3, 61, 0, 1],
                "temperature_2m_max": [28.2, 27.9, 26.1, 25.4, 22.8, 24.0, 25.1],
                "temperature_2m_min": [17.8, 17.2, 16.5, 16.0, 14.9, 15.2, 16.1]
              }
            }
        """.trimIndent()

        val resp = json.decodeFromString(OpenMeteoResponse.serializer(), sampleJson)
        assertEquals(26, resp.current?.temperature2m?.roundToInt())
        assertEquals(7, resp.daily?.time?.size)
        assertEquals(28, resp.daily?.tempMax?.first()?.roundToInt())
        assertEquals(18, resp.daily?.tempMin?.first()?.roundToInt())
    }

    @Test
    fun stationThematicVsReciterClassification() {
        val reciter = StationEntity(1, "إبراهيم الأخضر", "Ibrahim Al-Akdar", "https://backup.qurango.net/radio/ibrahim_alakdar")
        val tafsir = StationEntity(200, "إذاعة التفسير", "Tafsir Radio", "https://backup.qurango.net/radio/tafsir")
        val ruqyah = StationEntity(201, "الرقية الشرعية", "Ruqyah", "https://backup.qurango.net/radio/roqiah")

        assertFalse(reciter.isThematicChannel)
        assertTrue(tafsir.isThematicChannel)
        assertTrue(ruqyah.isThematicChannel)
    }
}
