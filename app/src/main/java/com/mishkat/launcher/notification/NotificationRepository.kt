package com.mishkat.launcher.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationRepository {
    private val _notifications = MutableStateFlow<List<AppNotificationItem>>(emptyList())
    val notifications: StateFlow<List<AppNotificationItem>> = _notifications.asStateFlow()

    fun isPermissionGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun openPermissionSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun addNotification(item: AppNotificationItem) {
        val current = _notifications.value.toMutableList()
        val index = current.indexOfFirst { it.key == item.key }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        _notifications.value = current.take(50)
    }

    fun removeNotification(key: String) {
        _notifications.value = _notifications.value.filterNot { it.key == key }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }
}
