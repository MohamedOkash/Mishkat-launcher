package com.mishkat.launcher.radio

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mishkat.launcher.MainActivity
import com.mishkat.launcher.core.Logger

/**
 * §4 + R12: Foreground MediaSessionService (`foregroundServiceType="mediaPlayback"`).
 * Survives leaving the home launcher into other apps (YouTube, Settings, etc.).
 * - `handleAudioFocus = true`: pauses/ducks automatically when another app takes audio focus.
 * - `setHandleAudioBecomingNoisy(true)`: pauses if Bluetooth speaker/headset disconnects (R12).
 * - `setAllowCrossProtocolRedirects(true)`: required for Cairo Quran Radio & Saudi Quran Radio
 *   which redirect from `https://stream.radiojar.com/...` to `http://n0*.radiojar.com/...`.
 */
class RadioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Linux; Android 11; MishkatTV) ExoPlayerLib/1.5")
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(12_000)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(httpDataSourceFactory))
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionIntent)
            .build()

        Logger.radio { "RadioPlaybackService created (crossProtocolRedirects=true)" }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        Logger.radio { "RadioPlaybackService destroyed" }
        super.onDestroy()
    }
}
