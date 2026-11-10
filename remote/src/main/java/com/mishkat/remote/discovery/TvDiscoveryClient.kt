package com.mishkat.remote.discovery

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

data class DiscoveredTv(
    val name: String,
    val ip: String,
    val port: Int = 8787,
    val version: String = "1.7.0",
    val lastSeenMs: Long = System.currentTimeMillis()
)

class TvDiscoveryClient(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val prefs = context.getSharedPreferences("mishkat_remote_prefs", Context.MODE_PRIVATE)

    private val _targetTv = MutableStateFlow<DiscoveredTv?>(loadSavedTv())
    val targetTv: StateFlow<DiscoveredTv?> = _targetTv.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var listenJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    init {
        startDiscovery()
    }

    private fun loadSavedTv(): DiscoveredTv? {
        val ip = prefs.getString("last_tv_ip", null) ?: return null
        val name = prefs.getString("last_tv_name", "Mishkat Smart TV") ?: "Mishkat Smart TV"
        val port = prefs.getInt("last_tv_port", 8787)
        return DiscoveredTv(name = name, ip = ip, port = port)
    }

    fun saveTv(tv: DiscoveredTv) {
        _targetTv.value = tv
        prefs.edit()
            .putString("last_tv_ip", tv.ip)
            .putString("last_tv_name", tv.name)
            .putInt("last_tv_port", tv.port)
            .apply()
    }

    fun setManualIp(ip: String) {
        val trimmed = ip.trim()
        if (trimmed.isNotBlank()) {
            val tv = DiscoveredTv(name = "Mishkat TV (يدوي)", ip = trimmed, port = 8787)
            saveTv(tv)
        }
    }

    fun startDiscovery() {
        if (listenJob?.isActive == true) return
        _isSearching.value = true

        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        runCatching {
            multicastLock = wifi?.createMulticastLock("MishkatUdpDiscovery")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        }

        listenJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(DISCOVERY_PORT))
                    soTimeout = 4000
                }
                val buffer = ByteArray(2048)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    try {
                        socket.receive(packet)
                        val data = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                        val json = JSONObject(data)
                        if (json.optString("service") == "mishkat_tv") {
                            val ip = json.optString("ip").ifBlank { packet.address.hostAddress ?: "" }
                            val name = json.optString("name", "Mishkat Smart TV")
                            val port = json.optInt("port", 8787)
                            val version = json.optString("version", "1.7.0")

                            if (ip.isNotBlank()) {
                                val discovered = DiscoveredTv(
                                    name = name,
                                    ip = ip,
                                    port = port,
                                    version = version,
                                    lastSeenMs = System.currentTimeMillis()
                                )
                                saveTv(discovered)
                            }
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        // Continue listening loop
                    } catch (e: Exception) {
                        delay(1000L)
                    }
                }
            } catch (_: Exception) {
            } finally {
                socket?.close()
                runCatching { multicastLock?.release() }
                _isSearching.value = false
            }
        }
    }

    fun stopDiscovery() {
        listenJob?.cancel()
        listenJob = null
        runCatching { multicastLock?.release() }
        _isSearching.value = false
    }

    companion object {
        const val DISCOVERY_PORT = 8788
    }
}
