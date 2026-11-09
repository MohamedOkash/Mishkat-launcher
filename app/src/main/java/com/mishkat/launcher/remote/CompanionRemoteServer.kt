package com.mishkat.launcher.remote

import android.view.KeyEvent
import androidx.compose.runtime.Immutable
import com.mishkat.launcher.core.Logger
import com.mishkat.launcher.input.MishkatAction
import com.mishkat.launcher.input.RemoteUnificationLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

@Immutable
data class CompanionServerState(
    val running: Boolean = false,
    val localIp: String = "192.168.1.6",
    val port: Int = 8787,
    val connectedClientsCount: Int = 0,
    val lastCommand: String = "",
) {
    val url: String
        get() = "http://$localIp:$port"
}

sealed class CompanionCommand {
    data class Key(val keyCode: Int) : CompanionCommand()
    data class SelectTheme(val themeId: String) : CompanionCommand()
    data class SelectWallpaper(val wallpaperId: String) : CompanionCommand()
    data class PlayStationIndex(val index: Int) : CompanionCommand()
    data object ToggleLockScreen : CompanionCommand()
    data object OpenTvReceiver : CompanionCommand()
    data object OpenSportsCenter : CompanionCommand()
}

/**
 * S9: Built-in Local Wi-Fi Mobile Companion Remote Server (`http://<tv-ip>:8787`).
 * - Zero external dependencies (uses standard `java.net.ServerSocket` on `Dispatchers.IO`).
 * - Lets any Android or iOS phone scan the QR code on the TV to open the full Liquid Glass
 *   Mishkat Mobile Remote (D-Pad, Volume, Quran Radio stations, 10 Themes, 4-Seasons Wallpapers,
 *   Receiver/HDMI switcher, and Parental Lock Screen).
 */
class CompanionRemoteServer(
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(CompanionServerState())
    val state: StateFlow<CompanionServerState> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<CompanionCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<CompanionCommand> = _commands.asSharedFlow()

    private var serverJob: Job? = null
    @Volatile
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (serverJob?.isActive == true) return
        val ip = detectLocalIpv4() ?: "127.0.0.1"
        _state.value = _state.value.copy(localIp = ip)

        serverJob = scope.launch(Dispatchers.IO) {
            runCatching {
                val ss = ServerSocket(DEFAULT_PORT)
                serverSocket = ss
                _state.value = _state.value.copy(
                    running = true,
                    localIp = detectLocalIpv4() ?: ip,
                    port = ss.localPort,
                )
                Logger.system { "CompanionRemoteServer listening on http://${_state.value.localIp}:${ss.localPort}" }

                while (isActive && !ss.isClosed) {
                    val client = runCatching { ss.accept() }.getOrNull() ?: break
                    launch(Dispatchers.IO) {
                        handleClient(client)
                    }
                }
            }.onFailure { t ->
                Logger.w(Logger.TAG_SYSTEM, t) { "CompanionRemoteServer stopped or port busy" }
                _state.value = _state.value.copy(running = false)
            }
        }
    }

    fun refreshIp() {
        val ip = detectLocalIpv4() ?: _state.value.localIp
        _state.value = _state.value.copy(localIp = ip)
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            s.soTimeout = 4_000
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val pathWithQuery = parts.getOrNull(1) ?: "/"

            // Drain headers
            while (true) {
                val line = reader.readLine()
                if (line.isNullOrEmpty()) break
            }

            if (pathWithQuery.startsWith("/cmd")) {
                val query = pathWithQuery.substringAfter("?", "")
                val params = parseQueryParams(query)
                val cmdType = params["type"].orEmpty()
                val cmdVal = params["val"].orEmpty()
                dispatchCommand(cmdType, cmdVal)

                val jsonBytes = """{"ok":true,"cmd":"$cmdType","val":"$cmdVal"}""".toByteArray(Charsets.UTF_8)
                val header = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: application/json; charset=utf-8\r\n")
                    append("Access-Control-Allow-Origin: *\r\n")
                    append("Content-Length: ${jsonBytes.size}\r\n")
                    append("Connection: close\r\n\r\n")
                }.toByteArray(Charsets.UTF_8)
                val out = s.getOutputStream()
                out.write(header)
                out.write(jsonBytes)
                out.flush()
            } else {
                val htmlBytes = MOBILE_REMOTE_HTML.toByteArray(Charsets.UTF_8)
                val header = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: text/html; charset=utf-8\r\n")
                    append("Cache-Control: no-cache\r\n")
                    append("Content-Length: ${htmlBytes.size}\r\n")
                    append("Connection: close\r\n\r\n")
                }.toByteArray(Charsets.UTF_8)
                val out = s.getOutputStream()
                out.write(header)
                out.write(htmlBytes)
                out.flush()
            }
        }
    }

    internal fun dispatchCommand(type: String, value: String) {
        _state.value = _state.value.copy(
            connectedClientsCount = (_state.value.connectedClientsCount + 1).coerceAtMost(999),
            lastCommand = "$type:$value",
        )
        when (type) {
            "key" -> {
                val keyCode = when (value) {
                    "up" -> KeyEvent.KEYCODE_DPAD_UP
                    "down" -> KeyEvent.KEYCODE_DPAD_DOWN
                    "left" -> KeyEvent.KEYCODE_DPAD_LEFT
                    "right" -> KeyEvent.KEYCODE_DPAD_RIGHT
                    "ok" -> KeyEvent.KEYCODE_DPAD_CENTER
                    "back" -> KeyEvent.KEYCODE_BACK
                    "home" -> KeyEvent.KEYCODE_HOME
                    "play_pause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
                    "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    "vol_up" -> KeyEvent.KEYCODE_VOLUME_UP
                    "vol_down" -> KeyEvent.KEYCODE_VOLUME_DOWN
                    "mute" -> KeyEvent.KEYCODE_VOLUME_MUTE
                    "tv" -> KeyEvent.KEYCODE_TV
                    "input" -> KeyEvent.KEYCODE_TV_INPUT
                    "red" -> KeyEvent.KEYCODE_PROG_RED
                    "green" -> KeyEvent.KEYCODE_PROG_GREEN
                    "yellow" -> KeyEvent.KEYCODE_PROG_YELLOW
                    "blue" -> KeyEvent.KEYCODE_PROG_BLUE
                    else -> null
                }
                if (keyCode != null) {
                    RemoteUnificationLayer.dispatch(keyCode, KeyEvent.ACTION_DOWN, 0, null)
                    RemoteUnificationLayer.dispatch(keyCode, KeyEvent.ACTION_UP, 0, null)
                    _commands.tryEmit(CompanionCommand.Key(keyCode))
                }
            }
            "theme" -> _commands.tryEmit(CompanionCommand.SelectTheme(value))
            "wallpaper" -> _commands.tryEmit(CompanionCommand.SelectWallpaper(value))
            "station" -> {
                val idx = value.toIntOrNull() ?: 0
                _commands.tryEmit(CompanionCommand.PlayStationIndex(idx))
            }
            "lock" -> _commands.tryEmit(CompanionCommand.ToggleLockScreen)
            "receiver" -> _commands.tryEmit(CompanionCommand.OpenTvReceiver)
            "sports" -> _commands.tryEmit(CompanionCommand.OpenSportsCenter)
        }
    }

    companion object {
        const val DEFAULT_PORT = 8787

        internal fun parseQueryParams(query: String): Map<String, String> {
            if (query.isBlank()) return emptyMap()
            return query.split("&").mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) null
                else {
                    val k = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                    val v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                    k to v
                }
            }.toMap()
        }

        fun detectLocalIpv4(): String? = runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching null
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return@runCatching ip
                        }
                    }
                }
            }
            null
        }.getOrNull()

        private val MOBILE_REMOTE_HTML = """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
            <title>ريموت مشكاة الذكي • Mishkat TV Remote</title>
            <style>
              :root {
                --bg: #08101E;
                --glass: rgba(255,255,255,0.08);
                --border: rgba(230,193,90,0.35);
                --gold: #E6C15A;
                --text: #F8FAFC;
                --muted: #94A3B8;
              }
              * { box-sizing: border-box; user-select: none; -webkit-tap-highlight-color: transparent; }
              body {
                margin: 0; padding: 16px;
                font-family: system-ui, -apple-system, sans-serif;
                background: radial-gradient(circle at top, #13284C, #070B14 75%);
                color: var(--text);
                min-height: 100vh;
              }
              .header {
                text-align: center; padding: 12px;
                background: var(--glass); border: 1px solid var(--border);
                border-radius: 18px; margin-bottom: 14px;
                backdrop-filter: blur(14px);
              }
              .header h1 { margin: 0; font-size: 19px; color: var(--gold); }
              .header p { margin: 4px 0 0; font-size: 12px; color: var(--muted); }
              .grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 14px; }
              .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 14px; }
              .grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; margin-bottom: 14px; }
              button {
                background: var(--glass); color: var(--text);
                border: 1px solid rgba(255,255,255,0.14);
                border-radius: 14px; padding: 13px 8px;
                font-size: 13px; font-weight: 700;
                cursor: pointer; transition: transform 0.08s, background 0.15s;
              }
              button:active { transform: scale(0.94); background: rgba(230,193,90,0.25); border-color: var(--gold); }
              .gold-btn { border-color: var(--border); color: var(--gold); }
              .dpad {
                display: grid; grid-template-columns: repeat(3, 82px); grid-template-rows: repeat(3, 64px);
                gap: 10px; justify-content: center; margin: 14px 0;
              }
              .dpad .ok {
                background: linear-gradient(135deg, rgba(230,193,90,0.32), rgba(230,193,90,0.12));
                border: 2px solid var(--gold); color: var(--gold); font-size: 17px; border-radius: 50%;
              }
              .sec-title { font-size: 13px; color: var(--gold); margin: 12px 4px 8px; font-weight: 700; }
              .dot-r { color: #EF4444; } .dot-g { color: #22C55E; } .dot-y { color: #EAB308; } .dot-b { color: #3B82F6; }
            </style>
            </head>
            <body>
              <div class="header">
                <h1>✦ ريموت مشكاة الذكي • Liquid Glass</h1>
                <p id="status">متصل بالشاشة عبر الواي فاي المحلي</p>
              </div>

              <div class="grid-4">
                <button onclick="cmd('receiver','1')" class="gold-btn">📡 الريسيفر</button>
                <button onclick="cmd('sports','1')" class="gold-btn">⚽ المباريات</button>
                <button onclick="cmd('lock','1')" class="gold-btn">🔒 قفل الشاشة</button>
                <button onclick="cmd('key','play_pause')" class="gold-btn">⏯ الإذاعة</button>
              </div>

              <div class="dpad">
                <div></div>
                <button onclick="cmd('key','up')">▲</button>
                <div></div>
                <button onclick="cmd('key','right')">►</button>
                <button class="ok" onclick="cmd('key','ok')">OK</button>
                <button onclick="cmd('key','left')">◄</button>
                <div></div>
                <button onclick="cmd('key','down')">▼</button>
                <div></div>
              </div>

              <div class="grid-3">
                <button onclick="cmd('key','back')">↩ رجوع</button>
                <button onclick="cmd('key','home')">🏠 الرئيسية</button>
                <button onclick="cmd('key','mute')">🔇 كتم</button>
              </div>

              <div class="grid-4">
                <button onclick="cmd('key','red')"><span class="dot-r">●</span> الإذاعة</button>
                <button onclick="cmd('key','green')"><span class="dot-g">●</span> المدخلات</button>
                <button onclick="cmd('key','yellow')"><span class="dot-y">●</span> الرياضة</button>
                <button onclick="cmd('key','blue')"><span class="dot-b">●</span> الثيمات</button>
              </div>

              <div class="sec-title">📻 تشغيل سريع للإذاعة والقراء</div>
              <div class="grid-2">
                <button onclick="cmd('station','0')">إذاعة القرآن - القاهرة</button>
                <button onclick="cmd('station','1')">إذاعة القرآن - السعودية</button>
                <button onclick="cmd('station','2')">عبدالباسط عبدالصمد</button>
                <button onclick="cmd('station','5')">محمد صديق المنشاوي</button>
                <button onclick="cmd('station','7')">محمود خليل الحصري</button>
                <button onclick="cmd('station','12')">مشاري العفاسي</button>
              </div>

              <div class="sec-title">🎨 تغيير ثيم الشاشة فوراً</div>
              <div class="grid-2">
                <button onclick="cmd('theme','rimal')">رمال مذهّب (زجاجي)</button>
                <button onclick="cmd('theme','zomorod')">زمرد سائل</button>
                <button onclick="cmd('theme','kaaba_gold')">كسوة وذهب</button>
                <button onclick="cmd('theme','sports_arena')">ملاعب وأبطال ⚽</button>
                <button onclick="cmd('theme','cinema_action')">سينما وأكشن 🎬</button>
                <button onclick="cmd('theme','winter_snow')">شتاء وثلوج ❄</button>
                <button onclick="cmd('theme','spring_blossom')">نسيم الربيع 🌸</button>
                <button onclick="cmd('theme','summer_sunset')">غروب الصيف 🌊</button>
              </div>

              <div class="sec-title">🖼 الخلفيات المتحركة للمواسم الأربعة</div>
              <div class="grid-3">
                <button onclick="cmd('wallpaper','weather_sync')">🌦 طقس حي</button>
                <button onclick="cmd('wallpaper','winter_snow')">❄ ثلوج</button>
                <button onclick="cmd('wallpaper','winter_rain')">🌧 أمطار</button>
                <button onclick="cmd('wallpaper','spring_petals')">🌸 ربيع</button>
                <button onclick="cmd('wallpaper','summer_waves')">🌊 صيف</button>
                <button onclick="cmd('wallpaper','arabesque_gold')">🕌 أرابيسك</button>
              </div>

              <script>
                function cmd(t, v) {
                  fetch('/cmd?type=' + encodeURIComponent(t) + '&val=' + encodeURIComponent(v))
                    .then(() => {
                      document.getElementById('status').innerText = 'تم الإرسال: ' + t + ' (' + v + ')';
                    })
                    .catch(() => {
                      document.getElementById('status').innerText = 'تأكد من الاتصال بنفس شبكة الواي فاي';
                    });
                }
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
