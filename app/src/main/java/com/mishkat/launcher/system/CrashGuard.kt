package com.mishkat.launcher.system

import android.content.Context

/**
 * R10: crash-loop detection (3 crashes within 60 s → Safe Mode).
 *
 * D-005: uses SharedPreferences (not DataStore) because the crash handler must persist synchronously
 * before the process dies. The read at startup is a tiny file and is the one documented exception to R5.
 */
class CrashGuard(context: Context) {
    private val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { record(System.currentTimeMillis()) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun isCrashLooping(now: Long = System.currentTimeMillis()): Boolean =
        CrashLoopPolicy.isLooping(read(), now)

    fun reset() {
        sp.edit().remove(KEY).apply()
    }

    private fun record(now: Long) {
        val kept = CrashLoopPolicy.prune(read() + now, now)
        // commit() on purpose: the process is about to die.
        sp.edit().putString(KEY, kept.joinToString(",")).commit()
    }

    private fun read(): List<Long> =
        sp.getString(KEY, null)?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()

    private companion object {
        const val PREFS = "mishkat_crash"
        const val KEY = "crash_times"
    }
}

/** Pure policy, unit-tested. */
object CrashLoopPolicy {
    const val WINDOW_MS = 60_000L
    const val THRESHOLD = 3
    private const val MAX_KEPT = 5

    fun isLooping(timestamps: List<Long>, now: Long): Boolean =
        timestamps.count { now - it in 0..WINDOW_MS } >= THRESHOLD

    fun prune(timestamps: List<Long>, now: Long): List<Long> =
        timestamps.filter { now - it in 0..WINDOW_MS }.takeLast(MAX_KEPT)
}
