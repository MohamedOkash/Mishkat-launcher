package com.mishkat.launcher.notification

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

@Immutable
data class AppNotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val appIcon: Drawable? = null,
)
