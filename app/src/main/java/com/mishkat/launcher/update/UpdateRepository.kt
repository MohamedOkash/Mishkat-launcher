package com.mishkat.launcher.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.mishkat.launcher.BuildConfig
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateUiState(
    val currentVersionName: String = "1.5.0",
    val currentVersionCode: Int = 12,
    val latestVersionName: String = "1.5.0",
    val latestVersionCode: Int = 12,
    val isChecking: Boolean = false,
    val updateAvailable: Boolean = false,
    val changelogAr: String = "",
    val changelogEn: String = "",
    val downloadUrl: String = "",
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null,
    val hasUnreadNotification: Boolean = true,
)

class UpdateRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(
        UpdateUiState(
            currentVersionName = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "2.0.0",
            currentVersionCode = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionCode }.getOrDefault(20),
            latestVersionName = "2.0.0",
            latestVersionCode = 20,
            updateAvailable = false,
            changelogAr = "الإصدار 2.0.0 هو أحدث إصدار رسمي من مشكاة لانشر بنمط Apple TV",
            changelogEn = "Version 2.0.0 is the latest official release of Mishkat TV Launcher",
            downloadUrl = "https://github.com/MohamedOkash/Mishkat-launcher/releases/latest/download/MishkatLauncher-Android-Latest.apk",
            hasUnreadNotification = false,
        )
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    // Official GitHub Raw repository OTA update endpoint
    private val updateEndpoint = "https://raw.githubusercontent.com/MohamedOkash/Mishkat-launcher/main/version.json"

    fun markNotificationsRead() {
        _state.value = _state.value.copy(hasUnreadNotification = false)
    }

    fun checkForUpdates(forceManual: Boolean = false) {
        scope.launch {
            _state.value = _state.value.copy(isChecking = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val conn = URL(updateEndpoint).openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.setRequestProperty("User-Agent", "MishkatLauncher-OTA")
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        val code = json.optInt("versionCode", _state.value.currentVersionCode)
                        val name = json.optString("versionName", _state.value.currentVersionName)
                        val url = json.optString("downloadUrl", "")
                        val logAr = json.optString("changelogAr", "")
                        val logEn = json.optString("changelogEn", "")
                        val available = code > _state.value.currentVersionCode
                        Triple(available, code, Triple(name, url, Pair(logAr, logEn)))
                    } else null
                }.getOrNull()
            }

            if (result != null) {
                val (available, code, extra) = result
                val (name, url, logs) = extra
                _state.value = _state.value.copy(
                    isChecking = false,
                    updateAvailable = available,
                    latestVersionCode = code,
                    latestVersionName = name,
                    downloadUrl = url,
                    changelogAr = logs.first,
                    changelogEn = logs.second,
                    hasUnreadNotification = available,
                )
                Logger.system { "OTA Update check success: available=$available, latest=$name ($code)" }
            } else {
                // If remote endpoint is not yet published, keep existing simulated update or mark checked
                _state.value = _state.value.copy(
                    isChecking = false,
                    errorMessage = if (forceManual) "أنت على أحدث إصدار متوفر حالياً" else null,
                )
            }
        }
    }

    fun startInstall() {
        val url = _state.value.downloadUrl
        if (url.isBlank()) return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
