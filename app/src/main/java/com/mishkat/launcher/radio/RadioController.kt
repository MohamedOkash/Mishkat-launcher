package com.mishkat.launcher.radio

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.mishkat.launcher.core.Logger
import com.mishkat.launcher.input.MishkatAction
import com.mishkat.launcher.input.RemoteUnificationLayer
import com.mishkat.launcher.system.SleepTimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Immutable
data class RadioUiState(
    val currentStation: StationEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorFallbackActive: Boolean = false,
)

/**
 * §9 + R12 + R13:
 * Bridges Compose UI, remote media keys, SleepTimer fade-out/stop, and [RadioPlaybackService].
 * Includes exponential retry/backoff (1.5s → 3s) and automatic fallback to the next station if a stream is dead.
 */
class RadioController(
    private val context: Context,
    private val stationRepository: StationRepository,
    private val sleepTimer: SleepTimerRepository,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(RadioUiState())
    val state: StateFlow<RadioUiState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var retryJob: Job? = null
    private var retryCount = 0

    fun start() {
        // Restore last selected station when stations list becomes ready.
        scope.launch {
            val savedId = runCatching { prefs.data.first()[KEY_LAST_STATION_ID] }.getOrNull()
            stationRepository.stations.collect { list ->
                if (list.isEmpty()) return@collect
                val current = _state.value.currentStation
                val updated = when {
                    current != null -> list.firstOrNull { it.id == current.id } ?: list.first()
                    savedId != null -> list.firstOrNull { it.id == savedId } ?: list.first()
                    else -> list.first()
                }
                _state.value = _state.value.copy(currentStation = updated)
            }
        }

        // Apply SleepTimer 5s fade-out volume and stop on expiry (R13).
        scope.launch {
            sleepTimer.state.collect { timer ->
                controller?.volume = if (timer.active) timer.volumeScale else 1f
            }
        }
        scope.launch {
            sleepTimer.expired.collect {
                controller?.let { c ->
                    c.pause()
                    c.volume = 1f
                }
            }
        }

        // Listen for remote media keys & numeric speed-dial (1..9 -> top 9 stations).
        scope.launch {
            RemoteUnificationLayer.actions.collect { action ->
                when (action) {
                    MishkatAction.PlayPause -> togglePlayPause()
                    MishkatAction.Next -> next()
                    MishkatAction.Previous -> previous()
                    is MishkatAction.ChannelNumber -> {
                        val idx = (action.digit - 1).coerceAtLeast(0)
                        val list = stationRepository.stations.value
                        if (action.digit in 1..9 && idx < list.size) {
                            playStation(list[idx])
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun playStation(station: StationEntity) {
        retryJob?.cancel()
        retryCount = 0
        _state.value = _state.value.copy(
            currentStation = station,
            isBuffering = true,
            errorFallbackActive = false,
        )
        stationRepository.markPlayed(station.id)
        scope.launch { prefs.edit { it[KEY_LAST_STATION_ID] = station.id } }

        withController { c ->
            c.volume = 1f
            val item = MediaItem.Builder()
                .setMediaId(station.id.toString())
                .setUri(station.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.nameAr)
                        .setSubtitle(station.nameEn)
                        .build(),
                )
                .build()
            c.setMediaItem(item)
            c.prepare()
            c.play()
            Logger.radio { "Playing station id=${station.id} (${station.nameEn})" }
        }
    }

    fun togglePlayPause() {
        val c = controller
        if (c != null && (c.isPlaying || c.playbackState == Player.STATE_BUFFERING)) {
            retryJob?.cancel()
            c.pause()
            return
        }
        val target = _state.value.currentStation ?: stationRepository.stations.value.firstOrNull() ?: return
        playStation(target)
    }

    fun next() = stepStation(+1)

    fun previous() = stepStation(-1)

    private fun stepStation(delta: Int) {
        val list = stationRepository.stations.value
        if (list.isEmpty()) return
        val curId = _state.value.currentStation?.id
        val curIdx = list.indexOfFirst { it.id == curId }.takeIf { it >= 0 } ?: 0
        val nextIdx = (curIdx + delta).mod(list.size)
        playStation(list[nextIdx])
    }

    private fun withController(block: (MediaController) -> Unit) {
        val existing = controller
        if (existing != null) {
            block(existing)
            return
        }
        val token = SessionToken(context, ComponentName(context, RadioPlaybackService::class.java))
        val future = controllerFuture ?: MediaController.Builder(context, token).buildAsync().also { f ->
            controllerFuture = f
            f.addListener(
                {
                    runCatching { f.get() }.onSuccess { mc ->
                        controller = mc
                        attachPlayerListener(mc)
                        block(mc)
                    }.onFailure { t ->
                        Logger.e(Logger.TAG_RADIO, t) { "Failed to connect MediaController" }
                        _state.value = _state.value.copy(isBuffering = false)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
        if (future.isDone) {
            runCatching { future.get() }.onSuccess(block)
        }
    }

    private fun attachPlayerListener(mc: MediaController) {
        mc.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) retryCount = 0
                _state.value = _state.value.copy(
                    isPlaying = isPlaying,
                    isBuffering = mc.playbackState == Player.STATE_BUFFERING,
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.value = _state.value.copy(
                    isPlaying = mc.isPlaying,
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                Logger.w(Logger.TAG_RADIO, error) { "Stream error (retry=$retryCount)" }
                handleStreamFailure()
            }
        })
    }

    private fun handleStreamFailure() {
        retryJob?.cancel()
        val station = _state.value.currentStation ?: return
        if (retryCount < MAX_RETRIES) {
            retryCount++
            val backoffMs = retryCount * 1_500L
            retryJob = scope.launch {
                delay(backoffMs)
                withController { c ->
                    c.prepare()
                    c.play()
                }
            }
        } else {
            // Fallback to the next available station (§9).
            retryCount = 0
            _state.value = _state.value.copy(errorFallbackActive = true)
            retryJob = scope.launch {
                delay(1_200L)
                next()
            }
        }
    }

    private companion object {
        const val MAX_RETRIES = 2
        val KEY_LAST_STATION_ID = intPreferencesKey("last_radio_station_id")
    }
}
