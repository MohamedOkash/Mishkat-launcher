package com.mishkat.launcher.core

import android.app.Application
import com.mishkat.launcher.focus.FocusRestorer
import com.mishkat.launcher.home.AppsRepository
import com.mishkat.launcher.i18n.LocaleRepository
import com.mishkat.launcher.livetv.LiveTvRepository
import com.mishkat.launcher.prayer.PrayerRepository
import com.mishkat.launcher.radio.MishkatDatabase
import com.mishkat.launcher.radio.RadioController
import com.mishkat.launcher.radio.StationRepository
import com.mishkat.launcher.remote.CompanionRemoteServer
import com.mishkat.launcher.sports.SportsRepository
import com.mishkat.launcher.system.CrashGuard
import com.mishkat.launcher.system.SleepTimerRepository
import com.mishkat.launcher.theme.ThemeRepository
import com.mishkat.launcher.tools.HonestCleaner
import com.mishkat.launcher.tv.TvInputsRepository
import com.mishkat.launcher.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual DI object graph (§1: no Hilt). One instance per process, owned by MishkatApp. */
class AppGraph(private val app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val prefs = app.mishkatPrefs
    val database = MishkatDatabase.get(app)

    val crashGuard = CrashGuard(app)
    val network = NetworkMonitor(app)
    val localeRepository = LocaleRepository(prefs, appScope)
    val themeRepository = ThemeRepository(app, prefs, appScope)
    val focusRestorer = FocusRestorer(prefs, appScope)
    val appsRepository = AppsRepository(app, prefs, appScope)
    val sleepTimerRepository = SleepTimerRepository(prefs, appScope)
    val stationRepository = StationRepository(app, database.stationDao(), appScope)
    val radioController = RadioController(app, stationRepository, sleepTimerRepository, prefs, appScope)
    val prayerRepository = PrayerRepository(app, prefs, sleepTimerRepository, appScope)
    val weatherRepository = WeatherRepository(prefs, prayerRepository, appScope)
    val sportsRepository = SportsRepository(app, prefs, appScope)
    val tvInputsRepository = TvInputsRepository(app, prefs, appScope)
    val backgroundProcessKiller = com.mishkat.launcher.tools.BackgroundProcessKiller(app, prefs, appScope)
    val liveTvRepository = LiveTvRepository(app, prefs, appScope)
    val localStreamProxy = com.mishkat.launcher.storage.LocalStreamProxy(appScope)
    val storageRepository = com.mishkat.launcher.storage.NetworkAndUsbStorageRepository(app, prefs, appScope, localStreamProxy)
    val updateRepository = com.mishkat.launcher.update.UpdateRepository(app, appScope)
    val companionRemoteServer = CompanionRemoteServer(appScope)
    val autoWallpaperManager = com.mishkat.launcher.wallpaper.AutoWallpaperManager(prefs, localeRepository, appScope)

    fun start() {
        crashGuard.install()
        localeRepository.start()
        network.start()
        backgroundProcessKiller.start()
        themeRepository.start()
        focusRestorer.start()
        appsRepository.start()
        sleepTimerRepository.start()
        stationRepository.start()
        radioController.start()
        prayerRepository.start()
        weatherRepository.start()
        sportsRepository.start()
        tvInputsRepository.start()
        storageRepository.start()
        autoWallpaperManager.start()
        companionRemoteServer.infoProvider = {
            val liveCh = liveTvRepository.state.value.currentChannel?.nameAr ?: ""
            val st = radioController.state.value.currentStation?.nameAr ?: ""
            val appsList = (appsRepository.state.value as? com.mishkat.launcher.home.AppsState.Ready)?.visibleApps.orEmpty()
            val channelsList = liveTvRepository.state.value.channels
            val appsJson = appsList.take(35).joinToString(",") { """{"pkg":"${it.packageName}","name":"${it.label.replace("\"", "\\\"")}"}""" }
            val channelsJson = channelsList.take(45).joinToString(",") { """{"id":"${it.id}","name":"${it.nameAr.replace("\"", "\\\"")}","sat":"${it.satellite}"}""" }
            """{"status":"online","tvName":"Mishkat Smart TV","currentChannel":"$liveCh","currentStation":"$st","appsCount":${appsList.size},"channelsCount":${channelsList.size},"version":"2.0.0","apps":[$appsJson],"channels":[$channelsJson]}"""
        }
        companionRemoteServer.boostHandler = {
            val res = backgroundProcessKiller.sweepNow()
            """{"ok":true,"killedCount":${res.killedCount},"freedMb":${String.format(java.util.Locale.US, "%.1f", res.freedMb)}}"""
        }
        companionRemoteServer.start()

        appScope.launch {
            companionRemoteServer.commands.collect { cmd ->
                when (cmd) {
                    is com.mishkat.launcher.remote.CompanionCommand.LaunchPackage -> {
                        val launchIntent = app.packageManager.getLeanbackLaunchIntentForPackage(cmd.packageName)
                            ?: app.packageManager.getLaunchIntentForPackage(cmd.packageName)
                        launchIntent?.let { intent ->
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { app.startActivity(intent) }
                        }
                    }
                    is com.mishkat.launcher.remote.CompanionCommand.TriggerBoost -> {
                        backgroundProcessKiller.sweepNow()
                    }
                    is com.mishkat.launcher.remote.CompanionCommand.ImportM3u -> {
                        if (cmd.payload.startsWith("http://") || cmd.payload.startsWith("https://")) {
                            liveTvRepository.importM3uUrl(cmd.payload)
                        } else {
                            liveTvRepository.importM3uPlaylist(cmd.payload)
                        }
                    }
                    else -> Unit
                }
            }
        }

        HonestCleaner.schedulePeriodicAutoClean(app)
    }
}
