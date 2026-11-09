package com.mishkat.launcher.weather

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mishkat.launcher.core.Logger
import com.mishkat.launcher.prayer.CitySpec
import com.mishkat.launcher.prayer.PrayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

/**
 * §3 + §9 + R3:
 * - Fetches keyless 7-day forecast from Open-Meteo (`https://api.open-meteo.com/v1/forecast`).
 * - Caches the snapshot in DataStore for 30 minutes (`CACHE_TTL_MS`).
 * - Falls back immediately to the cached snapshot when offline.
 */
class WeatherRepository(
    private val prefs: DataStore<Preferences>,
    private val prayerRepository: PrayerRepository,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _snapshot = MutableStateFlow<WeatherSnapshot?>(null)
    val snapshot: StateFlow<WeatherSnapshot?> = _snapshot.asStateFlow()

    fun start() {
        scope.launch(Dispatchers.IO) {
            // Load cached snapshot first so UI has data immediately offline (R3).
            val cachedJson = runCatching { prefs.data.first()[KEY_WEATHER_CACHE] }.getOrNull()
            if (!cachedJson.isNullOrBlank()) {
                runCatching {
                    json.decodeFromString(WeatherSnapshot.serializer(), cachedJson)
                }.onSuccess { _snapshot.value = it }
            }

            prayerRepository.state
                .distinctUntilChangedBy { it.city.id }
                .collect { prayerState ->
                    refreshIfNeeded(prayerState.city, force = false)
                }
        }
    }

    fun refreshNow() {
        val city = prayerRepository.state.value.city
        scope.launch(Dispatchers.IO) {
            refreshIfNeeded(city, force = true)
        }
    }

    private suspend fun refreshIfNeeded(city: CitySpec, force: Boolean) {
        val cur = _snapshot.value
        val now = System.currentTimeMillis()
        if (!force && cur != null && cur.cityId == city.id && (now - cur.fetchedAtEpochMs) < CACHE_TTL_MS) {
            return
        }

        runCatching {
            val url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto",
                city.lat,
                city.lon,
            )
            val raw = fetchHttps(url)
            val parsed = parseOpenMeteo(city.id, now, raw)
            _snapshot.value = parsed
            prefs.edit { it[KEY_WEATHER_CACHE] = json.encodeToString(WeatherSnapshot.serializer(), parsed) }
            Logger.system { "Weather updated for ${city.nameEn}: ${parsed.currentTempC}C" }
        }.onFailure { t ->
            Logger.w(Logger.TAG_SYSTEM, t) { "Weather fetch skipped (offline cache kept)" }
        }
    }

    internal fun parseOpenMeteo(cityId: String, nowMs: Long, rawJson: String): WeatherSnapshot {
        val resp = json.decodeFromString(OpenMeteoResponse.serializer(), rawJson)
        val curTemp = resp.current?.temperature2m?.roundToInt() ?: 0
        val curCode = resp.current?.weatherCode ?: 0
        val daily = resp.daily
        val days = ArrayList<DayForecast>()
        if (daily != null) {
            val count = minOf(
                daily.time.size,
                daily.weatherCode.size,
                daily.tempMax.size,
                daily.tempMin.size,
                7,
            )
            for (i in 0 until count) {
                days += DayForecast(
                    dateIso = daily.time[i],
                    weatherCode = daily.weatherCode[i],
                    tempMaxC = daily.tempMax[i].roundToInt(),
                    tempMinC = daily.tempMin[i].roundToInt(),
                )
            }
        }
        return WeatherSnapshot(
            cityId = cityId,
            fetchedAtEpochMs = nowMs,
            currentTempC = curTemp,
            currentWeatherCode = curCode,
            days = days,
        )
    }

    private fun fetchHttps(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val CACHE_TTL_MS = 30 * 60_000L
        private val KEY_WEATHER_CACHE = stringPreferencesKey("weather_snapshot_json")
    }
}
