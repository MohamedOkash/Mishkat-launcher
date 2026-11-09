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
    val liveTvRepository = LiveTvRepository(app, appScope)
    val updateRepository = com.mishkat.launcher.update.UpdateRepository(app, appScope)
    val companionRemoteServer = CompanionRemoteServer(appScope)

    fun start() {
        crashGuard.install()
        localeRepository.start()
        network.start()
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
        companionRemoteServer.start()
        HonestCleaner.schedulePeriodicAutoClean(app)
    }
}
