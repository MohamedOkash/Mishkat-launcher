package com.mishkat.launcher.prayer

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mishkat.launcher.core.Logger
import com.mishkat.launcher.system.SleepTimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.TimeZone

@Immutable
data class PrayerUiState(
    val city: CitySpec = DEFAULT_CITY,
    val methodId: String = "EGYPTIAN",
    val todayTimes: DayPrayerTimes? = null,
    val nextPrayer: NextPrayerInfo? = null,
    val hijriDateAr: String = "",
    val hijriDateEn: String = "",
    val activeWindow: AdhkarWindow = AdhkarWindow.MORNING,
    val windowAdhkar: List<DhikrSpec> = emptyList(),
    val currentDhikrIndex: Int = 0,
    val currentDhikrCount: Int = 0,
    /** Non-null when an Adhan alert is currently displayed in AdhanOverlay. */
    val activeAdhanAlert: PrayerKind? = null,
    val upcomingHijriEvent: HijriEventInfo? = null,
    val audioAdhanEnabled: Boolean = true,
) {
    val currentDhikr: DhikrSpec?
        get() = windowAdhkar.getOrNull(currentDhikrIndex)

    companion object {
        val DEFAULT_CITY = CitySpec(
            id = "eg_cairo",
            nameAr = "القاهرة",
            nameEn = "Cairo",
            country = "EG",
            lat = 30.0444,
            lon = 31.2357,
            tz = "Africa/Cairo",
        )
    }
}

/**
 * §4 + §9: Loads offline cities, country->method map, and Hisn al-Muslim adhkar from assets,
 * updates the next-prayer countdown and active Adhkar window every second, and triggers AdhanOverlay
 * when a prayer time arrives.
 */
class PrayerRepository(
    private val context: Context,
    private val prefs: DataStore<Preferences>,
    private val sleepTimer: SleepTimerRepository,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _cities = MutableStateFlow<List<CitySpec>>(emptyList())
    val cities: StateFlow<List<CitySpec>> = _cities.asStateFlow()

    private val _state = MutableStateFlow(PrayerUiState())
    val state: StateFlow<PrayerUiState> = _state.asStateFlow()

    private var countryMethods: Map<String, String> = emptyMap()
    private var allAdhkar: List<DhikrSpec> = emptyList()
    private var lastTriggeredPrayerEpoch: Long = 0L

    fun start() {
        scope.launch {
            withContext(Dispatchers.IO) {
                loadAssets()
            }
            val savedPrefs = runCatching { prefs.data.first() }.getOrNull()
            val savedCityId = savedPrefs?.get(KEY_CITY_ID)
            val audioEnabled = savedPrefs?.get(KEY_AUDIO_ADHAN) ?: true
            _state.value = _state.value.copy(audioAdhanEnabled = audioEnabled)
            val initialCity = selectInitialCity(savedCityId)
            applyCity(initialCity, persist = false)

            // 1-second ticker for countdown, Adhkar window transitions, and Adhan trigger.
            while (true) {
                tick(System.currentTimeMillis())
                delay(1_000L)
            }
        }
    }

    fun selectCity(city: CitySpec) {
        scope.launch {
            applyCity(city, persist = true)
            tick(System.currentTimeMillis())
        }
    }

    /** Called when the user presses OK on the Tasbih / Adhkar card. */
    fun incrementTasbih() {
        val cur = _state.value
        val dhikr = cur.currentDhikr ?: return
        val nextCount = cur.currentDhikrCount + 1
        if (nextCount < dhikr.targetCount) {
            _state.value = cur.copy(currentDhikrCount = nextCount)
        } else {
            val nextIdx = if (cur.windowAdhkar.isNotEmpty()) {
                (cur.currentDhikrIndex + 1) % cur.windowAdhkar.size
            } else {
                0
            }
            _state.value = cur.copy(
                currentDhikrIndex = nextIdx,
                currentDhikrCount = 0,
            )
        }
    }

    fun dismissAdhanAlert() {
        AdhanSoundPlayer.stop()
        _state.value = _state.value.copy(activeAdhanAlert = null)
    }

    /** Manual preview trigger from the Prayer card / panel so the user can test AdhanOverlay anytime. */
    fun triggerAdhanPreview(kind: PrayerKind = _state.value.nextPrayer?.kind ?: PrayerKind.FAJR) {
        _state.value = _state.value.copy(activeAdhanAlert = kind)
        if (_state.value.audioAdhanEnabled) {
            AdhanSoundPlayer.playAdhanChime(scope)
        }
    }

    fun setAudioAdhanEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(audioAdhanEnabled = enabled)
        if (!enabled) {
            AdhanSoundPlayer.stop()
        } else if (_state.value.activeAdhanAlert != null) {
            AdhanSoundPlayer.playAdhanChime(scope)
        }
        scope.launch {
            prefs.edit { it[KEY_AUDIO_ADHAN] = enabled }
        }
    }

    fun adhkarForWindow(window: AdhkarWindow): List<DhikrSpec> =
        allAdhkar.filter { it.window == window }

    private suspend fun applyCity(city: CitySpec, persist: Boolean) {
        val method = countryMethods[city.country] ?: countryMethods["DEFAULT"] ?: "MUSLIM_WORLD_LEAGUE"
        _state.value = _state.value.copy(city = city, methodId = method)
        if (persist) {
            prefs.edit { it[KEY_CITY_ID] = city.id }
        }
        Logger.system { "Prayer city=${city.nameEn} (${city.country}) -> method=$method" }
    }

    private fun tick(nowMs: Long) {
        val cur = _state.value
        val city = cur.city
        val method = cur.methodId
        val today = PrayerEngine.computeForEpoch(city, method, nowMs)
        val next = PrayerEngine.nextPrayer(city, method, today, nowMs)
        val window = PrayerEngine.resolveAdhkarWindow(
            today = today,
            nowEpochMs = nowMs,
            sleepTimerActive = sleepTimer.state.value.active,
        )

        // Check if any obligatory prayer just entered within the last 4 seconds.
        var alert = cur.activeAdhanAlert
        for ((kind, pMs) in today.obligatoryPrayers) {
            if (nowMs in pMs..(pMs + 4_000L) && lastTriggeredPrayerEpoch != pMs) {
                lastTriggeredPrayerEpoch = pMs
                alert = kind
                if (cur.audioAdhanEnabled) {
                    AdhanSoundPlayer.playAdhanChime(scope)
                }
                Logger.system { "Adhan alert triggered for $kind" }
                break
            }
        }

        val hijriEvent = cur.upcomingHijriEvent ?: PrayerEngine.computeUpcomingHijriEvent(nowMs, city.tz)

        val windowItems = if (window != cur.activeWindow || cur.windowAdhkar.isEmpty()) {
            allAdhkar.filter { it.window == window }
        } else {
            cur.windowAdhkar
        }
        val resetIndex = if (window != cur.activeWindow) 0 else cur.currentDhikrIndex
        val resetCount = if (window != cur.activeWindow) 0 else cur.currentDhikrCount

        _state.value = cur.copy(
            todayTimes = today,
            nextPrayer = next,
            hijriDateAr = PrayerEngine.formatHijriDate(nowMs, city.tz, isArabic = true),
            hijriDateEn = PrayerEngine.formatHijriDate(nowMs, city.tz, isArabic = false),
            activeWindow = window,
            windowAdhkar = windowItems,
            currentDhikrIndex = resetIndex,
            currentDhikrCount = resetCount,
            activeAdhanAlert = alert,
            upcomingHijriEvent = hijriEvent,
        )
    }

    private fun selectInitialCity(savedId: String?): CitySpec {
        val list = _cities.value
        if (list.isEmpty()) return PrayerUiState.DEFAULT_CITY
        if (savedId != null) {
            list.firstOrNull { it.id == savedId }?.let { return it }
        }
        val deviceTz = TimeZone.getDefault().id
        list.firstOrNull { it.tz.equals(deviceTz, ignoreCase = true) }?.let { return it }
        val deviceCountry = Locale.getDefault().country.uppercase(Locale.US)
        list.firstOrNull { it.country.equals(deviceCountry, ignoreCase = true) }?.let { return it }
        return list.first()
    }

    private fun loadAssets() {
        runCatching {
            context.assets.open("prayer/country_methods.json").bufferedReader().use { r ->
                countryMethods = json.decodeFromString(
                    MapSerializer(String.serializer(), String.serializer()),
                    r.readText(),
                )
            }
        }.onFailure { t -> Logger.e(Logger.TAG_SYSTEM, t) { "Failed to load country_methods.json" } }

        runCatching {
            context.assets.open("prayer/cities_pack.json").bufferedReader().use { r ->
                _cities.value = json.decodeFromString(
                    ListSerializer(CitySpec.serializer()),
                    r.readText(),
                )
            }
        }.onFailure { t -> Logger.e(Logger.TAG_SYSTEM, t) { "Failed to load cities_pack.json" } }

        runCatching {
            context.assets.open("prayer/adhkar.json").bufferedReader().use { r ->
                allAdhkar = json.decodeFromString(
                    ListSerializer(DhikrSpec.serializer()),
                    r.readText(),
                )
            }
        }.onFailure { t -> Logger.e(Logger.TAG_SYSTEM, t) { "Failed to load adhkar.json" } }
    }

    private companion object {
        val KEY_CITY_ID = stringPreferencesKey("prayer_city_id")
        val KEY_AUDIO_ADHAN = booleanPreferencesKey("prayer_audio_adhan_enabled")
    }
}
