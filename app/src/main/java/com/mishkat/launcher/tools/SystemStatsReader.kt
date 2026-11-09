package com.mishkat.launcher.tools

import android.content.Context
import android.os.Debug
import android.os.Environment
import android.os.StatFs
import androidx.compose.runtime.Immutable
import java.io.File
import java.util.Locale

@Immutable
data class SystemStats(
    val ramTotalBytes: Long = 0L,
    val ramAvailableBytes: Long = 0L,
    val storageTotalBytes: Long = 0L,
    val storageFreeBytes: Long = 0L,
    val socTempCelsius: Float? = null,
    val launcherPssMb: Int = 0,
    val appCacheBytes: Long = 0L,
) {
    val ramUsedBytes: Long
        get() = (ramTotalBytes - ramAvailableBytes).coerceAtLeast(0L)

    val ramUsedPercent: Int
        get() = if (ramTotalBytes > 0L) ((ramUsedBytes * 100L) / ramTotalBytes).toInt().coerceIn(0, 100) else 0

    val storageUsedBytes: Long
        get() = (storageTotalBytes - storageFreeBytes).coerceAtLeast(0L)

    val storageUsedPercent: Int
        get() = if (storageTotalBytes > 0L) ((storageUsedBytes * 100L) / storageTotalBytes).toInt().coerceIn(0, 100) else 0
}

/**
 * §4 + §9 + R24: Real kernel/filesystem readings (`/proc/meminfo`, `StatFs`, `/sys/class/thermal`, `Debug.getPss`).
 * Zero fabricated or random numbers.
 */
object SystemStatsReader {

    fun readSnapshot(context: Context): SystemStats {
        val memInfoText = runCatching { File("/proc/meminfo").readText() }.getOrDefault("")
        val (ramTotal, ramAvail) = parseMemInfo(memInfoText)

        val (storageTotal, storageFree) = runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.totalBytes to stat.availableBytes
        }.getOrDefault(0L to 0L)

        val socTemp = readThermalZones()
        val pssMb = runCatching { (Debug.getPss() / 1024L).toInt() }.getOrDefault(0)
        val cacheBytes = measureAppCacheBytes(context)

        return SystemStats(
            ramTotalBytes = ramTotal,
            ramAvailableBytes = ramAvail,
            storageTotalBytes = storageTotal,
            storageFreeBytes = storageFree,
            socTempCelsius = socTemp,
            launcherPssMb = pssMb,
            appCacheBytes = cacheBytes,
        )
    }

    /** Parses `MemTotal` and `MemAvailable` (or `MemFree + Cached` fallback) from `/proc/meminfo`. */
    internal fun parseMemInfo(text: String): Pair<Long, Long> {
        var totalKb = 0L
        var availKb = -1L
        var freeKb = 0L
        var cachedKb = 0L

        for (line in text.lineSequence()) {
            when {
                line.startsWith("MemTotal:") -> totalKb = extractKb(line)
                line.startsWith("MemAvailable:") -> availKb = extractKb(line)
                line.startsWith("MemFree:") -> freeKb = extractKb(line)
                line.startsWith("Cached:") -> cachedKb = extractKb(line)
            }
        }
        val effectiveAvailKb = if (availKb >= 0L) availKb else (freeKb + cachedKb)
        return (totalKb * 1024L) to (effectiveAvailKb.coerceAtMost(totalKb) * 1024L)
    }

    internal fun parseThermalRaw(rawText: String): Float? {
        val v = rawText.trim().toLongOrNull() ?: return null
        val c = if (v > 1_000L) v / 1_000f else v.toFloat()
        return if (c in 10f..115f) c else null
    }

    private fun readThermalZones(): Float? {
        val candidates = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
        )
        for (path in candidates) {
            val raw = runCatching { File(path).readText() }.getOrNull() ?: continue
            parseThermalRaw(raw)?.let { return it }
        }
        return null
    }

    fun measureAppCacheBytes(context: Context): Long {
        var sum = folderSize(context.cacheDir)
        context.codeCacheDir?.let { sum += folderSize(it) }
        context.externalCacheDir?.let { sum += folderSize(it) }
        return sum
    }

    fun folderSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var total = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            total += if (child.isDirectory) folderSize(child) else child.length()
        }
        return total
    }

    fun formatBytes(bytes: Long): String {
        val b = bytes.coerceAtLeast(0L)
        return when {
            b >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", b / 1_073_741_824.0)
            b >= 1_048_576L -> String.format(Locale.US, "%.2f MB", b / 1_048_576.0)
            b >= 1_024L -> String.format(Locale.US, "%.1f KB", b / 1_024.0)
            else -> "$b B"
        }
    }

    private fun extractKb(line: String): Long {
        val digits = line.filter { it.isDigit() }
        return digits.toLongOrNull() ?: 0L
    }
}
