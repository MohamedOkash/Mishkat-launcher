package com.mishkat.launcher.storage

import com.mishkat.launcher.core.Logger
import jcifs.CIFSContext
import jcifs.context.BaseContext
import jcifs.config.PropertyConfiguration
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Properties

/**
 * High-performance embedded streaming proxy for ExoPlayer:
 * Bridges remote Samba SMB and FTP file streams to a local HTTP URL on 127.0.0.1
 * with full HTTP Range seeking (fast-forward, rewind, resume).
 */
class LocalStreamProxy(
    private val scope: CoroutineScope,
) {
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    var port: Int = 8790
        private set

    var settings: SmbFtpSettings = SmbFtpSettings()

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            runCatching {
                val ss = ServerSocket(0)
                serverSocket = ss
                port = ss.localPort
                Logger.system { "LocalStreamProxy listening on http://127.0.0.1:$port" }

                while (isActive && !ss.isClosed) {
                    val client = runCatching { ss.accept() }.getOrNull() ?: break
                    launch(Dispatchers.IO) {
                        handleClient(client)
                    }
                }
            }.onFailure { t ->
                Logger.w(Logger.TAG_SYSTEM, t) { "LocalStreamProxy stopped" }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    fun getStreamUrl(type: StorageSourceType, rawPath: String, settings: SmbFtpSettings): String {
        val encPath = java.net.URLEncoder.encode(rawPath, "UTF-8")
        val typeStr = when (type) {
            StorageSourceType.ROUTER_SMB -> "smb"
            StorageSourceType.ROUTER_FTP -> "ftp"
            StorageSourceType.LOCAL_USB -> "local"
        }
        return "http://127.0.0.1:$port/stream?src=$typeStr&path=$encPath"
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            s.soTimeout = 10_000
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val reqLine = reader.readLine() ?: return
            val parts = reqLine.split(" ")
            val pathWithQuery = parts.getOrNull(1) ?: return
            val query = pathWithQuery.substringAfter("?", "")
            val params = parseParams(query)

            var rangeHeader: String? = null
            while (true) {
                val line = reader.readLine()
                if (line.isNullOrEmpty()) break
                if (line.startsWith("Range:", ignoreCase = true)) {
                    rangeHeader = line.substringAfter(":").trim()
                }
            }

            val srcType = params["src"] ?: "local"
            val targetPath = params["path"] ?: ""
            if (targetPath.isBlank()) return

            when (srcType) {
                "local" -> streamLocalFile(s.getOutputStream(), targetPath, rangeHeader)
                "smb" -> streamSmbFile(s.getOutputStream(), targetPath, rangeHeader)
                "ftp" -> streamFtpFile(s.getOutputStream(), targetPath, rangeHeader)
            }
        }
    }

    private fun streamLocalFile(out: OutputStream, path: String, rangeHeader: String?) {
        val file = java.io.File(path)
        if (!file.exists() || !file.canRead()) return
        val totalLen = file.length()
        val (start, end) = parseRange(rangeHeader, totalLen)
        val contentLen = end - start + 1
        val mime = getMimeType(file.name)

        sendHeaders(out, start > 0, start, end, totalLen, contentLen, mime)
        file.inputStream().use { input ->
            if (start > 0) input.skip(start)
            copyStream(input, out, contentLen)
        }
    }

    private fun streamSmbFile(out: OutputStream, smbUrl: String, rangeHeader: String?) {
        runCatching {
            val prop = Properties().apply {
                setProperty("jcifs.smb.client.minVersion", "SMB1")
                setProperty("jcifs.smb.client.maxVersion", "SMB311")
                setProperty("jcifs.smb.client.ipcSigningEnforced", "false")
                setProperty("jcifs.smb.client.responseTimeout", "10000")
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
            val smbFile = SmbFile(smbUrl, authContext)
            val totalLen = smbFile.length()
            val (start, end) = parseRange(rangeHeader, totalLen)
            val contentLen = end - start + 1
            val mime = getMimeType(smbFile.name)

            sendHeaders(out, start > 0, start, end, totalLen, contentLen, mime)
            smbFile.inputStream.use { input ->
                if (start > 0) input.skip(start)
                copyStream(input, out, contentLen)
            }
        }.onFailure { t ->
            Logger.w(Logger.TAG_SYSTEM, t) { "streamSmbFile error" }
        }
    }

    private fun streamFtpFile(out: OutputStream, ftpPath: String, rangeHeader: String?) {
        runCatching {
            val uri = java.net.URI(ftpPath)
            val host = if (!uri.host.isNullOrBlank()) uri.host else settings.ftpHost
            val port = if (uri.port > 0) uri.port else settings.ftpPort
            val path = uri.path.ifBlank { "/" }
            val ftp = FTPClient()
            ftp.connect(host, port)
            val user = if (settings.ftpIsAnonymous) "anonymous" else settings.ftpUser
            val pass = if (settings.ftpIsAnonymous) "" else settings.ftpPass
            if (!ftp.login(user, pass)) {
                ftp.login("guest", "")
            }
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            ftp.controlEncoding = "UTF-8"
            ftp.connectTimeout = 10000
            ftp.soTimeout = 10000

            val files = ftp.listFiles(path)
            val totalLen = files?.firstOrNull()?.size ?: -1L
            val (start, end) = parseRange(rangeHeader, if (totalLen > 0) totalLen else 0L)
            val contentLen = if (totalLen > 0) (end - start + 1) else -1L
            val mime = getMimeType(path)

            sendHeaders(out, start > 0, start, end, totalLen, contentLen, mime)
            if (start > 0) {
                ftp.setRestartOffset(start)
            }
            ftp.retrieveFileStream(path)?.use { input ->
                copyStream(input, out, if (contentLen > 0) contentLen else Long.MAX_VALUE)
            }
            ftp.disconnect()
        }.onFailure { t ->
            Logger.w(Logger.TAG_SYSTEM, t) { "streamFtpFile error" }
        }
    }

    private fun sendHeaders(
        out: OutputStream,
        isRange: Boolean,
        start: Long,
        end: Long,
        totalLen: Long,
        contentLen: Long,
        mime: String
    ) {
        val status = if (isRange) "206 Partial Content" else "200 OK"
        val sb = StringBuilder()
        sb.append("HTTP/1.1 $status\r\n")
        sb.append("Content-Type: $mime\r\n")
        sb.append("Accept-Ranges: bytes\r\n")
        if (contentLen >= 0) {
            sb.append("Content-Length: $contentLen\r\n")
        }
        if (isRange && totalLen > 0) {
            sb.append("Content-Range: bytes $start-$end/$totalLen\r\n")
        }
        sb.append("Connection: close\r\n\r\n")
        out.write(sb.toString().toByteArray(Charsets.UTF_8))
        out.flush()
    }

    private fun copyStream(input: InputStream, out: OutputStream, maxBytes: Long) {
        val buf = ByteArray(64 * 1024)
        var remaining = maxBytes
        while (remaining > 0) {
            val toRead = remaining.coerceAtMost(buf.size.toLong()).toInt()
            val r = input.read(buf, 0, toRead)
            if (r <= 0) break
            out.write(buf, 0, r)
            remaining -= r
        }
        out.flush()
    }

    private fun parseRange(rangeHeader: String?, totalLength: Long): Pair<Long, Long> {
        if (rangeHeader.isNullOrBlank() || !rangeHeader.startsWith("bytes=")) {
            return 0L to (totalLength - 1).coerceAtLeast(0L)
        }
        val spec = rangeHeader.removePrefix("bytes=").trim()
        val parts = spec.split("-")
        val start = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val end = parts.getOrNull(1)?.toLongOrNull() ?: (totalLength - 1).coerceAtLeast(start)
        return start to end
    }

    private fun parseParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&").mapNotNull {
            val idx = it.indexOf('=')
            if (idx <= 0) null
            else {
                val k = URLDecoder.decode(it.substring(0, idx), "UTF-8")
                val v = URLDecoder.decode(it.substring(idx + 1), "UTF-8")
                k to v
            }
        }.toMap()
    }

    private fun getMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "ts", "m2ts" -> "video/mp2t"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }
}
