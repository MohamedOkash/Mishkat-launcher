package com.mishkat.launcher.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class TvSourceCategory {
    RECEIVER_SATELLITE,
    RECEIVER_TERRESTRIAL,
    HDMI,
    AV_COMPOSITE,
    USB_STORAGE,
}

@Immutable
data class TvSourceItem(
    val id: String,
    val category: TvSourceCategory,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val glyph: String,
    val inputId: String? = null,
    val packageName: String? = null,
    val isConnected: Boolean = true,
    val isHardwareDetected: Boolean = false,
) {
    fun title(isArabic: Boolean): String = if (isArabic) titleAr else titleEn
    fun subtitle(isArabic: Boolean): String = if (isArabic) subtitleAr else subtitleEn
}

@Immutable
data class UsbDriveInfo(
    val id: String,
    val label: String,
    val path: String?,
)

@Immutable
data class TvInputsUiState(
    val sources: List<TvSourceItem> = emptyList(),
    val usbDrives: List<UsbDriveInfo> = emptyList(),
    val defaultSourceId: String = "dvb_s2",
    val autoStartLiveTvOnBoot: Boolean = false,
    val detectedLiveTvPackage: String? = null,
)

/**
 * S8: Built-in Satellite/Terrestrial Receiver (`DVB-S2 / DTV / ATV`), HDMI 1..4, AV & USB Flash Drive Hub.
 * Supports Android 7.0 (`minSdk = 24`) Smart TVs with integrated receivers as well as Android TV boxes.
 */
class TvInputsRepository(
    private val context: Context,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(TvInputsUiState())
    val state: StateFlow<TvInputsUiState> = _state.asStateFlow()

    private val tvInputManager: TvInputManager? by lazy {
        runCatching { context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager }.getOrNull()
    }

    fun start() {
        scope.launch {
            prefs.data.collect { p ->
                val defaultId = p[KEY_DEFAULT_SOURCE] ?: "dvb_s2"
                val autoBoot = p[KEY_AUTO_LIVE_TV_BOOT] ?: false
                _state.value = _state.value.copy(
                    defaultSourceId = defaultId,
                    autoStartLiveTvOnBoot = autoBoot,
                )
            }
        }
        refreshInputs()
        registerTvInputCallback()
    }

    fun refreshInputs() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                discoverAllSources()
            }
            _state.value = _state.value.copy(
                sources = result.first,
                usbDrives = result.second,
                detectedLiveTvPackage = result.third,
            )
        }
    }

    fun setDefaultSource(sourceId: String) {
        scope.launch {
            prefs.edit { it[KEY_DEFAULT_SOURCE] = sourceId }
        }
    }

    fun setAutoStartLiveTvOnBoot(enabled: Boolean) {
        scope.launch {
            prefs.edit { it[KEY_AUTO_LIVE_TV_BOOT] = enabled }
        }
    }

    /**
     * Launches the requested TV/Receiver source, HDMI port, or USB file manager.
     * Returns true if an activity was successfully started.
     */
    fun openSource(source: TvSourceItem): Boolean {
        setDefaultSource(source.id)

        if (source.category == TvSourceCategory.USB_STORAGE) {
            return openUsbStorage()
        }

        // 1) If backed by a real TvInputInfo hardware passthrough or tuner inputId:
        val inputId = source.inputId
        if (!inputId.isNullOrBlank()) {
            val uri = if (source.category == TvSourceCategory.RECEIVER_SATELLITE ||
                source.category == TvSourceCategory.RECEIVER_TERRESTRIAL
            ) {
                TvContract.buildChannelsUriForInput(inputId)
            } else {
                TvContract.buildChannelUriForPassthroughInput(inputId)
            }
            val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (tryStart(viewIntent)) return true
        }

        // 2) If a specific Live TV / Receiver package was detected on the TV:
        val pkg = source.packageName ?: _state.value.detectedLiveTvPackage
        if (!pkg.isNullOrBlank()) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                ?: context.packageManager.getLeanbackLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (tryStart(launch)) return true
            }
        }

        // 3) Standard Android TV live channels / TV input intents:
        val standardIntents = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("content://android.media.tv/channel")),
            Intent("android.media.tv.action.WATCH_NEXT"),
            Intent("com.android.tv.action.OPEN_INPUTS"),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in standardIntents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (tryStart(intent)) return true
        }
        return false
    }

    /** Quick-launches the user's default Receiver / Live TV input (e.g., when pressing TV key on remote). */
    fun launchDefaultLiveTv(): Boolean {
        val current = _state.value
        val target = current.sources.firstOrNull { it.id == current.defaultSourceId }
            ?: current.sources.firstOrNull()
            ?: return false
        return openSource(target)
    }

    fun openUsbStorage(): Boolean {
        // Check common TV file manager packages first, then standard storage intents.
        val fileManagerPackages = listOf(
            "com.Xiaomi.mitv.mediaexplorer",
            "com.xiaomi.mitv.mediaexplorer",
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.tv.filemanager",
            "com.mediatek.filemanager",
            "com.tcl.filemanager",
        )
        for (pkg in fileManagerPackages) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                ?: context.packageManager.getLeanbackLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (tryStart(intent)) return true
            }
        }
        val fallbackIntents = listOf(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            },
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
            Intent(Settings.ACTION_MEMORY_CARD_SETTINGS),
        )
        for (intent in fallbackIntents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (tryStart(intent)) return true
        }
        return false
    }

    private fun tryStart(intent: Intent): Boolean = runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    private fun discoverAllSources(): Triple<List<TvSourceItem>, List<UsbDriveInfo>, String?> {
        val pm = context.packageManager
        val detectedPkg = KNOWN_TV_RECEIVER_PACKAGES.firstOrNull { pkg ->
            runCatching {
                pm.getLaunchIntentForPackage(pkg) != null ||
                    pm.getLeanbackLaunchIntentForPackage(pkg) != null ||
                    pm.getPackageInfo(pkg, 0) != null
            }.getOrDefault(false)
        }

        val hwInputs = runCatching { tvInputManager?.tvInputList.orEmpty() }.getOrDefault(emptyList())
        val tunerInputs = hwInputs.filter { it.type == TvInputInfo.TYPE_TUNER }
        val hdmiInputs = hwInputs.filter { it.type == TvInputInfo.TYPE_HDMI }
        val avInputs = hwInputs.filter {
            it.type == TvInputInfo.TYPE_COMPOSITE || it.type == TvInputInfo.TYPE_COMPONENT
        }

        val list = ArrayList<TvSourceItem>(10)

        // 1) Satellite Receiver (DVB-S / DVB-S2)
        val satTuner = tunerInputs.firstOrNull()
        list.add(
            TvSourceItem(
                id = "dvb_s2",
                category = TvSourceCategory.RECEIVER_SATELLITE,
                titleAr = "الريسيفر الفضائي (DVB-S2)",
                titleEn = "Satellite Receiver (DVB-S2)",
                subtitleAr = if (satTuner != null || detectedPkg != null) {
                    "بث الفضائيات والنايل سات • متصل"
                } else {
                    "فتح تطبيق البث التلفزيوني / القنوات"
                },
                subtitleEn = if (satTuner != null || detectedPkg != null) {
                    "Built-in Satellite Tuner • Ready"
                } else {
                    "Open Live TV / Channels app"
                },
                glyph = "📡",
                inputId = satTuner?.id,
                packageName = satTuner?.serviceInfo?.packageName ?: detectedPkg,
                isConnected = true,
                isHardwareDetected = satTuner != null || detectedPkg != null,
            ),
        )

        // 2) Terrestrial / Digital TV (DTV / ATV)
        val dtvTuner = tunerInputs.getOrNull(1) ?: satTuner
        list.add(
            TvSourceItem(
                id = "dtv_antenna",
                category = TvSourceCategory.RECEIVER_TERRESTRIAL,
                titleAr = "البث الأرضي الرقمي (DTV/ATV)",
                titleEn = "Digital Terrestrial TV (DTV)",
                subtitleAr = "القنوات الأرضية الرقمية والمحلية",
                subtitleEn = "Local terrestrial antenna channels",
                glyph = "📺",
                inputId = dtvTuner?.id,
                packageName = dtvTuner?.serviceInfo?.packageName ?: detectedPkg,
                isConnected = true,
                isHardwareDetected = dtvTuner != null,
            ),
        )

        // 3) HDMI 1..3 (either real hardware TvInputInfo or standard HDMI port tiles)
        if (hdmiInputs.isNotEmpty()) {
            hdmiInputs.forEachIndexed { idx, info ->
                val portNum = idx + 1
                val state = runCatching { tvInputManager?.getInputState(info.id) }
                    .getOrNull() ?: TvInputManager.INPUT_STATE_CONNECTED
                val connected = state != TvInputManager.INPUT_STATE_DISCONNECTED
                val customLabel = runCatching { info.loadLabel(context)?.toString() }.getOrNull()
                list.add(
                    TvSourceItem(
                        id = "hdmi_$portNum",
                        category = TvSourceCategory.HDMI,
                        titleAr = customLabel?.takeIf { it.isNotBlank() } ?: "مدخل HDMI $portNum",
                        titleEn = customLabel?.takeIf { it.isNotBlank() } ?: "HDMI $portNum",
                        subtitleAr = if (connected) "إشارة نشطة • اضغط للتحويل" else "بانتظار توصيل جهاز",
                        subtitleEn = if (connected) "Signal active • Press OK" else "Standby / No cable",
                        glyph = "🔌",
                        inputId = info.id,
                        packageName = info.serviceInfo?.packageName ?: detectedPkg,
                        isConnected = connected,
                        isHardwareDetected = true,
                    ),
                )
            }
        } else {
            for (portNum in 1..3) {
                list.add(
                    TvSourceItem(
                        id = "hdmi_$portNum",
                        category = TvSourceCategory.HDMI,
                        titleAr = "مدخل HDMI $portNum",
                        titleEn = "HDMI $portNum",
                        subtitleAr = "ريسيفر خارجي / بلايستيشن / كمبيوتر",
                        subtitleEn = "External Box / Console / PC",
                        glyph = "🔌",
                        packageName = detectedPkg,
                        isConnected = true,
                        isHardwareDetected = false,
                    ),
                )
            }
        }

        // 4) AV / Composite Input
        val avInfo = avInputs.firstOrNull()
        list.add(
            TvSourceItem(
                id = "av_1",
                category = TvSourceCategory.AV_COMPOSITE,
                titleAr = "مدخل AV / Component",
                titleEn = "AV / Composite Input",
                subtitleAr = "الأجهزة الكلاسيكية ومدخل الألوان الثلاثة",
                subtitleEn = "Legacy RCA / Component devices",
                glyph = "🎛",
                inputId = avInfo?.id,
                packageName = avInfo?.serviceInfo?.packageName ?: detectedPkg,
                isConnected = true,
                isHardwareDetected = avInfo != null,
            ),
        )

        // 5) Connected USB Flash Drives
        val usbDrives = detectUsbDrives()
        if (usbDrives.isNotEmpty()) {
            usbDrives.forEachIndexed { idx, drive ->
                list.add(
                    TvSourceItem(
                        id = "usb_${idx + 1}",
                        category = TvSourceCategory.USB_STORAGE,
                        titleAr = "فلاشة USB: ${drive.label}",
                        titleEn = "USB Drive: ${drive.label}",
                        subtitleAr = "متصلة الآن • اضغط لفتح الملفات والميديا",
                        subtitleEn = "Connected • Press OK to browse media",
                        glyph = "💾",
                        isConnected = true,
                        isHardwareDetected = true,
                    ),
                )
            }
        } else {
            list.add(
                TvSourceItem(
                    id = "usb_slot",
                    category = TvSourceCategory.USB_STORAGE,
                    titleAr = "فلاشة USB والملفات",
                    titleEn = "USB Drive & Media",
                    subtitleAr = "تصفح الفلاشة وملفات الفيديو والصور",
                    subtitleEn = "Browse USB flash drives & local media",
                    glyph = "💾",
                    isConnected = false,
                    isHardwareDetected = false,
                ),
            )
        }

        return Triple(list, usbDrives, detectedPkg)
    }

    private fun detectUsbDrives(): List<UsbDriveInfo> {
        val drives = ArrayList<UsbDriveInfo>()
        runCatching {
            val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            sm?.storageVolumes?.forEach { vol ->
                if (vol.isRemovable) {
                    val desc = vol.getDescription(context) ?: "USB"
                    val dirPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        vol.directory?.absolutePath
                    } else {
                        null
                    }
                    drives.add(
                        UsbDriveInfo(
                            id = vol.uuid ?: desc,
                            label = desc,
                            path = dirPath,
                        ),
                    )
                }
            }
        }
        // Fallback for Android 7.0..9.0 TV boxes mounting under /storage/XXXX-XXXX or /mnt/media_rw
        if (drives.isEmpty()) {
            runCatching {
                val storageRoot = File("/storage")
                storageRoot.listFiles()?.forEach { f ->
                    val name = f.name
                    if (f.isDirectory && name != "emulated" && name != "self" && f.canRead()) {
                        drives.add(UsbDriveInfo(id = name, label = name, path = f.absolutePath))
                    }
                }
            }
        }
        return drives
    }

    private fun registerTvInputCallback() {
        runCatching {
            tvInputManager?.registerCallback(
                object : TvInputManager.TvInputCallback() {
                    override fun onInputAdded(inputId: String?) = refreshInputs()
                    override fun onInputRemoved(inputId: String?) = refreshInputs()
                    override fun onInputStateChanged(inputId: String?, state: Int) = refreshInputs()
                },
                android.os.Handler(android.os.Looper.getMainLooper()),
            )
        }
        runCatching {
            val usbFilter = IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addDataScheme("file")
            }
            context.registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        Logger.system { "USB storage event: ${intent?.action}" }
                        refreshInputs()
                    }
                },
                usbFilter,
            )
        }
    }

    companion object {
        val KEY_DEFAULT_SOURCE = stringPreferencesKey("tv_default_source_id")
        val KEY_AUTO_LIVE_TV_BOOT = booleanPreferencesKey("tv_auto_live_on_boot")

        /** Known Live TV / Satellite / DVB-S2 Receiver packages across Android 7+ Smart TVs. */
        val KNOWN_TV_RECEIVER_PACKAGES = listOf(
            "com.android.tv",
            "com.google.android.tv",
            "com.mediatek.wwtv.tvcenter",
            "com.tcl.tv",
            "com.hisense.tv",
            "com.skyworth.tv",
            "com.changhong.tv",
            "com.realtek.tv",
            "com.amlogic.tvinput",
            "org.dtvkit.inputsource",
            "com.cvte.tv.provision",
            "com.mstar.tv.tvplayer.ui",
            "com.konka.tv",
            "com.xiaomi.mitv.tvplayer",
        )
    }
}
