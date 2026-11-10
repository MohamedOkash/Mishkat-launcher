package com.mishkat.launcher.wallpaper

import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.mishkat.launcher.i18n.LocaleRepository
import com.mishkat.launcher.theme.WALLPAPER_PRESETS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class AutoWallpaperInterval(val minutes: Int, val labelAr: String, val labelEn: String) {
    OFF(0, "معطل", "Off"),
    EVERY_5_MIN(5, "كل 5 دقائق", "Every 5 min"),
    EVERY_15_MIN(15, "كل 15 دقيقة", "Every 15 min"),
    EVERY_60_MIN(60, "كل ساعة", "Every 1 hour"),
    ON_BOOT(-1, "عند فتح اللانشر", "On Launch");

    companion object {
        fun fromMinutes(min: Int): AutoWallpaperInterval =
            entries.firstOrNull { it.minutes == min } ?: OFF
    }
}

@Immutable
data class AutoWallpaperState(
    val interval: AutoWallpaperInterval = AutoWallpaperInterval.OFF,
    val currentWallpaperIndex: Int = 0,
)

class AutoWallpaperManager(
    private val prefs: DataStore<Preferences>,
    private val localeRepository: LocaleRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(AutoWallpaperState())
    val state: StateFlow<AutoWallpaperState> = _state.asStateFlow()

    private val availableWallpapers = WALLPAPER_PRESETS.map { it.id }.filter { it != "auto" }

    fun start() {
        scope.launch(Dispatchers.IO) {
            val savedMinutes = prefs.data.first()[KEY_AUTO_INTERVAL] ?: 0
            val interval = AutoWallpaperInterval.fromMinutes(savedMinutes)
            _state.value = _state.value.copy(interval = interval)

            if (interval == AutoWallpaperInterval.ON_BOOT) {
                cycleNextWallpaper()
            }

            while (true) {
                val current = _state.value.interval
                if (current.minutes > 0) {
                    val waitMs = current.minutes * 60_000L
                    delay(waitMs)
                    cycleNextWallpaper()
                } else {
                    delay(10_000L)
                }
            }
        }
    }

    fun setInterval(interval: AutoWallpaperInterval) {
        _state.value = _state.value.copy(interval = interval)
        scope.launch(Dispatchers.IO) {
            prefs.edit { it[KEY_AUTO_INTERVAL] = interval.minutes }
        }
    }

    fun cycleNextWallpaper() {
        if (availableWallpapers.isEmpty()) return
        val nextIdx = (_state.value.currentWallpaperIndex + 1) % availableWallpapers.size
        _state.value = _state.value.copy(currentWallpaperIndex = nextIdx)
        val nextId = availableWallpapers[nextIdx]
        localeRepository.setWallpaperStyle(nextId)
    }

    companion object {
        private val KEY_AUTO_INTERVAL = intPreferencesKey("wallpaper_auto_interval_min")
    }
}
