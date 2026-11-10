package com.mishkat.launcher.prayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import com.mishkat.launcher.R
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Level 2: Real Human Adhan Audio Engine.
 * - Plays authentic human adhan audio using [MediaPlayer] with gradual volume ramp-up.
 * - Seamlessly caches selected Muezzin (Makkah, Abdulbasit, Alafasy, Cairo) in `filesDir/adhan_cache/`.
 * - Embedded standard MP3 in `R.raw.adhan_makkah` provides 100% offline out-of-the-box real human voice.
 * - Retains procedural Maqam Hijaz [AudioTrack] synthesizer as ultimate fail-safe.
 */
object AdhanSoundPlayer {

    private var playJob: Job? = null
    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    @Volatile
    private var activeTrack: AudioTrack? = null

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true || activeTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

    fun playRealAdhan(
        context: Context,
        scope: CoroutineScope,
        muezzin: MuezzinVoice = MuezzinVoice.MAKKAH,
        onCompletion: (() -> Unit)? = null,
    ) {
        stop()
        playJob = scope.launch(Dispatchers.IO) {
            val cacheDir = File(context.filesDir, "adhan_cache").apply { mkdirs() }
            val cacheFile = File(cacheDir, "${muezzin.id}.mp3")

            // If not cached and not default makkah, download in background for future offline use
            if (!cacheFile.exists() && muezzin.streamUrl.isNotBlank()) {
                downloadMuezzinAsync(muezzin, cacheFile)
            }

            // Decide playback source: cache file -> raw resource -> fallback procedural
            val started = runCatching {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    if (cacheFile.exists() && cacheFile.length() > 10_000) {
                        setDataSource(context, Uri.fromFile(cacheFile))
                    } else {
                        // Fallback to embedded real Makkah MP3
                        val afd = context.resources.openRawResourceFd(R.raw.adhan_makkah)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    }
                    prepare()
                    setVolume(0.15f, 0.15f) // start soft
                    setOnCompletionListener {
                        stop()
                        onCompletion?.invoke()
                    }
                    start()
                }
                mediaPlayer = mp

                // Volume ramp-up over 2.5 seconds
                launch(Dispatchers.Default) {
                    for (step in 1..25) {
                        if (!isActive || mediaPlayer == null) break
                        delay(100L)
                        val vol = 0.15f + (0.85f * (step / 25f))
                        runCatching { mediaPlayer?.setVolume(vol, vol) }
                    }
                }
                true
            }.getOrElse { err ->
                Logger.w(Logger.TAG_SYSTEM, err) { "MediaPlayer real adhan failed, falling back to AudioTrack" }
                false
            }

            if (!started) {
                playProceduralFallback(scope)
            }
        }
    }

    private fun downloadMuezzinAsync(muezzin: MuezzinVoice, targetFile: File) {
        runCatching {
            val url = URL(muezzin.streamUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 12000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.renameTo(targetFile)
                Logger.system { "Downloaded and cached Muezzin ${muezzin.name} (${targetFile.length()} bytes)" }
            }
            conn.disconnect()
        }
    }

    private fun playProceduralFallback(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            runCatching {
                val sampleRate = 22050
                val phrase = listOf(
                    293.66 to 420, 311.13 to 380, 369.99 to 550, 392.00 to 750,
                    0.0 to 180, 392.00 to 380, 440.00 to 520, 392.00 to 460,
                    369.99 to 520, 311.13 to 420, 293.66 to 900,
                )
                val totalMs = phrase.sumOf { it.second }
                val totalSamples = (sampleRate * totalMs) / 1000
                val pcm = ShortArray(totalSamples)

                var cursor = 0
                for ((freq, durMs) in phrase) {
                    val count = (sampleRate * durMs) / 1000
                    for (i in 0 until count) {
                        if (cursor >= totalSamples) break
                        if (freq <= 0.0) {
                            pcm[cursor++] = 0
                        } else {
                            val t = i.toDouble() / sampleRate
                            val progress = i.toDouble() / count
                            val attack = (progress / 0.08).coerceAtMost(1.0)
                            val decay = exp(-2.2 * progress)
                            val env = attack * decay
                            val wave = sin(2.0 * PI * freq * t) * 0.72 +
                                sin(2.0 * PI * (freq * 2.0) * t) * 0.20 +
                                sin(2.0 * PI * (freq * 1.5) * t) * 0.08
                            val sample = (wave * env * 18000.0).toInt().coerceIn(
                                Short.MIN_VALUE.toInt(),
                                Short.MAX_VALUE.toInt(),
                            )
                            pcm[cursor++] = sample.toShort()
                        }
                    }
                }

                if (!isActive) return@runCatching

                val byteCount = pcm.size * 2
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(byteCount)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                activeTrack = track
                track.write(pcm, 0, pcm.size)
                track.play()
            }
        }
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        runCatching {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        mediaPlayer = null

        runCatching {
            activeTrack?.let { t ->
                if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
                t.release()
            }
        }
        activeTrack = null
    }
}
