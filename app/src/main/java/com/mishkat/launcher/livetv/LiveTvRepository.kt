package com.mishkat.launcher.livetv

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.datasource.DefaultHttpDataSource
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class LiveTvRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(LiveTvUiState())
    val state: StateFlow<LiveTvUiState> = _state.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var isDataSaverActive = true

    init {
        loadChannels()
    }

    private fun loadChannels() {
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = context.assets.open("channels_snapshot.json")
                    val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    json.decodeFromString<List<LiveChannel>>(text)
                }.getOrElse { err ->
                    Logger.system { "Failed to load channels_snapshot.json: ${err.message}" }
                    emptyList()
                }
            }
            _state.value = _state.value.copy(
                channels = list,
                currentChannel = list.firstOrNull(),
            )
            Logger.system { "Loaded ${list.size} Live TV Channels into LiveTvRepository" }
        }
    }

    fun getPlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing

        val selector = DefaultTrackSelector(context.applicationContext).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(if (isDataSaverActive) 960 else 1920, if (isDataSaverActive) 540 else 1080)
                .setMaxVideoBitrate(if (isDataSaverActive) 1_200_000 else Int.MAX_VALUE)
                .setPreferredAudioLanguage("ar")
                .build()
        }
        trackSelector = selector

        val player = ExoPlayer.Builder(context.applicationContext)
            .setTrackSelector(selector)
            .build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            _state.value = _state.value.copy(isBuffering = true, errorMessage = null)
                        }
                        Player.STATE_READY -> {
                            _state.value = _state.value.copy(isBuffering = false, isPlaying = true, errorMessage = null)
                        }
                        Player.STATE_ENDED -> {
                            _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
                        }
                        Player.STATE_IDLE -> Unit
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Logger.system { "Live TV ExoPlayer error: ${error.errorCodeName} / ${error.message}" }
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = error.localizedMessage ?: "خطأ في البث",
                    )
                }
            })
        }
        exoPlayer = player
        return player
    }

    fun playChannel(channel: LiveChannel) {
        val player = getPlayer()
        _state.value = _state.value.copy(
            currentChannel = channel,
            isBuffering = true,
            isPlaying = true,
            errorMessage = null,
        )
        val defaultHeaders = mutableMapOf<String, String>()
        if (channel.streamUrl.contains("rotana")) {
            defaultHeaders["Referer"] = "https://rotana.net/"
        } else if (channel.streamUrl.contains("spacetoon")) {
            defaultHeaders["Referer"] = "https://spacetoon.com"
        }
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 12; MiTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(defaultHeaders)
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(10000)
            .setAllowCrossProtocolRedirects(true)

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(channel.streamUrl))

        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()
        Logger.system { "Playing Live Channel: ${channel.nameAr} (${channel.streamUrl})" }
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
    }

    fun nextChannel() {
        val list = _state.value.filteredChannels.ifEmpty { _state.value.channels }
        if (list.isEmpty()) return
        val current = _state.value.currentChannel
        val idx = list.indexOfFirst { it.id == current?.id }
        val next = if (idx >= 0) list[(idx + 1) % list.size] else list.first()
        playChannel(next)
    }

    fun previousChannel() {
        val list = _state.value.filteredChannels.ifEmpty { _state.value.channels }
        if (list.isEmpty()) return
        val current = _state.value.currentChannel
        val idx = list.indexOfFirst { it.id == current?.id }
        val prev = if (idx > 0) list[idx - 1] else list.last()
        playChannel(prev)
    }

    fun selectCategory(category: ChannelCategory) {
        _state.value = _state.value.copy(activeCategory = category)
    }

    fun setDataSaver(enabled: Boolean) {
        isDataSaverActive = enabled
        trackSelector?.let { sel ->
            sel.parameters = sel.buildUponParameters()
                .setMaxVideoSize(if (enabled) 960 else 1920, if (enabled) 540 else 1080)
                .setMaxVideoBitrate(if (enabled) 1_200_000 else Int.MAX_VALUE)
                .build()
        }
        Logger.system { "ExoPlayer Data Saver updated: $enabled" }
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
