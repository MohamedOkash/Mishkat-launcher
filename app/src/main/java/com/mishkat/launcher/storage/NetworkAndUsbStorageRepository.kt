package com.mishkat.launcher.storage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Environment
import android.os.StatFs
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mishkat.launcher.core.Logger
import jcifs.context.BaseContext
import jcifs.config.PropertyConfiguration
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Properties

class NetworkAndUsbStorageRepository(
    private val context: Context,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
    private val proxy: LocalStreamProxy,
) {
    private val _state = MutableStateFlow(StorageUiState())
    val state: StateFlow<StorageUiState> = _state.asStateFlow()

    private var settings = SmbFtpSettings()
    private val json = Json { ignoreUnknownKeys = true }

    fun start() {
        proxy.start()
        scope.launch(Dispatchers.IO) {
            loadSavedSettings()
            scanAllStorageDevices()
            registerUsbReceiver()
        }
    }

    private suspend fun loadSavedSettings() {
        val raw = prefs.data.first()[KEY_STORAGE_SETTINGS]
        if (!raw.isNullOrBlank()) {
            runCatching {
                settings = json.decodeFromString(SmbFtpSettings.serializer(), raw)
                proxy.settings = settings
            }
        }
    }

    suspend fun saveSettings(newSettings: SmbFtpSettings) {
        settings = newSettings
        proxy.settings = newSettings
        prefs.edit { it[KEY_STORAGE_SETTINGS] = json.encodeToString(SmbFtpSettings.serializer(), newSettings) }
        scanAllStorageDevices()
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addDataScheme("file")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                scope.launch(Dispatchers.IO) {
                    scanLocalUsbStorage()
                }
            }
        }
        runCatching { context.registerReceiver(receiver, filter) }
    }

    suspend fun scanAllStorageDevices() = withContext(Dispatchers.IO) {
        val devices = mutableListOf<StorageDevice>()

        // 1. Local USB / HDD
        devices.addAll(scanLocalUsbStorage())

        // 2. Discover router gateway IP
        val gatewayIp = detectRouterGatewayIp() ?: settings.smbHost.ifBlank { "192.168.1.1" }

        // 3. Probe SMB (port 445 / 139)
        val smbHost = settings.smbHost.ifBlank { gatewayIp }
        val smbOnline = probePort(smbHost, settings.smbPort) || probePort(smbHost, 139)
        devices.add(
            StorageDevice(
                id = "router_smb",
                titleAr = "فلاشة / هارد الراوتر (Samba SMB)",
                titleEn = "Router USB Drive (Samba SMB)",
                type = StorageSourceType.ROUTER_SMB,
                isConnected = smbOnline,
                host = smbHost,
                port = settings.smbPort,
                rootPath = if (settings.smbShare.isNotBlank()) "smb://$smbHost/${settings.smbShare}/" else "smb://$smbHost/",
            )
        )

        // 4. Probe FTP (port 21)
        val ftpHost = settings.ftpHost.ifBlank { gatewayIp }
        val ftpOnline = probePort(ftpHost, settings.ftpPort)
        devices.add(
            StorageDevice(
                id = "router_ftp",
                titleAr = "خادم ملفات الراوتر (FTP)",
                titleEn = "Router FTP Server",
                type = StorageSourceType.ROUTER_FTP,
                isConnected = ftpOnline,
                host = ftpHost,
                port = settings.ftpPort,
                rootPath = "/",
            )
        )

        _state.value = _state.value.copy(devices = devices)
    }

    private fun scanLocalUsbStorage(): List<StorageDevice> {
        val list = mutableListOf<StorageDevice>()

        // 1. Check external removable files directories provided by Android Context
        val extDirs = context.getExternalFilesDirs(null)
        for (f in extDirs) {
            if (f != null && Environment.isExternalStorageRemovable(f)) {
                val root = getRootOfRemovableStorage(f)
                val stat = runCatching { StatFs(root.path) }.getOrNull()
                val totalGb = (stat?.totalBytes ?: 0L) / (1024.0 * 1024.0 * 1024.0)
                val freeGb = (stat?.availableBytes ?: 0L) / (1024.0 * 1024.0 * 1024.0)
                if (list.none { it.rootPath == root.absolutePath }) {
                    list.add(
                        StorageDevice(
                            id = "usb_${root.name}",
                            titleAr = "فلاشة / هارد خارجي (${root.name})",
                            titleEn = "USB / External Drive (${root.name})",
                            type = StorageSourceType.LOCAL_USB,
                            isConnected = true,
                            rootPath = root.absolutePath,
                            totalSpaceGb = totalGb,
                            freeSpaceGb = freeGb,
                        )
                    )
                }
            }
        }

        // 2. Direct inspection of /storage for mounted FAT/exFAT/NTFS drives on Android TV
        runCatching {
            val storageRoot = File("/storage")
            if (storageRoot.exists() && storageRoot.canRead()) {
                storageRoot.listFiles()?.forEach { dir ->
                    if (dir.isDirectory && !dir.name.equals("emulated", ignoreCase = true) && !dir.name.equals("self", ignoreCase = true)) {
                        val stat = runCatching { StatFs(dir.path) }.getOrNull()
                        if (stat != null && stat.totalBytes > 0) {
                            val totalGb = stat.totalBytes / (1024.0 * 1024.0 * 1024.0)
                            val freeGb = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
                            if (list.none { it.rootPath == dir.absolutePath }) {
                                list.add(
                                    StorageDevice(
                                        id = "usb_${dir.name}",
                                        titleAr = "فلاشة / هارد خارجي (${dir.name})",
                                        titleEn = "USB Drive (${dir.name})",
                                        type = StorageSourceType.LOCAL_USB,
                                        isConnected = true,
                                        rootPath = dir.absolutePath,
                                        totalSpaceGb = totalGb,
                                        freeSpaceGb = freeGb,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Always include primary internal storage as accessible fallback
        val primary = Environment.getExternalStorageDirectory()
        val stat = runCatching { StatFs(primary.path) }.getOrNull()
        val totalGb = (stat?.totalBytes ?: 0L) / (1024.0 * 1024.0 * 1024.0)
        val freeGb = (stat?.availableBytes ?: 0L) / (1024.0 * 1024.0 * 1024.0)
        list.add(
            StorageDevice(
                id = "local_storage",
                titleAr = if (list.isEmpty()) "الذاكرة الداخلية للشاشة" else "الذاكرة الداخلية",
                titleEn = "Internal Storage",
                type = StorageSourceType.LOCAL_USB,
                isConnected = true,
                rootPath = primary.absolutePath,
                totalSpaceGb = totalGb,
                freeSpaceGb = freeGb,
            )
        )

        return list
    }

    private fun getRootOfRemovableStorage(file: File): File {
        var current = file
        while (current.parentFile != null && current.parentFile?.path != "/storage") {
            current = current.parentFile ?: break
        }
        return current
    }

    private fun detectRouterGatewayIp(): String? {
        // 1. Try ConnectivityManager / LinkProperties route gateway (safe on Android 10+ without special permissions)
        val routeGw = runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val linkProps = activeNetwork?.let { cm.getLinkProperties(it) }
            val route = linkProps?.routes?.firstOrNull { it.isDefaultRoute && it.gateway != null }
            val gw = route?.gateway?.hostAddress
            if (!gw.isNullOrBlank() && gw != "0.0.0.0") gw else null
        }.getOrNull()
        if (!routeGw.isNullOrBlank()) return routeGw

        // 2. Fallback to WifiManager dhcpInfo safely wrapped in runCatching
        val wifiGw = runCatching {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcp = wifi?.dhcpInfo ?: return@runCatching null
            val gw = dhcp.gateway
            if (gw == 0) return@runCatching null
            "${gw and 0xFF}.${gw shr 8 and 0xFF}.${gw shr 16 and 0xFF}.${gw shr 24 and 0xFF}"
        }.getOrNull()
        if (!wifiGw.isNullOrBlank()) return wifiGw

        // 3. Probing common home router gateways
        for (candidate in listOf("192.168.1.1", "192.168.0.1", "192.168.100.1")) {
            if (probePort(candidate, 445) || probePort(candidate, 139) || probePort(candidate, 21) || probePort(candidate, 80)) {
                return candidate
            }
        }
        return null
    }

    private fun probePort(host: String, port: Int): Boolean {
        if (host.isBlank() || port <= 0) return false
        return runCatching {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), 450)
                true
            }
        }.getOrDefault(false)
    }

    fun getSettings(): SmbFtpSettings = settings

    fun selectTab(tab: StorageSourceType) {
        val device = _state.value.devices.firstOrNull { it.type == tab }
        val root = device?.rootPath ?: "/"
        _state.value = _state.value.copy(
            activeTab = tab,
            currentPath = root,
            breadcrumbs = listOf(root),
            errorMessage = null
        )
        loadDirectory(tab, root)
    }

    fun openFolder(path: String) {
        val newCrumbs = _state.value.breadcrumbs + path
        _state.value = _state.value.copy(
            currentPath = path,
            breadcrumbs = newCrumbs,
            errorMessage = null
        )
        loadDirectory(_state.value.activeTab, path)
    }

    fun navigateUp(): Boolean {
        val crumbs = _state.value.breadcrumbs
        if (crumbs.size <= 1) return false
        val newCrumbs = crumbs.dropLast(1)
        val parentPath = newCrumbs.last()
        _state.value = _state.value.copy(
            currentPath = parentPath,
            breadcrumbs = newCrumbs,
            errorMessage = null
        )
        loadDirectory(_state.value.activeTab, parentPath)
        return true
    }

    fun loadDirectory(type: StorageSourceType, path: String) {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        scope.launch(Dispatchers.IO) {
            when (type) {
                StorageSourceType.LOCAL_USB -> loadLocalDirectory(path)
                StorageSourceType.ROUTER_SMB -> loadSmbDirectory(path)
                StorageSourceType.ROUTER_FTP -> loadFtpDirectory(path)
            }
        }
    }

    private fun loadLocalDirectory(path: String) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            _state.value = _state.value.copy(isLoading = false, items = emptyList())
            return
        }
        val rawFiles = dir.listFiles() ?: emptyArray()
        val items = rawFiles.map { f ->
            val cat = if (f.isDirectory) FileCategory.FOLDER else categorizeFile(f.name)
            val streamUri = if (!f.isDirectory) proxy.getStreamUrl(StorageSourceType.LOCAL_USB, f.absolutePath, settings) else null
            StorageItem(
                name = f.name,
                path = f.absolutePath,
                isDirectory = f.isDirectory,
                sizeBytes = if (f.isDirectory) 0L else f.length(),
                lastModified = f.lastModified(),
                category = cat,
                streamUri = streamUri,
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        _state.value = _state.value.copy(isLoading = false, items = items)
    }

    private fun loadSmbDirectory(smbUrl: String) {
        runCatching {
            val prop = Properties().apply {
                setProperty("jcifs.smb.client.minVersion", "SMB1")
                setProperty("jcifs.smb.client.maxVersion", "SMB311")
                setProperty("jcifs.smb.client.ipcSigningEnforced", "false")
                setProperty("jcifs.smb.client.useExtendedSecurity", "true")
                setProperty("jcifs.smb.client.responseTimeout", "10000")
                setProperty("jcifs.smb.client.connTimeout", "6000")
            }
            val base = BaseContext(PropertyConfiguration(prop))
            val authContext = if (settings.smbIsAnonymous) {
                runCatching { base.withGuestCrendentials() }.getOrElse {
                    runCatching { base.withAnonymousCredentials() }.getOrElse {
                        base.withCredentials(NtlmPasswordAuthenticator("", "guest", ""))
                    }
                }
            } else {
                base.withCredentials(NtlmPasswordAuthenticator(settings.smbDomain, settings.smbUser, settings.smbPass))
            }
            val normalizedUrl = if (!smbUrl.endsWith("/")) "$smbUrl/" else smbUrl
            val smbDir = SmbFile(normalizedUrl, authContext)
            val list = smbDir.listFiles() ?: emptyArray()

            val items = list.map { f ->
                val isDir = f.isDirectory
                val name = f.name.removeSuffix("/")
                val cat = if (isDir) FileCategory.FOLDER else categorizeFile(name)
                val streamUri = if (!isDir) proxy.getStreamUrl(StorageSourceType.ROUTER_SMB, f.url.toString(), settings) else null
                StorageItem(
                    name = name,
                    path = f.url.toString(),
                    isDirectory = isDir,
                    sizeBytes = if (isDir) 0L else runCatching { f.length() }.getOrDefault(0L),
                    lastModified = runCatching { f.lastModified() }.getOrDefault(0L),
                    category = cat,
                    streamUri = streamUri,
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

            _state.value = _state.value.copy(isLoading = false, items = items, errorMessage = null)
        }.onFailure { err ->
            _state.value = _state.value.copy(
                isLoading = false,
                items = emptyList(),
                errorMessage = "تعذر الاتصال بـ Samba الراوتر (${err.localizedMessage ?: "تأكد من تفعيل مشاركة الملفات في الراوتر أو إدخال بيانات الدخول من زر ⚙️ الإعدادات"})"
            )
        }
    }

    private fun loadFtpDirectory(path: String) {
        runCatching {
            val ftp = FTPClient()
            val host = settings.ftpHost.ifBlank { detectRouterGatewayIp() ?: "192.168.1.1" }
            ftp.connect(host, settings.ftpPort)
            val user = if (settings.ftpIsAnonymous) "anonymous" else settings.ftpUser
            val pass = if (settings.ftpIsAnonymous) "" else settings.ftpPass
            val loggedIn = ftp.login(user, pass)
            if (!loggedIn) {
                ftp.disconnect()
                ftp.connect(host, settings.ftpPort)
                ftp.login("guest", "")
            }
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            ftp.controlEncoding = "UTF-8"
            ftp.connectTimeout = 10000
            ftp.soTimeout = 10000

            val targetPath = if (path.isBlank()) "/" else path
            val files = ftp.listFiles(targetPath) ?: emptyArray()
            val items = files.map { f ->
                val isDir = f.isDirectory
                val name = f.name
                val cat = if (isDir) FileCategory.FOLDER else categorizeFile(name)
                val fullPath = if (targetPath.endsWith("/")) "$targetPath$name" else "$targetPath/$name"
                val streamUri = if (!isDir) proxy.getStreamUrl(StorageSourceType.ROUTER_FTP, "ftp://$host:$targetPath/$name", settings) else null
                StorageItem(
                    name = name,
                    path = fullPath,
                    isDirectory = isDir,
                    sizeBytes = if (isDir) 0L else f.size,
                    lastModified = f.timestamp?.timeInMillis ?: 0L,
                    category = cat,
                    streamUri = streamUri,
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

            ftp.disconnect()
            _state.value = _state.value.copy(isLoading = false, items = items, errorMessage = null)
        }.onFailure { err ->
            _state.value = _state.value.copy(
                isLoading = false,
                items = emptyList(),
                errorMessage = "تعذر الاتصال بخادم FTP: ${err.localizedMessage ?: "تأكد من فتح منفذ 21 بالراوتر أو ضبط الإعدادات"}"
            )
        }
    }

    fun playMediaItem(item: StorageItem?) {
        _state.value = _state.value.copy(activeMediaItem = item)
    }

    private fun categorizeFile(name: String): FileCategory {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "mkv", "avi", "mov", "ts", "m2ts", "webm", "flv" -> FileCategory.VIDEO
            "mp3", "m4a", "aac", "flac", "wav", "ogg", "wma" -> FileCategory.AUDIO
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> FileCategory.IMAGE
            "apk" -> FileCategory.APK
            "pdf", "txt", "doc", "docx" -> FileCategory.DOCUMENT
            else -> FileCategory.OTHER
        }
    }

    companion object {
        private val KEY_STORAGE_SETTINGS = stringPreferencesKey("storage_network_settings")
    }
}
