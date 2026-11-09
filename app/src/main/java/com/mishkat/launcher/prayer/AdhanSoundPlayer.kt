package com.mishkat.launcher.prayer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * S8: Pure offline synthesized Maqam Hijaz Takbeer / Adhan chime using Android's native [AudioTrack].
 * - Zero MP3 asset bloat (keeps APK under 3 MB).
 * - Works 100% offline on Android 7.0+ (`minSdk = 24`).
 * - Can be stopped immediately when the user dismisses `AdhanOverlay` or mutes audio Adhan.
 */
object AdhanSoundPlayer {

    private var playJob: Job? = null
    @Volatile
    private var activeTrack: AudioTrack? = null

    /**
     * Plays a gentle, reverent Maqam Hijaz melodic call (D4 - Eb4 - F#4 - G4 - A4 cadence)
     * on `USAGE_NOTIFICATION` / `USAGE_MEDIA` so the user hears the prayer alert clearly on TV speakers.
     */
    fun playAdhanChime(scope: CoroutineScope) {
        stop()
        playJob = scope.launch(Dispatchers.Default) {
            runCatching {
                val sampleRate = 22050
                // Maqam Hijaz notes (Hz) & durations (ms): reverent two-phrase Takbeer cadence
                val phrase = listOf(
                    293.66 to 420, // D4
                    311.13 to 380, // Eb4
                    369.99 to 550, // F#4
                    392.00 to 750, // G4
                    0.0 to 180,    // breath
                    392.00 to 380, // G4
                    440.00 to 520, // A4
                    392.00 to 460, // G4
                    369.99 to 520, // F#4
                    311.13 to 420, // Eb4
                    293.66 to 900, // D4
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
                            // Smooth attack & warm decay envelope
                            val attack = (progress / 0.08).coerceAtMost(1.0)
                            val decay = exp(-2.2 * progress)
                            val env = attack * decay
                            // Fundamental + soft octave + fifth overtone for warm bell/oud timbre
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
            activeTrack?.let { t ->
                if (t.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    t.stop()
                }
                t.release()
            }
        }
        activeTrack = null
    }
}
