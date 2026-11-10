package com.mishkat.launcher.tools

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mishkat.launcher.accessibility.MishkatAccessibilityService
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class KillResult(
    val killedCount: Int = 0,
    val freedBytes: Long = 0L,
    val killedPackages: List<String> = emptyList(),
) {
    val freedMb: Double
        get() = freedBytes / (1024.0 * 1024.0)
}

@Immutable
data class BackgroundKillerState(
    val isAutoGuardEnabled: Boolean = true,
    val isRunningSweep: Boolean = false,
    val lastResult: KillResult? = null,
    val totalFreedBytesEver: Long = 0L,
)

/**
 * Level 1: Background Bloatware & Process Killer.
 * - Targets heavy background apps & ad-heavy stock launchers (PatchWall, TV recommendations, idle background apps)
 * - Safe Whitelist prevents killing system essentials, input methods, or Mishkat itself
 * - Automatic sweep on Boot, Home resume, and when RAM exceeds 75%
 */
class BackgroundProcessKiller(
    private val context: Context,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(BackgroundKillerState())
    val state: StateFlow<BackgroundKillerState> = _state.asStateFlow()

    private val activityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }

    private val packageManager by lazy { context.packageManager }

    companion object {
        val KEY_AUTO_GUARD = booleanPreferencesKey("bg_killer_auto_guard")
        private const val RAM_SWEEP_THRESHOLD_PERCENT = 75

        // Essential packages that should never be killed
        private val WHITELIST_PREFIXES = setOf(
            "com.mishkat.launcher",
            "android",
            "com.android.systemui",
            "com.android.inputmethod",
            "com.google.android.inputmethod",
            "com.android.bluetooth",
            "com.google.android.tts",
            "com.svox.pico",
        )

        // Known heavy TV bloatware and ad recommendation services to aggressively target
        private val BLOATWARE_PACKAGES = setOf(
            "com.xiaomi.mitv.tvhome",
            "com.mitv.tvhome",
            "com.xiaomi.mitv.upgrade",
            "com.xiaomi.mitv.advertise",
            "com.xiaomi.mitv.shop",
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcherx",
            "com.google.android.tvrecommendations",
            "com.google.android.videos",
            "com.google.android.youtube.tvunplugged",
        )
    }

    fun start() {
        scope.launch {
            val savedPref = runCatching { prefs.data.first()[KEY_AUTO_GUARD] }.getOrNull() ?: true
            _state.value = _state.value.copy(isAutoGuardEnabled = savedPref)
            
            // Initial sweep on startup if guard is active
            if (savedPref) {
                sweep(reason = "Initial Startup Guard")
            }
        }
    }

    fun toggleAutoGuard(enabled: Boolean) {
        scope.launch {
            prefs.edit { p -> p[KEY_AUTO_GUARD] = enabled }
            _state.value = _state.value.copy(isAutoGuardEnabled = enabled)
            Logger.system { "Auto background guard toggled: $enabled" }
        }
    }

    suspend fun sweepNow(): KillResult = sweep(reason = "Manual User Trigger")

    fun onHomeResumed() {
        if (!_state.value.isAutoGuardEnabled) return
        scope.launch {
            // Check if RAM pressure is high or sweep on heavy app exit
            val stats = withContext(Dispatchers.IO) { SystemStatsReader.readSnapshot(context) }
            if (stats.ramUsedPercent >= RAM_SWEEP_THRESHOLD_PERCENT) {
                sweep(reason = "High RAM Pressure (${stats.ramUsedPercent}%) on Home Resume")
            } else {
                sweep(reason = "Home Resume Clean")
            }
        }
    }

    fun onBootCompleted() {
        scope.launch {
            if (_state.value.isAutoGuardEnabled) {
                sweep(reason = "Boot Receiver Sweep")
            }
        }
    }

    private suspend fun sweep(reason: String): KillResult = withContext(Dispatchers.IO) {
        if (_state.value.isRunningSweep) {
            return@withContext _state.value.lastResult ?: KillResult()
        }

        _state.value = _state.value.copy(isRunningSweep = true)
        val am = activityManager ?: run {
            _state.value = _state.value.copy(isRunningSweep = false)
            return@withContext KillResult()
        }

        val beforeStats = SystemStatsReader.readSnapshot(context)
        val killedList = mutableListOf<String>()

        try {
            // 1. Identify all running / installed candidate packages
            val installedApps = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getInstalledApplications(0)
                }
            }.getOrElse { emptyList() }

            val myPkg = context.packageName

            for (appInfo in installedApps) {
                val pkg = appInfo.packageName ?: continue
                if (pkg == myPkg) continue

                // Check if whitelisted
                val isWhitelisted = WHITELIST_PREFIXES.any { prefix -> pkg.startsWith(prefix, ignoreCase = true) }
                if (isWhitelisted) continue

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isBloat = BLOATWARE_PACKAGES.contains(pkg)

                // Kill if third-party application or known bloatware package
                if (!isSystem || isBloat) {
                    runCatching {
                        am.killBackgroundProcesses(pkg)
                        killedList.add(pkg)
                    }
                }
            }

            // 2. Deep force-stop hook for stubborn bloatware packages via accessibility if available
            for (bloatPkg in BLOATWARE_PACKAGES) {
                runCatching {
                    MishkatAccessibilityService.instance?.deepForceStopPackage(bloatPkg)
                }
            }

        } catch (e: Exception) {
            Logger.w(Logger.TAG_SYSTEM, e) { "Error during background process sweep" }
        }

        // Measure RAM freed
        val afterStats = SystemStatsReader.readSnapshot(context)
        val freedBytes = (afterStats.ramTotalBytes - afterStats.ramUsedBytes) - (beforeStats.ramTotalBytes - beforeStats.ramUsedBytes)
        val actualFreed = freedBytes.coerceAtLeast(0L)

        val result = KillResult(
            killedCount = killedList.size,
            freedBytes = actualFreed,
            killedPackages = killedList,
        )

        _state.value = _state.value.copy(
            isRunningSweep = false,
            lastResult = result,
            totalFreedBytesEver = _state.value.totalFreedBytesEver + actualFreed,
        )

        Logger.system { "Background sweep completed [$reason]: killed ${result.killedCount} processes, freed ${result.freedMb} MB" }
        return@withContext result
    }
}
