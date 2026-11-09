package com.mishkat.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.mishkat.launcher.MainActivity

/**
 * Service to intercept TV Remote HOME key and monitor top-level window changes.
 * When the user presses HOME or when a stock TV launcher appears in the foreground,
 * this service brings MishkatLauncher cleanly to the front.
 */
class MishkatAccessibilityService : AccessibilityService() {

    private var lastHomeTriggerTime: Long = 0L

    companion object {
        private const val TAG = "MishkatAccessibility"
        private const val HOME_COOLDOWN_MS = 200L

        private val STOCK_LAUNCHER_PACKAGES = setOf(
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcherx",
            "com.xiaomi.mitv.tvhome",
            "com.mitv.tvhome",
            "com.android.launcher",
            "com.android.tv.launcher",
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        serviceInfo = info
        Log.i(TAG, "Mishkat Accessibility Service connected, key filtering enabled")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_GUIDE) {
            if (event.action == KeyEvent.ACTION_UP) {
                bringMishkatHome()
            }
            return true // Consume key event so system launcher never receives it
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg != packageName && pkg in STOCK_LAUNCHER_PACKAGES) {
                Log.d(TAG, "Detected stock launcher foreground: $pkg -> redirecting to Mishkat")
                bringMishkatHome()
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Mishkat Accessibility Service interrupted")
    }

    private fun bringMishkatHome() {
        val now = SystemClock.uptimeMillis()
        if (now - lastHomeTriggerTime < HOME_COOLDOWN_MS) return
        lastHomeTriggerTime = now

        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            pendingIntent.send()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send pending intent, using fallback startActivity", e)
            val fallback = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(fallback)
        }
    }
}
