package com.mishkat.launcher.storage

import com.mishkat.launcher.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.nio.charset.StandardCharsets

data class UpnpServerInfo(
    val friendlyName: String,
    val controlUrl: String,
    val locationUrl: String,
    val host: String,
    val port: Int,
)

object UpnpDlnaClient {

    private val COMMON_PROBE_PORTS = listOf(8200, 49152, 1900, 5000, 80, 2869)
    private val COMMON_DESC_PATHS = listOf(
        "/rootDesc.xml",
        "/description.xml",
        "/dms/description.xml",
        "/upnp/IGD.xml",
        "/dms.xml",
        "/miniupnpd.xml",
        "/etc/linuxigd/gatedesc.xml"
    )

    /**
     * Discovers a UPnP / DLNA media server on the local network.
     * Combines SSDP M-SEARCH broadcast with fast direct router probing.
     */
    suspend fun discover(routerHost: String): UpnpServerInfo? = withContext(Dispatchers.IO) {
        // 1. First probe router host directly (fastest on home networks where ZTE H188A is at 192.168.1.1)
        val directServer = probeHost(routerHost)
        if (directServer != null) {
            Logger.system { "Discovered UPnP Media Server directly: ${directServer.friendlyName} at ${directServer.controlUrl}" }
            return@withContext directServer
        }

        // 2. SSDP M-SEARCH multicast discovery (239.255.255.250:1900)
        val ssdpServer = ssdpDiscover()
        if (ssdpServer != null) {
            Logger.system { "Discovered UPnP Media Server via SSDP: ${ssdpServer.friendlyName} at ${ssdpServer.controlUrl}" }
            return@withContext ssdpServer
        }

        // 3. Probe common alternative router IPs
        for (candidate in listOf("192.168.1.1", "192.168.0.1", "192.168.1.254", "192.168.100.1")) {
            if (candidate != routerHost) {
                val found = probeHost(candidate)
                if (found != null) return@withContext found
            }
        }

        null
    }

    private fun ssdpDiscover(): UpnpServerInfo? {
        val message = "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: 2\r\n" +
                "ST: urn:schemas-upnp-org:service:ContentDirectory:1\r\n\r\n"

        return runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = 1200
                val group = InetAddress.getByName("239.255.255.250")
                val sendPacket = DatagramPacket(message.toByteArray(), message.length, group, 1900)
                socket.send(sendPacket)

                val buf = ByteArray(2048)
                val recvPacket = DatagramPacket(buf, buf.size)
                socket.receive(recvPacket)

                val response = String(recvPacket.data, 0, recvPacket.length)
                val location = response.lines()
                    .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()

                if (!location.isNullOrBlank()) {
                    fetchAndParseDeviceDescription(location)
                } else null
            }
        }.getOrNull()
    }

    private fun probeHost(host: String): UpnpServerInfo? {
        if (host.isBlank()) return null
        for (port in COMMON_PROBE_PORTS) {
            if (!isPortOpen(host, port)) continue
            for (path in COMMON_DESC_PATHS) {
                val urlStr = "http://$host:$port$path"
                val server = fetchAndParseDeviceDescription(urlStr)
                if (server != null) return server
            }
        }
        return null
    }

    private fun isPortOpen(host: String, port: Int): Boolean {
        return runCatching {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), 250)
                true
            }
        }.getOrDefault(false)
    }

    private fun fetchAndParseDeviceDescription(descUrl: String): UpnpServerInfo? {
        return runCatching {
            val url = URL(descUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 1500
                readTimeout = 2000
                requestMethod = "GET"
            }
            if (conn.responseCode != 200) return null
            val xml = conn.inputStream.bufferedReader().use { it.readText() }

            var friendlyName = "H188A: Media Server:"
            var inFriendlyName = false
            var inService = false
            var isContentDirectory = false
            var controlUrl = ""

            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "friendlyName" -> inFriendlyName = true
                            "service" -> {
                                inService = true
                                isContentDirectory = false
                            }
                            "serviceType" -> {
                                if (inService) {
                                    val text = parser.nextText()
                                    if (text.contains("ContentDirectory", ignoreCase = true)) {
                                        isContentDirectory = true
                                    }
                                }
                            }
                            "controlURL" -> {
                                if (inService && isContentDirectory) {
                                    controlUrl = parser.nextText().trim()
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inFriendlyName) {
                            val txt = parser.text.trim()
                            if (txt.isNotBlank()) friendlyName = txt
                            inFriendlyName = false
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "service") {
                            if (isContentDirectory && controlUrl.isNotBlank()) {
                                break
                            }
                            inService = false
                        }
                    }
                }
                eventType = parser.next()
            }

            if (controlUrl.isNotBlank()) {
                val fullControlUrl = if (controlUrl.startsWith("http://") || controlUrl.startsWith("https://")) {
                    controlUrl
                } else {
                    val base = "http://${url.host}:${url.port}"
                    if (controlUrl.startsWith("/")) "$base$controlUrl" else "$base/$controlUrl"
                }
                UpnpServerInfo(
                    friendlyName = friendlyName,
                    controlUrl = fullControlUrl,
                    locationUrl = descUrl,
                    host = url.host,
                    port = url.port,
                )
            } else null
        }.getOrNull()
    }

    /**
     * Browse UPnP DLNA ContentDirectory at [objectId].
     * Root container is usually "0".
     */
    suspend fun browse(controlUrl: String, objectId: String): List<StorageItem> = withContext(Dispatchers.IO) {
        val targetId = if (objectId.isBlank() || objectId == "/") "0" else objectId

        val soapEnvelope = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:Browse xmlns:u="urn:schemas-upnp-org:service:ContentDirectory:1">
                  <ObjectID>$targetId</ObjectID>
                  <BrowseFlag>BrowseDirectChildren</BrowseFlag>
                  <Filter>*</Filter>
                  <StartingIndex>0</StartingIndex>
                  <RequestedCount>300</RequestedCount>
                  <SortCriteria></SortCriteria>
                </u:Browse>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        runCatching {
            val url = URL(controlUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 6000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                setRequestProperty("SOAPAction", "\"urn:schemas-upnp-org:service:ContentDirectory:1#Browse\"")
            }

            conn.outputStream.use { os ->
                os.write(soapEnvelope.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }

            if (conn.responseCode !in 200..299) {
                Logger.e("UPNP") { "SOAP Browse failed with code ${conn.responseCode}" }
                return@withContext emptyList()
            }

            val responseXml = conn.inputStream.bufferedReader().use { it.readText() }
            val rawResult = extractResultNode(responseXml)
            if (rawResult.isNullOrBlank()) return@withContext emptyList()

            parseDidlLite(rawResult)
        }.getOrElse { err ->
            Logger.e("UPNP", err) { "Failed to browse UPnP container $targetId" }
            emptyList()
        }
    }

    private fun extractResultNode(soapXml: String): String? {
        val startTag = "<Result>"
        val endTag = "</Result>"
        val start = soapXml.indexOf(startTag)
        if (start == -1) return null
        val end = soapXml.indexOf(endTag, start)
        if (end == -1) return null
        val encoded = soapXml.substring(start + startTag.length, end).trim()
        return unescapeXml(encoded)
    }

    private fun unescapeXml(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }

    private fun parseDidlLite(didlXml: String): List<StorageItem> {
        val items = mutableListOf<StorageItem>()
        runCatching {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(didlXml))

            var eventType = parser.eventType
            var inContainer = false
            var inItem = false
            var currentId = ""
            var currentTitle = ""
            var currentResUrl = ""
            var currentProtocolInfo = ""
            var currentUpnpClass = ""
            var currentSize = 0L

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "container" -> {
                                inContainer = true
                                currentId = parser.getAttributeValue(null, "id") ?: ""
                                currentTitle = ""
                                currentUpnpClass = ""
                            }
                            "item" -> {
                                inItem = true
                                currentId = parser.getAttributeValue(null, "id") ?: ""
                                currentTitle = ""
                                currentResUrl = ""
                                currentProtocolInfo = ""
                                currentUpnpClass = ""
                                currentSize = 0L
                            }
                            "title", "dc:title" -> {
                                val t = parser.nextText().trim()
                                if (t.isNotBlank()) currentTitle = t
                            }
                            "class", "upnp:class" -> {
                                val c = parser.nextText().trim()
                                if (c.isNotBlank()) currentUpnpClass = c
                            }
                            "res" -> {
                                currentSize = parser.getAttributeValue(null, "size")?.toLongOrNull() ?: 0L
                                currentProtocolInfo = parser.getAttributeValue(null, "protocolInfo").orEmpty()
                                val url = parser.nextText().trim()
                                if (url.isNotBlank()) currentResUrl = url
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "container" -> {
                                if (currentId.isNotBlank()) {
                                    items.add(
                                        StorageItem(
                                            name = currentTitle.ifBlank { "مجلد $currentId" },
                                            path = currentId,
                                            isDirectory = true,
                                            category = FileCategory.FOLDER,
                                        )
                                    )
                                }
                                inContainer = false
                            }
                            "item" -> {
                                if (currentId.isNotBlank()) {
                                    val cat = determineCategory(
                                        name = currentTitle,
                                        resUrl = currentResUrl,
                                        upnpClass = currentUpnpClass,
                                        protocolInfo = currentProtocolInfo,
                                    )
                                    items.add(
                                        StorageItem(
                                            name = currentTitle.ifBlank { "ملف $currentId" },
                                            path = currentId,
                                            isDirectory = false,
                                            sizeBytes = currentSize,
                                            category = cat,
                                            streamUri = currentResUrl.ifBlank { null },
                                        )
                                    )
                                }
                                inItem = false
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }.onFailure { err ->
            Logger.e("UPNP", err) { "Failed to parse DIDL-Lite" }
        }

        return items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    private fun determineCategory(
        name: String,
        resUrl: String,
        upnpClass: String,
        protocolInfo: String,
    ): FileCategory {
        val upnpLower = upnpClass.lowercase()
        val protoLower = protocolInfo.lowercase()
        val urlLower = resUrl.lowercase()
        val nameLower = name.lowercase()

        return when {
            upnpLower.contains("video") || protoLower.contains("video") ||
                urlLower.matches(Regex(".*\\.(mp4|mkv|avi|mov|ts|m2ts|webm|flv|wmv)($|\\?.*)")) ||
                nameLower.matches(Regex(".*\\.(mp4|mkv|avi|mov|ts|m2ts|webm|flv|wmv)($|\\?.*)")) ||
                nameLower.contains("720p") || nameLower.contains("1080p") || nameLower.contains("2160p") ||
                nameLower.contains("4k") || nameLower.contains("bluray") || nameLower.contains("x264") ||
                nameLower.contains("x265") || nameLower.contains("web-dl") || nameLower.contains("hdrip") -> FileCategory.VIDEO

            upnpLower.contains("audio") || protoLower.contains("audio") ||
                urlLower.matches(Regex(".*\\.(mp3|m4a|aac|flac|wav|ogg|wma)($|\\?.*)")) ||
                nameLower.matches(Regex(".*\\.(mp3|m4a|aac|flac|wav|ogg|wma)($|\\?.*)")) -> FileCategory.AUDIO

            upnpLower.contains("image") || protoLower.contains("image") ||
                urlLower.matches(Regex(".*\\.(jpg|jpeg|png|webp|gif|bmp)($|\\?.*)")) ||
                nameLower.matches(Regex(".*\\.(jpg|jpeg|png|webp|gif|bmp)($|\\?.*)")) -> FileCategory.IMAGE

            nameLower.endsWith(".apk") -> FileCategory.APK
            nameLower.matches(Regex(".*\\.(pdf|txt|doc|docx|srt|sub)($|\\?.*)")) -> FileCategory.DOCUMENT
            resUrl.isNotBlank() -> FileCategory.VIDEO
            else -> FileCategory.OTHER
        }
    }

    private fun categorizeName(name: String): FileCategory {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "mkv", "avi", "mov", "ts", "m2ts", "webm", "flv", "wmv" -> FileCategory.VIDEO
            "mp3", "m4a", "aac", "flac", "wav", "ogg", "wma" -> FileCategory.AUDIO
            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> FileCategory.IMAGE
            "apk" -> FileCategory.APK
            "pdf", "txt", "doc", "docx" -> FileCategory.DOCUMENT
            else -> FileCategory.OTHER
        }
    }
}
