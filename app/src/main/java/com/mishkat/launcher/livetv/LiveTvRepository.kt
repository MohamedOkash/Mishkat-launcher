package com.mishkat.launcher.livetv

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class LiveTvRepository(
    private val context: Context,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(LiveTvUiState())
    val state: StateFlow<LiveTvUiState> = _state.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var isDataSaverActive = true

    companion object {
        val KEY_LAST_CHANNEL_ID = stringPreferencesKey("livetv_last_played_channel_id")
        private const val GITHUB_CHANNELS_URL = "https://raw.githubusercontent.com/MohamedOkash/Mishkat-launcher/main/shared/channels.json"
    }

    init {
        loadChannels()
    }

    private fun loadChannels() {
        scope.launch {
            val cacheFile = File(context.filesDir, "channels_cloud_cache.json")
            
            // 1. Instant local load (cache if available, otherwise built-in asset snapshot)
            val streamList = withContext(Dispatchers.IO) {
                val cachedContent = if (cacheFile.exists()) {
                    runCatching { cacheFile.readText(Charsets.UTF_8) }.getOrNull()
                } else null

                val content = cachedContent ?: runCatching {
                    context.assets.open("channels_snapshot.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
                }.getOrNull()

                if (content != null) {
                    runCatching { json.decodeFromString<List<LiveChannel>>(content) }.getOrElse { emptyList() }
                } else {
                    emptyList()
                }
            }

            // 2. Discover internal tuner channels on Smart TVs
            val tunerList = withContext(Dispatchers.IO) {
                InternalTunerManager.queryTunerChannels(context)
            }

            val combined = (tunerList + streamList).distinctBy { it.id }

            val savedLastId = runCatching { prefs.data.first()[KEY_LAST_CHANNEL_ID] }.getOrNull()
            val initialChannel = combined.firstOrNull { it.id == savedLastId } ?: combined.firstOrNull()

            if (combined.isNotEmpty()) {
                _state.value = _state.value.copy(
                    channels = combined,
                    currentChannel = initialChannel,
                    lastPlayedChannelId = savedLastId,
                )
                Logger.system { "Loaded ${combined.size} Live TV Channels into LiveTvRepository (${tunerList.size} tuner, ${streamList.size} streams)" }
            }

            // 3. Asynchronous cloud sync check
            syncCloudInternal(cacheFile, tunerList)
        }
    }

    private suspend fun syncCloudInternal(cacheFile: File, tunerList: List<LiveChannel>) = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(GITHUB_CHANNELS_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MishkatLauncher-AndroidTV")
            }
            if (conn.responseCode == 200) {
                val remoteText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val remoteList = json.decodeFromString<List<LiveChannel>>(remoteText)
                if (remoteList.isNotEmpty()) {
                    cacheFile.writeText(remoteText, Charsets.UTF_8)
                    val newCombined = (tunerList + remoteList).distinctBy { it.id }
                    _state.value = _state.value.copy(
                        channels = newCombined,
                        isSyncingCloud = false,
                    )
                    Logger.system { "Auto-synced ${remoteList.size} channels from GitHub Cloud" }
                }
            }
            conn.disconnect()
        }.onFailure { err ->
            _state.value = _state.value.copy(isSyncingCloud = false)
            Logger.system { "Channels cloud sync check: ${err.message}" }
        }
    }

    fun syncFromCloudNow() {
        scope.launch {
            _state.value = _state.value.copy(isSyncingCloud = true)
            val cacheFile = File(context.filesDir, "channels_cloud_cache.json")
            val tunerList = withContext(Dispatchers.IO) { InternalTunerManager.queryTunerChannels(context) }
            syncCloudInternal(cacheFile, tunerList)
            _state.value = _state.value.copy(isSyncingCloud = false)
        }
    }

    fun importM3uPlaylist(content: String) {
        scope.launch(Dispatchers.Default) {
            val lines = content.lines()
            val imported = mutableListOf<LiveChannel>()
            var currentName = ""
            var currentCategory = "all"
            var currentLogo = "📺"

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                    val titleMatch = trimmed.substringAfterLast(",", "")
                    currentName = if (titleMatch.isNotBlank()) titleMatch else "قناة مخصصة"
                    if (trimmed.contains("group-title=\"", ignoreCase = true)) {
                        val grp = trimmed.substringAfter("group-title=\"").substringBefore("\"")
                        currentCategory = when {
                            grp.contains("quran", true) || grp.contains("islam", true) -> "islamic_quran"
                            grp.contains("kid", true) -> "kids_education"
                            grp.contains("movie", true) || grp.contains("cinema", true) -> "cinema_movies"
                            grp.contains("series", true) || grp.contains("drama", true) -> "drama_series"
                            grp.contains("sport", true) -> "sports_cooking"
                            grp.contains("news", true) -> "news_doc"
                            else -> "egypt_general"
                        }
                    }
                } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    val id = "m3u_${trimmed.hashCode()}"
                    imported.add(
                        LiveChannel(
                            id = id,
                            nameAr = currentName.ifBlank { "قناة مستوردة" },
                            nameEn = currentName.ifBlank { "Imported Channel" },
                            category = currentCategory,
                            subtitleAr = "قائمة M3U خارجية",
                            streamUrl = trimmed,
                            glyph = currentLogo,
                            badge = "M3U",
                            satellite = "nilesat_7w",
                            sourceType = "HLS_STREAM",
                        )
                    )
                    currentName = ""
                }
            }

            if (imported.isNotEmpty()) {
                val merged = (imported + _state.value.channels).distinctBy { it.id }
                _state.value = _state.value.copy(channels = merged)
                Logger.system { "Imported ${imported.size} channels from external M3U" }
            }
        }
    }

    fun importM3uUrl(urlStr: String) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 7000
                val content = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                importM3uPlaylist(content)
            }.onFailure { e ->
                Logger.w(Logger.TAG_SYSTEM, e) { "Failed to download M3U playlist from $urlStr" }
            }
        }
    }

    fun selectCategory(category: ChannelCategory) {
        _state.value = _state.value.copy(activeCategory = category)
    }

    fun selectSatellite(satellite: SatelliteSource) {
        _state.value = _state.value.copy(activeSatellite = satellite)
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun jumpToChannelByNumber(number: Int) {
        val list = _state.value.filteredChannels.ifEmpty { _state.value.channels }
        if (list.isEmpty()) return
        val targetIdx = (number - 1).coerceIn(0, list.size - 1)
        playChannel(list[targetIdx])
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
        scope.launch {
            prefs.edit { it[KEY_LAST_CHANNEL_ID] = channel.id }
        }

        if (channel.isTuner) {
            // Internal hardware tuner handles playback via TvView in the UI
            exoPlayer?.stop()
            _state.value = _state.value.copy(
                currentChannel = channel,
                lastPlayedChannelId = channel.id,
                isPlaying = true,
                isBuffering = false,
                errorMessage = null,
            )
            Logger.system { "Playing Internal Tuner Channel: ${channel.nameAr} (${channel.tunerChannelUri})" }
            return
        }

        val player = getPlayer()
        _state.value = _state.value.copy(
            currentChannel = channel,
            lastPlayedChannelId = channel.id,
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
