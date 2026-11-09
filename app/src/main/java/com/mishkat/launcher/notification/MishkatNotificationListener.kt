package com.mishkat.launcher.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mishkat.launcher.core.Logger

class MishkatNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Logger.system { "MishkatNotificationListener connected successfully" }
        loadActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        processNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn ?: return
        NotificationRepository.removeNotification(sbn.key)
    }

    private fun loadActiveNotifications() {
        runCatching {
            val active = activeNotifications ?: return
            active.forEach { processNotification(it) }
        }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        // Skip ongoing persistent foreground service notifications (like our own radio player)
        if (sbn.packageName == packageName && sbn.isOngoing) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        if (title.isBlank() && text.isBlank()) return

        val pm = packageManager
        val appName = runCatching {
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(sbn.packageName)

        val appIcon = runCatching {
            pm.getApplicationIcon(sbn.packageName)
        }.getOrNull()

        val item = AppNotificationItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            timestampMillis = sbn.postTime,
            appIcon = appIcon,
        )

        NotificationRepository.addNotification(item)
    }
}
