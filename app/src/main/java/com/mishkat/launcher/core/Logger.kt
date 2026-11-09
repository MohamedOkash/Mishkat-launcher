package com.mishkat.launcher.core

import android.util.Log
import com.mishkat.launcher.BuildConfig

/**
 * R9: single logger. Tag format "[Mishkat <Area>]". Disabled in release builds.
 * Message builders are inline lambdas: when logging is disabled nothing is allocated (R6 hot paths).
 * Never use println in this codebase.
 */
object Logger {
    const val TAG_INPUT = "[Mishkat Input]"
    const val TAG_FOCUS = "[Mishkat Focus]"
    const val TAG_THEME = "[Mishkat Theme]"
    const val TAG_HOME = "[Mishkat Home]"
    const val TAG_RADIO = "[Mishkat Radio]"
    const val TAG_SYSTEM = "[Mishkat System]"

    @JvmField
    @Volatile
    var enabled: Boolean = BuildConfig.DEBUG

    inline fun input(msg: () -> String) = d(TAG_INPUT, msg)
    inline fun focus(msg: () -> String) = d(TAG_FOCUS, msg)
    inline fun theme(msg: () -> String) = d(TAG_THEME, msg)
    inline fun home(msg: () -> String) = d(TAG_HOME, msg)
    inline fun radio(msg: () -> String) = d(TAG_RADIO, msg)
    inline fun system(msg: () -> String) = d(TAG_SYSTEM, msg)

    inline fun d(tag: String, msg: () -> String) {
        if (enabled) Log.d(tag, msg())
    }

    inline fun w(tag: String, t: Throwable? = null, msg: () -> String) {
        if (enabled) Log.w(tag, msg(), t)
    }

    inline fun e(tag: String, t: Throwable? = null, msg: () -> String) {
        if (enabled) Log.e(tag, msg(), t)
    }
}
