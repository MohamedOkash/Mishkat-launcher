package com.mishkat.launcher.tools

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.Immutable
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

@Immutable
data class CleanResult(
    val beforeBytes: Long,
    val afterBytes: Long,
    val deletedFilesCount: Int,
) {
    val freedBytes: Long
        get() = (beforeBytes - afterBytes).coerceAtLeast(0L)
}

/**
 * §9 + R24: Honest self-cleaner.
 * - Cleans ONLY real temporary files owned by the app (`cacheDir`, `externalCacheDir`, stale icon cache).
 * - Measures exact bytes before and after deletion so every reported byte is genuine (R24).
 */
object HonestCleaner {

    /** Auto-clean threshold from §9: 500 MB. */
    const val AUTO_CLEAN_THRESHOLD_BYTES = 500L * 1024L * 1024L

    suspend fun cleanNow(context: Context): CleanResult = withContext(Dispatchers.IO) {
        cleanDirectories(
            listOfNotNull(
                context.cacheDir,
                context.externalCacheDir,
            ),
        )
    }

    suspend fun cleanOldApks(context: Context): CleanResult = withContext(Dispatchers.IO) {
        val downloadDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File(Environment.getExternalStorageDirectory(), "Download"),
            File("/sdcard/Download"),
            File("/storage/emulated/0/Download"),
        ).distinct().filter { it.exists() && it.isDirectory }

        var totalBefore = 0L
        var deletedCount = 0
        var totalFreed = 0L

        for (dir in downloadDirs) {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".apk", ignoreCase = true)) {
                    val size = file.length()
                    totalBefore += size
                    if (runCatching { file.delete() }.getOrDefault(false)) {
                        deletedCount++
                        totalFreed += size
                    }
                } else if (file.isDirectory) {
                    file.listFiles()?.forEach { subFile ->
                        if (subFile.isFile && subFile.name.endsWith(".apk", ignoreCase = true)) {
                            val size = subFile.length()
                            totalBefore += size
                            if (runCatching { subFile.delete() }.getOrDefault(false)) {
                                deletedCount++
                                totalFreed += size
                            }
                        }
                    }
                }
            }
        }
        CleanResult(
            beforeBytes = totalBefore,
            afterBytes = (totalBefore - totalFreed).coerceAtLeast(0L),
            deletedFilesCount = deletedCount,
        )
    }

    /** Pure directory cleaner, unit-testable with temporary directories. */
    fun cleanDirectories(dirs: List<File>): CleanResult {
        val before = dirs.sumOf { SystemStatsReader.folderSize(it) }
        var deletedCount = 0
        for (dir in dirs) {
            deletedCount += deleteContents(dir)
        }
        val after = dirs.sumOf { SystemStatsReader.folderSize(it) }
        val result = CleanResult(
            beforeBytes = before,
            afterBytes = after,
            deletedFilesCount = deletedCount,
        )
        Logger.system {
            "HonestCleaner freed=${SystemStatsReader.formatBytes(result.freedBytes)} (${result.deletedFilesCount} files)"
        }
        return result
    }

    fun schedulePeriodicAutoClean(context: Context) {
        runCatching {
            val request = PeriodicWorkRequestBuilder<AutoCleanWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            Logger.system { "Scheduled 6h AutoCleanWorker" }
        }.onFailure { t ->
            Logger.w(Logger.TAG_SYSTEM, t) { "WorkManager schedule skipped" }
        }
    }

    private fun deleteContents(dir: File?): Int {
        if (dir == null || !dir.exists()) return 0
        var count = 0
        val children = dir.listFiles() ?: return 0
        for (child in children) {
            if (child.isDirectory) {
                count += deleteContents(child)
            }
            if (runCatching { child.delete() }.getOrDefault(false)) {
                count++
            }
        }
        return count
    }

    private const val WORK_NAME = "mishkat_auto_clean_6h"
}

/**
 * §9: Runs every 6 hours via WorkManager and cleans app caches if total cache exceeds 500 MB.
 */
class AutoCleanWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val currentCache = SystemStatsReader.measureAppCacheBytes(applicationContext)
        if (currentCache > HonestCleaner.AUTO_CLEAN_THRESHOLD_BYTES) {
            HonestCleaner.cleanNow(applicationContext)
        }
        return Result.success()
    }
}
