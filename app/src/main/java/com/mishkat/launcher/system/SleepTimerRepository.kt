package com.mishkat.launcher.system

import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Immutable
data class SleepTimerState(
    val active: Boolean = false,
    val remainingMs: Long = 0L,
    /** Normalized audio volume multiplier in [0f..1f] during the 5-second fade-out window (R13). */
    val volumeScale: Float = 1f,
) {
    val isFading: Boolean
        get() = active && remainingMs in 1..SleepTimerPolicy.FADE_OUT_MS
}

/**
 * Pure sleep-timer math, unit-tested per R19.
 */
object SleepTimerPolicy {
    const val FADE_OUT_MS = 5_000L
    val PRESET_MINUTES = intArrayOf(0, 15, 30, 60, 90)

    fun computeState(targetEpochMs: Long, nowEpochMs: Long): SleepTimerState {
        if (targetEpochMs <= 0L) return SleepTimerState()
        val remaining = (targetEpochMs - nowEpochMs).coerceAtLeast(0L)
        if (remaining == 0L) return SleepTimerState(active = false, remainingMs = 0L, volumeScale = 0f)
        val scale = if (remaining < FADE_OUT_MS) {
            (remaining.toFloat() / FADE_OUT_MS.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
        return SleepTimerState(active = true, remainingMs = remaining, volumeScale = scale)
    }

    /** Cycles through [PRESET_MINUTES] (0 = off → 15 → 30 → 60 → 90 → 0). */
    fun nextPresetMinutes(currentRemainingMs: Long): Int {
        if (currentRemainingMs <= 0L) return PRESET_MINUTES[1]
        // Round up to the active preset bucket so clicking during minute 14:xx advances from 15 -> 30.
        val currentMin = ((currentRemainingMs + 59_999L) / 60_000L).toInt()
        val activeBucket = PRESET_MINUTES.firstOrNull { it >= currentMin } ?: 0
        if (activeBucket == 0) return 0
        return PRESET_MINUTES.firstOrNull { it > activeBucket } ?: 0
    }

    fun formatRemaining(remainingMs: Long): String {
        val totalSec = ((remainingMs + 999L) / 1000L).coerceAtLeast(0L)
        val min = totalSec / 60L
        val sec = totalSec % 60L
        return String.format(java.util.Locale.US, "%02d:%02d", min, sec)
    }
}

/**
 * R13: single source of truth for the Sleep Timer (serves Radio and screen/adhkar).
 * Persists [KEY_TARGET_EPOCH] in DataStore so an active timer survives process death.
 */
class SleepTimerRepository(
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    private var tickerJob: Job? = null

    fun start() {
        scope.launch {
            val savedTarget = runCatching { prefs.data.first()[KEY_TARGET_EPOCH] ?: 0L }.getOrDefault(0L)
            val now = System.currentTimeMillis()
            if (savedTarget > now) {
                Logger.system { "Restored sleep timer with ${(savedTarget - now) / 1000}s left" }
                startTicker(savedTarget)
            } else if (savedTarget > 0L) {
                prefs.edit { it.remove(KEY_TARGET_EPOCH) }
            }
        }
    }

    fun cyclePreset() {
        val nextMin = SleepTimerPolicy.nextPresetMinutes(_state.value.remainingMs)
        if (nextMin <= 0) cancel() else scheduleMinutes(nextMin)
    }

    fun scheduleMinutes(minutes: Int) {
        if (minutes <= 0) {
            cancel()
            return
        }
        val target = System.currentTimeMillis() + minutes * 60_000L
        scope.launch { prefs.edit { it[KEY_TARGET_EPOCH] = target } }
        Logger.system { "Scheduled sleep timer for $minutes min" }
        startTicker(target)
    }

    fun cancel() {
        tickerJob?.cancel()
        tickerJob = null
        _state.value = SleepTimerState()
        scope.launch { prefs.edit { it.remove(KEY_TARGET_EPOCH) } }
        Logger.system { "Sleep timer cancelled" }
    }

    private fun startTicker(targetEpochMs: Long) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val next = SleepTimerPolicy.computeState(targetEpochMs, now)
                _state.value = next
                if (!next.active) {
                    prefs.edit { it.remove(KEY_TARGET_EPOCH) }
                    Logger.system { "Sleep timer expired -> stopping playback" }
                    _expired.tryEmit(Unit)
                    _state.value = SleepTimerState()
                    break
                }
                // Tick faster (200ms) during the 5-second fade-out for smooth volume ramp.
                val stepMs = if (next.isFading) 200L else 1_000L
                delay(stepMs)
            }
        }
    }

    private companion object {
        val KEY_TARGET_EPOCH = longPreferencesKey("sleep_timer_target_epoch_ms")
    }
}
