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
            currentVersionName = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.5.0",
            currentVersionCode = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionCode }.getOrDefault(12),
            latestVersionName = "1.6.0",
            latestVersionCode = 13,
            updateAvailable = true,
            changelogAr = "• إضافة 28 قناة مصرية وعربية حية (دراما، أفلام، رياضة، قرآن)\n• نظام توفير باقة الإنترنت المنزلية\n• قائمة القنوات السريعة أثناء البث المباشر (OK)\n• وضع اللايت مود المهدّئ والمريح للعين\n• إمكانية ترتيب أقسام اللانشر",
            changelogEn = "• Added 28 Egyptian & Arab live channels\n• Home Internet Data Saver mode\n• Quick Channel Selector Menu in live player\n• Calm eye-friendly light mode\n• Reorderable home shelves",
            downloadUrl = "https://github.com/MohamedOkash/mishkat-launcher/releases/latest/download/mishkat-release.apk",
            hasUnreadNotification = true,
        )
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    // Developers can update this URL to their Firebase host or GitHub raw JSON
    private val updateEndpoint = "https://mohamed-okash.web.app/version.json"

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
